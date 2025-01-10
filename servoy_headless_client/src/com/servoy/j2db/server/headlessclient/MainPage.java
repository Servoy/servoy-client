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
package com.servoy.j2db.server.headlessclient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.border.Border;

import org.apache.wicket.Component;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Main page being a main container.
 */
public class MainPage extends Component implements IMainContainer
{
	private static final long serialVersionUID = 1L;

	private static final String DIV_DIALOG_REPEATER_ID = "divdialogs";
	private static final String DIV_DIALOGS_REPEATER_PARENT_ID = "divdialogsparent";
	private static final String FILE_UPLOAD_DIALOG_ID = "fileuploaddialog";
	private static final String FILE_UPLOAD_PAGEMAP = "fileupload";
	private static final String COOKIE_PREFIX = "dialog_";

	private int inputNameIds;


	private IApplication client;
	private Component title;
	private Component body;
	private Component listview;
	private List<IFormUIInternal< ? >> webForms;
	private WebForm main;

	private MainPage callingContainer;
	private final boolean closingAsDivPopup = false;
	private final boolean closingAChildDivPopoup = false;
	private final boolean closingAsWindow = false;

	private boolean showingInDialog = false;
	private boolean showingInWindow = false;

	private FormController currentForm;

	private History history;

	private Component componentToFocus;

	private Component focusedComponent;

	private boolean mustFocusNull;

	private transient boolean mainFormSwitched;
	private transient boolean versionNeedsPushing;

	private String statusText;

	private IMediaUploadCallback mediaUploadCallback;

	private ShowUrlInfo showUrlInfo;

	private boolean useAJAX;

	private boolean mediaUploadMultiSelect;
	private String mediaUploadFilter;

	private Dimension size = null; // keeps the size in case of browser windows (non-modal windows); not used for dialogs;

	private boolean tempRemoveMainForm = false;

	int minimumVersionNumber = -1;

	private boolean storeMinVersion;

	/**
	 *
	 */
	public void storeMinVersion()
	{
		// set the boolean that is used afterRender
		// because only then the actual version number is set.
		storeMinVersion = true;
	}


	public MainPage(IApplication sc)
	{
		super("main_page");
		init(sc);
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return "indicator"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getContainerName()
	 */
	public String getContainerName()
	{
		return "";
	}

	public boolean isUsingAjax()
	{
		return useAJAX;
	}

	@SuppressWarnings("nls")
	private void init(IApplication sc)
	{
		client = sc;
		webForms = new ArrayList<IFormUIInternal< ? >>();

		title = new Component("title"); //$NON-NLS-1$
		add(title);

		body = new Component("servoy_page"); //$NON-NLS-1$


		add(body);
		Component loadingIndicator = new Component("loading_indicator");
		body.add(loadingIndicator);

		if (useAJAX)
		{
		}
		else
		{
			body.add(new Component("fileuploaddialog")); //$NON-NLS-1$
		}


		Component main_form_style = new Component("main_form_style"); //$NON-NLS-1$
		add(main_form_style);


		listview = new Component("forms"); //$NON-NLS-1$
		// if versioning is disabled then table views can go wrong (don't rollback on a submit)
		//listview.setVersioned(false);

		Component form = new ServoyForm("servoy_dataform"); //$NON-NLS-1$


		form.add(listview);
		Component defaultButton = new Component("defaultsubmitbutton"); //$NON-NLS-1$
		defaultButton.setVisible(!useAJAX);
		form.add(defaultButton);

		body.add(form);
	}

	protected void restoreFocusedComponentInParentIfNeeded()
	{
		// if you open the dialog from a button for example, then you close it with X button (which is in the same parent main page)
		// the button will loose focus; so the following code restores focus to the button after the window was closed
		Component pageFocusedComponent = getFocusedComponent();
		if (pageFocusedComponent != null && componentToFocus == null) // if componentToFocus is already set, use that rather then overriding it
		{
			componentToFocus(pageFocusedComponent);
		}
	}

	/**
	 * Specifies if the main page is running in an additional iframe of the main window.
	 *
	 * @return true if the main page is in an additional window, false otherwise
	 */
	public boolean isShowingInDialog()
	{
		return showingInDialog;
	}

	/**
	 * Specifies if the main page is running in the main window.
	 *
	 * @return true if the main page is in the main window, false otherwise
	 */
	public boolean isShowingInWindow()
	{
		return showingInWindow;
	}


	public void flushCachedItems()
	{
		if (main != null) main.setMainPage(null);
		main = null;
		currentForm = null;
		navigator = null;
		componentToFocus = null;
		// both can't be set to null, else a login solution with a dialog wont close that dialog.
//		callingContainer = null;
//		closePopup = false;
		showingInDialog = false;
		showingInWindow = false;
		showUrlInfo = null;
		mainFormSwitched = false;
		versionNeedsPushing = false;

		webForms.clear();
		listview.removeAll();
		if (history != null)
		{
			history.clear();
		}
	}

	public void show(String name)
	{
		//ignore, add does all the work
	}

	public void showBlankPanel()
	{
		//not needed in webclient
	}

	public void add(final IComponent c, final String name)
	{
		// a new main form is added clear everything
		webForms.clear();
		FormController currentNavigator = navigator;
		Component container = (Component)c;
		if (!"webform".equals(container.getId())) //$NON-NLS-1$
		{
			throw new RuntimeException("only webforms with the name webform can be added to this mainpage"); //$NON-NLS-1$
		}

		if (main != null)
		{
			if (main.getMainPage() != this) main.setMainPage(null);
		}
		listview.removeAll();

		((WebForm)container).setMainPage(this);
		main = (WebForm)container;
		webForms.add(main);
		navigator = currentNavigator;
		if (navigator != null)
		{
			webForms.add(navigator.getFormUI());
		}

		/*
		 * if (navigator != null) { calculateFormAndNavigatorSizes(); }
		 */
	}

	public void setCallingContainerIfNull(MainPage callingContainer)
	{
		if (this.callingContainer == null) this.callingContainer = callingContainer;
	}

	/**
	 * @return the callingContainer
	 */
	public MainPage getCallingContainer()
	{
		return callingContainer;
	}

	public void remove(IComponent c)
	{
		if (main == c)
		{
			main = null;
			setController(null);
		}
		if (webForms.remove(c))
		{
			listview.removeAll();
		}
	}

	@Override
	public void setComponentVisible(boolean b)
	{
	}

	@Override
	public void setBackground(Color cbg)
	{
	}

	@Override
	public void setBorder(Border b)
	{
	}

	@Override
	public void setCursor(Cursor predefinedCursor)
	{
	}

	@Override
	public void setFont(Font f)
	{
	}

	@Override
	public void setForeground(Color cfg)
	{
	}

	@Override
	public void setLocation(Point loc)
	{
	}

	@Override
	public void setName(String name)
	{
		// ignore, can only be set through constructor (as id)
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	@Override
	public String getName()
	{
		return getId();
	}


	@Override
	public void setOpaque(boolean b)
	{
	}

	@Override
	public void setSize(Dimension size)
	{
	}

	@Override
	public void setToolTipText(String tooltip)
	{
	}

	private FormController navigator;

	public FormController getNavigator()
	{
		return navigator;
	}

	public FormController setNavigator(FormController c)
	{
		if (c == navigator) return c;
		FormController retval = navigator;
		if (c == null && navigator != null)
		{
			webForms.remove(navigator.getFormUI());
			listview.removeAll();
			navigator = null;
			return retval;
		}
		else if (navigator != null)
		{
			webForms.remove(navigator.getFormUI());
			listview.removeAll();
		}

		navigator = c;
		WebForm form = (WebForm)c.getFormUI();
		if (!webForms.contains(form))
		{
			webForms.add(form);
			listview.removeAll();
		}
		/*
		 * if (navigator != null) { calculateFormAndNavigatorSizes(); } else { dynamicCSSContent = ""; //$NON-NLS-1$ }
		 */

		return retval;
	}

	/**
	 * @return
	 */
	public IFormUIInternal<Component> getMainWebForm()
	{
		return main;
	}

	public void setTitle(String name)
	{
		Solution solution = this.client.getSolution();
		String titleString = ""; //$NON-NLS-1$
		String solutionTitle = solution.getTitleText();

		if (solutionTitle == null)
		{
			titleString = solution.getName();
		}
		else if (!solutionTitle.equals("<empty>")) //$NON-NLS-1$
		{
			titleString = solutionTitle;
		}

		titleString = client.getI18NMessageIfPrefixed(titleString);

		if (name != null && !name.trim().equals("") && !"<empty>".equals(name) && main != null) //$NON-NLS-1$ //$NON-NLS-2$
		{
			String nameString = client.getI18NMessageIfPrefixed(name);
			FormController formController = main.getController();
			if (formController != null)
			{
				String name2 = Text.processTags(nameString, formController.getTagResolver());
				if (name2 != null) nameString = name2;

			}
			else
			{
				String name2 = Text.processTags(nameString, TagResolver.createResolver(new PrototypeState(null)));
				if (name2 != null) nameString = name2;
			}
			if (!nameString.trim().equals("")) //$NON-NLS-1$
			{
				if ("".equals(titleString)) //$NON-NLS-1$
				{
					titleString += nameString;
				}
				else
				{
					titleString += " - " + nameString; //$NON-NLS-1$
				}
			}
		}
		String appName = "Servoy Web Client"; //$NON-NLS-1$
		boolean branding = Utils.getAsBoolean(client.getSettings().getProperty("servoy.branding", "false")); //$NON-NLS-1$ //$NON-NLS-2$
		String appTitle = client.getSettings().getProperty("servoy.branding.windowtitle"); //$NON-NLS-1$
		if (branding && appTitle != null)
		{
			appName = appTitle;
		}
		if (titleString.equals("")) //$NON-NLS-1$
		{
			titleString = appName;
		}
		else
		{
			titleString += " - " + appName; //$NON-NLS-1$
		}
	}

	public String nextInputNameId()
	{
		return Integer.toString(inputNameIds++);
	}

	public void setShowURLCMD(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		showUrlInfo = new ShowUrlInfo(url, target, target_options, timeout, onRootFrame, (target == null || target.equals("_self")));
	}

	/**
	 * @return
	 */
	public ShowUrlInfo getShowUrlInfo()
	{
		return showUrlInfo;
	}

	@SuppressWarnings("nls")
	public static String getShowUrlScript(ShowUrlInfo showUrlInfo, Properties props)
	{
		if (showUrlInfo != null)
		{
			String url = showUrlInfo.url;
			if (url != null)
			{
				url = url.replace("'", "\\'");
			}
			if (showUrlInfo.target.equalsIgnoreCase("_close"))
			{
				return "window.close();window.opener.location.reload(true)";
			}
			else if (showUrlInfo.target.equalsIgnoreCase("_self"))
			{
				return "showurl('" + url + "'," + showUrlInfo.timeout + "," + showUrlInfo.onRootFrame + "," + showUrlInfo.useIFrame + "," +
					showUrlInfo.pageExpiredRedirect + ");";
			}
			else if (showUrlInfo.target.equalsIgnoreCase("_top"))
			{
				String script = "window.top.location.href='" + url + "';";
				return "window.setTimeout(\"" + script + "\"," + showUrlInfo.timeout + ");";
			}
			else
			{
				StringBuilder script = new StringBuilder();
				if (!"_blank".equals(showUrlInfo.target))
				{
					script.append("if (top.window.frames['" + showUrlInfo.target + "'])");
					script.append("{top.window.frames['" + showUrlInfo.target + "'].document.location.href = '" + url + "';}else{");
				}
				if (showUrlInfo.target_options != null)
				{
					script.append("window.open('" + url + "','" + showUrlInfo.target + "','" + showUrlInfo.target_options + "');");
				}
				else
				{
					script.append("window.open('" + url + "','" + showUrlInfo.target + "');");
				}
				if (!"_blank".equals(showUrlInfo.target))
				{
					script.append("}");
				}
				if (showUrlInfo.timeout != 0)
				{
					return "window.setTimeout(\"" + script + "\"," + showUrlInfo.timeout + ");";
				}
				return script.toString();
			}
		}
		return null;
	}

	public String getShowUrlScript()
	{
		if (showUrlInfo != null)
		{
			try
			{
				return getShowUrlScript(showUrlInfo, client.getSettings());
			}
			finally
			{
				showUrlInfo = null;
			}
		}
		return null;
	}

	@Override
	public Color getBackground()
	{
		return null;
	}

	@Override
	public Border getBorder()
	{
		return null;
	}

	@Override
	public Font getFont()
	{
		return null;
	}

	@Override
	public Color getForeground()
	{
		return null;
	}

	@Override
	public Point getLocation()
	{
		return null;
	}

	@Override
	public Dimension getSize()
	{
		return null;
	}

	@Override
	public boolean isOpaque()
	{
		return false;
	}

	@Override
	public void setComponentEnabled(boolean enabled)
	{
	}

	public Iterator<IComponent> getComponentIterator()
	{
		return null;
	}

	public void setController(IFormController f)
	{
		if (currentForm != f)
		{
			mainFormSwitched = true;
			versionNeedsPushing = true;
			this.currentForm = (FormController)f;
		}
		if (currentForm == null)
		{
			if (navigator != null)
			{
				webForms.remove(navigator.getFormUI());
			}
			navigator = null;
		}
	}

	public void setController(IFormController f, List<Runnable> invokeLaterRunnables)
	{
		setController(f);
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getController()
	 */
	public FormController getController()
	{
		boolean destroyed = false;
		if (currentForm != null && currentForm.isDestroyed())
		{
			// if the current form is destroyed, try to fix this by getting the same (new) form from the form manager
			currentForm = (FormController)client.getFormManager().getForm(currentForm.getName());
			destroyed = true;
		}
		if (navigator != null && navigator.isDestroyed())
		{
			navigator = (FormController)client.getFormManager().getForm(navigator.getName());
			destroyed = true;
		}
		if (destroyed && currentForm != null)
		{
			add(currentForm.getFormUI(), currentForm.getName());
		}
		return currentForm;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getHistory()
	 */
	public History getHistory()
	{
		if (history == null)
		{
			history = new FormManager.History(client, this);
		}
		return history;
	}

	public void setFocusedComponent(Component component)
	{
		focusedComponent = component;
	}

	public Component getFocusedComponent()
	{
		return focusedComponent;
	}

	/**
	 * @param webDataField
	 */
	public void componentToFocus(Component component)
	{
		if (component instanceof IDelegate< ? >)
		{
			Object delegate = ((IDelegate< ? >)component).getDelegate();
			if (delegate instanceof Component)
			{
				componentToFocus = (Component)delegate;
				return;
			}
		}
		componentToFocus = component;
	}

	/**
	 *
	 */
	public Component getAndResetToFocusComponent()
	{
		Component c = componentToFocus;
		componentToFocus = null;
		if (c != null) focusedComponent = c;
		return c;
	}

	/**
	 * Sets the must-focus-null flag. This means than, if no other component is to be focused, then wicket should not call focus again for the previously
	 * focused component. This is needed for example when showing a non-modal dialog in IE7 (or otherwise the new window would be displayed in the background).
	 */
	public void mustFocusNullIfNoComponentToFocus()
	{
		mustFocusNull = true;
	}

	/**
	 * Says whether or not the focus component should be set to null for the ajax request target if no other component is to be focused
	 * (getAndResetToFocusComponent() returns null).
	 *
	 * @return true if the focus component should be set to null for the ajax request target if no other component is to be focused
	 *         (getAndResetToFocusComponent() returns null).
	 */
	public boolean getAndResetMustFocusNull()
	{
		boolean value = mustFocusNull;
		mustFocusNull = false;
		return value;
	}

	public boolean isMainFormSwitched()
	{
		return mainFormSwitched;
	}

	// only create new page version once (between the time when the new version is created and the time the new version of the page
	// is fully rendered to the browser (in a subsequent req.), there is a possibility that another AJAX request comes in
	// and we don't want that one bumping the version again)
	public void versionPush()
	{
		versionNeedsPushing = false;
	}

	public boolean isClosingAsDivPopup()
	{
		return closingAsDivPopup;
	}

	public boolean isAChildPopupClosing()
	{
		return closingAChildDivPopoup;
	}

	/**
	 * @param tempRemoveMainForms the tempRemoveMainForms to set
	 */
	public void setTempRemoveMainForm(boolean tempRemoveMainForms)
	{
		this.tempRemoveMainForm = tempRemoveMainForms;
	}

	public static class ShowUrlInfo implements Serializable
	{

		private final String url;
		private final String target;
		private final String target_options;
		private final int timeout;
		private boolean onRootFrame;
		private boolean useIFrame;
		private final boolean pageExpiredRedirect;

		public ShowUrlInfo(String url, String target, String target_options, int timeout, boolean onRootFrame, boolean useIFrame)
		{
			this(url, target, target_options, timeout, onRootFrame, useIFrame, false);
		}

		public ShowUrlInfo(String url, String target, String target_options, int timeout, boolean onRootFrame, boolean useIFrame, boolean pageExpiredRedirect)
		{
			this.url = url;
			this.useIFrame = useIFrame;
			this.target = target == null ? "_blank" : target; //$NON-NLS-1$
			this.target_options = target_options;
			this.timeout = timeout * 1000;
			this.onRootFrame = onRootFrame;
			this.pageExpiredRedirect = pageExpiredRedirect;
		}

		public String getUrl()
		{
			return url;
		}

		public String getTarget()
		{
			return target;
		}

		public void setUseIFrame(boolean useIFrame)
		{
			this.useIFrame = useIFrame;
		}

		public void setOnRootFrame(boolean onRootFrame)
		{
			this.onRootFrame = onRootFrame;
		}

	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		return null;
	}

	public String getStatusText()
	{
		return statusText;
	}

	public void setStatusText(String statusText)
	{
		this.statusText = statusText;

	}

	public void requestFocus()
	{
	}

	public boolean isApplicationShutDown()
	{
		return client.getPluginManager() == null;
	}

	public void setMainPageSwitched()
	{
		mainFormSwitched = true;
		versionNeedsPushing = true;
	}

	public void getMainPageReversedCloseSeq(ArrayList<String> al, Set<String> visited)
	{
		String name = this.getContainerName();
		if (!visited.contains(name))
		{
			visited.add(name);
			if (callingContainer != null)
			{
				callingContainer.getMainPageReversedCloseSeq(al, visited);
				if (!al.contains(name)) al.add(name);
			}
		}
	}


	/**
	 * Creates a browser javascript snippet that, when evaluated in the scriptExecutionMP (current request's main page) it will point to this MainPage's window object in the browser.
	 * @return the code snippet pointing to this MainPage's browser window from the scriptExecutionMP (current request's MainPage). The snippet will end with "." if it's not an empty String. It will return null if a way to access the desired window scope was not found.
	 */
	@SuppressWarnings("nls")
	private Pair<String, MainPage> getWindowScopeBrowserScript(MainPage scriptExecutionMP)
	{
		if (scriptExecutionMP == null) return null;
		if (scriptExecutionMP != this)
		{
			// generate a JS script that when ran inside browser for requestMP it will point to this main page;
			// find common parent and then generate script
			ArrayList<MainPage> requestMPsParents = new ArrayList<MainPage>();
			ArrayList<MainPage> thisMPsParents = new ArrayList<MainPage>();

			MainPage mp = scriptExecutionMP;
			while (mp != null && mp != this)
			{
				requestMPsParents.add(mp);
				mp = mp.callingContainer;
			}
			if (mp == this) requestMPsParents.add(this);

			mp = this;
			while (mp != null && !requestMPsParents.contains(mp))
			{
				thisMPsParents.add(mp);
				mp = mp.callingContainer;
			}

			int idx = requestMPsParents.indexOf(mp);
			if (idx != -1)
			{
				// found common parent; idx is the index in request page's parent array
				boolean ok = true;
				String goToCorrectScopeScript = "";
				for (int i = 0; i < idx && ok; i++)
				{
					// window.opener/parent depending on window type
					mp = requestMPsParents.get(i);
					if (mp.isShowingInDialog() || mp.isClosingAsDivPopup())
					{
						goToCorrectScopeScript += "window.parent.";
					}
					else if (mp.isShowingInWindow() || mp.closingAsWindow)
					{
						goToCorrectScopeScript += "window.opener.";
					}
					else ok = false; // some windows in the window chain are closed...
				}

				for (int i = thisMPsParents.size() - 1; i >= 0 && ok; i--)
				{
					mp = thisMPsParents.get(i);
					ok = false; // some windows in the window chain are closed...
				}

				if (ok)
				{
					return new Pair<String, MainPage>(goToCorrectScopeScript, scriptExecutionMP);
				}
				else
				{
					Debug.log("Cannot trigger ajax request between pages. Closed page.");
				}
			}
		}
		else
		{
			return new Pair<String, MainPage>("", this); // the request's page is actually this page; so we are already in the correct scope
		}
		return null;
	}

	public int getX()
	{
		return 0; // closed windows & non-modal browser windows are currently not aware of location
	}

	public int getY()
	{
		return 0; // closed windows & non-modal browser windows are currently not aware of location
	}

	public int getWidth()
	{
		return 0;
	}

	public int getHeight()
	{
		return 0;
	}

	private int orientation = -1;

	public int getOrientation()
	{
		return orientation;
	}

	private void setOrientation(String orientationString)
	{
		orientation = Utils.getAsInteger(orientationString);
	}

	public void setWindowSize(Dimension d)
	{
		size = d;
	}

	private static String getValidJSVariableName(String name)
	{
		return "v_" + name.replace('-', '_').replace(' ', '_').replace(':', '_'); //$NON-NLS-1$
	}


}
