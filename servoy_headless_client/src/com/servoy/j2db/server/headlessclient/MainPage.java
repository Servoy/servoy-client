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
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.border.Border;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IPageMap;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.PageMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.version.undo.Change;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.scripting.info.WEBCONSTANTS;
import com.servoy.j2db.server.headlessclient.PageJSActionBuffer.DivDialogAction;
import com.servoy.j2db.server.headlessclient.PageJSActionBuffer.JSChangeAction;
import com.servoy.j2db.server.headlessclient.PageJSActionBuffer.PageAction;
import com.servoy.j2db.server.headlessclient.PageJSActionBuffer.RenderComponentAction;
import com.servoy.j2db.server.headlessclient.dataui.AbstractServoyDefaultAjaxBehavior;
import com.servoy.j2db.server.headlessclient.dataui.AbstractServoyLastVersionAjaxBehavior;
import com.servoy.j2db.server.headlessclient.dataui.FormLayoutProviderFactory;
import com.servoy.j2db.server.headlessclient.dataui.IFormLayoutProvider;
import com.servoy.j2db.server.headlessclient.dataui.ISupportWebTabSeq;
import com.servoy.j2db.server.headlessclient.dataui.IWebFormContainer;
import com.servoy.j2db.server.headlessclient.dataui.PageContributorRepeatingView;
import com.servoy.j2db.server.headlessclient.dataui.StartEditOnFocusGainedEventBehavior;
import com.servoy.j2db.server.headlessclient.dataui.StyleAppendingModifier;
import com.servoy.j2db.server.headlessclient.dataui.StylePropertyChangeMarkupContainer;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.headlessclient.dataui.WebBaseSelectBox;
import com.servoy.j2db.server.headlessclient.dataui.WebEventExecutor;
import com.servoy.j2db.server.headlessclient.dataui.WebSplitPane;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel;
import com.servoy.j2db.server.headlessclient.eventthread.WicketEvent;
import com.servoy.j2db.server.headlessclient.jquery.JQueryLoader;
import com.servoy.j2db.server.headlessclient.tinymce.TinyMCELoader;
import com.servoy.j2db.server.headlessclient.util.HCUtils;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Main page being a main container.
 */
public class MainPage extends WebPage implements IMainContainer, IAjaxIndicatorAware
{
	private static final long serialVersionUID = 1L;

	private static final String DIV_DIALOG_REPEATER_ID = "divdialogs";
	private static final String DIV_DIALOGS_REPEATER_PARENT_ID = "divdialogsparent";
	private static final String FILE_UPLOAD_DIALOG_ID = "fileuploaddialog";
	private static final String FILE_UPLOAD_PAGEMAP = "fileupload";
	private static final String COOKIE_PREFIX = "dialog_";

	private final static ResourceReference servoy_js = new JavascriptResourceReference(MainPage.class, "servoy.js"); //$NON-NLS-1$


	private int inputNameIds;


	private WebClient client;
	private Label title;
	private PageContributor pageContributor;
	private WebMarkupContainer body;
	private ListView<IFormUIInternal< ? >> listview;
	private List<IFormUIInternal< ? >> webForms;
	private WebForm main;

	private WebMarkupContainer divDialogsParent;
	private RepeatingView divDialogRepeater;
	private final PageJSActionBuffer jsActionBuffer = new PageJSActionBuffer();
	private final DivDialogsKeeper divDialogs = new DivDialogsKeeper();

	private MainPage callingContainer;
	private boolean closingAsDivPopup = false;
	private boolean closingAChildDivPopoup = false;
	private boolean closingAsWindow = false;

	private boolean showingInDialog = false;
	private boolean showingInWindow = false;

	public ResourceReference serveResourceReference = new ResourceReference("resources"); //$NON-NLS-1$

	private FormController currentForm;

	private History history;

	private Component componentToFocus;

	private Component focusedComponent;

	private boolean mustFocusNull;

	private transient boolean mainFormSwitched;
	private transient boolean versionNeedsPushing;

	private final IValueMap bodyAttributes = new ValueMap();

	private String statusText;
	private SetStatusBehavior setStatusBehavior = null;

	private ServoyDivDialog fileUploadWindow;
	private IMediaUploadCallback mediaUploadCallback;

	private ShowUrlInfo showUrlInfo;

	private boolean useAJAX;

	private boolean mediaUploadMultiSelect;

	private Dimension size = null; // keeps the size in case of browser windows (non-modal windows); not used for dialogs;

	private boolean tempRemoveMainForm = false;


	private class DivDialogsKeeper
	{
		private final HashMap<String, ServoyDivDialog> divDialogsMap = new HashMap<String, ServoyDivDialog>();
		private final List<ServoyDivDialog> dialogsOrderedByOpenSequence = new ArrayList<ServoyDivDialog>(); // useful for knowing which modal is on top of which other modal

		public ServoyDivDialog remove(String pageMapName)
		{
			ServoyDivDialog dd = divDialogsMap.remove(pageMapName);
			if (dd != null) dialogsOrderedByOpenSequence.remove(dd);
			return dd;
		}

		public int size()
		{
			return dialogsOrderedByOpenSequence.size();
		}

		public void put(String pageMapName, ServoyDivDialog divDialog)
		{
			ServoyDivDialog oldDivDialog = divDialogsMap.put(pageMapName, divDialog);
			if (oldDivDialog != null) dialogsOrderedByOpenSequence.remove(oldDivDialog);
			dialogsOrderedByOpenSequence.add(divDialog);
		}

		public ServoyDivDialog get(String pageMapName)
		{
			return divDialogsMap.get(pageMapName);
		}

		public List<ServoyDivDialog> getOrderedByOpenSequence()
		{
			return dialogsOrderedByOpenSequence;
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.wicket.Page#getVersion(int)
	 */
	@Override
	public Page getVersion(int versionNumber)
	{
		final WebClient wc = WebClientSession.get() != null ? WebClientSession.get().getWebClient() : null;
		boolean prev = false;
		try
		{
			if (wc != null) prev = wc.blockEventExecution(true);
			// don't let the page version number go past the minimum that is set.
			if (versionNumber != -1 && minimumVersionNumber != -1 && versionNumber < minimumVersionNumber)
			{
				return super.getVersion(minimumVersionNumber);
			}
			return super.getVersion(versionNumber);
		}
		finally
		{
			if (wc != null) wc.blockEventExecution(prev);
		}
	}

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

	private class SetStatusBehavior extends AbstractBehavior
	{
		private String text;

		public void setStatusText(String text)
		{
			this.text = text;
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			String jsCall = "setStatusText('" + text + "');"; //$NON-NLS-1$ //$NON-NLS-2$
			response.renderOnLoadJavascript(jsCall);
		}
	}

	/**
	 * This behavior is useful when a request to one page generates changes to another page's content that should show quickly (rather then waiting for timer request on the latter).
	 * By calling through javascript the 'triggerAjaxUpdate', a request will be generated on the modified page.
	 */
	private class TriggerUpdateAjaxBehavior extends AbstractServoyLastVersionAjaxBehavior
	{

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			response.renderJavascript("function triggerAjaxUpdate() {setTimeout(\"" + getCallbackScript(true) + "\", 0);}", "triggerAjaxUpdate"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		@Override
		protected void execute(AjaxRequestTarget target)
		{
			if (main != null)
			{
				WebEventExecutor.generateResponse(target, getPage());
			}
		}

		@Override
		protected CharSequence getCallbackScript(boolean onlyTargetActivePage)
		{
			return generateCallbackScript("wicketAjaxGet('" + getCallbackUrl(onlyTargetActivePage) + "&ignoremp=true'");
		}

	}


	public MainPage(PageParameters pp)
	{
		super();
		if (pp.getString(StartupArguments.PARAM_KEY_SOLUTION) != null || pp.getString(StartupArguments.PARAM_KEY_SHORT_SOLUTION) != null)
		{
			throw new RestartResponseException(SolutionLoader.class, pp);
		}
		else
		{
			throw new RestartResponseException(SelectSolution.class);
		}
	}

	public MainPage(WebClient sc)
	{
		super();
		init(sc);
	}

	public MainPage(WebClient sc, IPageMap pagemap)
	{
		super(pagemap);
		init(sc);
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return WebClientSession.get().hideLoadingIndicator() ? null : "indicator"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getContainerName()
	 */
	public String getContainerName()
	{
		return getPageMap().getName();
	}

	public boolean isUsingAjax()
	{
		return useAJAX;
	}

	@SuppressWarnings("nls")
	private void init(WebClient sc)
	{
		setStatelessHint(false);
		client = sc;
		webForms = new ArrayList<IFormUIInternal< ? >>();

		title = new Label("title", new Model<String>("Servoy Web Client")); //$NON-NLS-1$ //$NON-NLS-2$
		title.setOutputMarkupId(true);
		add(title);

		useAJAX = Utils.getAsBoolean(client.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		int dataNotifyFrequency = Utils.getAsInteger(sc.getSettings().getProperty("servoy.webclient.datanotify.frequency", "5")); //$NON-NLS-1$  //$NON-NLS-2$
		if (dataNotifyFrequency > 0 && useAJAX)
		{
			add(new AbstractAjaxTimerBehavior(Duration.seconds(dataNotifyFrequency))
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onTimer(AjaxRequestTarget target)
				{
					if (isServoyEnabled() && !client.getFlattenedSolution().isInDesign(null) &&
						String.valueOf(MainPage.this.getCurrentVersionNumber()).equals(RequestCycle.get().getRequest().getParameter("pvs")))
					{
						WebEventExecutor.generateResponse(target, MainPage.this);
					}
				}

				@Override
				public void renderHead(IHeaderResponse response)
				{
					if (isServoyEnabled())
					{
						super.renderHead(response);

						String jsTimerScript = getJsTimeoutCall(getUpdateInterval());
						response.renderJavascript("function restartTimer() {" + jsTimerScript + "}", "restartTimer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}

				@Override
				protected CharSequence getPreconditionScript()
				{
					return "onAjaxCall(); if(Servoy.DD.isDragging) Servoy.DD.isRestartTimerNeeded=true; return !Servoy.DD.isDragging && !Servoy.redirectingOnSolutionClose;"; //$NON-NLS-1$
				}

				@Override
				protected CharSequence getFailureScript()
				{
					return "onAjaxError();restartTimer();"; //$NON-NLS-1$
				}

				@Override
				protected CharSequence getCallbackScript()
				{
					// if it is not enabled then just return an empty function. so that the timer stops.
					if (isServoyEnabled())
					{
						return generateCallbackScript("wicketAjaxGet('" + //$NON-NLS-1$
							getCallbackUrl(onlyTargetActivePage()) + "&ignoremp=true&pvs=" + MainPage.this.getCurrentVersionNumber() + "'"); //$NON-NLS-1$
					}
					return "Servoy.Utils.nop()";
				}

				@Override
				public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
				{
					if (getComponent() == null)
					{
						throw new IllegalArgumentException("Behavior must be bound to a component to create the URL"); //$NON-NLS-1$
					}
					return getComponent().urlFor(this, AlwaysLastPageVersionRequestListenerInterface.INTERFACE);
				}

				@Override
				protected String findIndicatorId()
				{
					return null; // main page defines it and the timer shouldnt show it
				}

				/*
				 * this can't be isEnabled(component) of the behavior itself because IE8 will constant call this on closed (modal)windows. So then this is
				 * marked as disabled and an AbortException is thrown what our code sees as a server error and will constantly restart the timer.
				 */
				private boolean isServoyEnabled()
				{
					return ((getController() != null && getController().isFormVisible()) || closingAsWindow);
				}

			});

		}
		add(new TriggerOrientationChangeAjaxBehavior());
		add(new TriggerResizeAjaxBehavior());

		add(new AbstractServoyLastVersionAjaxBehavior()
		{
			@Override
			protected void execute(AjaxRequestTarget target)
			{
				for (ServoyDivDialog divDialog : divDialogs.getOrderedByOpenSequence())
				{
					if (!divDialog.isShown())
					{
						// this means a refresh was probably triggered and we must re-show these... because we do not keep closed dialogs in divDialogs
						int x = divDialog.getX();
						int y = divDialog.getY();
						int w = divDialog.getWidth();
						int h = divDialog.getHeight();
						divDialog.show(target, null);
						divDialog.setBounds(target, x, y, w, h, null);
					}
				}
				WebEventExecutor.generateResponse(target, MainPage.this);
			}

			@Override
			public void renderHead(IHeaderResponse response)
			{
				super.renderHead(response);
				response.renderOnDomReadyJavascript(getCallbackScript(true).toString());
			}

			@Override
			public boolean isEnabled(Component component)
			{
				return divDialogs.size() > 0 && super.isEnabled(component);
			}
		});

		body = new WebMarkupContainer("servoy_page") //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see org.apache.wicket.Component#onComponentTag(org.apache.wicket.markup.ComponentTag)
			 */
			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				tag.putAll(bodyAttributes);
			}
		};
		body.add(new AttributeModifier("dir", true, new AbstractReadOnlyModel<String>() //$NON-NLS-1$
		{

			@Override
			public String getObject()
			{
				String value = AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE;
				Locale l = client.getLocale();
				Solution solution = client.getSolution();

				if (solution != null && l != null)
				{
					value = OrientationApplier.getHTMLContainerOrientation(l, solution.getTextOrientation());
				}
				return value;
			}
		}));

		add(body);
		pageContributor = new PageContributor(client, "contribution");
		body.add(pageContributor);

		Label loadingIndicator = new Label("loading_indicator", sc.getI18NMessage("servoy.general.loading"));
		loadingIndicator.add(new SimpleAttributeModifier("style", "display:none;"));

		body.add(loadingIndicator);
		divDialogsParent = new WebMarkupContainer(DIV_DIALOGS_REPEATER_PARENT_ID);
		divDialogsParent.setOutputMarkupPlaceholderTag(true);
		divDialogsParent.setVisible(false);
		body.add(divDialogsParent);

		if (useAJAX)
		{
			add(new TriggerUpdateAjaxBehavior()); // for when another page needs to trigger an ajax update on this page using js (see media upload)

			divDialogRepeater = new RepeatingView(DIV_DIALOG_REPEATER_ID);
			divDialogsParent.add(divDialogRepeater);

			fileUploadWindow = new ServoyDivDialog(FILE_UPLOAD_DIALOG_ID);
			body.add(fileUploadWindow);
			fileUploadWindow.setModal(true);
			fileUploadWindow.setPageMapName(null);
			fileUploadWindow.setCookieName("dialog_fileupload");
			fileUploadWindow.setResizable(true);
			fileUploadWindow.setInitialHeight(150);
			fileUploadWindow.setInitialWidth(400);
			fileUploadWindow.setMinimalHeight(130);
			fileUploadWindow.setMinimalWidth(400);
			fileUploadWindow.setUseInitialHeight(true); // no effect, can be removed
			fileUploadWindow.setPageCreator(new ModalWindow.PageCreator()
			{
				private static final long serialVersionUID = 1L;

				public Page createPage()
				{
					return new MediaUploadPage(PageMap.forName(FILE_UPLOAD_PAGEMAP), mediaUploadCallback, mediaUploadMultiSelect,
						getController().getApplication());
				}
			});
			fileUploadWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
			{
				private static final long serialVersionUID = 1L;

				public void onClose(AjaxRequestTarget target)
				{
					divDialogs.remove(FILE_UPLOAD_PAGEMAP);
					fileUploadWindow.setPageMapName(null);
					fileUploadWindow.remove(fileUploadWindow.getContentId());
					restoreFocusedComponentInParentIfNeeded();
					WebEventExecutor.generateResponse(target, findPage());
				}
			});
		}
		else
		{
			divDialogsParent.add(new Label("divdialogs"));
			body.add(new Label("fileuploaddialog")); //$NON-NLS-1$
		}

		IModel<String> styleHrefModel = new AbstractReadOnlyModel<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				if (main != null)
				{
					return getRequest().getRelativePathPrefixToContextRoot() + "servoy-webclient/templates/" + //$NON-NLS-1$
						client.getClientProperty(WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR) + "/servoy_web_client_default.css"; //$NON-NLS-1$
				}
				return null;
			}
		};
		Label main_form_style = new Label("main_form_style"); //$NON-NLS-1$
		main_form_style.add(new AttributeModifier("href", true, styleHrefModel)); //$NON-NLS-1$
		add(main_form_style);

		IModel<List<IFormUIInternal< ? >>> loopModel = new AbstractReadOnlyModel<List<IFormUIInternal< ? >>>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public List<IFormUIInternal< ? >> getObject()
			{
				return webForms;
			}
		};

		listview = new ListView<IFormUIInternal< ? >>("forms", loopModel) //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<IFormUIInternal< ? >> item)
			{
				final WebForm form = (WebForm)item.getModelObject();
				tempRemoveMainForm = true;
				try
				{
					if (form.getParent() != null)
					{
						form.remove(); // TODO isn't this already done by item.add(form) below in wicket's impl?
					}
					item.add(form);
				}
				finally
				{
					tempRemoveMainForm = false;
				}

				IFormLayoutProvider layoutProvider = FormLayoutProviderFactory.getFormLayoutProvider(client, client.getSolution(),
					form.getController().getForm(), form.getController().getName());

				TextualStyle styleToReturn = null;

				if ((navigator != null) && (form == navigator.getFormUI()))
				{
					styleToReturn = layoutProvider.getLayoutForForm(navigator.getForm().getSize().width, true, false);
				}
				else if (form == main)
				{
					int customNavWidth = 0;
					if (navigator != null) customNavWidth = navigator.getForm().getSize().width;
					styleToReturn = layoutProvider.getLayoutForForm(customNavWidth, false, false);
				}
				if (styleToReturn != null)
				{
					form.add(new StyleAppendingModifier(styleToReturn)
					{
						@Override
						public boolean isEnabled(Component component)
						{
							// Laurian: looks like bogus condition, shouldn't this be || as in WebForm ?
							return (component.findParent(WebTabPanel.class) == null) && (component.findParent(WebSplitPane.class) == null);
						}
					});
				}
				TabIndexHelper.setUpTabIndexAttributeModifier(item, ISupportWebTabSeq.SKIP);
			}

			@Override
			protected ListItem<IFormUIInternal< ? >> newItem(final int index)
			{
				return new ListItem<IFormUIInternal< ? >>(index, getListItemModel(getModel(), index))
				{
					@Override
					public void remove(Component component)
					{
						super.remove(component);
						// for example when a form is shown in a popup form (window plugin) it must know that it's main page changed
						if (!tempRemoveMainForm && component instanceof WebForm)
						{
							WebForm formUI = ((WebForm)component);
							if (MainPage.this == formUI.getMainPage())
							{
								// if the form is visible and it will be now removed from the mainpage
								// then call notifyVisble false on it to let the form know it will hide
								// we can't do much if that is blocked by an onhide here.
								// (could be triggered by a browser back button)
								if (formUI.getController().isFormVisible())
								{
									List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
									formUI.getController().notifyVisible(false, invokeLaterRunnables);
									Utils.invokeLater(client, invokeLaterRunnables);
								}
								formUI.setMainPage(null);
							}
						}
					}
				};
			}

			/**
			 * @see org.apache.wicket.markup.html.list.ListView#onBeforeRender()
			 */
			@Override
			protected void onBeforeRender()
			{
				super.onBeforeRender();
				// now first initialize all the tabs so that data from
				// tab x doesn't change anymore (so that it could alter data in tab y)
				// don't know if this still does anything because we need to do it
				// in the onBeforeRender of WebTabPanel itself, else tableviews don't have there models yet..
				visitChildren(WebTabPanel.class, new IVisitor<WebTabPanel>()
				{
					public Object component(WebTabPanel wtp)
					{
						wtp.initalizeFirstTab();
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});

				addWebAnchoringInfoIfNeeded();
			}
		};
		listview.setReuseItems(true);
		// if versioning is disabled then table views can go wrong (don't rollback on a submit)
		//listview.setVersioned(false);

		Form form = new ServoyForm("servoy_dataform"); //$NON-NLS-1$

		form.add(new SimpleAttributeModifier("autocomplete", "off")); //$NON-NLS-1$ //$NON-NLS-2$
		if (useAJAX) form.add(new SimpleAttributeModifier("onsubmit", "return false;")); //$NON-NLS-1$ //$NON-NLS-2$

		form.add(listview);
		WebMarkupContainer defaultButton = new WebMarkupContainer("defaultsubmitbutton", new Model()); //$NON-NLS-1$
		defaultButton.setVisible(!useAJAX);
		form.add(defaultButton);

		StylePropertyChangeMarkupContainer container = new StylePropertyChangeMarkupContainer("externaldivsparent");
		form.add(container);
		PageContributorRepeatingView repeatingView = new PageContributorRepeatingView("externaldivs", container);
		container.add(repeatingView);
		pageContributor.addRepeatingView(repeatingView);

		body.add(form);
	}

	private ServoyDivDialog createDivDialog(MainPage dialogContainer, String name)
	{
		final ServoyDivDialog divDialog = new ServoyDivDialog(divDialogRepeater.newChildId());
		divDialog.setPageMapName(null);
		divDialog.setCookieName(COOKIE_PREFIX + name);
		divDialog.setModal(true);
		dialogContainer.showingInDialog = true;
		dialogContainer.showingInWindow = false;
		divDialog.setPageCreator(new ModalWindow.PageCreator()
		{
			private static final long serialVersionUID = 1L;

			public Page createPage()
			{
				return (MainPage)((FormManager)client.getFormManager()).getOrCreateMainContainer(divDialog.getPageMapName());
			}
		});
		divDialog.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
		{
			private static final long serialVersionUID = 1L;

			public void onClose(AjaxRequestTarget target)
			{
				divDialogRepeater.remove(divDialog);
				String divDialogPageMapName = divDialog.getPageMapName();
				if (divDialogs.get(divDialogPageMapName) == divDialog)
				{
					divDialogs.remove(divDialogPageMapName);
				}
				if (divDialogs.size() == 0)
				{
					divDialogsParent.setVisible(false);
				}
				else
				{
					addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_DIALOG_ADDED_OR_REMOVED, new Object[] { divDialogsParent }));
				}
				divDialog.setPageMapName(null);

				restoreFocusedComponentInParentIfNeeded();

				WebEventExecutor.generateResponse(target, findPage());
			}
		});
		divDialog.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
		{
			private static final long serialVersionUID = 1L;

			public boolean onCloseButtonClicked(AjaxRequestTarget target)
			{
				if (!divDialog.isShown())
				{
					return false; // double clicked?
				}

				FormManager fm = ((FormManager)client.getFormManager());
				IMainContainer divDialogContainer = fm.getMainContainer(divDialog.getPageMapName());
				IMainContainer currentContainer = fm.getCurrentContainer();

				// get a lock on the dialog container (form onHide code will execute, make sure another req. on the dialog itself is not running at the same time)
				if (divDialogContainer instanceof MainPage)
				{
					((MainPage)divDialogContainer).touch();
				}

				// temporary set the dialog container as the current container (the close event is processed by the main container, not the dialog)
				fm.setCurrentContainer(divDialogContainer, divDialogContainer.getContainerName());

				if (client.getEventDispatcher() != null)
				{
					client.getEventDispatcher().addEvent(new WicketEvent(client, new Runnable()
					{
						public void run()
						{
							client.getRuntimeWindowManager().closeFormInWindow(divDialog.getPageMapName(), divDialog.getCloseAll());
						}
					}));
				}
				else
				{
					client.getRuntimeWindowManager().closeFormInWindow(divDialog.getPageMapName(), divDialog.getCloseAll());
				}

				// reset current container again
				fm.setCurrentContainer(currentContainer, currentContainer.getContainerName());
				if (divDialogContainer instanceof MainPage)
				{
					target.addComponent(divDialog);
				}
				WebEventExecutor.generateResponse(target, divDialog.getPage());

				return false;
			}
		});
		divDialogRepeater.add(divDialog);
		divDialogsParent.setVisible(true);
		addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_DIALOG_ADDED_OR_REMOVED, new Object[] { divDialogsParent }));
		divDialogs.put(name, divDialog);
		return divDialog;
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

	public void addWebAnchoringInfoIfNeeded()
	{
		if (getController() != null)
		{

			boolean webAnchorsEnabled = Utils.getAsBoolean(getController().getApplication().getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
			if (webAnchorsEnabled)
			{
				// test if there is a form in design
				Object isInDesign = visitChildren(IFormUIInternal.class, new IVisitor<Component>()
				{
					public Object component(Component component)
					{
						if (((IFormUIInternal< ? >)component).isDesignMode())
						{
							return Boolean.TRUE;
						}
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});
				if (isInDesign instanceof Boolean)
				{
					webAnchorsEnabled = !((Boolean)isInDesign).booleanValue();
				}
			}
			if (webAnchorsEnabled)
			{
				final SortedSet<FormAnchorInfo> formAnchorInfo = new TreeSet<FormAnchorInfo>();
				visitChildren(WebForm.class, new IVisitor<WebForm>()
				{
					public Object component(WebForm form)
					{
						if (form.isVisibleInHierarchy())
						{
							FormAnchorInfo fai = form.getFormAnchorInfo();
							if (fai != null)
							{
								if (form.isUIRecreated() && fai.isTableView)
								{
									pageContributor.removeFormAnchorInfo(fai);
								}

								if (form.equals(main))
								{
									fai.isTopLevelForm = true;
								}
								else
								{
									if (navigator != null)
									{
										if (form.equals(navigator.getFormUI()))
										{
											fai.isTopLevelNavigator = true;
										}
									}
								}
								formAnchorInfo.add(fai);
							}
							return IVisitor.CONTINUE_TRAVERSAL;
						}
						else
						{
							return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
					}
				});
				boolean isAjaxRequest = getRequestCycle().getRequestTarget() instanceof AjaxRequestTarget;
				if (!isAjaxRequest)
				{
					pageContributor.setFormAnchorInfos(null); // reset formAnchorInfo
				}
				pageContributor.setFormAnchorInfos(formAnchorInfo);
			}
			else
			{
				pageContributor.setFormAnchorInfos(null);
			}
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

	public IPageContributorInternal getPageContributor()
	{
		return pageContributor;
	}

	public MarkupContainer getBody()
	{
		return body;
	}

	public void addBodyAttributes(IValueMap map)
	{
		bodyAttributes.putAll(map);
	}

	/**
	 * @see wicket.Component#renderHead(wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@Override
	public void renderHead(HtmlHeaderContainer container)
	{
		super.renderHead(container);

		IHeaderResponse response = container.getHeaderResponse();


		String showUrl = getShowUrlScript();
		if (showUrl != null)
		{
			response.renderOnLoadJavascript(showUrl);
		}

		boolean allApplied = jsActionBuffer.apply(response);
		// some actions might not have been executed yet, because they might need an Ajax request;
		// for example a showDialog called in onLoad or onShow... so trigger an Ajax call as soon as possible in this case
		if (!allApplied) response.renderOnDomReadyJavascript("triggerAjaxUpdate();"); //$NON-NLS-1$

		response.renderJavascriptReference(servoy_js);
		YUILoader.renderYUI(response);
		JQueryLoader.render(response);
		TinyMCELoader.renderHTMLEdit(response);
	}

	/**
	 * @see wicket.Page#configureResponse()
	 */
	@SuppressWarnings("nls")
	@Override
	protected void configureResponse()
	{
		super.configureResponse();

		if (getWebRequestCycle().getResponse() instanceof WebResponse)
		{
			final WebResponse response = getWebRequestCycle().getWebResponse();
			HTTPUtils.setNoCacheHeaders(response.getHttpServletResponse(), "no-store");
		}

		final RequestCycle cycle = getRequestCycle();
		final Response response = cycle.getResponse();

		if (main != null)
		{
			final MarkupStream markupStream = main.getAssociatedMarkupStream(false);
			if (markupStream != null)
			{
//				markupStream.setCurrentIndex(0); // this doesn't seem to be needed
				MarkupElement m = markupStream.get();
				if (m != null)
				{
					String docType = m.toString().trim();
					if (docType.toUpperCase().startsWith("<!DOCTYPE"))
					{
						int index = docType.indexOf('>');
						if (index != -1)
						{
							response.write(docType.substring(0, index + 1));//delegate form doctype to be the mainpage doctype
						}
						else
						{
							response.write(docType);//delegate form doctype to be the mainpage doctype
						}
					}
				}
			}
		}
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
		if (RequestCycle.get() != null)
		{
			listview.removeAll();
		}
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

	/**
	 * @see wicket.Component#onAttach()
	 */
	@Override
	protected void onBeforeRender()
	{
		if (main != null && (main.getParent() == null || main.getParent().getParent() != listview))
		{
			// the main page is removed underneath from the page.
			// lets regenerate the listview.
			listview.removeAll();
		}
		super.onBeforeRender();
	}

	@Override
	protected void onAfterRender()
	{
		super.onAfterRender();

		mainFormSwitched = false;
		if (storeMinVersion)
		{
			minimumVersionNumber = getCurrentVersionNumber();
			storeMinVersion = false;
		}
		// make sure that all IProviderStylePropertyChanges are set to rendered on a full page render.
		visitChildren(IProviderStylePropertyChanges.class, new IVisitor<Component>()
		{
			@SuppressWarnings("nls")
			public Object component(Component component)
			{
				if (((IProviderStylePropertyChanges)component).getStylePropertyChanges().isChanged())
				{
					if (Debug.tracing())
					{
						if (component.isVisible())
						{
							Debug.trace("Component " + component + " is changed but is not rendered, deleted from template?");
						}
						else
						{
							Debug.trace("Component " + component + " is changed but is not rendered because it is not visible");
						}
					}
					((IProviderStylePropertyChanges)component).getStylePropertyChanges().setRendered();
				}
				return component.isVisible() ? IVisitor.CONTINUE_TRAVERSAL : IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});
	}


	/**
	 * @see wicket.markup.html.WebPage#onDetach()
	 */
	@Override
	protected void onDetach()
	{
		super.onDetach();

		// between requests a page is not versionable
		setVersioned(false);
	}

	public void add(final IComponent c, final String name)
	{
		// a new main form is added clear everything
		webForms.clear();
		FormController currentNavigator = navigator;
		WebMarkupContainer container = (WebMarkupContainer)c;
		if (!"webform".equals(container.getId())) //$NON-NLS-1$
		{
			throw new RuntimeException("only webforms with the name webform can be added to this mainpage"); //$NON-NLS-1$
		}

		if (main != null)
		{
			addStateChange(new Change()
			{
				private static final long serialVersionUID = 1L;

				final String formName = main.getController().getName();

				@Override
				public void undo()
				{
					((FormManager)client.getFormManager()).setCurrentContainer(MainPage.this, MainPage.this.getPageMap().getName());
					((FormManager)client.getFormManager()).showFormInMainPanel(formName, MainPage.this, null, true, MainPage.this.getPageMap().getName());
				}
			});
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

	public void setComponentVisible(boolean b)
	{
	}

	public void setBackground(Color cbg)
	{
	}

	public void setBorder(Border b)
	{
	}

	public void setCursor(Cursor predefinedCursor)
	{
	}

	public void setFont(Font f)
	{
	}

	public void setForeground(Color cfg)
	{
	}

	public void setLocation(Point loc)
	{
	}

	public void setName(String name)
	{
		// ignore, can only be set through constructor (as id)
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	public String getName()
	{
		return getId();
	}


	public void setOpaque(boolean b)
	{
	}

	public void setSize(Dimension size)
	{
	}

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
		touch();
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
		this.title.setDefaultModelObject(titleString);
		addJSAction(new RenderComponentAction(title));
	}

	public String nextInputNameId()
	{
		return Integer.toString(inputNameIds++);
	}

	public void setShowURLCMD(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		WebClientSession session = (WebClientSession)getSession();
		showUrlInfo = new ShowUrlInfo(url, target, target_options, timeout, onRootFrame,
			(url.equals(urlFor(serveResourceReference).toString()) && session != null && session.isServedResourceAttachment()) &&
				(target == null || target.equals("_self")));
	}

	/**
	 * @return
	 */
	public ShowUrlInfo getShowUrlInfo()
	{
		return showUrlInfo;
	}

	@SuppressWarnings("nls")
	public static String getShowUrlScript(ShowUrlInfo showUrlInfo)
	{
		if (showUrlInfo != null)
		{
			if (showUrlInfo.target.equalsIgnoreCase("_close"))
			{
				return "window.close();window.opener.location.reload(true)";
			}
			else if (showUrlInfo.target.equalsIgnoreCase("_self"))
			{
				String url = showUrlInfo.url;
				if (showUrlInfo.useIFrame)
				{
					url = HCUtils.replaceForwardedHost(RequestUtils.toAbsolutePath(url), ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest());
				}
				return "showurl('" + url + "'," + showUrlInfo.timeout + "," + showUrlInfo.onRootFrame + "," + showUrlInfo.useIFrame + "," +
					showUrlInfo.pageExpiredRedirect + ");";
			}
			else if (showUrlInfo.target.equalsIgnoreCase("_top"))
			{
				String script = "window.top.location.href='" + showUrlInfo.url + "';";
				return "window.setTimeout(\"" + script + "\"," + showUrlInfo.timeout + ");";
			}
			else
			{
				StringBuilder script = new StringBuilder();
				if (!"_blank".equals(showUrlInfo.target))
				{
					script.append("if (top.window.frames['" + showUrlInfo.target + "'])");
					script.append("{top.window.frames['" + showUrlInfo.target + "'].document.location.href = '" + showUrlInfo.url + "';}else{");
				}
				if (showUrlInfo.target_options != null)
				{
					script.append("window.open('" + showUrlInfo.url + "','" + showUrlInfo.target + "','" + showUrlInfo.target_options + "');");
				}
				else
				{
					script.append("window.open('" + showUrlInfo.url + "','" + showUrlInfo.target + "');");
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
				return getShowUrlScript(showUrlInfo);
			}
			finally
			{
				showUrlInfo = null;
			}
		}
		return null;
	}

	/**
	 * @param name
	 * @param bs
	 */
	public String serveResource(String fname, byte[] bs, String mimetype, String contentDisposition)
	{
		WebClientSession session = (WebClientSession)getSession();
		session.serveResource(fname, bs, mimetype, contentDisposition);
		return urlFor(serveResourceReference).toString();
	}

	@SuppressWarnings("nls")
	public void showOpenFileDialog(final IMediaUploadCallback callback, boolean multiSelect, String title)
	{
		if ((isShowingInDialog() || isClosingAsDivPopup()) && callingContainer != null)
		{
			callingContainer.showOpenFileDialog(callback, multiSelect, title);
		}
		else
		{
			touch();
			this.mediaUploadMultiSelect = multiSelect;
			this.mediaUploadCallback = new IMediaUploadCallback()
			{
				boolean uploaded = false;

				public void uploadComplete(IUploadData[] fu)
				{
					touch();
					uploaded = true;
					mediaUploadCallback = null;
					addJSAction(new DivDialogAction(fileUploadWindow, DivDialogAction.OP_CLOSE));
					callback.uploadComplete(fu);
				}

				public void onSubmit()
				{
					if (!uploaded)
					{
						mediaUploadCallback = null;
						divDialogs.remove(FILE_UPLOAD_PAGEMAP);
						fileUploadWindow.setPageMapName(null);
						fileUploadWindow.remove(fileUploadWindow.getContentId());
						addJSAction(new DivDialogAction(fileUploadWindow, DivDialogAction.OP_CLOSE));
					}
				}
			};

			fileUploadWindow.setPageMapName(FILE_UPLOAD_PAGEMAP);
			if (title == null)
			{
				fileUploadWindow.setTitle(client.getI18NMessage("servoy.filechooser.title"));
			}
			else if (!"".equals(title))
			{
				fileUploadWindow.setTitle(title);
			}
			divDialogs.put(FILE_UPLOAD_PAGEMAP, fileUploadWindow);
			addJSAction(new DivDialogAction(fileUploadWindow, DivDialogAction.OP_SHOW, new Object[] { FILE_UPLOAD_PAGEMAP }));
		}
	}

	public Color getBackground()
	{
		return null;
	}

	public Border getBorder()
	{
		return null;
	}

	public Font getFont()
	{
		return null;
	}

	public Color getForeground()
	{
		return null;
	}

	public Point getLocation()
	{
		return null;
	}

	public Dimension getSize()
	{
		return null;
	}

	public boolean isOpaque()
	{
		return false;
	}

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

	private final static ThreadLocal<Boolean> skipAttach = new ThreadLocal<Boolean>();

	/**
	 * @see org.apache.wicket.Component#onAttach()
	 */
	@Override
	public void onPageAttached()
	{
		if (skipAttach.get() != null) return;

		// between requests a page is not versionable
		setVersioned(true);
		String ignore = RequestCycle.get().getRequest().getParameter("ignoremp"); //$NON-NLS-1$
		if (!"true".equalsIgnoreCase(ignore)) //$NON-NLS-1$
		{
			// so it's not a timer/auto generated request - this means that the user interacted with the page - make it current container and bring it to foreground if needed
			FormManager fm = (FormManager)client.getFormManager();
			if (fm != null)
			{
				if (isShowingInDialog() && fm.getCurrentContainer() != MainPage.this && callingContainer != null)
				{
					ServoyDivDialog dialog = callingContainer.divDialogs.get(getPageMapName());
					// bring the dialog on top of other possible non-modals
					if (dialog != null && !dialog.isModal()) toFront();
				}
				fm.setCurrentContainer(MainPage.this, MainPage.this.getPageMap().getName());
			}
		}
		super.onPageAttached();
	}

	public void touch()
	{
		touch(false);
	}

	public boolean touch(boolean onlyIfNotInUse)
	{
		boolean touched = false;
		if (Session.exists() && RequestCycle.get() != null)
		{
			WebClientSession session = WebClientSession.get();
			// all the current locked pages for this request, that wants to lock this one.
			List<Page> touchedPages = session.getTouchedPages();
			touched = touchedPages.contains(this);
			if (!touched)
			{
				session.wantsToLock(touchedPages, this);

				skipAttach.set(Boolean.TRUE);
				if (onlyIfNotInUse) ((WebClientsApplication)getApplication()).getRequestCycleSettings().overrideTimeout(1);
				try
				{
					session.getPage(getPageMapName(), getPath(), Page.LATEST_VERSION);
					touched = true;
				}
				catch (WicketRuntimeException e)
				{
					// ignore if it is the timeout exception in case we only want to touch if not in use
					if (!onlyIfNotInUse || e.getCause() != null) throw new RuntimeException(
						"Touching page " + getPageMapName() + "/" + getPath() + " couldn't be done in thread: " + Thread.currentThread().getName(), e);
					Debug.trace("Touch page ignored.");
				}
				finally
				{
					skipAttach.remove();
					if (onlyIfNotInUse) ((WebClientsApplication)getApplication()).getRequestCycleSettings().restoreTimeout();
				}
			}
		}
		return touched;
	}

	@Override
	public void onNewBrowserWindow()
	{
		final IPageMap map = getSession().createAutoPageMap();
		FormManager fm = (FormManager)client.getFormManager();
		MainPage page = (MainPage)fm.getOrCreateMainContainer(map.getName());

		if (fm.getMainContainer(null) == this)
		{
			fm.setMainContainer(page);
		}
		fm.setCurrentContainer(page, map.getName());
		fm.showFormInCurrentContainer(getController().getName());
		if (page.getController() == null)
		{
			// form switch did not happen, login form?
			page.add(getController().getFormUI(), getController().getName());
		}
		page.setNavigator(getNavigator());
		setResponsePage(page);
	}

	@SuppressWarnings("nls")
	public void showPopupWindow(MainPage windowContainer, String titleString, Rectangle r2, boolean resizeable, boolean closeAll)
	{
		// all browser window main pages will be shown by main browser window and will have main browser window as callingContainer;
		// this is in order to avoid situations where some main pages need to reference each other in browser JS, but some window in the chain between them
		// has already been closed; so this way references to all non-modal browser windows will not be lost as long as main browser window remains open...
		// see also triggerBrowserRequestIfNeeded() that uses these references
		if (getPageMapName() != null && callingContainer != null) callingContainer.showPopupWindow(windowContainer, titleString, r2, resizeable, closeAll);
		else
		{
			touch();
			String windowVarName = MainPage.getValidJSVariableName(windowContainer.getPageMapName());
			StringBuilder sb = new StringBuilder(100);
			sb.append(windowVarName); // so that we can reference this window via current page JS
			sb.append("=window.open('");
			sb.append(RequestCycle.get().urlFor(windowContainer));
			sb.append("','");
			sb.append(windowContainer.getPageMap().getName());
			sb.append("','scrollbars=yes,menubar=no");
			if (FormManager.FULL_SCREEN.equals(r2))
			{
				sb.append(",fullscreen=yes"); // IE
				sb.append(",height='+(screen.height-30)+'"); // FF
				sb.append(",width='+(screen.width-5)+'"); // FF
				sb.append(",top=0,left=0");
			}
			else
			{
				sb.append(",height=").append(r2.height);
				sb.append(",width=").append(r2.width);
				sb.append(",top='+");
				sb.append("((window.screenTop | window.screenY)+");
				if (r2.y == -1) sb.append("((document.documentElement.clientHeight | document.body.clientHeight)-" + r2.height + ")/2");
				else sb.append(r2.y);
				sb.append(")");
				sb.append("+',left='+");
				sb.append("((window.screenLeft | window.screenX)+");
				if (r2.x == -1) sb.append("((document.documentElement.clientWidth | document.body.clientWidth)-" + r2.width + ")/2)");
				else sb.append(r2.x + ")");
				sb.append("+'");
			}
			sb.append(",resizable=").append(resizeable ? "yes" : "no");
			sb.append(",toolbar=no,location=no,status=no');");
			if (((WebClientInfo)getSession().getClientInfo()).getProperties().isBrowserSafari())
			{
				// safari doesn't tell you that a popup was blocked
				sb.append("if (");
				sb.append(windowVarName);
				sb.append(" == null || typeof(");
				sb.append(windowVarName);
				sb.append(") == \"undefined\") alert('Pop-up page could not be opened, please disable your pop-up blocker.');");
			}
			appendJavaScriptChanges(sb.toString());

			mustFocusNullIfNoComponentToFocus();

			windowContainer.callingContainer = this;
			windowContainer.jsActionBuffer.clear();
			windowContainer.showingInWindow = true;
			windowContainer.showingInDialog = false;
		}
	}

	public void showPopupDiv(MainPage dialogContainer, String titleString, Rectangle r2, boolean resizeable, boolean closeAll, boolean modal,
		boolean undecorated, boolean storeBounds, float opacity, boolean transparent)
	{
		// all iframe div window main pages will be shown by a browser window main page and will have it as callingContainer;
		// this is in order to avoid situations where some main pages need to reference each other in browser JS, but some div windows in the chain between them
		// have already been closed; so this way references to all iframe div windows will not be lost as long as the browser window that contains the iframes remains open
		// see also triggerBrowserRequestIfNeeded() that uses these references
		if ((isShowingInDialog() || isClosingAsDivPopup()) && callingContainer != null)
			callingContainer.showPopupDiv(dialogContainer, titleString, r2, resizeable, closeAll, modal, undecorated, storeBounds, opacity, transparent);
		else
		{
			if (useAJAX)
			{
				touch();
				String windowName = dialogContainer.getPageMap().getName();
				ServoyDivDialog divDialog = divDialogs.get(windowName);
				if (divDialog == null)
				{
					divDialog = createDivDialog(dialogContainer, windowName);
				}
				divDialog.setPageMapName(windowName);
				divDialog.setResizable(resizeable);
				divDialog.setStoreBounds(storeBounds);
				divDialog.setUseInitialHeight(true);
				divDialog.setModal(modal);
				divDialog.setOpacity(opacity);
				divDialog.setTransparent(transparent);
				if (undecorated) divDialog.setCssClassName("w_undecorated");
				Rectangle bounds = r2;
				if (FormManager.FULL_SCREEN.equals(r2))
				{
					// get the size of the browser window (that will contain the div window)
					bounds = new Rectangle(0, 0, getWidth(), getHeight() - 45); // it is a bit too high, why? Because windowBounds is size of what the div should occupy, while modalWindow.setInitialHeight() is only applied to the contents (without frame)
				}
				divDialog.setInitialHeight(bounds.height);
				divDialog.setInitialWidth(bounds.width);
				divDialog.setInitialLocation(new Point(bounds.x, bounds.y));
				divDialog.setCloseAll(closeAll);

				FormController fp = dialogContainer.getController();
				String titleStr = titleString;
				if (titleStr == null) titleStr = fp.getForm().getTitleText();
				if (titleStr == null) titleStr = fp.getName();
				titleStr = client.getI18NMessageIfPrefixed(titleStr);

				if (titleStr != null)
				{
					String name2 = Text.processTags(titleStr, fp.getTagResolver());
					if (name2 != null) titleStr = name2;
				}

				divDialog.setTitle(titleStr);
				addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_SHOW, new Object[] { windowName }));
			}

			dialogContainer.setWindowSize(null); // not used for dialogs
			dialogContainer.callingContainer = this;
		}
	}

	private void closeChildWindow(String popupName)
	{
		// first touch this page so that it is locked if this is a normal request
		touch();

		// set new current container
		FormManager formManager = ((FormManager)client.getFormManager());
		String mpmn = getModalPageMapName();
		if (mpmn != null)
		{
			// there is a modal div window opened; it is the one that should become the new current container
			formManager.setCurrentContainer(formManager.getMainContainer(mpmn), mpmn);
			// also refresh it
			((MainPage)formManager.getMainContainer(mpmn)).triggerBrowserRequestIfNeeded();
		}
		else
		{
			formManager.setCurrentContainer(this, getPageMap().getName());
		}
	}

	private String getModalPageMapName()
	{
		FormManager formManager = ((FormManager)client.getFormManager());
		// take the top-most modal dialog
		List<ServoyDivDialog> oos = divDialogs.getOrderedByOpenSequence();
		for (int hi = oos.size() - 1; hi >= 0; hi--)
		{
			ServoyDivDialog dw = oos.get(hi);
			if (dw.isModal() && dw.isShown() && dw != fileUploadWindow)
			{
				if (formManager.getMainContainer(dw.getPageMapName()) != null)
				{
					return dw.getPageMapName();
				}
				else
				{
					Debug.error("Cannot find page of an opened modal dialog: " + dw.getPageMapName());
				}
			}
		}
		return null;
	}

	public void setDialogBounds(String windowName, int x, int y, int width, int height)
	{
		touch();
		if (callingContainer != null)
		{
			callingContainer.touch();
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(windowName);
			if (divDialog != null)
			{
				callingContainer.addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_SET_BOUNDS, new Object[] { x, y, width, height }));
			}
		}
	}

	public void close()
	{
		// first touch this page so that it is locked if this is a normal request
		touch();

		setWindowSize(null);

		if (callingContainer != null)
		{
			if (isShowingInWindow())
			{
				// this is a non-modal browser window; close it through JS; using setTimeout to allow any pending triggerAjaxUpdate that initiated in this page to do it's job
				appendJavaScriptChanges("if (!self.closed) window.setTimeout('self.close();', 1);"); //$NON-NLS-1$
				closingAsWindow = true;
			}

			ServoyDivDialog divDialog = callingContainer.divDialogs.remove(getPageMapName());
			if (divDialog != null)
			{
				callingContainer.addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_SAVE_BOUNDS));
				callingContainer.closingAChildDivPopoup = true;
				closingAsDivPopup = true;
				callingContainer.addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_CLOSE, new Object[] { this })
				{
					@Override
					protected void onAfterApply()
					{
						MainPage.this.closingAsDivPopup = false;
					}
				});
			}

			callingContainer.closeChildWindow(getPageMapName());
		}
		showingInWindow = false;
		showingInDialog = false;
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
		if (versionNeedsPushing) ignoreVersionMerge();
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

	public void renderJavascriptChanges(final AjaxRequestTarget target)
	{
		closingAChildDivPopoup = false;
		closingAsWindow = false;

		if (callingContainer != null && (isShowingInDialog() || isClosingAsDivPopup()))
		{
			// to avoid concurrent modification exceptions on the action buffer, we try to touch the page; if the page is already in
			// use by another request, just apply only current page actions, to not block this req. until the callingContainer is released (it will execute it's actions on it's own request)
			if (callingContainer.touch(true))
			{
				// in this case execute both this page's actions and the other div window actions from parent (root) main page (only if they will all work both from parent and child)
				if (isClosingAsDivPopup()) callingContainer.closingAChildDivPopoup = false; // all div operations will get executed on this target anyway
				jsActionBuffer.apply(target, callingContainer.jsActionBuffer);
			}
			else
			{
				jsActionBuffer.apply(target);
			}
		}
		else
		{
			jsActionBuffer.apply(target);
		}
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
	public String getToolTipText()
	{
		return null;
	}

	public String getAdminInfo()
	{
		return client.getAdminInfo();
	}

	public String getStatusText()
	{
		return statusText;
	}

	public void setStatusText(String statusText)
	{
		this.statusText = statusText;
		if (setStatusBehavior == null)
		{
			setStatusBehavior = new SetStatusBehavior();
			add(setStatusBehavior);
		}
		setStatusBehavior.setStatusText(statusText);
	}

	public void requestFocus()
	{
	}

	public boolean isApplicationShutDown()
	{
		return client.getPluginManager() == null;
	}

	/**
	 * Respond to focus/blur events.
	 */
	@SuppressWarnings("nls")
	public void respond(AjaxRequestTarget target, final String event, final String markupId)
	{
		Component component = (Component)visitChildren(IComponent.class, new IVisitor<Component>()
		{
			public Object component(Component c)
			{
				Component targetComponent = c;
				if (c instanceof WebBaseSelectBox && ("blur".equals(event) || "focus".equals(event)))
				{
					Component[] cs = ((WebBaseSelectBox)c).getFocusChildren();
					if (cs != null && cs.length == 1) targetComponent = cs[0];
				}

				if (targetComponent.getMarkupId().equals(markupId))
				{
					return c;
				}
				return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		if (component == null)
		{
			Debug.log("Component not found markupId " + markupId); //$NON-NLS-1$
			return;
		}

		IFormUIInternal< ? > formui = component.findParent(IFormUIInternal.class);
		if (formui != null && formui.isDesignMode())
		{
			Debug.log("Event ignored because of design mode"); //$NON-NLS-1$
			return;
		}

		if (component instanceof IFieldComponent)
		{
			WebEventExecutor eventExecutor = (WebEventExecutor)((IFieldComponent)component).getEventExecutor();
			if (eventExecutor != null)
			{
				if ("focus".equals(event))
				{
					int webModifier = Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER));
					StartEditOnFocusGainedEventBehavior.startEditing(component, WebEventExecutor.convertModifiers(webModifier), target);
					eventExecutor.onEvent(JSEvent.EventType.focusGained, target, component, webModifier);
				}
				else if ("blur".equals(event))
				{
					// test if the data is really posted by looking up the key.
					if (component instanceof FormComponent< ? > && RequestCycle.get().getRequest().getParameter("nopostdata") == null)
					{
						// changed data is posted
						((FormComponent< ? >)component).processInput();
					}
					eventExecutor.onEvent(JSEvent.EventType.focusLost, target, component, IEventExecutor.MODIFIERS_UNSPECIFIED);
				}
				else
				{
					Debug.trace("Ignored event " + event);
				}
			}
			else
			{
				Debug.trace("Ignored event, no eventExecutor");
			}
		}
		else
		{
			// other non-field components like WebLabel
			ServoyForm form = component.findParent(ServoyForm.class);
			if (form != null)
			{
				Page page = form.getPage(); // JS might change the page this form belongs to... so remember it now

				int webModifier = Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER));
				WebEventExecutor.setSelectedIndex(component, target, WebEventExecutor.convertModifiers(webModifier), true);
				WebEventExecutor.generateResponse(target, page);
			}
		}
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

			touch();
			List<ServoyDivDialog> oos = divDialogs.getOrderedByOpenSequence();
			for (int i = oos.size() - 1; i >= 0; i--)
			{
				String dName = oos.get(i).getPageMapName();
				if (!al.contains(dName))
				{
					al.add(dName);
					visited.add(name);
				}
			}
		}
	}

	public void toBack()
	{
		touch();
		if (isShowingInDialog() && callingContainer != null)
		{
			callingContainer.touch();
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			if (divDialog != null)
			{
				callingContainer.addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_TO_BACK));
			}
		}
		else
		{
			appendJavaScriptChanges("window.blur();");
		}
	}

	public void toFront()
	{
		touch();
		if (isShowingInDialog() && callingContainer != null)
		{
			callingContainer.touch();
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			if (divDialog != null)
			{
				callingContainer.addJSAction(new DivDialogAction(divDialog, DivDialogAction.OP_TO_FRONT));
			}
		}
		else
		{
			appendJavaScriptChanges("window.focus();");
		}
	}

	public void resetBounds(String windowName)
	{
		touch();
		MainPage mp = this;
		if (callingContainer != null)
		{
			callingContainer.touch();
			mp = callingContainer;
		}
		mp.addJSAction(new DivDialogAction(null, DivDialogAction.OP_RESET_BOUNDS, new Object[] { COOKIE_PREFIX + windowName }));
	}

	public void appendJavaScriptChanges(String script)
	{
		touch();
		addJSAction(new JSChangeAction(script));
	}

	/**
	 * If current request is not on this MainPage, then generate a JS that will trigger an ajax request on this page.
	 */
	@SuppressWarnings("nls")
	public void triggerBrowserRequestIfNeeded()
	{
		if (!useAJAX) return;
		MainPage requestMP = MainPage.getRequestMainPage();
		if (requestMP != null && requestMP.jsActionBuffer != null && !requestMP.jsActionBuffer.hasAjaxUpdateTrigger(this))
		{
			Pair<String, MainPage> goToCorrectWindow = getWindowScopeBrowserScript(requestMP);
			// generate a JS script that when ran inside browser for requestMP it will trigger an ajax request on this main page;
			if (goToCorrectWindow != null && goToCorrectWindow.getRight() != this)
			{
				String triggerScript = "try { " + goToCorrectWindow.getLeft() + "triggerAjaxUpdate(); } catch(ignore) {}";
				goToCorrectWindow.getRight().jsActionBuffer.triggerAjaxUpdate(this, triggerScript);
			}
		}
	}

	@SuppressWarnings("nls")
	public String getTriggerBrowserRequestJS()
	{
		String script = null;
		if (useAJAX)
		{
			Pair<String, MainPage> goToCorrectWindow = getWindowScopeBrowserScript(MainPage.getRequestMainPage());
			// generate a JS script that will disable AJAX timer requests on this page
			if (goToCorrectWindow != null && goToCorrectWindow.getRight() != this)
			{
				script = "try { " + goToCorrectWindow.getLeft() + "setTimeout('triggerAjaxUpdate();', 0); } catch(ignore) {}";
			}
		}
		return script;
	}

	public static MainPage getRequestMainPage()
	{
		RequestCycle rc = RequestCycle.get();
		if (rc == null) return null; // can't find the page that generated this request
		Page tmp = rc.getResponsePage();
		if (!(tmp instanceof MainPage)) return null; // can't find the page that generated this request

		return (MainPage)tmp;
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
					if (mp.isShowingInDialog() || mp.isClosingAsDivPopup())
					{
						ServoyDivDialog dw = mp.callingContainer.divDialogs.get(mp.getPageMapName());
						if (dw != null) goToCorrectScopeScript += "Wicket.DivWindow.openWindows['" + dw.getJSId() + "'].content.contentWindow.";
						else ok = false;
					}
					else if (mp.isShowingInWindow() || mp.closingAsWindow)
					{
						goToCorrectScopeScript += MainPage.getValidJSVariableName(mp.getPageMapName()) + ".";
					}
					else ok = false; // some windows in the window chain are closed...
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
		if (isShowingInDialog() && callingContainer != null)
		{
			// it's showing in a div dialog
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			return divDialog != null ? divDialog.getX() : 0;
		}
		else return 0; // closed windows & non-modal browser windows are currently not aware of location
	}

	public int getY()
	{
		if (isShowingInDialog() && callingContainer != null)
		{
			// it's showing in a div dialog
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			return divDialog != null ? divDialog.getY() : 0;
		}
		else return 0; // closed windows & non-modal browser windows are currently not aware of location
	}

	public int getWidth()
	{
		if (isShowingInDialog() && callingContainer != null)
		{
			// it's showing in a div dialog
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			if (divDialog != null) return divDialog.getWidth();
		}
		if (isShowingInWindow() && size != null) return size.width;

		// keep backwards compatibility (if size cannot be found use main window width as stored in session properties)
		return ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties().getBrowserWidth();
	}

	public int getHeight()
	{
		if (isShowingInDialog() && callingContainer != null)
		{
			// it's showing in a div dialog
			ServoyDivDialog divDialog = callingContainer.divDialogs.get(getContainerName());
			if (divDialog != null) return divDialog.getHeight();
		}
		if (isShowingInWindow() && size != null) return size.height;

		// main page or closed page; keep backwards compatibility (if size cannot be found use main window width as stored in session properties)
		return ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties().getBrowserHeight();
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
		if (getPageMapName() != null)
		{
			size = d;
		}
		else if (d != null)
		{
			// main page
			ClientProperties properties = ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties();
			properties.setBrowserWidth(d.width);
			properties.setBrowserHeight(d.height);
		}
	}


	private class TriggerResizeAjaxBehavior extends AbstractServoyDefaultAjaxBehavior
	{
		@Override
		protected void respond(AjaxRequestTarget target)
		{
			MainPage page = (MainPage)findPage();
			if (page != null)
			{
				Map<String, String[]> params = getComponent().getRequest().getParameterMap();
				Iterator<String> it = params.keySet().iterator();
				while (it.hasNext())
				{
					final String key = it.next();
					if (key.equals("sfw_window")) //$NON-NLS-1$
					{
						try
						{
							final String width = params.get(key)[0];
							final String height = params.get("sfh_window")[0]; //$NON-NLS-1$
							page.setWindowSize(new Dimension(Utils.getAsInteger(width), Utils.getAsInteger(height)));
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
					else if (key.startsWith("sfw_form_")) //$NON-NLS-1$
					{
						try
						{
							final String width = params.get(key)[0];
							final String height = params.get("sfh_form_" + key.substring("sfw_form_".length()))[0]; //$NON-NLS-1$//$NON-NLS-2$
							final String containerMarkupId = key.substring("sfh_".length());
							page.visitChildren(WebForm.class, new Component.IVisitor<WebForm>()
							{
								public Object component(WebForm form)
								{
									if (containerMarkupId.equals(form.getContainerMarkupId()))
									{
										form.setFormWidth(Utils.getAsInteger(width));
										form.storeFormHeight(Utils.getAsInteger(height));
										return IVisitor.STOP_TRAVERSAL;
									}
									return IVisitor.CONTINUE_TRAVERSAL;
								}
							});
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
				}
				if (page.getController() != null)
				{
					WebForm webForm = (WebForm)page.getController().getFormUI();
					if (webForm.isFormWidthHeightChanged())
					{
						page.getController().notifyResized();
						webForm.clearFormWidthHeightChangedFlag();
					}
				}
				page.visitChildren(IWebFormContainer.class, new Component.IVisitor<Component>()
				{
					public Object component(Component component)
					{
						((IWebFormContainer)component).notifyResized();
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});
				getPageContributor().setResizing(true);
				WebEventExecutor.generateResponse(target, page);
				getPageContributor().setResizing(false);
			}
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			String jsCall = "Servoy.Resize.callback='" + getCallbackUrl() + "';"; //$NON-NLS-1$ //$NON-NLS-2$
			jsCall += "window.onresize = function() {"; //$NON-NLS-1$
			Page page = findPage();
			if (page instanceof MainPage && ((MainPage)page).getController() != null)
			{
				boolean webAnchorsEnabled = Utils.getAsBoolean(((MainPage)page).getController().getApplication().getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
				if (webAnchorsEnabled)
				{
					// test if there is a form in design
					Object isInDesign = visitChildren(IFormUIInternal.class, new IVisitor<Component>()
					{
						public Object component(Component component)
						{
							if (((IFormUIInternal< ? >)component).isDesignMode())
							{
								return Boolean.TRUE;
							}
							return IVisitor.CONTINUE_TRAVERSAL;
						}
					});
					if (isInDesign instanceof Boolean)
					{
						webAnchorsEnabled = !((Boolean)isInDesign).booleanValue();
					}
				}

				if (webAnchorsEnabled)
				{
					jsCall += "layoutEntirePage();"; //$NON-NLS-1$
				}
			}
			jsCall += "Servoy.Resize.onWindowResize();};"; //$NON-NLS-1$
			response.renderOnLoadJavascript(jsCall);
		}
	}

	private class TriggerOrientationChangeAjaxBehavior extends AbstractServoyDefaultAjaxBehavior
	{
		@Override
		protected void respond(AjaxRequestTarget target)
		{
			MainPage page = (MainPage)findPage();
			if (page != null)
			{
				Map<String, String[]> params = getComponent().getRequest().getParameterMap();
				Iterator<String> it = params.keySet().iterator();
				while (it.hasNext())
				{
					final String key = it.next();
					if (key.equals("orientation")) //$NON-NLS-1$
					{
						setOrientation(params.get(key)[0]);
						break;
					}
				}
				WebEventExecutor.generateResponse(target, page);
			}
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			String jsCall = "if ('onorientationchange' in window){"; //$NON-NLS-1$
			jsCall += "Servoy.Resize.orientationCallback='" + getCallbackUrl() + "';"; //$NON-NLS-1$ //$NON-NLS-2$
			jsCall += "window.onorientationchange = function() {"; //$NON-NLS-1$
			jsCall += "Servoy.Resize.onOrientationChange ();};};"; //$NON-NLS-1$
			response.renderOnLoadJavascript(jsCall);
		}
	}

	private static String getValidJSVariableName(String name)
	{
		return "v_" + name.replace('-', '_').replace(' ', '_').replace(':', '_'); //$NON-NLS-1$
	}

	protected void addJSAction(PageAction a)
	{
		jsActionBuffer.addAction(a);
		triggerBrowserRequestIfNeeded(); // this will probably do nothing cause a MediaUploadPage is the source of the req.
	}

	/**
	 * returns the top most {@link PageJSActionBuffer} for this main page. So it will return the calling parent forms action buffer
	 * if this one is opened  in a dialog.
	 */
	public PageJSActionBuffer getPageActionBuffer()
	{
		if (callingContainer != null)
		{
			return callingContainer.getPageActionBuffer();
		}
		return jsActionBuffer;
	}

}
