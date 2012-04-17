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

package com.servoy.extension.dependency;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.servoy.extension.VersionStringUtils;

/**
 * A path chooser able to use multiple tree path comparators to finally reach it's chosen path.
 * For example if it is set to use 3 comparators c1, c2 and c3, it sorts based on c1, then in case of c1 equals sorts on c2 then on c3.
 * @author acostescu
 */
public class MultipleCriteriaChooser implements IDependencyPathChooser
{

	protected Comparator<DependencyPath>[] comparators;

	public MultipleCriteriaChooser()
	{
		this(null);
	}

	public MultipleCriteriaChooser(Comparator<DependencyPath> comparators[])
	{
		this.comparators = comparators;
	}

	@SuppressWarnings("unchecked")
	public DependencyPath pickResolvePath(List<DependencyPath> allResolvedPaths)
	{
		final Comparator<DependencyPath> comps[];
		if (comparators == null)
		{
			comps = new Comparator[] { new LibConflictPathComparator(), new ReplacementPathComparator(), new ShortestPathComparator(), new HigherVersionComparator() };
		}
		else
		{
			comps = comparators;
		}

		Collections.sort(allResolvedPaths, new Comparator<DependencyPath>()
		{
			public int compare(DependencyPath o1, DependencyPath o2)
			{
				int result = 0;
				int i = 0;
				while (result == 0 && i < comps.length)
				{
					result = comps[i].compare(o1, o2);
					i++;
				}
				return result;
			}
		});
		return allResolvedPaths.get(0);
	}

	/**
	 * Paths with no valid lib choices are first, then paths with choices, then paths with conflicts.
	 */
	public static class LibConflictPathComparator implements Comparator<DependencyPath>
	{

		public int compare(DependencyPath o1, DependencyPath o2)
		{
			int result;
			boolean o1HasChoices = false;
			boolean o1HasConflicts = false;
			boolean o2HasChoices = false;
			boolean o2HasConflicts = false;

			if (o1.libChoices != null)
			{
				o1HasChoices = true;
				for (LibChoice lc : o1.libChoices)
				{
					if (lc.conflict) o1HasConflicts = true;
				}
			}

			if (o2.libChoices != null)
			{
				o2HasChoices = true;
				for (LibChoice lc : o2.libChoices)
				{
					if (lc.conflict) o2HasConflicts = true;
				}
			}

			if (o1HasChoices)
			{
				if (o2HasChoices)
				{
					if (o1HasConflicts)
					{
						if (o2HasConflicts) result = 0;
						else result = 1;
					}
					else
					{
						if (o2HasConflicts) result = -1;
						else result = 0;
					}
				}
				else
				{
					result = 1;
				}
			}
			else
			{
				if (o2HasChoices)
				{
					result = -1;
				}
				else
				{
					result = 0;
				}
			}

			return result;
		}

	}

	/**
	 * No upgrades/downgrades first, then only upgrades, then paths which contain downgrades but no upgrades, then both.<br>
	 * First extension node in the path is ignored.
	 */
	public static class ReplacementPathComparator implements Comparator<DependencyPath>
	{

		public int compare(DependencyPath o1, DependencyPath o2)
		{
			int result;
			boolean o1Upgrade = false;
			boolean o1Downgrade = false;
			boolean o2Upgrade = false;
			boolean o2Downgrade = false;

			for (int i = o1.extensionPath.length - 1; i > 0; i--) // ignore first
			{
				if (o1.extensionPath[i].resolveType == ExtensionNode.UPGRADE_RESOLVE) o1Upgrade = true;
				else if (o1.extensionPath[i].resolveType == ExtensionNode.DOWNGRADE_RESOLVE) o1Downgrade = true;
			}

			for (int i = o2.extensionPath.length - 1; i > 0; i--) // ignore first
			{
				if (o2.extensionPath[i].resolveType == ExtensionNode.UPGRADE_RESOLVE) o2Upgrade = true;
				else if (o2.extensionPath[i].resolveType == ExtensionNode.DOWNGRADE_RESOLVE) o2Downgrade = true;
			}

			if (o1Upgrade)
			{
				if (o2Upgrade)
				{
					if (o1Downgrade)
					{
						if (o2Downgrade) result = 0; // UD == UD
						else result = 1; // UD > U
					}
					else
					{
						if (o2Downgrade) result = -1; // U < UD
						else result = 0; // U = U
					}
				}
				else
				{
					if (o1Downgrade)
					{
						result = 1; // UD > D, UD > S
					}
					else
					{
						if (o2Downgrade) result = -1; // U < D
						else result = 1; // U > S
					}
				}
			}
			else
			{
				if (o2Upgrade)
				{
					if (o1Downgrade)
					{
						if (o2Downgrade) result = -1; // D < UD
						else result = 1; // D > U
					}
					else
					{
						result = -1; // S < U,  S < UD
					}
				}
				else
				{
					if (o1Downgrade)
					{
						if (o2Downgrade) result = 0; // D = D
						else result = 1; // D > S
					}
					else
					{
						if (o2Downgrade) result = -1; // S < D
						else result = 0; // S == S
					}
				}
			}

			return result;
		}

	}

	/**
	 * While going through the two paths, as long as they have the same extensions but different versions, the higher version is first.
	 */
	public static class HigherVersionComparator implements Comparator<DependencyPath>
	{

		public int compare(DependencyPath o1, DependencyPath o2)
		{
			int i = 0;
			int result;
			do
			{
				if (o1.extensionPath[i].id.equals(o2.extensionPath[i].id))
				{
					result = VersionStringUtils.compareVersions(o1.extensionPath[i].version, o2.extensionPath[i].version);
				}
				else
				{
					result = o1.extensionPath[i].id.compareTo(o2.extensionPath[i].id);
				}
				i++;
			}
			while (result == 0 && i < o1.extensionPath.length && i < o2.extensionPath.length);

			return result;
		}

	}

	/**
	 * Shorter path is always first.
	 */
	public static class ShortestPathComparator implements Comparator<DependencyPath>
	{

		public int compare(DependencyPath o1, DependencyPath o2)
		{
			return o1.extensionPath.length - o2.extensionPath.length;
		}

	}

}