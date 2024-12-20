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

package com.servoy.j2db.ui;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.ui.runtime.HasRuntimeFormat;
import com.servoy.j2db.ui.runtime.HasRuntimeImageURL;

/**
 * /**
 * The <code>IScriptRenderMethodsWithOptionalProps</code> interface defines components Components that are Renderable;
 * the <code>format</code> and <code>imageUrl</code> properties are optional; so scripting Renderable will always have
 * such properties but for some components (or the form itself that for example doesn't have format and imageUrl)
 * those will not do anything.
 *
 * @author jcompagner
 * @since 6.1
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "Renderable", publicName = "Renderable")
@ServoyClientSupport(mc = false, wc = true, sc = true)
public interface IScriptRenderMethodsWithOptionalProps extends IScriptRenderMethods, HasRuntimeFormat, HasRuntimeImageURL
{
	/**
	 * @see IScriptRenderMethods
	 */
	public final String JS_RENDERABLE = "Renderable";//$NON-NLS-1$
}
