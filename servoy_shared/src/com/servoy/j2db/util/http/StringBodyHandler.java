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

package com.servoy.j2db.util.http;

import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author jcompagner
 *
 */
public abstract class StringBodyHandler<T> implements BodyHandler<T>
{

	@Override
	public BodySubscriber<T> apply(ResponseInfo responseInfo)
	{
		Charset charset = getCharset(responseInfo);
		return BodySubscribers.mapping(
			BodySubscribers.ofString(charset),
			(String content) -> {
				return handleResponse(responseInfo, content);
			});
	}

	@SuppressWarnings("nls")
	private Charset getCharset(ResponseInfo info)
	{
		Optional<String> contentTypeOpt = info.headers().firstValue("Content-Type");
		if (contentTypeOpt.isPresent())
		{
			String contentType = contentTypeOpt.get();
			// Example Content-Type: "text/html; charset=UTF-8"
			String[] parts = contentType.split(";");
			for (String part : parts)
			{
				part = part.trim();
				if (part.toLowerCase().startsWith("charset="))
				{
					return Charset.forName(part.substring(8).trim());
				}
			}
		}
		return StandardCharsets.UTF_8; // Default charset if not specified
	}

	public abstract T handleResponse(ResponseInfo responseInfo, String content);

}
