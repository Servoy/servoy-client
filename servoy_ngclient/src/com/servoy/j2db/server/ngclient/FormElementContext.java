/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.FlattenedSolution;

/**
 * @author gboros
 *
 */
public class FormElementContext
{
	private final FormElement formElement;
	private final IServoyDataConverterContext context;
	private final JSONObject object;

	public FormElementContext(FormElement formElement)
	{
		this(formElement, null, null);
	}

	public FormElementContext(FormElement formElement, IServoyDataConverterContext context, JSONObject object)
	{
		this.formElement = formElement;
		this.context = context;
		this.object = object;
	}

	public String getPropertiesString() throws JSONException
	{
		String designString = formElement.propertiesAsTemplateJSON(null, this).toString();
		if (object != null)
		{
			JSONObject designValues = new JSONObject(designString);

			for (String key : object.keySet())
			{
				if ("conversions".equals(key))
				{
					// don't override conversions entirely cause template json might have values that are not in initialData and those might need conversion info
					JSONObject initialDataConversions = object.getJSONObject("conversions");
					JSONObject designConversions = designValues.optJSONObject("conversions");
					if (designConversions == null) designValues.put("conversions", initialDataConversions);
					else
					{
						// merge conversions as well
						for (String conversionKey : initialDataConversions.keySet())
						{
							designConversions.put(conversionKey, initialDataConversions.get(conversionKey));
						}
					}
				}
				else
				{
					// put 'initial' value instead of design value
					designValues.put(key, object.get(key));
				}
			}
			designString = designValues.toString();
		}
		return designString;
	}

	public String getTypeName()
	{
		return formElement.getTypeName();
	}

	public String getName()
	{
		return formElement.getName();
	}

	public Collection<String> getHandlers()
	{
		return formElement.getHandlers();
	}

	public FormElement getFormElement()
	{
		return formElement;
	}

	public IServoyDataConverterContext getContext()
	{
		return context;
	}

	public FlattenedSolution getFlattenedSolution()
	{
		if (context != null && context.getApplication() != null) return context.getApplication().getFlattenedSolution();
		return formElement.getFlattendSolution();
	}
}
