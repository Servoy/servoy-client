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

package com.servoy.j2db.scripting.solutionmodel;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;


/**
 * The <code>CSSPosition</code> interface in the Servoy environment provides methods for managing and manipulating CSS position properties such as <code>left</code>, <code>right</code>, <code>top</code>, <code>bottom</code>, <code>width</code>, and <code>height</code>.
 * It allows both getting and setting these properties in pixels or percentages, with chaining methods for streamlined updates.
 * The interface supports use cases in Servoy NG Client only.
 *
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "CSSPosition", publicName = "CSSPosition")
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public interface ICSSPosition
{

	/**
	 * Get/Set left css position (in pixels or percent).
	 *
	 * @sample
	 * var left = comp.cssPosition.left;
	 *
	 */
	@JSGetter
	String getLeft();

	@JSSetter
	void setLeft(String left);

	/**
	 * Get/Set right css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.right
	 *
	 */
	@JSGetter
	String getRight();

	@JSSetter
	void setRight(String right);

	/**
	 * Get/Set top css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.top
	 *
	 */
	@JSGetter
	String getTop();

	@JSSetter
	void setTop(String top);

	/**
	 * Get/Set bottom css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.bottom
	 *
	 */
	@JSGetter
	String getBottom();

	@JSSetter
	void setBottom(String bottom);

	/**
	 * Get/Set width css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.width
	 *
	 * @return width css position
	 */
	@JSGetter
	String getWidth();

	@JSSetter
	void setWidth(String width);

	/**
	 * Get/Set height css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.height
	 *
	 */
	@JSGetter
	String getHeight();

	@JSSetter
	void setHeight(String height);

	/**
	 * Set left css position (in pixels or percent).
	 *
	 * @param left left position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	ICSSPosition l(String left);

	/**
	 * Set right css position (in pixels or percent).
	 *
	 * @param right right position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.r("10").b("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	ICSSPosition r(String right);

	/**
	 * Set top css position (in pixels or percent).
	 *
	 * @param top top position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	ICSSPosition t(String top);

	/**
	 * Set bottom css position (in pixels or percent).
	 *
	 * @param bottom bottom position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.r("10").b("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	ICSSPosition b(String bottom);

	/**
	 * Set width css position (in pixels or percent).
	 *
	 * @param width width position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	ICSSPosition w(String width);

	/**
	 * Set height css position (in pixels or percent).
	 *
	 * @param height height position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	ICSSPosition h(String height);

}