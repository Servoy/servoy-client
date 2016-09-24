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
import java.util.Iterator;
import java.util.List;

import javax.swing.text.Document;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.IComponentInheritedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.value.IValueMap;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.dataui.StripHTMLTagsConverter.StrippedText;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;
import com.servoy.j2db.ui.scripting.IFormatScriptComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.text.ServoyMaskFormatter;

/**
 * Represents a label in the browser that displays data (has a dataprovider) and on on action event.
 *
 * @author jcompagner
 */
public class WebDataSubmitLink extends WebBaseSubmitLink implements IDisplayData, IDisplayTagText, IHeaderContributor
{
	private static final long serialVersionUID = 1L;

	private final static String CSS_OPEN_TAG = "<style type=\"text/css\"><!--/*--><![CDATA[/*><!--*/\n"; //$NON-NLS-1$
	private final static String CSS_CLOSE_TAG = "\n/*-->]]>*/</style>\n"; //$NON-NLS-1$

	private boolean needEntireState;
	private String dataProviderID;
	private String tagText;
	//private String tooltip;
	private String inputId;
	private boolean hasHTML;

	private StrippedText strippedText = new StripHTMLTagsConverter.StrippedText();
	protected ITagResolver resolver;
	private String bodyText;

	public WebDataSubmitLink(IApplication application, AbstractRuntimeBaseComponent scriptable, String id)
	{
		super(application, scriptable, id);
	}


	@Override
	public void setText(String txt)
	{
		//ignore, we don't want a model as created in super class, but data from record
	}

	@Override
	protected IModel< ? > initModel()
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
				// ct that is the owner of the model (that component
				// has to decide whether to version or not
				setVersioned(false);

				// return the shared inherited
				model = ((IComponentInheritedModel< ? >)model).wrapOnInheritance(this);
				return model;
			}
		}

		// No model for this component!
		return null;
	}

	@Override
	public IConverter getConverter(Class< ? > cls)
	{
		return getApplication().getConverterLocator().getConverter(cls);
	}

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/**
	 * @see org.apache.wicket.markup.html.link.AbstractLink#onBeforeRender()
	 */
	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();

		strippedText = new StripHTMLTagsConverter.StrippedText();

		IModel< ? > model = getInnermostModel();
		hasHTML = false;

		bodyText = null;
		if (needEntireState && model instanceof RecordItemModel)
		{
			if (dataProviderID != null)
			{
				Object value = getDefaultModelObject();
				if (value instanceof byte[])
				{
					setIcon((byte[])value);
					if (model instanceof RecordItemModel)
					{
						((RecordItemModel)model).updateRenderedValue(this);
					}
					return;
				}
				if (icon != null) setIcon(null);
				if (value instanceof String)
				{
					ComponentFormat cf = getComponentFormat();
					if (cf == null)
					{
						bodyText = Text.processTags((String)value, resolver);
					}
					else
					{
						try
						{
							bodyText = Text.processTags(
								TagResolver.formatObject(value, application.getLocale(), cf.parsedFormat,
									(cf.parsedFormat.getDisplayFormat() != null ? new ServoyMaskFormatter(cf.parsedFormat.getDisplayFormat(), true) : null)),
								resolver);
						}
						catch (ParseException e)
						{
							Debug.error(e);
						}
					}

				}
				else
				{
					bodyText = getDefaultModelObjectAsString();
				}
			}
			else
			{
				bodyText = Text.processTags(tagText, resolver);
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
				ComponentFormat cf = getComponentFormat();
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
			}
		}
		if (HtmlUtils.startsWithHtml(bodyText))
		{
			strippedText = StripHTMLTagsConverter.convertBodyText(this, bodyText, getScriptObject().trustDataAsHtml(), application.getFlattenedSolution());
			hasHTML = true;
		}
		else
		{
			strippedText.setBodyTxt(bodyText);
		}

		if (model instanceof RecordItemModel)
		{
			((RecordItemModel)model).updateRenderedValue(this);
		}
	}

	protected ComponentFormat getComponentFormat()
	{
		if (getScriptObject() instanceof IFormatScriptComponent)
		{
			return ((IFormatScriptComponent)getScriptObject()).getComponentFormat();
		}
		return null;
	}

	/**
	 * @see org.apache.wicket.markup.html.IHeaderContributor#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
	 */
	public void renderHead(IHeaderResponse response)
	{
		List<CharSequence> lst = strippedText.getJavascriptUrls();
		for (int i = 0; i < lst.size(); i++)
		{
			response.renderJavascriptReference(lst.get(i).toString());
		}
		lst = strippedText.getJavascriptScripts();
		for (int i = 0; i < lst.size(); i++)
		{
			response.renderJavascript(lst.get(i), "js_" + getMarkupId() + lst.get(i).hashCode()); //$NON-NLS-1$
		}
		lst = strippedText.getLinkTags();
		for (int i = 0; i < lst.size(); i++)
		{
			response.renderString(lst.get(i));
		}

		lst = strippedText.getStyles();
		for (CharSequence style : lst)
		{
			response.renderString(CSS_OPEN_TAG + style + CSS_CLOSE_TAG);
		}
		IValueMap map = strippedText.getBodyAttributes();
		if (map != null && map.size() > 0)
		{
			String onLoad = null;
			Iterator<String> iterator = map.keySet().iterator();
			while (iterator.hasNext())
			{
				String attributeName = iterator.next();
				if (attributeName.equalsIgnoreCase("onload")) //$NON-NLS-1$
				{
					onLoad = map.getString(attributeName);
					iterator.remove();
					break;
				}
			}
			if (onLoad != null)
			{
				response.renderOnLoadJavascript(onLoad);
			}
			Page findPage = findPage();
			if (findPage instanceof MainPage)
			{
				((MainPage)findPage).addBodyAttributes(map);
			}
		}
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseSubmitLink#onComponentTagBody(wicket.markup.MarkupStream, wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		CharSequence bodyText = strippedText.getBodyTxt();
		instrumentAndReplaceBody(markupStream, openTag, bodyText);
	}

	@Override
	protected boolean hasHtmlOrImage()
	{
		return hasHTML || super.hasHtmlOrImage() || getDefaultModelObject() instanceof byte[];
	}

	@Override
	protected CharSequence getBodyText()
	{
		return strippedText.getBodyTxt();
	}

	/**
	 * @see wicket.markup.html.form.FormComponent#getInputName()
	 */
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

	public void addEditListener(IEditListener l)
	{
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;

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
		// ignore
	}

	public boolean isValueValid()
	{
		return true;
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		// ignore
	}

	public void setValidationEnabled(boolean b)
	{
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

	public boolean isReadOnly()
	{
		return !isEnabled();
	}
}
