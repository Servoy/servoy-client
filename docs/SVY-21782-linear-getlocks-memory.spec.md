# Spec: SVY-21782 — plugins.clientmanager.getLocks() uses quadratic memory/time

(Revision 2: replaces the "clone each distinct Lock" design with a read-only `ILock` view, per reviewer feedback.)

## 1. Goal
Make `LockManager.getLocks()` and its consumers O(n) in time and memory for n locked rows. A single acquire of about 225,000 rows must no longer cause an OutOfMemoryError, and `getLocks()` must hold the `locks` monitor only for a linear copy of the key maps.

Approved direction (triage approach 1, refined by the reviewer): do not clone `Lock` objects for the outside world at all. Expose them through a read-only interface `ILock`. Only `LockManager` knows the mutable `Lock` class.

## 2. Background
Call chain: `ClientManagerProvider.js_getLocks()` (servoy-extensions, no change) -> `ServerAccessProvider.getLocks()` -> `SQLEngine.getLocks()` -> `LockManager.getLocks()`.

`LockManager.acquireLocks` creates ONE `Lock` per acquire. It holds a `TreeSet` of all n pk hashes. The same instance is stored under each of the n keys in `locks: Map<(server,table), Map<pkHash, Lock>>`.

`LockManager.getLocks()` runs `entry2.getValue().clone()` once per key, inside `synchronized (locks)`. `Lock.clone()` copies the set twice (once explicitly, once in the constructor). Result: 2n² set entries, about 80·n² bytes, all live, while all lock operations block.

`Lock` has all-final fields. The only mutator is `removePkHashKey`, which is called only from `LockManager.releaseLocks`. `getPkHashKeys()` already returns a copy. So `clone()` is only needed to protect the internal set from `removePkHashKey`, which outside code could call today.

Users of `Lock` (all in the `server` repo, bundle `j2db_server`): `LockManager`, `SQLEngine`, `ServerAccessProvider`, `ApplicationServer` (`ServerStatus.getLocks()`), `LocksBean`, `ConfigServlet` (line ~2311 `getPkHashKeys()`), `LocksServlet`. Only `LockManager` uses `removePkHashKey` (a grep over `D:\git\servoy_master` found no other caller).

Git history (from triage): the per-entry clone dates from the original design; the double copy in `Lock.clone()` since `a6d2a46d8` (2010). No recorded intent against sharing instances. The comment "return a copy so the caller cannot change lockmanager admin" only requires that callers cannot mutate internal state. A read-only interface satisfies that.

Repos:
- `D:\git\servoy_master\servoy-client\servoy_shared\src\com\servoy\j2db\server\shared\` (bundle `servoy_shared`, this repo): `Lock.java`, new `ILock.java`.
- `D:\git\servoy_master\server\j2db_server\src\...` (`server` repo): the consumers above.

## 3. Design
### 3.1 New `ILock` (servoy_shared, package `com.servoy.j2db.server.shared`)
Read-only view with the getters only:

```java
public interface ILock
{
	String getServerName();
	String getTableName();
	Set<Object> getPkHashKeys();   // returns a copy
	int getPkHashKeySize();
	String getClientId();
	long getAcquired();
}
```

No `removePkHashKey`, no `clone`. Package is already exported by `servoy_shared` (it contains `Lock`), so no MANIFEST change is expected. Verify.

### 3.2 `Lock implements ILock`, thread-safe set access
- `Lock` stays public (the constructor is used by `LockManager`) and keeps `removePkHashKey`. Add `ILock` to its `implements` list.
- Because snapshots now hand out the live instance, a consumer can call `getPkHashKeys()` / `getPkHashKeySize()` outside the `LockManager` monitor while `releaseLocks` mutates the set. Make the three methods that touch `pkHashKeySet` `synchronized` (`removePkHashKey`, `getPkHashKeys`, `getPkHashKeySize`). Lock order is always `LockManager.locks` then `Lock`; a consumer takes only the `Lock` monitor, so no deadlock is possible. The constructor copy needs no sync (object not yet published).
- `compareTo` reads `pkHashKeySet.hashCode()`, which iterates the set. It is used by `getClientLockSet()`'s `TreeSet<Lock>` and by `clientLockMap` `HashSet<Lock>` (identity hashing, no `hashCode` override). Leave `compareTo` as is and mention the thread-safety caveat in a comment. It is only called under the `LockManager` monitor.
- Remove `clone()` and the `Cloneable` marker. `getClientLockSet()` is only used by `ConfigServlet` (via `SQLEngine.getClientLockSet`) to display the locks per client, so it can return read-only `ILock`s too. The implementer must grep for any other `Lock.clone()` caller in all checkouts before removing it.
- The `Lock.java` copies in other checkouts (servoy-extensions, servoy-plugins) are build artifacts copied from `servoy_shared` so those projects compile. They are not edited by hand and need no separate change; they pick up the new `Lock` and `ILock` from `servoy_shared` when copied.

### 3.3 `LockManager.getLocks()` returns a snapshot of the maps, not of the locks
Change the return type to `Map<Pair<String,String>, Map<Object, ILock>>`. Copy only the key -> lock maps (same `TreeMap` structure as today), and put the same `Lock` reference under each key, typed as `ILock`. No `clone()`. Cost: O(total keys) time and memory, one reference per key. The monitor is held for this single pass only.

Semantics:
- The key set and key->lock mapping are a point-in-time snapshot.
- The pk set inside a lock is live: it can shrink after the snapshot if rows are released. Document this in the Javadoc of `getLocks()`. `ServerAccessProvider.getLocks()` builds its rows from the map keys and the lock's server/table/client/acquired, so it is unaffected.

### 3.4 Propagate `ILock` to consumers (server repo)
- `SQLEngine.getLocks()`: return type uses `ILock` in the map.
- `ServerAccessProvider.getLocks()`: use `ILock` for the values. No logic change.
- `IServerStatus.getLocks()` / `ApplicationServer.ServerStatus.getLocks()`: nothing uses them. The only `IServerStatus` use in the `server` repo is `ConfigServlet` ~8208 (`getServerStatus().clearPerformanceData(target)`). Remove `getLocks()` from the `IServerStatus` interface (`servoy_shared`) and from `ServerStatus`. This is safer than keeping a dead quadratic method. The implementer must grep all checkouts for other callers first (none found in `server` / `servoy-client`; `TestApplicationServer` only stubs `getServerStatus()`). Removing `IServerStatus` entirely (ConfigServlet using the performance registry directly) is out of scope.
- `LocksBean.getLocks()`: `LockData` is an immutable copy (strings plus a `String[]` of pk keys), so one instance can be shared. Use a local `IdentityHashMap<ILock, LockData>`: create one `LockData` per distinct lock and put the same instance in the result for every key of that lock. The result keeps one entry per key as today, so JMX consumers see no change, and cost is O(total keys).
- `ConfigServlet` (~2311, ~2407, ~2582) and `LocksServlet`: switch types to `ILock`. They only read. Where `getPkHashKeys()` is called per key, call it once per distinct lock.
- `LockManager.getClientLockSet()` / `SQLEngine.getClientLockSet()`: only used by `ConfigServlet` (~2280). Return `Set<ILock>` holding the live `Lock` references (new `HashSet`, no cloning, no `TreeSet` because `Lock.compareTo` is O(n)).

### 3.5 Explicitly unchanged
- `IDataServer` remote API (approach 2), cap/warning safeguard (approach 3), `ClientManagerProvider` in servoy-extensions, the superlinear client-side `acquireLock()` time.

## 4. Implementation plan
Use `eclipse-coder` tools via Code Mode `execute`. Run `getCompilationErrors` after each edit.

1. **servoy-client / `servoy_shared`**: create `ILock.java` (3.1); update `Lock.java` (3.2: `implements ILock`, `synchronized` on the three methods, remove `clone()` and `Cloneable`, comments).
   - Also in `servoy_shared`: remove `getLocks()` from `IServerStatus.java` (3.4).
2. **server repo / `j2db_server`**: `LockManager.getLocks()` and `getClientLockSet()` (3.3, 3.4), then fix compile errors in `SQLEngine`, `ServerAccessProvider`, `ApplicationServer` (remove `ServerStatus.getLocks()`), `LocksBean` (shared `LockData`), `ConfigServlet`, `LocksServlet`.
3. Grep all checkouts for `IServerStatus.getLocks` and `Lock.clone()` callers; record findings in the commit/PR description.
4. One commit per repo, subject: `SVY-21782 make getLocks() linear in memory and time [ai]`.

### Testing plan
- **`Lock` test (this repo)**: `LockTest` in `servoy_ngclient.tests` (if it can see `servoy_shared`), run with `eclipse-ide_runClassTests`. Cases: `getPkHashKeys()` returns an independent copy; `removePkHashKey` changes size; `clone()` is independent of the original; concurrent `removePkHashKey` + `getPkHashKeys()` from two threads gives no `ConcurrentModificationException`.
- **`LockManagerTest` (server repo)**: `LockManager` is package-private in `j2db_server`. Preferred placement (reviewer's wish): a separate `test` source folder in `j2db_server`, as in `servoy-extensions/com.servoy.extensions` (`test/` folder, `.classpath` entry with `test=true` attribute and output `target/test-classes`, `testSourceDirectory` in the pom, surefire + JUnit 6). Caveat to resolve first: `com.servoy.extensions` builds as a Maven project with `testSourceDirectory`, while `j2db_server` uses `eclipse-plugin` Tycho packaging with `build.properties` (`source.. = src/`), where plain surefire is not bound and a `test` folder would not be compiled or run by default. The implementer must check how `com.servoy.extensions` gets its tests run under Tycho (pom lines ~260-312) and whether the same setup works for `j2db_server` (including the ZKM obfuscation and `maven-jar-plugin` executions), WITHOUT putting JUnit in the production MANIFEST and WITHOUT packaging the test classes into the jar (keep `test/` out of `build.properties` `bin.includes`/`source..`). The package-private `LockManager` is reachable only if tests use the same package name (same-classpath, no OSGi split). If this cannot be made to work cleanly, fall back to a new `j2db_server_test` fragment project (`Fragment-Host: j2db_server`, like `servoy_ngclient.tests`) and record the decision in the PR. Cases:
  - acquire N = 20,000 keys; `getLocks()` returns N keys, all pointing to the same `ILock` instance, with `getPkHashKeySize() == N`.
  - two acquires by different clients give two distinct instances.
  - `getLocks()` for N = 20,000 finishes within a generous bound (old code needed ~800M set entries).
  - after `releaseLocks` of some keys, a new `getLocks()` shows the reduced key set.
- **Manual**: acquire locks on about 225,000 rows (`databaseManager.acquireLock(foundset, 0)`), call `plugins.clientmanager.getLocks()`; heap stays flat, the dataset has 225,000 rows, other clients are not noticeably blocked. Open the JMX `LocksBean` and the admin `LocksServlet` page.

## 5. Acceptance criteria
- [ ] `ILock` exists with getters only. `Lock implements ILock`; `removePkHashKey` is not reachable through any type returned outside `LockManager`.
- [ ] `LockManager.getLocks()` does not clone any `Lock`. Keys of one acquire share one instance in the snapshot.
- [ ] `getLocks()` memory and time are O(total locked keys). A single lock of 225,000 rows no longer causes an OutOfMemoryError.
- [ ] `Lock.getPkHashKeys()`, `getPkHashKeySize()` and `removePkHashKey()` are synchronized; no `ConcurrentModificationException` under concurrent release and read.
- [ ] `IServerStatus.getLocks()` and `ServerStatus.getLocks()` are removed (no callers).
- [ ] `LocksBean.getLocks()` builds one `LockData` per distinct lock and shares it across keys; the result shape (one entry per key) is unchanged.
- [ ] `getClientLockSet()` returns `Set<ILock>` without cloning; `Lock.clone()` and `Cloneable` are removed.
- [ ] `ServerAccessProvider.getLocks()` returns the same rows as before for a quiescent lock table.
- [ ] `ConfigServlet`, `LocksServlet`, `LocksBean.releaseLocks` behave unchanged.
- [ ] Tests exist as described, or their absence is documented with a manual verification record.
- [ ] No compilation errors in `servoy_shared` and `j2db_server`; no new high-severity Spotbugs findings.

## 6. Out of scope
- A lightweight snapshot API for `ServerAccessProvider.getLocks()` (approach 2, `IDataServer` change).
- A maximum size or log warning (approach 3).
- Copying outside the monitor; the superlinear client-side `acquireLock()` time; changes to `ClientManagerProvider`.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| Does any external code (other checkouts) call `IServerStatus.getLocks()`? Reviewer says no. | Implementer | Open: confirm by grep before removing |
| Any other caller of `Lock.clone()` in any checkout? | Implementer | Open: confirm by grep |
| Can a `test` source folder in the Tycho `eclipse-plugin` `j2db_server` be compiled and run (like `com.servoy.extensions/test`) without leaking into the jar or the MANIFEST? Fallback: `j2db_server_test` fragment. | Implementer / Maintainer | Open |
| `Lock.java` copies in other checkouts | Reviewer | Resolved: build artifacts copied from `servoy_shared` |
| `LockData` mutability | Reviewer | Resolved: immutable, can be shared |
