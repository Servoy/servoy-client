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
package com.servoy.j2db.ui;

import java.awt.Component;
import java.io.Serializable;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Shape;

/**
 * Factory interface to create elements
 * The implementation of this factory may deliver different GUI components for different frameworks (like Swing, or Wicket) 
 * 
 * @author jcompagner
 */
public interface ItemFactory
{
	/**
	 * @return
	 */
	IComponent createPanel();

	/**
	 * @param string
	 * @param stringIfPrefix
	 * @return
	 */
	IStandardLabel createLabel(String text, String string);

	/**
	 * @param obj
	 * @return
	 */
	IComponent createInvisibleBean(String name, Object obj);

	/**
	 * @param name
	 * @param obj
	 * @return
	 */
	IComponent createBeanHolder(String name, Component obj);


	/**
	 * @param name
	 * @return the rect created
	 */
	IRect createRect(String name, int type);

	/**
	 * @param name TODO
	 * @param orient
	 * @param b
	 * @return
	 */
	ITabPanel createTabPanel(String name, int orient, boolean b);

	/**
	 * @param name TODO
	 * @param orient
	 * @return
	 */
	ISplitPane createSplitPane(String name, int orient);

	/**
	 * @param meta
	 * @param dataProviderLookup
	 * @param el
	 * @param printing
	 * @return
	 */
	IPortalComponent createPortalComponent(Portal meta, Form form, IDataProviderLookup dataProviderLookup, IScriptExecuter el, boolean printing);


	/**
	 * @return
	 */
	IButton createDataButton(String name);

	/**
	 * @return
	 */
	IButton createScriptButton(String name);

	/**
	 * @return
	 */
	ILabel createScriptLabel(String name, boolean hasActionListner);

	/**
	 * @return
	 */
	ILabel createDataLabel(String name, boolean hasActionListner);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataPassword(String name);

	/**
	 * @param name TODO
	 * @param rtf_area
	 * @return
	 */
	IFieldComponent createDataTextEditor(String name, int type, boolean willBeEditable);


	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataTextArea(String name);

	/**
	 * @param name TODO
	 * @param list
	 * @param stringIfPrefix
	 * @return
	 */
	IFieldComponent createDataCheckBox(String name, String text, IValueList list);

	/**
	 * @param name TODO
	 * @param stringIfPrefix
	 * @return
	 */
	IFieldComponent createDataCheckBox(String name, String text);

	/**
	 * @param name TODO
	 * @param list
	 * @param b
	 * @return
	 */
	IFieldComponent createDataChoice(String name, IValueList list, boolean b);

	/**
	 * @param name TODO
	 * @param list
	 * @return
	 */
	IFieldComponent createDataComboBox(String name, IValueList list);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataCalendar(String name);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataImgMediaField(String name);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataField(String name);

	/**
	 * @param lookupValueList
	 * @param wicketIDParameter
	 */
	IFieldComponent createDataLookupField(String name, LookupValueList lookupValueList);

	/**
	 * @param wicketIDParameter
	 * @param list
	 */
	IFieldComponent createDataLookupField(String name, CustomValueList list);

	/**
	 * @param webID
	 * @param rec
	 * @return
	 */
	IComponent createShape(String webID, Shape rec);

	/**
	 * @param webID
	 * @param serverName
	 * @param tableName
	 * @return
	 */
	IFieldComponent createDataLookupField(String webID, String serverName, String tableName, String dataProviderID);

	/**
	 * Set a property on the runtime component
	 * 
	 * @param component
	 * @param key
	 * @param value
	 */
	void setComponentProperty(Object component, Object key, Serializable value);

	/**
	 * Get a property from the runtime component
	 * 
	 * @param component
	 * @param key
	 */
	Serializable getComponentProperty(Object component, Object key);
}
