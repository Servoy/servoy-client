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
package com.servoy.j2db.server.headlessclient;

import java.awt.Rectangle;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.AppendingStringBuffer;

/**
 * A div window that can be modal or non-modal. Based on wicket ModalWindow.
 * You should not use both DivWindow and ModalWindow at the same time in the same page as the mask will not behave correctly.
 * @author acostescu
 */
public class DivWindow extends ModalWindow
{

	private static ResourceReference JAVA_SCRIPT = new JavascriptResourceReference(DivWindow.class, "divwindow.js");
	private static int nextJSId = 0;

	private boolean modal = true;
	protected boolean isInsideIFrame;
	private String jsId;
	private Rectangle initialCookieBounds = null;

	/**
	 * Creates a new div window.
	 * @param id id of component. No duplicates are allowed.
	 * @param isInsideIFrame true if this DivWindow component is added to a Page that is shown in another div window's iframe and false otherwise.
	 * This will be used when creating new div windows so as to be able to keep track of all opened iframe div windows inside a browser window.
	 */
	public DivWindow(String id, boolean isInsideIFrame)
	{
		super(id);
		this.isInsideIFrame = isInsideIFrame;
		initialize();
	}

	/**
	 * Creates a new div window.
	 * @param id id of component. No duplicates are allowed.
	 * @param model model.
	 * @param isInsideIFrame true if this DivWindow component is added to a Page that is shown in another div window's iframe and false otherwise.
	 * This will be used when creating new div windows so as to be able to keep track of all opened iframe div windows inside a browser window.
	 */
	public DivWindow(String id, IModel< ? > model, boolean isInsideIFrame)
	{
		super(id, model);
		this.isInsideIFrame = isInsideIFrame;
		initialize();
	}

	private static String getNextJSId()
	{
		return "dw" + (nextJSId++);
	}

	protected void initialize()
	{
		jsId = getNextJSId();
		add(JavascriptPackageResource.getHeaderContribution(JAVA_SCRIPT));
	}

	/**
	 * Returns the jsId used to find the DivWindow in browser java-script.
	 * @return the jsId used to find the DivWindow in browser java-script.
	 */
	protected String getJSId()
	{
		return jsId;
	}

	/**
	 * Sets whether the window is modal or not.
	 * 
	 * @param modal true for modal, false for non-modal.
	 */
	public void setModal(boolean modal)
	{
		this.modal = modal;
	}

	/**
	 * Check if the window is modal or not.
	 * 
	 * @return true if the window is modal, false otherwise.
	 */
	public boolean isModal()
	{
		return modal;
	}

	@Override
	protected AppendingStringBuffer postProcessSettings(AppendingStringBuffer settings)
	{
		settings = super.postProcessSettings(settings);

		settings.append("settings.modal").append("=");
		settings.append(isModal());
		settings.append(";\n");

		settings.append("settings.jsId").append("=\"");
		settings.append(getJSId());
		settings.append("\";\n");

		return settings;
	}

	@Override
	protected Object getShowJavascript()
	{
		// we assume this gets called inside the window that will open this new div window; isInsideIFrame refers to current window
		String s = "var win = Wicket.DivWindow.create(settings, \"" + getJSId() + "\", " + isInsideIFrame + ");\n";
		if (initialCookieBounds != null)
		{
			if (initialCookieBounds.x < 0 && initialCookieBounds.y < 0)
			{
				s = s + "win.findPositionString(true);"; // delete cookie initial bounds so that window is centered and values set with setInitialHeight() and setInitialWidth() are used
			}
			else
			{
				s = s + "win.savePositionAs('" + initialCookieBounds.x + "px', '" + initialCookieBounds.y + "px', '" + initialCookieBounds.width + "px', '" +
					initialCookieBounds.height + "px');\n";
			}
		}
		s = s + "win.show();";
		return s;
	}

	protected String getActionJavascript(String actualActionScript)
	{
		return "var win; try { win = window.parent.Wicket.DivWindow; } catch (ignore) {}; if (typeof(win) == \"undefined\" || typeof(win.openWindows[\"" +
			getJSId() +
			"\"]) == \"undefined\") { try { win = window.Wicket.DivWindow; } catch (ignore) {} }; if (typeof(win) != \"undefined\") { var doAction = function(w) { w.setTimeout(function() { win" +
			actualActionScript + "(\"" + getJSId() + "\"); }, 0);  }; try { doAction(window.parent); } catch (ignore) { doAction(window); }; }";
	}

	@Override
	protected String getCloseJavacript()
	{
		return getActionJavascript(".close");
	}

	public void setCookieBoundsOnShow(Rectangle initialCookieBounds)
	{
		// we rely on ModalWindow cookie mechanism to simulate initial bounds behaviour (so if we set the correct cookie contents before showing the window, it should use them)
		this.initialCookieBounds = initialCookieBounds;
	}

	public void toFront(AjaxRequestTarget target)
	{
		target.appendJavascript(getActionJavascript(".toFront"));
	}

	public void toBack(AjaxRequestTarget target)
	{
		target.appendJavascript(getActionJavascript(".toBack"));
	}

}