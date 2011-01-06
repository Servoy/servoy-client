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
package com.servoy.j2db;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.servoy.j2db.util.JEscapeDialog;
import com.servoy.j2db.util.MemoryMonitor;
import com.servoy.j2db.util.Utils;

/**
 * Shows about info
 * 
 * @author jblok
 */
@SuppressWarnings("nls")
public class AboutDialog extends JEscapeDialog implements ActionListener
{
	private final JTextArea ta;
	private final MemoryMonitor mem;
	private final String dialogText;

	public AboutDialog(IApplication app)
	{
		super(app.getMainApplicationFrame(), app.getI18NMessage("servoy.aboutDialog.title"), false);
//		this.setSize(523,360);//340
		JPanel borderPane = new JPanel();
		borderPane.setLayout(new BorderLayout(10, 10));

		JPanel pane = new JPanel();
		pane.setLayout(new BorderLayout());
		pane.setOpaque(false);
		JLabel logo = new JLabel("     ", app.loadImage("logo2.gif"), SwingConstants.RIGHT);
		pane.add(logo, BorderLayout.CENTER);
		mem = new MemoryMonitor();
		pane.add(mem, BorderLayout.EAST);

		borderPane.add(pane, BorderLayout.NORTH);
		ta = new JTextArea();
		ta.setMargin(new Insets(3, 3, 3, 3));
		String version = ClientVersion.getVersion() + "-build " + ClientVersion.getReleaseNumber();
		version += "\nJava version " + System.getProperty("java.version") + " (" + System.getProperty("os.name") + ")";//$NON-NLS-2$
		dialogText = new String(
			app.getApplicationName() +
				"\nVersion " +
				version +
				"\n\nCopyright \u00A9 1997-" +
				Utils.formatTime(System.currentTimeMillis(), "yyyy") +
				" \nServoy BV\nwww.servoy.com\n\nWarning: This computer program is protected by\ncopyright law and international treaties. Unauthorized\nreproduction or distribution of this program, or any\nportion of it, may result in severe civil and criminal\npenalties, and will be prosecuted to the maximum extent\npossible under law.");
		ta.setText(dialogText);
		ta.setOpaque(false);
		ta.setEditable(false);
		ta.setFocusable(false);

		JScrollPane scroller = new JScrollPane(ta);
		borderPane.add(scroller, BorderLayout.CENTER);
		borderPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel helperPanel = new JPanel();
		helperPanel.setLayout(new BorderLayout());
		final JButton button = new JButton(app.getI18NMessage("servoy.button.ok"));
		button.addActionListener(this);
		button.setActionCommand("ok");
		helperPanel.add(button, BorderLayout.SOUTH);

		JButton button2 = new JButton(app.getI18NMessage("servoy.aboutDialog.credits"));
		button2.addActionListener(this);
		button2.setActionCommand("thanks");
		helperPanel.add(button2, BorderLayout.NORTH);
		helperPanel.setOpaque(false);

		borderPane.add(helperPanel, BorderLayout.EAST);
		borderPane.setBackground(Color.white);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(borderPane, BorderLayout.CENTER);
		getRootPane().setDefaultButton(button);

//		loadBounds("AboutDialog");
		pack();
		this.setResizable(false);
		mem.start();
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("ok")) cancel();
		if (command.equals("thanks")) thanks();
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */

	@Override
	public void cancel()
	{
		ta.setText(dialogText);
		mem.stop();
		setVisible(false);
		dispose();
	}

	public void thanks()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Used server libs\n");
		sb.append("\n");

		sb.append("Tomcat\n");
		sb.append("Copyright \u00A9 1999, 2000  The Apache Software Foundation\n");
		sb.append("Apache Software License,  Version 1.1\n");
		sb.append("http://www.apache.org\n");
		sb.append("\n");

		sb.append("Wicket\n");
		sb.append("Copyright \u00A9 1999, 2000  The Apache Software Foundation\n");
		sb.append("Apache Software License,  Version 2.0\n");
		sb.append("http://wicket.apache.org\n");
		sb.append("\n");

		sb.append("Apache commons\n");
		sb.append("Copyright \u00A9 1999, 2000  The Apache Software Foundation\n");
		sb.append("Apache Software License,  Version 2.0\n");
		sb.append("http://commons.apache.org\n");
		sb.append("\n");

		sb.append("Hibernate\n");
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n");
		sb.append("http://www.hibernate.org\n");
		sb.append("\n");

		sb.append("ANTLR\n");
		sb.append("public domain\n");
		sb.append("http://www.antlr.org/license.html\n");
		sb.append("\n");

		sb.append("TwoWaySocketFactory\n");
		sb.append("Copyright \u00A9 2000 Computer System Services, Inc.\n");
		sb.append("Freeware\n");
		sb.append("\n");

		sb.append("Joda time\n");
		sb.append("Apache Software License,  Version 2.0\n");
		sb.append("http://joda-time.sourceforge.net/license.html\n");
		sb.append("\n");

		sb.append("JUG\n");
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n");
		sb.append("http://jug.safehaus.org\n");
		sb.append("\n");

		sb.append("Jabsorb\n");
		sb.append("Apache Software License,  Version 2.0\n");
		sb.append("http://jabsorb.org\n");
		sb.append("\n");

		sb.append("Used client libs\n");
		sb.append("\n");

		sb.append("Kunststoff Look&Feel\n");
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n");
		sb.append("http://www.incors.org\n");
		sb.append("\n");

		sb.append("Rhino JavaScript Interpreter\n");
		sb.append("Copyright \u00A9 1997-1999 Netscape Communications Corporation.\n");
		sb.append("Netscape Public License Version 1.1\n");
		sb.append("http://www.mozilla.org/NPL/\n");
		sb.append("\n");

		sb.append("JFontChooser,JDateChooser\n");
		sb.append("Copyright \u00A9 1999 Lama Soft\n");
		sb.append("Freeware\n");
		sb.append("\n");

		sb.append("MRJAdapter\n");
		sb.append("Copyright \u00A9 2003 Steve Roy\n");
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n");
		sb.append("http://www.gnu.org/copyleft/lesser.html\n");
		sb.append("\n");

		sb.append("JPEGEncoder\n");
		sb.append("Copyright \u00A9 1998, James R. Weeks and BioElectroMech\n");
		sb.append("http://www.obrador.com/essentialjpeg/acceptance.htm\n");
		sb.append("\n");

		sb.append("Image Metadata Extractor\n");
		sb.append("Copyright \u00A9 2006, drewnoakes.com\n");
		sb.append("Freeware\n");
		sb.append("http://www.drewnoakes.com/code/exif/\n");
		sb.append("\n");

		sb.append("iText\n");
		sb.append("Copyright (C) 2000, Lowagie Bruno\n");
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE\n");
		sb.append("http://itextpdf.com\n");
		sb.append("\n");

		sb.append("BrowserLauncher2\n");
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n");
		sb.append("http://www.gnu.org/licenses/lgpl.html\n");
		sb.append("\n");

		sb.append("Used developer libs\n");
		sb.append("\n");

		sb.append("Eclipse\n");
		sb.append("Eclipse Public License v1.0\n");
		sb.append("http://www.eclipse.org/legal/epl-v10.html\n");
		sb.append("\n");

		sb.append("DLTK\n");
		sb.append("Eclipse Public License v1.0\n");
		sb.append("http://www.eclipse.org/legal/epl-v10.html\n");
		sb.append("\n");

		ta.setText(sb.toString());
		ta.setCaretPosition(0);
	}
}
