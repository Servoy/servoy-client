/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sablo.WebComponent;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.websocket.IForJsonConverter;
import org.sablo.websocket.IService;
import org.sablo.websocket.IWebsocketEndpoint;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DataConverterContext;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IClientManager;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class PersistFieldInstanceTest
{
	IValidateName validator = new IValidateName()
	{
		@Override
		public void checkName(String nameToCheck, int skip_element_id, ValidatorSearchContext searchContext, boolean sqlRelated) throws RepositoryException
		{
		}
	};

	private Solution solution;
	private NGClient client;

	@Before
	public void buildSolution() throws Exception
	{
		Types.registerTypes();

		File[] locations = new File[2];
		final File f = new File(PersistFieldInstanceTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		locations[0] = new File(f.getAbsoluteFile() + "/../war/servoydefault/"); //in eclipse we .. out of bin, in jenkins we .. out of @dot
		locations[1] = new File(f.getAbsoluteFile() + "/../war/servoycomponents/");
		new WebComponentSpecProvider(locations);

		final TestRepository tr = new TestRepository();
		try
		{


			UUID uuid = UUID.randomUUID();
			final RootObjectMetaData metadata = tr.createRootObjectMetaData(tr.getElementIdForUUID(uuid), uuid, "Test", IRepository.SOLUTIONS, 1, 1);

			solution = (Solution)tr.createRootObject(metadata);
			solution.setChangeHandler(new ChangeHandler(tr));
			solution.createNewForm(validator, null, "test", null, false, new Dimension(600, 400));
			ValueList valuelist = solution.createNewValueList(validator, "test");
			valuelist.setValueListType(IValueListConstants.CUSTOM_VALUES);

			client = new NGClient(new INGClientWebsocketSession()
			{

				@Override
				public void valueChanged()
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void registerService(String name, IService service)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void registerEndpoint(IWebsocketEndpoint endpoint)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void onOpen(String argument)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public boolean isValid()
				{
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public Object invokeApi(WebComponent receiver, WebComponentApiDefinition apiFunction, Object[] arguments)
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void handleMessage(JSONObject obj)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public String getUuid()
				{
					return null;
				}

				@Override
				public IService getService(String name)
				{
					return null;
				}

				@Override
				public List<IWebsocketEndpoint> getRegisteredEnpoints()
				{
					// TODO Auto-generated method stub
					return Collections.emptyList();
				}

				@Override
				public IForJsonConverter getForJsonConverter()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public IEventDispatcher getEventDispatcher()
				{
					return new IEventDispatcher()
					{

						@Override
						public void run()
						{
						}

						@Override
						public void suspend(Object object)
						{
						}

						@Override
						public void resume(Object object)
						{
						}

						@Override
						public boolean isEventDispatchThread()
						{
							return true;
						}

						@Override
						public void destroy()
						{
						}

						@Override
						public void addEvent(Runnable event)
						{
							event.run();
						}
					};
				}

				@Override
				public Object executeServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void deregisterEndpoint(IWebsocketEndpoint endpoint)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void updateForm(Form form, String name)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void touchForm(Form flattenedForm, String realInstanceName, boolean async)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void stopHandlingEvent()
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void startHandlingEvent()
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void solutionLoaded(Solution flattenedSolution)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void sendChanges(Map<String, Map<String, Map<String, Object>>> formData) throws IOException
				{
					// TODO Auto-generated method stub

				}

				@Override
				public INGApplication getClient()
				{
					return client;
				}

				@Override
				public void formCreated(String formName)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void closeSession()
				{
					// TODO Auto-generated method stub

				}
			})
			{

				/*
				 * (non-Javadoc)
				 *
				 * @see com.servoy.j2db.ClientState#createRepository()
				 */
				@Override
				protected IRepository createRepository() throws RemoteException
				{
					return tr;
				}

				@Override
				protected boolean startApplicationServerConnection()
				{
					applicationServer = new IApplicationServer()
					{

						@Override
						public ClientLogin login(Credentials credentials) throws RemoteException, RepositoryException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public SolutionMetaData[] getSolutionDefinitions(int solutionTypeFilter) throws RemoteException, RepositoryException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public SolutionMetaData getSolutionDefinition(String solutionName, int solutionTypeFilter) throws RemoteException, RepositoryException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public Remote getRemoteService(String cid, String rmiLookupName) throws RemoteException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public SolutionMetaData[] getLoginSolutionDefinitions(SolutionMetaData solutionMetaData) throws RemoteException, RepositoryException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public Solution getLoginSolution(SolutionMetaData mainSolution, SolutionMetaData loginSolution) throws RemoteException,
							RepositoryException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public String getClientID(String user_uid, String password) throws RemoteException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public IClientHost getClientHost() throws RemoteException
						{

							System.out.println("return le client host now");
							return new IClientHost()
							{

								@Override
								public void unregister(String client_id) throws RemoteException
								{
									// TODO Auto-generated method stub

								}

								@Override
								public Object[] register(IUserClient c, ClientInfo clientInfo) throws RemoteException
								{
									// TODO Auto-generated method stub
									return new Object[] { "uuid", new Integer(IClientManager.REGISTER_OK) };
								}

								@Override
								public void pushClientInfo(String clientId, ClientInfo clientInfo) throws RemoteException
								{
									// TODO Auto-generated method stub

								}

								@Override
								public Date getServerTime(String client_id) throws RemoteException
								{
									// TODO Auto-generated method stub
									return null;
								}
							};
						}

						@Override
						public IApplicationServerAccess getApplicationServerAccess(String clientId) throws RemoteException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException
						{
							// TODO Auto-generated method stub
							return null;
						}
					};
					return true;
				}

				@Override
				protected void createPluginManager()
				{
				}
			};
			client.setUseLoginSolution(false);
			client.loadSolutionsAndModules((SolutionMetaData)metadata);
		}
		catch (RepositoryException e)
		{
			e.printStackTrace();
		}
	}

	@Test
	public void testFieldWithValueList() throws RepositoryException
	{
		Form form = solution.getForm("test");
		Assert.assertNotNull(form);
		ValueList vl = solution.getValueList("test");
		Assert.assertNotNull(vl);

		Field field = form.createNewField(new Point(0, 0));
		field.setDataProviderID("mycolumn");
		field.setFormat("#,###.00");
		field.setDisplayType(Field.TYPE_AHEAD);
		field.setValuelistID(vl.getID());

		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new DataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		for (FormElement formElement : formElements)
		{
			WebFormComponent wc = ComponentFactory.createComponent(client, null, formElement, null);
			Object property = wc.getProperty("valuelistID");
			Assert.assertTrue(property != null ? property.getClass().getName() : "null", property instanceof CustomValueList);
			Assert.assertEquals("#,###.00", ((CustomValueList)property).getFormat().getDisplayFormat());

		}
	}

	@Test
	public void testTabPanelWithTabs() throws RepositoryException
	{
		Form form = solution.getForm("test");
		Assert.assertNotNull(form);

		Form tabForm = solution.createNewForm(validator, null, "tabform", null, false, new Dimension(600, 400));

		TabPanel tabpanel = form.createNewTabPanel("tabpanel");
		tabpanel.createNewTab("tab1", null, tabForm);
		tabpanel.createNewTab("tab2", null, tabForm);

		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), new DataConverterContext(client));
		Assert.assertEquals(1, formElements.size());
		for (FormElement formElement : formElements)
		{
			WebFormComponent wc = ComponentFactory.createComponent(client, null, formElement, null);
			List<Map<String, Object>> tabs = (List)wc.getConvertedPropertyWithDefault("tabs", false, true);
			Assert.assertEquals(2, tabs.size());
			Map<String, Object> map = tabs.get(1);
			Assert.assertSame(tabForm.getName(), map.get("containsFormId"));
		}
	}

	public static class TestRepository extends AbstractRepository
	{
		private final IntHashMap<UUID> intToUUID = new IntHashMap<>();
		private final HashMap<UUID, Integer> uuidToInt = new HashMap<>();
		private int elementIdCounter = 1;
		private RootObjectMetaData createdMetaData;
		private IRootObject rootObject;

		/**
		 * @param serverManager
		 */
		protected TestRepository()
		{
			super(null);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.persistence.AbstractRepository#createRootObjectMetaData(int, com.servoy.j2db.util.UUID, java.lang.String, int, int, int)
		 */
		@Override
		public RootObjectMetaData createRootObjectMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease,
			int latestRelease)
		{
			createdMetaData = super.createRootObjectMetaData(rootObjectId, rootObjectUuid, name, objectTypeId, activeRelease, latestRelease);
			return createdMetaData;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.persistence.AbstractRepository#createRootObject(com.servoy.j2db.persistence.RootObjectMetaData)
		 */
		@Override
		public IRootObject createRootObject(RootObjectMetaData metaData) throws RepositoryException
		{
			rootObject = super.createRootObject(metaData);
			return rootObject;
		}

		@Override
		public IColumnInfoManager getColumnInfoManager()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setUserProperties(int systemUserId, Map props) throws RepositoryException
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void setRootObjectActiveRelease(int rootObjectId, int releaseNumber) throws RepositoryException
		{
			// TODO Auto-generated method stub

		}

		@Override
		public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public byte[] getMediaBlob(int blob_id) throws RemoteException, RepositoryException
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getNewElementID(UUID new_uuid) throws RepositoryException
		{
			int id = elementIdCounter++;
			intToUUID.put(id, new_uuid);
			uuidToInt.put(new_uuid, Integer.valueOf(id));
			return id;
		}

		@Override
		public Properties getUserProperties(int user_id) throws RepositoryException
		{
			return null;
		}

		@Override
		protected Collection<RootObjectMetaData> loadRootObjectMetaDatas() throws Exception
		{
			return Arrays.asList(createdMetaData);
		}

		@Override
		protected IRootObject loadRootObject(RootObjectMetaData romd, int releaseNumber) throws RepositoryException
		{
			return rootObject;
		}

		@Override
		public IRootObject createNewRootObject(String name, int objectTypeId, int newElementID, UUID uuid) throws RepositoryException
		{
			return createRootObject(createRootObjectMetaData(newElementID, uuid, name, objectTypeId, 1, 1));
		}

		@Override
		public void restoreObjectToCurrentRelease(IPersist persist) throws RepositoryException
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void updateRootObject(IRootObject rootObject) throws RepositoryException
		{
			// TODO Auto-generated method stub

		}


		@Override
		public int resolveIdForElementUuid(UUID id) throws RepositoryException
		{
			Integer integer = uuidToInt.get(id);
			if (integer != null) return integer.intValue();
			return 0;
		}

		@Override
		protected ContentSpec loadContentSpec() throws RepositoryException
		{
			return StaticContentSpecLoader.getContentSpec();
		}

		@Override
		public UUID resolveUUIDForElementId(int id) throws RepositoryException
		{
			return intToUUID.get(id);
		}

	}
}
