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

package com.servoy.j2db.server.ngclient.component.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.servoy.j2db.persistence.ContentSpec.Element;

/**
 * @author obuligan
 *
 */
public class SpecTemplateModel
{
	private final String name;
	private final String displayName;
	private final Class< ? > apiInterface;
	private final String[] libraries;
	private final List<ApiMethod> apis = new ArrayList<>();
	private final int repositoryType;
	private List<Element> handlers;
	private List<Element> model;

	SpecTemplateModel(String name, String displayName, int repositoryType, Class< ? > apiInterface, String[] libaries)
	{
		this.name = name;
		this.displayName = displayName;
		this.apiInterface = apiInterface;
		this.repositoryType = repositoryType;
		this.libraries = libaries;
	}

	public String getName()
	{
		return name;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getRepositoryType()
	{
		return repositoryType;
	}

	public Class< ? > getApiInterface()
	{
		return apiInterface;
	}

	public List<Element> getModel()
	{
		return model;
	}

	public List<Element> getHandlers()
	{
		return handlers;
	}

	public void setModel(List<Element> model)
	{
		this.model = model;
	}

	public void setHandlers(List<Element> handlers)
	{
		this.handlers = handlers;
	}

	public List<ApiMethod> getApis()
	{
		return apis;
	}

	/**
	 * @return the libaries
	 */
	public String[] getLibraries()
	{
		return libraries;
	}

	public String getPropType(Element element)
	{
		String type = SpecGenerator.getSpecTypeFromRepoType(element);
		if (type.startsWith("{") || type.startsWith("["))
		{
			return type;
		}
		else
		{
			return "'" + type + "'";
		}
	}

	public void sortByName()
	{
		Collections.sort(apis, new Comparator<ApiMethod>()
		{
			@Override
			public int compare(ApiMethod o1, ApiMethod o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});

		Comparator<Element> elementComparator = new Comparator<Element>()
		{
			public int compare(Element o1, Element o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		};

		Collections.sort(model, elementComparator);
		Collections.sort(handlers, elementComparator);
	}
	
	public String getTypes()
	{
		if (name.equals("tabpanel") || name.equals("accordionpanel") || name.equals("splitpane")) 
		{
			return",\r\n" + 
				"types: {\r\n" + 
				"  tab: {\r\n" + 
				"  	model: {\r\n" + 
				"  		name: 'string',\r\n" + 
				"  		containsFormId: 'form',\r\n" + 
				"  		text: 'tagstring',\r\n" + 
				"  		relationName: 'relation',\r\n" + 
				"  		active: 'boolean',\r\n" + 
				"  		foreground: Color\r\n" + 
				"  	}\r\n" + 
				"  }\r\n" + 
				"}";
		}
		return null;
	}
}
