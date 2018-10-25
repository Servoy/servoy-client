/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.j2db.server.ngclient.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


/**
 * @author acostescu
 *
 */
@SuppressWarnings("nls")
public class ViewportChangeKeeperTest
{

	private ViewportChangeKeeper changeKeeper;

	@Before
	public void prepareViewportChangeKeeper()
	{
		if (changeKeeper == null) changeKeeper = new ViewportChangeKeeper();
		changeKeeper.reset(0, 5);
	}

	private void assertChangeOpEquals(int startIdx, int endIdx, String columnName, int type, ViewportOperation equivalentOps)
	{
		assertEquals(type, equivalentOps.type);
		assertEquals(startIdx, equivalentOps.startIndex);
		assertEquals(endIdx, equivalentOps.endIndex);
		assertEquals(columnName, equivalentOps.columnName);
	}

	@Test
	public void updatesAndPartialUpdates()
	{
		// one update
		assertFalse(changeKeeper.hasChanges());
		changeKeeper.processOperation(new ViewportOperation(2, 2, ViewportOperation.CHANGE));
		assertTrue(changeKeeper.hasChanges());
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.CHANGE, equivalentOps[0]);

		// partial update of the same row
		changeKeeper.processOperation(new ViewportOperation(2, 2, ViewportOperation.CHANGE, "columnA"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.CHANGE, equivalentOps[0]);

		// partial update of another row
		changeKeeper.processOperation(new ViewportOperation(4, 4, ViewportOperation.CHANGE, "columnA"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(4, 4, "columnA", ViewportOperation.CHANGE, equivalentOps[1]);

		// another partial update of row 4
		changeKeeper.processOperation(new ViewportOperation(4, 4, ViewportOperation.CHANGE, "columnB"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(4, 4, "columnA", ViewportOperation.CHANGE, equivalentOps[1]);
		assertChangeOpEquals(4, 4, "columnB", ViewportOperation.CHANGE, equivalentOps[2]);
		assertEquals(4, changeKeeper.getNumberOfUnchangedRows());

		// duplicate partial update of row 4
		changeKeeper.processOperation(new ViewportOperation(4, 4, ViewportOperation.CHANGE, "columnA"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(4, 4, "columnA", ViewportOperation.CHANGE, equivalentOps[1]);
		assertChangeOpEquals(4, 4, "columnB", ViewportOperation.CHANGE, equivalentOps[2]);

		// full update that overlaps partial update
		changeKeeper.processOperation(new ViewportOperation(4, 5, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(4, 5, null, ViewportOperation.CHANGE, equivalentOps[1]);
		assertEquals(3, changeKeeper.getNumberOfUnchangedRows());

		// full update that overlaps all previous updates
		changeKeeper.processOperation(new ViewportOperation(2, 5, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(2, 5, null, ViewportOperation.CHANGE, equivalentOps[0]);
	}

	@Test
	public void changesAndPartialChangesAtBeginningOrEndOfUnchangedInterval()
	{
		// partial update in the beginning of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(0, 0, ViewportOperation.CHANGE, "columnA"));
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(0, 0, "columnA", ViewportOperation.CHANGE, equivalentOps[0]);

		// full update at the end of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(4, 5, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(0, 0, "columnA", ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(4, 5, null, ViewportOperation.CHANGE, equivalentOps[1]);

		// full update at the end of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(3, 3, ViewportOperation.CHANGE, "columnB"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(0, 0, "columnA", ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(3, 3, "columnB", ViewportOperation.CHANGE, equivalentOps[1]);
		assertChangeOpEquals(4, 5, null, ViewportOperation.CHANGE, equivalentOps[2]);
	}

	@Test
	public void overlappingChanges()
	{
		changeKeeper.reset(100, 1000);

		// partial update in the beginning of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(150, 200, ViewportOperation.CHANGE));
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(150, 200, null, ViewportOperation.CHANGE, equivalentOps[0]);

		// full update at the end of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(175, 255, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(150, 255, null, ViewportOperation.CHANGE, equivalentOps[0]);

		// full update at the end of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(107, 165, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(107, 255, null, ViewportOperation.CHANGE, equivalentOps[0]);

		// end of unchanged interval partial update with larger size
		changeKeeper.processOperation(new ViewportOperation(106, 106, ViewportOperation.CHANGE, "columnC"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(106, 106, "columnC", ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(107, 255, null, ViewportOperation.CHANGE, equivalentOps[1]);

		// start of unchanged interval partial update with larger size
		changeKeeper.processOperation(new ViewportOperation(100, 100, ViewportOperation.CHANGE, "columnD"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(100, 100, "columnD", ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(106, 106, "columnC", ViewportOperation.CHANGE, equivalentOps[1]);
		assertChangeOpEquals(107, 255, null, ViewportOperation.CHANGE, equivalentOps[2]);
	}

	@Test
	public void insertsWithALargeNumberOfRows()
	{
		// before unchanged interval
		assertFalse(changeKeeper.hasChanges());
		changeKeeper.processOperation(new ViewportOperation(0, 15, ViewportOperation.INSERT));
		assertTrue(changeKeeper.hasChanges());
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(0, 15, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertEquals(6, changeKeeper.getNumberOfUnchangedRows());

		// after unchanged interval
		changeKeeper.processOperation(new ViewportOperation(22, 25, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(0, 15, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(22, 25, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertEquals(6, changeKeeper.getNumberOfUnchangedRows());

		// middle of unchanged interval
		changeKeeper.processOperation(new ViewportOperation(20, 50, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(0, 15, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(20, 50, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertChangeOpEquals(53, 56, null, ViewportOperation.INSERT, equivalentOps[2]);
		assertEquals(6, changeKeeper.getNumberOfUnchangedRows());
	}

	@Test
	public void insertAndChangeBasics()
	{
		changeKeeper.processOperation(new ViewportOperation(2, 2, ViewportOperation.INSERT));
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.INSERT, equivalentOps[0]);

		changeKeeper.processOperation(new ViewportOperation(4, 4, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 2, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(4, 4, null, ViewportOperation.INSERT, equivalentOps[1]);

		changeKeeper.processOperation(new ViewportOperation(2, 4, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 5, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(7, 7, null, ViewportOperation.INSERT, equivalentOps[1]);

		changeKeeper.processOperation(new ViewportOperation(3, 3, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 6, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(8, 8, null, ViewportOperation.INSERT, equivalentOps[1]);

		changeKeeper.processOperation(new ViewportOperation(9, 10, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 6, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(8, 10, null, ViewportOperation.INSERT, equivalentOps[1]);

		changeKeeper.processOperation(new ViewportOperation(7, 8, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(2, 9, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(10, 10, null, ViewportOperation.CHANGE, equivalentOps[1]);

		changeKeeper.processOperation(new ViewportOperation(0, 0, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(0, 0, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(2, 9, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertChangeOpEquals(10, 10, null, ViewportOperation.CHANGE, equivalentOps[2]);

		changeKeeper.processOperation(new ViewportOperation(12, 12, ViewportOperation.CHANGE, "columnA"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(4, equivalentOps.length);
		assertChangeOpEquals(0, 0, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(2, 9, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertChangeOpEquals(10, 10, null, ViewportOperation.CHANGE, equivalentOps[2]);
		assertChangeOpEquals(12, 12, "columnA", ViewportOperation.CHANGE, equivalentOps[3]);

		changeKeeper.processOperation(new ViewportOperation(13, 13, ViewportOperation.INSERT));
		changeKeeper.processOperation(new ViewportOperation(12, 12, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(6, equivalentOps.length);
		assertChangeOpEquals(0, 0, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(2, 9, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertChangeOpEquals(10, 10, null, ViewportOperation.CHANGE, equivalentOps[2]);
		assertChangeOpEquals(12, 12, null, ViewportOperation.INSERT, equivalentOps[3]);
		assertChangeOpEquals(13, 13, "columnA", ViewportOperation.CHANGE, equivalentOps[4]);
		assertChangeOpEquals(14, 14, null, ViewportOperation.INSERT, equivalentOps[5]);

		changeKeeper.processOperation(new ViewportOperation(3, 3, ViewportOperation.CHANGE, "columnB"));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(6, equivalentOps.length);
		assertChangeOpEquals(0, 0, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(2, 9, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertChangeOpEquals(10, 10, null, ViewportOperation.CHANGE, equivalentOps[2]);
		assertChangeOpEquals(12, 12, null, ViewportOperation.INSERT, equivalentOps[3]);
		assertChangeOpEquals(13, 13, "columnA", ViewportOperation.CHANGE, equivalentOps[4]);
		assertChangeOpEquals(14, 14, null, ViewportOperation.INSERT, equivalentOps[5]);

		changeKeeper.processOperation(new ViewportOperation(15, 16, ViewportOperation.INSERT));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(6, equivalentOps.length);
		assertChangeOpEquals(0, 0, null, ViewportOperation.CHANGE, equivalentOps[0]);
		assertChangeOpEquals(2, 9, null, ViewportOperation.INSERT, equivalentOps[1]);
		assertChangeOpEquals(10, 10, null, ViewportOperation.CHANGE, equivalentOps[2]);
		assertChangeOpEquals(12, 12, null, ViewportOperation.INSERT, equivalentOps[3]);
		assertChangeOpEquals(13, 13, "columnA", ViewportOperation.CHANGE, equivalentOps[4]);
		assertChangeOpEquals(14, 16, null, ViewportOperation.INSERT, equivalentOps[5]);
		assertEquals(3, changeKeeper.getNumberOfUnchangedRows());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException1()
	{
		// processing Viewport operations with unsupported type should throw an exception
		changeKeeper.processOperation(new ViewportOperation(1, 2, 987654)); // bogus type
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException2()
	{
		// partial interval with unsupported sizes and null column name
		new PartiallyUnchangedInterval(1, 2, 3, 4, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException3()
	{
		// interval with unsupported size
		new PartiallyUnchangedInterval(1, 2, 3, 4, "columnA");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException4()
	{
		// partial interval without column name
		new PartiallyUnchangedInterval(1, 1, 3, 3, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException5()
	{
		// interval with unsupported size and without column name
		new PartiallyUnchangedInterval(1, 2, 3, 3, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException6()
	{
		// interval with unsupported size and without column name
		new PartiallyUnchangedInterval(1, 1, 3, 4, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException7()
	{
		// try to apply out-of-viewport-bounds operations
		changeKeeper.processOperation(new ViewportOperation(-1, 3, ViewportOperation.CHANGE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException8()
	{
		// try to apply out-of-viewport-bounds operations
		changeKeeper.processOperation(new ViewportOperation(-10, -3, ViewportOperation.DELETE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException9()
	{
		// try to apply out-of-viewport-bounds operations
		changeKeeper.processOperation(new ViewportOperation(6, 6, ViewportOperation.CHANGE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException10()
	{
		// try to apply out-of-viewport-bounds operations
		changeKeeper.processOperation(new ViewportOperation(3, 8, ViewportOperation.DELETE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException11()
	{
		// try to apply out-of-viewport-bounds operations
		changeKeeper.processOperation(new ViewportOperation(7, 15, ViewportOperation.INSERT));
	}

	@Test
	public void deleteBasics()
	{
		changeKeeper.reset(5, 30);

		assertFalse(changeKeeper.hasChanges());
		changeKeeper.processOperation(new ViewportOperation(8, 9, ViewportOperation.DELETE));
		assertTrue(changeKeeper.hasChanges());
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(1, equivalentOps.length);
		assertChangeOpEquals(8, 9, null, ViewportOperation.DELETE, equivalentOps[0]);

		changeKeeper.processOperation(new ViewportOperation(8, 8, ViewportOperation.CHANGE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(8, 9, null, ViewportOperation.DELETE, equivalentOps[0]);
		assertChangeOpEquals(8, 8, null, ViewportOperation.CHANGE, equivalentOps[1]);

		changeKeeper.processOperation(new ViewportOperation(5, 5, ViewportOperation.DELETE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(3, equivalentOps.length);
		assertChangeOpEquals(5, 5, null, ViewportOperation.DELETE, equivalentOps[0]);
		assertChangeOpEquals(7, 8, null, ViewportOperation.DELETE, equivalentOps[1]);
		assertChangeOpEquals(7, 7, null, ViewportOperation.CHANGE, equivalentOps[2]);

		changeKeeper.processOperation(new ViewportOperation(26, 27, ViewportOperation.DELETE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(4, equivalentOps.length);
		assertChangeOpEquals(5, 5, null, ViewportOperation.DELETE, equivalentOps[0]);
		assertChangeOpEquals(7, 8, null, ViewportOperation.DELETE, equivalentOps[1]);
		assertChangeOpEquals(7, 7, null, ViewportOperation.CHANGE, equivalentOps[2]);
		assertChangeOpEquals(26, 27, null, ViewportOperation.DELETE, equivalentOps[3]);

		changeKeeper.processOperation(new ViewportOperation(5, 10, ViewportOperation.DELETE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();
		assertEquals(2, equivalentOps.length);
		assertChangeOpEquals(5, 13, null, ViewportOperation.DELETE, equivalentOps[0]);
		assertChangeOpEquals(20, 21, null, ViewportOperation.DELETE, equivalentOps[1]);
	}

	@Test
	public void mixedOperations()
	{
		changeKeeper.processOperation(new ViewportOperation(2, 2, ViewportOperation.CHANGE));
		changeKeeper.processOperation(new ViewportOperation(0, 3, ViewportOperation.INSERT));
		changeKeeper.processOperation(new ViewportOperation(3, 5, ViewportOperation.DELETE));
		changeKeeper.processOperation(new ViewportOperation(0, 1, ViewportOperation.INSERT));
		changeKeeper.processOperation(new ViewportOperation(7, 7, ViewportOperation.INSERT));
		changeKeeper.processOperation(new ViewportOperation(8, 8, ViewportOperation.CHANGE));
		ViewportOperation[] equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();

		assertEquals(4, equivalentOps.length);
		assertChangeOpEquals(0, 2, null, ViewportOperation.INSERT, equivalentOps[0]);
		assertChangeOpEquals(3, 5, null, ViewportOperation.CHANGE, equivalentOps[1]);
		assertChangeOpEquals(7, 7, null, ViewportOperation.INSERT, equivalentOps[2]);
		assertChangeOpEquals(8, 8, null, ViewportOperation.CHANGE, equivalentOps[3]);


		changeKeeper.processOperation(new ViewportOperation(0, 4, ViewportOperation.DELETE));
		equivalentOps = changeKeeper.getEquivalentSequenceOfOperations();

		assertEquals(4, equivalentOps.length);
		assertChangeOpEquals(0, 1, null, ViewportOperation.DELETE, equivalentOps[0]);
		assertChangeOpEquals(0, 0, null, ViewportOperation.CHANGE, equivalentOps[1]);
		assertChangeOpEquals(2, 2, null, ViewportOperation.INSERT, equivalentOps[2]);
		assertChangeOpEquals(3, 3, null, ViewportOperation.CHANGE, equivalentOps[3]);
	}

}