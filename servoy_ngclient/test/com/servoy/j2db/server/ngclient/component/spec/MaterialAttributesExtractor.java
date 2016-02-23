/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.server.ngclient.component.spec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * Extract the material layout attributes and default values from the material_layout.css and save them to attributes.json
 * @author emera
 */
public class MaterialAttributesExtractor
{


	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		FileInputStream is = new FileInputStream("test/com/servoy/j2db/server/ngclient/component/spec/material_layout.css");
		String css = IOUtils.toString(is, "UTF-8");
		is.close();

		Map<String, Set<String>> attributes = new HashMap<>();

		Pattern p = Pattern.compile("\\[(.+?)(\\=(.+?))?\\]");
		Matcher matcher = p.matcher(css);
		while (matcher.find())
		{
			Set<String> list = getValues(attributes, matcher.group(1));
			if (matcher.group(3) != null)
			{
				list.addAll(Arrays.asList(matcher.group(3).replaceAll("\"", "").split(" ")));
			}
		}

		ServoyJSONObject obj = new ServoyJSONObject(false, true);
		for (String key : attributes.keySet())
		{
			Set<String> values = attributes.get(key);
			String type = "string";
			if (!values.isEmpty())
			{
				//get the first non-null value
				String v = values.iterator().next();
				if ("0".equals(v) || Utils.getAsInteger(v) != 0) type = "object";
			}

			ServoyJSONObject attribute = new ServoyJSONObject(false, false);
			attribute.put("type", type);
			if (type.equals("string") && values.size() > 0)
			{
				ServoyJSONArray arr = new ServoyJSONArray(values);
				attribute.put("values", arr);
			}
			obj.put(key, attribute);
		}
		ServoyJSONObject o = new ServoyJSONObject(false, true);
		o.put("attributes", obj);
		FileUtils.writeStringToFile(new File("test/com/servoy/j2db/server/ngclient/component/spec/attributes.json"), o.toString());
		System.out.println("Generated file attributes.json");
	}

	/**
	 * @param attributes
	 * @param string
	 * @return
	 */
	private static Set<String> getValues(Map<String, Set<String>> attributes, String string)
	{
		Set<String> list = attributes.get(string);
		if (list == null)
		{
			attributes.put(string, new TreeSet<String>());
			list = attributes.get(string);
		}
		return list;
	}

}
