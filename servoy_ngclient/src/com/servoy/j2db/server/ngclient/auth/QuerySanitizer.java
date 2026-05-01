package com.servoy.j2db.server.ngclient.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utilities for sanitizing and building query strings when forwarding requests to the cloud.
 */
@SuppressWarnings("nls")
public final class QuerySanitizer
{
	private static final Logger log = LoggerFactory.getLogger(QuerySanitizer.class);

	// --- Sanitization and limits to mitigate SSRF/abuse when forwarding query parameters ---
	public static final int MAX_PARAM_ENCODED_LENGTH = 4096; // encoded length
	public static final int MAX_TOTAL_QUERY_LENGTH = 8192;
	public static final int MAX_PARAM_NAME_LENGTH = 256;
	public static final boolean SANITIZE_TRUNCATE_ON_MAX = false; // default: reject oversized values
	public static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

	private QuerySanitizer()
	{
	}

	/**
	 * Build a url-encoded query string from parameters, skipping any keys present in skipKeys.
	 */
	public static String buildQueryString(Map<String, String[]> params, Set<String> skipKeys)
	{
		if (params == null || params.isEmpty())
		{
			return "";
		}

		StringBuilder query = new StringBuilder();
		int totalLength = 0;

		for (Map.Entry<String, String[]> entry : params.entrySet())
		{
			String rawKey = entry.getKey();
			if (skipKeys != null && skipKeys.contains(rawKey)) continue;
			String name = sanitizeParamName(rawKey);
			if (name == null || name.isEmpty()) continue;
			String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
			String[] values = entry.getValue();
			if (values == null) continue;
			for (String value : values)
			{
				if (StringUtils.isBlank(value)) continue;
				String sanitizedValue = sanitizeParamValue(value);
				String encodedValue = URLEncoder.encode(sanitizedValue, StandardCharsets.UTF_8);

				// enforce per-parameter encoded length
				if (encodedValue.length() > MAX_PARAM_ENCODED_LENGTH)
				{
					if (SANITIZE_TRUNCATE_ON_MAX)
					{
						encodedValue = encodedValue.substring(0, MAX_PARAM_ENCODED_LENGTH);
						log.warn("Truncating parameter '{}' to {} characters", name, MAX_PARAM_ENCODED_LENGTH);
					}
					else
					{
						throw new IllegalArgumentException("Encoded parameter '" + name + "' exceeds max allowed length");
					}
				}

				int added = (query.length() == 0 ? 0 : 1) + encodedName.length() + 1 + encodedValue.length(); // [&]name=value
				if (totalLength + added > MAX_TOTAL_QUERY_LENGTH)
				{
					// total query too large
					if (SANITIZE_TRUNCATE_ON_MAX)
					{
						int remain = MAX_TOTAL_QUERY_LENGTH - totalLength - (query.length() == 0 ? 0 : 1) - encodedName.length() - 1;
						if (remain <= 0)
						{
							throw new IllegalArgumentException("No space left for parameter '" + name + "' in query string");
						}
						if (encodedValue.length() > remain)
						{
							encodedValue = encodedValue.substring(0, remain);
							log.warn("Truncating parameter '{}' to fit total query length", name);
						}
					}
					else
					{
						throw new IllegalArgumentException("Total query string would exceed max allowed length");
					}
				}

				if (query.length() > 0)
				{
					query.append('&');
				}
				query.append(encodedName).append('=').append(encodedValue);
				totalLength += added;
			}
		}

		return query.toString();
	}

	/**
	 * Build a url-encoded query string from parameters
	 */
	public static String buildQueryString(Map<String, String[]> params)
	{
		return buildQueryString(params, Collections.emptySet());
	}

	public static String sanitizeParamName(String name)
	{
		if (name == null) return null;
		String cleaned = CONTROL_CHARS.matcher(name).replaceAll("").trim();
		cleaned = cleaned.replaceAll("\\s+", "_");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cleaned.length(); i++)
		{
			char c = cleaned.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_' || c == '-' || c == '.')
			{
				sb.append(c);
			}
			else
			{
				sb.append('_');
			}
			if (sb.length() >= MAX_PARAM_NAME_LENGTH) break;
		}
		String result = sb.toString();
		if (result.isEmpty()) return null;
		return result;
	}

	public static String sanitizeParamValue(String value)
	{
		if (value == null) return "";
		String cleaned = CONTROL_CHARS.matcher(value).replaceAll("");
		cleaned = cleaned.trim();
		return cleaned;
	}
}
