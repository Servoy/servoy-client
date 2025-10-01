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

package com.servoy.j2db.ui;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;

/**
 * The <code>IScriptMobileBean</code> interface provides scripting support for mobile bean components in Servoy,
 * enabling management of <code>text</code> (deprecated) and <code>innerHTML</code> properties for customizing displayed content.
 *
 * @author gboros
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeBean", extendsComponent = "RuntimeComponent")
@ServoyClientSupport(mc = true, wc = false, sc = false)
public interface IScriptMobileBean extends IRuntimeComponent
{
	/**
	 * Gets or sets the text that is displayed on the bean
	 *
	 * @sample
	 * //gets the text of the element
	 * var my_text = %%prefix%%%%elementName%%.text;
	 *
	 * //sets the text of the element
	 * %%prefix%%%%elementName%%.text = my_text + 'is cool';
	 *
	 * @deprecated by innerHTML property
	 */
	@JSGetter
	@Deprecated
	public String getText();

	@JSSetter
	@Deprecated
	public void setText(String txt);


	/**
	 * Gets or sets the innerHTML that is displayed on the bean's div
	 *
	 * @sample
	 * //gets the innerHTML of the element
	 * var my_bean_innerHTML = %%prefix%%%%elementName%%.innerHTML;
	 *
	 * //sets the innerHTML of the element
	 * %%prefix%%%%elementName%%.innerHTML = '<div>my inner HTML</div>';
	 */
	@JSGetter
	public String getInnerHTML();

	@JSSetter
	public void setInnerHTML(String innerHTML);
}