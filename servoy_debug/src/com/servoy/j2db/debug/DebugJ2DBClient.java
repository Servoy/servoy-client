/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.debug;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.DefaultPersistenceDelegate;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;

import org.mozilla.javascript.RhinoException;

import com.servoy.j2db.Credentials;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormWindow;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IBrowserLauncher;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormManagerInternal;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ValidatingDelegateDataServer;
import com.servoy.j2db.gui.FormDialog;
import com.servoy.j2db.gui.LoginDialog;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IDebugApplicationServer;
import com.servoy.j2db.server.shared.RemoteActiveSolutionHandler;
import com.servoy.j2db.smart.FormFrame;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.LoadingUIEffects;
import com.servoy.j2db.smart.MainPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.smart.SwingFormManager;
import com.servoy.j2db.smart.SwingRuntimeWindow;
import com.servoy.j2db.smart.SwingRuntimeWindowManager;
import com.servoy.j2db.smart.cmd.CmdManager;
import com.servoy.j2db.smart.dataui.FormLookupPanel;
import com.servoy.j2db.smart.plugins.SmartClientPluginAccessProvider;
import com.servoy.j2db.smart.scripting.ScriptMenuItem;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDeveloperURLStreamHandler;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.ThreadingRemoteInvocationHandler;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jcompagner
 *
 */
public class DebugJ2DBClient extends J2DBClient implements IDebugJ2DBClient
{
	final class DebugSwingFormMananger extends SwingFormManager implements DebugUtils.DebugUpdateFormSupport
	{
		private boolean solutionLoaded = false;

		/**
		 * @param app
		 * @param mainContainer
		 */
		private DebugSwingFormMananger(ISmartClientApplication app, IMainContainer mainContainer)
		{
			super(app, mainContainer);
		}

		/**
		 * @see com.servoy.j2db.smart.SwingFormManager#getShowFormsAllInWindowMenu()
		 */
		@Override
		protected boolean getShowFormsAllInWindowMenu()
		{
			return true;
		}

		public void updateForm(Form form)
		{
			boolean isNew = !possibleForms.containsValue(form);
			boolean isDeleted = false;
			if (!isNew)
			{
				isDeleted = !((AbstractBase)form.getParent()).getAllObjectsAsList().contains(form);
			}
			updateForm(form, isNew, isDeleted);
		}

		/**
		 * @param form
		 * @param isNew
		 * @param isDeleted
		 */
		private void updateForm(Form form, boolean isNew, boolean isDeleted)
		{
			if (isNew)
			{
				addForm(form, false);
			}
			else if (isDeleted)
			{
				windowMenuDialog.removeForm(form);
				Iterator<Entry<String, Form>> iterator = possibleForms.entrySet().iterator();
				while (iterator.hasNext())
				{
					Map.Entry<String, Form> entry = iterator.next();
					if (entry.getValue().equals(form))
					{
						iterator.remove();
					}
				}
			}
			else
			{
				// just changed
				if (possibleForms.get(form.getName()) == null)
				{
					// name change, first remove the form
					updateForm(form, false, true);
					// then add it back in
					updateForm(form, true, false);
				}
				else
				{
					windowMenuDialog.refreshFrom(form);
				}
			}
		}

		// fill the scripts menu
		@Override
		protected ScriptMenuItem getScriptMenuItem(ScriptMethod sm, FunctionDefinition functionDefinition, int autoSortcut)
		{
			ScriptMenuItem item = new ScriptMenuItem(getApplication(), functionDefinition, sm.getName(), autoSortcut);
			if (sm.getShowInMenu())
			{
				item.setIcon(getApplication().loadImage("showinmenuform.gif"));
			}
			else
			{
				item.setIcon(getApplication().loadImage("empty.gif"));
			}

			return item;
		}

		@Override
		protected void makeSolutionSettings(Solution s)
		{
			IApplication app = getApplication();
			if (app instanceof IDebugJ2DBClient) ((IDebugJ2DBClient)app).onSolutionOpen();
			super.makeSolutionSettings(s);
			if (isShutDown()) return; // for example user could have hit the stop button while solution onLoad was running or the test client solution timeout kicked in; in this case the solution is not loaded anymore!
			solutionLoaded = true;
		}

		@Override
		protected void destroySolutionSettings()
		{
			solutionLoaded = false;
			super.destroySolutionSettings();
			readOnlyCheck.clear();
		}

		public boolean isSolutionLoaded()
		{
			return solutionLoaded;
		}

	}

	public class DebugSwingRuntimeWindowManager extends SwingRuntimeWindowManager
	{
		public DebugSwingRuntimeWindowManager(IApplication application)
		{
			super(application);
		}

		@Override
		protected RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parentWindow)
		{
			return new SwingRuntimeWindow((ISmartClientApplication)application, windowName, type, parentWindow)
			{

				@Override
				protected FormFrame createFormFrame(String formWindowName)
				{
					FormFrame ff = super.createFormFrame(formWindowName);
					setUpFormWindow(ff, ff.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW), ff.getRootPane().getActionMap());
					return ff;
				}

				@Override
				protected FormDialog createFormDialog(Window owner, boolean modal, String dialogName)
				{
					FormDialog debugFormDialog = super.createFormDialog(owner, modal, dialogName);
					setUpFormWindow(debugFormDialog, debugFormDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
						debugFormDialog.getRootPane().getActionMap());
					return debugFormDialog;
				}

				private void setUpFormWindow(final FormWindow window, InputMap im, ActionMap am)
				{
					am.put("CTRL+L", new AbstractAction()
					{
						public void actionPerformed(ActionEvent e)
						{
							IMainContainer debugFormMainContainer = window.getMainContainer();
							if (debugFormMainContainer != null)
							{
								FormController fc = debugFormMainContainer.getController();
								if (fc == null) return;
								Form form = fc.getForm();
								Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
								if (focusOwner != null)
								{
									while (!(focusOwner instanceof SwingForm) && focusOwner != null)
									{
										focusOwner = focusOwner.getParent();
									}
									if (focusOwner != null)
									{
										form = ((SwingForm)focusOwner).getController().getForm();
									}
								}
								DebugJ2DBClient.this.designerCallback.showFormInDesigner(form);
							}
						}
					});
					im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, J2DBClient.menuShortcutKeyMask), "CTRL+L");
				}

			};
		}
	}

	/**
	 * Class that is able to refresh persists sequentially and when no script is executing.
	 */
	// reason for creating this class:
	// 1. updating debug smart client as a result of editor save => form refresh/reload that triggers JS events that do application.updateUI(...) - this messes up update sequencing when using only invokeLater(...)
	// 2. updating debug smart client as a result of editor save while JS that contains application.updateUI(...) is still being executed in AWT
	public class RefreshPersistsSequencer
	{
		SequencedRunnable lastSequencedRunnable = null;

		public void runLaterInSequence(Runnable runnable)
		{
			boolean mustInvoke = false;
			synchronized (this)
			{
				if (lastSequencedRunnable == null)
				{
					// only first time
					lastSequencedRunnable = new SequencedRunnable(runnable);
					mustInvoke = true;
				}
				else
				{
					Pair<SequencedRunnable, Boolean> p = lastSequencedRunnable.setNextJob(runnable);
					lastSequencedRunnable = p.getLeft();
					mustInvoke = p.getRight().booleanValue();
				}
			}
			if (mustInvoke)
			{
				if (SwingUtilities.isEventDispatchThread())
				{
					lastSequencedRunnable.run();
				}
				else
				{
					SwingUtilities.invokeLater(lastSequencedRunnable);
				}
			}
		}

		private class SequencedRunnable implements Runnable
		{
			private static final int MAX_TIME_TO_WAIT_FOR_SCRIPTS_TO_FINISH = 15000; // ms

			private Runnable r;
			private SequencedRunnable next = null;
			private final long creationTimestamp;

			public SequencedRunnable(Runnable r)
			{
				this.r = r;
				this.creationTimestamp = System.currentTimeMillis();
			}

			public void run()
			{
				IExecutingEnviroment se = getScriptEngine();
				if (se instanceof RemoteDebugScriptEngine && ((RemoteDebugScriptEngine)se).isAWTSuspendedRunningScript() &&
					(System.currentTimeMillis() - creationTimestamp < MAX_TIME_TO_WAIT_FOR_SCRIPTS_TO_FINISH))
				{
					// try to avoid refresh (postpone it) while inside a script application.updateUI or application.sleep, if possible with MAX_TIME_TO_WAIT_FOR_SCRIPTS_TO_FINISH ms tolerance
					// added a timer instead of simply calling invokeLater, because otherwise the debug smart client UI would not repaint when using ALT+TAB in this case, although when hovering with the mouse over components, those would get repainted...
					new Timer(true).schedule(new TimerTask()
					{

						@Override
						public void run()
						{
							SwingUtilities.invokeLater(SequencedRunnable.this);
						}

					}, 1000);
				}
				else
				{
					try
					{
						r.run();
					}
					catch (Throwable e)
					{
						Debug.error(e);
					}
					synchronized (this)
					{
						r = null; // r is complete
						if (next != null)
						{
							SwingUtilities.invokeLater(next); // this would probably also work by calling next.run() directly here
						}
					}
				}
			}

			public synchronized Pair<SequencedRunnable, Boolean> setNextJob(Runnable runnable)
			{
				boolean mustInvoke = false;
				next = new SequencedRunnable(runnable);
				if (r == null)
				{
					// r is already done, we can start this right away
					mustInvoke = true;
				}
				return new Pair<SequencedRunnable, Boolean>(next, Boolean.valueOf(mustInvoke));
			}
		}

	}

	/**
	 * Make sure that showLoading doesn't show the main frame at inappropriate times, like first client initialisation that
	 * might happen when opening the first form designer (so only allow it while in a "run" cycle).
	 * @author acostescu
	 */
	protected class DebugLoadingUIEffects extends LoadingUIEffects
	{

		private boolean loading = false;
		private boolean clientShouldBeShowing = false;

		public DebugLoadingUIEffects(J2DBClient client, MainPanel mainPanel)
		{
			super(client, mainPanel);
		}

		@Override
		public void showSolutionLoading(boolean b)
		{
			loading = b;
			if (clientShouldBeShowing)
			{
				super.showSolutionLoading(loading);
			}
		}

		public void setClientShouldBeShowing(boolean clientShouldBeShowing)
		{
			if (clientShouldBeShowing != this.clientShouldBeShowing)
			{
				this.clientShouldBeShowing = clientShouldBeShowing;
				if (clientShouldBeShowing) super.showSolutionLoading(loading);
				else super.showSolutionLoading(false);
			}
		}

		@Override
		protected URL getWebStartURL()
		{
			try
			{
				return new URL("http://localhost:" + ApplicationServerRegistry.get().getWebServerPort());
			}
			catch (MalformedURLException e)
			{
				Debug.trace("Cannot find base URL for solution loading image...", e);
				return null;
			}
		}

	}

	private Solution current;
	private boolean shutDown = false;
	private boolean unitTestsRunning = false;
	private final IDesignerCallback designerCallback;
	private final RefreshPersistsSequencer refreshPersistsSequencer;
	private IBrowserLauncher browserLauncher;

	@Override
	public boolean isShutDown()
	{
		return shutDown;
	}

	public DebugJ2DBClient(boolean setSingletonServiceProvider, final IDesignerCallback callback)
	{
		super(setSingletonServiceProvider);
		this.designerCallback = callback;
		refreshPersistsSequencer = new RefreshPersistsSequencer();
		startupApplication(new String[0]);
		InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = mainPanel.getActionMap();
		actionMap.put("CTRL+L", new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				FormController fc = (FormController)getFormManager().getCurrentForm();
				if (fc == null) return;
				Form form = fc.getForm();
				Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				if (focusOwner != null)
				{
					while (!(focusOwner instanceof SwingForm) && focusOwner != null)
					{
						focusOwner = focusOwner.getParent();
					}
					if (focusOwner != null)
					{
						form = ((SwingForm)focusOwner).getController().getForm();
					}
				}
				callback.showFormInDesigner(form);
			}
		});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcutKeyMask), "CTRL+L");
		Settings.getInstance().loadUserProperties(defaultUserProperties);
	}

	@Override
	public boolean isEventDispatchThread()
	{
		return super.isEventDispatchThread() || Thread.currentThread() instanceof ServoyDebugger ||
			(!Utils.isAppleMacOS() && Thread.currentThread().getName().equals("JavaFX Application Thread"));
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#attachAppleMenu(java.util.Map)
	 */
	@Override
	protected void attachAppleMenu(Map<String, Action> atns)
	{
		// dont attach anything
	}

	private Map<String, Action> actions;

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#getServerURL()
	 */
	@Override
	public URL getServerURL()
	{
		try
		{
			return new URL("http://127.0.0.1:" + ApplicationServerRegistry.get().getWebServerPort());
		}
		catch (MalformedURLException e)
		{
			Debug.error(e);
		}
		return null;
	}

	private int showCounter = 0;

	@Override
	protected void showAd()
	{
		showCounter = 10;
	}

	public void show()
	{
		if (showCounter > 0)
		{
			if (showCounter % 10 == 0) super.showAd();
			showCounter++;
		}
		show(null);
	}

	@Override
	protected LoadingUIEffects createLoadingUIEffects()
	{
		return new DebugLoadingUIEffects(this, mainPanel);
	}

	@Override
	protected DebugLoadingUIEffects getLoadingUIEffects()
	{
		return (DebugLoadingUIEffects)super.getLoadingUIEffects();
	}

	/**
	 *
	 */
	public void show(final Form form)
	{
		shutDown = false;
		Runnable run = new Runnable()
		{
			public void run()
			{
				if (!getMainApplicationFrame().isVisible())
				{
					ComponentFactory.flushCachedItems(DebugJ2DBClient.this); // some stuff may have been cached while components are painted in form editor
					getMainApplicationFrame().setVisible(true);
				}
				else
				{
					getMainApplicationFrame().setState(Frame.NORMAL);
				}
				getLoadingUIEffects().setClientShouldBeShowing(!shutDown);
				if (unitTestsRunning)
				{
					getMainApplicationFrame().setState(Frame.ICONIFIED);
				}
				else
				{
					getMainApplicationFrame().toFront();
				}
				if (form == null)
				{
					if (isSolutionLoaded())
					{
						closeSolution(true, null);
					}
					else
					{
						selectAndOpenSolution();// fake first load
					}
				}
				else
				{
					getFormManager().showFormInMainPanel(form.getName());
				}
			}
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			run.run();
		}
		else
		{
			SwingUtilities.invokeLater(run);
		}
	}

	/**
	 * @param current the current to set
	 */
	public void setCurrent(Solution current)
	{
		if (this.current != current)
		{
			if (this.current != null)
			{
				closeSolution(true, null);
			}
			this.current = current;
			logout(null); // login (possibly dummy) from previous solution may not be valid for new one
		}
	}

	@Override
	protected LoginDialog createLoginDialog()
	{
		// Override login dialog, add a 'use dummy login' checkbox
		return new LoginDialog(frame, this, Messages.getString("servoy.logindialog.title"), false, false /* do not show remember-me/dummy login check */)
		{
			@Override
			protected JCheckBox createRememberMeCheckbox()
			{
				return new JCheckBox("Use dummy login (set in Preferences)", false);
			}

			@Override
			public Object[] showDialog(String name)
			{
				if (current == null)
				{
					return null;
				}
				if (current.getMustAuthenticate())
				{
					return super.showDialog(name);
				}

				// will only get here when enhanced security is turned on and solution.mustAuthenticate = false
				// Use the dummy auth to access the appserver, in real server access a login dialog would be shown

				DeveloperPreferences developerPreferences = new DeveloperPreferences(Settings.getInstance());
				boolean dummyAuth = developerPreferences.getUseDummyAuth();
				if (!dummyAuth)
				{
					Object[] loginResult = super.showDialog(name);
					dummyAuth = loginResult != null && loginResult.length >= 3 && Boolean.TRUE.equals(loginResult[2]);
					if (!dummyAuth)
					{
						return loginResult;
					}
					developerPreferences.setUseDummyAuth(true);
				}
				// dummy authentication
				try
				{
					authenticate(new Credentials(getClientID(), null, null, IApplicationServer.DUMMY_LOGIN));
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}

				// return null means user hit cancel, in case of dummy login, user id has changed
				handleClientUserUidChanged(null, getClientInfo().getUserUid());
				return null;
			}
		};
	}

	/**
	 * @return the current
	 */
	public Solution getCurrent()
	{
		return current;
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#initRMISocketFactory()
	 */
	@Override
	protected void initRMISocketFactory()
	{
		// ignore
	}

	@Override
	protected void bindUserClient()
	{
	}

	@Override
	public synchronized int exportObject(Remote object) throws RemoteException
	{
		// don't export in debug client
		return -1;
	}

	@Override
	public void invokeAndWait(Runnable r)
	{
		if (getScriptEngine() instanceof IScriptSupport && ((IScriptSupport)getScriptEngine()).isAWTSuspendedRunningScript())
		{
			r.run();
		}
		else
		{
			super.invokeAndWait(r);
		}
	}

	@Override
	protected void unBindUserClient() throws Exception
	{
	}


	/**
	 * @see com.servoy.j2db.smart.J2DBClient#initStreamHandlerFactory()
	 */
	@Override
	protected void initStreamHandlerFactory()
	{
		// ignore
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#addURLStreamHandler(java.lang.String, java.net.URLStreamHandler)
	 */
	@Override
	public void addURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		// TODO See Activator of application server eclipse project
		if (handler instanceof IDeveloperURLStreamHandler)
		{

			designerCallback.addURLStreamHandler(protocolName, (IDeveloperURLStreamHandler)handler);
		}
		else
		{
			Debug.error("Tried adding protocol: " + protocolName +
				" as an url stream handler in debug client, please implement the IDeveloperURLStreamHandler interface");
		}
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#shutDown(boolean)
	 */
	@Override
	public void shutDown(boolean force)
	{
		shutDown = true;
		try
		{
			Solution solution = getSolution();
			if (solution != null)
			{
				if (!closeSolution(force, null) && !force)
				{
					shutDown = false; // could not close
					return;
				}
			}
			logout(null);
			if (unitTestsRunning)
			{
				repository = null;
				applicationServerAccess = null;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		getLoadingUIEffects().setClientShouldBeShowing(!shutDown);

		//dispose owned windows
		Window[] ownedWindows = getMainApplicationFrame().getOwnedWindows();
		if (ownedWindows != null)
		{
			for (Window w : ownedWindows)
				w.dispose();
		}
		getMainApplicationFrame().setVisible(false);
		saveSettings();
	}

	public void shutDownAndDispose()
	{
		shutDown = true;
		super.shutDown(true);
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#getActions()
	 */
	@Override
	protected Map<String, Action> getActions()
	{
		if (actions == null)
		{
			actions = super.getActions();
		}
		return actions;
	}


	/**
	 * @see com.servoy.j2db.smart.J2DBClient#closeSolution(boolean, java.lang.Object[])
	 */
	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		boolean b = super.closeSolution(force, args);
		if (b)
		{
			if (actions != null)
			{
				// don't create for close
				createMenuBar(actions);
				fillToolbar(actions);

				String showMenuBar = settings.getProperty("servoy.smartclient.showMenuBar");
				if (showMenuBar != null && showMenuBar.equals("false")) frame.getJMenuBar().setVisible(false);
				else frame.getJMenuBar().setVisible(true);
				String showToolBar = settings.getProperty("servoy.smartclient.showToolBar");
				if (showToolBar != null && showToolBar.equals("false")) toolbarsPanel.setVisible(false);
				else toolbarsPanel.setVisible(true);
			}

			if (getPreferedSolutionNameToLoadOnInit() == null && getMainApplicationFrame().isVisible())
			{
				isSelectAndOpenSolutionStarted = false;
				invokeLater(new Runnable()
				{
					public void run()
					{
						invokeLater(new Runnable()
						{
							public void run()
							{
								if (!isSelectAndOpenSolutionStarted && solutionRoot.getMainSolutionMetaData() == null)
								{
									selectAndOpenSolution(); // automatically re-open solution in developer
								}
							}
						});
					}
				});
			}
		}
		return b;
	}

	private boolean isSelectAndOpenSolutionStarted;

	@Override
	public void selectAndOpenSolution()
	{
		isSelectAndOpenSolutionStarted = true;
		super.selectAndOpenSolution();
	}

	@Override
	protected boolean callCloseSolutionMethod(boolean force)
	{
		// do not call method if user not logged in and solution requires authentication
		if (getSolution() != null && getSolution().requireAuthentication() && getUserUID() == null) return true;
		return super.callCloseSolutionMethod(force);
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#initSettings()
	 */
	@Override
	protected void initSettings() throws Exception
	{
		settings = Settings.getInstance();
	}

	@Override
	protected ILAFManager createLAFManager()
	{
		return ApplicationServerRegistry.get().getLafManager();
	}

	@Override
	protected SmartClientPluginAccessProvider createClientPluginAccessProvider()
	{
		return new SmartClientPluginAccessProvider(this)
		{
			private final boolean useSerializingDataserverProxy = new DeveloperPreferences(Settings.getInstance()).useSerializingDataserverProxy();

			@Override
			public Remote getRemoteService(String name) throws Exception
			{
				// (de)serialize arguments and result of 'remote' services in developer to mimic rmi in smart client
				Remote remoteService = super.getRemoteService(name);
				if (useSerializingDataserverProxy)
				{
					return SerializingRemoteInvocationHandler.createSerializingSerializingInvocationHandler(DebugJ2DBClient.this, remoteService);
				}
				return remoteService;
			}
		};
	}

	@Override
	protected IDataServer createDataServer()
	{
		IDataServer dataServer = super.createDataServer();
		if (dataServer != null)
		{
			if (new DeveloperPreferences(Settings.getInstance()).useSerializingDataserverProxy())
			{
				dataServer = SerializingRemoteInvocationHandler.createSerializingSerializingInvocationHandler(this,
					ThreadingRemoteInvocationHandler.createThreadingRemoteInvocationHandler(dataServer, new Class< ? >[] { IDataServer.class }),
					new Class[] { IDataServer.class });
			}
			dataServer = new ProfileDataServer(new ValidatingDelegateDataServer(dataServer, this));
		}
		return dataServer;
	}

	@Override
	protected IBeanManager createBeanManager()
	{
		return ApplicationServerRegistry.get().getBeanManager();
	}

	@Override
	protected IFormManagerInternal createFormManager()
	{
		return new DebugSwingFormMananger(this, mainPanel);
	}

	@Override
	protected RuntimeWindowManager createJSWindowManager()
	{
		return new DebugSwingRuntimeWindowManager(this);
	}

	// overridden ssl-rmi seems not to work locally
	@Override
	protected IApplicationServer connectApplicationServer() throws Exception
	{
		return ApplicationServerRegistry.getService(IDebugApplicationServer.class);
	}

	/**
	 * @see com.servoy.j2db.smart.server.headlessclient.SessionClient#createScriptEngine()
	 */
	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		RemoteDebugScriptEngine engine = new RemoteDebugScriptEngine(this);

		if (designerCallback != null)
		{
			designerCallback.addScriptObjects(this, engine.getSolutionScope());
		}
		return engine;
	}

	@Override
	protected IActiveSolutionHandler createActiveSolutionHandler()
	{
		return new RemoteActiveSolutionHandler(getApplicationServer(), this)
		{
			@Override
			public void saveActiveSolution(Solution solution)
			{
				// no solution saving in debugger
			}
		};
	}

	@Override
	public void output(Object message, int level)
	{
		super.output(message, level);
		if (level == ILogLevel.WARNING || level == ILogLevel.ERROR)
		{
			DebugUtils.errorToDebugger(getScriptEngine(), message.toString(), null);
		}
		else
		{
			DebugUtils.stdoutToDebugger(getScriptEngine(), message);
		}
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#reportJSError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportJSError(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
		super.reportJSError(message, detail);
	}

	@Override
	public void reportJSWarning(String s)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), s, null);
		super.reportJSWarning(s);
	}

	@Override
	public void reportJSWarning(String s, Throwable t)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), s, t);
		super.reportJSWarning(s, t);
	}

	@Override
	public void reportJSInfo(String s)
	{
		DebugUtils.stdoutToDebugger(getScriptEngine(), "INFO: " + s);
		super.reportJSInfo(s);
	}

	@Override
	public void reportInfo(Component parentComponent, String message, String title)
	{
		DebugUtils.infoToDebugger(getScriptEngine(), message);
		super.reportInfo(parentComponent, message, title);
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#reportError(java.awt.Component, java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportError(final Component parentComponent, String msg, Object detail)
	{
		String message = msg;
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
		Debug.error(detail);
		mainPanel.getToolkit().beep();
		if (detail instanceof ServoyException)
		{
			message = ((ServoyException)detail).getMessage();
		}
		else if (detail instanceof RhinoException)
		{
			RhinoException re = (RhinoException)detail;
			if (re.getCause() != null) message = re.getCause().getLocalizedMessage();
		}
		if (message == null) message = "";
		if (message.length() > 100)
		{
			message = message.substring(0, 100) + "...";
		}
		final String m = message;
		// invoke later in the debug client else it can block the debugger.
		invokeLater(new Runnable()
		{
			public void run()
			{
				if (parentComponent.isVisible())
				{
					JOptionPane.showMessageDialog(parentComponent, m, Messages.getString("servoy.general.error"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}


	public void refreshForI18NChange(boolean recreateForms)
	{
		if (shutDown) return;
		refreshI18NMessages();

		if (recreateForms)
		{
			Runnable run = new Runnable()
			{
				@Override
				public void run()
				{
					for (IFormController fc : getFormManager().getCachedFormControllers())
					{
						fc.recreateUI();
					}

				}
			};
			if (SwingUtilities.isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				SwingUtilities.invokeLater(run);
			}
		}
	}

	/**
	 * @param changes
	 */
	public void refreshPersists(final Collection<IPersist> changes)
	{
		if (shutDown) return;

		refreshPersistsSequencer.runLaterInSequence(new Runnable()
		{
			public void run()
			{
				refreshPersistsNow(changes);
			}
		});
	}

	private void refreshPersistsNow(Collection<IPersist> changes)
	{
		if (shutDown) return;

		Set<IFormController>[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

		for (IFormController controller : scopesAndFormsToReload[1])
		{
			destroyForm(controller);
		}

		for (IFormController controller : scopesAndFormsToReload[0])
		{
			if (controller.getForm() instanceof FlattenedForm)
			{
				FlattenedForm ff = (FlattenedForm)controller.getForm();
				ff.reload();
			}
			if (!controller.isDestroyed())
			{
				controller.getFormScope().reload();
			}
		}
	}

	/**
	 * @param formController
	 * @return
	 */
	private void destroyForm(IFormController formController)
	{
		refreshI18NMessages();
		if (formController.isFormVisible())
		{
			IFoundSetInternal foundSet = formController.getFormModel();
			if (foundSet instanceof FoundSet)
			{
				((FoundSet)foundSet).refresh();
			}
			String name = null;
			if (formController.getForm() != null) name = formController.getForm().getName();
			if (name == null) name = formController.getName();
			if (getFormManager().getCurrentForm() == formController)
			{
				formController.destroy();
				getFormManager().showFormInCurrentContainer(name);
			}
			else
			{
				SwingForm swingForm = (SwingForm)formController.getFormUI();
				Container container = swingForm.getParent();
				boolean isNavigator = false;
				boolean isWindow = false;
				boolean isLookupPanel = false;
				if (container instanceof MainPanel)
				{
					isNavigator = ((MainPanel)container).getNavigator() == formController;
				}
				else if (container instanceof FormLookupPanel)
				{
					isLookupPanel = true;
				}
				else
				{
					while (container != null && !(container instanceof FormWindow))
					{
						container = container.getParent();
					}
					if (container instanceof FormWindow)
					{
						isWindow = true;
					}
				}
				formController.destroy();
				if (isLookupPanel)
				{
					FormLookupPanel flp = (FormLookupPanel)container;
					FormController newFormController = flp.getFormPanel();
					if (newFormController != null)
					{
						// deleted in developer ?
						newFormController.loadData(foundSet, null);

						List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
						newFormController.notifyVisible(true, invokeLaterRunnables);
						Utils.invokeLater(this, invokeLaterRunnables);
					}
				}
				else if (isNavigator)
				{
					// TODO isNavigator check will always be false for NGClient?
					FormController navigator = ((FormManager)getFormManager()).getFormController(name, container);
					if (navigator != null)
					{
						navigator.loadData(foundSet, null);
						List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
						navigator.notifyVisible(true, invokeLaterRunnables);
						Utils.invokeLater(this, invokeLaterRunnables);
					}
					mainPanel.setNavigator(navigator);
				}
				else if (isWindow)
				{
					// TODO isWindow check will always be false for NGClient?
					FormWindow w = (FormWindow)container;
					((FormManager)getFormManager()).showFormInMainPanel(name, w.getMainContainer(), w.getTitle(), false, w.getName());
				}
			}
		}
		else
		{
			formController.destroy();
		}
		return;
	}

	@Override
	protected CmdManager createCmdManager()
	{
		CmdManager cmdM = super.createCmdManager();

		// the AutoOpenSolution must be set to false in order to avoid at least one NullPointerException;
		// because this DebugJ2DBClient constructor runs in the AWT thread and it will also call invokeLater(...) on the AWT thread
		// this can cause problems in Eclipse. Other code in the Eclipse threads might run in parallel. For example,
		// a Form Designer editor is being opened and does things with the FlattenedSolution while this AWT thread
		// calls close() on the flattened solution when trying to show the open solution dialog (=> the NullPointerException I originally mentioned).
		cmdM.setAutoOpenSolutionSelectDialog(false);
		return cmdM;
	}

	public boolean isDoneLoading()
	{
		return ((DebugSwingFormMananger)formManager).isSolutionLoaded();
	}

	public void setUnitTestMode(boolean b)
	{
		unitTestsRunning = b;
	}

	@Override
	public void logout(Object[] solutionToOpenArgs)
	{
		if (unitTestsRunning)
		{
			// in tests, it is sometimes useful to be able to logout without closing the test solution - so that you can login with a different user
			if (getClientInfo().getClientId() != null)
			{
				try
				{
					IApplicationServerAccess asa = getApplicationServerAccess();
					if (asa != null)
					{
						asa.logout(getClientInfo().getClientId());
					}
					getClientInfo().clearUserInfo();
					// else not logged in
				}
				catch (Exception e)
				{
					Debug.error("Error during logout", e);
				}
			}
		}
		else
		{
			super.logout(solutionToOpenArgs);
		}
	}

	@Override
	protected SolutionMetaData selectSolutionToLoad() throws RepositoryException
	{
		return (current == null) ? null : current.getSolutionMetaData();
	}

	@Override
	protected boolean getAppleScreenMenuBar()
	{
		return false;
	}


	@Override
	protected void installShutdownHook()
	{
		//don't install the shutdownhook;
	}

	@Override
	protected void startupApplication(String[] args)
	{
		super.startupApplication(args);
		try
		{
			Introspector.getBeanInfo(SpecialMatteBorder.class).getBeanDescriptor().setValue("persistenceDelegate",
				new DefaultPersistenceDelegate(new String[] { "top", "left", "bottom", "right", "topColor", "leftColor", "bottomColor", "rightColor" }));
		}
		catch (IntrospectionException e)
		{
			Debug.error(e);
		}
	}

	public void setBrowserLauncher(IBrowserLauncher browserLauncher)
	{
		this.browserLauncher = browserLauncher;
	}

	public IBrowserLauncher getBrowserLauncher()
	{
		return browserLauncher;
	}

	@Override
	public boolean showURL(String url, String target, String target_options, int timeout, boolean closeDialogs)
	{
		return browserLauncher != null ? browserLauncher.showURL(url) : super.showURL(url, target, target_options, timeout, closeDialogs);
	}

	@Override
	protected void exitHard(int status)
	{
		// Do not exit developer
		if (!isShutDown())
		{
			shutDown(true);
		}
	}

	Map<String, String> defaultUserProperties = new HashMap<String, String>();

	@Override
	public void setUserProperty(String name, String value)
	{
		defaultUserProperties.remove(name);
		((Settings)getSettings()).setUserProperty(Settings.DEVELOPER_USER, name, value);
	}

	@Override
	public String getUserProperty(String name)
	{
		if (defaultUserProperties.containsKey(name))
		{
			return defaultUserProperties.get(name);
		}
		return ((Settings)getSettings()).getUserProperty(Settings.DEVELOPER_USER, name);
	}

	@Override
	public String[] getUserPropertyNames()
	{
		List<String> userPropertyNames = new ArrayList<String>(defaultUserProperties.keySet());
		Iterator<Object> it = getSettings().keySet().iterator();
		while (it.hasNext())
		{
			String key = (String)it.next();
			if (key.startsWith(Settings.DEVELOPER_USER))
			{
				String name = key.substring(Settings.DEVELOPER_USER.length());
				if (!userPropertyNames.contains(name))
				{
					userPropertyNames.add(name);
				}
			}
		}
		return userPropertyNames.toArray(new String[0]);
	}

	private HashMap<Object, Object> changedProperties;
	private boolean wasLoginSolution;

	@Override
	public boolean putClientProperty(Object name, Object value)
	{
		if (name != null && changedProperties != null && !changedProperties.containsKey(name))
		{
			changedProperties.put(name, getClientProperty(name));
			if (getSolution() != null && getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION)
			{
				wasLoginSolution = true;
			}
		}

		return super.putClientProperty(name, value);
	}

	public void onSolutionOpen()
	{
		if (changedProperties == null)
		{
			changedProperties = new HashMap<Object, Object>();
		}
		else
		{
			if (!wasLoginSolution)
			{
				if (changedProperties.containsKey(LookAndFeelInfo.class.getName()))
				{
					String selectedlnfSetting = getSettings().getProperty("selectedlnf");
					if (selectedlnfSetting != null) changedProperties.put(LookAndFeelInfo.class.getName(), selectedlnfSetting);
				}
				if (changedProperties.containsKey(Font.class.getName()))
				{
					String font = getSettings().getProperty("font");
					if (font != null) changedProperties.put(Font.class.getName(), PersistHelper.createFont(font));
				}

				Iterator<Map.Entry<Object, Object>> changedPropertiesIte = changedProperties.entrySet().iterator();
				Map.Entry<Object, Object> changedEntry;
				while (changedPropertiesIte.hasNext())
				{
					changedEntry = changedPropertiesIte.next();
					super.putClientProperty(changedEntry.getKey(), changedEntry.getValue());
				}
				changedProperties.clear();
			}
			else
			{
				wasLoginSolution = false;
			}
		}
	}

	/*
	 * @see com.servoy.j2db.IDebugClient#errorToDebugger(java.lang.String, java.lang.String)
	 */
	@Override
	public void errorToDebugger(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
	}
}
