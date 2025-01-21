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

package com.servoy.j2db.server.ngclient;

import static com.servoy.base.query.IQueryConstants.LEFT_OUTER_JOIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.Container;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.SwingRelatedFoundSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.property.AbstractSolutionTest;
import com.servoy.j2db.server.ngclient.property.ITestFoundset;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class DataAdapterListTest extends AbstractSolutionTest
{

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("DataAdapterListTest.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("DataAdapterListTest-tabpanel.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp1 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("tabpanel.spec", comp1);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		// form 1
		Form mainForm = solution.createNewForm(validator, null, "mainForm1", "mem:mainTable", false, new Dimension(600, 400));
		mainForm.setNavigatorID(-1);
		mainForm.createNewPart(IBaseSMPart.BODY, 5);
		WebComponent mainTabPanel = mainForm.createNewWebComponent("mainTabPanel", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm1\", \"relationName\": \"main_to_related1\" } ]"));

		Field field = mainForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("mainQuantity");
		field.setDataProviderID("quantity");

		Form relatedForm = solution.createNewForm(validator, null, "relatedForm1", "mem:relatedTable1", false, new Dimension(600, 400));
		relatedForm.setNavigatorID(-1);
		relatedForm.createNewPart(IBaseSMPart.BODY, 5);

		field = relatedForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("related1City");
		field.setDataProviderID("city");

		// form 2; 2 tabpanels, one with rel1 and one with rel1torel2
		mainForm = solution.createNewForm(validator, null, "mainForm2", "mem:mainTable", false, new Dimension(600, 400));
		mainForm.setNavigatorID(-1);
		mainForm.createNewPart(IBaseSMPart.BODY, 5);

		mainTabPanel = mainForm.createNewWebComponent("mainTabPanel1", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm1\", \"relationName\": \"main_to_related1\" } ]"));

		mainTabPanel = mainForm.createNewWebComponent("mainTabPanel2", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm2\", \"relationName\": \"main_to_related1.related1_to_related2\" } ]"));

		field = mainForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("mainQuantity");
		field.setDataProviderID("quantity");

		relatedForm = solution.createNewForm(validator, null, "relatedForm2", "mem:relatedTable2", false, new Dimension(600, 400));
		relatedForm.setNavigatorID(-1);
		relatedForm.createNewPart(IBaseSMPart.BODY, 5);

		field = relatedForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("related2ProductName");
		field.setDataProviderID("productName");

		// form 3; 1 tabpanel with rel1torel2; no visual form that uses only rel1
		mainForm = solution.createNewForm(validator, null, "mainForm3", "mem:mainTable", false, new Dimension(600, 400));
		mainForm.setNavigatorID(-1);
		mainForm.createNewPart(IBaseSMPart.BODY, 5);

		mainTabPanel = mainForm.createNewWebComponent("mainTabPanel", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm2\", \"relationName\": \"main_to_related1.related1_to_related2\" } ]"));

		field = mainForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("mainQuantity");
		field.setDataProviderID("quantity");

		// form 4; 2 tabpanels; 1 with globalrel1; one with mixedrel1 (both global and table primary keys)
		mainForm = solution.createNewForm(validator, null, "mainForm4", "mem:mainTable", false, new Dimension(600, 400));
		mainForm.setNavigatorID(-1);
		mainForm.createNewPart(IBaseSMPart.BODY, 5);

		mainTabPanel = mainForm.createNewWebComponent("mainTabPanel1", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm1\", \"relationName\": \"global_relation1\" } ]"));

		relatedForm = solution.createNewForm(validator, null, "relatedForm3", "mem:relatedTable1", false, new Dimension(600, 400));
		relatedForm.setNavigatorID(-1);
		relatedForm.createNewPart(IBaseSMPart.BODY, 5);

		field = relatedForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("related1City");
		field.setDataProviderID("city");

		mainTabPanel = mainForm.createNewWebComponent("mainTabPanel2", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm3\", \"relationName\": \"mixed_relation1\" } ]"));

		field = mainForm.createNewField(new Point(5, 5));
		field.setDisplayType(Field.TEXT_FIELD);
		field.setName("mainQuantity");
		field.setDataProviderID("quantity");

		// form 5; 1 tabpanels; 1 unrelated tab
		mainForm = solution.createNewForm(validator, null, "mainForm5", "mem:relatedTable2", false, new Dimension(600, 400));
		mainForm.setNavigatorID(-1);
		mainForm.createNewPart(IBaseSMPart.BODY, 5);

		mainTabPanel = mainForm.createNewWebComponent("mainTabPanel", "tabpanel");
		mainTabPanel.setProperty("tabs", new ServoyJSONArray(
			"[ { \"_id\": \"abc\", \"containedForm\": \"relatedForm1\" } ]"));
	}

	@Override
	protected void setupData() throws ServoyException
	{
		BufferedDataSet ds = new BufferedDataSet(new String[] { "orderid", "quantity" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.INTEGER });
		ds.addRow(new Object[] { Integer.valueOf(1), Integer.valueOf(1001) });
		ds.addRow(new Object[] { Integer.valueOf(2), Integer.valueOf(1002) });
		ds.addRow(new Object[] { Integer.valueOf(3), Integer.valueOf(1003) });
		client.getFoundSetManager().insertToDataSource("mainTable", ds, null, new WrappedObjectReference<String[]>(new String[] { "orderid" }), true, false);

		ds = new BufferedDataSet(new String[] { "detailsid", "orderid", "city", "productid" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		ds.addRow(new Object[] { Integer.valueOf(9001), Integer.valueOf(1), "City11", "p1" });
		ds.addRow(new Object[] { Integer.valueOf(9003), Integer.valueOf(1), "City12", "p2" });
		ds.addRow(new Object[] { Integer.valueOf(9004), Integer.valueOf(1), "City13", "p3" });

		ds.addRow(new Object[] { Integer.valueOf(9002), Integer.valueOf(153), "City153", "p4" });

		ds.addRow(new Object[] { Integer.valueOf(9005), Integer.valueOf(2), "City21", "p3" });
		ds.addRow(new Object[] { Integer.valueOf(9006), Integer.valueOf(2), "City22", "p1" });

		ds.addRow(new Object[] { Integer.valueOf(9007), Integer.valueOf(3), "City31", "p5" });
		client.getFoundSetManager().insertToDataSource("relatedTable1", ds, null, new WrappedObjectReference<String[]>(new String[] { "detailsid" }), true,
			false);

		ds = new BufferedDataSet(new String[] { "productName", "productid" },
			new int[] { IColumnTypes.TEXT, IColumnTypes.TEXT });
		ds.addRow(new Object[] { "Product 1", "p1" });
		ds.addRow(new Object[] { "Product 2", "p2" });
		ds.addRow(new Object[] { "Product 3", "p3" });
		ds.addRow(new Object[] { "Product 4", "p4" });
		ds.addRow(new Object[] { "Product 5", "p5" });
		client.getFoundSetManager().insertToDataSource("relatedTable2", ds, null, new WrappedObjectReference<String[]>(new String[] { "productid" }), true,
			false);

		ConcurrentHashMap<String, IServer> serverProxies = new ConcurrentHashMap<String, IServer>();
		serverProxies.put("_sv_inmem", DUMMY_ISERVER);
		solution.setServerProxies(serverProxies);

		// NOTE: here, in tests, that are using a fake perform query, make sure that
		// the foreign column in a relation is always listed as the second column in that datasource (test
		// code assumes that when computing related rows)
		Relation relation = solution.createNewRelation(validator, "main_to_related1", "mem:mainTable", "mem:relatedTable1", LEFT_OUTER_JOIN);
		Column primaryColumn = ((Table)client.getFoundSetManager().getTable(relation.getPrimaryDataSource())).getColumn("orderid");
		Column foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("orderid");
		relation.createNewRelationItem(client.getFoundSetManager(), primaryColumn, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);

		relation = solution.createNewRelation(validator, "related1_to_related2", "mem:relatedTable1", "mem:relatedTable2", LEFT_OUTER_JOIN);
		primaryColumn = ((Table)client.getFoundSetManager().getTable(relation.getPrimaryDataSource())).getColumn("productid");
		foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("productid");
		relation.createNewRelationItem(client.getFoundSetManager(), primaryColumn, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);

		ScriptVariable relGlobalScopeVarOrderID = solution.createNewScriptVariable(validator, "sc1", "relGlobalScopeVarOrderID", IColumnTypes.NUMBER);
		relGlobalScopeVarOrderID.setDefaultValue("1");
		relation = solution.createNewRelation(validator, "global_relation1", null, "mem:relatedTable1", LEFT_OUTER_JOIN);
		foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("orderid");
		relation.createNewRelationItem(client.getFoundSetManager(), relGlobalScopeVarOrderID, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);

		ScriptVariable relGlobalScopeVarProductID = solution.createNewScriptVariable(validator, "sc1", "relGlobalScopeVarProductID", IColumnTypes.TEXT);
		relGlobalScopeVarProductID.setDefaultValue("\"p2\"");
		relation = solution.createNewRelation(validator, "mixed_relation1", "mem:mainTable", "mem:relatedTable1", LEFT_OUTER_JOIN);
		primaryColumn = ((Table)client.getFoundSetManager().getTable(relation.getPrimaryDataSource())).getColumn("orderid");
		foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("orderid");
		relation.createNewRelationItem(client.getFoundSetManager(), primaryColumn, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);
		foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("productid");
		relation.createNewRelationItem(client.getFoundSetManager(), relGlobalScopeVarProductID, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);

		IFoundSetInternal foundset = client.getFoundSetManager().getSharedFoundSet("mem:mainTable");
		foundset.loadAllRecords();
		foundset.setSelectedIndex(0);

		foundset = client.getFoundSetManager().getSharedFoundSet("mem:relatedTable1");
		foundset.loadAllRecords();
		foundset.setSelectedIndex(0);

		foundset = client.getFoundSetManager().getSharedFoundSet("mem:relatedTable2");
		foundset.loadAllRecords();
		foundset.setSelectedIndex(0);
	}

	@Test
	public void foundset1TabPanelWithOneRelatedByColTab() throws JSONException
	{
		IWebFormController mainForm = (IWebFormController)client.getFormManager().showFormInCurrentContainer("mainForm1");
		assertNotNull(mainForm);

		// fake incoming request to show the related tab in the tabpanel

		//      service: "formService"
		//      methodname: "formvisibility"
		//		args:
		//		{
		//			formname: string,
		//			parentForm: string,
		//			bean: string,
		//			visible: boolean,
		//			relation: string,
		//			formIndex: int,
		//			show: { // in case visible is false above, then we can also show another form with the same request
		//				formname: string,
		//				relation: string,
		//				formIndex: int
		//			}
		//		}

		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm1\"," +
					"\"parentForm\":\"mainForm1\"," +
					"\"bean\":\"mainTabPanel\"," +
					"\"visible\":true," +
					"\"relation\":\"" + relationNamesAsSentToClient.get("main_to_related1").get("mainTabPanel") + "\"" +
					",\"formIndex\":1" +
				"}" +
			"}",
			true);
		// @formatter:on

		LinkedList<String> sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);

		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":1001");
		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":\\\"City11\\\"");

		IWebFormController relatedForm1 = client.getFormManager().getForm("relatedForm1");
		assertNotNull(relatedForm1);
		IFoundSetInternal relatedFoundset = relatedForm1.getFormModel();
		assertTrue(relatedFoundset instanceof SwingRelatedFoundSet);
		assertEquals(3, relatedFoundset.getSize());

		IFoundSetInternal mainFoundset = mainForm.getFormModel();

		// change data in related form; see that it is going to be sent to client
		mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1").getRecord(0).startEditing();
		mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1").getRecord(0).setValue("city", "City11*");
		saveData();

		String changesAsString = NGUtils.formChangesToString(((Container)relatedForm1.getFormUI()), FullValueToJSONConverter.INSTANCE);
		JSONObject changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("City11*", changes.getJSONObject("related1City").getString("dataProviderID"));

		// change main form selected record index
		mainFoundset.setSelectedIndex(1);

		changesAsString = NGUtils.formChangesToString(((Container)relatedForm1.getFormUI()), FullValueToJSONConverter.INSTANCE);
		changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("City21", changes.getJSONObject("related1City").getString("dataProviderID"));
	}

	@Test
	public void foundset2TabPanelsWithOneRel1AndRel1Rel2() throws JSONException
	{
		IWebFormController mainForm = (IWebFormController)client.getFormManager().showFormInCurrentContainer("mainForm2");
		assertNotNull(mainForm);

		// fake incoming request to show the related tab in the tabpanel

		//      service: "formService"
		//      methodname: "formvisibility"
		//		args:
		//		{
		//			formname: string,
		//			parentForm: string,
		//			bean: string,
		//			visible: boolean,
		//			relation: string,
		//			formIndex: int,
		//			show: { // in case visible is false above, then we can also show another form with the same request
		//				formname: string,
		//				relation: string,
		//				formIndex: int
		//			}
		//		}

		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm1\"," +
					"\"parentForm\":\"mainForm2\"," +
					"\"bean\":\"mainTabPanel1\"," +
					"\"visible\":true," +
					"\"relation\":\"" + relationNamesAsSentToClient.get("main_to_related1").get("mainTabPanel1") + "\"" +
					",\"formIndex\":1" +
				"}" +
			"}",
			true);
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm2\"," +
					"\"parentForm\":\"mainForm2\"," +
					"\"bean\":\"mainTabPanel2\"," +
					"\"visible\":true," +
					"\"relation\":\"" + relationNamesAsSentToClient.get("main_to_related1.related1_to_related2").get("mainTabPanel2") + "\"" +
					",\"formIndex\":1" +
				"}" +
			"}",
			true);
		// @formatter:on

		LinkedList<String> sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);

		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":1001");
		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":\\\"City11\\\"");
		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":\\\"Product 1\\\"");

		IWebFormController relatedForm1 = client.getFormManager().getForm("relatedForm1");
		IWebFormController relatedForm2 = client.getFormManager().getForm("relatedForm2");
		assertNotNull(relatedForm1);
		assertNotNull(relatedForm2);

		IFoundSetInternal relatedFoundset1 = relatedForm1.getFormModel();
		assertTrue(relatedFoundset1 instanceof SwingRelatedFoundSet);
		assertEquals(3, relatedFoundset1.getSize());

		IFoundSetInternal relatedFoundset2 = relatedForm2.getFormModel();
		assertTrue(relatedFoundset2 instanceof SwingRelatedFoundSet);
		assertEquals(1, relatedFoundset2.getSize());

		IFoundSetInternal mainFoundset = mainForm.getFormModel();

		// change data in relation2 primary column in related1 form; see that it is going to change the content in relatedForm2
		mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1").getRecord(0).startEditing();
		mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1").getRecord(0).setValue("productid", "p4");
		saveData();

		String changesAsString = NGUtils.formChangesToString(((Container)relatedForm2.getFormUI()), FullValueToJSONConverter.INSTANCE);
		JSONObject changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("Product 4", changes.getJSONObject("related2ProductName").getString("dataProviderID"));

		// change main form selected record index
		mainFoundset.setSelectedIndex(1);

		changesAsString = NGUtils.formChangesToString(((Container)relatedForm1.getFormUI()), FullValueToJSONConverter.INSTANCE);
		changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("City21", changes.getJSONObject("related1City").getString("dataProviderID"));
		changesAsString = NGUtils.formChangesToString(((Container)relatedForm2.getFormUI()), FullValueToJSONConverter.INSTANCE);
		changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("Product 3", changes.getJSONObject("related2ProductName").getString("dataProviderID"));
	}

	@SuppressWarnings("boxing")
	@Test
	public void foundset1TabPanelWithRel1Rel2() throws JSONException
	{
		IWebFormController mainForm = (IWebFormController)client.getFormManager().showFormInCurrentContainer("mainForm3");
		assertNotNull(mainForm);
		IFoundSetInternal mainFoundset = mainForm.getFormModel();

		int initialSelectionListenersOnMainFoundset = ((ISwingFoundSet)mainFoundset).getSelectionModel().getListSelectionListeners().length;
		int initialFoundsetEvenListenersOnMainFoundset = ((ITestFoundset)mainFoundset).getNumberOfFoundsetEventListeners();

		// fake incoming request to show the related tab in the tabpanel

		//      service: "formService"
		//      methodname: "formvisibility"
		//		args:
		//		{
		//			formname: string,
		//			parentForm: string,
		//			bean: string,
		//			visible: boolean,
		//			relation: string,
		//			formIndex: int,
		//			show: { // in case visible is false above, then we can also show another form with the same request
		//				formname: string,
		//				relation: string,
		//				formIndex: int
		//			}
		//		}

		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm2\"," +
					"\"parentForm\":\"mainForm3\"," +
					"\"bean\":\"mainTabPanel\"," +
					"\"visible\":true," +
					"\"relation\":\"" + relationNamesAsSentToClient.get("main_to_related1.related1_to_related2").get("mainTabPanel") + "\"" +
					",\"formIndex\":1" +
				"}" +
			"}",
			true);
		// @formatter:on

		// see that it added listeners when the related form was shown

		// this is commented out as for the main form's foundset, DAL does not add a foundset selection listener; it relies on the DAL.setRecord for that; only intermediate foundsets in case of multiple levels of relations do get new selection listeners from DAL code
		// assertThat(((ISwingFoundSet)mainFoundset).getSelectionModel().getListSelectionListeners().length, greaterThan(initialSelectionListenersOnMainFoundset));
		assertThat(((ITestFoundset)mainFoundset).getNumberOfFoundsetEventListeners(), greaterThan(initialFoundsetEvenListenersOnMainFoundset));

		IFoundSetInternal relFoundset1 = mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1");
		assertThat(((ISwingFoundSet)relFoundset1).getSelectionModel().getListSelectionListeners().length, greaterThan(0));
		// below we use greater then 1 because it will always have the GlobalFoundSetEventListener registered as a listener...
		assertThat(((ITestFoundset)relFoundset1).getNumberOfFoundsetEventListeners(), greaterThan(1));

		LinkedList<String> sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);

		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":1001");
		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":\\\"Product 1\\\"");

		IWebFormController relatedForm2 = client.getFormManager().getForm("relatedForm2");
		assertNotNull(relatedForm2);

		IFoundSetInternal relatedFoundset2 = relatedForm2.getFormModel();
		assertTrue(relatedFoundset2 instanceof SwingRelatedFoundSet);
		assertEquals(1, relatedFoundset2.getSize());

		// change data in relation2 primary column in related1 form; see that it is going to change the content in relatedForm2
		mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1").getRecord(0).startEditing();
		mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1").getRecord(0).setValue("productid", "p4");
		saveData();

		String changesAsString = NGUtils.formChangesToString(((Container)relatedForm2.getFormUI()), FullValueToJSONConverter.INSTANCE);
		JSONObject changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("Product 4", changes.getJSONObject("related2ProductName").getString("dataProviderID"));

		// change main form selected record index
		mainFoundset.setSelectedIndex(1);

		changesAsString = NGUtils.formChangesToString(((Container)relatedForm2.getFormUI()), FullValueToJSONConverter.INSTANCE);
		changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("Product 3", changes.getJSONObject("related2ProductName").getString("dataProviderID"));

		// fake incoming request to hide the related tab in the tabpanel
		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm2\"," +
					"\"parentForm\":\"mainForm3\"," +
					"\"bean\":\"mainTabPanel\"," +
					"\"visible\":false," +
				"}" +
			"}",
			true);
		// @formatter:on

		// see that it removed listeners that were added when the related form was hidden
		assertEquals(initialSelectionListenersOnMainFoundset, ((ISwingFoundSet)mainFoundset).getSelectionModel().getListSelectionListeners().length);
		assertEquals(initialFoundsetEvenListenersOnMainFoundset, ((ITestFoundset)mainFoundset).getNumberOfFoundsetEventListeners());

		relFoundset1 = mainFoundset.getRecord(0).getRelatedFoundSet("main_to_related1");
		assertEquals(0, ((ISwingFoundSet)relFoundset1).getSelectionModel().getListSelectionListeners().length);
		// below we use equal to 1 because it will always have the GlobalFoundSetEventListener registered as a listener...
		assertEquals(1, ((ITestFoundset)relFoundset1).getNumberOfFoundsetEventListeners());
	}

	@Test
	public void foundset2TabPanelsOneWithGlobalRelOneWithMixedRel() throws JSONException
	{
		IWebFormController mainForm = (IWebFormController)client.getFormManager().showFormInCurrentContainer("mainForm4");
		assertNotNull(mainForm);

		assertFalse(client.getScriptEngine().getScopesScope().getModificationSubject().hasListeners());

		// fake incoming request to show the related tab in the tabpanel

		//      service: "formService"
		//      methodname: "formvisibility"
		//		args:
		//		{
		//			formname: string,
		//			parentForm: string,
		//			bean: string,
		//			visible: boolean,
		//			relation: string,
		//			formIndex: int,
		//			show: { // in case visible is false above, then we can also show another form with the same request
		//				formname: string,
		//				relation: string,
		//				formIndex: int
		//			}
		//		}

		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm1\"," +
					"\"parentForm\":\"mainForm4\"," +
					"\"bean\":\"mainTabPanel1\"," +
					"\"visible\":true," +
					"\"relation\":\"" + relationNamesAsSentToClient.get("global_relation1").get("mainTabPanel1") + "\"" +
					",\"formIndex\":1" +
				"}" +
			"}",
			true);
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm3\"," +
					"\"parentForm\":\"mainForm4\"," +
					"\"bean\":\"mainTabPanel2\"," +
					"\"visible\":true," +
					"\"relation\":\"" + relationNamesAsSentToClient.get("mixed_relation1").get("mainTabPanel2") + "\"" +
					",\"formIndex\":1" +
				"}" +
			"}",
			true);
		// @formatter:on

		assertTrue(client.getScriptEngine().getScopesScope().getModificationSubject().hasListeners());

		LinkedList<String> sentTextMessages = endpoint.getSession().getBasicRemote().getAndClearSentTextMessages();
		Assert.assertTrue(sentTextMessages.size() > 0);

		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":1001");
		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":\\\"City11\\\"");
		assertMessagesContain(sentTextMessages, "\\\"dataProviderID\\\":\\\"City12\\\"");

		IWebFormController relatedForm1 = client.getFormManager().getForm("relatedForm1");
		assertNotNull(relatedForm1);

		IFoundSetInternal relatedFoundset1 = relatedForm1.getFormModel();
		assertTrue(relatedFoundset1 instanceof SwingRelatedFoundSet);
		assertEquals(3, relatedFoundset1.getSize());

		IWebFormController relatedForm3 = client.getFormManager().getForm("relatedForm3");
		assertNotNull(relatedForm3);

		IFoundSetInternal relatedFoundset3 = relatedForm3.getFormModel();
		assertTrue(relatedFoundset3 instanceof SwingRelatedFoundSet);
		assertEquals(1, relatedFoundset3.getSize());

		IFoundSetInternal mainFoundset = mainForm.getFormModel();

		client.getScriptEngine().getScopesScope().getGlobalScope("sc1").put("relGlobalScopeVarOrderID", Integer.valueOf(2));

		String changesAsString = NGUtils.formChangesToString(((Container)relatedForm1.getFormUI()), FullValueToJSONConverter.INSTANCE);
		JSONObject changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("City21", changes.getJSONObject("related1City").getString("dataProviderID"));

		client.getScriptEngine().getScopesScope().getGlobalScope("sc1").put("relGlobalScopeVarProductID", "p3");

		changesAsString = NGUtils.formChangesToString(((Container)relatedForm3.getFormUI()), FullValueToJSONConverter.INSTANCE);
		changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("City13", changes.getJSONObject("related1City").getString("dataProviderID"));

		// change main form selected record index
		mainFoundset.setSelectedIndex(1);

		changesAsString = NGUtils.formChangesToString(((Container)relatedForm3.getFormUI()), FullValueToJSONConverter.INSTANCE);
		changes = new JSONObject(changesAsString).getJSONObject("changes");
		assertEquals("City21", changes.getJSONObject("related1City").getString("dataProviderID"));

		// simulate the hide of tabs in tabpanels
		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm1\"," +
					"\"parentForm\":\"mainForm4\"," +
					"\"bean\":\"mainTabPanel1\"," +
					"\"visible\":false," +
				"}" +
			"}",
			true);
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm3\"," +
					"\"parentForm\":\"mainForm4\"," +
					"\"bean\":\"mainTabPanel2\"," +
					"\"visible\":false," +
				"}" +
			"}",
			true);
		// @formatter:on

		// currently the main DAL is not smart enough to unregister the getScopesScope().getModificationSubject()
		// because that can be added also by variables used by the main form directly etc.
		// and it is only unregistered when the DAL/main form is destroyed; this could be improved in the future
		// but currently we need to hide main form and destroy it before checking that this listener is gone
		client.getFormManager().showFormInCurrentContainer("relatedForm2");
		client.getFormManager().destroyFormInstance("mainForm4");

		assertFalse(client.getScriptEngine().getScopesScope().getModificationSubject().hasListeners());

		// when showing relatedForm1 as main form, it should keep it's foundset
		relatedForm1 = client.getFormManager().getForm("relatedForm1");
		assertNotNull(relatedForm1);

		relatedFoundset1 = relatedForm1.getFormModel();
		assertTrue(relatedFoundset1 instanceof SwingRelatedFoundSet);
		assertEquals(2, relatedFoundset1.getSize());

		// when showing relatedForm1 as and un-related tab, it should keep it's foundset
		client.getFormManager().showFormInCurrentContainer("mainForm5");

		// @formatter:off
		endpoint.incoming(
			"{" +
				"\"service\":\"formService\"," +
				"\"methodname\":\"formvisibility\"," +
				"\"args\":{" +
					"\"formname\":\"relatedForm1\"," +
					"\"parentForm\":\"mainForm5\"," +
					"\"bean\":\"mainTabPanel\"," +
					"\"visible\":true," +
					"\"formIndex\":1" +
				"}" +
			"}",
			true);
		// @formatter:on

		relatedForm1 = client.getFormManager().getForm("relatedForm1");
		assertNotNull(relatedForm1);

		relatedFoundset1 = relatedForm1.getFormModel();
		assertTrue(relatedFoundset1 instanceof SwingRelatedFoundSet);
		assertEquals(2, relatedFoundset1.getSize());
	}

	private void saveData()
	{
		try
		{
			new JSDatabaseManager(client).saveData();
		}
		catch (ServoyException e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void assertMessagesContain(LinkedList<String> sentTextMessages, String containedString)
	{
		assertTrue(sentTextMessages.stream().anyMatch(
			(msg) -> msg.contains(containedString)));
	}

}
