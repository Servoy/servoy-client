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


/**
 * @author jcomp
 *
 */
public interface ICSSPosition
{

	/**
	 * Get/Set left css position (in pixels or percent).
	 *
	 * @sample
	 * var left = comp.cssPosition.left;
	 *
	 */
	String getLeft();

	void setLeft(String left);

	/**
	 * Get/Set right css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.right
	 *
	 */
	String getRight();

	void setRight(String right);

	/**
	 * Get/Set top css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.top
	 *
	 */
	String getTop();

	void setTop(String top);

	/**
	 * Get/Set bottom css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.bottom
	 *
	 */
	String getBottom();

	void setBottom(String bottom);

	/**
	 * Get/Set width css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.width
	 *
	 * @return width css position
	 */
	String getWidth();

	void setWidth(String width);

	/**
	 * Get/Set height css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.height
	 *
	 */
	String getHeight();

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
	ICSSPosition h(String height);

}