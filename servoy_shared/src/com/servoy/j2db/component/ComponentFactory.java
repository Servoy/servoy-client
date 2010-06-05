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
package com.servoy.j2db.component;


import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.CSS;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.ITypedColumnConverter;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Shape;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.ui.IAccessible;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IRect;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStandardLabel;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.OpenProperties;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.XMLDecoder;
import com.servoy.j2db.util.gui.MyImageIcon;
import com.servoy.j2db.util.gui.OrientationApplier;
import com.servoy.j2db.util.gui.TabLikeBorder;

/**
 * Create UI objects based on repository objects
 * 
 * @author jblok, jcompagner
 */
@SuppressWarnings("nls")
public class ComponentFactory
{
	public static final String WEB_ID_PREFIX = "sv_";
	public static final int RTF_AREA = 7;
	public static final int HTML_AREA = 8;

	/**
	 * Grouping of elements in client.
	 */
	public static final String GROUPID_COMPONENT_PROPERTY = "groupId";


	public static boolean paintSampleData = false;
	private static Map<Object, IconHolder> lstIcons = new WeakHashMap<Object, IconHolder>();
	private static ConcurrentMap<Style, FixedStyleSheet> parsedStyles = new ConcurrentHashMap<Style, FixedStyleSheet>();

	private static Boolean element_name_as_uid_prefix;
	private static Boolean element_id_as_uid;

	public static String getWebID(Form form, IPersist persist)
	{
		String partId = getWebIDInternal(persist);
		if (form != null)
		{
			IPersist persistForm = persist.getAncestor(IRepository.FORMS);
			if (persistForm != null && form.getID() != persistForm.getID()) partId = new StringBuffer(partId).append("_").append(getWebIDInternal(form)).toString();
		}

		return partId;
	}

	private static String getWebIDInternal(IPersist meta)
	{
		String prefix = WEB_ID_PREFIX; //to stay javascript id ref compatible 

		if (element_name_as_uid_prefix == null)
		{
			Settings s = Settings.getInstance();
			element_name_as_uid_prefix = Boolean.valueOf(Utils.getAsBoolean(s.getProperty("servoy.webclient.templates.element_name_as_uid_prefix")));
		}
		if (element_name_as_uid_prefix == Boolean.TRUE && meta instanceof ISupportName)
		{
			String name = ((ISupportName)meta).getName();
			if (name != null && name.trim().length() != 0)
			{
				prefix = name + '_';
			}
		}

		if (element_id_as_uid == null)
		{
			Settings s = Settings.getInstance();
			element_id_as_uid = Boolean.valueOf(Utils.getAsBoolean(s.getProperty("servoy.webclient.templates.use_local_ids")));
		}
		String uid = null;
		if (element_id_as_uid == Boolean.TRUE)
		{
			uid = Integer.toHexString(meta.getID());
		}
		else
		{
			uid = meta.getUUID().toString();
			uid = Utils.stringReplace(uid, "-", "_");
		}

		return prefix + uid;
	}

	public static String stripIllegalCSSChars(String id)
	{
		return Utils.stringReplace(Utils.stringReplace(id, "-", "_"), "$", "__");
	}

	public static Object getBeanDesignInstance(IApplication application, FlattenedSolution flattenedSolution, Bean bean, Form form)
	{
		Object beanDesignComponent = null;
		synchronized (flattenedSolution)
		{
			beanDesignComponent = flattenedSolution.getBeanDesignInstance(bean);
			if (beanDesignComponent == null)
			{
				createDesignComponent(application, flattenedSolution, bean, form);
				beanDesignComponent = flattenedSolution.getBeanDesignInstance(bean);
			}
		}

		return beanDesignComponent;
	}

	public static Component createDesignComponent(IApplication application, FlattenedSolution flattenedSolution, IPersist meta, Form form)
	{
		Component c = null;
		if (meta.getTypeID() == IRepository.BEANS)
		{
			// can cast, design should always be a swing
			IComponent comp = createBean(application, form, (Bean)meta, flattenedSolution);
			if (comp instanceof InvisibleBean)
			{
				c = (InvisibleBean)comp;
			}
			else if (comp instanceof VisibleBean)
			{
				c = ((VisibleBean)comp).getDelegate();
			}
			else if (comp instanceof Component)
			{
				c = (Component)comp;
			}
		}
		else
		{
			switch (meta.getTypeID())
			{
				case IRepository.TABPANELS :
					JComponent retval = null;
					int orient = ((TabPanel)meta).getTabOrientation();
					if (orient == -1)
					{
						// Designer all always real components!!
						retval = (JComponent)application.getItemFactory().createLabel(null, "Tabless panel, for JavaScript use");
						((IStandardLabel)retval).setHorizontalAlignment(SwingConstants.CENTER);
						applyBasicComponentProperties(application, (IComponent)retval, (BaseComponent)meta, getStyleForBasicComponent(application,
							(BaseComponent)meta, form));
					}
					else
					{
						ComponentJTabbedPane tabs = new ComponentJTabbedPane();
						applyBasicComponentProperties(application, tabs, (BaseComponent)meta, getStyleForBasicComponent(application, (BaseComponent)meta, form));
						tabs.addTab("position example", new JLabel("form will appear here", SwingConstants.CENTER)); //$NON-NLS-2$
						tabs.addTab("position 2", new JLabel("another form showup here", SwingConstants.CENTER));
						if (orient == SwingConstants.TOP || orient == SwingConstants.LEFT || orient == SwingConstants.BOTTOM || orient == SwingConstants.RIGHT)
						{
							tabs.setTabPlacement(orient);
						}
						retval = tabs;
					}
					OrientationApplier.setOrientationToAWTComponent(retval, application.getLocale(), application.getSolution().getTextOrientation());
					return retval;

				default :
					IDataProviderLookup dataProviderLookup = application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), form);
					c = (Component)createComponentEx(application, form, meta, dataProviderLookup, null, false);
			}
		}
		if (c instanceof JComponent)
		{
			((JComponent)c).setDoubleBuffered(false);
		}
		if (c instanceof IDisplayData && paintSampleData)
		{
			if (form != null)
			{
				try
				{
					IFoundSet fs = application.getFoundSetManager().getSharedFoundSet(form.getDataSource());
					if (fs != null && fs.getSize() == 0) fs.loadAllRecords();
					if (fs != null && fs.getSize() > 0)
					{
						IRecord record = fs.getRecord(0);
						IDisplayData data = (IDisplayData)c;
						Object value = record.getValue(data.getDataProviderID());
						if (value == Scriptable.NOT_FOUND)
						{
							ScriptVariable variable = form.getScriptVariable(data.getDataProviderID());
							if (variable != null) value = variable.getDefaultValue();
						}
						data.setValueObject(value);
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		else if (meta instanceof GraphicalComponent && ((GraphicalComponent)meta).getDisplaysTags() && c instanceof ILabel)
		{
			((ILabel)c).setText(((GraphicalComponent)meta).getText());
		}
		OrientationApplier.setOrientationToAWTComponent(c, application.getLocale(), application.getSolution().getTextOrientation());
		return c;//removeTransparencyAndScrolling(c);
	}

//	private static Component removeTransparencyAndScrolling(Component c)
//	{
//		//remove any transparency of scrollpanes
//		if (c instanceof Container)
//		{
//			Component[] all = ((Container)c).getComponents();
//			for(int i = 0 ; i < all.length ; i++)
//			{
//				Component cold = all[i];
//				Component cnew = removeTransparencyAndScrolling(cold);
//				if (!cold.equals(cnew))
//				{
//					((Container)c).remove(cold);
//					((Container)c).add(cnew);
//				}
//			}
//		}
//		if (c instanceof JScrollPane)
//		{
//			c = removeTransparencyAndScrolling(((JScrollPane)c).getViewport().getView());
//		}
//		if (c instanceof JViewport)
//		{
//			c = removeTransparencyAndScrolling(((JViewport)c).getView());
//		}
//		if (c instanceof JComponent)
//		{
//			((JComponent)c).setOpaque(true);
//		}
//		return c;
//	}

	/**
	 * Create a component
	 * 
	 * @param meta the definition
	 * @param el the event listener such as action,mouse event listeners, can be null (Example:makes possible for button to call script)
	 */
	public static IComponent createComponent(IApplication application, Form form, IPersist meta, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing)
	{
		IComponent c = createComponentEx(application, form, meta, dataProviderLookup, el, printing);

		// set groupID property
		if (meta instanceof IFormElement && ((IFormElement)meta).getGroupID() != null)
		{
			String groupId = ((IFormElement)meta).getGroupID();
			if (groupId != null)
			{
				setComponentProperty(application, c, GROUPID_COMPONENT_PROPERTY, groupId);
			}
		}

		// Extra call so that focusable is user set...
		if (c instanceof Component)
		{
			Component comp = (Component)c;
			if (comp.isFocusable()) comp.setFocusable(true);
			OrientationApplier.setOrientationToAWTComponent(comp, application.getLocale(), application.getSolution().getTextOrientation());
		}
		int access = application.getFlattenedSolution().getSecurityAccess(meta.getUUID());
		if (access != -1)
		{
			boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
			if (!b_visible) c.setComponentVisible(false);
			if (c instanceof IAccessible)
			{
				boolean b_accessible = ((access & IRepository.ACCESSIBLE) != 0);
				if (!b_accessible) ((IAccessible)c).setAccessible(false);
			}
		}

		//special case requested by ayton (have own security interface)
		if (c instanceof ITabPanel && meta instanceof TabPanel)
		{
			try
			{
				int i = 0;
				Iterator<IPersist> it = ((TabPanel)meta).getTabs();
				while (it.hasNext())
				{
					Tab t = (Tab)it.next();
					int access1 = application.getFlattenedSolution().getSecurityAccess(t.getUUID());
					if (access1 != -1)
					{
						boolean b_accessible = ((access1 & IRepository.ACCESSIBLE) != 0);
						boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
						if (!b_accessible || !b_visible) ((ITabPanel)c).setTabEnabledAt(i, false);
					}
					i++;
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return c;
	}

	public static void setComponentProperty(IApplication applicatiom, Object component, Object key, Serializable value)
	{
		applicatiom.getItemFactory().setComponentProperty(component, key, value);
	}

	public static Serializable getComponentProperty(IApplication application, Object component, Object key)
	{
		return application.getItemFactory().getComponentProperty(component, key);
	}


	private static IComponent createComponentEx(IApplication application, Form form, IPersist meta, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing)
	{
		IComponent comp = null;
		switch (meta.getTypeID())
		{
			case IRepository.FIELDS :
				comp = createField(application, form, (Field)meta, dataProviderLookup, el, printing);
				break;

			case IRepository.GRAPHICALCOMPONENTS :
				comp = createGraphicalComponent(application, form, (GraphicalComponent)meta, el, dataProviderLookup);
				break;

			case IRepository.RECTSHAPES :
				comp = createRectangle(application, form, (RectShape)meta);
				break;

			case IRepository.SHAPES :
				comp = createShape(application, form, (Shape)meta);
				break;

			case IRepository.PORTALS :
				comp = createPortal(application, form, (Portal)meta, dataProviderLookup, el, printing);
				break;

			case IRepository.PARTS :
				comp = createPart(application, (Part)meta);
				break;

			case IRepository.TABPANELS :
				TabPanel tabPanelMeta = (TabPanel)meta;
				int orient = tabPanelMeta.getTabOrientation();
				if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) comp = createSplitPane(application, form, tabPanelMeta);
				else comp = createTabPanel(application, form, tabPanelMeta, el);
				break;

			case IRepository.TABS :
				comp = createTab(application, (Tab)meta);
				break;

			case IRepository.BEANS :
				comp = createBean(application, form, (Bean)meta, null);
				break;

			default :
				comp = application.getItemFactory().createLabel((meta instanceof ISupportName ? ((ISupportName)meta).getName() : null),
					"ComponentFactory:unkown type " + meta.getTypeID());
		}

		if (comp instanceof JComponent)
		{
			((JComponent)comp).putClientProperty("Id", ComponentFactory.getWebID(form, meta));
		}
		return comp;
	}

	public static FixedStyleSheet getCSSStyle(Style s)
	{
		if (s == null) return null;

		FixedStyleSheet ss = parsedStyles.get(s);
		if (ss == null)
		{
			ss = new FixedStyleSheet();
			try
			{
				ss.addRule(s.getCSSText());
			}
			catch (Exception e)
			{
				Debug.error(e);//parsing can fail in java 1.5
			}
			parsedStyles.put(s, ss);
		}
		return ss;
	}

	public static FixedStyleSheet getCSSStyleForForm(IServiceProvider sp, Form form)
	{
		return getCSSStyle(getStyleForForm(sp, form));
	}

	public static String getOverriddenStyleName(IServiceProvider sp, Form form)
	{
		if (sp != null)
		{
			@SuppressWarnings("unchecked")
			Map<String, String> overridenStyles = (Map<String, String>)sp.getRuntimeProperties().get(IServiceProvider.RT_OVERRIDESTYLE_CACHE);
			Style repos_style = sp.getFlattenedSolution().getStyleForForm(form);
			if (repos_style != null && sp.getFlattenedSolution().isUserStyle(repos_style))
			{
				return repos_style.getName();
			}
			String overridden = null;
			if (repos_style != null && overridenStyles != null && (overridden = overridenStyles.get(repos_style.getName())) != null)
			{
				return overridden;
			}
		}
		return null;
	}

	private static Style getStyleForForm(IServiceProvider sp, Form form)
	{
		Style repos_style = null;
		if (sp != null)
		{
			@SuppressWarnings("unchecked")
			Map<String, String> overridenStyles = (Map<String, String>)sp.getRuntimeProperties().get(IServiceProvider.RT_OVERRIDESTYLE_CACHE);
			repos_style = sp.getFlattenedSolution().getStyleForForm(form, overridenStyles);
		}
		else
		{
			repos_style = FlattenedSolution.loadStyleForForm(form);
		}
		return repos_style;
	}

	public static Pair<FixedStyleSheet, javax.swing.text.Style> getStyleForBasicComponent(IServiceProvider sp, BaseComponent bc, Form form)
	{
		return getStyleForBasicComponentInternal(sp, bc, form, new HashSet<Integer>());
	}

	public static Pair<FixedStyleSheet, javax.swing.text.Style> getStyleForBasicComponentInternal(IServiceProvider sp, BaseComponent bc, Form form,
		Set<Integer> visited)
	{
		if (bc == null || sp == null) return null;

		// Protection agains cycle in form inheritance hierarchy.
		if (visited.contains(new Integer(form.getID()))) return null;
		visited.add(new Integer(form.getID()));

		Style repos_style = getStyleForForm(sp, form);
		Pair<FixedStyleSheet, javax.swing.text.Style> pair = null;

		if (repos_style != null)
		{
			FixedStyleSheet ss = getCSSStyle(repos_style);

			String lookupName = getLookupName(bc);

			String formLookup = "";
			ISupportChilds parent = bc.getParent();
			if (parent instanceof Form)
			{
				String styleClass = ((Form)parent).getStyleClass();
				if (styleClass != null && styleClass.length() != 0)
				{
					formLookup = "form." + styleClass;
				}
				else
				{
					formLookup = "form";
				}
			}
			else if (parent instanceof Portal)
			{
				String styleClass = ((Portal)parent).getStyleClass();
				if (styleClass != null && styleClass.length() != 0)
				{
					formLookup = "portal." + styleClass;
				}
				else
				{
					formLookup = "portal";
				}
				parent = ((Portal)parent).getParent();
				if (parent instanceof Form)
				{
					styleClass = ((Form)parent).getStyleClass();
					if (styleClass != null && styleClass.length() != 0)
					{
						formLookup = "form." + styleClass + ' ' + formLookup;
					}
					else
					{
						formLookup = "form " + formLookup;
					}
				}
			}


			javax.swing.text.Style s = null;
			String styleClass = bc.getStyleClass();
			if (lookupName.equals("check") || lookupName.equals("combobox") || "radio".equals(lookupName))
			{
				if (styleClass != null && styleClass.length() != 0)
				{
					lookupName += '.' + styleClass;
				}
				lookupName = formLookup + ' ' + lookupName;
				s = ss.getRule(lookupName);

				if (s.getAttributeCount() > 0) return new Pair<FixedStyleSheet, javax.swing.text.Style>(ss, s);
				else lookupName = "field";
			}

			if (styleClass != null && styleClass.length() != 0)
			{
				lookupName += '.' + styleClass;
			}
			lookupName = formLookup + ' ' + lookupName;
			s = ss.getRule(lookupName);

			pair = new Pair<FixedStyleSheet, javax.swing.text.Style>(ss, s);
			//see BoxPainter for getBorder/getInsets/getLength examples
		}
		if ((pair == null || pair.getRight() == null || (pair.getRight()).getAttributeCount() == 0))
		{
			List<Form> formHierarchy = sp.getFlattenedSolution().getFormHierarchy(form);
			for (int i = 1; i < formHierarchy.size(); i++)
			{
				pair = getStyleForBasicComponentInternal(sp, bc, formHierarchy.get(i), visited);
				if (pair != null && pair.getRight() != null && (pair.getRight()).getAttributeCount() != 0)
				{
					break;
				}
			}
		}
		return pair;
	}

	public static final String[] LOOKUP_NAMES = { "button", "check", "combobox", "field", "form", "label", "portal", "radio", "tabpanel" };

	/**
	 * @param bc
	 * @return
	 */
	public static String getLookupName(BaseComponent bc)
	{
		String lookupName = "root";
		if (bc instanceof Field)
		{
			int fieldType = ((Field)bc).getDisplayType();
			switch (fieldType)
			{
				case Field.CHECKS :
					lookupName = "check";
					break;
				case Field.RADIOS :
					lookupName = "radio";
					break;
				case Field.COMBOBOX :
					lookupName = "combobox";
					break;
				default :
					lookupName = "field";
			}
		}
		else if (bc instanceof Portal)
		{
			lookupName = "portal";
		}
		else if (bc instanceof TabPanel)
		{
			lookupName = "tabpanel";
		}
		else if (bc instanceof GraphicalComponent &&
			(((GraphicalComponent)bc).getOnActionMethodID() > 0 || ((GraphicalComponent)bc).getOnActionMethodID() == -1))
		{
			if (((GraphicalComponent)bc).getShowClick())
			{
				lookupName = "button";
			}
			else
			{
				lookupName = "label";
			}
		}
		else if (bc instanceof GraphicalComponent && ((GraphicalComponent)bc).getOnActionMethodID() <= 0)
		{
			lookupName = "label";
		}
		return lookupName;
	}

	public static void applyBasicComponentProperties(IApplication application, IComponent c, BaseComponent bc,
		Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo)
	{
		// flag for border set by style config
		boolean isBorderStyle = false;

		c.setOpaque(true); // by default it is not transparent
		//apply any style
		if (styleInfo != null)
		{
			FixedStyleSheet ss = styleInfo.getLeft();
			javax.swing.text.Style s = styleInfo.getRight();
			if (ss != null && s != null)
			{
				if (s.getAttribute(CSS.Attribute.COLOR) != null)
				{
					Color cfg = ss.getForeground(s);
					if (cfg != null) c.setForeground(cfg);
				}
				Object sbackground_color = s.getAttribute(CSS.Attribute.BACKGROUND_COLOR);
				if (sbackground_color != null)
				{
					if ("transparent".equals(sbackground_color.toString()))
					{
						c.setOpaque(false);
					}
					else
					{
						Color cbg = ss.getBackground(s);
						if (cbg != null) c.setBackground(cbg);
					}
				}
				//else c.setOpaque(false); // no background-color means transparent
				if (s.getAttribute(CSS.Attribute.FONT_FAMILY) != null || s.getAttribute(CSS.Attribute.FONT) != null)
				{
					Font f = ss.getFont(s);
					if (f != null) c.setFont(f);
				}
				if (s.getAttribute(CSS.Attribute.BORDER) != null || s.getAttribute(CSS.Attribute.BORDER_STYLE) != null ||
					s.getAttribute(CSS.Attribute.BORDER_TOP) != null || s.getAttribute(CSS.Attribute.BORDER_TOP_WIDTH) != null)
				{
					Border b = ss.getBorder(s);
					if (b != null)
					{
						c.setBorder(b);
						isBorderStyle = true;
					}
				}
				if (s.getAttribute(CSS.Attribute.MARGIN) != null || s.getAttribute(CSS.Attribute.MARGIN_TOP) != null)
				{
					Insets i = ss.getMargin(s);
					if (i != null && c instanceof IButton) ((IButton)c).setMargin(i);
				}
			}
		}

//		java.awt.Point loc = bc.getLocation();
//		if (loc != null) c.setLocation(loc);
		java.awt.Dimension dim = bc.getSize();
		if (dim != null) c.setSize(bc.getSize());
		javax.swing.border.Border border = ComponentFactoryHelper.createBorder(bc.getBorderType());

		if (c instanceof JCheckBox/* DataCheckBox */&& (border != null || isBorderStyle))
		{
			((JCheckBox)c).setBorderPainted(true);
			((JCheckBox)c).setBorderPaintedFlat(false);
		}

		if (border != null)
		{
			if (border instanceof TitledBorder)
			{
				// apple bug.. i have to set the font again (as new!!)
				TitledBorder tb = (TitledBorder)border;
				Font f = tb.getTitleFont();
				if (f != null)
				{
					tb.setTitleFont(new Font(f.getName(), f.getStyle(), f.getSize()));
				}
				c.setBorder(border);
			}
			else
			{
				c.setBorder(border);
			}
		}


//		if (c instanceof IDelegate)
//		{
//			c = (JComponent)((IDelegate)c).getDelegate(); 
//		}

		String fontString = bc.getFontType();
		if (fontString != null)
		{
			Font f = PersistHelper.createFont(fontString);
			if (f != null) c.setFont(f);
		}
		java.awt.Color bg = bc.getBackground();
		if (bg != null) c.setBackground(bg);
		java.awt.Color fg = bc.getForeground();
		if (fg != null) c.setForeground(fg);
		String name = bc.getName();
		if (name != null) c.setName(name);
		if (bc.getTransparent()) c.setOpaque(false); // only use component property value if it is "checked" to be transparent
	}

	private static IComponent createBean(IApplication application, Form form, Bean bean, FlattenedSolution flattenedSolution)
	{
		IComponent c = null;
		try
		{
			Object obj = getBeanInstanceFromXML(application, bean);

			if (flattenedSolution != null && obj != null)
			{
				flattenedSolution.setBeanDesignInstance(bean, obj);
			}

			if (obj instanceof Component)
			{
				((Component)obj).setName(bean.getName());
			}
			else if (obj instanceof IComponent)
			{
				((IComponent)obj).setName(bean.getName());
			}

			if (obj instanceof IServoyAwareBean)
			{
				((IServoyAwareBean)obj).initialize((IClientPluginAccess)application.getPluginAccess());
			}

			if (obj instanceof IServoyBeanFactory)
			{
				obj = ((IServoyBeanFactory)obj).getBeanInstance(application.getApplicationType(), (IClientPluginAccess)application.getPluginAccess(),
					new Object[] { ComponentFactory.getWebID(form, bean) });
			}

			if (obj instanceof Applet)
			{
				((FormManager)application.getFormManager()).initializeApplet((Applet)obj, bean.getSize());
			}

			if (obj == null)
			{
				c = application.getItemFactory().createLabel(ComponentFactory.getWebID(form, bean), "bean missing " + bean.getBeanClassName());
			}
			else if (!(obj instanceof java.awt.Component) && !(obj instanceof IComponent))
			{
				c = application.getItemFactory().createInvisibleBean(ComponentFactory.getWebID(form, bean), obj);
			}
			else if (!(obj instanceof IComponent))
			{
				c = application.getItemFactory().createBeanHolder(ComponentFactory.getWebID(form, bean), (Component)obj);
			}
			else
			{
				c = (IComponent)obj;
			}

			if (c != null) c.setName(bean.getName());
			java.awt.Point loc = bean.getLocation();
			if (loc != null && c != null) c.setLocation(loc);
			java.awt.Dimension dim = bean.getSize();
			if (dim != null && c != null) c.setSize(dim);
		}
		catch (Throwable e)//sometimes setting size or location throws exception or even error...create label instead
		{
			Debug.error(e);
			c = application.getItemFactory().createLabel(bean.getName(), "error acessing bean" + bean.getBeanClassName());
			java.awt.Point loc = bean.getLocation();
			if (loc != null) c.setLocation(loc);
			java.awt.Dimension dim = bean.getSize();
			if (dim != null) c.setSize(bean.getSize());
		}

		return c;
	}

	/**
	 * Returns the bean Instance.
	 * 
	 * @return Object
	 */
	public static Object getBeanInstanceFromXML(IApplication application, Bean bean) throws Exception
	{
		Object retValue = null;
		String beanXML = bean.getBeanXML();
		String beanClassName = bean.getBeanClassName();
		ClassLoader bcl = application.getBeanManager().getClassLoader();
		ClassLoader saveCL = null;
		if (bcl != null)
		{
			if (!application.isRunningRemote())
			{
				//must create an temp instace to get it to work
				retValue = bcl.loadClass(beanClassName).newInstance();

				//TODO:remove if sun bug 4676532 is fixed (jdk 1.5)
				saveCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(bcl);
			}
		}
		if (beanXML != null && beanXML.length() != 0)
		{
			try
			{
				XMLDecoder decoder = new XMLDecoder(beanXML);
				retValue = decoder.readObject();
				decoder.close();
			}
			catch (Throwable e)
			{
				Debug.error(e);
			}
		}
		else if (retValue == null)
		{
			if (bcl != null) retValue = bcl.loadClass(beanClassName).newInstance();
			else retValue = ComponentFactory.class.getClassLoader().loadClass(beanClassName).newInstance();
		}

		if (!application.isRunningRemote())
		{
			Thread.currentThread().setContextClassLoader(saveCL);
		}
		return retValue;
	}

	public static void updateBeanWithItsXML(final Bean bean, final Object beanDesignInstance)
	{
		//-----save to XML------------------------
		if (beanDesignInstance != null)
		{
			Runnable run = new Runnable()
			{
				/**
				 * @see java.lang.Runnable#run()
				 */
				public void run()
				{
					try
					{
						ByteArrayOutputStream osw = new ByteArrayOutputStream();

						//TODO:remove if sun bug 4676532 is fixed (jdk 1.5)
						ClassLoader saveCL = Thread.currentThread().getContextClassLoader();
						ClassLoader cl = beanDesignInstance.getClass().getClassLoader();
						try
						{
							if (cl != null)
							{
								Thread.currentThread().setContextClassLoader(cl);
							}

							XMLEncoder encoder = new XMLEncoder(osw);
							encoder.writeObject(beanDesignInstance);
							encoder.close();
							Debug.trace("Bean XML: " + osw.toString());
							bean.setBeanXML(osw.toString());
						}
						finally
						{
							Thread.currentThread().setContextClassLoader(saveCL);
						}
					}
					catch (Throwable e)
					{
						//never ever throw something on the event thread, it does not releases locks!
						Debug.error(e);//what todo unable to save the bean...
					}
				}
			};

			try
			{
				if (SwingUtilities.isEventDispatchThread())
				{
					run.run();
				}
				else
				{
					SwingUtilities.invokeAndWait(run);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	private static IComponent createRectangle(IApplication application, Form form, RectShape rec)
	{
		IRect panel = application.getItemFactory().createRect(getWebID(form, rec), rec.getShapeType());
//		panel.setOpaque(!rec.getTransparent());
		panel.setLineWidth(rec.getLineSize());
		panel.setRadius(rec.getRoundedRadius());
//		JLabel panel = new JLabel("xxx");
		applyBasicComponentProperties(application, panel, rec, getStyleForBasicComponent(application, rec, form));
		return panel;
	}

	private static IComponent createShape(IApplication application, Form form, Shape rec)
	{
		IComponent panel = application.getItemFactory().createShape(getWebID(form, rec), rec);
		//ShapePainter panel = new ShapePainter(application, rec.getShapeType(),rec.getLineSize(),rec.getPolygon());
//		panel.setOpaque(!rec.getTransparent());
		applyBasicComponentProperties(application, panel, rec, getStyleForBasicComponent(application, rec, form));
// why did we do this? it breaks printslide dus to the fact it is to large and intersect with stuff
//		Point p = rec.getLocation();
//		Dimension d = panel.getSize();
//		panel.setSize(new Dimension(p.x+d.width+rec.getLineSize(),p.y+d.height+rec.getLineSize()));
		return panel;
	}

	@SuppressWarnings("unchecked")
	public static IValueList getRealValueList(IServiceProvider application, ValueList valuelist, boolean useSoftCacheForCustom, int type, String format,
		String dataprovider)
	{
		IValueList list = null;
		if (valuelist != null &&
			(valuelist.getValueListType() == ValueList.CUSTOM_VALUES || (valuelist.getValueListType() == ValueList.DATABASE_VALUES && valuelist.getDatabaseValuesType() == ValueList.TABLE_VALUES)))//reuse,those are static,OTHERS not!
		{
			WeakHashMap<ValueList, Object> hmValueLists = (WeakHashMap<ValueList, Object>)J2DBGlobals.getServiceProvider().getRuntimeProperties().get(
				IServiceProvider.RT_VALUELIST_CACHE);
			if (hmValueLists == null)
			{
				hmValueLists = new WeakHashMap<ValueList, Object>();
				J2DBGlobals.getServiceProvider().getRuntimeProperties().put(IServiceProvider.RT_VALUELIST_CACHE, hmValueLists);
			}

			Object object = hmValueLists.get(valuelist);
			if (object instanceof SoftReference< ? >)
			{
				SoftReference<IValueList> sr = (SoftReference<IValueList>)object;
				list = sr.get();
				// if it was inserted by a soft reference but now it can't be softly referenced, put it back in hard.
				if (list != null && !useSoftCacheForCustom)
				{
					hmValueLists.put(valuelist, list);
				}

			}
			else if (object instanceof IValueList)
			{
				list = (IValueList)object;
			}

			if (list == null)
			{
				list = ValueListFactory.createRealValueList(application, valuelist, type, format);
				if (valuelist.getFallbackValueListID() > 0 && valuelist.getFallbackValueListID() != valuelist.getID())
				{
					ValueList vl = application.getFlattenedSolution().getValueList(valuelist.getFallbackValueListID());
					vl.setDisplayValueType(valuelist.getDisplayValueType());
					list.setFallbackValueList(getRealValueList(application, vl, useSoftCacheForCustom, type, format, dataprovider));
				}
				if (!useSoftCacheForCustom && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
				{
					hmValueLists.put(valuelist, list);
					if (dataprovider != null)
					{
						((CustomValueList)list).addDataProvider(dataprovider);
					}
				}
				else
				{
					hmValueLists.put(valuelist, new SoftReference<IValueList>(list));

					if (dataprovider != null && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
					{
						((CustomValueList)list).addDataProvider(dataprovider);
					}
				}
			}
			else if (valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
			{
				if (application instanceof IApplication && ((IApplication)application).isInDeveloper())
				{
					int currentType = ((CustomValueList)list).getType();
					if (currentType == Types.OTHER)
					{
						((CustomValueList)list).setType(type);
						if (dataprovider != null)
						{
							((CustomValueList)list).addDataProvider(dataprovider);
						}
					}
					else if (type != Types.OTHER && type != currentType)
					{
						List<String> lst = ((CustomValueList)list).getDataProviders();

						StringBuffer message = new StringBuffer("The valuelist was already created for type: " +
							Column.getDisplayTypeString(((CustomValueList)list).getType()));
						message.append("\n for the dataproviders: ");
						for (int i = 0; i < lst.size(); i++)
						{
							String previousProviders = lst.get(i);
							message.append(previousProviders);
							message.append(",");
						}
						message.setLength(message.length() - 1);
						message.append("\nSo it can't be uses also for type: " + Column.getDisplayTypeString(type) + " for the dataprovider: " + dataprovider);
						message.append("\nPlease check these dataproviders of this valuelist: " + valuelist.getName());
						application.reportError("Valuelist: " + list.getName() + " used with different types", message);
					}
				}
			}
		}
		else
		{
			list = ValueListFactory.createRealValueList(application, valuelist, type, format);
			if (valuelist != null && valuelist.getFallbackValueListID() > 0 && valuelist.getFallbackValueListID() != valuelist.getID())
			{
				ValueList vl = application.getFlattenedSolution().getValueList(valuelist.getFallbackValueListID());
				vl.setDisplayValueType(valuelist.getDisplayValueType());
				list.setFallbackValueList(getRealValueList(application, vl, useSoftCacheForCustom, type, format, dataprovider));
			}
		}
		return list;
	}

	private static ValueList getValueList(IApplication application, Field field, IDataProviderLookup dataProviderLookup)
	{
		ValueList vl = null;
		try
		{
			vl = application.getFlattenedSolution().getValueList(field.getValuelistID());
			if (vl != null /* && application.getApplicationType() == IServiceProvider.DEVELOPER */&& vl.getValueListType() == ValueList.DATABASE_VALUES &&
				dataProviderLookup != null)
			{
				IDataProvider dp = dataProviderLookup.getDataProvider(field.getDataProviderID());
				if (dp != null)
				{
					int type = IColumnTypes.TEXT;
					String id = null;
					int total = vl.getShowDataProviders();
					if ((total & 7) == 1)
					{
						id = vl.getDataProviderID1();
					}
					else if ((total & 7) == 2)
					{
						id = vl.getDataProviderID2();
					}
					else if ((total & 7) == 4)
					{
						id = vl.getDataProviderID3();
					}
					else
					{
						//result must be string is concatenated
						type = IColumnTypes.TEXT;
					}

					IDataProvider p = application.getFlattenedSolution().getGlobalDataProvider(id);
					if (p == null && id != null)
					{
						ITable t = null;
						if (vl.getDatabaseValuesType() == ValueList.RELATED_VALUES)
						{
							String relationName = vl.getRelationName();
							Relation[] relations = application.getFlattenedSolution().getRelationSequence(relationName);
							if (relations != null) t = relations[relations.length - 1].getForeignTable();
						}
						else
						{
							t = vl.getTable();
						}

						if (t != null)
						{
							p = application.getFlattenedSolution().getDataProviderForTable((Table)t, id);
						}
					}

					if (p != null)
					{
						type = Column.mapToDefaultType(p.getDataProviderType());

//TODO this check has to be removed here because we are calculation the Display type and 
// not the Real type.. Should this check be done at another place????						
//						int dpft = Column.mapToDefaultType(dp.getDataProviderType(dataProviderLookup));
//						boolean incompat = false;
//						if (dpft == Column.NUMBER && (dpt != Column.NUMBER || dpt != Column.INTEGER)) incompat = true;
//						else if (dpft == Column.INTEGER && dpt != Column.INTEGER) incompat = true;
//						else if (dpft == Column.DATETIME && dpt != Column.DATETIME) incompat = true;
//						else if (dpft == Column.MEDIA) incompat = true;
//						
//						if (incompat)
//						{
//							application.reportError("Incompatible valuelist on field","Field "+field.getName()+" ("+field.getDataProviderID()+") has incompatible valuelist "+vl.getName()+"\nvaluelist is returning wrong type for field");
//						}
					}
					vl.setDisplayValueType(type);//store s owe can use it later on
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return vl;
	}

	public static String[] getFieldFormat(Field field, IDataProviderLookup dataProviderLookup, IServiceProvider application)
	{
		String format = null;
		int type = IColumnTypes.TEXT;
		IDataProvider dp = null;
		if (field.getDataProviderID() != null && dataProviderLookup != null)
		{
			try
			{
				dp = dataProviderLookup.getDataProvider(field.getDataProviderID());
				if (dp != null)
				{
					format = field.getFormat();
					type = dp.getDataProviderType();
					IColumn c = null;
					if (dp instanceof ColumnWrapper)
					{
						c = ((ColumnWrapper)dp).getColumn();
					}
					else if (dp instanceof Column)
					{
						c = (IColumn)dp;
					}


					if (c instanceof Column)
					{
						ColumnInfo ci = ((Column)c).getColumnInfo();
						if (ci != null)
						{
							if (format == null || format.length() == 0)
							{
								if (ci.getDefaultFormat() != null && ci.getDefaultFormat().length() > 0)
								{
									format = ci.getDefaultFormat();
								}
							}
							if (ci.getConverterName() != null && ci.getConverterName().trim().length() != 0)
							{
								IColumnConverter converter = ((FoundSetManager)application.getFoundSetManager()).getColumnConverterManager().getConverter(
									ci.getConverterName());
								if (converter instanceof ITypedColumnConverter)
								{
									try
									{
										OpenProperties props = new OpenProperties();
										if (ci.getConverterProperties() != null) props.load(new StringReader(ci.getConverterProperties()));
										type = ((ITypedColumnConverter)converter).getToObjectType(props);
										if (type == Integer.MAX_VALUE)
										{
											type = c.getDataProviderType();
										}
									}
									catch (IOException e)
									{
										Debug.error("Exception loading properties for converter " + converter.getName() + ", properties: " +
											ci.getConverterProperties(), e);
									}
								}
							}
						}

					}
					if (format == null || format.length() == 0)
					{
						format = TagResolver.getDefaultFormatForType(application.getSettings(), type);
					}
					if ("converter".equals(format))
					{
						format = null;
						type = IColumnTypes.TEXT;
					}

				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		return new String[] { format, new Integer(type).toString() };
	}

	private static IComponent createField(IApplication application, Form form, Field field, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing)
	{
		ValueList valuelist = null;
		if (field.getValuelistID() > 0) valuelist = getValueList(application, field, dataProviderLookup);
		String[] fieldFormat = getFieldFormat(field, dataProviderLookup, application);
		String format = fieldFormat[0];
		int type = Integer.parseInt(fieldFormat[1]);

		IDataProvider dp = null;
		if (field.getDataProviderID() != null && dataProviderLookup != null)
		{
			try
			{
				dp = dataProviderLookup.getDataProvider(field.getDataProviderID());
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}

		//apply any style
		Insets style_margin = null;
		int style_halign = -1;
		Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo = getStyleForBasicComponent(application, field, form);
		if (styleInfo != null)
		{
			FixedStyleSheet ss = styleInfo.getLeft();
			javax.swing.text.Style s = styleInfo.getRight();
			if (ss != null && s != null)
			{
				style_margin = ss.getMargin(s);
				style_halign = ss.getHAlign(s);
			}
		}

		IFieldComponent fl = null;
		switch (field.getDisplayType())
		{
			case Field.PASSWORD :
				fl = application.getItemFactory().createDataPassword(getWebID(form, field));
				break;
			case Field.RTF_AREA :
				fl = application.getItemFactory().createDataTextEditor(getWebID(form, field), RTF_AREA, field.getEditable());
				if (fl instanceof IScrollPane)
				{
					applyScrollBarsProperty((IScrollPane)fl, field);
				}
				break;
			case Field.HTML_AREA :
				fl = application.getItemFactory().createDataTextEditor(getWebID(form, field), HTML_AREA, field.getEditable());
				if (fl instanceof IScrollPane)
				{
					applyScrollBarsProperty((IScrollPane)fl, field);
				}
				break;
			case Field.TEXT_AREA :
				fl = application.getItemFactory().createDataTextArea(getWebID(form, field));
				if (fl instanceof IScrollPane)
				{
					applyScrollBarsProperty((IScrollPane)fl, field);
				}
				break;
			case Field.CHECKS :
				if (valuelist != null)
				{
					IValueList list = getRealValueList(application, valuelist, true, type, format, field.getDataProviderID());
					if (!(valuelist.getValueListType() == ValueList.DATABASE_VALUES && valuelist.getDatabaseValuesType() == ValueList.RELATED_VALUES) &&
						list.getSize() == 1 && valuelist.getAddEmptyValue() != ValueList.EMPTY_VALUE_ALWAYS)
					{
						fl = application.getItemFactory().createDataCheckBox(getWebID(form, field), application.getI18NMessageIfPrefixed(field.getText()), list);
					}
					else
					// 0 or >1
					{
						fl = application.getItemFactory().createDataChoice(getWebID(form, field), list, false);
						if (fl instanceof IScrollPane)
						{
							applyScrollBarsProperty((IScrollPane)fl, field);
						}
					}
				}
				else
				{
					fl = application.getItemFactory().createDataCheckBox(getWebID(form, field), application.getI18NMessageIfPrefixed(field.getText()));
				}
				break;
			case Field.RADIOS :
			{
				IValueList list = getRealValueList(application, valuelist, true, type, format, field.getDataProviderID());
				fl = application.getItemFactory().createDataChoice(getWebID(form, field), list, true);
				if (fl instanceof IScrollPane)
				{
					applyScrollBarsProperty((IScrollPane)fl, field);
				}
			}
				break;
			case Field.COMBOBOX :
			{
				IValueList list = getRealValueList(application, valuelist, true, type, format, field.getDataProviderID());
				fl = application.getItemFactory().createDataComboBox(getWebID(form, field), list);
			}
				break;

			case Field.CALENDAR :
				fl = application.getItemFactory().createDataCalendar(getWebID(form, field));
				break;

			case Field.IMAGE_MEDIA :
				fl = application.getItemFactory().createDataImgMediaField(getWebID(form, field));
				if (fl instanceof IScrollPane)
				{
					applyScrollBarsProperty((IScrollPane)fl, field);
				}
				break;
			case Field.TYPE_AHEAD :
				if (field.getValuelistID() > 0)
				{
					fl = createTypeAheadWithValueList(application, form, field, dataProviderLookup, type, format);
					break;
				}
				else if (dp != null && dp.getColumnWrapper() != null && dp.getColumnWrapper().getRelations() == null)//only allow plain columns
				{
					fl = application.getItemFactory().createDataLookupField(getWebID(form, field), form.getServerName(), form.getTableName(),
						dp == null ? field.getDataProviderID() : dp.getDataProviderID());
					break;
				}
				//$FALL-THROUGH$ else treat as the default case: TEXT_FIELD
			default ://Field.TEXT_FIELD
				if (field.getValuelistID() > 0)
				{
					fl = createTypeAheadWithValueList(application, form, field, dataProviderLookup, type, format);
				}
				else
				{
					fl = application.getItemFactory().createDataField(getWebID(form, field));
				}
				break;
		}
		if (fl instanceof ISupportAsyncLoading)
		{
			((ISupportAsyncLoading)fl).setAsyncLoadingEnabled(!printing);
		}
		fl.setSelectOnEnter(field.getSelectOnEnter());
		fl.setEditable(field.getEditable());
		try
		{
			int halign = field.getHorizontalAlignment();
			if (halign != -1)
			{
				fl.setHorizontalAlignment(halign);
			}
			else if (style_halign != -1)
			{
				fl.setHorizontalAlignment(style_halign);
			}
		}
		catch (RuntimeException e)
		{
			//just ignore...Debug.error(e);
		}
		fl.setToolTipText(application.getI18NMessageIfPrefixed(field.getToolTipText()));
		fl.setTitleText(application.getI18NMessageIfPrefixed(field.getText()));
		fl.setDataProviderID(dp == null ? field.getDataProviderID() : dp.getDataProviderID());
		if (field.getDataProviderID() != null && dataProviderLookup != null)
		{
			fl.setFormat(type, application.getI18NMessageIfPrefixed(format));

			if (dp != null)
			{
				//if (valuelist != null && valuelist.getValueListType() != ValueList.CUSTOM_VALUES) type = valuelist.getDisplayValueType();
				int l = dp.getLength();
				int defaultType = Column.mapToDefaultType(type);
				if (l > 0 && (defaultType == IColumnTypes.TEXT || defaultType == IColumnTypes.MEDIA))
				{
					fl.setMaxLength(l);
				}
			}
		}
//		fl.setOpaque(!field.getTransparent());
		fl.setNeedEntireState(field.getDisplaysTags());
		if (el != null) // el is an ActionListener
		{
			fl.addScriptExecuter(el);
			Object[] cmds = combineMethodsToCommands(form, form.getOnElementFocusGainedMethodID(), "onElementFocusGainedMethodID", field,
				field.getOnFocusGainedMethodID(), "onFocusGainedMethodID");
			if (cmds != null) fl.setEnterCmds((String[])cmds[0], (Object[][])cmds[1]);
			cmds = combineMethodsToCommands(form, form.getOnElementFocusLostMethodID(), "onElementFocusLostMethodID", field, field.getOnFocusLostMethodID(),
				"onFocusLostMethodID");
			if (cmds != null) fl.setLeaveCmds((String[])cmds[0], (Object[][])cmds[1]);
			if (field.getOnActionMethodID() != 0) fl.setActionCmd(Integer.toString(field.getOnActionMethodID()),
				Utils.parseJSExpressions(field.getInstanceMethodArguments("onActionMethodID")));
			if (field.getOnDataChangeMethodID() != 0) fl.setChangeCmd(Integer.toString(field.getOnDataChangeMethodID()),
				Utils.parseJSExpressions(field.getInstanceMethodArguments("onDataChangeMethodID")));
			if (field.getOnRightClickMethodID() > 0) fl.setRightClickCommand(Integer.toString(field.getOnRightClickMethodID()),
				Utils.parseJSExpressions(field.getInstanceMethodArguments("onRightClickMethodID")));
		}

		applyBasicComponentProperties(application, fl, field, styleInfo);

		if (fl instanceof INullableAware)
		{
			INullableAware nullAware = (INullableAware)fl;
			boolean allowNull = true;
			// for example if it is a check box linked to a non-null integer table column, it must force null to
			// become 0 (because it is unchecked) so that the user does not need to check/uncheck it for save
			try
			{
				if (dataProviderLookup != null && dataProviderLookup.getTable() != null && field.getDataProviderID() != null)
				{
					String dataproviderId = dp == null ? field.getDataProviderID() : dp.getDataProviderID();
					if (dataProviderLookup.getTable().getColumn(dataproviderId) != null)
					{
						allowNull = dataProviderLookup.getTable().getColumn(dataproviderId).getAllowNull();
					}
				}
			}
			catch (RepositoryException e)
			{
				// maybe this field is not linked to a table column... so leave it true
			}
			nullAware.setAllowNull(allowNull);
		}

		Insets m = field.getMargin();
		if (m != null)
		{
			fl.setMargin(m);
		}
		else if (style_margin != null)
		{
			fl.setMargin(style_margin);
		}
		return fl;
	}

	private static Object[] combineMethodsToCommands(AbstractBase persist1, int method1, String methodKey1, AbstractBase persist2, int method2,
		String methodKey2)
	{
		if (method1 <= 0 && method2 <= 0)
		{
			return null;
		}
		if (method1 > 0 && method2 > 0)
		{
			return new Object[] { new String[] { String.valueOf(method1), String.valueOf(method2) }, new Object[][] { Utils.parseJSExpressions(persist1.getInstanceMethodArguments(methodKey1)), Utils.parseJSExpressions(persist2.getInstanceMethodArguments(methodKey2)) } };
		}
		return new Object[] { new String[] { String.valueOf(method1 <= 0 ? method2 : method1) }, new Object[][] { Utils.parseJSExpressions((method1 <= 0
			? persist2 : persist1).getInstanceMethodArguments(method1 <= 0 ? methodKey2 : methodKey1)) } };
	}

	/**
	 * @param application
	 * @param field
	 * @param dataProviderLookup
	 * @param fl
	 * @return
	 */
	private static IFieldComponent createTypeAheadWithValueList(IApplication application, Form form, Field field, IDataProviderLookup dataProviderLookup,
		int type, String format)
	{
		IFieldComponent fl = null;
		ValueList valuelist = getValueList(application, field, dataProviderLookup);
		if (valuelist == null)
		{
			fl = application.getItemFactory().createDataField(getWebID(form, field));
		}
		else if (valuelist.getValueListType() == ValueList.DATABASE_VALUES)
		{
			LookupValueList lookupValueList = null;
			try
			{
				lookupValueList = new LookupValueList(valuelist, application);
				fl = application.getItemFactory().createDataLookupField(getWebID(form, field), lookupValueList);
			}
			catch (Exception e1)
			{
				Debug.error(e1);
			}
		}
		else if (valuelist.getValueListType() == ValueList.CUSTOM_VALUES || valuelist.getValueListType() == ValueList.GLOBAL_METHOD_VALUES)
		{
			fl = application.getItemFactory().createDataLookupField(getWebID(form, field),
				(CustomValueList)getRealValueList(application, valuelist, true, type, format, field.getDataProviderID()));
		}
		return fl;
	}

	private static IComponent createGraphicalComponent(IApplication application, Form form, GraphicalComponent label, IScriptExecuter el,
		IDataProviderLookup dataProviderLookup)
	{
		int style_halign = -1;
		int style_valign = -1;
		int textTransform = 0;
		int mediaid = 0;
		Pair<FixedStyleSheet, javax.swing.text.Style> styleInfo = getStyleForBasicComponent(application, label, form);
		if (styleInfo != null)
		{
			FixedStyleSheet ss = styleInfo.getLeft();
			javax.swing.text.Style s = styleInfo.getRight();
			if (ss != null && s != null)
			{
				style_valign = ss.getVAlign(s);
				style_halign = ss.getHAlign(s);

				// should we do it through css on the client side or throug the media property on the server side..
				// doing it through the media property keeps the smart and web more the same, so keeping that as the current behavior for now.
				//if (application.getApplicationType() != IApplication.WEB_CLIENT)
				{
					Object mediaUrl = s.getAttribute(CSS.Attribute.BACKGROUND_IMAGE);
					if (mediaUrl != null && mediaUrl.toString() != null)
					{
						String mediaUrlString = mediaUrl.toString();
						int start = mediaUrlString.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
						if (start != -1)
						{
							String name = mediaUrlString.substring(start + MediaURLStreamHandler.MEDIA_URL_DEF.length());
							if (name.endsWith("')") || name.endsWith("\")")) name = name.substring(0, name.length() - 2);
							if (name.endsWith(")")) name = name.substring(0, name.length() - 1);
							Media media = application.getFlattenedSolution().getMedia(name);
							if (media != null)
							{
								mediaid = media.getID();
							}
						}
					}
				}


				String transform = (String)s.getAttribute(CSS.getAttribute("text-transform"));
				if (transform != null)
				{
					if ("uppercase".equals(transform))
					{
						textTransform = ILabel.UPPERCASE;
					}
					else if ("lowercase".equals(transform))
					{
						textTransform = ILabel.LOWERCASE;
					}
					else if ("capitalize".equals(transform))
					{
						textTransform = ILabel.CAPITALIZE;
					}
				}
			}

		}

		IComponent l;
		if (label.getOnActionMethodID() != 0 && label.getShowClick())
		{
			if (label.getDataProviderID() == null && !label.getDisplaysTags())
			{
				l = application.getItemFactory().createScriptButton(getWebID(form, label));
			}
			else
			{
				l = application.getItemFactory().createDataButton(getWebID(form, label));
				IDataProvider dp = null;
				try
				{
					dp = dataProviderLookup == null ? null : dataProviderLookup.getDataProvider(label.getDataProviderID());
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
				((IDisplayData)l).setDataProviderID(dp == null ? label.getDataProviderID() : dp.getDataProviderID());
				((IDisplayTagText)l).setTagText(application.getI18NMessageIfPrefixed(label.getText()));
				((IDisplayData)l).setNeedEntireState(label.getDisplaysTags());
			}
			((ILabel)l).setMediaOption(label.getMediaOptions());
			if (label.getRolloverImageMediaID() > 0)
			{
				try
				{
					((IButton)l).setRolloverIcon(label.getRolloverImageMediaID());
					((IButton)l).setRolloverEnabled(true);
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
		else
		{
			if (label.getDataProviderID() == null && !label.getDisplaysTags())
			{
				l = application.getItemFactory().createScriptLabel(getWebID(form, label), (label.getOnActionMethodID() != 0));
			}
			else
			{
				l = application.getItemFactory().createDataLabel(getWebID(form, label), (label.getOnActionMethodID() != 0));
				IDataProvider dp = null;
				try
				{
					dp = dataProviderLookup == null ? null : dataProviderLookup.getDataProvider(label.getDataProviderID());
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
				((IDisplayData)l).setDataProviderID(dp == null ? label.getDataProviderID() : dp.getDataProviderID());
				((IDisplayTagText)l).setTagText(application.getI18NMessageIfPrefixed(label.getText()));
				((IDisplayData)l).setNeedEntireState(label.getDisplaysTags());
			}
			((ILabel)l).setMediaOption(label.getMediaOptions());
			if (label.getRolloverImageMediaID() > 0)
			{
				try
				{
					((ILabel)l).setRolloverIcon(label.getRolloverImageMediaID());
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
		String mnemonic = application.getI18NMessageIfPrefixed(label.getMnemonic());
		if (mnemonic != null && mnemonic.length() > 0)
		{
			((ILabel)l).setDisplayedMnemonic(mnemonic.charAt(0));
		}
		((ILabel)l).setTextTransform(textTransform);
		if (el != null && (label.getOnActionMethodID() > 0 || label.getOnDoubleClickMethodID() > 0 || label.getOnRightClickMethodID() > 0))
		{
			((ILabel)l).addScriptExecuter(el);
			if (label.getOnActionMethodID() > 0) ((ILabel)l).setActionCommand(Integer.toString(label.getOnActionMethodID()),
				Utils.parseJSExpressions(label.getInstanceMethodArguments("onActionMethodID")));
			if (label.getOnDoubleClickMethodID() > 0) ((ILabel)l).setDoubleClickCommand(Integer.toString(label.getOnDoubleClickMethodID()),
				Utils.parseJSExpressions(label.getInstanceMethodArguments("onDoubleClickMethodID")));
			if (label.getOnRightClickMethodID() > 0) ((ILabel)l).setRightClickCommand(Integer.toString(label.getOnRightClickMethodID()),
				Utils.parseJSExpressions(label.getInstanceMethodArguments("onRightClickMethodID")));
		}
		((ILabel)l).setRotation(label.getRotation());
		((ILabel)l).setFocusPainted(label.getShowFocus());
		l.setCursor(Cursor.getPredefinedCursor(label.getRolloverCursor()));
		try
		{
			int halign = label.getHorizontalAlignment();
			if (halign != -1)
			{
				((ILabel)l).setHorizontalAlignment(halign);
			}
			else if (style_halign != -1)
			{
				((ILabel)l).setHorizontalAlignment(style_halign);
			}
		}
		catch (RuntimeException e)
		{
			//just ignore...Debug.error(e);
		}

		int valign = label.getVerticalAlignment();
		if (valign != -1)
		{
			((ILabel)l).setVerticalAlignment(valign);
		}
		else if (style_valign != -1)
		{
			((ILabel)l).setVerticalAlignment(style_valign);
		}

		try
		{
			if (!label.getDisplaysTags())
			{
				((ILabel)l).setText(application.getI18NMessageIfPrefixed(label.getText()));
			}
		}
		catch (RuntimeException e1)
		{
			// ignore
		}
		l.setToolTipText(application.getI18NMessageIfPrefixed(label.getToolTipText()));
		if (label.getImageMediaID() > 0)
		{
			try
			{
				((ILabel)l).setMediaIcon(label.getImageMediaID());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (mediaid > 0)
		{
			try
			{
				((ILabel)l).setMediaIcon(mediaid);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		applyBasicComponentProperties(application, l, label, styleInfo);

		Border border = null;
		Insets insets = null;

		if (label.getBorderType() != null)
		{
			border = ComponentFactoryHelper.createBorder(label.getBorderType());
		}
		if (label.getMargin() != null)
		{
			insets = label.getMargin();
		}

		if (styleInfo != null && (border == null || insets == null))
		{
			FixedStyleSheet ss = styleInfo.getLeft();
			javax.swing.text.Style s = styleInfo.getRight();
			if (ss != null && s != null)
			{
				if (border == null &&
					(s.getAttribute(CSS.Attribute.BORDER) != null || s.getAttribute(CSS.Attribute.BORDER_STYLE) != null ||
						s.getAttribute(CSS.Attribute.BORDER_TOP) != null || s.getAttribute(CSS.Attribute.BORDER_TOP_WIDTH) != null))
				{
					border = ss.getBorder(s);
				}
				if (insets == null && (s.getAttribute(CSS.Attribute.MARGIN) != null || s.getAttribute(CSS.Attribute.MARGIN_TOP) != null))
				{
					insets = ss.getMargin(s);
				}
			}
		}


		if (border != null && insets != null)
		{
			l.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
		}
		else if (border == null && insets != null && l instanceof IButton)
		{
			((IButton)l).setMargin(insets);
		}
		// If there is no border, but there are margins, and we don't have a child of JButton (which
		// supports setMargin, then fake the margins through empty border. (issue 166391)
		else if (border == null && insets != null)
		{
			l.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right));
		}

//		l.setOpaque(!label.getTransparent());
		return l;
	}

	public static byte[] loadIcon(FlattenedSolution solution, Integer key)
	{
		Media m = solution.getMedia(key.intValue());
		if (m != null)
		{
			return m.getMediaData();
		}
		return null;
	}

	private static IComponent createPortal(IApplication application, Form form, Portal meta, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing)
	{
		return application.getItemFactory().createPortalComponent(meta, form, dataProviderLookup, el, printing);
	}

	private static IComponent createPart(IApplication application, Part meta)
	{
		IComponent part = application.getItemFactory().createPanel();
		java.awt.Color bg = meta.getBackground();
		if (bg != null) part.setBackground(bg);
		part.setSize(new Dimension(((Form)meta.getParent()).getSize().width, meta.getHeight()));
		return part;
	}

	private static IComponent createSplitPane(IApplication application, Form form, TabPanel meta)
	{
		ISplitPane splitPane = application.getItemFactory().createSplitPane(getWebID(form, meta), meta.getTabOrientation());
		applyBasicComponentProperties(application, splitPane, meta, getStyleForBasicComponent(application, meta, form));
		try
		{
			int index = 0;
			Iterator<IPersist> it = meta.getTabs();
			while (it.hasNext() && index < 2)
			{
				Tab tab = (Tab)it.next();
				Form f = application.getFlattenedSolution().getForm(tab.getContainsFormID());
				if (f != null)
				{
					IFormLookupPanel flp = splitPane.createFormLookupPanel(tab.getName(), tab.getRelationName(), f.getName());
					if (index < 1) splitPane.setLeftForm(flp);
					else splitPane.setRightForm(flp);
					index++;
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		splitPane.js_setDividerLocation(meta.getTabOrientation() == TabPanel.SPLIT_HORIZONTAL ? splitPane.getSize().width / 2 : splitPane.getSize().height / 2);
		return splitPane;
	}

	private static IComponent createTabPanel(IApplication application, Form form, TabPanel meta, IScriptExecuter el)
	{
		//HACK:To set the selected color on a tabpanel
		Color oldColor = null;
		if (meta.getSelectedTabColor() != null)
		{
			oldColor = UIManager.getColor("TabbedPane.selected");
			UIManager.put("TabbedPane.selected", meta.getSelectedTabColor());
		}
		int orient = meta.getTabOrientation();
		ITabPanel tabs = application.getItemFactory().createTabPanel(getWebID(form, meta), orient, meta.hasOneTab());
		if (meta.getScrollTabs())
		{
			tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		}
		else
		{
			tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		}

		if (el != null && meta.getOnTabChangeMethodID() > 0)
		{
			tabs.setOnTabChangeMethodCmd(Integer.toString(meta.getOnTabChangeMethodID()),
				Utils.parseJSExpressions(meta.getInstanceMethodArguments("onTabChangeMethodID")));
			tabs.addScriptExecuter(el);
		}

		applyBasicComponentProperties(application, tabs, meta, getStyleForBasicComponent(application, meta, form));
		//HACK:restore so not all tabpanel get that color!
		if (meta.getSelectedTabColor() != null) UIManager.put("TabbedPane.selected", oldColor);

		try
		{
			int index = 0;
			Iterator<IPersist> it = meta.getTabs();
			while (it.hasNext())
			{
				Tab tab = (Tab)it.next();
				Form f = application.getFlattenedSolution().getForm(tab.getContainsFormID());
				if (f != null)
				{
					IFormLookupPanel flp = tabs.createFormLookupPanel(tab.getName(), tab.getRelationName(), f.getName());
					Icon icon = null;
					if (tab.getImageMediaID() > 0)
					{
						try
						{
							icon = ImageLoader.getIcon(loadIcon(application.getFlattenedSolution(), new Integer(tab.getImageMediaID())), -1, -1, true);
//							((IGraphicalComponent)l).setIcon(c);
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}

					tabs.addTab(application.getI18NMessageIfPrefixed(tab.getText()), icon, flp, application.getI18NMessageIfPrefixed(tab.getToolTipText()));

					Color fg = tab.getForeground();
					Color bg = tab.getBackground();
					if (fg != null) tabs.setTabForegroundAt(index, fg);
					if (bg != null) tabs.setTabBackgroundAt(index, bg);
					index++;
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return tabs;
	}

	private static IComponent createTab(IApplication application, Tab meta)
	{
		IStandardLabel retval = application.getItemFactory().createLabel(meta.getName(), application.getI18NMessageIfPrefixed(meta.getText()));
		Form f = application.getFlattenedSolution().getForm(meta.getContainsFormID());
		String toolTip = "";
		if (f != null) toolTip += "Form: " + f.getName();
		if (meta.getRelationName() != null) toolTip += " Relation: " + meta.getRelationName();
		retval.setToolTipText(toolTip);
//		retval.setOpaque(false);
		retval.setBorder(new TabLikeBorder());//BorderFactory.createBevelBorder(BevelBorder.RAISED));
		retval.setHorizontalAlignment(SwingConstants.CENTER);
		retval.setVerticalAlignment(SwingConstants.TOP);
		if (meta.getImageMediaID() > 0)
		{
			try
			{
				retval.setIcon(loadIcon(application.getFlattenedSolution(), new Integer(meta.getImageMediaID())));
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		return retval;
	}

	public static void flushStyle(Style style)
	{
		parsedStyles.remove(style);
	}

	@SuppressWarnings("unchecked")
	public static void flushValueList(ValueList vl)
	{
		WeakHashMap<ValueList, Object> hmValueLists = (WeakHashMap<ValueList, Object>)J2DBGlobals.getServiceProvider().getRuntimeProperties().get(
			IServiceProvider.RT_VALUELIST_CACHE);
		if (hmValueLists != null)
		{
			hmValueLists.remove(vl);
		}
	}

	@SuppressWarnings("unchecked")
	public static void flushCachedItems()
	{
		parsedStyles = new ConcurrentHashMap<Style, FixedStyleSheet>();
		J2DBGlobals.getServiceProvider().getRuntimeProperties().put(IServiceProvider.RT_VALUELIST_CACHE, null);
		J2DBGlobals.getServiceProvider().getRuntimeProperties().put(IServiceProvider.RT_OVERRIDESTYLE_CACHE, null);

		Iterator<IconHolder> it = lstIcons.values().iterator();
		while (it.hasNext())
		{
			IconHolder ih = it.next();
			Icon icon = ih.icon.get();
			if (icon instanceof ImageIcon)
			{
				ImageIcon imageIcon = (ImageIcon)icon;
				if (imageIcon.getImage() != null)
				{
					imageIcon.getImage().flush();
				}
				imageIcon.setImageObserver(null);
			}
			else if (icon instanceof MyImageIcon)
			{
				((MyImageIcon)icon).flush();
			}
		}
		lstIcons = new WeakHashMap<Object, IconHolder>();
	}

	public static void applyScrollBarsProperty(IScrollPane pane, ISupportScrollbars element)
	{
		int scroll = element.getScrollbars();
		if (scroll != ISupportScrollbars.SCROLLBARS_WHEN_NEEDED)
		{
			if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED)
			{
				pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			}
			else
			{
				if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS)
				{
					pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				}
				else if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER)
				{
					pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
				}
			}
			if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED)
			{
				pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			}
			else
			{
				if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS)
				{
					pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				}
				else if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
				{
					pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				}
			}
		}
	}

	public static void registerIcon(Icon icon)
	{
		if (icon == null) return;
		Object image;
		if (icon instanceof ImageIcon)
		{
			image = ((ImageIcon)icon).getImage();
		}
		else if (icon instanceof MyImageIcon)
		{
			ImageIcon myIcon = ((MyImageIcon)icon).getOriginal();
			if (myIcon != null)
			{
				image = myIcon.getImage();
			}
			else
			{
				image = icon;
			}
		}
		else image = null;

		IconHolder ih = lstIcons.get(image);
		if (ih == null)
		{
			lstIcons.put(image, new IconHolder(icon));
		}
		else
		{
			ih.counter++;
		}
	}

	public static void deregisterIcon(final Icon icon)
	{
		if (icon == null) return;
		Object image;
		if (icon instanceof ImageIcon)
		{
			image = ((ImageIcon)icon).getImage();
		}
		else if (icon instanceof MyImageIcon)
		{
			ImageIcon myIcon = ((MyImageIcon)icon).getOriginal();
			if (myIcon != null)
			{
				image = myIcon.getImage();
			}
			else
			{
				image = icon;
			}
		}
		else image = null;

		if (image != null)
		{
			IconHolder ih = lstIcons.get(image);
			if (ih != null)
			{
				ih.counter--;
				if (ih.counter == 0)
				{
					lstIcons.remove(image);
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							Image img = null;
							if (icon instanceof ImageIcon)
							{
								ImageIcon imageIcon = (ImageIcon)icon;
								img = imageIcon.getImage();
								if (!lstIcons.containsKey(img))
								{
									img.flush();
									imageIcon.setImageObserver(null);
								}
							}
							else if (icon instanceof MyImageIcon)
							{
								ImageIcon myIcon = ((MyImageIcon)icon).getOriginal();
								if (myIcon != null)
								{
									img = myIcon.getImage();
								}
								if (img == null || !lstIcons.containsKey(img))
								{
									((MyImageIcon)icon).flush();
								}
							}
						}
					});
				}
			}
		}
	}

	static class IconHolder
	{
		public IconHolder(Icon icon)
		{
			this.icon = new WeakReference<Icon>(icon);
			this.counter = 1;
		}

		private final WeakReference<Icon> icon;
		private int counter;
	}

	/**
	 * @param originalStyleName
	 * @param newStyleName
	 */
	@SuppressWarnings("unchecked")
	public static void overrideStyle(String originalStyleName, String newStyleName)
	{
		HashMap<String, String> overridenStyles = (HashMap<String, String>)J2DBGlobals.getServiceProvider().getRuntimeProperties().get(
			IServiceProvider.RT_OVERRIDESTYLE_CACHE);
		if (overridenStyles == null)
		{
			overridenStyles = new HashMap<String, String>();
			J2DBGlobals.getServiceProvider().getRuntimeProperties().put(IServiceProvider.RT_OVERRIDESTYLE_CACHE, overridenStyles);
		}
		overridenStyles.put(originalStyleName, newStyleName);
	}

	/**
	 * Return a new list with the elements of the input list sorted on position. Grouped elements are placed together.
	 * @param elements
	 */
	public static List<IPersist> sortElementsOnPositionAndGroup(List<IPersist> elements)
	{
		if (elements == null) return null;

		// first sort on position, then move all grouped elements together
		List<IPersist> lst = new ArrayList<IPersist>(elements);
		Collections.sort(lst, PositionComparator.XY_PERSIST_COMPARATOR);
		for (int i = 0; i < lst.size(); i++)
		{
			IPersist element = lst.get(i);
			if (element instanceof IFormElement && ((IFormElement)element).getGroupID() != null)
			{
				// find other group elements, move them to the left
				for (int j = i + 2; j < lst.size(); j++)
				{
					IPersist element2 = lst.get(j);
					if (element2 instanceof IFormElement && ((IFormElement)element).getGroupID().equals(((IFormElement)element2).getGroupID()))
					{
						// same group, move to the left
						lst.add(i + 1, lst.remove(j));
						i++;
					}
				}
			}
		}
		return lst;
	}

}
