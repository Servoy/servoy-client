/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sablo.IChangeListener;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class RecordBasedPropertiesTest
{

	private RecordBasedProperties rbp1, rbp2;
	private boolean rbpsChanged = false;
	IChangeListener monitor = new IChangeListener()
	{

		@Override
		public void valueChanged()
		{
			rbpsChanged = true;
		}

	};

	@Before
	public void setUp()
	{
		rbpsChanged = false;

		rbp1 = new RecordBasedProperties();
		rbp1.addRecordBasedProperty("dp1", new String[] { "col1", "col2" }, null);
		rbp1.addRecordBasedProperty("dp2", new String[] { "col1" }, null);
		rbp1.addRecordBasedProperty("dp3", new String[] { "col3", "col4" }, null);

		rbp2 = new RecordBasedProperties();
	}

	@Test
	public void testSealAndAdd1()
	{
		assertFalse(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbp1.seal();
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		RecordBasedProperties rbpClone = rbp1.addRecordBasedProperty("dp3", new String[] { "col5" }, monitor);
		assertNotEquals(rbpClone, rbp1);
		rbp1 = rbpClone;
		assertFalse(rbpsChanged);
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		rbpClone = rbp1.addRecordBasedProperty("dp4", new String[] { "col3" }, monitor);
		assertSame(rbpClone, rbp1);
		assertTrue(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbpsChanged = false;

		rbp1.clearChanged();
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate()); // this I think does not matter as it is only checked in real life once - initially and here it would be later when it will no longer be checked

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp2", "dp3", "dp4" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp1", "dp2" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp3", "dp4" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col5", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col6", new String[] { });
	}

	@Test
	public void testSealAndAdd2()
	{
		assertFalse(rbpsChanged);

		RecordBasedProperties rbpClone = rbp2.addRecordBasedProperty("dp3", new String[] { "col5" }, monitor);
		assertEquals(rbpClone, rbp2);
		assertTrue(rbpsChanged);
		rbp2 = rbpClone;

		checkThatPropertiesAre(rbp2, new String[] { "dp3" });
		checkThatDpTriggersProps(rbp2, "col5", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp2, "col1", new String[] { });
	}

	@Test
	public void testSealAndAdd3()
	{
		assertFalse(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbp1.seal();
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		assertTrue(rbp1.contains("dp2"));
		assertFalse(rbp1.contains("dp6"));

		RecordBasedProperties rbpClone = rbp1.addRecordBasedProperty("dp4", new String[] { "col3" }, monitor);
		assertNotEquals(rbpClone, rbp1);
		rbp1 = rbpClone;
		assertTrue(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp2", "dp3", "dp4" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp1", "dp2" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp3", "dp4" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col6", new String[] { });
	}

	@Test
	public void testSealAndAdd4()
	{
		assertFalse(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbp1.seal();
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		RecordBasedProperties rbpClone = rbp1.addRecordBasedProperty("dp1", new String[] { "col5" }, monitor);
		assertNotEquals(rbpClone, rbp1);
		rbp1 = rbpClone;
		assertFalse(rbpsChanged);
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp2", "dp3" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp1", "dp2" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col5", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col6", new String[] { });
	}

	@Test
	public void testSealAndAdd5()
	{
		assertFalse(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbp1.seal();
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		// add a property that is linked to all / listens to changes on all column names
		RecordBasedProperties rbpClone = rbp1.addRecordBasedProperty("dp1", null, monitor);
		assertNotEquals(rbpClone, rbp1);
		rbp1 = rbpClone;
		assertFalse(rbpsChanged);
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp2", "dp3" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp2", "dp1" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp3", "dp1" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp3", "dp1" });
		checkThatDpTriggersProps(rbp1, "col5", new String[] { "dp1" });

		rbpClone = rbp1.addRecordBasedProperty("dp1", new String[] { "col1" }, monitor);
		assertSame(rbpClone, rbp1);

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp2", "dp3" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp2", "dp1" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp3" });
		checkThatDpTriggersProps(rbp1, "col5", new String[] { });
	}

	@Test
	public void testSealAndDelete1()
	{
		assertFalse(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbp1.seal();
		assertFalse(rbp1.areRecordBasedPropertiesChanged());
		assertFalse(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());

		// add a property that is linked to all / listens to changes on all column names
		RecordBasedProperties rbpClone = rbp1.removeRecordBasedProperty("dp2", monitor);
		assertNotEquals(rbpClone, rbp1);
		rbp1 = rbpClone;
		assertTrue(rbpsChanged);
		assertTrue(rbp1.areRecordBasedPropertiesChanged());
		assertTrue(rbp1.areRecordBasedPropertiesChangedComparedToTemplate());
		rbp1.clearChanged();

		rbpClone = rbp1.addRecordBasedProperty("dp1", null, monitor);
		assertSame(rbpClone, rbp1);

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp3" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp3", "dp1" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp3", "dp1" });
		checkThatDpTriggersProps(rbp1, "col5", new String[] { "dp1" });

		rbpClone = rbp1.addRecordBasedProperty("dp4", new String[] { "col3" }, monitor);
		assertSame(rbpClone, rbp1);
		rbpClone = rbp1.removeRecordBasedProperty("dp3", monitor);
		assertSame(rbpClone, rbp1);

		checkThatPropertiesAre(rbp1, new String[] { "dp1", "dp4" });
		checkThatDpTriggersProps(rbp1, "col1", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col2", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col3", new String[] { "dp4", "dp1" });
		checkThatDpTriggersProps(rbp1, "col4", new String[] { "dp1" });
		checkThatDpTriggersProps(rbp1, "col5", new String[] { "dp1" });
	}

	private void checkThatPropertiesAre(RecordBasedProperties rbp, String[] expectedPropNames)
	{
		List<String> props = new ArrayList<>();
		rbp.forEach(pn -> props.add(pn));
		assertArrayEquals(expectedPropNames, props.toArray());
	}

	private void checkThatDpTriggersProps(RecordBasedProperties rbp, String dp, String[] expectedPropNames)
	{
		List<String> props = new ArrayList<>();
		rbp.forEachPropertyNameThatListensToDp(dp, pn -> props.add(pn));
		assertArrayEquals(expectedPropNames, props.toArray());
	}

}
