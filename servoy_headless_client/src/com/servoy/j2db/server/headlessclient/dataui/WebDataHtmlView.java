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
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.SwingConstants;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.string.AppendingStringBuffer;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTextEditor;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents the HTML Field in the webbrowser. Has support for call backs of scripts through javascript:methodname links or other tags.
 * 
 * @author jcompagner
 */
public class WebDataHtmlView extends WebDataSubmitLink implements IFieldComponent, ISupportScriptCallback
{
	private static final long serialVersionUID = 1L;

	private AbstractDefaultAjaxBehavior inlineScriptExecutor;

	public WebDataHtmlView(IApplication application, AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String id)
	{
		super(application, scriptable, id);
		setHorizontalAlignment(SwingConstants.LEFT);
		setVerticalAlignment(SwingConstants.TOP);
		setEscapeModelStrings(false);
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
			String escapedScriptName = Utils.stringReplace(Utils.stringReplace(scriptName, "\'", "\\\'"), "\"", "&quot;");
			int browserVariableIndex = escapedScriptName.indexOf("browser:");
			if (browserVariableIndex != -1)
			{
				int start = 0;
				StringBuilder sb = new StringBuilder(escapedScriptName.length());
				while (browserVariableIndex != -1)
				{
					sb.append(escapedScriptName.substring(start, browserVariableIndex));
					sb.append("' + ");

					// is there a next variable
					int index = searchEndVariable(escapedScriptName, browserVariableIndex + 8);
					if (index == -1)
					{
						Debug.error("illegal script name encountered with browser arguments: " + escapedScriptName);
						break;
					}
					else
					{
						sb.append(escapedScriptName.substring(browserVariableIndex + 8, index));
						sb.append(" + '");
						int tmp = escapedScriptName.indexOf("browser:", index);
						if (tmp != -1)
						{
							start = index;
							browserVariableIndex = tmp;
						}
						else
						{
							sb.append(escapedScriptName.substring(index));
							escapedScriptName = sb.toString();
							break;
						}

					}
				}
			}
			asb.append(escapedScriptName);
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
	 * @param escapedScriptName
	 * @param i
	 * @return
	 */
	private int searchEndVariable(String script, int start)
	{
		int counter = start;
		int brace = 0;
		while (counter < script.length())
		{
			switch (script.charAt(counter))
			{
				case '\\' :
					if (brace == 0) return counter;
					break;
				case '\'' :
					if (brace == 0) return counter;
					break;
				case ',' :
					if (brace == 0) return counter;
					break;
				case '(' :
					brace++;
					break;
				case ')' :
					if (brace == 0) return counter;
					brace--;
					break;
			}
			counter++;
		}
		return 0;
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

	/**
	 * This is because in the super classes we add a container <span> tag to fix the horizontal/vertical 
	 * alignment, and that tag is missing the 'width' and 'height' attributes by default. By returning true
	 * here we force the two attributes to be added.
	 */
	@Override
	protected boolean hasHtml()
	{
		return true;
	}

	@Override
	protected String getCSSId()
	{
		return null;
	}

	@Override
	protected boolean isAnchored()
	{
		return Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
	}

	@Override
	public IEventExecutor getEventExecutor()
	{
		return null;
	}

	public void setActionCmd(String actionCmd, Object[] args)
	{
		IEventExecutor eventExecutor = super.getEventExecutor();
		if (eventExecutor instanceof WebEventExecutor) ((WebEventExecutor)eventExecutor).setActionCmd(actionCmd, args);
	}

	public void setChangeCmd(String changeCmd, Object[] args)
	{
	}

	public void setEditable(boolean editable)
	{
	}

	public boolean isEditable()
	{
		return false;
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

	@Override
	public void setFormat(int type, String format)
	{
		dataType = type;
		this.format = format;
	}

	private String format;
	private int dataType;

	@Override
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

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (isViewable() || !visible)
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
	}

	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setComponentEnabled(b);
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

	@Override
	public int getFontSize()
	{
		// Since the fontSize is used only for calculating web size, it is safe to just return 0
		// in order to avoid any padding adjustments. (issue 169037)
		return 0;
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	public void setReadOnly(boolean b)
	{

	}

	@Override
	public void setTitleText(String title)
	{
		super.setTitleText(title);
		//	see ComponentFactory createField	
		if (needEntireState() && getDataProviderID() == null)
		{
			setTagText(application.getI18NMessageIfPrefixed(title));
		}
	}
}