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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.impl.ClientService;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.headlessclient.dataui.AbstractFormLayoutProvider;
import com.servoy.j2db.server.ngclient.INGClientWindow.IFormHTMLAndJSGenerator;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateObjectWrapper;
import com.servoy.j2db.server.ngclient.template.FormWrapper;
import com.servoy.j2db.server.ngclient.template.PartWrapper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 2021.03
 */
public class AngularFormGenerator implements IFormHTMLAndJSGenerator
{


	private final NGClient client;
	private final Form form;
	private final String realFormName;

	/**
	 * @param client
	 * @param form
	 * @param realFormName
	 */
	public AngularFormGenerator(NGClient client, Form form, String realFormName)
	{
		this.client = client;
		this.form = form;
		this.realFormName = realFormName;
	}

	@SuppressWarnings("nls")
	@Override
	public String generateHTMLTemplate()
	{
		// no html template needed.
		StringBuilder sb = new StringBuilder();
		WebObjectSpecification[] specs = WebComponentSpecProvider.getSpecProviderState().getAllWebComponentSpecifications();
		Arrays.sort(specs, new Comparator<WebObjectSpecification>()
		{
			@Override
			public int compare(WebObjectSpecification o1, WebObjectSpecification o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		for (WebObjectSpecification spec : specs)
		{
			if (spec.isDeprecated()) continue;
			genereateSpec(sb, spec, spec.getName());
			if (spec.getName().equals("servoydefault-tabpanel"))
			{
				// also generate the tabless
				genereateSpec(sb, spec, "servoydefault-tablesspanel");
			}

		}
		System.err.println(sb.toString());
		return "";
	}

	/**
	 * @param sb
	 * @param spec
	 * @param specName
	 */
	@SuppressWarnings("nls")
	private void genereateSpec(StringBuilder sb, WebObjectSpecification spec, String specName)
	{
		sb.append("<ng-template #");
		sb.append(ClientService.convertToJSName(specName));
		sb.append(" let-callback=\"callback\" let-state=\"state\">");
		sb.append('<');
		sb.append(specName);
		sb.append(' ');

		ArrayList<PropertyDescription> specProperties = new ArrayList<>(spec.getProperties().values());
		Collections.sort(specProperties, new Comparator<PropertyDescription>()
		{
			@Override
			public int compare(PropertyDescription o1, PropertyDescription o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		for (PropertyDescription pd : specProperties)
		{
			String name = pd.getName();
			if (name.equals("anchors") || name.equals("formIndex")) continue;
			if (name.equals(IContentSpecConstants.PROPERTY_ATTRIBUTES))
			{
				name = "servoyAttributes";
			}
			if (name.equals(IContentSpecConstants.PROPERTY_VISIBLE))
			{
				sb.append(" *ngIf=\"state.model.");
			}
			else
			{
				sb.append(" [");
				sb.append(name);
				sb.append("]=\"state.model.");
			}
			sb.append(name);
			sb.append('"');

			// all properties that handle there own stuff, (that have converters on the server side)
			// should not have the need for an emitter/datachange call. this should be handled in the type itself.
			if (pd.getPushToServer() != null && pd.getPushToServer() != PushToServerEnum.reject &&
				!(pd.getType() instanceof FoundsetPropertyType ||
					pd.getType() instanceof FoundsetLinkedPropertyType ||
					pd.getType() instanceof ValueListPropertyType))
			{
				sb.append(" (");
				sb.append(name);
				sb.append("Change)=\"callback.datachange(state,'");
				sb.append(name);
				if (pd.getType() instanceof DataproviderPropertyType)
				{
					sb.append("',$event, true)\"");
				}
				else
				{
					sb.append("',$event)\"");
				}
			}
		}

		ArrayList<WebObjectFunctionDefinition> handlers = new ArrayList<>(spec.getHandlers().values());
		Collections.sort(handlers, new Comparator<WebObjectFunctionDefinition>()
		{
			@Override
			public int compare(WebObjectFunctionDefinition o1, WebObjectFunctionDefinition o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		for (WebObjectFunctionDefinition handler : handlers)
		{
			sb.append(" [");
			sb.append(handler.getName());
			sb.append("]=\"callback.getHandler(state,'");
			sb.append(handler.getName());
			sb.append("')\"");
		}
		sb.append(" [servoyApi]=\"callback.getServoyApi(state)\"");
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
		sb.append(specName);
		sb.append(">");

		sb.append("</ng-template>\n");
	}

	@SuppressWarnings("nls")
	@Override
	public String generateJS() throws IOException
	{
		IWebFormController cachedFormController = client.getFormManager().getCachedFormController(realFormName);
		ServoyDataConverterContext context = cachedFormController != null ? new ServoyDataConverterContext(cachedFormController)
			: new ServoyDataConverterContext(client);
		FormTemplateObjectWrapper formTemplate = new FormTemplateObjectWrapper(context, true, false);
		FormWrapper formWrapper = formTemplate.getFormWrapper(form);

		// for this form it is really just some json.
		StringWriter stringWriter = new StringWriter();
		final JSONWriter writer = new JSONWriter(stringWriter);
		writer.object();
		writer.key(realFormName);
		writer.object();
		writer.key("responsive");
		writer.value(form.isResponsiveLayout());
		writer.key("size");
		writer.object();
		writer.key("width");
		writer.value(form.getWidth());
		writer.key("height");
		writer.value(form.getSize().getHeight());
		writer.endObject();
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
		Map<String, Object> containerProperties = null;
		if (cachedFormController != null && cachedFormController.getFormUI() instanceof Container)
		{
			Container con = (Container)cachedFormController.getFormUI();
			DataConversion dataConversion = new DataConversion();
			TypedData<Map<String, Object>> typedProperties = con.getProperties();
			con.writeProperties(FullValueToJSONConverter.INSTANCE, null, writer, typedProperties, dataConversion);
			JSONUtils.writeClientConversions(writer, dataConversion);
			containerProperties = typedProperties.content;
		}
		final Map<String, Object> finalContainerProperties = containerProperties;
		if (formWrapper != null)
		{
			Map<String, Object> properties = formWrapper.getProperties();
			properties.forEach((key, value) -> {
				if (finalContainerProperties == null || !finalContainerProperties.containsKey(key))
				{
					writer.key(key);
					if (value instanceof Integer || value instanceof Long)
					{
						writer.value(((Number)value).longValue());
					}
					else if (value instanceof Float || value instanceof Double)
					{
						writer.value(((Number)value).doubleValue());
					}
					else if (value instanceof Boolean)
					{
						writer.value(((Boolean)value).booleanValue());
					}
					else if (value instanceof Dimension)
					{
						writer.object();
						writer.key("width");
						writer.value(((Dimension)value).getWidth());
						writer.key("height");
						writer.value(((Dimension)value).getHeight());
						writer.endObject();
					}
					else
					{
						writer.value(value);
					}
				}
			});
		}
		writer.endObject();
		writer.endObject();
		if (form.isResponsiveLayout())
		{
			form.acceptVisitor(new ChildrenJSONGenerator(writer,
				cachedFormController != null ? new ServoyDataConverterContext(cachedFormController) : new ServoyDataConverterContext(client), form, null,
				null, form, true), PositionComparator.XY_PERSIST_COMPARATOR);
		}
		else
		{
			Iterator<Part> it = form.getParts();
			while (it.hasNext())
			{
				Part part = it.next();
				if (!Part.rendersOnlyInPrint(part.getPartType()))
				{
					writer.object();
					writer.key("part");
					writer.value(true);
					writer.key("classes");
					writer.array();
					writer.value("svy-" + PartWrapper.getName(part));
					if (part.getStyleClass() != null)
					{
						writer.value(part.getStyleClass());
					}
					writer.endArray();
					writer.key("layout");
					writer.object();
					writer.key("position");
					writer.value("absolute");
					writer.key("left");
					writer.value("0px");
					writer.key("right");
					writer.value("0px");
					int top = form.getPartStartYPos(part.getID());
					if (part.getPartType() <= Part.BODY)
					{
						writer.key("top");
						writer.value(top + "px");
					}
					if (part.getPartType() >= Part.BODY)
					{
						writer.key("bottom");
						writer.value(form.getSize().height - part.getHeight() + "px");
					}
					if (part.getPartType() != Part.BODY)
					{
						writer.key("height");
						writer.value((part.getHeight() - top) + "px");
					}
					if (part.getBackground() != null && !form.getTransparent())
					{
						writer.key("background-color");
						writer.value(PersistHelper.createColorString(part.getBackground()));
					}
					if (part.getPartType() == Part.BODY)
					{
						writer.key("overflow-x");
						writer.value(AbstractFormLayoutProvider.getCSSScrolling(form.getScrollbars(), true));
						writer.key("overflow-y");
						writer.value(AbstractFormLayoutProvider.getCSSScrolling(form.getScrollbars(), false));
					}
					else
					{
						writer.key("overflow");
						writer.value("hidden"); //$NON-NLS-1$
					}
					writer.endObject();
					writer.key("children");
					// write the default form value object.
					writer.array();

					form.acceptVisitor(new ChildrenJSONGenerator(writer,
						cachedFormController != null ? new ServoyDataConverterContext(cachedFormController) : new ServoyDataConverterContext(client), form,
						null,
						part, form, true), ChildrenJSONGenerator.FORM_INDEX_WITH_HIERARCHY_COMPARATOR);
					writer.endArray();
					writer.endObject();
				}
			}
		}
		writer.endArray();
		writer.endObject();
		writer.endObject();
		System.err.println(stringWriter.toString());

		return stringWriter.toString();
	}


	/**
	 * @param writer
	 * @param o
	 */
	@SuppressWarnings("nls")
	public static void writePosition(JSONWriter writer, IPersist o, Form form)
	{
		if (o instanceof BaseComponent && ((BaseComponent)o).getCssPosition() != null)
		{
			CSSPosition position = ((BaseComponent)o).getCssPosition();
			writer.key("position");
			writer.object();
			if (CSSPositionUtils.isSet(position.left))
			{
				writer.key("left").value(CSSPositionUtils.getCSSValue(position.left));
			}
			if (CSSPositionUtils.isSet(position.top))
			{
				String top = position.top;
				Point location = CSSPositionUtils.getLocation((BaseComponent)o);
				Part prt = form.getPartAt(location.y);
				if (prt != null)
				{
					int topStart = form.getPartStartYPos(prt.getID());
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
				writer.key("top").value(CSSPositionUtils.getCSSValue(top));
			}
			if (CSSPositionUtils.isSet(position.bottom))
			{
				writer.key("bottom").value(CSSPositionUtils.getCSSValue(position.bottom));
			}
			if (CSSPositionUtils.isSet(position.right))
			{
				writer.key("right").value(CSSPositionUtils.getCSSValue(position.right));
			}
			if (CSSPositionUtils.isSet(position.width))
			{
				if (CSSPositionUtils.isSet(position.left) && CSSPositionUtils.isSet(position.right))
				{
					writer.key("min-width").value(CSSPositionUtils.getCSSValue(position.width));
				}
				else
				{
					writer.key("width").value(CSSPositionUtils.getCSSValue(position.width));
				}
			}
			if (CSSPositionUtils.isSet(position.height))
			{
				if (CSSPositionUtils.isSet(position.top) && CSSPositionUtils.isSet(position.bottom))
				{
					writer.key("min-height").value(CSSPositionUtils.getCSSValue(position.height));
				}
				else
				{
					writer.key("height").value(CSSPositionUtils.getCSSValue(position.height));
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
				int anchorFlags = ((BaseComponent)o).getAnchors();
				boolean anchoredTop = (anchorFlags & IAnchorConstants.NORTH) != 0;
				boolean anchoredRight = (anchorFlags & IAnchorConstants.EAST) != 0;
				boolean anchoredBottom = (anchorFlags & IAnchorConstants.SOUTH) != 0;
				boolean anchoredLeft = (anchorFlags & IAnchorConstants.WEST) != 0;

				if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
				if (!anchoredTop && !anchoredBottom) anchoredTop = true;
				writer.key("position");
				writer.object();

				if (anchoredTop)
				{
					writer.key("top");
					int top = location.y;
					Part prt = form.getPartAt(location.y);
					if (prt != null)
					{
						int topStart = form.getPartStartYPos(prt.getID());
						if (topStart > 0)
						{
							top = top - topStart;
						}
					}
					writer.value(top + "px");
				}
				if (anchoredBottom)
				{
					writer.key("bottom");
					writer.value(form.getSize().height - location.y - size.height + "px");
				}
				if (!anchoredTop || !anchoredBottom)
				{
					writer.key("height");
					writer.value(size.height + "px");
				}
				if (anchoredLeft)
				{
					writer.key("left");
					writer.value(location.x + "px");
				}
				if (anchoredRight)
				{
					writer.key("right");
					writer.value((form.getWidth() - location.x - size.width) + "px");
				}
				if (!anchoredLeft || !anchoredRight)
				{
					writer.key("width");
					writer.value(size.width + "px");
				}
				writer.endObject();
			}
		}
	}
}

