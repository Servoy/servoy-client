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

import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jcompagner
 *
 */
public class BorderPropertyType implements IConvertedPropertyType<Border>, IDesignToFormElement<JSONObject, Border, Border>,
	IFormElementToTemplateJSON<Border, Border>
{

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
	public Border fromJSON(Object newValue, Border previousValue, IDataConverterContext dataConverterContext)
	{
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Border value, DataConversion clientConversion) throws JSONException
	{
		Map<String, Object> javaResult = writeBorderToJson(value);
		return JSONUtils.toBrowserJSONFullValue(writer, key, javaResult, null, clientConversion);
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

			borderStyle.put("borderTopWidth", border.getTop() + "px");
			borderStyle.put("borderRightWidth", border.getRight() + "px");
			borderStyle.put("borderBottomWidth", border.getBottom() + "px");
			borderStyle.put("borderLeftWidth", border.getLeft() + "px");
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

	@Override
	public Object parseConfig(JSONObject json)
	{
		return Boolean.valueOf(json == null || !json.has("stringformat") || json.optBoolean("stringformat"));
	}

	@Override
	public Border defaultValue()
	{
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Border formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, IServoyDataConverterContext servoyDataConverterContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, browserConversionMarkers);
	}

	@Override
	public Border toFormElementValue(JSONObject designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, null);
	}

}
