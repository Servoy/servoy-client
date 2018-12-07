/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.impl.ClientService;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.INGClientWindow.IFormHTMLAndJSGenerator;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.FormElementToJSON;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class Angular2FormGenerator implements IFormHTMLAndJSGenerator
{


	private final NGClient client;
	private final Form form;
	private final String realFormName;

	/**
	 * @param client
	 * @param form
	 * @param realFormName
	 */
	public Angular2FormGenerator(NGClient client, Form form, String realFormName)
	{
		this.client = client;
		this.form = form;
		this.realFormName = realFormName;
	}

	@Override
	public String generateHTMLTemplate()
	{
		// no html template needed.
		StringBuilder sb = new StringBuilder();
		WebObjectSpecification[] specs = WebComponentSpecProvider.getSpecProviderState().getAllWebComponentSpecifications();
		for (WebObjectSpecification spec : specs)
		{
			sb.append("<ng-template #");
			sb.append(ClientService.convertToJSName(spec.getName()));
			sb.append(" let-state=\"state\">");
			sb.append('<');
			sb.append(spec.getName());
			sb.append(' ');

			for (PropertyDescription pd : spec.getProperties().values())
			{
				if (pd.getName().equals("anchors") || pd.getName().equals("formIndex")) continue;
				sb.append(" [");
				sb.append(pd.getName());
				sb.append("]=\"state.model.");
				sb.append(pd.getName());
				sb.append('"');

				if (pd.getPushToServer() != null && pd.getPushToServer() != PushToServerEnum.reject)
				{
					sb.append(" (");
					sb.append(pd.getName());
					sb.append("Change)=\"datachange(state.name,'");
					sb.append(pd.getName());
					sb.append("',$event)\"");
				}
			}

			for (WebObjectFunctionDefinition handler : spec.getHandlers().values())
			{
				sb.append(" [");
				sb.append(handler.getName());
				sb.append("]=\"getHandler(state,'");
				sb.append(handler.getName());
				sb.append("')\"");
			}
			sb.append(" [servoyApi]=\"getServoyApi(state)\"");
			sb.append(" [name]=\"state.name\" #cmp");
			sb.append(">");
			Collection<PropertyDescription> properties = spec.getProperties(FormPropertyType.INSTANCE);
			if (properties.size() == 0)
			{
				Map<String, IPropertyType< ? >> declaredCustomObjectTypes = spec.getDeclaredCustomObjectTypes();
				for (IPropertyType< ? > pt : declaredCustomObjectTypes.values())
				{
					if (pt instanceof CustomJSONPropertyType< ? >)
					{
						PropertyDescription customJSONSpec = ((CustomJSONPropertyType< ? >)pt).getCustomJSONTypeDefinition();
						properties = customJSONSpec.getProperties(FormPropertyType.INSTANCE);
						if (properties.size() > 0) break;
					}
				}
			}
			if (properties.size() > 0)
			{
				sb.append("<ng-template let-name='name'><svy-form *ngIf=\"isFormAvailable(name)\" [name]=\"name\"></svy-form></ng-template>");
			}
			sb.append("</");
			sb.append(spec.getName());
			sb.append(">");

			sb.append("</ng-template>\n");

		}
		System.err.println(sb.toString());
		return "";
	}

	@Override
	public String generateJS() throws IOException
	{
		IWebFormController cachedFormController = client.getFormManager().getCachedFormController(realFormName);
		// for this form it is really just some json.
		StringWriter stringWriter = new StringWriter();
		final JSONWriter writer = new JSONWriter(stringWriter);
		writer.object();
		writer.key(realFormName);
		writer.object();
		writer.key("responsive");
		writer.value(form.isResponsiveLayout());
		String styleClasses = form.getStyleClass();
		if (styleClasses != null)
		{
			writer.key("styleclass");
			String[] classes = styleClasses.split(" ");
			writer.array();
			for (String cls : classes)
			{
				writer.value(cls);
			}
			writer.endArray();
		}
		writer.key("children");
		// write the default form value object.
		writer.array();
		writer.object();
		writer.key("name");
		writer.value("");
		writer.key("model");
		writer.object();
		if (cachedFormController != null && cachedFormController.getFormUI() instanceof Container)
		{
			Container con = (Container)cachedFormController.getFormUI();
			DataConversion dataConversion = new DataConversion();
			con.writeProperties(FullValueToJSONConverter.INSTANCE, null, writer, con.getProperties(), dataConversion);
			JSONUtils.writeClientConversions(writer, dataConversion);
		}
		writer.endObject();
		writer.endObject();
		form.acceptVisitor(new ChildrenJSONGenerator(writer,
			cachedFormController != null ? new ServoyDataConverterContext(cachedFormController) : new ServoyDataConverterContext(client), form));
		writer.endArray();
		writer.endObject();
		writer.endObject();
		System.err.println(stringWriter.toString());

		return stringWriter.toString();
	}


	/**
	 * @author jcompagner
	 */
	private final class ChildrenJSONGenerator implements IPersistVisitor
	{
		private final JSONWriter writer;
		private final ServoyDataConverterContext context;
		private final WebFormUI formUI;
		private final Object skip;

		/**
		 * @param writer
		 * @param client
		 * @param cachedFormController
		 */
		private ChildrenJSONGenerator(JSONWriter writer, ServoyDataConverterContext context, Object skip)
		{
			this.writer = writer;
			this.context = context;
			this.skip = skip;
			formUI = (context.getForm() != null && context.getForm().getFormUI() instanceof WebFormUI) ? (WebFormUI)context.getForm().getFormUI() : null;
			if (formUI != null)
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

		@Override
		public Object visit(IPersist o)
		{
			if (o == skip) return IPersistVisitor.CONTINUE_TRAVERSAL;
			if (o instanceof IFormElement)
			{
				FormElement fe = null;
				if (formUI != null)
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
				fe = fe != null ? fe : FormElementHelper.INSTANCE.getFormElement((IFormElement)o, client.getFlattenedSolution(), null, false);
				writer.object();
				writer.key("name");
				String name = o instanceof AbstractBase ? ((AbstractBase)o).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_TEMPLATE_NAME) : fe.getName();
				if (name == null) name = fe.getName();
				writer.value(name);
				writer.key("type");
				if (o instanceof TabPanel)
				{
					// special support for TabPanel so that we have a specific tabpanel,tablesspanel,accordion and splitpane
					String type = "servoydefault-tabpanel";
					int orient = ((TabPanel)o).getTabOrientation();
					if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) type = "servoydefault-splitpane";
					else if (orient == TabPanel.ACCORDION_PANEL) type = "servoydefault-accordion";
					else if (orient == TabPanel.HIDE || (orient == TabPanel.DEFAULT_ORIENTATION && ((TabPanel)o).hasOneTab()))
						type = "servoydefault-tablesspanel";
					writer.value(ClientService.convertToJSName(type));
				}
				else
				{
					// hack for now to map it to the types that we know are there, so that we can test responsive without really already having to have bootstrap components.
					writer.value(mapOnDefaultForDebug(ClientService.convertToJSName(FormTemplateGenerator.getComponentTypeName((IFormElement)o))));
				}
				if (o instanceof BaseComponent && ((BaseComponent)o).getCssPosition() != null)
				{
					CSSPosition position = ((BaseComponent)o).getCssPosition();
					writer.key("position");
					writer.object();
					if (CSSPosition.isSet(position.left))
					{
						writer.key("left").value(CSSPosition.getCSSValue(position.left));
					}
					if (CSSPosition.isSet(position.top))
					{
						String top = position.top;
						Point location = CSSPosition.getLocation((BaseComponent)o);
						Part part = form.getPartAt(location.y);
						if (part != null)
						{
							int topStart = form.getPartStartYPos(part.getID());
							if (topStart > 0)
							{
								if (top.endsWith("px"))
								{
									top = top.substring(0, top.length() - 2);
								}
								int topInteger = Utils.getAsInteger(top, -1);
								if (topInteger != -1)
								{
									top = String.valueOf(topInteger - topStart);
								}
								else
								{
									top = "calc(" + top + "-" + topStart + "px)";
								}
							}
						}
						writer.key("top").value(CSSPosition.getCSSValue(top));
					}
					if (CSSPosition.isSet(position.bottom))
					{
						writer.key("bottom").value(CSSPosition.getCSSValue(position.bottom));
					}
					if (CSSPosition.isSet(position.right))
					{
						writer.key("right").value(CSSPosition.getCSSValue(position.right));
					}
					if (CSSPosition.isSet(position.width))
					{
						if (CSSPosition.isSet(position.left) && CSSPosition.isSet(position.right))
						{
							writer.key("min-width").value(CSSPosition.getCSSValue(position.width));
						}
						else
						{
							writer.key("width").value(CSSPosition.getCSSValue(position.width));
						}
					}
					if (CSSPosition.isSet(position.height))
					{
						if (CSSPosition.isSet(position.top) && CSSPosition.isSet(position.bottom))
						{
							writer.key("min-height").value(CSSPosition.getCSSValue(position.height));
						}
						else
						{
							writer.key("height").value(CSSPosition.getCSSValue(position.height));
						}
					}
					writer.endObject();
				}
				else
				{
					Point location = ((IFormElement)o).getLocation();
					Dimension size = ((IFormElement)o).getSize();
					if (location != null && size != null)
					{
						writer.key("position");
						writer.object();
						writer.key("left");
						writer.value(location.x + "px");
						writer.key("top");
						writer.value(location.y + "px");
						writer.key("width");
						writer.value(size.width + "px");
						writer.key("height");
						writer.value(size.height + "px");
						writer.endObject();
					}
				}
				writer.key("model");
				writer.object();
				if (formUI != null)
				{
					// there is a existing form, take the current properties from that.
					WebFormComponent webComponent = formUI.getWebComponent(fe.getName());
					TypedData<Map<String, Object>> properties = webComponent.getProperties();
					TypedData<Map<String, Object>> templateProperties = fe.propertiesForTemplateJSON();
					// remove from the templates properties all the properties that are current "live" in the component
					templateProperties.content.keySet().removeAll(properties.content.keySet());
					DataConversion dataConversion = new DataConversion();
					// write the template properties that are left
					JSONUtils.writeData(FormElementToJSON.INSTANCE, writer, templateProperties.content, templateProperties.contentType, dataConversion,
						new FormElementContext(fe));
					// write the actual values
					webComponent.writeProperties(FullValueToJSONConverter.INSTANCE, null, writer, properties, dataConversion);
					JSONUtils.writeClientConversions(writer, dataConversion);
				}
				else
				{
					fe.propertiesAsTemplateJSON(writer, new FormElementContext(fe));
				}
				if (o instanceof BaseComponent)
				{
					writer.key("attributes");
					writer.array();
					Map<String, String> attributes = new HashMap<String, String>(((BaseComponent)fe.getPersistIfAvailable()).getMergedAttributes());
					attributes.forEach((key, value) -> {
						writer.object();
						writer.key("key");
						writer.value(StringEscapeUtils.escapeEcmaScript(key));
						if (value != null && value.length() > 0)
						{
							writer.key("value");
							writer.value(HtmlUtils.escapeMarkup(value, false, false));
						}
						writer.endObject();
					});
					writer.endArray();
				}
				writer.endObject();
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
				writer.endObject();
			}
			else if (o instanceof LayoutContainer)
			{
				writer.object();
				writer.key("layout");
				writer.value(true);
				String styleClasses = ((LayoutContainer)o).getCssClasses();
				if (styleClasses != null)
				{
					writer.key("styleclass");
					String[] classes = styleClasses.split(" ");
					writer.array();
					for (String cls : classes)
					{
						writer.value(cls);
					}
					writer.endArray();
				}
				if (((LayoutContainer)o).getStyle() != null)
				{
					writer.key("style");
					writer.value(((LayoutContainer)o).getStyle());
				}
				writer.key("children");
				writer.array();
				o.acceptVisitor(new ChildrenJSONGenerator(writer, context, o));
				writer.endArray();
				writer.endObject();
				return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		}

		/**
		 * @param convertToJSName
		 * @return
		 */
		private String mapOnDefaultForDebug(String typeName)
		{
			switch (typeName)
			{
				case "bootstrapcomponentsTextbox" :
					return "servoydefaultTextfield";
				case "bootstrapcomponentsButton" :
					return "servoydefaultButton";
			}
			return typeName;
		}
	}

}

