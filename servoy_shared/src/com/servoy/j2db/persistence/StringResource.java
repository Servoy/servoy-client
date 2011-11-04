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

	private String content;
	private int resourceType;

	StringResource(IRepository repository, RootObjectMetaData metaData)
	{
		super(repository, metaData);
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String txt)
	{
		checkForChange(content, txt);
		this.content = txt;
	}

	public int getResourceType()
	{
		return resourceType;
	}

	public void setResourceType(int arg)
	{
		checkForChange(resourceType, arg);
		this.resourceType = arg;
	}

	public void loadFromFile(File f)
	{
		setContent(Utils.getTXTFileContent(f, Charset.forName("UTF8")));
	}
}
