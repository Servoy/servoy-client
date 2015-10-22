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
package com.servoy.j2db.smart.cmd;


import java.awt.event.ActionEvent;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.cmd.ICmd;
import com.servoy.j2db.util.IProvideButtonModel;
import com.servoy.j2db.util.gui.FixedDefaultButtonModel;

/** 
 *  Abstract class for all commands. The cmdmanager serves as a
 *  command shell for executing actions in much the same way that a
 *  DOS or UNIX commmand command shell executes programs. Each command
 *  can have a Hashtable of "command-line" arguments and also look at
 *  global variables (its environment). Once an instance of a Cmd is
 *  made, it can  perform that action. <p>
 *
 *  Since this is subclassed from class AbstractAction in the Swing
 *  user interface library, Cmd objects can be easily added to menus
 *  and toolbars. <p>
 */
public abstract class AbstractCmd extends MnemonicCheckAction implements ICmd, IProvideButtonModel
{

	////////////////////////////////////////////////////////////////
	// instance variables

	/** Arguments that configure the Cmd instance. */
	protected Hashtable _args;
	protected ISmartClientApplication application;


	////////////////////////////////////////////////////////////////
	// constructors


	protected void setAccelerator(KeyStroke k)
	{
		putValue(Action.ACCELERATOR_KEY, k);
	}

	protected void setToolTipText(String tip)
	{
		if (tip != null && tip.trim().length() == 0) tip = null;
		putValue(Action.SHORT_DESCRIPTION, tip);
	}

	protected void setActionCommand(String s)
	{
		putValue(Action.ACTION_COMMAND_KEY, s);
	}

	public AbstractCmd(ISmartClientApplication app, String classname, String name, String key)
	{
		this(app, classname, null, name, key);
	}

	public AbstractCmd(ISmartClientApplication app, String classname, String name, String key, char mnemonic)
	{
		this(app, classname, null, name, key);
		putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
	}

	public AbstractCmd(ISmartClientApplication app, String classname, String name, String key, char mnemonic, Icon icon)
	{
		this(app, classname, null, name, key, icon);
		putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
	}

	public AbstractCmd(ISmartClientApplication app, String classname, String name, String key, Icon icon)
	{
		this(app, classname, null, name, key, icon);
	}

	public AbstractCmd(ISmartClientApplication app, String classname, Hashtable args, String name, String key)
	{
		super(name, key);
		application = app;
		if (mustRegister()) application.getCmdManager().registerAction(classname, this);
		_args = args;
		setToolTipText(name);
		setEnabled(false);
	}

	public AbstractCmd(ISmartClientApplication app, String classname, Hashtable args, String name, String key, Icon icon)
	{
		super(name, key, icon);
		application = app;
		if (mustRegister()) application.getCmdManager().registerAction(classname, this);
		_args = args;
		setToolTipText(name);
		setEnabled(false);
	}

	protected boolean mustRegister()
	{
		return true;
	}

	////////////////////////////////////////////////////////////////
	// enabling and disabling

	/** Determine if this Cmd should be shown as grayed out in menus and
	 *  toolbars. */
	public void updateEnabled()
	{
		setEnabled(shouldBeEnabled());
	}

	/** Return true if this action should be available to the user. This
	 *  method should examine the ProjectBrowser that owns it.  Sublass
	 *  implementations of this method should always call
	 *  super.shouldBeEnabled first. */
	public boolean shouldBeEnabled()
	{
		return true;
	}


	////////////////////////////////////////////////////////////////
	// accessors

	/** Return a name for this Cmd suitable for display to the user */
	public String getName()
	{
		return (String)getValue(NAME);
	}

	public void setName(String n)
	{
		putValue(NAME, n);
	}

	/** Get the object stored as an argument under the given name. */
	protected Object getArgument(String key)
	{
		if (_args == null) return null;
		else return _args.get(key);
	}

	/** Get an argument by name.  If it's not defined then use the given
	 *  default. */
	protected Object getArgument(String key, Object defaultValue)
	{
		if (_args == null) return defaultValue;
		Object res = _args.get(key);
		if (res == null) return defaultValue;
		return res;
	}

	/** Store the given argument under the given name. */
	public void setArgument(String key, Object value)
	{
		if (_args == null)
		{
			_args = new Hashtable();
		}
		_args.put(key, value);
	}

	/** Reply true if this Cmd instance has the named argument defined. */
	protected boolean containsArg(String key)
	{
		return _args != null && _args.containsKey(key);
	}

	////////////////////////////////////////////////////////////////
	// Cmd API

	/** Return a URL that has user and programmer documentation.
	 *  <A HREF="../features.html#view_Cmd_documentation">
	 *  <TT>FEATURE: view_Cmd_documentation</TT></A>
	 */
	public String about()
	{
		return "http://www.servoy.com/j2db/docs.jsp?show=" + getClass().getName(); //$NON-NLS-1$
	}

	public void actionPerformed(ActionEvent ae)
	{
		application.getCmdManager().executeCmd(this, ae);
	}

	/** 
	 * Perform whatever Cmd this Cmd is meant to do. Subclasses
	 * should override this to do whatever is intended. When the Cmd
	 * executes, it should store enough information to undo itself later
	 * if needed.
	 * @param ea the event can be null
	 * @return the UndoableEdit (supposly a cmd clone)
	 */
	public abstract UndoableEdit doIt(java.util.EventObject ae);


	private ButtonModel model = null;

	public ButtonModel getModel()
	{
		if (model == null) model = createButtonModel();
		return model;
	}

	protected ButtonModel createButtonModel()
	{
		return new FixedDefaultButtonModel();
	}

}
