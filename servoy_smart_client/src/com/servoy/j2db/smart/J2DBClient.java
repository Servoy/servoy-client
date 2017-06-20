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
package com.servoy.j2db.smart;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.print.PageFormat;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.BeanManager;
import com.servoy.j2db.ClientRepository;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormManagerInternal;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IMessagesCallback;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IProvideFormName;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.LAFManager;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.Messages;
import com.servoy.j2db.MessagesResourceBundle;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.SwingModeManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.GlobalEditEvent;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IGlobalEditListener;
import com.servoy.j2db.dataprocessing.IInfoListener;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.gui.CustomColorChooserDialog;
import com.servoy.j2db.gui.GlobalAutoScrollerFocusListener;
import com.servoy.j2db.gui.JDateChooser;
import com.servoy.j2db.gui.JFontChooser;
import com.servoy.j2db.gui.LoginDialog;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.preference.ApplicationPreferences;
import com.servoy.j2db.preference.GeneralPanel;
import com.servoy.j2db.preference.LFPreferencePanel;
import com.servoy.j2db.preference.LocalePreferences;
import com.servoy.j2db.preference.PreferencePanel;
import com.servoy.j2db.preference.ServicePanel;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.RemoteActiveSolutionHandler;
import com.servoy.j2db.smart.cmd.CmdAbout;
import com.servoy.j2db.smart.cmd.CmdBrowseMode;
import com.servoy.j2db.smart.cmd.CmdClose;
import com.servoy.j2db.smart.cmd.CmdCopy;
import com.servoy.j2db.smart.cmd.CmdCut;
import com.servoy.j2db.smart.cmd.CmdDeleteAllRecord;
import com.servoy.j2db.smart.cmd.CmdDeleteRecord;
import com.servoy.j2db.smart.cmd.CmdDuplicateRecord;
import com.servoy.j2db.smart.cmd.CmdExit;
import com.servoy.j2db.smart.cmd.CmdExtendFind;
import com.servoy.j2db.smart.cmd.CmdFindAll;
import com.servoy.j2db.smart.cmd.CmdFindMode;
import com.servoy.j2db.smart.cmd.CmdHelp;
import com.servoy.j2db.smart.cmd.CmdHistoryBack;
import com.servoy.j2db.smart.cmd.CmdHistoryForward;
import com.servoy.j2db.smart.cmd.CmdInvertRecords;
import com.servoy.j2db.smart.cmd.CmdLogout;
import com.servoy.j2db.smart.cmd.CmdManager;
import com.servoy.j2db.smart.cmd.CmdNewRecord;
import com.servoy.j2db.smart.cmd.CmdNextRecord;
import com.servoy.j2db.smart.cmd.CmdOmitRecord;
import com.servoy.j2db.smart.cmd.CmdOpenSolution;
import com.servoy.j2db.smart.cmd.CmdPageSetup;
import com.servoy.j2db.smart.cmd.CmdPaste;
import com.servoy.j2db.smart.cmd.CmdPerformFind;
import com.servoy.j2db.smart.cmd.CmdPrevRecord;
import com.servoy.j2db.smart.cmd.CmdPreviewMode;
import com.servoy.j2db.smart.cmd.CmdReCopyValues;
import com.servoy.j2db.smart.cmd.CmdReduceFind;
import com.servoy.j2db.smart.cmd.CmdSaveData;
import com.servoy.j2db.smart.cmd.CmdSelectAll;
import com.servoy.j2db.smart.cmd.CmdShowOmitRecords;
import com.servoy.j2db.smart.cmd.CmdShowPreferences;
import com.servoy.j2db.smart.cmd.CmdSort;
import com.servoy.j2db.smart.cmd.CmdStopSearchFindAll;
import com.servoy.j2db.smart.cmd.CmdViewAsForm;
import com.servoy.j2db.smart.cmd.CmdViewAsList;
import com.servoy.j2db.smart.cmd.MenuEditAction;
import com.servoy.j2db.smart.cmd.MenuExportAction;
import com.servoy.j2db.smart.cmd.MenuFileAction;
import com.servoy.j2db.smart.cmd.MenuHelpAction;
import com.servoy.j2db.smart.cmd.MenuImportAction;
import com.servoy.j2db.smart.cmd.MenuSelectAction;
import com.servoy.j2db.smart.cmd.MenuViewAction;
import com.servoy.j2db.smart.dataui.DataField;
import com.servoy.j2db.smart.dataui.DataRendererFactory;
import com.servoy.j2db.smart.dataui.DataTextArea;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.smart.plugins.ClientPluginManager;
import com.servoy.j2db.smart.plugins.SmartClientPluginAccessProvider;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Ad;
import com.servoy.j2db.util.BrowserLauncher;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ExtendableURLStreamHandlerFactory;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.SwingHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.ActionCheckBoxMenuItem;
import com.servoy.j2db.util.gui.ActionMenuItem;
import com.servoy.j2db.util.gui.ActionRadioMenuItem;
import com.servoy.j2db.util.gui.IPropertyEditorDialog;
import com.servoy.j2db.util.gui.JDK131ProgressBar;
import com.servoy.j2db.util.gui.JMenuAlwaysEnabled;
import com.servoy.j2db.util.gui.OverlapRepaintManager;
import com.servoy.j2db.util.rmi.IRMIClientSocketFactoryFactory;
import com.servoy.j2db.util.rmi.IReconnectListener;
import com.servoy.j2db.util.toolbar.IToolbarPanel;
import com.servoy.j2db.util.toolbar.Toolbar;
import com.servoy.j2db.util.toolbar.ToolbarButton;
import com.servoy.j2db.util.toolbar.ToolbarPanel;

/**
 * This class is the main entry point and makes the actual swing client application
 *
 * @author jblok
 */
public class J2DBClient extends ClientState
	implements ISmartClientApplication, IGlobalEditListener, IInfoListener, IReconnectListener, IMessagesCallback, IProvideFormName
{
	protected JFrame frame;
	protected JRootPane rootPane; // root pane from applet or frame(above)

	public static final int BUTTON_SPACING = 5;
	public static final int COMPONENT_SPACING = 10;

	public static final int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private ItemFactory itemFactory;
	/**
	 * Some font settings
	 */
	public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10); // SansSerif //$NON-NLS-1$
	public static final Font defaultFont = new Font("SansSerif", Font.PLAIN, 11); // SansSerif //$NON-NLS-1$
	// public Font boldFont = new Font("Dialog", Font.BOLD, 12);
	public static final Font bigFont = new Font("SansSerif", Font.PLAIN, 12); // SansSerif //$NON-NLS-1$

	/**
	 * Managers
	 */
	private CmdManager cmdManager;
	private volatile IBeanManager beanManager;
	private volatile ILAFManager lafManager;
	private RuntimeWindowManager jsWindowManager;

	protected Icon empty;
	private Icon overwrite;
	private Icon insert;
	private Icon editing;
	private Icon transaction;
	private Icon locking;
	private Icon data_change;
	protected JLabel statusLabel;
	private JLabel editLabel;
	private JLabel insertModeLabel;
	private JLabel transactionLabel;
	private JLabel lockLabel;
	private JLabel dataChangeLabel;
	private JLabel sslLabel;

	private JDK131ProgressBar statusProgessBar;
	private JMenuBar menuBar;

	private JMenu import_Menu;
	private JMenu export_Menu;

	private final FlashDataChange flashDataChange = new FlashDataChange();

	private volatile ServoyScheduledExecutor scheduledExecutorService;

	/**
	 * Toolbars
	 */
	protected ToolbarPanel toolbarsPanel;

	/**
	 * Declaration of the Dialogs
	 */
	private ApplicationPreferences ap = null;
	private SelectSolutionDialog selectSolutionDialog = null;
	private IRMIClientSocketFactoryFactory rmiFactoryFactory;

	/**
	 * Decl. of the application name
	 */
	protected static String splashImage = "images/splashclient.png"; //$NON-NLS-1$

	public JMenu getImportMenu()
	{
		return import_Menu;
	}

	public JMenu getExportMenu()
	{
		return export_Menu;
	}

	public JFrame getMainApplicationFrame()
	{
		return frame;
	}

	public String getApplicationName()
	{
		return "Servoy Client"; //$NON-NLS-1$
	}

	public void updateUI(int millisec)
	{
		int remainingTime = millisec;
		FormController currentForm = (FormController)getFormManager().getCurrentForm();
		if (currentForm != null)
		{
			currentForm.getFormUI().updateFormUI();
		}
		long endTime = System.currentTimeMillis() + remainingTime;
		try
		{
			do
			{
				SwingHelper.dispatchEvents(remainingTime);
				remainingTime = (int)(endTime - System.currentTimeMillis());
				if (remainingTime <= 0 || millisec <= 20) // we could do here time <= 0 but if user just wants a short UI update if needed - and not to really waste time in this call he can do updateUI(1 - 20) which will run at most X ms of events but could finish earlier, if there is nothing to be painted...
				{
					break;
				}
				try
				{
					Thread.sleep(Math.min(40, remainingTime));
					remainingTime = (int)(endTime - System.currentTimeMillis());
				}
				catch (InterruptedException e)
				{
					// ignore
				}
			}
			while (remainingTime > 0);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public String getDisplayApplicationName()
	{
		String appName = getApplicationName();
		if (!appName.endsWith("Developer")) //$NON-NLS-1$
		{
			boolean branding = Utils.getAsBoolean(settings.getProperty("servoy.branding", "false")); //$NON-NLS-1$ //$NON-NLS-2$
			String appTitle = settings.getProperty("servoy.branding.windowtitle"); //$NON-NLS-1$
			if (branding && appTitle != null)
			{
				appName = appTitle;
			}
		}
		return appName;
	}

	public int getApplicationType()
	{
		return CLIENT;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getOSName()
	 */
	public String getClientOSName()
	{
		return System.getProperty("os.name"); //$NON-NLS-1$
	}

	public int getClientPlatform()
	{
		return Utils.getPlatform();
	}

	@Override
	protected IActiveSolutionHandler createActiveSolutionHandler()
	{
		return new RemoteActiveSolutionHandler(getApplicationServer(), this);
	}

	@Override
	public IRepository getRepository()
	{
		ClientRepository repo = (ClientRepository)super.getRepository(); // always a non-null ClientRepository, see createRepository()
		if (repo.getDelegate() == null)
		{
			// try connect underlying repository
			IApplicationServerAccess asa = getApplicationServerAccess();
			if (asa != null)
			{
				try
				{
					repo.setDelegate(asa.getRepository());
				}
				catch (RemoteException e)
				{
					Debug.error(e);
				}
			}
		}
		return repo;
	}

	/**
	 * Wrap the repository with a client-side repository wrapper that sets the repository in root objects transmitted over RMI.
	 */
	@Override
	protected IRepository createRepository() throws RemoteException
	{
		IRepository repo = super.createRepository();
		if (repo instanceof ClientRepository)
		{
			return repo;
		}

		// when super createRepo returns null the delegate repository will be updated in getRepository
		return new ClientRepository(repo);
	}

	@Override
	public boolean haveRepositoryAccess()
	{
		// always a non-null ClientRepository, see createRepository()
		return ((ClientRepository)getRepository()).getDelegate() != null;
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor()
	{
		if (scheduledExecutorService == null)
		{
			synchronized (J2DBGlobals.class)
			{
				if (scheduledExecutorService == null)
				{
					scheduledExecutorService = new ServoyScheduledExecutor(2, 7, 4)
					{
						@Override
						protected void beforeExecute(Thread t, Runnable r)
						{
							super.beforeExecute(t, r);
							J2DBGlobals.setServiceProvider(J2DBClient.this);
						}

						@Override
						protected void afterExecute(Runnable r, Throwable t)
						{
							super.afterExecute(r, t);
							J2DBGlobals.setServiceProvider(null);
						}
					};
				}
			}
		}
		return scheduledExecutorService;
	}

	/**
	 * Starting point
	 */
	@SuppressWarnings("nls")
	public static void main(final String[] args)
	{
		String userTimeZone = System.getProperty("user.timezone");
		// Bug around java when downloading the first time (pack or gzip seems to alter the timezone and don't set it back)
		if (userTimeZone != null && !userTimeZone.equals(TimeZone.getDefault().getID()))
		{
			TimeZone timeZone = TimeZone.getTimeZone(userTimeZone);
			if (userTimeZone.equals(timeZone.getID()))
			{
				TimeZone.setDefault(timeZone);
			}
		}


		boolean toggleTracing = false;
		StartupArguments arguments = new StartupArguments(args);
		Iterator<Entry<String, Object>> iterator = arguments.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<String, Object> arg = iterator.next();
			if (arg.getKey().startsWith("system.property."))
			{
				System.setProperty(arg.getKey().substring(16), (String)arg.getValue());
			}
			if (arg.getKey().equals("tracing") && arg.getValue().equals("true"))
			{
				toggleTracing = true;
			}
		}
		if (toggleTracing) Debug.toggleTracing();
		if (Boolean.getBoolean("servoy.usejaas"))
		{
			final boolean[] loginShown = new boolean[1];
			System.setProperty("javax.security.auth.useSubjectCredsOnly", "true");
			try
			{
				Debug.log("creating context");
				LoginContext lc = new LoginContext("ServoyClient", new CallbackHandler()
				{
					public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
					{
						Debug.log("handle call back");
						String loginName = null;
						String passwordString = null;
						for (Callback callback : callbacks)
						{
							if (callback instanceof TextOutputCallback)
							{
								final TextOutputCallback textOutputCallback = (TextOutputCallback)callback;
								switch (textOutputCallback.getMessageType())
								{
									case TextOutputCallback.INFORMATION :
										Debug.log(textOutputCallback.getMessage());
										break;
									case TextOutputCallback.WARNING :
										Debug.warn(textOutputCallback.getMessage());
										break;
									case TextOutputCallback.ERROR :
										Debug.error(textOutputCallback.getMessage());
										break;
									default :
										throw new IOException("Unsupported message type: " + textOutputCallback.getMessageType());
								}
							}
							else if (callback instanceof NameCallback)
							{
								final NameCallback nameCallback = (NameCallback)callback;
								if (loginName == null)
								{
									LoginDialog ld = new LoginDialog((Frame)null, null, "Sign on", false, true);
									Object[] credentials = ld.showDialog(null);
									if (credentials != null && credentials.length == 2)
									{
										loginName = (String)credentials[0];
										passwordString = (String)credentials[1];
									}
									if (loginName == null)
									{
										loginName = "";
										passwordString = "";
									}
									loginShown[0] = true;
								}
								nameCallback.setName(loginName);
							}
							else if (callback instanceof PasswordCallback)
							{
								final PasswordCallback passwordCallback = (PasswordCallback)callback;
								if (passwordString == null)
								{
									LoginDialog ld = new LoginDialog((Frame)null, null, "Sign on", false, true);
									Object[] credentials = ld.showDialog(null);
									if (credentials != null && credentials.length == 2)
									{
										loginName = (String)credentials[0];
										passwordString = (String)credentials[1];
									}
									if (passwordString == null)
									{
										loginName = "";
										passwordString = "";
									}
									loginShown[0] = true;
								}
								passwordCallback.setPassword(passwordString.toCharArray());
							}
							else
							{
								throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
							}
						}
					}
				});
				Debug.log("context created");
				boolean loggedIn = true;
				try
				{
					lc.login();
				}
				catch (LoginException e)
				{
					Debug.log("login failed", e);
					loggedIn = false;
				}

				if (loggedIn)
				{
					Subject.doAsPrivileged(lc.getSubject(), new PrivilegedExceptionAction<Void>()
					{

						public Void run() throws Exception
						{
							mainImpl(args);
							return null;
						}
					}, null);
				}
				else
				{
					mainImpl(args);
				}

			}
			catch (Exception e)
			{
				Debug.log("context creation failed", e);
				if (loginShown[0])
				{
					JOptionPane.showMessageDialog(null, "Couldnt login", "Login failed", JOptionPane.ERROR_MESSAGE);
				}
				mainImpl(args);
			}
		}
		else
		{
			mainImpl(args);
		}
	}

	private static void mainImpl(final String[] args)
	{
		Runnable main = new Runnable()
		{
			public void run()
			{
				// init application
				AbstractBaseQuery.initialize(); // make sure query domain mapping is installed
				J2DBClient base = new J2DBClient();
				base.startupApplication(args);
			}
		};

		//Special flag on request of customers/plugin makers to start a SWT bridge especially for macOSX if class is present
		boolean useDJNativeSwing = Utils.getAsBoolean(System.getProperty("javaws.useDJNativeSwing", "false")); //$NON-NLS-1$ //$NON-NLS-2$
		Class< ? > djNativeSwingClazz = null;
		if (useDJNativeSwing)
		{
			try
			{
				djNativeSwingClazz = Class.forName("chrriis.dj.nativeswing.swtimpl.NativeInterface"); //$NON-NLS-1$
				djNativeSwingClazz.getMethod("initialize").invoke(null); //$NON-NLS-1$
				SwingUtilities.invokeLater(main); // workarround for mac to prevent dead locks with system property -XstartOnFirstThread
				djNativeSwingClazz.getMethod("runEventPump").invoke(null); //$NON-NLS-1$
			}
			catch (Throwable ex)
			{
				Debug.error(ex);
				main.run();
			}
		}
		else
		{
			main.run();
		}
	}

	/**
	 * Main application constructor
	 */
	protected J2DBClient()
	{
		this(true);
	}

	/**
	 * Main application constructor
	 */
	protected J2DBClient(boolean setSingletonServiceProvider)
	{
		super();
		//security check: when run as this class instance it must run under webstart for security!
		if (getClass() == J2DBClient.class && !(WebStart.isRunningWebStart() && WebStart.getWebStartURL() != null))
		{
			throw new IllegalStateException();
		}
		getClientInfo().setApplicationType(getApplicationType());
		if (setSingletonServiceProvider) J2DBGlobals.setSingletonServiceProvider(this);
	}

	protected boolean getAppleScreenMenuBar()
	{
		return true;
	}

	@SuppressWarnings("nls")
	protected void startupApplication(String[] args)
	{
		try
		{
			logStartUp();
			startupArguments = args;
			handleArguments(args);

			// set some props
			// ie. for full gc timing 6 mins (1 minutes == default)
			System.setProperty("sun.rmi.dgc.client.gcInterval", "360000");
			// System.setProperty("java.rmi.server.codebase", "");//disable any rmi classloading
			System.setProperty("apple.laf.useScreenMenuBar", Boolean.toString(getAppleScreenMenuBar()));

			// if no mrj.version (this is in java 1.7), set the mrj.version from java 1.6
			// as MRJAdapter depends on this
			if (Utils.isAppleMacOS() && System.getProperty("mrj.version") == null) System.setProperty("mrj.version", "1070.1.6.0_45-451");

			UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);
			// The "TabbedPane.tabsOpaque" should not be set. If we set it, then the tabs (the little handles
			// that are used for switching between forms) become transparent in some L&F (for example Windows Classic)
			// which is not desired. Also, settting this property does not help with the gray stripe that appears
			// behind the tabs on Windows. That stripe only goes away if the tabpanel is made transparent.
			//UIManager.put("TabbedPane.tabsOpaque", Boolean.FALSE);
			Object originalHighlight = UIManager.get("TabbedPane.highlight");
			if (originalHighlight instanceof Color) UIManager.put("TabbedPane.highlight", ((Color)originalHighlight).darker()); //offset from white a bit since white is most used background

			initSecurityManager();

			initSettings();

			initRMISocketFactory();
			setLookAndFeel();
			createMainPanel();

			applicationSetup();
			applicationInit();
			if (applicationServerInit())
			{
				serverInit();
				selectAndOpenSolution(); // the select dialog is shown.
			}
		}
		catch (Exception ex)
		{
			Debug.error("Fatal Exception");
			Debug.error(ex);
		}
	}

	protected void initSettings() throws Exception
	{
		// overload settings from disk
		boolean uses_client_installer = (System.getProperty("servoy.server_url") != null); //$NON-NLS-1$
		settings = Settings.getInstance();
		if (!uses_client_installer)
		{
			((Settings)settings).loadFromServer(getServerURL()); // no disk loading needed for client, rely on webstart
		}
		else
		{
			//is special case for cytrix users, using a shared disk, to prevent webstarts installs
			File file = new File(System.getProperty("user.dir"), Settings.FILE_NAME); //$NON-NLS-1$
			((Settings)settings).loadFromFile(file);
		}
	}

	protected void initSecurityManager() throws SecurityException
	{
		System.setSecurityManager(null);// seems still needed for javaws(http://forum.java.sun.com/thread.jsp?thread=71233&forum=38&message=507926)
	}

	protected void initStreamHandlerFactory()
	{
		extendableURLStreamHandlerFactory = new ExtendableURLStreamHandlerFactory();
		extendableURLStreamHandlerFactory.addStreamHandler("media", new MediaURLStreamHandler()); //$NON-NLS-1$
		try
		{
			URL.setURLStreamHandlerFactory(extendableURLStreamHandlerFactory);
		}
		catch (Throwable ex)
		{
			Debug.error(ex);
		}
	}

	protected void initRMISocketFactory()
	{
		URL webstartbase = getServerURL();
		String rmiFactory = settings.getProperty("SocketFactory.rmiClientFactory", "com.servoy.j2db.rmi.DefaultClientSocketFactoryFactory"); //$NON-NLS-1$ //$NON-NLS-2$
		try
		{
			Class< ? > cls = Class.forName(rmiFactory.trim());
			Constructor< ? > constructor = cls.getConstructor(
				new Class[] { URL.class, ISmartClientApplication.class, Properties.class, IReconnectListener.class });
			rmiFactoryFactory = (IRMIClientSocketFactoryFactory)constructor.newInstance(new Object[] { webstartbase, this, getSettings(), this });
			Debug.trace("IRMISocketFactoryFactory instantiated: " + cls); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			Debug.error("couldn't instantiate the rmi socketfactory", e); //$NON-NLS-1$
		}
	}

	protected ExtendableURLStreamHandlerFactory extendableURLStreamHandlerFactory;

	protected MainPanel mainPanel;
	private LoadingUIEffects loadingUIEffects;

	protected void createMainPanel()
	{
		// init the frame
		frame = new JFrame();
		frame.addWindowFocusListener(new WindowAdapter()
		{
			@Override
			public void windowGainedFocus(WindowEvent e)
			{
				if (getFormManager() != null) ((FormManager)getFormManager()).setCurrentContainer(null, null);
			}
		});
		rootPane = frame.getRootPane();
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK + InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK), "enabletracing"); //$NON-NLS-1$
		ActionMap am = rootPane.getActionMap();
		am.put("enabletracing", new AbstractAction() //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e)
			{
				Debug.toggleTracing();
			}
		});

		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setName(IApplication.APP_WINDOW_NAME);

		frame.getContentPane().setLayout(new BorderLayout());
		frame.setTitle(getDisplayApplicationName());

		String branding = getSettings().getProperty("servoy.branding", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		String windowicon = getSettings().getProperty("servoy.branding.windowicon"); //$NON-NLS-1$
		if (branding.equals("true") && windowicon != null && Utils.isSwingClient(getApplicationType())) //$NON-NLS-1$
		{
			frame.setIconImage(getWindowIcon(windowicon));
		}
		else
		{
			frame.setIconImage(loadImage("windowicon.png").getImage()); //$NON-NLS-1$
		}

		mainPanel = new MainPanel(this, null);
		mainPanel.setPreferredSize(new Dimension(Settings.INITIAL_CLIENT_WIDTH, Settings.INITIAL_CLIENT_HEIGHT));
		frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

		frame.pack();
		Debug.trace("Main frame packed"); //$NON-NLS-1$

		// block when visible
		Component glassPane = rootPane.getGlassPane();
		glassPane.addMouseListener(new MouseAdapter()
		{
		});
		glassPane.addMouseMotionListener(new MouseMotionAdapter()
		{
		});
		glassPane.addKeyListener(new KeyAdapter()
		{
		});
	}

	protected Image getWindowIcon(String windowicon)
	{
		URL webstartUrl = getServerURL();
		try
		{
			String windowiconFile = null;
			String path = webstartUrl.getPath();
			if (!path.equals("") && path.endsWith("/"))
			{
				windowiconFile = path.substring(0, path.length() - 1) + windowicon;
			}
			else windowiconFile = windowicon;

			URL url = new URL(webstartUrl.getProtocol(), webstartUrl.getHost(), webstartUrl.getPort(), windowiconFile);
			return new ImageIcon(url).getImage();
		}
		catch (MalformedURLException ex)
		{
			Debug.error("Error loading the window icon image", ex); //$NON-NLS-1$
			return loadImage("windowicon.png").getImage(); //$NON-NLS-1$
		}
	}

	public void addURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		if (protocolName != null && !"http".equals(protocolName) && !"media".equals(protocolName)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			extendableURLStreamHandlerFactory.addStreamHandler(protocolName, handler);
		}
	}

	protected void installShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				if (isConnected() && frame != null)
				{
					Debug.error("Client closes abnormally, trying to unbind the client from the server'"); //$NON-NLS-1$

					//de register myself
					try
					{
						unRegisterClient(getClientID());
						unBindUserClient();
					}
					catch (Exception e)
					{
						Debug.error(e);// incase server is dead
					}
				}
			}
		});
	}

	@Override
	protected void applicationSetup()
	{
		installShutdownHook();

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(new GlobalAutoScrollerFocusListener());

		// create status
		JPanel statusPanel = createStatusPanel();
		String showStatusBar = settings.getProperty("servoy.smartclient.showStatusBar"); //$NON-NLS-1$
		if (showStatusBar != null && showStatusBar.equals("false")) statusPanel.setVisible(false); //$NON-NLS-1$
		mainPanel.add(statusPanel, BorderLayout.SOUTH);

		// first set the default locale if set.
		String str = getSettings().getProperty("locale.default"); //$NON-NLS-1$
		Locale loc = PersistHelper.createLocale(str);
		if (loc != null)
		{
			Locale.setDefault(loc);
		}
		TimeZone defaultTimeZone = TimeZone.getDefault();
		if (defaultTimeZone != null) //can this happen?
		{
			str = getSettings().getProperty("timezone.default", defaultTimeZone.getID()); //$NON-NLS-1$
			TimeZone tz = TimeZone.getTimeZone(str);
			if (tz != null)
			{
				getClientInfo().setTimeZone(tz);
				TimeZone.setDefault(tz);
			}
		}

		super.applicationSetup();

		jsWindowManager = createJSWindowManager();

		cmdManager = createCmdManager();

		// load all the actions
		Map<String, Action> actions = getActions();

		// load the toolbars
		toolbarsPanel = new ToolbarPanel(Settings.INITIAL_CLIENT_WIDTH - 200);
		mainPanel.add(toolbarsPanel, BorderLayout.NORTH);
		fillToolbar(actions);
		String showToolBar = settings.getProperty("servoy.smartclient.showToolBar"); //$NON-NLS-1$
		if (showToolBar != null && showToolBar.equals("false")) toolbarsPanel.setVisible(false); //$NON-NLS-1$

		// load menu
		JMenuBar menu = createMenuBar(actions);
		String showMenuBar = settings.getProperty("servoy.smartclient.showMenuBar"); //$NON-NLS-1$
		if (showMenuBar != null && showMenuBar.equals("false")) menu.setVisible(false); //$NON-NLS-1$
		frame.setJMenuBar(menu);
		((SwingRuntimeWindow)jsWindowManager.getWindow(null)).setJMenuBar(menu);
		if (Utils.isAppleMacOS())
		{
			attachAppleMenu(actions);
		}

		setMainFrameInitialBounds();
		showSolutionLoading(true);
	}

	protected LoadingUIEffects createLoadingUIEffects()
	{
		return new LoadingUIEffects(this, mainPanel);
	}

	@Override
	public void showSolutionLoading(boolean loading)
	{
		getLoadingUIEffects().showSolutionLoading(loading);
	}

	protected LoadingUIEffects getLoadingUIEffects()
	{
		if (loadingUIEffects == null) loadingUIEffects = createLoadingUIEffects();
		return loadingUIEffects;
	}

	/**
	 * @param actions
	 */
	protected void fillToolbar(Map<String, Action> actions)
	{
		toolbarsPanel.clear();

		SortedList<Toolbar.ToolbarKey> sortedList = new SortedList<Toolbar.ToolbarKey>();
		createToolBars(sortedList, actions);

		int offsetRow = 0;
		if (sortedList.size() > 0)
		{
			offsetRow = sortedList.get(0).getRow();
			if (offsetRow == -1) offsetRow = 0;
		}

		for (int i = 0; i < sortedList.size(); i++)
		{
			Toolbar.ToolbarKey key = sortedList.get(i);

			if (toolbarsPanel.getToolBar(key.getToolbar().getName()) == null)
			{
				if ("edit".equals(key.getToolbar().getName())) //$NON-NLS-1$
				{
					fillBrowseToolbar(key.getToolbar(), actions);
				}
				toolbarsPanel.addToolbar(key.getToolbar(), key.getRow() - offsetRow);
				toolbarsPanel.setToolbarVisible(key.getToolbar().getName(), key.isVisible());
			}
		}
	}

	protected void setMainFrameInitialBounds()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (!Settings.getInstance().loadBounds(frame))
				{
					Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
					frame.setLocation(screenSize.width / 2 - Settings.INITIAL_CLIENT_WIDTH / 2, screenSize.height / 2 - Settings.INITIAL_CLIENT_HEIGHT / 2);
				}
			}
		});
	}

	@Override
	protected boolean applicationInit()
	{
		try
		{
			blockGUI(Messages.getString("servoy.client.status.application.setup")); //$NON-NLS-1$

			initStreamHandlerFactory();

			super.applicationInit();

			// repaint manager that handles repaint for overlapping components properly (if a component
			// below other components is repainted, the components on top of it will be repainted too)
			RepaintManager current = RepaintManager.currentManager(frame);
			if (!(current instanceof OverlapRepaintManager))
			{
				if (current != null && current.getClass() != RepaintManager.class)
				{
					Debug.log("Overwriting a none default RepaintManager: " + current.getClass() + " with our overlay repaint manager");
				}
				RepaintManager.setCurrentManager(new OverlapRepaintManager());
			}

			// Add the windows listener
			WindowListener l = new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					shutDown(false);
				}
			};
			frame.addWindowListener(l);

			// register top level keystrokes
			registerKeyStrokes(rootPane);

			return true;
		}
		finally
		{
			releaseGUI();
		}
	}

	protected ILAFManager createLAFManager()
	{
		return new LAFManager();
	}

	@Override
	protected void createPluginManager()
	{
		pluginManager = new ClientPluginManager(this);
		pluginAccess = createClientPluginAccessProvider();

		getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				getPluginManager().init();
				((PluginManager)getPluginManager()).initClientPlugins(J2DBClient.this, (IClientPluginAccess)getPluginAccess());
				((FoundSetManager)getFoundSetManager()).setColumnManangers(getPluginManager().getColumnValidatorManager(),
					getPluginManager().getColumnConverterManager(), getPluginManager().getUIConverterManager());
			}
		});
	}

	protected SmartClientPluginAccessProvider createClientPluginAccessProvider()
	{
		return new SmartClientPluginAccessProvider(this);
	}

	protected IBeanManager createBeanManager()
	{
		return new BeanManager();
	}

	protected CmdManager createCmdManager()
	{
		return new CmdManager(this);
	}

	@Override
	protected IModeManager createModeManager()
	{
		return new SwingModeManager(this);
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		return new SwingFormManager(this, mainPanel);
	}

	protected void createToolBars(SortedList<Toolbar.ToolbarKey> list, Map<String, Action> actions)
	{
		int editPlace = Integer.parseInt(settings.getProperty("toolbar.edit.row", "0")); //$NON-NLS-1$ //$NON-NLS-2$
		int editIndex = Integer.parseInt(settings.getProperty("toolbar.edit.row.index", "0")); //$NON-NLS-1$ //$NON-NLS-2$
		boolean editVisible = settings.getProperty("toolbar.edit", "true").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		int textPlace = Integer.parseInt(settings.getProperty("toolbar.text.row", "0")); //$NON-NLS-1$ //$NON-NLS-2$
		int textIndex = Integer.parseInt(settings.getProperty("toolbar.text.row.index", "1")); //$NON-NLS-1$ //$NON-NLS-2$
		boolean textVisible = settings.getProperty("toolbar.text", "true").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Toolbar browseToolbar = new Toolbar("edit", Messages.getString("servoy.edittoolbar.label"), true); //$NON-NLS-1$//$NON-NLS-2$

		list.add(new Toolbar.ToolbarKey(editPlace, editIndex, editVisible, browseToolbar));
		list.add(new Toolbar.ToolbarKey(textPlace, textIndex, textVisible, new TextToolbar(this, actions)));
	}

	public IToolbarPanel getToolbarPanel()
	{
		return toolbarsPanel;
	}

	public JComponent getEditLabel()// used for rendering by printing
	{
		return editLabel;
	}

	/**
	 * updates the insert mode icon for the given display
	 *
	 * @param display
	 */
	public void updateInsertModeIcon(IDisplay display)
	{
		Icon icon = empty;
		if (display instanceof DataField)
		{
			DataField field = (DataField)display;
			AbstractFormatterFactory formatterFactory = field.getFormatterFactory();
			if (formatterFactory instanceof DefaultFormatterFactory)
			{
				DefaultFormatterFactory factory = ((DefaultFormatterFactory)formatterFactory);
				AbstractFormatter editFormatter = factory.getEditFormatter();
				if (editFormatter == null) editFormatter = factory.getDefaultFormatter();
				if (editFormatter instanceof DefaultFormatter && !(editFormatter instanceof MaskFormatter))
				{
					if (((DefaultFormatter)editFormatter).getOverwriteMode())
					{
						icon = overwrite;
					}
					else
					{
						icon = insert;
					}
				}
			}
		}
		else if (display instanceof DataTextArea)
		{
			if (((DataTextArea)display).getOverwriteMode())
			{
				icon = overwrite;
			}
			else
			{
				icon = insert;
			}
		}
		insertModeLabel.setIcon(icon);
	}


	protected JPanel createStatusPanel()
	{
		Color darkShadow = UIManager.getColor("controlShadow"); //$NON-NLS-1$
		Color lightShadow = UIManager.getColor("controlLtHighlight"); //$NON-NLS-1$

		JPanel status = new JPanel();
		status.setName("statusbar"); //$NON-NLS-1$
		Border border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, lightShadow, status.getBackground(), darkShadow, status.getBackground());

		// set the status
		statusLabel = new JLabel();
		// statusLabel.setFont(smallFont);
		statusLabel.setText(Messages.getString("servoy.general.status.ready")); //$NON-NLS-1$
		statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		statusLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 2, 0, 0)));// BorderFactory.createEtchedBorder());
		statusLabel.setMinimumSize(new Dimension(100, 18));
		statusLabel.setPreferredSize(new Dimension(4000, 18));

		empty = loadImage("empty.gif"); //$NON-NLS-1$
		editing = loadImage("editing.gif"); //$NON-NLS-1$
		overwrite = loadImage("overwrite.png"); //$NON-NLS-1$
		insert = loadImage("insert.png"); //$NON-NLS-1$
		transaction = loadImage("transaction.gif"); //$NON-NLS-1$
		locking = loadImage("lock.gif"); //$NON-NLS-1$
		data_change = loadImage("data_change.gif"); //$NON-NLS-1$

		sslLabel = new JLabel(empty, SwingConstants.TRAILING);
		// sslLabel.setFont(smallFont);
		sslLabel.setToolTipText(Messages.getString("servoy.client.ssllabel.tooltip")); //$NON-NLS-1$
		sslLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		sslLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		if (rmiFactoryFactory != null && rmiFactoryFactory.usingSSL())
		{
			sslLabel.setText("SSL"); //$NON-NLS-1$
			sslLabel.setIcon(null);
			sslLabel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 2, 0, 0)));// BorderFactory.createEtchedBorder());
		}
		else
		{
			sslLabel.setMinimumSize(new Dimension(18, 18));
			sslLabel.setPreferredSize(new Dimension(18, 18));
			sslLabel.setBorder(border);// BorderFactory.createEtchedBorder());
		}

		insertModeLabel = new JLabel(empty, SwingConstants.TRAILING);
		insertModeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		insertModeLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		insertModeLabel.setBorder(border);// BorderFactory.createEtchedBorder());
		insertModeLabel.setMinimumSize(new Dimension(18, 18));
		insertModeLabel.setPreferredSize(new Dimension(18, 18));
		editLabel = new JLabel(empty, SwingConstants.TRAILING);
		editLabel.setToolTipText(Messages.getString("servoy.client.editlabel.tooltip")); //$NON-NLS-1$
		editLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		editLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		editLabel.setBorder(border);// BorderFactory.createEtchedBorder());
		editLabel.setMinimumSize(new Dimension(18, 18));
		editLabel.setPreferredSize(new Dimension(18, 18));
		transactionLabel = new JLabel(empty, SwingConstants.TRAILING);
		transactionLabel.setToolTipText(Messages.getString("servoy.client.transactionlabel.tooltip")); //$NON-NLS-1$
		transactionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		transactionLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		transactionLabel.setBorder(border);// BorderFactory.createEtchedBorder());
		transactionLabel.setMinimumSize(new Dimension(18, 18));
		transactionLabel.setPreferredSize(new Dimension(18, 18));
		lockLabel = new JLabel(empty, SwingConstants.TRAILING);
		lockLabel.setToolTipText(Messages.getString("servoy.client.locklabel.tooltip")); //$NON-NLS-1$
		lockLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		lockLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		lockLabel.setBorder(border);// BorderFactory.createEtchedBorder());
		lockLabel.setMinimumSize(new Dimension(18, 18));
		lockLabel.setPreferredSize(new Dimension(18, 18));
		dataChangeLabel = new JLabel(empty, SwingConstants.TRAILING);
		dataChangeLabel.setToolTipText(Messages.getString("servoy.client.datachangelabel.tooltip")); //$NON-NLS-1$
		dataChangeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		dataChangeLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		dataChangeLabel.setBorder(border);// BorderFactory.createEtchedBorder());
		dataChangeLabel.setMinimumSize(new Dimension(18, 18));
		dataChangeLabel.setPreferredSize(new Dimension(18, 18));

		statusProgessBar = new JDK131ProgressBar();
		statusProgessBar.setMaximumSize(new Dimension(100, 18));
		statusProgessBar.setPreferredSize(new Dimension(100, 18));
		statusProgessBar.setMinimumSize(new Dimension(100, 18));

		statusProgessBar.setBorder(border);// BorderFactory.createEtchedBorder());
		statusProgessBar.setStringPainted(false);
		statusProgessBar.setAlignmentX(Component.RIGHT_ALIGNMENT);
		statusProgessBar.setAlignmentY(Component.BOTTOM_ALIGNMENT);

		status.setLayout(new BoxLayout(status, BoxLayout.X_AXIS));
		status.add(statusLabel);

		status.add(editLabel);
		status.add(insertModeLabel);
		status.add(transactionLabel);
		status.add(lockLabel);
		status.add(dataChangeLabel);
		status.add(sslLabel);

		status.add(statusProgessBar);
		status.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		return status;
	}

	private void registerKeyStrokes(JRootPane rp)
	{

		ActionListener actionListener2 = new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				// Debug.showConsole();
			}
		};
		KeyStroke stroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK);
		rp.registerKeyboardAction(actionListener2, stroke2, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	@Override
	public void shutDown(boolean force)
	{
		// hide
		try
		{
			if (getSolution() != null)
			{
				if (!closeSolution(force, null) && !force) return;
//				getFlattenedSolution().setSolution(null);
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		if (toolbarsPanel.getToolBar("edit") != null) //$NON-NLS-1$
		{
			settings.setProperty("toolbar.edit", toolbarsPanel.getToolBar("edit").isVisible() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			settings.setProperty("toolbar.edit.row", Integer.toString(toolbarsPanel.getToolBarRow("edit"))); //$NON-NLS-1$ //$NON-NLS-2$
			settings.setProperty("toolbar.edit.row.index", Integer.toString(toolbarsPanel.getToolbarRowIndex("edit"))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (toolbarsPanel.getToolBar("text") != null) //$NON-NLS-1$
		{
			settings.setProperty("toolbar.text", toolbarsPanel.getToolBar("text").isVisible() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			settings.setProperty("toolbar.text.row", Integer.toString(toolbarsPanel.getToolBarRow("text"))); //$NON-NLS-1$ //$NON-NLS-2$
			settings.setProperty("toolbar.text.row.index", Integer.toString(toolbarsPanel.getToolbarRowIndex("text"))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (frame != null)
		{
			frame.setVisible(false);
		}

		super.shutDown(force);

		if (scheduledExecutorService != null)
		{
			scheduledExecutorService.shutdownNow();
			scheduledExecutorService = null;
		}

		if (rmiFactoryFactory != null)
		{
			rmiFactoryFactory.close();
		}

		//clear the frame
		if (frame != null)
		{
			frame.dispose();
			frame = null;
		}
		invokeLater(new Runnable()
		{
			public void run()
			{
				// Exit really hard here!!
				exitHard(0);
			}
		});
	}

	protected void exitHard(int status)
	{
		System.exit(status);
	}

	@Override
	protected void saveSettings()
	{
		try
		{
			Settings.getInstance().saveBounds(frame);
			((Settings)settings).save();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public ImageIcon loadImage(String name)
	{
		java.net.URL iconUrl = ClientState.class.getResource("images/" + name); //$NON-NLS-1$
		if (iconUrl != null)
		{
			return new ImageIcon(iconUrl, iconUrl.toExternalForm().intern());
		}
		else
		{
			java.net.URL iconUrl2 = J2DBClient.class.getResource("images/error.gif"); //$NON-NLS-1$
			if (iconUrl2 != null)
			{
				return new ImageIcon(iconUrl2, iconUrl2.toExternalForm().intern());
			}
			else
			{
				return null;
			}
		}
	}

	public ILAFManager getLAFManager()
	{
		return lafManager;
	}

	/**
	 * Set the look and feel (platform dep. or indep.)
	 */
	@SuppressWarnings("nls")
	protected void setLookAndFeel()
	{
		try
		{
			String allowLAFWindowDecoration = settings.getProperty("servoy.smartclient.allowLAFWindowDecoration");
			if (allowLAFWindowDecoration != null && allowLAFWindowDecoration.equals("true"))
			{
				//Allow LAF's to control the JFrame and JDialog chrome
				JFrame.setDefaultLookAndFeelDecorated(true);
				JDialog.setDefaultLookAndFeelDecorated(true);

				//Settings aboive might cause flicker while resizing, see:
				//https://substance.dev.java.net/docs/faq.html, question 17
				//http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5079688
				if (Utils.getPlatform() == Utils.PLATFORM_WINDOWS && System.getProperty("sun.awt.noerasebackground") == null)
				{
					System.setProperty("sun.awt.noerasebackground", "true");
				}
			}

			// in case we use alloy
			System.setProperty("alloy.isLookAndFeelFrameDecoration", "true");

			String defaultLAFClassName = UIManager.getSystemLookAndFeelClassName();
			String lnf = settings.getProperty("selectedlnf", defaultLAFClassName);

			boolean isRunningWebStart = WebStart.isRunningWebStart();
			URL webstartbase = null;
			if (isRunningWebStart)
			{
				webstartbase = getServerURL();
				lnf = settings.getProperty(webstartbase.getHost() + webstartbase.getPort() + "_selectedlnf", lnf);
			}

			// Users may have set the lnf to spaces in the properties file
			if (lnf.trim().length() == 0)
			{
				lnf = defaultLAFClassName;
			}

			lafManager = createLAFManager();

			lafManager.init();

			// test if selected lnf is loaded through the lafManager
			List<LookAndFeelInfo> lst = lafManager.getLAFInfos(this);
			boolean found = false;
			for (int i = 0; i < lst.size(); i++)
			{
				UIManager.LookAndFeelInfo info = lst.get(i);
				if (info.getClassName().equals(lnf))
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				lnf = defaultLAFClassName;
			}

			putClientProperty(LookAndFeelInfo.class.getName(), lnf);


			String font = settings.getProperty("font");
			if (isRunningWebStart)
			{
				font = settings.getProperty(webstartbase.getHost() + webstartbase.getPort() + "_font", font);
			}
			Font dfltFont = PersistHelper.createFont(font);
			if (dfltFont == null)
			{
				if (!Utils.isAppleMacOS())
				{
					Font fnt = (Font)UIManager.getDefaults().get("MenuItem.font");
					if (fnt != null)
					{
						dfltFont = fnt;
						if (dfltFont.isBold()) dfltFont = dfltFont.deriveFont(Font.PLAIN);
					}
					else
					{
						dfltFont = PersistHelper.createFont("Tahoma", Font.PLAIN, 11);
					}
				}
			}
			replaceCtrlShortcutsWithMacShortcuts();

			if (dfltFont != null) putClientProperty(Font.class.getName(), dfltFont);

		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	protected void replaceCtrlShortcutsWithMacShortcuts()
	{
		if (Utils.isAppleMacOS() && UIManager.getLookAndFeel().getClass().getName().toUpperCase().indexOf("AQUA") < 0)
		{
			for (Object keyObj : UIManager.getLookAndFeelDefaults().keySet())
			{
				String key = keyObj.toString();

				if (key.contains("InputMap"))
				{
					Object val = UIManager.getLookAndFeelDefaults().get(key);

					if (val instanceof InputMapUIResource)
					{
						InputMapUIResource map = (InputMapUIResource)val;
						KeyStroke[] allKeys = map.allKeys();
						if (allKeys != null) for (KeyStroke keyStroke : allKeys)
						{
							int modifiers = keyStroke.getModifiers();

							if ((modifiers & InputEvent.CTRL_MASK) > 0)
							{
								modifiers -= InputEvent.CTRL_DOWN_MASK;
								modifiers -= InputEvent.CTRL_MASK;
								modifiers += InputEvent.META_DOWN_MASK + InputEvent.META_MASK;

								KeyStroke k = KeyStroke.getKeyStroke(keyStroke.getKeyCode(), modifiers);

								if (map.get(k) == null)
								{
									Object mapVal = map.get(keyStroke);
									map.remove(keyStroke);
									map.put(k, mapVal);
								}
							}

						}
					}
				}
			}
		}
	}

	public String getUserProperty(String name)
	{
		if (name == null) return null;
		return ((Settings)getSettings()).getUserProperty(Settings.USER, name);
	}

	public String[] getUserPropertyNames()
	{
		List<String> retval = new ArrayList<String>();
		Iterator<Object> it = getSettings().keySet().iterator();
		while (it.hasNext())
		{
			String key = (String)it.next();
			if (key.startsWith(Settings.USER))
			{
				retval.add(key.substring(Settings.USER.length()));
			}
		}
		return retval.toArray(new String[retval.size()]);
	}

	public void setUserProperty(String name, String value)
	{
		if (name == null) return;
		((Settings)getSettings()).setUserProperty(Settings.USER, name, value);
	}

	public Object getClientProperty(Object name)
	{
		if (name == null) return null;
		if (LookAndFeelInfo.class.getName().equals(name))
		{
			LookAndFeel lnf = UIManager.getLookAndFeel();
			return lnf == null ? null : lnf.getClass().getName();
		}
		else if (USE_SYSTEM_PRINT_DIALOG.equals(name))
		{
			return getSettings().get(USE_SYSTEM_PRINT_DIALOG);
		}
		else if (TOOLTIP_INITIAL_DELAY.equals(name))
		{
			return new Integer(ToolTipManager.sharedInstance().getInitialDelay());
		}
		else if (TOOLTIP_DISMISS_DELAY.equals(name))
		{
			return new Integer(ToolTipManager.sharedInstance().getDismissDelay());
		}
		else
		{
			UIDefaults uiDefaults = UIManager.getDefaults();
			if (Font.class.getName().equals(name))
			{
				Object f = uiDefaults.get("MenuItem.font"); //$NON-NLS-1$
				if (f == null) return null;
				if (f != uiDefaults.get("Menu.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("RadioButtonMenuItem.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("CheckBoxMenuItem.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("ComboBox.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("RadioButton.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("CheckBox.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("Button.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("Label.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("TabbedPane.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("Panel.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("TitledBorder.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("List.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("Table.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("TableHeader.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("Tree.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("TextArea.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("PasswordField.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("TextField.font")) return null; //$NON-NLS-1$
				if (f != uiDefaults.get("FormattedTextField.font")) return null; //$NON-NLS-1$
				return f;
			}
			else
			{
				return uiDefaults.get(name.toString());
			}
		}
	}

	public boolean putClientProperty(Object name, Object value)
	{
		if (name == null) return false;
		try
		{
			boolean mustSetFont = true;
			if (LookAndFeelInfo.class.getName().equals(name))
			{
				LookAndFeel laf = lafManager.createInstance(value.toString());
				if (laf != null)
				{
					if (laf instanceof MetalLookAndFeel)
					{
						UIManager.setLookAndFeel(laf);
						String themeName = getSettings().getProperty("lnf.theme", "com.servoy.j2db.util.gui.DefaultTheme"); //$NON-NLS-1$ //$NON-NLS-2$
						if (WebStart.isRunningWebStart())
						{
							URL webstartbase = getServerURL();
							themeName = settings.getProperty(webstartbase.getHost() + webstartbase.getPort() + "_lnf.theme", themeName); //$NON-NLS-1$
						}

						if (themeName != null && themeName.length() != 0)
						{
							MetalTheme theme = lafManager.createThemeInstance(themeName);
							if (theme != null)
							{
								MetalLookAndFeel.setCurrentTheme(theme);
								mustSetFont = false;
							}
						}
					}
					UIManager.setLookAndFeel(laf);// yes, this is the second time if there is a methalTHeme but this is only it works
					replaceCtrlShortcutsWithMacShortcuts();
				}
			}

			UIDefaults uiDefaults = UIManager.getDefaults();
			if (Font.class.getName().equals(name) && mustSetFont)
			{
				Font font = (Font)value;
				if (font != null && !(font instanceof FontUIResource))
				{
					font = new FontUIResource(font);
				}

				uiDefaults.put("MenuItem.font", font); //$NON-NLS-1$
				uiDefaults.put("Menu.font", font); //$NON-NLS-1$
				uiDefaults.put("RadioButtonMenuItem.font", font); //$NON-NLS-1$
				uiDefaults.put("CheckBoxMenuItem.font", font); //$NON-NLS-1$
				uiDefaults.put("ComboBox.font", font); //$NON-NLS-1$
				uiDefaults.put("RadioButton.font", font); //$NON-NLS-1$
				uiDefaults.put("CheckBox.font", font); //$NON-NLS-1$
				uiDefaults.put("Button.font", font); //$NON-NLS-1$
				uiDefaults.put("Label.font", font); // Was BIG_FONT //$NON-NLS-1$
				uiDefaults.put("TabbedPane.font", font); //$NON-NLS-1$
				uiDefaults.put("Panel.font", font); //$NON-NLS-1$
				uiDefaults.put("TitledBorder.font", font); //$NON-NLS-1$
				uiDefaults.put("List.font", font); //$NON-NLS-1$
				uiDefaults.put("Table.font", font); //$NON-NLS-1$
				uiDefaults.put("TableHeader.font", font); //$NON-NLS-1$
				uiDefaults.put("Tree.font", font); //$NON-NLS-1$
				uiDefaults.put("TextArea.font", font); //$NON-NLS-1$
				uiDefaults.put("PasswordField.font", font); //$NON-NLS-1$
				uiDefaults.put("TextField.font", font); //$NON-NLS-1$
				uiDefaults.put("FormattedTextField.font", font); //$NON-NLS-1$
			}
			else if (LookAndFeelInfo.class.getName().equals(name) && frame != null)
			{
				uiDefaults.put("ToolTip.hideAccelerator", Boolean.TRUE); //$NON-NLS-1$
				ToolTipManager.sharedInstance().setDismissDelay(8000);
				SwingUtilities.updateComponentTreeUI(frame);
				if (ap != null)
				{
					SwingUtilities.updateComponentTreeUI(ap);
				}

				Iterator<Window> windows = dialogs.values().iterator();
				while (windows.hasNext())
				{
					Window window = windows.next();
					SwingUtilities.updateComponentTreeUI(window);
				}
			}
			else if (USE_SYSTEM_PRINT_DIALOG.equals(name))
			{
				getSettings().put(USE_SYSTEM_PRINT_DIALOG, value.toString());
			}
			else if (TOOLTIP_INITIAL_DELAY.equals(name))
			{
				ToolTipManager.sharedInstance().setInitialDelay(((Number)value).intValue());
			}
			else if (TOOLTIP_DISMISS_DELAY.equals(name))
			{
				ToolTipManager.sharedInstance().setDismissDelay(((Number)value).intValue());
			}
			else
			{
				uiDefaults.put(name.toString(), value);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
		return true;
	}

	protected RuntimeWindowManager createJSWindowManager()
	{
		return new SwingRuntimeWindowManager(this);
	}

	public RuntimeWindowManager getRuntimeWindowManager()
	{
		return jsWindowManager;
	}

	/**
	 * Show the help private void showAppHelp() { // try // { // if (hb == null) // { // URL hsURL = new //
	 * URL(WebStart.getWebStartURL(),"docs/help/client.hs");//developer/server.hs? // HelpSet hs = new HelpSet(null, hsURL); // hb = hs.createHelpBroker(); // }
	 * // hb.setDisplayed(true); // } // catch (Exception ex) // { // Debug.error("Help not found\n" + ex); // return; // } }
	 */

	public IBeanManager getBeanManager()
	{
		if (beanManager == null)
		{
			beanManager = createBeanManager();
		}
		return beanManager;
	}

	public ICmdManager getCmdManager()
	{
		return cmdManager;
	}

	@Override
	protected void registerListeners()
	{
		// Note:add order is important

		// 1
		J2DBGlobals.addPropertyChangeListener(this, cmdManager); // register
		J2DBGlobals.addPropertyChangeListener(modeManager, cmdManager); // register

		// 2
		super.registerListeners();
	}

	@Override
	protected boolean startApplicationServerConnection()
	{
		try
		{
			applicationServer = connectApplicationServer();
		}
		catch (Exception ex)
		{
			reportError(Messages.getString("servoy.client.error.finding.dataservice"), ex); //$NON-NLS-1$
		}
		return applicationServer != null;
	}

	protected IApplicationServer connectApplicationServer() throws Exception
	{
		String name = IApplicationServer.class.getName();

		String host = getServerURL().getHost();
		int port = Utils.getAsInteger(settings.getProperty("usedRMIRegistryPort")); //$NON-NLS-1$
		try
		{
			if (register == null)
			{
				register = LocateRegistry.getRegistry(host, port, rmiFactoryFactory.getRemoteClientSocketFactory());
			}
			return (IApplicationServer)register.lookup(name);
		}
		catch (Exception e)
		{
			Debug.error("Error getting the service " + name + " from host " + host + ':' + port, e); //$NON-NLS-1$ //$NON-NLS-2$
			throw e;
		}
	}


	private Registry register;

	// solution loading monitor
	protected boolean solutionLoading;
	protected Object solutionLoadingMutex = new Object();

	@Override
	protected SolutionMetaData selectSolutionToLoad() throws RepositoryException
	{
		if (getSolution() != null)
		{
			int x = JOptionPane.showConfirmDialog(frame, Messages.getString("servoy.client.message.closeopensolution"), //$NON-NLS-1$
				Messages.getString("servoy.general.confirm"), //$NON-NLS-1$
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			if (x != JOptionPane.OK_OPTION)
			{
				return null;
			}
			closeSolution(false, null);
			// always return, because openSolutionDialog method is called in closeSolution
			return null;
		}

		SwingHelper.dispatchEvents(100);// hide menu

		return super.selectSolutionToLoad();
	}

	@Override
	public IFormManagerInternal getFormManager()
	{
		return (IFormManagerInternal)super.getFormManager();
	}

	@Override
	protected void loadSolution(final SolutionMetaData solutionMeta) throws RepositoryException
	{
		// regular solution
		showSolutionLoading(true);
		invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					blockGUI(Messages.getString("servoy.client.status.loadingsolution", new Object[] { solutionMeta.getName() })); //$NON-NLS-1$
					loadSolutionsAndModules(solutionMeta);
				}
				catch (final Exception ex)
				{
					showSolutionLoading(false);
					invokeLater(new Runnable()
					{
						public void run()
						{
							reportError(Messages.getString("servoy.client.error.opensolution"), ex); //$NON-NLS-1$
						}
					});
				}
				finally
				{
					getScriptEngine();
					releaseGUI();
				}
			}
		});

	}

	@Override
	protected SolutionMetaData showSolutionSelection(SolutionMetaData[] solutions)
	{
		if (selectSolutionDialog == null)
		{
			selectSolutionDialog = new SelectSolutionDialog(this);
		}
		showSolutionLoading(true);
		SolutionMetaData tmp = null;
		try
		{
			tmp = selectSolutionDialog.showDialog(solutions);
		}
		finally
		{
			if (tmp == null) showSolutionLoading(false);
		}
		return tmp;
	}

	@Override
	protected int getSolutionTypeFilter()
	{
		return super.getSolutionTypeFilter() | SolutionMetaData.SMART_CLIENT_ONLY;
	}

	@Override
	public boolean loadSolutionsAndModules(SolutionMetaData s)
	{
		boolean value = super.loadSolutionsAndModules(s);
		if (getSolution() != null)
		{
			OrientationApplier.setOrientationToAWTComponent(frame, getLocale(), getSolution().getTextOrientation());
		}
		return value;
	}

	@Override
	protected void solutionLoaded(final Solution solution)
	{
		super.solutionLoaded(solution);
		invokeLater(new Runnable()
		{
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run()
			{
				try
				{
					blockGUI(Messages.getString("servoy.client.status.initializing.solution")); //$NON-NLS-1$
					setTitle(""); //$NON-NLS-1$
					J2DBGlobals.firePropertyChange(J2DBClient.this, "solution", null, solution); //$NON-NLS-1$
					handleArguments(null); // clear the loaded solution names.
				}
				finally
				{
					releaseGUI();
				}
			}
		});
	}

	@Override
	public boolean saveSolution()
	{
		// not impleneted in this class
		return true;
	}

	public void setTitle(String name)
	{
		String title = ""; //$NON-NLS-1$
		String solutionTitle = getSolution().getTitleText();

		if (solutionTitle == null)
		{
			title = getSolution().getName();
		}
		else if (!solutionTitle.equals("<empty>")) //$NON-NLS-1$
		{
			title = solutionTitle;
		}

		title = getI18NMessageIfPrefixed(title);

		if (name != null && !name.trim().equals("") && !"<empty>".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			String i18nName = getI18NMessageIfPrefixed(name);

			FormController formController = (FormController)getFormManager().getCurrentForm();
			if (formController != null)
			{
				String name2 = Text.processTags(i18nName, formController.getTagResolver());
				if (name2 != null) i18nName = name2;

			}
			else
			{
				String name2 = Text.processTags(i18nName, TagResolver.createResolver(new PrototypeState(null)));
				if (name2 != null) i18nName = name2;
			}
			if (!i18nName.trim().equals("")) //$NON-NLS-1$
			{
				if ("".equals(title)) //$NON-NLS-1$
				{
					title += i18nName;
				}
				else
				{
					title += " - " + i18nName; //$NON-NLS-1$
				}
			}
		}
		String appName = getDisplayApplicationName();
		if (appName.endsWith("Developer")) //$NON-NLS-1$
		{
			title = appName + " - " + title; //$NON-NLS-1$
		}
		else
		{
			if (title.equals("")) //$NON-NLS-1$
			{
				title = appName;
			}
			else
			{
				title += " - " + appName; //$NON-NLS-1$
			}
		}
		frame.setTitle(title);
	}

	@Override
	protected void checkForActiveTransactions(boolean force)
	{
		if (foundSetManager != null)
		{
			if (foundSetManager.hasTransaction())
			{
				int but = JOptionPane.CANCEL_OPTION;
				if (!force)
				{
					but = JOptionPane.showConfirmDialog(frame, Messages.getString("servoy.client.message.activetransaction"), //$NON-NLS-1$
						Messages.getString("servoy.client.message.activetransaction.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
				}
				if (but == JOptionPane.OK_OPTION)
				{
					foundSetManager.commitTransaction(true, true);
				}
				else
				{
					foundSetManager.rollbackTransaction(true, false, true);
				}
			}
		}
	}

	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		if (getSolution() == null || isClosing) return true;

		blockGUI(Messages.getString("servoy.client.status.closingsolution")); //$NON-NLS-1$
		try
		{
			if (!super.closeSolution(force, args)) return false;

			if (frame != null) frame.setTitle(getDisplayApplicationName());

			// delete all dialogs
			Iterator<Window> it = dialogs.values().iterator();
			while (it.hasNext())
			{
				Window element = it.next();
				element.dispose();
			}
			dialogs = new HashMap<String, Window>();

			Collection<Style> userStyles = getFlattenedSolution().flushUserStyles();
			if (userStyles != null)
			{
				for (Style style : userStyles)
				{
					ComponentFactory.flushStyle(this, style);
				}
			}

			setStatusText("", null); //$NON-NLS-1$
			ComponentFactory.flushCachedItems(this);
			invokeLater(new Runnable()// make some stuff later null so its not created again
			{
				public void run()
				{
					editLabel.setIcon(empty);
					insertModeLabel.setIcon(empty);
					transactionLabel.setIcon(empty);
					lockLabel.setIcon(empty);
					dataChangeLabel.setIcon(empty);
					invokeLater(new Runnable()
					{
						public void run()
						{
							if (getSolution() == null && getClientInfo() != null)
							{
								// select new solution, run later because logout may trigger a login dialog as well
								handleClientUserUidChanged(null, getClientInfo().getUserUid());
							}
						}
					});
				}
			});

			return true;
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return false;
		}
		finally
		{
			releaseGUI();
		}
	}

	protected int blockCounter = 0;
	protected Stack<Pair<Color, String>> msgStack = new Stack<Pair<Color, String>>();
	protected Pair<Color, String> lastOne;

	@Override
	public void blockGUI(final String reason)
	{
		synchronized (msgStack)
		{
			blockCounter++;
			if (msgStack.size() == 0) lastOne = new Pair<Color, String>(statusLabel.getForeground(), statusLabel.getText());

			Runnable update = new Runnable()
			{
				public void run()
				{
					msgStack.push(new Pair<Color, String>(Color.BLACK, reason));
					statusLabel.setForeground(Color.BLACK);
					statusLabel.setText(reason);
					if (blockCounter == 1)
					{
						frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						statusProgessBar.setIndeterminate(true);
						rootPane.getGlassPane().setVisible(true);
						rootPane.getGlassPane().setEnabled(true);
					}
				}
			};

			if (isEventDispatchThread())
			{
				update.run();
			}
			else
			{
				invokeLater(update);
			}
		}
	}

	// Releases GUI interaction
	@Override
	public void releaseGUI()
	{
		synchronized (msgStack)
		{
			blockCounter--;
			if (blockCounter < 0)
			{
				Debug.error("releaseGUI to many times called"); //$NON-NLS-1$
				blockCounter = 0;
			}
			else if (blockCounter >= 0)
			{
				Runnable update = new Runnable()
				{
					public void run()
					{
						msgStack.pop();// remove myown msg, to be able to show previous
						Pair<Color, String> p = null;
						if (msgStack.size() == 0)
						{
							p = lastOne;
						}
						else
						{
							p = msgStack.peek();
						}
						statusLabel.setForeground(p.getLeft());
						statusLabel.setText(p.getRight());
						if (blockCounter == 0)
						{
							if (frame != null) frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							rootPane.getGlassPane().setEnabled(false);
							rootPane.getGlassPane().setVisible(false);
							statusProgessBar.setIndeterminate(false);
						}
					}
				};

				if (isEventDispatchThread())
				{
					update.run();
				}
				else
				{
					invokeLater(update);
				}
			}
		}
	}

	@Override
	public void reportWarning(String s)
	{
		reportWarningInStatus(s);
	}

	public void reportWarningInStatus(final String s)
	{
		if (s == null) return;
		mainPanel.getToolkit().beep();

		synchronized (msgStack)
		{
			if (msgStack.size() == 0) lastOne = new Pair<Color, String>(statusLabel.getForeground(), statusLabel.getText());

			Runnable update = new Runnable()
			{
				public void run()
				{
					msgStack.push(new Pair<Color, String>(Color.RED, s));
					statusLabel.setForeground(Color.RED);
					statusLabel.setText(s);
				}
			};

			if (isEventDispatchThread())
			{
				update.run();
			}
			else
			{
				invokeLater(update);
			}
		}
		getScheduledExecutor().execute(new HideStatusText(3000 + (s.length() * 100)));
	}

	class HideStatusText implements Runnable
	{
		private final int sleepTime;

		HideStatusText(int slt)
		{
			sleepTime = slt;
		}

		public void run()
		{
			try
			{
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException e)
			{
				Debug.error(e);
			}

			synchronized (msgStack)
			{
				Runnable update = new Runnable()
				{
					public void run()
					{
						msgStack.pop();// remove myown msg, to be able to show previous
						Pair<Color, String> p = null;
						if (msgStack.size() == 0)
						{
							p = lastOne;
						}
						else
						{
							p = msgStack.peek();
						}
						statusLabel.setForeground(p.getLeft());
						statusLabel.setText(p.getRight());
					}
				};

				invokeLater(update);
			}
		}
	}

	private void fillBrowseToolbar(JToolBar toolBar, Map<String, Action> actions)
	{
		JButton mi = null;
		Action action = null;

		action = actions.get("cmdopensolution"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}

		action = actions.get("cmdprint"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}

		/*
		 * action = (Action) actions.get("cmdspell"); if (action != null) { mi = new ToolbarButton(action); toolBar.add(mi); }
		 */
		toolBar.addSeparator();

		action = actions.get("cmdhistoryback"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}

		action = actions.get("cmdhistoryforward"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}

		toolBar.addSeparator();

		action = actions.get("cmdnewrecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}

		action = actions.get("cmddeleterecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}

		toolBar.addSeparator();

		action = actions.get("cmdsort"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ToolbarButton(action);
			toolBar.add(mi);
		}
	}

	protected Map<String, Action> getActions()
	{
		HashMap<String, Action> table = new HashMap<String, Action>();
		table.put("cmdopensolution", new CmdOpenSolution(this)); //$NON-NLS-1$
		// table.put("cmdspell", new CmdSpell(this)); //$NON-NLS-1$
		table.put("cmdnewrecord", new CmdNewRecord(this)); //$NON-NLS-1$
		table.put("cmdduplicaterecord", new CmdDuplicateRecord(this)); //$NON-NLS-1$
		table.put("cmddeleterecord", new CmdDeleteRecord(this)); //$NON-NLS-1$
		table.put("cmddeleteallrecord", new CmdDeleteAllRecord(this)); //$NON-NLS-1$
		table.put("cmdsort", new CmdSort(this)); //$NON-NLS-1$
		table.put("cmdclose", new CmdClose(this)); //$NON-NLS-1$
		table.put("cmdpagesetup", new CmdPageSetup(this)); //$NON-NLS-1$
		// table.put("cmdprint", new CmdPrint(this)); //$NON-NLS-1$
		table.put("cmdundo", cmdManager.getUndoAction()); //$NON-NLS-1$
		table.put("cmdredo", cmdManager.getRedoAction()); //$NON-NLS-1$
		table.put("cmdcut", new CmdCut(this)); //$NON-NLS-1$
		table.put("cmdcopy", new CmdCopy(this)); //$NON-NLS-1$
		table.put("cmdpaste", new CmdPaste(this)); //$NON-NLS-1$
		table.put("cmdselectall", new CmdSelectAll(this)); //$NON-NLS-1$
		table.put("cmdshowpreferences", new CmdShowPreferences(this)); //$NON-NLS-1$
		table.put("cmdbrowsemode", new CmdBrowseMode(this)); //$NON-NLS-1$
		table.put("cmdfindmode", new CmdFindMode(this)); //$NON-NLS-1$
		table.put("cmdperformfind", new CmdPerformFind(this)); //$NON-NLS-1$
		table.put("cmdreducefind", new CmdReduceFind(this)); //$NON-NLS-1$
		table.put("cmdextendfind", new CmdExtendFind(this)); //$NON-NLS-1$
		table.put("cmdpreviewmode", new CmdPreviewMode(this)); //$NON-NLS-1$
		table.put("cmdfindall", new CmdFindAll(this)); //$NON-NLS-1$
		table.put("cmdstopsearchfindall", new CmdStopSearchFindAll(this)); //$NON-NLS-1$
		table.put("cmdsavedata", new CmdSaveData(this)); //$NON-NLS-1$
		// table.put("cmdreplace", new CmdReplace(this)); //$NON-NLS-1$
		table.put("cmdhelp", new CmdHelp(this)); //$NON-NLS-1$
		// table.put("cmdhelpcontents", new CmdHelpContents(this)); //$NON-NLS-1$
		table.put("cmdabout", new CmdAbout(this)); //$NON-NLS-1$
		table.put("cmdexit", new CmdExit(this)); //$NON-NLS-1$
		table.put("cmdviewasform", new CmdViewAsForm(this)); //$NON-NLS-1$
		table.put("cmdviewaslist", new CmdViewAsList(this)); //$NON-NLS-1$
		// table.put("cmdviewastable", new CmdViewAsTable(this)); //$NON-NLS-1$
		table.put("cmdomitrecord", new CmdOmitRecord(this)); //$NON-NLS-1$
		table.put("cmdrevertrecords", new CmdInvertRecords(this)); //$NON-NLS-1$
		table.put("cmdshowomitrecords", new CmdShowOmitRecords(this)); //$NON-NLS-1$
		table.put("cmdrecopyvalues", new CmdReCopyValues(this)); //$NON-NLS-1$

		Action action = new CmdHistoryBack(this);
		rootPane.registerKeyboardAction(action, (KeyStroke)action.getValue(Action.ACCELERATOR_KEY), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		table.put("cmdhistoryback", action); //$NON-NLS-1$
		action = new CmdHistoryForward(this);
		rootPane.registerKeyboardAction(action, (KeyStroke)action.getValue(Action.ACCELERATOR_KEY), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		table.put("cmdhistoryforward", action); //$NON-NLS-1$

		table.put("cmdnextrecord", new CmdNextRecord(this)); //$NON-NLS-1$
		table.put("cmdprevrecord", new CmdPrevRecord(this)); //$NON-NLS-1$

		table.put("cmdlogout", new CmdLogout(this)); //$NON-NLS-1$

		table.put("menuimportaction", new MenuImportAction(this)); //$NON-NLS-1$
		table.put("menuexportaction", new MenuExportAction(this)); //$NON-NLS-1$

		return table;
	}

	/**
	 * create the menu bar
	 */
	protected JMenuBar createMenuBar(Map<String, Action> actions)
	{

		// MenuBar
		// menuBar = new JMenuBar();
		if (menuBar == null)
		{
			menuBar = new JMenuBar();
		}
		else
		{
			menuBar.removeAll();
		}
		// menuBar.setBorder(BorderFactory.createEmptyBorder());
		// menuBar.setMargin(new Insets(10,10,10,10));
		// menuBar.getAccessibleContext().setAccessibleName("Swing menus");

		// menuBar.add(Box.createRigidArea(new Dimension(10,10)));
		// File Menu
		JMenu file = menuBar.add(new JMenuAlwaysEnabled(new MenuFileAction(this)));
		//file.setOpaque(false);
		// file.setEnabled(!runsInApplet);
		JMenuItem mi = null;

		Action action = null;

		action = actions.get("releaseformpanels"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);
		}

		action = actions.get("cmdopensolution"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);
		}

		action = actions.get("cmdclose"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);

			file.addSeparator();
		}

		action = actions.get("cmdsolutionsettings"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);

			file.addSeparator();
		}

		action = actions.get("cmdpagesetup"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);
		}

		file.add(((SwingModeManager)modeManager).getPreviewModeMenuItem(actions));

		file.addSeparator();

		action = actions.get("menuimportaction"); //$NON-NLS-1$
		if (action != null)
		{
			if (import_Menu == null)
			{
				import_Menu = new JMenuAlwaysEnabled(action);
			}
			file.add(import_Menu);
		}

		action = actions.get("menuexportaction"); //$NON-NLS-1$
		if (action != null)
		{
			if (export_Menu == null)
			{
				export_Menu = new JMenuAlwaysEnabled(action);
			}
			file.add(export_Menu);
		}

		file.addSeparator();

		action = actions.get("cmdlogout"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);
		}

		action = actions.get("cmdexit"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			file.add(mi);
		}

		// Edit Menu
		JMenu edit = menuBar.add(new JMenuAlwaysEnabled(new MenuEditAction(this)));
		//		edit.setOpaque(false);

		action = actions.get("cmdundo"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}
		action = actions.get("cmdredo"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}

		edit.addSeparator();

		action = actions.get("cmdcopy"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}

		action = actions.get("cmdcut"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}

		action = actions.get("cmdpaste"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}

		edit.addSeparator();

		action = actions.get("cmdselectall"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}

		edit.addSeparator();

		action = actions.get("cmdshowpreferences"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			edit.add(mi);
		}

		// Mode Menu
		JMenu mode = menuBar.add(new JMenuAlwaysEnabled(new MenuViewAction(this)));
		//		mode.setOpaque(false);

		// mode.add(modeManager.getBrowseModeMenuItem(actions));
		ButtonGroup bg = new ButtonGroup();
		JRadioButtonMenuItem mi1 = null;
		JRadioButtonMenuItem mi2 = null;
		JRadioButtonMenuItem mi3 = null;
		action = actions.get("cmdviewasform"); //$NON-NLS-1$
		if (action != null)
		{
			mi1 = new ActionRadioMenuItem(action);
			mode.add(mi1);
			bg.add(mi1);
		}
		action = actions.get("cmdviewaslist"); //$NON-NLS-1$
		if (action != null)
		{
			mi2 = new ActionRadioMenuItem(action);
			mode.add(mi2);
			bg.add(mi2);
		}
		action = actions.get("cmdviewastable"); //$NON-NLS-1$
		if (action != null)
		{
			mi3 = new ActionRadioMenuItem(action);
			mode.add(mi3);
			bg.add(mi3);
		}

		((SwingFormManager)formManager).setViews(new JRadioButtonMenuItem[] { mi1, mi2, mi3 });

		ButtonGroup bg1 = new ButtonGroup();
		action = actions.get("cmdbasicfilter"); //$NON-NLS-1$
		if (action != null)
		{
			mode.addSeparator();
			mi = new ActionRadioMenuItem(action);
			mode.add(mi);
			bg1.add(mi);
		}
		action = actions.get("cmdadvancedfilter"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionRadioMenuItem(action);
			mode.add(mi);
			bg1.add(mi);
		}
		action = actions.get("cmdwebfilter"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionCheckBoxMenuItem(action);
			mode.add(mi);
		}

		action = actions.get("cmdshowrulers"); //$NON-NLS-1$
		if (action != null)
		{
			mode.addSeparator();
			mi = new ActionCheckBoxMenuItem(action);
			mode.add(mi);
		}

		action = actions.get("cmdshowgrid"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionCheckBoxMenuItem(action);
			mode.add(mi);
		}

		action = actions.get("cmdsnaptogrid"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionCheckBoxMenuItem(action);
			mode.add(mi);
		}

		mode.addSeparator();

		JMenu toolbars = toolbarsPanel.getMenu();

		mode.add(toolbars);

		// Select Menu
		JMenu select = menuBar.add(new JMenuAlwaysEnabled(new MenuSelectAction(this)));
		//		select.setOpaque(false);

		select.add(((SwingModeManager)modeManager).getFindModeMenuItem(actions));

		action = actions.get("cmdperformfind"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}
		action = actions.get("cmdreducefind"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}
		action = actions.get("cmdextendfind"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		action = actions.get("cmdfindall"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		select.addSeparator();

		action = actions.get("cmdsavedata"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		select.addSeparator();

		action = actions.get("cmdnewrecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		action = actions.get("cmdduplicaterecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		action = actions.get("cmddeleterecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		action = actions.get("cmddeleteallrecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		select.addSeparator();

		action = actions.get("cmdomitrecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}
		action = actions.get("cmdshowomitrecords"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}
		action = actions.get("cmdrevertrecords"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}
		select.addSeparator();

		action = actions.get("cmdprevrecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}
		action = actions.get("cmdnextrecord"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		select.addSeparator();

		action = actions.get("cmdsort"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		action = actions.get("cmdrecopyvalues"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		action = actions.get("cmdreplace"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			select.add(mi);
		}

		// Script Menu
		menuBar.add(((SwingFormManager)formManager).getScriptMenu()); // Script

		// Window Menu
		menuBar.add(((SwingFormManager)formManager).getWindowMenu());

		// Help Menu
		JMenu help = menuBar.add(new JMenuAlwaysEnabled(new MenuHelpAction(this)));
		//		help.setOpaque(false);

		action = actions.get("cmdhelp"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			help.add(mi);
		}

		action = actions.get("cmdhelpcontents"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			help.add(mi);
		}

		help.addSeparator();

		action = actions.get("cmdsupport"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			help.add(mi);
		}

		action = actions.get("cmdforum"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			help.add(mi);
		}

		action = actions.get("cmdnewversioncheck"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			help.add(mi);

			help.addSeparator();
		}

		action = actions.get("cmdabout"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			help.add(mi);
		}

		return menuBar;
	}

	protected void attachAppleMenu(Map<String, Action> actions)
	{
		Object appleObject;
		try
		{
			Class< ? > clazz = Class.forName("net.roydesign.app.Application"); //$NON-NLS-1$
			Method mi = clazz.getMethod("getInstance", (Class[])null); //$NON-NLS-1$
			appleObject = mi.invoke(null, (Object[])null);

			Action action = actions.get("cmdabout"); //$NON-NLS-1$
			if (action != null)
			{
				Method m = clazz.getMethod("getAboutJMenuItem", (Class[])null); //$NON-NLS-1$
				JMenuItem a_about = (JMenuItem)m.invoke(appleObject, (Object[])null);
				a_about.addActionListener(action);
			}

			action = actions.get("cmdexit"); //$NON-NLS-1$
			if (action != null)
			{
				Method m = clazz.getMethod("getQuitJMenuItem", (Class[])null); //$NON-NLS-1$
				JMenuItem a_about = (JMenuItem)m.invoke(appleObject, (Object[])null);
				a_about.addActionListener(action);
			}

			action = actions.get("cmdshowpreferences"); //$NON-NLS-1$
			if (action != null)
			{
				Method m = clazz.getMethod("getPreferencesJMenuItem", (Class[])null); //$NON-NLS-1$
				JMenuItem a_about = (JMenuItem)m.invoke(appleObject, (Object[])null);
				a_about.addActionListener(action);
			}
		}
		catch (Throwable e)
		{
			Debug.error(e);
		}
	}

	/**
	 * Show the application dialog
	 */
	public void showAppPrefs()
	{
		if (ap == null)
		{
			blockGUI(Messages.getString("servoy.client.status.loading.preferencepanels")); //$NON-NLS-1$
			SwingHelper.dispatchEvents(300);// show cursor / hide menu
			try
			{
				ap = new ApplicationPreferences(this);
				// Load all default tabs

				loadPreferecesPanels(ap);

				// Load plugins tabs
				((ClientPluginManager)pluginManager).addPreferenceTabs(ap);

				ap.pack();
				ap.setLocationRelativeTo(mainPanel);

			}
			finally
			{
				releaseGUI();
			}
		}
		ap.setVisible(true);
	}

	protected PreferencePanel createGeneralPanel()
	{
		return new GeneralPanel(this);
	}

	protected void loadPreferecesPanels(ApplicationPreferences appPrefs)
	{
		appPrefs.addPreferenceTab(createGeneralPanel());
		appPrefs.addPreferenceTab(new LFPreferencePanel(this));
		appPrefs.addPreferenceTab(new LocalePreferences(this));
		addServicePreferencesTab(appPrefs);
	}

	@SuppressWarnings("nls")
	protected void addServicePreferencesTab(ApplicationPreferences appPrefs)
	{
		String rmiFactory = settings.getProperty("SocketFactory.rmiClientFactory", "com.servoy.j2db.rmi.DefaultClientSocketFactoryFactory");
		if (rmiFactory == null || rmiFactory.endsWith("com.servoy.j2db.rmi.DefaultClientSocketFactoryFactory"))
		{
			appPrefs.addPreferenceTab(new ServicePanel(this));
		}
	}

	@Override
	public void logout(final Object[] solution_to_open_args)
	{
		if (getClientInfo().getUserUid() != null)
		{
			if (getSolution() == null)
			{
				super.logout(solution_to_open_args);
			}
			else
			{
				// close solution first
				invokeLater(new Runnable()
				{
					public void run()
					{
						boolean doLogOut = getClientInfo().getUserUid() != null;
						if (getSolution() != null)
						{
							doLogOut = closeSolution(false, solution_to_open_args);
						}
						if (doLogOut)
						{
							Action action = getCmdManager().getRegisteredAction("cmdlogout"); //$NON-NLS-1$
							if (action != null)
							{
								action.setEnabled(false);
							}

							if (getSolution() == null)
							{
								// calls super.logout()
								logout(solution_to_open_args);
							}
						}
					}
				});
			}
		}
	}

	@Override
	public String authenticate(Credentials credentials) throws RepositoryException
	{
		String jsreturn = super.authenticate(credentials);

		if (getClientInfo().getUserUid() != null)
		{
			// successfully logged in

			// store name
			URL serverURL = getServerURL();
			getSettings().setProperty(serverURL.getHost() + serverURL.getPort() + "lastLoggedinUserName", getClientInfo().getUserName()); //$NON-NLS-1$

			Action action = getCmdManager().getRegisteredAction("cmdlogout"); //$NON-NLS-1$
			if (action != null)
			{
				action.setEnabled(true);
			}
		}

		return jsreturn;
	}

	private LoginDialog loginDialog;

	@Override
	public void showDefaultLogin()
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				if (loginDialog != null)
				{
					return; // already showing login dialog
				}
				try
				{
					while (getClientInfo().getUserUid() == null)
					{
						URL serverURL = getServerURL();
						String name = getSettings().getProperty(serverURL.getHost() + serverURL.getPort() + "lastLoggedinUserName", //$NON-NLS-1$
							System.getProperty("user.name")); //$NON-NLS-1$

						if (loginDialog == null)
						{
							loginDialog = createLoginDialog();
						}
						Object[] name_password = loginDialog.showDialog(name);

						dispatchEventsToHideDialog(); // hide dialog

						if (name_password == null || name_password.length < 2 || name_password[0] == null || name_password[1] == null)
						{
							// user hit cancel
							showSolutionLoading(false);
							return;
						}

						authenticate(null, null, new Object[] { name_password[0].toString(), name_password[1].toString() });

						if (getClientInfo().getUserUid() == null)
						{
							JOptionPane.showMessageDialog(frame, Messages.getString("servoy.client.message.loginfailed"), //$NON-NLS-1$
								Messages.getString("servoy.client.message.loginfailed.title"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
						}
					}

					handleClientUserUidChanged(null, getClientInfo().getUserUid());
					if (!isShutDown() && solutionRoot.isMainSolutionLoaded() &&
						(getClientInfo().getUserUid() != null || !solutionRoot.getSolution().requireAuthentication()))

					{
						solutionLoaded(getSolution());
					}
				}
				catch (Exception e)
				{
					reportError(Messages.getString("servoy.client.message.loginfailed"), e); //$NON-NLS-1$
				}
				finally
				{
					if (loginDialog != null)
					{
						loginDialog.dispose();
						loginDialog = null;
					}
				}
			}
		};
		invokeLater(r);
	}

	protected void dispatchEventsToHideDialog()
	{
		SwingHelper.dispatchEvents(250);
	}

	@Override
	public void clearLoginForm()
	{
		Action action = getCmdManager().getRegisteredAction("cmdlogout"); //$NON-NLS-1$
		if (action != null)
		{
			action.setEnabled(true);
		}
		super.clearLoginForm();
	}

	public void setStatusProgress(int progress)
	{
		statusProgessBar.setValue(progress);
	}

	public void setStatusText(String statusText, String toolTip)
	{
		String text = (statusText.trim().length() == 0) ? READY : statusText;
		statusLabel.setForeground(Color.BLACK);
		statusLabel.setText(text);
		statusLabel.setToolTipText(toolTip);
		lastOne = new Pair<Color, String>(Color.BLACK, text);
	}

	private AboutDialog ad;

	public void showAboutDialog()
	{
		if (ad == null)
		{
			ad = new AboutDialog(this);
			ad.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					ad = null;
				}

				@Override
				public void windowClosed(WindowEvent e)
				{
					ad = null;
				}
			});
			ad.setLocationRelativeTo(frame);
			ad.setVisible(true);
		}
		else
		{
			ad.toFront();
		}
	}

	public void looseFocus()
	{
		getMainApplicationFrame().getContentPane().requestFocus();
	}

	public void reportInfo(String message)
	{
		reportInfo(frame, message, Messages.getString("servoy.general.info")); //$NON-NLS-1$
	}

	public void reportInfo(Component parentComponent, String message, String title)
	{
		JOptionPane.showMessageDialog(parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void reportError(String message, Object detail)
	{
		Window window = (Window)getRuntimeWindowManager().getCurrentWindowWrappedObject();
		if (window == null || !window.isVisible())
		{
			window = getMainApplicationFrame();
		}
		reportError(window, message, detail);
	}

	public void reportError(Component parentComponent, String message, Object detail)
	{
		// test if it is a Repository Exception with Client not register status code.
		if (!testClientRegistered(detail))
		{
			if (Debug.tracing())
			{
				if (detail instanceof Throwable)
				{
					Debug.trace("client is not registered, waiting for a reconnect", (Throwable)detail); //$NON-NLS-1$
				}
				else
				{
					Debug.trace("client is not registered, waiting for a reconnect"); //$NON-NLS-1$
				}
			}
			return;
		}
		if (detail instanceof Throwable)
		{
			Debug.error(message, (Throwable)detail);
		}
		else
		{
			Debug.error(detail);
		}
		mainPanel.getToolkit().beep();
		String dialogMessage = message;
		if (dialogMessage == null) dialogMessage = ""; //$NON-NLS-1$
		else if (dialogMessage.length() > 100)
		{
			dialogMessage = dialogMessage.substring(0, 100) + "..."; //$NON-NLS-1$
		}
		JOptionPane.showMessageDialog(parentComponent, dialogMessage, Messages.getString("servoy.general.error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		SwingHelper.dispatchEvents(100);// hide the dialog
	}

	public void setPageFormat(PageFormat pf)
	{
		pageFormat = pf;
		settings.setProperty("pageformat", PersistHelper.createPageFormatString(pf)); //$NON-NLS-1$
	}

	private PageFormat pageFormat = null;

	public PageFormat getPageFormat()
	{
		if (pageFormat == null)
		{
			pageFormat = PersistHelper.createPageFormat(settings.getProperty("pageformat")); //$NON-NLS-1$
			if (pageFormat == null)
			{
				pageFormat = new PageFormat();
			}
		}
		return pageFormat;
	}

	private HashMap<String, Window> dialogs = new HashMap<String, Window>();

	public void registerWindow(String name, Window d)
	{
		Window removed = null;
		if (d != null)
		{
			removed = dialogs.put(name, d);
		}
		else
		{
			removed = dialogs.remove(name);
		}
		if (removed != null && d != removed)
		{
			removed.dispose();
		}
	}

	public Window getWindow(String name)
	{
		return dialogs.get(name);
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new ScriptEngine(this);
	}

	@Override
	protected void createFoundSetManager()
	{
		foundSetManager = new FoundSetManager(this, new SwingFoundSetFactory());
		((FoundSetManager)foundSetManager).setInfoListener(this);
		foundSetManager.init();
		((FoundSetManager)foundSetManager).getEditRecordList().addEditListener(this);
	}

	private int getRmiExportPort() throws Exception
	{
		URL base = getServerURL();
		URL url = new URL(base, "servoy-rmi-portserver"); //$NON-NLS-1$
		DataInputStream is = new DataInputStream((InputStream)url.getContent());
		int port = is.readInt();
		is.close();
		return port;
	}

	@Override
	protected void bindUserClient()
	{
		try
		{
			int port = exportObject(userClient);
			Debug.trace("RMI export succeeded on port: " + port);
			getClientInfo().setHostPort(port);
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
	}

	private int rmiExportPort = -1;

	/**
	 * @return
	 * @throws RemoteException
	 */
	public synchronized int exportObject(Remote object) throws RemoteException
	{
		int counter = 0;
		while (counter++ < 100)
		{
			try
			{
				if (rmiExportPort == -1)
				{
					rmiExportPort = getRmiExportPort();
				}

				UnicastRemoteObject.exportObject(object, rmiExportPort, rmiFactoryFactory.getClientSocketFactory(), rmiFactoryFactory.getServerSocketFactory());
				counter = -1;
				break;
			}
			catch (Exception e)
			{
				// set the port on -1 and try again.
				rmiExportPort = -1;
			}
		}
		if (counter != -1)
		{
			Debug.error("Couldnt export object with port from server, trying to do on anonym port");
			rmiExportPort = 0;
			UnicastRemoteObject.exportObject(object, rmiExportPort, rmiFactoryFactory.getClientSocketFactory(), rmiFactoryFactory.getServerSocketFactory());
		}
		return rmiExportPort;
	}

	@Override
	protected void unBindUserClient() throws Exception
	{
		if (userClient != null)
		{
			try
			{
				Debug.trace("Unexporting userclient");
				int counter = 1;
				while (!UnicastRemoteObject.unexportObject(userClient, false))
				{
					Debug.trace("Unexporting userclient not yet successful for " + counter + " time");
					if (isRunningRemote() && counter < 5)
					{
						counter++;
						// Let the server be able to clean it up.
						synchronized (this)
						{
							this.wait(1000);
						}
					}
					else
					{
						UnicastRemoteObject.unexportObject(userClient, true);
						break;
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	public void output(Object msg, int level)
	{
		if (level == ERROR || level == FATAL)
		{
			System.err.println(msg);
		}
		else
		{
			System.out.println(msg);
		}
	}

	protected void showAd()
	{
		getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				final Object[] adsInfo = Ad.getAdInfo();
				final int w = Utils.getAsInteger(adsInfo[1]);
				final int h = Utils.getAsInteger(adsInfo[2]);
				if (w > 50 && h > 50)
				{
					J2DBClient.this.invokeLater(new Runnable()
					{
						public void run()
						{
							URL url = (URL)adsInfo[0];
							int t = Utils.getAsInteger(adsInfo[3]);
							JPanel comp = new InfoPanel(J2DBClient.this, url, t);
							comp.setSize(w, h);
							comp.setLocation((frame.getWidth() - comp.getWidth()) - 30, 60);
							frame.getRootPane().getLayeredPane().add(comp, JLayeredPane.MODAL_LAYER);
						}
					});
				}
			}
		});
	}

	@Override
	protected boolean registerClient(IUserClient uc) throws Exception
	{
		boolean registered = false;
		try
		{
			registered = super.registerClient(uc);
			if (!registered)
			{
				showAd();
			}
		}
		catch (final ApplicationException e)
		{
			invokeAndWait(new Runnable()
			{
				public void run()
				{
					if (e.getErrorCode() == ServoyException.NO_LICENSE)
					{
						JOptionPane.showMessageDialog(frame, Messages.getString("servoy.license.notrialleft"), Messages.getString("servoy.license.label"),
							JOptionPane.ERROR_MESSAGE);
						exitHard(1);
					}
					else if (e.getErrorCode() == ServoyException.MAINTENANCE_MODE)
					{
						JOptionPane.showMessageDialog(frame, Messages.getString("servoy.maintenance.clientRegisterForbidden"), //$NON-NLS-1$
							Messages.getString("servoy.maintenance.label"), JOptionPane.ERROR_MESSAGE);
						exitHard(1);
					}
					else if (e.getErrorCode() == ServoyException.InternalCodes.INVALID_RMI_SERVER_CONNECTION)
					{
						JOptionPane.showMessageDialog(frame, e.getMessage(), Messages.getString("servoy.general.error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
						exitHard(1);
					}
				}
			});
		}
		return registered;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IGlobalEditListener#editChange(com.servoy.j2db.dataprocessing.GlobalEditEvent)
	 */
	public void editChange(final GlobalEditEvent e)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			if (e.isEditing())
			{
				editLabel.setIcon(editing);
			}
			else
			{
				editLabel.setIcon(empty);
			}
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (e.isEditing())
					{
						editLabel.setIcon(editing);
					}
					else
					{
						editLabel.setIcon(empty);
					}
				}
			});
		}
	}

	public void showTransactionStatus(boolean b)
	{
		if (b)
		{
			transactionLabel.setIcon(transaction);
		}
		else
		{
			transactionLabel.setIcon(empty);
		}
	}

	public void showLocksStatus(boolean b)
	{
		if (b)
		{
			lockLabel.setIcon(locking);
		}
		else
		{
			lockLabel.setIcon(empty);
		}
	}

	public void showDataChange()// blink for 1 seconds
	{
		synchronized (flashDataChange)
		{
			flashDataChange.setHideTime(System.currentTimeMillis() + 1000);
			if (!flashDataChange.isRunning())
			{
				flashDataChange.setRunning(true);
				getScheduledExecutor().execute(flashDataChange);
			}
		}
	}

	class FlashDataChange implements Runnable
	{
		private static final long flashTime = 400;

		private long hideTime;
		private boolean visible;
		private boolean running;

		void setHideTime(long hideTime)
		{
			this.hideTime = hideTime;
		}

		void setRunning(boolean running)
		{
			this.running = running;
		}

		boolean isRunning()
		{
			return running;
		}

		public void run()
		{
			visible = true;
			dataChangeLabel.setIcon(data_change);
			while (true)
			{
				synchronized (this)
				{
					if (System.currentTimeMillis() > hideTime)
					{
						running = false;
						dataChangeLabel.setIcon(empty);
						visible = false;
						break;
					}
				}
				try
				{
					Thread.sleep(visible ? flashTime : flashTime / 2);
				}
				catch (InterruptedException e)
				{
					// Ignore (this is really not worth logging).
				}
				if (visible)
				{
					dataChangeLabel.setIcon(empty);
				}
				else
				{
					dataChangeLabel.setIcon(data_change);
				}
				visible = !visible;
			}
		}
	}

	/*
	 * @see com.servoy.j2db.IMessagesCallback#refresh()
	 */
	public void messagesLoaded()
	{
		for (String jre_key : Messages.JRE_DEFAULT_KEYS)
		{
			String message = Messages.getString(jre_key);
			if (!Messages.JRE_DEFAULT_KEY_VALUE.equals(message)) UIManager.put(jre_key, message);
		}

		if (getCmdManager() != null)
		{
			((CmdManager)getCmdManager()).i18nRefresh();
		}
		ClientState.READY = Messages.getString("servoy.general.status.ready");
	}

	/*
	 * @see IServiceProvider#setLocale(Locale)
	 */
	public void setLocale(Locale locale)
	{
		Locale old = Locale.getDefault();
		if (!old.equals(locale))
		{
			Locale.setDefault(locale);
			Messages.loadInternal(this, getFoundSetManager());
			J2DBGlobals.firePropertyChange(this, "locale", old, locale);
		}
	}

	public Locale getLocale()
	{
		return Locale.getDefault();
	}

	public TimeZone getTimeZone()
	{
		return TimeZone.getDefault();
	}

	public synchronized void setTimeZone(TimeZone zone)
	{
		if (getTimeZone().equals(zone)) return;
		TimeZone old = getTimeZone();
		TimeZone.setDefault(zone);
		J2DBGlobals.firePropertyChange(this, "timeZone", old, zone); //$NON-NLS-1$
	}

	/*
	 * @see IServiceProvider#getI18NMessage(String,Object[])
	 */
	public String getI18NMessage(String i18nKey, Object[] array)
	{
		if (array != null && array.length != 0)
		{
			return Messages.getString(i18nKey, array);
		}
		return Messages.getString(i18nKey);
	}

	public String getI18NMessage(String i18nKey)
	{
		return Messages.getString(i18nKey);
	}

	public void setI18NMessage(String key, String value)
	{
		Messages.setI18nScriptingMessage(key, value);

	}

	/*
	 * @see IServiceProvider#getI18NMessageIfPrefixed(String,Object[])
	 */
	public String getI18NMessageIfPrefixed(String i18nKey)
	{
		return Messages.getStringIfPrefix(i18nKey);
	}

	private String i18nColumnName;
	private String[] i18nColunmValue;

	public void setI18NMessagesFilter(String columnname, String[] value)
	{
		this.i18nColumnName = columnname;
		this.i18nColunmValue = value;
		refreshI18NMessages();
	}

	public String getI18NColumnNameFilter()
	{
		return i18nColumnName;
	}

	public String[] getI18NColumnValueFilter()
	{
		return i18nColunmValue;
	}

	public ResourceBundle getResourceBundle(Locale locale)
	{
		return new MessagesResourceBundle(this, locale == null ? getLocale() : locale, i18nColumnName, i18nColunmValue, getSolution().getSolutionID());
	}

	/*
	 * @see com.servoy.j2db.ClientState#refreshI18NMessages()
	 */
	@Override
	public void refreshI18NMessages()
	{
		Messages.loadInternal(this, getFoundSetManager());
	}

	@Override
	public boolean isRunningRemote()
	{
		return WebStart.isRunningWebStart();
	}

	@Override
	public URL getServerURL()
	{
		String server_url = System.getProperty("servoy.server_url"); //$NON-NLS-1$
		if (server_url != null)
		{
			try
			{
				return new URL(server_url);
			}
			catch (MalformedURLException e)
			{
				Debug.error(e);
			}
		}
		return WebStart.getWebStartURL();
	}

	private JDialog disconnectDialog;
	private IDataRendererFactory< ? > dataRenderFactory;


	/*
	 * @see disconnectFromServer()
	 */
	private void closeDisconnectDialog()
	{
		if (!isConnected())
		{
			int option = JOptionPane.showConfirmDialog(disconnectDialog, Messages.getString("servoy.client.serverdisconnect.optionpane.question"), //$NON-NLS-1$
				Messages.getString("servoy.client.serverdisconnect.optionpane.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
			if (option == JOptionPane.YES_OPTION)
			{
				saveSettings();
				exitHard(1);
			}
		}
	}

	public boolean isConnected()
	{
		return rmiFactoryFactory == null || rmiFactoryFactory.isConnected();
	}

	/**
	 * @see com.servoy.j2db.ClientState#testClientRegistered(Object)
	 */
	@Override
	protected boolean testClientRegistered(Object exception)
	{
		Object ex = exception;
		while (ex instanceof Exception && !(ex instanceof ServoyException))
		{
			ex = ((Exception)ex).getCause();
		}
		if (isConnected() && ex instanceof ServoyException && (((ServoyException)ex).getErrorCode() == ServoyException.InternalCodes.CLIENT_NOT_REGISTERED ||
			((ServoyException)ex).getErrorCode() == ServoyException.InternalCodes.INVALID_RMI_SERVER_CONNECTION))
		{
			if (rmiFactoryFactory != null)
			{
				if (!reconnecting)
				{
					Debug.error("Test exception, calling disconnect in thread: " + Thread.currentThread().getName(), (Exception)exception);
					rmiFactoryFactory.disconnect();
					return false;
				}
			}
			else
			{
				reconnectedToServer();
			}
			disconnectedFromServer();
			return false;
		}

		return isConnected();
	}

	/*
	 * @see com.servoy.j2db.util.rmi.IReconnectListener#disconnected()
	 */
	public void disconnectedFromServer()
	{
		if (disconnectDialog == null)
		{
			disconnectDialog = new JDialog(this.getMainApplicationFrame(), Messages.getString("servoy.client.serverdisconnect.dialog.title"), true);
			disconnectDialog.setPreferredSize(new Dimension(400, 80));
			disconnectDialog.setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.NORTH;
			disconnectDialog.getContentPane().add(new JLabel(Messages.getString("servoy.client.serverdisconnect.dialog.label")), gbc);

			// for Linux, a button is also added so that the user can close the disconnect dialog; the action is the same as for window close
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.SOUTH;
			JButton cancelButton = new JButton(Messages.getString("servoy.button.close")); //$NON-NLS-1$
			cancelButton.setFont(new Font("arial", Font.PLAIN, 11));
			cancelButton.setPreferredSize(new Dimension(100, 20));
			cancelButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					closeDisconnectDialog();
				}
			});
			disconnectDialog.add(cancelButton, gbc);
			disconnectDialog.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					closeDisconnectDialog();
				}
			});
			disconnectDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			disconnectDialog.pack();
		}
		Runnable dialogShower = new Runnable()
		{
			public void run()
			{
				if (!isConnected())
				{
					if (Debug.tracing())
					{
						Debug.trace("Setting disconnect dialog to true.");
					}
					disconnectDialog.setLocationRelativeTo(getMainApplicationFrame());
					disconnectDialog.setVisible(true);
				}
			}
		};
		if (isEventDispatchThread())
		{
			dialogShower.run();
		}
		else
		{
			invokeLater(dialogShower);
		}
	}

	private volatile boolean reconnecting;

	/*
	 * @see com.servoy.j2db.util.rmi.IReconnectListener#reconnect()
	 */
	public void reconnectedToServer()
	{
		if (reconnecting) return;
		getScheduledExecutor().execute(new Runnable()
		{
			@SuppressWarnings("nls")
			public void run()
			{
				if (reconnecting) return;
				reconnecting = true;
				final String prevClientId = getClientInfo().getClientId();

				try
				{
					if (Debug.tracing())
					{
						Debug.trace(Thread.currentThread().getName() + ", Reconnecting to the server, unexporting the current client");
					}
					try
					{
						// Try to force unexport the object.
						UnicastRemoteObject.unexportObject(userClient, true);
					}
					catch (Exception e1)
					{
						// ignore if not
					}

					// Create new clientinfo object. So that it can have different
					// addresses..
					getClientInfo().initHostInfo();

					// recreate the UserClient
					createUserClient();
					bindUserClient();
					try
					{
						registerClient(userClient);
					}
					catch (Exception e)
					{
						if (isConnected())
						{
							// Remote client object no longer exists on app server, must have been restarted
							Debug.error("Error reregistering client", e);
							invokeAndWait(new Runnable()
							{

								public void run()
								{
									JOptionPane.showMessageDialog(getMainApplicationFrame(), Messages.getString("servoy.client.message.error.registerclient"),
										Messages.getString("servoy.client.message.clientregister"), JOptionPane.ERROR_MESSAGE);
									exitHard(1);
								}
							});
						}
						else
						{
							reconnecting = false;
							disconnectedFromServer();
							reconnectedToServer();
							return;
						}
					}
				}
				finally
				{
					reconnecting = false;
				}

				if (prevClientId == null || !prevClientId.equals(getClientInfo().getClientId()))
				{
					Runnable closeAndLogout = new Runnable()
					{
						public void run()
						{
							JOptionPane.showMessageDialog(disconnectDialog, Messages.getString("servoy.client.serverdisconnect.restarting.solution"),
								Messages.getString("servoy.client.serverdisconnect.restarting.solution.title"), JOptionPane.INFORMATION_MESSAGE);
							if (Debug.tracing())
							{
								Debug.trace("Client reconnected with id " + getClientID() + " from id " + prevClientId);
							}
							if (Debug.tracing())
							{
								Debug.trace("Setting disconnect dialog to false.");
							}
							disconnectDialog.setVisible(false);
							closeSolution(true, startupArguments);
							// logout to make sure the login solution is reloaded in case the main solution needs state from the login solution
							logout(null);
						}
					};
					if (((FoundSetManager)getFoundSetManager()).hasLocks(null) || ((FoundSetManager)getFoundSetManager()).hasTransaction() ||
						((FoundSetManager)getFoundSetManager()).hasClientDataSources())
					{
						try
						{
							getDataServer().logMessage(
								"Client reconnected with id " + getClientID() + " from id " + prevClientId + ", client needs to restart");
						}
						catch (Exception ex)
						{
							// ignore
						}
						invokeLater(closeAndLogout);
						return;
					}
					// if logged in, login again
					if (getClientInfo().getUserUid() != null)
					{
						try
						{
							authenticate(new Credentials(getClientInfo().getClientId(), getClientInfo().getAuthenticatorType(),
								getClientInfo().getAuthenticatorMethod(), getClientInfo().getJsCredentials()));
						}
						catch (RepositoryException e)
						{
							Debug.error(e);
						}
						if (getClientInfo().getUserUid() == null)
						{
							try
							{
								getDataServer().logMessage("Client reconnected with id " + getClientID() + " from id " + prevClientId +
									", relogin with old credentials failed, restarting client");
							}
							catch (Exception ex)
							{
								// ignore
							}
							invokeLater(closeAndLogout);
							return;
						}
					}


					try
					{
						((FoundSetManager)getFoundSetManager()).registerClientTables(null);
					}
					catch (Exception e)
					{
						if (isConnected())
						{
							// Remote client object no longer exists on app server, must have been restarted
							Debug.error("Error reregistering client", e); //$NON-NLS-1$
							invokeAndWait(new Runnable()
							{
								public void run()
								{
									JOptionPane.showMessageDialog(getMainApplicationFrame(), Messages.getString("servoy.client.message.error.registerclient"),
										Messages.getString("servoy.client.message.clientregister"), JOptionPane.ERROR_MESSAGE);
									exitHard(1);
								}
							});
						}
						else
						{
							disconnectedFromServer();
							reconnectedToServer();
							return;
						}
					}
					((FoundSetManager)getFoundSetManager()).flushCachedDatabaseData(null);
				}
				if (Debug.tracing())
				{
					Debug.trace("Client reconnected with id " + getClientID() + " from id " + prevClientId);
				}
				invokeLater(new Runnable()
				{
					public void run()
					{
						if (Debug.tracing())
						{
							Debug.trace("Setting disconnect dialog to false.");
						}
						disconnectDialog.setVisible(false);
					}
				});
			}
		});
	}

	public boolean isEventDispatchThread()
	{
		return SwingUtilities.isEventDispatchThread();
	}

	@Override
	protected void doInvokeLater(Runnable r)
	{
		SwingUtilities.invokeLater(r);
	}

	public void invokeAndWait(Runnable r)
	{
		if (isEventDispatchThread())
		{
			r.run();
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(r);
			}
			catch (Exception e)
			{
				Debug.error("Error calling invoke an wait for a runnable", e);
			}
		}
	}

	@Override
	public void activateSolutionMethod(String globalMethodName, StartupArguments argumentsScope)
	{
		try
		{
			((IClientPluginAccess)getPluginAccess()).executeMethod(null, globalMethodName,
				new Object[] { argumentsScope.getFirstArgument(), argumentsScope.toJSMap() }, true);
			getMainApplicationFrame().toFront();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * @see com.servoy.j2db.IApplication#getItemFactory()
	 */
	public ItemFactory getItemFactory()
	{
		if (itemFactory == null)
		{
			itemFactory = new SwingItemFactory(this);
		}
		return itemFactory;
	}

	public IDataRendererFactory< ? > getDataRenderFactory()
	{
		if (dataRenderFactory == null)
		{
			dataRenderFactory = new DataRendererFactory();
		}
		return dataRenderFactory;
	}

	/**
	 * @see com.servoy.j2db.IApplication#isHeadless()
	 */
	public boolean isHeadless()
	{
		return false;
	}

	public RendererParentWrapper getPrintingRendererParent()
	{
		return new RendererParentWrapper(getEditLabel());
	}


	public boolean showURL(String url, String target, String target_options, int timeout, boolean closeDialogs)
	{
		// mail to doesn't work in showUrl through webstart
		if (WebStart.isRunningWebStart() && url.toLowerCase().startsWith("http"))
		{
			try
			{
				return WebStart.showURL(new URL(url));
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				// Service is not supported?
			}
		}

		try
		{
			BrowserLauncher.openURL(url);
			return true;
		}
		catch (Throwable e)//catch all for apple mac
		{
			Debug.error(e);
			WebStart.setClipboardContent(url);
			reportWarningInStatus("If running in client this url is shown in browser: " + url + " ,the url is pasted on your clipboard");
			return false;
		}
	}

	public Dimension getScreenSize()
	{
		try
		{
			int height = Toolkit.getDefaultToolkit().getScreenSize().height;
			int width = Toolkit.getDefaultToolkit().getScreenSize().width;
			return new Dimension(width, height);
		}
		catch (Exception e)
		{
			Debug.trace("Ignore, assume headless exc: " + e.getMessage());
		}
		return new Dimension(0, 0);
	}

	protected LoginDialog createLoginDialog()
	{
		return new LoginDialog(frame, this);
	}

	public String showI18NDialog(String preselect_key, String preselect_language)
	{
		I18NDialog sfd = (I18NDialog)getWindow("I18NDialog_main"); //$NON-NLS-1$
		JFrame window = getMainApplicationFrame();
		if (sfd == null || sfd.getOwner() != window)
		{
			if (sfd != null) sfd.setVisible(false);

			sfd = new I18NDialog(this, window, true);
			registerWindow("I18NDialog_main", sfd);
		}
		else
		{
			sfd.setEndUser(true);
		}

		sfd.setModal(true);
		String i18nkey = sfd.showDialog(preselect_key, preselect_language);
		sfd.setModal(false);

		if (i18nkey != null && i18nkey.length() != 0)
		{
			return i18nkey;
		}
		return null;
	}

	public Date showCalendar(String pattern, Date date)
	{
		JDateChooser chooser = (JDateChooser)getWindow("JDateChooser"); //$NON-NLS-1$
		Window windowParent = getMainApplicationFrame();
		if (chooser == null || SwingUtilities.getWindowAncestor(chooser) != windowParent)
		{
			if (chooser != null)
			{
				chooser.dispose();
				chooser = null;
				registerWindow("JDateChooser", chooser);
			}
			String dateFormat = TagResolver.getFormatString(Date.class, this);
			chooser = new JDateChooser((JFrame)windowParent, getI18NMessage("servoy.dateChooser.selectDate"), //$NON-NLS-1$
				dateFormat);
			registerWindow("JDateChooser", chooser);
		}


		if (date != null)
		{
			Calendar cal = chooser.getSelectedDate();
			cal.setTime(date);
			chooser.updateCalendar(cal);

		}
		if (chooser.showDialog(pattern) == JDateChooser.ACCEPT_OPTION)
		{
			Calendar selectedDate = chooser.getSelectedDate();
			return selectedDate.getTime();
		}

		return null;
	}

	private Window getUserWindow(String windowName)
	{
		Window w = null;
		if (windowName == null)
		{
			// no name specified; use default dialog if it is showing, or else the main application window
			w = getWindow(IFormManagerInternal.USER_WINDOW_PREFIX + FormManager.DEFAULT_DIALOG_NAME);
			if (w == null || (!w.isShowing()))
			{
				w = getMainApplicationFrame();
			}
		}
		else
		{
			// we use the window with the given name, if found
			w = getWindow(IFormManagerInternal.USER_WINDOW_PREFIX + windowName);
		}

		return w;
	}


	public void beep()
	{
		Toolkit.getDefaultToolkit().beep();
	}

	public void setClipboardContent(String string)
	{
		WebStart.setClipboardContent(string);
	}

	public String getClipboardString()
	{
		return WebStart.getClipboardString();
	}

	private KeyEventDispatcher dispatcher = null;

	public void setNumpadEnterAsFocusNextEnabled(boolean enabled)
	{
		if (dispatcher == null)
		{
			dispatcher = new KeyEventDispatcher()

			{
				public boolean dispatchKeyEvent(KeyEvent e)
				{
					if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD && e.getKeyCode() == 10) //numpad enter key
					{
						e.setKeyChar('\t');
						e.setKeyCode(9);
					}
					return false;
				}
			};
		}
		if (enabled)
		{
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
		}
		else
		{
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
			dispatcher = null;
		}
	}


	// special hack for FixedJTable
	private int paintTableImmediately = 0;

	public void setPaintTableImmediately(boolean b)
	{
		if (b) paintTableImmediately--;
		else paintTableImmediately++;

	}

	public int getPaintTableImmediately()
	{
		return paintTableImmediately;
	}

	public String showColorChooser(String originalColor)
	{
		Color org = PersistHelper.createColor(originalColor);
		CustomColorChooserDialog ccd = (CustomColorChooserDialog)getWindow("CustomColorChooserDialog"); //$NON-NLS-1$
		if (ccd == null || ccd.getOwner() != getMainApplicationFrame())
		{
			ccd = new CustomColorChooserDialog(getMainApplicationFrame(), this);
			registerWindow("CustomColorChooserDialog", ccd);
		}
		Color c = ccd.showDialog(org);
		if (c != null)
		{
			return PersistHelper.createColorString(c);
		}
		return null;
	}

	public String showFontChooser(String fontString)
	{
		Font font = PersistHelper.createFont(fontString);
		JFontChooser chooser = new JFontChooser(getMainApplicationFrame(), font);
		int but = chooser.showDialog(getMainApplicationFrame(), getI18NMessage("servoy.fontchooser.title"), false); //$NON-NLS-1$
		if (but == IPropertyEditorDialog.OK_OPTION)
		{
			Font f = chooser.getSelectedFont();
			if (f != null)
			{
				return PersistHelper.createFontString(f);
			}
		}
		return null;
	}

	// these methods are added because this class extends JPanel which is serializable
	private void writeObject(ObjectOutputStream stream) throws IOException
	{
		throw new IOException("A Servoy client is not serializable"); //$NON-NLS-1$
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		throw new IOException("A Servoy client is not serializable"); //$NON-NLS-1$
	}

	private boolean isFormElementsEditableInFindMode = true;

	/*
	 * @see com.servoy.j2db.IApplication#setFormElementsEditableInFindMode(boolean)
	 */
	public void setFormElementsEditableInFindMode(boolean editable)
	{
		isFormElementsEditableInFindMode = editable;
	}

	/*
	 * @see com.servoy.j2db.IApplication#isFormElementsEditableInFindMode()
	 */
	public boolean isFormElementsEditableInFindMode()
	{
		return isFormElementsEditableInFindMode;
	}

	@Override
	public String getFormNameFor(IComponent component)
	{
		if (component instanceof Component)
		{
			Container parent = ((Component)component).getParent();
			while (!(parent instanceof IFormUI))
			{
				parent = parent.getParent();
			}
			return ((IFormUI)parent).getController().getName();
		}
		return "";
	}
}
