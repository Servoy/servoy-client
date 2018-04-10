/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import java.io.InputStream;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.sablo.Container;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.JSONWrapperList;
import com.servoy.j2db.util.ServoyException;

/**
 * @author acostescu
 *
 */
@SuppressWarnings("nls")
public class ValuelistPropTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("ValuelistTest.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("ValuelistTest-mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp1 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp1);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		try
		{
			ValueList vl = solution.createNewValueList(validator, "myVl1");
			vl.setCustomValues("1\n2\n3");
			vl = solution.createNewValueList(validator, "myVl2");
			vl.setCustomValues("11\n22\n33");

			Form form = solution.createNewForm(validator, null, "test", "mem:test", false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			form.createNewWebComponent("myCustomComponent", "my-component");
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			throw new ServoyException();
		}
	}


	@Override
	protected void setupData() throws ServoyException
	{
		BufferedDataSet ds = new BufferedDataSet(new String[] { "pk", "test1", "test2" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		ds.addRow(new Object[] { Integer.valueOf(1), "value1", "valueA" });
		ds.addRow(new Object[] { Integer.valueOf(2), "value2", "valueB" });
		ds.addRow(new Object[] { Integer.valueOf(3), "value3", "valueC" });
		ds.addRow(new Object[] { Integer.valueOf(4), "value4", "valueD" });
		ds.addRow(new Object[] { Integer.valueOf(5), "value5", "valueE" });
		ds.addRow(new Object[] { Integer.valueOf(6), "value6", "valueF" });
		ds.addRow(new Object[] { Integer.valueOf(7), "value7", "valueG" });
		ds.addRow(new Object[] { Integer.valueOf(8), "value8", "valueH" });
		ds.addRow(new Object[] { Integer.valueOf(9), "value9", "valueI" });
		ds.addRow(new Object[] { Integer.valueOf(10), "value10", "valueJ" });
		ds.addRow(new Object[] { Integer.valueOf(11), "value11", "valueK" });
		ds.addRow(new Object[] { Integer.valueOf(12), "value12", "valueL" });
		ds.addRow(new Object[] { Integer.valueOf(13), "value13", "valueM" });
		ds.addRow(new Object[] { Integer.valueOf(14), "value14", "valueN" });
		ds.addRow(new Object[] { Integer.valueOf(15), "value15", "valueO" });
		ds.addRow(new Object[] { Integer.valueOf(16), "value16", "valueP" });
		ds.addRow(new Object[] { Integer.valueOf(17), "value17", "valueQ" });
		ds.addRow(new Object[] { Integer.valueOf(18), "value18", "valueR" });
		client.getFoundSetManager().createDataSourceFromDataSet("test", ds, null, new String[] { "pk" }, false);

		HashMap<String, IServer> serverProxies = new HashMap<String, IServer>();
		serverProxies.put("_sv_inmem", DUMMY_ISERVER);
		solution.setServerProxies(serverProxies);
	}

	@Test
	public void valueListAttachDetach() throws JSONException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		Assert.assertNotNull(form);

		// (SVY-12336)
		// assign a foundset and a column with foundset linked dp and valuelist linked to that dp; then set foundset to null, clear columns and set back all this; no exception should happen
		Context cx = Context.enter();
		try
		{
			cx.evaluateString(form.getFormScope(), "elements.myCustomComponent.myFoundset = foundset;" +
				"elements.myCustomComponent.columns = [{ myDataprovider: 'test1', myValuelist: 'myVl1' }];", "a", 1, null);

			String changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);
			JSONObject changesJSON = new JSONObject(changes);
			JSONObject col0JSON = changesJSON.getJSONObject("changes").getJSONObject("myCustomComponent").getJSONObject("columns").getJSONArray(
				"v").getJSONObject(0).getJSONObject("v");
			Assert.assertArrayEquals("data should get sent to client",
				new String[] { "value1", "value2", "value3", "value4", "value5", "value6", "value7", "value8", "value9", "value10", "value11", "value12", "value13", "value14", "value15", "value16", "value17", "value18" },
				new JSONWrapperList(col0JSON.getJSONObject("myDataprovider").getJSONArray("vp")).toArray());

			Assert.assertEquals("valuelist should get sent to client", 4, col0JSON.getJSONObject("myValuelist").getJSONArray("values").length());

			// detach valuelist in column + change foundset prop. value - detach should have cleared registered property change listener for foundset and changing the foundset would result in a NPE if those listeners were still executed for disposed valuelist prop.
			cx.evaluateString(form.getFormScope(),
				"elements.myCustomComponent.columns = [];" + "elements.myCustomComponent.myFoundset = databaseManager.getFoundSet(foundset.getDataSource());" +
					"elements.myCustomComponent.myFoundset.foundset.loadAllRecords();" +
					"elements.myCustomComponent.columns = [{ myDataprovider: 'test2', myValuelist: 'myVl2' }];",
				"a", 1, null);

			changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);
			changesJSON = new JSONObject(changes);
			col0JSON = changesJSON.getJSONObject("changes").getJSONObject("myCustomComponent").getJSONObject("columns").getJSONArray("v").getJSONObject(
				0).getJSONObject("v");
			Assert.assertArrayEquals("data should get sent to client",
				new String[] { "valueA", "valueB", "valueC", "valueD", "valueE", "valueF", "valueG", "valueH", "valueI", "valueJ", "valueK", "valueL", "valueM", "valueN", "valueO", "valueP", "valueQ", "valueR" },
				new JSONWrapperList(col0JSON.getJSONObject("myDataprovider").getJSONArray("vp")).toArray());
		}
		finally
		{
			Context.exit();
		}
	}

}