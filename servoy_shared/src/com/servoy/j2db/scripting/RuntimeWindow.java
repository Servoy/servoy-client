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

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.plugins.IRuntimeWindow;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Abstract class that gathers the basic functionality that windows should have.
 * Some of this functionality might not be currently available in web client.
 * @author acostescu
 * @since 6.0
 */
public abstract class RuntimeWindow implements IRuntimeWindow
{
	private final IApplication application;

	// values that remain unchanged
	protected final String windowName;
	protected final int windowType;
	protected final RuntimeWindow initialParentWindow;

	// values that are held independently of wrapped object (their values should only be relevant if wrapped object is null, otherwise these should be read from the wrapped object - to be implemented in subclasses)
//	protected int initialState = DEFAULT;
	protected Rectangle initialBounds = new Rectangle(-1, -1, 0, 0);//= null;
	protected boolean resizable = true;
//	protected Rectangle bounds = null;
//	protected int state = DEFAULT;
	protected String title = null;
	protected boolean showTextToolbar = false;

	protected boolean storeBounds = false;

	protected boolean destroyed = false;

	private final JSWindow jsWindow;

	private boolean undecorated = false;
	private float opacity = 1;
	private boolean isTransparent = false;

	protected RuntimeWindow(IApplication application, String windowName, int windowType, RuntimeWindow parentWindow)
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

	/**
	 * @return the application
	 */
	public IApplication getApplication()
	{
		return application;
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

	public boolean isResizable()
	{
		return resizable;
	}

	public boolean getStoreBounds()
	{
		return storeBounds;
	}

	public void setStoreBounds(boolean storeBounds)
	{
		this.storeBounds = storeBounds;
	}

	public abstract void resetBounds();

	public abstract void setLocation(int x, int y);

	public abstract int getX();

	public abstract int getY();

	public abstract void setSize(int width, int height);

	public abstract int getWidth();

	public abstract int getHeight();

	public void setUndecorated(boolean undecorated)
	{
		this.undecorated = undecorated;
	}

	public boolean isUndecorated()
	{
		return undecorated || opacity != 1;
	}

	public void setOpacity(float opacity)
	{
		this.opacity = opacity;
	}

	public float getOpacity()
	{
		return opacity;
	}

	public void setTransparent(boolean isTransparent)
	{
		this.isTransparent = isTransparent;
	}

	public boolean getTransparent()
	{
		return isTransparent;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public abstract void setTitle(String title, boolean delayed);

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

	public final boolean hide()
	{
		return hide(true);
	}


	/**
	 * @param closeAll legacy behavior parameter. (multiple forms stacked in same "default" modal dialog)
	 */
	public final boolean hide(boolean closeAll)
	{
		return application.getRuntimeWindowManager().closeFormInWindow(windowName, closeAll);
	}


	public void destroy()
	{
		if (isVisible() && !hide()) return;
		application.getRuntimeWindowManager().removeWindow(windowName);
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
	 * A servoy.properties property is able to inhibit the windows from changing bounds at JS calls.
	 * @return true if location and size altering through code is enabled.
	 */
	protected boolean canChangeBoundsThroughScripting()
	{
		return Utils.getAsBoolean(application.getSettings().getProperty("window.resize.location.enabled", "true")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public abstract void hideUI();

	public void oldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		if (destroyed)
		{
			throw new RuntimeException("Trying to show unloaded (destroyed) window");
		}
		doOldShow(formName, closeAll, legacyV3Behavior);
	}

	public void showObject(Object form) throws ServoyException
	{
		String f = null;
		if (form instanceof BasicFormController)
		{
			f = ((BasicFormController)form).getName();
		}
		else if (form instanceof FormScope)
		{
			f = ((FormScope)form).getFormController().getName();
		}
		else if (form instanceof FormController.JSForm)
		{
			f = ((FormController.JSForm)form).getFormPanel().getName();
		}
		else if (form instanceof String)
		{
			f = (String)form;
		}
		else if (form instanceof JSForm)
		{
			f = ((JSForm)form).getName();
		}
		if (f != null)
		{
			Form frm = application.getFlattenedSolution().getForm(f);
			IBasicFormManager fm = application.getFormManager();
			if (frm == null && fm.isPossibleForm(f)) frm = fm.getPossibleForm(f);
			if (!application.getFlattenedSolution().formCanBeInstantiated(frm))
			{
				// abstract form
				throw new ApplicationException(ServoyException.ABSTRACT_FORM, new Object[] { f });
			}

			show(f);
		}
	}

	/**
	 * @param formName the correct name of an existing form or a form that can be instantiated.
	 */
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

	public IFormController getController()
	{
		IBasicMainContainer container = application.getFormManager().getMainContainer(windowName);
		if (container != null)
		{
			return container.getController();
		}
		return null;
	}

	public abstract void setCSSClass(String cssClassName);
}
