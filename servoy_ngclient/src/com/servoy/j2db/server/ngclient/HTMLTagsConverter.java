/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Convert tags inside html content
 * @author gboros
 */
public class HTMLTagsConverter
{
	private static final String[] scanTags = new String[] { "src", "href", "background", "onsubmit", "onreset", "onselect", "onclick", "ondblclick", "onfocus", "onblur", "onchange", "onkeydown", "onkeypress", "onkeyup", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$
	private static final String javascriptPrefix = "javascript:"; //$NON-NLS-1$
	private static final String mediaPrefix = "media://"; //$NON-NLS-1$
	private static final String BROWSER_PARAM = "browser:"; //$NON-NLS-1$

	public static String convert(String htmlContent, IServoyDataConverterContext context, boolean convertInlineScript)
	{
		Document doc = Jsoup.parse(htmlContent);
		Elements bodyElements = doc.body().getAllElements();
		Iterator<Element> bodyElementsIte = bodyElements.iterator();
		Element e;
		Attributes attrs;
		Iterator<Attribute> attrsIte;
		Attribute attr;
		while (bodyElementsIte.hasNext())
		{
			e = bodyElementsIte.next();
			attrs = e.attributes();
			attrsIte = attrs.asList().iterator();
			while (attrsIte.hasNext())
			{
				attr = attrsIte.next();
				if (Arrays.asList(scanTags).indexOf(attr.getKey()) != -1)
				{
					String replaceContent = attr.getValue();
					if (convertInlineScript && replaceContent.startsWith(javascriptPrefix))
					{
						String script = replaceContent.substring(javascriptPrefix.length());
						ArrayList<String> browserArguments = getBrowserArguments(script);
						StringBuffer browserArgumentsMap = new StringBuffer("{");
						for (String browserArg : browserArguments)
						{
							if (browserArgumentsMap.length() > 1) browserArgumentsMap.append(", ");
							browserArgumentsMap.append("'").append(browserArg).append("' : ").append(browserArg);
						}
						browserArgumentsMap.append("}");
						String encryptedFormName = "";
						try
						{
							encryptedFormName = SecuritySupport.encrypt(Settings.getInstance(), context.getForm().getName());
							script = SecuritySupport.encrypt(Settings.getInstance(), script);
						}
						catch (Exception ex)
						{
							Debug.error("cannot encrypt javascript", ex);
							script = "";
						}
						if ("href".equals(attr.getKey()))
						{
							if (attrs.hasKey("externalcall"))
							{
								attrs.remove("externalcall");
							}
							else
							{
								attr.setValue("#");
								attrs.put("onclick",
									"executeInlineScript('" + encryptedFormName + "', '" + script + "', " + browserArgumentsMap.toString() + ");return false;");
							}
						}
						else
						{
							attr.setValue(
								"executeInlineScript('" + encryptedFormName + "', '" + script + "', " + browserArgumentsMap.toString() + ");return false;");
						}
					}
					else if (replaceContent.startsWith(mediaPrefix))
					{
						String media = replaceContent.substring(mediaPrefix.length());
						if (media.startsWith("/servoy_blobloader?"))
						{
							String blobpart = media.substring("servoy_blobloader?".length());
							try
							{
								blobpart = SecuritySupport.encryptUrlSafe(Settings.getInstance(), blobpart);
								attr.setValue(
									"resources/servoy_blobloader?blob=" + blobpart + "&uuid=" + context.getApplication().getWebsocketSession().getUuid());
							}
							catch (Exception e1)
							{
								Debug.error("could not encrypt blobloaderpart: " + blobpart);
							}
						}
						else attr.setValue("resources/fs/" + context.getSolution().getName() + media);
					}
				}
			}
		}

		return doc.html();
	}

	private static ArrayList<String> getBrowserArguments(String s)
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

	private static int searchEndVariable(String script, int start)
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

	public static String decryptInlineScript(String encryptedJavascript, JSONObject params)
	{
		try
		{
			String javascript = SecuritySupport.decrypt(Settings.getInstance(), encryptedJavascript);
			String browserParamWithArgument;
			Object arg;
			for (String browserArgument : getBrowserArguments(javascript))
			{
				browserParamWithArgument = BROWSER_PARAM + browserArgument;
				arg = params.opt(browserArgument);
				if (arg instanceof String) arg = "'" + arg + "'";
				javascript = javascript.replace(browserParamWithArgument, arg.toString());
			}

			return javascript;
		}
		catch (Exception ex)
		{
			Debug.error("Cannot decrypt inline javascript", ex);
		}
		return null;
	}
}
