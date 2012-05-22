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

package com.servoy.j2db.server.headlessclient;

import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestListenerInterface;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.component.listener.BehaviorRequestTarget;

/**
 * @author jcompagner
 *
 */
public interface AlwaysLastPageVersionRequestListenerInterface extends IBehaviorListener
{
	/** Behavior listener interface */
	public static final RequestListenerInterface INTERFACE = new RequestListenerInterface(AlwaysLastPageVersionRequestListenerInterface.class, false)
	{
		/**
		 * 
		 * @see org.apache.wicket.RequestListenerInterface#newRequestTarget(org.apache.wicket.Page,
		 *      org.apache.wicket.Component, org.apache.wicket.RequestListenerInterface,
		 *      org.apache.wicket.request.RequestParameters)
		 */
		@Override
		public IRequestTarget newRequestTarget(Page page, Component component, RequestListenerInterface listener, RequestParameters requestParameters)
		{
			return new BehaviorRequestTarget(page, component, listener, requestParameters);
		}
	};

}
