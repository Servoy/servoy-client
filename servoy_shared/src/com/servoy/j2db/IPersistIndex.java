/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db;

import java.util.Iterator;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportScope;


/**
 * @author jcompagner
 * @since 8.4
 *
 */
public interface IPersistIndex
{

	/**
	 * @param uuid
	 * @return the perist found i the index.
	 */
	IPersist getPersistByUUID(String uuid);

	/**
	 * @param uuid
	 * @return the perist found i the index.
	 */
	<T extends IPersist> T getPersistByUUID(String uuid, Class<T> clz);

	/**
	 * This cache assumes that for the given persist class the persist that are in the cache have unique names.
	 * So Forms/ValueList/Relations. But this won't work for Fields or WebComponents because those are only unique by there container Form.
	 * @param name
	 *  @return the perist found having that name
	 */
	<T extends IPersist> T getPersistByName(String name, Class<T> persistClass);

	/**
	 * @param id
	 * @return the perist found i the index.
	 */
	<T extends IPersist> T getPersistByID(int id, Class<T> clz);

	/**
	 * @param scopeName
	 * @param baseName
	 * @return
	 */
	ISupportScope getSupportScope(String scopeName, String baseName);

	public <T extends IPersist> Iterator<T> getIterableFor(Class<T> clz);

	void destroy();

	void reload();

}