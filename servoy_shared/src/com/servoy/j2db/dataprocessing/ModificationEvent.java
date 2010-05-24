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
package com.servoy.j2db.dataprocessing;


import java.util.EventObject;

/**
 * This class is used to inform about modifications
 * 
 * @author jblok
 * @since Servoy 5.0
 */
public class ModificationEvent extends EventObject
{
	private final String name;
	private final Object value;

	public ModificationEvent(String name, Object value, Object source)
	{
		super(source != null ? source : name);

		this.name = name;
		this.value = value;
	}

	/**
	 * Get the dataprovider name
	 * 
	 * @return the dataprovider
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Get the changed value
	 * 
	 * @return the value
	 */
	public Object getValue()
	{
		return value;
	}

	void setSource(Object source)
	{
		this.source = source;
	}

	/**
	 * Get the record this notification is for and value is from
	 * 
	 * @return the record
	 */
	public IRecord getRecord()
	{
		return source instanceof IRecord ? (IRecord)source : null;
	}

	void setRecord(IRecord record)
	{
		setSource(record);
	}

	@Override
	public String toString()
	{
		return "ModificationEvent [" + name + "," + value + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
