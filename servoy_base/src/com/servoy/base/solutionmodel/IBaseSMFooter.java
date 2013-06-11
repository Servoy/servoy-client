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

package com.servoy.base.solutionmodel;

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * Footer part in solution model.
 * 
 * @author rgansevles
 *
 */
@ServoyClientSupport(mc = true, wc = false, sc = false)
public interface IBaseSMFooter extends IBaseSMPart
{
	public boolean getSticky();

	public void setSticky(boolean sticky);

	public IBaseSMField newField(IBaseSMVariable dataprovider, int type, int x);

	public IBaseSMField newField(String dataprovidername, int type, int x);

	public IBaseSMText newTextField(IBaseSMVariable dataprovider, int x);

	public IBaseSMText newTextField(String dataprovidername, int x);

	public IBaseSMTextArea newTextArea(IBaseSMVariable dataprovider, int x);

	public IBaseSMTextArea newTextArea(String dataprovidername, int x);

	public IBaseSMCombobox newCombobox(IBaseSMVariable dataprovider, int x);

	public IBaseSMCombobox newCombobox(String dataprovidername, int x);

	public IBaseSMRadios newRadios(IBaseSMVariable dataprovider, int x);

	public IBaseSMRadios newRadios(String dataprovidername, int x);

	public IBaseSMChecks newCheck(IBaseSMVariable dataprovider, int x);

	public IBaseSMChecks newCheck(String dataprovidername, int x);

	public IBaseSMPassword newPassword(IBaseSMVariable dataprovider, int x);

	public IBaseSMPassword newPassword(String dataprovidername, int x);

	public IBaseSMCalendar newCalendar(IBaseSMVariable dataprovider, int x);

	public IBaseSMCalendar newCalendar(String dataprovidername, int x);

	public IBaseSMButton newButton(String txt, int x, IBaseSMMethod method);

	public IBaseSMLabel newLabel(String txt, int x);

	public boolean removeComponent(String name);

	public IBaseSMComponent[] getComponents();

}
