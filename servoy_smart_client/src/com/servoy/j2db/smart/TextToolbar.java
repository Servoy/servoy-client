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


import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.StyledEditorKit.StyledTextAction;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.gui.CustomColorChooserDialog;
import com.servoy.j2db.gui.JFontChooser;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FixedHTMLEditorKit;
import com.servoy.j2db.util.rtf.FixedRTFEditorKit;
import com.servoy.j2db.util.toolbar.Toolbar;
import com.servoy.j2db.util.toolbar.ToolbarButton;
import com.servoy.j2db.util.toolbar.ToolbarToggleButton;

/**
 * Swing texttool bar for use in windows and non modal dialogs
 * @author jblok
 */
public class TextToolbar extends Toolbar implements ActionListener
{
	private HashMap actions = new HashMap();
	private final ISmartClientApplication application;
	private final Font[] allFonts;
	private final Vector allFontNames;

	private final JComboBox styleCombo;
	private final JComboBox heightCombo;

	private final ToolbarToggleButton bold;
	private final ToolbarToggleButton italic;
	private final ToolbarToggleButton underline;
	private final ToolbarToggleButton left;
	private final ToolbarToggleButton hcenter;
	private final ToolbarToggleButton right;
	private final ToolbarButton fcolor;

	private final ToolbarButton moreCmds;
	private final JPopupMenu moreCmdsMenu;

	public TextToolbar(ISmartClientApplication app)
	{
		this(app, null);
	}

	public TextToolbar(ISmartClientApplication app, Map sourceActions)
	{
		super("text", Messages.getString("servoy.texttoolbar.label"), true); //$NON-NLS-1$ //$NON-NLS-2$
		application = app;

		allFontNames = new Vector();
		allFonts = JFontChooser.getAvailableFonts(allFontNames);

		JButton mi = null;
		Action action = null;

		if (sourceActions != null)
		{
			action = (Action)sourceActions.get("cmdspell"); //$NON-NLS-1$
			if (action != null)
			{
				mi = new ToolbarButton(action);
				add(mi);

				addSeparator();
			}
		}

		styleCombo = new JComboBox(allFontNames);
		styleCombo.setPreferredSize(new Dimension(150, ToolbarButton.PREF_HEIGHT));
		styleCombo.setMaximumSize(new Dimension(150, ToolbarButton.PREF_HEIGHT));
		styleCombo.addActionListener(this);
		styleCombo.setRequestFocusEnabled(false);
		add(styleCombo);

		heightCombo = new JComboBox();
		heightCombo.setMaximumSize(new Dimension(50, ToolbarButton.PREF_HEIGHT));
		heightCombo.setPreferredSize(new Dimension(50, ToolbarButton.PREF_HEIGHT));
		heightCombo.addActionListener(this);
		heightCombo.setRequestFocusEnabled(false);
		add(heightCombo);

		addSeparator();

		bold = new ToolbarToggleButton(application.loadImage("bold.gif")); //$NON-NLS-1$
		bold.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.bold")); //$NON-NLS-1$
		bold.setActionCommand("bold-font"); //$NON-NLS-1$
		bold.addActionListener(this);
		add(bold);

		italic = new ToolbarToggleButton(application.loadImage("italic.gif")); //$NON-NLS-1$
		italic.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.italic")); //$NON-NLS-1$
		italic.setActionCommand("italic-font"); //$NON-NLS-1$
		italic.addActionListener(this);
		add(italic);

		underline = new ToolbarToggleButton(application.loadImage("underline.gif")); //$NON-NLS-1$
		underline.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.underline")); //$NON-NLS-1$
		underline.setActionCommand("underline-font"); //$NON-NLS-1$
		underline.addActionListener(this);
		add(underline);

		addSeparator();

		ButtonGroup bg = new ButtonGroup();

		left = new ToolbarToggleButton(application.loadImage("left.gif")); //$NON-NLS-1$
		left.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.alignleft")); //$NON-NLS-1$
		left.setActionCommand("left-justify"); //$NON-NLS-1$
		left.addActionListener(this);
		bg.add(left);
		add(left);

		hcenter = new ToolbarToggleButton(application.loadImage("center.gif")); //$NON-NLS-1$
		hcenter.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.aligncenter")); //$NON-NLS-1$
		hcenter.setActionCommand("center-justify"); //$NON-NLS-1$
		hcenter.addActionListener(this);
		bg.add(hcenter);
		add(hcenter);

		right = new ToolbarToggleButton(application.loadImage("right.gif")); //$NON-NLS-1$
		right.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.alignright")); //$NON-NLS-1$
		right.setActionCommand("right-justify"); //$NON-NLS-1$
		right.addActionListener(this);
		bg.add(right);
		add(right);

		addSeparator();

		fcolor = new ToolbarButton(application.loadImage("font_color.gif")); //$NON-NLS-1$
		fcolor.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.fontcolor")); //$NON-NLS-1$
		fcolor.addActionListener(this);
		add(fcolor);

		moreCmds = new ToolbarButton(application.loadImage("view_menu.gif")); //$NON-NLS-1$
		moreCmds.setToolTipText(Messages.getString("servoy.texttoolbar.tooltip.more")); //$NON-NLS-1$
		moreCmds.setActionCommand("more"); //$NON-NLS-1$
		moreCmds.addActionListener(this);
		add(moreCmds);

		moreCmdsMenu = new JPopupMenu();

		setEnabled(false);

		JMenuItem InsertSup = new JMenuItem(Messages.getString("servoy.texttoolbar.menuitem.superscript")); //$NON-NLS-1$
		InsertSup.setActionCommand("InsertSup"); //$NON-NLS-1$
		InsertSup.addActionListener(this);
		moreCmdsMenu.add(InsertSup);

		JMenuItem InsertSub = new JMenuItem(Messages.getString("servoy.texttoolbar.menuitem.subscript")); //$NON-NLS-1$
		InsertSub.setActionCommand("InsertSub"); //$NON-NLS-1$
		InsertSub.addActionListener(this);
		moreCmdsMenu.add(InsertSub);

		JMenuItem pre = new JMenuItem(Messages.getString("servoy.texttoolbar.menuitem.preformatted")); //$NON-NLS-1$
		pre.setActionCommand("InsertPre"); //$NON-NLS-1$
		pre.addActionListener(this);
		moreCmdsMenu.add(pre);

		JMenuItem InsertOrderedList = new JMenuItem(Messages.getString("servoy.texttoolbar.menuitem.orderedlist"), app.loadImage("numbering.gif")); //$NON-NLS-1$//$NON-NLS-2$
		InsertOrderedList.setActionCommand("InsertOrderedList"); //$NON-NLS-1$
		InsertOrderedList.addActionListener(this);
		moreCmdsMenu.add(InsertOrderedList);//

		JMenuItem InsertUnorderedList = new JMenuItem(Messages.getString("servoy.texttoolbar.menuitem.unorderedlist"), app.loadImage("bulleting.gif")); //$NON-NLS-1$//$NON-NLS-2$
		InsertUnorderedList.setActionCommand("InsertUnorderedList"); //$NON-NLS-1$
		InsertUnorderedList.addActionListener(this);
		moreCmdsMenu.add(InsertUnorderedList);//

		JMenuItem InsertHR = new JMenuItem(Messages.getString("servoy.texttoolbar.menuitem.horizontalline")); //$NON-NLS-1$
		InsertHR.setActionCommand("InsertHR"); //$NON-NLS-1$
		InsertHR.addActionListener(this);
		moreCmdsMenu.add(InsertHR);//
	}

	/*
	 * _____________________________________________________________ Methods/attribs for styled text components
	 */
	private boolean isAdjusting = false;
	private DefaultComboBoxModel defaultFontSizes;

	public void setTextComponent(JEditorPane editor)
	{
		setupable = null;
		try
		{
			isAdjusting = true;

			actions = new HashMap();

			left.setEnabled(false);
			hcenter.setEnabled(false);
			right.setEnabled(false);
			bold.setEnabled(false);
			italic.setEnabled(false);
			underline.setEnabled(false);
			heightCombo.setEnabled(false);
			styleCombo.setEnabled(false);
			moreCmds.setEnabled(false);
			fcolor.setEnabled(false);

			if (editor == null) return;

			moreCmds.setEnabled(editor.getEditorKit() instanceof FixedHTMLEditorKit);

			if (defaultFontSizes == null)
			{
				defaultFontSizes = new DefaultComboBoxModel();
				defaultFontSizes.addElement(new Integer(3));
				defaultFontSizes.addElement(new Integer(4));
				defaultFontSizes.addElement(new Integer(5));
				defaultFontSizes.addElement(new Integer(6));
				defaultFontSizes.addElement(new Integer(7));
				defaultFontSizes.addElement(new Integer(8));
				defaultFontSizes.addElement(new Integer(9));
				defaultFontSizes.addElement(new Integer(10));
				defaultFontSizes.addElement(new Integer(11));
				defaultFontSizes.addElement(new Integer(12));
				defaultFontSizes.addElement(new Integer(13));
				defaultFontSizes.addElement(new Integer(14));
				defaultFontSizes.addElement(new Integer(15));
				defaultFontSizes.addElement(new Integer(16));
				defaultFontSizes.addElement(new Integer(17));
				defaultFontSizes.addElement(new Integer(18));
				defaultFontSizes.addElement(new Integer(19));
				defaultFontSizes.addElement(new Integer(20));
				defaultFontSizes.addElement(new Integer(21));
				defaultFontSizes.addElement(new Integer(22));
				defaultFontSizes.addElement(new Integer(23));
				defaultFontSizes.addElement(new Integer(24));
				defaultFontSizes.addElement(new Integer(36));
				defaultFontSizes.addElement(new Integer(48));
			}
			heightCombo.setModel(defaultFontSizes);
			//		heightCombo.setEditable(true); does not work looses focus
			Action[] actionArray = editor.getActions();
			for (Action element : actionArray)
			{
				String name = (String)element.getValue(Action.NAME);
				actions.put(name, element);
				if ("left-justify".equals(name)) //$NON-NLS-1$
				{
					left.setEnabled(true);
				}
				else if ("right-justify".equals(name)) //$NON-NLS-1$
				{
					right.setEnabled(true);
				}
				else if ("center-justify".equals(name)) //$NON-NLS-1$
				{
					hcenter.setEnabled(true);
				}
				else if ("font-bold".equals(name)) //$NON-NLS-1$
				{
					bold.setEnabled(true);
				}
				else if ("font-italic".equals(name)) //$NON-NLS-1$
				{
					italic.setEnabled(true);
				}
				else if ("font-underline".equals(name)) //$NON-NLS-1$
				{
					underline.setEnabled(true);
				}
			}
			heightCombo.setEnabled(true);
			styleCombo.setEnabled(true);
			fcolor.setEnabled(true);
			setSelectedFont(editor.getFont());
		}
		finally
		{
			isAdjusting = false;
		}
	}

	public void actionRuntimePerformed(ActionEvent event)
	{
		Action a = null;

		Object source = event.getSource();
		if (source == heightCombo)
		{
			Object val = heightCombo.getSelectedItem();
			int size = Utils.getAsInteger(val);
			if (size <= 0) size = 10;
			a = (Action)actions.get("font-size-" + size); //$NON-NLS-1$
			if (a == null)
			{
				a = new StyledEditorKit.FontSizeAction("font-size-custom", size); //$NON-NLS-1$
			}
		}
		else if (source == styleCombo)
		{
			a = new StyledEditorKit.FontFamilyAction("set-fontfamily", allFonts[styleCombo.getSelectedIndex()].getFamily()); //$NON-NLS-1$
		}
		else if (source == fcolor)
		{
			Window parent = SwingUtilities.getWindowAncestor(this);
			if (parent == null) parent = application.getMainApplicationFrame();

			CustomColorChooserDialog ccd = (CustomColorChooserDialog)application.getWindow("CustomColorChooserDialog"); //$NON-NLS-1$
			if (ccd == null || ccd.getOwner() != parent)
			{
				if (parent instanceof Frame)
				{
					ccd = new CustomColorChooserDialog((Frame)parent, application);
				}
				else if (parent instanceof Dialog)
				{
					ccd = new CustomColorChooserDialog((Dialog)parent, application);
				}
				application.registerWindow("CustomColorChooserDialog", ccd); //$NON-NLS-1$
			}
			Color c = ccd.showDialog(Color.black);
			if (c != null)
			{
				a = new ForegroundSetWithAttributeFilteringAction("set-foreground", c);//$NON-NLS-1$
			}
		}
		else if (source == moreCmds)
		{
			moreCmdsMenu.show(moreCmds, 0, moreCmds.getHeight());
		}
		else
		{
			String cmd = event.getActionCommand();
			if (cmd != null)
			{
				a = (Action)actions.get(cmd);
			}
		}

		if (a != null)
		{
			a.actionPerformed(event);
		}
	}

	/*
	 * _____________________________________________________________ Methods/attribs for designer
	 */

	private ISupportTextSetup setupable;
	private ChangeListener changeListener;

	private void setSelectedFont(Font f)
	{
		if (f == null)
		{
			f = UIManager.getFont("Label.font"); //$NON-NLS-1$
		}
		if (f == null) return;

		heightCombo.setSelectedItem(new Integer(f.getSize()));
		bold.setSelected((f.getStyle() & Font.BOLD) == Font.BOLD);
		italic.setSelected((f.getStyle() & Font.ITALIC) == Font.ITALIC);

		for (int i = 0; i < allFontNames.size(); i++)
		{
			String name = (String)allFontNames.elementAt(i);
			if (name.equals(f.getFamily()))
			{
				styleCombo.setSelectedIndex(i);
				break;
			}
		}
	}

	public void actionPerformed(ActionEvent event)
	{
		if (!isAdjusting)
		{
			if (setupable != null)
			{
				actionDesignPerformed(event);
			}
			else
			{
				actionRuntimePerformed(event);
			}
		}
	}

	public void actionDesignPerformed(ActionEvent event)
	{
		if (setupable != null)
		{
			Object source = event.getSource();
			if (source == bold || source == italic || source == heightCombo || source == styleCombo)
			{
				int style = Font.PLAIN;
				if (bold.isSelected()) style = style | Font.BOLD;
				if (italic.isSelected()) style = style | Font.ITALIC;

				int index = styleCombo.getSelectedIndex();
				if (index > 0)
				{
					String name = (String)allFontNames.elementAt(index);
					Integer size = (Integer)heightCombo.getSelectedItem();
					if (size != null)
					{
						Font constructedFont = new Font(name, style, size.intValue());
						if (setupable != null)
						{
							setupable.setFontType(PersistHelper.createFontString(constructedFont));
						}
					}
				}
			}

			if (source == left && left.isSelected()) setupable.setHorizontalAlignment(SwingConstants.LEFT);
			else if (source == hcenter && hcenter.isSelected()) setupable.setHorizontalAlignment(SwingConstants.CENTER);
			else if (source == right && right.isSelected()) setupable.setHorizontalAlignment(SwingConstants.RIGHT);

			if (changeListener != null) changeListener.stateChanged(new ChangeEvent(this));
		}
	}

	public void setSetupable(ChangeListener cl, ISupportTextSetup s)
	{
		changeListener = cl;
		setSetupable(s);
	}

	public void setSetupable(ISupportTextSetup s)
	{
		if (s == setupable) return;
		setSetupableEx(s);
	}

	//we do this becouse super.setEnabled does all and we want this toolbar usable from plugins (there buttons should not enable/disable)
	@Override
	public void setEnabled(boolean enabled)
	{
		styleCombo.setEnabled(enabled);
		heightCombo.setEnabled(enabled);

		bold.setEnabled(enabled);
		italic.setEnabled(enabled);
		underline.setEnabled(enabled);
		left.setEnabled(enabled);
		hcenter.setEnabled(enabled);
		right.setEnabled(enabled);
		fcolor.setEnabled(enabled);

		moreCmds.setEnabled(enabled);
		moreCmdsMenu.setEnabled(enabled);
	}

	private void setSetupableEx(ISupportTextSetup s)
	{
		isAdjusting = true;
		try
		{
			setupable = s;
			setEnabled(setupable != null);

			if (setupable == null) return;
			moreCmds.setEnabled(false);

			heightCombo.setModel(JFontChooser.createFontSizes());

			underline.setEnabled(false);
			fcolor.setEnabled(false);

			if (setupable != null)
			{
				setSelectedFont(PersistHelper.createFont(setupable.getFontType()));
				switch (setupable.getHorizontalAlignment())
				{
					case SwingConstants.LEFT :
						left.setSelected(true);
						break;
					case SwingConstants.CENTER :
						hcenter.setSelected(true);
						break;
					case SwingConstants.RIGHT :
						right.setSelected(true);
						break;
				}
			}
			else
			{
				changeListener = null;
			}
		}
		finally
		{
			isAdjusting = false;
		}
	}

	public void updateFont()
	{
		if (isEnabled() && setupable != null)
		{
			setSetupableEx(setupable);
		}
	}


	/**
	 * The modified version of the StyledEditorKit.ForegroundAction class from the swing text package.
	 * This is our private way of setting the foreground action. 
	 * We filter out the old foreground attributes in order to overwrite them and not have two 
	 * settings (HTML.Tag and CSS.Attribute) for the same purpose (HTML.Tag would overwrite CSS.Attribute
	 * which causes text no to change color after first setting of color).
	 * 
	 * @author acostache
	 *
	 */
	private class ForegroundSetWithAttributeFilteringAction extends StyledTextAction
	{
		private final Color fg;

		/**
		 * Creates a new ForegroundSetWithAttributeFilteringAction.
		 *
		 * @param nm the action name
		 * @param fg the foreground color
		 */
		public ForegroundSetWithAttributeFilteringAction(String nm, Color fg)
		{
			super(nm);
			this.fg = fg;
		}

		/**
		 * Sets the foreground color. 
		 * The main difference from StyledEditorKit.ForegroundAction is the filtering of attributes 
		 * before the setCharacterAttributes.
		 *
		 * @param e the action event
		 */
		public void actionPerformed(ActionEvent e)
		{
			JEditorPane editor = getEditor(e);
			if (editor != null)
			{
				if (editor.getEditorKit() instanceof FixedRTFEditorKit)
				{
					new StyledEditorKit.ForegroundAction("set-foreground", fg).actionPerformed(e); //$NON-NLS-1$
					return;
				}
				Color fg = this.fg;
				if ((e != null) && (e.getSource() == editor))
				{
					String s = e.getActionCommand();
					try
					{
						fg = Color.decode(s);
					}
					catch (NumberFormatException nfe)
					{
					}
				}
				if (fg != null)
				{
					// remove the (old) foreground attributes, but, at the same time, 
					// do not lose the other (old) formatting settings.
					MutableAttributeSet oldInputAttributes = getStyledEditorKit(editor).getInputAttributes();
					for (Enumeration eold = oldInputAttributes.getAttributeNames(); eold.hasMoreElements();)
					{
						Object key = eold.nextElement();
						if (key instanceof HTML.Tag && key.toString().equalsIgnoreCase("font"))
						{
							oldInputAttributes.removeAttribute(key);
						}
						if (key instanceof CSS.Attribute && key.toString().equalsIgnoreCase("color"))
						{
							oldInputAttributes.removeAttribute(key);
						}
					}

					// adding the old attributes without any foreground settings to new 
					// foreground attributes and replacing the old attributes with the new set.
					MutableAttributeSet attr = new SimpleAttributeSet();
					StyleConstants.setForeground(attr, fg);
					attr.addAttributes(oldInputAttributes);
					setCharacterAttributes(editor, attr, true);
				}
				else
				{
					UIManager.getLookAndFeel().provideErrorFeedback(editor);
				}
			}
		}
	}
}
