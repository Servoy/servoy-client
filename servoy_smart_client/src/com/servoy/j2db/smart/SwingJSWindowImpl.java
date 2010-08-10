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
package com.servoy.j2db.smart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.RootPaneContainer;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormDialog;
import com.servoy.j2db.FormFrame;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormWindow;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.LAFManager;
import com.servoy.j2db.scripting.JSWindowImpl;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UIUtils;

/**
 * Swing implementation of the JSWindow. It is based on a wrapped awt Window instance.
 * @author acostescu
 * @since 6.0
 */
public class SwingJSWindowImpl extends JSWindowImpl
{

	protected Window wrappedWindow = null; // will be null before the JSWindow is first shown or after the JSWindow is destroyed; can be JFrame (in case of main app. frame), FromFrame or FormDialog
	protected TextToolbar textToolbar;

	public SwingJSWindowImpl(IApplication application, String windowName, int windowType, JSWindowImpl parentWindow)
	{
		super(application, windowName, windowType, parentWindow);
	}

	@Override
	public int getHeight()
	{
		if (wrappedWindow == null)
		{
			return 0;
			// For future implementation of case 286968 change
//			return super.getHeight();
		}
		else
		{
			return wrappedWindow.getHeight();
		}
	}

	@Override
	public int getWidth()
	{
		if (wrappedWindow == null)
		{
			return 0;
			// For future implementation of case 286968 change
//			return super.getWidth();
		}
		else
		{
			return wrappedWindow.getWidth();
		}
	}

	@Override
	public int getX()
	{
		if (wrappedWindow == null)
		{
			return 0;
			// For future implementation of case 286968 change
//			return super.getX();
		}
		else
		{
			if (wrappedWindow.isShowing()) return wrappedWindow.getLocationOnScreen().x; // in case of multiple monitors that are used as virtual screen we want to get the virtual coordinates (so that when set back the window will show on the same monitor); getX, getY, ... will return the coordinates on current physical screen
			else return wrappedWindow.getLocation().x;
		}
	}

	@Override
	public int getY()
	{
		if (wrappedWindow == null)
		{
			return 0;
			// For future implementation of case 286968 change
//			return super.getY();
		}
		else
		{
			if (wrappedWindow.isShowing()) return wrappedWindow.getLocationOnScreen().y; // in case of multiple monitors that are used as virtual screen we want to get the virtual coordinates (so that when set back the window will show on the same monitor); getX, getY, ... will return the coordinates on current physical screen
			else return wrappedWindow.getLocation().y;
		}
	}

	// For future implementation of case 286968 change
//	@Override
//	public int getState()
//	{
//		if (wrappedWindow instanceof JFrame)
//		{
//			int extendedState = ((JFrame)wrappedWindow).getExtendedState();
//			int servoyState = NORMAL; // 0
//			if ((extendedState & Frame.ICONIFIED) == Frame.ICONIFIED) servoyState = servoyState | ICONIFIED;
//			if ((extendedState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) servoyState = servoyState | MAXIMIZED;
//			return servoyState;
//		}
//		else
//		{
//			return super.getState();
//		}
//	}

	@Override
	public JSWindow getParent()
	{
		if (wrappedWindow != null)
		{
			Window parent = wrappedWindow.getOwner();
			JSWindowImpl pw;
			if (parent == application.getMainApplicationFrame())
			{
				pw = application.getJSWindowManager().getWindow(null);
			}
			else if (parent != null)
			{
				pw = application.getJSWindowManager().getWindow(parent.getName());
			}
			else pw = null;
			return (pw != null) ? pw.getJSWindow() : null;
		}
		return super.getParent();
	}

	@Override
	public void setLocation(int x, int y)
	{
		if (wrappedWindow == null)
		{
			// For future implementation of case 286968 change
//			super.setLocation(x, y); // initial location
		}
		else if (canChangeBoundsThroughScripting())
		{
			if (x < 0 && y < 0)
			{
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

				x = (screenSize.width / 2) - (getWidth() / 2);
				y = (screenSize.height / 2) - (getHeight() / 2);
			}

			// For future implementation of case 286968 change
//			super.setLocation(x, y);
			wrappedWindow.setLocation(x, y);
		}
	}

	@Override
	public void setSize(int width, int height)
	{
		if (wrappedWindow == null)
		{
			// For future implementation of case 286968 change
//			super.setSize(width, height); // initial size
		}
		else if (canChangeBoundsThroughScripting())
		{
			// For future implementation of case 286968 change
//			super.setSize(width, height);
			wrappedWindow.setSize(width, height);
			wrappedWindow.validate();
		}
	}

	@Override
	public void setResizable(boolean resizable)
	{
		super.setResizable(resizable);
		if (wrappedWindow instanceof JFrame) ((JFrame)wrappedWindow).setResizable(resizable);
		else if (wrappedWindow instanceof FormDialog) ((FormDialog)wrappedWindow).setResizable(resizable);
	}

	// For future implementation of case 286968 change
//	@Override
//	public void setState(int state)
//	{
//		if (wrappedWindow instanceof JFrame)
//		{
//			int extendedState = Frame.NORMAL; // 0
//			if ((state & ICONIFIED) == ICONIFIED) extendedState = extendedState | Frame.ICONIFIED;
//			if ((state & MAXIMIZED) == MAXIMIZED) extendedState = extendedState | Frame.MAXIMIZED_BOTH;
//			((JFrame)wrappedWindow).setState(extendedState);
//		}
//		else
//		{
//			super.setState(state);
//		}
//	}

	@Override
	public void setTitle(String title)
	{
		super.setTitle(title);
		if (isVisible())
		{
			if (wrappedWindow instanceof FormFrame)
			{
				((FormFrame)wrappedWindow).setTitle(title);
			}
			else if (wrappedWindow instanceof FormDialog)
			{
				((FormDialog)wrappedWindow).setTitle(title);
			}
			else if (wrappedWindow instanceof JFrame) // for main app. frame
			{
				title = application.getI18NMessageIfPrefixed(title);
				((JFrame)wrappedWindow).setTitle(title);
			}
		} // else no use setting title as the tags can't be processed (if there is no form open)
	}

	@Override
	public void showTextToolbar(boolean showTextToolbar)
	{
		super.showTextToolbar(showTextToolbar);
		if (wrappedWindow instanceof RootPaneContainer)
		{
			applyTextToolbar((RootPaneContainer)wrappedWindow);
		}
	}

	@Override
	public void toBack()
	{
		if (wrappedWindow != null)
		{
			wrappedWindow.toBack();
		}
	}

	@Override
	public void toFront()
	{
		if (wrappedWindow != null)
		{
			wrappedWindow.toFront();
		}
	}

	@Override
	public boolean isVisible()
	{
		if (wrappedWindow != null)
		{
			return wrappedWindow.isShowing();
		}
		return false;
	}

	@Override
	public void destroy()
	{
		super.destroy();
		if (wrappedWindow != null)
		{
			// a MainPanel still keeps a reference to this Window via parent (& currently MainPanels are cached until a solution switch happens)
			// so to free up Window held memory for main panels that are no longer used, detach it
			if (wrappedWindow instanceof FormDialog) ((FormDialog)wrappedWindow).getContentPane().removeAll();
			else if (wrappedWindow instanceof JFrame) ((JFrame)wrappedWindow).getContentPane().removeAll();
			wrappedWindow.dispose();
			wrappedWindow = null;
			textToolbar = null;
		}
	}

	@Override
	public Window getWrappedObject()
	{
		return wrappedWindow;
	}

	@Override
	public void closeUI()
	{
		if (wrappedWindow instanceof FormWindow) // it should always be either null or instanceof FormWindow
		{
			((FormWindow)wrappedWindow).closeWindow();
		}
	}

	// For future implementation of case 286968
//	@Override
//	protected void doShow(String formName)
//	{
//		FormManager fm = ((FormManager)application.getFormManager());
//		boolean isDialog = (windowType == MODAL_DIALOG || windowType == DIALOG);
//		boolean isModal = (getType() == MODAL_DIALOG);
//		IMainContainer previousModalContainer = null;
//		boolean toFront = false;
//		boolean firstShow = (wrappedWindow == null);
//		IMainContainer currentMainContainer = null;
//		if (isDialog)
//		{
//			// parent matters for dialogs
//			JSWindow parentJSWindow;
//			if (initialParentWindow != null && initialParentWindow.getWrappedObject() != null && ((Window)initialParentWindow.getWrappedObject()).isShowing()) 
//			{
//				parentJSWindow = initialParentWindow;
//			}
//			else
//			{
//				IMainContainer currentModalContainer = fm.getModalDialogContainer();
//				parentJSWindow = application.getJSWindowManager().getWindow(currentModalContainer.getContainerName());
//			}
//
//			currentMainContainer = ((FormWindow)parentJSWindow.getWrappedObject()).getMainContainer();
//			Pair<Boolean, IMainContainer> p = createAndReparentDialogIfNeeded(fm, parentJSWindow, isModal);
//			toFront = p.getLeft().booleanValue();
//			previousModalContainer = p.getRight();
//			
//			// TO DO
//		}
//		else if (windowType == WINDOW)
//		{
//			// TO DO
//		} // else illegal situation that should never happen
//		else throw new RuntimeException("Unknown window type: " + windowType);
//		
//		IMainContainer container = ((FormWindow)wrappedWindow).getMainContainer();
//		final FormController fp = fm.showFormInMainPanel(formName, container, title, true, windowName);
//		
//		if (isDialog && fp != null && fp.getName().equals(formName))
//		{
//			FormDialog sfd = ((FormDialog)wrappedWindow);
//			sfd.setModal(isModal);
//			if (isModal)
//			{
//				// When a modal window is closed, the old modal window state will have to be restored...
//				// For example, when inside JS for an event you close a modal window and open another one,
//				// the new modal window must have as owner not the closed window, but the last opened modal window
//				// before the window just closed.
//				// This has to happen when setVisible(false) is called on the modal dialog. We cannot simply rely
//				// on executing this after sfd.setVisible(true) is unblocked, because then it will be executed
//				// after the new dialog is opened by java script. (because that execution continues as the next event on the EventThread)
//				sfd.setPreviousMainContainer(previousModalContainer, currentMainContainer);
//			}
//		}
//
//		finalizeShowWindow(fp, formName, container, false, false, toFront);
//	}

	@Override
	protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		if (windowType == JSWindow.WINDOW)
		{
			doOldShowInWindow(formName);
		}
		else
		{
			doOldShowInDialog(formName, closeAll, legacyV3Behavior);
		}
	}

	private void doOldShowInDialog(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		FormManager fm = ((FormManager)application.getFormManager());
		IMainContainer currentMainContainer = fm.getModalDialogContainer();
		JSWindowImpl currentModalJSWindow = application.getJSWindowManager().getWindow(currentMainContainer.getContainerName());
		boolean windowModal = ((legacyV3Behavior && wrappedWindow == null) || getType() == JSWindow.MODAL_DIALOG);

		Pair<Boolean, IMainContainer> p = createAndReparentDialogIfNeeded(fm, currentModalJSWindow, windowModal);

		boolean bringToFrontNeeded = p.getLeft().booleanValue();
		IMainContainer previousModalContainer = p.getRight();

		FormDialog sfd = (FormDialog)wrappedWindow;
		IMainContainer container = sfd.getMainContainer();

		// For none legacy the dialog must always be really closed 
		sfd.setCloseAll(closeAll || !legacyV3Behavior);

		if (sfd.isVisible())
		{
			sfd.storeBounds();
		}

		final FormController fp = fm.showFormInMainPanel(formName, container, title, closeAll && legacyV3Behavior, windowName);


		if (fp != null && fp.getName().equals(formName))
		{
			sfd.setModal(windowModal);
			if (windowModal)
			{
				// When a modal window is closed, the old modal window state will have to be restored...
				// For example, when inside JS for an event you close a modal window and open another one,
				// the new modal window must have as owner not the closed window, but the last opened modal window
				// before the window just closed.
				// This has to happen when setVisible(false) is called on the modal dialog. We cannot simply rely
				// on executing this after sfd.setVisible(true) is unblocked, because then it will be executed
				// after the new dialog is opened by java script. (because that execution continues as the next event on the EventThread)
				sfd.setPreviousMainContainer(previousModalContainer, currentMainContainer);
			}
			else
			{
				// If it is a none modal dialog, make sure the current container is reset to the currentMainContainer (== previous his parent)
				// else it is switched a bit to early (if a developer shows 2 dialogs at once from a main container)
				// the focus event of the FormDialog will set it correctly.
				fm.setCurrentContainer(currentMainContainer, currentMainContainer.getName());
			}
		}

		finalizeShowWindow(fp, formName, container, true, legacyV3Behavior, bringToFrontNeeded);
	}

	private void doOldShowInWindow(String formName)
	{
		FormManager fm = ((FormManager)application.getFormManager());
		boolean toFront = createFrameIfNeeded(fm);

		FormFrame frame = (FormFrame)wrappedWindow;
		if (frame.isVisible())
		{
			frame.storeBounds();
		}

		IMainContainer container = frame.getMainContainer();
		final FormController fp = fm.showFormInMainPanel(formName, container, title, true, windowName);

		finalizeShowWindow(fp, formName, container, true, false, toFront);
	}

	private boolean createFrameIfNeeded(FormManager fm)
	{
		IMainContainer container = fm.getOrCreateMainContainer(windowName);
		boolean bringToFrontNeeded = false;
		FormFrame frame = (FormFrame)wrappedWindow;
		if (frame == null)
		{
			wrappedWindow = frame = createFormFrame(application, windowName);
			frame.setResizable(resizable);
			frame.setMainContainer(container);
		}
		else if (frame.isVisible())
		{
			bringToFrontNeeded = true;
		}
		return bringToFrontNeeded;
	}

	private Pair<Boolean, IMainContainer> createAndReparentDialogIfNeeded(FormManager fm, JSWindowImpl parentJSWindow, boolean windowModal)
	{
		IMainContainer container = fm.getOrCreateMainContainer(windowName);
		IMainContainer previousModalContainer = null;

		FormDialog sfd = (FormDialog)wrappedWindow;
		boolean reparented = false;
		Object[] savedStatusForRecreate = null;
		// make sure the dialog has the correct owner
		if (sfd != null)
		{
			Window formDialogOwner = sfd.getOwner();
			Window owner = null;
			if (parentJSWindow == null || parentJSWindow.getWrappedObject() == null)
			{
				owner = application.getMainApplicationFrame();
			}
			else
			{
				owner = (Window)parentJSWindow.getWrappedObject();
			}
			if ((owner != sfd) && !owner.equals(formDialogOwner))
			{
				// wrong owner... will create a new window and close/dispose old one
				savedStatusForRecreate = saveWrappedWindowStatusForRecreate();
				close(true);
				sfd.dispose();
				sfd = null;
				wrappedWindow = null;
				reparented = true;
			}
		}

		if (windowModal)
		{
			previousModalContainer = fm.setModalDialogContainer(container);
		}

		boolean bringToFrontNeeded = false;
		if (sfd == null)
		{
			wrappedWindow = sfd = createFormDialog(application, parentJSWindow != null ? (Window)parentJSWindow.getWrappedObject() : null, windowModal,
				windowName);
			sfd.setResizable(resizable);
			sfd.setMainContainer(container);
			if (reparented)
			{
				restoreWrappedWindowStatusAfterRecreate(savedStatusForRecreate);
			}
		}
		else if (sfd.isVisible())
		{
			bringToFrontNeeded = true;
		}
		return new Pair<Boolean, IMainContainer>(Boolean.valueOf(bringToFrontNeeded), previousModalContainer);
	}

	private Object[] saveWrappedWindowStatusForRecreate()
	{
		return new Object[] { wrappedWindow.isShowing() ? wrappedWindow.getLocationOnScreen() : wrappedWindow.getLocation(), wrappedWindow.getSize()
//			, Integer.valueOf(getState()) // For future implementation of case 286968 change
		};
	}

	private void restoreWrappedWindowStatusAfterRecreate(Object[] savedStatusForRecreate)
	{
		wrappedWindow.setLocation((Point)savedStatusForRecreate[0]);
		wrappedWindow.setSize((Dimension)savedStatusForRecreate[1]);
		// For future implementation of case 286968 change
//		setState(((Integer)savedStatusForRecreate[2]).intValue());
		showTextToolbar(showTextToolbar);
	}

	private void finalizeShowWindow(final FormController fp, String formName, IMainContainer container, boolean oldShow, boolean legacyV3Behavior,
		boolean bringToFrontNeeded)
	{
		if (fp != null && fp.getName().equals(formName))
		{
			applyTextToolbar((RootPaneContainer)wrappedWindow);

			if (oldShow && !restoreWindowBounds())
			{
				// quickly set the form to visible if not visible.
				boolean visible = fp.getFormUI().isVisible();
				if (!visible)
				{
					((Component)fp.getFormUI()).setVisible(true);
				}
				// now calculate the preferred size
				wrappedWindow.pack();
				// if not visible before restore that state (will be set right later on)
				if (!visible) ((Component)fp.getFormUI()).setVisible(false);

				if (!FormManager.FULL_SCREEN.equals(initialBounds))
				{
					setWindowBounds(initialBounds, legacyV3Behavior);
				}
			}

			boolean findModeSet = false;

			Action action = application.getCmdManager().getRegisteredAction("cmdperformfind"); //$NON-NLS-1$
			if (action != null && fp.getFormModel() != null)
			{
				findModeSet = fp.getFormModel().isInFindMode() && !action.isEnabled();
				if (findModeSet) action.setEnabled(true);
			}
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					((RootPaneContainer)wrappedWindow).getRootPane().requestFocus();
					if (LAFManager.isUsingAppleLAF())
					{
						application.invokeLater(new Runnable()
						{
							public void run()
							{
								((Component)fp.getFormUI()).repaint();
							}
						});
					}
				}
			});

			// blocks in case of modal dialogs
			if (oldShow && FormManager.FULL_SCREEN.equals(initialBounds))
			{
				((FormWindow)wrappedWindow).setFullScreen();
			}
			else
			{
				wrappedWindow.setVisible(true);
				if (bringToFrontNeeded)
				{
					wrappedWindow.toFront();
				}
			}

			if (findModeSet && action != null) action.setEnabled(false);
		}
	}

	public void setWindowBounds(Rectangle r, boolean legacyV3Behavior)
	{
		if (wrappedWindow != null)
		{
			Rectangle r2 = wrappedWindow.getBounds();
			if (r != null)
			{
				if (r.height <= 0)
				{
					r.height = r2.height;
				}
				if (r.width <= 0)
				{
					r.width = r2.width;
				}
				if (!UIUtils.isOnScreen(r)) // with multiple monitors, all locations on a monitor can be negative
				{
					wrappedWindow.setSize(r.width, r.height);
					// if the current dialog is not visible, the the location
					// else let it be what it was.
					if (!wrappedWindow.isVisible() || legacyV3Behavior)
					{
						Window ow = wrappedWindow.getOwner();
						if (ow == null) ow = application.getMainApplicationFrame();
						wrappedWindow.setLocationRelativeTo(ow);
					}
				}
				else
				{
					wrappedWindow.setBounds(r);
					wrappedWindow.validate();
				}
			}
		}
	}

	protected FormDialog createFormDialog(IApplication app, Window owner, boolean modal, String dialogName)
	{
		if (owner == null || (!(owner instanceof JDialog || owner instanceof JFrame))) owner = app.getMainApplicationFrame();

		if (owner instanceof JDialog)
		{
			return new FormDialog(app, (JDialog)owner, modal, dialogName);
		}
		else
		{
			return new FormDialog(app, (JFrame)owner, modal, dialogName);
		}
	}

	protected FormFrame createFormFrame(IApplication app, String windowName)
	{
		FormFrame frame = new FormFrame(app, windowName);
		frame.setIconImage(application.getMainApplicationFrame().getIconImage());
		return frame;
	}

	public TextToolbar getTextToolbar()
	{
		return textToolbar;
	}

	private void applyTextToolbar(RootPaneContainer window)
	{
		if (showTextToolbar)
		{
			if (textToolbar == null)
			{
				textToolbar = new TextToolbar(application);
			}
			window.getContentPane().add(textToolbar, BorderLayout.NORTH);
		}
		else
		{
			if (textToolbar != null)
			{
				window.getContentPane().remove(textToolbar);
				textToolbar = null;
			}
		}
	}

	public void storeBounds()
	{
		if (wrappedWindow instanceof FormWindow)
		{
			((FormWindow)wrappedWindow).storeBounds();
		}
	}

	public boolean restoreWindowBounds()
	{
		if (wrappedWindow instanceof FormWindow && ((FormWindow)wrappedWindow).getMainContainer().getController() != null)
		{
			String name = ((FormWindow)wrappedWindow).getMainContainer().getController().getName();
			Rectangle r = ((FormWindow)wrappedWindow).getFormBounds(name);
			if (r != null)
			{
				setWindowBounds(r, false);
				return true;
			}
		}
		return false;
	}

}