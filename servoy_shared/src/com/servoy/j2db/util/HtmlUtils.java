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
package com.servoy.j2db.util;

import java.util.StringTokenizer;


public class HtmlUtils
{

	/** tags in HTML 4.0.1 (some of them are deprecated) */
	@SuppressWarnings("nls")
	public static final String[] tags = { "<!--", "<!DOCTYPE", "<a", "<abbr", "<acronym", "<address", "<applet", "<area", "<b", "<base", "<basefont", "<bdo", "<big", "<blockquote", "<body", "<br", "<button", "<caption", "<center", "<cite", "<code", "<col", "<colgroup", "<dd", "<del", "<dir", "<div", "<dfn", "<dl", "<dt", "<em", "<fieldset", "<font", "<form", "<frame", "<frameset", "<h1", "<h2", "<h3", "<h4", "<h5", "<h6", "<head", "<hr", "<html", "<i", "<iframe", "<img", "<input", "<ins", "<isindex", "<kbd", "<label", "<legend", "<li", "<link", "<map", "<menu", "<meta", "<noframes", "<noscript", "<object", "<ol", "<optgroup", "<option", "<p", "<param", "<pre", "<q", "<s", "<samp", "<script", "<select", "<small", "<span", "<strike", "<strong", "<style", "<sub", "<sup", "<table", "<tbody", "<td", "<textarea", "<tfoot", "<th", "<thead", "<title", "<tr", "<tt", "<u", "<ul", "<var", "<xmp" };

	/** tags that don't support inline style attributes (or tags that I want to align (and I must give the style in its parent)); */
	@SuppressWarnings("nls")
	public static final String[] alignNotSupportedTags = { "<b", "<u", "<i", "<br", "<hr", "<html", "<head", "<menu", "<sub", "<sup", "<style", "<button", "<a", "<center", "<blockquote", "<img", "<font", "<applet", "<bdo", "<big", "<button", "<abbr", "<area", "<!--", "<xmp", "<dir", "<script", "<meta" };
	@SuppressWarnings("nls")
	public static final String[] specialCaseTags = { "table", "tr", "td", "p", "div", "title", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li" };

	public static boolean startsWithHtml(Object object)
	{
		if (object instanceof CharSequence)
		{
			return startsWithHtml((CharSequence)object);
		}
		if (object != null)
		{
			return startsWithHtml(object.toString());
		}
		return false;
	}

	public static boolean startsWithHtml(CharSequence charsequence)
	{
		if (charsequence == null || charsequence.length() == 0) return false;

		int charsequenceLen = charsequence.length();

		int charIndex = 0;
		// first trim
		while (charIndex < charsequenceLen && Character.isWhitespace(charsequence.charAt(charIndex)))
		{
			charIndex++;
		}

		if (charIndex >= charsequenceLen || charsequence.charAt(charIndex) != '<') return false;
		if (charIndex >= charsequenceLen - 1 || Character.toLowerCase(charsequence.charAt(++charIndex)) != 'h') return false;
		if (charIndex >= charsequenceLen - 1 || Character.toLowerCase(charsequence.charAt(++charIndex)) != 't') return false;
		if (charIndex >= charsequenceLen - 1 || Character.toLowerCase(charsequence.charAt(++charIndex)) != 'm') return false;
		if (charIndex >= charsequenceLen - 1 || Character.toLowerCase(charsequence.charAt(++charIndex)) != 'l') return false;
		return true;
	}


	/**
	 * searches for content thats is to be shown in a web browser and returns true if that kind of content is found
	 *
	 * @param html the HTML content to be checked
	 * @return True if and only if the HTML content has some "display-able" content
	 */
	public static boolean hasUsefulHtmlContent(String html)
	{
		if (Utils.stringIsEmpty(html)) return false;
		String lowercaseText = html.toLowerCase();
		if (lowercaseText.indexOf("<html") == -1) //$NON-NLS-1$
		{
			return false;
		}

		int beginIndex = lowercaseText.indexOf("<body"); //$NON-NLS-1$
		String usefulContent = ""; //$NON-NLS-1$
		String enclosingTag = ""; //$NON-NLS-1$
		if (beginIndex == -1)
		{
			beginIndex = lowercaseText.indexOf("<html"); //$NON-NLS-1$
			beginIndex = lowercaseText.indexOf(">", beginIndex) + 1; //$NON-NLS-1$
			if (beginIndex == 0) return false;
			enclosingTag = "<html"; //$NON-NLS-1$
		}
		else
		{
			beginIndex = lowercaseText.indexOf(">", beginIndex) + 1; //$NON-NLS-1$
			if (beginIndex == 0) return false;
			enclosingTag = "<body"; //$NON-NLS-1$
		}
		try
		{
			usefulContent = findUsefulContent(enclosingTag, lowercaseText, beginIndex);
			return !Utils.stringIsEmpty(usefulContent);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public static boolean hasHtmlTag(String content)
	{
		if (content != null)
		{
			if (!content.contains("<")) return false; //$NON-NLS-1$
			for (String tag : tags)
			{
				if (content.contains(tag)) return true;
			}
		}
		return false;
	}

	/**
	 * returns the content wrapped by a given tag
	 *
	 * @param enclosingTag
	 * @param htmlContent
	 * @param beginIndex
	 * @return some "useful" content wrapped by the enclosing tag
	 */
	private static String findUsefulContent(String enclosingTag, String htmlContent, int beginIndex)
	{
		String wrapper = enclosingTag.replaceFirst("<", ""); //$NON-NLS-1$ //$NON-NLS-2$
		wrapper = wrapper.trim();


		int finalWrapperPosition = htmlContent.indexOf(wrapper, beginIndex);
		if (finalWrapperPosition == -1) return htmlContent.substring(beginIndex);

		boolean found = false;
		int position = finalWrapperPosition - 2;
		String contentWithWhiteSpaces = ""; //$NON-NLS-1$
		while (!found && position >= beginIndex)
		{
			contentWithWhiteSpaces = htmlContent.substring(position--, finalWrapperPosition);
			if (contentWithWhiteSpaces.trim().equals("</")) //$NON-NLS-1$
			{
				found = true;
				position++;
			}
		}
		if (found)
		{
			finalWrapperPosition = position;
		}

		String usefulContent = finalWrapperPosition >= beginIndex ? htmlContent.substring(beginIndex, finalWrapperPosition) : htmlContent.substring(beginIndex);
		if (usefulContent.trim().startsWith("</")) usefulContent = ""; //$NON-NLS-1$ //$NON-NLS-2$
		return usefulContent;
	}

	public static String stripHTML(String html)
	{
		String result = html.replace("<br />", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replace("<br/>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replace("<br>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("<[^>]*>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	/**
	 * Converts a Java String to an HTML markup string, but does not convert normal spaces to non-breaking space entities (&lt;nbsp&gt;).
	 *
	 * @param s The string to be escaped
	 * @see Utils#escapeMarkup(String, boolean)
	 * @return The escaped string
	 */
	public static CharSequence escapeMarkup(final String s)
	{
		return escapeMarkup(s, false);
	}

	/**
	 * Converts a Java String to an HTML markup String by replacing illegal characters with HTML entities where appropriate. Spaces are converted to
	 * non-breaking spaces (&lt;nbsp&gt;) if escapeSpaces is true, tabs are converted to four non-breaking spaces, less than signs are converted to &amp;lt;
	 * entities and greater than signs to &amp;gt; entities.
	 *
	 * @param s The string to escape
	 * @param escapeSpaces True to replace ' ' with nonbreaking space
	 * @return The escaped string
	 */
	public static CharSequence escapeMarkup(final String s, final boolean escapeSpaces)
	{
		return escapeMarkup(s, escapeSpaces, false);
	}

	/**
	 * Converts a Java String to an HTML markup String by replacing illegal characters with HTML entities where appropriate. Spaces are converted to
	 * non-breaking spaces (&lt;nbsp&gt;) if escapeSpaces is true, tabs are converted to four non-breaking spaces, less-than signs are converted to &amp;lt;
	 * entities and greater-than signs to &amp;gt; entities.
	 *
	 * @param s The string to escape
	 * @param escapeSpaces True to replace ' ' with nonbreaking space
	 * @param convertToHtmlUnicodeEscapes True to convert non-7 bit characters to unicode HTML (&#...)
	 * @return The escaped string
	 */
	public static CharSequence escapeMarkup(final String s, final boolean escapeSpaces, final boolean convertToHtmlUnicodeEscapes)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			int len = s.length();
			final StringBuffer buffer = new StringBuffer((int)(len * 1.1));

			for (int i = 0; i < len; i++)
			{
				final char c = s.charAt(i);

				switch (c)
				{
					case '\t' :
						if (escapeSpaces)
						{
							// Assumption is four space tabs (sorry, but that's
							// just how it is!)
							buffer.append("&nbsp;&nbsp;&nbsp;&nbsp;"); //$NON-NLS-1$
						}
						else
						{
							buffer.append(c);
						}
						break;

					case ' ' :
						if (escapeSpaces)
						{
							buffer.append("&nbsp;"); //$NON-NLS-1$
						}
						else
						{
							buffer.append(c);
						}
						break;

					case '<' :
						buffer.append("&lt;"); //$NON-NLS-1$
						break;

					case '>' :
						buffer.append("&gt;"); //$NON-NLS-1$
						break;

					case '&' :

						// if this is an entity (&#), then do not convert
						if ((i < len - 1) && (s.charAt(i + 1) == '#'))
						{
							buffer.append(c);

						}
						else
						{
							// it is not an entity, so convert it to &amp;
							buffer.append("&amp;"); //$NON-NLS-1$
						}
						break;

					case '"' :
						buffer.append("&quot;"); //$NON-NLS-1$
						break;

					case '\'' :
						buffer.append("&#039;"); //$NON-NLS-1$
						break;

					default :

						if (convertToHtmlUnicodeEscapes)
						{
							int ci = 0xffff & c;
							if (ci < 160)
							{
								// nothing special only 7 Bit
								buffer.append(c);
							}
							else
							{
								// Not 7 Bit use the unicode system
								buffer.append("&#"); //$NON-NLS-1$
								buffer.append(new Integer(ci).toString());
								buffer.append(';');
							}
						}
						else
						{
							buffer.append(c);
						}

						break;
				}
			}

			return buffer;
		}
	}

	/*
	 * raw unescape
	 */
	public static String unescape(String s)
	{
		String result = s;
		result = result.replaceAll("&lt;", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("&gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("&nbsp;", " "); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("&nbsp;&nbsp;&nbsp;&nbsp;", "\t"); //$NON-NLS-1$ //$NON-NLS-2$
		result = result.replaceAll("&quot;", "\""); //$NON-NLS-1$//$NON-NLS-2$
		result = result.replaceAll("&#039;", "\'"); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	public static String getValidFontFamilyValue(String cssValue)
	{
		StringBuffer sb = new StringBuffer();
		StringTokenizer tk = new StringTokenizer(cssValue, ","); //$NON-NLS-1$
		while (tk.hasMoreTokens())
		{
			String fontFamily = tk.nextToken();
			if (sb.toString().length() != 0)
			{
				sb.append(", "); //$NON-NLS-1$
			}
			fontFamily = fontFamily.trim();
			if (!fontFamily.startsWith("'") && !fontFamily.startsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
			{
				for (int i = 0; i < fontFamily.length(); i++)
				{
					boolean validCharacter = (fontFamily.charAt(i) >= 'a' && fontFamily.charAt(i) <= 'z') ||
						(fontFamily.charAt(i) >= 'A' && fontFamily.charAt(i) <= 'Z') || (fontFamily.charAt(i) >= '0' && fontFamily.charAt(i) <= '9') ||
						fontFamily.charAt(i) == '_' || fontFamily.charAt(i) == '-';
					if (!validCharacter)
					{
						fontFamily = "\"" + fontFamily + "\""; //$NON-NLS-1$//$NON-NLS-2$
						break;
					}
				}
			}
			sb.append(fontFamily);
		}
		return sb.toString();
	}

	/**
	 * Replaces all urls in a html document to make them absolute
	 * @param url the url of the document
	 * @param the html content of the document
	 * @return the document with absolute urls
	 */
	public static String htmlURLAbsEnhancer(String url, String htmldoc)
	{
		String currURL = url;
		int ind_currURL = url.lastIndexOf("/");
		if (ind_currURL != -1 && ind_currURL > 10)
		{
			currURL = url.substring(0, ind_currURL);
		}
		String baseURL = url;
		if (url.length() > 10)
		{
			int ind_baseURL = url.indexOf("/", 10);
			if (ind_baseURL != -1)
			{
				baseURL = url.substring(0, ind_baseURL);
			}
		}
		StringBuffer retval = new StringBuffer();
		String lowerCaseContent = htmldoc.toLowerCase();
		int index = 0;
		int old_index = 0;
		while (index != -1)
		{
			int formindex = lowerCaseContent.indexOf("<form", index);
			int aindex = lowerCaseContent.indexOf("<a", index);
			int imgindex = lowerCaseContent.indexOf("<img", index);
			int frameindex = lowerCaseContent.indexOf("<frame", index);
			int metaindex = lowerCaseContent.indexOf("<meta", index);
			int linkindex = lowerCaseContent.indexOf("<link", index);

			if (aindex != -1 && imgindex != -1)
			{
				int i = Math.min(aindex, imgindex);
				if (i != aindex)
				{
					aindex = -1;
				}
				else
				{
					imgindex = -1;
				}
			}

			if (frameindex != -1)
			{
				index = frameindex;
				int newindex = lowerCaseContent.indexOf("src", index);
				if (newindex == -1)
				{
					index++;
					continue;
				}

				index = newindex;
			}
			else if (metaindex != -1)
			{
				index = metaindex;
				int newindex = lowerCaseContent.indexOf(";url", index);
				if (newindex == -1)
				{
					index++;
					continue;
				}

				index = newindex;
			}
			else if (linkindex != -1)
			{
				index = linkindex;
				int newindex = lowerCaseContent.indexOf("href", index);
				if (newindex == -1)
				{
					index++;
					continue;
				}

				index = newindex;
			}
			else if (aindex != -1)
			{
				index = aindex;
				int newindex = lowerCaseContent.indexOf("href", index);
				if (newindex == -1)
				{
					index++;
					continue;
				}

				index = newindex;
			}
			else if (imgindex != -1)
			{
				index = imgindex;
				int newindex = lowerCaseContent.indexOf("src", index);
				if (newindex == -1)
				{
					index++;
					continue;
				}

				index = newindex;
			}
			else if (formindex != -1)
			{
				index = formindex;
				int newindex = lowerCaseContent.indexOf("action", index);
				if (newindex == -1)
				{
					index++;
					continue;
				}

				index = newindex;
			}
			else
			{
				break;
			}
			if ((index = lowerCaseContent.indexOf("=", index)) == -1) continue;

			index++; //skip '='

			String remaining = htmldoc.substring(index);

			StringTokenizer st = new StringTokenizer(remaining, "\t\n\r\"'>#");
			String strLink = st.nextToken();

			retval.append(htmldoc.substring(old_index, index));
			retval.append('"');

			if (strLink.startsWith("/"))
			{
				retval.append(baseURL);
			}
			else if (!strLink.startsWith(baseURL))
			{
				retval.append(currURL).append('/');
			}

			retval.append(strLink);

			retval.append('"');
			old_index = index + 1 + strLink.length() + 1;
		}
		retval.append(htmldoc.substring(old_index));
		return retval.toString();
	}

	public static boolean equalsIgnoreWhitespaceAndCase(CharSequence cs1, CharSequence cs2)
	{
		if (cs1 == null)
		{
			return cs2 == null;
		}
		if (cs2 == null)
		{
			return false;
		}

		NonWhitespaceCharacterSequenceIterator it1 = new NonWhitespaceCharacterSequenceIterator(cs1);
		NonWhitespaceCharacterSequenceIterator it2 = new NonWhitespaceCharacterSequenceIterator(cs2);

		while (true)
		{
			if (!it1.hasNext())
			{
				return !it2.hasNext();
			}
			if (!it2.hasNext())
			{
				return false;
			}

			char c1 = it1.next();
			char c2 = it2.next();
			if (c1 != c2 && Character.toLowerCase(c1) != Character.toLowerCase(c2))
			{
				return false;
			}
		}
	}

	private static class NonWhitespaceCharacterSequenceIterator
	{
		private final CharSequence cs;
		private int index = 0;

		NonWhitespaceCharacterSequenceIterator(CharSequence cs)
		{
			this.cs = cs;
		}

		boolean hasNext()
		{
			proceed();
			return index < cs.length();
		}

		char next()
		{
			return cs.charAt(index++);
		}

		private void proceed()
		{
			for (; index < cs.length(); index++)
			{
				char ch = cs.charAt(index);
				if (!Character.isWhitespace(ch))
				{
					return;
				}
			}
		}

	}

}
