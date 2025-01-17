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

import java.io.IOException;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class CloudResponseHandler implements HttpClientResponseHandler<Pair<Integer, JSONObject>>
{
	static final Logger log = LoggerFactory.getLogger("stateless.login");
	private final String endpoint;

	public CloudResponseHandler(String endpoint)
	{
		this.endpoint = endpoint;
	}

	@Override
	public Pair<Integer, JSONObject> handleResponse(ClassicHttpResponse response) throws HttpException, IOException
	{
		HttpEntity responseEntity = response.getEntity();
		if (responseEntity != null)
		{
			Pair<Integer, JSONObject> pair = new Pair<>(Integer.valueOf(response.getCode()), null);
			String responseString = EntityUtils.toString(responseEntity);
			if (responseString.startsWith("{"))
			{
				pair.setRight(new JSONObject(responseString));
			}
			else
			{
				log.error("The endpoint " + endpoint + " did not return json.");
			}
			return pair;
		}
		else
		{
			log.error("Could not access rest api endpoint " + endpoint + " " + response.getCode() + " " +
				response.getReasonPhrase());
		}
		return null;
	}
}