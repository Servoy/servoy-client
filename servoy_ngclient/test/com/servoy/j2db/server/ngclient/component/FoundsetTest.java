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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.Container;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.WebsocketEndpoint;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class FoundsetTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("FoundSetTest.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("FoundSetTest-mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.component.AbstractSoluionTest#createSolution()
	 */
	@Override
	protected void fillTestSolution() throws ServoyException
	{
		Form form = solution.createNewForm(validator, null, "test", "mem:test", false, new Dimension(600, 400));
		Bean bean = form.createNewBean("mycustombean", "my-component");
		bean.setInnerHTML("{myfoundset:{dataproviders:{firstname:'test1',lastname:'test2'}}}");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.component.AbstractSoluionTest#setupData()
	 */
	@Override
	protected void setupData() throws ServoyException
	{
		BufferedDataSet ds = new BufferedDataSet(new String[] { "pk", "test1", "test2" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		ds.addRow(new Object[] { Integer.valueOf(1), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(2), "value3", "value4" });
		client.getFoundSetManager().createDataSourceFromDataSet("test", ds, null, new String[] { "pk" });
	}

	@Test
	public void foundsetReadByDataproviders() throws JSONException
	{
		WebsocketEndpoint endpoint = (WebsocketEndpoint)WebsocketEndpoint.get();

		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		TypedData<Map<String, Map<String, Object>>> allComponentsProperties = form.getFormUI().getAllComponentsProperties();
		String full = JSONUtils.writeDataWithConversions(allComponentsProperties.content, allComponentsProperties.contentType);
		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mycustombean");
		JSONObject foundset = bean.getJSONObject("myfoundset");
		Assert.assertEquals(2, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		Assert.assertEquals(0, viewPort.getInt("startIndex"));
		Assert.assertEquals(0, viewPort.getInt("size"));
		Assert.assertEquals(0, viewPort.getJSONArray("rows").length());

		// fake incomming request for view port change.
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"myfoundset\":[{\"newViewPort\":{\"startIndex\":0,\"size\":2}}]}},\"service\":\"formService\"}",
			true);

		TypedData<Map<String, Map<String, Object>>> allComponentsChanges = ((Container)form.getFormUI()).getAllComponentsChanges();
		String changes = JSONUtils.writeDataWithConversions(allComponentsChanges.content, allComponentsChanges.contentType);
		object = new JSONObject(changes);
		bean = object.getJSONObject("mycustombean");
		foundset = bean.getJSONObject("myfoundset");
		Assert.assertEquals(2, foundset.getInt("serverSize"));
		viewPort = foundset.getJSONObject("viewPort");
		Assert.assertEquals(0, viewPort.getInt("startIndex"));
		Assert.assertEquals(2, viewPort.getInt("size"));
		JSONArray rows = viewPort.getJSONArray("rows");
		Assert.assertEquals(2, rows.length());

		JSONObject row0 = rows.getJSONObject(0);
		Assert.assertEquals("value1", row0.getString("firstname"));
		Assert.assertEquals("value2", row0.getString("lastname"));

		JSONObject row1 = rows.getJSONObject(1);
		Assert.assertEquals("value3", row1.getString("firstname"));
		Assert.assertEquals("value4", row1.getString("lastname"));

		// fake an update
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"myfoundset\":[{\"viewportDataChanged\":{\"_svyRowId\":\"" +
				row1.getString("_svyRowId") + "\",\"value\":\"value5\",\"dp\":\"lastname\"}}]}},\"service\":\"formService\"}", true);

		Assert.assertEquals("value5", form.getFormModel().getRecord(1).getValue("test2"));

	}
}
