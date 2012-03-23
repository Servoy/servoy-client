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
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.FormController;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner, jblok
 */
public class FormScope extends ScriptVariableScope implements Wrapper
{
	private volatile FormController _fp;
	private volatile LazyCompilationScope[] extendScopes;

	public FormScope(FormController fp, ISupportScriptProviders[] extendsHierarchy)
	{
		super(fp.getApplication().getScriptEngine().getSolutionScope(), fp.getApplication().getScriptEngine(), extendsHierarchy[0]);
		_fp = fp;
		putWithoutFireChange("_formname_", fp.getName()); //$NON-NLS-1$

		this.extendScopes = new LazyCompilationScope[extendsHierarchy.length - 1];
		LazyCompilationScope previous = null;
		for (int i = extendScopes.length; --i >= 0;)
		{
			extendScopes[i] = new ExtendsScope(previous, scriptEngine, extendsHierarchy[i + 1]);
			extendScopes[i].setPrototype(previous);
			extendScopes[i].setFunctionParentScriptable(this);
			previous = extendScopes[i];
		}
	}

	public void createVars()
	{
		//put all vars in scope if getForm is flattenform, it will return overridden scriptvars in correct order, sub wins
		Iterator<ScriptVariable> it = _fp.getForm().getScriptVariables(false);
		while (it.hasNext())
		{
			ScriptVariable var = it.next();
			put(var);
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.LazyCompilationScope#getFunctionSuper(com.servoy.j2db.persistence.IScriptProvider)
	 */
	@Override
	protected Scriptable getFunctionSuper(IScriptProvider sp)
	{
		if (extendScopes.length > 0)
		{
			for (int i = 0; i < extendScopes.length; i++)
			{
				if (extendScopes[i].getScriptLookup().getScriptMethod(sp.getID()) != null)
				{
					if (i + 1 < extendScopes.length)
					{
						// the super scope is the next extend scope
						return extendScopes[i + 1];
					}
					else
					{
						// if this was the last extendScope then the script doesn't have a super
						return null;
					}
				}
			}
			// if not found then the script method resides in the sup (FlattenedForm) itself
			// return the first extend scope.
			return extendScopes[0];
		}
		return super.getFunctionSuper(sp);
	}

	/**
	 * @see com.servoy.j2db.scripting.LazyCompilationScope#toString()
	 */
	@Override
	public String toString()
	{
		return "FormScope: " + _fp.getName();// + " extendsHierarchy: " + Arrays.toString(extendScopes); //$NON-NLS-1$
	}

	@Override
	public String getFunctionName(Integer id)
	{
		String name = super.getFunctionName(id);
		if (name == null)
		{
			for (LazyCompilationScope element : extendScopes)
			{
				name = element.getFunctionName(id);
				if (name != null) break;
			}
		}
		return name;
	}

	public FormController getFormController()
	{
		return _fp;
	}

	/**
	 * @see com.servoy.j2db.scripting.LazyCompilationScope#reload()
	 */
	@Override
	public void reload()
	{
		super.reload();
		for (LazyCompilationScope extendScope : extendScopes)
		{
			extendScope.reload();
		}
	}

	@Override
	public void destroy()
	{
		_fp = null;
		extendScopes = null;
		setPrototype(null);
		super.destroy();
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (_fp == null) return NOT_FOUND;

		_fp.touch();
		if ("alldataproviders".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();
			Table table = (Table)_fp.getTable();
			if (table != null)
			{
				try
				{
					Iterator<Column> columns = table.getColumnsSortedByName();
					while (columns.hasNext())
					{
						al.add(columns.next().getDataProviderID());
					}
					Iterator<AggregateVariable> aggs = _fp.getApplication().getFlattenedSolution().getAggregateVariables(table, true);
					while (aggs.hasNext())
					{
						al.add(aggs.next().getDataProviderID());
					}
					Iterator<ScriptCalculation> calcs = _fp.getApplication().getFlattenedSolution().getScriptCalculations(table, true);
					while (calcs.hasNext())
					{
						al.add(calcs.next().getDataProviderID());
					}
				}
				catch (RepositoryException ex)
				{
					Debug.error(ex);
				}
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		if ("allmethods".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();
			Iterator<ScriptMethod> it = _fp.getForm().getScriptMethods(true);
			while (it.hasNext())
			{
				ScriptMethod sm = it.next();
				al.add(sm.getName());
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		if ("allrelations".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();
			Form f = _fp.getForm();
			try
			{
				Iterator<Relation> it = _fp.getApplication().getFlattenedSolution().getRelations(
					_fp.getApplication().getFoundSetManager().getTable(f.getDataSource()), true, true);
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

		if ("allvariables".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();

			try
			{
				Iterator<ScriptVariable> itScriptVariable = _fp.getForm().getScriptVariables(false);
				while (itScriptVariable.hasNext())
				{
					al.add(itScriptVariable.next().getName());
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		Object object = super.get(name, start);
		if ((object == null || object == Scriptable.NOT_FOUND) && ("foundset".equals(name) || "elements".equals(name)))
		{
			Debug.error(Thread.currentThread().getName() + ": For form " + _fp + " the foundset was asked for but that was not set. " +
					(this == _fp.getFormScope()), new RuntimeException());
			if (name.equals("foundset")) return _fp.getFormModel();
		}
		return object;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#has(String, Scriptable)
	 */
	@Override
	public boolean has(String name, Scriptable start)
	{
		if ("allnames".equals(name) || "alldataproviders".equals(name) || "allrelations".equals(name) || "allmethods".equals(name) | "allvariables".equals(name)) return true; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		return super.has(name, start);
	}

	public Object unwrap()
	{
		return _fp;
	}

	private class ExtendsScope extends LazyCompilationScope implements Wrapper
	{
		public ExtendsScope(LazyCompilationScope parent, IExecutingEnviroment scriptEngine, ISupportScriptProviders methodLookup)
		{
			super(parent, scriptEngine, methodLookup);
		}

		/**
		 * @see com.servoy.j2db.scripting.LazyCompilationScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
		 */
		@Override
		public Object get(String name, Scriptable start)
		{
			Object object = super.get(name, start);
			if (object == Scriptable.NOT_FOUND && getFunctionParentScriptable() != null)
			{
				Object obj = getFunctionParentScriptable().get(name, start);
				// only return form variables not functions, they should be resolved by the (compile)scope.
				if (obj instanceof Function) return Scriptable.NOT_FOUND;
				return obj;
			}
			return object;
		}

		/**
		 * @see com.servoy.j2db.scripting.LazyCompilationScope#getFunctionSuper(com.servoy.j2db.persistence.IScriptProvider)
		 */
		@Override
		protected Scriptable getFunctionSuper(IScriptProvider sp)
		{
			return FormScope.this.getFunctionSuper(sp);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.mozilla.javascript.Wrapper#unwrap()
		 */
		public Object unwrap()
		{
			return _fp;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.ScriptVariableScope#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	@Override
	public void put(String name, Scriptable arg1, Object value)
	{
		if (name.equals("foundset")) //$NON-NLS-1$
		{
			throw new RuntimeException("Setting of foundset object is not possible on form: " + _fp.getName()); //$NON-NLS-1$
		}
		super.put(name, arg1, value);
	}


}