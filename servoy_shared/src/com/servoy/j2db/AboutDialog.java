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
public class AboutDialog extends JEscapeDialog implements ActionListener
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final JTextArea ta;
	private final MemoryMonitor mem;
	private final String dialogText;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public AboutDialog(IApplication app)
	{
		super(app.getMainApplicationFrame(), app.getI18NMessage("servoy.aboutDialog.title"), false); //$NON-NLS-1$
//		this.setSize(523,360);//340
		JPanel borderPane = new JPanel();
		borderPane.setLayout(new BorderLayout(10, 10));

		JPanel pane = new JPanel();
		pane.setLayout(new BorderLayout());
		pane.setOpaque(false);
		JLabel logo = new JLabel("     ", app.loadImage("logo2.gif"), SwingConstants.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
		pane.add(logo, BorderLayout.CENTER);
		mem = new MemoryMonitor();
		pane.add(mem, BorderLayout.EAST);

		borderPane.add(pane, BorderLayout.NORTH);
		ta = new JTextArea();
		ta.setMargin(new Insets(3, 3, 3, 3));
		String version = ClientVersion.getVersion() + "-build " + ClientVersion.getReleaseNumber(); //$NON-NLS-1$
		version += "\nJava version " + System.getProperty("java.version") + " (" + System.getProperty("os.name") + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		dialogText = new String(
			app.getApplicationName() +
				"\nVersion " + version + "\n\nCopyright \u00A9 1997-" + Utils.formatTime(System.currentTimeMillis(), "yyyy") + " \nServoy BV\nwww.servoy.com\n\nWarning: This computer program is protected by\ncopyright law and international treaties. Unauthorized\nreproduction or distribution of this program, or any\nportion of it, may result in severe civil and criminal\npenalties, and will be prosecuted to the maximum extent\npossible under law."); //$NON-NLS-1$ //$NON-NLS-2$
		ta.setText(dialogText);
		ta.setOpaque(false);
		ta.setEditable(false);
		ta.setFocusable(false);

		JScrollPane scroller = new JScrollPane(ta);
		borderPane.add(scroller, BorderLayout.CENTER);
		borderPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel helperPanel = new JPanel();
		helperPanel.setLayout(new BorderLayout());
		final JButton button = new JButton(app.getI18NMessage("servoy.button.ok")); //$NON-NLS-1$
		button.addActionListener(this);
		button.setActionCommand("ok"); //$NON-NLS-1$
		helperPanel.add(button, BorderLayout.SOUTH);

		JButton button2 = new JButton(app.getI18NMessage("servoy.aboutDialog.credits")); //$NON-NLS-1$
		button2.addActionListener(this);
		button2.setActionCommand("thanks"); //$NON-NLS-1$
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
		if (command.equals("ok")) cancel(); //$NON-NLS-1$
		if (command.equals("thanks")) thanks(); //$NON-NLS-1$
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
		sb.append("Used Components\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("Kunststoff Look&Feel\n"); //$NON-NLS-1$
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n"); //$NON-NLS-1$
		sb.append("http://www.incors.org\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("Tomcat servlet runner\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 1999, 2000  The Apache Software Foundation\n"); //$NON-NLS-1$
		sb.append("Apache Software License,  Version 1.1\n"); //$NON-NLS-1$
		sb.append("http://www.apache.org\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("Wicket\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 2004, 2005 Wicket developers\n"); //$NON-NLS-1$
		sb.append("Apache Software License,  Version 2.0\n"); //$NON-NLS-1$
		sb.append("http://wicket.sourceforge.net/license.html\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("ANTLR\n"); //$NON-NLS-1$
		sb.append("public domain\n"); //$NON-NLS-1$
		sb.append("http://www.antlr.org/license.html\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("JavaScript Interpreter\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 1997-1999 Netscape Communications Corporation.\n"); //$NON-NLS-1$
		sb.append("Netscape Public License Version 1.1\n"); //$NON-NLS-1$
		sb.append("http://www.mozilla.org/NPL/\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("JFontChooser,JDateChooser\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 1999 Lama Soft\n"); //$NON-NLS-1$
		sb.append("Freeware\n"); //$NON-NLS-1$
		sb.append("http://www.lamatek.com/lamasoft\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("TwoWaySocketFactory\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 2000 Computer System Services, Inc.\n"); //$NON-NLS-1$
		sb.append("Freeware\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("MRJAdapter\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 2003 Steve Roy\n"); //$NON-NLS-1$
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n"); //$NON-NLS-1$
		sb.append("http://www.gnu.org/copyleft/lesser.html\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("JavaServiceWrapper\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 1999, 2003 TanukiSoftware.org\n"); //$NON-NLS-1$
		sb.append("Freeware\n"); //$NON-NLS-1$
		sb.append("http://wrapper.tanukisoftware.org/doc/english/license.html\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("Hibernate\n"); //$NON-NLS-1$
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n"); //$NON-NLS-1$
		sb.append("http://www.hibernate.org\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("JPEGEncoder\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 1998, James R. Weeks and BioElectroMech\n"); //$NON-NLS-1$
		sb.append("http://www.obrador.com/essentialjpeg/acceptance.htm\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("Image Metadata Extractor\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 2006, drewnoakes.com\n"); //$NON-NLS-1$
		sb.append("Freeware\n"); //$NON-NLS-1$
		sb.append("http://www.drewnoakes.com/code/exif/\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("Oracle Java Help\n"); //$NON-NLS-1$
		sb.append("Copyright \u00A9 2002 Oracle\n"); //$NON-NLS-1$
		sb.append("Freeware\n"); //$NON-NLS-1$
		//OHJ and OHW are available for free from this Web site. You may redistribute OHJ as the help system for your application at no cost, and you may implement the OHW servlet at no cost. 
		//As a service to our customers and the software community, Oracle provides Oracle Help software and support for for free. This includes both Oracle Help for Java (OHJ) and Oracle Help for the Web (OHW).
		sb.append("http://technet.oracle.com/tech/java/help/content.html\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		sb.append("BrowserLauncher2\n"); //$NON-NLS-1$
		sb.append("GNU LESSER GENERAL PUBLIC LICENSE Version 2.1, February 1999\n"); //$NON-NLS-1$
		sb.append("http://www.gnu.org/licenses/lgpl.html\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$

		ta.setText(sb.toString());
		ta.setCaretPosition(0);
	}
}
