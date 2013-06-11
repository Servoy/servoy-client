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
import com.servoy.base.scripting.solutionhelper.IBaseSHInsetList;

/**
 * Interface for mobile client inset list manipulation.
 * @author acostescu
 */
@ServoyClientSupport(mc = true, wc = false, sc = false)
public interface ISHInsetList extends IBaseSHInsetList, ISHList
{

	/**
	 * Name of the relation to be used by the inset list.
	 * @sample
	 * var insetList = plugins.mobile.solutionHelper.getInsetList(f,'il1');
	 * 
	 * var newInsetList = plugins.mobile.solutionHelper.createInsetList(f,8,insetList.dataSource,insetList.relationName,insetList.headerText,insetList.textDataProviderID);
	 * newInsetList.name = 'il2';
	 */
	@JSGetter
	public String getRelationName();

	@JSSetter
	public void setRelationName(String relationName);

	/**
	 * This text will appear on top of the inset list if 'headerDataProviderID' is not set.
	 * @sampleas {@link #getRelationName()}
	 */
	@JSGetter
	public String getHeaderText();

	@JSSetter
	public void setHeaderText(String headerText);

	/**
	 * The text of this data-provider will appear on top of the inset list; if not specified, 'headerText' will be used instead.
	 * @sampleas {@link #getRelationName()}
	 */
	@JSGetter
	public String getHeaderDataProviderID();

	@JSSetter
	public void setHeaderDataProviderID(String headerDataProviderID);

	/**
	 * The name of this inset list.
	 * @sampleas {@link #getRelationName()}
	 */
	@JSGetter
	public String getName();

	@JSSetter
	public void setName(String name);

	/**
	 * The styleClass of the list header. Can have values from 'a' to 'e'.
	 * 
	 * @sample
	 * var newInsetList = plugins.mobile.solutionHelper.createInsetList(f,8,insetList.dataSource,insetList.relationName,insetList.headerText,insetList.textDataProviderID);
	 * newInsetList.headerStyleClass = 'e';
	 */
	@JSGetter
	public String getHeaderStyleClass();

	@JSSetter
	public void setHeaderStyleClass(String styleClass);

}
