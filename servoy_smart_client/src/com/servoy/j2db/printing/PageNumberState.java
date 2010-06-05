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
package com.servoy.j2db.printing;


import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.dataprocessing.FireCollector;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.Row;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Debug;

/**
 * Special state for part showing page number info
 * 
 * @author jblok
 */
public class PageNumberState implements IRecordInternal
{
	private int currentPage;
	private int totalPages = -1;
	private final PageList list;
	private IFoundSetInternal delegate_to_record;
	private int delegate_record_index;

	PageNumberState(IFoundSetInternal set, PageList list)
	{
		delegate_to_record = set;
		this.list = list;
	}

	public Object getValue(String dataProviderID)
	{
		if (dataProviderID == null) return null;


		if ("pageNumber".equals(dataProviderID)) //$NON-NLS-1$
		{
			return new Integer(currentPage);
		}
		else if ("totalNumberOfPages".equals(dataProviderID)) //$NON-NLS-1$
		{
			return new Integer(totalPages);
		}
		else if (getDelegate() != null)
		{
			return getDelegate().getValue(dataProviderID);
		}
		else
		{
			Debug.trace("no delegate in pagenumber state"); //$NON-NLS-1$
		}
		return null;
	}

	void initPagePositionAndSize(PageDefinition pd)
	{
		Iterator<DataRendererDefinition> it = pd.getPanels().iterator();
		while (it.hasNext())
		{
			DataRendererDefinition drd = it.next();
			if (drd != null && drd.getPart() != null && drd.getPart().getPartType() == Part.BODY)
			{
				IRecordInternal rec = drd.getState();
				if (rec != null)
				{
					delegate_to_record = rec.getParentFoundSet();
					delegate_record_index = delegate_to_record.getRecordIndex(rec);
				}
				//do not break we want the last body record
			}
		}

		currentPage = list.getPageNumber(pd);
		totalPages = list.getPageSectionLength(pd);
	}

	public void addModificationListener(IModificationListener l)
	{
		//ignore
	}

	@Deprecated
	public void addModificationListner(IModificationListener l)
	{
		addModificationListener(l);
	}

	@Deprecated
	public void removeModificationListner(IModificationListener l)
	{
		removeModificationListener(l);
	}


	public String getAsTabSeparated()
	{
		//ignore
		return null;
	}

	public IFoundSetInternal getParentFoundSet()
	{
		return delegate_to_record;
	}

	private IRecordInternal getDelegate()
	{
		if (delegate_to_record.getSize() != 0)
		{
			int idx = delegate_record_index;
			if (delegate_to_record.getSize() < idx)
			{
				idx = 0;
			}
			return delegate_to_record.getRecord(idx);
		}
		return null;
	}

	public String getPKHashKey()
	{
		if (getDelegate() != null)
		{
			return getDelegate().getPKHashKey();
		}
		else
		{
			return null;
		}
	}

	public Object[] getPK()
	{
		if (getDelegate() != null)
		{
			return getDelegate().getPK();
		}
		else
		{
			return null;
		}
	}

	public Row getRawData()
	{
		if (getDelegate() != null)
		{
			return getDelegate().getRawData();
		}
		else
		{
			return null;
		}
	}

	public IFoundSetInternal getRelatedFoundSet(String relationName)
	{
		return getRelatedFoundSet(relationName, null);
	}

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		if (getDelegate() != null)
		{
			return getDelegate().getRelatedFoundSet(relationName, defaultSortColumns);
		}
		return null;
	}

	public boolean isEditing()
	{
		return false;
	}

	public boolean existInDataSource()
	{
		return true;//the delegate is always a stored record
	}

	@Deprecated
	public boolean existInDB()
	{
		return existInDataSource();
	}

	public boolean isLocked()
	{
		return true;//prevent edit
	}

	public void removeModificationListener(IModificationListener l)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IRecord#has(java.lang.String)
	 */
	public boolean has(String dataprovider)
	{
		return false;
	}

	public Object setValue(String dataProviderID, Object value)
	{
		return setValue(dataProviderID, value, true);
	}

	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
	{
		//ignore
		return null;
	}

	public boolean startEditing()
	{
		//ignore
		return false;
	}

	public int stopEditing()
	{
		return ISaveConstants.STOPPED;
	}

	public void notifyChange(ModificationEvent e, FireCollector col)
	{
		//ignore
	}

	public boolean isRelatedFoundSetLoaded(String relationName, String restName)
	{
		return true;//return true to prevent async loading.
	}

	public boolean startEditing(boolean b)
	{
		return startEditing();
	}
}
