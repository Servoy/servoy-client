/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.solutionmodel;


/**
 * Solution model data source node.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMDataSourceNode
{

	/**
	 * Get the data source for this node.
	 * 
	 * @return the dataSource
	 */
	public String getDataSource();

	/**
	 * Get an existing calculation for the datasource node.
	 * 
	 * @param name The name of the calculation
	 * 
	 * @sampleas newCalculation(String, int)
	 * 
	 */
	public ISMCalculation getCalculation(String name);

	/**
	 * Gets all the calculations for the datasource node.
	 * 
	 * @sampleas newCalculation(String, int)
	 */
	public ISMCalculation[] getCalculations();

	/**
	 * Creates a new calculation for the given code, the type will be the column where it could be build on (if name is a column name),
	 * else it will default to JSVariable.TEXT;
	 * 
	 * @param code The code of the calculation, this must be a full function declaration.
	 * 
	 * @sampleas newCalculation(String, int)
	 * 
	 */
	public ISMCalculation newCalculation(String code);

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
	public ISMCalculation newCalculation(String code, int type);

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
	public boolean removeCalculation(String name);

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
	public ISMMethod newMethod(String code);

	/**
	 * Get an existing foundset method for the datasource node.
	 * 
	 * @param name The name of the method
	 * 
	 * @sampleas newMethod(String)
	 * 
	 */
	public ISMMethod getMethod(String name);

	/**
	 * Gets all the foundset methods for the datasource node.
	 * 
	 * @sampleas newMethod(String)
	 */
	public ISMMethod[] getMethods();

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
	public boolean removeMethod(String name);

}