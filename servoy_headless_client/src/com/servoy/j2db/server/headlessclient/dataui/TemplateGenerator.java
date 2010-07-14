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
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;

import org.apache.wicket.ResourceReference;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
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
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.ui.ISupportRowBGColorScript;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * HTML generator used by the web framework
 * 
 * @author jblok
 */
public class TemplateGenerator
{
	/**
	 * @author jcompagner
	 * 
	 */
	private static class FormCache
	{

		private FormCache(boolean monitorCache)
		{
			if (monitorCache)
			{
				new Thread(new Runnable()
				{

					public void run()
					{
						long sleepTime = 24 * 60 * 60 * 1000;
						while (true)
						{
							try
							{
								Thread.sleep(sleepTime); // sleep for 24 hours

								long time = System.currentTimeMillis();
								Iterator<CacheItem> it = cache.values().iterator();
								while (it.hasNext())
								{
									CacheItem item = it.next();
									if ((time - item.lasttouched) > sleepTime)
									{
										if (Debug.tracing())
										{
											Debug.trace("Removing cache item: " + item.content); //$NON-NLS-1$
										}
										it.remove();
									}
								}

							}
							catch (Exception e)
							{
								Debug.error("error in FormCacheMonitor", e); //$NON-NLS-1$
							}
						}
					}
				}, "FormCache Monitor").start(); //$NON-NLS-1$
			}
		}

		private static class CacheItem
		{
			long lastmodified;
			long lasttouched = System.currentTimeMillis();

			Pair<String, String> content;
		}

		private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<String, CacheItem>();

		/**
		 * @param sp
		 * @param f
		 * @param overriddenStyleName
		 * @return
		 */
		public Pair<String, String> getFormAndCssPair(Form f, String overriddenStyleName, IRepository repository)
		{
			Pair<String, String> retval = null;

			long t = getLastModifiedTime(f.getSolution(), f, overriddenStyleName, repository);

			String cacheKey = System.identityHashCode(f) + ":" + overriddenStyleName; //$NON-NLS-1$
			CacheItem cacheItem = cache.get(cacheKey);


			if (cacheItem != null && cacheItem.lastmodified == t)
			{
				retval = cacheItem.content;
				cacheItem.lasttouched = System.currentTimeMillis();
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
		public void putFormAndCssPair(Form f, String overriddenStyleName, IRepository repository, Pair<String, String> formAndCss)
		{
			long t = getLastModifiedTime(f.getSolution(), f, overriddenStyleName, repository);
			CacheItem cacheItem = new CacheItem();
			cacheItem.lastmodified = t;
			cacheItem.content = formAndCss;
			cache.put(System.identityHashCode(f) + ":" + overriddenStyleName, cacheItem); //$NON-NLS-1$
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
	public static final Insets DEFAULT_FIELD_PADDING = new Insets(1, 1, 1, 1);
	public static final Insets DEFAULT_BUTTON_BORDER_SIZE = new Insets(1, 1, 1, 1);
	public static final Insets DEFAULT_FIELD_BORDER_SIZE = new Insets(2, 2, 2, 2);

	public static final int SORTABLE_HEADER_PADDING = 2;
	public static final int NO_LABELFOR_DEFAULT_BORDER_WIDTH = 1;

	public static final String TABLE_VIEW_CELL_CLASS = "tableviewcell"; // this value is also used in servoy.js; if you change/remove it please update servoy.js //$NON-NLS-1$

	private static FormCache globalCache = new FormCache(true);

	private static Map<IServiceProvider, FormCache> serviceProviderCache = Collections.synchronizedMap(new WeakHashMap<IServiceProvider, FormCache>());


	public static Pair<String, String> getFormHTMLAndCSS(int solution_id, int form_id) throws RepositoryException, RemoteException
	{
		final IRepository repository = ApplicationServerSingleton.get().getLocalRepository();

		Solution solution = (Solution)repository.getActiveRootObject(solution_id);
		Form form = solution.getForm(form_id);
		IServiceProvider sp = null;
		if (WebClientSession.get() != null)
		{
			sp = WebClientSession.get().getWebClient();
		}
		return getFormHTMLAndCSS(solution, form, sp);
	}

	public static Pair<String, String> getFormHTMLAndCSS(Solution solution, Form f, IServiceProvider sp) throws RepositoryException, RemoteException
	{
		if (f == null) return null;
		final IRepository repository = ApplicationServerSingleton.get().getLocalRepository();
		boolean enableAnchoring = Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.webclient.enableAnchors", Boolean.TRUE.toString())); //$NON-NLS-1$ 

		String overriddenStyleName = null;
		FormCache formCache = null;
		if (sp == null)
		{
			if (Debug.tracing())
			{
				Debug.trace("IService Provider is null in template generator, is ok if it was a webdav request", new RuntimeException()); //$NON-NLS-1$
			}
			formCache = globalCache;
		}
		else
		{
			boolean isDesign = sp != null && sp.getFlattenedSolution().isInDesign(f) && f.getView() == FormController.LOCKED_TABLE_VIEW;
			boolean isFormCopy = sp.getFlattenedSolution().hasCopy(f);
			overriddenStyleName = ComponentFactory.getOverriddenStyleName(sp, f);

			if (isFormCopy || overriddenStyleName != null || isDesign)
			{
				formCache = serviceProviderCache.get(sp);
				if (formCache == null)
				{
					formCache = new FormCache(false);
					serviceProviderCache.put(sp, formCache);
				}
			}
			else
			{
				formCache = globalCache;
			}
		}

		Pair<String, String> retval = formCache.getFormAndCssPair(f, overriddenStyleName, repository);
		if (retval != null)
		{
			return retval;
		}

		if (f.getExtendsFormID() > 0)
		{
			FlattenedSolution fs = sp == null ? null : sp.getFlattenedSolution();
			if (fs == null)
			{
				try
				{
					fs = new FlattenedSolution(solution.getSolutionMetaData(), new AbstractActiveSolutionHandler()
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
					Debug.error("Couldn't create flattened form for the template generator", e); //$NON-NLS-1$ 
				}
				finally
				{
					if (fs != null)
					{
						try
						{
							fs.close(null);
						}
						catch (IOException e)
						{
						}
					}
				}
			}
			f = fs.getFlattenedForm(f);
		}

		StringBuffer html = new StringBuffer();
		TextualCSS css = new TextualCSS();

		IFormLayoutProvider layoutProvider = FormLayoutProviderFactory.getFormLayoutProvider(sp, solution, f);

		int viewType = layoutProvider.getViewType();
		Color bgColor = layoutProvider.getBackgroundColor();

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

			if (part.getPartType() == Part.BODY && (viewType == FormController.TABLE_VIEW || viewType == FormController.LOCKED_TABLE_VIEW))
			{
				layoutProvider.renderOpenTableViewHTML(html, css, part);
				createCellBasedView(f, f, html, css, layoutProvider.needsHeaders(), startY, endY, bgColor, sp);//tableview == bodypart
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

		retval = new Pair<String, String>(html.toString(), StripHTMLTagsConverter.convertMediaReferences(css.toString(), solution.getName(),
			new ResourceReference("media"), "../../").toString()); //$NON-NLS-1$ //$NON-NLS-2$ // string the formcss/solutionname/ out of the url.
		formCache.putFormAndCssPair(f, overriddenStyleName, repository, retval);
		return retval;
	}

	private static void placePartElements(Form f, int startY, int endY, StringBuffer html, TextualCSS css, Color formBgColor, boolean enableAnchoring,
		IServiceProvider sp) throws RepositoryException
	{
		Iterator<IPersist> it = f.getAllObjectsSortedByFormIndex();
		while (it.hasNext())
		{
			Point l = null;
			IPersist element = it.next();
			if (element instanceof Part) continue; // Skip parts
			if (element instanceof IFormElement)
			{
				l = ((IFormElement)element).getLocation();

				if (l == null) continue;//unknown where to add

				if (l.y >= startY && l.y < endY)
				{
					try
					{
						css.addCSSBoundsHandler(new YOffsetCSSBoundsHandler(-startY));
						createComponentHTML(element, f, html, css, formBgColor, startY, endY, enableAnchoring, sp);
						html.append('\n');
					}
					finally
					{
						css.removeCSSBoundsHandler();
					}
				}
			}
		}
	}

	private static void createCellBasedView(AbstractBase obj, Form form, StringBuffer html, TextualCSS css, boolean addHeaders, int startY, int endY,
		Color bgColor, IServiceProvider sp) throws RepositoryException
	{
		try
		{
			html.append('\n');
			boolean sortable = true;

			boolean shouldFillAllHorizSpace = false;
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

			if (obj instanceof Form)
			{
				html.append("<table border=0 cellpadding=0 cellspacing=0 width='100%'>\n");//$NON-NLS-1$ 
			}
			else
			//is portal
			{
				Portal p = (Portal)obj;
				sortable = p.getSortable();
				TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(p));
				BorderAndPadding ins = applyBaseComponentProperties(p, form, styleObj, null, null, sp);
				applyLocationAndSize(p, styleObj, ins, startY, endY, form.getSize().width, true);
				html.append("<div style='overflow: auto' ");//$NON-NLS-1$ 
				html.append(getWicketIDParameter(p));
//				html.append(getJavaScriptIDParameter(p));
				html.append(getCSSClassParameter("portal"));//$NON-NLS-1$ 
				html.append("><table cellpadding='0' cellspacing='0' class='portal'>\n");//$NON-NLS-1$ 
			}
			css.addCSSBoundsHandler(NoLocationCSSBoundsHandler.INSTANCE);
			html.append("<tr><td height='99%'><table border=0 cellpadding=0 cellspacing=0 width='100%'>\n");//$NON-NLS-1$ 
			int totalWidth = 0;
			if (addHeaders)
			{
				Map<String, GraphicalComponent> labelsFor = new HashMap<String, GraphicalComponent>();
				html.append("<thead ");//$NON-NLS-1$ 
//				if (sortable) html.append("servoy:id='header'");
				html.append("servoy:id='header'");//$NON-NLS-1$ 
				html.append("><tr class='headerbuttons'>\n");//$NON-NLS-1$ 
				Iterator<IPersist> it1 = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				while (it1.hasNext())
				{
					IPersist element = it1.next();
					if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
					{
						labelsFor.put(((GraphicalComponent)element).getLabelFor(), (GraphicalComponent)element);
					}
				}
				boolean usesImageMedia = false;

				boolean hasAtLeastOneLabelFor = false;
				int totalColumnsCount = 0;
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
							totalColumnsCount++;
							GraphicalComponent label = labelsFor.get(((IFormElement)element).getName());
							if (label != null) hasAtLeastOneLabelFor = true;
						}
					}
				}

				int currentColumnCount = 0;
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
							html.append("<th ");//$NON-NLS-1$ 
//							if (sortable) html.append(getWicketIDParameter(element));
							html.append(getWicketIDParameter(element));

							int w = ((IFormElement)element).getSize().width;
							totalWidth += w;
							w = w - (2 + 2); //minus left+rigth padding from css for th

							//					html.append("height='");
							//					html.append( ((IFormElement)element).getSize().height );
							//					html.append("' ");
							TextualStyle styleObj = new TextualStyle();
							GraphicalComponent label = labelsFor.get(((IFormElement)element).getName());
							if (label != null)
							{
								html.append(' ');
								BorderAndPadding ins = applyBaseComponentProperties(label, form, styleObj, (Insets)DEFAULT_LABEL_PADDING.clone(), null, sp);
								// some css attributes were not applied
								Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, label, form);
								Border cssBorder = null;
								if (styleInfo != null)
								{
									javax.swing.text.Style s = styleInfo.getRight();
									FixedStyleSheet ss = styleInfo.getLeft();
									if (ss != null && s != null)
									{
										addAttributeToStyle(styleObj, CSS.Attribute.COLOR.toString(), s.getAttribute(CSS.Attribute.COLOR));
										addAttributeToStyle(styleObj, CSS.Attribute.BACKGROUND_COLOR.toString(), s.getAttribute(CSS.Attribute.BACKGROUND_COLOR));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT.toString(), s.getAttribute(CSS.Attribute.FONT));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_FAMILY.toString(), s.getAttribute(CSS.Attribute.FONT_FAMILY));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_SIZE.toString(), s.getAttribute(CSS.Attribute.FONT_SIZE));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_STYLE.toString(), s.getAttribute(CSS.Attribute.FONT_STYLE));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_VARIANT.toString(), s.getAttribute(CSS.Attribute.FONT_VARIANT));
										addAttributeToStyle(styleObj, CSS.Attribute.FONT_WEIGHT.toString(), s.getAttribute(CSS.Attribute.FONT_WEIGHT));

										cssBorder = ss.getBorder(s);
									}
								}

								if (ins.border != null) w = w - ins.border.left - ins.border.right;
								applyTextProperties(label, styleObj);
								TextualStyle bgborderstyleObj = new TextualStyle();
								if (!sortable)
								{
									bgborderstyleObj.setProperty("text-align", styleObj.getProperty("text-align"));//$NON-NLS-1$ //$NON-NLS-2$
									bgborderstyleObj.setProperty("color", styleObj.getProperty("color"));//$NON-NLS-1$ //$NON-NLS-2$
								}
								if (label.getImageMediaID() <= 0)
								{
									bgborderstyleObj.setProperty("background-image", "none");//$NON-NLS-1$ //$NON-NLS-2$
									usesImageMedia = false;
								}
								else
								{
									usesImageMedia = true;
								}

								bgborderstyleObj.setProperty("background-color", styleObj.getProperty("background-color"));//$NON-NLS-1$ //$NON-NLS-2$
								// unless specified, set the font-weight for <th> to normal, as disabled parent component will make the font bold by default and we don't want that
								if (styleObj.getProperty(CSS.Attribute.FONT_WEIGHT.toString()) == null)
								{
									bgborderstyleObj.setProperty(CSS.Attribute.FONT_WEIGHT.toString(), "normal", false);//$NON-NLS-1$ 
								}
								if (cssBorder != null && label.getBorderType() == null) ComponentFactoryHelper.createBorderCSSProperties(
									ComponentFactoryHelper.createBorderString(cssBorder), bgborderstyleObj);
								else ComponentFactoryHelper.createBorderCSSProperties(label.getBorderType(), bgborderstyleObj);
								html.append(bgborderstyleObj.toString());
								Enumeration<Object> e = bgborderstyleObj.keys();
								while (e.hasMoreElements())
								{
									String key = (String)e.nextElement();
									styleObj.remove(key);
								}
							}
							else
							{
								// If there is no label-for, we put a default right-border to all columns except the last.
								if (!hasAtLeastOneLabelFor)
								{
									TextualStyle defaultBorder = new TextualStyle();
									defaultBorder.setProperty("border-right", NO_LABELFOR_DEFAULT_BORDER_WIDTH + "px solid gray"); //$NON-NLS-1$ //$NON-NLS-2$
									html.append(defaultBorder.toString());
								}
							}
							if (sortable && !usesImageMedia)
							{
								html.append("class='sortable'"); //$NON-NLS-1$ 
							}
							else
							{
								html.append("class='nosort'"); //$NON-NLS-1$ 
							}
							html.append("width='"); //$NON-NLS-1$ 
							html.append(w);
							html.append("' "); //$NON-NLS-1$ 
							html.append('>');
							int headerW = w;
							if (!hasAtLeastOneLabelFor) headerW -= 1;
							html.append("<table servoy:id='headerColumnTable' cellspacing='0' cellpadding='0' width='"); //$NON-NLS-1$ 
							html.append(headerW).append("px'><tr>"); //$NON-NLS-1$ 

							Object fontHeight = styleObj.get(CSS.Attribute.FONT_SIZE.toString());
							int cellHeight = 13;
							if (fontHeight != null)
							{
								String sFontHeight = fontHeight.toString().toLowerCase();
								if (sFontHeight.endsWith("px") || sFontHeight.endsWith("pt")) sFontHeight = sFontHeight.substring(0, sFontHeight.length() - 2); //$NON-NLS-1$ //$NON-NLS-2$ 
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
							html.append("<td height='").append(cellHeight).append("px'>"); //$NON-NLS-1$ //$NON-NLS-2$

							styleObj.setProperty("overflow", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$
							styleObj.setProperty("left", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
							styleObj.setProperty("right", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
							styleObj.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$

							TextualStyle sortLinkStyle = new TextualStyle();
							sortLinkStyle.setProperty("position", "relative"); //$NON-NLS-1$ //$NON-NLS-2$
							sortLinkStyle.setProperty("width", (headerW - SortableCellViewHeader.ARROW_WIDTH) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
							if (sortable)
							{
								sortLinkStyle.setProperty("text-align", styleObj.getProperty("text-align")); //$NON-NLS-1$ //$NON-NLS-2$
								html.append("<a servoy:id='sortLink' "); //$NON-NLS-1$ 
//								if (element instanceof ISupportDataProviderID && ((ISupportDataProviderID)element).getDataProviderID() != null)
//								{
//									html.append("class='orderOff' ");
//								}
								html.append(sortLinkStyle.toString());
								html.append('>');
								html.append("<div servoy:id='headertext' "); //$NON-NLS-1$ 
								styleObj.setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
								html.append(styleObj.toString());
								html.append("></div>"); //$NON-NLS-1$ 
								html.append("</a>"); //$NON-NLS-1$ 
							}
							else
							{
								html.append("<div servoy:id='sortLink' "); //$NON-NLS-1$ 
								html.append(sortLinkStyle.toString());
								html.append('>');
								html.append("<div servoy:id='headertext' "); //$NON-NLS-1$ 
								html.append(styleObj.toString());
								html.append("></div>"); //$NON-NLS-1$ 
								html.append("</div>"); //$NON-NLS-1$ 
							}
							html.append("</td><td valign='middle' align='right'>"); //$NON-NLS-1$ 
							if (sortable)
							{
								html.append("<img servoy:id='resizeBar'></img>"); //$NON-NLS-1$ 
							}
							else
							{
								html.append("<span servoy:id='resizeBar'>&nbsp;</span>"); //$NON-NLS-1$ 
//								html.append("<span ");
//								html.append(getWicketIDParameter(element));
//								html.append(styleObj.toString());
//								html.append("></span>");
							}

							html.append("</td></tr></table>\n"); //$NON-NLS-1$ 
							html.append("</th>\n"); //$NON-NLS-1$ 
						}
					}
				}

				// add filler, needed for column resize.
				// when the table has no horizontal scrollbar, then there should be no filler.
				if (!shouldFillAllHorizSpace)
				{
					html.append("<th"); //$NON-NLS-1$ 
					if (bgColor != null)
					{
						html.append(" style=\"background-image: none; background-color: "); //$NON-NLS-1$ 
						html.append(PersistHelper.createColorString(bgColor));
						html.append(";\""); //$NON-NLS-1$ 
					}
					html.append(">&nbsp;</th>\n"); // add filler (need to be a space else safari & ie7 will not display correctly) //$NON-NLS-1$
				}

				html.append("</tr></thead>\n"); //$NON-NLS-1$ 
			}
			html.append("<tbody>\n"); //$NON-NLS-1$ 

			StringBuffer columns = new StringBuffer();
			int colCount = 0;
			Iterator<IPersist> it = obj.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			int firstComponentHeight = -1;
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
						columns.append("<td ");//$NON-NLS-1$
						if (element instanceof ISupportName)
						{
							String name = ((ISupportName)element).getName();
							if (((name != null) && (name.trim().length() > 0)) || addHeaders)
							{
								// this column's cells can be made invisible (and <td> tag is the one that has to change)
								// so we will link this <td> to a wicket component
								columns.append("servoy:id='"); //$NON-NLS-1$
								columns.append(ComponentFactory.getWebID(element));
								columns.append("_' ");//$NON-NLS-1$
							}
						}
						if (firstComponentHeight == -1) firstComponentHeight = ((IFormElement)element).getSize().height;
						if (!addHeaders && !shouldFillAllHorizSpace)
						{
							columns.append("width='"); //$NON-NLS-1$ 
							int w = ((IFormElement)element).getSize().width;
							totalWidth += w;
//							w = w - (2+2);  //minus left+rigth padding from css for th
							columns.append(w);
							columns.append("' "); //$NON-NLS-1$ 
						}
						//					columns.append("valign='middle' ");
						columns.append('>');
						columns.append("<div class='" + TABLE_VIEW_CELL_CLASS + "'>"); //$NON-NLS-1$ //$NON-NLS-2$ 
						createComponentHTML(element, form, columns, css, bgColor, startY, endY, false, sp);
						TextualStyle idBasedStyle = css.addStyle('#' + ComponentFactory.getWebID(element));
						TextualStyle classBasedStyle = css.addStyle('.' + ComponentFactory.getWebID(element));
						classBasedStyle.copyAllFrom(idBasedStyle);
						if (element instanceof Field)
						{
							int type = ((Field)element).getDisplayType();
							if (type == Field.PASSWORD || type == Field.TEXT_FIELD || type == Field.TYPE_AHEAD || type == Field.TEXT_AREA)
							{
								classBasedStyle.setProperty("float", "left"); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						columns.append("</div>\n"); //$NON-NLS-1$ 
						columns.append("</td>\n"); //$NON-NLS-1$ 
						colCount++;
					}
				}
			}

			// add filler, needed for column resize.
			// no filler when the tableview has no horizontal scrollbar.
			if (!shouldFillAllHorizSpace)
			{
				columns.append("<td"); //$NON-NLS-1$ 
				if (bgColor != null)
				{
					columns.append(" style=\"background-image: none; background-color: "); //$NON-NLS-1$ 
					columns.append(PersistHelper.createColorString(bgColor));
					columns.append(";\""); //$NON-NLS-1$ 
				}
				columns.append(">&nbsp;</td>\n"); // add filler (need to be a space else safari & ie7 will not display correctly) //$NON-NLS-1$
			}

			html.append("<tr servoy:id='rows' "); //$NON-NLS-1$ 
			html.append("height='"); //$NON-NLS-1$ 
			html.append(firstComponentHeight);
			html.append("' "); //$NON-NLS-1$ 
			if ((obj instanceof ISupportRowBGColorScript) &&
				(((ISupportRowBGColorScript)obj).getRowBGColorScript() == null || ((ISupportRowBGColorScript)obj).getRowBGColorScript().trim().length() == 0))
			{
				html.append("class='even'"); //$NON-NLS-1$ 
			}
			html.append(">\n"); //$NON-NLS-1$ 
			html.append(columns);
			html.append("</tr>\n"); //$NON-NLS-1$ 
			html.append("</tbody></table>\n"); //$NON-NLS-1$ 
			html.append("</td></tr>\n"); //$NON-NLS-1$ 
			html.append("<tr valign='bottom'>\n"); //$NON-NLS-1$ 
			html.append("<td servoy:id='navigator' height='1%'>&nbsp;</td>\n"); //$NON-NLS-1$ 
			html.append("</tr>\n"); //$NON-NLS-1$ 
			html.append("</table>\n"); //$NON-NLS-1$ 
			if (!(obj instanceof Form))
			{
				html.append("</div>\n"); //$NON-NLS-1$
			}
			html.append("\n\n"); //$NON-NLS-1$ 
		}
		finally
		{
			css.removeCSSBoundsHandler();
		}
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
		private static final String[] strings = new String[] { "input.button", "span.label", "input.field", ".label", ".field" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

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

		public TextualStyle()
		{
			//used when style tag propery is needed
		}

		TextualStyle(String selector, TextualCSS css)
		{
			if (selector == null) throw new NullPointerException("selector cannot be null"); //$NON-NLS-1$ 
			this.selector = selector;
			this.css = css;
		}

		@Override
		public synchronized Object setProperty(String name, String value)
		{
			if (name == null) return null;
			if (value == null)
			{
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
			if (override)
			{
				order.remove(name);
				order.add(name);
				return super.put(name, value);
			}
			else
			{
				if (!super.containsKey(name))
				{
					order.add(name);
					return super.put(name, value);
				}
			}
			return null;
		}

		@Override
		public String toString()
		{
			StringBuffer retval = new StringBuffer();
			if (selector == null)
			{
				retval.append("style='"); //$NON-NLS-1$ 
			}
			else
			{
				retval.append(selector);
				retval.append("\n{\n"); //$NON-NLS-1$ 
			}
			Iterator<String> it = order.iterator();
			while (it.hasNext())
			{
				String name = it.next();
				String val = getProperty(name);
				if (selector != null) retval.append('\t');
				retval.append(name);
				retval.append(": "); //$NON-NLS-1$ 
				retval.append(val);
				if (it.hasNext()) retval.append(';');
				if (selector != null) retval.append('\n');
			}
			if (selector == null)
			{
				retval.append("' "); //$NON-NLS-1$ 
			}
			else
			{
				retval.append("}\n\n"); //$NON-NLS-1$ 
			}
			return retval.toString();
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
				retval.append(": "); //$NON-NLS-1$ 
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
		IRepository repository = ApplicationServerSingleton.get().getLocalRepository();
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
		TextualStyle styleObj = css.addStyle("div." + className + " label"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("vertical-align", "super"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("div." + className + " div.inl"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("display", "inline"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("white-space", "nowrap"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("float", "left"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("div." + className + " div label"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("vertical-align", "middle"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("div." + className + " div input"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("vertical-align", "middle"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("margin", "2px"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("padding", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("width", "16px"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("height", "16px"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getStyleCSS(String name) throws RepositoryException, RemoteException
	{
		if (name != null && name.toLowerCase().endsWith(".css")) //$NON-NLS-1$
		{
			name = name.substring(0, name.length() - 4);
		}
		TextualCSS css = new TextualCSS();
		TextualStyle styleObj = null;

		//default field stuff
		styleObj = css.addStyle(".field");//input, select, textarea"); //$NON-NLS-1$
		styleObj.setProperty("padding", createInsetsText(DEFAULT_FIELD_PADDING));//$NON-NLS-1$
		styleObj.setProperty("margin", "0");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-style", "inset");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-width", createInsetsText(DEFAULT_FIELD_BORDER_SIZE));//$NON-NLS-1$ 
		styleObj.setProperty("border-spacing", "0px 0px");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-color", "#D4D0C8");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#FFFFFF");//$NON-NLS-1$ //$NON-NLS-2$

		//default label stuff
		styleObj = css.addStyle(".label");//input, select, textarea"); //$NON-NLS-1$ 
		styleObj.setProperty("padding", createInsetsText(DEFAULT_LABEL_PADDING));//$NON-NLS-1$ 


		Style s = (Style)ApplicationServerSingleton.get().getLocalRepository().getActiveRootObject(name, IRepository.STYLES);
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

				if ("default".equalsIgnoreCase(a_style_name)) continue; //$NON-NLS-1$ 

//					a_style_name = Utils.stringReplaceExact(a_style_name, "button", "input.button");
				a_style_name = Utils.stringReplaceExact(a_style_name, "label", ".label");//$NON-NLS-1$ //$NON-NLS-2$
//				a_style_name = Utils.stringReplaceExact(a_style_name, "form", "body");
				a_style_name = Utils.stringReplaceExact(a_style_name, "tabpanel", ".tabpanel");//$NON-NLS-1$ //$NON-NLS-2$
				a_style_name = Utils.stringReplaceExact(a_style_name, "portal", "table.portal");//$NON-NLS-1$ //$NON-NLS-2$
				a_style_name = Utils.stringReplaceExact(a_style_name, "field", ".field");//$NON-NLS-1$ //$NON-NLS-2$
				a_style_name = Utils.stringReplaceExact(a_style_name, "check", ".check");//$NON-NLS-1$ //$NON-NLS-2$
				a_style_name = Utils.stringReplaceExact(a_style_name, "radio", ".radio");//$NON-NLS-1$ //$NON-NLS-2$
				// hack if previous command replaced label_field with label_.field
				a_style_name = Utils.stringReplace(a_style_name, "..field", ".field");//$NON-NLS-1$ //$NON-NLS-2$

				a_style_name = Utils.stringReplace(a_style_name, ".", "_", 1);//$NON-NLS-1$ //$NON-NLS-2$

				styleObj = css.addStyle(a_style_name);
				if (a_style_name.equals("body"))//$NON-NLS-1$ 
				{
					styleObj.setProperty("margin", "0px 0px 0px 0px");//$NON-NLS-1$ //$NON-NLS-2$
					bodyMarginAdded = true;
				}
				Enumeration< ? > names = a_style.getAttributeNames();
				while (names.hasMoreElements())
				{
					Object attr = names.nextElement();
					if ("name".equalsIgnoreCase(attr.toString())) continue;//$NON-NLS-1$ 
					Object val = a_style.getAttribute(attr);
					String s_attr = attr.toString();
					s_attr = Utils.stringReplace(s_attr, "margin", "padding");//$NON-NLS-1$ //$NON-NLS-2$
					if (s_attr.equals("font-size"))//$NON-NLS-1$ 
					{
						String tmp = val.toString();
						if (tmp.endsWith("px"))//$NON-NLS-1$ 
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
							styleObj.setProperty(s_attr, size + "px");//$NON-NLS-1$ 
						}
						else
						{
							int size = 0;
							if (tmp.endsWith("pt"))//$NON-NLS-1$ 
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
							styleObj.setProperty(s_attr, size + "pt");//$NON-NLS-1$ 
						}
					}
					else
					{
						styleObj.setProperty(s_attr, val.toString());
					}
				}

				//enhance existing button def
				if (a_style_name.startsWith("button"))//$NON-NLS-1$ 
				{
//					enhanceInputButton(styleObj);
				}
			}
		}

		//create button def if missing
		if (!css.containsKey("button"))//$NON-NLS-1$ 
		{
			styleObj = css.addStyle("button");//$NON-NLS-1$ 
//			enhanceInputButton(styleObj);
		}

		if (!bodyMarginAdded)
		{
			styleObj = css.addStyle("body");//$NON-NLS-1$ 
			styleObj.setProperty("margin", "0px 0px 0px 0px");//$NON-NLS-1$ //$NON-NLS-2$
		}

		int h1 = 0;
		int h2 = 2;

		//default tab panel stuff
		styleObj = css.addStyle("div.tabs");//$NON-NLS-1$ 
		styleObj.setProperty("padding", "3px 0px " + (h1) + "px 2px"); //controls position of bottom line of tab strip//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		styleObj.setProperty("margin", h2 + "px 0px 0px 0px");//controls the space outside the tabs rect.//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-bottom", "1px solid #9ac");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("left", "0px");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("right", "0px");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("position", "absolute");//$NON-NLS-1$ //$NON-NLS-2$

		//default tab stuff
		styleObj = css.addStyle("div.tabs div");//$NON-NLS-1$ 
		styleObj.setProperty("display", "inline");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("float", "left");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("clear", "none");//$NON-NLS-1$ //$NON-NLS-2$

		styleObj = css.addStyle("div.tabs div a");//$NON-NLS-1$ 
		styleObj.setProperty("text-decoration", "none");//$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("padding", (h2 - 2) + "px 10px " + (h1) + "px 10px");//space inside tab arround the text //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		styleObj.setProperty("margin-top", "0.2em");//space betweens tabs //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("margin-left", "0.2em");//space betweens tabs //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#e7e7f7"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border", "1px solid #9ac"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-bottom", "1px solid #9ac"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("float", "left"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("position", "relative"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("top", "1px"); //$NON-NLS-1$ //$NON-NLS-2$

		styleObj = css.addStyle("div.tabs div a:hover"); //$NON-NLS-1$ 
		styleObj.setProperty("color", "#fff"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#c7cce7"); //$NON-NLS-1$ //$NON-NLS-2$

		styleObj = css.addStyle("div.tabs div.selected_tab a"); //$NON-NLS-1$ 
		styleObj.setProperty("color", "#000"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#fff"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-bottom", "1px solid #fff"); //$NON-NLS-1$ //$NON-NLS-2$

		styleObj = css.addStyle("div.tabs div.disabled_tab a"); //$NON-NLS-1$ 
		styleObj.setProperty("color", "lightslategrey"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#e7e7f7"); //$NON-NLS-1$ //$NON-NLS-2$

		styleObj = css.addStyle("div.tabs div.disabled_selected_tab a"); //$NON-NLS-1$ 
		styleObj.setProperty("color", "lightslategrey"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#fff"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border-bottom", "1px solid #fff"); //$NON-NLS-1$ //$NON-NLS-2$

		//default font stuff
		styleObj = css.addStyle("body, input, button, select, td, th, textarea"); //$NON-NLS-1$ 
		styleObj.setProperty("font-family", "Tahoma, Arial, Helvetica, sans-serif"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("font-size", DEFAULT_FONT_SIZE + "px"); //$NON-NLS-1$ //$NON-NLS-2$
//			styleObj.setProperty("-moz-box-sizing", "content-box");


		// default radio/check stuff
		addDefaultCheckRadioStuff(css, "field"); //$NON-NLS-1$ 
		addDefaultCheckRadioStuff(css, "check"); //$NON-NLS-1$ 
		addDefaultCheckRadioStuff(css, "radio"); //$NON-NLS-1$ 

		//default table stuff (used by portal/listview)
		styleObj = css.addStyle("th.nosort"); //$NON-NLS-1$ 
		styleObj.setProperty("padding", "2px 2px 2px 2px"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("text-align", "center"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("vertical-align", "top"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("th.sortable"); //$NON-NLS-1$ 
		styleObj.setProperty("padding", SORTABLE_HEADER_PADDING + "px " + SORTABLE_HEADER_PADDING + "px " + SORTABLE_HEADER_PADDING + "px " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			SORTABLE_HEADER_PADDING + "px"); //$NON-NLS-1$ 
//			styleObj.setProperty("text-align","center");
		styleObj.setProperty("vertical-align", "top"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("td"); //$NON-NLS-1$ 
		styleObj.setProperty("vertical-align", "top"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("tr.headerbuttons"); //$NON-NLS-1$ 
		styleObj.setProperty("background-image", "url(/servoy-webclient/templates/lafs/kunststoff/images/button/secondary-enabled.gif)", false); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#DBE3EB", false); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-repeat", "repeat-x", false); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-position", "center center", false); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("th.sortable a:visited"); //$NON-NLS-1$ 
		styleObj.setProperty("text-decoration", "none"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("color", "black"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("th.sortable a:hover"); //$NON-NLS-1$ 
		styleObj.setProperty("text-decoration", "underline"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("color", "black"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj = css.addStyle("th.sortable a"); //$NON-NLS-1$ 
		styleObj.setProperty("text-decoration", "none");//underline //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("color", "black"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-position", "right"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-repeat", "no-repeat", false); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("display", "block"); //$NON-NLS-1$ //$NON-NLS-2$
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
		styleObj = css.addStyle(".tableviewcell"); //$NON-NLS-1$ 
		styleObj.setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("position", "relative"); //$NON-NLS-1$ //$NON-NLS-2$

		// tooltip stuff
		styleObj = css.addStyle("#mktipmsg"); //$NON-NLS-1$ 
		styleObj.setProperty("padding", "2px"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("background-color", "#FFFFE1"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("border", "1px solid #000000"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("white-space", "nowrap"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("font-family", "arial, helvetica, tahoma, sans-serif"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("font-size", "11px"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("color", "#000000"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("display", "none"); //$NON-NLS-1$ //$NON-NLS-2$
		styleObj.setProperty("position", "absolute;left:0px;top:0px"); //$NON-NLS-1$ //$NON-NLS-2$

		// disable outline on focused div's (makes scrollbars appear in FF when mouse is clicked inside some div's)
		styleObj = css.addStyle("div:focus"); //$NON-NLS-1$ 
		styleObj.setProperty("outline", "none"); //$NON-NLS-1$ //$NON-NLS-2$

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

	private static void createComponentHTML(IPersist meta, Form form, StringBuffer html, TextualCSS css, Color formBgColor, int startY, int endY,
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
				createPortalHTML((Portal)meta, form, html, css, startY, endY, formBgColor, sp);
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
				createBeanHTML((Bean)meta, form, html, css, startY, endY, enableAnchoring);
				break;

			default :
				Debug.trace("ComponentFactory:unkown type " + meta.getTypeID()); //$NON-NLS-1$
		}
	}

	private static void createBeanHTML(Bean bean, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(bean));
		applyLocationAndSize(bean, styleObj, null, startY, endY, form.getSize().width, enableAnchoring);

		boolean isComponent = false;
		try
		{
			Class< ? > beanClazz = ApplicationServerSingleton.get().getBeanManager().getClassLoader().loadClass(bean.getBeanClassName());
			isComponent = Component.class.isAssignableFrom(beanClazz);
		}
		catch (Throwable e)
		{
			Debug.error(e);
		}

		if (isComponent)
		{
			html.append("<input "); //$NON-NLS-1$ 
			html.append(getWicketIDParameter(bean));
			html.append(getJavaScriptIDParameter(bean));
			html.append("value='"); //$NON-NLS-1$ 
			html.append(bean.getName());
			html.append("' "); //$NON-NLS-1$ 
			html.append("type='image' "); //$NON-NLS-1$ 
			html.append("src='#' "); //$NON-NLS-1$ 
			html.append("/>"); //$NON-NLS-1$ 
		}
		else
		{
			styleObj.setProperty("overflow", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
			html.append("<div "); //$NON-NLS-1$ 
			html.append(getWicketIDParameter(bean));
			html.append(getJavaScriptIDParameter(bean));
			html.append(">only wicket components are supported in webclient</div>"); //$NON-NLS-1$ 
		}
	}

//	private static void createTabHTML(Tab meta, StringBuffer html, TextualCSS css)
//	{
//	}

	private static void createTabPanelHTML(TabPanel tabPanel, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(tabPanel));
		BorderAndPadding borderAndPadding = applyBaseComponentProperties(tabPanel, form, styleObj, null, null, sp);
		applyLocationAndSize(tabPanel, styleObj, borderAndPadding, startY, endY, form.getSize().width, enableAnchoring);
		// do not apply foreground to the whole tab panel
		styleObj.remove("color");
//		html.append("<table cellpadding=0 cellspacing=0 ");
		html.append("<div "); //$NON-NLS-1$ 
		html.append(getWicketIDParameter(tabPanel));
		html.append(getCSSClassParameter("tabpanel")); //$NON-NLS-1$ 
		html.append(">\n"); //$NON-NLS-1$ 

//		int yoffset = 0;
		if (tabPanel.getTabOrientation() != TabPanel.HIDE && tabPanel.getTabOrientation() != TabPanel.SPLIT_HORIZONTAL &&
			tabPanel.getTabOrientation() != TabPanel.SPLIT_VERTICAL && !(tabPanel.getTabOrientation() == TabPanel.DEFAULT && tabPanel.hasOneTab()))
		{
//			yoffset = 20;
//			html.append("\t<thead><tr valign='bottom'><th height=20>\n");
			html.append("\t<div tabholder='true' class='tabs'>\n"); //$NON-NLS-1$ 
			html.append("\t<servoy:remove>\n"); //$NON-NLS-1$ 
			html.append("\t\t<div class='selected_tab'><a href='tab1'>Tab1</a></div>\n"); //$NON-NLS-1$ 
			html.append("\t\t<div><a href='tab1'>Tab2</a></div>\n"); //$NON-NLS-1$ 
			html.append("\t</servoy:remove>\n"); //$NON-NLS-1$ 
			Iterator<IPersist> it = tabPanel.getAllObjects();
			if (it.hasNext())
			{
				Tab tab = (Tab)it.next();
				//				applyTextProperties(tab, styleObj);
				html.append("\t\t<div servoy:id='tablinks'"); //$NON-NLS-1$ 
				html.append("><a servoy:id='tablink' href='tab1'><span style=\"white-space: nowrap;\" servoy:id='linktext'>"); //$NON-NLS-1$ 
				html.append(getSafeText(tab.getText()));
				html.append("</span></a></div>\n"); //$NON-NLS-1$ 
			}
			else
			{
				html.append("\t\t<div servoy:id='tablinks'"); //$NON-NLS-1$ 
				html.append("><a servoy:id='tablink' href='tab1'><span style=\"white-space: nowrap;\" servoy:id='linktext'>"); //$NON-NLS-1$ 
				html.append("No tabs"); //$NON-NLS-1$ 
				html.append("</span></a></div>\n"); //$NON-NLS-1$ 
			}
			html.append("\t</div>\n"); //$NON-NLS-1$ 
//			html.append("\t</th></tr></thead>\n");
		}
//		html.append("\t<tbody><tr><td>\n");
//		Insets ins = borderAndPadding != null ? borderAndPadding.getSum() : new Insets(0, 0, 0, 0);
//		int t_width = tabPanel.getSize().width - (ins.left + ins.right + 2); //not sure why to add 2
//		int t_height = (tabPanel.getSize().height - yoffset) - (ins.top + ins.bottom + 4); //not sure why to add 4 
		TextualStyle style = new TextualStyle();
		style.setProperty("overflow", "auto");
		style.setProperty("position", "relative"); //$NON-NLS-1$ //$NON-NLS-2$
//		style.setProperty("width",t_width+"px");
//		style.setProperty("height",t_height+"px");
		if (tabPanel.getBorderType() == null)
		{
			if (!tabPanel.hasOneTab() && tabPanel.getTabOrientation() != TabPanel.HIDE)
			{
				//default
				style.setProperty("border-width", "0px 1px 1px 1px"); //$NON-NLS-1$ //$NON-NLS-2$
				style.setProperty("border-color", "#99AACC"); //$NON-NLS-1$ //$NON-NLS-2$
				style.setProperty("border-style", "solid"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else
		{
			ComponentFactoryHelper.createBorderCSSProperties(tabPanel.getBorderType(), styleObj);
		}
		if (tabPanel.getTabOrientation() == TabPanel.SPLIT_HORIZONTAL || tabPanel.getTabOrientation() == TabPanel.SPLIT_VERTICAL)
		{
			String tabPanelMarkupId = ComponentFactory.getWebID(tabPanel);

			StringBuffer leftPanelStyle = new StringBuffer("position: absolute; "); //$NON-NLS-1$
			StringBuffer rightPanelStyle = new StringBuffer("position: absolute; overflow: auto; "); //$NON-NLS-1$
			if (tabPanel.getTabOrientation() == TabPanel.SPLIT_HORIZONTAL)
			{
				leftPanelStyle.append("top: 0px; left: 0px; bottom: 0px;"); //$NON-NLS-1$
				rightPanelStyle.append("top: 0px; bottom: 0px; right: 0px"); //$NON-NLS-1$ 
			}
			else
			{
				leftPanelStyle.append("top: 0px; left: 0px; right: 0px;"); //$NON-NLS-1$ 
				rightPanelStyle.append("left: 0px; bottom: 0px; right: 0px;"); //$NON-NLS-1$ 
			}

			html.append("\t<div id='splitter_").append(tabPanelMarkupId).append("' servoy:id='splitter' style='").append(leftPanelStyle).append( //$NON-NLS-1$  //$NON-NLS-2$
				"'><div id='websplit_left_").append(tabPanelMarkupId).append("' servoy:id='websplit_left' style='overflow: auto; ").append(leftPanelStyle).append( //$NON-NLS-1$  //$NON-NLS-2$
				"' ><div servoy:id='webform'></div></div></div>"); //$NON-NLS-1$ 
			html.append("<div id='websplit_right_").append(tabPanelMarkupId).append("' servoy:id='websplit_right' style='").append(rightPanelStyle).append( //$NON-NLS-1$  //$NON-NLS-2$ 
				"'><div servoy:id='webform'></div></div>"); //$NON-NLS-1$ 
		}
		else
		{
			html.append("\t<div servoy:id='webform' " + style.toString() + "></div>"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		html.append("</div>"); //$NON-NLS-1$ 		


//		html.append("</td></tr></tbody></table>");
	}

	private static void createPortalHTML(Portal meta, Form form, StringBuffer html, TextualCSS css, int startY, int endY, Color formBgColor, IServiceProvider sp)
		throws RepositoryException
	{
		Color portalBgColor = meta.getBackground();
		if (portalBgColor == null) portalBgColor = formBgColor;
		createCellBasedView(meta, form, html, css, !meta.getMultiLine(), startY, endY, portalBgColor, sp);
	}

	private static void createShapeHTML(Shape shape, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(shape));
		BorderAndPadding ins = applyBaseComponentProperties(shape, form, styleObj, null, null, sp);
		html.append("<span "); //$NON-NLS-1$ 
		html.append(getWicketIDParameter(shape));
		html.append(getJavaScriptIDParameter(shape));
		//html.append(getCSSClassParameter((BaseComponent)shape,"field",ComponentFactory.getWebID(shape)));
		html.append("></span>"); //$NON-NLS-1$ 

		applyLocationAndSize(shape, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring);
	}

	private static void createRectangleHTML(RectShape rectshape, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(rectshape));
		BorderAndPadding ins = applyBaseComponentProperties(rectshape, form, styleObj, null, null, sp);

		html.append("<span "); //$NON-NLS-1$ 
		html.append(getWicketIDParameter(rectshape));
		html.append(getJavaScriptIDParameter(rectshape));
		//html.append(getCSSClassParameter((BaseComponent)rectshape,"field",ComponentFactory.getWebID(rectshape)));
		html.append("></span>"); //$NON-NLS-1$ 

		if (rectshape.getLineSize() > 0)
		{
			styleObj.setProperty("border-style", "solid"); //$NON-NLS-1$ //$NON-NLS-2$
			styleObj.setProperty("border-width", rectshape.getLineSize() + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			if (ins.border == null) ins.border = new Insets(rectshape.getLineSize(), rectshape.getLineSize(), rectshape.getLineSize(), rectshape.getLineSize());
			else
			{
				ins.border.top += rectshape.getLineSize();
				ins.border.right += rectshape.getLineSize();
				ins.border.bottom += rectshape.getLineSize();
				ins.border.left += rectshape.getLineSize();
			}
			styleObj.setProperty("border-color", PersistHelper.createColorString(rectshape.getForeground())); //$NON-NLS-1$ 
		}
		applyLocationAndSize(rectshape, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring);
	}

	private static void createGraphicalComponentHTML(GraphicalComponent label, Form form, StringBuffer html, TextualCSS css, int startY, int endY,
		boolean enableAnchoring, IServiceProvider sp)
	{
		String styleName = "#"; //$NON-NLS-1$ 
		Insets border = null;
		if (label.getOnActionMethodID() != 0 && !hasHTMLText(label.getText()) && label.getShowClick())
		{
//			styleName = "input.";
			border = DEFAULT_BUTTON_BORDER_SIZE;
		}
		TextualStyle styleObj = css.addStyle(styleName + ComponentFactory.getWebID(label));
		BorderAndPadding ins = applyBaseComponentProperties(label, form, styleObj, (Insets)DEFAULT_LABEL_PADDING.clone(), border, sp);
		applyTextProperties(label, styleObj);

		Field labelForField = null;
		String labelFor = label.getLabelFor();
		if (labelFor != null)
		{
			try
			{
				Iterator<IPersist> fields = label.getParent().getObjects(IRepository.FIELDS);
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
			catch (RepositoryException ex)
			{
				Debug.error(ex);
			}
		}

		int labelVAlign = label.getVerticalAlignment();
		Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, label, form);
		if (styleInfo != null)
		{
			FixedStyleSheet ss = styleInfo.getLeft();
			javax.swing.text.Style s = styleInfo.getRight();
			if (labelVAlign == -1) labelVAlign = ss.getVAlign(s);
		}

		if (label.getOnActionMethodID() != 0)
		{

			if ( /* !hasHTMLText(label.getText()) && */label.getShowClick())
			{
//				html.append("<input ");
				html.append("<button type='submit' "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(label));
				//			html.append(getJavaScriptIDParameter(label));
				html.append(getDataProviderIDParameter(label));
				html.append(getCSSClassParameter((label.getShowClick() ? "button" : "label"))); //$NON-NLS-1$ //$NON-NLS-2$
				html.append("value='"); //$NON-NLS-1$ 
				String val = ""; //$NON-NLS-1$ 
				if (!hasHTMLText(label.getText())) val = getSafeText(label.getText());
				if (val == "") val = " "; //needed for mac //$NON-NLS-1$ //$NON-NLS-2$
				html.append(val);
				html.append("' "); //$NON-NLS-1$ 
//				html.append("type='submit' ");
				html.append("></button>"); //$NON-NLS-1$ 
				// buttons are border-box by default!!
				ins = null;
			}
			else
			{
				if ((labelVAlign == -1 || labelVAlign == ISupportTextSetup.CENTER || labelVAlign == ISupportTextSetup.BOTTOM) &&
					(isFilledText(label.getText()) || label.getDataProviderID() != null) || label.getImageMediaID() > 0)
				{
					verticalCenterAlignText(label, labelVAlign, form, ins, sp);
					styleObj.setProperty("padding", createInsetsText(ins.padding)); //$NON-NLS-1$ 
				}

				if (labelForField != null)
				{
					html.append("<label for='"); //$NON-NLS-1$ 
					html.append(ComponentFactory.getWebID(labelForField));
					html.append("' "); //$NON-NLS-1$ 
				}
				else
				{
					html.append("<div "); //$NON-NLS-1$ 
				}
				// we want to wrap only if there is no html content in the label text
				if (!HtmlUtils.hasUsefulHtmlContent(label.getText())) html.append(" style=\"white-space: nowrap;\" "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(label));
//				html.append(getJavaScriptIDParameter(label));
				html.append(getDataProviderIDParameter(label));
				html.append(getCSSClassParameter((label.getShowClick() ? "button" : "label"))); //$NON-NLS-1$ //$NON-NLS-2$
				html.append(">"); //$NON-NLS-1$ 
				html.append(getSafeText(label.getText()));
				if (labelForField != null)
				{
					html.append("</label>"); //$NON-NLS-1$ 
				}
				else
				{
					html.append("</div>"); //$NON-NLS-1$ 
				}
				styleObj.setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// disabled this for 124675, previously it where almost always <input type=submit or buttons> also when show click as off, now there are spans
//			if (label.getBorderType() == null && !label.getShowClick())
//			{
//				styleObj.setProperty("border-style","none");//web always has default
//			}
		}
		else
		{
			if ((labelVAlign == -1 || labelVAlign == ISupportTextSetup.CENTER || labelVAlign == ISupportTextSetup.BOTTOM) &&
				(isFilledText(label.getText()) || label.getDataProviderID() != null))
			{

				verticalCenterAlignText(label, labelVAlign, form, ins, sp);
				styleObj.setProperty("padding", createInsetsText(ins.padding)); //$NON-NLS-1$ 
			}

			if (labelForField != null)
			{
				html.append("<label for='"); //$NON-NLS-1$ 
				html.append(ComponentFactory.getWebID(labelForField));
				html.append("' "); //$NON-NLS-1$ 
			}
			else
			{
				html.append("<div "); //$NON-NLS-1$ 
			}
			// we want to wrap only if there is no html content in the label text
			if (!HtmlUtils.hasUsefulHtmlContent(label.getText())) html.append(" style=\"white-space: nowrap;\" "); //$NON-NLS-1$ 
			html.append(getWicketIDParameter(label));
//			html.append(getJavaScriptIDParameter(label));
			html.append(getDataProviderIDParameter(label));
			html.append(getCSSClassParameter("label")); //$NON-NLS-1$ 
			html.append('>');
			boolean hasHtml = hasHTMLText(label.getText());
			if (hasHtml)
			{
				html.append("<servoy:remove>"); //$NON-NLS-1$ 
			}
			html.append(getSafeText(label.getText()));
			if (hasHtml)
			{
				html.append("</servoy:remove>"); //$NON-NLS-1$ 
			}
			if (labelForField != null)
			{
				html.append("</label>"); //$NON-NLS-1$ 
			}
			else
			{
				html.append("</div>"); //$NON-NLS-1$ 
			}
		}

		applyLocationAndSize(label, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring);
		styleObj.setProperty("overflow", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static boolean isFilledText(String text)
	{
		if (text == null) return false;
		if (text.trim().length() == 0) return false;
		return true;
	}

	private static void verticalCenterAlignText(GraphicalComponent label, int valign, Form form, BorderAndPadding borderAndPadding, IServiceProvider sp)
	{
		int size = DEFAULT_FONT_SIZE;
		String fontType = label.getFontType();
		if (fontType != null)
		{
			StringTokenizer tk = new StringTokenizer(fontType, ","); //$NON-NLS-1$
			if (tk.countTokens() >= 3)
			{
				tk.nextToken();
				tk.nextToken();
				size = Utils.getAsInteger(tk.nextToken());
			}
			size = (int)(size * (4 / (double)3));//pt->px
		}
		else
		{
			Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, label, form);
			if (styleInfo != null)
			{
				FixedStyleSheet ss = styleInfo.getLeft();
				javax.swing.text.Style s = styleInfo.getRight();
				if (s.getAttribute(CSS.Attribute.FONT_FAMILY) != null || s.getAttribute(CSS.Attribute.FONT) != null)
				{
					Font f = ss.getFont(s);
					if (f != null)
					{
						size = f.getSize();
						size = (int)(size * (4 / (double)3));//pt->px
					}
				}

			}
		}
		Insets padding = borderAndPadding.padding;
		Insets ins = borderAndPadding.getSum();
		padding.top += getVerticalAlignTopPadding(valign, label.getSize().height, ins.top, ins.bottom, size);
	}

	public static int getVerticalAlignTopPadding(int valign, int height, int topInset, int bottomInset, int fontSize)
	{
		int top = 0;
		if (valign == ISupportTextSetup.BOTTOM) top = height - (topInset + bottomInset) - fontSize;
		else if (valign == ISupportTextSetup.CENTER || valign == -1) top = ((height - (topInset + bottomInset) - fontSize) / 2);
		if (top < 0) top = 0;
		return top;
	}

	public static boolean hasHTMLText(String text)
	{
		return HtmlUtils.startsWithHtml(text);
	}

	public static String getSafeText(String text)
	{
		if (text == null) return ""; //$NON-NLS-1$ 

		String parsedTxt = text;
		if (hasHTMLText(parsedTxt))
		{
			//strip out any html page related tags
			parsedTxt = Utils.stringReplace(parsedTxt, "<html>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "</html>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "<body>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "</body>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "<form>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "</form>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "<head>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "</head>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "<title>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplace(parsedTxt, "</title>", "", -1, false, true); //$NON-NLS-1$ //$NON-NLS-2$

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
			parsedTxt = Utils.stringReplaceRecursive(parsedTxt, ">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
			parsedTxt = Utils.stringReplaceRecursive(parsedTxt, "<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		parsedTxt = Utils.stringReplaceRecursive(parsedTxt, "  ", " "); //$NON-NLS-1$ //$NON-NLS-2$
		parsedTxt = Utils.stringReplaceRecursive(parsedTxt, "\n\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

		return parsedTxt.trim();
	}

	public static boolean needsWrapperDivForAnchoring(Field field)
	{
		// this needs to be in sync with DesignModeBehavior.needsWrapperDivForAnchoring(String type)
		return (field.getDisplayType() == Field.PASSWORD) || (field.getDisplayType() == Field.TEXT_AREA) || (field.getDisplayType() == Field.COMBOBOX) ||
			(field.getDisplayType() == Field.TYPE_AHEAD) || (field.getDisplayType() == Field.TEXT_FIELD);
	}

	private static void createFieldHTML(Field field, Form form, StringBuffer html, TextualCSS css, int startY, int endY, boolean enableAnchoring,
		IServiceProvider sp)
	{
		boolean addWrapperDiv = enableAnchoring && needsWrapperDivForAnchoring(field);

		if (addWrapperDiv)
		{
			// Anchoring fields (<input>s, <textarea>s) with { left: 0px; right: 0px; } pair
			// or { top: 0px; bottom: 0px; } does not work. Thus we add a dummy wrapper <div>
			// which accepts this kind of anchoring, and we place the field inside the <div>
			// with { width: 100%; height: 100%; }, which works fine.
			String wrapperId = ComponentFactory.getWebID(field) + "_wrapper"; //$NON-NLS-1$ 
			TextualStyle wrapperStyle = css.addStyle('#' + wrapperId);
			wrapperStyle.setProperty("overflow", "visible");
			html.append("<div servoy:id='" + wrapperId + "' id='" + wrapperId + "'>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		}

		TextualStyle styleObj = css.addStyle('#' + ComponentFactory.getWebID(field));


		Insets padding = (Insets)DEFAULT_FIELD_PADDING.clone();
		Insets border = (Insets)DEFAULT_FIELD_BORDER_SIZE.clone();

		if (field.getDisplayType() == Field.COMBOBOX /* || (field.getDisplayType() == Field.CHECKS && field.getValuelistID() == 0) */)
		{
			padding = DEFAULT_LABEL_PADDING;
		}
		BorderAndPadding ins = applyBaseComponentProperties(field, form, styleObj, padding, border, sp);

		FixedStyleSheet ss = ComponentFactory.getCSSStyleForForm(sp, form);
		String cssClass = ""; // By default no css class applied. //$NON-NLS-1$ 
		switch (field.getDisplayType())
		{
			case Field.PASSWORD :
			{
				if (ins == null)
				{
					ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE, DEFAULT_FIELD_PADDING);
				}
				html.append("<input "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append("type='password' "); //$NON-NLS-1$ 
				if (field.getSelectOnEnter())
				{
					html.append("onfocus='this.select()'"); //$NON-NLS-1$ 
				}
				html.append("/>"); //$NON-NLS-1$ 
			}
				break;
			case Field.RTF_AREA :
			{
				applyScrolling(styleObj, field);
				html.append("<div "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append(">RTF field not supported in webclient</div>"); //$NON-NLS-1$ 
			}
				break;
			case Field.HTML_AREA :
			{
				applyScrolling(styleObj, field);
				html.append("<div "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append(">Editable HTML field not [yet] supported in webclient</div>"); //$NON-NLS-1$ 
			}
				break;
			case Field.TEXT_AREA :
			{
				if (ins == null)
				{
					ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE, DEFAULT_FIELD_PADDING);
				}
				applyScrolling(styleObj, field);
				html.append("<textarea "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
				//html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append("></textarea>"); //$NON-NLS-1$ 
			}
				break;
			case Field.CHECKS :
				IValueList val = null;
				ValueList valuelist = null;
				if (field.getValuelistID() > 0 && sp != null)
				{
					String[] fieldFormat = ComponentFactory.getFieldFormat(field,
						sp.getFlattenedSolution().getDataproviderLookup(sp.getFoundSetManager(), form), sp);
					valuelist = sp.getFlattenedSolution().getValueList(field.getValuelistID());
					if (valuelist != null) val = ComponentFactory.getRealValueList(sp, valuelist, true, Integer.parseInt(fieldFormat[1]), fieldFormat[0],
						field.getDataProviderID());
				}
				boolean addSingle = false;
				if (val != null && valuelist != null)
				{
					// see condition in ComponentFactory
					addSingle = !(valuelist.getValueListType() == ValueList.DATABASE_VALUES && valuelist.getDatabaseValuesType() == ValueList.RELATED_VALUES) &&
						(val.getSize() == 1) && (valuelist.getAddEmptyValue() != ValueList.EMPTY_VALUE_ALWAYS);
				}

				// If we have multiple checkboxes, then the default is "field".
				if (field.getValuelistID() > 0 && !addSingle) cssClass = "field"; //$NON-NLS-1$ 
				// If we have a style for the form, apply "check" class if present, default to "field" if "check" class is not present.
				if (ss != null)
				{
					cssClass = "field"; //$NON-NLS-1$ 
					String lookUpValue = "check"; //$NON-NLS-1$ 
					javax.swing.text.Style s = ss.getRule(lookUpValue);
					if (s.getAttributeCount() == 0)
					{
						if ((field.getStyleClass() != null) && (field.getStyleClass().trim().length() > 0))
						{
							lookUpValue += '.' + field.getStyleClass().trim();
							s = ss.getRule(lookUpValue);
							if (s.getAttributeCount() > 0) cssClass = "check"; //$NON-NLS-1$ 
						}
					}
					else
					{
						cssClass = "check"; //$NON-NLS-1$ 
					}
				}


				if (field.getValuelistID() > 0 && !addSingle)
				{
					applyScrolling(styleObj, field);
					html.append("<div "); //$NON-NLS-1$ 
					html.append(getWicketIDParameter(field));
					//html.append(getJavaScriptIDParameter(field));
					html.append(getDataProviderIDParameter(field));
					html.append(getCSSClassParameter(cssClass));
					html.append(">Multi checkboxes</div>"); //$NON-NLS-1$ 
				}
				else
				{
					html.append("<div "); //$NON-NLS-1$ 
					html.append(getCSSClassParameter(cssClass));
					html.append(getWicketIDParameter(field));
					html.append(" tabIndex=\"-1\" "); //$NON-NLS-1$ 
					html.append("><input style='border-width: 0px; padding: 0px; margin-top: 0px; margin-bottom: 0px; margin-left: 0px;' "); // //$NON-NLS-1$ 
					html.append(getWicketIDParameter(field, "check_")); //$NON-NLS-1$ 
					html.append(getDataProviderIDParameter(field));
					html.append("type='checkbox' "); //$NON-NLS-1$ 
					html.append("/>"); //$NON-NLS-1$ 
					html.append("<label for='check_"); //$NON-NLS-1$ 
					html.append(ComponentFactory.getWebID(field));
					html.append("' style='margin-top: 0px; margin-bottom: 0px; border-top: 0px; border-bottom: 0px; padding-top: 0px; padding-bottom: 0px;"); //$NON-NLS-1$ 
					html.append("' "); //$NON-NLS-1$ 
					html.append(getWicketIDParameter(field, "text_")); //$NON-NLS-1$ 
					html.append("></label>"); //$NON-NLS-1$ 
					html.append("</div>"); //$NON-NLS-1$ 
				}
				break;
			case Field.RADIOS :
			{
				cssClass = "field"; // By default the "field" class is applied. //$NON-NLS-1$ 
				// If we have a style for the form, apply "radio" class if present, default to "field" if "radio" class is not present.
				if (ss != null)
				{
					String lookUpValue = "radio"; //$NON-NLS-1$ 
					javax.swing.text.Style s = ss.getRule(lookUpValue);
					if (s.getAttributeCount() == 0)
					{
						if ((field.getStyleClass() != null) && (field.getStyleClass().trim().length() > 0))
						{
							lookUpValue += '.' + field.getStyleClass().trim();
							s = ss.getRule(lookUpValue);
							if (s.getAttributeCount() > 0) cssClass = "radio"; //$NON-NLS-1$ 
						}
					}
					else
					{
						cssClass = "radio"; //$NON-NLS-1$ 
					}
				}

				applyScrolling(styleObj, field);
				html.append("<div "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
//					html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCSSClassParameter(cssClass));
				html.append(">Multi radios</div>"); //$NON-NLS-1$ 
			}
				break;
			case Field.COMBOBOX :
			{
				ins = null;
//					if (ins == null)
//					{
//						ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE,DEFAULT_FIELD_PADDING);
//					}
				html.append("<select "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
//					html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append(">Combobox</select>"); //$NON-NLS-1$ 
			}
				break;
			case Field.CALENDAR :
			{
				html.append("<div "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append("style = 'overflow:hidden' "); //$NON-NLS-1$ 
				html.append("><table "); //$NON-NLS-1$ 
				TextualStyle inline = new TextualStyle();
				inline.setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("margin", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("padding", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("border-collapse", "collapse"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("table-layout", "fixed"); //$NON-NLS-1$ //$NON-NLS-2$
				html.append(inline.toString());
				html.append("><tr "); //$NON-NLS-1$ 
				inline.remove("border-collapse"); //$NON-NLS-1$ 
				inline.remove("table-layout"); //$NON-NLS-1$ 
				html.append(inline.toString());
				html.append("><td "); //$NON-NLS-1$ 
				html.append(inline.toString());
				html.append("><input type='text' servoy:id='datefield' "); //$NON-NLS-1$ 
				inline = new TextualStyle();
				inline.setProperty("border-style", "none"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("background-color", "transparent"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("margin", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("padding", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.copy("font-family", styleObj); //$NON-NLS-1$ 
				inline.copy("font-size", styleObj); //$NON-NLS-1$ 
				inline.copy("color", styleObj); //$NON-NLS-1$ 
				html.append(inline.toString());
				html.append(" /></td></tr></table></div>"); //$NON-NLS-1$ 
			}
				break;
			case Field.IMAGE_MEDIA :
			{
				applyScrolling(styleObj, field);
				html.append("<div "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
				//				html.append(getJavaScriptIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append('>');

				TextualStyle inline2 = new TextualStyle();
				inline2.setProperty("top", "1px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline2.setProperty("left", "1px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline2.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
				inline2.setProperty("cursor", "pointer"); //$NON-NLS-1$ //$NON-NLS-2$
				inline2.setProperty("background-color", "gray"); //$NON-NLS-1$ //$NON-NLS-2$
				inline2.setProperty("z-index", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				html.append("<img "); //$NON-NLS-1$ 
				html.append(inline2.toString());
				html.append(" border=0 servoy:id='save_icon' src='#' alt='Save' />"); //$NON-NLS-1$ 
				html.append("<img "); //$NON-NLS-1$ 
				inline2.setProperty("left", "17px"); //$NON-NLS-1$ //$NON-NLS-2$
				html.append(inline2.toString());
				html.append(" border=0 servoy:id='upload_icon' src='#' alt='Upload' />"); //$NON-NLS-1$ 
				html.append("<img "); //$NON-NLS-1$ 
				inline2.setProperty("left", "33px"); //$NON-NLS-1$ //$NON-NLS-2$
				html.append(inline2.toString());
				html.append(" border=0 servoy:id='remove_icon' src='#' alt='Remove' />"); //$NON-NLS-1$ 
//					html.append("<a ");
//					html.append(inline2.toString());
//					html.append(" servoy:id='upload' href='#' border=0><img servoy:id='upload_icon' src='#' alt='' /></a>");
//					html.append("<a ");
//					inline2.setProperty("left","16px");
//					html.append(inline2.toString());
//					html.append(" servoy:id='save' href='#' border=0><img servoy:id='save_icon' src='#' alt='' /></a>");

				TextualStyle inline = new TextualStyle();
				inline.setProperty("top", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("left", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
				inline.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$

				html.append("<input "); //$NON-NLS-1$ 
				html.append(inline.toString());
				html.append(getWicketIDParameter(field));
				html.append(getJavaScriptIDParameter(field));
				html.append("value='"); //$NON-NLS-1$ 
				html.append(field.getName());
				html.append("' "); //$NON-NLS-1$ 
				html.append("type='image' "); //$NON-NLS-1$ 
				html.append(" src='#' alt='' "); //$NON-NLS-1$ 
				html.append("/>"); //$NON-NLS-1$ 

//					html.append("<img ");
//					html.append(inline.toString());
//					html.append(" src='#' alt='' ");
//					html.append(getWicketIDParameter(field));
//					html.append(" />");

				html.append("</div>"); //$NON-NLS-1$ 
			}
				break;
			case Field.TYPE_AHEAD :
			default :
			case Field.TEXT_FIELD :
			{
				if (ins == null)
				{
					ins = new BorderAndPadding(DEFAULT_FIELD_BORDER_SIZE, DEFAULT_FIELD_PADDING);
				}
				html.append("<input "); //$NON-NLS-1$ 
				html.append(getWicketIDParameter(field));
//					html.append(getJavaScriptIDParameter(field));
				html.append(getDataProviderIDParameter(field));
				html.append(getCSSClassParameter("field")); //$NON-NLS-1$ 
				html.append("type='text' "); //$NON-NLS-1$ 
				if (field.getSelectOnEnter())
				{
					html.append("onfocus='this.select()'"); //$NON-NLS-1$ 
				}
				html.append("/>"); //$NON-NLS-1$ 
			}
				break;
		}
		applyTextProperties(field, styleObj);
		if (addWrapperDiv)
		{
			html.append("</div>"); //$NON-NLS-1$ 
			styleObj.setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
			styleObj.setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
			styleObj.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			applyLocationAndSize(field, styleObj, ins, startY, endY, form.getSize().width, enableAnchoring);
		}
	}

	private static void applyScrolling(TextualStyle styleObj, ISupportScrollbars field)
	{
		String overflow = "auto"; //$NON-NLS-1$ 
		if (field.getScrollbars() == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER + ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
		{
			overflow = "hidden"; //$NON-NLS-1$ 
		}
		styleObj.setProperty("overflow", overflow); //$NON-NLS-1$ 
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
			return "name='" + meta.getDataProviderID() + "' "; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ""; //$NON-NLS-1$ 
	}

	//returns space as last char
	private static String getCSSClassParameter(String servoy_css_type)
	{
		return "class='" + servoy_css_type + "' "; //$NON-NLS-1$ //$NON-NLS-2$ 

	}

	private static void applyLocationAndSize(ISupportBounds component, TextualStyle styleObj, BorderAndPadding ins, int startY, int endY, int formWidth,
		boolean enableAnchoring)
	{
		TextualCSS css = styleObj.getTextualCSS();
		ICSSBoundsHandler handler = css.getCSSBoundsHandler();
		handler.applyBounds(component, styleObj, ins == null ? new Insets(0, 0, 0, 0) : ins.getSum(), startY, endY, formWidth, enableAnchoring);
	}

	interface ICSSBoundsHandler
	{
		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring);
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
			sb.append("[border="); //$NON-NLS-1$ 
			if (border == null) sb.append("null"); //$NON-NLS-1$ 
			else sb.append(border.toString());
			sb.append(",padding="); //$NON-NLS-1$ 
			if (padding == null) sb.append("null"); //$NON-NLS-1$ 
			else sb.append(padding.toString());
			sb.append("]"); //$NON-NLS-1$ 
			return sb.toString();
		}
	}
	static class DefaultCSSBoundsHandler implements ICSSBoundsHandler
	{
		public static final DefaultCSSBoundsHandler INSTANCE = new DefaultCSSBoundsHandler();

		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring)
		{
			int y = component.getLocation().y;
//			if (ins != null) y += ins.top;
			int x = component.getLocation().x;
//			if (ins != null) x += ins.left;
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
			if (top != -1) styleObj.setProperty("top", top + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			if (left != -1) styleObj.setProperty("left", left + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			if (width != -1) styleObj.setProperty("width", width + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			if (height != -1) styleObj.setProperty("height", height + "px"); //$NON-NLS-1$ //$NON-NLS-2$

			if (enableAnchoring)
			{
				boolean anchoredTop = (anchorFlags & IAnchorConstants.NORTH) != 0;
				boolean anchoredRight = (anchorFlags & IAnchorConstants.EAST) != 0;
				boolean anchoredBottom = (anchorFlags & IAnchorConstants.SOUTH) != 0;
				boolean anchoredLeft = (anchorFlags & IAnchorConstants.WEST) != 0;

				if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
				if (!anchoredTop && !anchoredBottom) anchoredTop = true;

				if (anchoredTop) styleObj.setProperty("top", (top) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
				else styleObj.remove("top"); //$NON-NLS-1$ 
				if (anchoredBottom) styleObj.setProperty("bottom", (partEndY - partStartY - top - offsetHeight) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
				else styleObj.remove("bottom"); //$NON-NLS-1$ 
				if (!anchoredTop || !anchoredBottom) styleObj.setProperty("height", height + "px"); //$NON-NLS-1$ //$NON-NLS-2$
				else styleObj.remove("height"); //$NON-NLS-1$ 
				if (anchoredLeft) styleObj.setProperty("left", left + "px"); //$NON-NLS-1$ //$NON-NLS-2$
				else styleObj.remove("left"); //$NON-NLS-1$ 
				if (anchoredRight) styleObj.setProperty("right", (partWidth - left - offsetWidth) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
				else styleObj.remove("right"); //$NON-NLS-1$ 
				if (!anchoredLeft || !anchoredRight) styleObj.setProperty("width", width + "px"); //$NON-NLS-1$ //$NON-NLS-2$
				else styleObj.remove("width"); //$NON-NLS-1$ 
				styleObj.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				if ((top != -1) || (left != -1)) styleObj.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	static class NoLocationCSSBoundsHandler extends DefaultCSSBoundsHandler
	{
		public static final ICSSBoundsHandler INSTANCE = new NoLocationCSSBoundsHandler();

		@Override
		public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
			boolean enableAnchoring)
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
			boolean enableAnchoring)
		{
			int y = component.getLocation().y + offset;
//			if (ins != null) y += ins.top;
			int x = component.getLocation().x;
//			if (ins != null) x += ins.left;
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
			styleObj.setProperty("text-align", getHorizontalAlignValue(component.getHorizontalAlignment())); //$NON-NLS-1$ 
		}
	}

	private static String createInsetsText(Insets i)
	{
		StringBuffer pad = new StringBuffer();
		pad.append(i.top);
		pad.append("px "); //$NON-NLS-1$ 
		pad.append(i.right);
		pad.append("px "); //$NON-NLS-1$ 
		pad.append(i.bottom);
		pad.append("px "); //$NON-NLS-1$ 
		pad.append(i.left);
		pad.append("px"); //$NON-NLS-1$ 
		return pad.toString();
	}

	private static String getHorizontalAlignValue(int ha)
	{
		switch (ha)
		{
			case ISupportTextSetup.CENTER :
				return "center"; //$NON-NLS-1$ 
			case ISupportTextSetup.RIGHT :
				return "right"; //$NON-NLS-1$ 
			case ISupportTextSetup.LEFT :
			default :
				return "left"; //$NON-NLS-1$ 
		}
	}

//	private static String getVerticalAlignValue(int va)
//	{
//		switch (va)
//		{
//			case ISupportTextSetup.CENTER :
//				return "middle"; //$NON-NLS-1$ 
//			case ISupportTextSetup.TOP :
//				return "top"; //$NON-NLS-1$ 
//			case ISupportTextSetup.BOTTOM :
//			default :
//				return "bottom"; //$NON-NLS-1$ 
//		}
//	}

	protected static BorderAndPadding applyBaseComponentProperties(BaseComponent component, Form form, TextualStyle styleObj, Insets defaultPadding,
		Insets defaultBorder, IServiceProvider sp)
	{

		Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo = ComponentFactory.getStyleForBasicComponent(sp, component, form);

		Insets insetsBorder = null;
		if (component.getBorderType() != null)
		{
			insetsBorder = ComponentFactoryHelper.createBorderCSSProperties(component.getBorderType(), styleObj);
		}

		Insets insetsMargin = null;
		if (component instanceof ISupportTextSetup)
		{
			insetsMargin = ((ISupportTextSetup)component).getMargin();
		}

		if (styleInfo != null)
		{
			FixedStyleSheet ss = styleInfo.getLeft();
			javax.swing.text.Style s = styleInfo.getRight();
			if (ss != null && s != null)
			{
				Enumeration< ? > attributeNames = s.getAttributeNames();
				while (attributeNames.hasMoreElements())
				{
					Object name = attributeNames.nextElement();
					if (name == StyleConstants.NameAttribute) continue;

					String s_attr = name.toString();
					// Skip margin related attributes. Margin is computed separately below, and rendered as padding.
					if (s_attr.toLowerCase().contains("margin")) continue; //$NON-NLS-1$ 
					Object val = s.getAttribute(name);
					if (s_attr.equals("font-size")) //$NON-NLS-1$ 
					{
						String tmp = val.toString();
						if (tmp.endsWith("px")) //$NON-NLS-1$ 
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
							styleObj.setProperty(s_attr, size + "px"); //$NON-NLS-1$ 
						}
						else
						{
							int size = 0;
							if (tmp.endsWith("pt")) //$NON-NLS-1$ 
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
							styleObj.setProperty(s_attr, size + "pt"); //$NON-NLS-1$ 
						}
					}
					else
					{
						if (val.toString() != null) styleObj.setProperty(s_attr, val.toString(), false);
					}
				}
				if (insetsBorder == null)
				{
					if (s.getAttribute(CSS.Attribute.BORDER) != null || s.getAttribute(CSS.Attribute.BORDER_STYLE) != null ||
						s.getAttribute(CSS.Attribute.BORDER_TOP) != null || s.getAttribute(CSS.Attribute.BORDER_TOP_WIDTH) != null)
					{
						Border b = ss.getBorder(s);
						if (b != null)
						{
							// TODO??
							try
							{
								insetsBorder = b.getBorderInsets(null);
							}
							catch (Exception e)
							{
								Debug.error("for border " + b + " no insets could be extracted.", e); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					}
				}
				if (insetsMargin == null)
				{
					if (s.getAttribute(CSS.Attribute.MARGIN) != null || s.getAttribute(CSS.Attribute.MARGIN_TOP) != null)
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
			styleObj.setProperty("color", PersistHelper.createColorString(component.getForeground())); //$NON-NLS-1$ 
		}

		if (component.getTransparent())
		{
			styleObj.setProperty("background-color", "transparent"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if (component.getBackground() != null)
		{
			styleObj.setProperty("background-color", PersistHelper.createColorString(component.getBackground())); //$NON-NLS-1$ 
		}

		if (insetsBorder == null) insetsBorder = defaultBorder;

		if (insetsMargin == null && defaultPadding != null) insetsMargin = defaultPadding;

		BorderAndPadding bp = new BorderAndPadding(insetsBorder, insetsMargin);
		styleObj.setProperty("padding", createInsetsText(bp.getPadding())); //$NON-NLS-1$ 
		return bp;
	}

	//returns space as last char
	private static String getJavaScriptIDParameter(IPersist meta)
	{
		return getJavaScriptIDParameter(meta, ""); //$NON-NLS-1$ 
	}

	private static String getJavaScriptIDParameter(IPersist meta, String prefix)
	{
		return "id='" + prefix + ComponentFactory.getWebID(meta) + "' "; //$NON-NLS-1$ //$NON-NLS-2$
	}

	//returns space as last char
	private static String getWicketIDParameter(IPersist meta)
	{
		return getWicketIDParameter(meta, ""); //$NON-NLS-1$ 
	}

	private static String getWicketIDParameter(IPersist meta, String prefix)
	{
		return "servoy:id='" + prefix + ComponentFactory.getWebID(meta) + "' "; //$NON-NLS-1$ //$NON-NLS-2$
	}
}