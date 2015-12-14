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

package com.servoy.j2db.documentation.persistence.docs;

import java.awt.Dimension;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IRepository;

/**
 * Dummy class for use in the documentation generator.
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Label", scriptingName = "Label", realClass = GraphicalComponent.class, typeCode = IRepository.GRAPHICALCOMPONENTS)
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class DocsLabel extends BaseDocsGraphicalComponentWithTitle
{

	private static final long serialVersionUID = 1L;

	/**
	 * Label (header) size property.
	 * Sets the header size on a label. Valid values are between 'h1' to 'h6',
	 * default value is 'h4'.
	 * The numbers match the heading tags h1 to h6 as used in html.
	 */
	@ServoyClientSupport(mc = true, wc = false, sc = false)
	public int getLabelSize()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public void setLabelSize(int labelSize)
	{
	}

	@ServoyClientSupport(mc = true, wc = false, sc = false)
	public boolean getTitleVisible()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public void setTitleVisible(boolean arg)
	{
	}

	@ServoyClientSupport(mc = false, wc = true, sc = true)
	@Override
	public Dimension getSize()
	{
		return super.getSize();
	}
}