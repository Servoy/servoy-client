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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.property.types.StyleClassPropertyType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IFormElementCache;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Generates HTML for a flow layout form
 * @author jblok, lvostinar
 */
@SuppressWarnings("nls")
public class FormLayoutStructureGenerator
{
	public static class DesignProperties
	{
		String mainContainerUUID;

		public DesignProperties(String mainContainerUUID)
		{
			this.mainContainerUUID = mainContainerUUID;
		}

		public DesignProperties()
		{
			this.mainContainerUUID = null;
		}
	}

	public static void generateLayout(Form form, String realFormName, FlattenedSolution fs, PrintWriter writer, DesignProperties design)
	{
		try
		{
			FormLayoutGenerator.generateFormStartTag(writer, form, realFormName, false, design != null);
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
			FormLayoutGenerator.generateFormEndTag(writer, design != null);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private static boolean isSecurityVisible(FlattenedSolution fs, IPersist persist, Form form)
	{
		return (fs.getSecurityAccess(persist.getUUID(),
			form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS) & IRepository.VIEWABLE) != 0;
	}

	public static void generateLayoutContainer(LayoutContainer container, Form form, FlattenedSolution fs, PrintWriter writer, DesignProperties design,
		IFormElementCache cache)
	{
		WebLayoutSpecification spec = null;
		if (container.getPackageName() != null)
		{
			PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				container.getPackageName());
			if (pkg != null)
			{
				spec = pkg.getSpecification(container.getSpecName());
			}
		}
		boolean isCSSPositionContainer = CSSPositionUtils.isCSSPositionContainer(spec);
		writer.print("<");
		writer.print(container.getTagType());
		Set<String> writtenAttributes = new HashSet<>();
		if (design != null)
		{
			writer.print(" svy-id='");
			writer.print(PersistIdentifier.fromSimpleUUID(container.getUUID()).toHTMLEscapedJSONString());
			writer.print("'");
			writer.print(" svy-location='");
			writer.print(container.getLocation().x);
			writer.print("'");
			boolean highSet = false;
			JSONObject ngClass = new JSONObject();
			String layoutStyleClasses = "";
			String solutionStyleClasses = "";
			if (spec != null)
			{
				writer.print(" svy-layoutname='");
				writer.print(spec.getPackageName() + "." + spec.getName());
				writer.print("'");

				ngClass.put("svy-layoutcontainer", true);
				if (!Utils.equalObjects(container.getAncestor(IRepository.FORMS).getUUID(), form.getUUID()))//is this inherited?
				{
					ngClass.put("inheritedElement", true);
				}
				String designClass = spec.getDesignStyleClass() != null && spec.getDesignStyleClass().length() > 0 ? spec.getDesignStyleClass()
					: "customDivDesign";
				if ("customDivDesign".equals(designClass) && hasSameDesignClassAsParent(container, spec))
				{
					designClass = isEvenLayoutContainer(container) ? "customDivDesignOdd" : "customDivDesignEven";
				}
				highSet = true;
				if (!container.getUUID().toString().equals(design.mainContainerUUID))
				{
					ngClass.put(designClass, "<showWireframe<");//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
				}
				else
				{
					writer.print(" data-maincontainer='true'");
					ngClass.put(designClass, "<false<");
				}
				ngClass.put("highlight_element", "<design_highlight=='highlight_element'<");//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime

				List<String> containerStyleClasses = getStyleClassValues(spec, container.getCssClasses());
				solutionStyleClasses = getSolutionSpecificClasses(spec, container);
				if (!containerStyleClasses.isEmpty())
				{
					layoutStyleClasses = containerStyleClasses.stream().collect(Collectors.joining(" "));
					writer.print(" class='" + layoutStyleClasses + "'");
				}
				if (container.getCssClasses() != null && container.getCssClasses().trim().length() > 0)
				{
					writtenAttributes.add("class");
				}
				if (spec.getAllowedChildren().size() > 0 || spec.getExcludedChildren() != null)
				{
					ngClass.put("drop_highlight", "<canContainDraggedElement('" + spec.getPackageName() + "." + spec.getName() + "')<");//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
				}
			}

			if (!highSet) ngClass.put("highlight_element", "<design_highlight=='highlight_element'<");
			if (ngClass.length() > 0)
			{
				writer.print(" ng-class='" + ngClass.toString().replaceAll("\"<", "").replaceAll("<\"", "").replaceAll("'", "\"") + "'");

			}
			if (writtenAttributes.contains("class"))
			{
				writer.print(" svy-layout-class='" + layoutStyleClasses + "'");
				writer.print(" svy-solution-layout-class='" + solutionStyleClasses + "'");
			}
			writer.print(" svy-title='" + getLayouContainerTitle(container) + "'");
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
		if (isCSSPositionContainer)
		{
			// we need to specify the height
			writer.print(" style='height:");
			writer.print(container.getSize().height);
			writer.print("px;position: relative;' ");
		}
		Map<String, String> attributes = new HashMap<String, String>(container.getMergedAttributes());
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
		String classes = attributes.get("class");
		if (classes == null) classes = "svy-layoutcontainer";
		else classes += " svy-layoutcontainer";
		attributes.put("class", classes);
		for (Entry<String, String> entry : attributes.entrySet())
		{
			if (design != null && writtenAttributes.contains(entry.getKey())) continue;
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
				FormElement fe = cache.getFormElement((IFormElement)component, fs, null, design != null);
				if (!isSecurityVisible(fs, fe.getPersistIfAvailable(), form))
				{
					continue;
				}
				if (isCSSPositionContainer)
				{
					FormLayoutGenerator.generateFormElementWrapper(writer, fe, form, false);
				}
				FormLayoutGenerator.generateFormElement(writer, fe, form);
				if (isCSSPositionContainer)
				{
					FormLayoutGenerator.generateEndDiv(writer);
				}
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

	public static boolean hasSameDesignClassAsParent(LayoutContainer container, WebLayoutSpecification spec)
	{
		ISupportChilds realParent = container.getExtendsID() != null ? PersistHelper.getRealParent(container) : container.getParent();
		if (realParent instanceof LayoutContainer)
		{
			LayoutContainer parent = (LayoutContainer)realParent;
			if (parent.getPackageName() != null)
			{
				PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
					parent.getPackageName());
				if (pkg != null)
				{
					WebLayoutSpecification parentSpec = pkg.getSpecification(parent.getSpecName());
					return spec.getDesignStyleClass().equals(parentSpec.getDesignStyleClass());
				}
			}
		}
		return false;
	}

	public static boolean isEvenLayoutContainer(LayoutContainer c)
	{
		int i = 0;
		LayoutContainer container = c;
		while (container.getParent() instanceof LayoutContainer)
		{
			container = (LayoutContainer)container.getParent();
			i++;
		}
		return i % 2 == 0;
	}

	public static String getSolutionSpecificClasses(WebLayoutSpecification spec, LayoutContainer container)
	{
		List<String> containerStyleClasses = getStyleClassValues(spec, container.getCssClasses());
		return container.getCssClasses() != null
			? Arrays.stream(container.getCssClasses().split(" ")).filter(cls -> !containerStyleClasses.contains(cls)).collect(Collectors.joining(" "))
			: "";
	}

	/**
	 * Filters out the solution css classes (the ones which are not in the spec file of the layout container).
	 * @param spec the spec of the current container
	 * @param cssClasses that are set on the current container
	 * @return the css classes which are also in the spec
	 */
	public static List<String> getStyleClassValues(WebLayoutSpecification spec, String cssClasses)
	{
		List<String> result = new ArrayList<String>();
		if (cssClasses == null) return result;
		String[] classes = cssClasses.split(" ");
		JSONObject config = spec.getConfig() instanceof String ? new JSONObject((String)spec.getConfig()) : null;
		String defaultClass = config != null ? config.optString("class", "") : "";

		Collection<PropertyDescription> properties = spec.getProperties(StyleClassPropertyType.INSTANCE);
		for (PropertyDescription pd : properties)
		{
			for (String cls : classes)
			{
				if (defaultClass.equals(cls) || pd.hasDefault() && cls.equals(pd.getDefaultValue()))
				{
					result.add(cls);
				}
				else if (pd.getValues() != null)
				{
					if (pd.getValues().contains(cls))
					{
						result.add(cls);
					}
					else
					{
						for (Object value : pd.getValues())
						{
							String val = value.toString();
							if (val.endsWith("-") && cls.startsWith(val) && Utils.getAsInteger(cls.replaceFirst(val, "")) != -1)
							{
								result.add(cls);
								break;
							}
						}
					}
				}
			}
		}

		return result;
	}

	public static String getLayouContainerTitle(LayoutContainer container)
	{
		if (container.getCssClasses() == null) return container.getTagType();
		String title = container.getCssClasses().replaceFirst("col-", "");
		//we should make sure the container title in the wireframe is not too long
		if (title.length() > 20)
		{
			String[] parts = title.split(" ");
			title = parts[0];
			if (parts.length > 1)
			{
				int i = 1;
				do
				{
					title += " " + parts[i++];
				}
				while (i < parts.length && title.length() < 20);
			}
		}
		return title;
	}
}
