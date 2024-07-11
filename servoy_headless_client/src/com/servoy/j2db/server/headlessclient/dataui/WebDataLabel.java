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

import javax.swing.text.Document;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.scripting.AbstractRuntimeRendersupportComponent;
import com.servoy.j2db.ui.scripting.IFormatScriptComponent;

/**
* Represents a label in the browser that displays data (has a dataprovider)
*
* @author jcompagner
*/
public class WebDataLabel extends WebBaseLabel implements IDisplayData, IDisplayTagText
{
	private static final long serialVersionUID = 1L;

	protected String dataProviderID;
	private String tagText;
	private Object value;
	//private String tooltip;
	private boolean needEntireState;
	private boolean hasHTML;

	public WebDataLabel(IApplication application, AbstractRuntimeRendersupportComponent< ? extends IComponent> scriptable, String id)
	{
		super(application, scriptable, id);
	}

	@Override
	public void setText(String txt)
	{
		//ignore, we don't want a model as created in super class, but data from record
	}

	private CharSequence bodyText;

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	protected ComponentFormat getComponentFormat()
	{
		if (getScriptObject() instanceof IFormatScriptComponent)
		{
			return ((IFormatScriptComponent)getScriptObject()).getComponentFormat();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseLabel#hasHtml()
	 */
	@Override
	protected boolean hasHtmlOrImage()
	{
		return hasHTML || super.hasHtmlOrImage() || getValueObject() instanceof byte[];
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseLabel#getBodyText()
	 */
	@Override
	protected CharSequence getBodyText()
	{
		return bodyText;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValue()
	 */
	public Object getValueObject()
	{
		return value;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#setValue(java.lang.Object)
	 */
	public void setValueObject(Object obj)
	{
		value = obj;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListener()
	 */
	public boolean needEditListener()
	{
		return false;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
	 */
	public boolean needEntireState()
	{
		return needEntireState;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#setNeedEntireState(boolean)
	 */
	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;

	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#addEditListener(com.servoy.j2db.dataprocessing.IEditListener)
	 */
	public void addEditListener(IEditListener l)
	{
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#notifyLastNewValueWasChange(java.lang.Object)
	 */
	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		//ignore
	}

	public void setValidationEnabled(boolean b)
	{
	}

	public boolean isValueValid()
	{
		return true;
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		//ignore
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		return true;
	}

	/**
	 * @see com.servoy.j2db.ui.IDisplayTagText#setTagText(java.lang.String)
	 */
	public void setTagText(String tagText)
	{
		this.tagText = tagText;
	}

	/**
	 * @see com.servoy.j2db.ui.IDisplayTagText#getTagText()
	 */
	public String getTagText()
	{
		return tagText;
	}
}
