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
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.scripting.RuntimeDataButton;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a Button in a browser that also displays data (has a dataprovider)
 *
 * @author jcompagner
 */
public class WebDataButton extends WebBaseButton implements IDisplayData, IDisplayTagText
{
	private static final long serialVersionUID = 1L;

	private String dataProviderID;
	private String tagText;
	//private String tooltip;
	private String inputId;

	private CharSequence bodyText;

	public WebDataButton(IApplication application, RuntimeDataButton scriptable, String id)
	{
		super(application, scriptable, id);
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(null, null);
	}

	@Override
	public void setText(String txt)
	{
		//ignore, we don't want a model as created in super class, but data from record
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object value)
	{

		if (dataProviderID == null && needEntireState)
		{
			CharSequence current = Text.processTags(tagText, resolver);
			if (current != null && bodyText != null)
			{
				if (!Utils.equalObjects(bodyText.toString(), current.toString())) getScriptObject().getChangesRecorder().setChanged();
			}
			else if (current != null || bodyText != null)
			{
				getScriptObject().getChangesRecorder().setChanged();
			}
		}
		else
		{
			((ChangesRecorder)getScriptObject().getChangesRecorder()).testChanged(this, value);

		}
	}

	public boolean needEditListener()
	{
		return false;
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	private boolean needEntireState;

	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;

	}

	public void addEditListener(IEditListener l)
	{
		// TODO Auto-generated method stub

	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	public Document getDocument()
	{
		return null;
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		//ignore
	}

	public boolean isValueValid()
	{
		return true;
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		//ignore
	}

	public void setValidationEnabled(boolean b)
	{
		//ignore
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		return true;
	}


	public void setTagText(String tagText)
	{
		this.tagText = tagText;
	}

	public String getTagText()
	{
		return tagText;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValueObject()
	 */
	@Override
	public Object getValueObject()
	{
		return null;
	}
}
