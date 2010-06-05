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
package com.servoy.j2db.persistence;


/**
 * Tab index can be {@link SKIP}, {@link DEFAULT} or any value > 0. Other values for tab index are invalid.
 * @author jcompagner
 */
public interface ISupportTabSeq
{

	/**
	 * This tab index marks the entity as removed from tab sequence (it will be skipped when tabbing).
	 */
	public static final int SKIP = -2;

	/**
	 * This tab specifies that default tab sequence should be used for this entity.
	 */
	public static final int DEFAULT = 0;

	/**
	 * An index that specifies the position of the component in the tab sequence. The components 
	 * are put into the tab sequence in increasing order of this property. A value of 0 means
	 * to use the default mechanism of building the tab sequence (based on their location on the form).
	 * A value of -2 means to remove the component from the tab sequence.
	 */
	public abstract int getTabSeq();

	public abstract void setTabSeq(int i);
}