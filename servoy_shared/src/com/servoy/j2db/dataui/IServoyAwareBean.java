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
package com.servoy.j2db.dataui;

import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.plugins.IClientPluginAccess;

/**
 * Interface to be used by beans to make them aware of Servoy.
 * 
 * If you implement this interface you will get the selected record of the foundset.
 * <p>
 * If this bean is used in a List like TableView or ListView then setSelectedRecord will be called
 * more often. And the given record doesnt have to be the selected record of the foundset but is then
 * the record where the bean should render it self with.
 * 
 * @author jcompagner
 * 
 * @since 4.0
 * 
 * @see IServoyAwareVisibilityBean if you also want to be notified on visibility changes.
 */
public interface IServoyAwareBean extends IDisplay
{
	/**
	 * Initializes the bean. Called by Servoy after the bean is created.
	 * 
	 * This method is not called when this bean is created through the {@link IServoyBeanFactory#getBeanInstance(int, IClientPluginAccess, Object[])}
	 * you have to call initialize yourself then.
	 * 
	 * @param access object that gives plugins access to parts of Servoy.
	 */
	public void initialize(IClientPluginAccess access);

	/**
	 * Applies the record, currently selected record for editing or the record to render itself with, to the bean.
	 * 
	 * @param record the record for which the bean should render itself with.
	 */
	public void setSelectedRecord(IRecord record);
}