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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.servoy.j2db.util.LinkedListWithAccessToNodes;
import com.servoy.j2db.util.LinkedListWithAccessToNodes.Node;

/**
 * @author jcompagner
 */
public interface IFireCollectable
{

	/**
	 * Make sure that the Set<String> are never null! They should always contain at least 1 String (dataprovider).
	 * See {@link FireCollector}.
	 */
	default void completeFire(Map<IRecord, Set<String>> entries)
	{
		class UpdateOp
		{
			int start, end;
			Set<String> dataproviders;

			public UpdateOp(int start, int end, Set<String> dataproviders)
			{
				this.start = start;
				this.end = end;
				this.dataproviders = dataproviders;
			}
		}

		LinkedListWithAccessToNodes<UpdateOp> sortedUpdateOps = new LinkedListWithAccessToNodes<>();

		// merge multiple 1-record change events into less (even 1 change event with an interval) if possible; so that we fire as few changes as needed
		for (Entry<IRecord, Set<String>> entry : entries.entrySet())
		{
			int indexOfRecord = getRecordIndex(entry.getKey());

			if (indexOfRecord != -1)
			{
				Set<String> dataproviders = entry.getValue(); // CANNOT BE null!

				boolean addNewOpAtTheEnd = true;
				Node<UpdateOp> updateOpNode = sortedUpdateOps.size() > 0 ? sortedUpdateOps.getFirstNode() : null;

				while (updateOpNode != null)
				{
					UpdateOp updateOp = updateOpNode.item();
					if (indexOfRecord == updateOp.start - 1 || indexOfRecord == updateOp.end + 1)
					{
						// index is right next to updateOp interval; see if dataProviders match
						if (dataproviders.containsAll(updateOp.dataproviders) && updateOp.dataproviders.containsAll(dataproviders))
						{
							// yep, they do; just update interval
							if (indexOfRecord == updateOp.start - 1) updateOp.start--;
							if (indexOfRecord == updateOp.end + 1) updateOp.end++;
						}
						else
						{
							// no, they do not; create a new OP
							if (indexOfRecord == updateOp.start - 1)
								sortedUpdateOps.addBefore(updateOpNode, new UpdateOp(indexOfRecord, indexOfRecord, dataproviders));
							else /* indexOfRecord == updateOp.end + 1 */ sortedUpdateOps.addAfter(updateOpNode,
								new UpdateOp(indexOfRecord, indexOfRecord, dataproviders));

						}

						addNewOpAtTheEnd = false;
						updateOpNode = null; // finish iteration
					}
					// this else is not possible because we don't have the same record twice in the map - so it can't overlap a previously created updateOp
//					else if (indexOfRecord >= updateOp.start && indexOfRecord <= updateOp.end)
//					{
//						// index is inside of the updateOp changed interval; see if dataproviders match or are less then the current ones on the updateOp
//						if (!updateOp.dataproviders.containsAll(dataproviders))
//						{
//							// we have to split current updateOp... into a (maybe) before interval, one interval at record index and (maybe) an after interval
//							if (updateOp.start < indexOfRecord)
//								sortedUpdateOps.addBefore(updateOpNode, new UpdateOp(updateOp.start, indexOfRecord - 1, updateOp.dataproviders));
//
//							updateOp.start = updateOp.end = indexOfRecord;
//							updateOp.dataproviders = new HashSet<>(updateOp.dataproviders);
//							updateOp.dataproviders.addAll(dataproviders);
//
//							if (updateOp.end > indexOfRecord)
//								sortedUpdateOps.addAfter(updateOpNode, new UpdateOp(indexOfRecord + 1, updateOp.end, updateOp.dataproviders));
//
//						} // else nothing more to do here
//
//						addNewOpAtTheEnd = false;
//						updateOpNode = null; // finish iteration
//					}
					else if (indexOfRecord < updateOp.start - 1)
					{
						// it is a separate update op; it's not next to either it's previous change index or it's next change index
						sortedUpdateOps.addBefore(updateOpNode, new UpdateOp(indexOfRecord, indexOfRecord, dataproviders));

						addNewOpAtTheEnd = false;
						updateOpNode = null; // finish iteration
					}
					else updateOpNode = updateOpNode.next(); // continue searching to see if we can merge this change into an existing change op.
				}

				if (addNewOpAtTheEnd)
				{
					// we found no changed interval to add it to; add a new one
					sortedUpdateOps.addLast(new UpdateOp(indexOfRecord, indexOfRecord, dataproviders));
				}
			}
		}

		// ok, now fire the merged ops; because the initial "entries" might contain records in random order, it is also possible now
		// that 2 UpdateOp might be right next to each other with the same DPs - in that case merge the 2 UpdateOps into one event fire
		Node<UpdateOp> updateOpNode = sortedUpdateOps.getFirstNode();
		while (updateOpNode != null)
		{
			UpdateOp updateOp = updateOpNode.item();
			int start = updateOp.start, end = updateOp.end;

			Node<UpdateOp> nextUpdateOpNode = updateOpNode.next();

			while (nextUpdateOpNode != null && nextUpdateOpNode.item().start == updateOp.end + 1 &&
				nextUpdateOpNode.item().dataproviders.containsAll(updateOp.dataproviders) &&
				updateOp.dataproviders.containsAll(nextUpdateOpNode.item().dataproviders))
			{
				end = nextUpdateOpNode.item().end;
				nextUpdateOpNode = nextUpdateOpNode.next();
			}

			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE, updateOp.dataproviders);
			updateOpNode = nextUpdateOpNode;
		}
	}

	// this is a duplicate of the one in IFoundset; it's needed by the "default" method above
	public int getRecordIndex(IRecord record);

	void fireFoundSetEvent(int firstRow, int lastRow, int changeType, Set<String> dataproviders);

}
