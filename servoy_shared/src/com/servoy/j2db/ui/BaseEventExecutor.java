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

import com.servoy.base.scripting.api.IJSEvent.EventType;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.scripting.JSEvent;
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
	private String formName;

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
		return changeCommand != null || getFormElementChangeCommand() != null;
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
		boolean isValueValid = true;
		if (changeCommand != null)
		{
			Object o = fireEventCommand(EventType.dataChange, changeCommand, new Object[] { oldVal, newVal }, changeArgs, saveData, display, false,
				MODIFIERS_UNSPECIFIED, true);
			isValueValid = !Boolean.FALSE.equals(o) && !(o instanceof String && ((String)o).length() > 0);
			display.setValueValid(isValueValid, oldVal);
		}
		if (isValueValid)
		{
			String formElementChangeCommand = getFormElementChangeCommand();
			if (formElementChangeCommand != null)
			{
				Object o = fireEventCommand(EventType.dataChange, formElementChangeCommand, new Object[] { oldVal, newVal }, changeArgs, saveData, display, false,
					MODIFIERS_UNSPECIFIED, true);
				display.setValueValid(!Boolean.FALSE.equals(o) && !(o instanceof String && ((String)o).length() > 0), oldVal);
			}
		}
	}

	/* ----------------------------------------- */
	/* Event commands, JSEvent argument is added */

	public void fireEnterCommands(boolean focusEvent, Object display, int modifiers)
	{
		for (int i = 0; enterCommands != null && i < enterCommands.length; i++)
		{
			if (Boolean.FALSE.equals(fireEventCommand(JSEvent.EventType.focusGained, enterCommands[i], null,
				(enterArgs == null || enterArgs.length <= i) ? null : enterArgs[i], false, display, focusEvent, modifiers, false)))
			{
				break;
			}
		}
	}

	public void fireLeaveCommands(Object display, boolean focusEvent, int modifiers)
	{
		for (int i = 0; leaveCommands != null && i < leaveCommands.length; i++)
		{
			if (Boolean.FALSE.equals(fireEventCommand(JSEvent.EventType.focusLost, leaveCommands[i], null,
				(leaveArgs == null || leaveArgs.length <= i) ? null : leaveArgs[i], false, display, focusEvent, modifiers, false)))
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
		return fireRightclickCommand(saveData, display, modifiers, null, mouseLocation, null);
	}

	public Object fireRightclickCommand(boolean saveData, Object display, int modifiers, String formName, Point mouseLocation, Point absoluteMouseLocation)
	{
		return fireEventCommand(EventType.rightClick, rightClickCommand, null, rightClickArgs, saveData, display, false, modifiers, formName, false,
			mouseLocation, absoluteMouseLocation);
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
		return fireEventCommand(type, cmd, args, persistArgs, saveData, display, focusEvent, modifiers, formName, executeWhenFieldValidationFailed,
			mouseLocation, null);
	}

	public Object fireEventCommand(EventType type, String cmd, Object[] args, Object[] persistArgs, boolean saveData, Object display, boolean focusEvent,
		int modifiers, String formName, boolean executeWhenFieldValidationFailed, Point mouseLocation, Point absoluteMouseLocation)
	{
		if (actionListener == null) return null;
		FormController fc = actionListener.getFormController();
		if (fc == null) return null; // won't be able to execute - form is already destroyed

		// also fire when cmd is null (may trigger field validation)
		if (modifiers != MODIFIERS_UNSPECIFIED) actionListener.setLastKeyModifiers(modifiers);
		String name = getElementName(display);
		String fName = formName;
		if (fName == null) fName = getFormName(display);
		if (fName == null) fName = this.formName;
		if (fName == null) fName = getFormName();

		// TODO can't this be the only one used and formName/display args as well as formName member removed?
		if (fName == null) fName = fc.getName();

		if (this.formName == null && fName != null) setFormName(fName);

		Object source = getSource(display); // TODO can't an abstract Object getComponent() be created for this and display be removed as arg ? all subclasses have such a reference

		JSEvent event = new JSEvent();
		event.setType(type);
		event.setSource(source);
		event.setFormName(fName);
		event.setElementName(name);
		event.setModifiers(modifiers == MODIFIERS_UNSPECIFIED ? 0 : modifiers);
		if (mouseLocation != null) event.setLocation(mouseLocation);
		if (absoluteMouseLocation != null) event.setAbsoluteLocation(absoluteMouseLocation);
		return actionListener.executeFunction(cmd, Utils.arrayMerge(Utils.arrayJoin(args, new Object[] { event }), persistArgs), saveData, source, focusEvent,
			null, executeWhenFieldValidationFailed);
	}

	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	protected abstract String getFormName();

	protected abstract String getFormName(Object display);

	protected String getFormNameInternal()
	{
		return formName;
	}

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

	private String getFormElementChangeCommand()
	{
		if (actionListener != null)
		{
			FormController fc = actionListener.getFormController();
			if (fc != null && fc.getForm().getOnElementDataChangeMethodID() > 0)
			{
				return Integer.toString(fc.getForm().getOnElementDataChangeMethodID());
			}

		}
		return null;
	}
}
