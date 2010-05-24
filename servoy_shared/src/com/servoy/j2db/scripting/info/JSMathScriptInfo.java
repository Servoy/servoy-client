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
package com.servoy.j2db.scripting.info;


public class JSMathScriptInfo
{
	public JSMathScriptInfo()
	{
	}

	public Class[] getAllReturnedTypes()
	{
		return null;
	}

	public String[] getParameterNames(String methodName)
	{

		if ("parseInt".equals(methodName))
		{
			return new String[] { "number", "[base]" };
		}
		else if ("parseFloat".equals(methodName))
		{
			return new String[] { "number", "[base]" };
		}
		else if ("isNaN".equals(methodName))
		{
			return new String[] { "string" };
		}
		else if ("toFixed".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("typeof".equals(methodName))
		{
			return new String[] { "string" };
		}
		else if ("abs".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("floor".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("acos".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("asin".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("atan".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("atan2".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("ceil".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("cos".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("exp".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("log".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("max".equals(methodName))
		{
			return new String[] { "number1", "[number2]" };
		}
		else if ("min".equals(methodName))
		{
			return new String[] { "number1", "[number2]" };
		}
		else if ("pow".equals(methodName))
		{
			return new String[] { "number1", "pow_number" };
		}
		else if ("random".equals(methodName))
		{
			return new String[] { };
		}
		else if ("round".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("sin".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("sqrt".equals(methodName))
		{
			return new String[] { "number" };
		}
		else if ("tan".equals(methodName))
		{
			return new String[] { "number" };
		}

		return new String[] { };
	}

	public String getSample(String methodName)
	{
		// TODO Auto-generated method stub
		return "";
	}

	public String getToolTip(String methodName)
	{

		if ("parseInt".equals(methodName))
		{
			return "Makes a integer from the starting numbers in a given string in the base specified";
		}
		else if ("parseFloat".equals(methodName))
		{
			return "Makes a floating point number from the starting numbers in a given string";
		}
		else if ("isNaN".equals(methodName))
		{
			return "The NaN property indicates that a value is 'Not a Number'.";
		}
		else if ("toFixed".equals(methodName))
		{
			return "Fix the number of decimals into a string";
		}
		else if ("typeof".equals(methodName))
		{
			return "Returns the type of the given object, one of these get returned: number, string, boolean, object, function, undefined";
		}
		else if ("abs".equals(methodName))
		{
			return "Returns the absolute value of a number";
		}
		else if ("acos".equals(methodName))
		{
			return "Returns the arccosine (in radians) of a number";
		}
		else if ("asin".equals(methodName))
		{
			return "Returns the arcsine (in radians) of a number";
		}
		else if ("atan".equals(methodName))
		{
			return "Returns the arctangent (in radians) of a number";
		}
		else if ("atan2".equals(methodName))
		{
			return "Returns the arctangent of the quotient of its arguments";
		}
		else if ("ceil".equals(methodName))
		{
			return "Returns the smallest integer greater than or equal to a number";
		}
		else if ("cos".equals(methodName))
		{
			return "Returns the cosine of a number";
		}
		else if ("exp".equals(methodName))
		{
			return "Returns Enumber, where number is the argument, and E is Euler's constant, the base of the natural logarithms";
		}
		else if ("log".equals(methodName))
		{

		}
		else if ("max".equals(methodName))
		{
			return "Returns the greater of two (or more) numbers";
		}
		else if ("min".equals(methodName))
		{
			return "Returns the lesser of two (or more) numbers";
		}
		else if ("pow".equals(methodName))
		{
			return "Returns base to the exponent power, that is, base exponent";
		}
		else if ("random".equals(methodName))
		{
			return "Returns a pseudo-random number between 0 and 1";
		}
		else if ("round".equals(methodName))
		{
			return "Returns the value of a number rounded to the nearest integer";
		}
		else if ("sin".equals(methodName))
		{
			return "Returns the sine of a number";
		}
		else if ("sqrt".equals(methodName))
		{
			return "Returns the square root of a number";
		}
		else if ("tan".equals(methodName))
		{
			return "Returns the tangent of a number";
		}

		return null;
	}

	public boolean isDeprecated(String methodName)
	{
		return false;
	}


}
