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
package com.servoy.j2db.util.gui;

import java.util.Collection;
import java.util.Comparator;

import javax.swing.ComboBoxModel;

/**
 * @author jcompagner
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SortedComboModel extends SortedListModel implements ComboBoxModel
{
	private Object selectedObject;
	/**
	 * @param comparator
	 */
	public SortedComboModel(Comparator comparator) {
		super(comparator);
	}

	/**
	 * @param comparator
	 * @param collection
	 */
	public SortedComboModel(Comparator comparator, Collection collection) {
		super(comparator, collection);
	}

	/* (non-Javadoc)
	 * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
	 */
	public void setSelectedItem(Object anObject)
	{
      if ((selectedObject != null && !selectedObject.equals( anObject )) ||
     	    selectedObject == null && anObject != null) 
      {
     	    selectedObject = anObject;
     	    fireContentsChanged(-1, -1);
      }
	}

	/* (non-Javadoc)
	 * @see javax.swing.ComboBoxModel#getSelectedItem()
	 */
	public Object getSelectedItem()
	{
		return selectedObject;
	}

}
