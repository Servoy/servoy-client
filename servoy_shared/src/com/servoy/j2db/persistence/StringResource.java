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
package com.servoy.j2db.persistence;


import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;

import com.servoy.j2db.util.Utils;

/**
 * Base class for string-based resources.
 * 
 * @author jblok
 */
public abstract class StringResource extends AbstractRootObject implements Serializable
{
	public static final long serialVersionUID = 1L;

	//resource types
	public static final int CSS = 0; //.css
	public static final int FORM_TEMPLATE = 1; //.template
	public static final int ELEMENTS_TEMPLATE = 2; //.template

	StringResource(IRepository repository, RootObjectMetaData metaData)
	{
		super(repository, metaData);
	}

	public String getContent()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CONTENT);
	}

	public void setContent(String txt)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CONTENT, txt);
	}

	public int getResourceType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RESOURCETYPE).intValue();
	}

	public void setResourceType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_RESOURCETYPE, arg);
	}

	public void loadFromFile(File f)
	{
		loadFromFile(f, Charset.defaultCharset());
	}

	public void loadFromFile(File f, Charset encoding)
	{
		setContent(Utils.getTXTFileContent(f, encoding));
	}
}
