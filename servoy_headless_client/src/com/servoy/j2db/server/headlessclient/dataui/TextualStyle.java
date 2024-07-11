package com.servoy.j2db.server.headlessclient.dataui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.xhtmlrenderer.css.constants.CSSName;

import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ServoyStyleSheet;

public class TextualStyle extends Properties
{
	private static final long serialVersionUID = 1L;

	private String selector;
	private final List<String> order = new ArrayList<String>();
	private TextualCSS css;
	private final Map<String, List<String>> stackedValues = new HashMap<String, List<String>>();

	public TextualStyle()
	{
		//used when style tag propery is needed
	}

	TextualStyle(String selector, TextualCSS css)
	{
		if (selector == null) throw new NullPointerException("selector cannot be null");
		this.selector = selector;
		this.css = css;
	}

	@Override
	public synchronized Object setProperty(String name, String value)
	{
		if (name == null) return null;
		if (value == null)
		{
			stackedValues.remove(name);
			return remove(name);
		}
		else
		{
			return setProperty(name, value, true);
		}
	}

	@Override
	public synchronized Object remove(Object key)
	{
		order.remove(key);
		return super.remove(key);
	}

	public Object setProperty(String name, String value, boolean override)
	{
		return setProperty(name, (value != null ? new String[] { value } : null), override);
	}

	public Object setProperty(String name, String[] values, boolean override)
	{
		if (override)
		{
			stackedValues.remove(name);
			order.remove(name);
			order.add(name);
			stackedValues.put(name, Arrays.asList(values));
			return super.put(name, values[values.length - 1]);
		}
		else
		{
			if (!super.containsKey(name))
			{
				order.add(name);
				List<String> valuesList = stackedValues.get(name);
				List<String> newValues = Arrays.asList(values);
				if (valuesList == null)
				{
					valuesList = new ArrayList<String>();
					stackedValues.put(name, valuesList);
				}
				valuesList.addAll(newValues);
				return super.put(name, values[values.length - 1]);
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return toString(selector);
	}

	public String toString(String pSelector)
	{
		StringBuffer retval = new StringBuffer();
		if (pSelector == null)
		{
			retval.append("style='");
		}
		else
		{
			retval.append(pSelector);
			retval.append("\n{\n");
		}
		retval.append(getValuesAsString(pSelector));
		if (pSelector == null)
		{
			retval.append("' ");
		}
		else
		{
			retval.append("}\n\n");
		}
		return retval.toString();
	}

	public String getValuesAsString(String pSelector)
	{
		StringBuffer retval = new StringBuffer();
		for (String name : order)
		{
			String[] cssValues = null;
			List<String> values = stackedValues.get(name);
			if (values != null && values.size() > 1)
			{
				cssValues = values.toArray(new String[values.size()]);
			}
			else
			{
				cssValues = new String[] { getProperty(name) };
			}
			for (String val : cssValues)
			{
				if (CSSName.BACKGROUND_IMAGE.toString().equals(name) && val != null && val.startsWith("linear-gradient"))
				{
					String[] colors = getGradientColors(val);
					if (colors != null && colors.length == 2 && colors[0] != null)
					{
						appendValue(retval, pSelector, name, "-webkit-gradient(linear, " + (val.contains("top") ? "center" : "left") + " top, " +
							(val.contains("top") ? "center bottom" : "right top") + ", from(" + colors[0] + "), to(" + colors[1] + "))");
						boolean hasRoundedRadius = false;
						for (String attribute : ServoyStyleSheet.ROUNDED_RADIUS_ATTRIBUTES)
						{
							String value = getProperty(attribute);
							if (value != null && !value.startsWith("0"))
							{
								hasRoundedRadius = true;
								break;
							}
							List<String> roundedBorderValues = stackedValues.get(attribute);
							if (roundedBorderValues != null)
							{
								for (String borderValue : roundedBorderValues)
								{
									if (borderValue != null && !borderValue.startsWith("0"))
									{
										hasRoundedRadius = true;
										break;
									}
								}
							}
						}
						if (!hasRoundedRadius)
						{
							// filter doesn't get along with rounded border; css should define fallback for this
							appendValue(retval, pSelector, "filter", "progid:DXImageTransform.Microsoft.gradient(startColorStr=" + colors[0] +
								", EndColorStr=" + colors[1] + ", GradientType=" + (val.contains("top") ? "0" : "1") + ")");
						}
					}
					for (String linearIdentifier : ServoyStyleSheet.LinearGradientsIdentifiers)
					{
						appendValue(retval, pSelector, name, val.replace("linear-gradient", linearIdentifier));
					}
				}
				if (CSSName.OPACITY.toString().equals(name))
				{
					float opacity = com.servoy.j2db.util.Utils.getAsFloat(val);
					appendValue(retval, pSelector, "filter", "alpha(opacity=" + Float.valueOf(opacity * 100).intValue() + ")");
				}
				if (name.contains("radius") && name.contains("border"))
				{
					for (String prefix : ServoyStyleSheet.ROUNDED_RADIUS_PREFIX)
					{
						appendValue(retval, pSelector, prefix + name, val);
					}
					if ((getProperty(CSSName.BACKGROUND_COLOR.toString()) != null || stackedValues.get(CSSName.BACKGROUND_COLOR.toString()) != null) &&
						!retval.toString().contains("background-clip:"))
					{
						// we also have background color, i guess we expect padding-box background-clip
						appendValue(retval, pSelector, "-moz-background-clip", "padding");
						appendValue(retval, pSelector, "-webkit-background-clip", "padding-box");
						appendValue(retval, pSelector, "background-clip", "padding-box");
					}
				}
				if (name.equals(CSSName.FONT_FAMILY.toString()))
				{
					val = HtmlUtils.getValidFontFamilyValue(val);
				}
				appendValue(retval, pSelector, name, val);
			}
		}
		return retval.toString();
	}

	protected void appendValue(StringBuffer retval, String pSelector, String name, String value)
	{
		if (pSelector != null) retval.append('\t');
		retval.append(name);
		retval.append(": ");
		retval.append(value);
		retval.append(';');
		if (pSelector != null) retval.append('\n');
	}

	private String[] getGradientColors(String cssDeclaration)
	{
		String[] colors = new String[2];
		cssDeclaration = cssDeclaration.substring(cssDeclaration.indexOf('(') + 1, cssDeclaration.lastIndexOf(')'));
		StringTokenizer tokenizer = new StringTokenizer(cssDeclaration, ",");
		if (cssDeclaration.startsWith("top") || cssDeclaration.startsWith("left") || cssDeclaration.startsWith("right") ||
			cssDeclaration.startsWith("bottom")) tokenizer.nextElement();
		for (int i = 0; i < 2; i++)
		{
			if (tokenizer.hasMoreElements())
			{
				String colorString = tokenizer.nextToken().trim();
				if (colorString.startsWith("rgb"))
				{
					while (tokenizer.hasMoreElements())
					{
						String token = tokenizer.nextToken();
						colorString += "," + token;
						if (token.contains(")")) break;
					}
				}
				colors[i] = colorString;
			}
			else
			{
				return null;
			}
		}
		return colors;
	}

	public String getOnlyProperties()
	{
		StringBuffer retval = new StringBuffer();
		for (String name : order)
		{
			String val = getProperty(name);
			retval.append(name);
			retval.append(": ");
			retval.append(val);
			retval.append(';');
		}
		return retval.toString();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (selector == null) return false;
		return selector.equals(obj);
	}

	public TextualCSS getTextualCSS()
	{
		return css;
	}

	public void copy(String name, TextualStyle source)
	{
		if (source.containsKey(name)) setProperty(name, source.getProperty(name));
	}

	public void copyAllFrom(TextualStyle source)
	{
		for (String name : source.order)
		{
			String val = source.getProperty(name);
			setProperty(name, val);
		}
	}

//		public boolean containsKey(Object name)
//		{
//			return properties.containsKey(name);
//		}
//
//		public String getProperty(String name)
//		{
//			return (String) properties.get(name);
//		}
}