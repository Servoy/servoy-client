/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.sablo.BaseWebObject;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.impl.ClientService;

/**
 * Client service that provides our {@link IServoyDataConverterContext} to the service object for data conversion.
 *
 * @author jcompagner
 *
 */
public class ServoyClientService extends ClientService implements IContextProvider, INGWebObject
{
	private final INGClientWebsocketSession session;

	public ServoyClientService(String serviceName, WebObjectSpecification spec, INGClientWebsocketSession session)
	{
		super(serviceName, spec);
		this.session = session;
	}

	@Override
	public IServoyDataConverterContext getDataConverterContext()
	{
		return new ServoyDataConverterContext(session.getClient());
	}

	@Override
	public void executeAsyncServiceCall(String functionName, Object[] arguments)
	{
		super.executeAsyncServiceCall(functionName, arguments);
		session.valueChanged();
	}

	@Override
	public BaseWebObject getUnderlyingWebObject()
	{
		return this;
	}
}
