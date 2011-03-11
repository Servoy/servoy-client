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

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.InvisibleBean;
import com.servoy.j2db.component.VisibleBean;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Shape;
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

/**
 * Factory implementation for Swing GUI
 * 
 * @author jcompagner
 */
public class SwingItemFactory implements ItemFactory
{
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


	public IComponent createBeanHolder(String name, Component obj, int anchoring)
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
		ISplitPane splitPane = new SpecialSplitPane(application, orient, false);
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
	public IFieldComponent createSelectBox(String name, String text, IValueList list, boolean isRadio)
	{
		if (isRadio)
		{
			DataRadioButton rb = new DataRadioButton(application, text, list);
			rb.setName(name);
			return rb;
		}
		else
		{
			DataCheckBox dcb = new DataCheckBox(application, text, list);
			dcb.setName(name);
			return dcb;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ItemFactory#createDataCheckBox(java.lang.String)
	 */
	public IFieldComponent createSelectBox(String name, String text, boolean isRadio)
	{
		return createSelectBox(name, text, null, isRadio);
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
