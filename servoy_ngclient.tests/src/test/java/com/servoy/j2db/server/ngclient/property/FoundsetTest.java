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

import static com.servoy.base.query.IQueryConstants.LEFT_OUTER_JOIN;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.junit.Test;
import org.sablo.Container;
import org.sablo.IChangeListener;
import org.sablo.InMemPackageReader;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.WrappedObjectReference;

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
		String comp1 = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("FoundSetTest-pagingcomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String compWithPaging = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("FoundSetTest-mydynamiccomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp2 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp1);
		components.put("mydynamiccomponent.spec", comp2);
		components.put("mypagingcomponent.spec", compWithPaging);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		Form form = solution.createNewForm(validator, null, "test", "mem:test", false, new Dimension(600, 400));
		form.setNavigatorID(-1);
		form.createNewPart(IBaseSMPart.BODY, 5);
		WebComponent bean = form.createNewWebComponent("mycustombean", "my-component");
		bean.setProperty("myfoundset", new ServoyJSONObject("{foundsetSelector:'',dataproviders:{firstname:'test1',lastname:'test2'}}", false));
		bean.setProperty("myfoundsetWithAllow", new ServoyJSONObject("{foundsetSelector:'',dataproviders:{firstname:'test1',lastname:'test2'}}", false));

		WebComponent bean1 = form.createNewWebComponent("mydynamiccustombean", "my-dynamiccomponent");
		bean1.setProperty("myfoundset",
			new ServoyJSONObject("{foundsetSelector:'test_to_relatedtest', dataproviders:{dp1:'relatedtest1',dp2:'relatedtest2'}}", false));
		bean1.setProperty("myfoundsetWithAllow",
			new ServoyJSONObject("{foundsetSelector:'test_to_relatedtest', dataproviders:{dp1:'relatedtest1',dp2:'relatedtest2'}}", false));

		WebComponent bean2 = form.createNewWebComponent("mycustomseparatefoundsetbean", "my-component");
		bean2.setProperty("myfoundset", new ServoyJSONObject(
			"{foundsetSelector: \"mem:testseparatefoundset\", loadAllRecords: true, dataproviders:{firstname:'test1',lastname:'test2'}}", false));

		Form formSel8 = solution.createNewForm(validator, null, "testSel8", "mem:testsel8", false, new Dimension(600, 400));
		formSel8.setNavigatorID(-1);
		formSel8.createNewPart(IBaseSMPart.BODY, 5);

		WebComponent componentWithPaging = formSel8.createNewWebComponent("mycomponentwithpaging", "mypagingcomponent");
		componentWithPaging.setProperty("myfoundset",
			new ServoyJSONObject("{foundsetSelector:'',dataproviders:{firstname:'test1',lastname:'test2'}}", false));
		componentWithPaging.setProperty("pageSize", Integer.valueOf(7));
		componentWithPaging.setProperty("fakePageSize", Integer.valueOf(77));

		WebComponent componentWithPagingLT1_1 = formSel8.createNewWebComponent("mycomponentwithpagingLT1_1", "mypagingcomponent");
		componentWithPagingLT1_1.setProperty("myfoundset",
			new ServoyJSONObject("{foundsetSelector:'',dataproviders:{firstname:'test1',lastname:'test2'}}", false));
		componentWithPagingLT1_1.setProperty("pageSize", Integer.valueOf(0));

		WebComponent componentWithPagingLT1_2 = formSel8.createNewWebComponent("mycomponentwithpagingLT1_2", "mypagingcomponent");
		componentWithPagingLT1_2.setProperty("myfoundset",
			new ServoyJSONObject("{foundsetSelector:'',dataproviders:{firstname:'test1',lastname:'test2'}}", false));
		componentWithPagingLT1_2.setProperty("pageSize", Integer.valueOf(-1));
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
		client.getFoundSetManager().insertToDataSource("test", ds, null, new WrappedObjectReference<String[]>(new String[] { "pk" }), true, false);
		client.getFoundSetManager().insertToDataSource("testsel8", ds, null, new WrappedObjectReference<String[]>(new String[] { "pk" }), true, false);

		BufferedDataSet separateDSs = new BufferedDataSet(new String[] { "pk", "test1", "test2" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		for (int i = 0; i < 943; i++)
		{
			separateDSs.addRow(new Object[] { Integer.valueOf(i), "value" + i + "0", "value" + i + "1" });
		}
		client.getFoundSetManager().insertToDataSource("testseparatefoundset", separateDSs, null, new WrappedObjectReference<String[]>(new String[] { "pk" }),
			true, false);

		BufferedDataSet relatedDS = new BufferedDataSet(new String[] { "relatedtestpk", "testpk", "relatedtest1", "relatedtest2" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		relatedDS.addRow(new Object[] { Integer.valueOf(1), Integer.valueOf(1), "relatedvalue111", "relatedvalue112" });
		relatedDS.addRow(new Object[] { Integer.valueOf(2), Integer.valueOf(1), "relatedvalue121", "relatedvalue122" });
		relatedDS.addRow(new Object[] { Integer.valueOf(3), Integer.valueOf(1), "relatedvalue131", "relatedvalue132" });
		relatedDS.addRow(new Object[] { Integer.valueOf(4), Integer.valueOf(2), "relatedvalue241", "relatedvalue242" });
		relatedDS.addRow(new Object[] { Integer.valueOf(5), Integer.valueOf(1), "relatedvalue111", "relatedvalue112" });
		relatedDS.addRow(new Object[] { Integer.valueOf(6), Integer.valueOf(1), "relatedvalue121", "relatedvalue122" });
		relatedDS.addRow(new Object[] { Integer.valueOf(7), Integer.valueOf(1), "relatedvalue131", "relatedvalue132" });
		relatedDS.addRow(new Object[] { Integer.valueOf(8), Integer.valueOf(2), "relatedvalue241", "relatedvalue242" });
		relatedDS.addRow(new Object[] { Integer.valueOf(9), Integer.valueOf(1), "relatedvalue111", "relatedvalue112" });
		relatedDS.addRow(new Object[] { Integer.valueOf(10), Integer.valueOf(1), "relatedvalue121", "relatedvalue122" });
		relatedDS.addRow(new Object[] { Integer.valueOf(11), Integer.valueOf(1), "relatedvalue131", "relatedvalue132" });
		relatedDS.addRow(new Object[] { Integer.valueOf(12), Integer.valueOf(2), "relatedvalue241", "relatedvalue242" });
		relatedDS.addRow(new Object[] { Integer.valueOf(13), Integer.valueOf(1), "relatedvalue111", "relatedvalue112" });
		relatedDS.addRow(new Object[] { Integer.valueOf(14), Integer.valueOf(1), "relatedvalue121", "relatedvalue122" });
		relatedDS.addRow(new Object[] { Integer.valueOf(15), Integer.valueOf(1), "relatedvalue131", "relatedvalue132" });
		relatedDS.addRow(new Object[] { Integer.valueOf(16), Integer.valueOf(2), "relatedvalue241", "relatedvalue242" });
		client.getFoundSetManager().insertToDataSource("relatedtest", relatedDS, null, new WrappedObjectReference<String[]>(new String[] { "relatedtestpk" }),
			true, false);

		ConcurrentHashMap<String, IServer> serverProxies = new ConcurrentHashMap<String, IServer>();
		serverProxies.put("_sv_inmem", DUMMY_ISERVER);
		solution.setServerProxies(serverProxies);

		Relation relation = solution.createNewRelation(validator, "test_to_relatedtest", "mem:test", "mem:relatedtest", LEFT_OUTER_JOIN);
		Column primaryColumn = ((Table)client.getFoundSetManager().getTable(relation.getPrimaryDataSource())).getColumn("pk");
		Column foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("testpk");
		relation.createNewRelationItem(client.getFoundSetManager(), primaryColumn, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);

		IFoundSetInternal foundset = client.getFoundSetManager().getSharedFoundSet("mem:testsel8");
		foundset.loadAllRecords();
		foundset.setSelectedIndex(8);
	}

	@Test
	public void foundsetReadByDataprovidersPushToServerReject() throws JSONException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		String full = NGUtils.formComponentPropertiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);

		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mycustombean");
		JSONObject foundset = bean.getJSONObject("myfoundset");
		assertEquals(18, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		assertEquals(0, viewPort.getInt("startIndex"));
		// 15 is default preferredViewPortSize
		assertEquals(15, viewPort.getInt("size"));
		assertEquals(15, viewPort.getJSONArray("rows").length());

		// fake incomming request for view port change.
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"myfoundset\":[{\"newViewPort\":{\"startIndex\":0,\"size\":18}, \"id\": 4321}]}},\"service\":\"formService\"}",
			true);

		String changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);

		object = new JSONObject(changes).getJSONObject("changes");
		bean = object.getJSONObject("mycustombean");
		foundset = bean.getJSONObject("myfoundset");
		assertEquals(18, foundset.getInt("serverSize"));
		viewPort = foundset.getJSONObject("viewPort");
		assertEquals(0, viewPort.getInt("startIndex"));
		assertEquals(18, viewPort.getInt("size"));
		JSONArray rows = viewPort.getJSONArray("rows");
		assertEquals(18, rows.length());

		JSONArray handledClientReqIds = foundset.getJSONArray("handledClientReqIds");
		assertEquals(1, handledClientReqIds.length());
		assertEquals(4321, handledClientReqIds.getJSONObject(0).getInt("id"));
		assertTrue(handledClientReqIds.getJSONObject(0).getBoolean("value"));

		JSONObject row0 = rows.getJSONObject(0);
		assertEquals("value1", row0.getString("firstname"));
		assertEquals("value2", row0.getString("lastname"));

		JSONObject row1 = rows.getJSONObject(1);
		assertEquals("value3", row1.getString("firstname"));
		assertEquals("value4", row1.getString("lastname"));

		// fake an update
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"myfoundset\":[{\"viewportDataChanged\":{\"_svyRowId\":\"" +
				row1.getString("_svyRowId") + "\",\"value\":\"value5\",\"dp\":\"lastname\"}}]}},\"service\":\"formService\"}",
			true);

		assertEquals("value4", form.getFormModel().getRecord(1).getValue("test2")); // not value 5 cause pushToServer is rejected!
	}

	@Test
	public void foundsetReadByDataprovidersPushToServerAllow() throws JSONException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		String full = NGUtils.formComponentPropertiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);

		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mycustombean");
		JSONObject foundset = bean.getJSONObject("myfoundsetWithAllow");
		assertEquals(18, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		assertEquals(0, viewPort.getInt("startIndex"));
		// 15 is default preferredViewPortSize
		assertEquals(15, viewPort.getInt("size"));
		assertEquals(15, viewPort.getJSONArray("rows").length());

		// fake incomming request for view port change.
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"myfoundsetWithAllow\":[{\"newViewPort\":{\"startIndex\":0,\"size\":18}, \"id\": 1234}]}},\"service\":\"formService\"}",
			true);

		String changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);

		object = new JSONObject(changes).getJSONObject("changes");
		bean = object.getJSONObject("mycustombean");
		foundset = bean.getJSONObject("myfoundsetWithAllow");
		assertEquals(18, foundset.getInt("serverSize"));
		assertEquals(18, foundset.getInt("serverSize"));
		JSONArray handledClientReqIds = foundset.getJSONArray("handledClientReqIds");
		assertEquals(1, handledClientReqIds.length());
		assertEquals(1234, handledClientReqIds.getJSONObject(0).getInt("id"));
		assertTrue(handledClientReqIds.getJSONObject(0).getBoolean("value"));

		viewPort = foundset.getJSONObject("viewPort");
		assertEquals(0, viewPort.getInt("startIndex"));
		assertEquals(18, viewPort.getInt("size"));
		JSONArray rows = viewPort.getJSONArray("rows");
		assertEquals(18, rows.length());

		JSONObject row0 = rows.getJSONObject(0);
		assertEquals("value1", row0.getString("firstname"));
		assertEquals("value2", row0.getString("lastname"));

		JSONObject row1 = rows.getJSONObject(1);
		assertEquals("value3", row1.getString("firstname"));
		assertEquals("value4", row1.getString("lastname"));

		// fake an update
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mycustombean\",\"formname\":\"test\",\"changes\":{\"myfoundsetWithAllow\":[{\"viewportDataChanged\":{\"_svyRowId\":\"" +
				row1.getString("_svyRowId") + "\",\"value\":\"value5\",\"dp\":\"lastname\"}}]}},\"service\":\"formService\"}",
			true);

		assertEquals("value5", form.getFormModel().getRecord(1).getValue("test2"));
	}

	@Test
	public void foundsetWithDynamicDataproviders() throws JSONException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		WebFormComponent webComponent = form.getFormUI().getWebComponent("mydynamiccustombean");
		FoundsetTypeSabloValue property = (FoundsetTypeSabloValue)webComponent.getProperty("myfoundset");
		JSONArray json = new JSONArray("[{" + FoundsetTypeSabloValue.PREFERRED_VIEWPORT_SIZE + ":1}]");
		property.browserUpdatesReceived(json, webComponent.getSpecification().getProperty("myfoundset"),
			new BrowserConverterContext(webComponent, PushToServerEnum.allow));

		assertNotNull(form);
		String full = NGUtils.formComponentPropertiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);
		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mydynamiccustombean");
		JSONObject foundset = bean.getJSONObject("myfoundset");
		assertEquals(12, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		assertEquals(0, viewPort.getInt("startIndex"));
		assertEquals(12, viewPort.getInt("size"));
		assertEquals(12, viewPort.getJSONArray("rows").length());

		// fake incomming request for view port change.
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mydynamiccustombean\",\"formname\":\"test\",\"changes\":{\"myfoundset\":[{\"newViewPort\":{\"startIndex\":0,\"size\":3}, \"id\": 4312}]}},\"service\":\"formService\"}",
			true);

		String changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);
		object = new JSONObject(changes).getJSONObject("changes");
		bean = object.getJSONObject("mydynamiccustombean");
		foundset = bean.getJSONObject("myfoundset");
		assertEquals(12, foundset.getInt("serverSize"));
		viewPort = foundset.getJSONObject("viewPort");
		assertEquals(0, viewPort.getInt("startIndex"));
		assertEquals(3, viewPort.getInt("size"));
		JSONArray rows = viewPort.getJSONArray("rows");
		assertEquals(3, rows.length());

		JSONArray handledClientReqIds = foundset.getJSONArray("handledClientReqIds");
		assertEquals(1, handledClientReqIds.length());
		assertEquals(4312, handledClientReqIds.getJSONObject(0).getInt("id"));
		assertTrue(handledClientReqIds.getJSONObject(0).getBoolean("value"));

		JSONObject row0 = rows.getJSONObject(0);
		assertEquals("relatedvalue111", row0.getString("dp1"));
		assertEquals("relatedvalue112", row0.getString("dp2"));

		JSONObject row1 = rows.getJSONObject(1);
		assertEquals("relatedvalue121", row1.getString("dp1"));
		assertEquals("relatedvalue122", row1.getString("dp2"));
	}

	@Test
	public void foundsetRelated() throws JSONException// change selected index in main foundset and related foundset should change
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		BrowserConverterContext allowBrowserConverterContext2 = new BrowserConverterContext(form.getFormUI().getWebComponent("mydynamiccustombean"),
			PushToServerEnum.allow);

		FoundsetTypeSabloValue customBeanFoundSet = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycustombean").getRawPropertyValue("myfoundset");
		FoundsetTypeSabloValue dynamicBeanRelatedFoundset = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mydynamiccustombean").getRawPropertyValue(
			"myfoundset");
		dynamicBeanRelatedFoundset.getViewPort().setBounds(1, 1);
		customBeanFoundSet.getFoundset().setSelectedIndex(1);//selection is now 0, so set to 1 and then back again
		customBeanFoundSet.getFoundset().setSelectedIndex(0);
		assertEquals(12, dynamicBeanRelatedFoundset.getViewPort().getSize());
		assertEquals(0, dynamicBeanRelatedFoundset.getViewPort().getStartIndex());
		dynamicBeanRelatedFoundset.getViewPort().setBounds(1, 1);
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		dynamicBeanRelatedFoundset.addViewPort(jsonWriter);
		assertEquals("{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.2;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"}]}",
			stringWriter.toString());

		stringWriter.getBuffer().setLength(0);
		jsonWriter = new JSONWriter(stringWriter);
		dynamicBeanRelatedFoundset.changesToJSON(jsonWriter, allowBrowserConverterContext2);
		JSONAssert.assertEquals(format(
			"{\"upd_serverSize\":12,\"upd_foundsetId\":%d,\"upd_selectedRowIndexes\":[0],\"upd_viewPort\":{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.2;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"}]}}",
			dynamicBeanRelatedFoundset.getFoundset().getID()),
			stringWriter.toString(),
			true);

		customBeanFoundSet.getFoundset().setSelectedIndex(1);
		dynamicBeanRelatedFoundset.getViewPort().setBounds(0, 1);
		stringWriter.getBuffer().setLength(0);
		jsonWriter = new JSONWriter(stringWriter);
		dynamicBeanRelatedFoundset.changesToJSON(jsonWriter, allowBrowserConverterContext2);
		assertEquals(format(
			"{\"upd_serverSize\":4,\"upd_foundsetId\":%d,\"upd_selectedRowIndexes\":[0],\"upd_viewPort\":{\"startIndex\":0,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.4;\",\"dp1\":\"relatedvalue241\",\"dp2\":\"relatedvalue242\"}]}}",
			dynamicBeanRelatedFoundset.getFoundset().getID()),
			stringWriter.toString());
	}

	@Test
	public void setPreferredViewport() throws JSONException// change selected index in main foundset and related foundset should change
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc1 = form.getFormUI().getWebComponent("mycustombean");
		WebFormComponent wc2 = form.getFormUI().getWebComponent("mydynamiccustombean");
		FoundsetTypeSabloValue customBeanFoundSet = (FoundsetTypeSabloValue)wc1.getRawPropertyValue("myfoundset");
		FoundsetTypeSabloValue dynamicBeanRelatedFoundset = (FoundsetTypeSabloValue)wc2.getRawPropertyValue("myfoundset");

		BrowserConverterContext allowBrowserConverterContext2 = new BrowserConverterContext(wc1, PushToServerEnum.allow);

		dynamicBeanRelatedFoundset.getViewPort().setPreferredViewportSize(8);
		customBeanFoundSet.getFoundset().setSelectedIndex(1);//selection is now 0, so set to 1 and then back again
		customBeanFoundSet.getFoundset().setSelectedIndex(0);
		assertEquals(8, dynamicBeanRelatedFoundset.getViewPort().getSize());
		assertEquals(0, dynamicBeanRelatedFoundset.getViewPort().getStartIndex());
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		dynamicBeanRelatedFoundset.toJSON(jsonWriter, allowBrowserConverterContext2);
		assertEquals(format(
			"{\"serverSize\":12,\"foundsetId\":%d,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":8,\"rows\":[{\"_svyRowId\":\"1.1;\",\"dp1\":\"relatedvalue111\",\"dp2\":\"relatedvalue112\"},{\"_svyRowId\":\"1.2;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"},{\"_svyRowId\":\"1.3;\",\"dp1\":\"relatedvalue131\",\"dp2\":\"relatedvalue132\"},{\"_svyRowId\":\"1.5;\",\"dp1\":\"relatedvalue111\",\"dp2\":\"relatedvalue112\"},{\"_svyRowId\":\"1.6;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"},{\"_svyRowId\":\"1.7;\",\"dp1\":\"relatedvalue131\",\"dp2\":\"relatedvalue132\"},{\"_svyRowId\":\"1.9;\",\"dp1\":\"relatedvalue111\",\"dp2\":\"relatedvalue112\"},{\"_svyRowId\":\"2.10;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"}]}}",
			dynamicBeanRelatedFoundset.getFoundset().getID()),
			stringWriter.toString());

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		customBeanFoundSet.getFoundset().setSelectedIndex(1);//selection is now 0, so set to 1 and then back again
		dynamicBeanRelatedFoundset.changesToJSON(jsonWriter, allowBrowserConverterContext2);
		assertEquals(format(
			"{\"upd_serverSize\":4,\"upd_foundsetId\":%d,\"upd_selectedRowIndexes\":[0],\"upd_viewPort\":{\"startIndex\":0,\"size\":4,\"rows\":[{\"_svyRowId\":\"1.4;\",\"dp1\":\"relatedvalue241\",\"dp2\":\"relatedvalue242\"},{\"_svyRowId\":\"1.8;\",\"dp1\":\"relatedvalue241\",\"dp2\":\"relatedvalue242\"},{\"_svyRowId\":\"2.12;\",\"dp1\":\"relatedvalue241\",\"dp2\":\"relatedvalue242\"},{\"_svyRowId\":\"2.16;\",\"dp1\":\"relatedvalue241\",\"dp2\":\"relatedvalue242\"}]}}",
			dynamicBeanRelatedFoundset.getFoundset().getID()),
			stringWriter.toString());

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		customBeanFoundSet.getFoundset().setSelectedIndex(0);
		dynamicBeanRelatedFoundset.changesToJSON(jsonWriter, allowBrowserConverterContext2);
		assertEquals(format(
			"{\"upd_serverSize\":12,\"upd_foundsetId\":%d,\"upd_selectedRowIndexes\":[0],\"upd_viewPort\":{\"startIndex\":0,\"size\":8,\"rows\":[{\"_svyRowId\":\"1.1;\",\"dp1\":\"relatedvalue111\",\"dp2\":\"relatedvalue112\"},{\"_svyRowId\":\"1.2;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"},{\"_svyRowId\":\"1.3;\",\"dp1\":\"relatedvalue131\",\"dp2\":\"relatedvalue132\"},{\"_svyRowId\":\"1.5;\",\"dp1\":\"relatedvalue111\",\"dp2\":\"relatedvalue112\"},{\"_svyRowId\":\"1.6;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"},{\"_svyRowId\":\"1.7;\",\"dp1\":\"relatedvalue131\",\"dp2\":\"relatedvalue132\"},{\"_svyRowId\":\"1.9;\",\"dp1\":\"relatedvalue111\",\"dp2\":\"relatedvalue112\"},{\"_svyRowId\":\"2.10;\",\"dp1\":\"relatedvalue121\",\"dp2\":\"relatedvalue122\"}]}}",
			dynamicBeanRelatedFoundset.getFoundset().getID()),
			stringWriter.toString());
	}

	@Test
	public void foundsetViewportChangeData() throws JSONException, ServoyException// change rows in/near viewport
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc = form.getFormUI().getWebComponent("mycustombean");
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)wc.getRawPropertyValue("myfoundset");

		// right now the viewport change monitor will NOT ignore updates because the value is directly sent to the client (through the updateController code)
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		viewPort.setBounds(1, 1);
		viewPort.changeMonitor.clearChanges();
		rawPropertyValue.getFoundset().getRecord(0).startEditing();
		rawPropertyValue.getFoundset().getRecord(0).setValue("test1", "not test1 any more");
		rawPropertyValue.getFoundset().getRecord(0).stopEditing();
		assertEquals(0, viewPort.changeMonitor.viewPortDataChangeMonitor.getViewPortChanges().length);
		rawPropertyValue.getFoundset().getRecord(1).startEditing();
		rawPropertyValue.getFoundset().getRecord(1).setValue("test2", "not test2 any more");
		rawPropertyValue.getFoundset().getRecord(1).stopEditing();
		assertEquals(1, viewPort.changeMonitor.viewPortDataChangeMonitor.getViewPortChanges().length);

		// now simulate a send to client
		rawPropertyValue.toJSON(new JSONStringer(), new BrowserConverterContext(wc, PushToServerEnum.allow));

		rawPropertyValue.getFoundset().getRecord(0).startEditing();
		rawPropertyValue.getFoundset().getRecord(0).setValue("test1", "not test1 any more nor not test1 any more");
		rawPropertyValue.getFoundset().getRecord(0).stopEditing();
		assertEquals(0, viewPort.changeMonitor.viewPortDataChangeMonitor.getViewPortChanges().length);
		rawPropertyValue.getFoundset().getRecord(1).startEditing();
		rawPropertyValue.getFoundset().getRecord(1).setValue("test2", "not test2 any more nor not test2 any more");
		rawPropertyValue.getFoundset().getRecord(1).stopEditing();
		assertEquals(1, viewPort.changeMonitor.viewPortDataChangeMonitor.getViewPortChanges().length);
	}

	@Test
	public void foundsetViewportAllRecordChangedAndDeleted() throws JSONException, ServoyException// change rows in/near viewport
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc = form.getFormUI().getWebComponent("mycustombean");
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)wc.getRawPropertyValue("myfoundset");
		BrowserConverterContext allowBrowserConverterContext = new BrowserConverterContext(wc, PushToServerEnum.allow);
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		viewPort.setBounds(0, form.getFormModel().getSize());

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);


		JSONAssert.assertEquals(
			"{\"serverSize\":18,\"foundsetId\":3,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":18,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.3;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.4;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.5;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.6;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.7;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.8;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.9;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.10;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.11;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.12;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.13;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.14;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.15;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.16;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.17;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.18;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			stringWriter.toString(),
			true);

		form.getFormModel().fireFoundSetChanged();

		form.getFormModel().deleteRecord(0);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":17,\"foundsetId\":%d,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":17,\"rows\":[{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.3;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.4;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.5;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.6;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.7;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.8;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.9;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.10;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.11;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.12;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.13;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.14;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.15;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.16;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.17;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.18;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID()))
				.similar(new JSONObject(stringWriter.toString())));
	}

	@Test
	public void foundsetViewportAllRecordDeleted() throws JSONException, ServoyException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc = form.getFormUI().getWebComponent("mycustombean");
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)wc.getRawPropertyValue("myfoundset");
		BrowserConverterContext allowBrowserConverterContext = new BrowserConverterContext(wc, PushToServerEnum.allow);
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		FoundSet foundset = (FoundSet)form.getFormModel();
		viewPort.setBounds(5, 5);
		foundset.setSelectedIndex(6);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		// just to clear changed flags
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		// create an empty separate foundset from the same datasource
		FoundSet sepEmpFs = (FoundSet)client.getFoundSetManager().getNewFoundSet("mem:test");
		foundset.js_loadRecords(sepEmpFs);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.changesToJSON(jsonWriter, allowBrowserConverterContext);
		JSONAssert.assertEquals(
			"{\"upd_serverSize\":0,\"upd_selectedRowIndexes\":[],\"upd_viewPort\":{\"startIndex\":0,\"size\":0,\"upd_rows\":[{\"startIndex\":0,\"endIndex\":4,\"type\":2}]}}",
			stringWriter.toString(), true);
	}

	@Test
	public void foundsetViewportAddRemove() throws JSONException, ServoyException// add / remove rows in viewport, near viewport
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycustombean").getRawPropertyValue("myfoundset");

		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		viewPort.setPreferredViewportSize(1);
		viewPort.setBounds(1, 1);
		IFoundSetInternal foundSet = rawPropertyValue.getFoundset();
		foundSet.newRecord(1, false);
		assertEquals(1, viewPort.size);
		assertEquals(1, viewPort.startIndex);
		assertEquals(19, rawPropertyValue.getFoundset().getSize());
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.addViewPort(jsonWriter);

		assertTrue(
			new JSONObject("{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\".null;\",\"lastname\":null,\"firstname\":null}]}").similar(
				new JSONObject(stringWriter.toString())));
		foundSet.deleteRecord(1);


		assertEquals(18, rawPropertyValue.getFoundset().getSize());

		stringWriter.getBuffer().setLength(0);
		jsonWriter = new JSONWriter(stringWriter);

		rawPropertyValue.addViewPort(jsonWriter);

		assertTrue(
			new JSONObject("{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}").similar(
				new JSONObject(stringWriter.toString())));
		foundSet.newRecord(0, false);

		stringWriter.getBuffer().setLength(0);
		jsonWriter = new JSONWriter(stringWriter);

		rawPropertyValue.addViewPort(jsonWriter);

		assertTrue(
			new JSONObject("{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"}]}").similar(
				new JSONObject(stringWriter.toString())));

		foundSet.newRecord(3, false);
		assertEquals(20, rawPropertyValue.getFoundset().getSize());
		stringWriter.getBuffer().setLength(0);
		jsonWriter = new JSONWriter(stringWriter);

		rawPropertyValue.addViewPort(jsonWriter);

		assertTrue(
			new JSONObject("{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"}]}").similar(
				new JSONObject(stringWriter.toString())));


		//delete records added in this test
		foundSet.deleteRecord(0);
		foundSet.deleteRecord(2);//last record is now at index 2
		assertEquals(18, rawPropertyValue.getFoundset().getSize());
	}

	@Test
	public void foundsetViewportAddExtendsOrNotDueToPreferredViewportSize() throws JSONException, ServoyException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycustombean").getRawPropertyValue("myfoundset");

		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		IFoundSetInternal foundSet = rawPropertyValue.getFoundset();

		viewPort.setPreferredViewportSize(30);
		viewPort.setBounds(4, 3);
		assertEquals(18, foundSet.getSize());


		// new record before should shift right bounds; it will not grow in size automatically because it sees viewport was different then whole available rows even before
		rawPropertyValue.changeMonitor.clearChanges();
		foundSet.newRecord(1, false);

		assertEquals(3, viewPort.size);
		assertEquals(5, viewPort.startIndex);
		assertEquals(19, foundSet.getSize());
		assertTrue(viewPort.changeMonitor.shouldSendViewPortBounds());
		assertFalse(viewPort.changeMonitor.shouldSendWholeViewPort());
		assertFalse(viewPort.changeMonitor.hasViewportChanges());

		// insert happens inside viewport; it will not shift, not grow in size, just generate a viewport insert + delete data op (the previous last row in viewport is now out of the viewport)
		rawPropertyValue.changeMonitor.clearChanges();
		foundSet.newRecord(6, false);

		assertEquals(3, viewPort.size);
		assertEquals(5, viewPort.startIndex);
		assertEquals(20, foundSet.getSize());
		assertEquals(false, viewPort.changeMonitor.shouldSendViewPortBounds());
		assertEquals(false, viewPort.changeMonitor.shouldSendWholeViewPort());
		ArrayOperation[] vpChanges = viewPort.changeMonitor.getViewPortChanges();
		assertEquals(2, vpChanges.length);
		assertEquals(ArrayOperation.INSERT, vpChanges[0].type);
		assertEquals(1, vpChanges[0].startIndex);
		assertEquals(1, vpChanges[0].endIndex);
		assertEquals(ArrayOperation.DELETE, vpChanges[1].type);
		assertEquals(3, vpChanges[1].startIndex);
		assertEquals(3, vpChanges[1].endIndex);

		// insert happens after viewport; it will not shift, not grow in size, just know the foundset size is changed
		rawPropertyValue.changeMonitor.clearChanges();
		foundSet.newRecord(10, false);

		assertEquals(3, viewPort.size);
		assertEquals(5, viewPort.startIndex);
		assertEquals(21, foundSet.getSize());
		assertFalse(viewPort.changeMonitor.shouldSendViewPortBounds());
		assertFalse(viewPort.changeMonitor.shouldSendWholeViewPort());
		assertFalse(viewPort.changeMonitor.hasViewportChanges());
		assertTrue(viewPort.changeMonitor.shouldSendFoundsetSize());

		// OK now scenarios where viewport took up all rows but preferred is higher - then it will grow

		// insert happens after viewport; it will not shift, not grow in size, just know the foundset size is changed
		viewPort.setBounds(0, 21);
		rawPropertyValue.changeMonitor.clearChanges();
		foundSet.newRecord(10, false);

		assertEquals(22, viewPort.size);
		assertEquals(0, viewPort.startIndex);
		assertEquals(22, foundSet.getSize());
		assertTrue(viewPort.changeMonitor.shouldSendViewPortBounds());
		assertFalse(viewPort.changeMonitor.shouldSendWholeViewPort());
		assertTrue(viewPort.changeMonitor.hasViewportChanges());
		assertTrue(viewPort.changeMonitor.shouldSendFoundsetSize());
		vpChanges = viewPort.changeMonitor.getViewPortChanges();
		assertEquals(1, vpChanges.length);
		assertEquals(ArrayOperation.INSERT, vpChanges[0].type);
		assertEquals(10, vpChanges[0].startIndex);
		assertEquals(10, vpChanges[0].endIndex);
	}

	@Test
	public void foundsetViewportDeleteMustAdjustViewportSizeCorrectlyEvenIfItIsMarkedAsFullyChangedAlready() throws JSONException, ServoyException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycustombean").getRawPropertyValue("myfoundset");

		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		IFoundSetInternal foundSet = rawPropertyValue.getFoundset();

		viewPort.setPreferredViewportSize(30);
		viewPort.setBounds(4, 14);
		assertEquals(18, foundSet.getSize());

		// a delete that affects viewport bounds should update viewport bounds (no leave them invalid) even if viewport was previously marked as fully changed
		rawPropertyValue.changeMonitor.clearChanges();
		rawPropertyValue.changeMonitor.viewPortCompletelyChanged();
		foundSet.deleteRecord(3);

		// this went wrong before fixes for SVY-17654; viewport remained out-of-bounds
		assertEquals(13, viewPort.size);
		assertEquals(4, viewPort.startIndex);
		assertEquals(17, foundSet.getSize());
		assertTrue(viewPort.changeMonitor.shouldSendWholeViewPort());
		assertTrue(viewPort.changeMonitor.shouldSendFoundsetSize());
	}

	@Test
	public void foundsetViewportInsertShouldFireChangeMonitor() throws JSONException, ServoyException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycustombean").getRawPropertyValue("myfoundset");

		ValueReference<Boolean> valueChangeMotified = new ValueReference<>(Boolean.FALSE);
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		viewPort.changeMonitor.setChangeNotifier(new IChangeListener()
		{

			@Override
			public void valueChanged()
			{
				valueChangeMotified.value = Boolean.TRUE;
			}

		});
		IFoundSetInternal foundSet = rawPropertyValue.getFoundset();

		viewPort.setPreferredViewportSize(30);
		viewPort.setBounds(0, 18);
		assertEquals(18, foundSet.getSize());

		rawPropertyValue.changeMonitor.clearChanges();
		rawPropertyValue.changeMonitor.viewPortCompletelyChanged(); // I want the change notifier to trigger only due to foundset size change needing to get to client
		assertTrue(viewPort.changeMonitor.shouldSendWholeViewPort());
		assertFalse(viewPort.changeMonitor.shouldSendViewPortBounds());
		valueChangeMotified.value = Boolean.FALSE;

		foundSet.newRecord(5, false);

		assertEquals(19, viewPort.size);
		assertEquals(0, viewPort.startIndex);
		assertEquals(19, foundSet.getSize());
		assertTrue(viewPort.changeMonitor.shouldSendFoundsetSize());
		assertTrue(valueChangeMotified.value.booleanValue()); // due to foundset size change
	}

	@Test
	public void foundsetChangeMonitorChangeFlags() throws ServoyException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycustombean").getRawPropertyValue("myfoundset");

		IFoundSetInternal foundSet = rawPropertyValue.getFoundset();
		rawPropertyValue.changeMonitor.clearChanges();
		foundSet.newRecord(0, false);

		assertEquals(FoundsetTypeChangeMonitor.SEND_FOUNDSET_SIZE | FoundsetTypeChangeMonitor.SEND_SELECTED_INDEXES,
			rawPropertyValue.changeMonitor.changeFlags);

		rawPropertyValue.changeMonitor.clearChanges();
		foundSet.deleteRecord(0);

		assertEquals(FoundsetTypeChangeMonitor.SEND_FOUNDSET_SIZE | FoundsetTypeChangeMonitor.SEND_SELECTED_INDEXES,
			rawPropertyValue.changeMonitor.changeFlags);
	}

	@Test
	public void largeFoundsetUsageWithPreferredSize() throws Exception
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc = form.getFormUI().getWebComponent("mycustomseparatefoundsetbean");

		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)wc.getRawPropertyValue("myfoundset");
		BrowserConverterContext allowBrowserConverterContext = new BrowserConverterContext(wc, PushToServerEnum.allow);
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		viewPort.setPreferredViewportSize(7);
		rawPropertyValue.getFoundset().find();
		rawPropertyValue.getFoundset().search();

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		JSONAssert.assertEquals(new JSONObject(
			"{\"selectedRowIndexes\":[0],\"viewPort\":{\"startIndex\":0,\"size\":7,\"rows\":[{\"firstname\":\"value00\",\"_svyRowId\":\"1.0;\",\"lastname\":\"value01\"},{\"firstname\":\"value10\",\"_svyRowId\":\"1.1;\",\"lastname\":\"value11\"},{\"firstname\":\"value20\",\"_svyRowId\":\"1.2;\",\"lastname\":\"value21\"},{\"firstname\":\"value30\",\"_svyRowId\":\"1.3;\",\"lastname\":\"value31\"},{\"firstname\":\"value40\",\"_svyRowId\":\"1.4;\",\"lastname\":\"value41\"},{\"firstname\":\"value50\",\"_svyRowId\":\"1.5;\",\"lastname\":\"value51\"},{\"firstname\":\"value60\",\"_svyRowId\":\"1.6;\",\"lastname\":\"value61\"}]},\"sortColumns\":\"\",\"serverSize\":200,\"foundsetId\":2,\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":true}"),
			new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);
	}

	@Test
	public void largeFoundsetUsage() throws JSONException
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc = form.getFormUI().getWebComponent("mycustomseparatefoundsetbean");

		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)wc.getRawPropertyValue("myfoundset");
		BrowserConverterContext allowBrowserConverterContext = new BrowserConverterContext(wc, PushToServerEnum.allow);
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();

		viewPort.setBounds(0, 1);
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		JSONAssert.assertEquals(
			"{\"serverSize\":200,\"foundsetId\":2,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":true,\"viewPort\":{\"startIndex\":0,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.0;\",\"lastname\":\"value01\",\"firstname\":\"value00\"}]}}",
			stringWriter.toString(),
			true);

		// foundset loads more records due to server side access - client should be aware of new size and hasMoreRows
		rawPropertyValue.getFoundset().getRecord(200);

		StringWriter stringWriter2 = new StringWriter();
		JSONWriter jsonWriter2 = new JSONWriter(stringWriter2);
		rawPropertyValue.changesToJSON(jsonWriter2, allowBrowserConverterContext);

		// bounds is 0,1 we should only get size update, but no rows
		assertEquals(new JSONObject("{\"upd_serverSize\":799}").toString(), new JSONObject(stringWriter2.toString()).toString());

		viewPort.setBounds(0, 15);
		rawPropertyValue.getFoundset().getRecord(200);

		StringWriter stringWriter2_1 = new StringWriter();
		JSONWriter jsonWriter2_1 = new JSONWriter(stringWriter2_1);
		rawPropertyValue.changesToJSON(jsonWriter2_1, allowBrowserConverterContext);

		// bounds is 0,15 we should get the rows now
		assertEquals(new JSONObject(
			"{\"upd_viewPort\":{\"startIndex\":0,\"size\":15,\"rows\":[{\"_svyRowId\":\"1.0;\",\"firstname\":\"value00\",\"lastname\":\"value01\"},{\"_svyRowId\":\"1.1;\",\"firstname\":\"value10\",\"lastname\":\"value11\"},{\"_svyRowId\":\"1.2;\",\"firstname\":\"value20\",\"lastname\":\"value21\"},{\"_svyRowId\":\"1.3;\",\"firstname\":\"value30\",\"lastname\":\"value31\"},{\"_svyRowId\":\"1.4;\",\"firstname\":\"value40\",\"lastname\":\"value41\"},{\"_svyRowId\":\"1.5;\",\"firstname\":\"value50\",\"lastname\":\"value51\"},{\"_svyRowId\":\"1.6;\",\"firstname\":\"value60\",\"lastname\":\"value61\"},{\"_svyRowId\":\"1.7;\",\"firstname\":\"value70\",\"lastname\":\"value71\"},{\"_svyRowId\":\"1.8;\",\"firstname\":\"value80\",\"lastname\":\"value81\"},{\"_svyRowId\":\"1.9;\",\"firstname\":\"value90\",\"lastname\":\"value91\"},{\"_svyRowId\":\"2.10;\",\"firstname\":\"value100\",\"lastname\":\"value101\"},{\"_svyRowId\":\"2.11;\",\"firstname\":\"value110\",\"lastname\":\"value111\"},{\"_svyRowId\":\"2.12;\",\"firstname\":\"value120\",\"lastname\":\"value121\"},{\"_svyRowId\":\"2.13;\",\"firstname\":\"value130\",\"lastname\":\"value131\"},{\"_svyRowId\":\"2.14;\",\"firstname\":\"value140\",\"lastname\":\"value141\"}]}}")
				.toString(),
			new JSONObject(stringWriter2_1.toString()).toString());

		// foundset loads more records due to client side wanting more records
		viewPort.setBounds(800, 1);

		StringWriter stringWriter3 = new StringWriter();
		JSONWriter jsonWriter3 = new JSONWriter(stringWriter3);
		rawPropertyValue.changesToJSON(jsonWriter3, allowBrowserConverterContext);

		assertTrue(new JSONObject(
			"{\"upd_serverSize\":943,\"upd_hasMoreRows\":false,\"upd_viewPort\":{\"startIndex\":800,\"size\":1,\"rows\":[{\"_svyRowId\":\"3.800;\",\"lastname\":\"value8001\",\"firstname\":\"value8000\"}]}}")
				.similar(
					new JSONObject(stringWriter3.toString())));
	}

	@Test
	public void foundsetViewportBounds() throws Exception
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		assertNotNull(form);
		WebFormComponent wc = form.getFormUI().getWebComponent("mycustombean");

		FoundsetTypeSabloValue rawPropertyValue = (FoundsetTypeSabloValue)wc.getRawPropertyValue("myfoundset");
		BrowserConverterContext allowBrowserConverterContext = new BrowserConverterContext(wc, PushToServerEnum.allow);
		FoundsetTypeViewport viewPort = rawPropertyValue.getViewPort();
		viewPort.setBounds(0, 2);
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":18,\"foundsetId\":%d,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":2,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID()))
				.similar(new JSONObject(stringWriter.toString())));


		rawPropertyValue.getFoundset().setSort("test1 asc");

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":18,\"foundsetId\":%d,\"sortColumns\":\"firstname asc\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":2,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID()))
				.similar(new JSONObject(stringWriter.toString())));

		rawPropertyValue.getFoundset().setSort("test2 desc,pk asc,test1 asc");

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":18,\"foundsetId\":%d,\"sortColumns\":\"lastname desc\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":2,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID()))
				.similar(
					new JSONObject(stringWriter.toString())));

		rawPropertyValue.getFoundset().setSort("test2 desc,test1 asc");

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":18,\"foundsetId\":%d,\"sortColumns\":\"lastname desc,firstname asc\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":2,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID()))
				.similar(
					new JSONObject(stringWriter.toString())));

		rawPropertyValue.getFoundset().setSort("pk asc,test1 asc");

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		rawPropertyValue.toJSON(jsonWriter, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":18,\"foundsetId\":%d,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":0,\"size\":2,\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID()))
				.similar(
					new JSONObject(stringWriter.toString())));

		//changes to json
		//add changes test

		viewPort.setBounds(1, 1);
		StringWriter stringWriter2 = new StringWriter();
		JSONWriter jsonWriter2 = new JSONWriter(stringWriter2);
		rawPropertyValue.toJSON(jsonWriter2, allowBrowserConverterContext);

		assertTrue(new JSONObject(format(
			"{\"serverSize\":18,\"foundsetId\":%d,\"sortColumns\":\"\",\"selectedRowIndexes\":[0],\"multiSelect\":false,\"findMode\":false,\"hasMoreRows\":false,\"viewPort\":{\"startIndex\":1,\"size\":1,\"rows\":[{\"_svyRowId\":\"1.2;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}]}}",
			rawPropertyValue.getFoundset().getID())).similar(
				new JSONObject(stringWriter2.toString())));

		viewPort.loadExtraRecords(-1);

		StringWriter stringWriter3 = new StringWriter();
		JSONWriter jsonWriter3 = new JSONWriter(stringWriter3);
		rawPropertyValue.changesToJSON(jsonWriter3, allowBrowserConverterContext);

		assertTrue(new JSONObject(
			"{\"upd_viewPort\":{\"startIndex\":0,\"size\":2,\"upd_rows\":[{\"rows\":[{\"_svyRowId\":\"1.1;\",\"lastname\":\"value2\",\"firstname\":\"value1\"}],\"startIndex\":0,\"endIndex\":0,\"type\":1}]}}")
				.similar(
					new JSONObject(stringWriter3.toString())));

		viewPort.loadExtraRecords(-1);

		stringWriter3 = new StringWriter();
		jsonWriter3 = new JSONWriter(stringWriter3);
		rawPropertyValue.changesToJSON(jsonWriter3, allowBrowserConverterContext);

		assertEquals(new JSONObject("{\"n\":true}").toString(), new JSONObject(stringWriter3.toString()).toString());

		viewPort.loadExtraRecords(16);

		stringWriter3 = new StringWriter();
		jsonWriter3 = new JSONWriter(stringWriter3);
		rawPropertyValue.changesToJSON(jsonWriter3, allowBrowserConverterContext);

		assertTrue(new JSONObject(
			"{\"upd_viewPort\":{\"startIndex\":0,\"size\":18,\"upd_rows\":[{\"rows\":[{\"_svyRowId\":\"1.3;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.4;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.5;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.6;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.7;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"1.8;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"1.9;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.10;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.11;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.12;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.13;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.14;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.15;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.16;\",\"lastname\":\"value4\",\"firstname\":\"value3\"},{\"_svyRowId\":\"2.17;\",\"lastname\":\"value2\",\"firstname\":\"value1\"},{\"_svyRowId\":\"2.18;\",\"lastname\":\"value4\",\"firstname\":\"value3\"}],\"startIndex\":2,\"endIndex\":17,\"type\":1}]}}")
				.similar(
					new JSONObject(stringWriter3.toString())));

		viewPort.loadExtraRecords(1);

		stringWriter3 = new StringWriter();
		jsonWriter3 = new JSONWriter(stringWriter3);
		rawPropertyValue.changesToJSON(jsonWriter3, allowBrowserConverterContext);

		assertEquals("{\"n\":true}", stringWriter3.toString());
	}

	@Test
	public void foundsetWithInitialServerSizePageSize()
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("testSel8");
		assertNotNull(form);

		FoundsetTypeSabloValue foundSetPropValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycomponentwithpaging")
			.getRawPropertyValue("myfoundset");

		assertEquals(7, foundSetPropValue.getViewPort().getStartIndex()); // it does not center on selected record which is 8 (setupData()), because although spec says so, the "pageSize" has prio and it is applied to enter paging mode for selected page (page that has record 8 is the second page)
		assertEquals(7, foundSetPropValue.getViewPort().getSize()); // see that initially the viewport size is "7" according to "pageSize" property
	}

	@Test
	public void foundsetWithInitialServerSizePageSizeLessThen1()
	{
		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("testSel8");
		assertNotNull(form);

		FoundsetTypeSabloValue foundSetPropValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycomponentwithpagingLT1_1")
			.getRawPropertyValue("myfoundset");

		// see that initially the viewport size is the spec defined 12 according to "pageSize" = 0 that should not do anything
		assertEquals(12, foundSetPropValue.getViewPort().getSize());
		assertEquals(2, foundSetPropValue.getViewPort().getStartIndex()); // it centers on selected record which is 8 (setupData()), because spec says so and the "pageSize" is not applied to enter paging mode for selected page

		foundSetPropValue = (FoundsetTypeSabloValue)form.getFormUI().getWebComponent("mycomponentwithpagingLT1_2")
			.getRawPropertyValue("myfoundset");

		// see that initially the viewport size is the spec defined 12 according to "pageSize" = -1 that should not do anything
		assertEquals(12, foundSetPropValue.getViewPort().getSize());
		assertEquals(2, foundSetPropValue.getViewPort().getStartIndex()); // it centers on selected record which is 8 (setupData()), because spec says so and the "pageSize" is not applied to enter paging mode for selected page
	}

}
