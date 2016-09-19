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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.helper.StringUtil;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.BasicFormManager;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IFormElementCache;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Generates HTML for a absolute layout form
 * @author lvostinar
 */
@SuppressWarnings("nls")
public class FormLayoutGenerator
{

	public static String generateFormComponent(Form form, FlattenedSolution fs, IFormElementCache cache)
	{
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		Iterator<IPersist> components = fs.getFlattenedForm(form).getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (components.hasNext())
		{
			IPersist component = components.next();
			if (component instanceof LayoutContainer)
			{
				FormLayoutStructureGenerator.generateLayoutContainer((LayoutContainer)component, form, fs, writer, false, cache);
			}
			else if (component instanceof IFormElement)
			{
				FormElement fe = cache.getFormElement((IFormElement)component, fs, null, false);
				if (form != null && !form.isResponsiveLayout())
				{
					FormLayoutGenerator.generateFormElementWrapper(writer, fe, form, form.isResponsiveLayout());
				}
				FormLayoutGenerator.generateFormElement(writer, fe, form);
				if (form != null && !form.isResponsiveLayout())
				{
					FormLayoutGenerator.generateEndDiv(writer);
				}
			}
		}
		return out.getBuffer().toString();
	}

	public static void generateRecordViewForm(PrintWriter writer, Form form, String realFormName, IServoyDataConverterContext context, boolean design)
	{
		generateFormStartTag(writer, form, realFormName, false, design);
		Iterator<Part> it = form.getParts();

		if (design)
		{
			while (it.hasNext())
			{
				Part part = it.next();
				if (!Part.rendersOnlyInPrint(part.getPartType()))
				{
					writer.print("<div ng-style=\"");
					writer.print(PartWrapper.getName(part));
					writer.print("Style\"");
					String partClass = "svy-" + PartWrapper.getName(part);
					if (part.getStyleClass() != null)
					{
						partClass += " " + part.getStyleClass();
					}
					writer.print(" class=\"");
					writer.print(partClass);
					writer.print("\">");
					generateEndDiv(writer);
				}
			}
		}

		Map<IPersist, FormElement> cachedElementsMap = new HashMap<IPersist, FormElement>();
		if (context != null && context.getApplication() != null)
		{
			IFormController controller = ((BasicFormManager)context.getApplication().getFormManager()).getCachedFormController(realFormName);
			if (controller != null && controller.getFormUI() instanceof WebFormUI)
			{
				List<FormElement> cachedFormElements = ((WebFormUI)controller.getFormUI()).getFormElements();
				for (FormElement fe : cachedFormElements)
				{
					if (fe.getPersistIfAvailable() != null)
					{
						cachedElementsMap.put(fe.getPersistIfAvailable(), fe);
					}
				}
			}
		}
		it = form.getParts();
		while (it.hasNext())
		{
			Part part = it.next();
			if (!Part.rendersOnlyInPrint(part.getPartType()))
			{
				if (!design)
				{
					writer.print("<div ng-style=\"");
					writer.print(PartWrapper.getName(part));
					writer.print("Style\"");
					String partClass = "svy-" + PartWrapper.getName(part);
					if (part.getStyleClass() != null)
					{
						partClass += " " + part.getStyleClass();
					}
					writer.print(" class=\"");
					writer.print(partClass);
					writer.print("\">");
				}

				for (IFormElement bc : PartWrapper.getBaseComponents(part, form, context, design, false))
				{
					FormElement fe = null;
					if (cachedElementsMap.containsKey(bc))
					{
						fe = cachedElementsMap.get(bc);
					}
					if (fe == null)
					{
						fe = FormElementHelper.INSTANCE.getFormElement(bc, context.getSolution(), null, design);
					}

					generateFormElementWrapper(writer, fe, form, form.isResponsiveLayout());
					generateFormElement(writer, fe, form);
					generateEndDiv(writer);
				}

				if (!design) generateEndDiv(writer);
			}
		}

		generateFormEndTag(writer, design);
	}

	public static void generateFormStartTag(PrintWriter writer, Form form, String realFormName, boolean responsiveMode, boolean design)
	{
		if (design)
		{
			writer.print("<div ng-controller='DesignFormController' id='svyDesignForm' ");

			if (form.isResponsiveLayout())
			{
				List<String> allowedChildren = new ArrayList<String>();
				Collection<PackageSpecification<WebLayoutSpecification>> values = WebComponentSpecProvider.getInstance().getLayoutSpecifications().values();
				for (PackageSpecification<WebLayoutSpecification> specifications : values)
				{
					for (WebLayoutSpecification specification : specifications.getSpecifications().values())
					{
						if (specification.isTopContainer())
						{
							allowedChildren.add(specification.getPackageName() + "." + specification.getName());
						}
					}
				}
				if (allowedChildren.size() > 0)
				{
					String allowedChildrenJoin = StringUtil.join(allowedChildren, ",");
					writer.print(" svy-allowed-children=\"");
					writer.print(allowedChildrenJoin);
					writer.print("\"");

					JSONObject ngClass = new JSONObject();
					String dropHighlightCondition = "<canContainDraggedElement(";
					if (allowedChildren.size() > 1)
					{
						for (int i = 0; i < allowedChildren.size() - 1; i++)
						{
							dropHighlightCondition += "'" + allowedChildren.get(i) + "', ";
						}
					}
					dropHighlightCondition += "'" + allowedChildren.get(allowedChildren.size() - 1) + "'";
					dropHighlightCondition += ")<";
					ngClass.put("drop_highlight", dropHighlightCondition);//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
					writer.print(" ng-class='" + ngClass.toString().replaceAll("\"<", "").replaceAll("<\"", "").replaceAll("'", "\"") + "'");
				}
			}

		}
		else writer.print(String.format("<svy-formload formname=\"%1$s\"><div ng-controller=\"%1$s\" ", realFormName));
		if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.testingMode", "false")))
		{
			writer.print(String.format("data-svy-name=\"%1$s\" ", realFormName));
		}

		if (!form.isResponsiveLayout() && !responsiveMode)
		{
			writer.print("svy-formstyle=\"formStyle\" ");
		}
		else if (design)
		{
			writer.print(" style=\"height:100%\"");
		}
		writer.print("svy-layout-update svy-autosave ");
		// skip the scrollbars for forms in table or list view then the portal component does this.
		if (design || !isTableOrListView(form))
		{
			writer.print(" svy-scrollbars='formProperties.scrollbars'");
		}
		String formClass = "svy-form";
		if (form.getStyleClass() != null)
		{
			formClass += " " + form.getStyleClass();
		}
		writer.print(" class=\"");
		writer.print(formClass);
		writer.print("\"");
		writer.print(">");
	}

	public static boolean isTableOrListView(Form form)
	{
		return (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED ||
			form.getView() == IFormConstants.VIEW_TYPE_LIST || form.getView() == IFormConstants.VIEW_TYPE_LIST_LOCKED);
	}

	public static void generateEndDiv(PrintWriter writer)
	{
		writer.print("</div>");
	}

	public static void generateFormEndTag(PrintWriter writer, boolean design)
	{
		generateEndDiv(writer);
		if (!design) writer.print("</svy-formload>");
	}

	public static void generateFormElementWrapper(PrintWriter writer, FormElement fe, Form form, boolean isResponsive)
	{
		String designId = getDesignId(fe);
		String name = fe.getPersistIfAvailable() instanceof AbstractBase
			? ((AbstractBase)fe.getPersistIfAvailable()).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_TEMPLATE_NAME) : fe.getName();
		boolean selectable = false;
		if (name == null) name = fe.getName();
		else selectable = name.startsWith(FormElement.SVY_NAME_PREFIX);
		if (designId != null)
		{

			writer.print("<div ng-style=\"layout('");
			writer.print(designId);
			writer.print("')\"");

			JSONObject ngClass = new JSONObject();
			ngClass.put("invisible_element", "<getDesignFormControllerScope().model('" + designId + "').svyVisible == false<".toString());
			ngClass.put("highlight_element", "<design_highlight=='highlight_element'<".toString());//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
			if (fe.isFormComponentChild())
			{
				ngClass.put("formComponentChild", true);
			}
			writer.print(" ng-class='" + ngClass.toString().replaceAll("\"<", "").replaceAll("<\"", "").replaceAll("'", "\"") + "'");
			writer.print(" class=\"svy-wrapper\" ");
		}
		else
		{
			writer.print("<div ng-style=\"layout.");
			writer.print(name);
			writer.print("\"");
		}
		if (!isResponsive && fe.getPersistIfAvailable() instanceof BaseComponent)
		{
			BaseComponent bc = (BaseComponent)fe.getPersistIfAvailable();
			int anchors = bc.getAnchors();
			String style = "";
			if (((anchors & IAnchorConstants.EAST) > 0) && ((anchors & IAnchorConstants.WEST) > 0))
			{
				style += "min-width:" + bc.getSize().width + "px;";
			}
			if (((anchors & IAnchorConstants.NORTH) > 0) && ((anchors & IAnchorConstants.SOUTH) > 0))
			{
				style += "min-height:" + bc.getSize().height + "px";
			}
			if (!style.isEmpty())
			{
				writer.print(" style='");
				writer.print(style);
				writer.print("'");
			}
		}
		writer.print(" ng-class=\"'svy-wrapper'\" ");
		if (designId != null)
		{
			writer.print(" svy-id='");
			writer.print(designId);
			writer.print("'");
			if (selectable)
			{
				writer.print(" svy-non-selectable");
			}
			writer.print(" name='");
			writer.print(name);
			writer.print("'");
			List<String>[] typeAndPropertyNames = fe.getSvyTypesAndPropertiesNames();
			if (typeAndPropertyNames[0].size() > 0)
			{
				writer.print(" svy-types='");
				writer.print("[" + StringUtil.join(typeAndPropertyNames[0], ",") + "]");
				writer.print("'");
			}
			String directEditPropertyName = getDirectEditProperty(fe);
			if (directEditPropertyName != null)
			{
				writer.print(" directEditPropertyName='");
				writer.print(directEditPropertyName);
				writer.print("'");
			}
			List<String> forbiddenComponentNames = fe.getForbiddenComponentNames();
			if (forbiddenComponentNames.size() > 0)
			{
				writer.print(" svy-forbidden-components='");
				writer.print("[" + StringUtil.join(forbiddenComponentNames, ",") + "]");
				writer.print("'");
			}
			if (isNotSelectable(fe)) writer.print(" svy-non-selectable");
			Form currentForm = form;
			if (form instanceof FlattenedForm) currentForm = ((FlattenedForm)form).getForm();

			if (fe.getPersistIfAvailable() != null && Utils.isInheritedFormElement(fe.getPersistIfAvailable(), currentForm))
			{
				writer.print(" class='inherited_element'");
			}
		}
		writer.print(">");
	}

//	private static boolean canContainComponents(WebComponentSpecification spec)
//	{
//		Map<String, PropertyDescription> properties = spec.getProperties();
//		for (PropertyDescription propertyDescription : properties.values())
//		{
//			String simpleTypeName = propertyDescription.getType().getName().replaceFirst(spec.getName() + ".", "");
//			if (simpleTypeName.equals(ComponentPropertyType.TYPE_NAME)) return true;
//			Object configObject = propertyDescription.getConfig();
//			if (configObject != null)
//			{
//				try
//				{
//					if (configObject instanceof JSONObject && ((JSONObject)configObject).has(DesignerFilter.DROPPABLE))
//					{
//						Object droppable = ((JSONObject)configObject).get(DesignerFilter.DROPPABLE);
//						if (droppable instanceof Boolean && (Boolean)droppable)
//						{
//							if (simpleTypeName.equals("tab")) return true;
//						}
//					}
//				}
//				catch (JSONException e)
//				{
//					Debug.log(e);
//				}
//			}
//		}
//		return false;
//	}

	public static void generateFormElement(PrintWriter writer, FormElement fe, Form form)
	{
		String name = fe.getPersistIfAvailable() instanceof AbstractBase
			? ((AbstractBase)fe.getPersistIfAvailable()).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_TEMPLATE_NAME) : fe.getName();
		boolean selectable = false;
		if (name == null) name = fe.getName();
		else selectable = name.startsWith(FormElement.SVY_NAME_PREFIX);
		writer.print("<");
		writer.print(fe.getTagname());
		writer.print(" name='");
		writer.print(name);
		writer.print("'");
		if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.testingMode", "false")))
		{
			String elementName = name;
			if (elementName.startsWith("svy_") && fe.getPersistIfAvailable() != null)
			{
				elementName = "svy_" + fe.getPersistIfAvailable().getUUID().toString();
			}
			writer.print(" data-svy-name='");
			writer.print(form.getName() + "." + elementName);
			writer.print("'");
		}
		String designId = getDesignId(fe);
		if (designId != null)
		{
			if (form.isResponsiveLayout())
			{
				writer.print(" svy-id='");
				writer.print(designId);
				writer.print("'");
				if (selectable)
				{
					writer.print(" svy-non-selectable");
				}
				writer.print(" svy-formelement-type='");
				writer.print(fe.getTypeName());
				writer.print("'");
				JSONObject ngClass = new JSONObject();

				List<String>[] typeAndPropertyNames = fe.getSvyTypesAndPropertiesNames();
				if (typeAndPropertyNames[0].size() > 0)
				{
					writer.print(" svy-types='");
					writer.print("[" + StringUtil.join(typeAndPropertyNames[0], ",") + "]");
					writer.print("'");

					writer.print(" svy-types-properties='");
					writer.print("[" + StringUtil.join(typeAndPropertyNames[1], ",") + "]");
					writer.print("'");

					ngClass.put("drop_highlight",
						"<canContainDraggedElement('" + fe.getTypeName() + "',['" + StringUtil.join(typeAndPropertyNames[0], "','") + "'])<");
				}
				List<String> forbiddenComponentNames = fe.getForbiddenComponentNames();
				if (forbiddenComponentNames.size() > 0)
				{
					writer.print(" svy-forbidden-components='");
					writer.print("[" + StringUtil.join(forbiddenComponentNames, ",") + "]");
					writer.print("'");
				}
				String directEditPropertyName = getDirectEditProperty(fe);
				if (directEditPropertyName != null)
				{
					writer.print(" directEditPropertyName='");
					writer.print(directEditPropertyName);
					writer.print("'");
				}


				if (!fe.getForm().equals(form) || fe.getPersistIfAvailable() instanceof ISupportExtendsID &&
					PersistHelper.getSuperPersist((ISupportExtendsID)fe.getPersistIfAvailable()) != null) //is this inherited or override element?
				{
					ngClass.put("inheritedElement", true);
				}

				if (fe.isFormComponentChild())
				{
					ngClass.put("formComponentChild", true);
				}

				ngClass.put("invisible_element", "<getDesignFormControllerScope().model('" + designId + "').svyVisible == false<".toString());
				ngClass.put("highlight_element", "<design_highlight=='highlight_element'<".toString());//added <> tokens so that we can remove quotes around the values so that angular will evaluate at runtime
				writer.print(" ng-class='" + ngClass.toString().replaceAll("\"<", "").replaceAll("<\"", "").replaceAll("'", "\"") + "'");
			}
			writer.print(" svy-model=\"model('");
			writer.print(designId);
			writer.print("')\"");
			writer.print(" svy-api=\"api('");
			writer.print(designId);
			writer.print("')\"");
			writer.print(" svy-handlers=\"handlers('");
			writer.print(designId);
			writer.print("')\"");
			writer.print(" svy-servoyApi=\"servoyApi('");
			writer.print(designId);
			writer.print("')\"");
			if (fe.getPersistIfAvailable() instanceof IFormElement)
			{
				writer.print(" form-index=" + ((IFormElement)fe.getPersistIfAvailable()).getFormIndex() + "");
			}
		}
		else
		{
			writer.print(" svy-model='model.");
			writer.print(name);
			writer.print("'");
			writer.print(" svy-api='api.");
			writer.print(name);
			writer.print("'");
			writer.print(" svy-handlers='handlers.");
			writer.print(name);
			writer.print("'");
			writer.print(" svy-servoyApi='handlers.");
			writer.print(name);
			writer.print(".svy_servoyApi'");
		}

		writer.print(">");
		writer.print("</");
		writer.print(fe.getTagname());
		writer.print(">");
	}

	public static String getDesignId(FormElement fe)
	{
		return fe.getDesignId();
	}

	private static String getDirectEditProperty(FormElement fe)
	{
		Map<String, PropertyDescription> properties = fe.getWebComponentSpec(false).getProperties();
		for (PropertyDescription pd : properties.values())
		{
			if (Utils.getAsBoolean(pd.getTag("directEdit")))
			{
				return pd.getName();
			}
		}
		return null;
	}

	private static boolean isNotSelectable(FormElement fe)
	{
		return (fe.getDesignId() == null && fe.getTypeName().equals("servoycore-portal"));
	}
}
