package com.servoy.j2db.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.CSS.Attribute;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.newmatch.Matcher;
import org.xhtmlrenderer.css.newmatch.Selector;
import org.xhtmlrenderer.css.parser.CSSErrorHandler;
import org.xhtmlrenderer.css.parser.CSSParser;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.Ruleset;
import org.xhtmlrenderer.css.sheet.Stylesheet;


public class ServoyStyleSheet implements IStyleSheet
{
	public static final String[] BORDER_CSS = new String[] { CSSName.BORDER_BOTTOM_COLOR.toString(), CSSName.BORDER_BOTTOM_SHORTHAND.toString(), CSSName.BORDER_BOTTOM_STYLE.toString(), CSSName.BORDER_BOTTOM_WIDTH.toString(), CSSName.BORDER_TOP_COLOR.toString(), CSSName.BORDER_TOP_SHORTHAND.toString(), CSSName.BORDER_TOP_STYLE.toString(), CSSName.BORDER_TOP_WIDTH.toString(), CSSName.BORDER_LEFT_COLOR.toString(), CSSName.BORDER_LEFT_SHORTHAND.toString(), CSSName.BORDER_LEFT_STYLE.toString(), CSSName.BORDER_LEFT_WIDTH.toString(), CSSName.BORDER_RIGHT_COLOR.toString(), CSSName.BORDER_RIGHT_SHORTHAND.toString(), CSSName.BORDER_RIGHT_STYLE.toString(), CSSName.BORDER_RIGHT_WIDTH.toString(), CSSName.BORDER_COLOR_SHORTHAND.toString(), CSSName.BORDER_SHORTHAND.toString(), CSSName.BORDER_TOP_SHORTHAND.toString(), CSSName.BORDER_LEFT_SHORTHAND.toString(), CSSName.BORDER_BOTTOM_SHORTHAND.toString(), CSSName.BORDER_RIGHT_SHORTHAND.toString() };
	private final String[] MARGIN_CSS = new String[] { CSSName.MARGIN_BOTTOM.toString(), CSSName.MARGIN_TOP.toString(), CSSName.MARGIN_LEFT.toString(), CSSName.MARGIN_RIGHT.toString(), CSSName.MARGIN_SHORTHAND.toString() };
	public final static String[] BACKGROUND_IMAGE_CSS = new String[] { CSSName.BACKGROUND_ATTACHMENT.toString(), CSSName.BACKGROUND_IMAGE.toString(), CSSName.BACKGROUND_POSITION.toString(), CSSName.BACKGROUND_REPEAT.toString(), CSSName.BACKGROUND_SIZE.toString(), CSSName.OPACITY.toString() };
	public final static String[] ROUNDED_RADIUS_PREFIX = new String[] { "-webkit-", "-moz-" };


	static Attribute[] marginAttributes = new Attribute[] { CSS.Attribute.MARGIN, CSS.Attribute.MARGIN_BOTTOM, CSS.Attribute.MARGIN_LEFT, CSS.Attribute.MARGIN_RIGHT, CSS.Attribute.MARGIN_TOP };
	public static Attribute[] fontAttributes = new Attribute[] { CSS.Attribute.FONT, CSS.Attribute.FONT_FAMILY, CSS.Attribute.FONT_SIZE, CSS.Attribute.FONT_STYLE, CSS.Attribute.FONT_VARIANT, CSS.Attribute.FONT_WEIGHT };
	public static String[] borderAttributesExtensions = new String[] { "border-left-style", "border-right-style", "border-top-style", "border-bottom-style", "border-left-color", "border-right-color", "border-top-color", "border-bottom-color", "border-radius", "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius" };
	public static Attribute[] borderAttributes = new Attribute[] { CSS.Attribute.BORDER, CSS.Attribute.BORDER_BOTTOM, CSS.Attribute.BORDER_BOTTOM_WIDTH, CSS.Attribute.BORDER_COLOR, CSS.Attribute.BORDER_LEFT, CSS.Attribute.BORDER_LEFT_WIDTH, CSS.Attribute.BORDER_RIGHT, CSS.Attribute.BORDER_RIGHT_WIDTH, CSS.Attribute.BORDER_STYLE, CSS.Attribute.BORDER_TOP, CSS.Attribute.BORDER_TOP_WIDTH, CSS.Attribute.BORDER_WIDTH };

	public static String[] LinearGradientsIdentifiers = new String[] { "-webkit-linear-gradient", "-moz-linear-gradient", "-ms-linear-gradient", "-o-linear-gradient" };
	public static final String[] ROUNDED_RADIUS_ATTRIBUTES = new String[] { "border-radius", "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius" };
	private final CSSErrorHandler errorHandler;
	private Stylesheet styleSheet;
	private final FixedStyleSheet ss;

	public ServoyStyleSheet(String cssContent)
	{
		this(cssContent, "<noname>");
	}

	public ServoyStyleSheet(String cssContent, final String name)
	{
		this(cssContent, name, false);
	}

	public ServoyStyleSheet(String cssContent, final String name, final boolean skipSwingStyleSheetIgnoringErrors)
	{
		this.errorHandler = new CSSErrorHandler()
		{

			public void error(String uri, String message)
			{
				if (!skipSwingStyleSheetIgnoringErrors)
				{
					Debug.error("Error at css parsing, " + name + ',' + uri + ':' + message); //$NON-NLS-1$
				}
			}
		};
		CSSParser parser = new CSSParser(errorHandler);
		if (cssContent != null)
		{
			try
			{
				styleSheet = parser.parseStylesheet("servoy stylesheet" /* to fix NPE in parsing */, 0, new StringReader(cssContent));
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
		ss = new FixedStyleSheet();
		// if this is ignoring parsign errors then don't try to fill a swing stylesheet at all, this is very likely a
		// full css3 file.
		if (!skipSwingStyleSheetIgnoringErrors)
		{
			try
			{
				ss.addRule(cssContent);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	@Deprecated
	public AttributeSet getRule(String selector)
	{
		return ss.getRule(selector);
	}

	@Deprecated
	public Font getFont(AttributeSet a)
	{
		return ss.getFont(a);
	}

	@Deprecated
	public Insets getMargin(AttributeSet a)
	{
		return ss.getMargin(a);
	}

	@Deprecated
	public Border getBorder(AttributeSet a)
	{
		return ss.getBorder(a);
	}

	@Deprecated
	public int getHAlign(AttributeSet a)
	{
		return ss.getHAlign(a);
	}

	@Deprecated
	public int getVAlign(AttributeSet a)
	{
		return ss.getVAlign(a);
	}

	@Deprecated
	public Color getForeground(AttributeSet a)
	{
		return ss.getForeground(a);
	}

	@Deprecated
	public Color getBackground(AttributeSet a)
	{
		return ss.getBackground(a);
	}

	@Deprecated
	public boolean hasBorder(AttributeSet s)
	{
		return ss.hasBorder(s);
	}

	@Deprecated
	public boolean hasMargin(AttributeSet s)
	{
		return ss.hasMargin(s);
	}

	@Deprecated
	public boolean hasFont(AttributeSet s)
	{
		return ss.hasFont(s);
	}

	private final Map<String, IStyleRule> ruleCache = new ConcurrentHashMap<String, IStyleRule>();

	public IStyleRule getCSSRule(String selector)
	{
		IStyleRule styleRule = ruleCache.get(selector);
		if (styleRule != null) return styleRule;

		if (styleSheet != null)
		{
			Matcher matcher = new Matcher(new ServoyTreeResolver(), new ServoyAttributeResolver(), new ServoyStylesheetFactor(errorHandler),
				Arrays.asList(styleSheet), null);
			styleRule = new ServoyStyleRule(matcher.getCascadedStyle(selector, true));
			ruleCache.put(selector, styleRule);
		}
		return styleRule;
	}

	public Font getFont(IStyleRule a)
	{
		if (hasFont(a))
		{
			String family = "SansSerif"; //$NON-NLS-1$
			int size = 12;
			int style = Font.PLAIN;
			ServoyStyleRule rule = (ServoyStyleRule)a;
			if (a.hasAttribute(CSSName.FONT_FAMILY.toString()))
			{
				PropertyDeclaration declaration = rule.getPropertyDeclaration(CSSName.FONT_FAMILY.toString());
				if (declaration != null)
				{
					family = ((PropertyValue)declaration.getValue()).getStringArrayValue()[0];
				}
			}
			if (a.hasAttribute(CSSName.FONT_WEIGHT.toString()))
			{
				PropertyDeclaration declaration = rule.getPropertyDeclaration(CSSName.FONT_WEIGHT.toString());
				if (declaration != null)
				{
					if (declaration.getValue().getCssText().contains("bold") || Utils.getAsFloat(declaration.getValue().getCssText()) > 400) //$NON-NLS-1$
					{
						style |= Font.BOLD;
					}
				}
			}
			if (a.hasAttribute(CSSName.FONT_STYLE.toString()))
			{
				PropertyDeclaration declaration = rule.getPropertyDeclaration(CSSName.FONT_STYLE.toString());
				if (declaration != null)
				{
					if (declaration.getValue().getCssText().contains("italic")) //$NON-NLS-1$
					{
						style |= Font.ITALIC;
					}
				}
			}
			if (a.hasAttribute(CSSName.FONT_SIZE.toString()))
			{
				PropertyDeclaration declaration = rule.getPropertyDeclaration(CSSName.FONT_SIZE.toString());
				if (declaration != null)
				{
					float fontSize = ss.getLength(declaration.getValue().getCssText());
					if (fontSize > 0)
					{
						if (declaration.getValue().getCssText().endsWith("px")) //$NON-NLS-1$
						{
							fontSize = 4 * fontSize / 3;
						}
						size = Math.round(fontSize);
					}

				}
			}
			return PersistHelper.createFont(family, style, size);
		}
		return null;
	}

	public Insets getMargin(IStyleRule a)
	{
		if (hasMargin(a))
		{
			float top = ss.getLength(a.getValue(CSS.Attribute.MARGIN_TOP.toString()));
			float bottom = ss.getLength(a.getValue(CSS.Attribute.MARGIN_BOTTOM.toString()));
			float left = ss.getLength(a.getValue(CSS.Attribute.MARGIN_LEFT.toString()));
			float right = ss.getLength(a.getValue(CSS.Attribute.MARGIN_RIGHT.toString()));

			return new Insets(top < 0 ? 0 : (int)top, left < 0 ? 0 : (int)left, bottom < 0 ? 0 : (int)bottom, right < 0 ? 0 : (int)right);
		}
		return null;
	}

	public Border getBorder(final IStyleRule a)
	{
		return ss.getBorder(new AttributeSet()
		{

			public boolean isEqual(AttributeSet attr)
			{
				return false;
			}

			public boolean isDefined(Object attrName)
			{
				return a.hasAttribute(attrName.toString());
			}

			public AttributeSet getResolveParent()
			{
				return null;
			}

			public Enumeration< ? > getAttributeNames()
			{
				final Iterator<String> it = a.getAttributeNames().iterator();
				return new Enumeration()
				{
					public boolean hasMoreElements()
					{
						return it.hasNext();
					}

					public Object nextElement()
					{
						return it.next();
					}
				};
			}

			public int getAttributeCount()
			{
				return a.getAttributeCount();
			}

			public Object getAttribute(Object key)
			{
				if ("border-top-color".equals(key.toString()) || "border-right-color".equals(key.toString()) || "border-bottom-color".equals(key.toString()) ||
					"border-left-color".equals(key.toString()))
				{
					String[] values = a.getValues(key.toString());
					if (values != null && values.length > 0)
					{
						for (int i = values.length - 1; i >= 0; i--)
						{
							Color color = PersistHelper.createColor(values[i]);
							if (color != null)
							{
								return values[i];
							}
						}
					}
					return null;
				}
				if (CSSName.BORDER_TOP_LEFT_RADIUS.toString().equals(key.toString()) || CSSName.BORDER_TOP_RIGHT_RADIUS.toString().equals(key.toString()) ||
					CSSName.BORDER_BOTTOM_RIGHT_RADIUS.toString().equals(key.toString()) || CSSName.BORDER_BOTTOM_LEFT_RADIUS.toString().equals(key.toString()))
				{
					String[] values = a.getValues(key.toString());
					if (values != null && values.length > 0)
					{
						for (int i = values.length - 1; i >= 0; i--)
						{
							if (values[i] != null && !values[i].contains("%"))
							{
								// fallback mechanism
								// % border radius length is not supported in SC
								return values[i];
							}
						}
					}
					return null;
				}
				return a.getValue(key.toString());
			}

			public AttributeSet copyAttributes()
			{
				return null;
			}

			public boolean containsAttributes(AttributeSet attributes)
			{
				return false;
			}

			public boolean containsAttribute(Object name, Object value)
			{
				return false;
			}
		});
	}

	public int getHAlign(IStyleRule a)
	{
		return ss.getHAlign(a.getValue(CSSName.TEXT_ALIGN.toString()));
	}

	public int getVAlign(IStyleRule a)
	{
		return ss.getVAlign(a.getValue(CSSName.VERTICAL_ALIGN.toString()));
	}

	public Color getForeground(IStyleRule a)
	{
		return getLastValidColor(a, CSSName.COLOR.toString());
	}

	public List<Color> getForegrounds(IStyleRule a)
	{
		return getValidColors(a, CSSName.COLOR.toString());
	}

	public Color getBackground(IStyleRule a)
	{
		return getLastValidColor(a, CSSName.BACKGROUND_COLOR.toString());
	}

	public List<Color> getBackgrounds(IStyleRule a)
	{
		return getValidColors(a, CSSName.BACKGROUND_COLOR.toString());
	}

	private List<Color> getValidColors(IStyleRule a, String cssAttribute)
	{
		String[] cssDefinitions = a.getValues(cssAttribute);
		List<Color> cssValidColors = new ArrayList<Color>();

		if (cssDefinitions != null && cssDefinitions.length > 0)
		{
			for (String cssDefinition : cssDefinitions)
			{
				Color color = PersistHelper.createColorWithTransparencySupport(cssDefinition);
				if (color != null)
				{
					cssValidColors.add(color);
				}
			}
		}
		return (cssValidColors.size() == 0) ? null : cssValidColors;
	}

	private Color getLastValidColor(IStyleRule a, String cssAttribute)
	{
		String[] cssDefinitions = a.getValues(cssAttribute);
		if (cssDefinitions != null && cssDefinitions.length > 0)
		{
			for (int i = cssDefinitions.length - 1; i >= 0; i--)
			{
				Color color = PersistHelper.createColor(cssDefinitions[i]);
				if (color != null)
				{
					return color;
				}
			}
		}
		return null;
	}

	private boolean hasProperty(IStyleRule s, String[] names)
	{
		for (String name : names)
		{
			if (s.hasAttribute(name)) return true;
		}
		return false;
	}

	public boolean hasBorder(IStyleRule s)
	{
		return hasProperty(s, BORDER_CSS);
	}

	public boolean hasMargin(IStyleRule s)
	{
		return hasProperty(s, MARGIN_CSS);
	}

	public boolean hasFont(IStyleRule s)
	{
		return s.hasAttribute(CSSName.FONT_FAMILY.toString()) || s.hasAttribute(CSSName.FONT_SHORTHAND.toString()) ||
			s.hasAttribute(CSSName.FONT_SIZE.toString()) || s.hasAttribute(CSSName.FONT_STYLE.toString()) || s.hasAttribute(CSSName.FONT_VARIANT.toString()) ||
			s.hasAttribute(CSSName.FONT_WEIGHT.toString());
	}

	public List<String> getStyleNames()
	{
		List<String> list = new ArrayList<String>();
		if (styleSheet != null)
		{
			for (Object ruleset : styleSheet.getContents())
			{
				if (ruleset instanceof Ruleset)
				{
					for (Object selector : ((Ruleset)ruleset).getFSSelectors())
					{
						if (selector instanceof Selector)
						{
							list.add(((Selector)selector).getSelectorText().replace("null.", ".").trim());
						}
					}
				}
			}
		}
		return list;
	}
}
