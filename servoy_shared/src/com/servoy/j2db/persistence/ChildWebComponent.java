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
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * A web component persist that is a child of another web-component (so as a property of another web-component)
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ChildWebComponent extends WebComponent implements IChildWebObject
{

	public static final String COMPONENT_PROPERTY_TYPE_NAME = "component";

	public final static String TYPE_NAME_KEY = "typeName";
	public final static String DEFINITION_KEY = "definition";

	private transient final String jsonKey;
	private transient int index;

	private final JSONObject fullJSONInFrmFile;

	private final PropertyDescription pdAsChildComponent;

	public ChildWebComponent(IBasicWebObject parent, int element_id, UUID uuid, String jsonKey, int index, boolean isNew, PropertyDescription pdAsChildComponent)
	{
		super(parent, element_id, uuid);

		this.jsonKey = jsonKey;
		this.index = index;

		JSONObject json = WebObjectImpl.getFullJSONInFrmFile(this, isNew);
		fullJSONInFrmFile = json != null ? json : new ServoyJSONObject();
		if (!fullJSONInFrmFile.has(DEFINITION_KEY)) fullJSONInFrmFile.put(DEFINITION_KEY, new ServoyJSONObject());
		this.pdAsChildComponent = pdAsChildComponent;
	}

	@Override
	public JSONObject getFullJsonInFrmFile()
	{
		return fullJSONInFrmFile;
	}

	@Override
	public IBasicWebObject getParent()
	{
		return (IBasicWebObject)super.getParent();
	}

	@Override
	protected WebObjectBasicImpl createWebObjectImpl()
	{
		return new WebObjectImpl(this)
		{
			@Override
			public String getTypeName()
			{
				return getFullJsonInFrmFile().optString(TYPE_NAME_KEY);
			}

			@Override
			public void setTypeName(String arg)
			{
				getFullJsonInFrmFile().put(TYPE_NAME_KEY, arg);
			}

			@Override
			public JSONObject getJson()
			{
				return getFullJsonInFrmFile().optJSONObject(DEFINITION_KEY);
			}

			@Override
			public void setJsonInternal(JSONObject arg)
			{
				getFullJsonInFrmFile().put(DEFINITION_KEY, arg);
			}
		};
	}

	public String getJsonKey()
	{
		return jsonKey;
	}

	public int getIndex()
	{
		return index;
	}

	public void setIndex(int i)
	{
		index = i;
	}

	@Override
	public PropertyDescription getPropertyDescription()
	{
		return pdAsChildComponent;
	}

}
