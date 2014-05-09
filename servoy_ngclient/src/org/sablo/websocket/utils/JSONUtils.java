/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sablo.websocket.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyType;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.IDataConverterContext;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * Utility methods for JSON usage.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class JSONUtils
{
	/**
	 * Writes the given object into the JSONWriter. (it is meant to be used for transforming the basic types that can be sent by beans/components)
	 * @param writer the JSONWriter.
	 * @param value the value to be written to the writer.
	 * @return the new writer object to continue writing JSON.
	 * @throws JSONException
	 * @throws IllegalArgumentException if the given object could not be written to JSON for some reason.
	 */
	public static JSONWriter toJSONValue(JSONWriter writer, Object value) throws JSONException, IllegalArgumentException
	{
		return toJSONValue(writer, value, null);
	}

	/**
	 * Writes the given object into the JSONWriter. (it is meant to be used for transforming the basic types that can be sent by beans/components)
	 * @param writer the JSONWriter.
	 * @param value the value to be written to the writer.
	 * @param clientConversion the object where the type (like Date) of the conversion that should happen on the client.
	 * @return the new writer object to continue writing JSON.
	 * @throws JSONException
	 * @throws IllegalArgumentException if the given object could not be written to JSON for some reason.
	 */
	public static JSONWriter toJSONValue(JSONWriter writer, Object value, DataConversion clientConversion) throws JSONException, IllegalArgumentException
	{
		JSONWriter w = writer;
		if (value == null || value == UniqueTag.NOT_FOUND || value == Undefined.instance || value == JSONObject.NULL)
		{
			w = w.value(null); // null is allowed
		}
		else if (value instanceof Integer || value instanceof Long)
		{
			w = w.value(((Number)value).longValue());
		}
		else if (value instanceof Boolean)
		{
			w = w.value(((Boolean)value).booleanValue());
		}
		else if (value instanceof Number)
		{
			w = w.value(((Number)value).doubleValue());
		}
		else if (value instanceof String)
		{
			w = w.value(value);
		}
		else if (value instanceof CharSequence)
		{
			w = w.value(value.toString());
		}
		else if (value instanceof Date || value instanceof NativeDate)
		{
			if (clientConversion != null) clientConversion.convert("Date");
			Date date = (Date)((value instanceof NativeDate) ? ((NativeDate)value).unwrap() : value);
			w = w.value(date.getTime());
		}
		else if (value instanceof Color)
		{
			w = w.value(PersistHelper.createColorString((Color)value));
		}
		else if (value instanceof Point)
		{
			w = w.object();
			w = w.key("x").value(((Point)value).getX());
			w = w.key("y").value(((Point)value).getY());
			w = w.endObject();
		}
		else if (value instanceof Dimension)
		{
			w = w.object();
			w = w.key("width").value(((Dimension)value).getWidth());
			w = w.key("height").value(((Dimension)value).getHeight());
			w = w.endObject();
		}
		else if (value instanceof IValueList)
		{
			w = w.array();
			IValueList list = (IValueList)value;
			for (int i = 0; i < list.getSize(); i++)
			{
				w = w.object();
				Object realElement = list.getRealElementAt(i);
				w = w.key("realValue");
				toJSONValue(writer, realElement, clientConversion);
				w = w.key("displayValue");
				toJSONValue(writer, list.getElementAt(i), clientConversion);
				w = w.endObject();
			}
			w = w.endArray();
		}
		else if (value instanceof LookupListModel)
		{
			w = w.array();
			LookupListModel list = (LookupListModel)value;
			for (int i = 0; i < list.getSize(); i++)
			{
				w = w.object();
				Object realElement = list.getRealElementAt(i);
				w = w.key("realValue");
				toJSONValue(writer, realElement, clientConversion);
				w = w.key("displayValue");
				toJSONValue(writer, list.getElementAt(i), clientConversion);
				w = w.endObject();
			}
			w = w.endArray();
		}
		else if (value instanceof ComponentFormat)
		{
			ComponentFormat format = (ComponentFormat)value;
			w = w.object();
			String type = Column.getDisplayTypeString(format.uiType);
			if (type.equals("INTEGER")) type = "NUMBER";
			w.key("type").value(type);
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
			w.key("isMask").value(isMask);
			w.key("edit").value(mask);
			w.key("placeHolder").value(placeHolder);
			w.key("allowedCharacters").value(format.parsedFormat.getAllowedCharacters());
			w.key("display").value(format.parsedFormat.getDisplayFormat());
			w.endObject();
		}
		else if (value instanceof Border)
		{
			writeBorderToJson((Border)value, w);
		}
		else if (value instanceof Insets)
		{
			Insets i = (Insets)value;
			w.object();
			w.key("paddingTop").value(i.top + "px");
			w.key("paddingBottom").value(i.bottom + "px");
			w.key("paddingLeft").value(i.left + "px");
			w.key("paddingRight").value(i.right + "px");
			w.endObject();
		}
		else if (value instanceof Font)
		{
			Font font = (Font)value;
			w.object();
			if (font.isBold())
			{
				w.key("fontWeight").value("bold");
			}
			if (font.isItalic())
			{
				w.key("italic").value("italic"); //$NON-NLS-1$
			}
			w.key("fontSize").value(font.getSize() + "px");
			w.key("fontFamily").value(font.getFamily() + ", Verdana, Arial");
			w.endObject();
		}
		else if (value instanceof JSONArray) // TODO are we using JSON object or Map and Lists? ( as internal representation of properties)
		{
			w = w.value(value);
		}
		else if (value instanceof JSONObject)
		{
			w = w.value(value);
		}
		else if (value instanceof List)
		{
			List< ? > lst = (List< ? >)value;
			w.array();
			for (int i = 0; i < lst.size(); i++)
			{
				if (clientConversion != null) clientConversion.pushNode(String.valueOf(i));
				toJSONValue(w, lst.get(i), clientConversion);
				if (clientConversion != null) clientConversion.popNode();
			}
			w.endArray();
		}
		else if (value instanceof Object[])
		{
			Object[] array = (Object[])value;
			w.array();
			for (int i = 0; i < array.length; i++)
			{
				if (clientConversion != null) clientConversion.pushNode(String.valueOf(i));
				toJSONValue(w, array[i], clientConversion);
				if (clientConversion != null) clientConversion.popNode();
			}
			w.endArray();
		}
		else if (value instanceof Map)
		{
			w = w.object();
			Map<String, ? > map = (Map<String, ? >)value;
			for (Entry<String, ? > entry : map.entrySet())
			{
				if (clientConversion != null) clientConversion.pushNode(entry.getKey());
				//TODO remove the need for this when going to full tree recursion for sendChanges()
				String[] keys = entry.getKey().split("\\.");
				if (keys.length > 1)
				{
					//LIMITATION of JSONWriter because it can't add a property to an already written object
					// currently for 2 properties like complexmodel.firstNameDataprovider
					//								   size
					//								   complexmodel.lastNameDataprovider
					// it creates 2 json entries with the same key ('complexmodel') and on the client side it only takes one of them
					w.key(keys[0]);
					w.object();
					w.key(keys[1]);
					toJSONValue(w, entry.getValue(), clientConversion);
					w.endObject();
				}// END TODO REMOVE
				else
				{
					w.key(entry.getKey());
					toJSONValue(w, entry.getValue(), clientConversion);
				}
				if (clientConversion != null) clientConversion.popNode();
			}
			w = w.endObject();
		}
		else if (value instanceof Form)
		{
			w = w.value(toStringObject(value, PropertyType.form));
		}
		else if (value instanceof FormScope)
		{
			w = w.value(((FormScope)value).getFormController().getName());
		}
		else if (value instanceof RelatedFoundSet)
		{
			w = w.value(((RelatedFoundSet)value).getRelationName());
		}
		else if (value instanceof JSONWritable)
		{
			toJSONValue(w, ((JSONWritable)value).toMap(), clientConversion);
		}
		else if (value instanceof byte[])
		{
			MediaResourcesServlet.MediaInfo mediaInfo = MediaResourcesServlet.getMediaInfo((byte[])value);
			w.object();
			w.key("url").value("resources/" + MediaResourcesServlet.DYNAMIC_DATA_ACCESS + mediaInfo.getName());
			w.key("contentType").value(mediaInfo.getContentType());
			w.endObject();
		}
		else
		{
			throw new IllegalArgumentException("unsupported value type for value: " + value);
		}
		return w;
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
				{
					if (propertyValue instanceof Integer)
					{
						// special support for media type (that needs a FS to resolve the media)
						int mediaId = (Integer)propertyValue;//((JSONObject)json).getInt(key);
						Media media = converterContext.getSolution().getMedia(mediaId);
						if (media != null)
						{
							return "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" +
								media.getName();
						}
					}
				}
					break;
				case formscope :
				{
					INGApplication app = converterContext.getApplication();
					if (propertyValue instanceof String && app != null)
					{
						return app.getFormManager().getForm((String)propertyValue).getFormScope();
					}
					break;
				}
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

	/**
	 * Validates a String to be valid JSON content and normalizes it.
	 * @param json the json content to check.
	 * @return the given JSON normalized.
	 * @throws JSONException if the given JSON is not valid
	 */
	public static String validateAndTrimJSON(String json) throws JSONException
	{
		if (json == null) return null;

		return new JSONObject(json).toString(); // just to validate - can we do this nicer with available lib (we might not need the "normalize" part)?
	}

	/**
	 * Adds all properties of the given object as key-value pairs in the writer.
	 * @param propertyWriter the writer.
	 * @param objectToMerge the object contents to be merged into the writer prepared object.
	 * @throws JSONException if the writer is not prepared (to write object contents) or other json exception occurs.
	 */
	public static void addObjectPropertiesToWriter(JSONWriter propertyWriter, JSONObject objectToMerge) throws JSONException
	{
		Iterator< ? > it = objectToMerge.keys();
		while (it.hasNext())
		{
			String key = (String)it.next();
			propertyWriter.key(key).value(objectToMerge.get(key));
		}
	}

	public static JSONWriter addObjectPropertiesToWriter(JSONWriter jsonWriter, Map<String, Object> properties) throws JSONException, IllegalArgumentException
	{
		for (Entry<String, Object> entry : properties.entrySet())
		{
			toJSONValue(jsonWriter.key(entry.getKey()), entry.getValue());
		}
		return jsonWriter;
	}

	private static void writeBorderToJson(Border value, JSONWriter w) throws JSONException
	{

		if (value instanceof SpecialMatteBorder)
		{
			SpecialMatteBorder border = (SpecialMatteBorder)value;
			w.object();
			w.key("type").value(((border instanceof RoundedBorder) ? ComponentFactoryHelper.ROUNDED_BORDER : ComponentFactoryHelper.SPECIAL_MATTE_BORDER));
			w.key("borderStyle");
			w.object();
			w.key("borderTop").value(border.getTop() + "px");
			w.key("borderRight").value(border.getRight() + "px");
			w.key("borderBottom").value(border.getBottom() + "px");
			w.key("borderLeft").value(border.getLeft() + "px");
			w.key("borderTopColor").value(PersistHelper.createColorString(border.getTopColor()));
			w.key("borderRightColor").value(PersistHelper.createColorString(border.getRightColor()));
			w.key("borderBottomColor").value(PersistHelper.createColorString(border.getBottomColor()));
			w.key("borderLeftColor").value(PersistHelper.createColorString(border.getLeftColor()));

			if (border instanceof RoundedBorder)
			{
				float[] radius = ((RoundedBorder)border).getRadius();
				w.key("borderRadius").value(
					radius[0] + "px " + radius[2] + "px " + radius[4] + "px " + radius[6] + "px /" + radius[1] + "px " + radius[3] + "px " + radius[5] + "px " +
						radius[7] + "px");
				String styles[] = ((RoundedBorder)border).getBorderStyles();
				w.key("borderStyle").value(styles[0] + " " + styles[1] + " " + styles[2] + " " + styles[3] + " ");
			}
			else
			{
				w.key("borderRadius").value(border.getRoundingRadius() + "px"); //$NON-NLS-1$
				//retval += "," + SpecialMatteBorder.createDashString(border.getDashPattern()); //$NON-NLS-1$
				if (border.getDashPattern() != null)
				{
					w.key("borderStyle").value("dashed");
				}
				else
				{
					w.key("borderStyle").value("solid");
				}
			}
			w.endObject();// end borderStyle
			w.endObject();// end borderType
		}
		else if (value instanceof EtchedBorder)
		{
			EtchedBorder border = (EtchedBorder)value;
			w.object();
			w.key("type").value(ComponentFactoryHelper.ETCHED_BORDER);
			w.key("borderStyle");
			w.object();
			String hi = PersistHelper.createColorString(border.getHighlightColor());
			String sh = PersistHelper.createColorString(border.getShadowColor());
			if (border.getEtchType() != EtchedBorder.RAISED)
			{
				String tmp = hi;
				hi = sh;
				sh = tmp;
			}
			w.key("borderColor").value(hi + " " + sh + " " + sh + " " + hi);
			String etchedType = border.getEtchType() == EtchedBorder.RAISED ? "ridge" : "groove";
			w.key("borderStyle").value(etchedType);
			w.key("borderWidth").value("2px");
			w.endObject();// end borderStyle
			w.endObject();// end borderType
		}
		else if (value instanceof BevelBorder)
		{
			BevelBorder border = (BevelBorder)value;
			w.object();
			w.key("type").value(ComponentFactoryHelper.BEVEL_BORDER);
			w.key("borderStyle");
			w.object();
			String bevelType = border.getBevelType() == BevelBorder.RAISED ? "outset" : "inset";
			w.key("borderStyle").value(bevelType);

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
			w.key("borderColor").value(hiOut + " " + shOut + " " + shIn + " " + hiin);
			w.key("borderWidth").value("2px");
			w.endObject();// end borderStyle
			w.endObject();// end borderType
		}
		else if (value instanceof LineBorder)
		{
			LineBorder border = (LineBorder)value;
			w.object();
			w.key("type").value(ComponentFactoryHelper.LINE_BORDER);
			w.key("borderStyle");
			w.object();
			int thick = border.getThickness();
			String lineColor = PersistHelper.createColorString(border.getLineColor());
			w.key("borderColor").value(lineColor);
			w.key("borderStyle").value("solid");
			w.key("borderWidth").value(thick + "px");
			w.endObject();// end borderStyle
			w.endObject();// end borderType
		}
		else if (value instanceof MatteBorder)
		{
			MatteBorder border = (MatteBorder)value;
			w.object();
			w.key("type").value(ComponentFactoryHelper.MATTE_BORDER);
			w.key("borderStyle");
			w.object();
			Insets in = border.getBorderInsets();
			w.key("borderWidth").value(in.top + "px " + in.right + "px " + in.bottom + "px " + in.left + "px ");
			String lineColor = PersistHelper.createColorString(border.getMatteColor());
			w.key("borderColor").value(lineColor);
			w.key("borderStyle").value("solid");
			w.endObject();// end borderStyle
			w.endObject();// end borderType
		}
		else if (value instanceof EmptyBorder)
		{
			EmptyBorder border = (EmptyBorder)value;
			w.object();
			w.key("type").value(ComponentFactoryHelper.EMPTY_BORDER);
			w.key("borderStyle");
			w.object();
			Insets in = border.getBorderInsets();
			w.key("borderWidth").value(in.top + "px " + in.right + "px " + in.bottom + "px " + in.left + "px ");
			w.key("borderColor").value("rgba(0,0,0,0)");
			w.endObject();// end borderStyle
			w.endObject();// end borderType
		}
		else if (value instanceof TitledBorder)
		{
			TitledBorder border = (TitledBorder)value;
			w.object();
			w.key("type").value(ComponentFactoryHelper.TITLED_BORDER);
			w.key("title").value(border.getTitle());

			w.key("font").value(PersistHelper.createFontCssString(border.getTitleFont()));
			w.key("color").value(PersistHelper.createColorString(border.getTitleColor()));
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
			w.key("titleJustiffication").value(titleJust);
			//w.endObject();// end borderStyle
			w.endObject();// end borderType

//			retval = TITLED_BORDER + "," + s; //$NON-NLS-1$
//
//			int justification = border.getTitleJustification();
//			int position = border.getTitlePosition();
//			if (justification != 0 || position != 0 || f != null || c != null)
//			{
//				retval += "," + justification + "," + position; //$NON-NLS-1$ //$NON-NLS-2$
//				if (f != null)
//				{
//					retval += "," + PersistHelper.createFontString(f); //$NON-NLS-1$
//					if (c != null)
//					{
//						retval += "," + PersistHelper.createColorString(c); //$NON-NLS-1$
//					}
//				}
//			}
		}
	}

	public static interface JSONWritable
	{
		Map<String, Object> toMap();
	}
}
