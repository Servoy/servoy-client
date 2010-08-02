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

import java.util.ArrayList;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.string.AppendingStringBuffer;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.util.Utils;

/**
 * Represents the HTML Field in the webbrowser. Has support for call backs of scripts through javascript:methodname links or other tags.
 * 
 * @author jcompagner
 */
public class WebDataHtmlView extends WebDataSubmitLink implements IFieldComponent, IScriptTextEditorMethods, ISupportScriptCallback
{
	private static final long serialVersionUID = 1L;

	private AbstractDefaultAjaxBehavior inlineScriptExecutor;

	public WebDataHtmlView(IApplication application, String id)
	{
		super(application, id);
		setEscapeModelStrings(false);
		jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseSubmitLink#js_getElementType()
	 */
	@Override
	public String js_getElementType()
	{
		return "HTML_AREA"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseSubmitLink#onSubmit()
	 */
	@Override
	public void onSubmit()
	{
		super.onSubmit();

		String scriptName = RequestCycle.get().getRequest().getParameter(getInputName());
		WebForm wf = findParent(WebForm.class);
		if (wf != null)
		{
			wf.getController().eval(scriptName);
		}
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.ISupportScriptCallback#getCallBackUrl(java.lang.String, boolean)
	 */
	@SuppressWarnings("nls")
	public CharSequence getCallBackUrl(String scriptName, boolean testDoubleClick)
	{
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX"));
		if (useAJAX)
		{
			if (inlineScriptExecutor == null)
			{
				inlineScriptExecutor = new InlineScriptExecutorBehavior(this);
				add(inlineScriptExecutor);
			}
			CharSequence url = inlineScriptExecutor.getCallbackUrl();
			AppendingStringBuffer asb = new AppendingStringBuffer(url.length() + 30);
			if (testDoubleClick)
			{
				asb.append("if (testDoubleClickId('");
				asb.append(getMarkupId());
				asb.append("')) { ");
			}
			asb.append("wicketAjaxGet('");
			asb.append(url);
			asb.append("&scriptname=");
			asb.append(Utils.stringReplace(Utils.stringReplace(scriptName, "\'", "\\\'"), "\"", "&quot;"));
			asb.append("');");
			if (testDoubleClick)
			{
				asb.append("} ");
			}
			asb.append("return false;");
			return asb.toString();
		}
		else
		{
			return StripHTMLTagsConverter.getTriggerJavaScript(this, scriptName);
		}
	}

	/**
	 * @see wicket.markup.html.form.SubmitLink#onComponentTag(wicket.markup.ComponentTag)
	 */
	@SuppressWarnings("nls")
	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		tag.put("id", getMarkupId());
	}

	//TODO Needed to be like field

	@Override
	public IEventExecutor getEventExecutor()
	{
		return null;
	}

	public void setActionCmd(String actionCmd, Object[] args)
	{
	}

	public void setChangeCmd(String changeCmd, Object[] args)
	{
	}

	public void setEditable(boolean editable)
	{
	}

	public void setEnterCmds(String[] enterCmds, Object[][] args)
	{
	}

	public void setLeaveCmds(String[] leaveCmds, Object[][] args)
	{
	}

	public void setMaxLength(int maxLength)
	{
	}

	public void setSelectOnEnter(boolean selectOnEnter)
	{
	}

	public void setFormat(int type, String format)
	{
		dataType = type;
		this.format = format;
	}

	private String format;
	private int dataType;

	public int getDataType()
	{
		return dataType;
	}

	@Override
	public String getFormat()
	{
		return format;
	}


	private ArrayList<ILabel> labels;

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public String[] js_getLabelForElementNames()
	{
		if (labels != null)
		{
			ArrayList<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
	}

	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		super.setComponentVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				label.setComponentVisible(visible);
			}
		}
	}

	@Override
	public void js_setVisible(boolean visible)
	{
		super.js_setVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label instanceof IScriptBaseMethods)
				{
					((IScriptBaseMethods)label).js_setVisible(visible);
				}
				else
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}

	@Override
	public void setComponentEnabled(final boolean b)
	{
		super.setComponentEnabled(b);
		if (accessible)
		{
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
					label.setComponentEnabled(b);
				}
			}
		}
	}

	public String js_getAsPlainText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String js_getURL()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void js_setURL(String url)
	{
		// TODO Auto-generated method stub
	}


	@Override
	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}


	/*
	 * scrolling---------------------------------------------------
	 */
	public int js_getScrollX()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int js_getScrollY()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void js_setScroll(int x, int y)
	{
		// TODO Auto-generated method stub
	}


	/*
	 * readonly/editable---------------------------------------------------
	 */
	public void js_setEditable(boolean b)
	{
	}

	public boolean js_isEditable()
	{
		return false;
	}

	public boolean js_isReadOnly()
	{
		return false;
	}

	public void js_setReadOnly(boolean b)
	{
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public int js_getCaretPosition()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void js_requestFocus(Object[] vargs)
	{
//		if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0])) 
//		{
//			eventExecutor.skipNextFocusGain();
//		}
//		requestFocus();
//	}
//	public void requestFocus()
//	{
//		Page page = findPage();
//		if (page instanceof MainPage)
//		{
//			((MainPage)page).componentToFocus(this);
//		}
	}

	public String js_getSelectedText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void js_replaceSelectedText(String s)
	{
		// TODO Auto-generated method stub
	}

	public void js_selectAll()
	{
		// TODO Auto-generated method stub
	}

	public void js_setCaretPosition(int pos)
	{
		// TODO Auto-generated method stub
	}

	public String js_getBaseURL()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void js_setBaseURL(String url)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected int getFontSize()
	{
		// Since the fontSize is used only for calculating web size, it is safe to just return 0
		// in order to avoid any padding adjustments. (issue 169037)
		return 0;
	}
}