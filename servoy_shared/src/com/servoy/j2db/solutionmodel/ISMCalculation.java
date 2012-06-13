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
 * Solution model scrop calculation.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface ISMCalculation extends ISMHasUUID
{
	public int getVariableType();

	public void setVariableType(int type);

	/**
	 * This method returns the name of the stored calculation.
	 * 
	 * @sample
	 * var calc = solutionModel.newCalculation("function myCalculation() { return 123; }", JSVariable.INTEGER, "db:/example_data/customers");
	 * application.output(calc.getName()); 
	 * 
	 * @return the name of the stored calculation
	 */
	public String getName();

	/**
	 * Returns whether this calculation is a stored one or not.
	 * 
	 * @sample
	 * var calc = solutionModel.newCalculation("function myCalculation() { return 123; }", JSVariable.INTEGER, "db:/example_data/customers");
	 * if (calc.isStored()) application.output("The calculation is stored");
	 * else application.output("The calculation is not stored");
	 * 
	 * @return true if the calculation is stored, false otherwise
	 */
	public boolean isStored();

	public String getCode();

	public void setCode(String code);
}