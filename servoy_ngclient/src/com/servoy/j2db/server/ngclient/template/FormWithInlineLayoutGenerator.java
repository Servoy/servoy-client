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

package com.servoy.j2db.server.ngclient.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.parser.IXmlPullParser;
import org.apache.wicket.markup.parser.XmlPullParser;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.util.collections.ArrayListStack;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.value.IValueMap;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.util.Debug;

/**
 * @author jblok
 */
@SuppressWarnings("nls")
public class FormWithInlineLayoutGenerator
{
	public static void generateLayoutContainers(Form form, ServoyDataConverterContext context, PrintWriter writer)
	{
		try
		{
			writer.println(String.format(
				"<div ng-controller=\"%1$s\" svy-formstyle=\"formStyle\" svyScrollbars='formProperties.scrollbars' svy-layout-update svy-formload>",
				form.getName()));
			Iterator<LayoutContainer> it = form.getLayoutContainers();
			while (it.hasNext())
			{
				generateLayoutContainer(it.next(), context, writer);
			}
			writer.println("</div>");
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private static void generateLayoutContainer(LayoutContainer container, ServoyDataConverterContext context, PrintWriter writer)
	{
		writer.print("<");
		writer.print(container.getTagType());
		writer.print(" ");
		if (container.getElementId() != null)
		{
			writer.print("id='");
			writer.print(container.getElementId());
			writer.print("' ");
		}
		if (container.getStyle() != null)
		{
			writer.print("style='");
			writer.print(container.getStyle());
			writer.print("' ");
		}
		if (container.getCssClasses() != null)
		{
			writer.print("class='");
			writer.print(container.getCssClasses());
			writer.print("' ");
		}
		writer.println(">");

		Iterator<IPersist> components = container.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (components.hasNext())
		{
			IPersist component = components.next();
			if (component instanceof LayoutContainer)
			{
				generateLayoutContainer((LayoutContainer)component, context, writer);
			}
			else if (component instanceof IFormElement)
			{
				generateFormElement((IFormElement)component, context, writer);
			}
		}
		writer.print("</");
		writer.print(container.getTagType());
		writer.print(">");
	}

	private static void generateFormElement(IFormElement formElement, ServoyDataConverterContext context, PrintWriter writer)
	{
		FormElement fe = ComponentFactory.getFormElement(formElement, context, null);
		writer.print("<");
		writer.print(fe.getTagname());
		writer.print(" name='");
		writer.print(fe.getName());
		writer.print("'");
		writer.print(" svy-model='model.");
		writer.print(fe.getName());
		writer.print("'");
		writer.print(" svy-api='api.");
		writer.print(fe.getName());
		writer.print("'");
		writer.print(" svy-handlers='handlers.");
		writer.print(fe.getName());
		writer.print("'");
		writer.print(" svy-apply='handlers.");
		writer.print(fe.getName());
		writer.print(".svy_apply'");
		writer.print(" svy-servoyApi='handlers.");
		writer.print(fe.getName());
		writer.print(".svy_servoyApi'");
		writer.println(">");
		writer.print("</");
		writer.print(fe.getTagname());
		writer.println(">");
	}

	/**
	 * @param form
	 * @param fs
	 * @param writer
	 */
	public static void generate(Form form, ServoyDataConverterContext context, PrintWriter writer)
	{
		try
		{
			Map<String, FormElement> allFormElements = new HashMap<String, FormElement>();

			Iterator<IFormElement> it = form.getFormElementsSortedByFormIndex();
			while (it.hasNext())
			{
				IFormElement element = it.next();
				FormElement fe = ComponentFactory.getFormElement(element, context, null);
				allFormElements.put(element.getUUID().toString(), fe);

				//make life easy if a real name is used
				if (element.getName() != null) allFormElements.put(element.getName(), fe);
			}

			HTMLParser parser = new HTMLParser(form.getLayoutGrid());
			List<MarkupElement> elems = parser.parse();

			writer.println(String.format("<div ng-controller=\"%1$s\" ng-style=\"formStyle\" svy-layout-update>", form.getName()));

			XmlTag skipUntilClosed = null;
			Iterator<MarkupElement> elem_it = elems.iterator();
			while (elem_it.hasNext())
			{
				XmlTag tag = (XmlTag)elem_it.next();
				if (skipUntilClosed != null)
				{
					if (!tag.closes(skipUntilClosed)) continue;
					skipUntilClosed = null;
					continue;
				}

				IValueMap attributes = tag.getAttributes();
				String id = attributes.getString("id");
				FormElement fe = allFormElements.get(id);
				if (fe != null)
				{
//					if (fe.getTagname().equals(tag.getName())) does not need to be same, if id matches we replace
					{
						writer.print(fe.toString());
						if (!tag.isOpenClose()) skipUntilClosed = tag;
					}
				}
				else
				{
					writer.print(tag.toCharSequence());
				}
			}
			writer.println("</div>");
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * Merge of wicket MarkupParser,HTMLHandler,Markup (since not usable without running inside wicket)
	 * @author Jan Blok
	 */
	public static class HTMLParser
	{
		/** Map of simple tags. */
		private static final Map<String, Boolean> doesNotRequireCloseTag = new HashMap<String, Boolean>();

		static
		{
			// Tags which are allowed not be closed in HTML
			doesNotRequireCloseTag.put("p", Boolean.TRUE);
			doesNotRequireCloseTag.put("br", Boolean.TRUE);
			doesNotRequireCloseTag.put("img", Boolean.TRUE);
			doesNotRequireCloseTag.put("input", Boolean.TRUE);
			doesNotRequireCloseTag.put("hr", Boolean.TRUE);
			doesNotRequireCloseTag.put("link", Boolean.TRUE);
			doesNotRequireCloseTag.put("meta", Boolean.TRUE);
		}

		private final IXmlPullParser xmlParser;
		private final List<MarkupElement> markupElements;

		public HTMLParser(final String markup) throws IOException, ResourceStreamNotFoundException
		{
			xmlParser = new XmlPullParser();
			xmlParser.parse(markup);

			markupElements = new ArrayList<MarkupElement>();
		}

		public List<MarkupElement> parse() throws ParseException
		{
			// Loop through parser tags
			MarkupElement me = null;
			while ((me = xmlParser.nextTag()) != null)
			{
				markupElements.add(me);
			}

			// Loop through tags
			ArrayListStack<XmlTag> stack = new ArrayListStack<XmlTag>();
			Iterator<MarkupElement> it = markupElements.iterator();
			while (it.hasNext())
			{
				XmlTag tag = (XmlTag)it.next();

				// Check tag type
				if (tag.isOpen())
				{
					// Push onto stack
					stack.push(tag);
				}
				else if (tag.isClose())
				{
					// Check that there is something on the stack
					if (stack.size() > 0)
					{
						// Pop the top tag off the stack
						XmlTag top = stack.pop();

						// If the name of the current close tag does not match the
						// tag on the stack then we may have a mismatched close tag
						boolean mismatch = !top.hasEqualTagName(tag);

						if (mismatch)
						{
							// Pop any simple tags off the top of the stack
							while (mismatch && !requiresCloseTag(top.getName()))
							{
								// mark them as open/close
								top.setType(XmlTag.OPEN_CLOSE);

								// Pop simple tag
								if (stack.isEmpty())
								{
									break;
								}
								top = stack.pop();

								// Does new top of stack mismatch too?
								mismatch = !top.hasEqualTagName(tag);
							}

							// If adjusting for simple tags did not fix the problem,
							// it must be a real mismatch.
							if (mismatch)
							{
								throw new ParseException("Tag " + top.toUserDebugString() + " has a mismatched close tag at " + tag.toUserDebugString(),
									top.getPos());
							}
						}

						// Tag matches, so add pointer to matching tag
						tag.setOpenTag(top);
					}
					else
					{
						throw new ParseException("Tag " + tag.toUserDebugString() + " does not have a matching open tag", tag.getPos());
					}
				}
				else if (tag.isOpenClose())
				{
					// Tag closes itself
					tag.setOpenTag(tag);
				}
			}

			return markupElements;
		}

		/**
		 * Gets whether this tag does not require a closing tag.
		 *
		 * @param name
		 *            The tag's name, e.g. a, br, div, etc.
		 * @return True if this tag does not require a closing tag
		 */
		public static boolean requiresCloseTag(final String name)
		{
			return doesNotRequireCloseTag.get(name.toLowerCase()) == null;
		}
	}
}
