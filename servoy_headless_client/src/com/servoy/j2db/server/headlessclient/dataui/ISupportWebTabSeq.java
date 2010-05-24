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
package com.servoy.j2db.server.headlessclient.dataui;

import com.servoy.j2db.persistence.ISupportTabSeq;


/**
 * Indexes used in this class do not conflict with indexes used in class {@link ISupportTabSeq} like {@link ISupportTabSeq#SKIP} because those indexes have a different meaning.<BR>
 * These indexes are put directly as attributes in HTML tags, except for {@link #DEFAULT} that stands for not adding index info at all.
 */
public interface ISupportWebTabSeq
{

	/**
	 * This tab index marks the entity as removed from tab sequence (it will be skipped when using tab).
	 */
	public static final int SKIP = -1;

	/**
	 * This tab specifies that default tab sequence should be used for this entity.
	 */
	public static final int DEFAULT = -2;

	public void setTabIndex(int tabIndex);

}