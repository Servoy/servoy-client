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
package com.servoy.j2db.property;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMessagesCallback;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.gui.FixedJSplitPane;
import com.servoy.j2db.i18n.I18NMessagesModel;
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.TableSorter;

/**
 * @author jcompagner
 */
public class I18NPanel extends JPanel implements DocumentListener
{
	private static final long serialVersionUID = 1L;

	private String selectedLanguage;

	private I18NMessagesModel i18NMessagesModel;


	/**
	 * @author jcompagner
	 */
	public class ButtonCellEditor extends AbstractCellEditor implements ActionListener, TableCellEditor, TableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		JButton label;

		public ButtonCellEditor()
		{
			label = new JButton(application.loadImage("delete.gif")); //$NON-NLS-1$
			label.addActionListener(this);
		}

		/*
		 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
		 */
		public Component getTableCellEditorComponent(JTable t, Object value, boolean isSelected, int row, int column)
		{
			return label;
		}

		public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			return label;
		}

		@Override
		public boolean shouldSelectCell(EventObject anEvent)
		{
			return true;
		}

		public Object getCellEditorValue()
		{
			return ""; //$NON-NLS-1$
		}

		public void actionPerformed(ActionEvent e)
		{
			stopCellEditing();
			int row = getSelectedRow();
			String i18nKey = (String)messageModel.getValueAt(row, 0);
			int option = JOptionPane.showConfirmDialog(I18NPanel.this,
				"Are you sure you want to delete this key", "Deleting a i18n key", JOptionPane.YES_NO_OPTION); //$NON-NLS-1$ //$NON-NLS-2$
			Solution appSolution = application.getSolution();
			if (option == JOptionPane.YES_OPTION &&
				Messages.deleteKey(i18nKey, appSolution != null ? appSolution.getI18nDataSource() : null, application.getClientID(), application.getSettings(),
					application.getDataServer(), application.getRepository(), application.getFoundSetManager()))
			{
				messageModel.removeRow(row);
				if (i18nKey.equals(I18NPanel.this.key.getText()))
				{
					clearEditedKey();
				}
				else
				{
					// as the click on the delete button generated a change in the table selection
					// that would empty the current key fields - we must try to change the selection back to what it was
					findAndSelectKey(I18NPanel.this.key.getText());
				}
				refresh(I18NPanel.this);
			}
		}
	}

	private IApplication application;

	private final JTextField key;

	private final JTextArea areaReference;

	private final JTextArea areaCurrentLocale;

	private final JSplitPane tableSplit;

	private final JSplitPane textSplit;

	private JTable table;

	private DefaultTableModel messageModel;

	private boolean changed;

	private final JComboBox languagesCombo;

	private JTextField searchField;

	private boolean endUser;

	private JButton deleteButton;

	private final JButton addOrUpdate;
	private final JButton clearEditedKey;
	private final JPanel keyButtonsPanel;

	public I18NPanel()
	{
		setLayout(new BorderLayout(J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setPreferredSize(new Dimension(600, 500));

		key = new JTextField();
		key.setEnabled(!Messages.invalidConnection);
		key.getDocument().addDocumentListener(this);
		key.addFocusListener(new FocusListener()
		{
			public void focusGained(FocusEvent e)
			{ /* not used */
			}

			public void focusLost(FocusEvent e)
			{
				// when the key field looses focus, we will try to select that key if it is found in the table;
				// if the key looses focus to the table, we must not try to set the table selection, in order
				// not to influence the selection generated by it...
				if (e.getOppositeComponent() != table && e.getOppositeComponent() != deleteButton)
				{
					findAndSelectKey(key.getText());
				}
			}
		});

		addOrUpdate = new JButton("Add/Update"); // must switch to i18N if this is made usable from smart clients
		addOrUpdate.setEnabled(!Messages.invalidConnection);
		addOrUpdate.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				getText();
			}
		});
		clearEditedKey = new JButton("Clear"); // must switch to i18N if this is made usable from smart clients
		clearEditedKey.setEnabled(!Messages.invalidConnection);
		clearEditedKey.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				clearEditedKey();
			}
		});

		keyButtonsPanel = new JPanel();
		keyButtonsPanel.setLayout(new BorderLayout());
		keyButtonsPanel.add(addOrUpdate, BorderLayout.WEST);
		keyButtonsPanel.add(clearEditedKey, BorderLayout.EAST);

		JPanel keyPanel = new JPanel();
		keyPanel.setLayout(new BorderLayout(J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING));
		keyPanel.setBorder(BorderFactory.createTitledBorder("Key")); //$NON-NLS-1$
		keyPanel.add(key, BorderLayout.CENTER);
		keyPanel.add(keyButtonsPanel, BorderLayout.EAST);

		JPanel textPanel = new JPanel();
		textPanel.setBorder(BorderFactory.createEmptyBorder());
		textPanel.setLayout(new BorderLayout(J2DBClient.COMPONENT_SPACING, J2DBClient.COMPONENT_SPACING));

		areaReference = new JTextArea();
		areaReference.setWrapStyleWord(true);
		areaReference.setLineWrap(true);
		areaReference.setEnabled(!Messages.invalidConnection);

		areaCurrentLocale = new JTextArea();
		areaCurrentLocale.setWrapStyleWord(true);
		areaCurrentLocale.setLineWrap(true);
		areaCurrentLocale.setEnabled(!Messages.invalidConnection);

		areaReference.getDocument().addDocumentListener(this);
		areaCurrentLocale.getDocument().addDocumentListener(this);

		JPanel areaReferencePanel = new JPanel();
		areaReferencePanel.setLayout(new BorderLayout());
		areaReferencePanel.setBorder(BorderFactory.createTitledBorder(Messages.getString("servoy.i18nPanel.referenceText"))); //$NON-NLS-1$
		JScrollPane refScroller = new JScrollPane(areaReference);
		refScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		areaReferencePanel.add(refScroller, BorderLayout.CENTER);
		languagesCombo = new JComboBox();
		languagesCombo.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					getText(); // save the data for the current location (so not the new one) to database
					selectedLanguage = (String)((Object[])languagesCombo.getSelectedItem())[0];
					if (i18NMessagesModel != null)
					{
						i18NMessagesModel.setLanguage(new Locale(selectedLanguage));
					}
					refresh();
				}
			}
		});
		languagesCombo.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				String txt = " "; //$NON-NLS-1$
				if (value != null)
				{
					txt = (String)((Object[])value)[1];
				}
				return super.getListCellRendererComponent(list, txt, index, isSelected, cellHasFocus);
			}
		});

		JPanel areaCurrentLocalePanel = new JPanel();
		areaCurrentLocalePanel.setLayout(new BorderLayout());
		areaCurrentLocalePanel.setBorder(BorderFactory.createTitledBorder(Messages.getString("servoy.i18nPanel.localeText"))); //$NON-NLS-1$
		areaCurrentLocalePanel.add(languagesCombo, BorderLayout.NORTH);

		JScrollPane areaCurrentLocaleScroller = new JScrollPane(areaCurrentLocale);
		areaCurrentLocaleScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		areaCurrentLocalePanel.add(areaCurrentLocaleScroller, BorderLayout.CENTER);

		textSplit = new FixedJSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		textSplit.setLeftComponent(areaReferencePanel);
		textSplit.setRightComponent(areaCurrentLocalePanel);
		textSplit.setBorder(BorderFactory.createEmptyBorder());

		textPanel.add(textSplit, BorderLayout.CENTER);
		textPanel.add(keyPanel, BorderLayout.NORTH);

		tableSplit = new FixedJSplitPane(JSplitPane.VERTICAL_SPLIT);
		tableSplit.setLeftComponent(textPanel);
		tableSplit.setBorder(BorderFactory.createEmptyBorder());

		this.add(tableSplit, BorderLayout.CENTER);

		tableSplit.setDividerLocation(0.5);
		textSplit.setDividerLocation(0.5);
	}

	private void clearEditedKey()
	{
		I18NPanel.this.key.setText(""); //$NON-NLS-1$
		I18NPanel.this.areaReference.setText(""); //$NON-NLS-1$
		I18NPanel.this.areaCurrentLocale.setText(""); //$NON-NLS-1$
	}

	private int getSelectedRow()
	{
		int selectedRow = table.getSelectedRow();
		if (selectedRow != -1)
		{
			TableSorter ts = (TableSorter)table.getModel();
			return ts.getRealRowIndex(selectedRow);
		}
		else
		{
			return selectedRow;
		}
	}

	private void setUpButtonColumn()
	{
		if (table != null)
		{
			if (table.getColumnModel().getColumnCount() > 3)
			{
				TableColumn tc = table.getColumnModel().getColumn(3);
				tc.setMaxWidth(30);
				ButtonCellEditor cer = new ButtonCellEditor();
				tc.setCellRenderer(cer);
				tc.setCellEditor(cer);
				deleteButton = cer.label;
			}
		}
	}

	public void cancel()
	{
		setText("i18n:" + key.getText()); //$NON-NLS-1$
	}

	/**
	 * @param text
	 */
	public void setText(String text)
	{
		if (text == null) text = ""; //$NON-NLS-1$

		boolean enabled = (!Messages.invalidConnection && !Messages.noConnection);
		areaReference.setEnabled(enabled);
		areaCurrentLocale.setEnabled(enabled);
		key.setEnabled(enabled);
		addOrUpdate.setEnabled(enabled);
		clearEditedKey.setEnabled(enabled);

		if (text.startsWith("i18n:")) //$NON-NLS-1$
		{
			areaReference.setText(""); //$NON-NLS-1$
			areaCurrentLocale.setText(""); //$NON-NLS-1$
			text = text.substring(5);
			key.setText(text);

			boolean found = findAndSelectKey(text);
			if (!found)
			{
				// this can happen if, for example, since the last usage of the I18NPanel the user changed the database/table
				// that contains the messages (so the last edited key might not be available now)
				key.setText("");
			}
		}
		else
		{
			areaReference.setText(""); //$NON-NLS-1$
			key.setText(""); //$NON-NLS-1$
			areaCurrentLocale.setText(text);
		}
		changed = false;
	}

	/**
	 * Searches for a key in the loaded table. If it finds the key, it selects it and loads it's data into the text areas.
	 * 
	 * @return true if the key was found, false otherwise.
	 */
	public boolean findAndSelectKey(String i18nKey)
	{
		boolean foundKey = false;
		if (table != null)
		{
			TableModel model = table.getModel();
			for (int i = model.getRowCount(); i-- > 0;)
			{
				if (i18nKey.equals(model.getValueAt(i, 0)))
				{
					areaReference.setText((String)model.getValueAt(i, 1));
					areaCurrentLocale.setText((String)model.getValueAt(i, 2));
					table.setRowSelectionInterval(i, i);
					table.scrollRectToVisible(table.getCellRect(i, 0, true));
					key.setText(i18nKey);
					foundKey = true;
					break;
				}
			}
		}
		return foundKey;
	}

	/*
	 * @see javax.swing.JComponent#requestFocus()
	 */
	@Override
	public void requestFocus()
	{
		areaCurrentLocale.requestFocus();
	}

	public String getText()
	{
		String txt = key.getText();
		if ("".equals(txt)) //$NON-NLS-1$
		{
			txt = areaCurrentLocale.getText();
		}
		else
		{
			if (changed)
			{
				boolean databaseOK = createOrUpdateKey(txt, areaReference.getText(), areaCurrentLocale.getText());
				if (databaseOK)
				{
					if (application instanceof IMessagesCallback)
					{
						// TODO just a cast to IMessageCallback.. IApplication could inherited it but then debugger panel must also have those methods
						Messages.loadInternal((IMessagesCallback)application, application.getFoundSetManager());
					}
					boolean modelChanged = false;
					for (int i = 0; i < messageModel.getRowCount(); i++)
					{
						String i18nKey = (String)messageModel.getValueAt(i, 0);
						if (i18nKey.equals(txt))
						{
							messageModel.setValueAt(areaReference.getText(), i, 1);
							messageModel.setValueAt(areaCurrentLocale.getText(), i, 2);
							modelChanged = true;
							break;
						}
						else if (i18nKey.compareToIgnoreCase(txt) > 0)
						{
							messageModel.insertRow(i, new Object[] { txt, areaReference.getText(), areaCurrentLocale.getText() });
							modelChanged = true;
							break;
						}
					}

					if (!modelChanged)
					{
						messageModel.addRow(new Object[] { txt, areaReference.getText(), areaCurrentLocale.getText() });
					}

					refresh(this);
					table.repaint();
				}
			}

			txt = "i18n:" + txt; //$NON-NLS-1$
		}
		changed = false;
		return txt;
	}

	/**
	 * @param panel
	 */
	private static List alAllPanels = new ArrayList();

	private static void refresh(I18NPanel panel)
	{
		for (int i = alAllPanels.size(); --i >= 0;)
		{
			WeakReference wr = (WeakReference)alAllPanels.get(i);
			I18NPanel i18n = (I18NPanel)wr.get();
			if (i18n == null) alAllPanels.remove(i);
			else if (i18n != panel)
			{
				i18n.refresh();
			}

		}
	}

	public static void refreshAllPanels()
	{
		refresh(null);
	}

	public void setEndUser(boolean endUser)
	{
		if (this.endUser != endUser)
		{
			this.endUser = endUser;

			areaReference.setEditable(!endUser);
			key.setEditable(!endUser);
			keyButtonsPanel.setVisible(!endUser);

			refresh();
		}
	}

	public void setApplication(IApplication application, boolean endUser)
	{
		this.endUser = endUser;

		if (this.application == null)
		{
// try
// {
// application.blockGUI("Loading i18n info"); //$NON-NLS-1$
			i18NMessagesModel = new I18NMessagesModel(application.getSolution(), application.getClientID(), application.getSettings(),
				application.getDataServer(), application.getRepository());
			selectedLanguage = application.getLocale().getLanguage();
			i18NMessagesModel.setLanguage(new Locale(selectedLanguage));

			alAllPanels.add(new WeakReference(this));
			this.application = application;

			table = new JTable();
			initI18NMessagesModel(null);
			TableSorter tm = new TableSorter(messageModel);
			tm.setReallocateIndexesOnUpdate(false);
			tm.addMouseListenerToHeaderInTable(table);
			table.setModel(tm);
			setUpButtonColumn();

			table.setAutoscrolls(true);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (e.getClickCount() >= 1)
					{
						int selectedRow = getSelectedRow();
						if (selectedRow != -1)
						{
							String keyString = (String)messageModel.getValueAt(selectedRow, 0);
							if (!keyString.equals(key.getText()))
							{
								getText();
								key.setText(keyString);
							}
							areaReference.setText((String)messageModel.getValueAt(selectedRow, 1));
							areaCurrentLocale.setText((String)messageModel.getValueAt(selectedRow, 2));
						}
						changed = false;
					}
				}
			});
			JScrollPane pane = new JScrollPane(table);
			// pane.setBorder(BorderFactory.createEmptyBorder());

			searchField = new JTextField();
			searchField.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					initI18NMessagesModel(searchField.getText());
					TableSorter ts = new TableSorter(messageModel);
					ts.setReallocateIndexesOnUpdate(false);
					ts.addMouseListenerToHeaderInTable(table);
					table.setModel(ts);
					setUpButtonColumn();
				}
			});
			JPanel searchPanel = new JPanel();
			searchPanel.setLayout(new BorderLayout(J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
			searchPanel.add(new JLabel(Messages.getString("servoy.i18nPanel.filter")), BorderLayout.WEST); //$NON-NLS-1$
			searchPanel.add(searchField, BorderLayout.CENTER);
			JPanel panel = new JPanel(new BorderLayout(J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
			panel.setBorder(BorderFactory.createEmptyBorder(J2DBClient.BUTTON_SPACING, 0, 0, 0));
			panel.add(pane, BorderLayout.CENTER);
			panel.add(searchPanel, BorderLayout.NORTH);
			tableSplit.setRightComponent(panel);

			Locale[] locales = Locale.getAvailableLocales();
			TreeMap languages = new TreeMap(StringComparator.INSTANCE);
			for (Locale element : locales)
			{
				String name = element.getDisplayLanguage(element);

				languages.put(element.getLanguage(), name);
			}
			Object[] selected = null;

			DefaultComboBoxModel list = new DefaultComboBoxModel();
			Iterator it = languages.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry entry = (Entry)it.next();
				Object[] array = new Object[] { entry.getKey(), entry.getValue() };
				if (array[0].equals(selectedLanguage))
				{
					selected = array;
				}
				list.addElement(array);
			}
			languagesCombo.setModel(list);
			languagesCombo.setSelectedItem(selected);

// }
// finally
// {
// application.releaseGUI();
// }
		}

		areaReference.setEditable(!endUser);
		key.setEditable(!endUser);
		keyButtonsPanel.setVisible(!endUser);
	}

	public void focusFilterTextField()
	{
		if (searchField != null)
		{
			searchField.requestFocus();
		}
	}

	public void insertUpdate(DocumentEvent e)
	{
		changed = true;
	}

	public void removeUpdate(DocumentEvent e)
	{
		changed = true;
	}

	public void changedUpdate(DocumentEvent e)
	{
		changed = true;
	}

	/**
	 * @param newKey
	 * @param string
	 * @param string2
	 */
	private boolean createOrUpdateKey(String newKey, String referenceValue, String localeValue)
	{
		boolean operationPerformed = false;
		if (Messages.invalidConnection || Messages.noConnection || referenceValue == null || "".equals(referenceValue)) //$NON-NLS-1$
		return operationPerformed; // false

		String i18nDataSource = DataSourceUtils.getI18NDataSource(application.getSolution(), application.getSettings());

		if (i18nDataSource == null)
		{
			throw new IllegalStateException("Can't create key when there is no (valid) servername/tablename for messages"); //$NON-NLS-1$
		}

		String serverName = DataSourceUtils.getDataSourceServerName(i18nDataSource);
		String tableName = DataSourceUtils.getDataSourceTableName(i18nDataSource);

		String filterName = null;
		String[] filterValue = null;
		if (application instanceof IMessagesCallback)
		{
			filterName = ((IMessagesCallback)application).getI18NColumnNameFilter();
			filterValue = ((IMessagesCallback)application).getI18NColumnValueFilter();
		}

		if (Messages.customMessageLoader != null)
		{
			try
			{
				TreeMap<String, MessageEntry> messages = Messages.customMessageLoader.readMessages(serverName, tableName);
				adjustMessagesMap(newKey, referenceValue, localeValue, messages);
				Messages.customMessageLoader.save(serverName, tableName, messages);
				operationPerformed = true;
			}
			catch (Exception e)
			{
				Debug.error("exception when inserting/updating i18n key: " + newKey);
				Debug.error(e);
				// throw new RuntimeException(e);
			}
		}
		else
		{
			IDataServer dataServer = application.getDataServer();
			IRepository repository = application.getRepository();

			try
			{
				TreeMap<String, MessageEntry> repositoryMessages = I18NUtil.loadSortedMessagesFromRepository(repository, dataServer, application.getClientID(),
					serverName, tableName, filterName, filterValue, application.getFoundSetManager());
				TreeMap<String, MessageEntry> messages = new TreeMap<String, I18NUtil.MessageEntry>(repositoryMessages);
				adjustMessagesMap(newKey, referenceValue, localeValue, messages);
				I18NUtil.writeMessagesToRepository(serverName, tableName, repository, dataServer, application.getClientID(), messages, false, false,
					repositoryMessages, filterName, filterValue, application.getFoundSetManager());
				operationPerformed = true;
			}
			catch (Exception e)
			{
				Debug.error("exception when inserting new i18n key: " + newKey);
				Debug.error(e);
				// throw new RuntimeException(e);
			}
		}
		return operationPerformed;
	}

	private void adjustMessagesMap(String newKey, String referenceValue, String localeValue, TreeMap<String, MessageEntry> messages)
	{
		I18NUtil.MessageEntry messageEntry = new I18NUtil.MessageEntry(null, newKey, referenceValue);
		messages.put(messageEntry.getLanguageKey(), messageEntry);
		messageEntry = new I18NUtil.MessageEntry(getSelectedLanguage(), newKey, localeValue);
		if (localeValue == null || "".equals(localeValue))
		{
			messages.remove(messageEntry.getLanguageKey());
		}
		else
		{
			messages.put(messageEntry.getLanguageKey(), messageEntry);
		}
	}

	private void initI18NMessagesModel(String searchKey)
	{
		if (i18NMessagesModel != null)
		{
			String filterColumn = null;
			String[] filterValue = null;
			if (application instanceof IMessagesCallback)
			{
				filterColumn = ((IMessagesCallback)application).getI18NColumnNameFilter();
				filterValue = ((IMessagesCallback)application).getI18NColumnValueFilter();
			}

			Collection<I18NMessagesModelEntry> messages = i18NMessagesModel.getMessages(searchKey, filterColumn, filterValue, application.getFoundSetManager(),
				false);

			Object selLang = "<unknown>";
			if (languagesCombo.getSelectedItem() != null) selLang = ((Object[])languagesCombo.getSelectedItem())[1];

			String[] columns = null;
			if (endUser)
			{
				columns = new String[] { Messages.getString("servoy.i18nPanel.key"), Messages.getString("servoy.i18nPanel.default"), Messages.getString("servoy.i18nPanel.locale") +
					" (" + selLang + ")" };
			}
			else
			{
				columns = new String[] { Messages.getString("servoy.i18nPanel.key"), Messages.getString("servoy.i18nPanel.default"), Messages.getString("servoy.i18nPanel.locale") +
					" (" + selLang + ")", "" };
			}

			messageModel = new DefaultTableModel(columns, 0)
			{
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex)
				{
					return (columnIndex == 3 && !Messages.invalidConnection && !Messages.noConnection);
				}
			};

			for (I18NMessagesModelEntry entry : messages)
			{
				messageModel.addRow(new Object[] { entry.key, entry.defaultvalue, entry.localeValue, "" });
			}
		}
	}

	/**
	 * @return
	 */
	private String getSelectedLanguage()
	{
		return selectedLanguage;
	}


	/**
	 * 
	 */
	public void refresh()
	{
		String searchKey = null;
		if (searchField != null)
		{
			searchKey = searchField.getText();
			if ("".equals(searchKey)) searchKey = null; //$NON-NLS-1$
		}
		if (table != null)
		{
			initI18NMessagesModel(searchKey);
			TableSorter tm = new TableSorter(messageModel);
			tm.setReallocateIndexesOnUpdate(false);
			tm.addMouseListenerToHeaderInTable(table);
			table.setModel(tm);
		}
		setText(getText());
		setUpButtonColumn();
	}

	/**
	 * Selects the language specified by preselect_language.
	 * 
	 * @param preselect_language the language to be selected. For example "en", "nl" ...
	 */
	public void selectLanguage(String preselect_language)
	{
		for (int i = languagesCombo.getItemCount() - 1; i >= 0; i--)
		{
			Object[] val = (Object[])languagesCombo.getItemAt(i);
			if (preselect_language.equals(val[0]))
			{
				languagesCombo.setSelectedIndex(i);
				break;
			}
		}
	}
}
