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

package com.servoy.j2db.server.ngclient;

import java.util.Map;

import org.sablo.IChangeListener;
import org.sablo.specification.WebComponentSpec;

import com.servoy.j2db.IBasicFormUI;
import com.servoy.j2db.IView;

/**
 * @author jcompagner
 *
 */
public interface IWebFormUI extends IBasicFormUI, IView, IChangeListener
{

	/**
	 * @return
	 */
	Map<String, Map<String, Object>> getAllProperties();

	/**
	 * @param string
	 * @return
	 */
	WebFormComponent getWebComponent(String name);

	/**
	 * @return
	 */
	Map<String, WebFormComponent> getWebComponents();

	/**
	 * @return
	 */
	IDataAdapterList getDataAdapterList();

	/**
	 * 
	 */
	void init();

	boolean isReadOnly();

	void setReadOnly(boolean readOnly);

	int recalculateTabIndex(int startIndex, TabSequencePropertyWithComponent startComponent);

	void setParentContainer(WebFormComponent parentContainer);

	int getNextAvailableTabSequence();

	public String getParentWindowName();

	public void setParentWindowName(String parentWindowName);

	public IDataConverterContext getDataConverterContext();

	public void contributeComponentToElementsScope(FormElement fe, WebComponentSpec componentSpec, WebFormComponent component);

}
