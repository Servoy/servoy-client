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
package com.servoy.j2db.preference;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalLookAndFeel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.gui.JFontChooser;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.WebStart;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.IPropertyEditorDialog;

/**
 * Panel to show Look and Feel preferences in the SmartClient
 * 
 * @author jcompagner
 */
public class LFPreferencePanel extends PreferencePanel implements ItemListener, ActionListener
{
	private final IApplication _application;
	private DefaultComboBoxModel _dcbm;
	private LookAndFeelInfo _current;
	private JLabel _errorLabel;
	private Font _selectedFont;
	private Font _choosenFont;
	private Map themes;
	private JComboBox lnfBox;
	private JComboBox themesBox;
	private JButton fontButton;
	private ChangeListener listener;
	private boolean changed = false;

	public LFPreferencePanel(IApplication app)
	{
		super();
		_application = app;
		createUI();
	}

	private void createUI()
	{
		setLayout(new BorderLayout());

		List lafs = _application.getLAFManager().getLAFInfos(_application);
		_dcbm = new DefaultComboBoxModel();
		_dcbm.addElement(new LookAndFeelInfoWrapper(null));

		String sCurrent = UIManager.getLookAndFeel().getClass().getName();
		String sSystemLAF = UIManager.getSystemLookAndFeelClassName();
		Iterator it = lafs.iterator();
		while (it.hasNext())
		{
			LookAndFeelInfo li = (LookAndFeelInfo)it.next();
			_dcbm.addElement(new LookAndFeelInfoWrapper(li));
			if (!sSystemLAF.equals(sCurrent) && sCurrent.equals(li.getClassName()))
			{
				_current = li;
			}
		}

		String font = _application.getSettings().getProperty("font");
		if (WebStart.isRunningWebStart())
		{
			URL webstartbase = _application.getServerURL();
			font = _application.getSettings().getProperty(webstartbase.getHost() + webstartbase.getPort() + "_font", font);
		}

		_selectedFont = PersistHelper.createFont(font);
		lnfBox = new JComboBox(_dcbm);
		String msg = getFontButtonText();
		if (msg == null) msg = _application.getI18NMessage("servoy.preference.lookandfeel.msg.undefined"); //$NON-NLS-1$
		fontButton = new JButton(msg);
		fontButton.addActionListener(this);
		fontButton.setActionCommand("font");
		themes = _application.getLAFManager().getLoadedThemes(_application);
		SortedList sl = new SortedList(StringComparator.INSTANCE);
		sl.add(""); //$NON-NLS-1$
		Iterator th = themes.keySet().iterator();
		while (th.hasNext())
		{
			sl.add(th.next());
		}
		DefaultComboBoxModel modelThemes = new DefaultComboBoxModel(sl.toArray());
		String n = ""; //$NON-NLS-1$
		if (WebStart.isRunningWebStart())
		{
			URL webstartbase = _application.getServerURL();
			n = _application.getSettings().getProperty(webstartbase.getHost() + webstartbase.getPort() + "_lnf.theme", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			n = _application.getSettings().getProperty("lnf.theme", ""); //$NON-NLS-1$//$NON-NLS-2$
		}
		int indx = n.lastIndexOf('.');
		if (indx != -1) n = n.substring(indx + 1);
		modelThemes.setSelectedItem(n);
		themesBox = new JComboBox(modelThemes);
		JPanel comp = new JPanel(false);
		comp.setLayout(new GridLayout(3, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
		comp.add(lnfBox);
		comp.add(themesBox);
		comp.add(fontButton);

		if (_current != null) lnfBox.setSelectedItem(new LookAndFeelInfoWrapper(_current));

		lnfBox.addItemListener(this);
		themesBox.addItemListener(this);
		JPanel label = new JPanel(false);
		label.setLayout(new GridLayout(3, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
		label.add(new JLabel(_application.getI18NMessage("servoy.preference.lookandfeel.label.lnf"), SwingConstants.RIGHT)); //$NON-NLS-1$
		label.add(new JLabel(_application.getI18NMessage("servoy.preference.lookandfeel.label.theme"), SwingConstants.RIGHT)); //$NON-NLS-1$
		label.add(new JLabel(_application.getI18NMessage("servoy.preference.lookandfeel.label.defaultFont"), SwingConstants.RIGHT)); //$NON-NLS-1$
		JPanel panel = new JPanel(false);
		panel.setLayout(new BorderLayout(5, 5));
		panel.add(label, BorderLayout.WEST);
		panel.add(comp, BorderLayout.CENTER);
		this.add(panel, BorderLayout.NORTH);
		_errorLabel = new JLabel(""); //$NON-NLS-1$
		this.add(_errorLabel, BorderLayout.SOUTH);
	}

	@Override
	public void addChangeListener(ChangeListener l)
	{
		listener = l;
	}

	private void fireChangeEvent()
	{
		changed = true;
		listener.stateChanged(new ChangeEvent(this));
	}

	@Override
	public int getRequiredUserAction()
	{
		int retval = PreferencePanel.NO_USER_ACTION_REQUIRED;
		if (changed)
		{
			retval = PreferencePanel.APPLICATION_RESTART_NEEDED;
		}
		changed = false;
		return retval;
	}

	@Override
	public boolean handleCancel()
	{
		String msg = getFontButtonText();
		if (msg == null) msg = _application.getI18NMessage("servoy.preference.lookandfeel.msg.undefined"); //$NON-NLS-1$
		fontButton.setText(msg);
		if (_current != null) lnfBox.setSelectedItem(_current);
		String n = _application.getSettings().getProperty("lnf.theme", ""); //$NON-NLS-1$//$NON-NLS-2$
		int indx = n.lastIndexOf('.');
		if (indx != -1) n = n.substring(indx + 1);
		themesBox.setSelectedItem(n);
		return true;
	}

	protected String getFontButtonText()
	{
		String msg = null;
		if (_selectedFont != null)
		{
			String style = null;
			if (_selectedFont.isItalic()) style = "Italic";
			if (_selectedFont.isBold())
			{
				if (style != null) style = "Italic&Bold";
				else style = "Bold";
			}
			if (style == null) style = "Regular";
			msg = _selectedFont.getFontName() + " (" + style + "," + _selectedFont.getSize() + ")"; //$NON-NLS-1$
		}
		return msg;
	}

	@Override
	public String getTabName()
	{
		return _application.getI18NMessage("servoy.preference.lookandfeel.tabName"); //$NON-NLS-1$
	}

	@Override
	public boolean handleOK()
	{
		boolean update = false;
		// If the L&F or the font size change, we need to remove all dialog bounds,
		// because there is a chance that the dialogs will have different sizes under the 
		// new L&F or with the new font. If only the theme changes, there should be no
		// such situation.
		boolean removeAllBounds = false;
		LookAndFeelInfo selected = ((LookAndFeelInfoWrapper)lnfBox.getSelectedItem()).getLookAndFeelInfo();
		String themeName = (String)themesBox.getSelectedItem();
		String themeClassName = (String)themes.get(themeName);
		String currentTheme = _application.getSettings().getProperty("lnf.theme", ""); //$NON-NLS-1$//$NON-NLS-2$
		if (_choosenFont != null && !_choosenFont.equals(_selectedFont))
		{
			update = true;
			removeAllBounds = true;
			_selectedFont = _choosenFont;
			_choosenFont = null;
		}
		if (_current != selected)
		{
			update = true;
			removeAllBounds = true;
			_current = selected;
		}
		String clientPrefix = "";
		if (WebStart.isRunningWebStart())
		{
			URL webstartbase = _application.getServerURL();
			clientPrefix = webstartbase.getHost() + webstartbase.getPort() + "_"; //$NON-NLS-1$
		}
		if (themeClassName != null)
		{
			if (!themeClassName.equals(currentTheme))
			{
				update = true;
				_application.getSettings().setProperty(clientPrefix + "lnf.theme", themeClassName); //$NON-NLS-1$
			}
		}
		else
		{
			_application.getSettings().setProperty(clientPrefix + "lnf.theme", "");//clear $NON-NLS-1$//$NON-NLS-2$
		}
		if (update)
		{
			String s_laf = UIManager.getSystemLookAndFeelClassName();
			if (selected != null) s_laf = selected.getClassName();
			if (_application.putClientProperty(LookAndFeelInfo.class.getName(), s_laf) && _application.putClientProperty(Font.class.getName(), _selectedFont))
			{
				String c_laf = (_current == null ? "" : _current.getClassName());
				_application.getSettings().setProperty(clientPrefix + "selectedlnf", c_laf); //$NON-NLS-1$
				if (_selectedFont != null)
				{
					_application.getSettings().setProperty(clientPrefix + "font", PersistHelper.createFontString(_selectedFont)); //$NON-NLS-1$
				}
			}
			else
			{
				_application.getSettings().setProperty(clientPrefix + "lnf.theme", currentTheme); //$NON-NLS-1$
			}
			if (removeAllBounds)
			{
				((Settings)_application.getSettings()).deleteAllBounds();
			}
		}
		return true;
	}

	public void actionPerformed(ActionEvent evt)
	{
		if ("font".equals(evt.getActionCommand()))
		{
			fireChangeEvent();
			Font fnt = _selectedFont;
			if (_choosenFont != null) fnt = _choosenFont;
			JFontChooser chooser = new JFontChooser(SwingUtilities.getWindowAncestor(LFPreferencePanel.this), fnt);
			int but = chooser.showDialog(SwingUtilities.getWindowAncestor(LFPreferencePanel.this),
				_application.getI18NMessage("servoy.preference.lookandfeel.chooseFont"), false); //$NON-NLS-1$
			if (but == IPropertyEditorDialog.OK_OPTION)
			{
				_choosenFont = chooser.getSelectedFont();
				if (_choosenFont != null)
				{
					JButton button = (JButton)evt.getSource();
					button.setText(_choosenFont.getName() +
						" " + _application.getI18NMessage("servoy.preference.lookandfeel.fontsize") + _choosenFont.getSize()); //$NON-NLS-1$
				}
			}
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() == lnfBox && e.getStateChange() == ItemEvent.SELECTED)
		{
			fireChangeEvent();
			enableButtons();
		}
	}

	private void enableButtons()
	{
		LookAndFeelInfo lafi = ((LookAndFeelInfoWrapper)lnfBox.getSelectedItem()).getLookAndFeelInfo();
		boolean metalLnF = false;
		try
		{
			if (lafi != null)
			{
				LookAndFeel laf = _application.getLAFManager().createInstance(lafi.getClassName());
				metalLnF = (laf instanceof MetalLookAndFeel);
			}
			themesBox.setEnabled(metalLnF);
			fontButton.setEnabled(!metalLnF);
		}
		catch (Exception e)
		{
			themesBox.setEnabled(false);
			fontButton.setEnabled(false);
		}
	}

	private class LookAndFeelInfoWrapper
	{
		private final LookAndFeelInfo info;

		LookAndFeelInfoWrapper(LookAndFeelInfo l)
		{
			info = l;
		}

		@Override
		public String toString()
		{
			if (info == null) return _application.getI18NMessage("servoy.button.default"); //$NON-NLS-1$
			return Utils.stringReplace(info.getName(), "LookAndFeel", "");
		}

		public LookAndFeelInfo getLookAndFeelInfo()
		{
			return info;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof LookAndFeelInfoWrapper)
			{
				LookAndFeelInfoWrapper otherLAFW = (LookAndFeelInfoWrapper)other;
				if (info == null && otherLAFW.info == null)
				{
					return true;
				}
				if (info != null && otherLAFW.info != null && info.getClassName().equals(otherLAFW.info.getClassName()))
				{
					return true;
				}
			}
			return false;
		}
	}
}