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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.InvalidPublicKeyException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.util.Pair;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author emera
 */
public class JWTValidator
{
	static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static boolean verifyJWT(DecodedJWT decodedJWT, String jwks_uri) throws MalformedURLException
	{
		try
		{
			final JwkProvider jwkStore = new UrlJwkProvider(new URL(jwks_uri));
			if (decodedJWT.getKeyId() == null)
			{
				log.error("Cannot verify the token with jwks '" + jwks_uri //
					+ "' because the key id is missing in the token header.");
			}
			Algorithm algorithm = getAlgo(decodedJWT, jwkStore);
			com.auth0.jwt.JWTVerifier verifier = JWT.require(algorithm).acceptIssuedAt(60).acceptNotBefore(60).build();
			verifier.verify(decodedJWT);
		}
		catch (JwkException e)
		{
			log.error("Cannot verify the id_token with the key set.", e);
			return false;
		}
		catch (JWTVerificationException e)
		{
			log.error("Cannot verify the id_token", e);
			return false;
		}
		return true;
	}

	private static Algorithm getAlgo(DecodedJWT decodedJWT, final JwkProvider jwkStore) throws JwkException, InvalidPublicKeyException
	{
		Jwk jwk = jwkStore.get(decodedJWT.getKeyId());
		String algo = decodedJWT.getAlgorithm();
		PublicKey publicKey = jwk.getPublicKey();
		switch (algo)
		{
			case "RS256" :
				return Algorithm.RSA256((RSAPublicKey)publicKey, null);
			case "RS384" :
				return Algorithm.RSA384((RSAPublicKey)publicKey, null);
			case "RS512" :
				return Algorithm.RSA512((RSAPublicKey)publicKey, null);
			case "ES256" :
				return Algorithm.ECDSA256((ECPublicKey)publicKey, null);
			case "ES384" :
				return Algorithm.ECDSA384((ECPublicKey)publicKey, null);
			case "ES512" :
				return Algorithm.ECDSA512((ECPublicKey)publicKey, null);
		}
		return null;
	}

	static boolean checkOauthIdToken(Pair<Boolean, String> needToLogin, Solution solution, AUTHENTICATOR_TYPE authenticator,
		DecodedJWT decodedJWT, HttpServletRequest request, String refreshToken, boolean checkNonce)
	{
		if (!"svy".equals(decodedJWT.getIssuer()))
		{
			JSONObject auth = null;
			if (checkNonce)
			{
				//if token was refreshed it does not have nonce // TODO check
				String tokenNonce = decodedJWT.getClaim(OAuthParameters.nonce.name()).asString();
				auth = JWTValidator.checkNonce(request.getServletContext(), tokenNonce);
				if (auth == null)
				{
					log.error("The token was replayed or tampered with.");
					return false;
				}
			}
			if (auth.has(OAuthParameters.jwks_uri.name()))
			{
				try
				{
					if (verifyJWT(decodedJWT, auth.getString(OAuthParameters.jwks_uri.name())))
					{
						Boolean remember = Boolean.valueOf("offline".equals(auth.optString("access_type")));
						String payload = new String(java.util.Base64.getUrlDecoder().decode(decodedJWT.getPayload()));

						if (authenticator == AUTHENTICATOR_TYPE.OAUTH || authenticator == AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR)
						{
							JSONObject token = new JSONObject();
							JSONObject jsonObject = new JSONObject(payload);
							token.put(SvyID.LAST_LOGIN, jsonObject);
							Solution authenticatorModule = AuthenticatorManager.findAuthenticator(solution);
							if (authenticatorModule != null)
							{
								return AuthenticatorManager.callAuthenticator(needToLogin, request, remember, authenticatorModule, token, refreshToken);
							}
							else
							{
								log.error("Trying to login in solution " + solution.getName() +
									" with using an AUTHENTICATOR solution, but the main solution doesn't have that as a module");
							}
						}
						else if (authenticator == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
						{
							return CloudStatelessAccessManager.checkCloudOAuthPermissions(needToLogin, solution, payload, remember, refreshToken,
								auth.getString(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT)); //TODO where to move?
						}
					}
				}
				catch (MalformedURLException e)
				{
					log.error("The jwks url is malformed: " + auth.getString(OAuthParameters.jwks_uri.name()));
				}
			}
			else
			{
				log.error("The jwks_uri is missing.");
			}
		}

		return false;

	}

	/**
	 * This will return the value from the nonce context cache and remove it. (so return null means it was not a valid nonce)
	 * @param context
	 * @param nonceString
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static JSONObject checkNonce(ServletContext context, String nonceString)
	{
		if (nonceString != null)
		{
			Map<String, JSONObject> cache = (Map<String, JSONObject>)context.getAttribute(OAuthParameters.nonce.name());
			return cache.remove(nonceString);
		}
		return null;
	}
}
