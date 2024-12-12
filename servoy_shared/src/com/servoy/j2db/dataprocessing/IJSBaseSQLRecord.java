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
 * <p><code>JSBaseSQLRecord</code> serves as the foundational type for records in SQL-based
 * foundsets, such as those in <code>JSFoundSet</code> or <code>ViewFoundSet</code>. It extends
 * <code>JSBaseRecord</code>, inheriting its base functionality while introducing features
 * tailored to SQL data handling. The <code>exception</code> property handles errors at the
 * record level, while <code>foundset</code> links the record to its originating dataset.
 * Additionally, <code>recordMarkers</code> facilitates the attachment of metadata or
 * annotations directly to records.</p>
 *
 * <p>Key methods include <code>createMarkers</code>, which generates detailed annotations for a
 * record, and <code>getChangedData</code>, which retrieves altered data within a record. The
 * <code>getPKs</code> method identifies associated primary keys, while <code>hasChangedData</code>
 * confirms if modifications exist. Editing states are managed through <code>isEditing</code>, which
 * checks if a record is in edit mode, and <code>isNew</code>, which identifies newly created
 * records. Relationship management is supported by <code>isRelatedFoundSetLoaded</code>, ensuring
 * related datasets are loaded. The <code>revertChanges</code> method enables rolling back unsaved
 * modifications.</p>
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
