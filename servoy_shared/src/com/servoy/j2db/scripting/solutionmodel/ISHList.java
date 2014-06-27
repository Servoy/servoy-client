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

package com.servoy.j2db.scripting.solutionmodel;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.scripting.solutionhelper.IBaseSHList;
import com.servoy.base.solutionmodel.IBaseSMMethod;

/**
 * Interface for mobile client inset list/list form manipulation.
 * @author acostescu
 */
@ServoyClientSupport(mc = true, wc = false, sc = false)
public interface ISHList extends IBaseSHList
{
	@JSGetter
	public String getCountDataProviderID();

	@JSSetter
	public void setCountDataProviderID(String countDataProviderID);

	@JSGetter
	public String getText();

	@JSSetter
	public void setText(String text);

	@JSGetter
	public String getTextDataProviderID();

	@JSSetter
	public void setTextDataProviderID(String textDataPRoviderID);

	@JSSetter
	public void setOnAction(IBaseSMMethod method);

	@JSGetter
	public IBaseSMMethod getOnAction();

	@JSGetter
	public String getSubtext();

	@JSSetter
	public void setSubtext(String subtext);

	@JSGetter
	public String getSubtextDataProviderID();

	@JSSetter
	public void setSubtextDataProviderID(String subtextDataProviderID);

	@JSGetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String getDataIconType();

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setDataIconType(String iconType);

	@JSGetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String getDataIconDataProviderID();

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setDataIconDataProviderID(String dataIconDataProviderID);

	@JSGetter
	public String getListStyleClass();

	@JSSetter
	public void setListStyleClass(String styleClass);
}
