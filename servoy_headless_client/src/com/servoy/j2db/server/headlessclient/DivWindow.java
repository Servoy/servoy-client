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

import java.awt.Point;
import java.awt.Rectangle;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
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
 * @since 6.0
 */
public class DivWindow extends ModalWindow
{

	private static ResourceReference JAVA_SCRIPT = new JavascriptResourceReference(DivWindow.class, "divwindow.js");
	private static int nextJSId = 0;

	/**
	 * Callback for window resize operations.
	 * @author acostescu
	 */
	public static interface ResizeCallback extends IClusterable
	{
		/**
		 * The method is invoke when div window gets resized. The invocation is done using an ajax
		 * call, so <code>{@link AjaxRequestTarget}</code> instance is available.
		 * 
		 * @param target <code>{@link AjaxRequestTarget}</code> instance bound with the ajax request.
		 */
		public void onResize(AjaxRequestTarget target);
	}

	/**
	 * Callback for window move operations.
	 * @author acostescu
	 */
	public static interface MoveCallback extends IClusterable
	{
		/**
		 * The method is invoke when div window gets moved. The invocation is done using an ajax
		 * call, so <code>{@link AjaxRequestTarget}</code> instance is available.
		 * 
		 * @param target <code>{@link AjaxRequestTarget}</code> instance bound with the ajax request.
		 */
		public void onMove(AjaxRequestTarget target);
	}

	private class WindowClosedBehavior extends AbstractDefaultAjaxBehavior implements IWindowClosedBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			respondOnWindowClosed(target);
		}

		@Override
		public CharSequence getCallbackScript()
		{
			return getCallbackScript(true);
		}

	}

	private class ResizeBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{

			if (resize(target) && resizeCallback != null)
			{
				resizeCallback.onResize(target);
			}
		}

		// make it available to this compilation unit
		@Override
		protected CharSequence getCallbackScript()
		{
			return getCallbackScript(true);
		}

		@Override
		protected CharSequence getCallbackScript(boolean onlyTargetActivePage)
		{
			return generateCallbackScript("wicketAjaxGet('" + getCallbackUrl(onlyTargetActivePage) +
				"&divW=' + w + '&divH=' + h + (initialShow ? '&is=true' : '')");
		}


	}

	private class MoveBehavior extends AbstractDefaultAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			if (move(target) && moveCallback != null)
			{
				moveCallback.onMove(target);
			}
		}

		// make it available to this compilation unit
		@Override
		protected CharSequence getCallbackScript()
		{
			return getCallbackScript(true);
		}

		@Override
		protected CharSequence getCallbackScript(boolean onlyTargetActivePage)
		{
			return generateCallbackScript("wicketAjaxGet('" + getCallbackUrl(onlyTargetActivePage) +
				"&xLoc=' + x + '&yLoc=' + y + (initialShow ? '&is=true' : '')");
		}

	}

	protected class CloseButtonBehaviorActivePage extends CloseButtonBehavior
	{
		@Override
		protected CharSequence getCallbackScript(boolean onlyTargetActivePage)
		{
			return super.getCallbackScript(true);
		}
	}

	private boolean modal = true;
	private boolean storeBounds = true;
	private Point initialLocation = null;
	protected boolean isInsideIFrame;
	private String jsId;
	private ResizeCallback resizeCallback = null;
	private MoveCallback moveCallback = null;
	private int boundEventsDelay = 300;
	private final Rectangle bounds = new Rectangle();

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
		add(new MoveBehavior());
		add(new ResizeBehavior());
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
	 * In order for this window to be aware of it's bounds server-side, browser requests are triggered when the window is moved/resized.
	 * As many events can happen in a short move/resize operation that would trigger requests, you might want to limit the number of requests sent, by
	 * waiting msDelay milliseconds of drag inactivity before triggering a request. 
	 * @param msDelay the delay from when the user stopped dragging until the requests will be triggered. Values <= 0 mean all events will trigger requests. 300 ms by default.
	 * @return this window.
	 */
	public DivWindow setBoundEventsDelay(int msDelay)
	{
		boundEventsDelay = msDelay;
		return this;
	}

	/**
	 * Returns the value of the delay for triggering bound requests. See {@link #setBoundEventsDelay(int)}.
	 * @return the value of the delay for triggering bound requests.
	 */
	private int getBoundEventsDelay()
	{
		return boundEventsDelay;
	}

	/**
	 * Sets the <code>{@link ResizeCallback}</code> instance.
	 * 
	 * @param callback Callback instance
	 * @return this
	 */
	public DivWindow setResizeCallback(final ResizeCallback callback)
	{
		resizeCallback = callback;
		return this;
	}

	/**
	 * Sets the <code>{@link MoveCallback}</code> instance.
	 * 
	 * @param callback Callback instance
	 * @return this
	 */
	public DivWindow setMoveCallback(final MoveCallback callback)
	{
		moveCallback = callback;
		return this;
	}

	/**
	 * Override default onClose to make it only target last active page (otherwise you can get in trouble if you do something like win.close(); history.back()
	 * - you can end up with incorrect page version and unresponsive WebClient).
	 */
	@Override
	protected IWindowClosedBehavior newWindowClosedBehavior()
	{
		return new WindowClosedBehavior();
	}

	/**
	 * Only target if active page.
	 */
	@Override
	protected CloseButtonBehavior newCloseButtonBehavior()
	{
		return new CloseButtonBehaviorActivePage();
	}

	public int getX()
	{
		return bounds.x;
	}

	public int getY()
	{
		return bounds.y;
	}

	public int getWidth()
	{
		return bounds.width;
	}

	public int getHeight()
	{
		return bounds.height;
	}

	public void setInitialLocation(Point initialLocation)
	{
		this.initialLocation = initialLocation;
	}

	public Point getInitialLocation()
	{
		return initialLocation;
	}

	/**
	 * @return true if it was triggered by initial show, in which case the callbacks shouldn't be called.
	 */
	protected boolean resize(AjaxRequestTarget target)
	{
		Request request = RequestCycle.get().getRequest();
		bounds.width = Integer.parseInt(request.getParameter("divW")); //$NON-NLS-1$
		bounds.height = Integer.parseInt(request.getParameter("divH")); //$NON-NLS-1$
		return "true".equals(request.getParameter("is"));
	}

	/**
	 * @return true if it was triggered by initial show, in which case the callbacks shouldn't be called.
	 */
	protected boolean move(AjaxRequestTarget target)
	{
		Request request = RequestCycle.get().getRequest();
		bounds.x = Integer.parseInt(request.getParameter("xLoc"));
		bounds.y = Integer.parseInt(request.getParameter("yLoc"));
		return "true".equals(request.getParameter("is"));
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

	public boolean getStoreBounds()
	{
		return storeBounds;
	}

	public void setStoreBounds(boolean storeBounds)
	{
		this.storeBounds = storeBounds;
	}

	@Override
	protected AppendingStringBuffer postProcessSettings(AppendingStringBuffer settings)
	{
		settings = super.postProcessSettings(settings);

		settings.append("settings.boundEventsDelay").append("=");
		settings.append(getBoundEventsDelay());
		settings.append(";\n");

		settings.append("settings.modal").append("=");
		settings.append(isModal());
		settings.append(";\n");

		settings.append("settings.storeBounds").append("=");
		settings.append(getStoreBounds());
		settings.append(";\n");

		settings.append("settings.initialX").append("=");
		settings.append(getInitialLocation().x);
		settings.append(";\n");

		settings.append("settings.initialY").append("=");
		settings.append(getInitialLocation().y);
		settings.append(";\n");

		settings.append("settings.jsId").append("=\"");
		settings.append(getJSId());
		settings.append("\";\n");

		MoveBehavior mb = getBehaviors(MoveBehavior.class).get(0);
		settings.append("settings.onMove = function(x, y, initialShow) { ");
		settings.append(mb.getCallbackScript());
		settings.append("};\n");

		ResizeBehavior rb = getBehaviors(ResizeBehavior.class).get(0);
		settings.append("settings.onResize = function(w, h, initialShow) { ");
		settings.append(rb.getCallbackScript());
		settings.append("};\n");

		return settings;
	}

	@Override
	protected Object getShowJavascript()
	{
		// we assume this gets called inside the window that will open this new div window; isInsideIFrame refers to current window
		String s = "var win = Wicket.DivWindow.create(settings, \"" + getJSId() + "\", " + isInsideIFrame + ");\nwin.show();"; //$NON-NLS-1$ //$NON-NLS-2$
		return s;
	}

	protected String getActionJavascript(String actualActionScript, String parameters)
	{
		return "var win; try { win = window.parent.Wicket.DivWindow; } catch (ignore) {}; if (typeof(win) == \"undefined\" || typeof(win.openWindows[\"" +
			getJSId() +
			"\"]) == \"undefined\") { try { win = window.Wicket.DivWindow; } catch (ignore) {} }; if (typeof(win) != \"undefined\") { var winObj = win.openWindows[\"" +
			getJSId() + "\"]; if (typeof(winObj) != \"undefined\") { winObj" + actualActionScript + "(\"" + parameters + "\"); } }";
	}

	@Override
	protected String getCloseJavacript()
	{
		return getActionJavascript(".close", "");
	}

	public void setBounds(AjaxRequestTarget target, int x, int y, int width, int height)
	{
		target.appendJavascript(getActionJavascript(".setPosition", ((x >= 0) ? ("'" + x + "px'") : "winObj.window.style.left") + "," +
			((y >= 0) ? ("'" + y + "px'") : "winObj.window.style.top") + "," + ((width >= 0) ? ("'" + width + "px'") : "winObj.window.style.width") + "," +
			((height >= 0) ? ("'" + height + "px'") : "winObj.content.style.height")));
	}

	public void saveBounds(AjaxRequestTarget target)
	{
		target.appendJavascript(getActionJavascript(".savePosition", ""));
	}

	public static void deleteStoredBounds(AjaxRequestTarget target, String dialogName)
	{
		target.getHeaderResponse().renderJavascriptReference(JAVA_SCRIPT);
		target.appendJavascript("Wicket.DivWindow.deletePosition(\"" + dialogName + "\");");
	}

	public void toFront(AjaxRequestTarget target)
	{
		target.appendJavascript(getActionJavascript(".toFront", ""));
	}

	public void toBack(AjaxRequestTarget target)
	{
		target.appendJavascript(getActionJavascript(".toBack", ""));
	}

}