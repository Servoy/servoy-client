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
import javax.swing.JEditorPane;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.ui.DummyChangesRecorder;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IPortalComponent;
import com.servoy.j2db.ui.IRect;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStandardLabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.ItemFactory;
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

	public ITabPanel createTabPanel(RuntimeTabPanel scriptable, String name, int orient, boolean oneTab)
	{
		ITabPanel tabPanel = new SpecialTabPanel(application, scriptable, orient, oneTab);
		tabPanel.setName(name);
		return tabPanel;
	}

	public ITabPanel createAccordionPanel(RuntimeAccordionPanel scriptable, String name)
	{
		ITabPanel tabPanel = new SpecialTabPanel(application, scriptable, TabPanel.ACCORDION_PANEL, false);
		tabPanel.setName(name);
		return tabPanel;
	}

	public ISplitPane createSplitPane(RuntimeSplitPane scriptable, String name, int orient)
	{
		ISplitPane splitPane = new SpecialSplitPane(application, scriptable, orient, false);
		splitPane.setName(name);
		return splitPane;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createPortalComponent(com.servoy.j2db.persistence.Portal, com.servoy.j2db.persistence.IDataProviderLookup,
	 * com.servoy.j2db.IScriptExecuter, boolean)
	 */
	public IPortalComponent createPortalComponent(RuntimePortal scriptable, Portal meta, Form form, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing)
	{
		return new PortalComponent(application, scriptable, form, meta, el, printing);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataButton()
	 */
	public IButton createDataButton(RuntimeDataButton scriptable, String name)
	{
		DataButton db = new DataButton(application, scriptable);
		db.setName(name);
		return db;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createScriptButton()
	 */
	public IButton createScriptButton(RuntimeScriptButton scriptable, String name)
	{
		ScriptButton sb = new ScriptButton(application, scriptable);
		sb.setName(name);
		return sb;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createScriptLabel()
	 */
	public ILabel createScriptLabel(RuntimeScriptLabel scriptable, String name, boolean hasActionListener)
	{
		ScriptLabel sl = new ScriptLabel(application, scriptable);
		sl.setName(name);
		return sl;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataLabel()
	 */
	public ILabel createDataLabel(RuntimeDataLabel scriptable, String name, boolean hasActionListener)
	{
		DataLabel dl = new DataLabel(application, scriptable);
		dl.setName(name);
		return dl;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataPassword()
	 */
	public IFieldComponent createDataPassword(RuntimeDataPassword scriptable, String name)
	{
		DataPassword dp = new DataPassword(application, scriptable);
		dp.setName(name);
		return dp;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataTextEditor(int)
	 */
	public IFieldComponent createDataTextEditor(AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String name, int type,
		boolean willBeEditable)
	{
		DataTextEditor dte = new DataTextEditor(application, scriptable, type);
		dte.setName(name);
		return dte;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataTextArea()
	 */
	public IFieldComponent createDataTextArea(RuntimeTextArea scriptable, String name)
	{
		DataTextArea dta = new DataTextArea(application, scriptable);
		dta.setName(name);
		return dta;
	}

	public IFieldComponent createRadioButton(RuntimeRadioButton scriptable, String name, String text, IValueList list)
	{
		DataRadioButton rb = new DataRadioButton(application, scriptable, text, list);
		rb.setName(name);
		return rb;
	}

	public IFieldComponent createCheckBox(RuntimeCheckbox scriptable, String name, String text, IValueList list)
	{
		DataCheckBox dcb = new DataCheckBox(application, scriptable, text, list);
		dcb.setName(name);
		return dcb;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataChoice(com.servoy.j2db.dataprocessing.IValueList, boolean)
	 */
	public IFieldComponent createDataChoice(AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable, String name, IValueList list,
		boolean isRadio, boolean multiselect)
	{
		DataChoice dc = new DataChoice(application, scriptable, list, isRadio ? Field.RADIOS : Field.CHECKS, multiselect);
		dc.setName(name);
		return dc;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataComboBox(com.servoy.j2db.dataprocessing.IValueList)
	 */
	public IFieldComponent createDataComboBox(RuntimeDataCombobox scriptable, String name, IValueList list)
	{
		DataComboBox dcb = new DataComboBox(application, scriptable, list);
		dcb.setName(name);
		return dcb;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataCalendar()
	 */
	public IFieldComponent createDataCalendar(RuntimeDataCalendar scriptable, String name)
	{
		DataCalendar dc = new DataCalendar(application, scriptable);
		dc.setName(name);
		return dc;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataImgMediaField()
	 */
	public IFieldComponent createDataImgMediaField(RuntimeMediaField scriptable, String name)
	{
		DataImgMediaField field = new DataImgMediaField(application, scriptable);
		field.setName(name);
		return field;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ItemFactory#createDataField()
	 */
	public IFieldComponent createDataField(RuntimeDataField scriptable, String name)
	{
		DataField df = new DataField(application, scriptable);
		df.setName(name);
		return df;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.LookupValueList)
	 */
	public IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, LookupValueList lookupValueList)
	{
		DataLookupField dlf = new DataLookupField(application, scriptable, lookupValueList);
		dlf.setName(name);
		return dlf;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.CustomValueList)
	 */
	public IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, CustomValueList list)
	{
		DataLookupField dlf = new DataLookupField(application, scriptable, list);
		dlf.setName(name);
		return dlf;
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, String serverName, String tableName, String dataProviderID)
	{
		DataLookupField dlf = new DataLookupField(application, scriptable, serverName, tableName, dataProviderID);
		dlf.setName(name);
		return dlf;
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createRect(java.lang.String)
	 */
	public IRect createRect(RuntimeRectangle scriptable, String name, int type)
	{
		return new Rect(application, scriptable, type);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ItemFactory#createChangesRecorder()
	 */
	public IStylePropertyChangesRecorder createChangesRecorder()
	{
		return DummyChangesRecorder.INSTANCE;
	}

	public IFieldComponent createListBox(RuntimeListBox scriptable, String name, IValueList list, boolean multiSelect)
	{
		DataChoice dc = new DataChoice(application, scriptable, list, multiSelect ? Field.MULTISELECT_LISTBOX : Field.LIST_BOX, multiSelect);
		dc.setName(name);
		return dc;
	}

	public IFieldComponent createSpinner(RuntimeSpinner scriptable, String name, IValueList list)
	{
		DataSpinner dc = new DataSpinner(application, scriptable, list);
		dc.setName(name);
		return dc;
	}

}
