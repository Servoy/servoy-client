/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

/**
 * SQL types added in JDBC 4.0 (java 1.6).
 * 
 * @author rgansevles
 *
 */
public class Types4
{
	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>ROWID</code>
	 * 
	 * @since 1.6
	 *
	 */
	public final static int ROWID = -8;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>NCHAR</code>
	 *
	 * @since 1.6
	 */
	public static final int NCHAR = -15;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>NVARCHAR</code>.
	 *
	 * @since 1.6
	 */
	public static final int NVARCHAR = -9;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>LONGNVARCHAR</code>.
	 *
	 * @since 1.6
	 */
	public static final int LONGNVARCHAR = -16;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>NCLOB</code>.
	 *
	 * @since 1.6
	 */
	public static final int NCLOB = 2011;

	/**
	 * The constant in the Java programming language, sometimes referred to
	 * as a type code, that identifies the generic SQL type <code>XML</code>.
	 *
	 * @since 1.6 
	 */
	public static final int SQLXML = 2009;

}
