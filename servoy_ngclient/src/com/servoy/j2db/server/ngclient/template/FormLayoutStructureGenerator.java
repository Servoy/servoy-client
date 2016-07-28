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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IFormElementCache;
import com.servoy.j2db.util.Debug;

/**
 * Generates HTML for a flow layout form
 * @author jblok, lvostinar
 */
@SuppressWarnings("nls")
public class FormLayoutStructureGenerator
{
	public static void generateLayout(Form form, String realFormName, FlattenedSolution fs, PrintWriter writer, boolean design)
	{
		try
		{
			FormLayoutGenerator.generateFormStartTag(writer, form, realFormName, false, design);
			Iterator<IPersist> components = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			while (components.hasNext())
			{
				IPersist component = components.next();
				if (component instanceof LayoutContainer)
				{
					generateLayoutContainer((LayoutContainer)component, form, fs, writer, design, FormElementHelper.INSTANCE);
				}
				else if (component instanceof IFormElement)
				{
					FormLayoutGenerator.generateFormElement(writer, FormElementHelper.INSTANCE.getFormElement((IFormElement)component, fs, null, false), form);
				}
			}
			FormLayoutGenerator.generateFormEndTag(writer, design);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public static void generateLayoutContainer(LayoutContainer container, Form form, FlattenedSolution fs, PrintWriter writer, boolean design,
		IFormElementCache cache)
	{
		WebLayoutSpecification spec = null;
		if (container.getPackageName() != null)
		{
			PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getInstance().getLayoutSpecifications().get(container.getPackageName());
			if (pkg != null)
			{
				spec = pkg.getSpecification(container.getSpecName());
			}
		}
		writer.print("<");
		writer.print(container.getTagType());
		if (design)
		{
			writer.print(" svy-id='");
			writer.print(container.getUUID().toString());
			writer.print("'");
			boolean highSet = false;
			JSONObject ngClass = new JSONObject();
			if (spec != null)
			{
				writer.print(" svy-layoutname='");
				writer.print(spec.getPackageName() + "." + spec.getName());
				writer.print("'");

				ngClass.put("svy-layoutcontainer", true);
				if (!(container.getAncestor(IRepository.FORMS).getID() == form.getID()))//is this inherited?
				{
					ngClass.put("inheritedElement", true);
				}
				if (spec.getDesignStyleClass() != null && spec.getDesignStyleClass().length() > 0)
				{
					highSet = true;
					ngClass.put(spec.getDesignStyleClass(), "<showWireframe<");//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
					ngClass.put("highlight_element", "<design_highlight=='highlight_element'<");//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
				}
				if (spec.getAllowedChildren().size() > 0 || spec.getExcludedChildren().size() > 0)
				{
					ngClass.put("drop_highlight", "<canContainDraggedElement('" + spec.getPackageName() + "." + spec.getName() + "')<");//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
				}

			}

			if (!highSet) ngClass.put("highlight_element", "<design_highlight=='highlight_element'<");
			if (ngClass.length() > 0) writer.print(" ng-class='" + ngClass.toString().replaceAll("\"<", "").replaceAll("<\"", "").replaceAll("'", "\"") + "'");
		}
		else
		{
			writer.print(" ng-class=\"'svy-layoutcontainer'\" ");
		}
		if (container.getName() != null)
		{
			writer.print(" svy-name='");
			writer.print(container.getName());
			writer.print("' ");
		}
		if (container.getElementId() != null)
		{
			writer.print(" id='");
			writer.print(container.getElementId());
			writer.print("' ");
		}
		writer.print(" svy-autosave ");
		Map<String, String> attributes = new HashMap<String, String>(container.getAttributes());
		if (spec != null)
		{
			for (String propertyName : spec.getAllPropertiesNames())
			{
				PropertyDescription pd = spec.getProperty(propertyName);
				if (pd.getDefaultValue() != null && !attributes.containsKey(propertyName))
				{
					attributes.put(propertyName, pd.getDefaultValue().toString());
				}
			}
		}
		for (Entry<String, String> entry : attributes.entrySet())
		{
			writer.print(" ");
			try
			{
				StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(entry.getKey(), writer);
				if (entry.getValue() != null && entry.getValue().length() > 0)
				{
					writer.print("=\"");
					StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(entry.getValue(), writer);
					writer.print("\"");
				}
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
		writer.print(">");

		Iterator<IPersist> components = container.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (components.hasNext())
		{
			IPersist component = components.next();
			if (component instanceof LayoutContainer)
			{
				generateLayoutContainer((LayoutContainer)component, form, fs, writer, design, cache);
			}
			else if (component instanceof IFormElement)
			{
				FormElement fe = cache.getFormElement((IFormElement)component, fs, null, design);
				FormLayoutGenerator.generateFormElement(writer, fe, form);
			}
		}
		writer.print("</");
		writer.print(container.getTagType());
		writer.print(">");
	}
//	/**
//	 * @param form
//	 * @param fs
//	 * @param writer
//	 */
//	public static void generate(Form form, ServoyDataConverterContext context, PrintWriter writer)
//	{
//		try
//		{
//			Map<String, FormElement> allFormElements = new HashMap<String, FormElement>();
//
//			Iterator<IFormElement> it = form.getFormElementsSortedByFormIndex();
//			while (it.hasNext())
//			{
//				IFormElement element = it.next();
//				FormElement fe = ComponentFactory.getFormElement(element, context, null);
//				allFormElements.put(element.getUUID().toString(), fe);
//
//				//make life easy if a real name is used
//				if (element.getName() != null) allFormElements.put(element.getName(), fe);
//			}
//
//			HTMLParser parser = new HTMLParser(form.getLayoutGrid());
//			List<MarkupElement> elems = parser.parse();
//
//			writer.println(String.format("<div ng-controller=\"%1$s\" ng-style=\"formStyle\" svy-layout-update>", form.getName()));
//
//			XmlTag skipUntilClosed = null;
//			Iterator<MarkupElement> elem_it = elems.iterator();
//			while (elem_it.hasNext())
//			{
//				XmlTag tag = (XmlTag)elem_it.next();
//				if (skipUntilClosed != null)
//				{
//					if (!tag.closes(skipUntilClosed)) continue;
//					skipUntilClosed = null;
//					continue;
//				}
//
//				IValueMap attributes = tag.getAttributes();
//				String id = attributes.getString("id");
//				FormElement fe = allFormElements.get(id);
//				if (fe != null)
//				{
////					if (fe.getTagname().equals(tag.getName())) does not need to be same, if id matches we replace
//					{
//						writer.print(fe.toString());
//						if (!tag.isOpenClose()) skipUntilClosed = tag;
//					}
//				}
//				else
//				{
//					writer.print(tag.toCharSequence());
//				}
//			}
//			writer.println("</div>");
//		}
//		catch (Exception e)
//		{
//			Debug.error(e);
//		}
//	}

//	/**
//	 * Merge of wicket MarkupParser,HTMLHandler,Markup (since not usable without running inside wicket)
//	 * @author Jan Blok
//	 */
//	public static class HTMLParser
//	{
//		/** Map of simple tags. */
//		private static final Map<String, Boolean> doesNotRequireCloseTag = new HashMap<String, Boolean>();
//
//		static
//		{
//			// Tags which are allowed not be closed in HTML
//			doesNotRequireCloseTag.put("p", Boolean.TRUE);
//			doesNotRequireCloseTag.put("br", Boolean.TRUE);
//			doesNotRequireCloseTag.put("img", Boolean.TRUE);
//			doesNotRequireCloseTag.put("input", Boolean.TRUE);
//			doesNotRequireCloseTag.put("hr", Boolean.TRUE);
//			doesNotRequireCloseTag.put("link", Boolean.TRUE);
//			doesNotRequireCloseTag.put("meta", Boolean.TRUE);
//		}
//
//		private final IXmlPullParser xmlParser;
//		private final List<MarkupElement> markupElements;
//
//		public HTMLParser(final String markup) throws IOException, ResourceStreamNotFoundException
//		{
//			xmlParser = new XmlPullParser();
//			xmlParser.parse(markup);
//
//			markupElements = new ArrayList<MarkupElement>();
//		}
//
//		public List<MarkupElement> parse() throws ParseException
//		{
//			// Loop through parser tags
//			MarkupElement me = null;
//			while ((me = xmlParser.nextTag()) != null)
//			{
//				markupElements.add(me);
//			}
//
//			// Loop through tags
//			ArrayListStack<XmlTag> stack = new ArrayListStack<XmlTag>();
//			Iterator<MarkupElement> it = markupElements.iterator();
//			while (it.hasNext())
//			{
//				XmlTag tag = (XmlTag)it.next();
//
//				// Check tag type
//				if (tag.isOpen())
//				{
//					// Push onto stack
//					stack.push(tag);
//				}
//				else if (tag.isClose())
//				{
//					// Check that there is something on the stack
//					if (stack.size() > 0)
//					{
//						// Pop the top tag off the stack
//						XmlTag top = stack.pop();
//
//						// If the name of the current close tag does not match the
//						// tag on the stack then we may have a mismatched close tag
//						boolean mismatch = !top.hasEqualTagName(tag);
//
//						if (mismatch)
//						{
//							// Pop any simple tags off the top of the stack
//							while (mismatch && !requiresCloseTag(top.getName()))
//							{
//								// mark them as open/close
//								top.setType(XmlTag.OPEN_CLOSE);
//
//								// Pop simple tag
//								if (stack.isEmpty())
//								{
//									break;
//								}
//								top = stack.pop();
//
//								// Does new top of stack mismatch too?
//								mismatch = !top.hasEqualTagName(tag);
//							}
//
//							// If adjusting for simple tags did not fix the problem,
//							// it must be a real mismatch.
//							if (mismatch)
//							{
//								throw new ParseException("Tag " + top.toUserDebugString() + " has a mismatched close tag at " + tag.toUserDebugString(),
//									top.getPos());
//							}
//						}
//
//						// Tag matches, so add pointer to matching tag
//						tag.setOpenTag(top);
//					}
//					else
//					{
//						throw new ParseException("Tag " + tag.toUserDebugString() + " does not have a matching open tag", tag.getPos());
//					}
//				}
//				else if (tag.isOpenClose())
//				{
//					// Tag closes itself
//					tag.setOpenTag(tag);
//				}
//			}
//
//			return markupElements;
//		}
//
//		/**
//		 * Gets whether this tag does not require a closing tag.
//		 *
//		 * @param name
//		 *            The tag's name, e.g. a, br, div, etc.
//		 * @return True if this tag does not require a closing tag
//		 */
//		public static boolean requiresCloseTag(final String name)
//		{
//			return doesNotRequireCloseTag.get(name.toLowerCase()) == null;
//		}
//	}
}
