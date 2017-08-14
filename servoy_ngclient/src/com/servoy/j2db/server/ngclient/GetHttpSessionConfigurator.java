package com.servoy.j2db.server.ngclient;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * A class to store the http session (if set) into the user properties of the endpoint configuration so that the end point can pick this up.
 *
 * @author jcomp
 * @since 8.2.1
 */
public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator
{
	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response)
	{
		HttpSession httpSession = (HttpSession)request.getHttpSession();
		if (httpSession != null)
		{
			config.getUserProperties().put(HttpSession.class.getName(), httpSession);
		}
	}
}