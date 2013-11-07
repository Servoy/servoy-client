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

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.crypt.ICrypt;

import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The behavior used by components that want to eval a specific scriptname on the current form.
 * 
 * @author jcompagner
 * 
 * @see ISupportScriptCallback
 * @see WebDataHtmlView
 */
public final class InlineScriptExecutorBehavior extends AbstractServoyDefaultAjaxBehavior
{
	static final String BROWSER_PARAM = "browser:";

	/**
	 * 
	 */
	private final Component component;

	/**
	 * @param webDataHtmlView
	 */
	InlineScriptExecutorBehavior(Component component)
	{
		this.component = component;
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected void respond(AjaxRequestTarget target)
	{
		Page page = component.getPage();
		String scriptName = RequestCycle.get().getRequest().getParameter("snenc");

		ICrypt urlCrypt = Application.get().getSecuritySettings().getCryptFactory().newCrypt();
		scriptName = urlCrypt.decryptUrlSafe(scriptName);

		String argValue;
		for (String browserArgument : getBrowserArguments(scriptName))
		{
			argValue = RequestCycle.get().getRequest().getParameter(browserArgument);
			if (argValue == null) argValue = "";
			boolean isString = true;
			try
			{
				Double.parseDouble(argValue);
				isString = false;
			}
			catch (NumberFormatException ex)
			{
			}
			if (isString && ("true".equals(argValue) || "false".equals(argValue))) isString = false;

			scriptName = scriptName.replace(BROWSER_PARAM + browserArgument, isString ? "'" + argValue + "'" : argValue);
		}

		WebForm wf = component.findParent(WebForm.class);
		if (wf != null)
		{
			try
			{
				wf.getController().eval(scriptName);
			}
			catch (Exception e)
			{
				if (!(e instanceof ExitScriptException))
				{
					Debug.error("Exception evaluating: " + scriptName, e); //$NON-NLS-1$
				}
			}
			WebEventExecutor.generateResponse(target, page);
		}
		target.appendJavascript("clearDoubleClickId('" + component.getMarkupId() + "')"); //$NON-NLS-1$ //$NON-NLS-2$		
	}

	ArrayList<String> getBrowserArguments(String s)
	{
		ArrayList<String> browserArguments = new ArrayList<String>();

		String escapedScriptName = Utils.stringReplace(Utils.stringReplace(s, "\'", "\\\'"), "\"", "&quot;");
		int browserVariableIndex = escapedScriptName.indexOf(BROWSER_PARAM);
		if (browserVariableIndex != -1)
		{
			while (browserVariableIndex != -1)
			{
				// is there a next variable
				int index = searchEndVariable(escapedScriptName, browserVariableIndex + 8);
				if (index == -1)
				{
					Debug.error("illegal script name encountered with browser arguments: " + escapedScriptName);
					break;
				}
				else
				{
					browserArguments.add(escapedScriptName.substring(browserVariableIndex + 8, index));
					browserVariableIndex = escapedScriptName.indexOf(BROWSER_PARAM, index);
				}
			}
		}

		return browserArguments;
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
				case '&' :
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
}