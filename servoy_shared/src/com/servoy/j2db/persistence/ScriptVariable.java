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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.Wrapper;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * A so called script variable used as global under solution and as form variable used under form objects
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Variable", typeCode = IRepository.SCRIPTVARIABLES)
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class ScriptVariable extends AbstractBase implements IVariable, IDataProvider, ISupportUpdateableName, ISupportHTMLToolTipText, ISupportContentEquals,
	IPersistCloneable, ICloneable, ISupportScope, ISupportDeprecatedAnnotation
{

	private static final long serialVersionUID = 1L;

	private String prefixedName = null;

	public static final String GLOBAL_SCOPE = "globals"; //$NON-NLS-1$
	public static final String GLOBALS_DOT_PREFIX = GLOBAL_SCOPE + '.';
	public static final String SCOPES = "scopes"; //$NON-NLS-1$
	public static final String SCOPES_DOT_PREFIX = SCOPES + '.';

	/**
	 * Constructor I
	 */
	ScriptVariable(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.SCRIPTVARIABLES, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	/**
	 * update the name
	 *
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		prefixedName = null;
	}

	/**
	 * Set the name
	 *
	 * @param arg the name
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(getScopeName() != null ? getScopeName() : getRootObject(), IRepository.SCRIPTVARIABLES),
			false);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		prefixedName = null;
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMVariable#getName()
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Property for setting and getting the jsdoc text (comment) of the script variable.
	 *
	 * @return the value of the jsdoc text (comment) of the script variable
	 */
	public String getComment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_COMMENT);
	}

	/**
	 * @param arg the jsdoc text value for the script variable
	 */
	public void setComment(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_COMMENT, arg);
	}

	/**
	 * The name of the scope.
	 */
	public String getScopeName()
	{
		String value = getTypedProperty(StaticContentSpecLoader.PROPERTY_SCOPENAME);
		if (value == null && getParent() instanceof Solution)
		{
			// scope name is a bit strange, has 2 default values: null for non direct solution children (handled through ContentSpec) and "globals" for direct solution children...
			// it needs to be interpreted as "globals" if not available for solution children (for importing solutions from older versions before scopes were introduced - otherwise a "null.js" gets created)
			value = ScriptVariable.GLOBAL_SCOPE;
		}
		return value;
	}

	public void setScopeName(String scopeName)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SCOPENAME, scopeName);
	}

	/**
	 * Set the variableType
	 *
	 * @param arg the variableType
	 */
	@SuppressWarnings("nls")
	public void setVariableType(int arg)
	{
		int variableType = arg;
		if (variableType == Types.FLOAT) variableType = IColumnTypes.NUMBER;
		boolean hit = false;
		int[] types = Column.allDefinedTypes;
		for (int element : types)
		{
			if (variableType == element)
			{
				hit = true;
				break;
			}
		}
		if (!hit)
		{
			Debug.error("unknown variable type " + variableType + " for variable " + getName() + " reverting to previous or MEDIA type",
				new RuntimeException());
			if (getVariableType() == 0)
			{
				variableType = IColumnTypes.MEDIA;
			}
			else
			{
				return;
			}
		}

		setTypedProperty(StaticContentSpecLoader.PROPERTY_VARIABLETYPE, variableType);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMVariable#getVariableType()
	 */
	public int getVariableType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VARIABLETYPE).intValue();
	}

	@Override
	public String toString()
	{
		return getDataProviderID();
	}

	public void setDefaultValue(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DEFAULTVALUE, "".equals(arg) ? null : arg);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMVariable#getDefaultValue()
	 */
	public String getDefaultValue()
	{
		String value = getTypedProperty(StaticContentSpecLoader.PROPERTY_DEFAULTVALUE);
		return "".equals(value) ? null : value;
	}

	@SuppressWarnings("nls")
	public Object getInitValue()
	{
		String defaultValue = getDefaultValue();
		switch (Column.mapToDefaultType(getVariableType()))
		{
			case IColumnTypes.DATETIME :
				if ("now".equalsIgnoreCase(defaultValue))
				{
					return new java.util.Date();
				}
				if (defaultValue != null)
				{
					return parseDate(defaultValue);
				}
				return null;

			case IColumnTypes.TEXT :
			{
				if ("null".equalsIgnoreCase(defaultValue))
				{
					return null;
				}
				return (defaultValue == null ? "" : defaultValue); //$NON-NLS-1$
			}

			case IColumnTypes.NUMBER :
				try
				{
					double number = Utils.getAsDouble(defaultValue, true);
					return ("null".equalsIgnoreCase(defaultValue) ? null : new Double(number)); //$NON-NLS-1$
				}
				catch (RuntimeException e)
				{
					return defaultValue;
				}

			case IColumnTypes.INTEGER :
				try
				{
					int number = Utils.getAsInteger(defaultValue, true);
					return ("null".equalsIgnoreCase(defaultValue) ? null : new Integer(number)); //$NON-NLS-1$
				}
				catch (RuntimeException e)
				{
					return defaultValue;
				}

			case IColumnTypes.MEDIA :
				if ("null".equalsIgnoreCase(defaultValue))
				{
					return null;
				}
				return defaultValue;

			default :
				return null;
		}
	}

	public String toHTML()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("<b>"); //$NON-NLS-1$
		sb.append(Column.getDisplayTypeString(getVariableType()));
		sb.append("&nbsp;");
		sb.append(getName());
		sb.append("</b> "); //$NON-NLS-1$
		sb.append("<br><pre>defaultvalue: "); //$NON-NLS-1$
		sb.append(getDefaultValue());
		sb.append("</pre>");
		return sb.toString();
	}

	/*
	 * _____________________________________________________________ Methods from IDataProvider
	 */
	public String getDataProviderID()
	{
		if (prefixedName == null)
		{
			if (getParent() instanceof Solution)
			{
				prefixedName = ScopesUtils.getScopeString(this);
			}
			else
			{
				prefixedName = getName();
			}
		}
		return prefixedName;
	}

	public int getDataProviderType()
	{
		return getVariableType();
	}

//	public String[] getDependentDataProviderIDs()
//	{
//		return null;
//	}

	public ColumnWrapper getColumnWrapper()
	{
		return null;
	}

	public int getLength()
	{
		return -1;
	}

	public boolean isEditable()
	{
		return true;
	}

	public int getFlags()
	{
		return Column.NORMAL_COLUMN;
	}

	//the repository element id can differ!
	public boolean contentEquals(Object obj)
	{
		if (obj instanceof ScriptVariable)
		{
			ScriptVariable other = (ScriptVariable)obj;
			return (getDataProviderID().equals(other.getDataProviderID()) && getVariableType() == other.getVariableType() &&
				Utils.equalObjects(getDefaultValue(), other.getDefaultValue()));
		}
		return false;
	}


	@SuppressWarnings("nls")
	private static Date parseDate(String dateString)
	{
		if (dateString == null) return null;
		if (!dateString.toLowerCase().startsWith("new date(")) return null;

		String value = dateString.substring(9, dateString.lastIndexOf(')'));

		if (value.trim().length() == 0) return new Date();

		Object[] args = null;
		if (value.startsWith("\"") || value.startsWith("'"))
		{
			args = new Object[] { value.substring(1, value.length() - 1) };
		}
		else
		{
			ArrayList<String> al = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(value, ",");
			while (st.hasMoreTokens())
			{
				al.add(st.nextToken());
			}
			args = al.toArray();
		}
		Wrapper wrapper = (Wrapper)NativeDate.jsConstructor(args);
		return (Date)wrapper.unwrap();
	}

	/**
	 * @return
	 */
	public boolean isPrivate()
	{
		return getComment() != null && getComment().indexOf("@private") != -1;
	}

	public boolean isPublic()
	{
		return getComment() != null && getComment().indexOf("@public") != -1;
	}

	public boolean isDeprecated()
	{
		return getComment() != null && getComment().indexOf("@deprecated") != -1;
	}

	public int getLineNumberOffset()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LINENUMBEROFFSET).intValue();
	}

	public void setLineNumberOffset(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LINENUMBEROFFSET, arg);
	}

	/**
	 * @return
	 */
	public boolean isEnum()
	{
		return getComment() != null && getComment().indexOf("@enum") != -1;
	}
}