/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.persistence.constants;

/**
 * 
 * Constants useful when dealing with parts.
 * 
 * @author gboros
 */
public interface IPartConstants
{
	public static final int TITLE_HEADER = 1;
	public static final int HEADER = 2;
	public static final int LEADING_GRAND_SUMMARY = 3;//no group by
	public static final int LEADING_SUBSUMMARY = 4;//group by (n times!)
	public static final int BODY = 5;
	public static final int TRAILING_SUBSUMMARY = 6;//group by (n times!)
	public static final int TRAILING_GRAND_SUMMARY = 7;//no group by
	public static final int FOOTER = 8;
	public static final int TITLE_FOOTER = 9;
	public static final int PART_ARRAY_SIZE = 10;
}
