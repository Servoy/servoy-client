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

package com.servoy.j2db.ui.scripting;

import java.io.IOException;
import java.net.URL;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.util.Debug;

/**
 * Abstract scriptable text component with styled content (HTML,RTF)
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeTextEditor<C extends IFieldComponent, T extends JTextComponent> extends AbstractRuntimeTextComponent<C, T> implements
	IScriptTextEditorMethods
{
	public AbstractRuntimeTextEditor(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public void js_setURL(@SuppressWarnings("unused") String url)
	{
	}

	public String js_getURL()
	{
		return null;
	}

	public void js_setBaseURL(String url)
	{
		if (textComponent != null)
		{
			Document document = textComponent.getDocument();
			if (document instanceof HTMLDocument)
			{
				try
				{
					((HTMLDocument)document).setBase(new URL(url));
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
			}
		}
	}

	public String js_getBaseURL()
	{
		if (textComponent != null)
		{
			Document document = textComponent.getDocument();
			if (document instanceof HTMLDocument)
			{
				URL url = ((HTMLDocument)document).getBase();
				if (url != null)
				{
					return url.toString();
				}
			}
		}
		return null;
	}

	public String js_getAsPlainText()
	{
		if (textComponent != null)
		{
			Document doc = textComponent.getDocument();
			if (doc != null)
			{
				try
				{
					return doc.getText(0, doc.getLength());
				}
				catch (BadLocationException e)
				{
					Debug.error(e);
				}
			}
		}
		return null;
	}
}
