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

package com.servoy.j2db.querybuilder.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.DefaultJavaScope;

/**
 * @author rgansevles
 *
 * @since 6.1
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBColumns extends DefaultJavaScope
{
	private final static Map<String, NativeJavaMethod> jsFunctions = getJsFunctions(QBColumns.class);

	QBColumns(Scriptable scriptParent)
	{
		// use a LinkedHashMap for allVars so that columns are kept in order as they were added
		super(scriptParent, LinkedHashMap::new, jsFunctions);
	}

}
