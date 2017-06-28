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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.util.Utils;

/**
 * @author obuligan
 *
 */
public class SpecTemplateModel
{

	public static final int ADD = 1;
	public static final int REPLACE = 2;
	private static final int VERSION = 1;

	private final String name;
	private final String serverScript;
	private final String displayName;
	private final String icon;
	private final Class< ? > apiInterface;
	private final String[] libraries;
	private final List<ApiMethod> apis = new ArrayList<>();
	private final int repositoryType;
	private List<ApiMethod> handlers;
	private List<Element> model;
	private final int version;

	SpecTemplateModel(String name, String displayName, int version, String icon, int repositoryType, Class< ? > apiInterface, String[] libaries)
	{
		this(name, displayName, version, icon, repositoryType, apiInterface, libaries, null);
	}

	SpecTemplateModel(String name, String displayName, String icon, int repositoryType, Class< ? > apiInterface, String[] libaries)
	{
		this(name, displayName, VERSION, icon, repositoryType, apiInterface, libaries, null);
	}

	SpecTemplateModel(String name, String displayName, String icon, int repositoryType, Class< ? > apiInterface, String[] libaries, String serverScript)
	{
		this(name, displayName, VERSION, icon, repositoryType, apiInterface, libaries, serverScript);
	}

	SpecTemplateModel(String name, String displayName, int version, String icon, int repositoryType, Class< ? > apiInterface, String[] libaries,
		String serverScript)
	{
		this.name = name;
		this.displayName = displayName;
		this.version = version;
		this.icon = icon;
		this.apiInterface = apiInterface;
		this.repositoryType = repositoryType;
		this.libraries = libaries;
		this.serverScript = serverScript;
	}

	SpecTemplateModel(String name, String displayName, String icon, int repositoryType, Class< ? > apiInterface, String[] libaries, String serverScript,
		ApiMethod[] extraApiMethods)
	{
		this(name, displayName, VERSION, icon, repositoryType, apiInterface, libaries, serverScript, extraApiMethods);
	}

	SpecTemplateModel(String name, String displayName, int version, String icon, int repositoryType, Class< ? > apiInterface, String[] libaries,
		String serverScript, ApiMethod[] extraApiMethods)
	{
		this(name, displayName, version, icon, repositoryType, apiInterface, libaries, serverScript);
		apis.addAll(Arrays.asList(extraApiMethods));
	}

	public String getName()
	{
		return name;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getIcon()
	{
		return icon;
	}

	public int getRepositoryType()
	{
		return repositoryType;
	}

	public String getServerScript()
	{
		return serverScript;
	}

	public Class< ? > getApiInterface()
	{
		return apiInterface;
	}

	public List<Element> getModel()
	{
		return model;
	}

	public List<ApiMethod> getHandlers()
	{
		return handlers;
	}

	public void setModel(List<Element> model)
	{
		this.model = model;
	}

	public void setHandlers(List<ApiMethod> handlers)
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

	// called from spec_file.ftl
	public String getPropTypeWithDefault(String compName, Element element)
	{
		String type = SpecGenerator.getSpecTypeFromRepoType(compName, element);
		if (type.startsWith("{") || type.startsWith("[") || type.startsWith("\""))
		{
			return type;
		}
		if (element.getContentID() > 0 && !Utils.equalObjects(ContentSpec.getJavaClassMemberDefaultValue(element.getTypeID()), element.getDefaultClassValue()))
		{
			return "{\"type\":\"" + type + "\", \"default\":" + element.getDefaultTextualClassValue() + "}";
		}
		return "\"" + type + "\"";
	}

	public void sortByName()
	{
		Comparator<ApiMethod> functionComparator = new Comparator<ApiMethod>()
		{
			@Override
			public int compare(ApiMethod o1, ApiMethod o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(apis, functionComparator);

		Comparator<Element> elementComparator = new Comparator<Element>()
		{
			public int compare(Element o1, Element o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		};

		Collections.sort(model, elementComparator);
		Collections.sort(handlers, functionComparator);
	}

	public String getTypes()
	{
		if (name.equals("tabpanel"))
		{
			return ",\r\n" + "\"types\": {\r\n" + "  \"tab\": {\r\n" + "  		\"name\": \"string\",\r\n" + "  		\"containsFormId\": \"form\",\r\n"
				+ "  		\"text\": \"tagstring\",\r\n" + "  		\"relationName\": \"relation\",\r\n" + "  		\"active\": \"boolean\",\r\n"
				+ "  		\"foreground\": \"color\",\r\n" + "  		\"disabled\": \"boolean\",\r\n" + "  		\"imageMediaID\": \"media\",\r\n"
				+ "  		\"mnemonic\": \"string\"\r\n" + "  	}\r\n}";
		}
		if (name.equals("splitpane"))
		{
			return ",\r\n" + "\"types\": {\r\n" + "  \"tab\": {\r\n" + "  		\"name\": \"string\",\r\n" + "  		\"containsFormId\": \"form\",\r\n"
				+ "  		\"text\": \"tagstring\",\r\n" + "  		\"relationName\": \"relation\",\r\n" + "  		\"active\": \"boolean\",\r\n"
				+ "  		\"foreground\": \"color\",\r\n" + "  		\"disabled\": \"boolean\",\r\n" + "  		\"mnemonic\": \"string\"\r\n" + "  	}\r\n}";
		}
		return null;
	}

	public int getVersion()
	{
		return version;
	}
}
