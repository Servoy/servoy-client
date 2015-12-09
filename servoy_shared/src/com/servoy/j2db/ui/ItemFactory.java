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

import javax.swing.JComponent;
import javax.swing.JEditorPane;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.ui.scripting.AbstractRuntimeScrollableValuelistComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTextEditor;
import com.servoy.j2db.ui.scripting.RuntimeAccordionPanel;
import com.servoy.j2db.ui.scripting.RuntimeCheckbox;
import com.servoy.j2db.ui.scripting.RuntimeDataButton;
import com.servoy.j2db.ui.scripting.RuntimeDataCalendar;
import com.servoy.j2db.ui.scripting.RuntimeDataCombobox;
import com.servoy.j2db.ui.scripting.RuntimeDataField;
import com.servoy.j2db.ui.scripting.RuntimeDataLabel;
import com.servoy.j2db.ui.scripting.RuntimeDataLookupField;
import com.servoy.j2db.ui.scripting.RuntimeDataPassword;
import com.servoy.j2db.ui.scripting.RuntimeListBox;
import com.servoy.j2db.ui.scripting.RuntimeMediaField;
import com.servoy.j2db.ui.scripting.RuntimePortal;
import com.servoy.j2db.ui.scripting.RuntimeRadioButton;
import com.servoy.j2db.ui.scripting.RuntimeRectangle;
import com.servoy.j2db.ui.scripting.RuntimeScriptButton;
import com.servoy.j2db.ui.scripting.RuntimeScriptLabel;
import com.servoy.j2db.ui.scripting.RuntimeSpinner;
import com.servoy.j2db.ui.scripting.RuntimeSplitPane;
import com.servoy.j2db.ui.scripting.RuntimeTabPanel;
import com.servoy.j2db.ui.scripting.RuntimeTextArea;

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
	IStandardLabel createLabel(String name, String text);

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
	IComponent createBeanHolder(String name, Component obj, int anchoring);


	/**
	 * @param name
	 * @return the rect created
	 */
	IRect createRect(RuntimeRectangle scriptable, String name, int type);

	/**
	 *
	 * @param scriptable
	 * @param name
	 * @param orient
	 * @param oneTab
	 * @return
	 */
	ITabPanel createTabPanel(RuntimeTabPanel scriptable, String name, int orient, boolean oneTab);

	/**
	 * @param scriptable
	 * @param name
	 * @return
	 */
	ITabPanel createAccordionPanel(RuntimeAccordionPanel scriptable, String name);

	/**
	 * @param scriptable
	 * @param name
	 * @param orient
	 * @return
	 */
	ISplitPane createSplitPane(RuntimeSplitPane scriptable, String name, int orient);

	/**
	 * @param meta
	 * @param dataProviderLookup
	 * @param el
	 * @param printing
	 * @return
	 */
	IPortalComponent createPortalComponent(RuntimePortal scriptable, Portal meta, Form form, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing);


	/**
	 * @return
	 */
	IButton createDataButton(RuntimeDataButton scriptable, String name);

	/**
	 * @return
	 */
	IButton createScriptButton(RuntimeScriptButton scriptable, String name);

	/**
	 * @return
	 */
	ILabel createScriptLabel(RuntimeScriptLabel scriptable, String name, boolean hasActionListener);

	/**
	 * @return
	 */
	ILabel createDataLabel(RuntimeDataLabel scriptable, String name, boolean hasActionListener);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataPassword(RuntimeDataPassword scriptable, String name);

	/**
	 * @param name TODO
	 * @param rtf_area
	 * @return
	 */
	IFieldComponent createDataTextEditor(AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String name, int type, boolean willBeEditable);


	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataTextArea(RuntimeTextArea scriptable, String name);

	/**
	 * @param name TODO
	 * @param list
	 * @param stringIfPrefix
	 * @return
	 */
	IFieldComponent createRadioButton(RuntimeRadioButton scriptable, String name, String text, IValueList list);

	/**
	 * @param name TODO
	 * @param list
	 * @param stringIfPrefix
	 * @return
	 */
	IFieldComponent createCheckBox(RuntimeCheckbox scriptable, String name, String text, IValueList list);

	/**
	 * @param name TODO
	 * @param list
	 * @param b
	 * @param multiselect
	 * @return
	 */
	IFieldComponent createDataChoice(AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable, String name, IValueList list,
		boolean b, boolean multiselect);


	IFieldComponent createListBox(RuntimeListBox scriptable, String name, IValueList list, boolean multiSelect);

	IFieldComponent createSpinner(RuntimeSpinner scriptable, String name, IValueList list);

	/**
	 * @param name TODO
	 * @param list
	 * @return
	 */
	IFieldComponent createDataComboBox(RuntimeDataCombobox scriptable, String name, IValueList list);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataCalendar(RuntimeDataCalendar scriptable, String name);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataImgMediaField(RuntimeMediaField scriptable, String name);

	/**
	 * @param name TODO
	 * @return
	 */
	IFieldComponent createDataField(RuntimeDataField scriptable, String name);

	/**
	 * @param lookupValueList
	 * @param wicketIDParameter
	 */
	IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, LookupValueList lookupValueList);

	/**
	 * @param wicketIDParameter
	 * @param list
	 */
	IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, CustomValueList list);

	/**
	 * @param webID
	 * @param serverName
	 * @param tableName
	 * @return
	 */
	IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String webID, String serverName, String tableName, String dataProviderID);

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

	/**
	 * Create the changes recorder for the runtime component.
	 */
	IStylePropertyChangesRecorder createChangesRecorder();
}
