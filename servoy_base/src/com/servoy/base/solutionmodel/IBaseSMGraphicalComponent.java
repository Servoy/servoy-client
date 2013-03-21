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

package com.servoy.base.solutionmodel;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Solution model base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMGraphicalComponent extends IBaseSMComponent
{

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDataProviderID()
	 * 
	 * @sample
	 * // Normally the dataprovider is specified when a component is created.
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * // But it can be modified later if needed.
	 * field.dataProviderID = 'parent_table_id';
	 */
	public String getDataProviderID();

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDisplaysTags()
	 * 
	 * @sample
	 * var label = form.newLabel('You are viewing record no. %%parent_table_id%%. You are running on server %%serverURL%%.', 
	 *					10, 10, 600, 100);
	 * label.displaysTags = true;
	 */
	public boolean getDisplaysTags();

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getText()
	 * 
	 * @sample
	 * // In general the text is specified when creating the component.
	 * var label = form.newLabel('Initial text', 10, 10, 100, 20);
	 * // But it can be changed later if needed.
	 * label.text = 'Changed text';
	 */
	public String getText();

	public void setDataProviderID(String arg);

	public void setDisplaysTags(boolean arg);

	public void setText(String arg);

	public void setOnAction(IBaseSMMethod method);

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponent#getOnActionMethodID()
	 * 
	 * @sample
	 * var doNothingMethod = form.newMethod('function doNothing() { application.output("Doing nothing."); }');
	 * var onClickMethod = form.newMethod('function onClick(event) { application.output("I was clicked at " + event.getTimestamp()); }');
	 * var onDoubleClickMethod = form.newMethod('function onDoubleClick(event) { application.output("I was double-clicked at " + event.getTimestamp()); }');
	 * var onRightClickMethod = form.newMethod('function onRightClick(event) { application.output("I was right-clicked at " + event.getTimestamp()); }');
	 * // At creation the button has the 'doNothing' method as onClick handler, but we'll change that later.
	 * var btn = form.newButton('I am a button', 10, 40, 200, 20, doNothingMethod);
	 * btn.onAction = onClickMethod;
	 * btn.onDoubleClick = onDoubleClickMethod;
	 * btn.onRightClick = onRightClickMethod;
	 */
	public IBaseSMMethod getOnAction();

}