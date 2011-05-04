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

import java.awt.Dimension;
import java.awt.Point;

public interface ISupportCachedLocationAndSize
{
	public Point getCachedLocation();

	public Dimension getCachedSize();

	public void setCachedLocation(Point location);

	public void setCachedSize(Dimension size);
}
//TODO maybe this interface & it's use can be simplified/removed
//this interface might only be useful in current implementation to keep initial design Y location of components for smart-client TableView - as the X and width
//should not get altered on cell renderer/editor repositioning/painting; and the Y is used for ordering columns when a column changes location from script