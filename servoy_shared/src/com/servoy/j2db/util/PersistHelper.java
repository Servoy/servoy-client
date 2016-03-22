/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Helper class to the repository model persist side
 *
 * @author jblok
 */
public class PersistHelper
{
	public static final String COLOR_RGBA_DEF = "rgba"; //$NON-NLS-1$
	public static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);


	private PersistHelper()
	{
	}

	private static final Map<String, String> basicCssColors = new HashMap<String, String>();

	static
	{
		basicCssColors.put("black", "#000000"); //$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("silver", "#C0C0C0");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("gray", "#808080");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("white", "#FFFFFF");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("maroon", "#800000");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("red", "#FF0000");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("purple", "#800080");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("fuchsia", "#FF00FF");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("green", "#008000");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("lime", "#00FF00");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("olive", "#808000");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("yellow", "#FFFF00");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("navy", "#000080");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("blue", "#0000FF");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("teal", "#008080");//$NON-NLS-1$ //$NON-NLS-2$
		basicCssColors.put("aqua", "#00FFFF");//$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String createPageFormatString(PageFormat format)
	{
		if (format == null)
		{
			return null;
		}
		Paper paper = format.getPaper();
		StringBuilder sb = new StringBuilder();
		sb.append(format.getOrientation());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getWidth());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getHeight());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableX());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableY());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableWidth());
		sb.append(";"); //$NON-NLS-1$
		sb.append(paper.getImageableHeight());
		sb.append(";"); //$NON-NLS-1$
		return sb.toString();
	}

	public static PageFormat createPageFormat(String value)
	{
		PageFormat format = null;
		if (value != null)
		{
			//orientation;width;height;ImageableX;ImageableY,ImageableWidth;ImageableHeight
			StringTokenizer tk = new StringTokenizer(value, ";"); //$NON-NLS-1$
			if (tk.hasMoreTokens())
			{
				int orientation = Utils.getAsInteger(tk.nextToken());
				double width = Utils.getAsDouble(tk.nextToken());
				double height = Utils.getAsDouble(tk.nextToken());
				double imageableX = Utils.getAsDouble(tk.nextToken());
				double imageableY = Utils.getAsDouble(tk.nextToken());
				double imageableWidth = Utils.getAsDouble(tk.nextToken());
				double imageableHeight = Utils.getAsDouble(tk.nextToken());

				format = new PageFormat();
				format.setOrientation(orientation);
				Paper paper = new Paper();
				paper.setSize(width, height);
				paper.setImageableArea(imageableX, imageableY, imageableWidth, imageableHeight);
				format.setPaper(paper);
			}
		}
		else
		{
			format = null;
		}
		return format;
	}

	public static String createDimensionString(Dimension d)
	{
		if (d == null) return null;
		StringBuilder retval = new StringBuilder();
		retval.append(d.width);
		retval.append(","); //$NON-NLS-1$
		retval.append(d.height);
		return retval.toString();
	}

	public static Dimension createDimension(String d)
	{
		if (d == null) return null;
		int w = 0;
		int h = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(d, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) w = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) h = Utils.getAsInteger(tk.nextToken());
		}
		catch (NumberFormatException ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Dimension(w, h);
	}

	public static String createPointString(Point d)
	{
		if (d == null) return null;
		StringBuilder retval = new StringBuilder();
		retval.append(d.x);
		retval.append(","); //$NON-NLS-1$
		retval.append(d.y);
		return retval.toString();
	}

	public static Point createPoint(String d)
	{
		if (d == null) return null;
		int x = 0;
		int y = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(d, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) x = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) y = Utils.getAsInteger(tk.nextToken());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Point(x, y);
	}

	public static String createLocaleString(Locale loc)
	{
		if (loc == null) return null;
		StringBuilder retval = new StringBuilder();
		retval.append(loc.getLanguage());
		retval.append(","); //$NON-NLS-1$
		retval.append(loc.getCountry());
		retval.append(","); //$NON-NLS-1$
		retval.append(loc.getVariant());
		return retval.toString();
	}

	public static Locale createLocale(String d)
	{
		if (d == null) return null;
		String lang = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$
		try
		{
			StringTokenizer tk = new StringTokenizer(d, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) lang = tk.nextToken();
			if (tk.hasMoreTokens()) country = tk.nextToken();
			if (tk.hasMoreTokens()) variant = tk.nextToken();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Locale(lang, country, variant);
	}

	public static String createInsetsString(Insets ins)
	{
		if (ins == null) return null;
		StringBuilder retval = new StringBuilder();
		retval.append(ins.top);
		retval.append(","); //$NON-NLS-1$
		retval.append(ins.left);
		retval.append(","); //$NON-NLS-1$
		retval.append(ins.bottom);
		retval.append(","); //$NON-NLS-1$
		retval.append(ins.right);
		return retval.toString();
	}

	public static Insets createInsets(String s)
	{
		if (s == null) return null;
		int top = 0;
		int left = 0;
		int bottom = 0;
		int right = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(s, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) top = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) left = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) bottom = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) right = Utils.getAsInteger(tk.nextToken());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Insets(top, left, bottom, right);
	}

	public static String createRectangleString(Rectangle rect)
	{
		if (rect == null) return null;
		StringBuilder retval = new StringBuilder();
		Point p = rect.getLocation();
		retval.append(createPointString(p));
		retval.append(","); //$NON-NLS-1$
		Dimension d = rect.getSize();
		retval.append(createDimensionString(d));
		return retval.toString();
	}

	public static Rectangle createRectangle(String a_rect)
	{
		if (a_rect == null) return null;
		int x = 0;
		int y = 0;
		int w = 0;
		int h = 0;
		try
		{
			StringTokenizer tk = new StringTokenizer(a_rect, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens()) x = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) y = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) w = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) h = Utils.getAsInteger(tk.nextToken());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
		return new Rectangle(x, y, w, h);
	}

	public static Color createColorWithTransparencySupport(String s)
	{
		Color color = createColor(s);
		if (color == null)
		{
			if (s != null && s.startsWith(COLOR_RGBA_DEF))
			{
				String definition = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")")); //$NON-NLS-1$//$NON-NLS-2$
				StringTokenizer tokenizer = new StringTokenizer(definition, ","); //$NON-NLS-1$
				if (tokenizer.countTokens() == 4)
				{
					try
					{
						int r = Utils.getAsInteger(tokenizer.nextToken(), true);
						int g = Utils.getAsInteger(tokenizer.nextToken(), true);
						int b = Utils.getAsInteger(tokenizer.nextToken(), true);
						int a = (int)(Utils.getAsFloat(tokenizer.nextToken(), true) * 255);
						return Internalize.intern(new Color(r, g, b, a));
					}
					catch (Exception ex)
					{
						Debug.warn("Cannot parse rgba color : " + s); //$NON-NLS-1$
						return null;
					}
				}
				else
				{
					Debug.warn("Cannot parse rgba color : " + s); //$NON-NLS-1$
				}
			}
		}
		return color;
	}

	public static Color createColor(String s)
	{
		Color retval = null;

		if (s != null && (s.length() == 4 || s.length() == 7))
		{
			String ss = s;
			if (s.length() == 4) // abbreviated
			{
				ss = new String(new char[] { s.charAt(0), s.charAt(1), s.charAt(1), s.charAt(2), s.charAt(2), s.charAt(3), s.charAt(3) });
			}
			try
			{
				retval = Color.decode(ss);
			}
			catch (NumberFormatException e)
			{
				//ignore;
			}
		}
		if (IStyleSheet.COLOR_TRANSPARENT.equals(s))
		{
			return COLOR_TRANSPARENT;
		}
		if (s != null && retval == null)
		{
			try
			{
				Field field = Color.class.getField(s);
				return (Color)field.get(null);
			}
			catch (Exception e)
			{
				// ignore
				if (basicCssColors.containsKey(s.toLowerCase())) return createColor(basicCssColors.get(s.toLowerCase()));
			}
		}
		return Internalize.intern(retval);
	}

	public static String createColorString(Color c)
	{
		String retval = null;
		if (c != null)
		{
			int alpha = c.getAlpha();
			if (alpha == 255)
			{
				String r = Integer.toHexString(c.getRed());
				if (r.length() == 1) r = "0" + r; //$NON-NLS-1$
				String g = Integer.toHexString(c.getGreen());
				if (g.length() == 1) g = "0" + g; //$NON-NLS-1$
				String b = Integer.toHexString(c.getBlue());
				if (b.length() == 1) b = "0" + b; //$NON-NLS-1$
				retval = "#" + r + g + b; //$NON-NLS-1$
			}
			else if (alpha == 0)
			{
				retval = IStyleSheet.COLOR_TRANSPARENT;
			}
			else
			{
				retval = COLOR_RGBA_DEF + '(' + c.getRed() + ',' + c.getGreen() + ',' + c.getBlue() + ',' + Utils.formatNumber(Locale.US, alpha / 255f, 1) +
					')';
			}
		}
		return retval;
	}

	private static Map<String, Font> allFonts = new HashMap<String, Font>();
	private static Map<String, Object> guessedFonts;

	private synchronized static void initFonts() // it can be called by multiple concurrent threads in case of web-client startup; because HashMap is not synchronized it can end up (and it did) in a situation where iterators.next() and map.put() will do infinite loops => 100% CPU
	{
		if (allFonts.size() == 0)
		{
			Font[] allFontsArray = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
			for (Font element : allFontsArray)
			{
				allFonts.put(Utils.stringReplace(element.getName(), " ", "").toLowerCase(), element); //$NON-NLS-1$ //$NON-NLS-2$
			}
			guessedFonts = new HashMap<String, Object>();
		}
	}

	private static Map<String, Font> allCreatedFonts = new HashMap<String, Font>();
	private static Object getCompositeFontMethod;

	public static Font createFont(String name, int style, int size)
	{
		if (name == null) return null;

		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(","); //$NON-NLS-1$
		sb.append(style);
		sb.append(","); //$NON-NLS-1$
		sb.append(size <= 0 ? 1 : size);
		String fontName = sb.toString();
		if (allCreatedFonts.containsKey(fontName))
		{
			return allCreatedFonts.get(fontName);
		}
		Font font = createFontImpl(name, style, size);
		allCreatedFonts.put(fontName, font);
		return font;
	}

	/**
	 * Adds fonts to the internal cache, so when processing fonts Servoy can resolve it. Must be called as soon as possible, ideally in the onSolutionOpen callback<br>
	 * <br>
	 * For the Smart Client adding the fotn through this function is enough to use the font in StyleSheets. For the Web Client, the font needs to be added to the HTML markup by the developer.<br>
	 * <br>
	 * The custom fonts added to the system through this method can only be used in StyleSheets, not directly on elements using the fontStyle property<br>
	 * <br>
	 * The Form Editor in Servoy Developer might not display the correct font until a debug client is launched and the font is added to the system.
	 * A workaround can be making the font available to the OS on the development machine<br>
	 * <br>
	 * Supports multiple fontfiles per font, see http://stackoverflow.com/questions/24800886/how-to-import-a-custom-java-awt-font-from-a-font-family-with-multiple-ttf-files<br>
	 * <br>
	 *
	 * @param url Supports both media library and external urls
	 *
	 * @throws FontFormatException
	 * @throws IOException
	 */
	public static void addFont(String url) throws FontFormatException, IOException
	{
		initFonts();
		URL fontUrl = url.startsWith(MediaURLStreamHandler.MEDIA_URL_DEF) ? new URL(null, url, new MediaURLStreamHandler()) : new URL(url);
		Font font = Font.createFont(Font.TRUETYPE_FONT, fontUrl.openStream());
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		ge.registerFont(font);
		allFonts.put(Utils.stringReplace(font.getName(), " ", "").toLowerCase(), font); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static Font createFontImpl(String name, int style, int size)
	{
		if (name == null) return null;

		if (size <= 0) size = 1;

		Font retval = new Font(name, style, size);
		String specialName = Utils.stringReplace(name, " ", "").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
		initFonts();
		if (!allFonts.containsKey(specialName))
		{
			Font guess = guessFont(name);
			if (guess == null)
			{
				// maybe is family name
				guess = createFontByFamily(name, style, size);
			}
			if (guess != null)
			{
				retval = guess.deriveFont(style, size);//also make compatible with designer
			}
		}
		if (retval != null)
		{
			try
			{
				if (getCompositeFontMethod == null)
				{
					Class< ? > fontManager = Class.forName("sun.font.FontManager"); //$NON-NLS-1$
					getCompositeFontMethod = fontManager.getMethod("getCompositeFontUIResource", new Class[] { Font.class }); //$NON-NLS-1$
				}
			}
			catch (Exception e)
			{
				try
				{
					Class< ? > fontManager = Class.forName("sun.font.FontUtilities"); //$NON-NLS-1$
					getCompositeFontMethod = fontManager.getMethod("getCompositeFontUIResource", new Class[] { Font.class }); //$NON-NLS-1$
				}
				catch (Exception e1)
				{
					getCompositeFontMethod = Boolean.FALSE;
					Debug.trace("Couldn't create composite font for " + retval, e); //$NON-NLS-1$
				}
			}
			try
			{
				if (getCompositeFontMethod instanceof Method)
				{
					Object compositeFont = ((Method)getCompositeFontMethod).invoke(null, retval);
					if (compositeFont instanceof Font)
					{
						Font fnt = (Font)compositeFont;
						// force Font object instead of FontUIResource
						retval = fnt.deriveFont(fnt.getStyle());
					}
				}
			}
			catch (Exception e)
			{
				getCompositeFontMethod = Boolean.FALSE;
				Debug.trace("Couldn't create composite font for " + retval, e); //$NON-NLS-1$
			}

		}
		return retval;
	}

	private static Font createFontByFamily(String family, int style, int size)
	{
		initFonts();
		StringTokenizer tk = new StringTokenizer(family.toString(), ","); //$NON-NLS-1$
		String familyName = tk.nextToken().trim();
		if (familyName.startsWith("'") || familyName.startsWith("\"")) familyName = familyName.substring(1);
		if (familyName.endsWith("'") || familyName.endsWith("\"")) familyName = familyName.substring(0, familyName.length() - 1);
		for (Font font : allFonts.values())
		{
			if (font.getFamily().equalsIgnoreCase(familyName))
			{
				return font.deriveFont(style, size);
			}
		}
		return null;
	}

	public static Font createFont(String s)
	{
		if (s == null) return null;

		if (allCreatedFonts.containsKey(s))
		{
			return allCreatedFonts.get(s);//also nulls are returned
		}

		StringTokenizer tk = new StringTokenizer(s, ","); //$NON-NLS-1$
		if (tk.countTokens() >= 3)
		{
			String name = tk.nextToken();
			String style = tk.nextToken();
			String size = tk.nextToken();
//			String family = null;
//			if (tk.hasMoreTokens())
//			{
//				family = tk.nextToken();
//			}
			int istyle = Utils.getAsInteger(style);
			int isize = Utils.getAsInteger(size);
			Font retval = createFontImpl(name, istyle, isize);
			allCreatedFonts.put(s, retval);
			return retval;
		}
		else
		{
			return null;
		}
	}

	private static Font guessFont(String aName)
	{
		if (aName == null || aName.length() == 0) return null;

		Object cached = guessedFonts.get(aName);
		if (cached != null)
		{
			if (cached instanceof Font)
			{
				return (Font)cached;
			}
			// else not found previous call
			return null;
		}

		Font guessFont = doGuessFont(aName);
		guessedFonts.put(aName, guessFont == null ? Boolean.FALSE : guessFont);
		return guessFont;
	}

	private static Font doGuessFont(String aName)
	{
		String formatedName = stringFormat(aName);
		Font guessFont = null;
		int maxPieces = 0, minMissingPieces = 999;
		initFonts();
		Iterator<Font> it = allFonts.values().iterator();
		while (it.hasNext())
		{
			Font f = it.next();

			String fontName = f.getName();
			if (fontName.equalsIgnoreCase(aName)) return f;
			if (fontName.equalsIgnoreCase(formatedName)) return f;
			fontName = stringFormat(fontName);
			if (fontName.equalsIgnoreCase(aName)) return f;
			if (fontName.equalsIgnoreCase(formatedName)) return f;

			List<String> pieceList = new ArrayList<String>();
			StringTokenizer tk = new StringTokenizer(fontName, " "); //$NON-NLS-1$
			while (tk.hasMoreElements())
			{
				String element = (String)tk.nextElement();
				pieceList.add(element);
			}
			boolean substantial = false;
			int pieces = 0;
			int missingPieces = 0;
			for (int j = 0; j < pieceList.size(); j++)
			{
				String piece = pieceList.get(j);
				if (piece.length() <= 2) continue; // do not match 'a' or 'in'
				if (aName.indexOf(piece) >= 0)
				{
					pieces++;
					if (!isStyle(piece)) substantial = true;
				}
				missingPieces++;
			}
			if (substantial && (pieces > maxPieces || (pieces == maxPieces && missingPieces < minMissingPieces)))
			{
				guessFont = f;
				maxPieces = pieces;
				minMissingPieces = missingPieces;
			}
		}
		return guessFont;
	}

	private static boolean isStyle(String value)
	{
		String[] styles = { "bold", //$NON-NLS-1$
		"italic", //$NON-NLS-1$
		"regular", //$NON-NLS-1$
		"medium", //$NON-NLS-1$
		"oblique", //$NON-NLS-1$
		"semi", //$NON-NLS-1$
		"narrow", //$NON-NLS-1$
		"black", //$NON-NLS-1$
		"light", //$NON-NLS-1$
		"extra", //$NON-NLS-1$
		"condensed", //$NON-NLS-1$
		"cond", //$NON-NLS-1$
		"ultra", //$NON-NLS-1$
		"demi", //$NON-NLS-1$
		"thin", //$NON-NLS-1$
		"wide", //$NON-NLS-1$
		"rounded" }; //$NON-NLS-1$
		for (String element : styles)
		{
			if (element.equalsIgnoreCase(value)) return true;
		}
		return false;
	}

	/**
	 * Split string no separator char, taking into account braces.
	 * @param string
	 * @param separator
	 * @return
	 */
	public static List<String> splitStringWithBracesOnSeparator(String string, char separator)
	{
		List<String> tokens = new ArrayList<String>();
		if (string != null && string.length() > 0)
		{
			int depth = 0;
			StringBuilder current = new StringBuilder();
			for (int i = 0; i < string.length(); i++)
			{
				char c = string.charAt(i);
				if (depth <= 0 && c == separator)
				{
					tokens.add(current.toString());
					current = new StringBuilder();
				}
				else
				{
					if (c == '(')
					{
						depth++;
					}
					else if (c == ')')
					{
						depth--;
					}
					current.append(c);
				}
			}
			// add last one
			tokens.add(current.toString());
		}
		return tokens;
	}

	private static String stringFormat(String fontName)
	{
		if (fontName == null || fontName.length() == 0) return fontName;

		String fn = fontName;
		fn = Utils.stringReplace(fn, "MT", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "MS", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "LT", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "PS", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fn = Utils.stringReplace(fn, "-", " "); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuilder stringResult = new StringBuilder();
		for (int i = 0; i < fn.length(); i++)
		{
			char ch1 = fn.charAt(i);
			stringResult.append(ch1);
			if (Character.getType(ch1) == Character.LOWERCASE_LETTER && i < (fn.length() - 1))
			{
				char ch2 = fn.charAt(i + 1);
				if (Character.getType(ch2) == Character.UPPERCASE_LETTER)
				{
					stringResult.append(" "); //$NON-NLS-1$
				}
			}
		}
		return stringResult.toString();
	}

	public static String createFontString(Font f)
	{
		if (f == null) return null;
		StringBuilder sb = new StringBuilder();
		sb.append(f.getName());
		sb.append(","); //$NON-NLS-1$
		sb.append(f.getStyle());
		sb.append(","); //$NON-NLS-1$
		sb.append(f.getSize());
		if (!Utils.equalObjects(f.getName(), f.getFamily()))
		{
			sb.append(","); //$NON-NLS-1$
			sb.append(f.getFamily());
		}
		return sb.toString();
	}

	public static String createFontCssString(Font f)
	{
		if (f == null) return null;
		StringBuilder sb = new StringBuilder();
		if (f.isBold())
		{
			sb.append("bold "); //$NON-NLS-1$
		}
		if (f.isItalic())
		{
			sb.append("italic "); //$NON-NLS-1$
		}
		sb.append(f.getSize());
		sb.append("pt "); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append(f.getName());
		sb.append("\""); //$NON-NLS-1$
		return sb.toString();
	}

	public static Pair<String, String>[] createFontCSSProperties(String fontType)
	{
		if (fontType == null) return null;

		StringTokenizer tk = new StringTokenizer(fontType, ","); //$NON-NLS-1$
		if (tk.countTokens() >= 3)
		{
			String family = tk.nextToken();
			int istyle = Utils.getAsInteger(tk.nextToken());
			int isize = Utils.getAsInteger(tk.nextToken());
			if (tk.hasMoreTokens()) family = tk.nextToken();
			family = HtmlUtils.getValidFontFamilyValue(family);

			Pair<String, String> fam = new Pair<String, String>("font-family", family + ", Verdana, Arial"); //$NON-NLS-1$ //$NON-NLS-2$
			Pair<String, String> size = new Pair<String, String>("font-size", isize + "px"); //$NON-NLS-1$ //$NON-NLS-2$
			Pair<String, String> italic = null;
			Pair<String, String> bold = null;
			if (istyle == Font.PLAIN)
			{
				italic = new Pair<String, String>("font-style", "normal"); //$NON-NLS-1$ //$NON-NLS-2$
				bold = new Pair<String, String>("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$

			}
			else
			{
				italic = (Font.ITALIC == (Font.ITALIC & istyle) ? new Pair<String, String>("font-style", "italic") : null); //$NON-NLS-1$ //$NON-NLS-2$
				bold = (Font.BOLD == (Font.BOLD & istyle) ? new Pair<String, String>("font-weight", "bold") : null); //$NON-NLS-1$ //$NON-NLS-2$

			}
			return new Pair[] { fam, size, italic, bold };
		}
		return null;
	}

	/**
	 * Get highest super persist.
	 *
	 * @param persist
	 * @return
	 */
	public static IPersist getBasePersist(ISupportExtendsID persist)
	{
		ISupportExtendsID p = persist;
		while (true)
		{
			ISupportExtendsID superp = (ISupportExtendsID)PersistHelper.getSuperPersist(p);
			if (superp == null)
			{
				return (IPersist)p;
			}
			p = superp;
		}
	}

	public static IPersist getSuperPersist(final ISupportExtendsID persist)
	{
		if (persist instanceof IFlattenedPersistWrapper && ((IFlattenedPersistWrapper)persist).getWrappedPersist() instanceof Form)
		{
			return ((IFlattenedPersistWrapper<Form>)persist).getWrappedPersist().getExtendsForm();
		}
		if (persist instanceof Form)
		{
			return ((Form)persist).getExtendsForm();
		}
		final int extendsID = persist.getExtendsID();
		if (extendsID > 0)
		{
			Form form = (Form)((AbstractBase)persist).getAncestor(IRepository.FORMS);
			if (form != null)
			{
				form = form.getExtendsForm();
				while (form != null)
				{
					IPersist superPersist = (IPersist)form.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof ISupportExtendsID && (extendsID == o.getID() || extendsID == ((ISupportExtendsID)o).getExtendsID()))
							{
								return o;
							}
							return CONTINUE_TRAVERSAL;
						}
					});
					if (superPersist != null)
					{
						return superPersist;
					}
					form = form.getExtendsForm();
				}
			}
		}
		return null;
	}

	public static boolean isOverrideOrphanElement(ISupportExtendsID persist)
	{
		IPersist parentPersist = (IPersist)persist;
		if (parentPersist instanceof ISupportExtendsID && ((ISupportExtendsID)parentPersist).getExtendsID() == IRepository.UNRESOLVED_ELEMENT) return true;
		while (parentPersist instanceof ISupportExtendsID && isOverrideElement(((ISupportExtendsID)parentPersist)))
		{
			parentPersist = getSuperPersist(((ISupportExtendsID)parentPersist));
			if (parentPersist == null) return true;
		}
		return false;
	}

	public static boolean isOverrideElement(ISupportExtendsID persist)
	{
		return ((AbstractBase)persist).hasProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()) && persist.getExtendsID() > 0;
	}

	/**
	 * Example if a superform has a tabPanel with a tab, and a derived form adds another tab to the tabpannel then  hasOverrideChildren(derivedTabPanel) returns true
	 */
	public static boolean hasOverrideChildren(IPersist persist)
	{
		if (persist instanceof ISupportChilds && persist instanceof ISupportExtendsID)
		{
			ISupportChilds p = (ISupportChilds)persist;
			ISupportChilds superPersist = (ISupportChilds)PersistHelper.getSuperPersist((ISupportExtendsID)persist);
			if (superPersist == null)
			{
				return false;
			}
			for (IPersist child : Utils.iterate(p.getAllObjects()))
			{
				if (child instanceof ISupportExtendsID && !PersistHelper.isOverrideElement((ISupportExtendsID)child))
				{ // is is an etra child element compared to it's super child elements
					return true;
				}
				else if (((AbstractBase)child).hasOverrideProperties())
				{
					return true;
				}
			}

		}
		return false;

	}

	/**
	 * Get the override hierarchy of this element as list [self, super, super.super, ...]
	 */
	public static List<AbstractBase> getOverrideHierarchy(ISupportExtendsID persist)
	{
		List<AbstractBase> overrideHierarchy = new ArrayList<AbstractBase>(3);
		IPersist superPersist = (IPersist)persist;
		while (superPersist instanceof ISupportExtendsID)
		{
			overrideHierarchy.add((AbstractBase)superPersist);
			superPersist = getSuperPersist((ISupportExtendsID)superPersist);
		}
		return overrideHierarchy;
	}

	public static List<IPersist> getHierarchyChildren(AbstractBase parent)
	{
		if (parent instanceof ISupportExtendsID)
		{
			List<IPersist> children = new ArrayList<IPersist>();
			List<AbstractBase> parentHierarchy = new ArrayList<AbstractBase>();
			List<Integer> existingIDs = new ArrayList<Integer>();
			AbstractBase element = parent;
			while (element != null && !parentHierarchy.contains(element))
			{
				parentHierarchy.add(element);
				element = (AbstractBase)PersistHelper.getSuperPersist((ISupportExtendsID)element);
			}
			for (AbstractBase temp : parentHierarchy)
			{
				for (IPersist child : temp.getAllObjectsAsList())
				{
					Integer extendsID = new Integer(((ISupportExtendsID)child).getExtendsID());
					if (!existingIDs.contains(new Integer(child.getID())) && !existingIDs.contains(extendsID))
					{
						if (PersistHelper.isOverrideOrphanElement((ISupportExtendsID)child))
						{
							// some deleted element
							continue;
						}
						existingIDs.add(child.getID());
						children.add(child);
					}
					if (extendsID.intValue() > 0 && !existingIDs.contains(extendsID))
					{
						existingIDs.add(extendsID);
					}
				}
			}
			return children;
		}
		else
		{
			return parent.getAllObjectsAsList();
		}

	}

	/**
	 * Similar to {@link AbstractBase#getPropertiesMap()}, but takes into account persist inheritance.
	 * @param extendable a persist which could be inherit from another.
	 * @return the map of property values collected from the persist's hierarchy chain.
	 */
	public static Map<String, Object> getFlattenedPropertiesMap(ISupportExtendsID extendable)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		List<AbstractBase> hierarchy = PersistHelper.getOverrideHierarchy(extendable);
		for (int i = hierarchy.size() - 1; i >= 0; i--)
		{
			map.putAll(hierarchy.get(i).getPropertiesMap());
		}
		return map;
	}

	public static List<String> getOrderedStyleSheets(FlattenedSolution fs)
	{
		List<String> styleSheets = new ArrayList<String>();
		List<Solution> orderedModules = new ArrayList<Solution>();

		orderedModules.add(fs.getSolution());
		if (fs.getModules() != null)
		{
			buildOrderedModulesList(fs.getSolution(), orderedModules, new ArrayList<Solution>(Arrays.asList(fs.getModules())));
		}
		for (Solution solution : orderedModules)
		{
			if (solution.getStyleSheetID() > 0)
			{
				Media media = fs.getMedia(solution.getStyleSheetID());
				if (!styleSheets.contains(media.getName()))
				{
					styleSheets.add(media.getName());
				}
			}
		}
		return styleSheets;
	}

	private static void buildOrderedModulesList(Solution parent, List<Solution> orderedModules, List<Solution> fsModules)
	{
		List<Solution> newParents = new ArrayList<Solution>();
		for (String moduleName : Utils.getTokenElements(parent.getModulesNames(), ",", true))
		{
			Iterator<Solution> it = fsModules.iterator();
			while (it.hasNext())
			{
				Solution module = it.next();
				if (module.getName().equals(moduleName))
				{
					if (!orderedModules.contains(module)) orderedModules.add(module);
					it.remove();
					newParents.add(module);
					break;
				}
			}
		}
		for (Solution newParent : newParents)
		{
			buildOrderedModulesList(newParent, orderedModules, fsModules);
		}
	}
}
