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
package com.servoy.j2db.util.keyword;

import com.servoy.j2db.util.Utils;


/**
 * @author jblok
 */
@SuppressWarnings("nls")
public class Ident
{
	public final static String[] keywords = new String[] { //Java or JS Related
	"abstract", //  
	"double", //  
	"int", //  
	"strictfp", //  
	"boolean", //  
	"else", //  
	"interface", //  
	"super", //  
	"break", //  
	"extends", //  
	"long", //  
	"switch", //  
	"byte", //  
	"final", //  
	"native", //  
	"synchronized", //  
	"case", //  
	"finally", //  
	"new", //  
	"this", //  
	"catch", //  
	"float", //  
	"package", //  
	"throw", //  
	"char", //  
	"for", //  
	"private", //  
	"throws", //  
	"class", //  
	"goto", //  
	"protected", //  
	"transient", //  
	"const", //  
	"if", //  
	"public", //  
	"try", //  
	"continue", //  
	"implements", //  
	"return", //  
	"void", //  
	"default", //  
	"import", //  
	"short", //  
	"volatile", //  
	"do", //  
	"instanceof", //  
	"static", //  
	"while", //  

		//JS Related
	"null", //  
	"export", //  
	"undefined", //  
	"constant", //  
	"function", //  
	"debugger", //  
	"in", //  
	"typeof", //  
	"native", //  
	"var", //  
	"enum", //  
	"export", //  
	"with", //  
	"delete", //  
	"date", // to prevent Date 
	"array", // to prevent Array 
	"arguments", // to prevent an dataprovider with the name arguments 

		//Standard j2db DOM things
	"databaseManager", //  
	"application", //  
	"currentform", //  
	"currentcontroller", //  
	"currentRecordIndex", //  
	"history", //  
	"math", //  
	"form", //  
	"controller", //  
	"elements", //  
	"length", //  
	"globals", //  
	"scopes", //  
	"plugins", //  
	"forms", //  
	"foundset", //  
	"model", //  
	"utils", //  
	"security", // 
	"solutionModel", // 
	"recordIndex", //  
	"allnames", //  
	"allmethods", //  
	"allrelations", //  
	"allvariables", //  
	"exception", //  
	"jsunit", //  
	"servoyDeveloper", //  
	// New
	"_super" };

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

	public static String generateNormalizedName(String plainSQLName)
	{
		if (plainSQLName == null) return null;

		String name = Utils.toEnglishLocaleLowerCase(plainSQLName.trim());//to lower case because the not all databases support camelcasing and jdbc drivers comeback with all to upper or lower
		char[] chars = name.toCharArray();
		boolean replaced = false;
		for (int i = 0; i < chars.length; i++)
		{
			switch (chars[i])
			{
				// not allowed in windows
				case '/' :
				case '\\' :
				case '?' :
				case '%' :
				case '*' :
				case ':' :
				case '|' :
				case '"' :
				case '<' :
				case '>' :
					// not allowed in scripting
				case ' ' :
				case '-' :
					chars[i] = '_';
					replaced = true;
					break;
			}
		}

		return replaced ? new String(chars) : name;
	}

	public static final String RESERVED_NAME_PREFIX = "_"; //$NON-NLS-1$

	public static String generateNormalizedNonReservedName(String plainSQLName)
	{
		String name = generateNormalizedName(plainSQLName);
		if (checkIfKeyword(name))
		{
			name = RESERVED_NAME_PREFIX + name;
		}
		return name;
	}
}
