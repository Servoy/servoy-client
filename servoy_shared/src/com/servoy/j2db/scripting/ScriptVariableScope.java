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


import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.debug.IDebuggerWithWatchPoints;
import org.mozilla.javascript.xml.XMLObject;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IModificationSubject;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.ModificationSubject;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * This scope holds the variables of the specific servoy elements (form/global).
 * It remembers the type of the variable and will convert to that type when a value is set.
 * Media types {@link IColumnTypes#MEDIA} will not be converted but will go into this scope as is.
 *
 * @author jblok
 */
public abstract class ScriptVariableScope extends LazyCompilationScope
{
	private final Map<String, Integer> nameType; //name -> type
	private Map<String, Integer> replacedNameType; //name -> type

	public ScriptVariableScope(Scriptable parent, IExecutingEnviroment scriptEngine, ISupportScriptProviders scriptLookup)
	{
		super(parent, scriptEngine, scriptLookup);
		nameType = new HashMap<String, Integer>();
		nameType.put("_super", new Integer(IColumnTypes.MEDIA)); //$NON-NLS-1$
	}

	public void put(ScriptVariable var)
	{
		put(var, false);
	}

	public void put(ScriptVariable var, boolean overwriteInitialValue)
	{
		putScriptVariable(var.getDataProviderID(), var, overwriteInitialValue);
	}

	protected void putScriptVariable(String name, ScriptVariable var, boolean overwriteInitialValue)
	{
		if (ScopesUtils.isVariableScope(name))
		{
			// global variable: name should have been stripped in GlobalScope
			throw new RuntimeException("Trying to set variable '" + name + "' in non-global scope " + this);
		}

		int prevType = 0;
		if (replacedNameType != null && replacedNameType.containsKey(name)) prevType = Utils.getAsInteger(replacedNameType.get(name));
		else prevType = Utils.getAsInteger(nameType.get(name));
		boolean existingWithSameType = (prevType == var.getVariableType());
		if (!existingWithSameType || overwriteInitialValue)//if same as previous, then leave initial value, this happens in developer or from login->main
		{
			nameType.put(name, new Integer(var.getVariableType()));
			if (replacedNameType != null) replacedNameType.remove(name);
			Object initValue = var.getInitValue();
			if (initValue instanceof String)
			{
				String str = (String)initValue;
				if (str.trim().length() > 0)
				{
					int commentIndex = str.lastIndexOf("//"); //$NON-NLS-1$
					if (commentIndex != -1)
					{
						int singleQuote = str.lastIndexOf('\'');
						if (singleQuote < commentIndex)
						{
							int doubleQuote = str.lastIndexOf('"');
							if (doubleQuote < commentIndex)
							{
								int nextNewLine = str.indexOf('\n', commentIndex);
								if (nextNewLine == -1 || str.lastIndexOf('\n') <= nextNewLine)
								{
									str = str.substring(0, commentIndex).trim();
									if (str.endsWith(";")) str = str.substring(0, str.length() - 1); //$NON-NLS-1$
								}
							}
						}
					}
					str = '(' + str + ')'; // add brackets so that unnamed objects are evaluated correctly (otherwise it will give a syntax error)
				}
				String sourceName = var.getSerializableRuntimeProperty(IScriptProvider.FILENAME);
				if (sourceName == null)
				{
					sourceName = name;
				}
				initValue = evalValue(name, str, sourceName, var.getLineNumberOffset());
			}
			putWithoutFireChange(name, initValue);
			if (Utils.mapGetKeyByValue(allIndex, name) == null)
			{//insert "inded-> name" only if "index -> name" isn't already present  (case of form inheritance)
				allIndex.put(new Integer(allIndex.size()), name);
			}
		}
	}

	private Object evalValue(String name, String str, String sourceName, int lineNumber)
	{
		Object retValue = null;
		Context cx = Context.enter();
		boolean debug = cx.isGeneratingDebug();
		int level = cx.getOptimizationLevel();
		Debugger debugger = cx.getDebugger();
		Object debuggerContextData = cx.getDebuggerContextData();
		try
		{
			cx.setGeneratingDebug(lineNumber != -1);
			if (lineNumber != -1 || Utils.getAsBoolean(System.getProperty(ScriptEngine.SERVOY_DISABLE_SCRIPT_COMPILE_PROPERTY, "false"))) //flag should only be used in rich client //$NON-NLS-1$
			{
				cx.setOptimizationLevel(-1);
			}
			else
			{
				cx.setOptimizationLevel(9);
			}
			//cx.setDebugger(null, null);
			Object o = cx.evaluateString(this, str, sourceName, lineNumber == -1 ? 0 : lineNumber, null);
			if (o instanceof Wrapper && !(o instanceof NativeArray))
			{
				o = ((Wrapper)o).unwrap();
			}
			if (o == Scriptable.NOT_FOUND || o == Undefined.instance)
			{
				o = null;
			}
			retValue = o;
			Integer previousType = null;
			if (retValue instanceof Date)
			{
				// this is really an instance of Date so the type should be reset to that.
				previousType = nameType.put(name, new Integer(IColumnTypes.DATETIME));
			}
			else if (retValue instanceof XMLObject)
			{
				previousType = nameType.put(name, new Integer(Types.OTHER));
			}
			else if (retValue instanceof Number)
			{
				Integer prevType = nameType.get(name);
				if (prevType == null || !(prevType.intValue() == IColumnTypes.NUMBER || prevType.intValue() == IColumnTypes.INTEGER))
				{
					previousType = nameType.put(name, new Integer(IColumnTypes.NUMBER));
				}
			}
			else if (!(retValue instanceof String) && !(retValue == null && nameType.get(name) != null))
			{
				// this is not an instanceof a String and a Date so make it a
				previousType = nameType.put(name, new Integer(IColumnTypes.MEDIA));
			}
			if (previousType != null)
			{
				if (replacedNameType == null)
				{
					replacedNameType = new HashMap<String, Integer>(3);
					replacedNameType.put(name, previousType);
				}
				else if (!replacedNameType.containsKey(name))
				{
					replacedNameType.put(name, previousType);
				}
			}

		}
		catch (Exception ex)
		{
			IServiceProvider serviceProvider = J2DBGlobals.getServiceProvider();
			if (serviceProvider != null)
			{
				serviceProvider.reportJSError("cant parse variable '" + name + '\'', ex);
			}
			else
			{
				Debug.log("cant parse variable '" + name + '\'', ex); //$NON-NLS-1$
			}
		}
		finally
		{
			try
			{
				cx.setGeneratingDebug(debug);
				cx.setOptimizationLevel(level);
				cx.setDebugger(debugger, debuggerContextData);
			}
			finally
			{
				Context.exit();
			}
		}
		return retValue;
	}

	public void putWithoutFireChange(String name, Object value)
	{
		Object oldVar = allVars.get(name);
		if (value == null || !value.equals(oldVar))
		{
			allVars.put(name, value);
		}
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		Object o = allIndex.get(new Integer(index));
		if (o instanceof String && has((String)o, start))
		{
			return get((String)o, start);
		}
		return super.get(index, start);
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
		String name = (String)allIndex.get(new Integer(index));
		if (name != null)
		{
			put(name, start, value);
		}
		else
		{
			super.put(index, start, value);
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		// just return the names for a global scope
		return allVars.keySet().toArray();
	}

	/*
	 * @see Scriptable#put(String, Scriptable, Object)
	 */
	@Override
	public void put(String name, Scriptable arg1, Object value)
	{
		if (value instanceof Function)
		{
			super.put(name, arg1, value);
		}
		else
		{
			try
			{
				Context currentContext = Context.getCurrentContext();
				if (currentContext != null)
				{
					Debugger debugger = currentContext.getDebugger();
					if (debugger != null)
					{
						if (debugger instanceof IDebuggerWithWatchPoints)
						{
							IDebuggerWithWatchPoints wp = (IDebuggerWithWatchPoints)debugger;
							wp.modification(name, this);
						}
					}
				}
				put(name, value);
			}
			catch (RuntimeException re)
			{
				throw new WrappedException(re);
			}
		}
	}

	public Object put(String name, Object val)
	{
		Object value = Utils.mapToNullIfUnmanageble(val);

		Integer variableType = nameType.get(name);
		int type = 0;

		boolean xmlType = false;
		if (variableType == null)
		{
			// global doesn't exist. dynamic new one.. so MEDIA type
			type = IColumnTypes.MEDIA;
			nameType.put(name, new Integer(type));
			Debug.trace("Warning: " + name + " is not defined in the variables, a dynamic (media type) variable is created"); //$NON-NLS-1$//$NON-NLS-2$
		}
		else
		{
			if (variableType.intValue() == Types.OTHER)
			{
				type = IColumnTypes.MEDIA;
				xmlType = true;
			}
			else
			{
				type = Column.mapToDefaultType(variableType.intValue());
			}
		}

		if (type == IColumnTypes.TEXT)
		{
			Object txt = value;
			while (txt instanceof IDelegate< ? >)
			{
				txt = ((IDelegate< ? >)txt).getDelegate();
			}
			if (txt instanceof IDataSet)
			{
				IDataSet set = (IDataSet)txt;
				StringBuilder sb = new StringBuilder();
				sb.append('\n');
				for (int i = 0; i < set.getRowCount(); i++)
				{
					sb.append(set.getRow(i)[0]);
					sb.append('\n');
				}
				value = sb.toString();
			}
			else if (txt instanceof FoundSet)
			{
				StringBuilder sb = new StringBuilder();
				sb.append('\n');
				FoundSet fs = (FoundSet)txt;
				for (int i = 0; i < fs.getSize(); i++)
				{
					IRecordInternal record = fs.getRecord(i);
					sb.append(record.getPKHashKey());
					sb.append('\n');
				}
				value = sb.toString();
			}
		}

		if (value != null && variableType != null)
		{
			Object unwrapped = value;
			while (unwrapped instanceof Wrapper)
			{
				unwrapped = ((Wrapper)unwrapped).unwrap();
				if (unwrapped == value)
				{
					break;
				}
			}

			if (type == IColumnTypes.MEDIA)
			{
				if (!(unwrapped instanceof UUID))
				{
					Object previousValue = get(name);
					if (previousValue instanceof UUID || previousValue == null)
					{
						Iterator<ScriptVariable> scriptVariablesIte = getScriptLookup().getScriptVariables(false);
						ScriptVariable sv;
						while (scriptVariablesIte.hasNext())
						{
							sv = scriptVariablesIte.next();
							if (name.equals(sv.getName()))
							{
								if (UUID.class.getSimpleName().equals(getDeclaredType(sv)))
								{
									value = Utils.getAsUUID(unwrapped, false);
								}
								break;
							}
						}
					}
				}
			}
			else
			{
				value = unwrapped;
				try
				{
					value = Column.getAsRightType(variableType.intValue(), Column.NORMAL_COLUMN, value, null, Integer.MAX_VALUE, null, true); // dont convert with timezone here, its not ui but from scripting
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException(Messages.getString("servoy.conversion.error.global", //$NON-NLS-1$
						new Object[] { name, Column.getDisplayTypeString(variableType.intValue()), value }));
				}
			}
		}

		if (value instanceof Date)
		{
			// make copy so then when it is further used in js it won't change this one.
			value = new Date(((Date)value).getTime());
		}
		else if (xmlType && value instanceof String)
		{
			value = evalValue(name, (String)value, "internal_anon", -1); //$NON-NLS-1$
		}
		Object oldVar = allVars.get(name);
		allVars.put(name, value);
		if (variableType != null && !Utils.equalObjects(oldVar, value))
		{
			fireModificationEvent(name, value);
		}
		return oldVar;
	}

	private String getDeclaredType(ScriptVariable sv)
	{
		String comment = sv.getComment();
		int typeIdx;
		if (comment != null && (typeIdx = comment.indexOf("@type")) != -1) //$NON-NLS-1$
		{
			int s = comment.indexOf('{', typeIdx + 5);
			if (s != -1)
			{
				int e = comment.indexOf('}', s + 1);
				if (e != -1)
				{
					return comment.substring(s + 1, e).trim();
				}
			}
		}
		return null;
	}

/*
 * _____________________________________________________________ JavaScriptModificationListener
 */
	private final IModificationSubject modificationSubject = new ModificationSubject();

	public IModificationSubject getModificationSubject()
	{
		return modificationSubject;
	}

	private void fireModificationEvent(String name, Object value)
	{
		if (modificationSubject.hasListeners())
		{
			modificationSubject.fireModificationEvent(new ModificationEvent(getDataproviderEventName(name), unwrap(value), this));
		}
	}

	protected String getDataproviderEventName(String name)
	{
		return name;
	}

	public Object get(String dataProviderID)
	{
		return unwrap(getImpl(dataProviderID, this));
	}

	public static Object unwrap(Object obj)
	{
		Object o = obj;
		while (o instanceof Wrapper)
		{
			Object tmp = ((Wrapper)o).unwrap();
			if (tmp == o) break;
			if (tmp != null && (tmp instanceof Object[]))
			{
				Object[] array = (Object[])tmp;
				Object[] newArray = new Object[array.length];
				for (int i = 0; i < array.length; i++)
				{
					newArray[i] = unwrap(array[i]);
				}
				tmp = newArray;
			}
			o = tmp;
		}
		if (o instanceof XMLObject)
		{
			o = ((XMLObject)o).toString();
		}
		return o;
	}

	public void remove(ScriptVariable var)
	{
		if (locked) throw new WrappedException(new RuntimeException(Messages.getString("servoy.javascript.error.lockedForDeleteName", new Object[] { var }))); //$NON-NLS-1$
		nameType.remove(var.getName());
		if (replacedNameType != null) replacedNameType.remove(var.getName());

		remove(var.getName());
	}

	@Override
	public Object remove(String name)
	{
		removeIndexByValue(name); // in index based map we keep the variable names as value not the real variable obj; see "get(int, Scriptable)"
		return super.remove(name);
	}

	@Override
	public void destroy()
	{
		nameType.clear();
		if (replacedNameType != null) replacedNameType.clear();
		super.destroy();
	}

}
