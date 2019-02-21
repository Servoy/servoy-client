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


import java.util.List;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Dummy state to prevent the J(Edit)List to retrieve all the rows in the formmodel(=foundset)
 *
 * @author jblok
 */
public class PrototypeState extends Record
{
	public PrototypeState(IFoundSetInternal parent)
	{
		super(parent);
	}

	@Override
	public boolean isEditing()
	{
		return isEditing;
	}

	boolean isEditing = false;

	@Override
	public boolean startEditing()
	{
		isEditing = true;
		return isEditing;//true is needed for search in navigator
	}

	@Override
	public int stopEditing()
	{
		isEditing = false;
		return ISaveConstants.STOPPED;
	}

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	@Override
	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		if (parent != null)
		{
			if (relationName != null)
			{
				int dot = relationName.indexOf('.');
				String firstRelation = (dot > 0) ? relationName.substring(0, dot) : relationName;
				Relation relation = parent.getFoundSetManager().getApplication().getFlattenedSolution().getRelation(firstRelation);
				if (relation != null && (relation.isGlobal() || relation.isParentRef()))//only do handle global relations or pass the same foundset
				{
					return super.getRelatedFoundSet(relationName, defaultSortColumns);
				}
			}
		}
		return null;
	}

	@Override
	public Object getValue(String dataProviderID, boolean converted)
	{
		if (dataProviderID == null || parent == null) return null;

		//check if is related value request
		if (ScopesUtils.isVariableScope(dataProviderID))
		{
			return parent.getDataProviderValue(dataProviderID);
		}

		int index = dataProviderID.lastIndexOf('.');
		if (index > 0)
		{
			String relationName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);

			IFoundSetInternal foundSet = getRelatedFoundSet(relationName);
			if (foundSet != null)
			{
				//related data
				IRecordInternal state = foundSet.getRecord(foundSet.getSelectedIndex());
				if (state != null)
				{
					return state.getValue(restName, converted);
				}
			}
			return null;
		}
		else
		{
			if (parent.getColumnIndex(dataProviderID) != -1)
			{
				return null;
			}
			if (parent.containsCalculation(dataProviderID))
			{
				return null;
			}
			if (parent.containsAggregate(dataProviderID))
			{
				return null;//parent.getDataProviderValue(dataProviderID);
			}
			if (parent.isValidRelation(dataProviderID))
			{
				return getRelatedFoundSet(dataProviderID);
			}
			return Scriptable.NOT_FOUND;
		}
	}

	@Override
	public Object setValue(String dataProviderID, Object value)
	{
		//check if is related value request
		if (ScopesUtils.isVariableScope(dataProviderID))
		{
			return parent.setDataProviderValue(dataProviderID, value);
		}
		int index = dataProviderID.lastIndexOf('.');
		if (index > 0)
		{
			String relationName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);
			IFoundSetInternal foundSet = getRelatedFoundSet(relationName);
			if (foundSet != null)
			{
				return foundSet.setDataProviderValue(restName, value);
			}
		}
		parent.isRecordEditable(0);
		return null;
	}

	@Override
	public boolean existInDataSource()
	{
		return true;//pretend to be stored, we never want to store this
	}

	@Override
	public boolean isLocked()
	{
		return true;//prevent edit
	}

	@Override
	public String getPKHashKey()
	{
		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("PrototypeState[] COLUMS: ");
		for (Object element : getIds())
		{
			sb.append(element);
			sb.append(',');
		}
		return sb.toString();
	}
}
