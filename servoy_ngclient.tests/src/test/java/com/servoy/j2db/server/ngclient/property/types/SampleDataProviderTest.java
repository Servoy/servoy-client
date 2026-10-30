package com.servoy.j2db.server.ngclient.property.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.persistence.IColumnTypes;

/**
 * SVY-21610: pins the value fabrication of {@link SampleDataProvider} that lets the stateless form-template render
 * ({@code getFormLayout}) show data instead of empty component chrome.
 *
 * <p>
 * The assertion that matters most here is <b>determinism</b>. The agent loop this feature exists for is <i>render,
 * change one thing, render again, diff</i>, so a value that varies between two renders of an unchanged form makes the
 * tool useless for its own purpose - the agent cannot tell its own edit apart from the noise. A regression to
 * {@code Math.random()}, a static counter or {@code new Date()} would still "show data" and so would pass any smoke
 * test; only these tests catch it.
 * </p>
 *
 * <p>
 * Only the pure fabrication seam is covered: resolving a dataprovider to a variable/valuelist/column needs a live
 * flattened solution and is exercised by the form-template integration path instead.
 * </p>
 */
public class SampleDataProviderTest
{
	@Nested
	class Determinism
	{
		@Test
		void repeatedCallsForTheSameDataproviderAndRowReturnEqualValues()
		{
			for (int columnType : new int[] { IColumnTypes.TEXT, IColumnTypes.INTEGER, IColumnTypes.NUMBER, IColumnTypes.DATETIME })
			{
				Object first = SampleDataProvider.getSampleValueForType(columnType, "orderid", 0);
				Object second = SampleDataProvider.getSampleValueForType(columnType, "orderid", 0);
				assertEquals(first, second, "sample value for column type " + columnType + " must be stable across calls");
			}
		}

		@Test
		void valueDependsOnlyOnTheDataproviderNameAndRowIndex()
		{
			// same name + row -> same value, even interleaved with other names/rows/types
			Object a = SampleDataProvider.getSampleValueForType(IColumnTypes.TEXT, "shipname", 1);
			SampleDataProvider.getSampleValueForType(IColumnTypes.TEXT, "somethingelse", 7);
			SampleDataProvider.getSampleValueForType(IColumnTypes.INTEGER, "shipname", 1);
			Object b = SampleDataProvider.getSampleValueForType(IColumnTypes.TEXT, "shipname", 1);

			assertEquals(a, b, "fabrication must not depend on call order or any hidden state");
		}

		@Test
		void differentDataprovidersGetDifferentTextValues()
		{
			// not strictly a correctness requirement, but a specimen sheet in which every field shows the same word
			// tells the agent nothing; this guards that the seeding actually uses the name
			Set<Object> values = new HashSet<>();
			for (String dp : new String[] { "companyname", "contactname", "city", "country", "phone" })
			{
				values.add(SampleDataProvider.getSampleValueForType(IColumnTypes.TEXT, dp, 0));
			}
			assertTrue(values.size() > 1, "text samples should vary by dataprovider name, got: " + values);
		}
	}

	@Nested
	class PerColumnType
	{
		@Test
		void textYieldsANonEmptyString()
		{
			Object value = SampleDataProvider.getSampleValueForType(IColumnTypes.TEXT, "companyname", 0);
			String text = assertInstanceOf(String.class, value);
			assertTrue(text.length() > 0, "a text sample must be non-empty, otherwise the field still renders blank");
		}

		@Test
		void integerYieldsAnInteger()
		{
			assertInstanceOf(Integer.class, SampleDataProvider.getSampleValueForType(IColumnTypes.INTEGER, "orderid", 0));
		}

		@Test
		void numberYieldsADouble()
		{
			assertInstanceOf(Double.class, SampleDataProvider.getSampleValueForType(IColumnTypes.NUMBER, "freight", 0));
		}

		@Test
		void datetimeYieldsADate()
		{
			assertInstanceOf(Date.class, SampleDataProvider.getSampleValueForType(IColumnTypes.DATETIME, "orderdate", 0));
		}

		@Test
		void mediaYieldsNothingSoNothingIsWritten()
		{
			// sample media is deliberately out of scope; null means "write no value", i.e. exactly the behaviour
			// before SVY-21610
			assertNull(SampleDataProvider.getSampleValueForType(IColumnTypes.MEDIA, "picture", 0));
		}

		@Test
		void unresolvedColumnTypeYieldsNothing()
		{
			// -1 is what getColumnType() returns for a dataprovider it cannot resolve (typo, unreachable table, ...);
			// such a field must keep rendering empty rather than showing a misleading value
			assertNull(SampleDataProvider.getSampleValueForType(-1, "nosuchcolumn", 0));
		}
	}

	@Nested
	class AcrossSampleRows
	{
		@Test
		void threeSampleRowsAreReported()
		{
			// the grid contract depends on this: serverSize == viewPort.rows.length == SAMPLE_ROW_COUNT with
			// hasMoreRows false, otherwise the grid calls loadExtraRecordsAsync() and hangs with no websocket session
			assertEquals(3, SampleDataProvider.SAMPLE_ROW_COUNT);
		}

		@Test
		void datesDifferPerRowSoAGridColumnIsNotAllTheSameDay()
		{
			Date row0 = (Date)SampleDataProvider.getSampleValueForType(IColumnTypes.DATETIME, "orderdate", 0);
			Date row1 = (Date)SampleDataProvider.getSampleValueForType(IColumnTypes.DATETIME, "orderdate", 1);
			assertNotEquals(row0, row1, "a date column should show distinguishable values across sample rows");
		}

		@Test
		void everySampleRowGetsAValueForATextColumn()
		{
			for (int row = 0; row < SampleDataProvider.SAMPLE_ROW_COUNT; row++)
			{
				Object value = SampleDataProvider.getSampleValueForType(IColumnTypes.TEXT, "companyname", row);
				String text = assertInstanceOf(String.class, value, "row " + row);
				assertTrue(text.length() > 0, "row " + row + " must not be blank");
			}
		}
	}
}
