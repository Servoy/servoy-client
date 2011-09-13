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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Text;

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

	public WebDataLabel(IApplication application, AbstractRuntimeBaseComponent< ? > scriptable, String id)
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

	/**
	 * @see org.apache.wicket.Component#onBeforeRender()
	 */
	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();

		IModel< ? > model = getInnermostModel();
		hasHTML = false;
		if (needEntireState && model instanceof RecordItemModel)
		{
			if (dataProviderID != null)
			{
				Object val = getDefaultModelObject();
				if (val instanceof byte[])
				{
					setIcon((byte[])val);
				}
				else if (icon != null)
				{
					setIcon(null);
				}
				else if (val instanceof String)
				{
					bodyText = Text.processTags((String)val, resolver);
				}
				else
				{
					bodyText = TagResolver.formatObject(val, application.getSettings());
				}
			}
			else
			{
				bodyText = Text.processTags(tagText, resolver);
			}
			if (bodyText != null)
			{
				if (HtmlUtils.startsWithHtml(bodyText))
				{
					bodyText = StripHTMLTagsConverter.convertBodyText(this, bodyText, application.getFlattenedSolution()).getBodyTxt();
					hasHTML = true;
				}
				else
				{
					// convert the text (strip html if needed)
					final IConverter converter = getConverter(String.class);
					bodyText = converter.convertToString(bodyText, getLocale());
				}
			}
		}
		else
		{
			Object modelObject = getDefaultModelObject();
			if (modelObject instanceof byte[])
			{
				setIcon((byte[])modelObject);
			}
			else if (icon != null)
			{
				setIcon(null);
			}
			else
			{
				bodyText = TagResolver.formatObject(modelObject, application.getSettings());
				if (HtmlUtils.startsWithHtml(modelObject))
				{
					// ignore script/header contributions for now
					bodyText = StripHTMLTagsConverter.convertBodyText(this, bodyText, application.getFlattenedSolution()).getBodyTxt();
					hasHTML = true;
				}
			}
		}

		if (model instanceof RecordItemModel)
		{
			((RecordItemModel)model).updateRenderedValue(this);
		}
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseSubmitLink#onComponentTagBody(wicket.markup.MarkupStream, wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		instrumentAndReplaceBody(markupStream, openTag, bodyText);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseLabel#hasHtml()
	 */
	@Override
	protected boolean hasHtml()
	{
		return hasHTML || super.hasHtml();
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
		((ChangesRecorder)getScriptObject().getChangesRecorder()).testChanged(this, obj);
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

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getFormat()
	 */
	public String getFormat()
	{
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub

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
