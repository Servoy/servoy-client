/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;

/**
 * Useful when monitoring the selection in a chain of related foundsets is needed.
 * If you have a relation like "relation1.relation2.relation3.relation4" this class will monitor selection changes in foundsets relation 1 to relation 3 and notify when it was changed.
 *
 * After the selection change, the old intermediate foundsets are still used; if you want to monitor the new chain of related foundsets that the selection change determined you will have
 * to call {@link #update(IFoundSetInternal, IRecordInternal, String)}
 *
 * @author acostescu
 */
public class ChainedRelatedFoundsetSelectionMonitor
{

	private IFoundSetInternal relatedFoundset;
	private final List<ISwingFoundSet> monitoredChainedFoundsets = new ArrayList<>();
	private IRecordInternal rootRecord;
	private String nestedRelationNames;
	private final ListSelectionListener listSelectionListener;

	public ChainedRelatedFoundsetSelectionMonitor(final IRelatedFoundsetChainSelectionChangeListener selectionInChainChangedListener)
	{
		listSelectionListener = new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				selectionInChainChangedListener.selectionChanged(rootRecord, nestedRelationNames);
			}
		};
	}

	/**
	 * Updates record selection listeners - if needed - for each related foundset in the link of relations.
	 *
	 * @param newRelatedFoundset newRelatedFoundset will always be rootRecord.getValue(nestedRelationNames)
	 * @param rootRecord the root record where the chain of relations to monitor starts
	 * @param nestedRelationNames the chain of relations like "relation1.relation2.relation3"
	 */
	public void update(IFoundSetInternal newRelatedFoundset, @SuppressWarnings("hiding") IRecordInternal rootRecord,
		@SuppressWarnings("hiding") String nestedRelationNames)
	{
		if (relatedFoundset != newRelatedFoundset)
		{
			unregisterListeners();
			relatedFoundset = newRelatedFoundset;

			// add new listeners
			String[] relationNames = nestedRelationNames.split("\\.");
			IRecordInternal currentRecord = rootRecord;
			for (int i = 0; i < relationNames.length - 1; i++)
			{
				ISwingFoundSet currentRelatedFoundset = (ISwingFoundSet)currentRecord.getValue(relationNames[i]);
				currentRelatedFoundset.getSelectionModel().addListSelectionListener(listSelectionListener);
				monitoredChainedFoundsets.add(currentRelatedFoundset);
			}
		}
		this.rootRecord = rootRecord;
		this.nestedRelationNames = nestedRelationNames;
	}

	/**
	 * Returns the related foundset that is currently being monitored through the relation chain.
	 */
	public IFoundSetInternal getRelatedFoundset()
	{
		return relatedFoundset;
	}

	public void unregisterListeners()
	{
		for (ISwingFoundSet cf : monitoredChainedFoundsets)
		{
			cf.getSelectionModel().removeListSelectionListener(listSelectionListener);
		}
		monitoredChainedFoundsets.clear();
	}

	public static interface IRelatedFoundsetChainSelectionChangeListener
	{

		void selectionChanged(IRecordInternal rootRecord, String nestedRelationNames);

	}

}
