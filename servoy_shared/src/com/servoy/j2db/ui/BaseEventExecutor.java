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
package com.servoy.j2db.ui;


import java.awt.Point;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.api.IJSEvent.EventType;
import com.servoy.j2db.util.Utils;

/**
 * Base event executor that handles all events from the elements that are on the form.
 * Subclasses implement specific code for the specific gui implementations (Swing or Wicket/Web)
 * 
 * @author jcompagner
 * 
 */
public abstract class BaseEventExecutor implements IEventExecutor
{
	protected IScriptExecuter actionListener;

	private String actionCommand;
	private Object[] actionArgs;
	private String doubleClickCommand;
	private Object[] doubleClickArgs;
	protected String rightClickCommand;
	private Object[] rightClickArgs;
	private String[] enterCommands;
	private Object[][] enterArgs;
	private String[] leaveCommands;
	private Object[][] leaveArgs;
	private String changeCommand;
	private Object[] changeArgs;

	private boolean selectOnEnter;

	private boolean validationEnabled = true;
	protected String formName;

	public void setScriptExecuter(IScriptExecuter el)
	{
		actionListener = el;
	}

	public void setChangeCmd(String id, Object[] args)
	{
		changeCommand = id;
		changeArgs = args;
	}

	public boolean hasChangeCmd()
	{
		return changeCommand != null;
	}

	public boolean hasActionCmd()
	{
		return actionCommand != null;
	}

	public void setActionCmd(String id, Object[] args)
	{
		actionCommand = id;
		actionArgs = args;
	}

	public boolean hasDoubleClickCmd()
	{
		return doubleClickCommand != null;
	}

	public void setDoubleClickCmd(String id, Object[] args)
	{
		doubleClickCommand = id;
		doubleClickArgs = args;
	}

	public boolean hasRightClickCmd()
	{
		return rightClickCommand != null;
	}

	public void setRightClickCmd(String id, Object[] args)
	{
		rightClickCommand = id;
		rightClickArgs = args;
	}

	public void setEnterCmds(String[] ids, Object[][] args)
	{
		if (ids != null && ids.length > 0)
		{
			enterCommands = ids;
			enterArgs = args;
		}
	}

	public boolean hasEnterCmds()
	{
		return enterCommands != null;
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		if (ids != null && ids.length > 0)
		{
			leaveCommands = ids;
			leaveArgs = args;
		}
	}

	public boolean hasLeaveCmds()
	{
		return leaveCommands != null;
	}

	public void setValidationEnabled(boolean b)
	{
		validationEnabled = b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IEventExecutor#getValidationEnabled()
	 */
	public boolean getValidationEnabled()
	{
		return validationEnabled;
	}

	public void setSelectOnEnter(boolean b)
	{
		selectOnEnter = b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IEventExecutor#getSelectOnEnter()
	 */
	public boolean getSelectOnEnter()
	{
		return selectOnEnter;
	}

	protected boolean skipFireFocusGainedCommand = false;

	public void skipNextFocusGain()
	{
		skipFireFocusGainedCommand = true;
	}

	public void fireChangeCommand(Object oldVal, Object newVal, boolean saveData, IDisplayData display)
	{
		Object o = fireEventCommand(EventType.dataChange, changeCommand, new Object[] { oldVal, newVal }, changeArgs, saveData, display, false,
			MODIFIERS_UNSPECIFIED, true);
		display.setValueValid(!Boolean.FALSE.equals(o), oldVal);
	}

	/* ----------------------------------------- */
	/* Event commands, JSEvent argument is added */

	public void fireEnterCommands(boolean focusEvent, Object display, int modifiers)
	{
		for (int i = 0; enterCommands != null && i < enterCommands.length; i++)
		{
			if (Boolean.FALSE.equals(fireEventCommand(JSEvent.EventType.focusGained, enterCommands[i], null, (enterArgs == null || enterArgs.length <= i)
				? null : enterArgs[i], false, display, focusEvent, modifiers, false)))
			{
				break;
			}
		}
	}

	public void fireLeaveCommands(Object display, boolean focusEvent, int modifiers)
	{
		for (int i = 0; leaveCommands != null && i < leaveCommands.length; i++)
		{
			if (Boolean.FALSE.equals(fireEventCommand(JSEvent.EventType.focusLost, leaveCommands[i], null, (leaveArgs == null || leaveArgs.length <= i) ? null
				: leaveArgs[i], false, display, focusEvent, modifiers, false)))
			{
				break;
			}
		}
	}

	public Object fireActionCommand(boolean saveData, Object display)
	{
		return fireActionCommand(saveData, display, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	public Object fireActionCommand(boolean saveData, Object display, int modifiers)
	{
		return fireActionCommand(saveData, display, modifiers, null);
	}

	public Object fireActionCommand(boolean saveData, Object display, int modifiers, Point mouseLocation)
	{
		return fireEventCommand(JSEvent.EventType.action, actionCommand, null, actionArgs, saveData, display, false, modifiers, null, false, mouseLocation);
	}

	public Object fireDoubleclickCommand(boolean saveData, Object display, int modifiers, Point mouseLocation)
	{
		return fireEventCommand(JSEvent.EventType.doubleClick, doubleClickCommand, null, doubleClickArgs, saveData, display, false, modifiers, null, false,
			mouseLocation);
	}


	public Object fireRightclickCommand(boolean saveData, Object display, int modifiers, Point mouseLocation)
	{
		return fireRightclickCommand(saveData, display, modifiers, null, mouseLocation);
	}

	public Object fireRightclickCommand(boolean saveData, Object display, int modifiers, String formName, Point mouseLocation)
	{
		return fireEventCommand(EventType.rightClick, rightClickCommand, null, rightClickArgs, saveData, display, false, modifiers, formName, false,
			mouseLocation);
	}

	/* ----------------------------------------- */

	public Object fireEventCommand(EventType type, String cmd, Object[] args, Object[] persistArgs, boolean saveData, Object display, boolean focusEvent,
		int modifiers, boolean executeWhenFieldValidationFailed)
	{
		return fireEventCommand(type, cmd, args, persistArgs, saveData, display, focusEvent, modifiers, null, executeWhenFieldValidationFailed, null);
	}

	public Object fireEventCommand(EventType type, String cmd, Object[] args, Object[] persistArgs, boolean saveData, Object display, boolean focusEvent,
		int modifiers, String formName, boolean executeWhenFieldValidationFailed, Point mouseLocation)
	{
		if (actionListener == null)
		{
			return null;
		}

		// also fire when cmd is null (may trigger field validation)
		if (modifiers != MODIFIERS_UNSPECIFIED) actionListener.setLastKeyModifiers(modifiers);
		String name = getElementName(display);
		String fName = formName;
		if (fName == null) fName = getFormName(display);

		if (fName == null) fName = this.formName;

		Object source = getSource(display);

		JSEvent event = new JSEvent();
		event.setType(type);
		event.setSource(source);
		event.setFormName(fName);
		event.setElementName(name);
		event.setModifiers(modifiers == MODIFIERS_UNSPECIFIED ? 0 : modifiers);
		if (mouseLocation != null) event.setLocation(mouseLocation);
		return actionListener.executeFunction(cmd, Utils.arrayMerge(Utils.arrayJoin(args, new Object[] { event }), persistArgs), saveData, source, focusEvent,
			null, executeWhenFieldValidationFailed);
	}

	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	protected abstract String getFormName(Object display);

	protected String getElementName(Object display)
	{
		if (display instanceof IComponent)
		{
			return ((IComponent)display).getName();
		}
		return null;
	}

	protected Object getSource(Object display)
	{
		return display;
	}
}
