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
package com.servoy.j2db.scripting;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 */
public class TableScope extends LazyCompilationScope
{
	private final static ThreadLocal<Object[]> values = new ThreadLocal<Object[]>();
	private final static ThreadLocal<UsedDataProviderTracker> usedDataProviderTracker = new ThreadLocal<UsedDataProviderTracker>();

	private final Table table;
	private final FlattenedSolution solution;

	public TableScope(Scriptable parent, IExecutingEnviroment engine, ITable table, FlattenedSolution solution, ISupportScriptProviders scriptLookup)
	{
		super(parent, engine, scriptLookup);
		this.table = (Table)table;
		this.solution = solution;
		setFunctionParentScriptable(new RecordingScriptable(null, this));
	}

	public void setArguments(Object[] vargs)
	{
		Object[] array = getThreadLocalArray();
		array[1] = vargs;
	}

	public UsedDataProviderTracker setUsedDataProviderTracker(UsedDataProviderTracker usedDataProviderTracker)
	{
		UsedDataProviderTracker current = TableScope.usedDataProviderTracker.get();
		if (usedDataProviderTracker == null)
		{
			TableScope.usedDataProviderTracker.remove();
		}
		else
		{
			TableScope.usedDataProviderTracker.set(usedDataProviderTracker);
		}
		return current;
	}

	private Object[] getThreadLocalArray()
	{
		Object[] array = values.get();
		if (array == null)
		{
			array = new Object[3];
			values.set(array);
		}
		return array;
	}

	@Override
	public Scriptable getPrototype()
	{
		Object[] array = getThreadLocalArray();
		return (Scriptable)array[0];
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		getThreadLocalArray()[0] = prototype;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if ("allrelations".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();
			try
			{
				Iterator<Relation> it = solution.getRelations(table, true, true);
				while (it.hasNext())
				{
					Relation r = it.next();
					if (!r.isGlobal())
					{
						al.add(r.getName());
					}
				}
			}
			catch (RepositoryException ex)
			{
				Debug.error(ex);
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		Object o = super.get(name, start);
		if (o instanceof Function)
		{
			// get all calcs via the Record (parent scope), if the value changes it will be saved/cached in the record
			return Scriptable.NOT_FOUND;
		}
		return o;
	}

	private Object getCalculationValue(Function calculation, String name)
	{
		Scriptable proto = getPrototype();
		// if this happens record is not set as prototype.. (for example debugger)
		// we can't return then anything.
		if (proto == null)
		{
			return null;
		}

		Object[] array = getThreadLocalArray();
		List<String> callStack = (List<String>)array[2];
		if (callStack == null)
		{
			callStack = new ArrayList<String>();
			array[2] = callStack;
		}

		Record record = null;
		String callStackName = name;
		if (proto instanceof Record)
		{
			record = (Record)proto;
			callStackName = callStackName + '_' + record.getRawData().getPKHashKey();
		}
		UsedDataProviderTracker tracker = null;
		try
		{
			boolean contains = callStack.contains(callStackName);
			callStack.add(callStackName);//first add before doing anything else!

			//now we decide if we try to return from row cache or calculate again
			if (contains)
			{
				if (record != null)
				{
					return record.getRawData().getValue(name);
				}
				throw new RuntimeException(Messages.getString("servoy.error.cycleDetected", new Object[] { name, table.getName(), callStack })); //$NON-NLS-1$
			}
			tracker = usedDataProviderTracker.get();
			if (tracker != null)
			{
				((RecordingScriptable)getFunctionParentScriptable()).pushRecordingTracker(tracker);
				setUsedDataProviderTracker(null);
			}

			Object o = scriptEngine.executeFunction(calculation, this, getFunctionParentScriptable(), (Object[])array[1], false, false);
			if (o instanceof Undefined && record != null) o = record.getRawData().getValue(name);//record value trick
			return o;
		}
		catch (Exception e)
		{
			throw new RuntimeException(Messages.getString("servoy.error.executingCalculation", new Object[] { name, table.getName(), e.toString() }), e); //$NON-NLS-1$
		}
		finally
		{
			if (tracker != null)
			{
				((RecordingScriptable)getFunctionParentScriptable()).popRecordingTracker();
			}
			callStack.remove(callStackName);
			if (callStack.size() == 0)
			{
				// clear the thread locals.
				values.remove();
			}
		}
	}

	public Object getCalculationValue(String calcName, Scriptable start)
	{
		Object o = super.get(calcName, start);
		if (o instanceof Function)
		{
			return getCalculationValue((Function)o, calcName);
		}
		return o;
	}


	/**
	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	@Override
	public void put(String name, Scriptable start, Object value)
	{
		if (value instanceof Wrapper)
		{
			value = ((Wrapper)value).unwrap();
		}
		// Only functions can be added to a TableScope
		if (value instanceof Function)
		{
			super.put(name, start, value);
		}
		else
		{
			Scriptable prototype = getPrototype();
			if (prototype != null)
			{
				if (prototype.has(name, start))
				{
					prototype.put(name, start, value);
				}
			}
		}
	}

	//used by scripting
	public void putPrintVar(String name, Scriptable start, Object value)
	{
		if (value instanceof Wrapper)
		{
			value = ((Wrapper)value).unwrap();
		}
		super.put(name, start, value);
	}

	@Override
	public String getScopeName()
	{
		return table.getServerName() + "." + table.getName();
	}
}
