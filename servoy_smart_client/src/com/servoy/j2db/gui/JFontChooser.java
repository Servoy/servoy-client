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
package com.servoy.j2db.gui;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.WebStart;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.gui.IPropertyEditorDialog;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * A simple FontChooser dialog that implements similar functionality to
 * the JFileChooser, JOptionPane and JColorChooser components provided
 * with Swing.<p>
 * Upon initialization, the JFontChooser polls the system for all available
 * fonts, initializes the various JList components to the values of the 
 * default font and provides a preview of the font. As options are changed/selected
 * the preview is updated to display the current font.<p>
 * JFileChooser can either be created and added to a GUI as a typical
 * JComponent or it can display a JDialog using the {@link #showDialog(Component, String) showDialog}
 * method (just like the <b>JFileChooser</b> component). Objects
 * that extend JFontChooser should use the {@link #acceptSelection() acceptSelection} and
 * {@link #cancelSelection() cancelSelection} methods to determine either
 * an accept or cancel selection.<p>
 * <i>Example:</i>
 * <blockquote>
 * <samp>
 * JFontChooser chooser = new JFontChooser(new Font("Arial", Font.BOLD, 12));
 * if (chooser.showDialog(this, "Choose a font...") == JFontChooser.ACCEPT_OPTION) {
 * 	Font f = chooser.getSelectedFont();
 * 	// Process font here...
 * }
 * </samp></blockquote>
 * <p>
 */

public class JFontChooser extends JComponent implements ActionListener, ListSelectionListener, IPropertyEditorDialog
{
	private static final String DIALOG_NAME = "FontChooserDialog";

	private static final Font DEFAULT = new Font("sansserif", Font.PLAIN, 11); //$NON-NLS-1$
	private static final String DEFAULT_FONT = "sansserif"; //$NON-NLS-1$
	private static Font[] availableFonts;

	private Font font;
	private JList fontNames, fontSizes, fontStyles;
	private JTextField currentSize;
	private JButton okay, cancel, copyButton, defaultButton;
	private JFontPreviewPanel preview;
	private JDialog dialog;
	private int returnValue;

	/**
	 * Constructs a new JFontChooser component initialized to the supplied font object.
	 * @see	com.lamasoft.JFontChooser#showDialog(Component, String)
	 */
	public JFontChooser(Window parent, Font font)
	{
		super();

		this.parent = parent;
		this.font = (font == null ? DEFAULT : font);

		try
		{
			setup();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	private Window parent;

	public JFontChooser(Window parent)
	{
		this(parent, new Font(DEFAULT_FONT, Font.PLAIN, 11));
	}

	public static Font[] getAvailableFonts(Vector names)
	{
		if (availableFonts == null)
		{
			try
			{
				availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				Debug.trace("GraphicsEnvironment only availble under 1.2"); //$NON-NLS-1$
			}
		}
		if (names != null)
		{
			for (Font availableFont : availableFonts)
			{
				String fontName = availableFont.getName();
				if (!names.contains(fontName))
				{
					names.addElement(fontName);
				}
			}
		}
		return availableFonts;
	}

	private void setup()
	{
		setLayout(new BorderLayout(5, 5));

		Vector names = new Vector();
		availableFonts = getAvailableFonts(names);
		fontNames = new FixedJList(names);
		fontNames.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontNames.setCellRenderer(new DefaultListCellRenderer());
		JScrollPane fontNamesScroll = new JScrollPane(fontNames);
		fontNames.addListSelectionListener(this);

		Object[] styles = { "Regular", "Bold", "Italic", "BoldItalic" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		fontStyles = new FixedJList(styles);
		fontStyles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontStyles.setCellRenderer(new DefaultListCellRenderer());
		JScrollPane fontStylesScroll = new JScrollPane(fontStyles);
		fontStyles.setSelectedIndex(0);
		fontStyles.addListSelectionListener(this);

		fontSizes = new FixedJList();
		fontSizes.setModel(createFontSizes());
		fontSizes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontSizes.setCellRenderer(new DefaultListCellRenderer());
		JScrollPane fontSizesScroll = new JScrollPane(fontSizes);
		fontSizes.addListSelectionListener(this);

		currentSize = new JTextField(5);
		currentSize.setText((new Integer(font.getSize())).toString());
		currentSize.addActionListener(this);

		JPanel sizePane = new JPanel(new BorderLayout(5, 5));
		sizePane.add(currentSize, BorderLayout.NORTH);
		sizePane.add(fontSizesScroll, BorderLayout.CENTER);
		JPanel styleSizePane = new JPanel(new BorderLayout(5, 5));
		styleSizePane.add(fontStylesScroll, BorderLayout.WEST);
		styleSizePane.add(sizePane, BorderLayout.CENTER);

		preview = new JFontPreviewPanel(this.font);

		okay = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		okay.addActionListener(this);

		copyButton = new JButton(Messages.getString("servoy.button.copy")); //$NON-NLS-1$
		copyButton.addActionListener(this);
		copyButton.setActionCommand("copy"); //$NON-NLS-1$
		cancel = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancel.addActionListener(this);

		defaultButton = new JButton(Messages.getString("servoy.button.default")); //$NON-NLS-1$
		defaultButton.addActionListener(this);
		defaultButton.setActionCommand("default"); //$NON-NLS-1$

		JPanel top = new JPanel(new BorderLayout(5, 5));
		top.add(fontNamesScroll, BorderLayout.CENTER);
		top.add(styleSizePane, BorderLayout.EAST);

		preview.setBorder(preview.getBorder());
		add("North", top); //$NON-NLS-1$
		add("Center", preview); //$NON-NLS-1$


		setSelectedFont(font);
	}

	public static DefaultComboBoxModel createFontSizes()
	{
		DefaultComboBoxModel dlm = new DefaultComboBoxModel();
		for (int i = 3; i < 72; i++)
			dlm.addElement(new Integer(i));
		return dlm;
	}

	public void setSelectedFont(Font a_font)
	{
		if (a_font == null)
		{
			a_font = UIManager.getFont("Label.font"); //$NON-NLS-1$
			if (a_font == null)
			{
				a_font = new Font(DEFAULT_FONT, Font.PLAIN, 11);
			}
		}
		font = a_font;
		preview.setFont(a_font);
		fontSizes.setSelectedValue((new Integer(a_font.getSize())), true);
		fontNames.setSelectedValue(a_font.getFamily(), true);
		if (a_font.getStyle() == Font.PLAIN) fontStyles.setSelectedValue("Regular", false); //$NON-NLS-1$
		else if (a_font.getStyle() == Font.ITALIC) fontStyles.setSelectedValue("Italic", false); //$NON-NLS-1$
		else if (a_font.getStyle() == Font.BOLD) fontStyles.setSelectedValue("Bold", false); //$NON-NLS-1$
		else if (a_font.getStyle() == (Font.BOLD | Font.ITALIC)) fontStyles.setSelectedValue("BoldItalic", false); //$NON-NLS-1$
	}

	private void updateFont(Font f)
	{
		this.font = f;
		preview.setFont(this.font);
	}

	private void updateFontSize(int size)
	{
		Font f = new Font(font.getName(), font.getStyle(), size);
		updateFont(f);//font.deriveFont((new Integer(size)).floatValue()));
	}

	private void updateFontStyle(int style)
	{
		Font f = new Font(font.getName(), style, font.getSize());
		updateFont(f);// font.deriveFont(style));
	}

	/**
	 * Returns the currently selected font. Typically called after receipt
	 * of an ACCEPT_OPTION (using the {@link #showDialog(Component, String) showDialog} option) or from within the
	 * approveSelection method (using the component option).
	 * @return java.awt.Font A font class that represents the currently selected font.
	 */
	public Font getSelectedFont()
	{
		return font;
	}

	/**
	 * Processes action events from the okay and cancel buttons
	 * as well as the current size TextField. 
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == okay)
		{
			returnValue = OK_OPTION;
			if (dialog != null) dialog.setVisible(false);
		}
		else if (e.getSource() == cancel)
		{
			returnValue = CANCEL_OPTION;
			if (dialog != null) dialog.setVisible(false);
		}
		else if (e.getSource() == copyButton)
		{
			copy();
		}
		else if (e.getSource() == defaultButton)
		{
			font = null;
			returnValue = OK_OPTION;
			if (dialog != null) dialog.setVisible(false);
		}
		else if (e.getSource() == currentSize)
		{
			fontSizes.setSelectedValue(new Integer(currentSize.getText()), true);
		}
	}

	private void copy()
	{
		if (font != null)
		{
			String sfont = PersistHelper.createFontString(font);
			WebStart.setClipboardContent("'" + sfont + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Processes events received from the various JList objects.
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getSource() == fontNames)
		{
			Font f = availableFonts[fontNames.getSelectedIndex()];
			if (font == null) font = DEFAULT;
			f = new Font(f.getName()/* Font */, font.getStyle(), font.getSize());
			updateFont(f);
		}
		if (e.getSource() == fontSizes)
		{
			currentSize.setText(fontSizes.getSelectedValue().toString());
			updateFontSize((new Integer(currentSize.getText())).intValue());
		}
		if (e.getSource() == fontStyles)
		{
			int style = Font.PLAIN;
			String selection = (String)fontStyles.getSelectedValue();
			if (selection.equals("Regular")) //$NON-NLS-1$
			style = Font.PLAIN;
			if (selection.equals("Bold")) //$NON-NLS-1$
			style = Font.BOLD;
			if (selection.equals("Italic")) //$NON-NLS-1$
			style = Font.ITALIC;
			if (selection.equals("BoldItalic")) //$NON-NLS-1$
			style = (Font.BOLD | Font.ITALIC);
			updateFontStyle(style);
		}
	}

	public int showDialog()
	{
		return showDialog(parent, "Font", true); //$NON-NLS-1$
	}

	/**
	 * Pops up a Font chooser dialog with the supplied <i>title</i>, centered
	 * about the component <i>parent</i>.
	 * @return int An integer that equates to the static variables <i>ERROR_OPTION</i>, <i>ACCEPT_OPTION</i> or <i>CANCEL_OPTION</i>.
	 */
	public int showDialog(Window parent, String title, boolean showDefault)
	{
		this.parent = parent;

		returnValue = CANCEL_OPTION;
		if (parent instanceof Frame)
		{
			dialog = new JEscapeDialog((Frame)parent, title, true)
			{
				@Override
				protected void cancel()
				{
					setVisible(false);
				}
			};
		}
		else
		{
			dialog = new JEscapeDialog((Dialog)parent, title, true)
			{
				@Override
				protected void cancel()
				{
					setVisible(false);
				}
			};
		}
		dialog.setName(DIALOG_NAME);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		dialog.getContentPane().add(this, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(copyButton);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okay);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(cancel);
		if (showDefault)
		{
			buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
			buttonPane.add(defaultButton);
		}
		dialog.getContentPane().add(buttonPane, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		setSelectedFont(font);
		dialog.setVisible(true);
		return returnValue;
	}
}

class JFontPreviewPanel extends JLabel
{

	public JFontPreviewPanel(Font f)
	{
		super();
		setOpaque(true);
		setFont(f);
		setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), Messages.getString("servoy.general.preview"))); //$NON-NLS-1$
	}


	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		setText(f.getName());
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(getSize().width, 75);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}
}
