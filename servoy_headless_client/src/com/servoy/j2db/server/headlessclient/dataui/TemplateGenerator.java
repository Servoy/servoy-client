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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.CSS;
import javax.swing.text.html.CSS.Attribute;

import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ResourceReference;
import org.xhtmlrenderer.css.constants.CSSName;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Shape;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.ui.ISupportRowBGColorScript;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyStyleSheet;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * HTML generator used by the web framework
 *
 * @author jblok
 */
@SuppressWarnings("nls")
public class TemplateGenerator
{
	/**
	 * @author jcompagner
	 *
	 */
	private static class FormCache
	{
		private final ReferenceQueue<Pair<String, ArrayList<Pair<String, String>>>> queue = new ReferenceQueue<Pair<String, ArrayList<Pair<String, String>>>>();

		private static class CacheItem extends SoftReference<Pair<String, ArrayList<Pair<String, String>>>>
		{
			long lastmodified;
			private final String key;

			/**
			 * @param key
			 * @param referent
			 */
			public CacheItem(String key, long lastModified, Pair<String, ArrayList<Pair<String, String>>> referent,
				ReferenceQueue<Pair<String, ArrayList<Pair<String, String>>>> queue)
			{
				super(referent, queue);
				this.key = key;
				this.lastmodified = lastModified;
			}
		}

		private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<String, CacheItem>();

		/**
		 * @param sp
		 * @param f
		 * @param overriddenStyleName
		 * @return
		 */
		public Pair<String, ArrayList<Pair<String, String>>> getFormAndCssPair(Form f, String instanceFormName, String overriddenStyleName,
			IRepository repository)
		{
			Pair<String, ArrayList<Pair<String, String>>> retval = null;

			long t = getLastModifiedTime(f.getSolution(), f, overriddenStyleName, repository);

			String cacheKey = System.identityHashCode(f) + ":" + instanceFormName + ":" + overriddenStyleName;
			CacheItem cacheItem = cache.get(cacheKey);


			if (cacheItem != null && cacheItem.lastmodified == t)
			{
				retval = cacheItem.get();
				if (retval == null)
				{
					cache.remove(cacheKey);
				}
			}
			else
			{
				cache.remove(cacheKey);
			}
			return retval;
		}


		/**
		 * @param sp
		 * @param f
		 * @param overriddenStyleName
		 * @param repository
		 * @param retval
		 */
		public void putFormAndCssPair(Form f, String instanceFormName, String overriddenStyleName, IRepository repository,
			Pair<String, ArrayList<Pair<String, String>>> formAndCss)
		{
			long t = getLastModifiedTime(f.getSolution(), f, overriddenStyleName, repository);
			String key = System.identityHashCode(f) + ":" + instanceFormName + ":" + overriddenStyleName;
			cache.put(key, new CacheItem(key, t, formAndCss, queue));

			// clean the cache.
			CacheItem ref = (CacheItem)queue.poll();
			while (ref != null)
			{
				cache.remove(ref.key);
				ref = (CacheItem)queue.poll();
			}
		}

		/**
		 * @param solution
		 * @param f
		 * @param overridenStyleName
		 * @param repository
		 * @return
		 * @throws RemoteException
		 * @throws RepositoryException
		 */
		private static long getLastModifiedTime(Solution solution, Form f, String overridenStyleName, final IRepository repository)
		{
			long t = solution.getLastModifiedTime();
			if (f.getLastModified() > t)
			{
				t = f.getLastModified();
			}
			String styleName = overridenStyleName;
			if (styleName == null && f != null) styleName = f.getStyleName();
			if (styleName != null)
			{
				Style style = null;
				try
				{
					style = (Style)repository.getActiveRootObject(styleName, IRepository.STYLES);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
				if (style != null && style.getLastModifiedTime() > t)
				{
					t = style.getLastModifiedTime();
				}
			}
			return t;
		}
	}

	public static final int DEFAULT_FONT_SIZE = 11;
	public static final Insets DEFAULT_LABEL_PADDING = new Insets(0, 0, 0, 0);
	public static final Insets DEFAULT_BUTTON_PADDING = new Insets(0, 0, 0, 0);
	public static final Insets DEFAULT_FIELD_PADDING = new Insets(1, 1, 1, 1);
	public static final Insets DEFAULT_BUTTON_BORDER_SIZE = new Insets(1, 1, 1, 1);
	public static final Insets DEFAULT_FIELD_BORDER_SIZE = new Insets(2, 2, 2, 2);

	public static final int SORTABLE_HEADER_PADDING = 2;
	public static final int NO_LABELFOR_DEFAULT_BORDER_WIDTH = 1;

	public static final Color DEFAULT_FORM_BG_COLOR = Color.WHITE;
	public static final String TABLE_VIEW_CELL_CLASS = "tableviewcell"; // this value is also used in servoy.js; if you change/remove it please update servoy.js
	public static final String COMBOBOX_CLASS = "select_wrapper";
	public static final String WRAPPER_SUFFIX = "_wrapper";

	private static final FormCache formCache = new FormCache();

	private static final String[] DEFAULT_FONT_ELEMENTS = { "input", "button", "select", "td", "th", "textarea" };

	private static HashMap<String, String> getWebFormIDToMarkupIDMap(WebForm wf, final ArrayList<String> ids)
	{
		final HashMap<String, String> webFormIDToMarkupIDMap = new HashMap<String, String>();
		wf.visitChildren(new IVisitor<org.apache.wicket.Component>()
		{
			public Object component(org.apache.wicket.Component c)
			{
				// ignore forms from container components
				if (c instanceof WebForm) return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;

				String id = "#" + c.getId();
				//components to selectors
				if (ids.indexOf(id) != -1 && !webFormIDToMarkupIDMap.containsKey(id))
				{
					webFormIDToMarkupIDMap.put(id, "#" + c.getMarkupId());
					for (String dfe : DEFAULT_FONT_ELEMENTS)
					{
						String dfeSelector = id + " " + dfe;
						if (ids.indexOf(dfeSelector) != -1) webFormIDToMarkupIDMap.put(dfeSelector, "#" + c.getMarkupId() + " " + dfe);
					}
				}
				//selectors to components
				for (String sid : ids)
				{
					if (sid.startsWith(id) && !webFormIDToMarkupIDMap.containsKey(sid) && webFormIDToMarkupIDMap.containsKey(id))
					{
						webFormIDToMarkupIDMap.put(sid, webFormIDToMarkupIDMap.get(id) + sid.substring(id.length()));

					}
				}
				return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		return webFormIDToMarkupIDMap;
	}

	private static String getWebFormCSS(ArrayList<Pair<String, String>> formCSS, Map<String, String> IDToMarkupIDMap)
	{
		StringBuffer webFormCSS = new StringBuffer();

		String selector;
		for (Pair<String, String> cssItem : formCSS)
		{
			selector = (IDToMarkupIDMap != null && IDToMarkupIDMap.containsKey(cssItem.getLeft())) ? IDToMarkupIDMap.get(cssItem.getLeft()) : cssItem.getLeft();
			webFormCSS.append(selector).append(cssItem.getRight());
		}

		return webFormCSS.toString();
	}


	public static Pair<String, String> getFormHTMLAndCSS(int solution_id, int form_id) throws RepositoryException, RemoteException
	{
		final IRepository repository = ApplicationServerRegistry.get().getLocalRepository();

		Solution solution = (Solution)repository.getActiveRootObject(solution_id);
		Form form = solution.getForm(form_id);
		IServiceProvider sp = null;
		if (WebClientSession.get() != null)
		{
			sp = WebClientSession.get().getWebClient();
		}
		return getFormHTMLAndCSS(solution, form, sp, form.getName());
	}

	public static Pair<String, String> getFormHTMLAndCSS(Solution solution, Form form, IServiceProvider sp, String formInstanceName)
		throws RepositoryException, RemoteException
	{
		if (form == null) return null;
		final IRepository repository = ApplicationServerRegistry.get().getLocalRepository();
		boolean enableAnchoring = sp != null ? Utils.getAsBoolean(sp.getRuntimeProperties().get("enableAnchors"))
			: Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.webclient.enableAnchors", Boolean.TRUE.toString()));

		String overriddenStyleName = null;

		Pair<String, ArrayList<Pair<String, String>>> retval = formCache.getFormAndCssPair(form, formInstanceName, overriddenStyleName, repository);

		Form f = form;
		FlattenedSolution fsToClose = null;
		try
		{
			if (retval == null)
			{
				if (f.getExtendsID() > 0)
				{
					FlattenedSolution fs = sp == null ? null : sp.getFlattenedSolution();
					if (fs == null)
					{
						try
						{
							IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
							fsToClose = fs = new FlattenedSolution(solution.getSolutionMetaData(), new AbstractActiveSolutionHandler(as)
							{
								@Override
								public IRepository getRepository()
								{
									return repository;
								}
							});
						}
						catch (RepositoryException e)
						{
							Debug.error("Couldn't create flattened form for the template generator", e);
						}
					}
					f = fs.getFlattenedForm(f);

					if (f == null)
					{
						Debug.log("TemplateGenerator couldn't get a FlattenedForm for " + form + ", solution closed?");
						f = form;
					}
				}

				StringBuffer html = new StringBuffer();
				TextualCSS css = new TextualCSS();

				IFormLayoutProvider layoutProvider = FormLayoutProviderFactory.getFormLayoutProvider(sp, solution, f, formInstanceName);

				int viewType = layoutProvider.getViewType();

				layoutProvider.renderOpenFormHTML(html, css);

				int startY = 0;
				Iterator<Part> parts = f.getParts();
				while (parts.hasNext())
				{
					Part part = parts.next();
					int endY = part.getHeight();

					if (Part.rendersOnlyInPrint(part.getPartType()))
					{
						startY = part.getHeight();
						continue;//is never shown (=printing only)
					}

					Color bgColor = ComponentFactory.getPartBackground(sp, part, f);

					if (part.getPartType() == Part.BODY && (viewType == FormController.TABLE_VIEW || viewType == FormController.LOCKED_TABLE_VIEW ||
						viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW))
					{
						layoutProvider.renderOpenTableViewHTML(html, css, part);
						createCellBasedView(f, f, html, css, layoutProvider.needsHeaders(), startY, endY, bgColor, sp, viewType, enableAnchoring);//tableview == bodypart
						layoutProvider.renderCloseTableViewHTML(html);
					}
					else
					{
						layoutProvider.renderOpenPartHTML(html, css, part);
						placePartElements(f, startY, endY, html, css, bgColor, enableAnchoring, sp);
						layoutProvider.renderClosePartHTML(html, part);
					}
					startY = part.getHeight();
				}

				layoutProvider.renderCloseFormHTML(html);

				retval = new Pair<String, ArrayList<Pair<String, String>>>(html.toString(), css.getAsSelectorValuePairs());
				formCache.putFormAndCssPair(form, formInstanceName, overriddenStyleName, repository, retval);
			}

			Map<String, String> formIDToMarkupIDMap = null;
			if (sp instanceof IApplication)
			{
				Map runtimeProps = sp.getRuntimeProperties();
				Map<WebForm, Map<String, String>> clientFormsIDToMarkupIDMap = (Map<WebForm, Map<String, String>>)runtimeProps.get("WebFormIDToMarkupIDCache");
				if (clientFormsIDToMarkupIDMap == null)
				{
					clientFormsIDToMarkupIDMap = new WeakHashMap<WebForm, Map<String, String>>();
					runtimeProps.put("WebFormIDToMarkupIDCache", clientFormsIDToMarkupIDMap);
				}

				IForm wfc = ((IApplication)sp).getFormManager().getForm(formInstanceName);
				if (wfc instanceof FormController)
				{
					IFormUIInternal wf = ((FormController)wfc).getFormUI();
					if (wf instanceof WebForm)
					{
						if (!((WebForm)wf).isUIRecreated()) formIDToMarkupIDMap = clientFormsIDToMarkupIDMap.get(wf);
						if (formIDToMarkupIDMap == null)
						{
							ArrayList<Pair<String, String>> formCSS = retval.getRight();
							ArrayList<String> selectors = new ArrayList<String>(formCSS.size());
							for (Pair<String, String> formCSSEntry : formCSS)
								selectors.add(formCSSEntry.getLeft());
							formIDToMarkupIDMap = getWebFormIDToMarkupIDMap((WebForm)wf, selectors);
							clientFormsIDToMarkupIDMap.put((WebForm)wf, formIDToMarkupIDMap);
						}
					}
				}
			}

			String webFormCSS = getWebFormCSS(retval.getRight(), formIDToMarkupIDMap);
			webFormCSS = StripHTMLTagsConverter.convertMediaReferences(webFormCSS, solution.getName(), new ResourceReference("media"), "", false).toString(); // string the formcss/solutionname/ out of the url.
			return new Pair<String, String>(retval.getLeft(), webFormCSS);
		}
		finally
		{
			if (fsToClose != null)
			{
				fsToClose.close(null);
			}
		}
	}

	private static void placePartElements(Form f, int startY, int endY, StringBuffer html, TextualCSS css, Color formPartBgColor, boolean enableAnchoring,
		IServiceProvider sp) throws RepositoryException
	{
		Iterator<IFormElement> it = f.getFormElementsSortedByFormIndex();
		while (it.hasNext())
		{
			Point l = null;
			IFormElement element = it.next();
			l = element.getLocation();

			if (l == null) continue;//unknown where to add

			if (l.y >= startY && l.y < endY)
			{
				try
				{
					css.addCSSBoundsHandler(new YOffsetCSSBoundsHandler(-startY));
					createComponentHTML(element, f, html, css, formPartBgColor, startY, endY, enableAnchoring, sp);
					html.append('\n');
				}
				finally
				{
					css.removeCSSBoundsHandler();
				}
			}
		}
	}

	private static void createCellBasedView(AbstractBase obj, Form form, StringBuffer html, TextualCSS css, boolean addHeaders, int startY, int endY,
		Color bgColor, IServiceProvider sp, int viewType, boolean enableAnchoring) throws RepositoryException
	{
		try
		{
			html.append('\n');
			boolean sortable = true;
			boolean shouldFillAllHorizSpace = false;
			boolean userCssClassAdded[] = new boolean[] { false };
			if (obj instanceof ISupportScrollbars)
			{
				int scrollbars = ((ISupportScrollbars)obj).getScrollbars();
				if ((scrollbars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
				{
					boolean hasAtLeastOneAnchored = false;
					Iterator<IPersist> it2 = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
					while (it2.hasNext())
					{
						IPersist element = it2.next();
						if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
						{
							if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
							{
								continue;
							}

							Point l = ((IFormElement)element).getLocation();
							if (l == null)
							{
								continue;//unkown where to add
							}

							if (l.y >= startY && l.y < endY)
							{
								if (element instanceof ISupportAnchors)
								{
									int anchors = ((ISupportAnchors)element).getAnchors();
									if (((anchors & IAnchorConstants.EAST) != 0) && ((anchors & IAnchorConstants.WEST) != 0))
									{
										hasAtLeastOneAnchored = true;
										break;
									}
								}
							}
						}
					}
					if (hasAtLeastOneAnchored) shouldFillAllHorizSpace = true;
				}
			}

			// true for list view type and multi-line portals
			boolean listViewMode = viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW;

			if (obj instanceof Form)
			{
				html.append("<span servoy:id='info'></span>\n<table border=0 cellpadding=0 cellspacing=0 width='100%'>\n");
				html.append(getCssClassForElement(obj, ""));
			}
			else
			//is portal
			{
				Portal p = (Portal)obj;
				listViewMode = p.getMultiLine();

				sortable = p.getSortable();
				TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(form, p));
				BorderAndPadding ins = applyBaseComponentProperties(p, form, styleObj, null, null, sp);
				applyLocationAndSize(p, styleObj, ins, startY, endY, form.getSize().width, true, p.getAnchors(), sp);
				html.append("<div style='overflow: auto' ");
				html.append(getWicketIDParameter(form, p));
//				html.append(getJavaScriptIDParameter(p));
				html.append(getCssClassForElement(obj, userCssClassAdded, "portal"));
				html.append("><span servoy:id='info'></span>\n<table cellpadding='0' cellspacing='0' class='portal'>\n");//$NON-NLS-1$
			}

			int yOffset = 0;
			if (listViewMode)
			{
				if (obj instanceof Portal)
				{
					Iterator<IPersist> it = obj.getAllObjects();
					boolean isYOffsetSet = false;
					while (it.hasNext())
					{
						IPersist element = it.next();
						if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
						{
							Point l = ((IFormElement)element).getLocation();

							if (l == null) continue;//unknown where to add

							if (l.y >= startY && l.y < endY)
							{
								if (!isYOffsetSet || yOffset > l.y)
								{
									yOffset = l.y;
									isYOffsetSet = true;
								}
							}
						}
					}
				}
				css.addCSSBoundsHandler(new YOffsetCSSBoundsHandler(-startY - yOffset));
			}
			else
			{
				css.addCSSBoundsHandler(NoLocationCSSBoundsHandler.INSTANCE);
			}

			html.append("<tr><td height='99%'><table border=0 cellpadding=0 cellspacing=0 width='100%'>\n");
			if (addHeaders)
			{
				Map<String, GraphicalComponent> labelsFor = new HashMap<String, GraphicalComponent>();
				html.append("<thead ");
//				if (sortable) html.append("servoy:id='header'");
				html.append("servoy:id='header'");
				html.append("><tr class='headerbuttons'>\n");
				Iterator<IPersist> it1 = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				while (it1.hasNext())
				{
					IPersist element = it1.next();
					if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null &&
						!((GraphicalComponent)element).getLabelFor().equals(""))
					{
						labelsFor.put(((GraphicalComponent)element).getLabelFor(), (GraphicalComponent)element);
					}
				}
				boolean usesImageMedia = false;

				boolean hasAtLeastOneLabelFor = false;
				Iterator<IPersist> it2 = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				while (it2.hasNext())
				{
					IPersist element = it2.next();
					if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
					{
						if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
						{
							continue;
						}

						Point l = ((IFormElement)element).getLocation();
						if (l == null)
						{
							continue;//unkown where to add
						}

						if (l.y >= startY && l.y < endY)
						{
							GraphicalComponent label = labelsFor.get(((IFormElement)element).getName());
							if (label != null) hasAtLeastOneLabelFor = true;
						}
					}
				}

				int currentColumnCount = 0;
				int headerHeight = 0;
				it2 = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				while (it2.hasNext())
				{
					IPersist element = it2.next();
					if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
					{
						if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
						{
							continue;
						}

						Point l = ((IFormElement)element).getLocation();
						if (l == null)
						{
							continue;//unkown where to add
						}

						if (l.y >= startY && l.y < endY)
						{
							currentColumnCount++;
							html.append("<th ");
//							if (sortable) html.append(getWicketIDParameter(element));
							html.append(getWicketIDParameter(form, element));

							int w = ((IFormElement)element).getSize().width;
							w = w - 2; //minus left padding from css for th

							//					html.append("height='");
							//					html.append( ((IFormElement)element).getSize().height );
							//					html.append("' ");
							TextualStyle styleObj = new TextualStyle();
							GraphicalComponent label = labelsFor.get(((IFormElement)element).getName());
							if (label != null)
							{
								if (currentColumnCount == 1) headerHeight = label.getSize().height;
								html.append(' ');
								BorderAndPadding ins = applyBaseComponentProperties(label, form, styleObj, (Insets)DEFAULT_LABEL_PADDING.clone(), null, sp);
								// some css attributes were not applied
								Pair<IStyleSheet, IStyleRule> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, label, form);
								Border cssBorder = null;
								if (styleInfo != null)
								{
									IStyleRule s = styleInfo.getRight();
									IStyleSheet ss = styleInfo.getLeft();
									if (ss != null && s != null)
									{
										addAttributeToStyle(styleObj, CSS.Attribute.COLOR.toString(), s.getValue(CSS.Attribute.COLOR.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.BACKGROUND_COLOR.toString(),
											s.getValue(CSS.Attribute.BACKGROUND_COLOR.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT.toString(), s.getValue(CSS.Attribute.FONT.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_FAMILY.toString(), s.getValue(CSS.Attribute.FONT_FAMILY.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_SIZE.toString(), s.getValue(CSS.Attribute.FONT_SIZE.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_STYLE.toString(), s.getValue(CSS.Attribute.FONT_STYLE.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_VARIANT.toString(), s.getValue(CSS.Attribute.FONT_VARIANT.toString()));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_WEIGHT.toString(), s.getValue(CSS.Attribute.FONT_WEIGHT.toString()));

										cssBorder = ss.getBorder(s);
									}
								}

								if (ins.border != null) w = w - ins.border.left - ins.border.right;
								applyTextProperties(label, styleObj);
								TextualStyle bgborderstyleObj = new TextualStyle();
								if (!sortable)
								{
									bgborderstyleObj.setProperty("text-align", styleObj.getProperty("text-align"));
									bgborderstyleObj.setProperty("color", styleObj.getProperty("color"));
								}
								if (label.getImageMediaID() <= 0)
								{
									bgborderstyleObj.setProperty("background-image", "none");
									usesImageMedia = false;
								}
								else
								{
									usesImageMedia = true;
									//always remove background image here
									styleObj.remove(CSS.Attribute.BACKGROUND_IMAGE.toString());
								}

								String styleObjBgColor = styleObj.getProperty("background-color");
								if (styleObjBgColor != null && !"".equals(styleObjBgColor))
								{
									bgborderstyleObj.setProperty("background-color", styleObjBgColor);
								}
								// unless specified, set the font-weight for <th> to normal, as disabled parent component will make the font bold by default and we don't want that
								if (styleObj.getProperty(CSS.Attribute.FONT_WEIGHT.toString()) == null)
								{
									bgborderstyleObj.setProperty(CSS.Attribute.FONT_WEIGHT.toString(), "normal", false);
								}
								if (cssBorder != null && label.getBorderType() == null)
									ComponentFactoryHelper.createBorderCSSProperties(ComponentFactoryHelper.createBorderString(cssBorder), bgborderstyleObj);
								else ComponentFactoryHelper.createBorderCSSProperties(label.getBorderType(), bgborderstyleObj);
								if (headerHeight > 0) bgborderstyleObj.setProperty(CSS.Attribute.HEIGHT.toString(), headerHeight + "px");
								html.append(bgborderstyleObj.toString());
								Enumeration<Object> e = bgborderstyleObj.keys();
								while (e.hasMoreElements())
								{
									String key = (String)e.nextElement();
									styleObj.remove(key);
								}
								if (cssBorder != null || label.getBorderType() != null)
								{
									for (Attribute att : ServoyStyleSheet.borderAttributes)
									{
										// make sure all border attributes are removed
										styleObj.remove(att.toString());
									}
									for (String extendedStyleAttribute : ServoyStyleSheet.borderAttributesExtensions)
									{
										// make sure all border attributes are removed
										styleObj.remove(extendedStyleAttribute);
									}
								}
							}
							else
							{
								// If there is no label-for, we put a default right-border to all columns except the last.
								if (!hasAtLeastOneLabelFor)
								{
									TextualStyle defaultBorder = new TextualStyle();
									defaultBorder.setProperty("border-right", NO_LABELFOR_DEFAULT_BORDER_WIDTH + "px solid gray");
									html.append(defaultBorder.toString());
								}
							}
							if (sortable && !usesImageMedia)
							{
								html.append(getCssClassForElement((AbstractBase)element, "sortable"));
							}
							else
							{
								html.append(getCssClassForElement((AbstractBase)element, "nosort"));
							}
							html.append("width='");
							html.append(w);
							html.append("' ");
							html.append('>');
							int headerW = w;
							if (!hasAtLeastOneLabelFor) headerW -= 1;
							html.append("<table servoy:id='headerColumnTable' cellspacing='0' cellpadding='0' width='");
							html.append(headerW).append("px'><tr>");

							Object fontHeight = styleObj.get(CSS.Attribute.FONT_SIZE.toString());
							int cellHeight = 13;
							if (fontHeight != null)
							{
								String sFontHeight = fontHeight.toString().toLowerCase();
								if (sFontHeight.endsWith("px") || sFontHeight.endsWith("pt")) sFontHeight = sFontHeight.substring(0, sFontHeight.length() - 2);
								try
								{
									cellHeight = Integer.parseInt(sFontHeight);
									cellHeight += 2;
								}
								catch (NumberFormatException ex)
								{
									Debug.error(ex);
								}
							}
							html.append("<td height='").append(cellHeight).append("px'>");

							styleObj.setProperty("overflow", "hidden");
							styleObj.setProperty("left", "0px");
							styleObj.setProperty("right", "0px");
							styleObj.setProperty("position", "absolute");

							TextualStyle sortLinkStyle = new TextualStyle();
							sortLinkStyle.setProperty("position", "relative");
							int cellWidth = (headerW - SortableCellViewHeader.ARROW_WIDTH);
							sortLinkStyle.setProperty("width", cellWidth + "px");
							if (sortable)
							{
								sortLinkStyle.setProperty("text-align", styleObj.getProperty("text-align"));
								html.append("<a servoy:id='sortLink' ");
//								if (element instanceof ISupportDataProviderID && ((ISupportDataProviderID)element).getDataProviderID() != null)
//								{
//									html.append("class='orderOff' ");
//								}
								html.append(sortLinkStyle.toString());
								html.append('>');
								html.append("<div servoy:id='headertext' ");
								styleObj.setProperty("cursor", "pointer");
								styleObj.setProperty("width", cellWidth + "px");
								html.append(styleObj.toString());
								html.append("></div>");
								html.append("</a>");
							}
							else
							{
								html.append("<div servoy:id='sortLink' ");
								html.append(sortLinkStyle.toString());
								html.append('>');
								html.append("<div servoy:id='headertext' ");
								html.append(styleObj.toString());
								html.append("></div>");
								html.append("</div>");
							}
							html.append("</td><td valign='middle' align='right'>");
							if (sortable)
							{
								html.append("<img servoy:id='resizeBar'></img>");
							}
							else
							{
								html.append("<span servoy:id='resizeBar'>&nbsp;</span>");
//								html.append("<span ");
//								html.append(getWicketIDParameter(element));
//								html.append(styleObj.toString());
//								html.append("></span>");
							}

							html.append("</td></tr></table>\n");
							html.append("</th>\n");
						}
					}
				}

				// add filler, needed for column resize.
				// when the table has no horizontal scrollbar, then there should be no filler.
				if (!shouldFillAllHorizSpace)
				{
					html.append("<th");
					if (bgColor != null)
					{
						html.append(" style=\"background-image: none; background-color: ");
						html.append(PersistHelper.createColorString(bgColor));
						html.append(";\"");
					}
					html.append(">&nbsp;</th>\n"); // add filler (need to be a space else safari & ie7 will not display correctly)
				}

				html.append("</tr></thead>\n");
			}
			html.append("<tbody servoy:id='rowsContainerBody'>\n");

			StringBuffer columns = new StringBuffer();
			int firstComponentHeight = -1;
			if (listViewMode)
			{
				if (!(obj instanceof Portal))
				{
					Iterator<Part> partIte = form.getParts();
					while (partIte.hasNext())
					{
						Part p = partIte.next();
						if (p.getPartType() == Part.BODY)
						{
							firstComponentHeight = p.getHeight() - startY;
							break;
						}
					}
				}

				StringBuffer sbElements = new StringBuffer();
				Iterator< ? > it = obj instanceof Portal ? obj.getAllObjects() : form.getFormElementsSortedByFormIndex();
				while (it.hasNext())
				{
					Object element = it.next();
					if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
					{
						Point l = ((IFormElement)element).getLocation();

						if (l == null) continue;//unknown where to add

						if (l.y >= startY && l.y < endY)
						{
							if (obj instanceof Portal)
							{
								Dimension d = ((IFormElement)element).getSize();
								if (l.y + d.height > firstComponentHeight) firstComponentHeight = l.y + d.height;
							}

							createTableViewComponentHTMLAndStyles((IFormElement)element, form, sbElements, css, bgColor, startY, endY, enableAnchoring, sp);
							sbElements.append('\n');
						}
					}
				}

				columns.append("<td><div servoy:id='listViewItem' class=\"listViewItem\" tabindex=\"-1\" style=\"position: absolute; height: ").append(
					firstComponentHeight - yOffset).append("px;");
				if (enableAnchoring || obj instanceof Portal)
				{
					columns.append(" left: 0px; right: 0px;");
				}
				else
				{
					columns.append(" width: ").append(form.getWidth()).append("px;");
				}
				columns.append("\">");
				columns.append(sbElements);
				columns.append("</div></td>\n");
			}
			else
			{
				Iterator<IPersist> it = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				while (it.hasNext())
				{
					IPersist element = it.next();
					if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
					{
						if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
						{
							continue;
						}

						Point l = ((IFormElement)element).getLocation();
						if (l == null)
						{
							continue;//unkown where to add
						}
						if (l.y >= startY && l.y < endY)
						{
							columns.append("<td ");
							if (element instanceof ISupportName)
							{
								String name = ((ISupportName)element).getName();
								if (((name != null) && (name.trim().length() > 0)) || addHeaders)
								{
									// this column's cells can be made invisible (and <td> tag is the one that has to change)
									// so we will link this <td> to a wicket component
									columns.append("servoy:id='");
									columns.append(ComponentFactory.getWebID(form, element));
									columns.append("_' ");
								}
							}
							if (firstComponentHeight == -1) firstComponentHeight = ((IFormElement)element).getSize().height;
							if (!addHeaders && !shouldFillAllHorizSpace)
							{
								columns.append("width='");
								int w = ((IFormElement)element).getSize().width;
//								w = w - (2+2);  //minus left+rigth padding from css for th
								columns.append(w);
								columns.append("' ");
							}
							//					columns.append("valign='middle' ");
							columns.append('>');
							String cssClass = TABLE_VIEW_CELL_CLASS;
							if (element instanceof Field && ((Field)element).getDisplayType() == Field.COMBOBOX)
							{
								cssClass += " " + COMBOBOX_CLASS;
							}
							columns.append("<div class='" + cssClass + "' style='position:relative'>");
							TextualStyle classBasedStyle = createTableViewComponentHTMLAndStyles(element, form, columns, css, bgColor, startY, endY, false, sp);
							if (element instanceof Field)
							{
								int type = ((Field)element).getDisplayType();
								if (type == Field.PASSWORD || type == Field.TEXT_FIELD || type == Field.TYPE_AHEAD || type == Field.TEXT_AREA)
								{
									classBasedStyle.setProperty("float", "left");
								}
							}
							columns.append("</div>\n");
							columns.append("</td>\n");
						}
					}
				}
			}

			// add filler, needed for column resize.
			// no filler when the tableview has no horizontal scrollbar.
			if (!shouldFillAllHorizSpace)
			{
				columns.append("<td>&nbsp;</td>\n"); // add filler (need to be a space else safari & ie7 will not display correctly)
			}

			html.append("<tr servoy:id='rows' ");
			html.append("height='");
			html.append(firstComponentHeight - yOffset);
			html.append("' ");
			if ((obj instanceof ISupportRowBGColorScript) &&
				(((ISupportRowBGColorScript)obj).getRowBGColorScript() == null || ((ISupportRowBGColorScript)obj).getRowBGColorScript().trim().length() == 0))
			{
				html.append("class='even'");
			}
			html.append(">\n");
			html.append(columns);
			html.append("</tr>\n");
			html.append("</tbody></table>\n");
			html.append("</td></tr>\n");
			html.append("<tr valign='bottom'>\n");
			html.append("<td servoy:id='navigator' height='1%'>&nbsp;</td>\n");
			html.append("</tr>\n");
			html.append("</table>\n");
			if (!(obj instanceof Form))
			{
				html.append("</div>\n");
			}
			html.append("\n\n");
		}
		finally
		{
			css.removeCSSBoundsHandler();
		}
	}

	private static TextualStyle createTableViewComponentHTMLAndStyles(IPersist element, Form form, StringBuffer columns, TextualCSS css, Color bgColor,
		int startY, int endY, boolean enableAnchoring, IServiceProvider sp) throws RepositoryException
	{
		createComponentHTML(element, form, columns, css, bgColor, startY, endY, enableAnchoring, sp);
		TextualStyle idBasedStyle = css.addStyle('#' + ComponentFactory.getWebID(form, element));
		TextualStyle classBasedStyle = css.addStyle('.' + ComponentFactory.getWebID(form, element));
		classBasedStyle.copyAllFrom(idBasedStyle);
		if (element instanceof Field && isCompositeTextField(((Field)element).getDisplayType()))
		{
			// change it from id selector to class selector for table columns
			String s = ComponentFactory.getWebID(form, element) + WebDataCompositeTextField.AUGMENTED_FIELD_ID;
			TextualStyle classBasedTextStyle = css.get("#" + s);
			if (classBasedTextStyle != null) css.put("." + s, classBasedTextStyle);
		}


		return classBasedStyle;
	}

	private static void addAttributeToStyle(TextualStyle style, String attributeKey, Object attributeValue)
	{
		if (attributeValue != null)
		{
			style.setProperty(attributeKey, attributeValue.toString(), false);
		}
	}

	public static class TextualCSS extends TreeMap<String, TextualStyle>
	{
		private static final String[] strings = new String[] { "input.button", "span.label", "input.field", ".label", ".field" };

		static
		{
			Arrays.sort(strings);
		}

		private static final long serialVersionUID = 1L;

		private static Comparator<String> css = new Comparator<String>()
		{

			public int compare(String o1, String o2)
			{
				if (o1.equals(o2)) return 0;
				int index1 = Arrays.binarySearch(strings, o1);
				int index2 = Arrays.binarySearch(strings, o2);
				if (index1 >= 0 && index2 >= 0)
				{
					return index1 - index2;
				}
				else if (index1 >= 0) return -1;
				else if (index2 >= 0) return 1;
				return (o1).compareToIgnoreCase(o2);
			}

		};

		private final Stack<ICSSBoundsHandler> handlers = new Stack<ICSSBoundsHandler>();

		public TextualCSS()
		{
			super(css);
			handlers.push(DefaultCSSBoundsHandler.INSTANCE);
		}

		public ArrayList<Pair<String, String>> getAsSelectorValuePairs()
		{
			ArrayList<Pair<String, String>> selectorValuePairs = new ArrayList<Pair<String, String>>();

			Iterator<Map.Entry<String, TextualStyle>> iter = entrySet().iterator();
			while (iter.hasNext())
			{
				Map.Entry<String, TextualStyle> selectorTextualStyle = iter.next();
				selectorValuePairs.add(new Pair<String, String>(selectorTextualStyle.getKey(), selectorTextualStyle.getValue().toString("")));
			}

			return selectorValuePairs;
		}

		@Override
		public String toString()
		{
			StringBuffer cssString = new StringBuffer();
			Iterator<TextualStyle> iter = values().iterator();
			while (iter.hasNext())
			{
				Object element_style = iter.next();
				cssString.append(element_style.toString());
			}
			return cssString.toString();
		}

		public ICSSBoundsHandler getCSSBoundsHandler()
		{
			return handlers.peek();
		}

		public void addCSSBoundsHandler(ICSSBoundsHandler b)
		{
			handlers.push(b);
		}

		public void removeCSSBoundsHandler()
		{
			handlers.pop();
		}

		public TextualStyle addStyle(String selector)
		{
			TextualStyle ts = get(selector);
			if (ts != null)
			{
				return ts;
			}
			ts = new TextualStyle(selector, this);
			put(selector, ts);
			return ts;
		}
	}

	public static class TextualStyle extends Properties
	{
		private static final long serialVersionUID = 1L;

		private String selector;
		private final List<String> order = new ArrayList<String>();
		private TextualCSS css;
		private final Map<String, List<String>> stackedValues = new HashMap<String, List<String>>();

		public TextualStyle()
		{
			//used when style tag propery is needed
		}

		TextualStyle(String selector, TextualCSS css)
		{
			if (selector == null) throw new NullPointerException("selector cannot be null");
			this.selector = selector;
			this.css = css;
		}

		@Override
		public synchronized Object setProperty(String name, String value)
		{
			if (name == null) return null;
			if (value == null)
			{
				stackedValues.remove(name);
				return remove(name);
			}
			else
			{
				return setProperty(name, value, true);
			}
		}

		@Override
		public synchronized Object remove(Object key)
		{
			order.remove(key);
			return super.remove(key);
		}

		public Object setProperty(String name, String value, boolean override)
		{
			return setProperty(name, (value != null ? new String[] { value } : null), override);
		}

		public Object setProperty(String name, String[] values, boolean override)
		{
			if (override)
			{
				stackedValues.remove(name);
				order.remove(name);
				order.add(name);
				stackedValues.put(name, Arrays.asList(values));
				return super.put(name, values[values.length - 1]);
			}
			else
			{
				if (!super.containsKey(name))
				{
					order.add(name);
					List<String> valuesList = stackedValues.get(name);
					List<String> newValues = Arrays.asList(values);
					if (valuesList == null)
					{
						valuesList = new ArrayList<String>();
						stackedValues.put(name, valuesList);
					}
					valuesList.addAll(newValues);
					return super.put(name, values[values.length - 1]);
				}
			}
			return null;
		}

		@Override
		public String toString()
		{
			return toString(selector);
		}

		public String toString(String pSelector)
		{
			StringBuffer retval = new StringBuffer();
			if (pSelector == null)
			{
				retval.append("style='");
			}
			else
			{
				retval.append(pSelector);
				retval.append("\n{\n");
			}
			retval.append(getValuesAsString(pSelector));
			if (pSelector == null)
			{
				retval.append("' ");
			}
			else
			{
				retval.append("}\n\n");
			}
			return retval.toString();
		}

		public String getValuesAsString(String pSelector)
		{
			StringBuffer retval = new StringBuffer();
			Iterator<String> it = order.iterator();
			while (it.hasNext())
			{
				String name = it.next();
				String[] cssValues = null;
				List<String> values = stackedValues.get(name);
				if (values != null && values.size() > 1)
				{
					cssValues = values.toArray(new String[values.size()]);
				}
				else
				{
					cssValues = new String[] { getProperty(name) };
				}
				for (String val : cssValues)
				{
					if (CSSName.BACKGROUND_IMAGE.toString().equals(name) && val != null && val.startsWith("linear-gradient"))
					{
						String[] colors = getGradientColors(val);
						if (colors != null && colors.length == 2 && colors[0] != null)
						{
							appendValue(retval, pSelector, name, "-webkit-gradient(linear, " + (val.contains("top") ? "center" : "left") + " top, " +
								(val.contains("top") ? "center bottom" : "right top") + ", from(" + colors[0] + "), to(" + colors[1] + "))");
							boolean hasRoundedRadius = false;
							for (String attribute : ServoyStyleSheet.ROUNDED_RADIUS_ATTRIBUTES)
							{
								String value = getProperty(attribute);
								if (value != null && !value.startsWith("0"))
								{
									hasRoundedRadius = true;
									break;
								}
								List<String> roundedBorderValues = stackedValues.get(attribute);
								if (roundedBorderValues != null)
								{
									for (String borderValue : roundedBorderValues)
									{
										if (borderValue != null && !borderValue.startsWith("0"))
										{
											hasRoundedRadius = true;
											break;
										}
									}
								}
							}
							if (!hasRoundedRadius)
							{
								// filter doesn't get along with rounded border; css should define fallback for this
								appendValue(retval, pSelector, "filter", "progid:DXImageTransform.Microsoft.gradient(startColorStr=" + colors[0] +
									", EndColorStr=" + colors[1] + ", GradientType=" + (val.contains("top") ? "0" : "1") + ")");
							}
						}
						for (String linearIdentifier : ServoyStyleSheet.LinearGradientsIdentifiers)
						{
							appendValue(retval, pSelector, name, val.replace("linear-gradient", linearIdentifier));
						}
					}
					if (CSSName.OPACITY.toString().equals(name))
					{
						float opacity = Utils.getAsFloat(val);
						appendValue(retval, pSelector, "filter", "alpha(opacity=" + Float.valueOf(opacity * 100).intValue() + ")");
					}
					if (name.contains("radius") && name.contains("border"))
					{
						for (String prefix : ServoyStyleSheet.ROUNDED_RADIUS_PREFIX)
						{
							appendValue(retval, pSelector, prefix + name, val);
						}
						if ((getProperty(CSSName.BACKGROUND_COLOR.toString()) != null || stackedValues.get(CSSName.BACKGROUND_COLOR.toString()) != null) &&
							!retval.toString().contains("background-clip:"))
						{
							// we also have background color, i guess we expect padding-box background-clip
							appendValue(retval, pSelector, "-moz-background-clip", "padding");
							appendValue(retval, pSelector, "-webkit-background-clip", "padding-box");
							appendValue(retval, pSelector, "background-clip", "padding-box");
						}
					}
					if (name.equals(CSSName.FONT_FAMILY.toString()))
					{
						val = HtmlUtils.getValidFontFamilyValue(val);
					}
					appendValue(retval, pSelector, name, val);
				}
			}
			return retval.toString();
		}

		protected void appendValue(StringBuffer retval, String pSelector, String name, String value)
		{
			if (pSelector != null) retval.append('\t');
			retval.append(name);
			retval.append(": ");
			retval.append(value);
			retval.append(';');
			if (pSelector != null) retval.append('\n');
		}

		private String[] getGradientColors(String cssDeclaration)
		{
			String[] colors = new String[2];
			cssDeclaration = cssDeclaration.substring(cssDeclaration.indexOf('(') + 1, cssDeclaration.lastIndexOf(')'));
			StringTokenizer tokenizer = new StringTokenizer(cssDeclaration, ",");
			if (cssDeclaration.startsWith("top") || cssDeclaration.startsWith("left") || cssDeclaration.startsWith("right") ||
				cssDeclaration.startsWith("bottom")) tokenizer.nextElement();
			for (int i = 0; i < 2; i++)
			{
				if (tokenizer.hasMoreElements())
				{
					String colorString = tokenizer.nextToken().trim();
					if (colorString.startsWith("rgb"))
					{
						while (tokenizer.hasMoreElements())
						{
							String token = tokenizer.nextToken();
							colorString += "," + token;
							if (token.contains(")")) break;
						}
					}
					colors[i] = colorString;
				}
				else
				{
					return null;
				}
			}
			return colors;
		}

		public String getOnlyProperties()
		{
			StringBuffer retval = new StringBuffer();
			Iterator<String> it = order.iterator();
			while (it.hasNext())
			{
				String name = it.next();
				String val = getProperty(name);
				retval.append(name);
				retval.append(": ");
				retval.append(val);
				retval.append(';');
			}
			return retval.toString();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (selector == null) return false;
			return selector.equals(obj);
		}

		public TextualCSS getTextualCSS()
		{
			return css;
		}

		public void copy(String name, TextualStyle source)
		{
			if (source.containsKey(name)) setProperty(name, source.getProperty(name));
		}

		public void copyAllFrom(TextualStyle source)
		{
			Iterator<String> it = source.order.iterator();
			while (it.hasNext())
			{
				String name = it.next();
				String val = source.getProperty(name);
				setProperty(name, val);
			}
		}

//		public boolean containsKey(Object name)
//		{
//			return properties.containsKey(name);
//		}
//
//		public String getProperty(String name)
//		{
//			return (String) properties.get(name);
//		}
	}


	@SuppressWarnings("unchecked")
	public static Pair<String, String>[] getStyles() throws RepositoryException, RemoteException
	{
		List<Pair<String, String>> retval = new ArrayList<Pair<String, String>>();
		IRepository repository = ApplicationServerRegistry.get().getLocalRepository();
		RootObjectMetaData[] styleMetaDatas = repository.getRootObjectMetaDatasForType(IRepository.STYLES);
		if (styleMetaDatas != null)
		{
			for (RootObjectMetaData styleMetaData : styleMetaDatas)
			{
				retval.add(new Pair<String, String>(styleMetaData.getName(), getStyleCSS(styleMetaData.getName())));
			}
		}
		return retval.toArray(new Pair[retval.size()]);
	}

	private static void addDefaultCheckRadioStuff(TextualCSS css, String className)
	{
		TextualStyle styleObj = css.addStyle("div." + className + " label");
		styleObj.setProperty("vertical-align", "super");
		styleObj = css.addStyle("div." + className + " div.inl");
		styleObj.setProperty("display", "inline");
		styleObj.setProperty("white-space", "nowrap");
		styleObj.setProperty("float", "left");
		styleObj = css.addStyle("div." + className + " div.inl:focus");
		styleObj.setProperty("outline", "1px dotted black;");
		styleObj = css.addStyle("div." + className + " div.blk:focus");
		styleObj.setProperty("outline", "1px dotted black;");
		styleObj = css.addStyle("div." + className + " div label");
		styleObj.setProperty("vertical-align", "middle");
		styleObj = css.addStyle("div." + className + " div input");
		styleObj.setProperty("vertical-align", "middle");
		styleObj.setProperty("margin", "2px");
		styleObj.setProperty("padding", "0px");
		styleObj.setProperty("width", "16px");
		styleObj.setProperty("height", "16px");
	}

	public static String getStyleCSS(String name) throws RepositoryException, RemoteException
	{
		if (name != null && name.toLowerCase().endsWith(".css"))
		{
			name = name.substring(0, name.length() - 4);
		}
		TextualCSS css = new TextualCSS();
		TextualStyle styleObj = null;

		// loading indicator stuff
		styleObj = css.addStyle("span.indicator");
		styleObj.setProperty("z-index", "99999");
		styleObj.setProperty("position", "absolute");
		styleObj.setProperty("right", "0px");
		styleObj.setProperty("top", "0px");
		styleObj.setProperty("background-color", "#FAD163");
		styleObj.setProperty("color", "#000000");

		//default field stuff
		styleObj = css.addStyle(".field , .listbox, .spinner");//input, select, textarea, listbox");
		styleObj.setProperty("padding", createInsetsText(DEFAULT_FIELD_PADDING));
		styleObj.setProperty("margin", "0");
		styleObj.setProperty("border-style", "inset");
		styleObj.setProperty("border-width", createInsetsText(DEFAULT_FIELD_BORDER_SIZE));
		styleObj.setProperty("border-spacing", "0px 0px");
		styleObj.setProperty("border-color", "#D4D0C8");
		styleObj.setProperty("background-color", "#FFFFFF");

		// default list separator stuff
		styleObj = css.addStyle("optgroup.separator"); // combobox, listbox
		// trying to make a gray horizontal line here with style - but <optgroup> element is very restricted on most browsers (couldn't make IE/FF/Opera work here)
//		styleObj.setProperty("background-image", "url('/images/grayDot.gif')");
//		styleObj.setProperty("background-repeat", "repeat-x");
//		styleObj.setProperty("background-position", "0% 50%");
//		styleObj.setProperty("height", "1px");
//		styleObj.setProperty("margin-top", "7px");
//		styleObj.setProperty("margin-bottom", "7px");
		// or the same effect only works in FF
		styleObj.setProperty("border-top", "1px solid gray");
		styleObj.setProperty("margin-top", "7px");
		styleObj.setProperty("margin-bottom", "7px");

		//default radio/check field stuff
		styleObj = css.addStyle(".radioCheckField");
		styleObj.setProperty("padding", createInsetsText(DEFAULT_FIELD_PADDING));
		styleObj.setProperty("margin", "0");
		styleObj.setProperty("border-style", "inset");
		styleObj.setProperty("border-width", createInsetsText(DEFAULT_FIELD_BORDER_SIZE));
		styleObj.setProperty("border-spacing", "0px 0px");
		styleObj.setProperty("border-color", IStyleSheet.COLOR_TRANSPARENT);
		styleObj.setProperty("background-color", "#FFFFFF");

		//default label stuff
		styleObj = css.addStyle(".label");//input, select, textarea");
		styleObj.setProperty("padding", createInsetsText(DEFAULT_LABEL_PADDING));
		styleObj.setProperty("overflow", "hidden");

		//default button stuff
		styleObj = css.addStyle(".button");
		styleObj.setProperty("padding", createInsetsText(DEFAULT_BUTTON_PADDING));
		styleObj.setProperty("overflow", "hidden");

		Style s = (Style)ApplicationServerRegistry.get().getLocalRepository().getActiveRootObject(name, IRepository.STYLES);
		boolean bodyMarginAdded = false;
		if (s != null)
		{
			FixedStyleSheet ss = new FixedStyleSheet();
			try
			{
				String styleText = s.getCSSText();
				//				System.out.println(styleText);
				ss.addRule(styleText);
			}
			catch (Exception e)
			{
				Debug.error(e);//parsing can fail in java 1.5
			}
			Enumeration< ? > e = ss.getStyleNames();
			while (e.hasMoreElements())
			{
				javax.swing.text.Style a_style = ss.getStyle((String)e.nextElement());
				String a_style_name = a_style.getName();

				if ("default".equalsIgnoreCase(a_style_name)) continue;

//					a_style_name = Utils.stringReplaceExact(a_style_name, "button", "input.button");
				a_style_name = Utils.stringReplaceExact(a_style_name, "label", ".label");
//				a_style_name = Utils.stringReplaceExact(a_style_name, "form", "body");
				a_style_name = Utils.stringReplaceExact(a_style_name, "tabpanel", ".tabpanel");
				a_style_name = Utils.stringReplaceExact(a_style_name, "portal", "table.portal");
				a_style_name = Utils.stringReplaceExact(a_style_name, "field", ".field");
				a_style_name = Utils.stringReplaceExact(a_style_name, "check", ".check");
				a_style_name = Utils.stringReplaceExact(a_style_name, "radio", ".radio");
				a_style_name = Utils.stringReplaceExact(a_style_name, "listbox", ".listbox");
				a_style_name = Utils.stringReplaceExact(a_style_name, "spinner", ".spinner");
				// hack if previous command replaced label_field with label_.field
				a_style_name = Utils.stringReplace(a_style_name, "..field", ".field");

				a_style_name = Utils.stringReplace(a_style_name, ".", "_", 1);

				styleObj = css.addStyle(a_style_name);
				if (a_style_name.equals("body"))
				{
					styleObj.setProperty("margin", "0px 0px 0px 0px");
					bodyMarginAdded = true;
				}
				Enumeration< ? > names = a_style.getAttributeNames();
				while (names.hasMoreElements())
				{
					Object attr = names.nextElement();
					if ("name".equalsIgnoreCase(attr.toString())) continue;
					Object val = a_style.getAttribute(attr);
					String s_attr = attr.toString();
					s_attr = Utils.stringReplace(s_attr, "margin", "padding");
					if (s_attr.equals("font-size"))
					{
						String tmp = val.toString();
						if (tmp.endsWith("px"))
						{
							int size = Utils.getAsInteger(tmp.substring(0, tmp.length() - 2));

							// 9 should be defined hard. Because 12 (1.33*9) is to big.
							if (size == 9)
							{
								size = 11;
							}
							else
							{
								size = (int)(size * (4 / (double)3));
							}
							styleObj.setProperty(s_attr, size + "px");
						}
						else
						{
							int size = 0;
							if (tmp.endsWith("pt"))
							{
								size = Utils.getAsInteger(tmp.substring(0, tmp.length() - 2));
							}
							else
							{
								size = Utils.getAsInteger(tmp);
							}
							// 9 should be defined hard. Because 6 (0.75*9) is to small.
							if (size == 9)
							{
								size = 7;
							}
							else
							{
								size = (int)(size * (3 / (double)4));
							}
							styleObj.setProperty(s_attr, size + "pt");
						}
					}
					else
					{
						styleObj.setProperty(s_attr, val.toString());
					}
				}

				//enhance existing button def
				if (a_style_name.startsWith("button"))
				{
//					enhanceInputButton(styleObj);
				}
			}
		}

		//create button def if missing
		if (!css.containsKey("button"))
		{
			styleObj = css.addStyle("button");
//			enhanceInputButton(styleObj);
		}

		if (!bodyMarginAdded)
		{
			styleObj = css.addStyle("body");
			styleObj.setProperty("margin", "0px 0px 0px 0px");
		}

		int h1 = 0;
		int h2 = 2;

		//default tab panel stuff
		styleObj = css.addStyle("div.tabs");
		styleObj.setProperty("padding", "3px 0px " + (h1) + "px 2px"); //controls position of bottom line of tab strip
		styleObj.setProperty("margin", h2 + "px 0px 0px 0px");//controls the space outside the tabs rect.
		styleObj.setProperty("border-bottom", "1px solid #9ac");
		styleObj.setProperty("left", "0px");
		styleObj.setProperty("right", "0px");
		styleObj.setProperty("position", "absolute");

		styleObj = css.addStyle("div.webform");//$NON-NLS-1$
		styleObj.setProperty("background-color", PersistHelper.createColorString(DEFAULT_FORM_BG_COLOR));

		styleObj = css.addStyle("div.opaquecontainer");//$NON-NLS-1$
		styleObj.setProperty("background-color", PersistHelper.createColorString(DEFAULT_FORM_BG_COLOR));

		//default tab stuff
		styleObj = css.addStyle("div.tabs div");
		styleObj.setProperty("display", "inline");
		styleObj.setProperty("float", "left");
		styleObj.setProperty("clear", "none");

		styleObj = css.addStyle("div.tabs div a");
		styleObj.setProperty("text-decoration", "none");
		styleObj.setProperty("padding", (h2 - 1) + "px 10px " + (h1 + 1) + "px 1px");//space inside tab arround the text
		styleObj.setProperty("margin-top", "0.2em");//space betweens tabs
		styleObj.setProperty("margin-left", "0.2em");//space betweens tabs
		styleObj.setProperty("background-color", "#e7e7f7");
		styleObj.setProperty("border", "1px solid #9ac");
		styleObj.setProperty("border-bottom", "1px solid #9ac");
		styleObj.setProperty("float", "left");
		styleObj.setProperty("position", "relative");
		styleObj.setProperty("top", "1px");

		styleObj = css.addStyle("div.tabs div a span");
		styleObj.setProperty("padding-left", "10px");

		styleObj = css.addStyle("div.tabs div a:hover");
		styleObj.setProperty("color", "#fff");
		styleObj.setProperty("background-color", "#c7cce7");

		styleObj = css.addStyle("div.tabs div.selected_tab a");
		styleObj.setProperty("color", "#000");
		styleObj.setProperty("background-color", "#fff");
		styleObj.setProperty("border-bottom", "1px solid #fff");

		styleObj = css.addStyle("div.tabs div.disabled_tab a");
		styleObj.setProperty("color", "lightslategrey");
		styleObj.setProperty("background-color", "#e7e7f7");

		styleObj = css.addStyle("div.tabs div.disabled_selected_tab a");
		styleObj.setProperty("color", "lightslategrey");
		styleObj.setProperty("background-color", "#fff");
		styleObj.setProperty("border-bottom", "1px solid #fff");

		styleObj = css.addStyle(".tabcontainer");
		styleObj.setProperty("border-width", "0px 1px 1px 1px");
		styleObj.setProperty("border-color", "#99AACC");
		styleObj.setProperty("border-style", "solid");


		//default font stuff
		StringBuilder defaultFontElements = new StringBuilder();
		defaultFontElements.append("body");
		for (String dfe : DEFAULT_FONT_ELEMENTS)
		{
			defaultFontElements.append(", ");
			defaultFontElements.append(dfe);
		}
		styleObj = css.addStyle(defaultFontElements.toString());
		styleObj.setProperty("font-family", "Tahoma, Arial, Helvetica, sans-serif");
		styleObj.setProperty("font-size", DEFAULT_FONT_SIZE + "px");
//			styleObj.setProperty("-moz-box-sizing", "content-box");


		// default radio/check stuff
		addDefaultCheckRadioStuff(css, "radioCheckField");
		addDefaultCheckRadioStuff(css, "check");
		addDefaultCheckRadioStuff(css, "radio");

		//default table stuff (used by portal/listview)
		styleObj = css.addStyle("th.nosort");
		styleObj.setProperty("padding", "2px 0px 2px 2px");
		styleObj.setProperty("text-align", "center");
		styleObj.setProperty("vertical-align", "top");
		styleObj.setProperty("font-weight", "normal");
		styleObj = css.addStyle("th.sortable");
		styleObj.setProperty("padding", SORTABLE_HEADER_PADDING + "px 0px " + SORTABLE_HEADER_PADDING + "px " + SORTABLE_HEADER_PADDING + "px");
//			styleObj.setProperty("text-align","center");
		styleObj.setProperty("vertical-align", "top");
		styleObj = css.addStyle("td");
		styleObj.setProperty("vertical-align", "top");
		styleObj = css.addStyle("tr.headerbuttons");
		styleObj.setProperty("background-image", "url(/servoy-webclient/templates/lafs/kunststoff/images/button/secondary-enabled.gif)", false);
		styleObj.setProperty("background-color", "#DBE3EB", false);
		styleObj.setProperty("background-repeat", "repeat-x", false);
		styleObj.setProperty("background-position", "center center", false);
		styleObj = css.addStyle("th.sortable a:visited, th.nosort a:visited");
		styleObj.setProperty("text-decoration", "none");
		styleObj.setProperty("color", "black");
		styleObj = css.addStyle("th.sortable a:hover, th.nosort a:hover");
		styleObj.setProperty("text-decoration", "underline");
		styleObj.setProperty("color", "black");
		styleObj = css.addStyle("th.nosort a");
		styleObj.setProperty("text-decoration", "none");
		styleObj = css.addStyle("th.sortable a");
		styleObj.setProperty("text-decoration", "none");//underline
		styleObj.setProperty("font-weight", "normal");
		styleObj.setProperty("color", "black");
		styleObj.setProperty("background-position", "right");
		styleObj.setProperty("background-repeat", "no-repeat", false);
		styleObj.setProperty("display", "block");
//		styleObj = css.addStyle("th.sortable a.orderOff");
//		styleObj.setProperty("background-image", "url(/servoy-webclient/templates/lafs/kunststoff/images/sortheader/arrow_off.png)");
//		styleObj = css.addStyle("th.sortable a.orderAsc");
//		styleObj.setProperty("background-image", "url(/servoy-webclient/templates/lafs/kunststoff/images/sortheader/arrow_down.png)");
//		styleObj = css.addStyle("th.sortable a.orderDesc");
//		styleObj.setProperty("background-image", "url(/servoy-webclient/templates/lafs/kunststoff/images/sortheader/arrow_up.png)");

		//let users define this
		//		styleObj = css.addStyle("tr.odd");
		//		styleObj.add("background-color","#fff");
		//		styleObj = css.addStyle("tr.even");
		//		styleObj.add("background-color","#fea");
		styleObj = css.addStyle(".tableviewcell");
		styleObj.setProperty("width", "100%");
		styleObj.setProperty("height", "100%");

		//accordion link margin for image custom
		styleObj = css.addStyle(".accordionlinkmargin");
		styleObj.setProperty("margin", "5px");

		// tooltip stuff
		styleObj = css.addStyle("#mktipmsg");
		styleObj.setProperty("padding", "2px");
		styleObj.setProperty("background-color", "#FFFFE1");
		styleObj.setProperty("border", "1px solid #000000");
		styleObj.setProperty("font-family", "arial, helvetica, tahoma, sans-serif");
		styleObj.setProperty("font-size", "11px");
		styleObj.setProperty("color", "#000000");
		styleObj.setProperty("display", "none");
		styleObj.setProperty("position", "absolute;left:0px;top:0px");

		// blocker body
		styleObj = css.addStyle(".blocker");
		styleObj.setProperty("cursor", "progress");

		// disable outline on focused div's (makes scrollbars appear in FF when mouse is clicked inside some div's)
		styleObj = css.addStyle("div:focus");
		styleObj.setProperty("outline", "none");

		// disable text area resizing
		styleObj = css.addStyle("textarea");
		styleObj.setProperty("resize", "none");


		// dialog
		styleObj = css.addStyle("div.wicket-modal div.w_content_2");
		styleObj.setProperty("background-color", "transparent !important");
		styleObj.setProperty("padding-top", "0 !important");

		styleObj = css.addStyle("div.wicket-modal div.w_content");
		styleObj.setProperty("background-color", "transparent !important");

		// modal dialog
		styleObj = css.addStyle(
			"div.wicket-modal div.w_undecorated div.w_caption,div.wicket-modal div.w_undecorated div.w_top,div.wicket-modal div.w_undecorated div.w_bottom,div.wicket-modal div.w_undecorated div.w_topLeft,div.wicket-modal div.w_undecorated div.w_topRight,div.wicket-modal div.w_undecorated div.w_bottomRight,div.wicket-modal div.w_undecorated div.w_bottomLeft,div.wicket-modal div.w_undecorated a.w_close");
		styleObj.setProperty("display", "none");

		styleObj = css.addStyle(
			"div.wicket-modal div.w_undecorated div.w_caption,div.wicket-modal div.w_undecorated div.w_content,div.wicket-modal div.w_undecorated div.w_content_container,div.wicket-modal div.w_undecorated div.w_content_1,div.wicket-modal div.w_undecorated div.w_content_2,div.wicket-modal div.w_undecorated div.w_content_3,div.wicket-modal div.w_undecorated div.w_top,div.wicket-modal div.w_undecorated div.w_top_1,div.wicket-modal div.w_undecorated div.w_bottom,div.wicket-modal div.w_undecorated div.w_bottom_1,div.wicket-modal div.w_undecorated div.w_topLeft,div.wicket-modal div.w_undecorated div.w_left,div.wicket-modal div.w_undecorated div.w_topRight,div.wicket-modal div.w_undecorated div.w_right,div.wicket-modal div.w_undecorated div.w_right_1,div.wicket-modal div.w_undecorated div.w_bottomRight,div.wicket-modal div.w_undecorated div.w_bottomLeft,div.wicket-modal div.w_undecorated a.w_close,div.wicket-modal div.w_undecorated span.w_captionText");
		styleObj.setProperty("padding", "0px");
		styleObj.setProperty("margin", "0px");
		styleObj.setProperty("border-width", "0px");

		// placeholder style
		styleObj = css.addStyle(".placeholder");
		styleObj.setProperty("color", "#aaaaaa");
		styleObj = css.addStyle("::-webkit-input-placeholder");
		styleObj.setProperty("color", "#aaaaaa");
		styleObj = css.addStyle(":-moz-placeholder");
		styleObj.setProperty("color", "#aaaaaa");

		// IMAGE MEDIA action buttons style
		styleObj = css.addStyle(".image-media-save");
		styleObj.setProperty("position", "absolute");
		styleObj.setProperty("top", "1px");
		styleObj.setProperty("left", "1px");
		styleObj.setProperty("width", "16px");
		styleObj.setProperty("height", "16px");
		styleObj.setProperty("cursor", "pointer");
		styleObj.setProperty("background-color", "gray");
		styleObj.setProperty("z-index", "1");
		styleObj = css.addStyle(".image-media-upload");
		styleObj.setProperty("position", "absolute");
		styleObj.setProperty("top", "1px");
		styleObj.setProperty("left", "17px");
		styleObj.setProperty("width", "16px");
		styleObj.setProperty("height", "16px");
		styleObj.setProperty("cursor", "pointer");
		styleObj.setProperty("background-color", "gray");
		styleObj.setProperty("z-index", "1");
		styleObj = css.addStyle(".image-media-remove");
		styleObj.setProperty("position", "absolute");
		styleObj.setProperty("top", "1px");
		styleObj.setProperty("left", "33px");
		styleObj.setProperty("width", "16px");
		styleObj.setProperty("height", "16px");
		styleObj.setProperty("cursor", "pointer");
		styleObj.setProperty("background-color", "gray");
		styleObj.setProperty("z-index", "1");

		//		div.wicket-modal div.w_undecorated div.w_caption
		return css.toString();
	}

//	private static void enhanceInputButton(TextualStyle styleObj)
//	{
//		styleObj.setProperty("background-image","url(/servoy-webclient/templates/lafs/kunststoff/images/button/secondary-enabled.gif)",false);
//		styleObj.setProperty("background-color","#DBE3EB",false);
//		styleObj.setProperty("background-repeat","repeat-x",false);
//		styleObj.setProperty("background-position","center center",false);
//		styleObj.setProperty("color","#000000",false);
////		styleObj.setProperty("border-style","solid",false);
//		styleObj.setProperty("border-width",createInsetsText(DEFAULT_BUTTON_BORDER_SIZE),false);
//		styleObj.setProperty("border-color","#7F888E #555E66 #0D161C #555E66",false);
//	}

	private static void createComponentHTML(IPersist meta, Form form, StringBuffer html, TextualCSS css, Color formPartBgColor, int startY, int endY,
		boolean enableAnchoring, IServiceProvider sp) throws RepositoryException
	{
		switch (meta.getTypeID())
		{
			case IRepository.FIELDS :
				createFieldHTML((Field)meta, form, html, css, startY, endY, enableAnchoring, sp);
				break;

			case IRepository.GRAPHICALCOMPONENTS :
				createGraphicalComponentHTML((GraphicalComponent)meta, form, html, css, startY, endY, enableAnchoring, sp);
				break;

			case IRepository.RECTSHAPES :
				createRectangleHTML((RectShape)meta, form, html, css, startY, endY, enableAnchoring, sp);
				break;

			case IRepository.SHAPES :
				createShapeHTML((Shape)meta, form, html, css, startY, endY, enableAnchoring, sp);
				break;

			case IRepository.PORTALS :
				createPortalHTML((Portal)meta, form, html, css, startY, endY, formPartBgColor, sp);
				break;

			case IRepository.PARTS :
//			createPartHTML((Part)meta,html,css);
				break;

			case IRepository.TABPANELS :
				createTabPanelHTML((TabPanel)meta, form, html, css, startY, endY, enableAnchoring, sp);
				break;

			case IRepository.TABS :
//				createTabHTML((Tab)meta, html, css);
				break;

			case IRepository.BEANS :
				createBeanHTML((Bean)meta, form, html, css, startY, endY, enableAnchoring, sp);
				break;

			default :
				Debug.error("ComponentFactory:unkown type " + meta.getTypeID() + ", uuid: " + meta.getUUID() + ", parent:" + meta.getParent());
				html.append("<label ");
				html.append(getWicketIDParameter(form, meta));
				html.append(">ComponentFactory:unkown type " + meta.getTypeID() + ", uuid: " + meta.getUUID() + ", parent:" + meta.getParent());
				html.append("</label>");
		}
	}

	/**
	 * If it was previously added at a wrapper level don't add it again .
	 * @param comp
	 */
	private static String getCssClassForElementHelper(AbstractBase comp, boolean[] alreadyAdded)
	{
		if (!(comp instanceof BaseComponent || comp instanceof Form)) return "";
		if ("true".equals(Settings.getInstance().getProperty("servoy.webclient.pushClassToHTMLElement", "false")) && alreadyAdded != null && !alreadyAdded[0])
		{
			if (alreadyAdded != null) alreadyAdded[0] = true;
			String className = comp instanceof BaseComponent ? ((BaseComponent)comp).getStyleClass() : ((Form)comp).getStyleClass();
			return (className != null ? className : "");
		}
		return "";
	}

	public static String getCssClassForElement(AbstractBase comp, boolean[] alreadyAdded, String extraClass)
	{
		String css = getCssClassForElementHelper(comp, alreadyAdded) + ' ' + extraClass;
		if (css.equals(" ")) return "";
		return " class='" + css + "' ";
	}

	public static String getCssClassForElement(AbstractBase comp, String extraClass)
	{
		return getCssClassForElement(comp, null, extraClass);
	}

	private static void createBeanHTML(Bean bean, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		boolean isComponent = false;
		try
		{
			Class< ? > beanClazz = ApplicationServerRegistry.get().getBeanManager().getClassLoader().loadClass(bean.getBeanClassName());
			isComponent = Component.class.isAssignableFrom(beanClazz);
		}
		catch (Throwable e)
		{
			Debug.error(e);
		}

		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(form, bean));
		if (enableAnchoring)
		{
			styleObj.setProperty("width", "100%");
			styleObj.setProperty("height", "100%");
			styleObj.setProperty("position", "absolute");
			TextualStyle wrapperStyleObj = css.addStyle('#' + ComponentFactory.getWebID(form, bean) + WRAPPER_SUFFIX);
			wrapperStyleObj.setProperty("overflow", "visible");
			applyLocationAndSize(bean, wrapperStyleObj, null, startY, endY, form.getSize().width, enableAnchoring, bean.getAnchors(), sp);

			html.append("<div ");
			html.append(getWicketIDParameter(form, bean, "", WRAPPER_SUFFIX));
			html.append(getJavaScriptIDParameter(form, bean, "", WRAPPER_SUFFIX));
			html.append(">");
		}
		else
		{
			applyLocationAndSize(bean, styleObj, null, startY, endY, form.getSize().width, enableAnchoring, bean.getAnchors(), sp);
		}

		if (isComponent)
		{
			html.append("<input ");
			html.append(getWicketIDParameter(form, bean));
			html.append(getJavaScriptIDParameter(form, bean));
			html.append("value='");
			html.append(bean.getName());
			html.append("' ");
			html.append("type='image' ");
			html.append("src='#' ");
			html.append("/>");
		}
		else
		{
			styleObj.setProperty("overflow", "auto");
			html.append("<div ");
			html.append(getWicketIDParameter(form, bean));
			html.append(getJavaScriptIDParameter(form, bean));
			html.append(">only wicket components are supported in webclient</div>");
		}

		if (enableAnchoring)
		{
			html.append("</div>");
		}
	}

//	private static void createTabHTML(Tab meta, StringBuffer html, TextualCSS css)
//	{
//	}

	private static void createTabPanelHTML(TabPanel tabPanel, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(form, tabPanel));
		BorderAndPadding borderAndPadding = applyBaseComponentProperties(tabPanel, form, styleObj, null, null, sp);
		applyLocationAndSize(tabPanel, styleObj, borderAndPadding, startY, endY, form.getSize().width, enableAnchoring, tabPanel.getAnchors(), sp);
		boolean isSplitPane = tabPanel.getTabOrientation() == TabPanel.SPLIT_HORIZONTAL || tabPanel.getTabOrientation() == TabPanel.SPLIT_VERTICAL;
		// do not apply foreground to the whole tab panel
		styleObj.remove("color");
//		html.append("<table cellpadding=0 cellspacing=0 ");
		html.append("<div ");
		html.append(getWicketIDParameter(form, tabPanel));
		String tabPanelCssClass = "tabpanel";
		if (!tabPanel.getTransparent()) tabPanelCssClass = "opaquecontainer " + tabPanelCssClass;
		boolean userCssClassAdded[] = new boolean[] { false };
		html.append(getCssClassForElement(tabPanel, userCssClassAdded, tabPanelCssClass));
		if (isSplitPane) html.append(" style='overflow: hidden;' ");
		html.append(">\n");
		boolean isTabbedTabPanel = false;

//		int yoffset = 0;
		if (tabPanel.getTabOrientation() != TabPanel.HIDE && tabPanel.getTabOrientation() != TabPanel.ACCORDION_PANEL && !isSplitPane &&
			!(tabPanel.getTabOrientation() == TabPanel.DEFAULT_ORIENTATION && tabPanel.hasOneTab()))
		{
//			yoffset = 20;
//			html.append("\t<thead><tr valign='bottom'><th height=20>\n");
			isTabbedTabPanel = true;
			html.append("\t<div tabholder='true' class='tabs'>\n");
			html.append("\t<servoy:remove>\n");
			html.append("\t\t<div class='selected_tab'><a href='tab1'>Tab1</a></div>\n");
			html.append("\t\t<div><a href='tab1'>Tab2</a></div>\n");
			html.append("\t</servoy:remove>\n");
			Iterator<IPersist> it = tabPanel.getAllObjects();
			if (it.hasNext())
			{
				Tab tab = (Tab)it.next();
				//				applyTextProperties(tab, styleObj);
				html.append("\t\t<div servoy:id='tablinks'");
				html.append(
					"><a servoy:id='tablink' href='tab1' class='tablink'><div servoy:id='icon'></div><span style=\"white-space: nowrap;\" servoy:id='linktext'>");
				html.append(getSafeText(tab.getText()));
				html.append("</span></a></div>\n");
			}
			else
			{
				html.append("\t\t<div servoy:id='tablinks'");
				html.append(
					"><a servoy:id='tablink' href='tab1' class='tablink'><div servoy:id='icon'></div><span style=\"white-space: nowrap;\" servoy:id='linktext'>");
				html.append("No tabs");
				html.append("</span></a></div>\n");
			}
			html.append("\t</div>\n");
//			html.append("\t</th></tr></thead>\n");
		}
//		html.append("\t<tbody><tr><td>\n");
//		Insets ins = borderAndPadding != null ? borderAndPadding.getSum() : new Insets(0, 0, 0, 0);
//		int t_width = tabPanel.getSize().width - (ins.left + ins.right + 2); //not sure why to add 2
//		int t_height = (tabPanel.getSize().height - yoffset) - (ins.top + ins.bottom + 4); //not sure why to add 4
		TextualStyle style = new TextualStyle();
		style.setProperty("overflow", "auto");
		style.setProperty("position", "relative");
//		style.setProperty("width",t_width+"px");
//		style.setProperty("height",t_height+"px");
		if (tabPanel.getBorderType() != null)
		{
			ComponentFactoryHelper.createBorderCSSProperties(tabPanel.getBorderType(), styleObj);
		}
		if (tabPanel.getTabOrientation() == TabPanel.ACCORDION_PANEL)
		{
			for (Attribute att : ServoyStyleSheet.fontAttributes)
			{
				styleObj.remove(att.toString());
			}
			int alignment = ISupportTextSetup.LEFT;
			String styleAlignment = null;
			if (tabPanel.getHorizontalAlignment() >= 0)
			{
				alignment = tabPanel.getHorizontalAlignment();
			}
			else if (styleObj.containsKey(CSS.Attribute.TEXT_ALIGN.toString()))
			{
				styleAlignment = (String)styleObj.get(CSS.Attribute.TEXT_ALIGN.toString());
			}
			if (styleAlignment == null)
			{
				styleAlignment = getHorizontalAlignValue(alignment);
			}
			styleObj.remove(CSS.Attribute.TEXT_ALIGN.toString());
			String tabPanelMarkupId = ComponentFactory.getWebID(form, tabPanel);
			Iterator<IPersist> it = tabPanel.getAllObjects();
			String text = "";
			if (it.hasNext())
			{
				Tab tab = (Tab)it.next();
				text = tab.getText();
			}
			html.append("\t\t<div id='accordion_").append(tabPanelMarkupId).append("'servoy:id='accordion_").append(tabPanelMarkupId).append(
				"'><div servoy:id='tabs'");
			html.append("><h3 style='text-align:");
			html.append(styleAlignment);
			html.append(
				";' onclick='this.getElementsByTagName(\"a\")[0].click()'><a servoy:id='tablink' href='#'><div servoy:id='icon'></div><span style=\"white-space: nowrap;\" servoy:id='linktext'>");
			html.append(getSafeText(text));
			html.append("</span></a></h3><div servoy:id='webform' ");
			//html.append(getCSSClassParameter("webform"));
			html.append("></div></div></div>\n");
		}
		else if (isSplitPane)
		{
			String tabPanelMarkupId = ComponentFactory.getWebID(form, tabPanel);

			StringBuffer leftPanelStyle = new StringBuffer("position: absolute; ");
			StringBuffer rightPanelStyle = new StringBuffer("position: absolute; ");
			if (tabPanel.getTabOrientation() == TabPanel.SPLIT_HORIZONTAL)
			{
				leftPanelStyle.append("top: 0px; left: 0px; bottom: 0px;");
				rightPanelStyle.append("top: 0px; bottom: 0px; right: 0px");
			}
			else
			{
				leftPanelStyle.append("top: 0px; left: 0px; right: 0px;");
				rightPanelStyle.append("left: 0px; bottom: 0px; right: 0px;");
			}

			html.append("\t<div id='splitter_").append(tabPanelMarkupId).append("' servoy:id='splitter' style='").append(leftPanelStyle).append( //$NON-NLS-1$  //$NON-NLS-2$
				"'><div id='websplit_left_").append(tabPanelMarkupId).append("' servoy:id='websplit_left' style='").append(leftPanelStyle).append("' ").append( //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
					"><div servoy:id='webform'").append(getCSSClassParameter("webform")).append("></div></div></div>"); //$NON-NLS-1$
			html.append("<div id='websplit_right_").append(tabPanelMarkupId).append("' servoy:id='websplit_right' style='").append(rightPanelStyle).append( //$NON-NLS-1$//$NON-NLS-2$
				"' ").append( //$NON-NLS-1$
					"><div servoy:id='webform'").append(getCSSClassParameter("webform")).append("></div></div>"); //$NON-NLS-1$
		}
		else
		{
			html.append("\t<div servoy:id='webform' ").append(style.toString());
			String cssClass = "webform";
			boolean tabPanelHasBorder = tabPanel.getBorderType() != null || styleObj.containsKey(CSS.Attribute.BORDER_STYLE.toString());
			if (isTabbedTabPanel && !tabPanelHasBorder)
			{
				// apply default border
				cssClass = "tabcontainer " + cssClass;
			}
			html.append(getCSSClassParameter(cssClass));
			html.append("></div>");

		}

		html.append("</div>");


//		html.append("</td></tr></tbody></table>");
	}

	private static void createPortalHTML(Portal meta, Form form, StringBuffer html, TextualCSS css, int startY, int endY, Color formPartBgColor,
		IServiceProvider sp) throws RepositoryException
	{
		Color portalBgColor = meta.getBackground();
		if (portalBgColor == null) portalBgColor = formPartBgColor;
		createCellBasedView(meta, form, html, css, !meta.getMultiLine(), meta.getLocation().y, meta.getLocation().y + meta.getSize().height, portalBgColor, sp,
			-1, false);
	}

	private static void createShapeHTML(Shape shape, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(form, shape));

		BorderAndPadding ins = applyBaseComponentProperties(shape, form, styleObj, null, null, sp);
		html.append("<span ");
		html.append(getCssClassForElement(shape, "field"));
		html.append(getWicketIDParameter(form, shape));
		html.append(getJavaScriptIDParameter(form, shape));
		//html.append(getCSSClassParameter((BaseComponent)shape,"field",ComponentFactory.getWebID(shape)));
		html.append("></span>");

		applyLocationAndSize(shape, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring, shape.getAnchors(), sp);
	}

	private static void createRectangleHTML(RectShape rectshape, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(form, rectshape));
		BorderAndPadding ins = applyBaseComponentProperties(rectshape, form, styleObj, null, null, sp);

		html.append("<span ");
		html.append(getWicketIDParameter(form, rectshape));
		html.append(getJavaScriptIDParameter(form, rectshape));
		//html.append(getCSSClassParameter((BaseComponent)rectshape,"field",ComponentFactory.getWebID(rectshape)));
		html.append("></span>");

		if (rectshape.getLineSize() > 0)
		{
			boolean titledBorder = rectshape.getBorderType() != null && ComponentFactoryHelper.createBorder(rectshape.getBorderType()) instanceof TitledBorder;
			styleObj.setProperty("border-style", "solid");
			if (rectshape.getBorderType() != null)
			{
				Properties properties = new Properties();
				ComponentFactoryHelper.createBorderCSSProperties(rectshape.getBorderType(), properties);
				styleObj.setProperty("border-style", properties.getProperty("border-style"));
			}
			if (!titledBorder)
			{
				styleObj.setProperty("border-width", rectshape.getLineSize() + "px");
				if (ins.border == null)
				{
					ins.border = new Insets(rectshape.getLineSize(), rectshape.getLineSize(), rectshape.getLineSize(), rectshape.getLineSize());
				}
				else
				{
					ins.border.top += rectshape.getLineSize();
					ins.border.right += rectshape.getLineSize();
					ins.border.bottom += rectshape.getLineSize();
					ins.border.left += rectshape.getLineSize();
				}
			}
			if (rectshape.getForeground() != null)
			{
				styleObj.setProperty("border-color", PersistHelper.createColorString(rectshape.getForeground()));
			}
			if (rectshape.getRoundedRadius() > 0)
			{
				styleObj.setProperty("border-radius", rectshape.getRoundedRadius() / 2 + "px");
			}
			else if (rectshape.getShapeType() == RectShape.OVAL)
			{
				styleObj.setProperty("border-radius", rectshape.getSize().width / 2 + "px");
			}
		}
		applyLocationAndSize(rectshape, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring, rectshape.getAnchors(), sp);
	}

	private static void createGraphicalComponentHTML(GraphicalComponent label, Form form, StringBuffer html, TextualCSS css, int startY, int endY,
		boolean enableAnchoring, IServiceProvider sp)
	{
		String styleName = "#";
		Insets border = null;
		if (ComponentFactory.isButton(label) && !hasHTMLText(label.getText()))
		{
//			styleName = "input.";
			border = DEFAULT_BUTTON_BORDER_SIZE;
		}

		TextualStyle styleObj = css.addStyle(styleName + ComponentFactory.getWebID(form, label));
		BorderAndPadding ins = applyBaseComponentProperties(label, form, styleObj, (Insets)DEFAULT_LABEL_PADDING.clone(), border, sp);
		// fix the background img, see ComponentFactory.createGraphicalComponent
		// background image through css will only be used when repeat or position are set (or linear gradient is used).
		// if both are not specified then it is used as the MEDIA of the label/button, so bck_img is removed from the css.
		boolean keepBgImageStyle = false;
		for (String attribute : ServoyStyleSheet.BACKGROUND_IMAGE_CSS)
		{
			if (attribute.equals(CSS.Attribute.BACKGROUND_IMAGE.toString()))
			{
				if (styleObj.getProperty(attribute) == null)
				{
					keepBgImageStyle = false;
					break;
				}
				else if (!styleObj.getProperty(attribute).contains(MediaURLStreamHandler.MEDIA_URL_DEF))
				{
					keepBgImageStyle = true;
					break;
				}
			}
			else if (styleObj.getProperty(attribute) != null)
			{
				keepBgImageStyle = true;
				break;
			}
		}
		if (!keepBgImageStyle)
		{
			styleObj.remove(CSS.Attribute.BACKGROUND_IMAGE.toString());
		}
		applyTextProperties(label, styleObj);

		Field labelForField = null;
		String labelFor = label.getLabelFor();
		if (labelFor != null)
		{
			Iterator<IPersist> fields = form.getObjects(IRepository.FIELDS);
			while (fields.hasNext())
			{
				Field fld = (Field)fields.next();
				if (labelFor.equals(fld.getName()))
				{
					labelForField = fld;
					break;
				}
			}
		}

		int labelHAlign = label.getHorizontalAlignment();
		int labelVAlign = label.getVerticalAlignment();
		Pair<IStyleSheet, IStyleRule> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, label, form);
		if (styleInfo != null)
		{
			IStyleSheet ss = styleInfo.getLeft();
			IStyleRule s = styleInfo.getRight();
			if (labelHAlign == -1) labelHAlign = ss.getHAlign(s);
			if (labelVAlign == -1) labelVAlign = ss.getVAlign(s);
		}
		// Defaults are mapped to CENTER (or MIDDLE for vertical) to keep behavior from Smart client.
		if (labelHAlign == -1) labelHAlign = ISupportTextSetup.CENTER;
		if (labelVAlign == -1) labelVAlign = ISupportTextSetup.CENTER;

		boolean isButton = ComponentFactory.isButton(label);
		boolean userCssClassAdded[] = new boolean[] { false };
		TextualStyle wrapperStyle = null;
		Properties minSizeStyle = styleObj;
		if (isButton && enableAnchoring)
		{
			// Anchoring <button> with { left: 0px; right: 0px; } pair
			// or { top: 0px; bottom: 0px; } does not work. Thus we add a dummy wrapper <div>
			// which accepts this kind of anchoring, and we place the button inside the <div>
			// with { width: 100%; height: 100%; }, which works fine.
			String wrapperId = ComponentFactory.getWebID(form, label) + WRAPPER_SUFFIX;
			wrapperStyle = css.addStyle(styleName + wrapperId);
			minSizeStyle = wrapperStyle;
			html.append("<div ");
			html.append(getCssClassForElement(label, userCssClassAdded, ""));
			html.append(getWicketIDParameter(form, label, "", WRAPPER_SUFFIX));
			html.append(getJavaScriptIDParameter(form, label, "", WRAPPER_SUFFIX));
			html.append(">");
		}

		Insets buttonBorder = null;
		if (isButton)
		{
			html.append("<button type='submit' ");
			html.append(" style=\"white-space: nowrap;\" ");
			html.append(getWicketIDParameter(form, label));
			html.append(getDataProviderIDParameter(label));
			html.append(getCssClassForElement(label, userCssClassAdded, "button"));
			html.append(">");
			html.append("</button>");
			// buttons are border-box by default!!
			buttonBorder = ins.getBorder();
			ins = null;
		}
		else
		{
			if (labelForField != null)
			{
				html.append("<label ");
				// Needed for FF to accept a <div> inside a <label>.
				styleObj.setProperty("display", "block");
			}
			else
			{
				html.append("<div ");
			}
			// we want to wrap only if there is no html content in the label text
			if (label.getText() != null && !HtmlUtils.hasUsefulHtmlContent(label.getText()))
			{
				html.append(" style=\"white-space: nowrap;\"");
			}
			html.append(getWicketIDParameter(form, label));
			html.append(getDataProviderIDParameter(label));
			html.append(getCssClassForElement(label, userCssClassAdded, "label"));
			html.append('>');
			boolean hasHtml = hasHTMLText(label.getText());
			if (hasHtml && (label.getOnActionMethodID() != 0))
			{
				html.append("<servoy:remove>");
			}
			html.append(getSafeText(label.getText()));
			if (hasHtml && (label.getOnActionMethodID() != 0))
			{
				html.append("</servoy:remove>");
			}
			if (labelForField != null)
			{
				html.append("</label>");
			}
			else
			{
				html.append("</div>");
			}
			if (label.getOnActionMethodID() > 0) styleObj.setProperty("cursor", "pointer");
		}

		Insets borderAndPadding = ins == null ? new Insets(0, 0, 0, 0) : ins.getSum();

		WebAnchoringHelper.addMinSize(label.getAnchors(), sp, minSizeStyle,
			new Dimension(label.getSize().width - borderAndPadding.left - borderAndPadding.right,
				label.getSize().height - borderAndPadding.top - borderAndPadding.bottom),
			label);

		if (isButton && enableAnchoring)
		{
			html.append("</div>");
			styleObj.setProperty("width", "100%");
			styleObj.setProperty("height", "100%");
			styleObj.setProperty("position", "absolute");
			applyLocationAndSize(label, wrapperStyle, ins, startY, endY, form.getSize().width, enableAnchoring,
				isListViewBodyElement(form, label.getLocation()) ? new Point(-3, 0) : null);
		}
		else
		{
			applyLocationAndSize(label, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring,
				isListViewBodyElement(form, label.getLocation()) ? new Point(-3, 0) : null);
		}
		if (label.getRolloverCursor() == Cursor.HAND_CURSOR)
		{
			styleObj.setProperty("cursor", "pointer");
		}
		int height = label.getSize().height;
		// Firefox has a problem when rendering <button> tags. A solution is to tweak the bottom padding
		// and make it equal to the height of the <button>.
		// See: http://doctype.com/html-button-tag-renders-strangely-firefox
		if (isButton)
		{
			int bottomPadding = 0;
			if (labelVAlign != ISupportTextSetup.CENTER)
			{
				bottomPadding = height;
				if (buttonBorder != null) bottomPadding -= buttonBorder.top + buttonBorder.bottom;
			}
			styleObj.setProperty("padding-bottom", bottomPadding + "px");
		}
	}

	public static boolean isFilledText(String text)
	{
		if (text == null) return false;
		if (text.trim().length() == 0) return false;
		return true;
	}

	public static boolean hasHTMLText(String text)
	{
		return HtmlUtils.startsWithHtml(text);
	}

	public static String getSafeText(String text)
	{
		if (text == null) return "";

		String parsedTxt = text;
		if (hasHTMLText(parsedTxt))
		{
			//strip out any html page related tags
			parsedTxt = Utils.stringReplace(parsedTxt, "<html>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "</html>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "<body>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "</body>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "<form>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "</form>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "<head>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "</head>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "<title>", "", -1, false, true);
			parsedTxt = Utils.stringReplace(parsedTxt, "</title>", "", -1, false, true);

//TODO:handle servoy method calls
//			int index == -1
//			while ((index = parsedTxt.indexOf("servoymethod:")) != -1)
//			{
//				int openIndex = parsedTxt.indexOf("(",index);
//
//			}
//			parsedTxt = Utils.stringReplace(parsedTxt, "servoymethod:", "javascript:servoymethod('", -1, false, true);
//			getElementById(\'servoy_eval\').value='servoy_method_eval
		}
		else
		{
			parsedTxt = Utils.stringReplaceRecursive(parsedTxt, ">", "&gt;");
			parsedTxt = Utils.stringReplaceRecursive(parsedTxt, "<", "&lt;");
		}

		parsedTxt = Utils.stringReplaceRecursive(parsedTxt, "  ", " ");
		parsedTxt = Utils.stringReplaceRecursive(parsedTxt, "\n\n", "\n");

		return parsedTxt.trim();
	}

	private static void createFieldHTML(Field field, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		boolean addWrapperDiv = enableAnchoring && WebAnchoringHelper.needsWrapperDivForAnchoring(field);

		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(form, field));
		boolean userCssClassAdded[] = new boolean[] { false };
		Properties minSizeStyle = styleObj;
		if (addWrapperDiv)
		{
			// Anchoring fields (<input>s, <textarea>s) with { left: 0px; right: 0px; } pair
			// or { top: 0px; bottom: 0px; } does not work. Thus we add a dummy wrapper <div>
			// which accepts this kind of anchoring, and we place the field inside the <div>
			// with { width: 100%; height: 100%; }, which works fine.
			String wrapperId = ComponentFactory.getWebID(form, field) + WRAPPER_SUFFIX;
			TextualStyle wrapperStyle = css.addStyle('#' + wrapperId);
			wrapperStyle.setProperty("overflow", "visible");
			minSizeStyle = wrapperStyle;
			html.append("<div ");
			html.append(getWicketIDParameter(form, field, "", WRAPPER_SUFFIX));
			html.append(getJavaScriptIDParameter(form, field, "", WRAPPER_SUFFIX));
			if (field.getDisplayType() == Field.COMBOBOX)
			{
				html.append(getCssClassForElement(field, userCssClassAdded, COMBOBOX_CLASS));
			}
			html.append(getCssClassForElement(field, userCssClassAdded, ""));
			html.append(">");
		}

		Insets padding = (Insets)DEFAULT_FIELD_PADDING.clone();
		Insets border = (Insets)DEFAULT_FIELD_BORDER_SIZE.clone();

		if (field.getDisplayType() == Field.COMBOBOX || field.getDisplayType() == Field.LIST_BOX || field.getDisplayType() == Field.MULTISELECT_LISTBOX)
		{
			padding = DEFAULT_LABEL_PADDING;
		}
		BorderAndPadding ins = applyBaseComponentProperties(field, form, styleObj, padding, border, sp);


		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getCSSPairStyleForForm(sp, form);
		IStyleSheet ss = pairStyle != null ? pairStyle.getLeft() : null;

		String cssClass = ""; // By default no css class applied.
		switch (field.getDisplayType())
		{
			case Field.PASSWORD :
			{
				if (ins == null)
				{
					ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE, DEFAULT_FIELD_PADDING);
				}
				html.append("<input ");
				html.append(getWicketIDParameter(form, field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCssClassForElement(field, userCssClassAdded, "field"));
				html.append("type='password' ");
				if (field.getSelectOnEnter())
				{
					html.append("onfocus='Servoy.Utils.doSelect(this)'"); //$NON-NLS-1$
				}
				html.append("/>");
			}
				break;
			case Field.RTF_AREA :
			{
				applyScrolling(styleObj, field);
				html.append("<div ");
				html.append(getCssClassForElement(field, userCssClassAdded, "field"));
				html.append(getWicketIDParameter(form, field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(">RTF field not supported in webclient</div>");
			}
				break;
			case Field.HTML_AREA :
				if (!field.getEditable())
				{
					applyScrolling(styleObj, field);
					html.append("<div ");
					html.append(getWicketIDParameter(form, field));
					//html.append(getJavaScriptIDParameter(field));
					html.append(getCssClassForElement(field, userCssClassAdded, "field"));
					html.append(">non editable HTML field</div>");

					boolean hasFontFamily = styleObj.containsKey("font-family");
					boolean hasFontSize = styleObj.containsKey("font-size");
					if (hasFontFamily || hasFontSize)
					{
						for (String dfe : DEFAULT_FONT_ELEMENTS)
						{
							TextualStyle htmlAreaFont = css.addStyle('#' + ComponentFactory.getWebID(form, field) + " " + dfe);
							if (hasFontFamily) htmlAreaFont.setProperty("font-family", styleObj.getProperty("font-family"));
							if (hasFontSize) htmlAreaFont.setProperty("font-size", styleObj.getProperty("font-size"));
						}
					}
					break;
				}
				else
				{
					String editorId = "editor_" + ComponentFactory.getWebID(form, field);
					html.append("<div ");
					html.append(getWicketIDParameter(form, field));
					html.append("><textarea id='");
					html.append(editorId);
					html.append("' name='");
					html.append(editorId);
					html.append("' ");
					html.append(getWicketIDParameter(form, field, "editor_", ""));
					html.append(" rows=\"20\" cols=\"75\"></textarea></div>");
					styleObj.setProperty("padding", "0px");
					styleObj.setProperty("overflow", "hidden");
				}
				break;
			case Field.TEXT_AREA :
			{
				if (ins == null)
				{
					ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE, DEFAULT_FIELD_PADDING);
				}
				applyScrolling(styleObj, field);
				html.append("<textarea ");
				html.append(getWicketIDParameter(form, field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCssClassForElement(field, userCssClassAdded, "field"));
				html.append("></textarea>");
			}
				break;
			case Field.CHECKS :
			case Field.RADIOS :
				boolean isRadio = (field.getDisplayType() == Field.RADIOS);
				String selector = (isRadio ? "radio" : "check");
				cssClass = (isRadio ? "radio" : "check");
				IValueList val = null;
				ValueList valuelist = null;
				if (field.getValuelistID() > 0 && sp != null)
				{
					valuelist = sp.getFlattenedSolution().getValueList(field.getValuelistID());
				}
				boolean addSingle = ComponentFactory.isSingleValue(valuelist);

				// If we have multiple checkboxes, then the default is "field".
				if (field.getValuelistID() > 0 && !addSingle && !isRadio) cssClass = "radioCheckField";
				// If we have a style for the form, apply "check" class if present, default to "field" if "check" class is not present.
				if (ss != null)
				{
					cssClass = "radioCheckField";
					String lookUpValue = selector;
					IStyleRule s = ss.getCSSRule(lookUpValue);
					if (s.getAttributeCount() == 0)
					{
						if ((field.getStyleClass() != null) && (field.getStyleClass().trim().length() > 0))
						{
							lookUpValue += '.' + field.getStyleClass().trim();
							s = ss.getCSSRule(lookUpValue);
							if (s.getAttributeCount() > 0) cssClass = selector;
						}
					}
					else
					{
						cssClass = selector;
					}
				}

				if ((field.getValuelistID() > 0 || isRadio) && !addSingle)
				{
					applyScrolling(styleObj, field);
					html.append("<div ");
					html.append(getWicketIDParameter(form, field));
					//html.append(getJavaScriptIDParameter(field));
					html.append(getDataProviderIDParameter(field));
					html.append(getCssClassForElement(field, userCssClassAdded, cssClass));
					html.append(">Multi checkboxes</div>");
				}
				else
				{
					html.append("<div ");
					html.append(getCssClassForElement(field, userCssClassAdded, cssClass));
					html.append(getWicketIDParameter(form, field));
					html.append(" tabIndex=\"-1\" ");
					html.append(">"); //
					html.append(
						"<table cellspacing='0' cellpadding='0' border='0' width='100%' height='100%'><tr><td id='check_td' style='vertical-align: middle;'><input onmousedown='radioCheckInputMouseDown=true' onmouseup='radioCheckInputMousedDown=false' onmouseout='radioCheckInputMouseDown=false' class='radioCheckInput' style='border-width: 0px; padding: " +
							(isRadio ? "0px" : "3px") + "; margin: 0px; vertical-align: middle;' "); //
					html.append(getWicketIDParameter(form, field, "check_", ""));
					html.append(getDataProviderIDParameter(field));
					if (isRadio)
					{
						html.append("type='radio' ");
					}
					else
					{
						html.append("type='checkbox' ");
					}
					html.append("/>");
					html.append("<label style='border-width: 0px; padding-top: " + (isRadio ? "0px" : "2px") +
						"; padding-left: 3px; margin: 0px; vertical-align: middle;");
					html.append("' ");
					html.append(getWicketIDParameter(form, field, "text_", ""));
					html.append(">");
					html.append("</label>");
					html.append("</td></tr></table></div>");
				}
				break;
			case Field.COMBOBOX :
			{
				ins = null;
//					if (ins == null)
//					{
//						ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE,DEFAULT_FIELD_PADDING);
//					}
				html.append("<select ");
				html.append(getWicketIDParameter(form, field));
//					html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCssClassForElement(field, userCssClassAdded, "field"));
				html.append(">Combobox</select>");
			}
				break;
			case Field.MULTISELECT_LISTBOX :
			case Field.LIST_BOX :
			{
				ins = null;
				html.append("<select ");
				if (field.getDisplayType() == Field.MULTISELECT_LISTBOX)
				{
					html.append("multiple=\"multiple\"");
				}
				html.append(getWicketIDParameter(form, field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCssClassForElement(field, userCssClassAdded, "listbox"));
				html.append(">Listbox</select>");
			}
				break;
			case Field.CALENDAR :
			case Field.SPINNER :
				createCompositeFieldHTML(html, form, field, styleObj, userCssClassAdded);
				break;
			case Field.IMAGE_MEDIA :
			{
				applyScrolling(styleObj, field);
				// in tableview position is not set
				styleObj.setProperty("position", "absolute");
				html.append("<div ");
				html.append(getWicketIDParameter(form, field));
				//				html.append(getJavaScriptIDParameter(field));
				html.append(getCssClassForElement(field, userCssClassAdded, "field"));
				html.append('>');

				TextualStyle inline2 = new TextualStyle();
				//inline2.setProperty("top", "1px");
				//inline2.setProperty("left", "1px");
				//inline2.setProperty("position", "absolute");
				//inline2.setProperty("cursor", "pointer");
				//inline2.setProperty("background-color", "gray");
				inline2.setProperty("z-index", "1");
				html.append("<img ");
				html.append(inline2.toString());
				html.append(" border=0 servoy:id='save_icon' src='#' class='image-media-save' alt='Save' />");
				html.append("<img ");
				//	inline2.setProperty("left", "17px");
				html.append(inline2.toString());
				html.append(" border=0 servoy:id='upload_icon' src='#' class='image-media-upload' alt='Upload' />");
				html.append("<img ");
				//inline2.setProperty("left", "33px");
				html.append(inline2.toString());
				html.append(" border=0 servoy:id='remove_icon' src='#' class='image-media-remove' alt='Remove' />");
//					html.append("<a ");
//					html.append(inline2.toString());
//					html.append(" servoy:id='upload' href='#' border=0><img servoy:id='upload_icon' src='#' alt='' /></a>");
//					html.append("<a ");
//					inline2.setProperty("left","16px");
//					html.append(inline2.toString());
//					html.append(" servoy:id='save' href='#' border=0><img servoy:id='save_icon' src='#' alt='' /></a>");

				TextualStyle inline = new TextualStyle();
				inline.setProperty("top", "0px");
				inline.setProperty("left", "0px");
				inline.setProperty("position", "absolute");
				if (field.getOnActionMethodID() < 1)
				{
					inline.setProperty("cursor", "default");
				}

				html.append("<input ");
				html.append(inline.toString());
				html.append(getWicketIDParameter(form, field));
				html.append(getJavaScriptIDParameter(form, field));
				html.append("value='");
				if (field.getName() != null) html.append(field.getName());
				html.append("' ");
				html.append("type='image' ");
				html.append(" src='#' alt='' ");
				html.append(" onclick='return false;' ");
				html.append("/>");

//					html.append("<img ");
//					html.append(inline.toString());
//					html.append(" src='#' alt='' ");
//					html.append(getWicketIDParameter(field));
//					html.append(" />");

				html.append("</div>");
			}
				break;

			default :
			case Field.TYPE_AHEAD :
			case Field.TEXT_FIELD :
			{
				if (ins == null)
				{
					ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE, DEFAULT_FIELD_PADDING);
				}
				html.append("<input ");
				html.append(getWicketIDParameter(form, field));
//					html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCssClassForElement(field, userCssClassAdded, field.getValuelistID() > 0 ? "field typeahead" : "field"));
				html.append("type='text' ");
				if (field.getSelectOnEnter())
				{
					html.append("onfocus='Servoy.Utils.doSelect(this)'"); //$NON-NLS-1$
				}
				html.append("/>");
			}
				break;
		}
		if (field.getHorizontalAlignment() != -1)
		{
			if (isCompositeTextField(field.getDisplayType())) // all who's actual implementation is based on WebDataCompositeTextField
			{
				TextualStyle childTextCSS = css.addStyle('#' + ComponentFactory.getWebID(form, field) + WebDataCompositeTextField.AUGMENTED_FIELD_ID);
				applyTextProperties(field, childTextCSS);
			}
			else
			{
				applyTextProperties(field, styleObj);
			}
		}
		Insets borderAndPadding = ins == null ? new Insets(0, 0, 0, 0) : ins.getSum();

		WebAnchoringHelper.addMinSize(field.getAnchors(), sp, minSizeStyle,
			new Dimension(field.getSize().width - borderAndPadding.left - borderAndPadding.right,
				field.getSize().height - borderAndPadding.top - borderAndPadding.bottom),
			field);

		if (addWrapperDiv)
		{
			html.append("</div>");
			styleObj.setProperty("width", "100%");
			styleObj.setProperty("height", "100%");
			styleObj.setProperty("position", "absolute");
		}
		else
		{
			applyLocationAndSize(field, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring,
				isListViewBodyElement(form, field.getLocation()) ? new Point(-3, 0) : null);
		}
	}

	private static boolean isCompositeTextField(int type)
	{
		return type == Field.SPINNER || type == Field.CALENDAR;
	}

	private static boolean isListViewBodyElement(Form form, Point location)
	{
		if (form.getView() == IForm.LIST_VIEW || form.getView() == FormController.LOCKED_LIST_VIEW)
		{
			int startY = 0;
			Iterator<Part> parts = form.getParts();
			while (parts.hasNext())
			{
				Part part = parts.next();
				int endY = part.getHeight();
				if (part.getPartType() == Part.BODY)
				{
					if (location.getY() >= startY && location.getY() <= endY)
					{
						return true;
					}
					else
					{
						return false;
					}
				}
				startY = part.getHeight();
			}
			return true;
		}
		return false;
	}

	/**
	 * @param styleObj
	 * @param field
	 * @param form
	 * @param html
	 *
	 */
	private static void createCompositeFieldHTML(StringBuffer html, Form form, Field field, TextualStyle styleObj, boolean[] userCssClassAdded)
	{
		html.append("<div ");
		html.append(getWicketIDParameter(form, field));
		html.append(getDataProviderIDParameter(field));
		html.append(getCssClassForElement(field, userCssClassAdded,
			field.getDisplayType() == Field.SPINNER ? "spinner" : (field.getDisplayType() == Field.CALENDAR ? "field calendar" : "field")));
		html.append("style = 'overflow:hidden' ");
		html.append("><table ");
		TextualStyle inline = new TextualStyle();
		inline.setProperty("height", "100%");
		inline.setProperty("width", "100%");
		inline.setProperty("margin", "0px");
		inline.setProperty("padding", "0px");
		inline.setProperty("border-collapse", "collapse");
		inline.setProperty("table-layout", "fixed");
		html.append(inline.toString());
		html.append("><tr ");
		inline.remove("border-collapse");
		inline.remove("table-layout");
		html.append(inline.toString());
		html.append("><td ");
		html.append(inline.toString());
		html.append("><input type='text' ");
		html.append(getWicketIDParameter(form, field, "", WebDataCompositeTextField.AUGMENTED_FIELD_ID));
		inline = new TextualStyle();
		inline.setProperty("border-style", "none");
		inline.setProperty("background-color", IStyleSheet.COLOR_TRANSPARENT);
		inline.setProperty("height", "100%");
		inline.setProperty("width", "100%");
		inline.setProperty("margin", "0px");
		inline.setProperty("padding", "0px");
		inline.copy("font-family", styleObj);
		inline.copy("font-size", styleObj);
		inline.copy("color", styleObj);
		html.append(inline.toString());
		html.append(" /></td></tr></table></div>");
	}

	private static void applyScrolling(TextualStyle styleObj, ISupportScrollbars field)
	{
		String overflowX = AbstractFormLayoutProvider.getCSSScrolling(field.getScrollbars(), true);
		String overflowY = AbstractFormLayoutProvider.getCSSScrolling(field.getScrollbars(), false);
		if (Utils.equalObjects(overflowX, overflowY))
		{
			styleObj.setProperty("overflow", overflowX);
		}
		else
		{
			styleObj.setProperty("overflow-x", overflowX);
			styleObj.setProperty("overflow-y", overflowY);
		}
	}

	public static Insets sumInsets(Insets a, Insets b)
	{
		if (a == null && b != null) return (Insets)b.clone();
		if (a != null && b == null) return (Insets)a.clone();
		if (a == null && b == null) return null;

		return new Insets(a.top + b.top, a.left + b.left, a.bottom + b.bottom, a.right + b.right);
	}

	//returns space as last char
	private static String getDataProviderIDParameter(ISupportDataProviderID meta)
	{
		if (meta.getDataProviderID() != null)
		{
			return "name='" + meta.getDataProviderID() + "' ";
		}
		return "";
	}

	//returns space as last char
	private static String getCSSClassParameter(String servoy_css_type)
	{
		return "class='" + servoy_css_type + "' ";

	}

	private static void applyLocationAndSize(ISupportBounds component, TextualStyle styleObj, BorderAndPadding ins, int startY, int endY, int formWidth,
		boolean enableAnchoring, int anchors, IServiceProvider sp)
	{
		TextualCSS css = styleObj.getTextualCSS();
		ICSSBoundsHandler handler = css.getCSSBoundsHandler();
		Insets borderAndPadding = ins == null ? new Insets(0, 0, 0, 0) : ins.getSum();
		handler.applyBounds(component, styleObj, borderAndPadding, startY, endY, formWidth, enableAnchoring, null);
		if (component.getSize() != null)
		{
			WebAnchoringHelper.addMinSize(anchors, sp, styleObj,
				new Dimension(component.getSize().width - borderAndPadding.left - borderAndPadding.right,
					component.getSize().height - borderAndPadding.top - borderAndPadding.bottom),
				component instanceof IFormElement ? (IFormElement)component : null);
		}
	}

	private static void applyLocationAndSize(ISupportBounds component, TextualStyle styleObj, BorderAndPadding ins, int startY, int endY, int formWidth,
		boolean enableAnchoring, Point locationModifier)
	{
		TextualCSS css = styleObj.getTextualCSS();
		ICSSBoundsHandler handler = css.getCSSBoundsHandler();
		handler.applyBounds(component, styleObj, ins == null ? new Insets(0, 0, 0, 0) : ins.getSum(), startY, endY, formWidth, enableAnchoring,
			locationModifier);
	}

	interface ICSSBoundsHandler
	{
		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring, Point locationModifier);
	}

	static class BorderAndPadding
	{
		Insets border;
		Insets padding;

		/**
		 * @param insetsBorder
		 * @param insetsMargin
		 */
		public BorderAndPadding(Insets insetsBorder, Insets insetsMargin)
		{
			border = (Insets)(insetsBorder == null ? null : insetsBorder.clone());
			padding = (Insets)(insetsMargin == null ? null : insetsMargin.clone());
		}

		Insets getSum()
		{
			Insets ret = sumInsets(padding, border);
			return ret == null ? new Insets(0, 0, 0, 0) : ret;
		}

		Insets getPadding()
		{
			return padding == null ? new Insets(0, 0, 0, 0) : padding;
		}

		Insets getBorder()
		{
			return border == null ? new Insets(0, 0, 0, 0) : border;
		}

		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append("[border=");
			if (border == null) sb.append("null");
			else sb.append(border.toString());
			sb.append(",padding=");
			if (padding == null) sb.append("null");
			else sb.append(padding.toString());
			sb.append("]");
			return sb.toString();
		}
	}
	static class DefaultCSSBoundsHandler implements ICSSBoundsHandler
	{
		public static final DefaultCSSBoundsHandler INSTANCE = new DefaultCSSBoundsHandler();

		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring, Point locationModifier)
		{
			int y = component.getLocation().y;
//			if (ins != null) y += ins.top;
			int x = component.getLocation().x;
//			if (ins != null) x += ins.left;
			if (locationModifier != null)
			{
				y = Math.max(y + locationModifier.y, 0);
				x = Math.max(x + locationModifier.x, 0);
			}
			int w = component.getSize().width;
			if (ins != null) w -= (ins.left + ins.right);
			int h = component.getSize().height;
			if (ins != null) h -= (ins.top + ins.bottom);

			int anchorFlags = 0;
			if (component instanceof ISupportAnchors) anchorFlags = ((ISupportAnchors)component).getAnchors();

			createBounds(styleObj, y, x, w, h, component.getSize().width, component.getSize().height, anchorFlags, partStartY, partEndY, partWidth,
				enableAnchoring);
		}

		protected void createBounds(TextualStyle styleObj, int top, int left, int width, int height, int offsetWidth, int offsetHeight, int anchorFlags,
			int partStartY, int partEndY, int partWidth, boolean enableAnchoring)
		{
			if (top != -1) styleObj.setProperty("top", top + "px");
			if (left != -1) styleObj.setProperty("left", left + "px");
			if (width != -1) styleObj.setProperty("width", width + "px");
			if (height != -1) styleObj.setProperty("height", height + "px");

			if (enableAnchoring)
			{
				boolean anchoredTop = (anchorFlags & IAnchorConstants.NORTH) != 0;
				boolean anchoredRight = (anchorFlags & IAnchorConstants.EAST) != 0;
				boolean anchoredBottom = (anchorFlags & IAnchorConstants.SOUTH) != 0;
				boolean anchoredLeft = (anchorFlags & IAnchorConstants.WEST) != 0;

				if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
				if (!anchoredTop && !anchoredBottom) anchoredTop = true;

				if (anchoredTop) styleObj.setProperty("top", (top) + "px");
				else styleObj.remove("top");
				if (anchoredBottom) styleObj.setProperty("bottom", (partEndY - partStartY - top - offsetHeight) + "px");
				else styleObj.remove("bottom");
				if (!anchoredTop || !anchoredBottom) styleObj.setProperty("height", height + "px");
				else styleObj.remove("height");
				if (anchoredLeft) styleObj.setProperty("left", left + "px");
				else styleObj.remove("left");
				if (anchoredRight) styleObj.setProperty("right", (partWidth - left - offsetWidth) + "px");
				else styleObj.remove("right");
				if (!anchoredLeft || !anchoredRight) styleObj.setProperty("width", width + "px");
				else styleObj.remove("width");
				styleObj.setProperty("position", "absolute");
			}
			else
			{
				if ((top != -1) || (left != -1)) styleObj.setProperty("position", "absolute");
			}
		}
	}
	static class NoLocationCSSBoundsHandler extends DefaultCSSBoundsHandler
	{
		public static final ICSSBoundsHandler INSTANCE = new NoLocationCSSBoundsHandler();

		@Override
		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring, Point locationModifier)
		{
			int w = component.getSize().width;
			if (ins != null) w -= (ins.left + ins.right); //seems not needed??? (it does! table view)
			int h = component.getSize().height;
			if (ins != null) h -= (ins.top + ins.bottom); //seems not needed? (it does! table view)

			createBounds(styleObj, -1, -1, w, h, component.getSize().width, component.getSize().height, -1, partStartY, partEndY, partWidth, enableAnchoring);
		}
	}

	static class YOffsetCSSBoundsHandler extends DefaultCSSBoundsHandler
	{
		private final int offset;

		YOffsetCSSBoundsHandler(int offset)
		{
			this.offset = offset;
		}

		@Override
		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring, Point locationModifier)
		{
			int y = component.getLocation().y + offset;
//			if (ins != null) y += ins.top;
			int x = component.getLocation().x;
//			if (ins != null) x += ins.left;
			if (locationModifier != null)
			{
				y = Math.max(y + locationModifier.y, 0);
				x = Math.max(x + locationModifier.x, 0);
			}
			int w = component.getSize().width;
			if (ins != null) w -= (ins.left + ins.right);
			int h = component.getSize().height;
			if (ins != null) h -= (ins.top + ins.bottom);

			int anchorFlags = 0;
			if (component instanceof ISupportAnchors) anchorFlags = ((ISupportAnchors)component).getAnchors();

			createBounds(styleObj, y, x, w, h, component.getSize().width, component.getSize().height, anchorFlags, partStartY, partEndY, partWidth,
				enableAnchoring);
		}
	}

	private static void applyTextProperties(ISupportTextSetup component, TextualStyle styleObj)
	{
		if (component.getHorizontalAlignment() != -1)
		{
			styleObj.setProperty("text-align", getHorizontalAlignValue(component.getHorizontalAlignment()));
		}
	}

	private static String createInsetsText(Insets i)
	{
		StringBuffer pad = new StringBuffer();
		pad.append(i.top);
		pad.append("px ");
		pad.append(i.right);
		pad.append("px ");
		pad.append(i.bottom);
		pad.append("px ");
		pad.append(i.left);
		pad.append("px");
		return pad.toString();
	}

	public static String getHorizontalAlignValue(int ha)
	{
		switch (ha)
		{
			case ISupportTextSetup.CENTER :
				return "center";
			case ISupportTextSetup.RIGHT :
				return "right";
			case ISupportTextSetup.LEFT :
			default :
				return "left";
		}
	}

	public static String getVerticalAlignValue(int va)
	{
		switch (va)
		{
			case ISupportTextSetup.CENTER :
				return "middle";
			case ISupportTextSetup.TOP :
				return "top";
			case ISupportTextSetup.BOTTOM :
			default :
				return "bottom";
		}
	}

	protected static BorderAndPadding applyBaseComponentProperties(BaseComponent component, Form form, TextualStyle styleObj, Insets defaultPadding,
		Insets defaultBorder, IServiceProvider sp)
	{

		Pair<IStyleSheet, IStyleRule> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, component, form);

		Insets insetsBorder = null;
		Border designBorder = null;
		if (component.getBorderType() != null)
		{
			insetsBorder = ComponentFactoryHelper.createBorderCSSProperties(component.getBorderType(), styleObj);
			designBorder = ComponentFactoryHelper.createBorder(component.getBorderType());
		}

		Insets insetsMargin = null;
		if (component instanceof ISupportTextSetup)
		{
			insetsMargin = ((ISupportTextSetup)component).getMargin();
		}

		if (styleInfo != null)
		{
			IStyleSheet ss = styleInfo.getLeft();
			IStyleRule s = styleInfo.getRight();
			if (ss != null && s != null)
			{
				List<String> attributeNames = s.getAttributeNames();
				for (String s_attr : attributeNames)
				{
					// Skip margin related attributes. Margin is computed separately below, and rendered as padding.
					if (s_attr.toLowerCase().contains("margin")) continue;
					// do not add any border attributes if set on component
					if (s_attr.toLowerCase().contains("border") && component.getBorderType() != null) continue;
					String val = s.getValue(s_attr);
					if (s_attr.equals("font-size"))
					{
						String tmp = val;
						if (tmp.endsWith("px"))
						{
							int size = Utils.getAsInteger(tmp.substring(0, tmp.length() - 2));

							// 9 should be defined hard. Because 12 (1.33*9) is to big.
							if (size == 9)
							{
								size = 11;
							}
							else
							{
								size = (int)(size * (4 / (double)3));
							}
							styleObj.setProperty(s_attr, size + "px");
						}
						else
						{
							int size = 0;
							if (tmp.endsWith("pt"))
							{
								size = Utils.getAsInteger(tmp.substring(0, tmp.length() - 2));
							}
							else
							{
								size = Utils.getAsInteger(tmp);
							}
							// 9 should be defined hard. Because 6 (0.75*9) is to small.
							if (size == 9)
							{
								size = 7;
							}
							else
							{
								size = (int)(size * (3 / (double)4));
							}
							styleObj.setProperty(s_attr, size + "pt");
						}
					}
					else
					{
						if (val.toString() != null) styleObj.setProperty(s_attr, s.getValues(s_attr), false);
					}
				}
				if (component.getBorderType() == null)
				{
					if (ss.hasBorder(s))
					{
						Border b = ss.getBorder(s);
						if (b != null)
						{
							try
							{
								insetsBorder = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
							}
							catch (Exception e)
							{
								Debug.error("for border " + b + " no insets could be extracted.", e);
							}
							TextualStyle borderStyle = new TextualStyle();
							ComponentFactoryHelper.createBorderCSSProperties(ComponentFactoryHelper.createBorderString(b), borderStyle);
							Enumeration<Object> cssAttributes = borderStyle.keys();
							while (cssAttributes.hasMoreElements())
							{
								String att = (String)cssAttributes.nextElement();
								// put the default values, if not all specified in css
								styleObj.setProperty(att, borderStyle.getProperty(att), false);
							}
						}
					}
				}
				if (insetsMargin == null)
				{
					if (ss.hasMargin(s))
					{
						insetsMargin = ss.getMargin(s);
					}
				}
			}
		}
		if (component.getFontType() != null)
		{
			Pair<String, String>[] props = PersistHelper.createFontCSSProperties(component.getFontType());
			if (props != null)
			{
				for (Pair<String, String> element : props)
				{
					if (element == null) continue;
					styleObj.setProperty(element.getLeft(), element.getRight());
				}
			}
		}
		if (component.getForeground() != null)
		{
			styleObj.setProperty("color", PersistHelper.createColorString(component.getForeground()));
		}

		if (component.getTransparent())
		{
			styleObj.setProperty("background-color", IStyleSheet.COLOR_TRANSPARENT);
		}
		else if (component.getBackground() != null)
		{
			styleObj.setProperty("background-color", PersistHelper.createColorString(component.getBackground()));
		}

		if (insetsBorder == null) insetsBorder = defaultBorder;

		if (insetsMargin == null && defaultPadding != null) insetsMargin = defaultPadding;

		BorderAndPadding bp = new BorderAndPadding(insetsBorder, insetsMargin);
		styleObj.setProperty("padding", (designBorder instanceof EmptyBorder && !(designBorder instanceof MatteBorder)) ? createInsetsText(bp.getSum())
			: createInsetsText(bp.getPadding()));
		return bp;
	}

	//returns space as last char
	private static String getJavaScriptIDParameter(Form form, IPersist meta)
	{
		return getJavaScriptIDParameter(form, meta, "", "");
	}

	private static String getJavaScriptIDParameter(Form form, IPersist meta, String prefix, String suffix)
	{
		return "id='" + prefix + ComponentFactory.getWebID(form, meta) + suffix + "' ";
	}

	//returns space as last char
	private static String getWicketIDParameter(Form form, IPersist meta)
	{
		return getWicketIDParameter(form, meta, "", "");
	}

	private static String getWicketIDParameter(Form form, IPersist meta, String prefix, String suffix)
	{
		return "servoy:id='" + prefix + ComponentFactory.getWebID(form, meta) + suffix + "' ";
	}

	public static boolean isTableViewComponent(IFormElement element)
	{
		boolean isTableViewComponent = false;
		if (element != null)
		{
			Portal parentPortal = (Portal)element.getAncestor(IRepository.PORTALS);
			if (parentPortal != null && !parentPortal.equals(element))
			{
				isTableViewComponent = true;
			}
			else
			{
				Form parentForm = (Form)element.getAncestor(IRepository.FORMS);
				if (parentForm != null && !parentForm.equals(element) &&
					(parentForm.getView() == FormController.TABLE_VIEW || parentForm.getView() == FormController.LOCKED_TABLE_VIEW))
				{
					isTableViewComponent = true;
				}
			}
		}
		return isTableViewComponent;
	}

	public static void clearFormCache()
	{
		formCache.cache.clear();
	}
}