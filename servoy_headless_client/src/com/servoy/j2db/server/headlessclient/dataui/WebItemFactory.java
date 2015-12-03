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
import javax.swing.JEditorPane;

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
		RuntimeScriptLabel scriptable = new RuntimeScriptLabel(createChangesRecorder(), application);
		ILabel label = new WebBaseLabel(application, scriptable, name, text);
		scriptable.setComponent(label, null);
		return label;
	}

	public IComponent createPanel()
	{
		return null;//NOP in web side
	}


	public ILabel createBeanHolder(String name, Component obj, int anchoring)
	{
		ILabel beanHolder;
		if (obj instanceof JComponent)
		{
			RuntimeScriptButton scriptable = new RuntimeScriptButton(createChangesRecorder(), application);
			beanHolder = new WebImageBeanHolder(application, scriptable, name, (JComponent)obj, anchoring);
			scriptable.setComponent((IButton)beanHolder, null);
		}
		else
		{
			RuntimeScriptLabel scriptable = new RuntimeScriptLabel(createChangesRecorder(), application);
			beanHolder = new WebBeanHolder(application, scriptable, name, obj);
			scriptable.setComponent(beanHolder, null);
		}
		return beanHolder;
	}

	public IComponent createInvisibleBean(String name, Object obj)
	{
		RuntimeScriptLabel scriptable = new RuntimeScriptLabel(createChangesRecorder(), application);
		ILabel invisibleBean = new WebBeanHolder(application, scriptable, name, obj);
		scriptable.setComponent(invisibleBean, null);
		return invisibleBean;
	}


	public IButton createScriptButton(RuntimeScriptButton scriptable, String name)
	{
		return new WebScriptButton(application, scriptable, name);
	}

	public ILabel createScriptLabel(RuntimeScriptLabel scriptable, String name, boolean hasActionListener)
	{
		if (!hasActionListener)
		{
			return new WebScriptLabel(application, scriptable, name);
		}
		return new WebScriptSubmitLink(application, scriptable, name);
	}

	public IButton createDataButton(RuntimeDataButton scriptable, String name)
	{
		return new WebDataButton(application, scriptable, name);
	}

	public ILabel createDataLabel(RuntimeDataLabel scriptable, String name, boolean hasActionListener)
	{
		if (!hasActionListener)
		{
			return new WebDataLabel(application, scriptable, name);
		}
		return new WebDataSubmitLink(application, scriptable, name);
	}

	public IFieldComponent createDataCalendar(RuntimeDataCalendar scriptable, String name)
	{
		return new WebDataCalendar(application, scriptable, name);
	}

	public IFieldComponent createCheckBox(RuntimeCheckbox scriptable, String name, String text, IValueList list)
	{
		return new WebDataCheckBox(application, scriptable, name, text, list);
	}

	public IFieldComponent createRadioButton(RuntimeRadioButton scriptable, String name, String text, IValueList list)
	{
		return new WebDataRadioButton(application, scriptable, name, text, list);
	}

	public IFieldComponent createDataChoice(AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable, String name, IValueList list,
		boolean isRadioList, boolean multiselect)
	{
		if (isRadioList)
		{
			return new WebDataRadioChoice(application, scriptable, name, list);
		}
		return new WebDataCheckBoxChoice(application, scriptable, name, list, multiselect);
	}

	public IFieldComponent createDataComboBox(RuntimeDataCombobox scriptable, String name, IValueList list)
	{
		return new WebDataComboBox(application, scriptable, name, list);
	}

	public IFieldComponent createDataField(RuntimeDataField scriptable, String name)
	{
		return new WebDataField(application, scriptable, name);
	}

	public IFieldComponent createDataImgMediaField(RuntimeMediaField scriptable, String name)
	{
		return new WebDataImgMediaField(application, scriptable, name);
	}


	public IFieldComponent createDataPassword(RuntimeDataPassword scriptable, String name)
	{
		//the template handeler makes it appier as password
		return new WebDataPasswordField(application, scriptable, name);
	}

	public IFieldComponent createDataTextArea(RuntimeTextArea scriptable, String name)
	{
		return new WebDataTextArea(application, scriptable, name);
	}

	public IFieldComponent createDataTextEditor(AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String name, int type,
		boolean willBeEditable)
	{
		if (type == ComponentFactory.HTML_AREA)
		{
			if (willBeEditable)
			{
				return new WebDataHtmlArea(application, scriptable, name);
			}
			return new WebDataHtmlView(application, scriptable, name);
		}
		return new WebDataRtfField(application, scriptable, name);
	}

	public IPortalComponent createPortalComponent(RuntimePortal scriptable, Portal meta, Form form, IDataProviderLookup dataProviderLookup, IScriptExecuter el,
		boolean printing)
	{
		int endY = meta.getLocation().y + meta.getSize().height;
		return new WebCellBasedView(ComponentFactory.getWebID(form, meta), application, scriptable, form, meta, dataProviderLookup, el, !meta.getMultiLine(),
			meta.getLocation().y, endY, meta.getSize().height, -1);
	}


	public ITabPanel createTabPanel(RuntimeTabPanel scriptable, String name, int orient, boolean oneTab)
	{
		return new WebTabPanel(application, scriptable, name, orient, oneTab);
	}

	public ITabPanel createAccordionPanel(RuntimeAccordionPanel scriptable, String name)
	{
		return new WebAccordionPanel(application, scriptable, name);
	}

	public ISplitPane createSplitPane(RuntimeSplitPane scriptable, String name, int orient)
	{
		return new WebSplitPane(application, scriptable, name, orient);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.LookupValueList)
	 */
	public IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, LookupValueList lookupValueList)
	{
		return new WebDataLookupField(application, scriptable, name, lookupValueList);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, com.servoy.j2db.dataprocessing.CustomValueList)
	 */
	public IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, CustomValueList list)
	{
		return new WebDataLookupField(application, scriptable, name, list);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createDataLookupField(java.lang.String, java.lang.String, java.lang.String)
	 */
	public IFieldComponent createDataLookupField(RuntimeDataLookupField scriptable, String name, final String serverName, final String tableName,
		String dataProviderID)
	{
		return new WebDataLookupField(application, scriptable, name, serverName, tableName, dataProviderID);
	}

	/**
	 * @see com.servoy.j2db.ui.ItemFactory#createRect(java.lang.String)
	 */
	public IRect createRect(RuntimeRectangle scriptable, String name, int type)
	{
		return new WebRect(name, application, scriptable, type);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ItemFactory#createChangesRecorder()
	 */
	public IStylePropertyChangesRecorder createChangesRecorder()
	{
		return new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ItemFactory#createListBox(com.servoy.j2db.ui.scripting.RuntimeListBox, java.lang.String,
	 * com.servoy.j2db.dataprocessing.IValueList, boolean)
	 */
	public IFieldComponent createListBox(RuntimeListBox scriptable, String name, IValueList list, boolean multiSelect)
	{
		return new WebDataListBox(application, scriptable, name, list, multiSelect);
	}

	public IFieldComponent createSpinner(RuntimeSpinner scriptable, String name, IValueList list)
	{
		return new WebDataSpinner(application, scriptable, name, list);
	}

}
