/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.ResourceReference;

import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.ISupportWebBounds;

/**
 * Interface for components that can display image
 * @author gboros
 *
 */
public interface IImageDisplay extends IComponent, ISupportWebBounds
{
	public MediaResource getIcon();

	public ResourceReference getIconReference();

	public Media getMedia();

	public int getMediaOptions();

	public String getIconUrl();

	public String getTextUrl();

	public ResourceReference getRolloverIconReference();

	public Media getRolloverMedia();

	public String getRolloverUrl();
}
