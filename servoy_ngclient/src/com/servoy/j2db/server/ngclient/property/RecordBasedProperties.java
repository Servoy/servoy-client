/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.sablo.IChangeListener;

/**
 * A class to keep track of the record based properties (of a component) and the dataProviders that each such property depends on.
 *
 * @author acostescu
 */
public class RecordBasedProperties
{

	private final List<String> recordBasedPropertyNames = new ArrayList<>();
	private final Map<String, List<String>> dataproviderToRecordBasedPropertyNames = new HashMap<>();
	private final List<String> recordBasedPropertiesThatListenToAllDPs = new ArrayList<>();
	private boolean sealed = false;

	private boolean recordBasedPropertiesChanged = false;
	private boolean recordBasedPropertiesChangedComparedToTemplate = false;

	/**
	 * Remembers/registers that a property is record-based on the returned instance. Updates changed flags as well.<br/>
	 * The call could have no effect if the given property is already known and it's dps are the same.<br/><br/>
	 *
	 * If this object is sealed for changes but it needs to update state, a clone will be generated, updated and returned instead of this object.
	 *
	 * @param propertyName the name of the record based property.
	 * @param dps the dataProviders it depends on; can be null if it depends on all dataproviders (or depends on some but those are unknown).
	 * @param monitor can be null if we are computing record based props. at FormElement stage - so nothing was sent to client yet; if non- null and
	 * the client side information about record based properties needs updates due to this method executing, valueChanged will be called on this monitor.
	 *
	 * @return this - if this is not sealed for changes or if the add operation does not change state of this object; a clone with updated state otherwise.
	 */
	public RecordBasedProperties addRecordBasedProperty(String propertyName, String[] dps, IChangeListener monitor)
	{
		RecordBasedProperties updatedRBP = this;

		if (!updatedRBP.recordBasedPropertyNames.contains(propertyName))
		{
			if (updatedRBP.sealed) updatedRBP = makeACopy();

			updatedRBP.recordBasedPropertyNames.add(propertyName);

			updatedRBP.recordBasedPropertiesChanged = true;
			updatedRBP.recordBasedPropertiesChangedComparedToTemplate = true;
			if (monitor != null) monitor.valueChanged();
		}

		if (dps != null)
		{
			for (String dp : dps)
			{
				List<String> propsThatDependOnThisDP = updatedRBP.dataproviderToRecordBasedPropertyNames.get(dp);
				if (propsThatDependOnThisDP == null)
				{
					if (updatedRBP.sealed) updatedRBP = makeACopy();

					propsThatDependOnThisDP = new ArrayList<>();
					propsThatDependOnThisDP.add(propertyName);
					updatedRBP.dataproviderToRecordBasedPropertyNames.put(dp, propsThatDependOnThisDP);
				}
				else if (!propsThatDependOnThisDP.contains(propertyName))
				{
					if (updatedRBP.sealed)
					{
						updatedRBP = makeACopy();
						propsThatDependOnThisDP = updatedRBP.dataproviderToRecordBasedPropertyNames.get(dp);
					}
					propsThatDependOnThisDP.add(propertyName);
				}
			}
			if (updatedRBP.recordBasedPropertiesThatListenToAllDPs.contains(propertyName))
			{
				if (updatedRBP.sealed) updatedRBP = makeACopy();
				updatedRBP.recordBasedPropertiesThatListenToAllDPs.remove(propertyName);
			}
		}
		else
		{
			if (updatedRBP.sealed) updatedRBP = makeACopy();
			updatedRBP.recordBasedPropertiesThatListenToAllDPs.add(propertyName);
		}

		return updatedRBP;
	}

	/**
	 * A property is no longer record-based.
	 *
	 * @param propertyName the name of the former record based property.
	 * @param monitor if the client side information about record based properties needs updates, valueChanged will be called on this monitor.
	 */
	public RecordBasedProperties removeRecordBasedProperty(String propertyName, IChangeListener monitor)
	{
		RecordBasedProperties updatedRBP = this;

		if (updatedRBP.recordBasedPropertyNames.remove(propertyName))
		{
			updatedRBP.recordBasedPropertiesChanged = true;
			updatedRBP.recordBasedPropertiesChangedComparedToTemplate = true;
			monitor.valueChanged();

			if (updatedRBP.sealed) updatedRBP = makeACopy();

			updatedRBP.recordBasedPropertiesThatListenToAllDPs.remove(propertyName);

			// remove this property from all DP arrays and remove resulting empty DP arrays
			updatedRBP.dataproviderToRecordBasedPropertyNames.entrySet().stream()
				.filter(entry -> (!entry.getValue().remove(propertyName) || entry.getValue().size() > 0));
		}

		return updatedRBP;
	}

	/**
	 * Marks this object to not be modified in the future. Calls like {@link #addRecordBasedProperty(String, String[])} that would change
	 * this object's state will just generate and return an updated copy (that is not sealed) after this method is called, without altering this instance.
	 */
	public void seal()
	{
		sealed = true;

		// these typically get sealed when assigned to FormElement values
		recordBasedPropertiesChanged = false;
		recordBasedPropertiesChangedComparedToTemplate = false;
	}

	private RecordBasedProperties makeACopy()
	{
		RecordBasedProperties copy = new RecordBasedProperties();

		copy.recordBasedPropertyNames.addAll(recordBasedPropertyNames);
		dataproviderToRecordBasedPropertyNames.forEach((dp, props) -> copy.dataproviderToRecordBasedPropertyNames.put(dp, new ArrayList<>(props)));
		copy.recordBasedPropertiesThatListenToAllDPs.addAll(recordBasedPropertiesThatListenToAllDPs);

		return copy;
	}

	public boolean contains(String propertyName)
	{
		return recordBasedPropertyNames.contains(propertyName);
	}

	public boolean areRecordBasedPropertiesChangedComparedToTemplate()
	{
		return recordBasedPropertiesChangedComparedToTemplate;
	}

	public boolean areRecordBasedPropertiesChanged()
	{
		return recordBasedPropertiesChanged;
	}

	public void clearChanged()
	{
		recordBasedPropertiesChanged = false;
	}

	/**
	 * You can use this method to iterate on names of all properties that are record-based; no matter what columnDPs they are interested in.
	 */
	public void forEach(Consumer<String> propertyNameConsumer)
	{
		recordBasedPropertyNames.forEach(propertyNameConsumer);
	}

	/**
	 * You can use this method to iterate on names of all properties that are interested in the given columnDPName and those that are interested in all column DPs.
	 */
	public void forEachPropertyNameThatListensToDp(String columnDPName, Consumer<String> propertyNameConsumer)
	{
		// iterate over properties that are interested in given DP
		List<String> propertiesThatListenToDP = dataproviderToRecordBasedPropertyNames.get(columnDPName);
		if (propertiesThatListenToDP != null) propertiesThatListenToDP.forEach(propertyNameConsumer);

		// also iterate on those properties that are interested in all column DP changes
		forEachPropertyNameThatListensToAllDPs(propertyNameConsumer);
	}

	private void forEachPropertyNameThatListensToAllDPs(Consumer<String> propertyNameConsumer)
	{
		recordBasedPropertiesThatListenToAllDPs.forEach(propertyNameConsumer);
	}

}
