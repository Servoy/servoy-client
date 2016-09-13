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

package com.servoy.j2db.documentation;

import java.util.SortedSet;

import org.dom4j.Element;

import com.servoy.base.util.ITagResolver;


/**
 * Object documentation interface.
 */
public interface IObjectDocumentation
{
	public IFunctionDocumentation getFunction(String functionName);

	public IFunctionDocumentation getFunction(String functionName, String[] argumentsTypes);

	public IFunctionDocumentation getFunction(String functionName, int argCount);

	public IFunctionDocumentation getFunction(String functionName, Class< ? >[] argumentsTypes);

	public SortedSet<IFunctionDocumentation> getFunctions();

	public void addReturnedType(String qname);

	public SortedSet<String> getReturnedTypes();

	public String getQualifiedName();

	public String getPublicName();

	public String getCategory();

	public String[] getParentClasses();

	public String getExtendsClass();

	public void addFunction(IFunctionDocumentation function);

	public void setHide(boolean hide);

	public void runResolver(ITagResolver resolver);

	public void addServerProperty(String name, String description);

	public boolean goesToXML(boolean hideDeprecated);

	public Element toXML(IDocumentationManager docManager, boolean hideDeprecated, boolean pretty);

	public ClientSupport getClientSupport();

	public boolean isDeprecated();
}
