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
package com.servoy.j2db.scripting;

import java.awt.Rectangle;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

/**
 * Some of the functionality might not be available in web client.
 */
public abstract class JSWindowImpl
{

	@ServoyDocumented(category = ServoyDocumented.RUNTIME)
	public static class JSWindow implements IConstantsObject
	{
		/**
		 * Window type constant that identifies a non-modal dialog type.
		 * Non-modal dialogs will allow the user to interact with parent windows, but are less independent then windows with WINDOW type.
		 * Dialogs will stay on top of parent windows and are less accessible through the OS window manager. In web-client dialogs will not
		 * open in a separate browser window.
		 * @sample
		 * // create a non-modal dialog on top of current active form's window and show a form inside it
		 * var myWindow = application.createWindow("myName", JSWindow.DIALOG);
		 * forms.myForm.show(myWindow);
		 */
		public final static int DIALOG = 0;

		/**
		 * Window type constant that identifies a modal dialog type. Modal dialogs will not allow the user to interact with the parent window(s) until closed.
		 * Dialogs will stay on top of parent windows and are less accessible through the OS window manager. In web-client dialogs will not
		 * open in a separate browser window. NOTE: no code is executed in Smart Client after a modal dialog is shown (the show operation blocks) until this dialog closes.
		 * @sample
		 * // create a modal dialog on top of current active form's window and show a form inside it
		 * var myWindow = application.createWindow("myName", JSWindow.MODAL_DIALOG);
		 * forms.myForm.show(myWindow);
		 */
		public final static int MODAL_DIALOG = 1;

		/**
		 * Window type constant that identifies a window type. WINDOW type is the most independent type of window. It will be more accessible through the OS window
		 * manager, it can appear both in front of and under other windows and it doesn't block user interaction for other windows. In web-client windows will
		 * open in a separate browser window.
		 * @sample
		 * // create a window and show a form inside it
		 * var myWindow = application.createWindow("myName", JSWindow.WINDOW);
		 * forms.myForm.show(myWindow);
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

		private final JSWindowImpl impl;

		private JSWindow(JSWindowImpl impl)
		{
			this.impl = impl;
		}

		public JSWindowImpl getImpl()
		{
			return impl;
		}

		/**
		 * Sets the initial window bounds.
		 * The initial bounds are only used the first time this window is shown.
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

		/**
		 * Set whether or not this window can be resized by the user.
		 * @param resizable true or false.
		 */
		public void js_setResizable(boolean resizable)
		{
			impl.setResizable(resizable);
		}

		/**
		 * Returns the x coordinate.
		 * @return the x coordinate.
		 */
		public int js_getX()
		{
			return impl.getX();
		}

		/**
		 * Returns the y coordinate.
		 * @return the y coordinate.
		 */
		public int js_getY()
		{
			return impl.getY();
		}

		/**
		 * Returns the width.
		 * @return the width.
		 */
		public int js_getWidth()
		{
			return impl.getWidth();
		}

		/**
		 * Returns the height.
		 * @return the height.
		 */
		public int js_getHeight()
		{
			return impl.getHeight();
		}

		/**
		 * Set the title text of this window.
		 * @param title the title text.
		 */
		public void js_setTitle(String title)
		{
			impl.setTitle(title);
		}

		/**
		 * Returns the title text.
		 * @return the title text.
		 */
		public String js_getTitle()
		{
			return impl.getTitle();
		}

		/**
		 * Sets whether or not this window should have a text tool bar. Has no effect on web client or smart client main application frame.
		 * @param showTextToolbar true if you want a text tool bar to be added to this window, false otherwise.
		 */
		public void js_showTextToolbar(boolean showTextToolbar)
		{
			impl.showTextToolbar(showTextToolbar);
		}

		/**
		 * Returns the window name. It will be null in case of main application frame.
		 * @return the window name.
		 */
		public String js_getName()
		{
			return impl.getName();
		}

		/**
		 * Returns the window type.
		 * @return the window type. Can be one of JSWindow.DIALOG, JSWindow.MODAL_DIALOG, JSWindow.WINDOW.
		 */
		public int js_getType()
		{
			return impl.getType();
		}

		/**
		 * Returns the parent JSWindow, if available.
		 * @return the parent JSWindow, if available. If there is no parent JSWindow, it will return null.
		 */
		public JSWindow js_getParent()
		{
			return impl.getParent();
		}

		/**
		 * Closes (hides) the window. It can be shown again using controller.show() or controller.showRecords().
		 * The main application window cannot be closed.
		 * @return Boolean true if the window was successfully closed and false otherwise.
		 */
		public boolean js_close()
		{
			return impl.close();
		}

		/**
		 * Frees the resources allocated by this window. If window is visible, it will close it first.
		 * The window will no longer be available with application.getWindow('windowName') and will no longer be usable.
		 * 
		 * The main application window cannot be destroyed. 
		 */
		public void js_destroy()
		{
			impl.destroy();
		}

		/**
		 * Returns true if the window is visible, false otherwise.
		 * @return true if the window is visible, false otherwise.
		 */
		public boolean js_isVisible()
		{
			return impl.isVisible();
		}

		/**
		 * Bring this window in front of other windows, if possible.
		 */
		public void js_toFront()
		{
			impl.toFront();
		}

		/**
		 * Shows this window behind other windows, if possible.
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

		// For future implementation of case 286968 change
//		/**
//		 * Set the window location. Can be called before showing the window.
//		 * @param x x coordinate.
//		 * @param y y coordinate.
//		 */
//		public abstract void js_setLocation(int x, int y);

		// For future implementation of case 286968 change
//		/**
//		 * Set the window size. Can be called before showing the window.
//		 * @param width the width.
//		 * @param height the height.
//		 */
//		public abstract void js_setSize(int width, int height);

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

	protected IApplication application;

	// values that remain unchanged
	protected final String windowName;
	protected final int windowType;
	protected final JSWindowImpl initialParentWindow;

	// values that are held independently of wrapped object (their values should only be relevant if wrapped object is null, otherwise these should be read from the wrapped object - to be implemented in subclasses)
//	protected int initialState = DEFAULT;
	protected Rectangle initialBounds = new Rectangle(-1, -1, 0, 0);//= null;
	protected boolean resizable = true;
//	protected Rectangle bounds = null;
//	protected int state = DEFAULT;
	protected String title = null;
	protected boolean showTextToolbar = false;

	protected boolean destroyed = false;

	private final JSWindow jsWindow;


	public JSWindowImpl(IApplication application, String windowName, int windowType, JSWindowImpl parentWindow)
	{
		if (windowType != JSWindow.WINDOW && windowType != JSWindow.DIALOG && windowType != JSWindow.MODAL_DIALOG)
		{
			throw new IllegalArgumentException("JSWindow type must be one of JSWindow.WINDOW, JSWindow.DIALOG, JSWindow.MODAL_DIALOG.");
		}

		this.windowName = windowName;
		this.application = application;
		this.windowType = windowType;
		this.initialParentWindow = parentWindow;

		this.jsWindow = new JSWindow(this);
	}

	public JSWindow getJSWindow()
	{
		return jsWindow;
	}

	public void setInitialBounds(int x, int y, int width, int height)
	{
		this.initialBounds = new Rectangle(x, y, width, height);
	}

	public void setResizable(boolean resizable)
	{
		this.resizable = resizable;
	}

	public abstract void setLocation(int x, int y);

	// For future implementation of case 286968 change
//	{
//		if (bounds == null) bounds = new Rectangle(x, y, DEFAULT, DEFAULT);
//		else
//		{
//			this.bounds.x = x;
//			this.bounds.y = x;
//		}
//	}

	public abstract int getX();

	// For future implementation of case 286968 change
//	{
//		return bounds != null ? bounds.x : -1;
//	}

	public abstract int getY();

	// For future implementation of case 286968 change
//	{
//		return bounds != null ? bounds.y : -1;
//	}

	public abstract void setSize(int width, int height);

	// For future implementation of case 286968 change
//	{
//		if (bounds == null) bounds = new Rectangle(DEFAULT, DEFAULT, width, height);
//		else
//		{
//			this.bounds.width = width;
//			this.bounds.height = height;
//		}
//	}

	public abstract int getWidth();

//	{
//		return bounds != null ? bounds.width : -1;
//	}

	public abstract int getHeight();

//	{
//		return bounds != null ? bounds.height : -1;
//	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public void showTextToolbar(boolean showTextToolbar)
	{
		this.showTextToolbar = showTextToolbar;
	}

	public String getName()
	{
		return windowName;
	}

	public int getType()
	{
		return windowType;
	}

	public JSWindow getParent()
	{
		return initialParentWindow != null ? initialParentWindow.getJSWindow() : null;
	}

	public boolean close()
	{
		return close(true);
	}

	public void destroy()
	{
		if (isVisible()) close();
		application.getJSWindowManager().removeWindow(windowName);
		destroyed = true;
	}

	public abstract boolean isVisible();

	public abstract void toFront();

	public abstract void toBack();

	/**
	 * Returns the underlying representation of the window. For example, in case of smart-client this would be a awt Window instance.
	 * @return the underlying representation of the window. For example, in case of smart-client this would be a awt Window instance.
	 */
	public abstract Object getWrappedObject();

	/**
	 * @param closeAll legacy behaviour param. (multiple forms stacked in same "default" modal dialog)
	 */
	public boolean close(boolean closeAll)
	{
		return application.getJSWindowManager().closeFormInWindow(windowName, closeAll);
	}

	/**
	 * A servoy.properties property is able to inhibit the windows from changing bounds at JS calls.
	 * @return true if location and size altering through code is enabled.
	 */
	protected boolean canChangeBoundsThroughScripting()
	{
		return Utils.getAsBoolean(application.getSettings().getProperty("window.resize.location.enabled", "true")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public abstract void closeUI();

	public void oldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		if (destroyed)
		{
			throw new RuntimeException("Trying to show unloaded (destroyed) window");
		}
		doOldShow(formName, closeAll, legacyV3Behavior);
	}

	public void show(String formName)
	{
		if (destroyed)
		{
			throw new RuntimeException("Trying to show unloaded (destroyed) window");
		}
		doShow(formName);
	}


	protected void doShow(String formName)
	{
		doOldShow(formName, true, false);
	}

	protected abstract void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior);

}
