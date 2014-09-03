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

package com.servoy.j2db.server.ngclient.startup;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.design.DesignNGClient;
import com.servoy.j2db.server.ngclient.design.DesignNGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.design.IDesignerSolutionProvider;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 */
public class Activator implements BundleActivator
{
	private final class DeveloperDesignClient extends DesignNGClient implements IPersistChangeListener
	{
		private final IDesignerSolutionProvider solutionProvider;

		/**
		 * @param wsSession
		 * @param solutionProvider
		 */
		private DeveloperDesignClient(INGClientWebsocketSession wsSession, IDesignerSolutionProvider solutionProvider) throws Exception
		{
			super(wsSession);
			this.solutionProvider = solutionProvider;
			solutionProvider.addPersistListener(this);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.NGClient#shutDown(boolean)
		 */
		@Override
		public synchronized void shutDown(boolean force)
		{
			super.shutDown(force);
			solutionProvider.removePersistListener(this);
		}

		@Override
		protected IActiveSolutionHandler createActiveSolutionHandler()
		{
			return new AbstractActiveSolutionHandler(getApplicationServer())
			{

				@Override
				protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
				{
					return solutionProvider.getEditingSolution(solutionDef.getName());
				}

				@Override
				public IRepository getRepository()
				{
					try
					{
						return getApplicationServerAccess().getRepository();
					}
					catch (RemoteException e)
					{
						Debug.error(e);
					}
					return null;
				}
			};
		}

		@Override
		public void loadSolution(String solutionName) throws com.servoy.j2db.persistence.RepositoryException
		{
			SolutionMetaData metaData = (SolutionMetaData)solutionProvider.getActiveEditingSolution().getMetaData();
			if (getSolution() == null || !getSolution().getName().equals(metaData.getName()))
			{
				loadSolution(metaData);
			}
			// fake this so that it seems to be in solution model node.
			solutionRoot.getSolutionCopy(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.persistence.IPersistChangeListener#persistChanges(java.util.Collection)
		 */
		@Override
		public void persistChanges(Collection<IPersist> changes)
		{
			final Map<Form, List<IFormElement>> frms = new HashMap<>();
			for (IPersist persist : changes)
			{
				if (persist instanceof IFormElement)
				{
					IPersist parent = persist;
					while (parent != null)
					{
						if (parent instanceof Form)
						{
							List<IFormElement> list = frms.get(persist);
							if (list == null)
							{
								list = new ArrayList<>();
								frms.put((Form)parent, list);
							}
							list.add((IFormElement)persist);
							break;
						}
						parent = parent.getParent();
					}
				}
			}
			if (frms.size() > 0)
			{
				getWebsocketSession().getEventDispatcher().addEvent(new Runnable()
				{
					@Override
					public void run()
					{
						for (Entry<Form, List<IFormElement>> entry : frms.entrySet())
						{
							List<IFormController> cachedFormControllers = getFormManager().getCachedFormControllers(entry.getKey());
							ServoyDataConverterContext cntxt = new ServoyDataConverterContext(DeveloperDesignClient.this);
							for (IFormController fc : cachedFormControllers)
							{
								boolean bigChange = false;
								outer : for (IFormElement persist : entry.getValue())
								{
									if (persist.getParent().getChild(persist.getUUID()) == null)
									{
										// deleted persist
										bigChange = true;
										break;
									}
									FormElement newFe = new FormElement(persist, cntxt, new PropertyPath());

									IWebFormUI formUI = (IWebFormUI)fc.getFormUI();
									WebFormComponent webComponent = formUI.getWebComponent(newFe.getName());
									if (webComponent != null)
									{
										FormElement existingFe = webComponent.getFormElement();

										WebComponentSpecification spec = webComponent.getSpecification();
										Map<String, PropertyDescription> handlers = spec.getHandlers();
										for (String property : newFe.getRawPropertyValues().keySet())
										{
											Object currentPropValue = existingFe.getPropertyValue(property);
											Object newPropValue = newFe.getPropertyValue(property);
											if (!Utils.equalObjects(currentPropValue, newPropValue))
											{
												if (handlers.get(property) != null)
												{
													// this is a handler change so a big change (component could react to a handler differently)
													bigChange = true;
													break outer;
												}
												PropertyDescription prop = spec.getProperty(property);
												if ("design".equals(prop.getScope()))
												{
													// this is a design property change so a big change
													bigChange = true;
													break outer;
												}
												webComponent.setFormElement(newFe);
												webComponent.setProperty(property, newFe.getPropertyValueConvertedForWebComponent(property, webComponent));
											}
										}
									}
									else
									{
										// no webcomponent found, so new one or name change, recreate all
										bigChange = true;
										break;
									}
								}
								if (bigChange) fc.recreateUI();
							}
						}
						getWebsocketSession().getService(DesignNGClientWebsocketSession.EDITOR_CONTENT_SERVICE).executeAsyncServiceCall("refreshDecorators",
							new Object[] { });
					}
				});
			}
		}
	}

	private static BundleContext context;

	public static BundleContext getContext()
	{
		return context;
	}

	@Override
	public void start(BundleContext ctx) throws Exception
	{
		Activator.context = ctx;
		if (ApplicationServerRegistry.getServiceRegistry() != null)
		{
			final IDebugClientHandler service = ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class);
			if (service != null)
			{
				WebsocketSessionManager.setWebsocketSessionFactory(new IWebsocketSessionFactory()
				{
					private DesignNGClientWebsocketSession designerSession = null;

					@Override
					public IWebsocketSession createSession(String endpointType, String uuid) throws Exception
					{
						switch (endpointType)
						{
							case WebsocketSessionFactory.DESIGN_ENDPOINT :
								if (designerSession == null || !designerSession.isValid())
								{
									final IDesignerSolutionProvider solutionProvider = ApplicationServerRegistry.getServiceRegistry().getService(
										IDesignerSolutionProvider.class);
									designerSession = new DesignNGClientWebsocketSession(uuid);
									DesignNGClient client = new DeveloperDesignClient(designerSession, solutionProvider);
									designerSession.setClient(client);
								}
								return designerSession;
							case WebsocketSessionFactory.CLIENT_ENDPOINT :
								NGClientWebsocketSession wsSession = new NGClientWebsocketSession(uuid);
								wsSession.setClient((NGClient)service.createDebugNGClient(wsSession));
								return wsSession;
						}
						return null;
					}
				});
			}
		}
	}

	@Override
	public void stop(BundleContext ctx) throws Exception
	{
	}
}
