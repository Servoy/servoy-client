/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.base.persistence.constants.IContentSpecConstantsBase;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.FormElementToJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignerDefaultWriter;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public final class ChildrenJSONGenerator implements IPersistVisitor
{
	public static final Comparator<IPersist> FORM_INDEX_WITH_HIERARCHY_COMPARATOR = new Comparator<IPersist>()
	{
		@Override
		public int compare(IPersist o1, IPersist o2)
		{
			if (o1 instanceof IFormElement && o2 instanceof IFormElement)
			{
				return FlattenedForm.FORM_INDEX_WITH_HIERARCHY_COMPARATOR.compare((IFormElement)o1, (IFormElement)o2);
			}
			if (o1 instanceof IFormElement) return 1;
			if (o2 instanceof IFormElement) return -1;
			return o1.getID() - o2.getID();
		}
	};

	public static final Comparator<IPersist> FORM_INDEX_WITH_HIERARCHY_AND_TABSEQUENCE_COMPARATOR = new Comparator<IPersist>()
	{
		@Override
		public int compare(IPersist o1, IPersist o2)
		{
			int result = FORM_INDEX_WITH_HIERARCHY_COMPARATOR.compare(o1, o2);
			if (result == 0)
			{
				result = TabSeqComparator.compareTabSeq(FormLayoutGenerator.getTabSeq((IFormElement)o1), o1,
					FormLayoutGenerator.getTabSeq((IFormElement)o2), o2);
			}
			return result;
		}
	};
	private final JSONWriter writer;
	private final ServoyDataConverterContext context;
	private final WebFormUI formUI;
	private final Object skip;
	private final IFormElementCache cache;
	private final Part part;
	private final Form form;
	private final boolean designer;
	private static final String TAG_DIRECT_EDIT = "directEdit";

	public ChildrenJSONGenerator(JSONWriter writer, ServoyDataConverterContext context, Object skip, IFormElementCache cache, Part part, Form form,
		boolean mainFormGeneration, boolean designer)
	{
		this.writer = writer;
		this.context = context;
		this.skip = skip;
		this.cache = cache;
		this.form = form;
		this.designer = designer;
		formUI = (context.getForm() != null && context.getForm().getFormUI() instanceof WebFormUI)
			? (WebFormUI)context.getForm().getFormUI() : null;
		this.part = part;
		if (formUI != null && mainFormGeneration)
		{
			// write component properties is not called so do register the container here with the current window.
			CurrentWindow.get().registerContainer(formUI);

			// add default navigator
			if (context.getForm().getForm().getNavigatorID() == Form.NAVIGATOR_DEFAULT)
			{
				visit(DefaultNavigator.INSTANCE);
			}
		}
	}

	@SuppressWarnings("nls")
	@Override
	public Object visit(IPersist o)
	{
		if (o == skip) return IPersistVisitor.CONTINUE_TRAVERSAL;
		if (!isSecurityVisible(o))
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		if (part != null && (o instanceof IFormElement || o instanceof CSSPositionLayoutContainer))
		{
			int startPos = form.getPartStartYPos(part.getID());
			int endPos = part.getHeight();
			Point location = CSSPositionUtils.getLocation(((ISupportBounds)o), form);
			if (location != null && (startPos > location.y || endPos <= location.y))
			{
				return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		}
		if (o instanceof IFormElement)
		{
			FormElement fe = null;
			if (cache != null)
			{
				// this is for form component elements finding
				fe = cache.getFormElement((IFormElement)o, this.context.getSolution(), null, designer);
			}
			if (fe == null && formUI != null)
			{
				List<FormElement> cachedFormElements = formUI.getFormElements();
				for (FormElement cachedFE : cachedFormElements)
				{
					if (Utils.equalObjects(cachedFE.getPersistIfAvailable(), o))
					{
						fe = cachedFE;
						break;
					}
				}
			}
			fe = fe != null ? fe : FormElementHelper.INSTANCE.getFormElement((IFormElement)o, this.context.getSolution(), null, designer);
			writer.object();

			writeFormElement(writer, o, form, fe, formUI, context, designer);

			if (o instanceof WebComponent)
			{
				WebObjectSpecification spec = fe.getWebComponentSpec();
				if (spec != null)
				{
					Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
					if (properties.size() > 0)
					{
						boolean isResponsive = false;
						List<String> children = new ArrayList<>();
						for (PropertyDescription pd : properties)
						{
							Object propertyValue = fe.getPropertyValue(pd.getName());
							Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution());
							if (frm == null) continue;
							frm = context.getSolution().getFlattenedForm(frm);
							isResponsive = frm.isResponsiveLayout();
							// listformcomponents that are responsive must be also send over here (the components are also send over in the FormComponentSabloValue)
							// this will result in duplicate component data, but we need the structure (and the component names in the right place)
//							if (!isResponsive && pd.getConfig() instanceof ComponentTypeConfig && ((ComponentTypeConfig)pd.getConfig()).forFoundset != null)
//								continue;
							children.add("children_" + pd.getName());
							writer.key("children_" + pd.getName());
							writer.array();
							FormComponentCache fccc = FormElementHelper.INSTANCE.getFormComponentCache(fe, pd,
								(JSONObject)propertyValue, frm,
								context.getSolution());
							if (isResponsive || frm.containsResponsiveLayout())
							{
								// layout containers are not in the cache we need to generate manually the model
								frm.acceptVisitor(new ChildrenJSONGenerator(writer, context, frm, new IFormElementCache()
								{
									@Override
									public FormElement getFormElement(IFormElement component, FlattenedSolution flattendSol, PropertyPath path,
										boolean design)
									{
										for (FormElement formElement : fccc.getFormComponentElements())
										{
											if (component.getID() == formElement.getPersistIfAvailable().getID())
											{
												return formElement;
											}
										}
										return FormElementHelper.INSTANCE.getFormElement(component, flattendSol, path, design);
									}
								}, null, this.form, false, designer), PositionComparator.XY_PERSIST_COMPARATOR);
							}
							else
							{
								List<IPersist> formElements = fccc.getFormComponentElements().stream().map(element -> element.getPersistIfAvailable())
									.sorted(FORM_INDEX_WITH_HIERARCHY_AND_TABSEQUENCE_COMPARATOR).toList();
								for (IPersist persistOfElement : formElements)
								{
									persistOfElement.acceptVisitor(new ChildrenJSONGenerator(writer, context, null, null, null, this.form, false, designer),
										FORM_INDEX_WITH_HIERARCHY_AND_TABSEQUENCE_COMPARATOR);
								}

							}
							writer.endArray();
						}
						writer.key("formComponent");
						writer.array();
						children.stream().forEach((child) -> writer.value(child));
						writer.endArray();
					}
				}
			}
			writer.endObject();
		}
		else if (o instanceof LayoutContainer)
		{
			writer.object();
			LayoutContainer layoutContainer = (LayoutContainer)o;

			writeLayoutContainer(writer, layoutContainer, formUI, form, designer);

			writer.key("children");
			writer.array();
			o.acceptVisitor(new ChildrenJSONGenerator(writer, context, o, cache, null, this.form, false, designer),
				PositionComparator.XY_PERSIST_COMPARATOR);
			writer.endArray();
			writer.endObject();
			return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
		}
		return IPersistVisitor.CONTINUE_TRAVERSAL;
	}

	public boolean isSecurityVisible(IPersist persist)
	{
		if (context.getApplication() == null || persist.getUUID() == null || !(persist instanceof IFormElement)) return true;
		int access = context.getApplication().getFlattenedSolution().getSecurityAccess(persist.getUUID(),
			form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
		boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
		return b_visible;
	}

	public static void writeFormElement(JSONWriter writer, IPersist o, Form form, FormElement fe, WebFormUI formUI, ServoyDataConverterContext context,
		boolean designer)
	{
		writer.key("name");
		String name = designer ? fe.getDesignId() : fe.getName();
		writer.value(name);
		writer.key("specName");
		if (o instanceof TabPanel)
		{
			handleTabpanelSpecNameAndElementName(writer, o);
		}
		else
		{
			// hack for now to map it to the types that we know are there, so that we can test responsive without really already having to have bootstrap components.
			writer.value(FormTemplateGenerator.getComponentTypeName((IFormElement)o));
		}
		WebFormComponent webComponent = (formUI != null) ? formUI.getWebComponent(fe.getName()) : null;

		AngularFormGenerator.writePosition(writer, o, form, webComponent, designer);
		writer.key("model");
		writer.object();
		Map<String, String> attributes = null;
		if (o instanceof BaseComponent)
		{
			attributes = new HashMap<String, String>(((BaseComponent)fe.getPersistIfAvailable()).getMergedAttributes());
		}
		if (designer || webComponent == null)
		{
			// can webcomponnt be null in actual client ?
			TypedData<Map<String, Object>> templateProperties = fe.propertiesForTemplateJSON();

			if (designer)
			{
				fe.getWebComponentSpec().getProperties().values().forEach(pd -> {
					if (pd.getType() instanceof IDesignerDefaultWriter)
					{
						((IDesignerDefaultWriter)pd.getType()).toDesignerDefaultJSONValue(writer, pd.getName());
						templateProperties.content.keySet().remove(pd.getName());
					}
				});
			}
			templateProperties.content.keySet().remove(IContentSpecConstants.PROPERTY_ATTRIBUTES);
			JSONUtils.writeData(FormElementToJSON.INSTANCE, writer, templateProperties.content, templateProperties.contentType,
				new FormElementContext(fe, context, null));
			if (designer)
			{
				writer.key("svyVisible").value(fe.isVisible()); // see fe.propertiesAsTemplateJSON(JSONWriter writer, FormElementContext context, boolean writeAsValue) which does the same

				if (Utils.isInheritedFormElement(o, form))
				{
					writer.key("svyInheritedElement");
					writer.value(true);
				}
			}
		}
		else
		{
			TypedData<Map<String, Object>> properties = webComponent.getProperties();
			TypedData<Map<String, Object>> templateProperties = fe.propertiesForTemplateJSON();
			// remove from the templates properties all the properties that are current "live" in the component
			templateProperties.content.keySet().removeAll(properties.content.keySet());
			templateProperties.content.keySet().remove(IContentSpecConstants.PROPERTY_ATTRIBUTES);
			if (properties.content.containsKey(IContentSpecConstants.PROPERTY_ATTRIBUTES))
			{
				properties.content = new HashMap<String, Object>(properties.content);
				if (attributes != null)
				{
					attributes.putAll((Map<String, String>)properties.content.get(IContentSpecConstants.PROPERTY_ATTRIBUTES));
				}
				properties.content.keySet().remove(IContentSpecConstants.PROPERTY_ATTRIBUTES);
			}

			// write the template properties that are left
			JSONUtils.writeData(FormElementToJSON.INSTANCE, writer, templateProperties.content, templateProperties.contentType,
				new FormElementContext(fe, context, null));
			// write the actual values
			webComponent.writeProperties(FullValueToJSONConverter.INSTANCE, null, writer, properties);
		}

		if (o instanceof BaseComponent)
		{
			writer.key("servoyAttributes");
			writer.object();
			if (designer)
			{
				attributes.put("svy-id", fe.getDesignId());
				attributes.put("svy-formelement-type", fe.getTypeName());
				attributes.put("svy-name", fe.getName());
				attributes.put("svy-anchors", Integer.toString(((BaseComponent)o).getAnchors()));
				List<String>[] typeAndPropertyNames = fe.getSvyTypesAndPropertiesNames();
				if (typeAndPropertyNames[0].size() > 0)
				{
					attributes.put("svy-types", String.join(",", typeAndPropertyNames[0]));
					attributes.put("svy-types-properties", String.join(",", typeAndPropertyNames[1]));
				}
				attributes.put("svy-priority",
					form.isResponsiveLayout() || o.getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) != null
						? String.valueOf(((ISupportBounds)o).getLocation().x) : String.valueOf(((BaseComponent)o).getFormIndex()));
				String directEditPropertyName = getDirectEditProperty(fe);
				if (directEditPropertyName != null)
				{
					attributes.put("directEditPropertyName", directEditPropertyName);
				}
			}
			if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.testingMode", "false")))
			{
				String elementName = name;
				if (elementName.startsWith("svy_") && o.getUUID() != null)
				{
					elementName = "svy_" + o.getUUID().toString();
				}
				attributes.put("data-cy", form.getName() + "." + elementName);
			}
			attributes.forEach((key, value) -> {
				writer.key(StringEscapeUtils.escapeEcmaScript(key));
				writer.value(value);
			});
			writer.endObject();
		}
		writer.endObject();

		WebObjectSpecification spec = fe.getWebComponentSpec();
		if (spec != null)
		{
			Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
			if (properties.size() > 0)
			{
				boolean isResponsive = false;
				for (PropertyDescription pd : properties)
				{
					Object propertyValue = fe.getPropertyValue(pd.getName());
					Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution());
					if (frm == null) continue;
					isResponsive = frm.isResponsiveLayout();
				}
				// responsive state can change, so send it with updates
				writer.key("responsive");
				writer.value(isResponsive);
			}
		}

		Collection<String> handlers = fe.getHandlers();
		if (handlers.size() > 0)
		{
			writer.key("handlers");
			writer.array();
			for (String handler : handlers)
			{
				writer.value(handler);
			}
			writer.endArray();
		}
	}

	public static void handleTabpanelSpecNameAndElementName(JSONWriter writer, IPersist o)
	{
		// special support for TabPanel so that we have a specific tabpanel, tablesspanel, accordion and splitpane

		// default splitpane has a different .spec file (but still TabPanelPersist)
		// default tabpanel, tablesspanel, accordion all share the tabpanel spec file but with different element/tag names on client

		String elementTypeForClient = null;
		String specName = "servoydefault-tabpanel";
		int orient = ((TabPanel)o).getTabOrientation();
		if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) specName = "servoydefault-splitpane";
		else if (orient == TabPanel.ACCORDION_PANEL) elementTypeForClient = "servoydefault-accordion";
		else if (orient == TabPanel.HIDE || (orient == TabPanel.DEFAULT_ORIENTATION && ((TabPanel)o).hasOneTab()))
			elementTypeForClient = "servoydefault-tablesspanel";

		writer.value(specName);
		if (elementTypeForClient != null) writer.key("elType").value(elementTypeForClient);
	}

	public static void writeLayoutContainer(JSONWriter writer, LayoutContainer layoutContainer, WebFormUI formUI, Form form, boolean designer)
	{
		WebLayoutSpecification spec = null;
		if (layoutContainer.getPackageName() != null)
		{
			PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				layoutContainer.getPackageName());
			if (pkg != null)
			{
				spec = pkg.getSpecification(layoutContainer.getSpecName());
			}
		}
		if (layoutContainer instanceof CSSPositionLayoutContainer)
		{
			CSSPosition cssPosition = ((CSSPositionLayoutContainer)layoutContainer).getCssPosition();
			if (cssPosition != null) AngularFormGenerator.writeCSSPosition(writer, ((CSSPositionLayoutContainer)layoutContainer), form, designer, cssPosition);
		}

		writer.key("layout");
		writer.value(true);
		writer.key("cssPositionContainer");
		boolean cssPositionContainer = CSSPositionUtils.isCSSPositionContainer(layoutContainer);
		writer.value(cssPositionContainer);
		String tagType = layoutContainer.getTagType();
		if (spec != null && spec.getDirectives().size() > 0)
		{
			tagType = spec.getName();
		}
		if (!"div".equals(tagType))
		{
			writer.key("tagname");
			writer.value("svyResponsive" + tagType);
		}
		String styleClasses = layoutContainer.getCssClasses();
		if (layoutContainer instanceof CSSPositionLayoutContainer)
		{
			styleClasses = styleClasses != null ? styleClasses + " svy-responsivecontainer" : "svy-responsivecontainer";
		}
		if (styleClasses != null)
		{
			writer.key("styleclass");
			String[] classes = styleClasses.split(" ");
			writer.array();
			for (String cls : classes)
			{
				if (!cls.trim().isEmpty())
					writer.value(cls);
			}
			writer.endArray();
		}
		Map<String, String> attributes = new HashMap<String, String>(layoutContainer.getMergedAttributes());
		// properties in the .spec file for layouts are seen as "attributes to add to html tag"
		// except for "class" and "size" that are special - treatead separately below
		if (spec != null)
		{
			for (String propertyName : spec.getAllPropertiesNames())
			{
				if (IContentSpecConstantsBase.PROPERTY_SIZE.equals(propertyName) || "class".equals(propertyName)) continue;

				PropertyDescription pd = spec.getProperty(propertyName);
				if (pd.getDefaultValue() != null && !attributes.containsKey(propertyName))
				{
					attributes.put(propertyName, pd.getDefaultValue().toString()); // they should be already strings in the spec files!
				}
			}
		}
		if (formUI != null && layoutContainer.getName() != null)
		{
			attributes.put("name", formUI.getName() + "." + layoutContainer.getName());
		}
		if (designer)
		{
			attributes.put("svy-id", layoutContainer.getUUID().toString());
			if (spec != null)
			{
				attributes.put("svy-layoutname", spec.getPackageName() + "." + spec.getName());
				String solutionStyleClasses = FormLayoutStructureGenerator.getSolutionSpecificClasses(spec, layoutContainer);
				if (solutionStyleClasses != null && !solutionStyleClasses.isEmpty())
				{
					attributes.put("svy-solution-layout-class", solutionStyleClasses);
				}
			}
			if (layoutContainer.getName() != null)
			{
				attributes.put("svy-name", layoutContainer.getName());
			}
			attributes.put("svy-priority", String.valueOf(layoutContainer.getLocation().x));
			if (spec != null)
			{
				String designClass = spec.getDesignStyleClass() != null && spec.getDesignStyleClass().length() > 0 ? spec.getDesignStyleClass()
					: "customDivDesign";
				if ("customDivDesign".equals(designClass) && FormLayoutStructureGenerator.hasSameDesignClassAsParent(layoutContainer, spec))
				{
					designClass = FormLayoutStructureGenerator.isEvenLayoutContainer(layoutContainer) ? "customDivDesignOdd" : "customDivDesignEven";
				}
				attributes.put("designclass", designClass);
			}

			attributes.put("svy-title", FormLayoutStructureGenerator.getLayouContainerTitle(layoutContainer));
		}
		writer.key("attributes");
		writer.object();
		attributes.remove("class");
		if (cssPositionContainer)
		{
			// client side needs to know height in this case
			Dimension sizePropValue = layoutContainer.hasProperty(IContentSpecConstantsBase.PROPERTY_SIZE) ? layoutContainer.getSize() : null;
			if (sizePropValue != null)
			{
				DimensionPropertyType.INSTANCE.toJSON(writer, IContentSpecConstantsBase.PROPERTY_SIZE, sizePropValue, null, null);
			}
			else if (spec != null)
			{
				// use default value from. spec if available
				PropertyDescription pd = spec.getProperty(IContentSpecConstantsBase.PROPERTY_SIZE);
				if (pd.getDefaultValue() != null)
				{
					writer.key(IContentSpecConstantsBase.PROPERTY_SIZE).value(pd.getDefaultValue());
				}
				else
				{
					// no default in spec, property not set in form designer => use default from AbstractContainer.getSize()
					DimensionPropertyType.INSTANCE.toJSON(writer, IContentSpecConstantsBase.PROPERTY_SIZE, layoutContainer.getSize(), null, null);
				}
			}
		}
		attributes.forEach((key, value) -> {
			writer.key(key);
			writer.value(value);
		});
		writer.endObject();
	}

	private static String getDirectEditProperty(FormElement fe)
	{
		Map<String, PropertyDescription> properties = fe.getWebComponentSpec(false).getProperties();
		for (PropertyDescription pd : properties.values())
		{
			if (Utils.getAsBoolean(pd.getTag(TAG_DIRECT_EDIT)))
			{
				return pd.getName();
			}
		}
		return null;
	}

}