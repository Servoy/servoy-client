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
import java.util.Iterator;
import java.util.Map;

import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.base.persistence.constants.IContentSpecConstantsBase;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.server.headlessclient.dataui.AbstractFormLayoutProvider;
import com.servoy.j2db.server.ngclient.INGClientWindow.IFormHTMLAndJSGenerator;
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
	private final FlattenedSolution flattenedSolution;
	private final boolean isDesigner;
	private final LayoutContainer zoomedInContainer;

	/**
	 * @param client
	 * @param form
	 * @param realFormName
	 */
	public AngularFormGenerator(NGClient client, Form form, String realFormName, boolean isDesigner)
	{
		this.client = client;
		this.form = form;
		this.realFormName = realFormName;
		this.flattenedSolution = null;
		this.isDesigner = isDesigner;
		this.zoomedInContainer = null;
	}

	public AngularFormGenerator(FlattenedSolution fs, Form form, String realFormName, boolean isDesigner, LayoutContainer zoomedInContainer)
	{
		this.flattenedSolution = fs;
		this.form = form;
		this.realFormName = realFormName;
		this.client = null;
		this.isDesigner = isDesigner;
		this.zoomedInContainer = zoomedInContainer;
	}

	@SuppressWarnings("nls")
	@Override
	public String generateHTMLTemplate()
	{
		return "";
	}

	@SuppressWarnings("nls")
	@Override
	public String generateJS() throws IOException
	{
		IWebFormController cachedFormController = client != null ? client.getFormManager().getCachedFormController(realFormName) : null;

		FormTemplateObjectWrapper formTemplate = new FormTemplateObjectWrapper(getAContext(), true, false);
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
			if (zoomedInContainer != null)
			{
				zoomedInContainer.acceptVisitor(new ChildrenJSONGenerator(writer,
					getAContext(), form, null,
					null, form, true, isDesigner), PositionComparator.XY_PERSIST_COMPARATOR);
			}
			else
			{
				form.acceptVisitor(new ChildrenJSONGenerator(writer,
					getAContext(), form, null,
					null, form, true, isDesigner), PositionComparator.XY_PERSIST_COMPARATOR);
			}

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
						getAContext(), form,
						null,
						part, form, true, isDesigner), ChildrenJSONGenerator.FORM_INDEX_WITH_HIERARCHY_COMPARATOR);
					writer.endArray();
					writer.endObject();
				}
			}
		}
		writer.endArray();
		writer.endObject();
		writer.endObject();
		return stringWriter.toString();
	}


	/**
	 * @param writer
	 * @param o
	 */
	@SuppressWarnings("nls")
	public static void writePosition(JSONWriter writer, IPersist o, Form form, WebFormComponent webComponent, boolean isDesigner)
	{
		if (o instanceof BaseComponent && ((BaseComponent)o).getCssPosition() != null)
		{
			CSSPosition position = ((BaseComponent)o).getCssPosition();
			if (webComponent != null)
			{
				Object runtimeValue = webComponent.getProperty(IContentSpecConstants.PROPERTY_CSS_POSITION);
				if (runtimeValue instanceof CSSPosition)
				{
					position = (CSSPosition)runtimeValue;
				}
			}
			writer.key("position");
			writer.object();
			if (CSSPositionUtils.isSet(position.left))
			{
				writer.key("left").value(CSSPositionUtils.getCSSValue(position.left));
			}
			if (CSSPositionUtils.isSet(position.top))
			{
				String top = position.top;
				if (!isDesigner)
				{
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
			if (webComponent != null)
			{
				Object runtimeValue = webComponent.getProperty(IContentSpecConstantsBase.PROPERTY_LOCATION);
				if (runtimeValue instanceof Point)
				{
					location = (Point)runtimeValue;
				}
				runtimeValue = webComponent.getProperty(IContentSpecConstantsBase.PROPERTY_SIZE);
				if (runtimeValue instanceof Dimension)
				{
					size = (Dimension)runtimeValue;
				}
			}
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
					writer.value(location.y + "px");
				}
				if (anchoredBottom)
				{
					writer.key("bottom");
					int partHeight = form.getSize().height;
					if (!isDesigner)
					{
						// search for element's part using its design time location
						Part prt = form.getPartAt(((IFormElement)o).getLocation().y);
						if (prt != null)
						{
							int prtEnd = form.getPartEndYPos(prt.getID());
							if (prtEnd > form.getSize().height) prtEnd = form.getSize().height;
							partHeight = prtEnd - form.getPartStartYPos(prt.getID());
						}
					}
					writer.value(partHeight - location.y - size.height + "px");
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
				if (anchoredTop && anchoredBottom)
				{
					writer.key("min-height");
					writer.value(size.height + "px");
				}
				if (anchoredLeft && anchoredRight)
				{
					writer.key("min-width");
					writer.value(size.width + "px");
				}

				writer.endObject();
			}
		}
	}

	private ServoyDataConverterContext getAContext()
	{
		if (client != null)
		{
			IWebFormController cachedFormController = client.getFormManager().getCachedFormController(realFormName);
			return cachedFormController != null ? new ServoyDataConverterContext(cachedFormController)
				: new ServoyDataConverterContext(client);
		}
		return new ServoyDataConverterContext(flattenedSolution);
	}


	@Override
	public boolean waitForBackgroundFormLoad()
	{
		return false;
	}
}

