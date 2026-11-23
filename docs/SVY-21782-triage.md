# Triage Report — SVY-21782

**Verdict:** PROCEED

## Reported problem
`plugins.clientmanager.getLocks()` uses memory and time quadratic in the number of locked rows. A lock on about 225,000 rows led to an OutOfMemoryError in production. The reporter's measurements show about 80 bytes x n² of allocation, held until the call returns. While the call runs, other clients block on lock handling and saves.

The ticket asks for three things:
1. Linear memory and time.
2. `getLocks()` must not block other clients longer than it takes to read the list.
3. Optionally, a safeguard such as a maximum or a log warning.

## Root-cause assessment
The root cause is confirmed in code. I did not run the reproduction. The reporter's numbers match the code path exactly.

Call chain: `ClientManagerProvider.js_getLocks()` -> `ServerAccessProvider.getLocks()` (j2db_server) -> `SQLEngine.getLocks()` -> `LockManager.getLocks()`.

**`LockManager.getLocks()`** (`server/j2db_server/.../dataprocessing/LockManager.java:244`)
- `locks` is `Map<(server,table), Map<pkHash, Lock>>`.
- `acquireLocks` creates ONE `Lock` object per acquire call. That `Lock` holds a `TreeSet` of all n pk hashes. The same `Lock` instance is put into the per-datasource map under each of the n keys (lines 79-83).
- `getLocks()` iterates the n entries and calls `entry2.getValue().clone()` for each one (line 255).
- `Lock.clone()` (`servoy_shared/.../server/shared/Lock.java:63`) copies the TreeSet twice. It runs `new TreeSet(pkHashKeySet)` in `clone()`, and the constructor then runs `new TreeSet(pkHashKeySet)` again.
- Result: n clones x 2 copies x n entries = 2n² TreeSet entries. At about 40 bytes per entry that is about 80 n² bytes, which matches the measurements (8,000 rows -> about 4.9 GB).
- The whole copy runs inside `synchronized (locks)`. This is why all lock acquires, releases and checks (`acquireLocks`, `releaseLocks`, `isClientLockOwner`, `hasLocksForOtherClients`) hang for the duration. It is also why saves hang, since they check lock ownership.
- The n² data is garbage right after the copy, but it is all live inside the call. That is why peak equals allocation.

**`ServerAccessProvider.getLocks()`** (line 536)
- It builds a dataset with one row per pk key and reads only `getServerName()`, `getTableName()`, `getAcquired()` and `getClientId()` from the lock. It never needs the pk set of the clone. The deep copy is wasted work here.

**Same pattern elsewhere**
- `ApplicationServer.ServerStatus.getLocks()` (`ApplicationServer.java:1588`) clones every `Lock` per entry again. This is a second n² pass, and it also runs on the already-cloned copies. It is used by the JMX `LocksBean`.
- `LockManager.getClientLockSet()` is fine. It clones each distinct `Lock` once, so it is O(total keys).
- `Lock.getPkHashKeys()` copies the set on each call. `LocksServlet` and `ConfigServlet` call `sqlEngine.getLocks()` as well, so they pay the same cost. I did not check them for further per-entry copying.

**Not verified**
- The reporter says `acquireLock()` is also superlinear (8x rows -> 15x time). `LockManager.acquireLocks` looks O(n log n) (TreeSet build plus hash puts), so the cause is probably n log n plus client-side work in `FoundSetManager.acquireLock`. I did not investigate this. It is a secondary observation, not the cause of the OOM.

## Ticket premise check
- The problem is in this product's code. It is not user error or third-party behaviour.
- The ticket proposes no specific fix, only expectations. Expectations 1 and 2 are valid and achievable.
- The returned dataset has n rows, so a linear result is the floor. 225,000 rows of (datasource, date, clientId, pkHash) is acceptable.
- "Blocking other clients only as long as it takes to read the lock list" is achievable. Once the copy is O(n) it is short. Any further reduction would mean not copying under the monitor, which would need a more invasive change.
- The ticket's "maximum or warning" safeguard is optional. It becomes mostly unnecessary once the cost is linear.

## Approaches considered
1. **Clone each distinct `Lock` once (identity-based dedupe) in `LockManager.getLocks()`, and fix `Lock.clone()` to copy the TreeSet once.**
   - Pros:
     - Minimal change. All consumers keep their API. It is O(n) in time and memory.
     - The lock is held for much less time.
     - Fixes the JMX, servlet and plugin paths together.
   - Cons:
     - The map entries sharing one cloned `Lock` instance is a small semantic change. Callers get a snapshot where keys of the same acquire share an instance, which mirrors the real structure.
     - It still copies each pk set once.
2. **Lightweight snapshot for `ServerAccessProvider.getLocks()` that avoids copying pk sets.**
   - Pros:
     - Avoids all TreeSet copying for the plugin path.
     - Per-row data is read directly under the monitor.
   - Cons:
     - Needs a new `LockManager`/`SQLEngine`/`IDataServer` method, or a change to the existing contract. `IDataServer` is a remote interface shared across repos.
     - The widest change.
3. **Add a cap or a warning log for large lock lists.**
   - Pros: protects the server against huge results.
   - Cons:
     - It does not fix the quadratic cost.
     - It changes the API semantics (truncated results).
     - Not needed once the cost is linear. Optional.
4. **No code change.**
   - Pros: none.
   - Cons:
     - The defect is real and reproducible. The production outage was caused by it.
     - Not acceptable.

## Recommendation
**PROCEED with approach 1.** Concretely:
- `LockManager.getLocks()`: clone each distinct `Lock` once, using an `IdentityHashMap<Lock, Lock>`, and put the shared clone under each key. Keep the work inside the monitor, but now O(n).
- `Lock.clone()`: avoid the double TreeSet copy. For example, add a private copy constructor or construct directly from the existing set.
- `ApplicationServer.ServerStatus.getLocks()` (`ApplicationServer.java:1588`): return each distinct lock once. Dedupe by identity, or iterate distinct values. It does not need to clone again, because `LockManager.getLocks()` already returns copies. The result is then one `Lock` per acquire, not one per key. Check how the JMX `LocksBean` consumes it before changing what it returns.
- Optional follow-up: have `ServerAccessProvider.getLocks()` avoid the extra copy (approach 2) if profiling still shows a cost at 225k rows. Add a unit test in `j2db_test` or an equivalent location. It should lock about 10k keys and assert that the allocation and time stay linear, or at least that `getLocks()` finishes and returns n rows.
- Tell the reporter that the safeguard (approach 3) is not planned unless they insist.

Note: the changes are in the `server` repo (`j2db_server`) and in `servoy_shared` (`Lock.java`) in this repo, so two repos are touched. The clientmanager plugin in servoy-extensions needs no change.

## Git history findings
- `LockManager.java`: the last substantive changes are `7086f7b6` (2018-08-02, SVY-12660, removal of Terracotta) and `f22e0ebb` (2013, package split). The per-entry clone in `getLocks()` dates from the original design. The "Terracotta READ lock" comments are leftovers.
- `Lock.java` (servoy_shared): it comes from the 2010 `ServerStatus` addition (`a6d2a46d8`). The double TreeSet copy in `clone()` has been there since then.
- There is no prior spec in `docs/` for SVY-21782, and no intentional decision against sharing clones. The comment in `ApplicationServer.getLocks()` ("return a copy so the caller cannot change lockmanager admin") only wants the caller isolated from internal state, which a single shared snapshot still provides.
