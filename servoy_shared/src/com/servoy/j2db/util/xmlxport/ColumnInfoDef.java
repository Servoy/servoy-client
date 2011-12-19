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
package com.servoy.j2db.util.xmlxport;

import java.io.Serializable;

import com.servoy.j2db.persistence.ColumnInfo;

@SuppressWarnings("nls")
public final class ColumnInfoDef implements Serializable
{

	public static final String FLAGS = "flags";
	public static final String ELEMENT_TEMPLATE_PROPERTIES = "elementTemplateProperties";
	public static final String DEFAULT_FORMAT = "defaultFormat";
	public static final String VALIDATOR_NAME = "validatorName";
	public static final String VALIDATOR_PROPERTIES = "validatorProperties";
	public static final String CONVERTER_PROPERTIES = "converterProperties";
	public static final String CONVERTER_NAME = "converterName";
	public static final String FOREIGN_TYPE = "foreignType";
	public static final String DESCRIPTION = "description";
	public static final String TITLE_TEXT = "titleText";
	public static final String DATABASE_SEQUENCE_NAME = "databaseSequenceName";
	public static final String LOOKUP_VALUE = "lookupValue";
	public static final String DEFAULT_VALUE = "defaultValue";
	public static final String POST_SEQUENCE_CHARS = "postSequenceChars";
	public static final String PRE_SEQUENCE_CHARS = "preSequenceChars";
	public static final String SEQUENCE_STEP_SIZE = "sequenceStepSize";
	public static final String AUTO_ENTER_SUB_TYPE = "autoEnterSubType";
	public static final String AUTO_ENTER_TYPE = "autoEnterType";
	public static final String ALLOW_NULL = "allowNull";
	public static final String LENGTH = "length";
	public static final String DATA_TYPE = "dataType";
	public static final String CREATION_ORDER_INDEX = "creationOrderIndex";

	public String name = null;
	public int datatype = 0;
	public int length = 0;
	public int flags = 0;
	public boolean allowNull = false;
	public int autoEnterType = ColumnInfo.NO_AUTO_ENTER;
	public int autoEnterSubType = ColumnInfo.NO_SYSTEM_VALUE;
	public String preSequenceChars = null;
	public String postSequenceChars = null;
	public int sequenceStepSize = 1;
	public String defaultValue = null;
	public String lookupValue = null;
	public String databaseSequenceName = null;
	public String titleText = null;
	public String description = null;
	public String foreignType = null;
	public String converterName = null;
	public String converterProperties = null;
	public String validatorProperties = null;
	public String validatorName = null;
	public String defaultFormat = null;
	public String elementTemplateProperties = null;
	public int creationOrderIndex = -1; // used only during import; at export the value is written directly into the XML

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ColumnInfoDef)
		{
			if (name != null)
			{
				return name.equalsIgnoreCase(((ColumnInfoDef)obj).name);
			}
			return name == null && ((ColumnInfoDef)obj).name == null;
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "ColumnInfoDef[" + name + "," + datatype + "]";
	}
}