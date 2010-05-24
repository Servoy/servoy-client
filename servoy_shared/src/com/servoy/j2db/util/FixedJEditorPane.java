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

import java.awt.Rectangle;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.Executor;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;

/**
 * A modified JEditorPane; it makes possible async loading for html documents without using setPage(URL) (in most cases the content will be provided via
 * InputStream)
 * 
 * @author Paul
 */

public class FixedJEditorPane extends JEditorPane
{
	private final Executor exe;
	public static final String CHARSET_DIRECTIVE = "charset"; //$NON-NLS-1$


	public FixedJEditorPane(Executor exe)
	{
		super();
		this.exe = exe;
	}

	/**
	 * a FilterInputStream used by the loader
	 */
	private PageStream loading;

	@Override
	public void read(InputStream in, Object desc) throws IOException
	{
		if (desc == null)
		{
			read(in);
		}
		else
		{
			EditorKit kit = this.getEditorKit();
			if (desc instanceof HTMLDocument && kit instanceof FixedHTMLEditorKit)
			{
				HTMLDocument hdoc = (HTMLDocument)desc;
				Document doc = getDocument();
				try
				{
					doc.remove(0, doc.getLength());
				}
				catch (BadLocationException e)
				{
					throw new RuntimeException("Can't set document:", e); //$NON-NLS-1$
				}
				setDocument(hdoc);
				super.read(in, hdoc);
				/** I don't need it now. I just keep it for further reviews */
//				firePropertyChange("document", doc, hdoc);
			}
			else
			{
				String charset = (String)getClientProperty(CHARSET_DIRECTIVE);
				Reader r = (charset != null) ? new InputStreamReader(in, charset) : new InputStreamReader(in);
				super.read(r, desc);
			}
		}
	}

	/**
	 * the method does not override the <code>super.read</code> method... it's only used by the async read method
	 * 
	 * @param in
	 * @param doc
	 * @throws IOException
	 */
	private void read(InputStream in, Document doc) throws IOException
	{
		try
		{
			EditorKit kit = this.getEditorKit();
			putClientProperty(CHARSET_DIRECTIVE, "UTF-8");
			String charset = (String)getClientProperty(CHARSET_DIRECTIVE);
			Reader reader = (charset != null) ? new InputStreamReader(in, charset) : new InputStreamReader(in);
			doc.putProperty("IgnoreCharsetDirective", new Boolean(true));
			kit.read(reader, doc, 0);
		}
		catch (BadLocationException e)
		{
			throw new IOException(e.getMessage());
		}
		catch (ChangedCharSetException ccse)
		{
			String charSetSpec = ccse.getCharSetSpec();
			if (ccse.keyEqualsCharSet())
			{
				putClientProperty(CHARSET_DIRECTIVE, charSetSpec);
			}
			else
			{
				setCharsetFromContentTypeParameters(charSetSpec);
			}

			in.reset();
			try
			{
				doc.remove(0, doc.getLength());
			}
			catch (BadLocationException e)
			{
			}
			doc.putProperty("IgnoreCharsetDirective", new Boolean(true)); //$NON-NLS-1$
			read(in, doc);
		}
	}

	private void setCharsetFromContentTypeParameters(String paramlist)
	{
		String charset = null;
		try
		{
			int semi = paramlist.indexOf(';');
			if (semi > -1 && semi < paramlist.length() - 1)
			{
				paramlist = paramlist.substring(semi + 1);
			}

			if (paramlist.length() > 0)
			{
				// try to find charset directive from header
				HeaderParser hdrParser = new HeaderParser(paramlist);
				charset = hdrParser.findValue(CHARSET_DIRECTIVE);
				if (charset != null)
				{
					putClientProperty(CHARSET_DIRECTIVE, charset);
				}
			}
		}
		catch (Exception e)
		{
		}
	}


	/**
	 * makes an asynchronous read; the method is "derived" from <code>JEditorPane.setPage</code>
	 * 
	 * @param in
	 */
	public void read(InputStream in) throws IOException
	{
		scrollRectToVisible(new Rectangle(0, 0, 1, 1));
		if (getEditorKit() != null)
		{
			Document doc = getEditorKit().createDefaultDocument();

			doc.putProperty(Document.StreamDescriptionProperty, in);
			synchronized (this)
			{
				if (loading != null)
				{
					/** we are loading asynchronously, so we need to cancel the old stream */
					loading.cancel();
					loading = null;
				}
			}

			/**
			 * Asynchronous Loading: first setDocument() then create thread that loads content from input stream (via parser) see also
			 * AbstractDocument.readObject + readLock/readUnlock
			 * 
			 * asynchronous loading is done by setting doc.setAsynchronousLoadPriority(priority_value) in <code>EditorKit.createDefaultDocument</code> where 1 <=
			 * priority_value <= 10
			 */
			if (doc instanceof AbstractDocument)
			{
				AbstractDocument adoc = (AbstractDocument)doc;
				int p = adoc.getAsynchronousLoadPriority();
				if (p >= Thread.MIN_PRIORITY)
				{
					if (p > Thread.MAX_PRIORITY) p = Thread.MAX_PRIORITY;
					setDocument(doc);
					synchronized (this)
					{
						loading = new PageStream(in);
						PageLoader pl = new PageLoader(doc, loading);
						exe.execute(pl);
					}
					return;
				}
			}
			/** Synchronous Loading: see how setDocument is called after reading the stream: that makes the difference */
			/** I keep the original implementation of the read */
			super.read(in, doc);
			setDocument(doc);
		}
	}


	/**
	 * A modified page loader for asynchronous document loading
	 * 
	 * @author Paul
	 */
	class PageLoader implements Runnable
	{
		/** The stream to load the document with */
		InputStream in;

		/**
		 * The Document instance to load into. This is cached in case a new Document is created between the time the thread this is created and run.
		 */
		Document doc;

		/**
		 * Construct an asynchronous page loader.
		 */
		PageLoader(Document doc, InputStream in)
		{
			this.in = in;
			this.doc = doc;
		}

		/**
		 * Try to load the document, then scroll the view to the reference (if specified). When done, fire a page property change event.
		 */
		public void run()
		{
			try
			{
				read(in, doc);
				synchronized (FixedJEditorPane.this)
				{
					loading = null;
				}
				//scroll if necessary
				Runnable callScrollToReference = new Runnable()
				{
					public void run()
					{
						if (doc instanceof HTMLDocument)
						{
							// I'll leave this for the moment... in case I want to review later
//							HTMLDocument d = (HTMLDocument)doc;
//							HTMLDocument.Iterator iter = d.getIterator(HTML.Tag.HTML);
//							if (iter.isValid())
//							{
//								iter.next();
//								int start = iter.getStartOffset();
//								try
//								{
//									Rectangle r = modelToView(0);
//									if (r != null)
//									{
//										Rectangle vis = getVisibleRect();
//										r.height = vis.height;
//										scrollRectToVisible(r);
//									}
//								}
//								catch (BadLocationException ble)
//								{
//									UIManager.getLookAndFeel().provideErrorFeedback(FixedJEditorPane.this);
//								}
//							}
						}
					}
				};
//				SwingUtilities.invokeLater(callScrollToReference);
			}
			catch (IOException ioe)
			{
				UIManager.getLookAndFeel().provideErrorFeedback(FixedJEditorPane.this);
			}
			finally
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						/** causes null pointer exception and now I don't need it. I just keep it for further reviews */
//						firePropertyChange("document", null, doc);
					}
				});
			}
		}

	}

	static class PageStream extends FilterInputStream
	{

		boolean canceled;

		public PageStream(InputStream i)
		{
			super(i);
			canceled = false;
		}

		/**
		 * Cancel the loading of the stream by throwing an IOException on the next request.
		 */
		public synchronized void cancel()
		{
			canceled = true;
		}

		protected synchronized void checkCanceled() throws IOException
		{
			if (canceled)
			{
				throw new IOException("page canceled");
			}
		}

		@Override
		public int read() throws IOException
		{
			checkCanceled();
			return super.read();
		}

		@Override
		public long skip(long n) throws IOException
		{
			checkCanceled();
			return super.skip(n);
		}

		@Override
		public int available() throws IOException
		{
			checkCanceled();
			return super.available();
		}

		@Override
		public void reset() throws IOException
		{
			checkCanceled();
			super.reset();
		}

	}


	/**
	 * class used for parsing the header after charset directive
	 * 
	 * @author paul
	 * 
	 */
	class HeaderParser
	{
		String raw;
		String[][] tab;

		public HeaderParser(String raw)
		{
			this.raw = raw;
			tab = new String[10][2];
			parse();
		}

		private void parse()
		{

			if (raw != null)
			{
				raw = raw.trim();
				char[] ca = raw.toCharArray();
				int beg = 0, end = 0, i = 0;
				boolean inKey = true;
				boolean inQuote = false;
				int len = ca.length;
				while (end < len)
				{
					char c = ca[end];
					if (c == '=')
					{
						tab[i][0] = new String(ca, beg, end - beg).toLowerCase();
						inKey = false;
						end++;
						beg = end;
					}
					else if (c == '\"')
					{
						if (inQuote)
						{
							tab[i++][1] = new String(ca, beg, end - beg);
							inQuote = false;
							do
							{
								end++;
							}
							while (end < len && (ca[end] == ' ' || ca[end] == ','));
							inKey = true;
							beg = end;
						}
						else
						{
							inQuote = true;
							end++;
							beg = end;
						}
					}
					else if (c == ' ' || c == ',')
					{
						if (inQuote)
						{
							end++;
							continue;
						}
						else if (inKey)
						{
							tab[i++][0] = (new String(ca, beg, end - beg)).toLowerCase();
						}
						else
						{
							tab[i++][1] = (new String(ca, beg, end - beg));
						}
						while (end < len && (ca[end] == ' ' || ca[end] == ','))
						{
							end++;
						}
						inKey = true;
						beg = end;
					}
					else
					{
						end++;
					}
				}
				// get last key/val, if any
				if (--end > beg)
				{
					if (!inKey)
					{
						if (ca[end] == '\"')
						{
							tab[i++][1] = (new String(ca, beg, end - beg));
						}
						else
						{
							tab[i++][1] = (new String(ca, beg, end - beg + 1));
						}
					}
					else
					{
						tab[i][0] = (new String(ca, beg, end - beg + 1)).toLowerCase();
					}
				}
				else if (end == beg)
				{
					if (!inKey)
					{
						if (ca[end] == '\"')
						{
							tab[i++][1] = String.valueOf(ca[end - 1]);
						}
						else
						{
							tab[i++][1] = String.valueOf(ca[end]);
						}
					}
					else
					{
						tab[i][0] = String.valueOf(ca[end]).toLowerCase();
					}
				}
			}

		}

		public String findKey(int i)
		{
			if (i < 0 || i > 10) return null;
			return tab[i][0];
		}

		public String findValue(int i)
		{
			if (i < 0 || i > 10) return null;
			return tab[i][1];
		}

		public String findValue(String key)
		{
			return findValue(key, null);
		}

		public String findValue(String k, String Default)
		{
			if (k == null) return Default;
			k.toLowerCase();
			for (int i = 0; i < 10; ++i)
			{
				if (tab[i][0] == null)
				{
					return Default;
				}
				else if (k.equals(tab[i][0]))
				{
					return tab[i][1];
				}
			}
			return Default;
		}

		public int findInt(String k, int Default)
		{
			try
			{
				return Integer.parseInt(findValue(k, String.valueOf(Default)));
			}
			catch (Throwable t)
			{
				return Default;
			}
		}
	}


}
