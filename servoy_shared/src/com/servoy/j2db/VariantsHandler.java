/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.j2db;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.j2db.persistence.Media;

/**
 * @author jcompagner
 * @since 2022.12
 */
@SuppressWarnings("nls")
public class VariantsHandler
{
	private final Map<String, JSONArray> nameToClasses = new HashMap<>();
	private final Map<String, JSONArray> categoryToVariants = new HashMap<>();
	private final FlattenedSolution fs;
	private long lastModified;

	public VariantsHandler(FlattenedSolution fs)
	{
		this.fs = fs;
		loadIfNeeded();
	}

	private void loadIfNeeded()
	{
		Media media = fs.getMedia("variants.json");
		if (media == null && categoryToVariants.size() > 0)
		{
			// variants changed, or module references
			categoryToVariants.clear();
			nameToClasses.clear();
			lastModified = -1;
		}
		else if (media != null && media.getLastModifiedTime() != lastModified)
		{
			categoryToVariants.clear();
			nameToClasses.clear();
			if (media != null)
			{
				lastModified = media.getLastModifiedTime();
				String json = new String(media.getMediaData(), Charset.forName("UTF-8"));
				var jsonObject = new JSONObject(json);
				Iterator<String> keys = jsonObject.keys();
				while (keys.hasNext())
				{
					String name = keys.next();
					var variantObject = jsonObject.getJSONObject(name);
					variantObject.put("name", name);

					nameToClasses.put(name, variantObject.getJSONArray("classes"));
					var category = variantObject.getString("category");

					JSONArray jsonArray = categoryToVariants.get(category);
					if (jsonArray == null)
					{
						jsonArray = new JSONArray();
						categoryToVariants.put(category, jsonArray);
					}
					jsonArray.put(variantObject);
				}
			}
		}
	}


	public JSONArray getVariantsForCategory(String category)
	{
		loadIfNeeded();
		JSONArray jsonArray = categoryToVariants.get(category);
		if (jsonArray == null) jsonArray = new JSONArray();
		return jsonArray;
	}

	public JSONArray getVariantClasses(String variantName)
	{
		loadIfNeeded();
		JSONArray jsonArray = nameToClasses.get(variantName);
		if (jsonArray == null) jsonArray = new JSONArray();
		return jsonArray;

	}
}
