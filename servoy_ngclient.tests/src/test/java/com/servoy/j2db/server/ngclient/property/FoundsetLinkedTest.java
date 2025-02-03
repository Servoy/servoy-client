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
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.RowManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class FoundsetLinkedTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("PropertyTests.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("FoundSetTest-mycomponent.spec");
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
			Form form = solution.createNewForm(validator, null, "dummyForm", null, false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			solution.setFirstFormID(form.getID()); // just a dummy form as first form so that it doesn't initialize our first form too soon, when solution is first shown (the test form will still get populated with design-time content at the beginning of tests)

			form = solution.createNewForm(validator, null, "test", "mem:test", false, new Dimension(600, 400));
			form.setNavigatorID(-1);
			form.createNewPart(IBaseSMPart.BODY, 5);
			WebComponent bean = form.createNewWebComponent("mycustombean", "my-component");
			bean.setProperty("myfoundsetWithAllow", new ServoyJSONObject("{foundsetSelector:'',dataproviders:{firstname:'test1',lastname:'test2'}}", false));
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
		ds.addRow(new Object[] { Integer.valueOf(1), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(2), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(3), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(4), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(5), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(6), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(7), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(8), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(9), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(10), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(11), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(12), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(13), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(14), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(15), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(16), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(17), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(18), "value3", "value4" });
		client.getFoundSetManager().insertToDataSource("test", ds, null, new WrappedObjectReference<>(new String[] { "pk" }), true, false);

		ConcurrentHashMap<String, IServer> serverProxies = new ConcurrentHashMap<String, IServer>();
		serverProxies.put("_sv_inmem", DUMMY_ISERVER);
		solution.setServerProxies(serverProxies);
	}

	@Test
	public void foundsetLinkedWithFormVariable() throws JSONException, RepositoryException
	{
		// create and use the form variable (which is a DP that does not depend on the foundset at all so it should just work with simple values, not viewports)
		Form designTimeform = solution.getForm("test");
		designTimeform.createNewScriptVariable(validator, "formVarAllow", IColumnTypes.NUMBER).setDefaultValue("105");
		designTimeform.createNewScriptVariable(validator, "formVarReject", IColumnTypes.NUMBER).setDefaultValue("105");
		WebComponent designTimeComponent = designTimeform.getWebComponents().next();
		designTimeComponent.setProperty("datalinkedDPAllow", "formVarAllow");
		designTimeComponent.setProperty("datalinkedDPReject", "formVarReject");

		// check what happens
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		Assert.assertNotNull(form);
		String full = NGUtils.formComponentPropertiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);

		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mycustombean");
		JSONObject foundset = bean.getJSONObject("myfoundsetWithAllow");

		Assert.assertEquals(18, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		Assert.assertEquals(0, viewPort.getInt("startIndex"));
		// 15 is default preferredViewPortSize
		Assert.assertEquals(15, viewPort.getInt("size"));
		Assert.assertEquals(15, viewPort.getJSONArray("rows").length());

		JSONObject fsLinkedReject = bean.getJSONObject("datalinkedDPReject");
		JSONObject fsLinkedAllow = bean.getJSONObject("datalinkedDPAllow");
		Assert.assertEquals(105, fsLinkedReject.optInt(FoundsetLinkedPropertyType.SINGLE_VALUE));
		Assert.assertEquals(105, fsLinkedAllow.optInt(FoundsetLinkedPropertyType.SINGLE_VALUE));

		// fake incomming update (simple dataPush not svyPush) for DP changes on these fsLinked properties; one should get rejected, the other allowed
		// so this will change the property value but not push to the dataprovider/form var
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"datalinkedDPReject\":501}},\"service\":\"formService\"}",
			true);
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"datalinkedDPAllow\":501}},\"service\":\"formService\"}",
			true);

		WebFormComponent comp = form.getFormUI().getWebComponent("mycustombean");
		Assert.assertEquals(501.0,
			((DataproviderTypeSabloValue)((FoundsetLinkedTypeSabloValue)comp.getProperty("datalinkedDPAllow")).getWrappedValue()).getValue());
		Assert.assertEquals(105.0,
			((DataproviderTypeSabloValue)((FoundsetLinkedTypeSabloValue)comp.getProperty("datalinkedDPReject")).getWrappedValue()).getValue()); // not value 501 cause pushToServer is rejected!

		// fake incomming update/svyPush for DP changes on these fsLinked properties; one should get rejected, the other allowed and changed in the DP
		String pkFromClientForThirdRow = RowManager.createPKHashKey(new Object[] { 3 }); // this is not needed for form variables but I think client would send it anyway
		endpoint.incoming(
			"{\"methodname\":\"svyPush\",\"args\":{\"fslRowID\":\"" + pkFromClientForThirdRow +
				"\",\"property\":\"datalinkedDPReject\",\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"datalinkedDPReject\":[{\"propertyChange\":111}]}},\"service\":\"formService\"}",
			true);
		endpoint.incoming(
			"{\"methodname\":\"svyPush\",\"args\":{\"fslRowID\":\"" + pkFromClientForThirdRow +
				"\",\"property\":\"datalinkedDPAllow\",\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"datalinkedDPAllow\":[{\"propertyChange\":111}]}},\"service\":\"formService\"}",
			true);

		Assert.assertEquals(111.0, form.getFormScope().get("formVarAllow"));
		Assert.assertEquals(105.0, form.getFormScope().get("formVarReject"));
	}

	@Test
	public void foundsetLinkedWithFoundsetColumn() throws JSONException, RepositoryException
	{
		// use table column DPs for fsLinked properties
		Form designTimeform = solution.getForm("test");
		WebComponent designTimeComponent = designTimeform.getWebComponents().next();
		designTimeComponent.setProperty("datalinkedDPAllow", "test1");
		designTimeComponent.setProperty("datalinkedDPReject", "test2");

		// check what happens
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		Assert.assertNotNull(form);
		String full = NGUtils.formComponentPropertiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);

		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mycustombean");
		JSONObject foundset = bean.getJSONObject("myfoundsetWithAllow");

		Assert.assertEquals(18, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		Assert.assertEquals(0, viewPort.getInt("startIndex"));
		// 15 is default preferredViewPortSize
		Assert.assertEquals(15, viewPort.getInt("size"));
		Assert.assertEquals(15, viewPort.getJSONArray("rows").length());

		JSONArray fsLinkedReject = bean.getJSONObject("datalinkedDPReject").optJSONArray(FoundsetLinkedPropertyType.VIEWPORT_VALUE);
		JSONArray fsLinkedAllow = bean.getJSONObject("datalinkedDPAllow").optJSONArray(FoundsetLinkedPropertyType.VIEWPORT_VALUE);
		Assert.assertEquals(15, fsLinkedReject.length());
		Assert.assertEquals(15, fsLinkedAllow.length());
		Assert.assertEquals("value1", fsLinkedAllow.optString(0));
		Assert.assertEquals("value2", fsLinkedReject.optString(0));
		Assert.assertEquals("value3", fsLinkedAllow.optString(1));
		Assert.assertEquals("value4", fsLinkedReject.optString(1));

		// fake incomming update/svyPush for DP changes on these fsLinked properties; one should get rejected, the other allowed
		String pkFromClientForThirdRow = RowManager.createPKHashKey(new Object[] { 3 });
		endpoint.incoming(
			"{\"methodname\":\"svyPush\",\"args\":{\"fslRowID\":\"" + pkFromClientForThirdRow +
				"\", \"property\":\"datalinkedDPReject\",\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"datalinkedDPReject\":[{\"viewportDataChanged\":{\"_svyRowId\":\"" +
				pkFromClientForThirdRow + "\", \"value\":\"value501\"}}]}},\"service\":\"formService\"}",
			true);
		endpoint.incoming(
			"{\"methodname\":\"svyPush\",\"args\":{\"fslRowID\":\"" + pkFromClientForThirdRow +
				"\",\"property\":\"datalinkedDPAllow\", \"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"datalinkedDPAllow\":[{\"viewportDataChanged\":{\"_svyRowId\":\"" +
				pkFromClientForThirdRow + "\", \"value\":\"value501\"}}]}},\"service\":\"formService\"}",
			true);

		WebFormComponent comp = form.getFormUI().getWebComponent("mycustombean");
		form.getFormModel().setSelectedIndex(2); // so that the getWrappedValue calls below target the correct row (FoundsetDataAdapterList is updated currently based on selection which also means that the wrapped values will match the selection)
		Assert.assertEquals("value501",
			((DataproviderTypeSabloValue)((FoundsetLinkedTypeSabloValue)comp.getProperty("datalinkedDPAllow")).getWrappedValue()).getValue());
		Assert.assertEquals("value2",
			((DataproviderTypeSabloValue)((FoundsetLinkedTypeSabloValue)comp.getProperty("datalinkedDPReject")).getWrappedValue()).getValue()); // not value 501 cause pushToServer is rejected!
	}

}
