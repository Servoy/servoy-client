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

import java.text.ParseException;

import javax.swing.text.Document;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IComponentInheritedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.scripting.RuntimeDataButton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.text.ServoyMaskFormatter;

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

	@Override
	protected IModel<String> initModel()
	{
		// Search parents for CompoundPropertyModel
		for (Component current = getParent(); current != null; current = current.getParent())
		{
			// Get model
			IModel< ? > model = current.getDefaultModel();

			if (model instanceof IWrapModel< ? >)
			{
				model = ((IWrapModel< ? >)model).getWrappedModel();
			}

			if (model instanceof IComponentInheritedModel< ? >)
			{
				// we turn off versioning as we share the model with another
				// component that is the owner of the model (that component
				// has to decide whether to version or not
				setVersioned(false);

				// return the shared inherited
				return ((IComponentInheritedModel< ? >)model).wrapOnInheritance(this);
			}
		}

		// No model for this component!
		return null;
	}

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
		if (needEntireState && model instanceof RecordItemModel)
		{
			if (dataProviderID != null)
			{
				Object val = getModelObject();
				if (val instanceof byte[])
				{
					setIcon((byte[])val);
				}
				else if (icon != null)
				{
					setIcon(null);
				}
				else
				{
					try
					{
						ComponentFormat fp = getScriptObject().getComponentFormat();
						if (fp == null)
						{
							bodyText = Text.processTags((String)val, resolver);
						}
						else
						{
							bodyText = Text.processTags(
								TagResolver.formatObject(val, application.getLocale(), fp.parsedFormat,
									(fp.parsedFormat.getDisplayFormat() != null ? new ServoyMaskFormatter(fp.parsedFormat.getDisplayFormat(), true) : null)),
								resolver);
						}
					}
					catch (ParseException e)
					{
						Debug.error(e);
					}
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
					bodyText = StripHTMLTagsConverter.convertBodyText(this, bodyText, getScriptObject().trustDataAsHtml(),
						application.getFlattenedSolution()).getBodyTxt();
				}
				else
				{
					// convert the text
					final IConverter converter = getConverter(String.class);
					bodyText = converter.convertToString(bodyText, getLocale());
				}
			}
		}
		else
		{
			Object modelObject = getModelObject();
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
				ComponentFormat cf = getScriptObject().getComponentFormat();
				if (cf == null)
				{
					bodyText = Text.processTags(getDefaultModelObjectAsString(), resolver);
				}
				else
				{
					try
					{
						bodyText = TagResolver.formatObject(modelObject, application.getLocale(), cf.parsedFormat,
							(cf.parsedFormat.getDisplayFormat() != null ? new ServoyMaskFormatter(cf.parsedFormat.getDisplayFormat(), true) : null));
					}
					catch (ParseException e)
					{
						Debug.error(e);
					}
				}
				if (HtmlUtils.startsWithHtml(modelObject))
				{
					// ignore script/header contributions for now
					bodyText = StripHTMLTagsConverter.convertBodyText(this, bodyText, getScriptObject().trustDataAsHtml(),
						application.getFlattenedSolution()).getBodyTxt();
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

	@Override
	public String getInputName()
	{
		if (inputId == null)
		{
			Page page = findPage();
			if (page instanceof MainPage)
			{
				inputId = ((MainPage)page).nextInputNameId();
			}
			else
			{
				return super.getInputName();
			}
		}
		return inputId;
	}

	public Object getValueObject()
	{
		return getDefaultModelObject();
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
}
