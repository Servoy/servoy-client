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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.border.Border;

import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.plugins.IWebRuntimeWindow;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.server.headlessclient.dataui.WebDefaultRecordNavigator;
import com.servoy.j2db.util.ComponentFactoryHelper;

/**
 * Web implementation of the JSWindow.
 * @author acostescu
 * @since 6.0
 */
public class WebRuntimeWindow extends RuntimeWindow implements IWebRuntimeWindow
{
	public WebRuntimeWindow(IWebClientApplication application, String windowName, int windowType, RuntimeWindow parentWindow)
	{
		super(application, windowName, windowType, parentWindow);
	}

	@Override
	public IWebClientApplication getApplication()
	{
		return (IWebClientApplication)super.getApplication();
	}

	@Override
	protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		FormManager fm = (FormManager)getApplication().getFormManager();
		IMainContainer parentContainer = getParentContainerForShow(fm);
		IMainContainer dialogContainer = fm.getOrCreateMainContainer(windowName);

		//calling container can be set just after the creation of the container (needed for browser back button (wicket undo))
		((MainPage)dialogContainer).setCallingContainerIfNull((MainPage)parentContainer);
		if (formName != null)
		{
			final FormController fp = fm.showFormInMainPanel(formName, dialogContainer, title, closeAll || !legacyV3Behavior, windowName);
			if (fp != null && fp.getName().equals(formName) && dialogContainer != parentContainer)
			{
				Rectangle r2;
				if (FormManager.FULL_SCREEN.equals(initialBounds))
				{
					r2 = initialBounds;
				}
				else
				{
					r2 = getSizeAndLocation(initialBounds, dialogContainer, fp);
					if (Application.get().getDebugSettings().isAjaxDebugModeEnabled())
					{
						r2.height += 40;
					}
				}

				if (windowType == JSWindow.WINDOW)
				{
					((MainPage)parentContainer).showPopupWindow((MainPage)dialogContainer, title, r2, resizable, closeAll || !legacyV3Behavior);
				}
				else
				{
					((MainPage)parentContainer).showPopupDiv((MainPage)dialogContainer, title, r2, isUndecorated() ? false : resizable,
						closeAll || !legacyV3Behavior, (windowType == JSWindow.MODAL_DIALOG), isUndecorated(), storeBounds, getOpacity(), getTransparent());
				}
			}
		}
		if (getTitle() != null) setTitle(getTitle());

		if (windowType == JSWindow.MODAL_DIALOG && ((WebClient)getApplication()).getEventDispatcher() != null)
		{
			((WebClient)getApplication()).getEventDispatcher().suspend(this);
		}
	}

	private IMainContainer getParentContainerForShow(FormManager fm)
	{
		IMainContainer parentContainer = null;
		if (initialParentWindow != null && initialParentWindow.isVisible()) parentContainer = fm.getMainContainer(initialParentWindow.getName());
		if (parentContainer == null) parentContainer = fm.getCurrentContainer();
		return parentContainer;
	}

	@Override
	public int getHeight()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)getApplication().getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getHeight(); // can never be null normally...
		else return 0;
	}

	@Override
	public int getWidth()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)getApplication().getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getWidth(); // can never be null normally...
		else return 0;
	}

	@Override
	public int getX()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)getApplication().getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getX(); // can never be null normally...
		else return 0;
	}

	@Override
	public int getY()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)getApplication().getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getY(); // can never be null normally...
		else return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.RuntimeWindow#hide(boolean)
	 */
//	@Override
//	public boolean hide(boolean closeAll)
//	{
//		boolean hide = super.hide(closeAll);
//		if (hide && (windowType == JSWindow.MODAL_DIALOG))
//		{
//			IFunctionExecutor executor = (IFunctionExecutor)getApplication().getScriptEngine();
//			executor.resume(this);
//		}
//		return hide;
//	}

	@Override
	public void hideUI()
	{
		MainPage mp = getMainPage();
		if (mp != null) mp.close();
		if (windowType == JSWindow.MODAL_DIALOG && ((WebClient)getApplication()).getEventDispatcher() != null)
		{
			((WebClient)getApplication()).getEventDispatcher().resume(this);
		}
	}

	@Override
	public void setTitle(String title)
	{
		setTitle(title, false);
	}

	@Override
	public void setTitle(final String title, boolean delayed)
	{
		super.setTitle(title);
		final MainPage mp = getMainPage();
		if (mp != null)
		{
			if (delayed)
			{
				getApplication().invokeLater(new Runnable()
				{
					public void run()
					{
						// see FormManager showFormInMainPanel, title is set delayed, have to delay here also
						mp.setTitle(title);
					}
				});
			}
			else
			{
				mp.setTitle(title);
			}
		}
	}

	@Override
	public Object getWrappedObject()
	{
		return null; // not yet used in WC
	}

	@Override
	public void setLocation(int x, int y)
	{
		initialBounds.x = x;
		initialBounds.y = y;
		MainPage dialogContainer = (MainPage)((FormManager)getApplication().getFormManager()).getOrCreateMainContainer(windowName);
		if (windowType == JSWindow.WINDOW)
		{
			if (dialogContainer != null && dialogContainer.isShowingInWindow())
			{
				dialogContainer.appendJavaScriptChanges("window.moveTo(" + x + "," + y + ");");
			}
		}
		else
		{
			if (dialogContainer != null && dialogContainer.isShowingInDialog())
			{
				dialogContainer.setDialogBounds(windowName, x, y, -1, -1);
			}
		}
	}

	@Override
	public void setSize(int width, int height)
	{
		initialBounds.width = width;
		initialBounds.height = height;
		MainPage dialogContainer = (MainPage)((FormManager)getApplication().getFormManager()).getOrCreateMainContainer(windowName);
		if (windowType == JSWindow.WINDOW)
		{
			if (dialogContainer != null && dialogContainer.isShowingInWindow())
			{
				dialogContainer.appendJavaScriptChanges("window.resizeTo(" + width + "," + height + ");");
			}
		}
		else
		{
			if (dialogContainer != null && dialogContainer.isShowingInDialog())
			{
				dialogContainer.setDialogBounds(windowName, -1, -1, width, height);
			}
		}
	}

	@Override
	public void toBack()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			mp.toBack();
		}
	}

	@Override
	public void toFront()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			mp.toFront();
		}
	}

	@Override
	public boolean isVisible()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			return (mp.isShowingInDialog() || mp.isShowingInWindow());
		}
		return false;
	}

	private Rectangle getSizeAndLocation(Rectangle r, IMainContainer container, final FormController fp)
	{
		int navid = fp.getForm().getNavigatorID();
		Dimension size = new Dimension(fp.getForm().getSize());
		if (navid == Form.NAVIGATOR_DEFAULT && fp.getForm().getView() != FormController.TABLE_VIEW &&
			fp.getForm().getView() != FormController.LOCKED_TABLE_VIEW)
		{
			size.width += WebDefaultRecordNavigator.DEFAULT_WIDTH;
			if (size.height < WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB) size.height = WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB;
		}
		else if (navid != Form.NAVIGATOR_NONE)
		{
			FormController currentNavFC = container.getNavigator();
			if (currentNavFC != null)
			{
				size.width += currentNavFC.getForm().getSize().width;
				int navHeight = currentNavFC.getForm().getSize().height;
				if (size.height < navHeight) size.height = navHeight;
			}
		}

		// Why 22 here? From Wicket CSS:
		// "div.wicket-modal div.w_right_1" brings 10px through "margin-left" property
		// "div.wicket-modal div.w_right_1" brings 10px through "margin-right" property
		// "div.wicket-modal div.w_right_1" brings 2px through "border" property (1px from left and 1px from right)
		if (!isUndecorated()) size.width += 22;

		Border b = fp.getFormUI().getBorder();
		if (b != null)
		{
			Insets bIns = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
			size.height = size.height + bIns.top + bIns.bottom;
			size.width = size.width + bIns.left + bIns.right;
		}


		Rectangle r2 = new Rectangle(size);
		if (r != null)
		{
			if (r.height > 0)
			{
				r2.height = r.height;
			}
			if (r.width > 0)
			{
				r2.width = r.width;
			}
			r2.x = r.x;
			r2.y = r.y;
		}
		return r2;
	}

	protected MainPage getMainPage()
	{
		return (MainPage)((FormManager)getApplication().getFormManager()).getMainContainer(windowName);
	}

	@Override
	public void resetBounds()
	{
		MainPage mp = getMainPage();
		if (mp == null)
		{
			RequestCycle rc = RequestCycle.get();
			if (rc != null)
			{
				Page tmp = rc.getResponsePage();
				if ((tmp instanceof MainPage))
				{
					mp = (MainPage)tmp;
				}
			}
			if (mp == null) mp = (MainPage)((FormManager)getApplication().getFormManager()).getMainContainer(null);
		}
		if (mp != null)
		{
			mp.resetBounds(windowName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setCSSClass(java.lang.String)
	 */
	@Override
	public void setCSSClass(String cssClassName)
	{
		// ignored
	}
}