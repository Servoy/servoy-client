/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.persistence;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 *
 * @since 7.0
 */
//do not tag class as mobile until https://support.servoy.com/browse/SVY-3949 is fixed
public interface IBaseField extends IBaseFieldCommon
{
	/**
	 * The valuelist that is used by this field when displaying data. Can be used
	 * with fields of type CHECKS, COMBOBOX, RADIOS and TYPE_AHEAD.
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	int getValuelistID();

	void setValuelistID(int arg);

	/**
	 * @sameas com.servoy.base.persistence.IBaseGraphicalComponent#getOnActionMethodID()
	 * 
	 * @templatedescription Perform the element default action
	 * @templatename onAction
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	int getOnActionMethodID();

	void setOnActionMethodID(int arg);

	/**
	 * Method that is executed when the data in the component is successfully changed.
	 * 
	 * @templatedescription Handle changed data
	 * @templatename onDataChange
	 * @templatetype Boolean
	 * @templateparam ${dataproviderType} oldValue old value
	 * @templateparam ${dataproviderType} newValue new value
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	int getOnDataChangeMethodID();

	void setOnDataChangeMethodID(int arg);


}