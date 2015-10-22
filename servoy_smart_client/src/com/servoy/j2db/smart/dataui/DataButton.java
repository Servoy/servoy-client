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


import java.awt.event.MouseEvent;
import java.text.ParseException;

import javax.swing.Icon;
import javax.swing.text.Document;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.scripting.RuntimeDataButton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.text.ServoyMaskFormatter;

/**
 * Runtime swing button
 * @author jblok
 */
public class DataButton extends AbstractScriptButton implements IDisplayData, IDisplayTagText, ISupportOnRender
{
	private String dataProviderID;
	private Object value;

	private String tooltip;

	private String tagText = ""; //$NON-NLS-1$

	public DataButton(IApplication app, RuntimeDataButton scriptable)
	{
		super(app, scriptable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(String text)
	{
		if (text != null && text.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tooltip = text;
		}
		else
		{
			super.setToolTipText(text);
		}
	}

	protected boolean needEntireState;

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
	}

	public void setTagText(String s)
	{
		if (s != null)
		{
			tagText = s;
		}
	}


	/**
	 * @see com.servoy.j2db.ui.IDisplayTagText#getTagText()
	 */
	public String getTagText()
	{
		return tagText;
	}

	public Document getDocument()
	{
		return null;
	}

	@Override
	public String toString()
	{
		return "DataButton[" + getName() + ":" + dataProviderID + ":" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @see javax.swing.JComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		if (resolver != null && tooltip != null)
		{
			String oldValue = tooltip;
			tooltip = null;
			super.setToolTipText(Text.processTags(oldValue, resolver));
			tooltip = oldValue;
		}
		return super.getToolTipText();
	}

	/**
	 * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
	 */
	@Override
	public String getToolTipText(MouseEvent event)
	{
		return getToolTipText();
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object obj)
	{
		if (needEntireState)
		{
			if (resolver != null)
			{
				if (dataProviderID != null)
				{
					try
					{
						ComponentFormat cf = getScriptObject().getComponentFormat();
						setText(Text.processTags(
							obj != null ? TagResolver.formatObject(obj, application.getLocale(), cf.parsedFormat, (cf.parsedFormat.getDisplayFormat() != null
								? new ServoyMaskFormatter(cf.parsedFormat.getDisplayFormat(), true) : null)) : "", resolver));
					}
					catch (ParseException e)
					{
						Debug.error(e);
					}
				}
				else
				{
					setText(Text.processTags(tagText, resolver));
				}
			}
			else
			{
				setText(""); //$NON-NLS-1$
			}
			if (tooltip != null)
			{
				super.setToolTipText("button"); // empty tooltip will unregister from tooltip manager //$NON-NLS-1$
			}
		}
		else
		{
			if (obj != null)
			{
				if (obj instanceof byte[])
				{
					setIcon((byte[])obj);
				}
				else
				{
					ComponentFormat cf = getScriptObject().getComponentFormat();
					try
					{
						setText(TagResolver.formatObject(obj, application.getLocale(), cf.parsedFormat, (cf.parsedFormat.getDisplayFormat() != null
							? new ServoyMaskFormatter(cf.parsedFormat.getDisplayFormat(), true) : null)));
					}
					catch (ParseException e)
					{
						Debug.error(e);
					}
				}
			}
			else
			{
				if (value instanceof byte[])
				{
					setIcon((Icon)null);
				}
				else if (value != null)
				{
					setText(""); //$NON-NLS-1$
				}
			}
			this.value = obj;
		}

		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(hasFocus());
		}
	}

	public Object getValueObject()
	{
		return value;
	}

	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		//ignore
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
	}

	public void setValidationEnabled(boolean b)
	{
		//ignore
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

	public void setChangeCommand(String id)
	{
		//ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataprocessing.IDisplay#stopEditing()
	 */
	public boolean stopUIEditing(boolean looseFocus)
	{
		return true;
	}
}
