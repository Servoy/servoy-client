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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;

import javax.swing.text.Document;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.printing.IFixedPreferredWidth;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.scripting.RuntimeDataLabel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.text.ServoyMaskFormatter;

/**
 * Runtime swing label component
 * @author jblok, jcompagner
 */
public class DataLabel extends AbstractScriptLabel implements IDisplayData, IDisplayTagText, IFixedPreferredWidth, PropertyChangeListener, ISupportOnRender
{
	private String dataProviderID;
	private Object value;
	private String tooltip;

	public DataLabel(IApplication app, RuntimeDataLabel scriptable)
	{
		super(app, scriptable);
		scriptable.addPropertyChangeListener(this);
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

	public Object getValueObject()
	{
		return value;
	}

	public Document getDocument()
	{
		return null;
	}

	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		//ignore
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

	private String tagText;

	public void setTagText(String s)
	{
		tagText = s;
	}


	/**
	 * @see com.servoy.j2db.ui.IDisplayTagText#getTagText()
	 */
	public String getTagText()
	{
		return tagText;
	}

	@Override
	public String toString()
	{
		return "DataLabel[" + getName() + ":" + dataProviderID + ":" + getValueObject() + "]"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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

	private Object rawValue;

	private void resetRawValue()
	{
		value = null;
		setValueObject(rawValue);
	}

	public void setValueObject(Object obj)
	{
		this.rawValue = obj;
		if (needEntireState)
		{
			String txt = "";

			if (resolver != null)
			{
				if (dataProviderID != null)
				{
					try
					{
						ComponentFormat fp = getScriptObject().getComponentFormat();
						txt = Text.processTags(TagResolver.formatObject(obj != null ? obj : "", application.getLocale(), fp.parsedFormat,
							(fp.parsedFormat.getDisplayFormat() != null ? new ServoyMaskFormatter(fp.parsedFormat.getDisplayFormat(), true) : null)), resolver);
					}
					catch (ParseException e)
					{
						Debug.error(e);
					}
				}
				else
				{
					txt = Text.processTags(tagText, resolver);
				}
				if (txt == null) txt = "";
			}

			if (!txt.equals(value))
			{
				setText(txt);
				value = txt;
			}
			if (tooltip != null)
			{
				super.setToolTipText(tooltip);
			}
		}
		else
		{
			if (obj != null)
			{
				if (obj.equals(value))
				{
					if (scriptable != null && scriptable.getRenderEventExecutor().isRenderStateChanged()) fireOnRender(false);
					return;
				}

				if (obj instanceof byte[])
				{
					setIconDirect((byte[])obj, getNextSeq());
//					setIcon(new ImageIcon((byte[])obj));
				}
				else
				{
					try
					{
						ComponentFormat fp = getScriptObject().getComponentFormat();
						setText(TagResolver.formatObject(obj, application.getLocale(), fp.parsedFormat, (fp.parsedFormat.getDisplayFormat() != null
							? new ServoyMaskFormatter(fp.parsedFormat.getDisplayFormat(), true) : null)));
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
					setIcon((byte[])null);
				}
				else
				{
					setText(""); //$NON-NLS-1$
				}
			}
			this.value = obj;
			if (tooltip != null)
			{
				super.setToolTipText(tooltip);
			}
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

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
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

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplay#stopEditing()
	 */
	public boolean stopUIEditing(boolean looseFocus)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("format".equals(evt.getPropertyName()))
		{
			resetRawValue();
		}
	}
}