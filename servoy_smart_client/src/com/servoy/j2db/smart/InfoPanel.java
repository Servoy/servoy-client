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
package com.servoy.j2db.smart;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.gui.FixedHTMLEditorKit;

public class InfoPanel extends JPanel implements HyperlinkListener, MouseListener, Runnable
{
	private final IApplication app;
	private final URL url;
	private final JEditorPane editorPane;
	private int time_to_show;

	public InfoPanel(IApplication app, URL url, int time_to_show)
	{
		this.app = app;
		this.url = url;
		this.time_to_show = time_to_show;
		setVisible(false);
		this.setLayout(new BorderLayout());

		editorPane = new JEditorPane();
		editorPane.setEditable(false);
		FixedHTMLEditorKit kit = new FixedHTMLEditorKit(app);
		editorPane.setEditorKit(kit);
		editorPane.addHyperlinkListener(this);
		editorPane.setBorder(null);
		editorPane.setOpaque(false);
		this.add(editorPane, BorderLayout.CENTER);
		JLabel closeButton = new JLabel(app.getI18NMessage("servoy.button.close")); //$NON-NLS-1$
		closeButton.setBorder(null);
		closeButton.addMouseListener(this);
		closeButton.setOpaque(false);
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		buttonPane.add(closeButton);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.setOpaque(false);
		setOpaque(false);

		this.add(buttonPane, BorderLayout.SOUTH);
		app.getScheduledExecutor().execute(this);
	}

	public void run()
	{
		try
		{
			editorPane.setPage(url);
			app.invokeLater(new Runnable()
			{
				public void run()
				{
					setVisible(true);
				}
			});
			if (time_to_show < 3000) time_to_show = 3000;
			Thread.sleep(time_to_show);
		}
		catch (Exception e)
		{
			Debug.trace(e);
		}
		close();
	}

	public void mousePressed(MouseEvent e)
	{
		close();
	}

	public void close()
	{
		app.invokeLater(new Runnable()
		{
			public void run()
			{
				setVisible(false);
				if (getParent() != null) getParent().remove(InfoPanel.this);
			}
		});
	}

	public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent)
	{
		HyperlinkEvent.EventType type = hyperlinkEvent.getEventType();
		if (type == HyperlinkEvent.EventType.ACTIVATED)
		{
			try
			{
				URL url = hyperlinkEvent.getURL();
				if (url == null) return;
				app.showURL(url.toString(), null, null, 0, true);
				close();
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (type == HyperlinkEvent.EventType.ENTERED)
		{
			editorPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else
		{
			editorPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}
}
