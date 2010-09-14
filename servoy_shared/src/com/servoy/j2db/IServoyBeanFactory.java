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
package com.servoy.j2db;

import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.ui.IComponent;

/**
 * Interface to leave type creation to bean implementor, so that the implementor can deliver multiple instances like SWT/Swing/Wicket
 * @author jblok
 */
public interface IServoyBeanFactory extends ISupportName
{
	/**
	 * Factory method to create the actual component, the servoyApplicationType is one the {@link IClientPluginAccess} client constants like
	 * {@link IClientPluginAccess#CLIENT} for the smart client.
	 * 
	 * The first argument of the cargs parameter is always set and is the name that should be used as the wicket id for the wicket componet.
	 * This argument is not needed for the swing component.
	 * 
	 * If the component that is returned implements {@link IServoyAwareBean} then the {@link IServoyAwareBean#initialize(IClientPluginAccess)} is not called on that one.
	 * you have to do that yourself.
	 * 
	 * @param servoyApplicationType The client's application type.
	 * @param access The {@link IClientPluginAccess} 
	 * @param cargs Arguments for this bean, the first argument is the bean (wicket)id, the second is the form name, the 3th is the form stylesheet name
	 * 
	 * @return The actual bean component
	 */
	public IComponent getBeanInstance(int servoyApplicationType, IClientPluginAccess access, Object[] cargs);
}
