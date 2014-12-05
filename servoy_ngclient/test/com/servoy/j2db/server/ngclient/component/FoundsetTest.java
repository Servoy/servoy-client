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
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.Container;
import org.sablo.InMemPackageReader;
import org.sablo.websocket.WebsocketEndpoint;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
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
		String comp1 = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("FoundSetTest-mydynamiccomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp2 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp1);
		components.put("mydynamiccomponent.spec", comp2);
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

		Bean bean1 = form.createNewBean("mydynamiccustombean", "my-dynamiccomponent");
		bean1.setInnerHTML("{myfoundset:{foundsetSelector:'test_to_relatedtest', dataproviders:{dp1:'relatedtest1',dp2:'relatedtest2'}}}");
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

		BufferedDataSet relatedDS = new BufferedDataSet(new String[] { "relatedtestpk", "testpk", "relatedtest1", "relatedtest2" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		relatedDS.addRow(new Object[] { Integer.valueOf(1), Integer.valueOf(1), "relatedvalue111", "relatedvalue112" });
		relatedDS.addRow(new Object[] { Integer.valueOf(2), Integer.valueOf(1), "relatedvalue121", "relatedvalue122" });
		relatedDS.addRow(new Object[] { Integer.valueOf(3), Integer.valueOf(1), "relatedvalue131", "relatedvalue132" });
		relatedDS.addRow(new Object[] { Integer.valueOf(4), Integer.valueOf(2), "relatedvalue241", "relatedvalue242" });
		client.getFoundSetManager().createDataSourceFromDataSet("relatedtest", relatedDS, null, new String[] { "relatedtestpk" });

		HashMap<String, IServer> serverProxies = new HashMap<String, IServer>();
		serverProxies.put("_sv_inmem", new IServer()
		{

			@Override
			public ITable getTable(String tableName) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ITable getTableBySqlname(String tableSQLName) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getTableAndViewNames(boolean hideTemporary) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getTableNames(boolean hideTempTables) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, ITable> getInitializedTables() throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getViewNames(boolean hideTempViews) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getTableType(String tableName) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getName() throws RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isValid() throws RemoteException
			{
				return true;
			}

			@Override
			public String getDatabaseProductName() throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getQuotedIdentifier(String tableSqlName, String columnSqlName) throws RepositoryException, RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getDataModelClonesFrom() throws RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

		});
		solution.setServerProxies(serverProxies);

		Relation relation = solution.createNewRelation(validator, "test_to_relatedtest", "mem:test", "mem:relatedtest", ISQLJoin.LEFT_OUTER_JOIN);
		Column primaryColumn = ((Table)client.getFoundSetManager().getTable(relation.getPrimaryDataSource())).getColumn("pk");
		Column foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("testpk");
		relation.createNewRelationItem(client.getFoundSetManager(), primaryColumn, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);
	}

	@Test
	public void foundsetReadByDataproviders() throws JSONException
	{
		WebsocketEndpoint endpoint = (WebsocketEndpoint)WebsocketEndpoint.get();

		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");

		String full = NGUtils.formComponentPropetiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);

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

		String changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);

		object = new JSONObject(changes).getJSONObject("changes");
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

	@Test
	public void foundsetWithDynamicDataproviders() throws JSONException
	{
		WebsocketEndpoint endpoint = (WebsocketEndpoint)WebsocketEndpoint.get();

		IWebFormController form = (IWebFormController)client.getFormManager().showFormInCurrentContainer("test");
		String full = NGUtils.formComponentPropetiesToString(form.getFormUI(), FullValueToJSONConverter.INSTANCE);
		JSONObject object = new JSONObject(full);
		JSONObject bean = object.getJSONObject("mydynamiccustombean");
		JSONObject foundset = bean.getJSONObject("myfoundset");
		Assert.assertEquals(3, foundset.getInt("serverSize"));
		JSONObject viewPort = foundset.getJSONObject("viewPort");
		Assert.assertEquals(0, viewPort.getInt("startIndex"));
		Assert.assertEquals(0, viewPort.getInt("size"));
		Assert.assertEquals(0, viewPort.getJSONArray("rows").length());

		// fake incomming request for view port change.
		endpoint.incoming(
			"{\"methodname\":\"dataPush\",\"args\":{\"beanname\":\"mydynamiccustombean\",\"formname\":\"test\",\"changes\":{\"myfoundset\":[{\"newViewPort\":{\"startIndex\":0,\"size\":3}}]}},\"service\":\"formService\"}",
			true);

		String changes = NGUtils.formChangesToString(((Container)form.getFormUI()), FullValueToJSONConverter.INSTANCE);
		object = new JSONObject(changes).getJSONObject("changes");
		bean = object.getJSONObject("mydynamiccustombean");
		foundset = bean.getJSONObject("myfoundset");
		Assert.assertEquals(3, foundset.getInt("serverSize"));
		viewPort = foundset.getJSONObject("viewPort");
		Assert.assertEquals(0, viewPort.getInt("startIndex"));
		Assert.assertEquals(3, viewPort.getInt("size"));
		JSONArray rows = viewPort.getJSONArray("rows");
		Assert.assertEquals(3, rows.length());

		JSONObject row0 = rows.getJSONObject(0);
		Assert.assertEquals("relatedvalue111", row0.getString("dp1"));
		Assert.assertEquals("relatedvalue112", row0.getString("dp2"));

		JSONObject row1 = rows.getJSONObject(1);
		Assert.assertEquals("relatedvalue121", row1.getString("dp1"));
		Assert.assertEquals("relatedvalue122", row1.getString("dp2"));
	}
}
