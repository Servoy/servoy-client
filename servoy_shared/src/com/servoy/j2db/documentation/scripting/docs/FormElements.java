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
package com.servoy.j2db.documentation.scripting.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;

/**
 * Helper class for easier documentation of our JavaScript API for forms.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "elements", scriptingName = "elements")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class FormElements
{
	/**
	 * Get the names of all elements of the form, as an array.
	 * 
	 * @sample
	 * for (var i=0; i<%%prefix%%elements.allnames.length; i++)
	 * {
	 * 	var name = %%prefix%%elements.allnames[i];
	 * 	var elem = %%prefix%%elements[name];
	 * 	application.output(name + ": " + elem.getDataProviderID());	
	 * }
	 * 
	 * @special
	 */
	@JSReadonlyProperty
	public String[] allnames()
	{
		return null;
	}

	/**
	 * Get the number of elements of the form.
	 * 
	 * @sample
	 * for (var i=0; i<%%prefix%%elements.length; i++)
	 * {
	 * 	var elem = %%prefix%%elements[i];
	 * 	application.output(elem.getName() + ": " + elem.getDataProviderID());
	 * }
	 */
	@JSReadonlyProperty
	public Number length()
	{
		return null;
	}

	/**
	 * Get an element of the form by its name.
	 * 
	 * @sampleas allnames()
	 */
	@JSReadonlyProperty
	public IRuntimeComponent array__indexedby_name()
	{
		return null;
	}

	/**
	 * Get an element of the form by its index.
	 * 
	 * @sampleas length()
	 */
	@JSReadonlyProperty
	public IRuntimeComponent array__indexedby_index()
	{
		return null;
	}

}
