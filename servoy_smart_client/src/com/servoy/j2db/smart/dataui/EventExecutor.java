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
package com.servoy.j2db.smart.dataui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.util.Utils;

/**
 * The event executor for the swing (smart) client.
 *
 * @author jcompagner
 */
public class EventExecutor extends BaseEventExecutor implements MouseListener, FocusListener, Serializable, KeyListener
{
	public static final int PREVENT_MULTIPLE_GAIN = 1;
	public static final int PREVENT_MULTIPLE_LOST = 2;

	private final Component component;
	private Component enclosedComponent;
	private boolean didAddFocusListener;
	private boolean selectAllLater = false;

	public EventExecutor(Component comp)
	{
		this(comp, comp);
	}

	public EventExecutor(Component comp, Component enclosedcomp)
	{
		component = comp;
		enclosedComponent = enclosedcomp;
	}

	public void setEnclosedComponent(Component enclosed)
	{
		if (didAddFocusListener)
		{
			enclosedComponent.removeFocusListener(this);
			enclosed.addFocusListener(this);
		}
		enclosedComponent = enclosed;
	}

	public void mouseReleased(MouseEvent e)
	{
		if (enclosedComponent instanceof JTextComponent && !((JTextComponent)enclosedComponent).isEditable() && SwingUtilities.isLeftMouseButton(e) &&
			((JTextComponent)enclosedComponent).isEnabled())
		{
			actionPerformed(e.getModifiers(), e.getPoint());
		}
	}

	private boolean enterKeyPressed = false;

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			enterKeyPressed = true;
		}
	}

	public void keyReleased(KeyEvent e)
	{
		//for the moment it's hard-coded (in future, define different methods for different keys
		if (e.getKeyCode() == KeyEvent.VK_ENTER && enterKeyPressed)
		{
			enterKeyPressed = false;
			if (enclosedComponent instanceof JTextArea && !((JTextArea)enclosedComponent).isEditable())
			{
				actionPerformed(e.getModifiers());
			}
		}
		else if (rightClickCommand != null && (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU || (e.getKeyCode() == KeyEvent.VK_F10 && e.isShiftDown())))
		{
			Component fieldComponent = enclosedComponent;
			if (!(fieldComponent instanceof IComponent))
			{
				if (fieldComponent.getParent() instanceof IFieldComponent)
				{
					fieldComponent = fieldComponent.getParent();
				}
				else if (fieldComponent.getParent() != null && fieldComponent.getParent().getParent() instanceof IFieldComponent)
				{
					fieldComponent = fieldComponent.getParent().getParent();
				}
			}
			fireRightclickCommand(true, fieldComponent, e.getModifiers(), null);
			e.consume();
		}
	}

	public void keyTyped(KeyEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		if (selectAllLater && getSelectOnEnter() && enclosedComponent instanceof JTextComponent)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					selectAllLater = false;
					((JTextComponent)enclosedComponent).selectAll();
				}
			});
		}
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void setEnterCmds(String[] enterCommands, Object[][] args)
	{
		super.setEnterCmds(enterCommands, args);
		if (hasEnterCmds() && !didAddFocusListener)
		{
			enclosedComponent.addFocusListener(this);
			didAddFocusListener = true;
		}
	}

	@Override
	public void setLeaveCmds(String[] leaveCommands, Object[][] args)
	{
		super.setLeaveCmds(leaveCommands, args);
		if (hasLeaveCmds() && !didAddFocusListener)
		{
			enclosedComponent.addFocusListener(this);
			didAddFocusListener = true;
		}
	}


	@Override
	public void setSelectOnEnter(boolean selectOnEnter)
	{
		super.setSelectOnEnter(selectOnEnter);
		if (selectOnEnter && !didAddFocusListener)
		{
			enclosedComponent.addFocusListener(this);
			didAddFocusListener = true;
		}
	}

	/*
	 * _____________________________________________________________ Methods for handling AWT action event
	 */
	public void actionPerformed(int modifiers)
	{
		actionPerformed(modifiers, null);
	}

	public void actionPerformed(int modifiers, Point mouseLocation)
	{
		if (hasActionCmd())
		{
			actionListener.setLastKeyModifiers(modifiers);
			fireActionCommand(true, component, modifiers, mouseLocation);
		}
	}

	/*
	 * _____________________________________________________________ Methods for handling AWT focus events
	 */
	//safety boolean for extra check in stopEditing (in a listview or table)
	private boolean skipFireFocusLostCommand = false;

	private boolean skipFireFocusGainedCommandDialogTest = false;


	private boolean skipSelectOnEnter = false;

	public void skipSelectOnEnter()
	{
		skipSelectOnEnter = true;
	}

	public void focusGained(FocusEvent e)
	{
		selectOnEnter();
		skipSelectOnEnter = false;
		skipFireFocusLostCommand = false;
		if (skipFireFocusGainedCommand)
		{
			skipFireFocusGainedCommand = false;
			skipFireFocusGainedCommandDialogTest = false;
			return;
		}
		if (skipFireFocusGainedCommandDialogTest)
		{
			skipFireFocusGainedCommandDialogTest = false;
			return;
		}

		if (component instanceof DataChoice)
		{
			if (e.getOppositeComponent() != null && e.getOppositeComponent().getParent() == null &&
				(((DataChoice)component).getChoiceType() == Field.LIST_BOX || ((DataChoice)component).getChoiceType() == Field.MULTISELECT_LISTBOX))
			{
				// for listbox only trigger if we really entered the field
				return;
			}
		}

		fireEnterCommands(true, component, MODIFIERS_UNSPECIFIED);

		// only if the component still has focus set the skip on true.
		// if a action command did display a dialog the focus is already gone again.
		if (e.getComponent().hasFocus())
		{
			skipFireFocusGainedCommandDialogTest = true;
		}
	}

	/**
	 *
	 */
	public void selectOnEnter()
	{
		if (getSelectOnEnter() && !skipSelectOnEnter)
		{
			if (enclosedComponent instanceof JTextComponent)
			{
				if (Utils.isAppleMacOS())
				{
					// aqua caret will modify caret position on mouse click
					selectAllLater = true;
				}
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						((JTextComponent)enclosedComponent).selectAll();
					}
				});
			}
		}
	}

	public void focusLost(FocusEvent e)
	{
		skipFireFocusGainedCommandDialogTest = false;
		if (skipFireFocusLostCommand)
		{
			skipFireFocusLostCommand = false;
			return;
		}
		skipFireFocusLostCommand = true;
		if (component instanceof DataChoice)
		{
			if (e.getOppositeComponent() != null && e.getOppositeComponent().getParent() == null &&
				(((DataChoice)component).getChoiceType() == Field.LIST_BOX || ((DataChoice)component).getChoiceType() == Field.MULTISELECT_LISTBOX))
			{
				// for listbox only trigger if we really left the field
				return;
			}
		}
		fireLeaveCommands(component, true, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	public boolean mustFireFocusGainedCommand()
	{
		return !skipFireFocusGainedCommand && !skipFireFocusGainedCommandDialogTest;
	}

	public boolean mustFireFocusLostCommand()
	{
		return !skipFireFocusLostCommand;
	}

	public void skipNextFocusLost()
	{
		skipFireFocusLostCommand = true;
	}

	public void resetFireFocusGainedCommand()
	{
		skipFireFocusGainedCommandDialogTest = false;
		skipFireFocusGainedCommand = false;
	}

	@Override
	protected String getFormName()
	{
		return getFormName(component);
	}

	@Override
	protected String getFormName(Object display)
	{
		for (Component container = (Component)display; container != null; container = container.getParent())
		{
			if (container instanceof IFormUI)
			{
				return ((IFormUI)container).getController().getName();
			}
		}
		return super.getFormNameInternal();
	}

}
