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

package com.servoy.base.scripting.api;

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * @author lvostinar
 * @since 7.0
 *
 */
@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
public interface IJSHistory
{
	@ServoyClientSupport(ng = true, mc = true)
	public void back();

	@ServoyClientSupport(ng = true, mc = true)
	public void forward();

	@ServoyClientSupport(ng = false, mc = true)
	public void clear();

	@ServoyClientSupport(ng = false, mc = true)
	String getFormName(int i);

	@ServoyClientSupport(ng = false, mc = true)
	void go(int i);

	@ServoyClientSupport(ng = false, mc = true)
	int size();

	@ServoyClientSupport(ng = false, mc = true)
	int getCurrentIndex();

	@ServoyClientSupport(ng = false, mc = true)
	boolean removeIndex(int index);

	@ServoyClientSupport(ng = true, mc = true)
	boolean removeForm(String formName);
}