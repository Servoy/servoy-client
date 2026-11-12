package com.servoy.j2db.server.ngclient.property.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;

import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.IWebFormController;

/**
 * Tests the never-ending client/server auto-correction loop avoidance of {@link DataproviderTypeSabloValue}
 * (its private {@code AvoidCorrectionLoopsWithClient}), exercised through {@link DataproviderTypeSabloValue#checkValueForChanges}.
 * <p>
 * The test value has no record and a form without a form scope, so the "server side value" of the dataprovider is always
 * <code>null</code>. A client push of a non-null value is therefore always corrected by the server (back to null), while a
 * client push of <code>null</code> is accepted as is.
 * <p>
 * Behavior pinned here (MAX_LOOP_COUNT = 10, MAX_CORRECTION_TWO_WAY_LOOP_TIME_MS = 3000): every correction that follows the
 * previous one by less than the time window adds 1 to the loop count; the first correction and corrections that come later than the
 * time window do not. A fast loop therefore lets {@value #FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT} corrections through to the client
 * and interrupts on the next one. The time of the previous correction is moved back by reflection instead of sleeping.
 */
@SuppressWarnings("nls")
class DataproviderTypeSabloValueCorrectionLoopTest
{

	private static final int FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT = 10;
	private static final long WELL_INSIDE_TIME_WINDOW_MS = 500;
	private static final long WELL_OUTSIDE_TIME_WINDOW_MS = 3500;
	private static final String CLIENT_VALUE = "valueFromClient";

	private final AtomicInteger valueChangedCount = new AtomicInteger();
	private final List<Runnable> pendingEvents = new ArrayList<>();
	private IJSONStringWithClientSideType jsonSentToClient;
	private DataproviderTypeSabloValue value;

	@BeforeEach
	void setUp()
	{
		INGApplication application = proxy(INGApplication.class, (method, args) -> {
			if ("invokeLater".equals(method.getName())) pendingEvents.add((Runnable)args[0]);
			return null;
		});
		IWebFormController form = proxy(IWebFormController.class, (method, args) -> null); // getFormScope() == null => server side value is null
		IServoyDataConverterContext context = proxy(IServoyDataConverterContext.class, (method, args) -> {
			if ("getApplication".equals(method.getName())) return application;
			if ("getForm".equals(method.getName())) return form;
			return null;
		});
		IDataAdapterList dataAdapterList = proxy(IDataAdapterList.class, (method, args) -> null);
		PropertyDescription dpPD = new PropertyDescriptionBuilder().withName("slider").build();

		jsonSentToClient = proxy(IJSONStringWithClientSideType.class, (method, args) -> null);
		value = new DataproviderTypeSabloValue("myDataprovider", dataAdapterList, context, dpPD);
		value.changeMonitor = valueChangedCount::incrementAndGet;
	}

	/** Client pushes a value that the server does not accept: server value (null) differs from what client has. */
	private void clientPushesValueAndServerCorrectsIt()
	{
		value.uiValue = CLIENT_VALUE;
		value.jsonValue = jsonSentToClient;
		value.checkValueForChanges(null);
	}

	/** Client pushes a value that the server accepts as is (it equals the server side value, null). */
	private void clientPushIsAccepted()
	{
		value.uiValue = null;
		value.jsonValue = jsonSentToClient;
		value.checkValueForChanges(null);
	}

	private void repeatCorrections(int count, long previousCorrectionMsAgo) throws Exception
	{
		for (int i = 0; i < count; i++)
		{
			if (i > 0 && previousCorrectionMsAgo >= 0) setPreviousCorrectionTimeToMsAgo(previousCorrectionMsAgo);
			clientPushesValueAndServerCorrectsIt();
		}
	}

	private void setPreviousCorrectionTimeToMsAgo(long msAgo) throws Exception
	{
		Field loopAvoiderField = DataproviderTypeSabloValue.class.getDeclaredField("avoidCorrectionLoopsWithClient");
		loopAvoiderField.setAccessible(true);
		Object loopAvoider = loopAvoiderField.get(value);
		Field lastCorrectionTime = loopAvoider.getClass().getDeclaredField("lastCorrectionTimeMs");
		lastCorrectionTime.setAccessible(true);
		lastCorrectionTime.setLong(loopAvoider, System.currentTimeMillis() - msAgo);
	}

	private void runPendingEvents()
	{
		List<Runnable> toRun = new ArrayList<>(pendingEvents);
		pendingEvents.clear();
		toRun.forEach(Runnable::run);
	}

	private void assertLastCorrectionWasSentToClient()
	{
		assertNull(value.uiValue, "corrected (server) value should be the new ui value");
		assertNull(value.jsonValue, "cached client representation should be invalidated so the corrected value is re-sent");
	}

	private void assertLastCorrectionWasIgnored()
	{
		assertSame(CLIENT_VALUE, value.uiValue, "ui value should be left as the client sent it (out of sync with server)");
		assertSame(jsonSentToClient, value.jsonValue, "client representation should not be invalidated");
	}

	@Nested
	class CorrectionsCountedAsLoop
	{

		@Test
		void firstCorrectionIsSentBackToClient()
		{
			clientPushesValueAndServerCorrectsIt();

			assertEquals(1, valueChangedCount.get());
			assertLastCorrectionWasSentToClient();
			assertTrue(pendingEvents.isEmpty(), "no loop yet, nothing to wait for");
		}

		@Test
		void acceptedPushIsNotSentBackToClient()
		{
			clientPushIsAccepted();

			assertEquals(0, valueChangedCount.get());
			assertTrue(pendingEvents.isEmpty());
		}

		@Test
		void fastCorrectionLoopIsInterruptedAfterTheLimit() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, -1);
			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get());
			assertLastCorrectionWasSentToClient();
			assertTrue(pendingEvents.isEmpty(), "loop not interrupted yet");

			clientPushesValueAndServerCorrectsIt(); // the one that exceeds the limit

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get(), "correction that exceeds the limit must not be sent");
			assertLastCorrectionWasIgnored();
			assertEquals(1, pendingEvents.size(), "reset must be scheduled for after the pending events are processed");
		}

		@Test
		void correctionsAfterInterruptionAreIgnoredAndResetIsScheduledOnlyOnce() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, -1); // last one interrupts

			repeatCorrections(10, -1);

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get());
			assertLastCorrectionWasIgnored();
			assertEquals(1, pendingEvents.size());
		}

		@Test
		void correctionsStillCloseEnoughInTimeAreCountedAsLoop() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, WELL_INSIDE_TIME_WINDOW_MS);

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get());
			assertLastCorrectionWasIgnored();
			assertEquals(1, pendingEvents.size());
		}

	}

	@Nested
	class TimeWindow
	{

		/** Javadoc: corrections less frequent than the time window might be user changes and are not counted as an automatic loop. */
		@Test
		void manyCorrectionsFarApartInTimeAreNeverBlocked() throws Exception
		{
			repeatCorrections(15, WELL_OUTSIDE_TIME_WINDOW_MS);

			assertEquals(15, valueChangedCount.get(), "corrections that are far apart in time are not a loop");
			assertTrue(pendingEvents.isEmpty(), "no loop was detected");
		}

		@Test
		void correctionsFarApartInTimeAreNotConsideredALoop() throws Exception
		{
			// a fast loop would have been interrupted by the 11th correction
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 2, WELL_OUTSIDE_TIME_WINDOW_MS);

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 2, valueChangedCount.get());
			assertLastCorrectionWasSentToClient();
			assertTrue(pendingEvents.isEmpty(), "no loop was detected");
		}

		@Test
		void slowCorrectionsDoNotAddToTheLoopCountButFastOnesStillDo() throws Exception
		{
			repeatCorrections(6, -1); // fast: loop count is now 5 (the very first correction is not counted)
			for (int i = 0; i < 10; i++) // slow ones: loop count stays 5
			{
				setPreviousCorrectionTimeToMsAgo(WELL_OUTSIDE_TIME_WINDOW_MS);
				clientPushesValueAndServerCorrectsIt();
			}
			assertEquals(16, valueChangedCount.get());
			assertTrue(pendingEvents.isEmpty(), "slow corrections must not bring the loop to the limit");

			repeatCorrections(4, -1); // fast: count 6, 7, 8, 9 => all still sent
			assertEquals(20, valueChangedCount.get());
			assertLastCorrectionWasSentToClient();

			clientPushesValueAndServerCorrectsIt(); // fast: count 10 => loop interrupted
			assertEquals(20, valueChangedCount.get());
			assertLastCorrectionWasIgnored();
			assertEquals(1, pendingEvents.size());
		}

	}

	@Nested
	class ResetOfLoopCounting
	{

		/** After an interrupted loop was reset, a second loop must be interrupted again (and not leave the avoider stuck forever). */
		@Test
		void secondLoopAfterAResetIsAlsoInterruptedAndScheduledForReset() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, -1); // first loop interrupted
			runPendingEvents(); // count reset to 0, but the previous correction time is still within the time window

			repeatCorrections(20, -1); // second loop

			assertEquals(1, pendingEvents.size(), "second loop must be interrupted with a reset scheduled; otherwise corrections are ignored forever");
			runPendingEvents();
			int before = valueChangedCount.get();
			clientPushesValueAndServerCorrectsIt();
			assertEquals(before + 1, valueChangedCount.get(), "corrections must work again after the reset");
		}

		@Test
		void acceptedPushFromClientResetsTheLoopCount() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT - 1, -1); // loop count 8; without a reset the next fast ones would be interrupted
			clientPushIsAccepted();

			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT - 1, -1);

			assertEquals(2 * (FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT - 1), valueChangedCount.get());
			assertLastCorrectionWasSentToClient();
			assertTrue(pendingEvents.isEmpty(), "no loop interruption expected");
		}

		@Test
		void acceptedPushIsIgnoredWhileLoopIsInterrupted() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, -1); // interrupted
			clientPushIsAccepted();

			clientPushesValueAndServerCorrectsIt();

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get(), "accepted push must not end the interruption");
			assertLastCorrectionWasIgnored();
			assertEquals(1, pendingEvents.size(), "only the original reset is pending");
		}

		@Test
		void interruptionEndsWhenPendingEventsWereProcessed() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, -1); // interrupted
			clientPushesValueAndServerCorrectsIt();
			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get());

			runPendingEvents();
			clientPushesValueAndServerCorrectsIt();

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, valueChangedCount.get(), "corrections must be sent to client again");
			assertLastCorrectionWasSentToClient();
		}

		@Test
		void interruptionDoesNotEndBeforePendingEventsWereProcessed() throws Exception
		{
			repeatCorrections(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT + 1, -1); // interrupted

			setPreviousCorrectionTimeToMsAgo(WELL_OUTSIDE_TIME_WINDOW_MS); // even a long time later, only the event queue drain ends it
			clientPushesValueAndServerCorrectsIt();

			assertEquals(FAST_LOOP_CORRECTIONS_SENT_BEFORE_INTERRUPT, valueChangedCount.get());
			assertLastCorrectionWasIgnored();
			assertEquals(1, pendingEvents.size());
		}

	}

	private interface CallHandler
	{
		Object handle(Method method, Object[] args);
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> iface, CallHandler handler)
	{
		return (T)Proxy.newProxyInstance(iface.getClassLoader(), new Class< ? >[] { iface }, (proxy, method, args) -> {
			if (method.getDeclaringClass() == Object.class)
			{
				switch (method.getName())
				{
					case "equals" :
						return Boolean.valueOf(proxy == args[0]);
					case "hashCode" :
						return Integer.valueOf(System.identityHashCode(proxy));
					default :
						return iface.getSimpleName() + " test proxy";
				}
			}
			Object result = handler.handle(method, args);
			Class< ? > returnType = method.getReturnType();
			if (result == null && returnType.isPrimitive() && returnType != void.class)
			{
				return Array.get(Array.newInstance(returnType, 1), 0);
			}
			return result;
		});
	}

}
