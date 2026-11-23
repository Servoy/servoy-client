package com.servoy.j2db.server.shared.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.servoy.j2db.server.shared.Lock;

class LockTest
{
	private static Set<Object> keys(int n)
	{
		Set<Object> set = new HashSet<>();
		for (int i = 0; i < n; i++)
		{
			set.add(Integer.valueOf(i));
		}
		return set;
	}

	@Test
	@DisplayName("getPkHashKeys returns an independent copy")
	void getPkHashKeysIsCopy()
	{
		Lock lock = new Lock("srv", "tbl", keys(5), "client", 1L);
		Set<Object> copy = lock.getPkHashKeys();
		copy.clear();
		copy.add("other");
		assertAll(() -> assertEquals(5, lock.getPkHashKeySize(), "lock must not be changed through the returned set"),
			() -> assertEquals(keys(5), lock.getPkHashKeys(), "keys must be unchanged"));
	}

	@Test
	@DisplayName("constructor copies the given set")
	void constructorCopiesInput()
	{
		Set<Object> input = keys(5);
		Lock lock = new Lock("srv", "tbl", input, "client", 1L);
		input.clear();
		assertEquals(5, lock.getPkHashKeySize(), "mutating the input set must not change the lock");
	}

	@Test
	@DisplayName("removePkHashKey shrinks the lock")
	void removeChangesSize()
	{
		Lock lock = new Lock("srv", "tbl", keys(5), "client", 1L);
		assertAll(() -> assertTrue(lock.removePkHashKey(Integer.valueOf(2)), "existing key is removed"),
			() -> assertFalse(lock.removePkHashKey(Integer.valueOf(2)), "second removal returns false"),
			() -> assertFalse(lock.removePkHashKey(Integer.valueOf(99)), "unknown key returns false"),
			() -> assertEquals(4, lock.getPkHashKeySize()),
			() -> assertFalse(lock.getPkHashKeys().contains(Integer.valueOf(2))));
	}

	@Test
	@DisplayName("getters return the constructor values")
	void getters()
	{
		Lock lock = new Lock("srv", "tbl", keys(1), "client", 42L);
		assertAll(() -> assertEquals("srv", lock.getServerName()), () -> assertEquals("tbl", lock.getTableName()),
			() -> assertEquals("client", lock.getClientId()), () -> assertEquals(42L, lock.getAcquired()));
	}

	@Test
	@Timeout(60)
	@DisplayName("concurrent remove and read gives no ConcurrentModificationException")
	void concurrentRemoveAndRead() throws Exception
	{
		int n = 20000;
		Lock lock = new Lock("srv", "tbl", keys(n), "client", 1L);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		CountDownLatch start = new CountDownLatch(1);

		Thread remover = new Thread(() -> {
			try
			{
				start.await();
				for (int i = 0; i < n; i++)
				{
					lock.removePkHashKey(Integer.valueOf(i));
				}
			}
			catch (Throwable t)
			{
				failure.compareAndSet(null, t);
			}
		});
		Thread reader = new Thread(() -> {
			try
			{
				start.await();
				while (lock.getPkHashKeySize() > 0)
				{
					// iterating the copy would throw ConcurrentModificationException if the copy were not taken under the lock
					int count = 0;
					for (Object o : lock.getPkHashKeys())
					{
						if (o != null) count++;
					}
					assertTrue(count <= n);
				}
			}
			catch (Throwable t)
			{
				failure.compareAndSet(null, t);
			}
		});
		remover.start();
		reader.start();
		start.countDown();
		remover.join(TimeUnit.SECONDS.toMillis(50));
		reader.join(TimeUnit.SECONDS.toMillis(50));
		assertNull(failure.get(), "no exception expected in worker threads: " + failure.get());
		assertEquals(0, lock.getPkHashKeySize());
	}
}
