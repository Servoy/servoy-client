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
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.mozilla.javascript.RhinoException;

import com.servoy.j2db.Credentials;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormDialog;
import com.servoy.j2db.FormFrame;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormWindow;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IApplicationServer;
import com.servoy.j2db.IApplicationServerAccess;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.Messages;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
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
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.ApplicationServerSingleton;
import com.servoy.j2db.server.RemoteActiveSolutionHandler;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.MainPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.smart.SwingFormManager;
import com.servoy.j2db.smart.cmd.CmdManager;
import com.servoy.j2db.smart.dataui.FormLookupPanel;
import com.servoy.j2db.smart.scripting.ScriptMenuItem;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.LocalhostRMIRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

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
		private DebugSwingFormMananger(IApplication app, IMainContainer mainContainer)
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
		protected ScriptMenuItem getScriptMenuItem(ScriptMethod sm, FormController fp, int autoSortcut)
		{
			ScriptMenuItem item = new ScriptMenuItem(getApplication(), fp, sm.getName(), autoSortcut);
			if (sm.getShowInMenu())
			{
				item.setIcon(getApplication().loadImage("showinmenuform.gif"));//$NON-NLS-1$
			}
			else
			{
				item.setIcon(getApplication().loadImage("empty.gif"));//$NON-NLS-1$
			}

			return item;
		}

		@Override
		protected void makeSolutionSettings(Solution s)
		{
			super.makeSolutionSettings(s);
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

		@Override
		protected FormFrame createFormFrame(IApplication app, String windowName)
		{
			FormFrame ff = super.createFormFrame(app, windowName);
			setUpFormWindow(ff, ff.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW), ff.getRootPane().getActionMap());
			return ff;
		}

		@Override
		protected FormDialog createFormDialog(IApplication app, Window owner, boolean modal, String dialogName)
		{
			FormDialog debugFormDialog = super.createFormDialog(app, owner, modal, dialogName);
			setUpFormWindow(debugFormDialog, debugFormDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
				debugFormDialog.getRootPane().getActionMap());
			return debugFormDialog;
		}

		private void setUpFormWindow(final FormWindow window, InputMap im, ActionMap am)
		{
			am.put("CTRL+L", new AbstractAction() //$NON-NLS-1$
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
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, J2DBClient.menuShortcutKeyMask), "CTRL+L"); //$NON-NLS-1$
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
				if (se instanceof RemoteDebugScriptEngine && ((RemoteDebugScriptEngine)se).isAlreadyExecutingFunctionInDebug() &&
					(System.currentTimeMillis() - creationTimestamp < MAX_TIME_TO_WAIT_FOR_SCRIPTS_TO_FINISH))
				{
					// try to avoid refresh (postpone it) while inside a script application.updateUI or application.sleep, if possible with MAX_TIME_TO_WAIT_FOR_SCRIPTS_TO_FINISH ms tolerance
					// added a timer instead of simply calling invokeLater, because otherwise the debug smart client UI would not repaint when using ALT+TAB in this case, although when hovering with the mouse over components, those would get repainted...
					new Timer().schedule(new TimerTask()
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

	private Solution current;
	private boolean shutDown = true;
	private boolean unitTestsRunning = false;
	private final IDesignerCallback designerCallback;
	private final RefreshPersistsSequencer refreshPersistsSequencer;

	public boolean isShutDown()
	{
		return shutDown;
	}

	public DebugJ2DBClient(final IDesignerCallback callback)
	{
		this.designerCallback = callback;
		refreshPersistsSequencer = new RefreshPersistsSequencer();
		startupApplication(new String[0]);
		InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = mainPanel.getActionMap();
		actionMap.put("CTRL+L", new AbstractAction() //$NON-NLS-1$
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
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcutKeyMask), "CTRL+L"); //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#attachAppleMenu(java.util.Map)
	 */
	@Override
	protected void attachAppleMenu(Map<String, Action> atns)
	{
		// dont attach anything
	}

	@Override
	public boolean isInDeveloper()
	{
		return true;
	}

	@Override
	public void showDefaultLogin()
	{
		if (getMainApplicationFrame().isVisible())
		{
			super.showDefaultLogin();
		}
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#setFrameVisible(boolean)
	 */
	@Override
	protected void setFrameVisible(boolean b)
	{
		// don't do anything, controlled by eclipse (show method)
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
			return new URL("http://127.0.0.1:" + ApplicationServerSingleton.get().getWebServerPort()); //$NON-NLS-1$
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
					ComponentFactory.flushCachedItems(); // some stuff may have been cached while components are painted in form editor
					getMainApplicationFrame().setVisible(true);
				}
				else
				{
					getMainApplicationFrame().setState(Frame.NORMAL);
				}
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
					handleClientUserUidChanged(null, null);
				}
				else
				{
					((FormManager)getFormManager()).showFormInMainPanel(form.getName());
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
			if (current != null)
			{
				closeSolution(true, null);
			}
			this.current = current;
			handleClientUserUidChanged(null, getClientInfo().getUserUid());
		}
	}

	@Override
	protected LoginDialog createLoginDialog()
	{
		// Override login dialog, add a 'use dummy login' checkbox
		return new LoginDialog(frame, this, Messages.getString("servoy.logindialog.title"), false, true) //$NON-NLS-1$
		{
			@Override
			protected JCheckBox createRememberMeCheckbox()
			{
				return new JCheckBox("Use dummy login (set in Preferences)", false);
			}

			@Override
			public Object[] showDialog(String name)
			{
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
		Debug.error("Tried adding protocol: " + protocolName + " as an url stream handler in debug client"); //$NON-NLS-1$ //$NON-NLS-2$
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
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		getMainApplicationFrame().setVisible(false);
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
				frame.getJMenuBar().setVisible(true);
				((JPanel)super.getToolbarPanel()).setVisible(true);
			}
		}
		return b;
	}

	@Override
	protected boolean callCloseSolutionMethod(boolean force)
	{
		// do not call method if user not logged in and solution requires authentication
		if (getSolution() != null && getSolution().getMustAuthenticate() && getUserUID() == null) return true;
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
		return ApplicationServerSingleton.get().getLafManager();
	}

	@Override
	protected IBeanManager createBeanManager()
	{
		return ApplicationServerSingleton.get().getBeanManager();
	}

	@Override
	protected IFormManager createFormManager()
	{
		return new DebugSwingFormMananger(this, mainPanel);
	}


	// overridden ssl-rmi seems not to work locally
	@Override
	protected IApplicationServer connectApplicationServer() throws Exception
	{
		return (IApplicationServer)LocalhostRMIRegistry.getService(IApplicationServer.NAME + IApplicationServer.DEBUG_POSTFIX);
	}

	/**
	 * @see com.servoy.j2db.smart.server.headlessclient.SessionClient#createScriptEngine()
	 */
	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new RemoteDebugScriptEngine(this);
	}

	@Override
	protected IActiveSolutionHandler createActiveSolutionHandler()
	{
		return new RemoteActiveSolutionHandler(this)
		{
			@Override
			public void saveActiveSolution(Solution solution) throws IOException
			{
				// no solution saving in debugger
			}
		};
	}

	@Override
	public void output(Object message, int level)
	{
		Object msg = message;
		super.output(msg, level);
		if (msg == null)
		{
			msg = "<null>"; //$NON-NLS-1$
		}

		if (getScriptEngine() != null)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)getScriptEngine()).getDebugger();
			if (debugger != null)
			{
				debugger.outputStdOut(msg.toString() + '\n');
			}
		}
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#reportJSError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportJSError(String message, Object detail)
	{
		errorToDebugger(message, detail);
		super.reportJSError(message, detail);
	}

	/**
	 * @param message
	 * @param errorDetail
	 */
	private void errorToDebugger(String message, Object errorDetail)
	{
		Object detail = errorDetail;
		RemoteDebugScriptEngine engine = (RemoteDebugScriptEngine)getScriptEngine();
		if (engine != null)
		{
			DBGPDebugger debugger = engine.getDebugger();
			if (debugger != null)
			{
				RhinoException rhinoException = null;
				if (detail instanceof Exception)
				{
					Throwable exception = (Exception)detail;
					while (exception != null)
					{
						if (exception instanceof RhinoException)
						{
							rhinoException = (RhinoException)exception;
							break;
						}
						exception = exception.getCause();
					}
				}
				String msg = message;
				if (rhinoException != null)
				{
					if (msg == null)
					{
						msg = rhinoException.getLocalizedMessage();
					}
					else msg += '\n' + rhinoException.getLocalizedMessage();
					msg += '\n' + rhinoException.getScriptStackTrace();
				}
				else if (detail instanceof Exception)
				{
					Object e = ((Exception)detail).getCause();
					if (e != null)
					{
						detail = e;
					}
					msg += "\n > " + detail.toString(); // complete stack? //$NON-NLS-1$
				}
				else if (detail != null)
				{
					msg += "\n" + detail; //$NON-NLS-1$
				}
				debugger.outputStdErr(msg.toString() + "\n"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#reportError(java.awt.Component, java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportError(final Component parentComponent, String msg, Object detail)
	{
		String message = msg;
		errorToDebugger(message, detail);
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
		if (message == null) message = ""; //$NON-NLS-1$
		if (message.length() > 100)
		{
			message = message.substring(0, 100) + "..."; //$NON-NLS-1$
		}
		final String m = message;
		// invoke later in the debug client else it can block the debugger.
		invokeLater(new Runnable()
		{
			public void run()
			{
				if (parentComponent.isVisible())
				{
					JOptionPane.showMessageDialog(parentComponent, m, Messages.getString("servoy.general.error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				}
			}
		});
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

		List<FormController>[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

		for (FormController controller : scopesAndFormsToReload[1])
		{
			destroyForm(controller);
		}

		for (FormController controller : scopesAndFormsToReload[0])
		{
			if (controller.getForm() instanceof FlattenedForm)
			{
				FlattenedForm ff = (FlattenedForm)controller.getForm();
				ff.reload();
			}
			controller.getFormScope().reload();
		}
	}

	/**
	 * @param formController
	 * @return
	 */
	private void destroyForm(FormController formController)
	{
		refreshI18NMessages();
		if (formController.isFormVisible())
		{
			IFoundSetInternal foundSet = formController.getFoundSet();
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
				((FormManager)getFormManager()).showFormInCurrentContainer(name);
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
					newFormController.loadData(foundSet, null);

					List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
					newFormController.notifyVisible(true, invokeLaterRunnables);
					Utils.invokeLater(this, invokeLaterRunnables);
				}
				else if (isNavigator)
				{
					FormController navigator = ((FormManager)getFormManager()).getFormController(name, container);
					navigator.loadData(foundSet, null);
					List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
					navigator.notifyVisible(true, invokeLaterRunnables);
					Utils.invokeLater(this, invokeLaterRunnables);
					mainPanel.setNavigator(navigator);
				}
				else if (isWindow)
				{
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
					// else not logged in
				}
				catch (Exception e)
				{
					Debug.error("Error during logout", e); //$NON-NLS-1$
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
}
