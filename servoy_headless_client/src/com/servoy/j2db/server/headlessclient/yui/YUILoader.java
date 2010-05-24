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
package com.servoy.j2db.server.headlessclient.yui;

import java.io.Serializable;

import org.apache.wicket.Application;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

/**
 * @author jcompagner
 * 
 */
public class YUILoader implements Serializable
{
	public static final ResourceReference CSS_FONTS = new CompressedResourceReference(YUILoader.class, "fonts-min.css"); //$NON-NLS-1$
	public static final ResourceReference CSS_RESIZE = new CompressedResourceReference(YUILoader.class, "resize.css"); //$NON-NLS-1$
	public static final ResourceReference CSS_MENU = new CompressedResourceReference(YUILoader.class, "menu.css"); //$NON-NLS-1$

	public static final JavascriptResourceReference JS_YAHOO_DOM_EVENT = new JavascriptResourceReference(YUILoader.class, "yahoo-dom-event.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_DRAGDROP = new JavascriptResourceReference(YUILoader.class, "dragdrop-min.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_ELEMENT = new JavascriptResourceReference(YUILoader.class, "element-min.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_MENU = new JavascriptResourceReference(YUILoader.class, "menu-min.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_CONTAINER = new JavascriptResourceReference(YUILoader.class, "container_core-min.js"); //$NON-NLS-1$
//	private static final JavascriptResourceReference ANIMATION = new JavascriptResourceReference(DesignModeBehavior.class, "yui/animation-min.js");


	public static final JavascriptResourceReference JS_YAHOO_DEBUG = new JavascriptResourceReference(YUILoader.class, "yahoo-debug.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_DOM_DEBUG = new JavascriptResourceReference(YUILoader.class, "dom-debug.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_EVENT_DEBUG = new JavascriptResourceReference(YUILoader.class, "event-debug.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_DRAGDROP_DEBUG = new JavascriptResourceReference(YUILoader.class, "dragdrop-debug.js"); //$NON-NLS-1$
	public static final JavascriptResourceReference JS_RESIZE_DEBUG = new JavascriptResourceReference(YUILoader.class, "resize-debug-patched270.js"); //$NON-NLS-1$

	private YUILoader()
	{
	}

	public static void renderDragNDrop(IHeaderResponse response)
	{
		if (Application.exists() && Application.get().getDebugSettings().isAjaxDebugModeEnabled())
		{
			response.renderJavascriptReference(YUILoader.JS_YAHOO_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_DOM_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_EVENT_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_DRAGDROP_DEBUG);
		}
		else
		{
			response.renderJavascriptReference(YUILoader.JS_YAHOO_DOM_EVENT);
			response.renderJavascriptReference(YUILoader.JS_DRAGDROP);
		}
	}


	public static void renderResize(IHeaderResponse response)
	{
		response.renderCSSReference(CSS_FONTS);
		response.renderCSSReference(CSS_RESIZE);

		if (Application.exists() && Application.get().getDebugSettings().isAjaxDebugModeEnabled())
		{
			response.renderJavascriptReference(YUILoader.JS_YAHOO_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_DOM_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_EVENT_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_ELEMENT);
			response.renderJavascriptReference(YUILoader.JS_DRAGDROP_DEBUG);
			response.renderJavascriptReference(YUILoader.JS_RESIZE_DEBUG);
		}
		else
		{
			response.renderJavascriptReference(YUILoader.JS_YAHOO_DOM_EVENT);
			response.renderJavascriptReference(YUILoader.JS_ELEMENT);
			response.renderJavascriptReference(YUILoader.JS_DRAGDROP);
			response.renderJavascriptReference(YUILoader.JS_RESIZE_DEBUG);
		}
	}

}
