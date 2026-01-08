/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.auth;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author emera
 */
public class AuthenticatorManager
{
	private static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static Solution findAuthenticator(Solution solution)
	{
		Solution authenticator = null;
		String modulesNames = solution.getModulesNames();
		IRepository localRepository = ApplicationServerRegistry.get().getLocalRepository();
		for (String moduleName : Utils.getTokenElements(modulesNames, ",", true))
		{
			try
			{
				Solution module = (Solution)localRepository.getActiveRootObject(moduleName, IRepository.SOLUTIONS);
				if (module != null && module.getSolutionType() == SolutionMetaData.AUTHENTICATOR)
				{
					authenticator = module;
					break;
				}
			}
			catch (RepositoryException e)
			{
				log.error("Cannot find authenticator.", e);
			}
		}
		return authenticator;
	}

	public static boolean callAuthenticator(Pair<Boolean, String> needToLogin, HttpServletRequest request, boolean rememberUser, Solution authenticator,
		JSONObject json, String refreshToken)
	{
		addCustomParameters(request, json);
		Credentials credentials = new Credentials(null, authenticator.getName(), null, json.toString());
		IApplicationServer applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
		try
		{
			ClientLogin login = applicationServer.login(credentials);
			if (login != null)
			{
				SvyTokenBuilder builder = new SvyTokenBuilder(login.getUserName(), login.getUserUid(), login.getUserGroups())//
					.withRememberUser(Boolean.valueOf(rememberUser)) //
					.withRefreshToken(refreshToken);
				String token = builder.sign();
				needToLogin.setLeft(Boolean.FALSE);
				needToLogin.setRight(token);
				return true;
			}
		}
		catch (RemoteException | RepositoryException e)
		{
			log.error("Cannot call authenticator.", e);
		}
		return false;
	}

	private static void addCustomParameters(HttpServletRequest request, JSONObject json)
	{
		Map<String, String[]> parameters = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : parameters.entrySet())
		{
			if (entry.getKey().startsWith("custom_"))
			{
				String[] values = entry.getValue();
				for (String value : values)
				{
					json.put(entry.getKey(), value);
				}
			}

			if (entry.getKey().startsWith(OAuthParameters.state.name()))
			{
				String[] values = entry.getValue();
				if (values.length > 0)
				{
					String stateValue = values[0];
					//state has a 'state' and a 'query' and a uuid (which the authenticator does not need)
					addParsedStateParameterToJson(stateValue, json);
				}
			}
		}
	}

	private static void addParsedStateParameterToJson(String stateValue, JSONObject json)
	{
		Map<String, String> params = Arrays.stream(stateValue.split("&"))
			.map(p -> p.split("=", 2))
			.collect(Collectors.toMap(
				p -> p[0],
				p -> p.length > 1 ? p[1] : "",
				(a, b) -> a,
				LinkedHashMap::new // preserve order
			));

		if (params.containsKey(OAuthParameters.state.name()))
		{
			String decodedState = URLDecoder.decode(params.get(OAuthParameters.state.name()), StandardCharsets.UTF_8);
			json.put(OAuthParameters.state.name(), decodedState);
		}

		// remove the OAuth specific params
		params.remove(OAuthParameters.state.name());
		params.remove("svyuuid");

		String originalQuery = params.entrySet().stream()
			.map(e -> e.getKey() + "=" + e.getValue())
			.collect(Collectors.joining("&"));
		json.put("query", originalQuery);
	}

	public static boolean checkAuthenticatorPermissions(String username, String password, boolean remember, SvyID oldToken, Pair<Boolean, String> needToLogin,
		Solution solution,
		HttpServletRequest request)
	{
		Solution authenticator = findAuthenticator(solution);
		if (authenticator != null)
		{
			JSONObject json = new JSONObject();
			json.put(SvyID.USERNAME, oldToken != null ? oldToken.getUsername() : username);
			json.put(StatelessLoginHandler.PASSWORD, password);
			String refreshToken = null;
			if (oldToken != null)
			{
				String payload = new String(java.util.Base64.getUrlDecoder().decode(oldToken.getPayload()));
				JSONObject token = new JSONObject(payload);
				json.put(SvyID.LAST_LOGIN, token);
				refreshToken = oldToken.getStringClaim(StatelessLoginHandler.REFRESH_TOKEN);
			}

			return callAuthenticator(needToLogin, request, remember, authenticator, json, refreshToken);
		}
		else
		{
			log.error("Trying to login in solution " + solution.getName() +
				" with using an AUTHENTICATOR solution, but the main solution doesn't have that as a module");
		}
		return false;
	}
}