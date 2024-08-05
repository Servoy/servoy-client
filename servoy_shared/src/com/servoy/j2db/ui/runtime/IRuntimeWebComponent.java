/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.solutionmodel.ICSSPosition;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeWebComponent", scriptingName = "RuntimeWebComponent", extendsComponent = "Component")
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
public interface IRuntimeWebComponent extends IBaseForInterfaceRuntimeComponent
{
	/**
	 * CSS position is a replacement for anchoring system making it more intuitive to place a component.
	 * CSS position should be set on form, an absolute position form can either work with anchoring or with css position.
	 *
	 * This property is only available when the form in is css positioning mode, in responsive mode components don't have this property.
	 *
	 * This is only working in NGClient.
	 *
	 * @sample
	 * elements.button.cssPosition.r("10").b("10").w("20%").h("30px")
	 */
	@JSGetter
	@ServoyClientSupport(ng = true, wc = false, sc = false, mc = false)
	public ICSSPosition getCssPosition();

	@JSSetter
	@ServoyClientSupport(ng = true, wc = false, sc = false, mc = false)
	public void setCssPosition(ICSSPosition cssPosition);
}
