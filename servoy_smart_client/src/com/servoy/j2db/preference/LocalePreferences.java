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
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
public class LocalePreferences extends PreferencePanel implements ItemListener, FocusListener
{
	public static final String SELECTION_NONE = "<none>"; //$NON-NLS-1$

	private final IApplication _application;
	private JTextField _tfDateFormat;
	private JTextField _tfNumberFormat;
	private JTextField _tfIntegerFormat;
	private JComboBox _cbLocales;
	private JComboBox _cbTimezones;
	private JComboBox _cbServer;
	private JComboBox _cbTable;

	private String _defaultLocale;

	public LocalePreferences(IApplication app)
	{
		super();
		_application = app;
		createUI();
	}

	private void createUI()
	{
		setLayout(new BorderLayout());

		final Properties settings = _application.getSettings();

		_tfDateFormat = new JTextField();
		_tfDateFormat.setText(settings.getProperty("locale.dateformat")); //$NON-NLS-1$
		_tfDateFormat.addFocusListener(this);

		_tfNumberFormat = new JTextField();
		_tfNumberFormat.setText(settings.getProperty("locale.numberformat")); //$NON-NLS-1$
		_tfNumberFormat.addFocusListener(this);

		_tfIntegerFormat = new JTextField();
		_tfIntegerFormat.setText(settings.getProperty("locale.integerformat")); //$NON-NLS-1$
		_tfIntegerFormat.addFocusListener(this);

		JPanel panelLabels = new JPanel();
		panelLabels.setLayout(new GridLayout(0, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
		panelLabels.setBorder(BorderFactory.createEmptyBorder(J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING,
			J2DBClient.COMPONENT_SPACING));
		panelLabels.add(new JLabel(_application.getI18NMessage("servoy.preference.locale.dateFormat"))); //$NON-NLS-1$
		panelLabels.add(new JLabel(_application.getI18NMessage("servoy.preference.locale.numberFormat"))); //$NON-NLS-1$
		panelLabels.add(new JLabel(_application.getI18NMessage("servoy.preference.locale.integerFormat"))); //$NON-NLS-1$
		panelLabels.add(new JLabel(_application.getI18NMessage("servoy.preference.locale.defaultLocale"))); //$NON-NLS-1$
		panelLabels.add(new JLabel(_application.getI18NMessage("servoy.preference.locale.defaultTimezone"))); //$NON-NLS-1$


		JPanel panelText = new JPanel();
		panelText.setLayout(new GridLayout(0, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
		panelText.setBorder(BorderFactory.createEmptyBorder(J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING,
			J2DBClient.COMPONENT_SPACING));
		panelText.add(_tfDateFormat);
		panelText.add(_tfNumberFormat);
		panelText.add(_tfIntegerFormat);


		ArrayList al = new ArrayList();
		Locale[] locales = Locale.getAvailableLocales();
		for (int i = 0; i < locales.length; i++)
		{
			if (locales[i].getCountry() != null && !locales[i].getCountry().equals("")) //$NON-NLS-1$
			{
				al.add(locales[i]);
			}
		}
		locales = new Locale[al.size()];
		locales = (Locale[])al.toArray(locales);
		Arrays.sort(locales, new LocaleSorter());

		_cbLocales = new JComboBox(locales);
		_defaultLocale = new String(_application.getI18NMessage("servoy.i18nPanel.default"));
		_cbLocales.insertItemAt(_defaultLocale, 0);
		_cbLocales.setRenderer(new LocaleRenderer());
		String locale = settings.getProperty("locale.default"); //$NON-NLS-1$
		if (locale == null)
		{
			_cbLocales.setSelectedItem(_defaultLocale);
		}
		else
		{
			_cbLocales.setSelectedItem(Locale.getDefault());
		}

		_cbLocales.addItemListener(this);
		panelText.add(_cbLocales);

		String[] timeZones = TimeZone.getAvailableIDs();
		Arrays.sort(timeZones);
		_cbTimezones = new JComboBox(timeZones);
		panelText.add(_cbTimezones);
		_cbTimezones.setSelectedItem(TimeZone.getDefault().getID());
		_cbTimezones.addItemListener(this);

		JPanel holder = new JPanel();
		holder.setLayout(new BorderLayout());
		holder.add(panelLabels, BorderLayout.WEST);
		holder.add(panelText, BorderLayout.CENTER);

		this.add(holder, BorderLayout.NORTH);
	}

	private ChangeListener listener;

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

	private boolean changed = false;

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() == _cbServer)
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				try
				{
					String serverName = (String)_cbServer.getSelectedItem();
					fillTableCombo(SELECTION_NONE.equals(serverName) ? null : _application.getRepository().getServer(serverName));
					_cbTable.setSelectedItem(SELECTION_NONE);
				}
				catch (Exception e1)
				{
					Debug.error(e1);
				}
			}
		}
		fireChangeEvent();
	}

//	@Override
//	public void setVisible(boolean v)
//	{
//		super.setVisible(v);
//		try
//		{
//			if (_application.isInDeveloper() && v && _cbTable != null && _cbTable.getModel().getSize() <= 1)
//			{
//				Object selected = _cbTable.getSelectedItem();
//				String serverName = (String)_cbServer.getSelectedItem();
//				fillTableCombo(SELECTION_NONE.equals(serverName) ? null : _application.getRepository().getServer(serverName));
//				_cbTable.setSelectedItem(selected);
//			}
//		}
//		catch (Exception e1)
//		{
//			Debug.error(e1);
//		}
//	}

	/**
	 * @param server
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	private void fillTableCombo(IServer server) throws RepositoryException, RemoteException
	{
		if (server == null)
		{
			_cbTable.setModel(new DefaultComboBoxModel(new String[] { SELECTION_NONE }));
		}
		else
		{
			List al = new ArrayList();
			al.add(SELECTION_NONE);
			List lst = ((IServerInternal)server).getTableAndViewNames(true, true);
			for (int i = 0; i < lst.size(); i++)
			{
				String tableName = (String)lst.get(i);
				Table table = (Table)server.getTable(tableName);
				if (table.getColumnInfoID("message_key") == -1) continue; //$NON-NLS-1$
				if (table.getColumnInfoID("message_value") == -1) continue; //$NON-NLS-1$
				if (table.getColumnInfoID("message_language") == -1) continue; //$NON-NLS-1$
				al.add(tableName);
			}
			DefaultComboBoxModel model = new DefaultComboBoxModel(al.toArray());
			_cbTable.setModel(model);
		}
	}

	public void focusGained(FocusEvent e)
	{
		fireChangeEvent();
	}

	public void focusLost(FocusEvent e)
	{
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

	/**
	 * @see com.servoy.j2db.preference.PreferencePanel#cancel()
	 */
	@Override
	public boolean handleCancel()
	{
		Properties settings = _application.getSettings();
		_tfDateFormat.setText(settings.getProperty("locale.dateformat")); //$NON-NLS-1$
		_tfNumberFormat.setText(settings.getProperty("locale.numberformat")); //$NON-NLS-1$
		_tfIntegerFormat.setText(settings.getProperty("locale.integerformat")); //$NON-NLS-1$
//		if(_application.getApplicationType() == IApplication.DEVELOPER)
		String locale = settings.getProperty("locale.default"); //$NON-NLS-1$
		if (locale == null)
		{
			_cbLocales.setSelectedItem(_defaultLocale);
		}
		else
		{
			_cbLocales.setSelectedItem(Locale.getDefault());
		}

//		if (_application.isInDeveloper())
//		{
//			String serverName = settings.getProperty("defaultMessagesServer"); //$NON-NLS-1$
//			String tableName = settings.getProperty("defaultMessagesTable"); //$NON-NLS-1$
//
//			if (serverName == null)
//			{
//				_cbServer.setSelectedItem(SELECTION_NONE);
//			}
//			else
//			{
//				_cbServer.setSelectedItem(serverName);
//			}
//			if (tableName != null) _cbTable.setSelectedItem(tableName);
//			else _cbTable.setSelectedItem(SELECTION_NONE);
//		}
		return true;
	}

	/**
	 * @see com.servoy.j2db.preference.PreferencePanel#ok()
	 */
	@Override
	public boolean handleOK()
	{
		Properties settings = _application.getSettings();

		String dateFormat = _tfDateFormat.getText();
		String numberFormat = _tfNumberFormat.getText();
		String integerFormat = _tfIntegerFormat.getText();

		if (dateFormat == null || dateFormat.trim().length() == 0)
		{
			settings.remove("locale.dateformat"); //$NON-NLS-1$
		}
		else
		{
			settings.setProperty("locale.dateformat", dateFormat); //$NON-NLS-1$
		}
		if (numberFormat == null || numberFormat.trim().length() == 0)
		{
			settings.remove("locale.numberformat"); //$NON-NLS-1$
		}
		else
		{
			settings.setProperty("locale.numberformat", numberFormat); //$NON-NLS-1$
		}
		if (integerFormat == null || integerFormat.trim().length() == 0)
		{
			settings.remove("locale.integerformat"); //$NON-NLS-1$
		}
		else
		{
			settings.setProperty("locale.integerformat", integerFormat); //$NON-NLS-1$
		}


		Locale loc = null;
		Object selectedLocale = _cbLocales.getSelectedItem();
		if (selectedLocale.equals(_defaultLocale))
		{
			settings.remove("locale.default");//$NON-NLS-1$ 
			loc = Locale.getDefault();
		}
		else
		{
			loc = (Locale)selectedLocale;
			settings.setProperty("locale.default", PersistHelper.createLocaleString(loc)); //$NON-NLS-1$
		}
		_application.setLocale(loc);


		String timezone = (String)_cbTimezones.getSelectedItem();
		TimeZone.setDefault(TimeZone.getTimeZone(timezone));
		settings.setProperty("timezone.default", timezone); //$NON-NLS-1$

//		if (_application.isInDeveloper())
//		{
//			String serverName = ""; //$NON-NLS-1$
//			String tableName = (String)_cbTable.getSelectedItem();
//			if (tableName == null || SELECTION_NONE.equals(tableName))
//			{
//				tableName = ""; //$NON-NLS-1$
//			}
//
//			String selectedServerName = (String)_cbServer.getSelectedItem();
//			if (!SELECTION_NONE.equals(selectedServerName) && selectedServerName != null)
//			{
//				serverName = selectedServerName;
//			}
//			settings.setProperty("defaultMessagesServer", serverName); //$NON-NLS-1$
//			if (!Utils.equalObjects(settings.getProperty("defaultMessagesTable"), tableName)) //$NON-NLS-1$
//			{
//				settings.setProperty("defaultMessagesTable", tableName); //$NON-NLS-1$
//				if (_application instanceof IMessagesCallback)
//				{
//					Messages.load((IMessagesCallback)_application);
//				}
//			}
//		}
		return true;
	}

	/**
	 * @see com.servoy.j2db.preference.PreferencePanel#getTabName()
	 */
	@Override
	public String getTabName()
	{
		return _application.getI18NMessage("servoy.preference.locale.tabName"); //$NON-NLS-1$
	}

	private class LocaleRenderer extends DefaultListCellRenderer
	{
		/**
		 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			String localeName = "";

			if (value != null && value instanceof String)
			{
				localeName = value.toString();
			}
			else
			{
				localeName = (value == null) ? " " : ((Locale)value).getDisplayName(((Locale)value)); //$NON-NLS-1$
			}
			return super.getListCellRendererComponent(list, localeName, index, isSelected, cellHasFocus);
		}
	}

	private class LocaleSorter implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			String name1 = ((Locale)o1).getDisplayName((Locale)o1);
			String name2 = ((Locale)o2).getDisplayName((Locale)o2);
			return name1.compareToIgnoreCase(name2);
		}
	}

}
