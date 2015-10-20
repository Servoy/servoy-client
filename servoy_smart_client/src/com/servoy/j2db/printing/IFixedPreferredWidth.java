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
package com.servoy.j2db.printing;


/**
 * This interface is intended to be implemented by AWT components (java.awt.Component) that are able to calculate
 * a preferred height based on a given width.<BR><BR>
 * Normally, classes that implement this interface will also need to overwrite the
 * {@link java.awt.Component#getPreferredSize()} method to return the fixed preferred width if set and the
 * according preferred height.
 * @author acostescu
 */
public interface IFixedPreferredWidth
{

	/**
	 * Sets a fixed preferred width for this component. This means that the component's getPreferredSize() will
	 * return (after fixed width is set) a Dimension with the fixed with and the height that it would want to
	 * have for the specified fixed width.<BR>
	 * If preferredWidth == -1, the default getPreferredSize() behavior is restored.
	 * @param preferredWidth the preferred width that the Component will report and use to compute a
	 * preferred height. If -1, the default getPreferredSize() behavior is restored to the component.
	 */
	void setPreferredWidth(int preferredWidth);

}