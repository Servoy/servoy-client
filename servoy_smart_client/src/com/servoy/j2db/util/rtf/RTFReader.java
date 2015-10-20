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
package com.servoy.j2db.util.rtf;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabStop;

/**
 * Takes a sequence of RTF tokens and text and appends the text described by the RTF to a <code>StyledDocument</code> (the <em>target</em>). The RTF is
 * lexed from the character stream by the <code>RTFParser</code> which is this class's superclass.
 * 
 * This class is an indirect subclass of OutputStream. It must be closed in order to guarantee that all of the text has been sent to the text acceptor.
 * 
 * @see RTFParser
 * @see java.io.OutputStream
 */
class RTFReader extends RTFParser
{
	/** The object to which the parsed text is sent. */
	StyledDocument target;

	/**
	 * Miscellaneous information about the parser's state. This dictionary is saved and restored when an RTF group begins or ends.
	 */
	Dictionary parserState; /* Current parser state */
	/**
	 * This is the "dst" item from parserState. rtfDestination is the current rtf destination. It is cached in an instance variable for speed.
	 */
	Destination rtfDestination;
	/** This holds the current document attributes. */
	MutableAttributeSet documentAttributes;

	/** This Dictionary maps Integer font numbers to String font names. */
	Dictionary fontTable;
	/** This array maps color indices to Color objects. */
	Color[] colorTable;
	/** This array maps character style numbers to Style objects. */
	Style[] characterStyles;
	/** This array maps paragraph style numbers to Style objects. */
	Style[] paragraphStyles;
	/** This array maps section style numbers to Style objects. */
	Style[] sectionStyles;

	/**
	 * This is the RTF version number, extracted from the \rtf keyword. The version information is currently not used.
	 */
	int rtfversion;

	/**
	 * <code>true</code> to indicate that if the next keyword is unknown, the containing group should be ignored.
	 */
	boolean ignoreGroupIfUnknownKeyword;

	/**
	 * The parameter of the most recently parsed \\ucN keyword, used for skipping alternative representations after a Unicode character.
	 */
	int skippingCharacters;

	static private Dictionary straightforwardAttributes;
	static
	{
		straightforwardAttributes = RTFAttributes.attributesByKeyword();
	}

	private final MockAttributeSet mockery;

	/* this should be final, but there's a bug in javac... */
	/**
	 * textKeywords maps RTF keywords to single-character strings, for those keywords which simply insert some text.
	 */
	static Dictionary textKeywords = null;
	static
	{
		textKeywords = new Hashtable();
		textKeywords.put("\\", "\\"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("{", "{"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("}", "}"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put(" ", "\u00A0"); /* not in the spec... *///$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("~", "\u00A0"); /* nonbreaking space *///$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("_", "\u2011"); /* nonbreaking hyphen *///$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("bullet", "\u2022"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("emdash", "\u2014"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("emspace", "\u2003"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("endash", "\u2013"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("enspace", "\u2002"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("ldblquote", "\u201C"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("lquote", "\u2018"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("ltrmark", "\u200E"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("rdblquote", "\u201D"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("rquote", "\u2019"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("rtlmark", "\u200F"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("tab", "\u0009"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("zwj", "\u200D"); //$NON-NLS-1$ //$NON-NLS-2$
		textKeywords.put("zwnj", "\u200C"); //$NON-NLS-1$ //$NON-NLS-2$

		/*
		 * There is no Unicode equivalent to an optional hyphen, as far as I can tell.
		 */
		textKeywords.put("-", "\u2027"); /* TODO: optional hyphen *///$NON-NLS-1$ //$NON-NLS-2$
	}

	/* some entries in parserState */
	static final String TabAlignmentKey = "tab_alignment"; //$NON-NLS-1$
	static final String TabLeaderKey = "tab_leader"; //$NON-NLS-1$

	static Dictionary characterSets;
	static boolean useNeXTForAnsi = false;
	static
	{
		characterSets = new Hashtable();
	}

/* TODO: per-font font encodings ( \fcharset control word ) ? */

	/**
	 * Creates a new RTFReader instance. Text will be sent to the specified TextAcceptor.
	 * 
	 * @param destination The TextAcceptor which is to receive the text.
	 */
	public RTFReader(StyledDocument destination)
	{
		int i;

		target = destination;
		parserState = new Hashtable();
		fontTable = new Hashtable();

		rtfversion = -1;

		mockery = new MockAttributeSet();
		documentAttributes = new SimpleAttributeSet();
	}

	/**
	 * Called when the RTFParser encounters a bin keyword in the RTF stream.
	 * 
	 * @see RTFParser
	 */
	@Override
	public void handleBinaryBlob(byte[] data)
	{
		if (skippingCharacters > 0)
		{
			/* a blob only counts as one character for skipping purposes */
			skippingCharacters--;
			return;
		}

		/* someday, someone will want to do something with blobs */
	}


	/**
	 * Handles any pure text (containing no control characters) in the input stream. Called by the superclass.
	 */
	@Override
	public void handleText(String text)
	{
		if (skippingCharacters > 0)
		{
			if (skippingCharacters >= text.length())
			{
				skippingCharacters -= text.length();
				return;
			}
			else
			{
				text = text.substring(skippingCharacters);
				skippingCharacters = 0;
			}
		}

		if (rtfDestination != null)
		{
			rtfDestination.handleText(text);
			return;
		}

		warning("Text with no destination. oops."); //$NON-NLS-1$
	}

	/** The default color for text which has no specified color. */
	Color defaultColor()
	{
		return Color.black;
	}

	/**
	 * Called by the superclass when a new RTF group is begun. This implementation saves the current <code>parserState</code>, and gives the current
	 * destination a chance to save its own state.
	 * 
	 * @see RTFParser#begingroup
	 */
	@Override
	public void begingroup()
	{
		if (skippingCharacters > 0)
		{
			/* TODO this indicates an error in the RTF. Log it? */
			skippingCharacters = 0;
		}

		/*
		 * we do this little dance to avoid cloning the entire state stack and immediately throwing it away.
		 */
		Object oldSaveState = parserState.get("_savedState"); //$NON-NLS-1$
		if (oldSaveState != null) parserState.remove("_savedState"); //$NON-NLS-1$
		Dictionary saveState = (Dictionary)((Hashtable)parserState).clone();
		if (oldSaveState != null) saveState.put("_savedState", oldSaveState); //$NON-NLS-1$
		parserState.put("_savedState", saveState); //$NON-NLS-1$

		if (rtfDestination != null) rtfDestination.begingroup();
	}

	/**
	 * Called by the superclass when the current RTF group is closed. This restores the parserState saved by <code>begingroup()</code> as well as invoking the
	 * endgroup method of the current destination.
	 * 
	 * @see RTFParser#endgroup
	 */
	@Override
	public void endgroup()
	{
		if (skippingCharacters > 0)
		{
			/* NB this indicates an error in the RTF. Log it? */
			skippingCharacters = 0;
		}

		Dictionary restoredState = (Dictionary)parserState.get("_savedState"); //$NON-NLS-1$
		Destination restoredDestination = (Destination)restoredState.get("dst"); //$NON-NLS-1$
		if (restoredDestination != rtfDestination)
		{
			rtfDestination.close(); /* allow the destination to clean up */
			rtfDestination = restoredDestination;
		}
		Dictionary oldParserState = parserState;
		parserState = restoredState;
		if (rtfDestination != null) rtfDestination.endgroup(oldParserState);
	}

	protected void setRTFDestination(Destination newDestination)
	{
		/*
		 * Check that setting the destination won't close the current destination (should never happen)
		 */
		Dictionary previousState = (Dictionary)parserState.get("_savedState"); //$NON-NLS-1$
		if (previousState != null)
		{
			if (rtfDestination != previousState.get("dst")) { //$NON-NLS-1$
				warning("Warning, RTF destination overridden, invalid RTF."); //$NON-NLS-1$
				rtfDestination.close();
			}
		}
		rtfDestination = newDestination;
		parserState.put("dst", rtfDestination); //$NON-NLS-1$
	}

	/**
	 * Called by the user when there is no more input (<i>i.e.</i>, at the end of the RTF file.)
	 * 
	 * @see OutputStream#close
	 */
	@Override
	public void close() throws IOException
	{
		Enumeration docProps = documentAttributes.getAttributeNames();
		while (docProps.hasMoreElements())
		{
			Object propName = docProps.nextElement();
			target.putProperty(propName, documentAttributes.getAttribute(propName));
		}

		/* RTFParser should have ensured that all our groups are closed */

		warning("RTF filter done."); //$NON-NLS-1$

		super.close();
	}

	/**
	 * Handles a parameterless RTF keyword. This is called by the superclass (RTFParser) when a keyword is found in the input stream.
	 * 
	 * @returns <code>true</code> if the keyword is recognized and handled; <code>false</code> otherwise
	 * @see RTFParser#handleKeyword
	 */
	@Override
	public boolean handleKeyword(String keyword)
	{
		Object item;
		boolean ignoreGroupIfUnknownKeywordSave = ignoreGroupIfUnknownKeyword;

		if (skippingCharacters > 0)
		{
			skippingCharacters--;
			return true;
		}

		ignoreGroupIfUnknownKeyword = false;

		if ((item = textKeywords.get(keyword)) != null)
		{
			handleText((String)item);
			return true;
		}

		if (keyword.equals("fonttbl")) { //$NON-NLS-1$
			setRTFDestination(new FonttblDestination());
			return true;
		}

		if (keyword.equals("colortbl")) { //$NON-NLS-1$
			setRTFDestination(new ColortblDestination());
			return true;
		}

		if (keyword.equals("stylesheet")) { //$NON-NLS-1$
			setRTFDestination(new StylesheetDestination());
			return true;
		}

		if (keyword.equals("info")) { //$NON-NLS-1$
			setRTFDestination(new InfoDestination());
			return false;
		}

		if (keyword.equals("mac")) { //$NON-NLS-1$
			setCharacterSet("mac"); //$NON-NLS-1$
			return true;
		}

		if (keyword.equals("ansi")) { //$NON-NLS-1$
			if (useNeXTForAnsi) setCharacterSet("NeXT"); //$NON-NLS-1$
			else setCharacterSet("ansi"); //$NON-NLS-1$
			return true;
		}

		if (keyword.equals("next")) { //$NON-NLS-1$
			setCharacterSet("NeXT"); //$NON-NLS-1$
			return true;
		}

		if (keyword.equals("pc")) { //$NON-NLS-1$
			setCharacterSet("cpg437"); /* IBM Code Page 437 *///$NON-NLS-1$
			return true;
		}

		if (keyword.equals("pca")) { //$NON-NLS-1$
			setCharacterSet("cpg850"); /* IBM Code Page 850 *///$NON-NLS-1$
			return true;
		}

		if (keyword.equals("*")) { //$NON-NLS-1$
			ignoreGroupIfUnknownKeyword = true;
			return true;
		}

		if (rtfDestination != null)
		{
			if (rtfDestination.handleKeyword(keyword)) return true;
		}

		/* this point is reached only if the keyword is unrecognized */

		/* other destinations we don't understand and therefore ignore */
		if (keyword.equals("aftncn") || //$NON-NLS-1$
			keyword.equals("aftnsep") || //$NON-NLS-1$
			keyword.equals("aftnsepc") || //$NON-NLS-1$
			keyword.equals("annotation") || //$NON-NLS-1$
			keyword.equals("atnauthor") || //$NON-NLS-1$
			keyword.equals("atnicn") || //$NON-NLS-1$
			keyword.equals("atnid") || //$NON-NLS-1$
			keyword.equals("atnref") || //$NON-NLS-1$
			keyword.equals("atntime") || //$NON-NLS-1$
			keyword.equals("atrfend") || //$NON-NLS-1$
			keyword.equals("atrfstart") || //$NON-NLS-1$
			keyword.equals("bkmkend") || //$NON-NLS-1$
			keyword.equals("bkmkstart") || //$NON-NLS-1$
			keyword.equals("datafield") || //$NON-NLS-1$
			keyword.equals("do") || //$NON-NLS-1$
			keyword.equals("dptxbxtext") || //$NON-NLS-1$
			keyword.equals("falt") || //$NON-NLS-1$
			keyword.equals("field") || //$NON-NLS-1$
			keyword.equals("file") || //$NON-NLS-1$
			keyword.equals("filetbl") || //$NON-NLS-1$
			keyword.equals("fname") || //$NON-NLS-1$
			keyword.equals("fontemb") || //$NON-NLS-1$
			keyword.equals("fontfile") || //$NON-NLS-1$
			keyword.equals("footer") || //$NON-NLS-1$
			keyword.equals("footerf") || //$NON-NLS-1$
			keyword.equals("footerl") || //$NON-NLS-1$
			keyword.equals("footerr") || //$NON-NLS-1$
			keyword.equals("footnote") || //$NON-NLS-1$
			keyword.equals("ftncn") || //$NON-NLS-1$
			keyword.equals("ftnsep") || //$NON-NLS-1$
			keyword.equals("ftnsepc") || //$NON-NLS-1$
			keyword.equals("header") || //$NON-NLS-1$
			keyword.equals("headerf") || //$NON-NLS-1$
			keyword.equals("headerl") || //$NON-NLS-1$
			keyword.equals("headerr") || //$NON-NLS-1$
			keyword.equals("keycode") || //$NON-NLS-1$
			keyword.equals("nextfile") || //$NON-NLS-1$
			keyword.equals("object") || //$NON-NLS-1$
			keyword.equals("pict") || //$NON-NLS-1$
			keyword.equals("pn") || //$NON-NLS-1$
			keyword.equals("pnseclvl") || //$NON-NLS-1$
			keyword.equals("pntxtb") || //$NON-NLS-1$
			keyword.equals("pntxta") || //$NON-NLS-1$
			keyword.equals("revtbl") || //$NON-NLS-1$
			keyword.equals("rxe") || //$NON-NLS-1$
			keyword.equals("tc") || //$NON-NLS-1$
			keyword.equals("template") || //$NON-NLS-1$
			keyword.equals("txe") || //$NON-NLS-1$
			keyword.equals("xe")) { //$NON-NLS-1$
			ignoreGroupIfUnknownKeywordSave = true;
		}

		if (ignoreGroupIfUnknownKeywordSave)
		{
			setRTFDestination(new DiscardingDestination());
		}

		return false;
	}

	/**
	 * Handles an RTF keyword and its integer parameter. This is called by the superclass (RTFParser) when a keyword is found in the input stream.
	 * 
	 * @returns <code>true</code> if the keyword is recognized and handled; <code>false</code> otherwise
	 * @see RTFParser#handleKeyword
	 */
	@Override
	public boolean handleKeyword(String keyword, int parameter)
	{
		boolean ignoreGroupIfUnknownKeywordSave = ignoreGroupIfUnknownKeyword;

		if (skippingCharacters > 0)
		{
			skippingCharacters--;
			return true;
		}

		ignoreGroupIfUnknownKeyword = false;

		if (keyword.equals("cb")) //$NON-NLS-1$
		{
			parserState.put(keyword, new Integer(parameter));
			return true;
		}

		if (keyword.equals("uc")) //$NON-NLS-1$
		{
			/* count of characters to skip after a unicode character */
			parserState.put("UnicodeSkip", new Integer(parameter)); //$NON-NLS-1$
			return true;
		}

		if (keyword.equals("u")) { //$NON-NLS-1$
			if (parameter < 0) parameter = parameter + 65536;
			handleText((char)parameter);
			Number skip = (Number)(parserState.get("UnicodeSkip")); //$NON-NLS-1$
			if (skip != null)
			{
				skippingCharacters = skip.intValue();
			}
			else
			{
				skippingCharacters = 1;
			}
			return true;
		}

		if (keyword.equals("rtf")) { //$NON-NLS-1$
			rtfversion = parameter;
			setRTFDestination(new DocumentDestination());
			return true;
		}

		if (keyword.startsWith("NeXT") || //$NON-NLS-1$
			keyword.equals("private")) //$NON-NLS-1$
		ignoreGroupIfUnknownKeywordSave = true;

		if (rtfDestination != null)
		{
			if (rtfDestination.handleKeyword(keyword, parameter)) return true;
		}

		/* this point is reached only if the keyword is unrecognized */

		if (ignoreGroupIfUnknownKeywordSave)
		{
			setRTFDestination(new DiscardingDestination());
		}

		return false;
	}

	private void setTargetAttribute(String name, Object value)
	{
//    target.changeAttributes(new LFDictionary(LFArray.arrayWithObject(value), LFArray.arrayWithObject(name)));
	}

	/**
	 * setCharacterSet sets the current translation table to correspond with the named character set. The character set is loaded if necessary.
	 * 
	 * @see AbstractFilter
	 */
	public void setCharacterSet(String name)
	{
		Object set;

		try
		{
			set = getCharacterSet(name);
		}
		catch (Exception e)
		{
			warning("Exception loading RTF character set \"" + name + "\": " + e); //$NON-NLS-1$ //$NON-NLS-2$
			set = null;
		}

		if (set != null)
		{
			translationTable = (char[])set;
		}
		else
		{
			warning("Unknown RTF character set \"" + name + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			if (!name.equals("ansi")) { //$NON-NLS-1$
				try
				{
					translationTable = (char[])getCharacterSet("ansi"); //$NON-NLS-1$
				}
				catch (IOException e)
				{
					throw new InternalError("RTFReader: Unable to find character set resources (" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		setTargetAttribute(Constants.RTFCharacterSet, name);
	}

	/**
	 * Adds a character set to the RTFReader's list of known character sets
	 */
	public static void defineCharacterSet(String name, char[] table)
	{
		if (table.length < 256) throw new IllegalArgumentException("Translation table must have 256 entries."); //$NON-NLS-1$
		characterSets.put(name, table);
	}

	/**
	 * Looks up a named character set. A character set is a 256-entry array of characters, mapping unsigned byte values to their Unicode equivalents. The
	 * character set is loaded if necessary.
	 * 
	 * @returns the character set
	 */
	public static Object getCharacterSet(final String name) throws IOException
	{
		char[] set;

		set = (char[])characterSets.get(name);
		if (set == null)
		{
			InputStream charsetStream;
			charsetStream = (InputStream)java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
			{
				public Object run()
				{
					return RTFReader.class.getResourceAsStream("charsets/" + name + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			set = readCharset(charsetStream);
			defineCharacterSet(name, set);
		}
		return set;
	}

	/**
	 * Parses a character set from an InputStream. The character set must contain 256 decimal integers, separated by whitespace, with no punctuation. B- and C-
	 * style comments are allowed.
	 * 
	 * @returns the newly read character set
	 */
	static char[] readCharset(InputStream strm) throws IOException
	{
		char[] values = new char[256];
		int i;
		StreamTokenizer in = new StreamTokenizer(new BufferedReader(new InputStreamReader(strm)));

		in.eolIsSignificant(false);
		in.commentChar('#');
		in.slashSlashComments(true);
		in.slashStarComments(true);

		i = 0;
		while (i < 256)
		{
			int ttype;
			try
			{
				ttype = in.nextToken();
			}
			catch (Exception e)
			{
				throw new IOException("Unable to read from character set file (" + e + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (ttype != StreamTokenizer.TT_NUMBER)
			{
//	    System.out.println("Bad token: type=" + ttype + " tok=" + in.sval);
				throw new IOException("Unexpected token in character set file"); //$NON-NLS-1$
//	    continue;
			}
			values[i] = (char)(in.nval);
			i++;
		}

		return values;
	}

	static char[] readCharset(java.net.URL href) throws IOException
	{
		return readCharset(href.openStream());
	}

	/**
	 * An interface (could be an entirely abstract class) describing a destination. The RTF reader always has a current destination which is where text is sent.
	 * 
	 * @see RTFReader
	 */
	interface Destination
	{
		void handleBinaryBlob(byte[] data);

		void handleText(String text);

		boolean handleKeyword(String keyword);

		boolean handleKeyword(String keyword, int parameter);

		void begingroup();

		void endgroup(Dictionary oldState);

		void close();
	}

	/**
	 * This data-sink class is used to implement ignored destinations (e.g. {\*\blegga blah blah blah} ) It accepts all keywords and text but does nothing with
	 * them.
	 */
	class DiscardingDestination implements Destination
	{
		public void handleBinaryBlob(byte[] data)
		{
			/* Discard binary blobs. */
		}

		public void handleText(String text)
		{
			/* Discard text. */
		}

		public boolean handleKeyword(String text)
		{
			/* Accept and discard keywords. */
			return true;
		}

		public boolean handleKeyword(String text, int parameter)
		{
			/* Accept and discard parameterized keywords. */
			return true;
		}

		public void begingroup()
		{
			/*
			 * Ignore groups --- the RTFReader will keep track of the current group level as necessary
			 */
		}

		public void endgroup(Dictionary oldState)
		{
			/* Ignore groups */
		}

		public void close()
		{
			/* No end-of-destination cleanup needed */
		}
	}

	/**
	 * Reads the fonttbl group, inserting fonts into the RTFReader's fontTable dictionary.
	 */
	class FonttblDestination implements Destination
	{
		int nextFontNumber;
		String nextFontFamily;

		public void handleBinaryBlob(byte[] data)
		{ /* Discard binary blobs. */
		}

		/*
		 * TODO do these routines work correctly if a write buffer divides a font name? (Probably not. Should allow for it as rare case)
		 */
		public void handleText(String text)
		{
			int semicolon = text.indexOf(';');
			String fontName;
			Object fontNum; /* an Integer, but we don't care */

			if (semicolon > 0) fontName = text.substring(0, semicolon);
			else fontName = text;

			/* TODO: do something with the font family. */

			fontTable.put(new Integer(nextFontNumber), fontName);

			nextFontNumber = -1;
			nextFontFamily = null;
			return;
		}

		public boolean handleKeyword(String keyword)
		{
			if (keyword.charAt(0) == 'f')
			{
				nextFontFamily = keyword.substring(1);
				return true;
			}

			return false;
		}

		public boolean handleKeyword(String keyword, int parameter)
		{
			if (keyword.equals("f")) { //$NON-NLS-1$
				nextFontNumber = parameter;
				return true;
			}

			return false;
		}

		/* Groups are irrelevant. */
		public void begingroup()
		{
		}

		public void endgroup(Dictionary oldState)
		{
		}

		/*
		 * currently, the only thing we do when the font table ends is dump its contents to the debugging log.
		 */
		public void close()
		{
			Enumeration nums = fontTable.keys();
			warning("Done reading font table."); //$NON-NLS-1$
			while (nums.hasMoreElements())
			{
				Integer num = (Integer)nums.nextElement();
				warning("Number " + num + ": " + fontTable.get(num)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Reads the colortbl group. Upon end-of-group, the RTFReader's color table is set to an array containing the read colors.
	 */
	class ColortblDestination implements Destination
	{
		int red, green, blue;
		Vector proTemTable;

		public ColortblDestination()
		{
			red = 0;
			green = 0;
			blue = 0;
			proTemTable = new Vector();
		}

		public void handleText(String text)
		{
			int index = 0;

			for (index = 0; index < text.length(); index++)
			{
				if (text.charAt(index) == ';')
				{
					Color newColor;
					newColor = new Color(red, green, blue);
					proTemTable.addElement(newColor);
				}
			}
		}

		public void close()
		{
			int count = proTemTable.size();
			warning("Done reading color table, " + count + " entries."); //$NON-NLS-1$ //$NON-NLS-2$
			colorTable = new Color[count];
			proTemTable.copyInto(colorTable);
		}

		public boolean handleKeyword(String keyword, int parameter)
		{
			if (keyword.equals("red")) //$NON-NLS-1$
			red = parameter;
			else if (keyword.equals("green")) //$NON-NLS-1$
			green = parameter;
			else if (keyword.equals("blue")) //$NON-NLS-1$
			blue = parameter;
			else return false;

			return true;
		}

		/* Colortbls don't understand any parameterless keywords */
		public boolean handleKeyword(String keyword)
		{
			return false;
		}

		/* Groups are irrelevant. */
		public void begingroup()
		{
		}

		public void endgroup(Dictionary oldState)
		{
		}

		/* Shouldn't see any binary blobs ... */
		public void handleBinaryBlob(byte[] data)
		{
		}
	}

	/**
	 * Handles the stylesheet keyword. Styles are read and sorted into the three style arrays in the RTFReader.
	 */
	class StylesheetDestination extends DiscardingDestination implements Destination
	{
		Dictionary definedStyles;

		public StylesheetDestination()
		{
			definedStyles = new Hashtable();
		}

		@Override
		public void begingroup()
		{
			setRTFDestination(new StyleDefiningDestination());
		}

		@Override
		public void close()
		{
			Vector chrStyles, pgfStyles, secStyles;
			chrStyles = new Vector();
			pgfStyles = new Vector();
			secStyles = new Vector();
			Enumeration styles = definedStyles.elements();
			while (styles.hasMoreElements())
			{
				StyleDefiningDestination style;
				Style defined;
				style = (StyleDefiningDestination)styles.nextElement();
				defined = style.realize();
				warning("Style " + style.number + " (" + style.styleName + "): " + defined); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				String stype = (String)defined.getAttribute(Constants.StyleType);
				Vector toSet;
				if (stype.equals(Constants.STSection))
				{
					toSet = secStyles;
				}
				else if (stype.equals(Constants.STCharacter))
				{
					toSet = chrStyles;
				}
				else
				{
					toSet = pgfStyles;
				}
				if (toSet.size() <= style.number) toSet.setSize(style.number + 1);
				toSet.setElementAt(defined, style.number);
			}
			if (!(chrStyles.isEmpty()))
			{
				Style[] styleArray = new Style[chrStyles.size()];
				chrStyles.copyInto(styleArray);
				characterStyles = styleArray;
			}
			if (!(pgfStyles.isEmpty()))
			{
				Style[] styleArray = new Style[pgfStyles.size()];
				pgfStyles.copyInto(styleArray);
				paragraphStyles = styleArray;
			}
			if (!(secStyles.isEmpty()))
			{
				Style[] styleArray = new Style[secStyles.size()];
				secStyles.copyInto(styleArray);
				sectionStyles = styleArray;
			}

/*
 * (old debugging code) int i, m; if (characterStyles != null) { m = characterStyles.length; for(i=0;i<m;i++)
 * warnings.println("chrStyle["+i+"]="+characterStyles[i]); } else warnings.println("No character styles."); if (paragraphStyles != null) { m =
 * paragraphStyles.length; for(i=0;i<m;i++) warnings.println("pgfStyle["+i+"]="+paragraphStyles[i]); } else warnings.println("No paragraph styles."); if
 * (sectionStyles != null) { m = characterStyles.length; for(i=0;i<m;i++) warnings.println("secStyle["+i+"]="+sectionStyles[i]); } else warnings.println("No
 * section styles.");
 */
		}

		/** This subclass handles an individual style */
		class StyleDefiningDestination extends AttributeTrackingDestination implements Destination
		{
			final int STYLENUMBER_NONE = 222;
			boolean additive;
			boolean characterStyle;
			boolean sectionStyle;
			public String styleName;
			public int number;
			int basedOn;
			int nextStyle;
			boolean hidden;

			Style realizedStyle;

			public StyleDefiningDestination()
			{
				additive = false;
				characterStyle = false;
				sectionStyle = false;
				styleName = null;
				number = 0;
				basedOn = STYLENUMBER_NONE;
				nextStyle = STYLENUMBER_NONE;
				hidden = false;
			}

			@Override
			public void handleText(String text)
			{
				if (styleName != null) styleName = styleName + text;
				else styleName = text;
			}

			@Override
			public void close()
			{
				if (styleName != null)
				{
					int semicolon = styleName.indexOf(';');
					if (semicolon > 0)
					{
						styleName = styleName.substring(0, semicolon);
					}
				}
				definedStyles.put(new Integer(number), this);
				super.close();
			}

			@Override
			public boolean handleKeyword(String keyword)
			{
				if (keyword.equals("additive")) { //$NON-NLS-1$
					additive = true;
					return true;
				}
				if (keyword.equals("shidden")) { //$NON-NLS-1$
					hidden = true;
					return true;
				}
				return super.handleKeyword(keyword);
			}

			@Override
			public boolean handleKeyword(String keyword, int parameter)
			{
				if (keyword.equals("s")) { //$NON-NLS-1$
					characterStyle = false;
					sectionStyle = false;
					number = parameter;
				}
				else if (keyword.equals("cs")) { //$NON-NLS-1$
					characterStyle = true;
					sectionStyle = false;
					number = parameter;
				}
				else if (keyword.equals("ds")) { //$NON-NLS-1$
					characterStyle = false;
					sectionStyle = true;
					number = parameter;
				}
				else if (keyword.equals("sbasedon")) { //$NON-NLS-1$
					basedOn = parameter;
				}
				else if (keyword.equals("snext")) { //$NON-NLS-1$
					nextStyle = parameter;
				}
				else
				{
					return super.handleKeyword(keyword, parameter);
				}
				return true;
			}

			public Style realize()
			{
				Style basis = null;
				Style next = null;

				if (realizedStyle != null) return realizedStyle;

				if (basedOn != STYLENUMBER_NONE)
				{
					StyleDefiningDestination styleDest;
					styleDest = (StyleDefiningDestination)definedStyles.get(new Integer(basedOn));
					if (styleDest != null)
					{
						basis = styleDest.realize();
					}
				}

				/*
				 * NB: Swing StyleContext doesn't allow distinct styles with the same name; RTF apparently does. This may confuse the user.
				 */
				realizedStyle = target.addStyle(styleName, basis);

				if (characterStyle)
				{
					realizedStyle.addAttributes(currentTextAttributes());
					realizedStyle.addAttribute(Constants.StyleType, Constants.STCharacter);
				}
				else if (sectionStyle)
				{
					realizedStyle.addAttributes(currentSectionAttributes());
					realizedStyle.addAttribute(Constants.StyleType, Constants.STSection);
				}
				else
				{ /* must be a paragraph style */
					realizedStyle.addAttributes(currentParagraphAttributes());
					realizedStyle.addAttribute(Constants.StyleType, Constants.STParagraph);
				}

				if (nextStyle != STYLENUMBER_NONE)
				{
					StyleDefiningDestination styleDest;
					styleDest = (StyleDefiningDestination)definedStyles.get(new Integer(nextStyle));
					if (styleDest != null)
					{
						next = styleDest.realize();
					}
				}

				if (next != null) realizedStyle.addAttribute(Constants.StyleNext, next);
				realizedStyle.addAttribute(Constants.StyleAdditive, new Boolean(additive));
				realizedStyle.addAttribute(Constants.StyleHidden, new Boolean(hidden));

				return realizedStyle;
			}
		}
	}

	/**
	 * Handles the info group. Currently no info keywords are recognized so this is a subclass of DiscardingDestination.
	 */
	class InfoDestination extends DiscardingDestination implements Destination
	{
	}

	/**
	 * RTFReader.TextHandlingDestination is an abstract RTF destination which simply tracks the attributes specified by the RTF control words in internal form
	 * and can produce acceptable AttributeSets for the current character, paragraph, and section attributes. It is up to the subclasses to determine what is
	 * done with the actual text.
	 */
	abstract class AttributeTrackingDestination implements Destination
	{
		/**
		 * This is the "chr" element of parserState, cached for more efficient use
		 */
		MutableAttributeSet characterAttributes;
		/**
		 * This is the "pgf" element of parserState, cached for more efficient use
		 */
		MutableAttributeSet paragraphAttributes;
		/**
		 * This is the "sec" element of parserState, cached for more efficient use
		 */
		MutableAttributeSet sectionAttributes;

		public AttributeTrackingDestination()
		{
			characterAttributes = rootCharacterAttributes();
			parserState.put("chr", characterAttributes); //$NON-NLS-1$
			paragraphAttributes = rootParagraphAttributes();
			parserState.put("pgf", paragraphAttributes); //$NON-NLS-1$
			sectionAttributes = rootSectionAttributes();
			parserState.put("sec", sectionAttributes); //$NON-NLS-1$
		}

		abstract public void handleText(String text);

		public void handleBinaryBlob(byte[] data)
		{
			/*
			 * This should really be in TextHandlingDestination, but since *nobody* does anything with binary blobs, this is more convenient.
			 */
			warning("Unexpected binary data in RTF file."); //$NON-NLS-1$
		}

		public void begingroup()
		{
			AttributeSet characterParent = currentTextAttributes();
			AttributeSet paragraphParent = currentParagraphAttributes();
			AttributeSet sectionParent = currentSectionAttributes();

			/*
			 * It would probably be more efficient to use the resolver property of the attributes set for implementing rtf groups, but that's needed for styles.
			 */

			/* update the cached attribute dictionaries */
			characterAttributes = new SimpleAttributeSet();
			characterAttributes.addAttributes(characterParent);
			parserState.put("chr", characterAttributes); //$NON-NLS-1$

			paragraphAttributes = new SimpleAttributeSet();
			paragraphAttributes.addAttributes(paragraphParent);
			parserState.put("pgf", paragraphAttributes); //$NON-NLS-1$

			sectionAttributes = new SimpleAttributeSet();
			sectionAttributes.addAttributes(sectionParent);
			parserState.put("sec", sectionAttributes); //$NON-NLS-1$
		}

		public void endgroup(Dictionary oldState)
		{
			characterAttributes = (MutableAttributeSet)parserState.get("chr"); //$NON-NLS-1$
			paragraphAttributes = (MutableAttributeSet)parserState.get("pgf"); //$NON-NLS-1$
			sectionAttributes = (MutableAttributeSet)parserState.get("sec"); //$NON-NLS-1$
		}

		public void close()
		{
		}

		public boolean handleKeyword(String keyword)
		{
			if (keyword.equals("ulnone")) { //$NON-NLS-1$
				return handleKeyword("ul", 0); //$NON-NLS-1$
			}

			{
				Object item = straightforwardAttributes.get(keyword);
				if (item != null)
				{
					RTFAttribute attr = (RTFAttribute)item;
					boolean ok;

					switch (attr.domain())
					{
						case RTFAttribute.D_CHARACTER :
							ok = attr.set(characterAttributes);
							break;
						case RTFAttribute.D_PARAGRAPH :
							ok = attr.set(paragraphAttributes);
							break;
						case RTFAttribute.D_SECTION :
							ok = attr.set(sectionAttributes);
							break;
						case RTFAttribute.D_META :
							mockery.backing = parserState;
							ok = attr.set(mockery);
							mockery.backing = null;
							break;
						case RTFAttribute.D_DOCUMENT :
							ok = attr.set(documentAttributes);
							break;
						default :
							/* should never happen */
							ok = false;
							break;
					}
					if (ok) return true;
				}
			}


			if (keyword.equals("plain")) { //$NON-NLS-1$
				resetCharacterAttributes();
				return true;
			}

			if (keyword.equals("pard")) { //$NON-NLS-1$
				resetParagraphAttributes();
				return true;
			}

			if (keyword.equals("sectd")) { //$NON-NLS-1$
				resetSectionAttributes();
				return true;
			}

			return false;
		}

		public boolean handleKeyword(String keyword, int parameter)
		{
			boolean booleanParameter = (parameter != 0);

			if (keyword.equals("fc")) //$NON-NLS-1$
			keyword = "cf"; /* whatEVER, dude. *///$NON-NLS-1$

			if (keyword.equals("f")) { //$NON-NLS-1$
				parserState.put(keyword, new Integer(parameter));
				return true;
			}
			if (keyword.equals("cf")) { //$NON-NLS-1$
				parserState.put(keyword, new Integer(parameter));
				return true;
			}

			{
				Object item = straightforwardAttributes.get(keyword);
				if (item != null)
				{
					RTFAttribute attr = (RTFAttribute)item;
					boolean ok;

					switch (attr.domain())
					{
						case RTFAttribute.D_CHARACTER :
							ok = attr.set(characterAttributes, parameter);
							break;
						case RTFAttribute.D_PARAGRAPH :
							ok = attr.set(paragraphAttributes, parameter);
							break;
						case RTFAttribute.D_SECTION :
							ok = attr.set(sectionAttributes, parameter);
							break;
						case RTFAttribute.D_META :
							mockery.backing = parserState;
							ok = attr.set(mockery, parameter);
							mockery.backing = null;
							break;
						case RTFAttribute.D_DOCUMENT :
							ok = attr.set(documentAttributes, parameter);
							break;
						default :
							/* should never happen */
							ok = false;
							break;
					}
					if (ok) return true;
				}
			}

			if (keyword.equals("fs")) { //$NON-NLS-1$
				StyleConstants.setFontSize(characterAttributes, (parameter / 2));
				return true;
			}

			/* TODO: superscript/subscript */

			if (keyword.equals("sl")) { //$NON-NLS-1$
				if (parameter == 1000)
				{ /* magic value! */
					characterAttributes.removeAttribute(StyleConstants.LineSpacing);
				}
				else
				{
					/*
					 * TODO: The RTF sl attribute has special meaning if it's negative. Make sure that SwingText has the same special meaning, or find a way to
					 * imitate that. When SwingText handles this, also recognize the slmult keyword.
					 */
					StyleConstants.setLineSpacing(characterAttributes, parameter / 20f);
				}
				return true;
			}

			/* TODO: Other kinds of underlining */

			if (keyword.equals("tx") || keyword.equals("tb")) { //$NON-NLS-1$ //$NON-NLS-2$
				float tabPosition = parameter / 20f;
				int tabAlignment, tabLeader;
				Number item;

				tabAlignment = TabStop.ALIGN_LEFT;
				item = (Number)(parserState.get("tab_alignment")); //$NON-NLS-1$
				if (item != null) tabAlignment = item.intValue();
				tabLeader = TabStop.LEAD_NONE;
				item = (Number)(parserState.get("tab_leader")); //$NON-NLS-1$
				if (item != null) tabLeader = item.intValue();
				if (keyword.equals("tb")) //$NON-NLS-1$
				tabAlignment = TabStop.ALIGN_BAR;

				parserState.remove("tab_alignment"); //$NON-NLS-1$
				parserState.remove("tab_leader"); //$NON-NLS-1$

				TabStop newStop = new TabStop(tabPosition, tabAlignment, tabLeader);
				Dictionary tabs;
				Integer stopCount;

				tabs = (Dictionary)parserState.get("_tabs"); //$NON-NLS-1$
				if (tabs == null)
				{
					tabs = new Hashtable();
					parserState.put("_tabs", tabs); //$NON-NLS-1$
					stopCount = new Integer(1);
				}
				else
				{
					stopCount = (Integer)tabs.get("stop count"); //$NON-NLS-1$
					stopCount = new Integer(1 + stopCount.intValue());
				}
				tabs.put(stopCount, newStop);
				tabs.put("stop count", stopCount); //$NON-NLS-1$
				parserState.remove("_tabs_immutable"); //$NON-NLS-1$

				return true;
			}

			if (keyword.equals("s") && //$NON-NLS-1$
				paragraphStyles != null)
			{
				parserState.put("paragraphStyle", paragraphStyles[parameter]); //$NON-NLS-1$
				return true;
			}

			if (keyword.equals("cs") && //$NON-NLS-1$
				characterStyles != null)
			{
				parserState.put("characterStyle", characterStyles[parameter]); //$NON-NLS-1$
				return true;
			}

			if (keyword.equals("ds") && //$NON-NLS-1$
				sectionStyles != null)
			{
				parserState.put("sectionStyle", sectionStyles[parameter]); //$NON-NLS-1$
				return true;
			}

			return false;
		}

		/**
		 * Returns a new MutableAttributeSet containing the default character attributes
		 */
		protected MutableAttributeSet rootCharacterAttributes()
		{
			MutableAttributeSet set = new SimpleAttributeSet();

			/* TODO: default font */

			StyleConstants.setItalic(set, false);
			StyleConstants.setBold(set, false);
			StyleConstants.setUnderline(set, false);
			StyleConstants.setForeground(set, defaultColor());

			return set;
		}

		/**
		 * Returns a new MutableAttributeSet containing the default paragraph attributes
		 */
		protected MutableAttributeSet rootParagraphAttributes()
		{
			MutableAttributeSet set = new SimpleAttributeSet();

			StyleConstants.setLeftIndent(set, 0f);
			StyleConstants.setRightIndent(set, 0f);
			StyleConstants.setFirstLineIndent(set, 0f);

			/* TODO: what should this be, really? */
			set.setResolveParent(target.getStyle(StyleContext.DEFAULT_STYLE));

			return set;
		}

		/**
		 * Returns a new MutableAttributeSet containing the default section attributes
		 */
		protected MutableAttributeSet rootSectionAttributes()
		{
			MutableAttributeSet set = new SimpleAttributeSet();

			return set;
		}

		/**
		 * Calculates the current text (character) attributes in a form suitable for SwingText from the current parser state.
		 * 
		 * @returns a new MutableAttributeSet containing the text attributes.
		 */
		MutableAttributeSet currentTextAttributes()
		{
			MutableAttributeSet attributes = new SimpleAttributeSet(characterAttributes);
			Integer fontnum;
			Integer stateItem;

			/* figure out the font name */
			/*
			 * TODO: catch exceptions for undefined attributes, bad font indices, etc.? (as it stands, it is the caller's job to clean up after corrupt RTF)
			 */
			fontnum = (Integer)parserState.get("f"); //$NON-NLS-1$
			/* note setFontFamily() can not handle a null font */
			String fontFamily;
			if (fontnum != null) fontFamily = (String)fontTable.get(fontnum);
			else fontFamily = null;
			if (fontFamily != null) StyleConstants.setFontFamily(attributes, fontFamily);
			else attributes.removeAttribute(StyleConstants.FontFamily);

			if (colorTable != null)
			{
				stateItem = (Integer)parserState.get("cf"); //$NON-NLS-1$
				if (stateItem != null)
				{
					Color fg = colorTable[stateItem.intValue()];
					StyleConstants.setForeground(attributes, fg);
				}
				else
				{
					/* AttributeSet dies if you set a value to null */
					attributes.removeAttribute(StyleConstants.Foreground);
				}
			}

			if (colorTable != null)
			{
				stateItem = (Integer)parserState.get("cb"); //$NON-NLS-1$
				if (stateItem != null)
				{
					Color bg = colorTable[stateItem.intValue()];
					attributes.addAttribute(StyleConstants.Background, bg);
				}
				else
				{
					/* AttributeSet dies if you set a value to null */
					attributes.removeAttribute(StyleConstants.Background);
				}
			}

			Style characterStyle = (Style)parserState.get("characterStyle"); //$NON-NLS-1$
			if (characterStyle != null) attributes.setResolveParent(characterStyle);

			/* Other attributes are maintained directly in "attributes" */

			return attributes;
		}

		/**
		 * Calculates the current paragraph attributes (with keys as given in StyleConstants) from the current parser state.
		 * 
		 * @returns a newly created MutableAttributeSet.
		 * @see StyleConstants
		 */
		MutableAttributeSet currentParagraphAttributes()
		{
			/* NB if there were a mutableCopy() method we should use it */
			MutableAttributeSet bld = new SimpleAttributeSet(paragraphAttributes);

			Integer stateItem;

			/** * Tab stops ** */
			TabStop tabs[];

			tabs = (TabStop[])parserState.get("_tabs_immutable"); //$NON-NLS-1$
			if (tabs == null)
			{
				Dictionary workingTabs = (Dictionary)parserState.get("_tabs"); //$NON-NLS-1$
				if (workingTabs != null)
				{
					int count = ((Integer)workingTabs.get("stop count")).intValue(); //$NON-NLS-1$
					tabs = new TabStop[count];
					for (int ix = 1; ix <= count; ix++)
						tabs[ix - 1] = (TabStop)workingTabs.get(new Integer(ix));
					parserState.put("_tabs_immutable", tabs); //$NON-NLS-1$
				}
			}
			if (tabs != null) bld.addAttribute(Constants.Tabs, tabs);

			Style paragraphStyle = (Style)parserState.get("paragraphStyle"); //$NON-NLS-1$
			if (paragraphStyle != null) bld.setResolveParent(paragraphStyle);

			return bld;
		}

		/**
		 * Calculates the current section attributes from the current parser state.
		 * 
		 * @returns a newly created MutableAttributeSet.
		 */
		public AttributeSet currentSectionAttributes()
		{
			MutableAttributeSet attributes = new SimpleAttributeSet(sectionAttributes);

			Style sectionStyle = (Style)parserState.get("sectionStyle"); //$NON-NLS-1$
			if (sectionStyle != null) attributes.setResolveParent(sectionStyle);

			return attributes;
		}

		/**
		 * Resets the filter's internal notion of the current character attributes to their default values. Invoked to handle the \plain keyword.
		 */
		protected void resetCharacterAttributes()
		{
			handleKeyword("f", 0); //$NON-NLS-1$
			handleKeyword("cf", 0); //$NON-NLS-1$

			handleKeyword("fs", 24); /* 12 pt. *///$NON-NLS-1$

			Enumeration attributes = straightforwardAttributes.elements();
			while (attributes.hasMoreElements())
			{
				RTFAttribute attr = (RTFAttribute)attributes.nextElement();
				if (attr.domain() == RTFAttribute.D_CHARACTER) attr.setDefault(characterAttributes);
			}

			handleKeyword("sl", 1000); //$NON-NLS-1$

			parserState.remove("characterStyle"); //$NON-NLS-1$
		}

		/**
		 * Resets the filter's internal notion of the current paragraph's attributes to their default values. Invoked to handle the \pard keyword.
		 */
		protected void resetParagraphAttributes()
		{
			parserState.remove("_tabs"); //$NON-NLS-1$
			parserState.remove("_tabs_immutable"); //$NON-NLS-1$
			parserState.remove("paragraphStyle"); //$NON-NLS-1$

			StyleConstants.setAlignment(paragraphAttributes, StyleConstants.ALIGN_LEFT);

			Enumeration attributes = straightforwardAttributes.elements();
			while (attributes.hasMoreElements())
			{
				RTFAttribute attr = (RTFAttribute)attributes.nextElement();
				if (attr.domain() == RTFAttribute.D_PARAGRAPH) attr.setDefault(characterAttributes);
			}
		}

		/**
		 * Resets the filter's internal notion of the current section's attributes to their default values. Invoked to handle the \sectd keyword.
		 */
		protected void resetSectionAttributes()
		{
			Enumeration attributes = straightforwardAttributes.elements();
			while (attributes.hasMoreElements())
			{
				RTFAttribute attr = (RTFAttribute)attributes.nextElement();
				if (attr.domain() == RTFAttribute.D_SECTION) attr.setDefault(characterAttributes);
			}

			parserState.remove("sectionStyle"); //$NON-NLS-1$
		}
	}

	/**
	 * RTFReader.TextHandlingDestination provides basic text handling functionality. Subclasses must implement:
	 * <dl>
	 * <dt>deliverText()
	 * <dd>to handle a run of text with the same attributes
	 * <dt>finishParagraph()
	 * <dd>to end the current paragraph and set the paragraph's attributes
	 * <dt>endSection()
	 * <dd>to end the current section
	 * </dl>
	 */
	abstract class TextHandlingDestination extends AttributeTrackingDestination implements Destination
	{
		/**
		 * <code>true</code> if the reader has not just finished a paragraph; false upon startup
		 */
		boolean inParagraph;

		public TextHandlingDestination()
		{
			super();
			inParagraph = false;
		}

		@Override
		public void handleText(String text)
		{
			if (!inParagraph) beginParagraph();

			deliverText(text, currentTextAttributes());
		}

		abstract void deliverText(String text, AttributeSet characterAttributes);

		@Override
		public void close()
		{
			if (inParagraph) inParagraph = false;
			super.close();
		}

		@Override
		public boolean handleKeyword(String keyword)
		{
			if (keyword.equals("\r") || keyword.equals("\n")) { //$NON-NLS-1$ //$NON-NLS-2$
				keyword = "par"; //$NON-NLS-1$
			}

			if (keyword.equals("par")) { //$NON-NLS-1$
//	    warnings.println("Ending paragraph.");
				endParagraph();
				return true;
			}

			if (keyword.equals("sect")) { //$NON-NLS-1$
//	    warnings.println("Ending section.");
				endSection();
				return true;
			}

			return super.handleKeyword(keyword);
		}

		protected void beginParagraph()
		{
			inParagraph = true;
		}

		protected void endParagraph()
		{
			AttributeSet pgfAttributes = currentParagraphAttributes();
			AttributeSet chrAttributes = currentTextAttributes();
			finishParagraph(pgfAttributes, chrAttributes);
			inParagraph = false;
		}

		abstract void finishParagraph(AttributeSet pgfA, AttributeSet chrA);

		abstract void endSection();
	}

	/**
	 * RTFReader.DocumentDestination is a concrete subclass of TextHandlingDestination which appends the text to the StyledDocument given by the
	 * <code>target</code> ivar of the containing RTFReader.
	 */
	class DocumentDestination extends TextHandlingDestination implements Destination
	{
		@Override
		public void deliverText(String text, AttributeSet characterAttributes)
		{
			try
			{
				target.insertString(target.getLength(), text, currentTextAttributes());
			}
			catch (BadLocationException ble)
			{
				/* This shouldn't be able to happen, of course */
				/* TODO is InternalError the correct error to throw? */
				throw new InternalError(ble.getMessage());
			}
		}

		@Override
		public void finishParagraph(AttributeSet pgfAttributes, AttributeSet chrAttributes)
		{
			int pgfEndPosition = target.getLength();
			try
			{
				target.insertString(pgfEndPosition, "\n", chrAttributes); //$NON-NLS-1$
				target.setParagraphAttributes(pgfEndPosition, 1, pgfAttributes, true);
			}
			catch (BadLocationException ble)
			{
				/* This shouldn't be able to happen, of course */
				/* TODO is InternalError the correct error to throw? */
				throw new InternalError(ble.getMessage());
			}
		}

		@Override
		public void endSection()
		{
			/* If we implemented sections, we'd end 'em here */
		}
	}

}
