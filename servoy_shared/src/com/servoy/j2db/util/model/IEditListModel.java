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
package com.servoy.j2db.util.model;


/**
 * The <code>TableModel</code> interface specifies the methods the <code>JTable</code> will use to interrogate a tabular data model.
 * <p>
 * 
 * The <code>JTable</code> can be set up to display any data model which implements the <code>TableModel</code> interface with a couple of lines of code:
 * <p>
 * 
 * <pre>
 * TableModel myData = new MyTableModel();
 * JTable table = new JTable(myData);
 * </pre>
 * 
 * <p>
 * 
 * @author jblok
 */
public interface IEditListModel extends javax.swing.ListModel
{


	/**
	 * Returns true if the cell at <code>rowIndex</code> and <code>columnIndex</code> is editable. Otherwise, <code>setValueAt</code> on the cell will not
	 * change the value of that cell.
	 * 
	 * @param rowIndex the row whose value to be queried
	 * @return true if the cell is editable
	 * @see #setValueAt
	 */
	public boolean isCellEditable(int rowIndex);


	/**
	 * Sets the value in the cell at <code>columnIndex</code> and <code>rowIndex</code> to <code>aValue</code>.
	 * 
	 * @param aValue the new value
	 * @param rowIndex the row whose value is to be changed
	 * @see #getValueAt
	 * @see #isCellEditable
	 */
	public void setElementAt(Object aValue, int rowIndex);
}
