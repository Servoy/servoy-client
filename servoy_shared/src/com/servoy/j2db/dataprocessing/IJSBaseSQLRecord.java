/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * JSBaseSQLRecord is the base type for a record from SQL based foundsets, so it represents one row of a SQL FoundSet (either a JSFoundSet or ViewFoundSet).
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSBaseSQLRecord", scriptingName = "JSBaseSQLRecord", extendsComponent = "JSBaseRecord")
public interface IJSBaseSQLRecord extends IJSBaseRecord
{
	@JSReadonlyProperty
	Exception getException();

	@JSGetter
	JSRecordMarkers getRecordMarkers();

	@JSSetter
	void setRecordMarkers(JSRecordMarkers object);

	@JSFunction
	public IJSDataSet getChangedData();

	@JSFunction
	public boolean hasChangedData();

	@JSFunction
	JSRecordMarkers createMarkers();

	@JSFunction
	public Object[] getPKs();

	@JSFunction
	public boolean isNew();

	@JSFunction
	public boolean isEditing();

	@JSFunction
	boolean isRelatedFoundSetLoaded(String relationName);

	@JSFunction
	public void revertChanges();
}
