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

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebComponentInitializer;
import org.sablo.WebComponent;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;

/**
 * Value used at runtime as foundset type value proxy for multiple interested parties (browser, designtime, scripting).
 * 
 * @author acostescu
 */
public class FoundsetTypeValue implements IComplexPropertyValue
{

	private final Object designJSONValue;
	private boolean changed;
	private WebFormComponent component;

	public FoundsetTypeValue(Object designJSONValue, Object config)
	{
		changed = true;
		this.designJSONValue = designJSONValue; // maybe we should parse it and not keep it as JSON (it can be reconstructed afterwards from parseed content if needed)
		// TODO ac Auto-generated constructor stub
	}

	@Override
	public void initialize(IWebComponentInitializer fe, String propertyName, Object defaultValue)
	{
		// TODO ac Auto-generated method stub

	}

	@Override
	public void attachToComponent(IChangeListener changeMonitor, WebComponent component)
	{
		this.component = (WebFormComponent)component;
		// TODO ac Auto-generated method stub
		if (changed)
		{
			changeMonitor.valueChanged();
		}
	}

	@Override
	public JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO ac Auto-generated method stub
		changed = false; // TODO any partial messages?
		return destinationJSON.value("dummyFoundsetvalue");
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO ac Auto-generated method stub
		if (changed) return toJSON(destinationJSON, conversionMarkers);
		else destinationJSON.value(null);
		return destinationJSON;
	}

	@Override
	public JSONWriter toDesignJSON(JSONWriter writer) throws JSONException
	{
		return writer.value(designJSONValue);
	}

	@Override
	public Object toServerObj()
	{
		// TODO implement more here if we want this type of properties accessible in scripting
		return null;
	}

	public void browserUpdatesReceived(Object jsonValue)
	{
		// TODO ac Auto-generated method stub

	}

	public IDataAdapterList getDataAdapterList()
	{
		// this method gets called by linked component type property/properties
		// TODO ac this is a dummy impl.; it must be a data adapter list useable with this foundset's contents and on the correct relation
		return ((IWebFormUI)component.getParent()).getDataAdapterList();
	}
}
