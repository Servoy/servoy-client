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
package com.servoy.base.query;


/**
 * Common interface for conditions in queries.
 * 
 * @author rgansevles
 * 
 */
public interface IBaseSQLCondition extends IBaseQueryElement
{
	public static final int EQUALS_OPERATOR = 0; //=
	public static final int GT_OPERATOR = 1; //>
	public static final int LT_OPERATOR = 2; //<
	public static final int GTE_OPERATOR = 3; //>=
	public static final int LTE_OPERATOR = 4; //<=
	public static final int NOT_OPERATOR = 5; //!= equals <>
	public static final int IN_OPERATOR = 6; //IN
	public static final int LIKE_OPERATOR = 7; //LIKE
	public static final int NOT_IN_OPERATOR = 8; //NOT IN
	public static final int NOT_LIKE_OPERATOR = 9; //NOT LIKE
	public static final int BETWEEN_OPERATOR = 10; //BETWEEN
	public static final int NOT_BETWEEN_OPERATOR = 11; //NOT BETWEEN
	public static final int ISNULL_OPERATOR = 12; //ISNULL
	public static final int ISNOTNULL_OPERATOR = 13; //ISNOTNULL

	public static final int OPERATOR_MASK = 0xff; // use LSB for operators, rest for modifiers

	public static final int[] ALL_DEFINED_OPERATORS = new int[] { //
	EQUALS_OPERATOR,//
	GT_OPERATOR,//
	LT_OPERATOR,//
	GTE_OPERATOR,//
	LTE_OPERATOR,//
	NOT_OPERATOR,//
	IN_OPERATOR,//
	LIKE_OPERATOR,//
	NOT_IN_OPERATOR,//
	NOT_LIKE_OPERATOR,//
	BETWEEN_OPERATOR,//
	NOT_BETWEEN_OPERATOR,//
	ISNULL_OPERATOR,//
	ISNOTNULL_OPERATOR //
	};//


	public static final String[] OPERATOR_STRINGS = new String[] {//
	"=", //$NON-NLS-1$
	">", //$NON-NLS-1$
	"<", //$NON-NLS-1$
	">=", //$NON-NLS-1$
	"<=", //$NON-NLS-1$
	"!=", //$NON-NLS-1$
	"in", //$NON-NLS-1$
	"like", //$NON-NLS-1$
	"not in", //$NON-NLS-1$
	"not like", //$NON-NLS-1$,
	"between", //$NON-NLS-1$,
	"not between", //$NON-NLS-1$,
	"is null", //$NON-NLS-1$,
	"is not null" //$NON-NLS-1$
	};

	public static final int[] OPERATOR_NEGATED = new int[] {//
	NOT_OPERATOR,//
	LTE_OPERATOR, GTE_OPERATOR,//
	LT_OPERATOR, GT_OPERATOR,//
	EQUALS_OPERATOR,//
	NOT_IN_OPERATOR,//
	NOT_LIKE_OPERATOR,//
	IN_OPERATOR, //
	LIKE_OPERATOR,//
	NOT_BETWEEN_OPERATOR,//
	BETWEEN_OPERATOR, //
	ISNOTNULL_OPERATOR, //
	ISNULL_OPERATOR //
	};

	// modifiers included in the operator
	public static final int ORNULL_MODIFIER = 0x100;
	public static final int CASEINSENTITIVE_MODIFIER = 0x200;

	public static final int[] ALL_MODIFIERS = new int[] {//
	ORNULL_MODIFIER,//
	CASEINSENTITIVE_MODIFIER //
	};
	public static final String[] MODIFIER_STRINGS = new String[] {//
	"^||", // ORNULL_MODIFIER  //$NON-NLS-1$
	"#" // CASEINSENTITIVE_MODIFIER //$NON-NLS-1$
	};


	IBaseSQLCondition negate();
}
