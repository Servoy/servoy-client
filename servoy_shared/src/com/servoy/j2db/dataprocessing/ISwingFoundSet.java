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

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;
import com.servoy.j2db.util.model.IEditListModel;

/**
 * @author jblok
 */
public interface ISwingFoundSet extends IFoundSetInternal, TableModel, IEditListModel
{
	public AlwaysRowSelectedSelectionModel getSelectionModel();

	public void addTableModelListener(TableModelListener l);

	public void removeTableModelListener(TableModelListener l);

	public void fireTableModelEvent(int row, int row2, int column, int update);

	public void pinMultiSelectIfNeeded(boolean multiSelect, String formName, int pinLevel);

	public void unpinMultiSelectIfNeeded(String formName);

	public void addSelectionChangeListener(ISelectionChangeListener l);

	public void removeSelectionChangeListener(ISelectionChangeListener l);
}
