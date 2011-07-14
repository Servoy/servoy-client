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
package com.servoy.j2db.ui;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.FormScope;

/**
 * Interface to which split pane components need to conform, to expose the same script methods
 * 
 * @author gboros
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeSplitPane", extendsComponent = "RuntimeComponent")
public interface IScriptSplitPaneMethods extends IScriptTransparentMethods, IScriptReadOnlyMethods
{

	/**
	 * Set a relationless or related form as left panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setLeftForm(forms.orders);
	 *
	 * @param form the specified form or form name you wish to add as left panel
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setLeftForm(Object form);

	/**
	 * Set a relationless or related form as left panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setLeftForm(forms.orders,'orders_to_order_details');
	 *
	 * @param form the specified form or form name you wish to add as left panel
	 * @param relation the relation name or a related foundset or null for relationless
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setLeftForm(Object form, Object relation);


	/**
	 * Returns the left form of the split pane.
	 * 
	 * @sample var leftForm = %%prefix%%%%elementName%%.getLeftForm();
	 * 
	 * @return left form of the split pane
	 */
	public FormScope js_getLeftForm();

	/**
	 * Set a relationless or related form as right panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setRightForm(forms.orders);
	 *
	 * @param form the specified form or form name you wish to add as right panel
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setRightForm(Object form);

	/**
	 * Set a relationless or related form as right panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setRightForm(forms.orders,'orders_to_order_details');
	 *
	 * @param form the specified form or form name you wish to add as right panel
	 * @param relation the relation name or a related foundset or null for relationless
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setRightForm(Object form, Object relation);

	/**
	 * Returns the right form of the split pane.
	 * 
	 * @sample var rightForm = %%prefix%%%%elementName%%.getRightForm();
	 * 
	 * @return right form of the split pane
	 */
	public FormScope js_getRightForm();

	/**
	 * Gets or sets divider location.
	 * If location is less then 1 then the location will be considered at (location * 100) percent of the split pane from left,
	 * otherwise it will represent the pixels from left.
	 * @sample %%prefix%%%%elementName%%.dividerLocation = 0.75;
	 */
	public double js_getDividerLocation();

	public void js_setDividerLocation(double location);

	/**
	 * Gets or sets divider size in pixels.
	 * @sample %%prefix%%%%elementName%%.dividerSize = 10;
	 */
	public int js_getDividerSize();

	public void js_setDividerSize(int size);

	/**
	 * Specifies how to distribute extra space when the size of the split pane changes.
	 * A value of 0, the default, indicates the right/bottom component gets all the extra space (the left/top component acts fixed),
	 * where as a value of 1 specifies the left/top component gets all the extra space (the right/bottom component acts fixed).
	 * Specifically, the left/top component gets (weight * diff) extra space and the right/bottom component gets (1 - weight) * diff extra space
	 * @sample %%prefix%%%%elementName%%.resizeWeight = 0.5;
	 */
	public double js_getResizeWeight();

	public void js_setResizeWeight(double resizeWeight);

	/**
	 * Gets or sets if the components should continuously be redrawn as the divider changes position.
	 * @sample %%prefix%%%%elementName%%.continuousLayout = true;
	 */
	public boolean js_getContinuousLayout();

	public void js_setContinuousLayout(boolean b);

	/**
	 * Gets or sets right form minimum size in pixels.
	 * @sample %%prefix%%%%elementName%%.rightFormMinSize = 100;
	 */
	public int js_getRightFormMinSize();

	public void js_setRightFormMinSize(int minSize);

	/**
	 * Gets or sets left form minimum size in pixels.
	 * @sample %%prefix%%%%elementName%%.leftFormMinSize = 100;
	 */
	public int js_getLeftFormMinSize();

	public void js_setLeftFormMinSize(int minSize);
}
