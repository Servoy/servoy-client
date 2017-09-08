/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.util.Debug;

/**
 * Generates HTML for a flow layout form
 * @author jblok, lvostinar
 */
@SuppressWarnings("nls")
public class DesignFormLayoutStructureGenerator
{
	public static void generateLayout(Form form, FlattenedSolution fs, PrintWriter writer)
	{
		try
		{
			Iterator<IPersist> components = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			while (components.hasNext())
			{
				IPersist component = components.next();
				if (component instanceof LayoutContainer)
				{
					generateLayoutContainer((LayoutContainer)component, form, fs, writer, 1);
				}
				else if (component instanceof IFormElement)
				{
					generateFormElement(writer, (IFormElement)component, form, fs);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public static void generateLayoutContainer(LayoutContainer container, Form form, FlattenedSolution fs, PrintWriter writer, int nested)
	{
		WebLayoutSpecification spec = null;
		if (container.getPackageName() != null)
		{
			PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				container.getPackageName());
			if (pkg != null)
			{
				spec = pkg.getSpecification(container.getSpecName());
			}
		}
		writer.print("<");
		writer.print(container.getTagType());
		if (container.getName() != null)
		{
			writer.print(" svy-name='");
			writer.print(container.getName());
			writer.print("' ");
		}
		if (container.getElementId() != null)
		{
			writer.print(" id='");
			writer.print(container.getElementId());
			writer.print("' ");
		}

		Map<String, String> attributes = new HashMap<String, String>(container.getAttributes());
		if (spec != null)
		{
			for (String propertyName : spec.getAllPropertiesNames())
			{
				PropertyDescription pd = spec.getProperty(propertyName);
				if (pd.getDefaultValue() != null && !attributes.containsKey(propertyName))
				{
					attributes.put(propertyName, pd.getDefaultValue().toString());
				}
			}
		}
		for (Entry<String, String> entry : attributes.entrySet())
		{
			writer.print(" ");
			try
			{
				StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(entry.getKey(), writer);
				if (entry.getValue() != null && entry.getValue().length() > 0)
				{
					writer.print("=\"");
					StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(entry.getValue(), writer);
					writer.print("\"");
				}
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
		writer.print(" svy-id='");
		writer.print(container.getID());
		writer.print("'");

		writer.print(">\n");


		Iterator<IPersist> components = container.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (components.hasNext())
		{
			int count = 0;
			while (count++ < nested)
			{
				writer.print('\t');
			}
			IPersist component = components.next();
			if (component instanceof LayoutContainer)
			{
				generateLayoutContainer((LayoutContainer)component, form, fs, writer, nested + 1);
			}
			else if (component instanceof IFormElement)
			{
				generateFormElement(writer, (IFormElement)component, form, fs);
			}
		}
		int count = 1;
		while (count++ < nested)
		{
			writer.print('\t');
		}
		writer.print("</");
		writer.print(container.getTagType());
		writer.print(">\n");
	}

	public static void generateFormElement(PrintWriter writer, IFormElement formElement, Form form, FlattenedSolution fs)
	{
		String tagName = FormTemplateGenerator.getComponentTypeName(formElement);
		String name = formElement.getName();
		writer.print("<");
		writer.print(tagName);
		writer.print(" name='");
		writer.print(name);
		writer.print("'");

		FormElement fe = new FormElement(formElement, fs, new PropertyPath(), false);

		JSONObject json = new JSONObject(fe.getPropertiesString());
		for (String key : json.keySet())
		{
			// for now skip these 2, the size is weird that it has that "Dimension" in it
			if (key.equals("size") || key.equals("visible")) continue;
			writer.write(' ');
			writer.write(key);
			writer.write("='");
			writer.write(json.opt(key).toString());
			writer.write("'");
		}


		writer.print(" svy-id='");
		writer.print(formElement.getID());
		writer.print("'");

		writer.print("/>\n");
	}
}
