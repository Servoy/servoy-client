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
package com.servoy.j2db.server.ngclient.property.types;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.FontPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jcompagner
 *
 */
public class BorderPropertyType extends DefaultPropertyType<Border>
	implements IConvertedPropertyType<Border>, IDesignToFormElement<JSONObject, Border, Border>, IFormElementToTemplateJSON<Border, Border>,
	IRhinoToSabloComponent<Border>, ISabloComponentToRhino<Border>, IRhinoDesignConverter, IDesignValueConverter<Object>
{
	private static final String TYPE = "type";
	private static final String BORDER_RADIUS = "borderRadius";
	private static final String TITLE_POSITION = "titlePosition";
	private static final String TITLE_JUSTIFICATION = "titleJustification";
	private static final String TITLE = "title";
	private static final String BORDER_RIGHT_COLOR = "borderRightColor";
	private static final String BORDER_LEFT_COLOR = "borderLeftColor";
	private static final String BORDER_BOTTOM_COLOR = "borderBottomColor";
	private static final String BORDER_TOP_COLOR = "borderTopColor";
	private static final String BORDER_RIGHT_WIDTH = "borderRightWidth";
	private static final String BORDER_BOTTOM_WIDTH = "borderBottomWidth";
	private static final String BORDER_LEFT_WIDTH = "borderLeftWidth";
	private static final String BORDER_TOP_WIDTH = "borderTopWidth";
	private static final String BORDER_COLOR = "borderColor";
	private static final String BORDER_WIDTH = "borderWidth";
	private static final String BORDER_STYLE = "borderStyle";

	public static final BorderPropertyType INSTANCE = new BorderPropertyType();
	public static final String TYPE_NAME = "border";

	private BorderPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Border fromJSON(Object newValue, Border previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue == null) return null;
		JSONObject object = (JSONObject)newValue;
		String type = object.optString(TYPE);
		if (type == null) return null;
		JSONObject borderStyle = object.optJSONObject(BORDER_STYLE);
		switch (type)
		{
			case ComponentFactoryHelper.BEVEL_BORDER :
			{
				int borderType = "outset".equals(borderStyle.optString(BORDER_STYLE)) ? BevelBorder.RAISED : BevelBorder.LOWERED;
				String borderColor = borderStyle.optString(BORDER_COLOR);
				StringTokenizer st = new StringTokenizer(borderColor, " ");
				Color hiOut = null;
				Color hiin = null;
				Color shOut = null;
				Color shIn = null;
				if (st.hasMoreTokens()) hiOut = PersistHelper.createColor(st.nextToken());
				if (st.hasMoreTokens()) shOut = PersistHelper.createColor(st.nextToken());
				if (st.hasMoreTokens()) shIn = PersistHelper.createColor(st.nextToken());
				if (st.hasMoreTokens()) hiin = PersistHelper.createColor(st.nextToken());
				return BorderFactory.createBevelBorder(borderType, hiOut, hiin, shOut, shIn);
			}
			case ComponentFactoryHelper.EMPTY_BORDER :
			{
				Insets insets = PersistHelper.createInsets(borderStyle.optString(BORDER_WIDTH).replaceAll("px ", ","));
				return BorderFactory.createEmptyBorder(insets.top, insets.right, insets.bottom, insets.left);
			}
			case ComponentFactoryHelper.ETCHED_BORDER :
			{

				StringTokenizer st = new StringTokenizer(borderStyle.optString(BORDER_COLOR), " ");
				Color hi = null;
				Color sh = null;
				if (st.hasMoreTokens()) hi = PersistHelper.createColor(st.nextToken());
				if (st.hasMoreTokens()) sh = PersistHelper.createColor(st.nextToken());
				int t = borderStyle.optString(BORDER_STYLE).equals("ridge") ? EtchedBorder.RAISED : EtchedBorder.LOWERED;
				return BorderFactory.createEtchedBorder(t, hi, sh);
			}
			case ComponentFactoryHelper.LINE_BORDER :
			{
				Color borderColor = PersistHelper.createColor(borderStyle.optString(BORDER_COLOR));
				String width = borderStyle.optString(BORDER_WIDTH);
				if (width != null) width = width.substring(0, width.length() - 2);
				return BorderFactory.createLineBorder(borderColor, Utils.getAsInteger(width));
			}
			case ComponentFactoryHelper.MATTE_BORDER :
			{
				Insets insets = PersistHelper.createInsets(borderStyle.optString(BORDER_WIDTH).replaceAll("px ", ","));
				Color borderColor = PersistHelper.createColor(borderStyle.optString(BORDER_COLOR));
				return BorderFactory.createMatteBorder(insets.top, insets.right, insets.bottom, insets.left, borderColor);
			}
			case ComponentFactoryHelper.ROUNDED_BORDER :
			{
				float top = Utils.getAsFloat(borderStyle.optString(BORDER_TOP_WIDTH).replace("px", ""));
				float left = Utils.getAsFloat(borderStyle.optString(BORDER_LEFT_WIDTH).replace("px", ""));
				float bottom = Utils.getAsFloat(borderStyle.optString(BORDER_BOTTOM_WIDTH).replace("px", ""));
				float right = Utils.getAsFloat(borderStyle.optString(BORDER_RIGHT_WIDTH).replace("px", ""));
				Color topColor = PersistHelper.createColor(borderStyle.optString(BORDER_TOP_COLOR));
				Color bottomColor = PersistHelper.createColor(borderStyle.optString(BORDER_BOTTOM_COLOR));
				Color leftColor = PersistHelper.createColor(borderStyle.optString(BORDER_LEFT_COLOR));
				Color rightColor = PersistHelper.createColor(borderStyle.optString(BORDER_RIGHT_COLOR));
				RoundedBorder border = new RoundedBorder(top, left, bottom, right, topColor, leftColor, bottomColor, rightColor);
				border.setBorderStyles(borderStyle.optString(BORDER_STYLE).replaceAll(" ", ";"));
				border.setRoundingRadius(borderStyle.optString(BORDER_RADIUS).replaceAll("px", ";").replaceAll(" ", "").replaceAll("/", ""));
				return border;
			}
			case ComponentFactoryHelper.SPECIAL_MATTE_BORDER :
			{
				float top = Utils.getAsFloat(borderStyle.optString(BORDER_TOP_WIDTH).replace("px", ""));
				float left = Utils.getAsFloat(borderStyle.optString(BORDER_LEFT_WIDTH).replace("px", ""));
				float bottom = Utils.getAsFloat(borderStyle.optString(BORDER_BOTTOM_WIDTH).replace("px", ""));
				float right = Utils.getAsFloat(borderStyle.optString(BORDER_RIGHT_WIDTH).replace("px", ""));
				Color topColor = PersistHelper.createColor(borderStyle.optString(BORDER_TOP_COLOR));
				Color bottomColor = PersistHelper.createColor(borderStyle.optString(BORDER_BOTTOM_COLOR));
				Color leftColor = PersistHelper.createColor(borderStyle.optString(BORDER_LEFT_COLOR));
				Color rightColor = PersistHelper.createColor(borderStyle.optString(BORDER_RIGHT_COLOR));
				return new SpecialMatteBorder(top, left, bottom, right, topColor, leftColor, bottomColor, rightColor);
			}
			case ComponentFactoryHelper.TITLED_BORDER :
			{
				String borderTitle = object.optString(TITLE);
				int titleJustification = TitledBorder.DEFAULT_JUSTIFICATION;
				switch (object.optString(TITLE_JUSTIFICATION))
				{
					case "left" :
						titleJustification = TitledBorder.LEFT;
						break;
					case "right" :
						titleJustification = TitledBorder.RIGHT;
						break;
					case "center" :
						titleJustification = TitledBorder.CENTER;
						break;
					case "leading" :
						titleJustification = TitledBorder.LEADING;
						break;
					case "trailing" :
						titleJustification = TitledBorder.TRAILING;
				}
				int titlePosition = TitledBorder.DEFAULT_POSITION;
				switch (object.optString(TITLE_POSITION))
				{
					case "Above top" :
						titlePosition = TitledBorder.ABOVE_TOP;
						break;
					case "Top" :
						titlePosition = TitledBorder.TOP;
						break;
					case "Below top" :
						titlePosition = TitledBorder.BELOW_TOP;
						break;
					case "Above bottom" :
						titlePosition = TitledBorder.ABOVE_BOTTOM;
						break;
					case "Below bottom" :
						titlePosition = TitledBorder.BELOW_BOTTOM;
						break;
					case "Bottom" :
						titlePosition = TitledBorder.BOTTOM;
						break;
				}

				Font titleFont = FontPropertyType.INSTANCE.fromJSON(object.optJSONObject("font"), null, null, dataConverterContext, null);//TODO previous value
				Color titleColor = PersistHelper.createColor(object.optString("color"));
				return BorderFactory.createTitledBorder(null, borderTitle, titleJustification, titlePosition, titleFont, titleColor);
			}

		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Border value, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		Map<String, Object> javaResult = writeBorderToJson(value);
		return JSONUtils.toBrowserJSONFullValue(writer, key, javaResult, null, clientConversion, null);
	}

	public static Map<String, Object> writeBorderToJson(Border value)
	{
		Map<String, Object> map = new HashMap<>();
		if (value instanceof SpecialMatteBorder)
		{
			SpecialMatteBorder border = (SpecialMatteBorder)value;
			map.put(TYPE, ((border instanceof RoundedBorder) ? ComponentFactoryHelper.ROUNDED_BORDER : ComponentFactoryHelper.SPECIAL_MATTE_BORDER));
			Map<String, Object> borderStyle = new HashMap<>();
			map.put(BORDER_STYLE, borderStyle);

			borderStyle.put(BORDER_TOP_WIDTH, border.getTop() + "px");
			borderStyle.put(BORDER_RIGHT_WIDTH, border.getRight() + "px");
			borderStyle.put(BORDER_BOTTOM_WIDTH, border.getBottom() + "px");
			borderStyle.put(BORDER_LEFT_WIDTH, border.getLeft() + "px");
			borderStyle.put(BORDER_TOP_COLOR, border.getTopColor());
			borderStyle.put(BORDER_RIGHT_COLOR, border.getRightColor());
			borderStyle.put(BORDER_BOTTOM_COLOR, border.getBottomColor());
			borderStyle.put(BORDER_LEFT_COLOR, border.getLeftColor());

			if (border instanceof RoundedBorder)
			{
				float[] radius = ((RoundedBorder)border).getRadius();
				borderStyle.put(BORDER_RADIUS, radius[0] + "px " + radius[2] + "px " + radius[4] + "px " + radius[6] + "px /" + radius[1] + "px " + radius[3] +
					"px " + radius[5] + "px " + radius[7] + "px");
				String styles[] = ((RoundedBorder)border).getBorderStyles();
				borderStyle.put(BORDER_STYLE, styles[0] + " " + styles[1] + " " + styles[2] + " " + styles[3]);
			}
			else
			{
				borderStyle.put(BORDER_RADIUS, border.getRoundingRadius() + "px"); //$NON-NLS-1$
				//retval += "," + SpecialMatteBorder.createDashString(border.getDashPattern()); //$NON-NLS-1$
				if (border.getDashPattern() != null)
				{
					borderStyle.put(BORDER_STYLE, "dashed");
				}
				else
				{
					borderStyle.put(BORDER_STYLE, "solid");
				}
			}
		}
		else if (value instanceof EtchedBorder)
		{
			EtchedBorder border = (EtchedBorder)value;
			map.put(TYPE, ComponentFactoryHelper.ETCHED_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put(BORDER_STYLE, borderStyle);
			String hi = PersistHelper.createColorString(border.getHighlightColor());
			String sh = PersistHelper.createColorString(border.getShadowColor());
			if (border.getEtchType() != EtchedBorder.RAISED)
			{
				String tmp = hi;
				hi = sh;
				sh = tmp;
			}
			borderStyle.put(BORDER_COLOR, hi + " " + sh + " " + sh + " " + hi);
			borderStyle.put(BORDER_STYLE, border.getEtchType() == EtchedBorder.RAISED ? "ridge" : "groove");
			borderStyle.put(BORDER_WIDTH, "2px");
		}
		else if (value instanceof BevelBorder)
		{
			BevelBorder border = (BevelBorder)value;
			map.put(TYPE, ComponentFactoryHelper.BEVEL_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put(BORDER_STYLE, borderStyle);
			borderStyle.put(BORDER_STYLE, border.getBevelType() == BevelBorder.RAISED ? "outset" : "inset");

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
			borderStyle.put(BORDER_COLOR, hiOut + " " + shOut + " " + shIn + " " + hiin);
			borderStyle.put(BORDER_WIDTH, "2px");
		}
		else if (value instanceof LineBorder)
		{
			LineBorder border = (LineBorder)value;
			map.put(TYPE, ComponentFactoryHelper.LINE_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put(BORDER_STYLE, borderStyle);
			int thick = border.getThickness();
			borderStyle.put(BORDER_COLOR, border.getLineColor());
			borderStyle.put(BORDER_STYLE, "solid");
			borderStyle.put(BORDER_WIDTH, thick + "px");
		}
		else if (value instanceof MatteBorder)
		{
			MatteBorder border = (MatteBorder)value;
			map.put(TYPE, ComponentFactoryHelper.MATTE_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put(BORDER_STYLE, borderStyle);
			Insets in = border.getBorderInsets();
			borderStyle.put(BORDER_WIDTH, in.top + "px " + in.right + "px " + in.bottom + "px " + in.left + "px ");
			borderStyle.put(BORDER_COLOR, border.getMatteColor());
			borderStyle.put(BORDER_STYLE, "solid");
		}
		else if (value instanceof EmptyBorder)
		{
			EmptyBorder border = (EmptyBorder)value;
			map.put(TYPE, ComponentFactoryHelper.EMPTY_BORDER);
			Map<String, Object> borderStyle = new HashMap<>();
			map.put(BORDER_STYLE, borderStyle);
			Insets in = border.getBorderInsets();
			borderStyle.put(BORDER_WIDTH, in.top + "px " + in.right + "px " + in.bottom + "px " + in.left + "px ");
			borderStyle.put(BORDER_COLOR, "rgba(0,0,0,0)");
		}
		else if (value instanceof TitledBorder)
		{
			TitledBorder border = (TitledBorder)value;
			map.put(TYPE, ComponentFactoryHelper.TITLED_BORDER);
			map.put(TITLE, border.getTitle());

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
				case TitledBorder.LEADING :
					titleJust = "leading";
					break;
				case TitledBorder.TRAILING :
					titleJust = "trailing";
					break;
			}
			map.put(TITLE_JUSTIFICATION, titleJust);
			String titlePosition = "Top";
			switch (border.getTitlePosition())
			{
				case TitledBorder.ABOVE_TOP :
					titlePosition = "Above top";
					break;
				case TitledBorder.BELOW_TOP :
					titlePosition = "Below top";
					break;
				case TitledBorder.ABOVE_BOTTOM :
					titlePosition = "Above bottom";
					break;
				case TitledBorder.BELOW_BOTTOM :
					titlePosition = "Below bottom";
					break;
				case TitledBorder.BOTTOM :
					titlePosition = "Bottom";
					break;
			}
			map.put(TITLE_POSITION, titlePosition);
		}
		return map;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json != null && Boolean.valueOf(json.optBoolean("stringformat"));
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Border formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}

	@Override
	public Border toFormElementValue(JSONObject designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, pd, null, null);
	}

	@Override
	public Border toSabloComponentValue(Object rhinoValue, Border previousComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		if (rhinoValue instanceof String)
		{
			return ComponentFactoryHelper.createBorder((String)rhinoValue);
		}
		return (Border)(rhinoValue instanceof Border ? rhinoValue : null);
	}

	@Override
	public boolean isValueAvailableInRhino(Border webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Border webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return webComponentValue; // TODO any conversion needed here?
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof String)
		{
			Border border = ComponentFactoryHelper.createBorder((String)value);
			JSONStringer writer = new JSONStringer();
			try
			{
				writer.object();
				toJSON(writer, pd.getName(), border, pd, new DataConversion(), null);
				writer.endObject();
				return new JSONObject(writer.toString()).get(pd.getName());
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
		}
		return JSWebComponent.defaultRhinoToDesignValue(value, application);
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		Border border = fromJSON(value, null, pd, null, null);
		return ComponentFactoryHelper.createBorderString(border);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IDesignValueConverter#fromDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public Object fromDesignValue(Object newValue, PropertyDescription propertyDescription)
	{
		if (!(Boolean)propertyDescription.getConfig())
		{
			try
			{
				return fromJSON((newValue instanceof String && ((String)newValue).startsWith("{")) ? new JSONObject((String)newValue) : newValue, null,
					propertyDescription, null, null);
			}
			catch (Exception e)
			{
				Debug.error("can't parse '" + newValue + "' to the real type for property converter: " + propertyDescription.getType(), e);
				return null;
			}
		}
		return newValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IDesignValueConverter#toDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public Object toDesignValue(Object value, PropertyDescription pd)
	{
		if (value instanceof Border)
		{
			JSONStringer writer = new JSONStringer();
			toJSON(writer, null, (Border)value, pd, null, null);
			return new JSONObject(writer.toString());
		}
		return value;
	}
}
