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


import java.beans.PropertyChangeEvent;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class SpecialUndoManager extends UndoManager
{
	protected boolean _undo;
	protected boolean _redo;
	protected CmdManager _cmdManager;

	public SpecialUndoManager(CmdManager cmdManager)
	{
		_undo = false;
		_redo = false;
		_cmdManager = cmdManager;
	}

	/*
	 * @see UndoableEdit#undo()
	 */
	@Override
	public void undo() throws CannotUndoException
	{
		super.undo();
		boolean undo = canUndo();
		boolean redo = canRedo();
		if (undo != _undo)
		{
			_undo = undo;
			_cmdManager.propertyChange(new PropertyChangeEvent(this, "undomanager", "cmdundo", new Boolean(_undo))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (redo != _redo)
		{
			_redo = redo;
			_cmdManager.propertyChange(new PropertyChangeEvent(this, "undomanager", "cmdredo", new Boolean(_redo))); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	/*
	 * @see UndoableEdit#redo()
	 */
	@Override
	public void redo() throws CannotRedoException
	{
		super.redo();
		boolean undo = canUndo();
		boolean redo = canRedo();
		if (undo != _undo)
		{
			_undo = undo;
			_cmdManager.propertyChange(new PropertyChangeEvent(this, "undomanager", "cmdundo", new Boolean(_undo))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (redo != _redo)
		{
			_redo = redo;
			_cmdManager.propertyChange(new PropertyChangeEvent(this, "undomanager", "cmdredo", new Boolean(_redo))); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * @see UndoableEdit#addEdit(UndoableEdit)
	 */
	@Override
	public boolean addEdit(UndoableEdit anEdit)
	{
		boolean test = super.addEdit(anEdit);
		boolean undo = canUndo();
		boolean redo = canRedo();
		if (undo != _undo)
		{
			_undo = undo;
			_cmdManager.propertyChange(new PropertyChangeEvent(this, "undomanager", "cmdundo", new Boolean(_undo))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (redo != _redo)
		{
			_redo = redo;
			_cmdManager.propertyChange(new PropertyChangeEvent(this, "undomanager", "cmdredo", new Boolean(_redo))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return test;
	}

}
