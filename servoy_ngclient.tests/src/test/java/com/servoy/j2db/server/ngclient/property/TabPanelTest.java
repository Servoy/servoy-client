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
import java.io.StringWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.skyscreamer.jsonassert.JSONAssert;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.ServoyException;

/**
 * @author gboros
 *
 */
public class TabPanelTest extends AbstractSolutionTest
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
		Form f1 = solution.createNewForm(validator, null, "f1", null, false, new Dimension(600, 400));
		f1.setNavigatorID(Form.NAVIGATOR_NONE);
		Form f2 = solution.createNewForm(validator, null, "f2", null, false, new Dimension(600, 400));
		Form f3 = solution.createNewForm(validator, null, "f3", null, false, new Dimension(600, 400));
		Form f4 = solution.createNewForm(validator, null, "f4", null, false, new Dimension(600, 400));

		TabPanel tabpanelF2 = f1.createNewTabPanel("tabpanel");
		tabpanelF2.createNewTab("tab1", "relation2", f2);
		tabpanelF2.createNewTab("tab2", "relation3", f3);
		tabpanelF2.createNewTab("tab3", "relation4", f4);
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

	@SuppressWarnings("nls")
	@Test
	public void testRelatedFormsInTabPanels() throws JSONException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("f1");
		WebFormComponent webComponent = form.getFormUI().getWebComponent("tabpanel");
		Object property = webComponent.getProperty("tabIndex");
		Assert.assertEquals(((Number)property).longValue(), 1);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		form.getFormUI().writeAllComponentsProperties(jsonWriter, FullValueToJSONConverter.INSTANCE);
		jsonWriter.endObject();

		JSONAssert.assertEquals(new JSONObject(
			"{\"\":{\"enabled\":true,\"visible\":true,\"findmode\":false},\"tabpanel\":{\"enabled\":true,\"tabs\":{\"vEr\":3,\"v\":[{\"vEr\":3,\"v\":{\"relationName\":\"-1\",\"active\":true,\"disabled\":false,\"text\":\"tab1\",\"containsFormId\":\"f2\",\"toolTipText\":\"\"}},{\"vEr\":3,\"v\":{\"relationName\":\"-1\",\"active\":false,\"disabled\":false,\"text\":\"tab2\",\"containsFormId\":\"f3\",\"toolTipText\":\"\"}},{\"vEr\":3,\"v\":{\"relationName\":\"-1\",\"active\":false,\"disabled\":false,\"text\":\"tab3\",\"containsFormId\":\"f4\",\"toolTipText\":\"\"}}]},\"svyMarkupId\":\"s554517c05b68828168c38c67974bf993\"}}"),
			new JSONObject(stringWriter.toString()), true);
		webComponent.setProperty("tabIndex", "tab2");

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		form.getFormUI().writeAllComponentsProperties(jsonWriter, FullValueToJSONConverter.INSTANCE);
		jsonWriter.endObject();

		JSONAssert.assertEquals(new JSONObject(
			"{\"\":{\"enabled\":true,\"visible\":true,\"findmode\":false},\"tabpanel\":{\"enabled\":true,\"tabs\":{\"vEr\":4,\"v\":[{\"vEr\":4,\"v\":{\"relationName\":\"-1\",\"active\":true,\"disabled\":false,\"text\":\"tab1\",\"containsFormId\":\"f2\",\"toolTipText\":\"\"}},{\"vEr\":4,\"v\":{\"relationName\":\"-1\",\"active\":false,\"disabled\":false,\"text\":\"tab2\",\"containsFormId\":\"f3\",\"toolTipText\":\"\"}},{\"vEr\":4,\"v\":{\"relationName\":\"-1\",\"active\":false,\"disabled\":false,\"text\":\"tab3\",\"containsFormId\":\"f4\",\"toolTipText\":\"\"}}]},\"svyMarkupId\":\"s554517c05b68828168c38c67974bf993\",\"tabIndex\":\"tab2\"}}"),
			new JSONObject(stringWriter.toString()), true);

		webComponent.setProperty("tabIndex", Integer.valueOf(3));

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		form.getFormUI().writeAllComponentsProperties(jsonWriter, FullValueToJSONConverter.INSTANCE);
		jsonWriter.endObject();

		JSONAssert.assertEquals(new JSONObject(
			"{\"\":{\"enabled\":true,\"visible\":true,\"findmode\":false},\"tabpanel\":{\"enabled\":true,\"tabs\":{\"vEr\":5,\"v\":[{\"vEr\":5,\"v\":{\"relationName\":\"-1\",\"active\":true,\"disabled\":false,\"text\":\"tab1\",\"containsFormId\":\"f2\",\"toolTipText\":\"\"}},{\"vEr\":5,\"v\":{\"relationName\":\"-1\",\"active\":false,\"disabled\":false,\"text\":\"tab2\",\"containsFormId\":\"f3\",\"toolTipText\":\"\"}},{\"vEr\":5,\"v\":{\"relationName\":\"-1\",\"active\":false,\"disabled\":false,\"text\":\"tab3\",\"containsFormId\":\"f4\",\"toolTipText\":\"\"}}]},\"svyMarkupId\":\"s554517c05b68828168c38c67974bf993\",\"tabIndex\":3}}"),
			new JSONObject(stringWriter.toString()), true);
	}
}
