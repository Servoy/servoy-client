/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.server.ngclient.FormElement;

/**
 * Types can implement this interface to tell the system that the type does or does not support values in the template.
 * Default the system will see this value as true (so all types do support that values are going through the template).
 *
 *
 * @author jcompagner
 *
 */
public interface ISupportTemplateValue<FormElementT>
{
	boolean valueInTemplate(FormElementT object, PropertyDescription pd, FormElement formElement);
}
