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
package com.servoy.j2db.util.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TextAction;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.HTMLFactory;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.HTMLWriter;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.MinimalHTMLWriter;
import javax.swing.text.html.ObjectView;
import javax.swing.text.html.StyleSheet;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 */
public class FixedHTMLEditorKit extends StyledEditorKit implements ISupportAsyncLoading
{
	private final IApplication application;

	static class BasicHTMLViewFactory extends HTMLEditorKit.HTMLFactory
	{
		private final MediaURLStreamHandler streamHandler;

		public BasicHTMLViewFactory(MediaURLStreamHandler streamHandler)
		{
			this.streamHandler = streamHandler;
		}

		@Override
		public View create(Element elem)
		{
			View view = null;
			AttributeSet attributes = elem.getAttributes();
			if (attributes != null)
			{
				AttributeSet anchor = (AttributeSet)attributes.getAttribute(HTML.Tag.A);
				if (anchor != null)
				{
					String href = (String)anchor.getAttribute(HTML.Attribute.HREF);
					Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
					if (o instanceof HTML.Tag)
					{
						HTML.Tag kind = (HTML.Tag)o;
						if (kind == HTML.Tag.CONTENT) view = new HyperlinkInlineView(elem, href);
						else if (kind == HTML.Tag.IMG) view = new HyperlinkImageView(elem, href, streamHandler);
						else view = super.create(elem);
					}
				}
			}
			if (view == null)
			{
				Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
				if (o == HTML.Tag.IMG)
				{
					view = new UrlStreamHandlerImageView(elem, streamHandler);
				}
				else
				{
					view = super.create(elem);
				}
			}
			if (view instanceof ImageView)
			{
				((ImageView)view).setLoadsSynchronously(true);
			}
			return view;
		}
	}

	public static class BoldAction extends StyledTextAction
	{
		public BoldAction()
		{
			super("bold-font");
		}

		public void actionPerformed(ActionEvent e)
		{
			JEditorPane editor = getEditor(e);
			if (editor != null)
			{
				StyledEditorKit kit = getStyledEditorKit(editor);
				MutableAttributeSet attr = kit.getInputAttributes();
				boolean bold = (StyleConstants.isBold(attr)) ? false : true;
				SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setBold(sas, bold);
				setCharacterAttributes(editor, sas, false);
//				readAsynchronously(editor);
			}
		}
	}

	public static class ItalicAction extends StyledTextAction
	{
		public ItalicAction()
		{
			super("italic-font");
		}

		public void actionPerformed(ActionEvent e)
		{
			JEditorPane editor = getEditor(e);
			if (editor != null)
			{
				StyledEditorKit kit = getStyledEditorKit(editor);
				MutableAttributeSet attr = kit.getInputAttributes();
				boolean italic = (StyleConstants.isItalic(attr)) ? false : true;
				SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setItalic(sas, italic);
				setCharacterAttributes(editor, sas, false);
//				readAsynchronously(editor);
			}
		}
	}

	public static class UnderlineAction extends StyledTextAction
	{
		public UnderlineAction()
		{
			super("underline-font");
		}

		public void actionPerformed(ActionEvent e)
		{
			JEditorPane editor = getEditor(e);
			if (editor != null)
			{
				StyledEditorKit kit = getStyledEditorKit(editor);
				MutableAttributeSet attr = kit.getInputAttributes();
				boolean underline = (StyleConstants.isUnderline(attr)) ? false : true;
				SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setUnderline(sas, underline);
				setCharacterAttributes(editor, sas, false);
//				readAsynchronously(editor);
			}
		}
	}

	static public class HyperlinkInlineView extends InlineView
	{
		private final String url;

		public HyperlinkInlineView(Element elem, String url)
		{
			super(elem);
			this.url = url;
		}

		@Override
		public void paint(Graphics g, Shape a)
		{
			Object hkey = null;
			Graphics2D g2d = null;
			try
			{
				if (g instanceof Graphics2D)
				{
					g2d = (Graphics2D)g;
					Iterator it = g2d.getRenderingHints().keySet().iterator();
					while (it.hasNext())
					{
						Object key = it.next();
						if ("HyperLinkKey".equals(key.toString()))
						{
							hkey = key;
						}
					}
					if (hkey != null) g2d.setRenderingHint((RenderingHints.Key)hkey, url);
				}
				super.paint(g, a);

			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			finally
			{
				if (g2d instanceof Graphics2D && hkey != null)
				{
					g2d.setRenderingHint((RenderingHints.Key)hkey, null);
				}
			}
		}
	}

	static public class UrlStreamHandlerImageView extends ImageView
	{
		private final MediaURLStreamHandler streamHandler;

		public UrlStreamHandlerImageView(Element elem, MediaURLStreamHandler streamHandler)
		{
			super(elem);
			this.streamHandler = streamHandler;
		}

		@Override
		public URL getImageURL()
		{
			String src = (String)getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
			if (src == null)
			{
				return null;
			}
			if (src.startsWith("media://"))
			{
				URL reference = ((HTMLDocument)getDocument()).getBase();
				try
				{
					return new URL(reference, src, streamHandler);
				}
				catch (MalformedURLException e)
				{
					return null;
				}
			}
			return super.getImageURL();
		}
	}

	static public class HyperlinkImageView extends UrlStreamHandlerImageView
	{
		private final String url;

		public HyperlinkImageView(Element elem, String url, MediaURLStreamHandler streamHandler)
		{
			super(elem, streamHandler);
			this.url = url;
		}

		@Override
		public void paint(Graphics g, Shape a)
		{
			Object hkey = null;
			Graphics2D g2d = null;
			try
			{
				if (g instanceof Graphics2D)
				{
					g2d = (Graphics2D)g;
					Iterator it = g2d.getRenderingHints().keySet().iterator();
					while (it.hasNext())
					{
						Object key = it.next();
						if ("HyperLinkKey".equals(key.toString()))
						{
							hkey = key;
						}
					}
					if (hkey != null) g2d.setRenderingHint((RenderingHints.Key)hkey, url);
				}
				super.paint(g, a);

			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			finally
			{
				if (g2d instanceof Graphics2D && hkey != null)
				{
					g2d.setRenderingHint((RenderingHints.Key)hkey, null);
				}
			}
		}


	}

	/**
	 * Constructs an HTMLEditorKit, creates a StyleContext, and loads the style sheet.
	 */
	public FixedHTMLEditorKit(IApplication application)
	{
		this.application = application;

	}

	private static NavigateLinkAction nextLinkAction = new NavigateLinkAction("next-link-action"); //$NON-NLS-1$

	private static NavigateLinkAction previousLinkAction = new NavigateLinkAction("previous-link-action"); //$NON-NLS-1$

	private static ActivateLinkAction activateLinkAction = new ActivateLinkAction("activate-link-action"); //$NON-NLS-1$

	private LinkController linkHandler = new LinkController();

	/** HTML used when inserting tables. */
	private static final String INSERT_TABLE_HTML = "<table border=1><tr><td>#</td></tr></table>"; //$NON-NLS-1$

	/** HTML used when inserting unordered lists. */
	private static final String INSERT_UL_HTML = "<ul><li>#</li></ul>"; //$NON-NLS-1$

	/** HTML used when inserting ordered lists. */
	private static final String INSERT_OL_HTML = "<ol><li>#</li></ol>"; //$NON-NLS-1$

	/** HTML used when inserting hr. */
	private static final String INSERT_HR_HTML = "<hr>"; //$NON-NLS-1$

	/** HTML used when inserting pre. */
	private static final String INSERT_PRE_HTML = "<pre>#</pre>"; //$NON-NLS-1$
	private static final String INSERT_SUB_HTML = "<sub>#</sub>"; //$NON-NLS-1$
	private static final String INSERT_SUP_HTML = "<sup>#</sup>"; //$NON-NLS-1$

	/** alignment attribute insertion */
	private static final String LEFT_ALIGN_ATTRIBUTE = "style=\"text-align:left\""; //$NON-NLS-1$
	private static final String CENTER_ALIGN_ATTRIBUTE = "style=\"text-align:center\""; //$NON-NLS-1$
	private static final String RIGHT_ALIGN_ATTRIBUTE = "style=\"text-align:right\""; //$NON-NLS-1$


	private static final Action[] defaultActions = { new InsertHTMLTextAction("InsertTable", INSERT_TABLE_HTML, HTML.Tag.BODY, HTML.Tag.TABLE), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertTableRow", INSERT_TABLE_HTML, HTML.Tag.TABLE, HTML.Tag.TR, HTML.Tag.BODY, HTML.Tag.TABLE), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertTableDataCell", INSERT_TABLE_HTML, HTML.Tag.TR, HTML.Tag.TD, HTML.Tag.BODY, HTML.Tag.TABLE), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertUnorderedList", INSERT_UL_HTML, HTML.Tag.BODY, HTML.Tag.UL), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertUnorderedListItem", INSERT_UL_HTML, HTML.Tag.UL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.UL), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertOrderedList", INSERT_OL_HTML, HTML.Tag.BODY, HTML.Tag.OL), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertOrderedListItem", INSERT_OL_HTML, HTML.Tag.OL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.OL), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertPre", INSERT_PRE_HTML, HTML.Tag.BODY, HTML.Tag.PRE), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertSub", INSERT_SUB_HTML, HTML.Tag.BODY, HTML.Tag.SUB), //$NON-NLS-1$
	new InsertHTMLTextAction("InsertSup", INSERT_SUP_HTML, HTML.Tag.BODY, HTML.Tag.SUP), //$NON-NLS-1$
	new InsertAttributeAction("center-justify", CENTER_ALIGN_ATTRIBUTE), //$NON-NLS-1$
	new InsertAttributeAction("left-justify", LEFT_ALIGN_ATTRIBUTE), //$NON-NLS-1$
	new InsertAttributeAction("right-justify", RIGHT_ALIGN_ATTRIBUTE), //$NON-NLS-1$
	new StyledInsertBreakAction(), new InsertHRAction(), nextLinkAction, previousLinkAction, activateLinkAction, new BoldAction(), new ItalicAction(), new UnderlineAction()
	//		new ActivateLinkAction("activate-link-action")
	};

	@Override
	public Action[] getActions()
	{
		return TextAction.augmentList(super.getActions(), FixedHTMLEditorKit.defaultActions);
	}

	/**
	 * needed after each operation into the document; after document is updated by different actions, this method must be called
	 * 
	 * @param editor the editor that holds the (HTML) Document
	 */
	private static void readAsynchronously(JEditorPane editor)
	{
		if (editor == null) return;
		String content = editor.getText();
		int selectionStart = editor.getSelectionStart();
		try
		{
			editor.setText(content);
			ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
			editor.read(bais, null);
			if (selectionStart != -1 && editor.getDocument() != null && editor.getDocument().getLength() > selectionStart)
			{
				editor.setSelectionStart(selectionStart);
			}
		}
		catch (IOException ioe)
		{
			throw new RuntimeException("Unable to insert: " + ioe);
		}
		catch (ClassCastException cce)
		{
		}
	}

	/**
	 * Class to watch the associated component and fire hyperlink events on it when appropriate.
	 */
	public static class LinkController extends MouseAdapter implements MouseMotionListener, Serializable
	{
		private Element curElem = null;
		/**
		 * If true, the current element (curElem) represents an image.
		 */
		private boolean curElemImage = false;
		private String href = null;
		/**
		 * This is used by viewToModel to avoid allocing a new array each time.
		 */
		private final Position.Bias[] bias = new Position.Bias[1];
		/**
		 * Current offset.
		 */
		private int curOffset;
		private Point clickPoint;

		// cannot use mouseClicked callback as we must also track mouse events that
		// are repost from EditListUI, and that is ignoring reposting mouseClicked events
		// so, we try to figure out if it is a click from mousePressed & mouseReleased

		@Override
		public void mousePressed(MouseEvent e)
		{
			clickPoint = e.getPoint();
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			// sometimes EditListUI will ignore reposting the mousePressed event, so also
			// handle the case when we have no clickPoint
			if (clickPoint == null || clickPoint.equals(e.getPoint())) // it is a mouse click
			{
				handleMouseClick(e);
			}
			clickPoint = null;
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			clickPoint = null;
		}

		/**
		 * Called for a mouse click event. If the component is read-only (ie a browser) then the clicked event is used to drive an attempt to follow the
		 * reference specified by a link.
		 * 
		 * @param e the mouse event
		 * @see MouseListener#mouseClicked
		 */
		private void handleMouseClick(MouseEvent e)
		{
			JEditorPane editor = (JEditorPane)e.getSource();

			if (!editor.isEditable())
			{
				Point pt = new Point(e.getX(), e.getY());
				int pos = editor.viewToModel(pt);
				if (pos >= 0)
				{
					activateLink(pos, editor, e.getX(), e.getY());
				}
			}
		}

		// ignore the drags
		@Override
		public void mouseDragged(MouseEvent e)
		{
		}

		// track the moving of the mouse.
		@Override
		public void mouseMoved(MouseEvent e)
		{
			JEditorPane editor = (JEditorPane)e.getSource();
			FixedHTMLEditorKit kit = (FixedHTMLEditorKit)editor.getEditorKit();
			boolean adjustCursor = true;
			Cursor newCursor;
			if (!editor.isEditable())
			{
				newCursor = kit.getDefaultCursor();
				Point pt = new Point(e.getX(), e.getY());
				int pos = editor.getUI().viewToModel(editor, pt, bias);
				if (bias[0] == Position.Bias.Backward && pos > 0)
				{
					pos--;
				}
				if (pos >= 0 && (editor.getDocument() instanceof HTMLDocument))
				{
					HTMLDocument hdoc = (HTMLDocument)editor.getDocument();
					Element elem = hdoc.getCharacterElement(pos);
					if (!doesElementContainLocation(editor, elem, pos, e.getX(), e.getY()))
					{
						elem = null;
					}
					if (curElem != elem || curElemImage)
					{
						Element lastElem = curElem;
						curElem = elem;
						String href = null;
						curElemImage = false;
						if (elem != null)
						{
							AttributeSet a = elem.getAttributes();
							AttributeSet anchor = (AttributeSet)a.getAttribute(HTML.Tag.A);
							if (anchor == null)
							{
								curElemImage = (a.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMG);
								if (curElemImage)
								{
									href = getMapHREF(editor, hdoc, elem, a, pos, e.getX(), e.getY());
								}
							}
							else
							{
								href = (String)anchor.getAttribute(HTML.Attribute.HREF);
							}
						}

						if (href != this.href)
						{
							// reference changed, fire event(s)
							fireEvents(editor, hdoc, href, lastElem);
							this.href = href;
							if (href != null)
							{
								newCursor = kit.getLinkCursor();
							}
						}
						else
						{
							adjustCursor = false;
						}
					}
					else
					{
						adjustCursor = false;
					}
					curOffset = pos;
				}
			}
			else newCursor = kit.getEditCursor();
			if (adjustCursor && editor.getCursor() != newCursor)
			{
				editor.setCursor(newCursor);
			}
		}

		/**
		 * Returns a string anchor if the passed in element has a USEMAP that contains the passed in location.
		 */
		private String getMapHREF(JEditorPane html, HTMLDocument hdoc, Element elem, AttributeSet attr, int offset, int x, int y)
		{
			//			Object useMap = attr.getAttribute(HTML.Attribute.USEMAP);
			//			if (useMap != null && (useMap instanceof String)) {
			//				Map m = hdoc.getMap((String)useMap);
			//				if (m != null && offset < hdoc.getLength()) {
			//					Rectangle bounds;
			//					TextUI ui = html.getUI();
			//					try {
			//						Shape lBounds = ui.modelToView(html, offset,
			//												   Position.Bias.Forward);
			//						Shape rBounds = ui.modelToView(html, offset + 1,
			//												   Position.Bias.Backward);
			//						bounds = lBounds.getBounds();
			//						bounds.add((rBounds instanceof Rectangle) ?
			//									(Rectangle)rBounds : rBounds.getBounds());
			//					} catch (BadLocationException ble) {
			//						bounds = null;
			//					}
			//					if (bounds != null) {
			//						AttributeSet area = m.getArea(x - bounds.x,
			//													  y - bounds.y,
			//													  bounds.width,
			//													  bounds.height);
			//						if (area != null) {
			//							return (String)area.getAttribute(HTML.Attribute.
			//															 HREF);
			//						}
			//					}
			//				}
			//			}
			return null;
		}

		/**
		 * Returns true if the View representing <code>e</code> contains the location <code>x</code>, <code>y</code>. <code>offset</code> gives the
		 * offset into the Document to check for.
		 */
		private boolean doesElementContainLocation(JEditorPane editor, Element e, int offset, int x, int y)
		{
			if (e != null && offset > 0 && e.getStartOffset() == offset)
			{
				try
				{
					TextUI ui = editor.getUI();
					Shape s1 = ui.modelToView(editor, offset, Position.Bias.Forward);
					Rectangle r1 = (s1 instanceof Rectangle) ? (Rectangle)s1 : s1.getBounds();
					Shape s2 = ui.modelToView(editor, e.getEndOffset(), Position.Bias.Backward);
					Rectangle r2 = (s2 instanceof Rectangle) ? (Rectangle)s2 : s2.getBounds();
					r1.add(r2);
					return r1.contains(x, y);
				}
				catch (BadLocationException ble)
				{
				}
			}
			return true;
		}

		/**
		 * Calls linkActivated on the associated JEditorPane if the given position represents a link.
		 * <p>
		 * This is implemented to forward to the method with the same name, but with the following args both == -1.
		 * 
		 * @param pos the position
		 * @param html the editor pane
		 */
		protected void activateLink(int pos, JEditorPane editor)
		{
			activateLink(pos, editor, -1, -1);
		}

		/**
		 * Calls linkActivated on the associated JEditorPane if the given position represents a link. If this was the result of a mouse click, <code>x</code>
		 * and <code>y</code> will give the location of the mouse, otherwise they will be < 0.
		 * 
		 * @param pos the position
		 * @param html the editor pane
		 */
		void activateLink(int pos, JEditorPane html, int x, int y)
		{
			Document doc = html.getDocument();
			if (doc instanceof HTMLDocument)
			{
				HTMLDocument hdoc = (HTMLDocument)doc;
				Element e = hdoc.getCharacterElement(pos);
				AttributeSet a = e.getAttributes();
				AttributeSet anchor = (AttributeSet)a.getAttribute(HTML.Tag.A);
				HyperlinkEvent linkEvent = null;

				if (anchor == null)
				{
					href = getMapHREF(html, hdoc, e, a, pos, x, y);
				}
				else
				{
					href = (String)anchor.getAttribute(HTML.Attribute.HREF);
				}

				if (href != null)
				{
					linkEvent = createHyperlinkEvent(html, hdoc, href, anchor, e);
				}
				if (linkEvent != null)
				{
					html.fireHyperlinkUpdate(linkEvent);
				}
			}
		}

		/**
		 * Creates and returns a new instance of HyperlinkEvent. If <code>hdoc</code> is a frame document a HTMLFrameHyperlinkEvent will be created.
		 */
		HyperlinkEvent createHyperlinkEvent(JEditorPane html, HTMLDocument hdoc, String href, AttributeSet anchor, Element element)
		{
			URL u;
			try
			{
				URL base = hdoc.getBase();
				u = new URL(base, href);
				// Following is a workaround for 1.2, in which
				// new URL("file://...", "#...") causes the filename to
				// be lost.
				if (href != null && "file".equals(u.getProtocol()) //$NON-NLS-1$
					&& href.startsWith("#"))
				{
					String baseFile = base.getFile();
					String newFile = u.getFile();
					if (baseFile != null && newFile != null && !newFile.startsWith(baseFile))
					{
						u = new URL(base, baseFile + href);
					}
				}
			}
			catch (MalformedURLException m)
			{
				u = null;
			}
			HyperlinkEvent linkEvent = null;

			String target = (anchor != null) ? (String)anchor.getAttribute(HTML.Attribute.TARGET) : null;
			if (target == null)//!hdoc.isFrameDocument())
			{
				linkEvent = new HyperlinkEvent(html, HyperlinkEvent.EventType.ACTIVATED, u, href);//,
				//					element);
			}
			else
			{
				if ((target == null) || (target.equals("")))
				{
					target = "_self";
				}
				linkEvent = new HTMLFrameHyperlinkEvent(html, HyperlinkEvent.EventType.ACTIVATED, u, href, element, target);
			}
			return linkEvent;
		}

		void fireEvents(JEditorPane editor, HTMLDocument doc, String href, Element lastElem)
		{
			if (this.href != null)
			{
				// fire an exited event on the old link
				URL u;
				try
				{
					u = new URL(doc.getBase(), this.href);
				}
				catch (MalformedURLException m)
				{
					u = null;
				}
				HyperlinkEvent exit = new HyperlinkEvent(editor, HyperlinkEvent.EventType.EXITED, u);//, this.href,
				//								 lastElem);
				editor.fireHyperlinkUpdate(exit);
			}
			if (href != null)
			{
				// fire an entered event on the new link
				URL u;
				try
				{
					u = new URL(doc.getBase(), href);
				}
				catch (MalformedURLException m)
				{
					u = null;
				}
				HyperlinkEvent entered = new HyperlinkEvent(editor, HyperlinkEvent.EventType.ENTERED, u);//, href, curElem);
				editor.fireHyperlinkUpdate(entered);
			}
		}
	}

	static class StyledInsertBreakAction extends HTMLTextAction //StyledEditorKit.StyledTextAction
	{
		StyledInsertBreakAction()
		{
			super(DefaultEditorKit.insertBreakAction);
		}

		public void actionPerformed(ActionEvent e)
		{
			JEditorPane target = getEditor(e);
			if (target != null)
			{
				if ((!target.isEditable()) || (!target.isEnabled()))
				{
					//					target.getToolkit().beep();
					return;
				}
				InsertHTMLTextAction action = null;
				int int4 = target.getSelectionStart();
				if (e.getModifiers() != ActionEvent.CTRL_MASK)
				{
					Element[] elems = getElementsAt(getHTMLDocument(target), int4);
					for (int i = elems.length - 1; i >= 0; i--)
					{
						Element elem = elems[i];
						if ("ol".equalsIgnoreCase(elem.getName().toLowerCase()))
						{
							action = new InsertHTMLTextAction("InsertLine", "<li>#<li>", HTML.Tag.OL, HTML.Tag.LI);
							break;
						}
						if ("ul".equalsIgnoreCase(elem.getName().toLowerCase()))
						{
							action = new InsertHTMLTextAction("InsertLine", "<li>#<li>", HTML.Tag.UL, HTML.Tag.LI);
							break;
						}
					}
				}
				if (action == null)
				{
					target.replaceSelection("\n");
					target.setSelectionStart(int4);
					target.setSelectionEnd((int4 + 1));
					SimpleAttributeSet simpleAttributeSet5 = new SimpleAttributeSet();
					simpleAttributeSet5.addAttribute(StyleConstants.NameAttribute, HTML.Tag.BR);
					setCharacterAttributes((target), (simpleAttributeSet5), false);
					target.setSelectionStart((int4 + 1));
//					readAsynchronously(target);
				}
				else
				{
					action.actionPerformed(e);
				}
			}
			else
			{
				// See if we are in a JTextComponent.
				JTextComponent text = getTextComponent(e);
				if (text != null)
				{
					if ((!text.isEditable()) || (!text.isEnabled()))
					{
						text.getToolkit().beep();
						return;
					}
					text.replaceSelection("<br>");
				}
			}
		}
	}

	public static class InsertAttributeAction extends InsertHTMLTextAction
	{
		//the html attribute code
		protected String html;

		public InsertAttributeAction(String name, String html)
		{
			super(name);
			this.html = html;
		}

		/**
		 * The selected text must be enclosed by an end of tag and a beginning of the next tag. More, the enclosing tags must be tags that support inline
		 * alignment style
		 * 
		 * @param selectedText the initial selected text (as it is selected by the user in the htmlarea
		 * @param wholeText the whole text (including html tags - which aren't visible in the htmlarea)
		 * @return the new selected text (may contain other tags (like <b>selectedText</b> for example))
		 */
		protected String correctSelectedText(String selectedText, String wholeText, int startPosition)
		{
			//unfortunately editor.getSelectedText() doesn't include the formatting tags (like <b>, <i>, etc)
			int startIndex = formattedIndexOf(selectedText, wholeText, startPosition);
			if (startIndex < 0) return "";

			String firstPart = wholeText.substring(0, startIndex);
			int offset = startIndex + selectedText.length();
			String secondPart = wholeText.substring(offset);

			String leftTag = iterateThroughTags(firstPart);
			String leftTagStripped = leftTag;
			if (leftTag.indexOf(" ") > 0) leftTagStripped = leftTag.substring(0, leftTag.indexOf(" "));
			String rightTag = !Utils.stringIsEmpty(leftTag) ? "</" + leftTagStripped.substring(1) : "";
			if (!Utils.stringIsEmpty(leftTag) && !Utils.stringIsEmpty(rightTag) && wholeText.indexOf(rightTag) > 0)
			{
				int startOffset = firstPart.lastIndexOf(leftTag) + leftTag.length() + 1;
				int endOffset = wholeText.indexOf(rightTag, startOffset);//offset + secondPart.indexOf(rightTag);
				return wholeText.substring(startOffset, endOffset);
			}
			else return selectedText;
		}

		/**
		 * returns the classic indexOf(), but the checking is made on a String which has html tags, while the searched String is plain text this method of
		 * course works only in this case, where we know that a selected text's (editor.getSelectedText) tokens are included in the html document. Otherwise,
		 * this method gives only "the most probably" indexOf :)
		 * 
		 * @param unformattedQuery the searched text
		 * @param target the String to search in
		 * @return the position of the unformatted text into the formatted one
		 */
		protected int formattedIndexOf(String unformattedQuery, String target, int startIndex)
		{
			if (Utils.stringIsEmpty(unformattedQuery)) return -1;
			//simplest case: selected text is not formatted
			int offset = target.indexOf(unformattedQuery);
			if (offset != -1 && offset == target.lastIndexOf(unformattedQuery))
			{
				return offset;
			}
			offset = getFormattedOffset(unformattedQuery, target, offset, startIndex);
			if (offset != -1 && offset > startIndex) return offset;
			StringTokenizer tokens = new StringTokenizer(unformattedQuery, " ");
			String currentToken = tokens.hasMoreTokens() ? tokens.nextToken() : "";

			// I remember the first token, in case of an adjustment
			String firstToken = currentToken;
			int firstTokenPosition = target.indexOf(currentToken, startIndex);

			// a limitation here: if you have "som<i>e text </i>" in the editor kit and the selected text is the whole text, then first token will be "some", 
			// which has the -1 index in the whole (formatted) text
			if (firstTokenPosition == -1 || Utils.stringIsEmpty(currentToken)) return -1;

			//all the tokens must have a position != -1 and greater than the position of the previous token			
			int currentPosition = firstTokenPosition;
			int oldPosition = firstTokenPosition;
			while (tokens.hasMoreTokens())
			{
				currentPosition = getFormattedOffset(currentToken, target, currentPosition, oldPosition);
				if (currentToken.indexOf("&#") != -1 && (currentPosition == -1 || currentPosition < oldPosition)) return -1;
				oldPosition = currentPosition + 1;
				currentToken = tokens.nextToken();
			}
			if (currentPosition > firstTokenPosition)
			{
				String rawSelection = target.substring(firstTokenPosition, currentPosition);
				int relativeIndex = rawSelection.lastIndexOf(firstToken);
				if (relativeIndex > firstTokenPosition)
				{
					firstTokenPosition += relativeIndex;
				}
			}
			return firstTokenPosition;
		}

		private int getFormattedOffset(String query, String target, int offset, int startIndex)
		{
			if (offset != -1)
			{
				int nextIndex = offset;
				while (nextIndex < startIndex && nextIndex != -1 && offset < target.length())
				{
					nextIndex = target.indexOf(query, offset + 1);
					if (nextIndex != -1) offset = nextIndex;
				}
				return offset;
			}
			return -1;
		}

		/**
		 * @param tag
		 */
		protected String iterateThroughTags(String text)
		{
			int startTagPosition = text.lastIndexOf("<");
			int endTagPosition = text.lastIndexOf(">");
			if (startTagPosition < 0 || endTagPosition < 0)
			{
				return "";
			}
			if (startTagPosition > endTagPosition)
			{
				text = text.substring(0, startTagPosition);
				if (Utils.stringIsEmpty(text)) return "";
				return iterateThroughTags(text);
			}
			//current tag should be something like <tag (without attributes)
			String currentTag = text.substring(startTagPosition, endTagPosition);
			String currentTagStripped = "";
			if (currentTag.indexOf(" ") > 0) currentTagStripped = currentTag.substring(0, currentTag.indexOf(" "));
			else currentTagStripped = currentTag.toLowerCase();
			boolean valid = false;
			for (int i = 0; i < HtmlUtils.tags.length && !valid; i++)
				if (currentTagStripped.equalsIgnoreCase(HtmlUtils.tags[i])) valid = true;
			for (int i = 0; i < HtmlUtils.alignNotSupportedTags.length && valid; i++)
				if (currentTagStripped.equalsIgnoreCase(HtmlUtils.alignNotSupportedTags[i])) valid = false;

			if (valid)
			{
				return currentTag;
			}
			else
			{
				text = startTagPosition > 1 ? text.substring(0, startTagPosition - 1) : "";
				return iterateThroughTags(text);
			}
		}

		/**
		 * Inserts the HTML attribute in the correct tag
		 * 
		 * @param event the event
		 */
		@Override
		public void actionPerformed(ActionEvent event)
		{
			JEditorPane editor = getEditor(event);
			if (editor != null)
			{
				HTMLDocument htmlDocument = getHTMLDocument(editor);

				String wholeText = editor.getText();
				String selectedText = editor.getSelectedText();

				selectedText = correctSelectedText(selectedText, wholeText, editor.getSelectionStart());
				if (selectedText == null || selectedText.trim().length() == 0) return;

				int offset = wholeText.indexOf(selectedText);
				if (offset == -1) return;
				String firstPart = wholeText.substring(0, offset);
				String secondPart = wholeText.substring(offset + selectedText.length(), wholeText.length() - 1);
				if (firstPart.lastIndexOf("<") > (firstPart.lastIndexOf(">") + 1)) return;
				String wrapperElem = firstPart.substring(firstPart.lastIndexOf("<"), firstPart.lastIndexOf(">") + 1);
				firstPart = firstPart.substring(0, firstPart.lastIndexOf(wrapperElem));
				wrapperElem = transformWrapperElement(wrapperElem, html);
				try
				{
					String modifiedContent = firstPart + wrapperElem + selectedText + secondPart;
					htmlDocument.remove(0, htmlDocument.getLength());
//					editor.setText(modifiedContent);
//					put this instead of editor.setText(modifiedContent); the setText method calls the read from FixedHtmlEditorKit (but we use the fixedJeditorPane.read() instead
					ByteArrayInputStream bais = new ByteArrayInputStream(modifiedContent.getBytes());
					editor.read(bais, null);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}

		/**
		 * looks at the current wrapper element and inspects the attributes; if the current set attribute overrides the old one, only its value is updated This
		 * is done in order to avoid duplication of attributes (especially style attributes). If you will want to add other style "sub-attributes", you'll have
		 * to extend this class and override this method
		 * 
		 * @param wrapperElement is the element that wraps the selected text
		 * @param htmlAttribute is the attribute that must be set to the current wrapper element
		 * @return the updated wrapper element
		 */
		protected String transformWrapperElement(String wrapperElement, String htmlAttribute)
		{
			String wrapper = wrapperElement.replaceFirst("<", "");
			if (wrapper.length() == 0) return "";
			wrapper = wrapper.substring(0, wrapper.length() - 1);
			StringTokenizer tokenizer = new StringTokenizer(wrapper, " ");

			//for special case: editor automatically makes <td style="align:left"> into <td align="left">
			boolean specialCase = false;
			try
			{
				for (String element : HtmlUtils.specialCaseTags)
				{
					if (wrapper.equalsIgnoreCase(element) || wrapper.substring(0, wrapper.indexOf(" ")).equalsIgnoreCase(element))
					{
						specialCase = true;
						break;
					}
				}
			}
			catch (NullPointerException e)
			{
				specialCase = false;
			}
			catch (StringIndexOutOfBoundsException e)
			{
				specialCase = false;
			}

			String attributeName = htmlAttribute.substring(0, html.indexOf('='));
			String attributeValue;
			if (specialCase && "style".equalsIgnoreCase(attributeName))
			{
				attributeName = "align";
				attributeValue = htmlAttribute.substring(htmlAttribute.indexOf(":") + 1, htmlAttribute.lastIndexOf("\""));
				attributeValue = "\"" + attributeValue + "\"";
			}
			else
			{
				attributeValue = htmlAttribute.substring(htmlAttribute.indexOf("\""), htmlAttribute.lastIndexOf("\""));
			}
			String oldAttributeValue = "";

			while (tokenizer.hasMoreTokens())
			{
				String currentToken = tokenizer.nextToken();
				if (currentToken.indexOf("=") > -1)
				{
					String name = currentToken.substring(0, currentToken.indexOf("="));
					if (name.equalsIgnoreCase(attributeName)) oldAttributeValue = currentToken.substring(currentToken.indexOf("=") + 1, currentToken.length());
				}
			}
			if ("".equals(oldAttributeValue.trim()))
			{
				// if attribute with the same name does not exist, put the new attribute at the "end" of the tag (before ">")
				wrapperElement = wrapperElement.substring(0, wrapperElement.indexOf(">")) + " " + htmlAttribute + ">";
				return wrapperElement;
			}
			else
			{
				//if same attribute exists, its value will be replaced 
				return wrapperElement.replaceAll(oldAttributeValue, attributeValue);
			}
		}


	}

	/**
	 * InsertHTMLTextAction can be used to insert an arbitrary string of HTML into an existing HTML document. At least two HTML.Tags need to be supplied. The
	 * first Tag, parentTag, identifies the parent in the document to add the elements to. The second tag, addTag, identifies the first tag that should be added
	 * to the document as seen in the HTML string. One important thing to remember, is that the parser is going to generate all the appropriate tags, even if
	 * they aren't in the HTML string passed in.
	 * <p>
	 * For example, lets say you wanted to create an action to insert a table into the body. The parentTag would be HTML.Tag.BODY, addTag would be
	 * HTML.Tag.TABLE, and the string could be something like &lt;table&gt;&lt;tr&gt;&lt;td&gt;&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;.
	 * <p>
	 * There is also an option to supply an alternate parentTag and addTag. These will be checked for if there is no parentTag at offset.
	 */
	public static class InsertHTMLTextAction extends HTMLTextAction
	{
		public InsertHTMLTextAction(String name)
		{
			super(name);
		}

		public InsertHTMLTextAction(String name, String html, HTML.Tag parentTag, HTML.Tag addTag)
		{
			this(name, html, parentTag, addTag, null, null);
		}

		public InsertHTMLTextAction(String name, String html, HTML.Tag parentTag, HTML.Tag addTag, HTML.Tag alternateParentTag, HTML.Tag alternateAddTag)
		{
			this(name, html, parentTag, addTag, alternateParentTag, alternateAddTag, true);
		}

		/* public */
		InsertHTMLTextAction(String name, String html, HTML.Tag parentTag, HTML.Tag addTag, HTML.Tag alternateParentTag, HTML.Tag alternateAddTag,
			boolean adjustSelection)
		{
			super(name);
			this.html = html;
			this.parentTag = parentTag;
			this.addTag = addTag;
			this.alternateParentTag = alternateParentTag;
			this.alternateAddTag = alternateAddTag;
			this.adjustSelection = adjustSelection;
		}

		/**
		 * A cover for HTMLEditorKit.insertHTML. If an exception it thrown it is wrapped in a RuntimeException and thrown.
		 */
		protected void insertHTML(JEditorPane editor, HTMLDocument doc, int offset, String html, int popDepth, int pushDepth, HTML.Tag addTag)
		{
			try
			{
				//				int end = editor.getSelectionEnd();
				//				int pos = html.indexOf('#');
				String sel = ""; //$NON-NLS-1$
				if (addTag.isBlock() || addTag == HTML.Tag.SUP || addTag == HTML.Tag.SUB)
				{
					sel = editor.getSelectedText();
					if (sel == null)
					{
						sel = "";
					}
					else
					{
						editor.replaceSelection("");
					}
				}
				html = Utils.stringReplace(html, "#", sel);

				getHTMLEditorKit(editor).insertHTML(doc, offset, html, popDepth, pushDepth, addTag);

				//				editor.setSelectionStart(end+pos+1);
			}
			catch (IOException ioe)
			{
				throw new RuntimeException("Unable to insert: " + ioe);
			}
			catch (BadLocationException ble)
			{
				throw new RuntimeException("Unable to insert: " + ble);
			}
		}

		/**
		 * This is invoked when inserting at a boundary. It determines the number of pops, and then the number of pushes that need to be performed, and then
		 * invokes insertHTML.
		 * 
		 * @since 1.3
		 */
		protected void insertAtBoundary(JEditorPane editor, HTMLDocument doc, int offset, Element insertElement, String html, HTML.Tag parentTag,
			HTML.Tag addTag)
		{
			insertAtBoundry(editor, doc, offset, insertElement, html, parentTag, addTag);
		}

		/**
		 * This is invoked when inserting at a boundary. It determines the number of pops, and then the number of pushes that need to be performed, and then
		 * invokes insertHTML.
		 * 
		 * @deprecated As of Java 2 platform v1.3, use insertAtBoundary
		 */
		@Deprecated
		protected void insertAtBoundry(JEditorPane editor, HTMLDocument doc, int offset, Element insertElement, String html, HTML.Tag parentTag, HTML.Tag addTag)
		{
			// Find the common parent.
			Element e;
			Element commonParent;
			boolean isFirst = (offset == 0);

			if (offset > 0 || insertElement == null)
			{
				e = doc.getDefaultRootElement();
				while (e != null && e.getStartOffset() != offset && !e.isLeaf())
				{
					e = e.getElement(e.getElementIndex(offset));
				}
				commonParent = (e != null) ? e.getParentElement() : null;
			}
			else
			{
				// If inserting at the origin, the common parent is the
				// insertElement.
				commonParent = insertElement;
			}
			if (commonParent != null)
			{
				// Determine how many pops to do.
				int pops = 0;
				int pushes = 0;
				if (isFirst && insertElement != null)
				{
					e = commonParent;
					while (e != null && !e.isLeaf())
					{
						e = e.getElement(e.getElementIndex(offset));
						pops++;
					}
				}
				else
				{
					e = commonParent;
					offset--;
					while (e != null && !e.isLeaf())
					{
						e = e.getElement(e.getElementIndex(offset));
						pops++;
					}

					// And how many pushes
					e = commonParent;
					offset++;
					while (e != null && e != insertElement)
					{
						e = e.getElement(e.getElementIndex(offset));
						pushes++;
					}
				}
				pops = Math.max(0, pops - 1);

				// And insert!
				insertHTML(editor, doc, offset, html, pops, pushes, addTag);
			}
		}

		/**
		 * If there is an Element with name <code>tag</code> at <code>offset</code>, this will invoke either insertAtBoundary or <code>insertHTML</code>.
		 * This returns true if there is a match, and one of the inserts is invoked.
		 */
		/* protected */
		boolean insertIntoTag(JEditorPane editor, HTMLDocument doc, int offset, HTML.Tag tag, HTML.Tag addTag)
		{
			Element e = findElementMatchingTag(doc, offset, tag);
			if (e != null && e.getStartOffset() == offset)
			{
				insertAtBoundary(editor, doc, offset, e, html, tag, addTag);
				return true;
			}
			else if (offset > 0)
			{
				int depth = elementCountToTag(doc, offset - 1, tag);
				if (depth != -1)
				{
					insertHTML(editor, doc, offset, html, depth, 0, addTag);
					return true;
				}
			}
			return false;
		}

		/**
		 * Called after an insertion to adjust the selection.
		 */
		/* protected */
		void adjustSelection(JEditorPane pane, HTMLDocument doc, int startOffset, int oldLength)
		{
			int newLength = doc.getLength();
			if (newLength != oldLength && startOffset < newLength)
			{
				if (startOffset > 0)
				{
					String text;
					try
					{
						text = doc.getText(startOffset - 1, 1);
					}
					catch (BadLocationException ble)
					{
						text = null;
					}
					if (text != null && text.length() > 0 && text.charAt(0) == '\n')
					{
						pane.select(startOffset, startOffset);
					}
					else
					{
						pane.select(startOffset + 1, startOffset + 1);
					}
				}
				else
				{
					pane.select(1, 1);
				}
			}
		}

		/**
		 * Inserts the HTML into the document.
		 * 
		 * @param ae the event
		 */
		public void actionPerformed(ActionEvent ae)
		{
			JEditorPane editor = getEditor(ae);
			if (editor != null)
			{
				HTMLDocument doc = getHTMLDocument(editor);
				int offset = editor.getSelectionStart();
				int length = doc.getLength();
				boolean inserted;
				// Try first choice
				if (!insertIntoTag(editor, doc, offset, parentTag, addTag) && alternateParentTag != null)
				{
					// Then alternate.
					inserted = insertIntoTag(editor, doc, offset, alternateParentTag, alternateAddTag);
				}
				else
				{
					inserted = true;
				}
				if (adjustSelection && inserted)
				{
//					readAsynchronously(editor);
					adjustSelection(editor, doc, offset, length);
				}
			}
		}

		/** HTML to insert. */
		protected String html;
		/** Tag to check for in the document. */
		protected HTML.Tag parentTag;
		/** Tag in HTML to start adding tags from. */
		protected HTML.Tag addTag;
		/**
		 * Alternate Tag to check for in the document if parentTag is not found.
		 */
		protected HTML.Tag alternateParentTag;
		/**
		 * Alternate tag in HTML to start adding tags from if parentTag is not found and alternateParentTag is found.
		 */
		protected HTML.Tag alternateAddTag;
		/** True indicates the selection should be adjusted after an insert. */
		boolean adjustSelection;
	}

	/**
	 * Get the MIME type of the data that this kit represents support for. This kit supports the type <code>text/html</code>.
	 * 
	 * @return the type
	 */
	@Override
	public String getContentType()
	{
		return "text/html";
	}

	/**
	 * Fetch a factory that is suitable for producing views of any models that are produced by this kit.
	 * 
	 * @return the factory
	 */
	@Override
	public ViewFactory getViewFactory()
	{
		return currentFactory;
	}

	/**
	 * Create an uninitialized text storage model that is appropriate for this type of editor.
	 * 
	 * @return the model
	 */
	@Override
	public Document createDefaultDocument()
	{
		StyleSheet styles = getStyleSheet();
		StyleSheet ss = createStyleSheet();

		ss.addStyleSheet(styles);


		// make <BR> tags add "\n" into the document and do not add white space "\n" to the document
		HTMLDocument doc = new HTMLDocument(ss)
		{
			@Override
			public ParserCallback getReader(int pos)
			{
				Object desc = getProperty(Document.StreamDescriptionProperty);
				if (desc instanceof URL)
				{
					setBase((URL)desc);
				}
				HTMLReader reader = new HTMLReader(pos)
				{

					private boolean insideHead = false;

					@Override
					protected void addSpecialElement(Tag t, MutableAttributeSet a)
					{
						int l1 = parseBuffer.size();
						super.addSpecialElement(t, a);
						if (l1 < parseBuffer.size() && t == HTML.Tag.BR)
						{
							char[] one = new char[1];
							one[0] = '\n';

							ElementSpec es = parseBuffer.lastElement();
							ElementSpec newEs = new ElementSpec(es.getAttributes().copyAttributes(), ElementSpec.ContentType, one, 0, 1);
							parseBuffer.setElementAt(newEs, parseBuffer.size() - 1);
						}
					}

					@Override
					protected void blockClose(Tag t)
					{
						super.blockClose(t);
						if (t == HTML.Tag.HEAD)
						{
							insideHead = false;
						}
					}

					@Override
					protected void blockOpen(Tag t, MutableAttributeSet attr)
					{
						super.blockOpen(t, attr);
						if (t == HTML.Tag.HEAD)
						{
							insideHead = true;
						}
					}

					@Override
					protected void addContent(char[] data, int offs, int length, boolean generateImpliedPIfNecessary)
					{
						if (!insideHead)
						{
							super.addContent(data, offs, length, generateImpliedPIfNecessary);
						}
					}

				};
				return reader;
			}
		};
		doc.setParser(getParser());
		doc.setAsynchronousLoadPriority(4);
		doc.setTokenThreshold(50);
		return doc;
	}

	/**
	 * Inserts content from the given stream. If <code>doc</code> is an instance of HTMLDocument, this will read HTML 3.2 text. Inserting HTML into a
	 * non-empty document must be inside the body Element, if you do not insert into the body an exception will be thrown. When inserting into a non-empty
	 * document all tags outside of the body (head, title) will be dropped.
	 * 
	 * @param in the stream to read from
	 * @param doc the destination for the insertion
	 * @param pos the location in the document to place the content
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid location within the document
	 * @exception RuntimeException (will eventually be a BadLocationException) if pos is invalid
	 */
	@Override
	public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException
	{

		if (doc instanceof HTMLDocument)
		{
			HTMLDocument hdoc = (HTMLDocument)doc;
			HTMLEditorKit.Parser p = getParser();
			if (p == null)
			{
				throw new IOException("Can't load parser");
			}
			if (pos > doc.getLength())
			{
				throw new BadLocationException("Invalid location", pos);
			}
			synchronized (this)
			{
				HTMLEditorKit.ParserCallback receiver = hdoc.getReader(pos);
				Boolean ignoreCharset = (Boolean)doc.getProperty("IgnoreCharsetDirective"); //$NON-NLS-1$
				p.parse(in, receiver, (ignoreCharset == null) ? false : ignoreCharset.booleanValue());
				receiver.flush();
			}
		}
		else
		{
			super.read(in, doc, pos);
		}
	}

	/**
	 * Inserts HTML into an existing document.
	 * 
	 * @param doc the document to insert into
	 * @param offset the offset to insert HTML at
	 * @param popDepth the number of ElementSpec.EndTagTypes to generate before inserting
	 * @param pushDepth the number of ElementSpec.StartTagTypes with a direction of ElementSpec.JoinNextDirection that should be generated before inserting, but
	 *            after the end tags have been generated
	 * @param insertTag the first tag to start inserting into document
	 * @exception RuntimeException (will eventually be a BadLocationException) if pos is invalid
	 */
	public void insertHTML(HTMLDocument doc, int offset, String html, int popDepth, int pushDepth, HTML.Tag insertTag) throws BadLocationException, IOException
	{
		HTMLEditorKit.Parser p = getParser();
		if (p == null)
		{
			throw new IOException("Can't load parser");
		}
		if (offset > doc.getLength())
		{
			throw new BadLocationException("Invalid location", offset);
		}

		HTMLEditorKit.ParserCallback receiver = doc.getReader(offset, popDepth, pushDepth, insertTag);
		Boolean ignoreCharset = (Boolean)doc.getProperty("IgnoreCharsetDirective"); //$NON-NLS-1$
		p.parse(new StringReader(html), receiver, (ignoreCharset == null) ? false : ignoreCharset.booleanValue());
		receiver.flush();
	}

	/**
	 * Write content from a document to the given stream in a format appropriate for this kind of content handler.
	 * 
	 * @param out the stream to write to
	 * @param doc the source for the write
	 * @param pos the location in the document to fetch the content
	 * @param len the amount to write out
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid location within the document
	 */
	@Override
	public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException
	{

		if (doc instanceof HTMLDocument)
		{
			HTMLWriter w = new HTMLWriter(out, (HTMLDocument)doc, pos, len);
			w.write();
		}
		else if (doc instanceof StyledDocument)
		{
			MinimalHTMLWriter w = new MinimalHTMLWriter(out, (StyledDocument)doc, pos, len);
			w.write();
		}
		else
		{
			super.write(out, doc, pos, len);
		}
	}

	/**
	 * Called when the kit is being installed into the a JEditorPane.
	 * 
	 * @param c the JEditorPane
	 */
	@Override
	public void install(JEditorPane c)
	{
		c.addMouseListener(linkHandler);
		c.addMouseMotionListener(linkHandler);
		c.addCaretListener(nextLinkAction);
		super.install(c);
	}

	/**
	 * Called when the kit is being removed from the JEditorPane. This is used to unregister any listeners that were attached.
	 * 
	 * @param c the JEditorPane
	 */
	@Override
	public void deinstall(JEditorPane c)
	{
		c.removeMouseListener(linkHandler);
		c.removeMouseMotionListener(linkHandler);
		super.deinstall(c);
	}

	/**
	 * Default Cascading Style Sheet file that sets up the tag views.
	 */
	public static final String DEFAULT_CSS = "default.css"; //$NON-NLS-1$

	/**
	 * Set the set of styles to be used to render the various HTML elements. These styles are specified in terms of CSS specifications. Each document produced
	 * by the kit will have a copy of the sheet which it can add the document specific styles to. By default, the StyleSheet specified is shared by all
	 * HTMLEditorKit instances. This should be reimplemented to provide a finer granularity if desired.
	 */
	public void setStyleSheet(StyleSheet s)
	{
		defaultStyles = s;
	}

	/**
	 * Get the set of styles currently being used to render the HTML elements. By default the resource specified by DEFAULT_CSS gets loaded, and is shared by
	 * all HTMLEditorKit instances.
	 */
	public StyleSheet getStyleSheet()
	{
		if (defaultStyles == null)
		{
			defaultStyles = createStyleSheet();
			try
			{
				InputStream is = HTMLEditorKit.class.getResourceAsStream(DEFAULT_CSS);
				Reader r = new BufferedReader(new InputStreamReader(is));
				defaultStyles.loadRules(r, null);
				r.close();
			}
			catch (Throwable e)
			{
				// on error we simply have no styles... the html
				// will look mighty wrong but still function.
			}
		}
		return defaultStyles;
	}

	protected StyleSheet createStyleSheet()
	{
		return new FixedStyleSheet();
	}

	/**
	 * Fetch a resource relative to the HTMLEditorKit classfile. If this is called on 1.2 the loading will occur under the protection of a doPrivileged call to
	 * allow the HTMLEditorKit to function when used in an applet.
	 * 
	 * @param name the name of the resource, relative to the HTMLEditorKit class
	 * @return a stream representing the resource
	 */
	static InputStream getResourceAsStream(String name)
	{
		return HTMLEditorKit.class.getResourceAsStream(name);
	}

	/**
	 * Copies the key/values in <code>element</code>s AttributeSet into <code>set</code>. This does not copy component, icon, or element names attributes.
	 * Subclasses may wish to refine what is and what isn't copied here. But be sure to first remove all the attributes that are in <code>set</code>.
	 * <p>
	 * This is called anytime the caret moves over a different location.
	 * 
	 */
	@Override
	protected void createInputAttributes(Element element, MutableAttributeSet set)
	{
		set.removeAttributes(set);
		set.addAttributes(element.getAttributes());
		set.removeAttribute(StyleConstants.ComposedTextAttribute);

		Object o = set.getAttribute(StyleConstants.NameAttribute);
		if (o instanceof HTML.Tag)
		{
			HTML.Tag tag = (HTML.Tag)o;
			// PENDING: we need a better way to express what shouldn't be
			// copied when editing...
			if (tag == HTML.Tag.IMG)
			{
				// Remove the related image attributes, src, width, height
				set.removeAttribute(HTML.Attribute.SRC);
				set.removeAttribute(HTML.Attribute.HEIGHT);
				set.removeAttribute(HTML.Attribute.WIDTH);
				set.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
			}
			else if (tag == HTML.Tag.HR || tag == HTML.Tag.BR)
			{
				// Don't copy HRs or BRs either.
				set.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
			}
			else if (tag == HTML.Tag.COMMENT)
			{
				// Don't copy COMMENTs either
				set.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
				set.removeAttribute(HTML.Attribute.COMMENT);
			}
			else if (tag == HTML.Tag.INPUT)
			{
				// or INPUT either
				set.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
				set.removeAttribute(HTML.Tag.INPUT);
			}
			else if (tag instanceof HTML.UnknownTag)
			{
				// Don't copy unknowns either:(
				set.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
				set.removeAttribute(HTML.Attribute.ENDTAG);
			}
		}
	}

	/**
	 * Gets the input attributes used for the styled editing actions.
	 * 
	 * @return the attribute set
	 */
	@Override
	public MutableAttributeSet getInputAttributes()
	{
		if (input == null)
		{
			input = getStyleSheet().addStyle(null, null);
		}
		return input;
	}

	/**
	 * Sets the default cursor.
	 * 
	 * @since 1.3
	 */
	public void setDefaultCursor(Cursor cursor)
	{
		defaultCursor = cursor;
	}

	/**
	 * Returns the default cursor.
	 * 
	 * @since 1.3
	 */
	public Cursor getDefaultCursor()
	{
		return defaultCursor;
	}

	public void setEditCursor(Cursor cursor)
	{
		editCursor = cursor;
	}

	public Cursor getEditCursor()
	{
		return editCursor;
	}

	/**
	 * Sets the cursor to use over links.
	 * 
	 * @since 1.3
	 */
	public void setLinkCursor(Cursor cursor)
	{
		linkCursor = cursor;
	}

	/**
	 * Returns the cursor to use over hyper links.
	 */
	public Cursor getLinkCursor()
	{
		return linkCursor;
	}

	/**
	 * Creates a copy of the editor kit.
	 * 
	 * @return the copy
	 */
	@Override
	public Object clone()
	{
		FixedHTMLEditorKit o = (FixedHTMLEditorKit)super.clone();
		if (o != null)
		{
			o.input = null;
			o.linkHandler = new LinkController();
		}
		return o;
	}

	/**
	 * Fetch the parser to use for reading HTML streams. This can be reimplemented to provide a different parser. The default implementation is loaded
	 * dynamically to avoid the overhead of loading the default parser if it's not used. The default parser is the HotJava parser using an HTML 3.2 DTD.
	 */
	protected HTMLEditorKit.Parser getParser()
	{
		if (defaultParser == null)
		{
			try
			{
				Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator"); //$NON-NLS-1$
				defaultParser = (HTMLEditorKit.Parser)c.newInstance();
			}
			catch (Throwable e)
			{
			}
		}
		return defaultParser;
	}

	// --- variables ------------------------------------------

	private static final Cursor MoveCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static final Cursor DefaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	private static final Cursor EditCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

	/** Shared factory for creating HTML Views. */
	private static final ViewFactory defaultFactory = new HTMLFactory();

	private ViewFactory currentFactory = defaultFactory;

	MutableAttributeSet input;
	private static StyleSheet defaultStyles = null;
	private static HTMLEditorKit.Parser defaultParser = null;
	private Cursor defaultCursor = DefaultCursor;
	private Cursor editCursor = EditCursor;
	private Cursor linkCursor = MoveCursor;
	// --- Action implementations ------------------------------

	/**
	 * The bold action identifier
	 */
	public static final String BOLD_ACTION = "html-bold-action"; //$NON-NLS-1$
	/**
	 * The italic action identifier
	 */
	public static final String ITALIC_ACTION = "html-italic-action"; //$NON-NLS-1$
	/**
	 * The paragraph left indent action identifier
	 */
	public static final String PARA_INDENT_LEFT = "html-para-indent-left"; //$NON-NLS-1$
	/**
	 * The paragraph right indent action identifier
	 */
	public static final String PARA_INDENT_RIGHT = "html-para-indent-right"; //$NON-NLS-1$
	/**
	 * The font size increase to next value action identifier
	 */
	public static final String FONT_CHANGE_BIGGER = "html-font-bigger"; //$NON-NLS-1$
	/**
	 * The font size decrease to next value action identifier
	 */
	public static final String FONT_CHANGE_SMALLER = "html-font-smaller"; //$NON-NLS-1$
	/**
	 * The Color choice action identifier The color is passed as an argument
	 */
	public static final String COLOR_ACTION = "html-color-action"; //$NON-NLS-1$
	/**
	 * The logical style choice action identifier The logical style is passed in as an argument
	 */
	public static final String LOGICAL_STYLE_ACTION = "html-logical-style-action"; //$NON-NLS-1$
	/**
	 * Align images at the top.
	 */
	public static final String IMG_ALIGN_TOP = "html-image-align-top"; //$NON-NLS-1$

	/**
	 * Align images in the middle.
	 */
	public static final String IMG_ALIGN_MIDDLE = "html-image-align-middle"; //$NON-NLS-1$

	/**
	 * Align images at the bottom.
	 */
	public static final String IMG_ALIGN_BOTTOM = "html-image-align-bottom"; //$NON-NLS-1$

	/**
	 * Align images at the border.
	 */
	public static final String IMG_BORDER = "html-image-border"; //$NON-NLS-1$

	/**
	 * An abstract Action providing some convenience methods that may be useful in inserting HTML into an existing document.
	 * <p>
	 * NOTE: None of the convenience methods obtain a lock on the document. If you have another thread modifying the text these methods may have inconsistent
	 * behavior, or return the wrong thing.
	 */
	public static abstract class HTMLTextAction extends StyledTextAction
	{
		public HTMLTextAction(String name)
		{
			super(name);
		}

		/**
		 * @return HTMLDocument of <code>e</code>.
		 */
		protected HTMLDocument getHTMLDocument(JEditorPane e)
		{
			Document d = e.getDocument();
			if (d instanceof HTMLDocument)
			{
				return (HTMLDocument)d;
			}
			throw new IllegalArgumentException("document must be HTMLDocument");
		}

		/**
		 * @return HTMLEditorKit for <code>e</code>.
		 */
		protected FixedHTMLEditorKit getHTMLEditorKit(JEditorPane e)
		{
			EditorKit k = e.getEditorKit();
			if (k instanceof FixedHTMLEditorKit)
			{
				return (FixedHTMLEditorKit)k;
			}
			throw new IllegalArgumentException("EditorKit must be HTMLEditorKit");
		}

		/**
		 * Returns an array of the Elements that contain <code>offset</code>. The first elements corresponds to the root.
		 */
		protected Element[] getElementsAt(HTMLDocument doc, int offset)
		{
			return getElementsAt(doc.getDefaultRootElement(), offset, 0);
		}

		/**
		 * Recursive method used by getElementsAt.
		 */
		private Element[] getElementsAt(Element parent, int offset, int depth)
		{
			if (parent.isLeaf())
			{
				Element[] retValue = new Element[depth + 1];
				retValue[depth] = parent;
				return retValue;
			}
			Element[] retValue = getElementsAt(parent.getElement(parent.getElementIndex(offset)), offset, depth + 1);
			retValue[depth] = parent;
			return retValue;
		}

		/**
		 * Returns number of elements, starting at the deepest leaf, needed to get to an element representing <code>tag</code>. This will return -1 if no
		 * elements is found representing <code>tag</code>, or 0 if the parent of the leaf at <code>offset</code> represents <code>tag</code>.
		 */
		protected int elementCountToTag(HTMLDocument doc, int offset, HTML.Tag tag)
		{
			int depth = -1;
			Element e = doc.getCharacterElement(offset);
			while (e != null && e.getAttributes().getAttribute(StyleConstants.NameAttribute) != tag)
			{
				e = e.getParentElement();
				depth++;
			}
			if (e == null)
			{
				return -1;
			}
			return depth;
		}

		/**
		 * Returns the deepest element at <code>offset</code> matching <code>tag</code>.
		 */
		protected Element findElementMatchingTag(HTMLDocument doc, int offset, HTML.Tag tag)
		{
			Element e = doc.getDefaultRootElement();
			Element lastMatch = null;
			while (e != null)
			{
				if (e.getAttributes().getAttribute(StyleConstants.NameAttribute) == tag)
				{
					lastMatch = e;
				}
				e = e.getElement(e.getElementIndex(offset));
			}
			return lastMatch;
		}
	}

	/**
	 * InsertHRAction is special, at actionPerformed time it will determine the parent HTML.Tag based on the paragraph element at the selection start.
	 */
	static class InsertHRAction extends InsertHTMLTextAction
	{
		InsertHRAction()
		{
			super("InsertHR", "<hr>", null, HTML.Tag.IMPLIED, null, null, false);
		}

		/**
		 * Inserts the HTML into the document.
		 * 
		 * @param ae the event
		 */
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			JEditorPane editor = getEditor(ae);
			if (editor != null)
			{
				HTMLDocument doc = getHTMLDocument(editor);
				int offset = editor.getSelectionStart();
				Element paragraph = doc.getParagraphElement(offset);
				if (paragraph.getParentElement() != null)
				{
					parentTag = (HTML.Tag)paragraph.getParentElement().getAttributes().getAttribute(StyleConstants.NameAttribute);
					super.actionPerformed(ae);
				}
			}
		}

	}

	/*
	 * Returns the object in an AttributeSet matching a key
	 */
	static private Object getAttrValue(AttributeSet attr, HTML.Attribute key)
	{
		Enumeration names = attr.getAttributeNames();
		while (names.hasMoreElements())
		{
			Object nextKey = names.nextElement();
			Object nextVal = attr.getAttribute(nextKey);
			if (nextVal instanceof AttributeSet)
			{
				Object value = getAttrValue((AttributeSet)nextVal, key);
				if (value != null)
				{
					return value;
				}
			}
			else if (nextKey == key)
			{
				return nextVal;
			}
		}
		return null;
	}

	/*
	 * Action to move the focus on the next or previous hypertext link or object. TODO: This method relies on support from the javax.accessibility package. The
	 * text package should support keyboard navigation of text elements directly.
	 */
	static class NavigateLinkAction extends TextAction implements CaretListener
	{

		private static int prevHypertextOffset = -1;
		private static boolean foundLink = false;
		private final FocusHighlightPainter focusPainter = new FocusHighlightPainter(null);
		private Object selectionTag;
		private boolean focusBack = false;

		/*
		 * Create this action with the appropriate identifier.
		 */
		public NavigateLinkAction(String actionName)
		{
			super(actionName);
			if ("previous-link-action".equals(actionName))
			{
				focusBack = true;
			}
		}

		/**
		 * Called when the caret position is updated.
		 * 
		 * @param e the caret event
		 */
		public void caretUpdate(CaretEvent e)
		{
			if (foundLink)
			{
				foundLink = false;
				// TODO: The AccessibleContext for the editor should register
				// as a listener for CaretEvents and forward the events to
				// assistive technologies listening for such events.
				Object src = e.getSource();
				//			if (src instanceof JTextComponent) {
				//				((JTextComponent)src).getAccessibleContext().firePropertyChange(
				//							AccessibleContext.ACCESSIBLE_HYPERTEXT_OFFSET,
				//					new Integer(prevHypertextOffset),
				//					new Integer(e.getDot()));
				//			}
			}
		}

		/*
		 * The operation to perform when this action is triggered.
		 */
		public void actionPerformed(ActionEvent e)
		{
			JTextComponent comp = getTextComponent(e);
			if (comp == null || comp.isEditable())
			{
				return;
			}
			Document doc = comp.getDocument();
			if (doc == null)
			{
				return;
			}
			// TODO: Should start successive iterations from the
			// current caret position.
			ElementIterator ei = new ElementIterator(doc);

			int currentOffset = comp.getCaretPosition();
			int prevStartOffset = -1;
			int prevEndOffset = -1;

			// highlight the next link or object after the current caret position
			Element nextElement = null;
			while ((nextElement = ei.next()) != null)
			{
				String name = nextElement.getName();
				AttributeSet attr = nextElement.getAttributes();

				Object href = getAttrValue(attr, HTML.Attribute.HREF);
				if (!(name.equals(HTML.Tag.OBJECT.toString())) && href == null)
				{
					continue;
				}

				int elementOffset = nextElement.getStartOffset();
				if (focusBack)
				{
					if (elementOffset >= currentOffset && prevStartOffset >= 0)
					{

						foundLink = true;
						comp.setCaretPosition(prevStartOffset);
						moveCaretPosition(comp, prevStartOffset, prevEndOffset);
						prevHypertextOffset = prevStartOffset;
						return;
					}
				}
				else
				{ // focus forward
					if (elementOffset > currentOffset)
					{

						foundLink = true;
						comp.setCaretPosition(elementOffset);
						moveCaretPosition(comp, elementOffset, nextElement.getEndOffset());
						prevHypertextOffset = elementOffset;
						return;
					}
				}
				prevStartOffset = nextElement.getStartOffset();
				prevEndOffset = nextElement.getEndOffset();
			}
		}

		/*
		 * Moves the caret from mark to dot
		 */
		private void moveCaretPosition(JTextComponent comp, int mark, int dot)
		{
			Highlighter h = comp.getHighlighter();
			if (h != null)
			{
				int p0 = Math.min(dot, mark);
				int p1 = Math.max(dot, mark);
				try
				{
					if (selectionTag != null)
					{
						h.changeHighlight(selectionTag, p0, p1);
					}
					else
					{
						Highlighter.HighlightPainter p = focusPainter;
						selectionTag = h.addHighlight(p0, p1, p);
					}
				}
				catch (BadLocationException e)
				{
				}
			}
		}

		/**
		 * A highlight painter that draws a one-pixel border around the highlighted area.
		 */
		class FocusHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter
		{

			FocusHighlightPainter(Color color)
			{
				super(color);
			}

			/**
			 * Paints a portion of a highlight.
			 * 
			 * @param g the graphics context
			 * @param offs0 the starting model offset >= 0
			 * @param offs1 the ending model offset >= offs1
			 * @param bounds the bounding box of the view, which is not necessarily the region to paint.
			 * @param c the editor
			 * @param view View painting for
			 * @return region in which drawing occurred
			 */
			@Override
			public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view)
			{

				Color color = getColor();

				if (color == null)
				{
					g.setColor(c.getSelectionColor());
				}
				else
				{
					g.setColor(color);
				}
				if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset())
				{
					// Contained in view, can just use bounds.
					Rectangle alloc;
					if (bounds instanceof Rectangle)
					{
						alloc = (Rectangle)bounds;
					}
					else
					{
						alloc = bounds.getBounds();
					}
					g.drawRect(alloc.x, alloc.y, alloc.width - 1, alloc.height);
					return alloc;
				}
				else
				{
					// Should only render part of View.
					try
					{
						// --- determine locations ---
						Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
						Rectangle r = (shape instanceof Rectangle) ? (Rectangle)shape : shape.getBounds();
						g.drawRect(r.x, r.y, r.width - 1, r.height);
						return r;
					}
					catch (BadLocationException e)
					{
						// can't render
					}
				}
				// Only if exception
				return null;
			}
		}
	}

	/*
	 * Action to activate the hypertext link that has focus. TODO: This method relies on support from the javax.accessibility package. The text package should
	 * support keyboard navigation of text elements directly.
	 */
	static class ActivateLinkAction extends TextAction
	{

		/**
		 * Create this action with the appropriate identifier.
		 */
		public ActivateLinkAction(String actionName)
		{
			super(actionName);
		}

		/*
		 * activates the hyperlink at offset
		 */
		private void activateLink(String href, HTMLDocument doc, JEditorPane editor, int offset)
		{
			try
			{
				URL page = (URL)doc.getProperty(Document.StreamDescriptionProperty);
				URL url = new URL(page, href);
				HyperlinkEvent linkEvent = new HyperlinkEvent(editor, HyperlinkEvent.EventType.ACTIVATED, url);//, url.toExternalForm());//, 
				//				 doc.getCharacterElement(offset));
				editor.fireHyperlinkUpdate(linkEvent);
			}
			catch (MalformedURLException m)
			{
			}
		}

		/*
		 * Invokes default action on the object in an element
		 */
		private void doObjectAction(JEditorPane editor, Element elem)
		{
			View view = getView(editor, elem);
			if (view != null && view instanceof ObjectView)
			{
				Component comp = ((ObjectView)view).getComponent();
				if (comp != null && comp instanceof Accessible)
				{
					AccessibleContext ac = ((Accessible)comp).getAccessibleContext();
					if (ac != null)
					{
						AccessibleAction aa = ac.getAccessibleAction();
						if (aa != null)
						{
							aa.doAccessibleAction(0);
						}
					}
				}
			}
		}

		/*
		 * Returns the root view for a document
		 */
		private View getRootView(JEditorPane editor)
		{
			return editor.getUI().getRootView(editor);
		}

		/*
		 * Returns a view associated with an element
		 */
		private View getView(JEditorPane editor, Element elem)
		{
			Object lock = lock(editor);
			try
			{
				View rootView = getRootView(editor);
				int start = elem.getStartOffset();
				if (rootView != null)
				{
					return getView(rootView, elem, start);
				}
				return null;
			}
			finally
			{
				unlock(lock);
			}
		}

		private View getView(View parent, Element elem, int start)
		{
			if (parent.getElement() == elem)
			{
				return parent;
			}
			int index = parent.getViewIndex(start, Position.Bias.Forward);

			if (index != -1 && index < parent.getViewCount())
			{
				return getView(parent.getView(index), elem, start);
			}
			return null;
		}

		/*
		 * If possible acquires a lock on the Document. If a lock has been obtained a key will be retured that should be passed to <code>unlock</code>.
		 */
		private Object lock(JEditorPane editor)
		{
			Document document = editor.getDocument();

			if (document instanceof AbstractDocument)
			{
				((AbstractDocument)document).readLock();
				return document;
			}
			return null;
		}

		/*
		 * Releases a lock previously obtained via <code>lock</code>.
		 */
		private void unlock(Object key)
		{
			if (key != null)
			{
				((AbstractDocument)key).readUnlock();
			}
		}

		/*
		 * The operation to perform when this action is triggered.
		 */
		public void actionPerformed(ActionEvent e)
		{

			JTextComponent c = getTextComponent(e);
			if (c.isEditable() || !(c instanceof JEditorPane))
			{
				return;
			}
			JEditorPane editor = (JEditorPane)c;

			Document d = editor.getDocument();
			if (d == null || !(d instanceof HTMLDocument))
			{
				return;
			}
			HTMLDocument doc = (HTMLDocument)d;

			ElementIterator ei = new ElementIterator(doc);
			int currentOffset = editor.getCaretPosition();

			// invoke the next link or object action
			Element currentElement = null;
			while ((currentElement = ei.next()) != null)
			{
				String name = currentElement.getName();
				AttributeSet attr = currentElement.getAttributes();

				Object href = getAttrValue(attr, HTML.Attribute.HREF);
				if (href != null)
				{
					if (currentOffset >= currentElement.getStartOffset() && currentOffset <= currentElement.getEndOffset())
					{

						activateLink((String)href, doc, editor, currentOffset);
						return;
					}
				}
				else if (name.equals(HTML.Tag.OBJECT.toString()))
				{
					Object obj = getAttrValue(attr, HTML.Attribute.CLASSID);
					if (obj != null)
					{
						if (currentOffset >= currentElement.getStartOffset() && currentOffset <= currentElement.getEndOffset())
						{

							doObjectAction(editor, currentElement);
							return;
						}
					}
				}
			}
		}
	}

	/**
	 * @see com.servoy.j2db.dataui.ISupportAsyncLoading#setAsyncLoadingEnabled(boolean)
	 */
	public void setAsyncLoadingEnabled(boolean asyncLoading)
	{
		if (asyncLoading)
		{
			currentFactory = defaultFactory;
		}
		else
		{
			currentFactory = new BasicHTMLViewFactory(new MediaURLStreamHandler(application));
		}
	}
}
