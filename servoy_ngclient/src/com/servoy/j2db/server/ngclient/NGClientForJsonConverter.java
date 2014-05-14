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

package com.servoy.j2db.server.ngclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyType;
import org.sablo.websocket.IForJsonConverter;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class NGClientForJsonConverter implements IForJsonConverter
{
	public static NGClientForJsonConverter INSTANCE = new NGClientForJsonConverter();

	@Override
	public Object convertForJson(Object value)
	{
		// convert simple values to json values
		if (value == UniqueTag.NOT_FOUND || value == Undefined.instance)
		{
			return null;
		}

		if (value instanceof NativeDate)
		{
			return ((NativeDate)value).unwrap();
		}

		if (value instanceof Color)
		{
			return PersistHelper.createColorString((Color)value);
		}

		if (value instanceof Form)
		{
			return ((Form)value).getName();
		}

		if (value instanceof FormScope)
		{
			return ((FormScope)value).getFormController().getName();
		}

		if (value instanceof RelatedFoundSet)
		{
			return ((RelatedFoundSet)value).getRelationName();
		}

		// complex values: use map or list
		if (value instanceof IValueList)
		{
			IValueList list = (IValueList)value;
			List<Map<String, Object>> array = new ArrayList<>(list.getSize());
			Map<String, Object> map = new HashMap<String, Object>();
			for (int i = 0; i < list.getSize(); i++)
			{
				array.add(map = new HashMap<>());
				map.put("realValue", list.getRealElementAt(i));
				map.put("displayValue", list.getElementAt(i));
			}
			return array;
		}

		if (value instanceof LookupListModel)
		{
			LookupListModel list = (LookupListModel)value;
			List<Map<String, Object>> array = new ArrayList<>(list.getSize());
			Map<String, Object> map;
			for (int i = 0; i < list.getSize(); i++)
			{
				array.add(map = new HashMap<>());
				map.put("realValue", list.getRealElementAt(i));
				map.put("displayValue", list.getElementAt(i));
			}
			return list;
		}

		if (value instanceof ComponentFormat)
		{
			ComponentFormat format = (ComponentFormat)value;
			Map<String, Object> map = new HashMap<>();
			String type = Column.getDisplayTypeString(format.uiType);
			if (type.equals("INTEGER")) type = "NUMBER";
			map.put("type", type);

			boolean isMask = format.parsedFormat.isMask();
			String mask = format.parsedFormat.getEditFormat();
			if (isMask && type.equals("DATETIME"))
			{
				mask = format.parsedFormat.getDateMask();
			}
			else if (format.parsedFormat.getDisplayFormat() != null && type.equals("TEXT"))
			{
				isMask = true;
				mask = format.parsedFormat.getDisplayFormat();
			}
			String placeHolder = null;
			if (format.parsedFormat.getPlaceHolderString() != null) placeHolder = format.parsedFormat.getPlaceHolderString();
			else if (format.parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(format.parsedFormat.getPlaceHolderCharacter());
			map.put("isMask", Boolean.valueOf(isMask));
			map.put("edit", mask);
			map.put("placeHolder", placeHolder);
			map.put("allowedCharacters", format.parsedFormat.getAllowedCharacters());
			map.put("display", format.parsedFormat.getDisplayFormat());

			return map;
		}

		if (value instanceof Border)
		{
			return writeBorderToJson((Border)value);
		}

		if (value instanceof byte[])
		{
			Map<String, Object> map = new HashMap<>();
			MediaResourcesServlet.MediaInfo mediaInfo = MediaResourcesServlet.getMediaInfo((byte[])value);
			map.put("url", "resources/" + MediaResourcesServlet.DYNAMIC_DATA_ACCESS + mediaInfo.getName());
			map.put("contentType", mediaInfo.getContentType());
			return map;
		}

		// default conversion
		return value;
	}

	private Map<String, Object> writeBorderToJson(Border value)
	{
		Map<String, Object> map = new HashMap<>();
		if (value instanceof SpecialMatteBorder)
		{
			SpecialMatteBorder border = (SpecialMatteBorder)value;
			map.put("type", ((border instanceof RoundedBorder) ? ComponentFactoryHelper.ROUNDED_BORDER : ComponentFactoryHelper.SPECIAL_MATTE_BORDER));
			Map<String, Object> borderStyle = new HashMap<>();
			map.put("borderStyle", borderStyle);

			borderStyle.put("borderTop", border.getTop() + "px");
			borderStyle.put("borderRight", border.getRight() + "px");
			borderStyle.put("borderBottom", border.getBottom() + "px");
			borderStyle.put("borderLeft", border.getLeft() + "px");
			borderStyle.put("borderTopColor", border.getTopColor());
			borderStyle.put("borderRightColor", border.getRightColor());
			borderStyle.put("borderBottomColor", border.getBottomColor());
			borderStyle.put("borderLeftColor", border.getLeftColor());

			if (border instanceof RoundedBorder)
			{
				float[] radius = ((RoundedBorder)border).getRadius();
				borderStyle.put("borderRadius", radius[0] + "px " + radius[2] + "px " + radius[4] + "px " + radius[6] + "px /" + radius[1] + "px " + radius[3] +
					"px " + radius[5] + "px " + radius[7] + "px");
				String styles[] = ((RoundedBorder)border).getBorderStyles();
				borderStyle.put("borderStyle", styles[0] + " " + styles[1] + " " + styles[2] + " " + styles[3] + " ");
			}
			else
			{
				borderStyle.put("borderRadius", border.getRoundingRadius() + "px"); //$NON-NLS-1$
				//retval += "," + SpecialMatteBorder.createDashString(border.getDashPattern()); //$NON-NLS-1$
				if (border.getDashPattern() != null)
				{
					borderStyle.put("borderStyle", "dashed");
				}
				else
				{
					borderStyle.put("borderStyle", "solid");
				}
			}
		}
		else if (value instanceof EtchedBorder)
		{
			EtchedBorder border = (EtchedBorder)value;
			map.put("type", ComponentFactoryHelper.ETCHED_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put("borderStyle", borderStyle);
			String hi = PersistHelper.createColorString(border.getHighlightColor());
			String sh = PersistHelper.createColorString(border.getShadowColor());
			if (border.getEtchType() != EtchedBorder.RAISED)
			{
				String tmp = hi;
				hi = sh;
				sh = tmp;
			}
			borderStyle.put("borderColor", hi + " " + sh + " " + sh + " " + hi);
			borderStyle.put("borderStyle", border.getEtchType() == EtchedBorder.RAISED ? "ridge" : "groove");
			borderStyle.put("borderWidth", "2px");
		}
		else if (value instanceof BevelBorder)
		{
			BevelBorder border = (BevelBorder)value;
			map.put("type", ComponentFactoryHelper.BEVEL_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put("borderStyle", borderStyle);
			borderStyle.put("borderStyle", border.getBevelType() == BevelBorder.RAISED ? "outset" : "inset");

			String hiOut = PersistHelper.createColorString(border.getHighlightOuterColor());
			String hiin = PersistHelper.createColorString(border.getHighlightInnerColor());
			String shOut = PersistHelper.createColorString(border.getShadowOuterColor());
			String shIn = PersistHelper.createColorString(border.getShadowInnerColor());
			if (border.getBevelType() == BevelBorder.LOWERED)
			{
				String temp = hiOut; // swap 1-3
				hiOut = shOut;
				shOut = temp;
				temp = hiin; // swap 2-4
				hiin = shIn;
				shIn = temp;
			}
			borderStyle.put("borderColor", hiOut + " " + shOut + " " + shIn + " " + hiin);
			borderStyle.put("borderWidth", "2px");
		}
		else if (value instanceof LineBorder)
		{
			LineBorder border = (LineBorder)value;
			map.put("type", ComponentFactoryHelper.LINE_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put("borderStyle", borderStyle);
			int thick = border.getThickness();
			borderStyle.put("borderColor", border.getLineColor());
			borderStyle.put("borderStyle", "solid");
			borderStyle.put("borderWidth", thick + "px");
		}
		else if (value instanceof MatteBorder)
		{
			MatteBorder border = (MatteBorder)value;
			map.put("type", ComponentFactoryHelper.MATTE_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put("borderStyle", borderStyle);
			Insets in = border.getBorderInsets();
			borderStyle.put("borderWidth", in.top + "px " + in.right + "px " + in.bottom + "px " + in.left + "px ");
			borderStyle.put("borderColor", border.getMatteColor());
			borderStyle.put("borderStyle", "solid");
		}
		else if (value instanceof EmptyBorder)
		{
			EmptyBorder border = (EmptyBorder)value;
			map.put("type", ComponentFactoryHelper.EMPTY_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put("borderStyle", borderStyle);
			Insets in = border.getBorderInsets();
			borderStyle.put("borderWidth", in.top + "px " + in.right + "px " + in.bottom + "px " + in.left + "px ");
			borderStyle.put("borderColor", "rgba(0,0,0,0)");
		}
		else if (value instanceof TitledBorder)
		{
			TitledBorder border = (TitledBorder)value;
			map.put("type", ComponentFactoryHelper.TITLED_BORDER);
			map.put("title", border.getTitle());

			map.put("font", border.getTitleFont());
			map.put("color", border.getTitleColor());
			int just = border.getTitleJustification();
			String titleJust = "left";
			switch (just)
			{
				case TitledBorder.LEFT :
					titleJust = "left";
					break;
				case TitledBorder.RIGHT :
					titleJust = "right";
					break;
				case TitledBorder.CENTER :
					titleJust = "center";
					break;
			}
			map.put("titleJustiffication", titleJust);
		}
		return map;
	}

	/**
	 * Converts a JSON value to a Java value based on bean spec type.
	 * @param propertyValue can be a JSONObject or array or primitive. (so something deserialized from a JSON string)
	 * @return the corresponding Java object based on bean spec.
	 */
	public static Object toJavaObject(Object json, PropertyDescription componentSpecType, IDataConverterContext converterContext) throws JSONException
	{
		Object propertyValue = json;
		if (propertyValue != null && componentSpecType != null)
		{
			switch (componentSpecType.getType())
			{
				case dimension :
					if (propertyValue instanceof Object[])
					{
						return new Dimension(Utils.getAsInteger(((Object[])propertyValue)[0]), Utils.getAsInteger(((Object[])propertyValue)[1]));
					}
					if (propertyValue instanceof JSONObject)
					{
						return new Dimension(((JSONObject)propertyValue).getInt("width"), ((JSONObject)propertyValue).getInt("height"));
					}
					if (propertyValue instanceof NativeObject)
					{
						NativeObject value = (NativeObject)propertyValue;
						return new Dimension(Utils.getAsInteger(value.get("width", value)), Utils.getAsInteger(value.get("height", value)));
					}
					break;

				case point :
					if (propertyValue instanceof Object[])
					{
						return new Point(Utils.getAsInteger(((Object[])propertyValue)[0]), Utils.getAsInteger(((Object[])propertyValue)[1]));
					}
					if (propertyValue instanceof JSONObject)
					{
						return new Point(((JSONObject)propertyValue).getInt("x"), ((JSONObject)propertyValue).getInt("y"));
					}
					if (propertyValue instanceof NativeObject)
					{
						NativeObject value = (NativeObject)propertyValue;
						return new Point(Utils.getAsInteger(value.get("x", value)), Utils.getAsInteger(value.get("y", value)));
					}
					break;

				case color :
					if (propertyValue instanceof String)
					{
						return PersistHelper.createColor(propertyValue.toString());
					}
					break;

				case format :
					if (propertyValue instanceof String)
					{
						//todo recreate ComponentFormat object (it has quite a lot of dependencies , application,pesist  etc)
						return propertyValue;
					}
					break;

				case border :
					if (propertyValue instanceof String)
					{
						return ComponentFactoryHelper.createBorder((String)propertyValue);
					}
					break;

				case media :
					if (propertyValue instanceof Integer)
					{
						// special support for media type (that needs a FS to resolve the media)
						Media media = converterContext.getSolution().getMedia(((Integer)propertyValue).intValue());
						if (media != null)
						{
							return "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" +
								media.getName();
						}
					}
					break;

				case formscope :
					INGApplication app = converterContext.getApplication();
					if (propertyValue instanceof String && app != null)
					{
						return app.getFormManager().getForm((String)propertyValue).getFormScope();
					}
					break;

				case custom :
				{
					if (json instanceof JSONObject)
					{
						JSONObject jsonObject = (JSONObject)json;
						PropertyDescription typeSpec = (PropertyDescription)componentSpecType.getConfig();
						Map<String, Object> ret = new HashMap<String, Object>();
						for (Entry<String, PropertyDescription> entry : typeSpec.getProperties().entrySet())
						{
							String key = entry.getKey();
							if (jsonObject.has(key)) // ((JSONObject)json).get(key) can be null in the case of partial update
							{
								ret.put(key, toJavaObject(jsonObject.get(key), entry.getValue(), converterContext));
							}
						}
						return ret;
					}
					break;
				}

				default :
			}
		}

		return propertyValue;
	}

	public static Object toStringObject(Object propertyValue, PropertyType propertyType)
	{
		if (propertyValue != null && propertyType != null)
		{
			switch (propertyType)
			{
				case dimension :
					if (propertyValue instanceof Dimension)
					{
						return PersistHelper.createDimensionString((Dimension)propertyValue);
					}
					break;

				case point :
					if (propertyValue instanceof Point)
					{
						return PersistHelper.createPointString((Point)propertyValue);
					}
					break;

				case color :
					if (propertyValue instanceof Color)
					{
						return PersistHelper.createColorString((Color)propertyValue);
					}
					break;

				case form :
					if (propertyValue instanceof Form)
					{
						return ((Form)propertyValue).getName();
					}
					break;

				default :
			}
		}

		return propertyValue;
	}
}