/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.sablo.specification.property.types.DefaultPropertyType;

/**
 * @author gboros
 *
 */
public abstract class ReferencePropertyType<T> extends DefaultPropertyType<T>
{
	private final List<WeakReference<T>> allRefs = new ArrayList<WeakReference<T>>();

	protected int addReference(T ref)
	{
		if (ref == null) return 0;
		int hashCode = ref.hashCode();
		WeakReference<T> wf = new WeakReference<T>(ref);
		if (!allRefs.contains(wf))
		{
			allRefs.add(wf);
		}
		return hashCode;
	}

	protected T getReference(int hashCode)
	{
		if (hashCode > 0)
		{
			for (int i = 0; i < allRefs.size(); i++)
			{
				WeakReference<T> wr = allRefs.get(i);
				T ref = wr.get();
				if (ref != null && ref.hashCode() == hashCode)
				{
					return ref;
				}
			}
		}
		return null;
	}
}
