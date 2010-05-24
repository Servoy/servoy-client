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
package com.servoy.j2db;

import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.ui.IDataRenderer;

/**
 * @author jcompagner
 * 
 */
public interface IDataRendererFactory<T>
{

	/**
	 * @param application
	 * @param form
	 * @param scriptExecuter
	 * @param part_panels
	 * @param i
	 * @param b
	 * @param undoManager
	 */
	Map completeRenderers(IApplication application, Form form, IScriptExecuter scriptExecuter, Map part_panels, int width, boolean printing,
		ControllerUndoManager undoManager, TabSequence<T> tabSequence) throws Exception;

	/**
	 * @param name TODO
	 * @param application
	 * @return
	 */
	IDataRenderer getEmptyDataRenderer(String Id, String name, IApplication application, boolean showSelection);

	/**
	 * @param app
	 * @param meta
	 * @param form
	 * @param el
	 * @param printing
	 * @param object
	 * @return
	 */
	IDataRenderer createPortalRenderer(IApplication app, Portal meta, Form form, IScriptExecuter el, boolean printing, ControllerUndoManager undoManager)
		throws Exception;

	void extendTabSequence(List<T> tabSequence, IFormUIInternal containerImpl);

	void applyTabSequence(List<T> tabSequence, IFormUIInternal containerImpl);

	void reapplyTabSequence(IFormUIInternal containerImpl, int newBaseTabSequenceIndex);
}
