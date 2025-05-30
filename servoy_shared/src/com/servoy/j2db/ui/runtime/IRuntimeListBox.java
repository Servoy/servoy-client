/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.ui.runtime;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * The <code>IRuntimeListBox</code> interface supports scripting for runtime list box components in Servoy,
 * combining functionalities for fields, read-only mode, scrolling, and valuelist items.
 *
 * @author lvostinar
 * @since 6.1
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeListBox", extendsComponent = "RuntimeComponent")
@ServoyClientSupport(mc = false, wc = true, sc = true)
public interface IRuntimeListBox extends IRuntimeField, HasRuntimeReadOnly, HasRuntimeScroll, HasRuntimeValuelistItems
{
}
