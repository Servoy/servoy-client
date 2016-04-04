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
import java.util.List;

/**
 * The event object for foundset event listeners
 *
 * @author jblok
 * @since Servoy 5.0
 */
public class FoundSetEvent extends EventObject
{
	/**
	 * The foundset's find mode was changed.
	 */
	public static final int FIND_MODE_CHANGE = 10;

	/**
	 * The foundset's content was changed.
	 */
	public static final int CONTENTS_CHANGED = 11;

	/**
	 * A new foundset has been created for the table. Used by {@link IFoundSetListener#newValue(FoundSetEvent)}.
	 */
	public static final int NEW_FOUNDSET = 12;

	/**
	 * A foundset was invalidated.
	 */
	public static final int FOUNDSET_INVALIDATED = 13;

	/**
	 * Change related to multiSelect property.
	 */
	public static final int SELECTION_MODE_CHANGE = 14; // currently only used to broadcast foundset multiselect unpin/lower pinLevel events, but can be extended with change type if needed

	/**
	 * Change type data is inserted when type is CONTENTS_CHANGED.
	 */
	public static final int CHANGE_INSERT = 1;
	/**
	 * Change type data is updated when type is CONTENTS_CHANGED.
	 */
	public static final int CHANGE_UPDATE = 0;
	/**
	 * Change type data is deleted when type is CONTENTS_CHANGED.
	 */
	public static final int CHANGE_DELETE = -1;

	private final int type;
	private final int changeType;
	private final int firstRow;
	private final int lastRow;
	private final List<String> dataProviders;

	public FoundSetEvent(IFoundSet source, int type, int changeType, int firstRow, int lastRow)
	{
		this(source, type, changeType, firstRow, lastRow, null);
	}

	public FoundSetEvent(IFoundSet source, int type, int changeType, int firstRow, int lastRow, List<String> dataProviders)
	{
		super(source);
		this.type = type;
		this.changeType = changeType;
		this.firstRow = firstRow;
		this.lastRow = lastRow;
		this.dataProviders = dataProviders;
	}

	public FoundSetEvent(IFoundSet source, int type, int changeType)
	{
		this(source, type, changeType, -1, -1, null);
	}

	public IFoundSet getSourceFoundset()
	{
		return (IFoundSet)getSource();
	}

	/**
	 * Returns the type of the event. Can be {@link FoundSetEvent#FIND_MODE_CHANGE}, {@link FoundSetEvent#CONTENTS_CHANGED} or
	 * {@link FoundSetEvent#NEW_FOUNDSET}.
	 *
	 * @return the type of the event.
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Returns the change type of the event when type is {@link FoundSetEvent#CONTENTS_CHANGED}. Can be {@link FoundSetEvent#CHANGE_DELETE}, {@link FoundSetEvent#CHANGE_INSERT} or
	 * {@link FoundSetEvent#CHANGE_UPDATE}.
	 *
	 * @return the change type of the event.
	 */
	public int getChangeType()
	{
		return changeType;
	}

	/**
	 * Returns the first row of the change when it was a contents change update (INSERT,DELETE,UPDATE)
	 * @return the firstRow
	 * @since 7.4
	 */
	public int getFirstRow()
	{
		return firstRow;
	}

	/**
	 * Returns the last row of the change when it was a contents change update (INSERT,DELETE,UPDATE)
	 * @return the lastRow
	 * @since 7.4
	 */
	public int getLastRow()
	{
		return lastRow;
	}

	/**
	 * @return the dataProviders
	 */
	public List<String> getDataProviders()
	{
		return dataProviders;
	}

	@Override
	public String toString()
	{
		return "FoundSetEvent[" + getType() + "," + getChangeType() + ",firstRow:" + firstRow + ",lastRow:" + lastRow + "] from source " + source; //$NON-NLS-1$
	}

}
