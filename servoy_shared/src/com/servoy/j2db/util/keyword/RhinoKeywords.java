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

@SuppressWarnings("nls")
public class RhinoKeywords
{
	public final static String[] keywords = new String[] { // Currently in use by Rhino.
	"break", 
	"case", 
	"catch", 
	"continue", 
	"default", 
	"delete", 
	"do", 
	"else", 
	"finally", 
	"for", 
	"function", 
	"if", 
	"in", 
	"instanceof", 
	"new", 
	"return", 
	"switch", 
	"this", 
	"throw", 
	"try", 
	"typeof", 
	"var", 
	"void", 
	"while", 
	"with",  

	// Reserved by Rhino for future use.
	"abstract", 
	"boolean", 
	"byte", 
	"char", 
	"class", 
	"const", 
	"debugger", 
	"double", 
	"enum", 
	"export", 
	"extends", 
	"final", 
	"float", 
	"goto", 
	"implements", 
	"import", 
	"int", 
	"interface", 
	"long", 
	"native", 
	"package", 
	"private", 
	"protected", 
	"public", 
	"short", 
	"static", 
	"super", 
	"synchronized", 
	"throws", 
	"transient", 
	"volatile"  
	};

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