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
package com.servoy.j2db.smart.dataui;

import java.awt.Component;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.InvisibleBean;
import com.servoy.j2db.component.VisibleBean;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Shape;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IPortalComponent;
import com.servoy.j2db.ui.IRect;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStandardLabel;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 */
public class SwingItemFactory implements ItemFactory
{
	private static Map<String, WeakReference<Class< ? >>> beanClassCache = new ConcurrentHashMap<String, WeakReference<Class< ? >>>();

	private final IApplication application;

	/**
	 * 
	 */
	public SwingItemFactory(IApplication application)
	{
		super();
		this.application = application;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createPanel()
	 */
	public IComponent createPanel()
	{
		return new ComponentJPanel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createLabel(java.lang.String)
	 */
	public IStandardLabel createLabel(String name, String txt)
	{
		return new ComponentJLabel(txt);
	}


	public IComponent createBeanHolder(String name, Component obj)
	{
		return new VisibleBean(obj);
	}

	public IComponent createInvisibleBean(String name, Object obj)
	{
		return new InvisibleBean(obj);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createTabPanel(int, boolean)
	 */
	public ITabPanel createTabPanel(String name, int orient, boolean oneTab)
	{
		ITabPanel tabPanel = new SpecialTabPanel(application, orient, oneTab);
		tabPanel.setName(name);
		return tabPanel;
	}

	public ISplitPane createSplitPane(String name, int orient)
	{
		ISplitPane splitPane = new SpecialSplitPane(application, orient);
		splitPane.setName(name);
		return splitPane;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createPortalComponent(com.servoy.j2db.persistence.Portal, com.servoy.j2db.persistence.IDataProviderLookup,
	 * com.servoy.j2db.IScriptExecuter, boolean)
	 */
	public IPortalComponent createPortalComponent(Portal meta, Form form, IDataProviderLookup dataProviderLookup, IScriptExecuter el, boolean printing)
	{
		return new PortalComponent(application, form, meta, dataProviderLookup, el, printing);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataButton()
	 */
	public IButton createDataButton(String name)
	{
		DataButton db = new DataButton(application);
		db.setName(name);
		return db;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createScriptButton()
	 */
	public IButton createScriptButton(String name)
	{
		ScriptButton sb = new ScriptButton(application);
		sb.setName(name);
		return sb;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createScriptLabel()
	 */
	public ILabel createScriptLabel(String name, boolean hasActionListner)
	{
		ScriptLabel sl = new ScriptLabel(application);
		sl.setName(name);
		return sl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataLabel()
	 */
	public ILabel createDataLabel(String name, boolean hasActionListner)
	{
		DataLabel dl = new DataLabel(application);
		dl.setName(name);
		return dl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataPassword()
	 */
	public IFieldComponent createDataPassword(String name)
	{
		DataPassword dp = new DataPassword(application);
		dp.setName(name);
		return dp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataTextEditor(int)
	 */
	public IFieldComponent createDataTextEditor(String name, int type, boolean willBeEditable)
	{
		DataTextEditor dte = new DataTextEditor(application, type);
		dte.setName(name);
		return dte;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataTextArea()
	 */
	public IFieldComponent createDataTextArea(String name)
	{
		DataTextArea dta = new DataTextArea(application);
		dta.setName(name);
		return dta;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataCheckBox(java.lang.String, com.servoy.j2db.dataprocessing.IValueList)
	 */
	public IFieldComponent createDataCheckBox(String name, String text, IValueList list)
	{
		DataCheckBox dcb = new DataCheckBox(application, text, list);
		dcb.setName(name);
		return dcb;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataCheckBox(java.lang.String)
	 */
	public IFieldComponent createDataCheckBox(String name, String text)
	{
		DataCheckBox dcb = new DataCheckBox(application, text);
		dcb.setName(name);
		return dcb;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataChoice(com.servoy.j2db.dataprocessing.IValueList, boolean)
	 */
	public IFieldComponent createDataChoice(String name, IValueList list, boolean b)
	{
		DataChoice dc = new DataChoice(application, list, b);
		dc.setName(name);
		return dc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataComboBox(com.servoy.j2db.dataprocessing.IValueList)
	 */
	public IFieldComponent createDataComboBox(String name, IValueList list)
	{
		DataComboBox dcb = new DataComboBox(application, list);
		dcb.setName(name);
		return dcb;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataCalendar()
	 */
	public IFieldComponent createDataCalendar(String name)
	{
		DataCalendar dc = new DataCalendar(application);
		dc.setName(name);
		return dc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataImgMediaField()
	 */
	public IFieldComponent createDataImgMediaField(String name)
	{
		DataImgMediaField field = new DataImgMediaField(application);
		field.setName(name);
		return field;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataField()
	 */
	public IFieldComponent createDataField(String name)
	{
		DataField df = new DataField(application);
		df.setName(name);
		return df;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.LookupValueList)
	 */
	public IFieldComponent createDataLookupField(String name, LookupValueList lookupValueList)
	{
		DataLookupField dlf = new DataLookupField(application, lookupValueList);
		dlf.setName(name);
		return dlf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.CustomValueList)
	 */
	public IFieldComponent createDataLookupField(String name, CustomValueList list)
	{
		DataLookupField dlf = new DataLookupField(application, list);
		dlf.setName(name);
		return dlf;
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IFieldComponent createDataLookupField(String name, String serverName, String tableName, String dataProviderID)
	{
		DataLookupField dlf = new DataLookupField(application, serverName, tableName, dataProviderID);
		dlf.setName(name);
		return dlf;
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createRect(java.lang.String)
	 */
	public IRect createRect(String name, int type)
	{
		return new Rect(application, type);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createShape(java.lang.String, com.servoy.j2db.persistence.Shape)
	 */
	public IComponent createShape(String webID, Shape rec)
	{
		return new ShapePainter(application, rec.getShapeType(), rec.getLineSize(), rec.getPolygon());
	}

	public static Class getPersistClass(IApplication application, IPersist persist)
	{
		if (persist instanceof GraphicalComponent)
		{
			GraphicalComponent label = (GraphicalComponent)persist;
			if (label.getOnActionMethodID() != 0 && label.getShowClick())
			{
				if (label.getDataProviderID() == null && !label.getDisplaysTags())
				{
					return ScriptButton.class;
				}
				else
				{
					return DataButton.class;
				}
			}
			else
			{
				if (label.getDataProviderID() == null && !label.getDisplaysTags())
				{
					return ScriptLabel.class;
				}
				else
				{
					return DataLabel.class;
				}
			}
		}
		else if (persist instanceof Field)
		{
			Field field = (Field)persist;

			switch (field.getDisplayType())
			{
				case Field.PASSWORD :
					return DataPassword.class;
				case Field.RTF_AREA :
				case Field.HTML_AREA :
					return DataTextEditor.class;
				case Field.TEXT_AREA :
					return DataTextArea.class;
				case Field.CHECKS :
					if (field.getValuelistID() > 0)
					{
//						IValueList list = getRealValueList(application, valuelist, true, type, format, field.getDataProviderID());
//						if (!(valuelist.getValueListType() == ValueList.DATABASE_VALUES && valuelist.getDatabaseValuesType() == ValueList.RELATED_VALUES) &&
//							list.getSize() == 1 && valuelist.getAddEmptyValue() != ValueList.EMPTY_VALUE_ALWAYS)
//						{
//							fl = application.getItemFactory().createDataCheckBox(getWebID(field), application.getI18NMessageIfPrefixed(field.getText()), list);
//						}
//						else
						// 0 or >1
						return DataChoice.class;
					}
					else
					{
						return DataCheckBox.class;
					}
				case Field.RADIOS :
					return DataChoice.class;
				case Field.COMBOBOX :
					return DataComboBox.class;
				case Field.CALENDAR :
					return DataCalendar.class;
				case Field.IMAGE_MEDIA :
					return DataImgMediaField.class;
				case Field.TYPE_AHEAD :
					return DataLookupField.class;
				default :
					if (field.getValuelistID() > 0)
					{
						return DataLookupField.class;
					}
					else
					{
						return DataField.class;
					}
			}

		}
		else if (persist instanceof Bean)
		{
			Bean bean = (Bean)persist;
			String beanClassName = bean.getBeanClassName();
			WeakReference<Class< ? >> beanClassRef = beanClassCache.get(beanClassName);
			Class< ? > beanClass = null;
			if (beanClassRef != null)
			{
				beanClass = beanClassRef.get();
			}
			if (beanClass == null)
			{
				ClassLoader bcl = application.getBeanManager().getClassLoader();
				try
				{
					beanClass = bcl.loadClass(beanClassName);
					if (IServoyBeanFactory.class.isAssignableFrom(beanClass))
					{
						IServoyBeanFactory beanFactory = (IServoyBeanFactory)beanClass.newInstance();
						Object beanInstance = beanFactory.getBeanInstance(application.getApplicationType(), (IClientPluginAccess)application.getPluginAccess(),
							new Object[] { ComponentFactory.getWebID(null, bean) });
						beanClass = beanInstance.getClass();
						if (beanInstance instanceof IScriptObject)
						{
							ScriptObjectRegistry.registerScriptObjectForClass(beanClass, (IScriptObject)beanInstance);
						}
					}
					beanClassCache.put(beanClassName, new WeakReference<Class< ? >>(beanClass));
				}
				catch (Exception e)
				{
					Debug.error("Error loading bean: " + bean.getName() + " clz: " + beanClassName, e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return beanClass;
		}
		else if (persist instanceof TabPanel)
		{
			int orient = ((TabPanel)persist).getTabOrientation();
			if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) return SpecialSplitPane.class;
			else return SpecialTabPanel.class;
		}
		else if (persist instanceof Portal)
		{
			return PortalComponent.class;
		}
		return null;

	}

	/**
	 * Set a property on the runtime component
	 */
	public void setComponentProperty(Object component, Object key, Serializable value)
	{
		if (component instanceof JComponent)
		{
			((JComponent)component).putClientProperty(key, value);
		}
	}

	/**
	 * get a property of the runtime component
	 */
	public Serializable getComponentProperty(Object component, Object key)
	{
		if (component instanceof JComponent)
		{
			return (Serializable)((JComponent)component).getClientProperty(key);
		}
		return null;
	}
}
