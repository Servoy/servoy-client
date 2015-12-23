/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.j2db.scripting.solutionmodel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ICloneable;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.TypeIterator;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.TableScope;
import com.servoy.j2db.solutionmodel.ISMDataSourceNode;

/**
 * Solution model holder for calculations and foundset methods.
 *
 * @author rgansevles
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSDataSourceNode implements IJSScriptParent<TableNode>, IConstantsObject, ISMDataSourceNode
{
	private final IApplication application;
	private final String dataSource;

	public JSDataSourceNode(IApplication application, String dataSource)
	{
		this.application = application;
		this.dataSource = dataSource;
	}

	public final void checkModification()
	{
		// no need to create a copy here, a table node copy will be created when needed.
	}

	@SuppressWarnings("unchecked")
	public <T extends IScriptProvider> T getScriptCopy(T script) throws RepositoryException
	{
		TableNode tableNode = application.getFlattenedSolution().getSolutionCopyTableNode(dataSource);
		T sc = AbstractBase.selectByName(new TypeIterator<T>(tableNode.getAllObjects(), script.getTypeID()), script.getName());
		if (sc == null)
		{
			sc = (T)((ICloneable)script).clonePersist();
			tableNode.addChild(sc);
		}

		return sc;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getJSParent()
	 */
	public IJSParent< ? > getJSParent()
	{
		return null;
	}

	/**
	 * Get the data source for this node.
	 *
	 * @sample
	 * var nodeDataSource = solutionModel.getDataSourceNode("db:/example_data/customers").getDataSource();
	 *
	 * @return the dataSource
	 */
	@JSFunction
	public String getDataSource()
	{
		return dataSource;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getBaseComponent()
	 */
	public TableNode getSupportChild()
	{
		try
		{
			return application.getFlattenedSolution().getSolutionCopyTableNode(dataSource);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get an existing calculation for the datasource node.
	 *
	 * @param name The name of the calculation
	 *
	 * @sampleas newCalculation(String, int)
	 *
	 */
	@JSFunction
	public JSCalculation getCalculation(String name)
	{
		ScriptCalculation scriptCalculation = application.getFlattenedSolution().getScriptCalculation(name, dataSource);
		if (scriptCalculation != null)
		{
			return new JSCalculation(this, scriptCalculation, application, false);
		}
		return null;
	}

	/**
	 * Gets all the calculations for the datasource node.
	 *
	 * @sampleas newCalculation(String, int)
	 */
	@JSFunction
	public JSCalculation[] getCalculations()
	{
		List<JSCalculation> calculations = new ArrayList<JSCalculation>();
		Iterator<ScriptCalculation> scriptCalculations = application.getFlattenedSolution().getScriptCalculations(dataSource, true);
		while (scriptCalculations.hasNext())
		{
			calculations.add(new JSCalculation(this, scriptCalculations.next(), application, false));
		}
		return calculations.toArray(new JSCalculation[calculations.size()]);
	}

	/**
	 * Creates a new calculation for the given code, the type will be the column where it could be build on (if name is a column name),
	 * else it will default to JSVariable.TEXT;
	 *
	 * @param code The code of the calculation, this must be a full function declaration.
	 *
	 * @sampleas newCalculation(String, int)
	 *
	 */
	@JSFunction
	public JSCalculation newCalculation(String code)
	{
		return newCalculation(code, IColumnTypes.TEXT);
	}

	/**
	 * Creates a new calculation for the given code and the type, if it builds on a column (name is a column name) then type will be ignored.
	 *
	 * @param code The code of the calculation, this must be a full function declaration.
	 * @param type The type of the calculation, one of the JSVariable types.
	 *
	 * @sample
	 * var calc = solutionModel.getDataSourceNode("db:/example_data/customers").newCalculation("function myCalculation() { return 123; }", JSVariable.INTEGER);
	 * var calc2 = solutionModel.getDataSourceNode("db:/example_data/customers").newCalculation("function myCalculation2() { return '20'; }");
	 * var calc3 = solutionModel.getDataSourceNode("db:/example_data/employees").newCalculation("function myCalculation3() { return 'Hello World!'; }",	JSVariable.TEXT);
	 *
	 * var c = solutionModel.getDataSourceNode("db:/example_data/customers").getCalculation("myCalculation");
	 * application.output("Name: " + c.getName() + ", Stored: " + c.isStored());
	 *
	 * var allCalcs = solutionModel.getDataSourceNode("db:/example_data/customers").getCalculations();
	 * for (var i = 0; i < allCalcs.length; i++) {
	 * 	application.output(allCalcs[i]);
	 * }
	 *
	 */
	@JSFunction
	public JSCalculation newCalculation(String code, int type)
	{
		try
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			TableNode tablenode = fs.getSolutionCopyTableNode(dataSource);

			String name = JSMethod.parseName(code);
			ScriptCalculation scriptCalculation = tablenode.createNewScriptCalculation(new ScriptNameValidator(fs), name, null, fs.getTable(dataSource));
			scriptCalculation.setDeclaration(code);
			scriptCalculation.setTypeAndCheck(type, application);
			TableScope tableScope = (TableScope)application.getScriptEngine().getTableScope(scriptCalculation.getTable());
			if (tableScope != null)
			{
				tableScope.put(scriptCalculation, scriptCalculation);
				((FoundSetManager)application.getFoundSetManager()).flushSQLSheet(dataSource);
			}

			return new JSCalculation(this, scriptCalculation, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes the calculation specified by name.
	 *
	 * @sample
	 * var calc1 = solutionModel.getDataSourceNode("db:/example_data/customers").newCalculation("function myCalculation1() { return 123; }", JSVariable.INTEGER);
	 * var calc2 = solutionModel.getDataSourceNode("db:/example_data/customers").newCalculation("function myCalculation2() { return '20'; }");
	 *
	 * var c = solutionModel.getDataSourceNode("db:/example_data/customers").getCalculation("myCalculation1");
	 * application.output("Name: " + c.getName() + ", Stored: " + c.isStored());
	 *
	 * solutionModel.getDataSourceNode("db:/example_data/customers").removeCalculation("myCalculation1");
	 * c = solutionModel.getDataSourceNode("db:/example_data/customers").getCalculation("myCalculation1");
	 * if (c != null) {
	 * 	application.output("myCalculation could not be removed.");
	 * }
	 *
	 * var allCalcs = solutionModel.getDataSourceNode("db:/example_data/customers").getCalculations();
	 * for (var i = 0; i < allCalcs.length; i++) {
	 * 	application.output(allCalcs[i]);
	 * }
	 *
	 * @param name the name of the calculation to be removed
	 *
	 * @return true if the removal was successful, false otherwise
	 */
	@JSFunction
	public boolean removeCalculation(String name)
	{
		try
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			TableNode tablenode = fs.getSolutionCopyTableNode(dataSource);
			ScriptCalculation sc = tablenode.getScriptCalculation(name);
			if (sc != null)
			{
				tablenode.removeChild(sc);
				return true;
			}

			//it is a design time calculation, therefore we "hide" it
			sc = fs.getScriptCalculation(name, dataSource);
			if (sc != null)
			{
				fs.addToRemovedPersists(sc);
				return true;
			}

			// not found
			return false;
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new foundset method with the specified code.
	 *
	 * @sample
	 * var method = solutionModel.getDataSourceNode("db:/example_data/orders").newMethod("function doubleSize() { return 2*getSize(); }");
	 *
	 * application.output('Doubled orders for this customer: '+customers_to_orders.doubleSize())
	 *
	 * @param code the specified code for the foundset method
	 *
	 * @return a JSMethod object
	 */
	@JSFunction
	public JSMethod newMethod(String code)
	{
		try
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			TableNode tablenode = fs.getSolutionCopyTableNode(dataSource);
			if (tablenode == null) throw new RuntimeException("Couldnt create method for datasource: " + dataSource);

			String name = JSMethod.parseName(code);
			ScriptMethod method = tablenode.createNewFoundsetMethod(new ScriptNameValidator(fs), name, null);
			method.setDeclaration(code);
			((FoundSetManager)application.getFoundSetManager()).reloadFoundsetMethod(dataSource, method);

			return new JSMethod(this, method, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get an existing foundset method for the datasource node.
	 *
	 * @param name The name of the method
	 *
	 * @sampleas newMethod(String)
	 *
	 */
	@JSFunction
	public JSMethod getMethod(String name)
	{
		ScriptMethod method = application.getFlattenedSolution().getFoundsetMethod(name, dataSource);
		if (method != null)
		{
			return new JSMethod(this, method, application, false);
		}
		return null;
	}

	/**
	 * Gets all the foundset methods for the datasource node.
	 *
	 * @sampleas newMethod(String)
	 */
	@JSFunction
	public JSMethod[] getMethods()
	{
		List<JSMethod> methods = new ArrayList<JSMethod>();
		Iterator<ScriptMethod> fsMethods = application.getFlattenedSolution().getFoundsetMethods(dataSource, true);
		while (fsMethods.hasNext())
		{
			methods.add(new JSMethod(this, fsMethods.next(), application, false));
		}
		return methods.toArray(new JSMethod[methods.size()]);
	}

	/**
	 * Removes the foundset method specified by name.
	 *
	 * @sample
	 * var method1 = solutionModel.getDataSourceNode("db:/example_data/customers").newMethod("function myFoundsetMethod1() { return 123; }");
	 * var method2 = solutionModel.getDataSourceNode("db:/example_data/customers").newCalculation("function myFoundsetMethod2() { return '20'; }");
	 *
	 * var m = solutionModel.getDataSourceNode("db:/example_data/customers").getMethod("myFoundsetMethod1");
	 * application.output("Name: " + m.getName());
	 *
	 * solutionModel.getDataSourceNode("db:/example_data/customers").removeMethod("myFoundsetMethod1");
	 * m = solutionModel.getDataSourceNode("db:/example_data/customers").getCalculation("myFoundsetMethod1");
	 * if (m != null) { application.output("myFoundsetMethod1 could not be removed."); }
	 *
	 * var allMethods = solutionModel.getDataSourceNode("db:/example_data/customers").getMethod();
	 * for (var i = 0; i < allMethods; i++)
	 * {
	 * 	application.output(allMethods[i]);
	 * }
	 *
	 * @param name the name of the method to be removed
	 *
	 * @return true if the removal was successful, false otherwise
	 */
	@JSFunction
	public boolean removeMethod(String name)
	{
		try
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			TableNode tablenode = fs.getSolutionCopyTableNode(dataSource);
			ScriptMethod sc = tablenode.getFoundsetMethod(name);
			if (sc != null)
			{
				tablenode.removeChild(sc);
				return true;
			}

			//it is a design time method, therefore we "hide" it
			sc = fs.getFoundsetMethod(name, dataSource);
			if (sc != null)
			{
				fs.addToRemovedPersists(sc);
				if (application.getFormManager() instanceof FormManager) ((FormManager)application.getFormManager()).fillScriptMenu();
				return true;
			}

			// not found
			return false;
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}


	@Override
	public String toString()
	{
		return "JSDataSourceNode[" + dataSource + ']';
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSDataSourceNode other = (JSDataSourceNode)obj;
		if (dataSource == null)
		{
			if (other.dataSource != null) return false;
		}
		else if (!dataSource.equals(other.dataSource)) return false;
		return true;
	}

}
