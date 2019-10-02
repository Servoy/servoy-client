/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.util;

import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;

/**
 * Interface used by types that want to contribute special behavior for solutionModel usage.
 * Converts between the value that a property has when returned/set via solution model in solution JS core and the design JSON value that it will have in the .frm file (I think).
 *
 * @author lvostinar
 */
public interface IRhinoDesignConverter
{

	/**
	 * Converts from solution model value to .frm design JSON value (I think).
	 *
	 * @param solutionModelScriptingValue the solution model value that is available in solution scripting for this property
	 * @return the design JSON value that it will have in the .frm file.
	 */
	Object fromRhinoToDesignValue(Object solutionModelScriptingValue, PropertyDescription pd, IApplication application, JSWebComponent webComponent);

	/**
	 * Converts from .frm design JSON value to solution model value (I think).
	 *
	 * @param frmJSONValue the .frm design JSON value.
	 * @return what frmJSONValue would be in solution model scripting (in solution JS code).
	 */
	Object fromDesignToRhinoValue(Object frmJSONValue, PropertyDescription pd, IApplication application, JSWebComponent webComponent);

}
