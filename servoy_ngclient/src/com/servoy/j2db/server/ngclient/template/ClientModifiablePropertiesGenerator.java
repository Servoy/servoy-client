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

package com.servoy.j2db.server.ngclient.template;

import java.io.PrintWriter;
import java.util.Collection;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;

/**
 * In order to improve performance, not all properties on all components/services are watched for client/browser changes in angular.
 * Depending on the spec files, just some of them need to be watched (for sending changes back to server) - so watch count is greatly reduced clientside.
 *
 * This class generates a list of properties to be watched for each component/service type.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ClientModifiablePropertiesGenerator
{

	public final static String TWO_WAY_BINDINGS_LIST = "propsToWatch";
	public final static String TWO_WAY = "twoWay";
	public final static String DEEP_WATCH = "deep";
	public final static String SHALLOW_WATCH = "shallow";

	private final PrintWriter w;

	/**
	 * @param w writer used to write the JSON.
	 */
	public ClientModifiablePropertiesGenerator(PrintWriter w)
	{
		this.w = w;
		w.print("angular.module('");
		w.print(ClientModifiablePropertiesGenerator.TWO_WAY_BINDINGS_LIST);
		w.println("',['sabloApp']).run(function ($propertyWatchesRegistry) {");
		w.println("  $propertyWatchesRegistry.clearAutoWatchPropertiesList();");
	}

	/**
	 * Writes the given set of watched properties to the JSON, under the given category name.
	 * @param webComponentSpecifications specifications to look in for watched properties
	 * @param categoryName for example "components", "services"
	 */
	public void appendAll(WebComponentSpecification[] webComponentSpecifications, String categoryName)
	{
		w.print("  $propertyWatchesRegistry.setAutoWatchPropertiesList('");
		w.print(categoryName);
		w.println("', {");
		boolean first1 = true;
		for (WebComponentSpecification webComponentSpec : webComponentSpecifications)
		{
			Collection<PropertyDescription> twoWayProps = webComponentSpec.getTaggedProperties(TWO_WAY);
			if (twoWayProps.size() > 0)
			{
				if (!first1) w.println(',');
				first1 = false;
				w.print("    '");
				w.print(getFullWebComponentType(webComponentSpec));
				w.println("': {");
				boolean first2 = true;
				for (PropertyDescription shallowWatched : twoWayProps)
				{
					if (!first2) w.println(',');
					first2 = false;
					w.print("      '");
					w.print(shallowWatched.getName());
					w.print("': ");
					w.print(DEEP_WATCH.equals(shallowWatched.getTag(TWO_WAY)));
				}
				if (!first2) w.println("");
				w.print("    }");
			}
		}
		if (!first1) w.println("");
		w.println("  });");
	}

	public String getFullWebComponentType(WebComponentSpecification webComponentSpec)
	{
//		return webComponentSpec.getPackageName() + " -> " + webComponentSpec.getName(); // if you use this instead, client side must know packagename as well when looking up watches
		return webComponentSpec.getName(); // use just name for now as if two packages define components with the exact same name that won't work correctly anyway
	}

	public void done()
	{
		w.print("});");
	}

}
