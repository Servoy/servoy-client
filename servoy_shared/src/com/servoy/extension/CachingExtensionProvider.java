/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Caches meta-data & contents of an IExtensionProvider for fast reuse.
 * Some ExtensionProviders will acquire data through network connections, some will parse it from disk, but this class
 * helps to avoid repeating resource & time consuming operations.
 * 
 * @author acostescu
 */
public abstract class CachingExtensionProvider implements IExtensionProvider
{

	// keeps track of the version intervals already cached per extension id; the list must be a sorted List
	protected final Map<String, List<VersionInterval>> cachedDepencencyMetadataVersions = new HashMap<String, List<VersionInterval>>();

	// all cached versions of DependencyMetadata info per extension id
	protected final Map<String, List<DependencyMetadata>> cachedDependencyMetadata = new HashMap<String, List<DependencyMetadata>>();

	public DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency)
	{
		boolean cached = false;
		List<VersionInterval> cachedIntervals = cachedDepencencyMetadataVersions.get(extensionDependency.id);
		if (cachedIntervals != null)
		{
			for (int i = cachedIntervals.size() - 1; !cached && i >= 0; i--)
			{
				VersionInterval cachedInterval = cachedIntervals.get(i);
				if (compareVersions(cachedInterval.max, extensionDependency.minVersion, false, true) < 0)
				{
					i = -1; // no use searching further (backwards) in the sorted list
				}
				else
				{
					// so cached.max >= interval.min
					if (compareVersions(cachedInterval.min, extensionDependency.maxVersion, true, false) <= 0)
					{
						// so cached.min <= interval.max && cached.max >= interval.min
						VersionInterval intersection = intersection(cachedInterval, new VersionInterval(extensionDependency.minVersion,
							extensionDependency.maxVersion));
						if (intersection == null || intersection.min != extensionDependency.minVersion || intersection.max != extensionDependency.maxVersion)
						{
							i = -1;
						}
						else
						{
							cached = true;
						}
					} // else cached.min > interval.max; unrelated intervals
				}
			}
		}

		DependencyMetadata[] result;
		if (cached)
		{
			List<DependencyMetadata> resultList = new ArrayList<DependencyMetadata>();
			List<DependencyMetadata> cachedVersions = cachedDependencyMetadata.get(extensionDependency.id);
			if (cachedVersions != null)
			{
				for (DependencyMetadata version : cachedVersions)
				{
					if (VersionStringUtils.belongsToInterval(version.version, extensionDependency.minVersion, extensionDependency.maxVersion))
					{
						resultList.add(version);
					}
				}
			}

			result = resultList.size() == 0 ? null : resultList.toArray(new DependencyMetadata[resultList.size()]);
		}
		else
		{
			result = getDependencyMetadataImpl(extensionDependency);
			// update cache
			addCachedDependencyMetadataVersionInterval(extensionDependency.id, new VersionInterval(extensionDependency.minVersion,
				extensionDependency.maxVersion));
			if (result != null)
			{
				for (DependencyMetadata version : result)
				{
					cacheDependencyMetadataVersion(version);
				}
			}
		}

		return result;
	}

	protected abstract DependencyMetadata[] getDependencyMetadataImpl(ExtensionDependencyDeclaration extensionDependency);

	public void flushCache()
	{
		cachedDepencencyMetadataVersions.clear();
		cachedDependencyMetadata.clear();
	}

	/**
	 * @return true if the the given extension version was cached, false if it was not (maybe it was already cached?)
	 */
	protected boolean cacheDependencyMetadataVersion(DependencyMetadata version)
	{
		List<DependencyMetadata> cachedVersions = cachedDependencyMetadata.get(version.id);
		if (cachedVersions == null)
		{
			cachedVersions = new ArrayList<DependencyMetadata>();
			cachedDependencyMetadata.put(version.id, cachedVersions);
		}

		// do not allow duplicates
		boolean found = false;
		for (DependencyMetadata dmd : cachedVersions)
		{
			if (dmd.id.equals(version.id) && dmd.version.equals(version.version))
			{
				found = true;
				break;
			}
		}

		if (!found) cachedVersions.add(version);
		return !found;
	}

	protected void addCachedDependencyMetadataVersionInterval(String extensionId, VersionInterval interval)
	{
		List<VersionInterval> cachedVersionIntervals = cachedDepencencyMetadataVersions.get(extensionId);
		if (cachedVersionIntervals == null)
		{
			cachedVersionIntervals = new ArrayList<CachingExtensionProvider.VersionInterval>();
			cachedDepencencyMetadataVersions.put(extensionId, cachedVersionIntervals);
			cachedVersionIntervals.add(interval);
		}
		else
		{
			VersionInterval intervalToAdd = interval;

			// merge this new interval with existing intervals
			int cacheSize = cachedVersionIntervals.size();
			int insertIndex = cacheSize;
			for (int i = 0; insertIndex == cacheSize && i < cacheSize; i++)
			{
				if (compareVersions(intervalToAdd.min, cachedVersionIntervals.get(i).min, true, true) >= 0)
				{
					insertIndex = i;
				}
			}

			// we found the place where this needs to be added based on minVer; we must check the previous interval and
			// the following intervals to see if any of the intervals need merging/removing
			if (insertIndex - 1 > 0)
			{
				VersionInterval previousInterval = cachedVersionIntervals.get(insertIndex - 1);
				if (compareVersions(intervalToAdd.min, previousInterval.max, true, false) <= 0)
				{
					intervalToAdd = reunion(previousInterval, intervalToAdd);
					cachedVersionIntervals.remove(previousInterval);
					insertIndex--;
				}
				int nextIntervalIndex = insertIndex + 1;
				while (nextIntervalIndex < cachedVersionIntervals.size())
				{
					VersionInterval nextInterval = cachedVersionIntervals.get(nextIntervalIndex);
					if (compareVersions(nextInterval.min, intervalToAdd.max, true, false) <= 0)
					{
						intervalToAdd = reunion(nextInterval, intervalToAdd);
						cachedVersionIntervals.remove(nextIntervalIndex);
					}
					else
					{
						// no more overlapping intervals
						nextIntervalIndex = cachedVersionIntervals.size();
					}
				}
				cachedVersionIntervals.add(insertIndex, intervalToAdd);
			}

		}
	}

	private VersionInterval reunion(VersionInterval interval1, VersionInterval interval2)
	{
		// needs merging
		int compMin = compareVersions(interval1.min, interval2.min, true, true);
		int compMax = compareVersions(interval1.max, interval2.max, false, false);
		String min = compMin < 0 ? interval1.min : (compMin == 0 ? (VersionStringUtils.isExclusive(interval1.min) ? interval2.min : interval1.min)
			: interval2.min); // use inclusive if possible and equal
		String max = compMax > 0 ? interval1.max : (compMax == 0 ? (VersionStringUtils.isExclusive(interval1.max) ? interval2.max : interval1.max)
			: interval2.max); // use inclusive if possible and equal

		return new VersionInterval(min, max);
	}

	private VersionInterval intersection(VersionInterval interval1, VersionInterval interval2)
	{
		// needs merging
		int compMin = compareVersions(interval1.min, interval2.min, true, true);
		int compMax = compareVersions(interval1.max, interval2.max, false, false);
		String min = compMin < 0 ? interval2.min : (compMin == 0 ? (VersionStringUtils.isExclusive(interval1.min) ? interval1.min : interval2.min)
			: interval1.min); // use exclusive if possible and equal
		String max = compMax > 0 ? interval2.max : (compMax == 0 ? (VersionStringUtils.isExclusive(interval1.max) ? interval1.max : interval2.max)
			: interval1.max); // use exclusive if possible and equal

		int validCompare = compareVersions(min, max, true, false);
		if ((validCompare > 0) || (validCompare == 0 && (VersionStringUtils.isExclusive(min) || VersionStringUtils.isExclusive(max))))
		{
			return null;
		}
		return new VersionInterval(min, max);
	}

	// compares two min/max versions; does not differentiate between inclusive and exclusive versions
	private int compareVersions(String ver1, String ver2, boolean ver1Min, boolean ver2Min)
	{
		int result;
		if (ver1 == VersionStringUtils.UNBOUNDED && ver2 == VersionStringUtils.UNBOUNDED)
		{
			result = (ver1Min ^ ver2Min) ? (ver1Min ? -1 : 1) : 0;
		}
		else if (ver1 == VersionStringUtils.UNBOUNDED) // and the other is not
		{
			result = (ver1Min ? -1 : 1);
		}
		else if (ver2 == VersionStringUtils.UNBOUNDED) // and the other is not
		{
			result = (ver2Min ? 1 : -1);
		}
		else
		{
			// neither is unbounded
			result = VersionStringUtils.compareVersions(ver1, ver2);
		}
		return result;
	}

	protected class VersionInterval
	{
		public final String min;
		public final String max;

		public VersionInterval(String min, String max)
		{
			this.min = min;
			this.max = max;
		}

		@Override
		@SuppressWarnings("nls")
		public String toString()
		{
			return "{" + min + "," + max + "}";
		}

	}

}
