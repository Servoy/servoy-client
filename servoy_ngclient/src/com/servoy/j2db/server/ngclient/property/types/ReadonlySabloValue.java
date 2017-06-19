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

package com.servoy.j2db.server.ngclient.property.types;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.property.ISmartPropertyValue;

import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class ReadonlySabloValue implements ISmartPropertyValue
{

	private boolean savedOppositeOfValue;

	private IWebObjectContext webObjectContext;

	private PropertyChangeListener oppositeOfListener;

	private final ReadonlyConfig configuration;

	private final boolean readOnly;

	private boolean findModeChange;

	public ReadonlySabloValue(ReadonlyConfig config, boolean readOnly)
	{
		this.configuration = config;
		this.readOnly = readOnly;
		this.savedOppositeOfValue = !readOnly;
	}

	public ReadonlySabloValue(ReadonlyConfig config, boolean readOnly, boolean oldValue)
	{
		this.configuration = config;
		this.readOnly = readOnly;
		this.savedOppositeOfValue = oldValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.ISmartPropertyValue#attachToBaseObject(org.sablo.IChangeListener, org.sablo.BaseWebObject)
	 */
	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectCntxt)
	{
		this.webObjectContext = webObjectCntxt;
		if (readOnly && !oppositeValue())
		{
			addOppositeOfListener();
			return;
		}

		if (readOnly)
		{
			setOppositeValue(false);
			savedOppositeOfValue = true;
		}
		else
		{
			setOppositeValue(savedOppositeOfValue);
		}

		addOppositeOfListener();

	}

	/**
	 *
	 */
	private void addOppositeOfListener()
	{
		oppositeOfListener = new PropertyChangeListener()
		{
			@SuppressWarnings("boxing")
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				// ignore the change when this is a find mode change.
				if (findModeChange) return;
				savedOppositeOfValue = (Boolean)evt.getNewValue();
			}
		};

		webObjectContext.addPropertyChangeListener(configuration.getOppositeOf(), oppositeOfListener);
	}


	private void setOppositeValue(boolean b)
	{

		webObjectContext.setProperty(configuration.getOppositeOf(), b);
	}


	private boolean oppositeValue()
	{
		if (configuration != null && webObjectContext != null)
		{
			Object value = webObjectContext.getProperty(configuration.getOppositeOf());
			if (value == null) value = webObjectContext.getPropertyDescription(configuration.getOppositeOf()).getDefaultValue();
			if (value != null) return (boolean)value;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.ISmartPropertyValue#detach()
	 */
	@Override
	public void detach()
	{
		if (webObjectContext != null && oppositeOfListener != null)
		{
			webObjectContext.removePropertyChangeListener(configuration.getOppositeOf(), oppositeOfListener);
		}
	}

	/**
	 * @param writer
	 * @return
	 */
	public JSONWriter toJSON(JSONWriter writer)
	{
		try
		{
			writer.value(!oppositeValue());
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		return writer;
	}

	public boolean getValue()
	{
		return !oppositeValue();
	}

	/**
	 * @return
	 */
	public boolean getOldOppositeOfValue()
	{
		return savedOppositeOfValue;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ReadonlySabloValue)
		{
			return ((ReadonlySabloValue)obj).readOnly == readOnly;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return readOnly ? 1231 : 1237;
	}

	/**
	 * @param b
	 */
	public void setFindModeChange(boolean findModeChange)
	{
		this.findModeChange = findModeChange;
	}
}
