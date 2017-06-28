/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.awt.Dimension;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.util.ServoyException;

/**
 * @author gboros
 *
 */
public class RelatedFormsTest extends AbstractSolutionTest
{

	/*
	 * @see com.servoy.j2db.server.ngclient.property.AbstractSolutionTest#setupData()
	 */
	@Override
	protected void setupData() throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * @see com.servoy.j2db.server.ngclient.property.AbstractSolutionTest#fillTestSolution()
	 */
	@Override
	protected void fillTestSolution() throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * @see com.servoy.j2db.server.ngclient.property.AbstractSolutionTest#getTestComponents()
	 */
	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Test
	public void testRelatedFormsInTabPanels() throws RepositoryException
	{
//		f1 is the main
//
//		3 forms on that main
//
//		f2 "relation2"
//		f3 "relation2.relation3"
//		f4 "relation2.relation3.relation4"


		Form f1 = solution.createNewForm(validator, null, "f1", null, false, new Dimension(600, 400));
		IWebFormController f1Controller = new TestFormController(f1, client);
		IDataAdapterList dataAdapterListF1 = f1Controller.getFormUI().getDataAdapterList();

		Form f2 = solution.createNewForm(validator, null, "f2", null, false, new Dimension(600, 400));
		IWebFormController f2Controller = new TestFormController(f2, client);
		IDataAdapterList dataAdapterListF2 = f2Controller.getFormUI().getDataAdapterList();

		Form f3 = solution.createNewForm(validator, null, "f3", null, false, new Dimension(600, 400));
		IWebFormController f3Controller = new TestFormController(f3, client);
		IDataAdapterList dataAdapterListF3 = f3Controller.getFormUI().getDataAdapterList();

		Form f4 = solution.createNewForm(validator, null, "f4", null, false, new Dimension(600, 400));
		IWebFormController f4Controller = new TestFormController(f4, client);
		IDataAdapterList dataAdapterListF4 = f4Controller.getFormUI().getDataAdapterList();

		TabPanel tabpanelF2 = f1.createNewTabPanel("tabpanelF2");
		tabpanelF2.createNewTab("tab1", "relation2", f2);

		TabPanel tabpanelF3 = f1.createNewTabPanel("tabpanelF3");
		tabpanelF3.createNewTab("tab1", "relation2.relation3", f3);

		TabPanel tabpanelF4 = f1.createNewTabPanel("tabpanelF4");
		tabpanelF4.createNewTab("tab1", "relation2.relation3.relation4", f4);


		dataAdapterListF1.addVisibleChildForm(f2Controller, "relation2", true);
//		relatedForms of f1 has now f2
		Assert.assertEquals(1, dataAdapterListF1.getRelatedForms().size());
		Assert.assertEquals("relation2", dataAdapterListF1.getRelatedForms().get(f2Controller));

		dataAdapterListF1.addVisibleChildForm(f3Controller, "relation2.relation3", true);
//		relatedForms of f1 has now f2 and f3
		Assert.assertEquals(2, dataAdapterListF1.getRelatedForms().size());
		Assert.assertEquals("relation2.relation3", dataAdapterListF1.getRelatedForms().get(f3Controller));

//		also f2 relatedForms has f3
		Assert.assertEquals(1, dataAdapterListF2.getRelatedForms().size());
		Assert.assertEquals("relation3", dataAdapterListF2.getRelatedForms().get(f3Controller));
//		and f3 relatedParentsForms has f2
		Assert.assertEquals(1, dataAdapterListF3.getParentRelatedForms().size());
		Assert.assertEquals(dataAdapterListF3.getParentRelatedForms().toString() + " should have:" + f2Controller, 0,
			dataAdapterListF3.getParentRelatedForms().indexOf(f2Controller));

		Assert.assertNotEquals(dataAdapterListF3.getParentRelatedForms().toString() + " should have:" + f2Controller, -1,
			dataAdapterListF3.getParentRelatedForms().indexOf(f2Controller));

		dataAdapterListF1.addVisibleChildForm(f4Controller, "relation2.relation3.relation4", true);
//		relatedForms of f1 has now f2 and f3 and f4
		Assert.assertEquals(3, dataAdapterListF1.getRelatedForms().size());
		Assert.assertEquals("relation2.relation3.relation4", dataAdapterListF1.getRelatedForms().get(f4Controller));
//		relatedForms of f2 has now f3 and f4
		Assert.assertEquals(2, dataAdapterListF2.getRelatedForms().size());
		Assert.assertEquals("relation3", dataAdapterListF2.getRelatedForms().get(f3Controller));
		Assert.assertEquals("relation3.relation4", dataAdapterListF2.getRelatedForms().get(f4Controller));
//		and f4 relatedParentsForms has f2 and f3
		Assert.assertEquals(2, dataAdapterListF4.getParentRelatedForms().size());
		Assert.assertTrue(dataAdapterListF4.getParentRelatedForms() + " should have " + f2Controller,
			dataAdapterListF4.getParentRelatedForms().indexOf(f2Controller) >= 0);
		Assert.assertTrue(dataAdapterListF4.getParentRelatedForms() + " should have " + f3Controller,
			dataAdapterListF4.getParentRelatedForms().indexOf(f3Controller) >= 0);

		dataAdapterListF1.removeVisibleChildForm(f4Controller, true);
//		relatedForms of f1 has now f2 and f3
		Assert.assertEquals(2, dataAdapterListF1.getRelatedForms().size());
		Assert.assertEquals("relation2", dataAdapterListF1.getRelatedForms().get(f2Controller));
		Assert.assertEquals("relation2.relation3", dataAdapterListF1.getRelatedForms().get(f3Controller));
//		relatedForms of f2 has now f3
		Assert.assertEquals(1, dataAdapterListF2.getRelatedForms().size());
		Assert.assertEquals("relation3", dataAdapterListF2.getRelatedForms().get(f3Controller));


		dataAdapterListF2.removeVisibleChildForm(f3Controller, true);
//		relatedForms of f1 has now f2 and f3
		Assert.assertEquals(2, dataAdapterListF1.getRelatedForms().size());
		Assert.assertEquals("relation2", dataAdapterListF1.getRelatedForms().get(f2Controller));
		Assert.assertEquals("relation2.relation3", dataAdapterListF1.getRelatedForms().get(f3Controller));
//		relatedForms of f2 has no related forms
		Assert.assertEquals(0, dataAdapterListF2.getRelatedForms().size());
	}
}
