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

package com.servoy.base.scripting.solutionhelper;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMButtonConstants;

/**
 * @author acostescu
 * @deprecated please use {@link IBaseSMButtonConstants} instead.
 */
@ServoyClientSupport(mc = true, wc = false, sc = false)
@Deprecated
public interface IPredefinedIconConstants
{
	/**
	 * Constant for specifying a predefined icon type for a button.
	 * @deprecated please use JSButton.ICON_... constants instead.
	 * @sample
	 * plugins.mobile.solutionHelper.setIconType(myJSButton, plugins.mobile.SolutionHelper.ICON_...);
	 */
	@Deprecated
	public static final String ICON_GEAR = IBaseSMButtonConstants.ICON_GEAR;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_LEFT = IBaseSMButtonConstants.ICON_LEFT;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_RIGHT = IBaseSMButtonConstants.ICON_RIGHT;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_UP = IBaseSMButtonConstants.ICON_UP;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_DOWN = IBaseSMButtonConstants.ICON_DOWN;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_DELETE = IBaseSMButtonConstants.ICON_DELETE;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_PLUS = IBaseSMButtonConstants.ICON_PLUS;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_MINUS = IBaseSMButtonConstants.ICON_MINUS;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_CHECK = IBaseSMButtonConstants.ICON_CHECK;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_REFRESH = IBaseSMButtonConstants.ICON_REFRESH;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_FORWARD = IBaseSMButtonConstants.ICON_FORWARD;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_BACK = IBaseSMButtonConstants.ICON_BACK;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_GRID = IBaseSMButtonConstants.ICON_GRID;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_STAR = IBaseSMButtonConstants.ICON_STAR;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_ALERT = IBaseSMButtonConstants.ICON_ALERT;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_INFO = IBaseSMButtonConstants.ICON_INFO;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_HOME = IBaseSMButtonConstants.ICON_HOME;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_SEARCH = IBaseSMButtonConstants.ICON_SEARCH;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_EDIT = IBaseSMButtonConstants.ICON_EDIT;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMButtonConstants#ICON_GEAR
	 */
	@Deprecated
	public static final String ICON_BARS = IBaseSMButtonConstants.ICON_BARS;

}
