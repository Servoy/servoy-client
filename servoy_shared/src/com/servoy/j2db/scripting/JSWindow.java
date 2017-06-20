/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.scripting;

import java.awt.Rectangle;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.ServoyException;

/**
 * This is the wrapper class exposed to javascript access.
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSWindow")
public class JSWindow implements IConstantsObject
{
	/**
	 * Window type constant that identifies a non-modal dialog type.
	 * Non-modal dialogs will allow the user to interact with parent windows, but are less independent then windows with WINDOW type.
	 * Dialogs will stay on top of parent windows and are less accessible through the OS window manager. In web-client dialogs will not
	 * open in a separate browser window.
	 * @sample
	 * // create a non-modal dialog on top of current active form's window and show a form inside it
	 * var myWindow = application.createWindow("myName", JSWindow.DIALOG);
	 * myWindow.show(forms.myForm);
	 */
	public final static int DIALOG = 0;

	/**c
	 * Window type constant that identifies a modal dialog type. Modal dialogs will not allow the user to interact with the parent window(s) until closed.
	 * Dialogs will stay on top of parent windows and are less accessible through the OS window manager. In web-client dialogs will not
	 * open in a separate browser window. NOTE: no code is executed in Smart Client after a modal dialog is shown (the show operation blocks) until this dialog closes.
	 * @sample
	 * // create a modal dialog on top of current active form's window and show a form inside it
	 * var myWindow = application.createWindow("myName", JSWindow.MODAL_DIALOG);
	 * myWindow.show(forms.myForm);
	 */
	public final static int MODAL_DIALOG = 1;

	/**
	 * Window type constant that identifies a window type. WINDOW type is the most independent type of window. It will be more accessible through the OS window
	 * manager, it can appear both in front of and under other windows and it doesn't block user interaction for other windows. In web-client windows will
	 * open in a separate browser window.
	 * @sample
	 * // create a window and show a form inside it
	 * var myWindow = application.createWindow("myName", JSWindow.WINDOW);
	 * myWindow.show(forms.myForm);
	 */
	public final static int WINDOW = 2;

	// For future implementation of case 286968
//		/**
//		 * Normal state of a window (default). Not MAXIMIZED, not ICONIFIED. Only relevant for WINDOW type windows.
//		 * @sample
//		 * // bring a maximized window back to normal state
//		 * var win = controller.getWindow();
//		 * if (win.getState() == JSWindow.MAXIMIZED) win.setState(JSWindow.NORMAL);
//		 */
//		public final static int NORMAL = 0; // do not change value (used for bitmask)
	//
//		/**
//		 * Maximized state of a window. Only relevant for WINDOW type windows.
//		 * @sample
//		 * // open a new window that is initially maximized
//		 * var win = application.createWindow("myName", JSWindow.WINDOW);
//		 * win.setInitialState(JSWindow.MAXIMIZED);
//		 * forms.myForm.show(win);
//		 */
//		public final static int MAXIMIZED = 3; // do not change value (used for bitmask)
	//
//		/**
//		 * Iconified state of a window. Only relevant for WINDOW type windows.
//		 * @sample
//		 * // open a new window that is initially iconified
//		 * var win = application.createWindow("myName", JSWindow.WINDOW);
//		 * win.setInitialState(JSWindow.ICONIFIED);
//		 * forms.myForm.show(win);
//		 */
//		public final static int ICONIFIED = 4; // do not change value (used for bitmask)
	//
//		/**
//		 * Window state for when a window is maximized, but also iconified. Only relevant for WINDOW type windows.
//		 * @sample
//		 * // maximize & iconify a window
//		 * var win = application.getWindow("myName");
//		 * if (win != null && win.isVisible()) win.setState(JSWindow.MAXIMIZED_ICONIFIED);
//		 */
//		public final static int MAXIMIZED_ICONIFIED = (MAXIMIZED | ICONIFIED); // do not change value (used for bitmask)

	/**
	 * Value used for x, y, width, height of initial bounds when you want the window to auto-determine bounds when shown for the first time.
	 * @sample
	 * // show a dialog that self-determines bounds the first time it it open, then remembers last bounds for future show operations
	 * var win = application.createWindow("myName", JSWindow.DIALOG);
	 * win.setInitialBounds(JSWindow.DEFAULT, JSWindow.DEFAULT, JSWindow.DEFAULT, JSWindow.DEFAULT); // will be shown initially centred and with preferred size
	 * forms.myForm.show(win);
	 */
	public final static int DEFAULT = -1; // also used internally for other "DEFAULT" int values

	/**
	 * Value that can be used for bounds in order to specify that a dialog/window should completely fill the screen.
	 * @sample
	 * // create and show a window, with specified title, full screen
	 * var win = application.createWindow("windowName", JSWindow.WINDOW);
	 * win.setInitialBounds(JSWindow.FULL_SCREEN, JSWindow.FULL_SCREEN, JSWindow.FULL_SCREEN, JSWindow.FULL_SCREEN);
	 * win.setTitle("This is a window");
	 * controller.show(win);
	 */
	public final static int FULL_SCREEN = IApplication.FULL_SCREEN;

	private final RuntimeWindow impl;

	JSWindow(RuntimeWindow impl)
	{
		this.impl = impl;
	}

	public RuntimeWindow getImpl()
	{
		return impl;
	}

	/**
	 * Shows the given form(form name, form object or JSForm) in this window.
	 *
	 * @sample
	 * win.show(forms.myForm);
	 * // win.show("myForm");
	 *
	 * @param form the form that will be shown inside this window. It can be a form name or a form object (actual form or JSForm).
	 */
	public void js_show(Object form) throws ServoyException
	{
		impl.showObject(form);
	}

	/**
	 * Sets the initial window bounds.
	 * The initial bounds are only used the first time this window is shown (what first show means depends on storeBounds property).
	 *
	 * @sample
	 * var win = application.createWindow("myName", JSWindow.DIALOG);
	 * win.setInitialBounds(20, 10, 300, 200);
	 * forms.myForm.show(win);
	 *
	 * @param x the initial x coordinate of the window. Can be JSWindow.DEFAULT, JSWindow.FULL_SCREEN.
	 * @param y the initial y coordinate of the window. Can be JSWindow.DEFAULT, JSWindow.FULL_SCREEN.
	 * @param width the initial width of the window. Can be JSWindow.DEFAULT, JSWindow.FULL_SCREEN.
	 * @param height the initial height of the window. Can be JSWindow.DEFAULT, JSWindow.FULL_SCREEN.
	 */
	public void js_setInitialBounds(int x, int y, int width, int height)
	{
		impl.setInitialBounds(x, y, width, height);
	}

	public void js_setStoreBounds(boolean storeBounds)
	{
		impl.setStoreBounds(storeBounds);
	}

	/**
	 * Tells whether or not the bounds of this window should be stored/persisted (default false).
	 * If true, the window's bounds will be stored when the window is closed. Stored bounds will be used when the window is shown again instead of initialBounds.
	 * For non resizable windows, only location is stored/persisted.
	 * @sample
	 * var win1 = application.createWindow("Window 1", JSWindow.DIALOG, null);
	 * win1.setInitialBounds(200, 200, 450, 350);
	 * win1.resizable = false;
	 * win1.storeBounds = true;
	 * win1.title = "Window 1";
	 * controller.show(win1);
	 */
	public boolean js_getStoreBounds()
	{
		return impl.getStoreBounds();
	}

	/**
	 * Gets/Sets whether or not this window can be resized by the user (default true).
	 *
	 * @sampleas js_isVisible()
	 */
	public boolean js_isResizable()
	{
		return impl.isResizable();
	}

	public void js_setResizable(boolean resizable)
	{
		impl.setResizable(resizable);
	}

	/**
	 * Returns the x coordinate.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the x coordinate.
	 */
	public int js_getX()
	{
		return impl.getX();
	}

	/**
	 * Returns the y coordinate.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the y coordinate.
	 */
	public int js_getY()
	{
		return impl.getY();
	}

	/**
	 * Returns the width.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the width.
	 */
	public int js_getWidth()
	{
		return impl.getWidth();
	}

	/**
	 * Returns the height.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the height.
	 */
	public int js_getHeight()
	{
		return impl.getHeight();
	}

	public void js_setUndecorated(boolean undecorated)
	{
		impl.setUndecorated(undecorated);
	}

	/**
	 * Gets/Sets the undecorated property.
	 * If set then this window will not have any decoration and can't be moved/resized or closed. This should be set before dialog/window is shown, otherwise has no effect.
	 *
	 * @sampleas js_getName()
	 *
	 * @return if this window will be undecorated
	 */
	public boolean js_isUndecorated()
	{
		return impl.isUndecorated();
	}

	@JSSetter
	public void setOpacity(float opacity)
	{
		impl.setOpacity(opacity);
	}

	/**
	 * Gets/Sets the transparency property.
	 * NOTE: For smart clients, the window must be undecorated or the
	 * servoy.smartclient.allowLAFWindowDecoration property set to true
	 *
	 * @sampleas js_getName()
	 *
	 * @return transparency state of the window
	 */
	@JSGetter
	public boolean getTransparent()
	{
		return impl.getTransparent();
	}

	@JSSetter
	public void setTransparent(boolean isTransparent)
	{
		impl.setTransparent(isTransparent);
	}

	/**
	 * Gets/Sets the opacity property. By default will have value 1 (completely opaque), and can be assigned to values between 0 and 1.
	 * If set then window will also be undecorated. This should be set before the dialog/window is shown, otherwise it has no effect.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the opacity of this window
	 */
	@JSGetter
	public float getOpacity()
	{
		return impl.getOpacity();
	}

	public void js_setTitle(final String title)
	{
		impl.setTitle(title, true);
	}

	/**
	 * Gets/Sets the title text.
	 *
	 * @sample
	 * var win1 = application.createWindow("Window 1", JSWindow.WINDOW, null);
	 * win1.setInitialBounds(200, 200, 450, 350);
	 * win1.title = "Window 1";
	 * controller.show(win1);
	 *
	 */
	public String js_getTitle()
	{
		return impl.getTitle();
	}

	/**
	 * Sets whether or not this window should have a text tool bar. Has no effect on web client or smart client main application frame.
	 *
	 * @param showTextToolbar true if you want a text tool bar to be added to this window, false otherwise.
	 *
	 * @sampleas js_toFront()
	 */
	public void js_showTextToolbar(boolean showTextToolbar)
	{
		impl.showTextToolbar(showTextToolbar);
	}

	/**
	 * Returns the window name. It will be null in case of main application frame.
	 *
	 * @sample
	 * var someWindow = application.createWindow("someWindowName", JSWindow.WINDOW, null);
	 * someWindow.setInitialBounds(200, 200, 450, 350);
	 * controller.show(someWindow);
	 *
	 * var name = "Name: " + someWindow.getName() + "\n"
	 * var parent = "Parent: " + (someWindow.getParent() == null ? "none" : someWindow.getParent()) + "\n"
	 * var type = "TypeNumber: " + someWindow.getType() + "\n"
	 * var height = "Height: " + someWindow.getHeight() + "\n"
	 * var width = "Width: " + someWindow.getWidth() + "\n"
	 * var undecorated = "Undecorated: " + someWindow.isUndecorated() + "\n"
	 * var opacity = "Opacity: " + someWindow.opacity + "\n"
	 * var transparent = "Transparent: " + someWindow.transparent + "\n"
	 * var locationX = "Location-X-coordinate: " + someWindow.getX() + "\n"
	 * var locationY = "Location-Y-coordinate: " + someWindow.getY() + "\n"
	 * var info = name + parent + type + height + width + locationX + locationY + undecorated + "\n"
	 * var closeMsg = "Press 'Ok' to close this dialog."
	 *
	 * var infoDialog = plugins.dialogs.showInfoDialog("Window Info", info + closeMsg, "Ok");
	 * if (infoDialog == "Ok") someWindow.close()
	 *
	 * @return the window name.
	 */
	public String js_getName()
	{
		return impl.getName();
	}

	/**
	 * Returns the window type.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the window type. Can be one of JSWindow.DIALOG, JSWindow.MODAL_DIALOG, JSWindow.WINDOW.
	 */
	public int js_getType()
	{
		return impl.getType();
	}

	/**
	 * Returns the parent JSWindow, if available.
	 *
	 * @sampleas js_getName()
	 *
	 * @return the parent JSWindow, if available. If there is no parent JSWindow, it will return null.
	 */
	public JSWindow js_getParent()
	{
		return impl.getParent();
	}

	/**
	 * Closes (hides) the window. It can be shown again using window.show(), controller.show() or controller.showRecords().
	 * The main application window cannot be closed.
	 *
	 * @deprecated  As of release 6.0, replaced by {@link #hide()}
	 *
	 * @sample
	 * //creates and shows a window for 3 seconds before closing it
	 * var win = application.createWindow("someWindowName", JSWindow.WINDOW, null);
	 * win.setInitialBounds(200, 200, 450, 350);
	 * controller.show(win);
	 * application.sleep(3000);
	 * win.close();
	 *
	 * @return Boolean true if the window was successfully closed and false otherwise.
	 */
	@Deprecated
	public boolean js_close()
	{
		return impl.hide();
	}


	/**
	 * Hides the window. It can be shown again using window.show(), controller.show() or controller.showRecords().
	 * The main application window cannot be hidden.
	 *
	 * @sample
	 * //creates and shows a window for 3 seconds before closing it
	 * var win = application.createWindow("someWindowName", JSWindow.WINDOW, null);
	 * win.setInitialBounds(200, 200, 450, 350);
	 * controller.show(win);
	 * application.sleep(3000);
	 * win.hide();
	 *
	 * @return Boolean true if the window was successfully closed and false otherwise.
	 */
	public boolean js_hide()
	{
		return impl.hide();
	}

	/**
	 * Frees the resources allocated by this window. If window is visible, it will close it first.
	 * The window will no longer be available with application.getWindow('windowName') and will no longer be usable.
	 *
	 * The main application window cannot be destroyed.
	 *
	 * @sample
	 * var getWindow = application.getWindow("someWindowName");
	 * getWindow.destroy();
	 * getWindow = application.getWindow("someWindowName");
	 * if (getWindow == null) {
	 * 	application.output("Window has been destroyed");
	 * } else {
	 * 	application.output("Window could not be destroyed");
	 * }
	 */
	public void js_destroy()
	{
		impl.destroy();
	}

	/**
	 * Returns true if the window is visible, false otherwise.
	 *
	 * @sample
	 * var someWindow = application.getWindow("someWindowName");
	 * if (someWindow.isVisible() == false) {
	 * 	controller.show(someWindow);
	 * 	someWindow.resizable = false;
	 * }
	 *
	 * @return true if the window is visible, false otherwise.
	 */
	public boolean js_isVisible()
	{
		return impl.isVisible();
	}

	/**
	 * Bring this window in front of other windows, if possible.
	 *
	 * @sample
	 * var win1 = application.createWindow("Window 1", JSWindow.WINDOW, null);
	 * win1.setInitialBounds(200, 200, 450, 350);
	 * win1.setTitle("Window 1");
	 * win1.showTextToolbar(false);
	 * controller.show(win1);
	 *
	 * var win2 = application.createWindow("Window 2", JSWindow.WINDOW, null);
	 * win2.setInitialBounds(500, 500, 450, 350);
	 * win2.setTitle("Window 2");
	 * win2.showTextToolbar(false);
	 * controller.show(win2);
	 *
	 * var win3 = application.createWindow("Window 3", JSWindow.WINDOW, null);
	 * win3.setInitialBounds(650, 700, 450, 350);
	 * win3.setTitle("Window 3");
	 * win3.showTextToolbar(true);
	 * controller.show(win3);
	 *
	 * application.sleep(2000);
	 * win3.toBack();
	 * application.sleep(2000);
	 * win1.toFront();
	 *
	 */
	public void js_toFront()
	{
		impl.toFront();
	}

	/**
	 * Shows this window behind other windows, if possible.
	 *
	 * @sampleas js_toFront()
	 */
	public void js_toBack()
	{
		impl.toBack();
	}

	// For future implementation of case 286968
//		/**
//		 * Sets the initial window state. Can be one of JSWindow.NORMAL (default), JSWindow.MAXIMIZED, JSWindow.ICONIFIED and JSWindow.MAXIMIZED_ICONIFIED.
//		 * This only affects WINDOW type windows.
//		 *
//		 * The initial state of a window is only used the first time a window with this name is shown (there is no previous stored state for this window).
//		 * If initial state or bounds are set, the last state and bounds of the window will be stored persistently (across client sessions) and restored the next time this window (identified by windowName) is shown.
//		 * If setInitialState()/setInitialBounds() are not called after a window is created and before it is shown, no old stored state/bounds will be restored (even if it is available).
//		 *
//		 * setSize()/setLocation()/setState() have priority over setInitialState(). If setSize()/setLocation() are called or if neither setState() nor setInitialState() are called before the window is shown, the window will default to JSWindow.NORMAL state.
//		 *
//		 * @sample
//		 * var win = application.createWindow("myName", JSWindow.WINDOW); // type must be JSWindow.WINDOW for initial state to have any effect
//		 * win.setInitialState(JSWindow.MAXIMIZED);
//		 * // do not use for example win.setState(JSWindow.NORMAL); here as it would override the effect of setInitialState()
//		 * forms.myForm.show(win);
//		 *
//		 * @param initialState the initial state of the window. Can be JSWindow.NORMAL (default), JSWindow.MAXIMIZED, JSWindow.ICONIFIED or JSWindow.MAXIMIZED_ICONIFIED.
//		 */
//		public void js_setInitialState(int initialState)a
//		{
//			if (initialState != NORMAL && initialState != MAXIMIZED && initialState != ICONIFIED && initialState != MAXIMIZED_ICONIFIED)
//			{
//				throw new IllegalArgumentException("State must be one of JSWindow.NORMAL, JSWindow.MAXIMIZED, JSWindow.ICONIFIED and JSWindow.MAXIMIZED_ICONIFIED.");
//			}
//			this.initialState = initialState;
//		}

	// For future implementation of case 286968
//		/**
//		 * Sets the initial window bounds.
//		 *
//		 * The initial bounds of a window are only used the first time a window with this name is shown (there are no previous stored bounds for this window).
//		 * If initial bounds or state are set, the last bounds and state of the window will be stored persistently (across client sessions) and restored the next time this window (identified by windowName) is shown.
//		 * If setInitialState()/setInitialBounds() are not called after a window is created and before it is shown, no old stored state/bounds will be restored (even if they are available).
//		 * Use JSWindow.DEFAULT for parameters if you want the window to auto-determine bounds the first time it is shown.
//		 *
//		 * setSize()/setLocation()/setState()/setInitialState() have priority over setInitialBounds(). If none of these 5 methods are called before the window is shown, the window will be shown using self-determined bounds.
//		 * in sample: // do not use win.setSize(...) or win.setLocation(...) here as they would override the effect of setInitialBounds()
//		 *

	/**
	 * Set the window location.
	 * If the coordinates are not valid they might be corrected. (for example out of screen locations)
	 *
	 * @sample
	 * var window = application.createWindow('test',JSWindow.DIALOG);
	 * window.setLocation(0,0);
	 * window.setSize(400,600);
	 * window.show(forms.child1);
	 *
	 * @param x x coordinate.
	 * @param y y coordinate.
	 */
	public void js_setLocation(int x, int y)
	{
		impl.setLocation(x, y);
	}


	/**
	 * Deletes the window's currently stored bounds. It will only affect the next show of the window.
	 *
	 * @sample
	 * var win1 = application.createWindow("Window 1", JSWindow.DIALOG, null);
	 * win1.title = "Window 1";
	 * win1.setInitialBounds(200, 200, 400, 600);
	 * win1.storeBounds = true;
	 * if (newSolutionVersion) win1.resetBounds();
	 * win1.show(forms.myform);
	 */
	public void js_resetBounds()
	{
		impl.resetBounds();
	}

	/**
	 * Set the window size.
	 *
	 * @sample
	 * var window = application.createWindow('test',JSWindow.DIALOG);
	 * window.setLocation(0,0);
	 * window.setSize(400,600);
	 * window.show(forms.child1);
	 *
	 * @param width the width.
	 * @param height the height.
	 */
	public void js_setSize(int width, int height)
	{
		impl.setSize(width, height);
	}

	/**
	 * Sets the dialog CSS class, can not be used to alter it when already showing, this should be set before the dialog is used.
	 * See sample code for examples of CSS classes for display customizations
	 *
	 * @sample
	 * // Here are some examples of a classes for customizing the display of the window
	 *
	 * //	.myDlgCSS {
	 * //		border-radius: initial; // show edged dialog corners
	 * //		-webkit-box-shadow: 0 5px 15px rgba(0,0,0,.0); // hide dialog box shadow
	 * //		box-shadow: 0 5px 15px rgba(0,0,0,.0); // hide dialog box shadow
	 * //	}
	 * //
	 * //	// dialog header styling
	 * //	.myDlgCSS .window-header {
	 * //		background: green;
	 * //	}
	 * //
	 * //	// style/hide dialog close button
	 * //	.myDlgCSS .window-header .svy-dialog-close {
	 * //		display: none;
	 * //	}
	 * //
	 * //	// dialog body styling
	 * //	.myDlgCSS .window-body {
	 * //		background: yellow;
	 * //	}
	 * //
	 * //	// dialog footer styling/hiding
	 * //	.myDlgCSS .window-footer {
	 * //		background: blue;
	 * //		display: none !important;
	 * //	}
	 *
	 * var win = application.createWindow("myName", JSWindow.DIALOG);
	 * win.setCSSClass('myDlgCSS');
	 *
	 * @param cssClassName CSS class name
	 */
	public void js_setCSSClass(String cssClassName)
	{
		impl.setCSSClass(cssClassName);
	}

	/**
	 * Get the current controller from the window/dialog.
	 *
	 * @sample
	 * var formName = application.getWindow('test').controller.getName();
	 */
	@JSReadonlyProperty
	public JSForm getController()
	{
		IFormController controller = impl.getController();
		if (controller != null)
		{
			return controller.initForJSUsage();
		}
		return null;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSWindow[name:" + impl.getName() + ",visible:" + impl.isVisible() + ",destroyed:" + impl.destroyed + ",bounds:" +
			new Rectangle(impl.getX(), impl.getY(), impl.getWidth(), impl.getHeight()) + ",title:" + impl.getTitle() + ']';
	}
	// For future implementation of case 286968
//		/**
//		 * Set the state of the window. Can be called before showing the window.
//		 * This only affects WINDOW type windows.
//		 * @param state the window state. Can be JSWindow.NORMAL (default), JSWindow.MAXIMIZED, JSWindow.ICONIFIED or JSWindow.MAXIMIZED_ICONIFIED.
//		 */
//		public void js_setState(int state)
//		{
//			if (state != NORMAL && state != MAXIMIZED && state != ICONIFIED && state != MAXIMIZED_ICONIFIED)
//			{
//				throw new IllegalArgumentException("State must be one of JSWindow.NORMAL, JSWindow.MAXIMIZED, JSWindow.ICONIFIED and JSWindow.MAXIMIZED_ICONIFIED.");
//			}
//			this.state = state;
//		}

//		/**
//		 * Returns the window state. It only makes sense for WINDOW type windows.
//		 * @return the window state.Will be one of JSWindow.NORMAL (default), JSWindow.MAXIMIZED, JSWindow.ICONIFIED and JSWindow.MAXIMIZED_ICONIFIED.
//		 */
//		public int js_getState()
//		{
//			return state == DEFAULT ? NORMAL : state;
//		}

}