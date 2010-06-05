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
package com.servoy.j2db.scripting;

import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Debug;

/**
 * Keep track of used data when calculating a calculation value.
 * 
 * @author rob
 *
 */
public class UsedDataProviderTracker
{
	private final FlattenedSolution flattenedSolution;

	Set<String> usedGlobals = null;
	Set<UsedDataProvider> usedColumns = null;
	Set<UsedRelation> usedRelations = null;
	Set<UsedAggregate> usedAggregates = null;

	public UsedDataProviderTracker(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	public void usedRelatedFoundSet(String name, RelatedFoundSet foundSet)
	{
		// add a dependency for relation size changes, rowManager of current record will listen to changes to the related foundset identified by whereArgsHash.
		String whereArgsHash = foundSet.getWhereArgsHash();
		if (usedRelations == null)
		{
			usedRelations = new HashSet<UsedRelation>();
		}
		usedRelations.add(new UsedRelation(name, whereArgsHash));
	}

	public void usedName(Scriptable scriptable, String name)
	{
		if (name.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			// global
			usedGlobal(name);
		}

		else if (scriptable instanceof IFoundSetInternal)
		{
			IFoundSetInternal foundSet = (IFoundSetInternal)scriptable;
			if (foundSet.getSQLSheet().containsAggregate(name))
			{
				// aggregate
				usedAggregate(foundSet, name);
			}
			else
			{
				usedFromRecord(foundSet.getRecord(foundSet.getSelectedIndex()) /* may be null for global relations */, name);
			}
		}

		else if (scriptable instanceof IRecordInternal)
		{
			usedFromRecord((IRecordInternal)scriptable, name);
		}

		else if (scriptable instanceof TableScope)
		{
			usedFromRecord((IRecordInternal)scriptable.getPrototype(), name);
		}
	}

	/**
	 * Used name from Record,
	 * @param record may be null (for global relations)
	 * @param name
	 */
	protected void usedFromRecord(IRecordInternal record, String name)
	{
		IRecordInternal currentRecord = record;
		try
		{
			String[] parts = name.split("\\."); //$NON-NLS-1$
			for (String part : parts)
			{
				Relation relation = flattenedSolution.getRelation(part);
				if (relation != null)
				{
					// calc depends on the relation, add a dependency for the primary data providers for the relation
					IDataProvider[] primaryDataProviders = relation.getPrimaryDataProviders(flattenedSolution);
					for (IDataProvider prim : primaryDataProviders)
					{
						String primdp = prim.getDataProviderID();
						if (primdp.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
						{
							// global
							usedGlobal(primdp);
						}
						else
						{ // column
							if (currentRecord != null)
							{
								usedColumn(currentRecord.getParentFoundSet().getDataSource(), currentRecord.getRawData().getPKHashKey(), primdp);
							}
						}
					}

					IFoundSetInternal foundSet = null;
					if (currentRecord != null) foundSet = currentRecord.getRelatedFoundSet(relation.getName());
					currentRecord = null;
					if (foundSet instanceof RelatedFoundSet)
					{
						usedRelatedFoundSet(relation.getName(), (RelatedFoundSet)foundSet);
						currentRecord = foundSet.getRecord(foundSet.getSelectedIndex());
					}
				}
				else
				{
					if (currentRecord != null)
					{
						IFoundSetInternal foundSet = currentRecord.getParentFoundSet();
						if (foundSet.getSQLSheet().containsAggregate(part))
						{
							// aggregate
							usedAggregate(foundSet, part);
						}
						else
						{
							// field or calc
							if (currentRecord.has(part))
							{
								usedColumn(foundSet.getDataSource(), currentRecord.getRawData().getPKHashKey(), part);
							}
						}
					}
					return;
				}

				if (currentRecord == null)
				{
					return;
				}
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
	}

	public Set<String> getGlobals()
	{
		return usedGlobals;
	}

	public Set<UsedAggregate> getAggregates()
	{
		return usedAggregates;
	}

	public Set<UsedDataProvider> getColumns()
	{
		return usedColumns;
	}

	public Set<UsedRelation> getRelations()
	{
		return usedRelations;
	}


	public void usedAggregate(IFoundSetInternal foundSet, String name)
	{
		if (usedAggregates == null)
		{
			usedAggregates = new HashSet<UsedAggregate>();
		}
		usedAggregates.add(new UsedAggregate(foundSet, name));
	}

	public void usedColumn(String dataSource, String pkHashKey, String dataProviderId)
	{
		if (usedColumns == null)
		{
			usedColumns = new HashSet<UsedDataProvider>();
		}
		usedColumns.add(new UsedDataProvider(dataSource, pkHashKey, dataProviderId));
	}

	public void usedGlobal(String name)
	{
		if (usedGlobals == null)
		{
			usedGlobals = new HashSet<String>();
		}
		usedGlobals.add(name);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("UsedDataProviderTracker ["); //$NON-NLS-1$
		if (usedAggregates != null)
		{
			sb.append(" aggregates="); //$NON-NLS-1$
			sb.append(usedAggregates);
		}
		if (usedColumns != null)
		{
			sb.append(" columns="); //$NON-NLS-1$
			sb.append(usedColumns);
		}
		if (usedGlobals != null)
		{
			sb.append(" globals="); //$NON-NLS-1$
			sb.append(usedGlobals);
		}
		if (usedRelations != null)
		{
			sb.append(" relations="); //$NON-NLS-1$
			sb.append(usedRelations);
		}
		return sb.append(" ]").toString(); //$NON-NLS-1$
	}

	public static class UsedDataProvider
	{
		public final String dataSource;
		public final String pkHashKey;
		public final String dataProviderId;

		public UsedDataProvider(String dataSource, String pkHashKey, String dataProviderId)
		{
			this.dataSource = dataSource;
			this.pkHashKey = pkHashKey;
			this.dataProviderId = dataProviderId;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dataProviderId == null) ? 0 : dataProviderId.hashCode());
			result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
			result = prime * result + ((pkHashKey == null) ? 0 : pkHashKey.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			UsedDataProvider other = (UsedDataProvider)obj;
			if (dataProviderId == null)
			{
				if (other.dataProviderId != null) return false;
			}
			else if (!dataProviderId.equals(other.dataProviderId)) return false;
			if (dataSource == null)
			{
				if (other.dataSource != null) return false;
			}
			else if (!dataSource.equals(other.dataSource)) return false;
			if (pkHashKey == null)
			{
				if (other.pkHashKey != null) return false;
			}
			else if (!pkHashKey.equals(other.pkHashKey)) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "UsedDataProvider [dataProviderId=" + dataProviderId + ", dataSource=" + dataSource + ", pkHashKey=" + pkHashKey + ']'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public static class UsedRelation
	{
		public final String name;
		public final String whereArgsHash;

		public UsedRelation(String name, String whereArgsHash)
		{
			this.name = name;
			this.whereArgsHash = whereArgsHash;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((whereArgsHash == null) ? 0 : whereArgsHash.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			UsedRelation other = (UsedRelation)obj;
			if (name == null)
			{
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			if (whereArgsHash == null)
			{
				if (other.whereArgsHash != null) return false;
			}
			else if (!whereArgsHash.equals(other.whereArgsHash)) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "UsedRelation [name=" + name + ", whereArgsHash=" + whereArgsHash + ']'; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static class UsedAggregate
	{
		public final IFoundSetInternal foundSet;
		public final String name;

		public UsedAggregate(IFoundSetInternal foundSet, String name)
		{
			this.foundSet = foundSet;
			this.name = name;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((foundSet == null) ? 0 : foundSet.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			UsedAggregate other = (UsedAggregate)obj;
			if (foundSet == null)
			{
				if (other.foundSet != null) return false;
			}
			else if (!foundSet.equals(other.foundSet)) return false;
			if (name == null)
			{
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "UsedAggregate [foundSet=" + foundSet + ", name=" + name + ']'; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
