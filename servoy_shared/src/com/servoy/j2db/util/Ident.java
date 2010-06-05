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
package com.servoy.j2db.util;


/**
 * @author jblok
 */
public class Ident
{
	public final static String[] keywords = new String[] { //Java or JS Related
	"abstract", //$NON-NLS-1$
	"double", //$NON-NLS-1$
	"int", //$NON-NLS-1$
	"strictfp", //$NON-NLS-1$
	"boolean", //$NON-NLS-1$
	"else", //$NON-NLS-1$
	"interface", //$NON-NLS-1$
	"super", //$NON-NLS-1$
	"break", //$NON-NLS-1$
	"extends", //$NON-NLS-1$
	"long", //$NON-NLS-1$
	"switch", //$NON-NLS-1$
	"byte", //$NON-NLS-1$
	"final", //$NON-NLS-1$
	"native", //$NON-NLS-1$
	"synchronized", //$NON-NLS-1$
	"case", //$NON-NLS-1$
	"finally", //$NON-NLS-1$
	"new", //$NON-NLS-1$
	"this", //$NON-NLS-1$
	"catch", //$NON-NLS-1$
	"float", //$NON-NLS-1$
	"package", //$NON-NLS-1$
	"throw", //$NON-NLS-1$
	"char", //$NON-NLS-1$
	"for", //$NON-NLS-1$
	"private", //$NON-NLS-1$
	"throws", //$NON-NLS-1$
	"class", //$NON-NLS-1$
	"goto", //$NON-NLS-1$
	"protected", //$NON-NLS-1$
	"transient", //$NON-NLS-1$
	"const", //$NON-NLS-1$
	"if", //$NON-NLS-1$
	"public", //$NON-NLS-1$
	"try", //$NON-NLS-1$
	"continue", //$NON-NLS-1$
	"implements", //$NON-NLS-1$
	"return", //$NON-NLS-1$
	"void", //$NON-NLS-1$
	"default", //$NON-NLS-1$
	"import", //$NON-NLS-1$
	"short", //$NON-NLS-1$
	"volatile", //$NON-NLS-1$
	"do", //$NON-NLS-1$
	"instanceof", //$NON-NLS-1$
	"static", //$NON-NLS-1$
	"while", //$NON-NLS-1$

	//JS Related
	"null", //$NON-NLS-1$
	"export", //$NON-NLS-1$
	"undefined", //$NON-NLS-1$
	"constant", //$NON-NLS-1$
	"function", //$NON-NLS-1$
	"debugger", //$NON-NLS-1$
	"in", //$NON-NLS-1$
	"typeof", //$NON-NLS-1$
	"native", //$NON-NLS-1$
	"var", //$NON-NLS-1$
	"enum", //$NON-NLS-1$
	"export", //$NON-NLS-1$
	"with", //$NON-NLS-1$
	"delete", //$NON-NLS-1$
	"date",//to prevent Date //$NON-NLS-1$
	"array",//to prevent Array //$NON-NLS-1$
	"arguments",//to prevent an dataprovider with the name arguments //$NON-NLS-1$

	//Standard j2db DOM things
	"databaseManager", //$NON-NLS-1$
	"application", //$NON-NLS-1$
	"currentform", //$NON-NLS-1$
	"currentcontroller", //$NON-NLS-1$
	"currentRecordIndex", //$NON-NLS-1$
	"history", //$NON-NLS-1$
	"math", //$NON-NLS-1$
	"form", //$NON-NLS-1$
	"controller", //$NON-NLS-1$
	"elements", //$NON-NLS-1$
	"length", //$NON-NLS-1$
	"globals", //$NON-NLS-1$
	"plugins", //$NON-NLS-1$
	"forms", //$NON-NLS-1$
	"foundset", //$NON-NLS-1$
	"model", //$NON-NLS-1$
	"utils", //$NON-NLS-1$
	"security", //$NON-NLS-1$
	"recordIndex", //$NON-NLS-1$
	"allnames", //$NON-NLS-1$
	"allmethods", //$NON-NLS-1$
	"allrelations", //$NON-NLS-1$
	"allvariables", //$NON-NLS-1$
	"exception", //$NON-NLS-1$
	"jsunit", //$NON-NLS-1$
	// New
	"_super" }; //$NON-NLS-1$

	public static boolean checkIfKeyword(String name)
	{
		if (name == null) return false;
		String lname = name.trim().toLowerCase();
		for (String element : keywords)
		{
			if (element.equals(lname))
			{
				return true;
			}
		}
		return false;
	}
}
