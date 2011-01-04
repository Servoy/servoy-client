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

import java.awt.Component;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.apache.wicket.MetaDataKey;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
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
 * Factory implementation for Wicket GUI
 * 
 * @author jcompagner
 */
public class WebItemFactory implements ItemFactory
{
	private final IApplication application;

	private static final Map<Object, MetaDataKey> metaDataKeys = new HashMap<Object, MetaDataKey>();

	public WebItemFactory(IApplication application)
	{
		this.application = application;
	}

	public IStandardLabel createLabel(String name, String text)
	{
		return new WebBaseLabel(application, name, text);
	}

	public IComponent createPanel()
	{
		return null;//NOP in web side
	}


	public ILabel createBeanHolder(String name, Component obj)
	{
		if (obj instanceof JComponent)
		{
			return new WebImageBeanHolder(application, name, (JComponent)obj);
		}
		else
		{
			return new WebBeanHolder(application, name, obj);
		}
	}

	public IComponent createInvisibleBean(String name, Object obj)
	{
		return new WebBeanHolder(application, name, obj);
	}


	public IButton createScriptButton(String name)
	{
		return new WebScriptButton(application, name);
	}

	public ILabel createScriptLabel(String name, boolean hasActionListner)
	{
		if (!hasActionListner)
		{
			return new WebScriptLabel(application, name);
		}
		else return new WebScriptSubmitLink(application, name);
	}

	public IButton createDataButton(String name)
	{
		return new WebDataButton(application, name);
	}

	public ILabel createDataLabel(String name, boolean hasActionListner)
	{
		if (!hasActionListner)
		{
			return new WebDataLabel(application, name);
		}
		else return new WebDataSubmitLink(application, name);
	}

	public IFieldComponent createDataCalendar(String name)
	{
		return new WebDataCalendar(application, name);
	}

	public IFieldComponent createSelectBox(String name, String text, IValueList list, boolean isRadio)
	{
		return new WebDataCheckBox(application, name, text, list, isRadio);
	}

	public IFieldComponent createSelectBox(String name, String text, boolean isRadio)
	{
		return new WebDataCheckBox(application, name, text, isRadio);
	}

	public IFieldComponent createDataChoice(String name, IValueList list, boolean isRadioList)
	{
		if (isRadioList)
		{
			return new WebDataRadioChoice(application, name, list);
		}
		else
		{
			return new WebDataCheckBoxChoice(application, name, list);
		}
	}

	public IFieldComponent createDataComboBox(String name, IValueList list)
	{
		return new WebDataComboBox(application, name, list);
	}

	public IFieldComponent createDataField(String name)
	{
		return new WebDataField(application, name);
	}

	public IFieldComponent createDataImgMediaField(String name)
	{
		return new WebDataImgMediaField(application, name);
	}


	public IFieldComponent createDataPassword(String name)
	{
		//the template handeler makes it appier as password
		return new WebDataPasswordField(application, name);
	}

	public IFieldComponent createDataTextArea(String name)
	{
		return new WebDataTextArea(application, name);
	}

	public IFieldComponent createDataTextEditor(String name, int type, boolean willBeEditable)
	{
		if (type == ComponentFactory.HTML_AREA)
		{
			if (false && willBeEditable)
			{
				return new WebDataHtmlArea(application, name);
			}
			else
			{
				return new WebDataHtmlView(application, name);
			}
		}
		else
		{
			return new WebDataRtfField(application, name);
		}
	}

	public IPortalComponent createPortalComponent(Portal meta, Form form, IDataProviderLookup dataProviderLookup, IScriptExecuter el, boolean printing)
	{
		int endY = meta.getLocation().y + meta.getSize().height;
		return new WebCellBasedView(ComponentFactory.getWebID(meta), application, form, meta, dataProviderLookup, el, !meta.getMultiLine(),
			meta.getLocation().y, endY, meta.getSize().height);
	}


	public ITabPanel createTabPanel(String name, int orient, boolean oneTab)
	{
		return new WebTabPanel(application, name, orient, oneTab);
	}

	public ISplitPane createSplitPane(String name, int orient)
	{
		return new WebSplitPane(application, name, orient);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.LookupValueList)
	 */
	public IFieldComponent createDataLookupField(String name, LookupValueList lookupValueList)
	{
		//return new WebDataField(application,name);
		return new WebDataLookupField(application, name, lookupValueList);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.CustomValueList)
	 */
	public IFieldComponent createDataLookupField(String name, CustomValueList list)
	{
		return new WebDataLookupField(application, name, list);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IFieldComponent createDataLookupField(String name, final String serverName, final String tableName, String dataProviderID)
	{
		//return new WebDataField(application,name);
		return new WebDataLookupField(application, name, serverName, tableName, dataProviderID);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createRect(java.lang.String)
	 */
	public IRect createRect(String name, int type)
	{
		return new WebRect(name, application, type);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createShape(java.lang.String, com.servoy.j2db.persistence.Shape)
	 */
	public IComponent createShape(String name, Shape rec)
	{
		return new WebRect(name, application, rec.getShapeType());
	}

	/**
	 * Set a property on the runtime component
	 */
	public void setComponentProperty(Object component, Object key, Serializable value)
	{
		if (component instanceof org.apache.wicket.Component)
		{
			MetaDataKey metaDataKey = metaDataKeys.get(key);
			if (metaDataKey == null)
			{
				metaDataKey = new MetaDataKey<Serializable>()
				{
					private static final long serialVersionUID = 1L;
				};
				metaDataKeys.put(key, metaDataKey);
			}
			((org.apache.wicket.Component)component).setMetaData(metaDataKey, value);
		}
	}

	/**
	 * get a property of the runtime component
	 */
	public Serializable getComponentProperty(Object component, Object key)
	{
		if (component instanceof org.apache.wicket.Component)
		{
			MetaDataKey metaDataKey = metaDataKeys.get(key);
			if (metaDataKey != null)
			{
				return ((org.apache.wicket.Component)component).getMetaData(metaDataKey);
			}
		}
		return null;
	}
}
