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

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TextAction;

/**
 * This is the default implementation of RTF editing functionality. The RTF support was not written by the Swing team. In the future we hope to improve the
 * support provided.
 * 
 * @author Timothy Prinzing (of this class, not the package!)
 */
public class FixedRTFEditorKit extends StyledEditorKit
{

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
			}
		}
	}


	/**
	 * Constructs an RTFEditorKit.
	 */
	public FixedRTFEditorKit()
	{
		super();
	}

	private static final Action[] defaultActions = { new BoldAction(), new ItalicAction(), new UnderlineAction() };

	/**
	 * Fetches the command list for the editor. This is the list of commands supported by the superclass augmented by the collection of commands defined locally
	 * for style operations.
	 * 
	 * @return the command list
	 */
	@Override
	public Action[] getActions()
	{
		return TextAction.augmentList(super.getActions(), FixedRTFEditorKit.defaultActions);
	}


	/**
	 * Get the MIME type of the data that this kit represents support for. This kit supports the type <code>text/rtf</code>.
	 * 
	 * @return the type
	 */
	@Override
	public String getContentType()
	{
		return "text/rtf"; //$NON-NLS-1$
	}

	/**
	 * Insert content from the given stream which is expected to be in a format appropriate for this kind of content handler.
	 * 
	 * @param in The stream to read from
	 * @param doc The destination for the insertion.
	 * @param pos The location in the document to place the content.
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid location within the document.
	 */
	@Override
	public void read(InputStream in, Document doc, int pos) throws IOException, BadLocationException
	{

		if (doc instanceof StyledDocument)
		{
			// PENDING(prinz) this needs to be fixed to
			// insert to the given position.
			RTFReader rdr = new RTFReader((StyledDocument)doc);
			rdr.readFromStream(in);
			rdr.close();
		}
		else
		{
			// treat as text/plain
			super.read(in, doc, pos);
		}
	}

	/**
	 * Write content from a document to the given stream in a format appropriate for this kind of content handler.
	 * 
	 * @param out The stream to write to
	 * @param doc The source for the write.
	 * @param pos The location in the document to fetch the content.
	 * @param len The amount to write out.
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid location within the document.
	 */
	@Override
	public void write(OutputStream out, Document doc, int pos, int len) throws IOException, BadLocationException
	{

		// PENDING(prinz) this needs to be fixed to
		// use the given document range.
		RTFGenerator.writeDocument(doc, out);
	}

	/**
	 * Insert content from the given stream, which will be treated as plain text.
	 * 
	 * @param in The stream to read from
	 * @param doc The destination for the insertion.
	 * @param pos The location in the document to place the content.
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid location within the document.
	 */
	@Override
	public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException
	{

		if (doc instanceof StyledDocument)
		{
			RTFReader rdr = new RTFReader((StyledDocument)doc);
			rdr.readFromReader(in);
			rdr.close();
		}
		else
		{
			// treat as text/plain
			super.read(in, doc, pos);
		}
	}

	/**
	 * Write content from a document to the given stream as plain text.
	 * 
	 * @param out The stream to write to
	 * @param doc The source for the write.
	 * @param pos The location in the document to fetch the content.
	 * @param len The amount to write out.
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid location within the document.
	 */
	@Override
	public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException
	{

		throw new IOException("RTF is an 8-bit format"); //$NON-NLS-1$
	}

}
