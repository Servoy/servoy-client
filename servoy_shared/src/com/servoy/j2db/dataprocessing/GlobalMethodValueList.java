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
package com.servoy.j2db.dataprocessing;

import java.sql.Types;

import org.mozilla.javascript.Function;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class GlobalMethodValueList extends CustomValueList
{

	private final Pair<String, String> globalFunction;
	private IRecordInternal record;
	private String displayString;
	private Object realObject;
	private boolean filling;

	/**
	 * @param application
	 * @param vlName
	 */
	public GlobalMethodValueList(IServiceProvider application, ValueList vl)
	{
		super(application, vl);
		setValueType(Types.OTHER);

		ScriptMethod globalScriptMethod = application.getFlattenedSolution().getScriptMethod(vl.getCustomValues());
		Pair<String, String> scope = ScopesUtils.getVariableScope(globalScriptMethod != null ? globalScriptMethod.getPrefixedName() : null);
		if (vl.getValueListType() != IValueListConstants.GLOBAL_METHOD_VALUES || scope == null || scope.getLeft() == null)
		{
			throw new RuntimeException("GlobalMethodValueList couldnt be made for: " + ScopesUtils.getScopeString(scope)); //$NON-NLS-1$
		}
		if (vl.getAddEmptyValue() == IValueListConstants.EMPTY_VALUE_ALWAYS)
		{
			allowEmptySelection = true;
		}
		globalFunction = scope;
		if (application.getScriptEngine() != null)
		{
			GlobalScope gs = application.getScriptEngine().getScopesScope().getGlobalScope(globalFunction.getLeft());
			Function func = gs == null ? null : gs.getFunctionByName(globalFunction.getRight());
			if (func == null)
			{
				throw new RuntimeException("global Function not found for: " + ScopesUtils.getScopeString(scope)); //$NON-NLS-1$
			}
		}
	}

	private boolean hasRealValue = false;

	/**
	 * @see com.servoy.j2db.dataprocessing.CustomValueList#hasRealValues()
	 */
	@Override
	public boolean hasRealValues()
	{
		return hasRealValue;
	}

	public void fill()
	{
		fill(record, null, null);
	}

	public void fill(IRecordInternal state, String filter, Object real)
	{
		String display = filter == null ? null : filter.toLowerCase();
		if (filling)
		{
			return;
		}

		try
		{
			filling = true;
			if (this.record != state || !Utils.equalObjects(display, this.displayString) || !Utils.equalObjects(real, this.realObject))
			{
				this.displayString = display;
				this.realObject = real;
				this.record = state;
				super.fill(state);

				GlobalScope globalScope = application.getScriptEngine().getScopesScope().getGlobalScope(globalFunction.getLeft());
				if (globalScope != null)
				{
					Function function = globalScope.getFunctionByName(globalFunction.getRight());
					try
					{
						if (real == null || "".equals(real)) //$NON-NLS-1$
						{
							application.invokeAndWait(new Runnable()
							{
								public void run()
								{
									realValues = new SafeArrayList<Object>();
									removeAllElements();
								}
							});
						}
						else if (realValues == null)
						{
							realValues = new SafeArrayList<Object>();
						}


						Object[] args = null;
						if (display != null && !"".equals(display)) //$NON-NLS-1$
						{
							args = new Object[] { display, null, state, valueList.getName(), Boolean.valueOf(state instanceof FindState), filter };
						}
						else if (real != null && !"".equals(real)) //$NON-NLS-1$
						{
							args = new Object[] { null, real, state, valueList.getName(), Boolean.valueOf(state instanceof FindState), null };
						}
						else
						{
							args = new Object[] { null, null, state, valueList.getName(), Boolean.valueOf(state instanceof FindState), null };
						}

						final Object retValue = application.getScriptEngine().executeFunction(function, globalScope, globalScope, args, false, true);

						application.invokeAndWait(new Runnable()
						{
							public void run()
							{
								//add empty row
								if (valueList.getAddEmptyValue() == IValueListConstants.EMPTY_VALUE_ALWAYS)
								{
									addElement(""); //$NON-NLS-1$
									realValues.add(null);
								}
								if (retValue instanceof IDataSet && ((IDataSet)retValue).getRowCount() > 0)
								{
									startBundlingEvents();
									try
									{
										hasRealValue = false;
										IDataSet dataSet = (IDataSet)retValue;
										for (int i = 0; i < dataSet.getRowCount(); i++)
										{
											Object[] row = dataSet.getRow(i);
											if (row.length == 1)
											{
												realValues.add(CustomValueList.handleRowData(valueList, false, 1, row, application));
											}
											else
											{
												hasRealValue = true;
												realValues.add(CustomValueList.handleRowData(valueList, false, 2, row, application));
											}
											addElement(CustomValueList.handleRowData(valueList, false, 1, row, application));
										}
									}
									finally
									{
										stopBundlingEvents();
									}
								}
							}
						});
					}
					catch (Exception e)
					{
						application.reportError("error getting data from global method valuelist", e); //$NON-NLS-1$
					}
				}
			}
		}
		finally
		{
			filling = false;
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.CustomValueList#fill(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	@Override
	public void fill(IRecordInternal parentState)
	{
		super.fill(parentState);
		fill(parentState, null, null);
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.CustomValueList#realValueIndexOf(java.lang.Object)
	 */
	@Override
	public int realValueIndexOf(Object obj)
	{
		int index = super.realValueIndexOf(obj);
		if (index == -1 && obj != null)
		{
			fill(record, null, obj);
			index = super.realValueIndexOf(obj);
		}
		return index;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.CustomValueList#deregister()
	 */
	@Override
	public void deregister()
	{
		super.deregister();
		realValues = new SafeArrayList<Object>();
		removeAllElements();
	}

	@Override
	public IDataProvider[] getDependedDataProviders()
	{
		return new IDataProvider[0];
	}
}
