/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.scripting.solutionhelper;

import com.servoy.base.scripting.annotations.ServoyMobile;
import com.servoy.base.scripting.api.solutionmodel.IBaseSMMethod;

/**
 * Interface for mobile client inset list/list form manipulation.
 * @author acostescu
 */
@ServoyMobile
public interface IBaseSHList
{
	public String getCountDataProviderID();

	public void setCountDataProviderID(String countDataProviderID);

	public String getText();

	public void setText(String text);

	public String getTextDataProviderID();

	public void setTextDataProviderID(String textDataPRoviderID);

	public String getSubtext();

	public void setSubtext(String subtext);

	public String getSubtextDataProviderID();

	public void setSubtextDataProviderID(String subtextDataProviderID);

	public String getDataIconType();

	public void setDataIconType(String dataIconType);

	public String getDataIconDataProviderID();

	public void setDataIconDataProviderID(String dataIconDataProviderID);

	public void setOnAction(IBaseSMMethod method);

	public IBaseSMMethod getOnAction();

}
