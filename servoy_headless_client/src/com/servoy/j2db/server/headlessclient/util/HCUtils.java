/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.headlessclient.util;

import static java.lang.Integer.parseInt;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import com.servoy.j2db.util.HtmlUtils;

import ua_parser.OS;
import ua_parser.Parser;
import ua_parser.UserAgent;

/**
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class HCUtils
{
	private static String[] ALL_TAGS = new String[] { "a", "b", "blockquote", "br", "caption", "cite", "code", "col", "colgroup", //
		"dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6", "i", "img", "li", "ol", "p", "pre", "q", "small", "span", //
		"strike", "strong", "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u", "ul" };

	private static final Safelist WHITELIST;

	static
	{
		WHITELIST = Safelist.relaxed() //
			.preserveRelativeLinks(true) //
			.addTags("html", "head", "body", "style") //
			.addAttributes("style", "type") //
			.addAttributes("table", "border") //
			.addProtocols("a", "href", "javascript") //
			.addProtocols("img", "src", "media") //
		;
		for (String tag : ALL_TAGS)
		{
			WHITELIST.addAttributes(tag, "class", "style", "align");
		}
	}

	private static final Parser USER_AGENT_PARSER = new Parser();

	/**
	 * Sanitize html against XSS attacks.
	 *
	 * @param html
	 * @return sanitized html
	 */
	public static String sanitize(CharSequence html)
	{
		if (html == null)
		{
			return null;
		}

		if (!HtmlUtils.startsWithHtml(html))
		{
			// just some html, not an entire document
			String sanitizedBody = Jsoup.clean(html.toString(), "http://any", WHITELIST);
			// remove body wrapper
			if (sanitizedBody.startsWith("<body>") && sanitizedBody.endsWith("</body>"))
			{
				sanitizedBody = sanitizedBody.substring(6, sanitizedBody.length() - 7);
			}
			return sanitizedBody;
		}


		// parse entire html
		Document dirty = Jsoup.parse(html.toString(), "http://any");

		// wrap with html, real html will be in the body which will be copied over
		Document doc = new Document(dirty.baseUri());
		doc.appendElement("html").appendElement("body").appendChild(dirty);

		Cleaner cleaner = new Cleaner(WHITELIST);
		Document clean = cleaner.clean(doc);

		// unwrap again
		Element sanitized = clean.body().child(0);

		return sanitized.html();
	}

	/**
	 * Replace absolute url with an url that works against the original (proxy) host, using standard request headers
	 * for proxy information.
	 *
	 * @param absoluteUrl
	 * @param request
	 *
	 * @return modified absolute url
	 */
	public static String replaceForwardedHost(String absoluteUrl, HttpServletRequest request)
	{
		// headers X-Forwarded-XXX
		String forwardedHost = (String)request.getAttribute("X-Forwarded-Host");
		if (forwardedHost == null) forwardedHost = request.getHeader("X-Forwarded-Host");

		String forwardedScheme = (String)request.getAttribute("X-Forwarded-Proto");
		if (forwardedScheme == null) forwardedScheme = request.getHeader("X-Forwarded-Proto");
		if (forwardedScheme == null)
		{
			forwardedScheme = request.getHeader("X-Forwarded-Scheme");
		}

		// Header Forwarded (RFC 7239)
		String forwardedHeader = request.getHeader("Forwarded");
		if (forwardedHeader != null)
		{
			for (String s : forwardedHeader.split(";"))
			{
				if (s.startsWith("host="))
				{
					forwardedHost = s.substring(5);
				}
				else if (s.startsWith("proto="))
				{
					forwardedScheme = s.substring(6);
				}
			}
		}

		// Can be multiple values (separated by comma) in case of chained proxies, use first (original proxy)
		if (forwardedHost != null)
		{
			forwardedHost = forwardedHost.split(",")[0].trim();
		}
		if (forwardedScheme != null)
		{
			forwardedScheme = forwardedScheme.split(",")[0].trim();
		}

		String url = absoluteUrl;

		// replace scheme with forwarded
		String scheme = request.getScheme();
		if (scheme != null && forwardedScheme != null && url.startsWith(scheme))
		{
			url = forwardedScheme + url.substring(scheme.length());
		}

		// replace host (includes port) with forwarded
		String hostHeader = request.getHeader("Host");
		if (hostHeader != null && forwardedHost != null)
		{
			int index = url.indexOf(hostHeader);
			if (index >= 0)
			{
				url = url.substring(0, index) + forwardedHost + url.substring(index + hostHeader.length());
			}
		}

		return url;
	}

	public static String getOSName(String userAgent)
	{
		if (userAgent != null)
		{
			OS os = USER_AGENT_PARSER
				.parseOS(userAgent);
			String osName = os.family;
			if (os.major != null)
			{
				osName += " " + os.major;
				if (os.minor != null && !os.minor.equals("0"))
				{
					osName += "." + os.minor;
				}
			}
			return osName;
		}
		return null;
	}

	/** Does the CSP level 3, specifically strict-dynamic.
	 *
	 * @see <a href="https://caniuse.com/#feat=mdn-http_headers_csp_content-security-policy_strict-dynamic">caniuse.com</a>
	 */
	public static boolean supportsContentSecurityPolicyLevel3(String userAgentHeader)
	{
		if (userAgentHeader == null) return false;
		UserAgent userAgent = USER_AGENT_PARSER.parseUserAgent(userAgentHeader);
		if (("Chrome".equals(userAgent.family) || "Chromium".equals(userAgent.family)) && safeParseInt(userAgent.major) >= 52)
		{
			return true;
		}
		if ("Firefox".equals(userAgent.family) && safeParseInt(userAgent.major) >= 52)
		{
			return true;
		}
		if ("Edge".equals(userAgent.family) && safeParseInt(userAgent.major) >= 74) // Chromium-based
		{
			return true;
		}
		if ("Opera".equals(userAgent.family) && safeParseInt(userAgent.major) >= 39)
		{
			return true;
		}
		if ("Chrome Mobile WebView".equals(userAgent.family) && safeParseInt(userAgent.major) >= 76)
		{
			return true;
		}
		if ("Opera Mini".equals(userAgent.family) && safeParseInt(userAgent.major) >= 46)
		{
			return true;
		}
		if ("Samsung Internet".equals(userAgent.family) &&
			(safeParseInt(userAgent.major) > 6 || (safeParseInt(userAgent.major) == 6 && safeParseInt(userAgent.minor) >= 2)))
		{
			return true;
		}
		if (userAgent.family != null && userAgent.family.contains("Safari") &&
			(safeParseInt(userAgent.major) > 15 || (safeParseInt(userAgent.major) >= 15 && safeParseInt(userAgent.minor) >= 4)))
		{
			return true;
		}
		return false;
	}

	private static int safeParseInt(String s)
	{
		if (s != null)
		{
			try
			{
				return parseInt(s);
			}
			catch (NumberFormatException e)
			{
				// Hmm should not happen
			}
		}
		return 0;
	}
}
