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
@SuppressWarnings("nls")
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

	private class WindowClosedBehavior extends AbstractDefaultAjaxBehavior implements IWindowClosedBehavior, AlwaysLastPageVersionRequestListenerInterface
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

		@Override
		public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
		{
			if (getComponent() == null)
			{
				throw new IllegalArgumentException("Behavior must be bound to a component to create the URL"); //$NON-NLS-1$
			}

			return getComponent().urlFor(this, AlwaysLastPageVersionRequestListenerInterface.INTERFACE);
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
	private String jsId;
	private ResizeCallback resizeCallback = null;
	private MoveCallback moveCallback = null;
	private int boundEventsDelay = 300;
	private final Rectangle bounds = new Rectangle(-1, -1, -1, -1); // initially unknown bounds; -1 in order for setBounds(getBounds()) to not have any undesired effect when bounds are not known

	// if you would have an app shown in an iframe of another app, then you can't know browser side which browser window to use for running actions just by checking that DivWindow is defined or not in parent iframe
	// because all of them definde DivWindow code & if the response comes from a dialog iframe, then you should target parent browser window, otherwise you should target current browser window.
	// So this is added for more control in this (probably rare) case (of multiple levels of nested iframes).
	private String tmpChildFrameActionBatch = null; // by default search for the parent
	private boolean onCloseButtonBehaviorIsSet = false;
	private boolean onCloseBehaviorIsSet = false;

	/**
	 * Creates a new div window.
	 * @param id id of component. No duplicates are allowed.
	 * @param isInsideIFrame true if this DivWindow component is added to a Page that is shown in another div window's iframe and false otherwise.
	 * This will be used when creating new div windows so as to be able to keep track of all opened iframe div windows inside a browser window.
	 */
	public DivWindow(String id)
	{
		super(id);
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
	 * @return true if it was not triggered by initial show or other operations for which the callback shouldn't be called.
	 */
	protected boolean resize(AjaxRequestTarget target)
	{
		Request request = RequestCycle.get().getRequest();
		bounds.width = Integer.parseInt(request.getParameter("divW")); //$NON-NLS-1$
		bounds.height = Integer.parseInt(request.getParameter("divH")); //$NON-NLS-1$
		return !("true".equals(request.getParameter("is")));
	}

	/**
	 * @return true if it was not triggered by initial show or other operations for which the callback shouldn't be called.
	 */
	protected boolean move(AjaxRequestTarget target)
	{
		Request request = RequestCycle.get().getRequest();
		bounds.x = Integer.parseInt(request.getParameter("xLoc"));
		bounds.y = Integer.parseInt(request.getParameter("yLoc"));
		return !("true".equals(request.getParameter("is")));
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

		// if this will be evaluated in child window response, the callback urls are invalid; just put something that will generate JS errors
		// when called and put the correct ones when a request arrives on parent page
		// (for example a dialog could close itself and show another dialog => window.Wicket undefined in the closed window, but
		// that is the window that generated the request, so callbacks like resize when showing would want to use Wicket. ...)
		if (settings.indexOf("settings.onCloseButton = function() {") != -1)
		{
			onCloseButtonBehaviorIsSet = true;
			if (tmpChildFrameActionBatch != null) settings.append("delete settings.onCloseButton;\n");
		}
		if (settings.indexOf("settings.onClose = function() {") != -1)
		{
			onCloseBehaviorIsSet = true;
			if (tmpChildFrameActionBatch != null) settings.append("delete settings.onClose;\n");
		}

		settings.append("settings.boundEventsDelay=");
		settings.append(getBoundEventsDelay());
		settings.append(";\n");

		settings.append("settings.modal=");
		settings.append(isModal());
		settings.append(";\n");

		String closeText = WebClientSession.get().getWebClient().getI18NMessage("servoy.webclient.dialogCloseText");
		if (closeText != null && closeText.length() > 0 && closeText.indexOf("servoy.webclient.dialogCloseText") == -1)
		{
			settings.append("settings.dialogCloseText=");
			settings.append("'" + closeText + "'");
			settings.append(";\n");
		}

		settings.append("settings.storeBounds=");
		settings.append(getStoreBounds());
		settings.append(";\n");

		if (getInitialLocation() != null)
		{
			settings.append("settings.initialX=");
			settings.append(getInitialLocation().x);
			settings.append(";\n");

			settings.append("settings.initialY=");
			settings.append(getInitialLocation().y);
			settings.append(";\n");
		}

		settings.append("settings.jsId=\"");
		settings.append(getJSId());
		settings.append("\";\n");

		if (tmpChildFrameActionBatch == null) attachOnMove(settings);
		if (tmpChildFrameActionBatch == null) attachOnResize(settings);

		return settings;
	}

	protected void attachOnMove(AppendingStringBuffer settings)
	{
		MoveBehavior mb = getBehaviors(MoveBehavior.class).get(0);
		settings.append("settings.onMove = function(x, y, initialShow) {\n");
		settings.append(mb.getCallbackScript());
		settings.append("};\n");
	}

	protected void attachOnResize(AppendingStringBuffer settings)
	{
		ResizeBehavior rb = getBehaviors(ResizeBehavior.class).get(0);
		settings.append("settings.onResize = function(w, h, initialShow) {\n");
		settings.append(rb.getCallbackScript());
		settings.append("};\n");
	}

	/**
	 * When show was initially called from a child iframe request (thus callback scripts were generated using that
	 * page's target), you need to call this method subsequently on a request from the root main frame, to make
	 * behaviors work with the main page as you would expect (otherwise problems occur when you try to close it).
	 * @param mainFrameTarget
	 * @param childFrameBatchId should never be null; it is the child frame batchId that will execute/has executed the show. 
	 */
	public void reAttachBehaviorsAfterShow(AjaxRequestTarget mainFrameTarget, String childFrameBatchId)
	{
		if (childFrameBatchId == null) throw new IllegalArgumentException("'reAttachBehaviors' is only to be called if a show happened on child frame.");
		AppendingStringBuffer settingsToUpdate = new AppendingStringBuffer(500);

		// if show was already called (as a result of a child frame request), just re-register; otherwise wait for show to be called and that will do the re-register directly
		settingsToUpdate.append("function (settings) {\n");
		attachOnMove(settingsToUpdate);
		attachOnResize(settingsToUpdate);
		reattachOnClose(settingsToUpdate);
		reattachOnCloseButton(settingsToUpdate);
		settingsToUpdate.append("}");

		mainFrameTarget.appendJavascript("Wicket.DivWindow.reAttachBehaviorsAfterShow(" + settingsToUpdate.toString() + ", \"" + getJSId() + "\", \"" +
			childFrameBatchId + "\");");
	}

	protected boolean reattachOnCloseButton(AppendingStringBuffer settingsToUpdate)
	{
		if (onCloseButtonBehaviorIsSet)
		{
			CloseButtonBehaviorActivePage behavior = getBehaviors(CloseButtonBehaviorActivePage.class).get(0);
			settingsToUpdate.append("settings.onCloseButton = function() { ");
			settingsToUpdate.append(behavior.getCallbackScript(true));
			settingsToUpdate.append("};\n");
		}
		return onCloseButtonBehaviorIsSet;
	}

	protected boolean reattachOnClose(AppendingStringBuffer settingsToUpdate)
	{
		if (onCloseBehaviorIsSet)
		{
			IWindowClosedBehavior behavior = getBehaviors(IWindowClosedBehavior.class).get(0);
			settingsToUpdate.append("settings.onClose = function() { ");
			settingsToUpdate.append(behavior.getCallbackScript());
			settingsToUpdate.append(" };\n");
		}
		return onCloseBehaviorIsSet;
	}

	/**
	 * @deprecated please use {@link #show(AjaxRequestTarget, boolean)}
	 */
	@Deprecated
	@Override
	public void show(AjaxRequestTarget target)
	{
		super.show(target);
	}

	/**
	 * IMPORTANT: if childFrameBatchId != null (so you are running this from a child iframe) you MUST call {@link #reAttachBehaviorsAfterShow(AjaxRequestTarget, String)} as soon as possible from the main/parent iframe.
	 * Otherwise behavior callbacks will be wrong.
	 * @param childFrameBatchId null if this target is of the main window (that contains all dialog iframes), an unique ID if it's of an iframe. Must always be wrapped by {@link #beginActionBatch(AjaxRequestTarget, String)} and {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID if it is not null.
	 */
	public void show(AjaxRequestTarget target, String childFrameBatchId)
	{
		tmpChildFrameActionBatch = childFrameBatchId;
		show(target);
		tmpChildFrameActionBatch = null; // default
	}

	@Override
	protected Object getShowJavascript()
	{
		if (tmpChildFrameActionBatch != null) tmpChildFrameActionBatch = '"' + tmpChildFrameActionBatch + '"';
		String s = "Wicket.DivWindow.createAndShow(settings, \"" + getJSId() + "\", " + tmpChildFrameActionBatch + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return s;
	}

	protected String getActionJavascript(String actualActionScript, String parameters, String childFrameBatchId)
	{
		if (childFrameBatchId != null) childFrameBatchId = '"' + childFrameBatchId + '"';
		return "Wicket.DivWindow.executeAction(function(winObj) { winObj" + actualActionScript + "(" + parameters + "); }, \"" + getJSId() + "\", " +
			childFrameBatchId + ");";
	}

	/**
	 * @deprecated please use {@link #close(AjaxRequestTarget, boolean)}
	 */
	@Deprecated
	@Override
	public void close(AjaxRequestTarget target)
	{
		super.close(target);
	}

	/**
	 * @param childFrameBatchId null if this target is of the main window (that contains all dialog iframes), an unique ID if it's of an iframe. Must always be wrapped by {@link #beginActionBatch(AjaxRequestTarget, String)} and {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID if it is not null.
	 */
	public void close(AjaxRequestTarget target, String childFrameBatchId)
	{
		tmpChildFrameActionBatch = childFrameBatchId;
		close(target);
		tmpChildFrameActionBatch = null; // default
	}

	@Override
	protected String getCloseJavacript()
	{
		return getActionJavascript(".close", "", tmpChildFrameActionBatch);
	}

	/**
	 * @param childFrameBatchId null if this target is of the main window (that contains all dialog iframes), an unique ID if it's of an iframe. Must always be wrapped by {@link #beginActionBatch(AjaxRequestTarget, String)} and {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID if it is not null.
	 */
	public void setBounds(AjaxRequestTarget target, int x, int y, int width, int height, String childFrameBatchId)
	{
		target.appendJavascript(getActionJavascript(".setPosition", ((x >= 0) ? ("'" + x + "px'") : "winObj.window.style.left") + "," +
			((y >= 0) ? ("'" + y + "px'") : "winObj.window.style.top") + "," + ((width >= 0) ? ("'" + width + "px'") : "winObj.window.style.width") + "," +
			((height >= 0) ? ("'" + height + "px'") : "winObj.content.style.height"), childFrameBatchId));
		if (x >= 0) bounds.x = x;
		if (y >= 0) bounds.y = y;
		if (width >= 0) bounds.width = width;
		if (height >= 0) bounds.height = height;
	}

	/**
	 * @param childFrameBatchId null if this target is of the main window (that contains all dialog iframes), an unique ID if it's of an iframe. Must always be wrapped by {@link #beginActionBatch(AjaxRequestTarget, String)} and {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID if it is not null.
	 */
	public void saveBounds(AjaxRequestTarget target, String childFrameBatchId)
	{
		target.appendJavascript(getActionJavascript(".savePosition", "", childFrameBatchId));
	}

	/**
	 * @param childFrameBatchId null if this target is of the main window (that contains all dialog iframes), an unique ID if it's of an iframe. Must always be wrapped by {@link #beginActionBatch(AjaxRequestTarget, String)} and {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID if it is not null.
	 */
	public void toFront(AjaxRequestTarget target, String childFrameBatchId)
	{
		target.appendJavascript(getActionJavascript(".toFront", "", childFrameBatchId));
	}

	/**
	 * @param childFrameBatchId null if this target is of the main window (that contains all dialog iframes), an unique ID if it's of an iframe. Must always be wrapped by {@link #beginActionBatch(AjaxRequestTarget, String)} and {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID if it is not null.
	 */
	public void toBack(AjaxRequestTarget target, String childFrameBatchId)
	{
		target.appendJavascript(getActionJavascript(".toBack", "", childFrameBatchId));
	}

	public static void deleteStoredBounds(AjaxRequestTarget target, String dialogName)
	{
		target.getHeaderResponse().renderJavascriptReference(JAVA_SCRIPT);
		target.appendJavascript("Wicket.DivWindow.deletePosition(\"" + dialogName + "\");");
	}

	/**
	 * Should only be called if 'target' from a page inside a DivWindow iframe. Must always be followed by
	 * {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID, after the appropriate actions are batched.
	 */
	public static void beginActionBatch(AjaxRequestTarget target, String batchID)
	{
		target.appendJavascript("Wicket.DivWindow.beginActionBatch(\"" + batchID + "\");");
	}

	/**
	 * Should only be called if 'target' from a page inside a DivWindow iframe. Must always be preceded by
	 * {@link #actionBatchComplete(AjaxRequestTarget, String)} with the same batchID and the appropriate actions that are to be batched.
	 */
	public static void actionBatchComplete(AjaxRequestTarget target, String batchID)
	{
		target.appendJavascript("Wicket.DivWindow.actionBatchComplete(\"" + batchID + "\");");
	}

}