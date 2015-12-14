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


import java.io.Serializable;

/**
 * Represents a like html stylesheet
 *
 * @author jblok
 */
public class Style extends StringResource implements Serializable
{
	private static final long serialVersionUID = 1L;

	public Style(IRepository repository, RootObjectMetaData metaData)
	{
		super(repository, metaData);
	}

	public String getCSSText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CSSTEXT);
	}

	public void setCSSText(String txt)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CSSTEXT, txt);
	}

	@Override
	public String getContent()
	{
		return getCSSText();
	}

	@Override
	public void setContent(String txt)
	{
		setCSSText(txt);
	}

}
