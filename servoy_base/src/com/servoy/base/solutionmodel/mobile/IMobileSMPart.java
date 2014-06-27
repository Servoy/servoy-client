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

package com.servoy.base.solutionmodel.mobile;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMPart;

/**
 * Interface for mobile parts.
 * 
 * @author acostescu
 */
@ServoyClientSupport(mc = true, wc = false, sc = false)
public interface IMobileSMPart extends IBaseSMPart
{

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A header that won't changes position when the page content is scrolled 
	 * 
	 * @sample
	 * var stickyHeader = form.newPart(JSPart.STICKY_HEADER);
	 */
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public static final int STICKY_HEADER = TITLE_HEADER;


	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A footer that won't changes position when the page content is scrolled 
	 * 
	 * @sample
	 * var stickyHeader = form.newPart(JSPart.STICKY_FOOTER);
	 */
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public static final int STICKY_FOOTER = TITLE_FOOTER;

}
