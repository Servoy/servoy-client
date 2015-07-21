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
package com.servoy.j2db.persistence;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Utils;

/**
 * Add some info to a column
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "ColumnInfo")
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class ColumnInfo implements Serializable, ISupportHTMLToolTipText
{
	public static final long serialVersionUID = -6167880772070620512L;

	//auto_enter_type
	public static final int NO_AUTO_ENTER = 0;
	public static final int SYSTEM_VALUE_AUTO_ENTER = 1;
	public static final int SEQUENCE_AUTO_ENTER = 2;
	public static final int CUSTOM_VALUE_AUTO_ENTER = 3;
	public static final int CALCULATION_VALUE_AUTO_ENTER = 4;
	public static final int LOOKUP_VALUE_AUTO_ENTER = 5;

	//auto_enter_sub_types to use if auto_enter_type == SYSTEM_VALUE_AUTO_ENTER
	public static final int NO_SYSTEM_VALUE = 0;
	public static final int SYSTEM_VALUE_CREATION_DATETIME = 1;
	public static final int SYSTEM_VALUE_CREATION_USERNAME = 2;
	public static final int SYSTEM_VALUE_MODIFICATION_DATETIME = 3;
	public static final int SYSTEM_VALUE_MODIFICATION_USERNAME = 4;
	public static final int DATABASE_MANAGED = 5;
	public static final int SYSTEM_VALUE_CREATION_USERUID = 6;
	public static final int SYSTEM_VALUE_MODIFICATION_USERUID = 7;
	public static final int SYSTEM_VALUE_CREATION_SERVER_DATETIME = 8;
	public static final int SYSTEM_VALUE_MODIFICATION_SERVER_DATETIME = 9;

	//auto_enter_sub_types to use if auto_enter_type == SEQUENCE_AUTO_ENTER
	public static final int NO_SEQUENCE_SELECTED = -1;
	public static final int SERVOY_SEQUENCE = 0;
	public static final int DATABASE_SEQUENCE = 1;
	public static final int DATABASE_IDENTITY = 2;
	public static final int UUID_GENERATOR = 3;

	public static final int[] allDefinedSeqTypes = { NO_SEQUENCE_SELECTED, SERVOY_SEQUENCE, DATABASE_SEQUENCE, DATABASE_IDENTITY, UUID_GENERATOR };
	public static final int[] allDefinedSystemValues = { NO_SYSTEM_VALUE, SYSTEM_VALUE_CREATION_DATETIME, SYSTEM_VALUE_CREATION_USERNAME, SYSTEM_VALUE_MODIFICATION_DATETIME, SYSTEM_VALUE_MODIFICATION_USERNAME, DATABASE_MANAGED, SYSTEM_VALUE_CREATION_USERUID, SYSTEM_VALUE_MODIFICATION_USERUID, SYSTEM_VALUE_CREATION_SERVER_DATETIME, SYSTEM_VALUE_MODIFICATION_SERVER_DATETIME };

	private String element_template_properties = null;

	/*
	 * This property is not saved but determined on the fly from the db.
	 */
	private String databaseDefaultValue = null;

	private int columninfo_id;
	private boolean storedPersistently;
	private boolean changed;
	private int autoEnterType = NO_AUTO_ENTER;
	private int autoEnterSubType = NO_SEQUENCE_SELECTED;
	private String databaseSequenceName = null;
	private String preSequenceChars = null;
	private String postSequenceChars = null;
	private long nextSequence = 1;
	private int sequenceStepSize = 1;
	private String defaultValue = null;
	private String lookupValue = null;
	private String titleText = null;
	private String dataProviderID = null;
	private String description = null;
	private String converterProperties = null;
	private String converterName = null;
	private String foreignType = null;
	private String validatorProperties = null;
	private String validatorName = null;
	private String defaultFormat = null;
	private Integer containsMetaData = null;

	private ColumnType configuredColumnType; // as configured by developer
	private List<ColumnType> compatibleColumnTypes; // compatible with configured


	private int flags = 0;

	public ColumnInfo(int columninfo_id, boolean storedPersistently)
	{
		this.columninfo_id = columninfo_id;
		this.storedPersistently = storedPersistently;

		if (!storedPersistently) changed = true;
	}

	public int getID()
	{
		return columninfo_id;
	}

	public void setID(int id)
	{
		columninfo_id = id;
	}

	public boolean isStoredPersistently()
	{
		return storedPersistently;
	}

	public void setStoredPersistently(boolean storedPersistently)
	{
		this.storedPersistently = storedPersistently;
	}

	public void flagChanged()
	{
		changed = true;
	}

	public boolean isChanged()
	{
		return changed;
	}

	public void flagStored()
	{
		changed = false;
		storedPersistently = true;
	}

	/**
	 * Flag that shows if the column is excluded. Excluded columns are ignored by Servoy and are not
	 * visible as data providers.
	 */
	public boolean isExcluded()
	{
		return hasFlag(Column.EXCLUDED_COLUMN);
	}

	public boolean isDBManaged()
	{
		return (autoEnterType == SYSTEM_VALUE_AUTO_ENTER && autoEnterSubType == DATABASE_MANAGED);
	}

	public boolean isDBSequence()
	{
		return (autoEnterType == SEQUENCE_AUTO_ENTER && autoEnterSubType == DATABASE_SEQUENCE);
	}

	public void setDatabaseDefaultValue(String value)
	{
		databaseDefaultValue = value;
	}

	/**
	 * The database default value that is used when autoenter is set to database default.
	 */
	public String getDatabaseDefaultValue()
	{
		return databaseDefaultValue;
	}

	/*
	 * _____________________________________________________________ Property methods
	 */

	/**
	 * The type of autoenter configured for the column. Can be one of: none,
	 * system value, sequence, custom value or lookup value.
	 * 
	 */
	public int getAutoEnterType()
	{
		return autoEnterType;
	}

	public void setAutoEnterType(int t)
	{
		autoEnterType = t;
	}

	/**
	 * The subtype of autoenter configured for the column. The available options depend
	 * on the type of autoenter. 
	 * 
	 * If autoenter is set to system value, then the subtype can be one of: none, creation datetime,
	 * creation username, modification datetime, modification username, database managed,
	 * creation user uid, modification user uid, creation server datetime or modification server datetime.
	 * 
	 * If autoenter is set to sequence, then the subtype can be one of: none, Servoy sequence,
	 * database sequence, database identity or universally unique identifier.
	 */
	public int getAutoEnterSubType()
	{
		return autoEnterSubType;
	}

	public void setAutoEnterSubType(int t)
	{
		autoEnterSubType = t;
	}

	/**
	 * The database sequence name that is used when autoenter is set to sequence and autoenter subtype
	 * is set to database sequence.
	 */
	public String getDatabaseSequenceName()
	{
		return databaseSequenceName;
	}

	public void setDatabaseSequenceName(String databaseSequenceName)
	{
		if (databaseSequenceName != null)
		{
			databaseSequenceName = databaseSequenceName.trim();
			if (databaseSequenceName.length() == 0)
			{
				databaseSequenceName = null;
			}
			else if (databaseSequenceName.length() > 50)
			{
				databaseSequenceName = databaseSequenceName.substring(0, 50);
			}
		}
		this.databaseSequenceName = databaseSequenceName;
	}

	public String getPreSequenceChars()
	{
		return preSequenceChars;
	}

	public void setPreSequenceChars(String s)
	{
		preSequenceChars = getNonEmptyValue(s);
	}

	public String getPostSequenceChars()
	{
		return postSequenceChars;
	}

	public void setPostSequenceChars(String s)
	{
		postSequenceChars = getNonEmptyValue(s);
	}

	/**
	 * this is normally a snapshot of the sequence when running the application, especially when getNextSequence on Repository is called by clients
	 */
	public long getNextSequence()
	{
		return nextSequence;
	}

	public void setNextSequence(long l)
	{
		nextSequence = l;
	}

	public int getSequenceStepSize()
	{
		return sequenceStepSize;
	}

	public void setSequenceStepSize(int s)
	{
		sequenceStepSize = s;
	}

	/**
	 * The value that is used when autoenter is set to custom value.
	 */
	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String s)
	{
		defaultValue = getNonEmptyValue(s);
	}

	/**
	 * The lookup value that is used when autotype is set to lookup.
	 */
	public String getLookupValue()
	{
		return lookupValue;
	}

	public void setLookupValue(String s)
	{
		lookupValue = getNonEmptyValue(s);
	}

	/**
	 * The title of the column.
	 */
	public String getTitleText()
	{
		return titleText;
	}

	public void setTitleText(String s)
	{
		titleText = getNonEmptyValue(s);
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/**
	 * The description of the column.
	 */
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String s)
	{
		description = getNonEmptyValue(s);
	}

	/**
	 * The properties of the converter used for this column.
	 */
	public String getConverterProperties()
	{
		return converterProperties;
	}

	public void setConverterProperties(String s)
	{
		converterProperties = getNonEmptyValue(s);
	}

	/**
	 * The name of the converter used for this column.
	 */
	public String getConverterName()
	{
		return converterName;
	}

	public void setConverterName(String s)
	{
		converterName = getNonEmptyValue(s);
	}

	/**
	 * The foreign type of the column. It is used for foreign key columns, to hold the foreign table they point to.
	 */
	public String getForeignType()
	{
		return foreignType;
	}

	public void setForeignType(String s)
	{
		foreignType = getNonEmptyValue(s);
	}

	/**
	 * The properties of the validator used for the column.
	 */
	public String getValidatorProperties()
	{
		return validatorProperties;
	}

	public void setValidatorProperties(String s)
	{
		validatorProperties = getNonEmptyValue(s);
	}

	/**
	 * The name of the validator used for the column.
	 */
	public String getValidatorName()
	{
		return validatorName;
	}

	public void setValidatorName(String s)
	{
		validatorName = getNonEmptyValue(s);
	}

	/**
	 * The default format of the column. 
	 * Currently only strings or numbers are supported.
	 */
	public String getDefaultFormat()
	{
		return this.defaultFormat;
	}

	public void setDefaultFormat(String s)
	{
		this.defaultFormat = getNonEmptyValue(s);
	}

	/**
	 * @param i
	 */
	public static String getSeqDisplayTypeString(int i)
	{
		switch (i)
		{
			case SERVOY_SEQUENCE :
				return "servoy seq"; //$NON-NLS-1$
			case DATABASE_SEQUENCE :
				return "db seq"; //$NON-NLS-1$
			case DATABASE_IDENTITY :
				return "db identity"; //$NON-NLS-1$
			case UUID_GENERATOR :
				return "uuid generator"; //$NON-NLS-1$
			case NO_SEQUENCE_SELECTED :
			default :
				return "none"; //$NON-NLS-1$
		}
	}

	public static String getAutoEnterTypeString(int i)
	{
		switch (i)
		{
			case SYSTEM_VALUE_AUTO_ENTER :
				return "System Value"; //$NON-NLS-1$
			case SEQUENCE_AUTO_ENTER :
				return "Sequence Value"; //$NON-NLS-1$
			case CUSTOM_VALUE_AUTO_ENTER :
				return "Custom Value"; //$NON-NLS-1$
			case CALCULATION_VALUE_AUTO_ENTER :
				return "Calculation Value"; //$NON-NLS-1$
			case LOOKUP_VALUE_AUTO_ENTER :
				return "Related Value"; //$NON-NLS-1$
			case NO_AUTO_ENTER :
			default :
				return "none"; //$NON-NLS-1$
		}
	}

	public String getAutoEnterSubTypeString(int mainType, int subType)
	{
		if (mainType == SYSTEM_VALUE_AUTO_ENTER)
		{
			switch (subType)
			{
				case SYSTEM_VALUE_CREATION_DATETIME :
					return "creation datetime"; //$NON-NLS-1$
				case SYSTEM_VALUE_CREATION_USERNAME :
					return "creation username"; //$NON-NLS-1$
				case SYSTEM_VALUE_MODIFICATION_DATETIME :
					return "modification datetime"; //$NON-NLS-1$
				case SYSTEM_VALUE_MODIFICATION_USERNAME :
					return "modification username"; //$NON-NLS-1$
				case DATABASE_MANAGED :
					return "database managed"; //$NON-NLS-1$
				case SYSTEM_VALUE_CREATION_USERUID :
					return "creation user uid"; //$NON-NLS-1$
				case SYSTEM_VALUE_MODIFICATION_USERUID :
					return "modification user uid"; //$NON-NLS-1$
				case SYSTEM_VALUE_CREATION_SERVER_DATETIME :
					return "creation server datetime"; //$NON-NLS-1$
				case SYSTEM_VALUE_MODIFICATION_SERVER_DATETIME :
					return "modification server datetime"; //$NON-NLS-1$

				case NO_SYSTEM_VALUE :
				default :
					return "none"; //$NON-NLS-1$
			}
		}
		else if (mainType == SEQUENCE_AUTO_ENTER)
		{
			switch (subType)
			{
				case SERVOY_SEQUENCE :
					return "Servoy sequence"; //$NON-NLS-1$
				case DATABASE_SEQUENCE :
					return "database sequence"; //$NON-NLS-1$
				case DATABASE_IDENTITY :
					return "database identity"; //$NON-NLS-1$
				case UUID_GENERATOR :
					return "universally unique identifier"; //$NON-NLS-1$

				case NO_SEQUENCE_SELECTED :
				default :
					return "none"; //$NON-NLS-1$
			}
		}
		else if (mainType == CUSTOM_VALUE_AUTO_ENTER)
		{
			return getDefaultValue();
		}
//		else if (mainType == CALCULATION_VALUE_AUTO_ENTER)
//		{
//			return getDefaultValueCalculationScript();
//		}
		else if (mainType == LOOKUP_VALUE_AUTO_ENTER)
		{
			return getLookupValue();
		}
		return ""; //$NON-NLS-1$
	}

	public String toHTML()
	{
		return getTextualPropertyInfo(true);
	}

	public String getTextualPropertyInfo(boolean html)
	{
		StringBuffer sb = new StringBuffer();
		if (html) sb.append("<html>"); //$NON-NLS-1$
		if (getAutoEnterType() != NO_AUTO_ENTER)
		{
			if (html) sb.append("<B>Auto Enter: </B>"); //$NON-NLS-1$
			else sb.append("AE: "); //$NON-NLS-1$
			sb.append(getAutoEnterTypeString(autoEnterType));
			String sub = getAutoEnterSubTypeString(autoEnterType, autoEnterSubType);
			if (sub != null && sub.length() > 0)
			{
				sb.append(","); //$NON-NLS-1$
				sb.append(sub);
				sb.append(" ");
			}
		}
		if (titleText != null && titleText.length() > 0)
		{
			if (html) sb.append("<B>Title: </B>"); //$NON-NLS-1$
			else sb.append("T: "); //$NON-NLS-1$
			sb.append(titleText);
			if (html) sb.append("<br>"); //$NON-NLS-1$
			else sb.append(",");//$NON-NLS-1$
		}
		if (defaultFormat != null && defaultFormat.length() > 0)
		{
			if (html) sb.append("<B>Default Format: </B> "); //$NON-NLS-1$
			else sb.append("DF: "); //$NON-NLS-1$
			sb.append(defaultFormat);
			if (html) sb.append("<br>"); //$NON-NLS-1$
			else sb.append(",");//$NON-NLS-1$
		}
		if (foreignType != null && foreignType.length() > 0)
		{
			if (html) sb.append("<B>Foreign Type: </B> "); //$NON-NLS-1$
			else sb.append("FT: "); //$NON-NLS-1$
			sb.append(foreignType);
			if (html) sb.append("<br>"); //$NON-NLS-1$
			else sb.append(",");//$NON-NLS-1$
		}

		if (validatorName != null)
		{
			if (html) sb.append("<B>Validator: </B>"); //$NON-NLS-1$
			else sb.append("V: "); //$NON-NLS-1$
			sb.append(validatorName);
			if (html) sb.append("<br>"); //$NON-NLS-1$
			else sb.append(",");//$NON-NLS-1$
		}
		if (converterName != null)
		{
			if (html) sb.append("<B>Converter: </B>"); //$NON-NLS-1$
			else sb.append("C: "); //$NON-NLS-1$
			sb.append(converterName);
			if (html) sb.append("<br>"); //$NON-NLS-1$
			else sb.append(",");//$NON-NLS-1$
		}
		if (html) sb.append("</html>"); //$NON-NLS-1$

		if (html && sb.length() == 13) return null; //= empty html

		return sb.toString();
	}

	public boolean hasSequence()
	{
		if (getAutoEnterType() == SEQUENCE_AUTO_ENTER)
		{
			if (getAutoEnterSubType() != NO_SEQUENCE_SELECTED)
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasSystemValue()
	{
		if (getAutoEnterType() == SYSTEM_VALUE_AUTO_ENTER)
		{
			if (getAutoEnterSubType() != NO_SYSTEM_VALUE)
			{
				return true;
			}
		}
		return false;
	}

	public int getFlags()
	{
		return flags;
	}

	/** Should always called via Column */
	public void setFlags(int flags)
	{
		this.flags = flags;
	}

	/**
	 * Set or clear a flag.
	 * 
	 * @param flag
	 * @param set
	 */
	public void setFlag(int flag, boolean set)
	{
		setFlags(set ? (getFlags() | flag) : (getFlags() & ~flag));
	}

	/**
	 * @param flag
	 */
	public boolean hasFlag(int flag)
	{
		return (flags & flag) != 0;
	}

	/**
	 * Returns true if the auto enter type specified has the same semantics as the auto enter type of this column info.
	 * 
	 */
	public boolean hasSameAutoEnterType(int autoEnterType2, int autoEnterSubType2, String databaseSequenceName2)
	{

		if (this.autoEnterType == NO_AUTO_ENTER)
		{
			if (autoEnterType2 == SEQUENCE_AUTO_ENTER)
			{
				return autoEnterSubType2 == NO_SEQUENCE_SELECTED;
			}
			if (autoEnterType2 == SYSTEM_VALUE_AUTO_ENTER)
			{
				return autoEnterSubType2 == NO_SYSTEM_VALUE;
			}
			return autoEnterSubType2 == NO_AUTO_ENTER;
		}
		if (autoEnterType2 == NO_AUTO_ENTER)
		{
			if (this.autoEnterType == SEQUENCE_AUTO_ENTER)
			{
				return this.autoEnterSubType == NO_SEQUENCE_SELECTED;
			}
			if (this.autoEnterType == SYSTEM_VALUE_AUTO_ENTER)
			{
				return this.autoEnterSubType == NO_SYSTEM_VALUE;
			}
			return this.autoEnterType == NO_AUTO_ENTER;
		}
		if (this.autoEnterType == SEQUENCE_AUTO_ENTER && autoEnterType2 == SEQUENCE_AUTO_ENTER && this.autoEnterSubType == DATABASE_SEQUENCE &&
			autoEnterSubType2 == DATABASE_SEQUENCE)
		{
			// if the sequence name was changed, the auto enter is different
			return Utils.stringSafeEquals(this.databaseSequenceName, databaseSequenceName2);
		}
		return autoEnterType2 == this.autoEnterType && autoEnterSubType2 == this.autoEnterSubType;
	}

	public String getElementTemplateProperties()
	{
		return element_template_properties;
	}

	public void setElementTemplateProperties(String s)
	{
		element_template_properties = getNonEmptyValue(s);
	}

	/**
	 * @return the containsMetaData
	 */
	public Integer getContainsMetaData()
	{
		return containsMetaData;
	}

	/**
	 * @param containsMetaData the containsMetaData to set
	 */
	public void setContainsMetaData(Integer containsMetaData)
	{
		this.containsMetaData = containsMetaData;
	}

	/**
	 * Set the column type as configured by developer
	 */
	public void setConfiguredColumnType(ColumnType columnType)
	{
		this.configuredColumnType = Column.checkColumnType(columnType);
	}

	/**
	 * Get the column type as configured by developer.
	 * <p>Note, do not call this method directly, use Column.getConfiguredColumnType() instead so that in case of 
	 * old column info (with configured type not set), there will be a fallback to the db column type.
	 * 
	 * @see Column#getConfiguredColumnType()
	 */
	public ColumnType getConfiguredColumnType()
	{
		return configuredColumnType;
	}

	/**
	 * @return the compatibleColumnTypes
	 */
	public List<ColumnType> getCompatibleColumnTypes()
	{
		return compatibleColumnTypes == null ? null : Collections.unmodifiableList(compatibleColumnTypes);
	}

	/**
	 * Check if the column type is listed as compatible
	 */
	public boolean isCompatibleColumnType(ColumnType columnType)
	{
		return columnType != null && (columnType.equals(configuredColumnType) || (compatibleColumnTypes != null && compatibleColumnTypes.contains(columnType)));
	}

	/**
	 * @param compatibleColumnTypes the compatibleColumnTypes to set
	 */
	public void setCompatibleColumnTypes(List<ColumnType> compatibleColumnTypes)
	{
		this.compatibleColumnTypes = compatibleColumnTypes == null ? null : new ArrayList<ColumnType>(compatibleColumnTypes);
	}

	/**
	 * @param columnType
	 */
	public void addCompatibleColumnType(ColumnType columnType)
	{
		if (compatibleColumnTypes == null)
		{
			compatibleColumnTypes = new ArrayList<ColumnType>();
		}
		if (!compatibleColumnTypes.contains(columnType))
		{
			compatibleColumnTypes.add(columnType);
		}
	}

	/**
	 * Turns strings with no content (blank spaces are not considered content) into null. Returns other strings unchanged.
	 * 
	 * @param s the string.
	 * @return null if there is no content in string, s otherwise.
	 */
	private String getNonEmptyValue(String s)
	{
		if (s == null || s.trim().length() == 0) return null;
		else return s;
	}

	/**
	 * Copy column info into this column info.
	 * 
	 * @param sourceColumnInfo
	 */
	public void copyFrom(ColumnInfo sourceColumnInfo)
	{
		if (sourceColumnInfo == null)
		{
			return;
		}

		setValidatorProperties(sourceColumnInfo.getValidatorProperties());
		setDefaultValue(sourceColumnInfo.getDefaultValue());
		setTitleText(sourceColumnInfo.getTitleText());
		setConverterName(sourceColumnInfo.getConverterName());
		setConverterProperties(sourceColumnInfo.getConverterProperties());
		setForeignType(sourceColumnInfo.getForeignType());
		setValidatorName(sourceColumnInfo.getValidatorName());
		setDescription(sourceColumnInfo.getDescription());
		setDataProviderID(sourceColumnInfo.getDataProviderID());
		setContainsMetaData(sourceColumnInfo.getContainsMetaData());
		setLookupValue(sourceColumnInfo.getLookupValue());
		setFlags(sourceColumnInfo.getFlags());
		setSequenceStepSize(sourceColumnInfo.getSequenceStepSize());
		setPostSequenceChars(sourceColumnInfo.getPostSequenceChars());
		setPreSequenceChars(sourceColumnInfo.getPreSequenceChars());

		setAutoEnterType(sourceColumnInfo.getAutoEnterType());
		setAutoEnterSubType(sourceColumnInfo.getAutoEnterSubType());

		setDatabaseSequenceName(sourceColumnInfo.getDatabaseSequenceName());
		setConfiguredColumnType(sourceColumnInfo.getConfiguredColumnType());
		setCompatibleColumnTypes(sourceColumnInfo.getCompatibleColumnTypes());

		// Flag that the column is changed.
		flagChanged();

		// Don't set the next sequence; it's a new sequence, so you generally want to start at the beginning.
	}
}
