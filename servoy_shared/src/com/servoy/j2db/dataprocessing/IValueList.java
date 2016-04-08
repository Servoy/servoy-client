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
package com.servoy.j2db.dataprocessing;


import javax.swing.ListModel;

import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ValueList;

/**
 * Runtime Valuelist interface that extends {@link ListModel} for attaching a {@link ValueList} to a UI Element.
 *
 * @author jcompagner, jblok
 *
 * @see CustomValueList
 * @see DBValueList
 * @see RelatedValueList
 * @see GlobalMethodValueList
 */
public interface IValueList extends ListModel
{
	/**
	 * Constant for the design-time value that should be interpreted as a separator.
	 */
	public static final Object SEPARATOR_DESIGN_VALUE = "-";

	/**
	 * As ppl. might want to use "-" as a real value in a valuelist, not just as separator, they can specify this at designtime by using this constant's value (that is unlikely to be needed as a real valuelist value).
	 */
	public static final String ESCAPED_SEPARATOR_DESIGN_VALUE = "\\-";

	/**
	 * Constant for the runtime value returned by valuelists to components that should be interpreted as a separator when possible by fields that display the valuelist.
	 */
	public static final Object SEPARATOR = new Object();

	public Object getRealElementAt(int row);//real value, getElementAt is display value

	public String getRelationName();

	public void fill(IRecordInternal parentState);//to create all the rows

	public int realValueIndexOf(Object obj);

	public int indexOf(Object elem);

	public void deregister();

	public boolean getAllowEmptySelection();

	public String getName();

	/**
	 * @return
	 */
	public boolean hasRealValues();

	public void setFallbackValueList(IValueList list);

	public IValueList getFallbackValueList();

	public ValueList getValueList();

	/**
	 * Returns the dataprovders this valuelist depends on, if none then return null
	 * If it can't really determine then return a new IDataProvider[0] instance so
	 * it will threat this valuelist that it depends on all dataprovider changes.
	 *
	 * @return
	 */
	public IDataProvider[] getDependedDataProviders();

	/**
	 * @param errorMessage
	 */
	public void reportJSError(String errorMessage);
}
