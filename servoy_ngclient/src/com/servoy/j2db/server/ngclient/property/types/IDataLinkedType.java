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

package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;

/**
 * Interface for types that can have their value linked to data in a record or in a form/global variable.
 * For example dataprovider types can be linked to nothing, to a global or form variable or to a record dataprovider.<br/><br/>
 *
 * The type can report whether or not a (runtime - sablo component property) value is or is not dependent on the foundset's record.
 * This way components that are linked to foundsets can know what to send for all records and what to send for each record to the browser.<br/><br/>
 *
 * Usually this type of property also uses {@link IDataLinkedPropertyValue} as a runtime (sablo) property value.<br/><br/>
 *
 * By default, types that do not implement this interface are considered to not depend on (record or variables) data.
 *
 * @author acostescu
 * @see IDataLinkedPropertyValue
 */
public interface IDataLinkedType<FormElementT, T> extends IPropertyType<T>
{

	public class TargetDataLinks
	{

		/**
		 * Used for properties that implement {@link IDataLinkedType} but decide not to be 'linked to data' based on their value.
		 */
		public static final TargetDataLinks NOT_LINKED_TO_DATA = new TargetDataLinks(null, false);

		/**
		 * Used by properties that are interested in changes to all data (record change or any dataProvider change).
		 */
		public static final TargetDataLinks LINKED_TO_ALL = new TargetDataLinks(null, true);

		/** the dataProvider that a property is interested in. */
		public final String[] dataProviderIDs;
		/** true of this is a record dependent dataProvider and false otherwise (scope/form variable). */
		public final boolean recordLinked;

		/**
		 * Creates a new TargetDataLinks instance. If the given dataProviderID is not null, that means that the property
		 * is interested in changes to one dataProviderID only (so whole record changing or that specific dataProvider changing).
		 * @param dataProviderID the dataProvider that a property is interested in.
		 * @param recordLinked true of this is a record dependent dataProvider and false otherwise (scope/form variable).
		 */
		public TargetDataLinks(String[] dataProviderIDs, boolean recordLinked)
		{
			this.dataProviderIDs = dataProviderIDs;
			this.recordLinked = recordLinked;
		}

		public TargetDataLinks concatDataLinks(String[] dataproviderids, boolean linked)
		{
			if (dataproviderids.length > 0)
			{
				String[] concat = new String[this.dataProviderIDs.length + dataproviderids.length];
				System.arraycopy(this.dataProviderIDs, 0, concat, 0, this.dataProviderIDs.length);
				System.arraycopy(dataproviderids, 0, concat, this.dataProviderIDs.length, dataProviderIDs.length);
				return new TargetDataLinks(concat, this.recordLinked || linked);
			}
			return this;
		}
	}

	/**
	 * Returns the target data that this property type is interested in.<BR/><BR/>
	 * If this property does not want to be linked to data it should return {@link #NOT_LINKED_TO_DATA}.
	 * If it wants to be notified of all data changes (record change + all dataProvider changes), it should return {@link #LINKED_TO_ALL}.
	 * If it wants to be notified of specific data changes (for example only some dataprovider changes + record change it should return an appropriate instance of {@link TargetDataLinks}.
	 *
	 * @param value this is the template/form element value of the property.
	 * @return some TargetDataLinks if this value depends on foundset record or scripting variables or {@link TargetDataLinks#NOT_LINKED_TO_DATA} if not. Another predifined value you can use is {@link TargetDataLinks#LINKED_TO_ALL}.
	 */
	TargetDataLinks getDataLinks(FormElementT formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement);

}
