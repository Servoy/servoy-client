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

import org.sablo.specification.property.ISmartPropertyValue;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;

/**
 * Complex properties that are to be used within Servoy NG/Titanium components - interested in Servoy specific data binding behavior.
 *
 * This value's property type must implement {@link IDataLinkedType}. Depending on how that interface is implemented, {@link #dataProviderOrRecordChanged(IRecordInternal, String, boolean, boolean, boolean)}
 * gets called when any dataProvider changes or only when one dataProvider changes.
 *
 * @author acostescu
 * @see IDataLinkedType
 */
public interface IDataLinkedPropertyValue extends ISmartPropertyValue
{

	/**
	 * Called when the record a component is bound to changes or when a data-provider (see also {@link IDataLinkedType#getDataLinks(Object, org.sablo.specification.PropertyDescription, com.servoy.j2db.FlattenedSolution, com.servoy.j2db.server.ngclient.FormElement)}) changes for the component's context. (can be a global variable, form variable, record...)<br/><br/>
	 * It can only be called after {@link ISmartPropertyValue#attachToComponent(org.sablo.IChangeListener, org.sablo.WebComponent)}<br/><br/>
	 *
	 * Note: you can use the DataAdapterList provided by {@link IFormElementToSabloComponent#toSabloComponentValue(Object, org.sablo.specification.PropertyDescription, com.servoy.j2db.server.ngclient.FormElement, com.servoy.j2db.server.ngclient.WebFormComponent, com.servoy.j2db.server.ngclient.DataAdapterList)} in your type in order to handle all these parameters correctly.
	 * @param record the new record if it changed or the old one if only a data-provider value changed.
	 * @param dataProvider the data-provider that changed; is null if the entire record changed; otherwise it can be a global/form variable or record dataprovider.
	 * @param isFormDP true if 'dataProvider' represents a form variable. Only relevant if dataProvider != null.
	 * @param isGloalDP true if 'dataProvider' represents a global/scope variable. Only relevant if dataProvider != null.
	 * @param fireChangeEvent if the component supports a change event for the changed dataprovider, it should be fired. Can be false if for example only the record changed, so there was no real data change.
	 */
	void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent);

}
