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


import org.json.JSONObject;

import com.servoy.j2db.util.UUID;

/**
 * @author gboros
 */
public class WebComponent extends BaseComponent implements IBasicWebComponent, IBasicWebObject
{
	public WebComponent(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.WEBCOMPONENTS, parent, element_id, uuid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#setTypeName(java.lang.String)
	 */
	@Override
	public void setTypeName(String arg)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#getTypeName()
	 */
	@Override
	public String getTypeName()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#setJson(org.json.JSONObject)
	 */
	@Override
	public void setJson(JSONObject arg)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#setJsonSubproperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setJsonSubproperty(String key, Object value)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#getJson()
	 */
	@Override
	public JSONObject getJson()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#getFlattenedJson()
	 */
	@Override
	public JSONObject getFlattenedJson()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#getParentComponent()
	 */
	@Override
	public IBasicWebComponent getParentComponent()
	{
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IBasicWebObject#updateJSON()
	 */
	@Override
	public void updateJSON()
	{
	}
}
