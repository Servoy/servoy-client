/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.dataui;

import com.servoy.j2db.scripting.FunctionDefinition;

/**
 * Enumeration of property editors supported by Servoy Developer to be used for bean properties.
 * 
 * @author rgansevles
 *
 * @since 5.0
 */
public enum PropertyEditorClass
{
	/**
	 * Method name property editor, supports {@link FunctionDefinition} or String properties.
	 * <p> In case of String properties, the format is either <code>globals</code>.myglobalmethod or myform.myformmethod.
	 * This string is supported by the {@link FunctionDefinition} 1-string constructor: <pre>new FunctionDefinition(myDataprovider).execute(access, null, false);</pre>
	 * <p>Supported options: {@link PropertyEditorOption#includeNone}, {@link PropertyEditorOption#includeForm},
	 * {@link PropertyEditorOption#includeGlobal}.
	 */
	method,
	/**
	 * Dataprovider name property editor, supports String properties.
	 * <p>Supported options: {@link PropertyEditorOption#includeNone}, {@link PropertyEditorOption#includeColumns},
	 *  {@link PropertyEditorOption#includeCalculations}, {@link PropertyEditorOption#includeRelatedCalculations},
	 *  {@link PropertyEditorOption#includeForm}, {@link PropertyEditorOption#includeGlobal}, 
	 *  {@link PropertyEditorOption#includeAggregates},  {@link PropertyEditorOption#includeRelatedAggregates},
	 *  {@link PropertyEditorOption#includeRelations}, {@link PropertyEditorOption#includeNestedRelations}.
	 */
	dataprovider,
	/**
	 * Relation name property editor, supports String properties.
	 * <p>Supported options: {@link PropertyEditorOption#includeNone}, {@link PropertyEditorOption#includeNestedRelations}.
	 */
	relation,
	/**
	 * Form name property editor, supports String properties.
	 * <p>Supported options: {@link PropertyEditorOption#includeNone}.
	 */
	form,
	/**
	 * Value list name property editor, supports String properties.
	 * <p>Supported options: {@link PropertyEditorOption#includeNone}.
	 */
	valuelist,
	/**
	 * Media name property editor, supports String properties.
	 * <p>Supported options: {@link PropertyEditorOption#includeNone}.
	 */
	media,
	/**
	 * Style class name property editor, supports String properties.
	 * <p>Supported options: {@link PropertyEditorOption#styleLookupName}.
	 */
	styleclass,
	/**
	 * Use the default inferred editor, use this PropertyEditorClass to set options but use the default editor.
	 */
	defaultEditor
}