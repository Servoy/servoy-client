/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.solutionmodel.mobile;

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * Solution model constants for mobile JSButton.
 * 
 * @author rgansevles
 */
@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
public interface IMobileSMButtonConstants
{

	/** Constant for specifying a predefined icon type for a button.
	 * 
	 * @sample myButton.iconType = JSButton.ICON_...;
	 */
	public static final String ICON_GEAR = "gear"; // DataIcon.GEAR.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_LEFT = "arrow-l"; // DataIcon.LEFT.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_RIGHT = "arrow-r"; // DataIcon.RIGHT.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_UP = "arrow-u"; // DataIcon.UP.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_DOWN = "arrow-d"; // DataIcon.DOWN.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_DELETE = "delete"; // DataIcon.DELETE.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_PLUS = "plus"; // DataIcon.PLUS.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_MINUS = "minus"; // DataIcon.MINUS.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_CHECK = "check"; // DataIcon.CHECK.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_REFRESH = "refresh"; // DataIcon.REFRESH.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_FORWARD = "forward"; // DataIcon.FORWARD.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_BACK = "back"; // DataIcon.BACK.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_GRID = "grid"; // DataIcon.GRID.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_STAR = "star"; // DataIcon.STAR.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_ALERT = "alert"; // DataIcon.ALERT.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_INFO = "info"; // DataIcon.INFO.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_HOME = "home"; // DataIcon.HOME.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_SEARCH = "search"; // DataIcon.SEARCH.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_BARS = "bars"; // DataIcon.BARS.getJqmValue() //$NON-NLS-1$
	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants#ICON_GEAR
	 */
	public static final String ICON_EDIT = "edit"; // DataIcon.EDIT.getJqmValue() //$NON-NLS-1$

}
