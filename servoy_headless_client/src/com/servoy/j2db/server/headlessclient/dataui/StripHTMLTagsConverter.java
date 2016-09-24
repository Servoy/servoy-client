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
package com.servoy.j2db.server.headlessclient.dataui;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.parser.XmlPullParser;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Converts full html text, to html that can be inlined in the main html.
 * Has support for tranfering styles,javascripts and javascript urls to the head tag of the main html in the browser.
 *
 * @author jcompagner
 */
public class StripHTMLTagsConverter implements IConverter
{
	public static final String BLOB_LOADER_PARAM = "sb"; //$NON-NLS-1$

	/**
	 * @author jcompagner
	 *
	 */
	public static class StrippedText
	{
		private CharSequence bodyTxt;

		private final List<CharSequence> javascriptUrls;
		private final List<CharSequence> javascriptScripts;
		private final List<CharSequence> linkTags;

		private IValueMap bodyAttributes;

		private final List<CharSequence> styles;

		StrippedText()
		{
			javascriptUrls = new ArrayList<CharSequence>();
			javascriptScripts = new ArrayList<CharSequence>();
			styles = new ArrayList<CharSequence>();
			linkTags = new ArrayList<CharSequence>();
		}

		public final CharSequence getBodyTxt()
		{
			return bodyTxt;
		}

		public final void setBodyTxt(CharSequence bodyTxt)
		{
			this.bodyTxt = bodyTxt;
		}

		public final List<CharSequence> getJavascriptUrls()
		{
			return javascriptUrls;
		}

		public final List<CharSequence> getJavascriptScripts()
		{
			return javascriptScripts;
		}

		public final List<CharSequence> getLinkTags()
		{
			return linkTags;
		}

		/**
		 * @param attributes
		 */
		public void addBodyAttributes(IValueMap attributes)
		{
			this.bodyAttributes = attributes;
		}

		/**
		 * @return the bodyAttributes
		 */
		public IValueMap getBodyAttributes()
		{
			return bodyAttributes;
		}

		/**
		 * @return
		 */
		public List<CharSequence> getStyles()
		{
			return styles;
		}

	}

	private static final long serialVersionUID = 1L;

	public static final IConverter htmlStripper = new StripHTMLTagsConverter();

	public static final String[] scanTags = new String[] { "src", "href", "background", "onsubmit", "onreset", "onselect", "onclick", "ondblclick", "onfocus", "onblur", "onchange", "onkeydown", "onkeypress", "onkeyup", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$

	public static final Set<String> ignoreTags;

	static
	{
//		scanTags = new HashMap();
//		scanTags.put("img", new String[] { "src" });
//		scanTags.put("a", new String[] { "href", "onclick" });
//		scanTags.put("input", new String[] { "onclick" });

		ignoreTags = new HashSet<String>();
		ignoreTags.add("html"); //$NON-NLS-1$
		ignoreTags.add("body"); //$NON-NLS-1$
		ignoreTags.add("form"); //$NON-NLS-1$
		ignoreTags.add("head"); //$NON-NLS-1$
		ignoreTags.add("title"); //$NON-NLS-1$
	}

	/**
	 * @param bodyText
	 * @param solution
	 * @return
	 */
	@SuppressWarnings("nls")
	public static StrippedText convertBodyText(Component component, CharSequence unsanitizedbodyText, boolean trustDataAsHtml, FlattenedSolution solutionRoot)
	{
		CharSequence bodyText = WebBaseButton.sanitize(unsanitizedbodyText, trustDataAsHtml);

		StrippedText st = new StrippedText();
		if (RequestCycle.get() == null)
		{
			st.setBodyTxt(bodyText);
			return st;
		}

		ResourceReference rr = new ResourceReference("media"); //$NON-NLS-1$
		String solutionName = solutionRoot.getSolution().getName();


		StringBuilder bodyTxt = new StringBuilder(bodyText.length());
		XmlPullParser parser = new XmlPullParser();

		ICrypt urlCrypt = null;
		if (Application.exists()) urlCrypt = Application.get().getSecuritySettings().getCryptFactory().newCrypt();

		try
		{
			parser.parse(new ByteArrayInputStream(bodyText.toString().getBytes("UTF8")), "UTF8"); //$NON-NLS-1$ //$NON-NLS-2$
			XmlTag me = (XmlTag)parser.nextTag();

			while (me != null)
			{
				CharSequence tmp = parser.getInputFromPositionMarker(me.getPos());
				if (tmp.toString().trim().length() > 0) bodyTxt.append(tmp);
				parser.setPositionMarker();

				String currentTagName = me.getName().toLowerCase();

				if (currentTagName.equals("script")) //$NON-NLS-1$
				{
					if (!me.isClose())
					{
						String srcUrl = (String)me.getAttributes().get("src"); //$NON-NLS-1$
						if (srcUrl == null) srcUrl = (String)me.getAttributes().get("SRC"); //$NON-NLS-1$
						me = (XmlTag)parser.nextTag();
						if (srcUrl != null)
						{
							st.getJavascriptUrls().add(convertMediaReferences(srcUrl, solutionName, rr, "", true).toString());
						}
						else
						{
							if (me != null)
							{
								st.getJavascriptScripts().add(parser.getInputFromPositionMarker(me.getPos()));
								parser.setPositionMarker();
							}
						}
					}
					else
					{
						me = (XmlTag)parser.nextTag();
					}
					continue;
				}
				else if (currentTagName.equals("style"))
				{
					if (me.isOpen())
					{
						me = (XmlTag)parser.nextTag();
						List<CharSequence> styles = st.getStyles();
						String style = parser.getInputFromPositionMarker(me.getPos()).toString().trim();
						if (!"".equals(style) && !styles.contains(style))
						{
							styles.add(convertMediaReferences(style, solutionName, rr, "", false));
						}
						parser.setPositionMarker();
					}
					else
					{
						me = (XmlTag)parser.nextTag();
					}
					continue;
				}
				else if (currentTagName.equals("link"))
				{
					if (me.isOpen() || me.isOpenClose())
					{
						String end = "\n";
						if (me.isOpen()) end = "</link>\n";
						st.getLinkTags().add(convertMediaReferences(me.toXmlString(null) + end, solutionName, rr, "", false));
					}
					me = (XmlTag)parser.nextTag();
					continue;
				}
				if (ignoreTags.contains(currentTagName))
				{
					if (currentTagName.equals("body") && (me.isOpen() || me.isOpenClose()))
					{
						if (me.getAttributes().size() > 0)
						{
							st.addBodyAttributes(me.getAttributes());
						}
						me = (XmlTag)parser.nextTag();
					}
					else
					{
						me = (XmlTag)parser.nextTag();
					}
					continue;
				}

				if (currentTagName.equals("img") && component instanceof ILabel)
				{
					ILabel label = (ILabel)component;
					String onload = "Servoy.Utils.setLabelChildHeight('" + component.getMarkupId() + "', " + label.getVerticalAlignment() + ");";
					onload = me.getAttributes().containsKey("onload") ? me.getAttributes().getString("onload") + ";" + onload : onload;
					me.getAttributes().put("onload", onload);
				}

				boolean ignoreOnclick = false;
				IValueMap attributeMap = me.getAttributes();
				// first transfer over the tabindex to anchor tags
				if (currentTagName.equals("a"))
				{
					int tabIndex = TabIndexHelper.getTabIndex(component);
					if (tabIndex != -1) attributeMap.put("tabindex", Integer.valueOf(tabIndex));
				}
				// TODO attributes with casing?
				// now they have to be lowercase. (that is a xhtml requirement)
				for (String attribute : scanTags)
				{
					if (ignoreOnclick && attribute.equals("onclick")) continue; //$NON-NLS-1$
					String src = attributeMap.getString(attribute);
					if (src == null)
					{
						continue;
					}
					String lowercase = src.toLowerCase();
					if (lowercase.startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
					{
						String name = src.substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
						if (name.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
						{
							String url = generateBlobloaderUrl(component, urlCrypt, name);
							me.getAttributes().put(attribute, url);
						}
						else
						{
							String translatedUrl = MediaURLStreamHandler.getTranslatedMediaURL(solutionRoot, lowercase);
							if (translatedUrl != null)
							{
								me.getAttributes().put(attribute, translatedUrl);
							}
						}
					}
					else if (component instanceof ISupportScriptCallback && lowercase.startsWith("javascript:"))
					{
						String script = src;
						if (script.length() > 13)
						{
							String scriptName = script.substring(11);
							if ("href".equals(attribute))
							{
								if (attributeMap.containsKey("externalcall"))
								{
									attributeMap.remove("externalcall");
								}
								else
								{
									me.getAttributes().put("href", "#");
									me.getAttributes().put("onclick", ((ISupportScriptCallback)component).getCallBackUrl(scriptName, true));
									ignoreOnclick = true;
								}
							}
							else
							{
								me.getAttributes().put(attribute, ((ISupportScriptCallback)component).getCallBackUrl(scriptName, "onclick".equals(attribute)));
							}
						}
					}
					else if (component instanceof FormComponent< ? > && lowercase.startsWith("javascript:"))
					{
						String script = src;
						if (script.length() > 13)
						{
							String scriptName = script.substring(11);
							if ("href".equals(attribute))
							{
								me.getAttributes().put("href", "#");
								me.getAttributes().put("onclick", getTriggerJavaScript((FormComponent< ? >)component, scriptName));
								ignoreOnclick = true;
							}
							else
							{
								me.getAttributes().put(attribute, getTriggerJavaScript((FormComponent< ? >)component, scriptName));
							}
						}
					}
				}
				bodyTxt.append(me.toString());
				me = (XmlTag)parser.nextTag();
			}
			bodyTxt.append(parser.getInputFromPositionMarker(-1));

			st.setBodyTxt(convertMediaReferences(convertBlobLoaderReferences(bodyTxt, component), solutionName, rr, "", false)); //$NON-NLS-1$
		}
		catch (ParseException ex)
		{
			Debug.error(ex);
			bodyTxt.append("<span style=\"color : #ff0000;\">"); //$NON-NLS-1$
			bodyTxt.append(ex.getMessage());
			bodyTxt.append(bodyText.subSequence(ex.getErrorOffset(), Math.min(ex.getErrorOffset() + 100, bodyText.length())));
			bodyTxt.append("</span></body></html>"); //$NON-NLS-1$
			st.setBodyTxt(bodyTxt);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			bodyTxt.append("<span style=\"color : #ff0000;\">"); //$NON-NLS-1$
			bodyTxt.append(ex.getMessage());
			bodyTxt.append("</span></body></html>"); //$NON-NLS-1$
			st.setBodyTxt(bodyTxt);
		}
		return st;
	}

	/**
	 * @param component
	 * @param urlCrypt
	 * @param name
	 * @return
	 */
	public static String generateBlobloaderUrl(Component component, ICrypt urlCrypt, String name)
	{
		String mediaUrlPart = name.substring((MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + '?').length());
		if (urlCrypt != null)
		{
			mediaUrlPart = WicketURLEncoder.QUERY_INSTANCE.encode(urlCrypt.encryptUrlSafe(mediaUrlPart));
		}
		else
		{
			// if no url crypt then the old way
			mediaUrlPart = "true&" + mediaUrlPart; //$NON-NLS-1$
		}
		return RequestCycle.get().urlFor(component, IResourceListener.INTERFACE).toString() + '&' + BLOB_LOADER_PARAM + '=' + mediaUrlPart;
	}

	public static String getBlobLoaderUrlPart(Request request)
	{
		String url = request.getParameter(BLOB_LOADER_PARAM);
		if (url != null)
		{
			// old url
			if (url.equals("true")) //$NON-NLS-1$
			{
				url = request.getURL();
			}
			else
			{
				// encrypted
				if (Application.exists())
				{
					try
					{
						ICrypt urlCrypt = Application.get().getSecuritySettings().getCryptFactory().newCrypt();
						url = urlCrypt.decryptUrlSafe(url);
						url = url.replace("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (Exception e)
					{
						Debug.error("Error decrypting blobloader url: " + url, e);
					}
				}
			}
		}
		return url;
	}


	@SuppressWarnings("nls")
	public static CharSequence convertBlobLoaderReferences(CharSequence text, Component component)
	{
		if (text != null)
		{
			String txt = text.toString();
			int index = txt.indexOf("media:///servoy_blobloader?");
			if (index == -1) return txt;
			ICrypt urlCrypt = null;
			if (Application.exists()) urlCrypt = Application.get().getSecuritySettings().getCryptFactory().newCrypt();

			if (urlCrypt != null)
			{
				while (index != -1)
				{
					// just try to search for the ending quote
					int index2 = Utils.firstIndexOf(txt, new char[] { '\'', '"', ' ', '\t', ')' }, index);

					// if ending can't be resolved don't encrypt it.
					if (index2 == -1) return Strings.replaceAll(text, "media:///servoy_blobloader?",
						RequestCycle.get().urlFor(component, IResourceListener.INTERFACE) + "&" + BLOB_LOADER_PARAM + "=true&");

					String bloburl = generateBlobloaderUrl(component, urlCrypt, txt.substring(index + "media:///".length(), index2));
					txt = txt.substring(0, index) + bloburl + txt.substring(index2);

					index = txt.indexOf("media:///servoy_blobloader?", index + 1);
				}
				return txt;
			}
		}
		if (RequestCycle.get() != null) return Strings.replaceAll(text, "media:///servoy_blobloader?",
			RequestCycle.get().urlFor(component, IResourceListener.INTERFACE) + "&" + BLOB_LOADER_PARAM + "=true&");
		return text;
	}

	public static CharSequence convertMediaReferences(CharSequence text, String solutionName, ResourceReference media, String prefix,
		boolean quoteSpecialHTMLChars) // TODO quoteSpecialHTMLChars - shouldn't this always be true? (currently in most places it is false)
	{
		if (RequestCycle.get() != null) return Strings.replaceAll(text, "media:///", //$NON-NLS-1$
			prefix + RequestCycle.get().urlFor(media) + "?s=" + solutionName + (quoteSpecialHTMLChars ? "&amp;" : "&") + "id="); //$NON-NLS-1$//$NON-NLS-2$
		return text;
	}

	public static String getTriggerJavaScript(FormComponent< ? > component, String value)
	{
		ServoyForm form = (ServoyForm)component.getForm();
		StringBuilder sb = new StringBuilder(100);
		sb.append("javascript:document.getElementById('"); //$NON-NLS-1$
		sb.append(form.getHiddenField());
		sb.append("').name=\'"); //$NON-NLS-1$
		sb.append(component.getInputName());
		sb.append("';"); //$NON-NLS-1$
		sb.append("document.getElementById('"); //$NON-NLS-1$
		sb.append(form.getHiddenField());
		sb.append("').value=\'"); //$NON-NLS-1$
		sb.append(Utils.stringReplace(value, "\'", "\\\'")); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("';"); //$NON-NLS-1$

		sb.append("var f=document.getElementById('"); //$NON-NLS-1$
		sb.append(form.getJavascriptCssId());
		sb.append("');"); //$NON-NLS-1$

		sb.append("if (f.onsubmit != undefined) { if (f.onsubmit()==false) return false; }"); //$NON-NLS-1$

		sb.append("f.submit();return false;"); //$NON-NLS-1$
		return sb.toString();
	}

	public static String getTriggerJavaScript(IFormSubmittingComponent component, String value)
	{
		ServoyForm form = (ServoyForm)component.getForm();
		StringBuilder sb = new StringBuilder(100);
		sb.append("javascript:document.getElementById('"); //$NON-NLS-1$
		sb.append(form.getHiddenField());
		sb.append("').name=\'"); //$NON-NLS-1$
		sb.append(component.getInputName());
		sb.append("';"); //$NON-NLS-1$
		sb.append("document.getElementById('"); //$NON-NLS-1$
		sb.append(form.getHiddenField());
		sb.append("').value=\'"); //$NON-NLS-1$
		sb.append(Utils.stringReplace(value, "\'", "\\\'")); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("';"); //$NON-NLS-1$

		sb.append("var f=document.getElementById('"); //$NON-NLS-1$
		sb.append(form.getJavascriptCssId());
		sb.append("');"); //$NON-NLS-1$

		sb.append("if (f.onsubmit != undefined) { if (f.onsubmit()==false) return false; }"); //$NON-NLS-1$

		sb.append("f.submit();return false;"); //$NON-NLS-1$
		return sb.toString();
	}


	/**
	 * @see wicket.util.convert.IConverter#convertToObject(java.lang.String, java.util.Locale)
	 */
	public Object convertToObject(String value, Locale locale)
	{
		return value;
	}

	/**
	 * @see wicket.util.convert.IConverter#convertToString(java.lang.Object, java.util.Locale)
	 */
	public String convertToString(Object value, Locale locale)
	{
		if (value == null) return null;
		return TemplateGenerator.getSafeText(value.toString());
	}
}