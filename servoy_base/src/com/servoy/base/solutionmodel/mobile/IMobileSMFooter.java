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
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMVariable;

/**
 * Footer part in solution model.
 * 
 * @author rgansevles
 *
 */
@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
public interface IMobileSMFooter extends IMobileSMPart
{
	public boolean getSticky();

	public void setSticky(boolean sticky);

	public IBaseSMField newField(IBaseSMVariable dataprovider, int type, int x);

	public IBaseSMField newField(String dataprovidername, int type, int x);

	public IMobileSMText newTextField(IBaseSMVariable dataprovider, int x);

	public IMobileSMText newTextField(String dataprovidername, int x);

	public IMobileSMTextArea newTextArea(IBaseSMVariable dataprovider, int x);

	public IMobileSMTextArea newTextArea(String dataprovidername, int x);

	public IMobileSMCombobox newCombobox(IBaseSMVariable dataprovider, int x);

	public IMobileSMCombobox newCombobox(String dataprovidername, int x);

	public IMobileSMRadios newRadios(IBaseSMVariable dataprovider, int x);

	public IMobileSMRadios newRadios(String dataprovidername, int x);

	public IMobileSMChecks newCheck(IBaseSMVariable dataprovider, int x);

	public IMobileSMChecks newCheck(String dataprovidername, int x);

	public IMobileSMPassword newPassword(IBaseSMVariable dataprovider, int x);

	public IMobileSMPassword newPassword(String dataprovidername, int x);

	public IMobileSMCalendar newCalendar(IBaseSMVariable dataprovider, int x);

	public IMobileSMCalendar newCalendar(String dataprovidername, int x);

	public IBaseSMButton newButton(String txt, int x, IBaseSMMethod method);

	public IBaseSMLabel newLabel(String txt, int x);

	public boolean removeComponent(String name);

	public IBaseSMComponent[] getComponents();

}
