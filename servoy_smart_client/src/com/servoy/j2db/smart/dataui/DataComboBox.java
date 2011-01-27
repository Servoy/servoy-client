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
package com.servoy.j2db.smart.dataui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.ItemSelectable;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.ComboModelListModelWrapper;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;

/**
 * Runtime swing combo box component
 * @author jblok
 */
public class DataComboBox extends JComboBox implements IDisplayData, IDisplayRelatedData, IFieldComponent, ISkinnable, ItemListener,
	IScriptDataComboboxMethods, ISupportCachedLocationAndSize
{
	private String dataProviderID;
	private final ComboModelListModelWrapper list;
	private Format format;

	private Border marginBorder;
	protected IValueList vl;
	protected IApplication application;

	private final EventExecutor eventExecutor;
	private String tooltip;
	private MouseAdapter rightclickMouseAdapter = null;
	// the call to super() on winXP L&F (probably others as well) will set the foreground color
	// of the popup to what getForeground() returns; the members are not yet
	// initialized in this stage - so booleans are not set yet to default values (they are false);
	// so this is why I changed isValueValid to invalid (so the initial value is false and getForeground
	// does not return Color.RED)
	private boolean invalid = false;
	private Object previousValidValue;
	private FormattedComboBoxEditor formattedComboEditor;
	private final ComboBoxAccesibleStateHolder accesibleStateHolder;
	private final DocumentListener closePopupDocumentListener;
	private int keyReleaseToBeIgnored = -1;

	public DataComboBox(IApplication application, IValueList vl)
	{
		super();

		hackDefaultPopupWidthBehavior();
		this.application = application;
		this.vl = vl;
		eventExecutor = new EventExecutor(this);

		list = new ComboModelListModelWrapper(vl, false);
		setModel(list);
		accesibleStateHolder = new ComboBoxAccesibleStateHolder(new ComboBoxStateApplier()
		{
			public void setEditable(boolean editable)
			{
				DataComboBox.this.setComboEditable(editable);
			}

			public void setEnabled(boolean enabled)
			{
				if (isEnabled() != enabled)
				{
					DataComboBox.this.setEnabled(enabled);
				}
			}

			public void setLabelsEnabled(boolean labelsEnabled)
			{
				if (labels != null)
				{
					for (int i = 0; i < labels.size(); i++)
					{
						ILabel label = labels.get(i);
						label.setComponentEnabled(labelsEnabled);
					}
				}
			}
		});

//DISABLED:for 1.4			
//		setPrototypeDisplayValue(new Integer(0));
		setMaximumRowCount(20);
//		addPopupMenuListener(this);
//		setLightWeightPopupEnabled(false);

		// if we have an editable combo, close popup if user starts to type;
		// otherwise we will not know what value to use when ENTER is pressed - the one from the popup
		// or the one from the text field
		closePopupDocumentListener = new DocumentListener()
		{
			public void changedUpdate(DocumentEvent e)
			{
				if (isPopupVisible())
				{
					hidePopup();
				}
			}

			public void insertUpdate(DocumentEvent e)
			{
				if (isPopupVisible())
				{
					hidePopup();
				}
			}

			public void removeUpdate(DocumentEvent e)
			{
				if (isPopupVisible())
				{
					hidePopup();
				}
			}
		};

		// When drop-down is visible, key_up and key_down must not fire action/selection events. Only final selection should trigger these events.
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4199622
		// for 1.3
		putClientProperty("JComboBox.lightweightKeyboardNavigation", "Lightweight"); //$NON-NLS-1$ //$NON-NLS-2$
		// for >= 1.4
		if (UIManager.getLookAndFeel().getClass().getName().toUpperCase().indexOf("AQUA") < 0) //$NON-NLS-1$
		{
			// this in not needed on MAC as default L&F overrides this behavior...
			// this would also cause appearance problems for comboboxes on MAC (JVM bug(s)) 
			putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE); //$NON-NLS-1$
		}

		// this property says whether you can select items from the drop-down with ENTER or not in an editable combobox (since 1.5xx - visible bug effect in 1.6)
		UIManager.put("ComboBox.isEnterSelectablePopup", Boolean.TRUE); //$NON-NLS-1$

		setRenderer(new DividerListCellRenderer(getRenderer()));
	}

	private void hackDefaultPopupWidthBehavior()
	{
		// workaround to make popup width take into consideration the length of text in all items;
		// the workaround was provided by Argos (Anuradha)
		addPopupMenuListener(new PopupMenuListener()
		{
			//Popup state to prevent feedback
			private boolean stateCmb = false;
			private int defaultWidth = 0;

			//Extend JComboBox's length and reset it
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				JComboBox cmb = (JComboBox)e.getSource();
				if (cmb.getItemCount() > 0)
				{
					if (!stateCmb)
					{
						defaultWidth = cmb.getSize().width;
					}
					//Extend JComboBox
					cmb.setSize(getExtendedWidth(cmb), cmb.getHeight());
					//If it pops up now JPopupMenu will still be short
					//Fire popupMenuCanceled...
					if (!stateCmb)
					{
						cmb.firePopupMenuCanceled();
					}
					//Reset JComboBox and state
					stateCmb = false;
					cmb.setSize(defaultWidth, cmb.getHeight());
				}
			}

			private int getExtendedWidth(JComboBox cmb)
			{

				int width = (int)cmb.getSize().getWidth();


				for (int i = 0; i < cmb.getItemCount(); i++)
				{
					Object text = cmb.getItemAt(i);
					if (text == null) continue;

					int textWidth = cmb.getFontMetrics(cmb.getFont()).stringWidth(text.toString());
					width = Math.max(width, textWidth + 10);//add offset 10
				}

				return width;
			}

			//Show extended JPopupMenu

			public void popupMenuCanceled(PopupMenuEvent e)
			{
				JComboBox cmb = (JComboBox)e.getSource();
				if (cmb.getItemCount() > 0)
				{
					stateCmb = true;
					//JPopupMenu is long now, so repop
					cmb.showPopup();
				}
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				stateCmb = false;
			}
		});
	}

	/**
	 * @return the list
	 */
	private ComboModelListModelWrapper getListModelWrapper()
	{
		return (ComboModelListModelWrapper)getModel();
	}

	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
		// if actionListner then fire action command on item change
		addItemListener(this);
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}


	public void setEnterCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setEnterCmds(ids, args);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setLeaveCmds(ids, args);
	}

	public boolean isValueValid()
	{
		return !invalid;
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		if (isEditable())
		{
			((FormattedComboBoxEditor)getEditor()).editor.setValueValid(valid, oldVal);
		}

		invalid = !valid;
		if (invalid)
		{
			previousValidValue = oldVal;
			if (!isEditable())
			{
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						requestFocus();
					}
				});
			} // else the editor will grab focus itself
		}
		else
		{
			previousValidValue = null;
		}
	}

	@Override
	public Color getForeground()
	{
		if (isValueValid())
		{
			return super.getForeground();
		}
		return Color.RED;
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		if (previousValidValue != null)
		{
			eventExecutor.fireChangeCommand(previousValidValue, newVal, false, this);
		}
		else
		{
			eventExecutor.fireChangeCommand(oldVal, newVal, false, this);
		}


		// if change cmd is not succeeded also don't call action cmd?
		if (isValueValid())
		{
			eventExecutor.fireActionCommand(false, this);
		}
	}

	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
		if (rightClickCmd != null && rightclickMouseAdapter == null)
		{
			rightclickMouseAdapter = new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (e.isPopupTrigger()) handle(e);
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (e.isPopupTrigger()) handle(e);
				}

				private void handle(MouseEvent e)
				{
					if (isEnabled())
					{
						eventExecutor.fireRightclickCommand(true, DataComboBox.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			if (isEditable())
			{
				FormattedComboBoxEditor ed = (FormattedComboBoxEditor)getEditor();
				ed.getEditorComponent().addMouseListener(rightclickMouseAdapter);
				ed.getEditorComponent().addKeyListener(eventExecutor);
			}
			else
			{
				addMouseListener(rightclickMouseAdapter);
				// as this is a composite control, also add listener to its parts
				int nrComboParts = getComponentCount();
				for (int i = 0; i < nrComboParts; i++)
					getComponent(i).addMouseListener(rightclickMouseAdapter);
				addKeyListener(eventExecutor);
			}
		}
	}

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;
		eventExecutor.setValidationEnabled(b);

		if (vl.getFallbackValueList() != null)
		{
			if (b)
			{
				getListModelWrapper().register(vl);
			}
			else
			{
				getListModelWrapper().register(vl.getFallbackValueList());
			}
		}

		if (b)
		{
			accesibleStateHolder.applyState();
		}
		else
		{
			setComboEditable(!getListModelWrapper().hasRealValues());
			if (!isEnabled() && accesibleStateHolder.isAccessible()) setEnabled(true);
		}
		if (isEditable())
		{
			// changing validation enabled state on the editor will change it's document;
			// so register the listener to the correct document again
			formattedComboEditor.editor.getDocument().removeDocumentListener(closePopupDocumentListener);
			formattedComboEditor.setValidationEnabled(b);
			formattedComboEditor.editor.getDocument().addDocumentListener(closePopupDocumentListener);
			formattedComboEditor.setItem(getSelectedItem());
		}

	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	public void destroy()
	{
		getListModelWrapper().deregister();
	}

	class DividerListCellRenderer implements ListCellRenderer
	{
		ListCellRenderer dividerRenderer;

		public DividerListCellRenderer(ListCellRenderer renderer)
		{
			super();
			this.dividerRenderer = renderer;
		}

		public Component getListCellRendererComponent(JList jlist, Object listValue, int index, boolean isSelected, boolean cellHasFocus)
		{
			if (IValueList.SEPARATOR.equals(listValue))
			{
				return new VariableSizeJSeparator(SwingConstants.HORIZONTAL, 15);
			}
			else
			{
				Object formattedValue = listValue;
				if (!"".equals(formattedValue) && format != null && formattedValue != null && !(formattedValue instanceof String)) //$NON-NLS-1$
				{
					try
					{
						formattedValue = format.format(formattedValue);
					}
					catch (IllegalArgumentException ex)
					{
						Debug.error("Error formatting value for combobox " + dataProviderID + ", " + ex); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				Component comp = dividerRenderer.getListCellRendererComponent(jlist, formattedValue, index, isSelected, cellHasFocus);
//nothing does work we cannot get the collapst combobox transparent				
//				if (comp instanceof JComponent)
//				{
//					((JComponent)list.getParent()).setOpaque(DataComboBox.this.isOpaque());
//					list.setOpaque(DataComboBox.this.isOpaque());
//					((JComponent)comp).setOpaque(DataComboBox.this.isOpaque());
//				}
				if (comp instanceof JLabel)
				{
					((JLabel)comp).setHorizontalAlignment(getHorizontalAlignment());
					if (marginBorder != null)
					{
						((JLabel)comp).setBorder(marginBorder);
					}
				}
				return comp;
			}
		}
	}

	/**
	 * A JSeparator substitute that is able to take up more then two pixels of space and draw the separator graphics in the middle of the occupied space.
	 */
	class VariableSizeJSeparator extends JPanel
	{

		private final int space;
		private final int orientation;

		public VariableSizeJSeparator(int orientation, int space)
		{
			super(new GridBagLayout());
			this.space = space;
			this.orientation = orientation;

			setOpaque(false);
			GridBagConstraints c = new GridBagConstraints();
			int row = 0, col = 0;

			JLabel l = new JLabel();
			c.gridy = row;
			c.gridx = col;
			if (orientation == SwingConstants.HORIZONTAL)
			{
				c.weighty = 1;
				row++;
			}
			else
			{
				c.weightx = 1;
				col++;
			}
			add(l, c);

			c = new GridBagConstraints();
			JSeparator separator = new JSeparator(orientation);
			c.gridy = row;
			c.gridx = col;
			if (orientation == SwingConstants.HORIZONTAL)
			{
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 1;
				row++;
			}
			else
			{
				c.fill = GridBagConstraints.VERTICAL;
				c.weighty = 1;
				col++;
			}
			add(separator, c);

			c = new GridBagConstraints();
			l = new JLabel();
			c.gridy = row;
			c.gridx = col;
			if (orientation == SwingConstants.HORIZONTAL)
			{
				c.weighty = 1;
			}
			else
			{
				c.weightx = 1;
			}
			add(l, c);
		}

		@Override
		public Dimension getPreferredSize()
		{
			if (orientation == SwingConstants.HORIZONTAL)
			{
				return new Dimension(0, space);
			}
			else
			{
				return new Dimension(space, 0);
			}
		}

	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	public void setMaxLength(int i)
	{
		ComboBoxEditor cbe = getEditor();
		if (cbe instanceof FormattedComboBoxEditor)
		{
			((FormattedComboBoxEditor)cbe).setMaxLength(i);
		}
	}

	public void setMargin(Insets m)
	{
		ComboBoxEditor cbe = getEditor();
		if (cbe instanceof FormattedComboBoxEditor)
		{
			((FormattedComboBoxEditor)cbe).setMargin(m);
		}
		else
		{
			marginBorder = BorderFactory.createEmptyBorder(m.top, m.left, m.bottom, m.right);
//			setBorder(BorderFactory.createCompoundBorder(getBorder(), ));
		}
	}

	public Document getDocument()
	{
		ComboBoxEditor cbe = getEditor();
		if (cbe instanceof FormattedComboBoxEditor)
		{
			return ((FormattedComboBoxEditor)cbe).getDocument();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataui.IFieldComponent#setOpaque(boolean)
	 */
	@Override
	public void setOpaque(boolean b)
	{
		super.setOpaque(b);
		ComboBoxEditor cbe = getEditor();
		if (cbe != null && cbe.getEditorComponent() != null && cbe.getEditorComponent() instanceof JComponent)
		{
			((JComponent)cbe.getEditorComponent()).setOpaque(b);
		}
	}

	// implements a Servoy IFieldComponent interface method, but also overrides JComboBox method - and we
	// must differentiate between the two
	@Override
	public void setEditable(boolean editable)
	{
		// never allow combos with real values to be editable
		accesibleStateHolder.setEditable(editable && (!getListModelWrapper().hasRealValues()));
	}

	@Override
	public void contentsChanged(ListDataEvent e)
	{
		super.contentsChanged(e);
		boolean editable = accesibleStateHolder.isEditable();
		if (editable && getListModelWrapper().hasRealValues())
		{
			// happens when set valuelist items at runtime
			setEditable(false);
		}
	}

	private void setComboEditable(boolean editable)
	{
		boolean wasEditable = isEditable();
		if (editable)
		{
			if (!wasEditable || formattedComboEditor == null)
			{
				if (formattedComboEditor == null)
				{
					formattedComboEditor = new FormattedComboBoxEditor(application, getEditor(), getModel());
					formattedComboEditor.editor.setDataProviderID(getDataProviderID());
					formattedComboEditor.getEditorComponent().setName(getName());
					formattedComboEditor.editor.setOpaque(isOpaque());
					formattedComboEditor.editor.setHorizontalAlignment(getHorizontalAlignment());
					formattedComboEditor.editor.setBorder(null);
					formattedComboEditor.editor.getDocument().addDocumentListener(closePopupDocumentListener);
					formattedComboEditor.editor.setTransferHandler(getTransferHandler());
					if (marginBorder instanceof EmptyBorder)
					{
						formattedComboEditor.editor.setMargin(((EmptyBorder)marginBorder).getBorderInsets());
					}
					if (!Utils.isAppleMacOS())
					{
						// on MAC default L&F, if you type something in an editable combo while the dropdown is visible
						// and then press ENTER - the typed content will dissapear - so it can be annoying to
						// show the dropdown when the combo gets focus (the user would need to press ESC in order to type...)
						formattedComboEditor.getEditorComponent().addFocusListener(new FocusListener()
						{
							//dipatch to my listers
							public void focusGained(FocusEvent e)
							{
								if (!DataComboBox.this.isPopupVisible() && DataComboBox.this.isVisible() && DataComboBox.this.isEnabled())
								{
									try
									{
										showPopup();
									}
									catch (Exception ex)
									{
									}
								}
							}

							//dipatch to my listers
							public void focusLost(FocusEvent e)
							{
								if (DataComboBox.this.isPopupVisible() && DataComboBox.this.isVisible())
								{
									try
									{
										// Fix for 1.4.1 focus goes to a window (the popup itself)
										if (!(e.getOppositeComponent() instanceof Window))
										{
											hidePopup();
										}
									}
									catch (Exception ex)
									{
									}
								}
							}
						});
					}
				}
				setEditor(formattedComboEditor);
				eventExecutor.setEnclosedComponent(getEditor().getEditorComponent());
				super.setEditable(true);
				if (editProvider != null)
				{
					removeFocusListener(editProvider);
					formattedComboEditor.getEditorComponent().addFocusListener(editProvider);
				}
				repaint();
			}
		}
		else if (wasEditable)
		{
			// it is possible that the current enclosed component of
			// the even executor has focusLost in the event queue, that
			// will be ignored if we set a new enclosing component, so do it after
			if (application.isEventDispatchThread())
			{
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						setComboNotEditable();
					}
				});
			}
			else setComboNotEditable();
		}
	}

	private void setComboNotEditable()
	{
		setEditor(formattedComboEditor.getDefaultEditor());
		eventExecutor.setEnclosedComponent(this);
		super.setEditable(false);
		if (editProvider != null)
		{
			addFocusListener(editProvider);
			formattedComboEditor.getEditorComponent().removeFocusListener(editProvider);
		}
		repaint();
	}

	@Override
	protected void processFocusEvent(FocusEvent e)
	{
		super.processFocusEvent(e);
		if (!isEditable())
		{
			if (e.getID() == FocusEvent.FOCUS_GAINED && !isPopupVisible() && isVisible() && isEnabled())
			{
				try
				{
					showPopup();
				}
				catch (Exception ex)
				{
				}
			}
			else if (e.getID() == FocusEvent.FOCUS_LOST && isPopupVisible() && isVisible())
			{
				try
				{
					// Fix for 1.4.1 focus goes to a window (the popup itself)
					if (!(e.getOppositeComponent() instanceof Window))
					{
						hidePopup();
					}
				}
				catch (Exception ex)
				{
				}
			}
		}
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		// in find mode, ESC or ENTER while popup is open should not trigger search (should not reach
		// the perform/cancel search key bindings in parent components; if popup was closed on keypress
		// then consider keyReleased on the same key used also
		boolean popupWasOpen = isPopupVisible();
		boolean keyEventWasUsed = super.processKeyBinding(ks, e, condition, pressed);
		if ((e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) && e.getModifiers() == InputEvent.ALT_MASK)
		{
			keyReleaseToBeIgnored = -1;
			keyEventWasUsed = false;
		}
		else if (e.getKeyCode() == KeyEvent.VK_F && e.getModifiers() == InputEvent.CTRL_MASK)
		{
			keyReleaseToBeIgnored = -1;
			keyEventWasUsed = false;
		}
		else if (popupWasOpen && !isPopupVisible() && ks.getKeyEventType() == KeyEvent.KEY_PRESSED &&
			(ks.getKeyCode() == KeyEvent.VK_ESCAPE || ks.getKeyCode() == KeyEvent.VK_ENTER))
		{
			keyReleaseToBeIgnored = ks.getKeyCode();
		}
		else if (keyReleaseToBeIgnored == ks.getKeyCode() && ks.getKeyEventType() == KeyEvent.KEY_RELEASED)
		{
			keyReleaseToBeIgnored = -1;
			return true;
		}
		else if (ks.getKeyEventType() != KeyEvent.KEY_TYPED)
		{
			keyReleaseToBeIgnored = -1; // get rid of this value, because this in only implemented for simple scenarios
			// and it shouldn't cause trouble
		}
		return keyEventWasUsed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		super.setName(name);
	}

	public int getDataType()
	{
		return dataType;
	}

	private int dataType;

	public void setFormat(int dataType, String formatString)
	{
		this.dataType = dataType;
		if (isEditable())
		{
			FormattedComboBoxEditor ed = (FormattedComboBoxEditor)getEditor();
			ed.setFormat(dataType, formatString);
		}
		if (formatString != null && formatString.length() != 0)
		{
			try
			{
				switch (Column.mapToDefaultType(dataType))
				{
					case IColumnTypes.NUMBER :
						format = new RoundHalfUpDecimalFormat(formatString, application.getLocale());
						break;
					case IColumnTypes.INTEGER :
						format = new RoundHalfUpDecimalFormat(formatString, application.getLocale());
						break;
					case IColumnTypes.DATETIME :
						format = new StateFullSimpleDateFormat(formatString, Boolean.TRUE.equals(UIUtils.getUIProperty(this,
							IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE)));
						// format = new SimpleDateFormat(formatString);
						break;
					default :
						// TODO Jan/Johan what to do here? Should we create our own MaskFormatter? Where we can insert a mask??
						break;
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	public String getFormat()
	{
		if (isEditable())
		{
			FormattedComboBoxEditor ed = (FormattedComboBoxEditor)getEditor();
			return ed.getFormat();
		}
		return null;
	}

	public String js_getFormat()
	{
		return getFormat();
	}

	public void js_setFormat(String frmt)
	{
		setFormat(dataType, application.getI18NMessageIfPrefixed(frmt));
	}

	private int halign;

	private boolean selectingItem;

	public void setHorizontalAlignment(int a)
	{
		if (isEditable())
		{
			FormattedComboBoxEditor ed = (FormattedComboBoxEditor)getEditor();
			ed.setHorizontalAlignment(a);
		}
		halign = a;
	}

	private int getHorizontalAlignment()
	{
		return halign;
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (adjusting || getListModelWrapper().isValueListChanging()) return;

		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			if (editProvider != null) editProvider.itemStateChanged(e);
			// Do not call the action command here, it will be called in notifyLastNewValueWasChange when on change was successful
		}
		else if (e.getStateChange() == ItemEvent.DESELECTED && getModel().getSelectedItem() == null)
		{
			if (editProvider != null) editProvider.itemStateChanged(new ItemEvent((ItemSelectable)e.getSource(), e.getID(), e.getItem(), ItemEvent.SELECTED));
			// Do not call the action command here, it will be called in notifyLastNewValueWasChange when on change was successful
		}
	}

	@Override
	public void setBackground(Color bg)
	{
		if (isEditable())
		{
			FormattedComboBoxEditor ed = (FormattedComboBoxEditor)getEditor();
			ed.getEditorComponent().setBackground(bg);
		}
		super.setBackground(bg);
	}

	@Override
	public void setForeground(Color fg)
	{
		if (isEditable())
		{
			FormattedComboBoxEditor ed = (FormattedComboBoxEditor)getEditor();
			ed.getEditorComponent().setForeground(fg);
		}
		super.setForeground(fg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataui.IFieldComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(String tip)
	{
		if (tip != null && tip.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tooltip = tip;
		}
		else if (!Utils.stringIsEmpty(tip))
		{
			if (!Utils.stringContainsIgnoreCase(tip, "<html")) //$NON-NLS-1$
			{
				super.setToolTipText(tip);
			}
			else if (HtmlUtils.hasUsefulHtmlContent(tip))
			{
				super.setToolTipText(tip);
			}
		}
		else
		{
			super.setToolTipText(null);
		}
	}

	/**
	 * Overwritten for handling statefull dates. So we can merge dates from the selected element in the list with the orignal date
	 */
	@Override
	public void setSelectedIndex(int anIndex)
	{
		int size = dataModel.getSize();

		if (anIndex == -1)
		{
			setSelectedItem(null);
		}
		else if (anIndex >= size)
		{
			throw new IllegalArgumentException("setSelectedIndex: " + anIndex + " out of bounds"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			Object value = dataModel.getElementAt(anIndex);
			if (IValueList.SEPARATOR.equals(value))
			{
				// separators should not be selectable - so hijack this one and choose the nearest non-separator value
				boolean found = false;
				int i = 1;
				while (!found && (anIndex + i < size || anIndex - i >= 0))
				{
					if (anIndex + i < size && !IValueList.SEPARATOR.equals(dataModel.getElementAt(anIndex + i)))
					{
						found = true;
						setSelectedIndex(anIndex + i);
					}
					else if (anIndex - i >= 0 && !IValueList.SEPARATOR.equals(dataModel.getElementAt(anIndex - i)))
					{
						found = true;
						setSelectedIndex(anIndex - i);
					}
					i++;
				}
				if (!found)
				{
					setSelectedItem(-1);
				}
			}
			else
			{
				Object oldValue = getValueObject();
				if (oldValue instanceof Date && value instanceof Date && format instanceof StateFullSimpleDateFormat)
				{
					StateFullSimpleDateFormat sfsd = (StateFullSimpleDateFormat)format;
					sfsd.setOriginal((Date)oldValue);
					String stringRep = sfsd.format(value);
					try
					{
						sfsd.parse(stringRep);
						value = sfsd.getMergedDate();
					}
					catch (ParseException e)
					{
						Debug.error(e);
					}
				}
				setSelectedItem(value);
			}
		}
	}

	@Override
	public void setSelectedItem(Object anObject)
	{
		Object oldSelection = selectedItemReminder;
		if (oldSelection == null || !oldSelection.equals(anObject))
		{

			if (anObject != null && !isEditable())
			{
				// For non editable combo boxes, an invalid selection
				// will be rejected.
				boolean found = false;

				// Test if it is a date, so that we only compare the format string (what the user sees)
				// as the equals value and not the complete date, that can have seperate input fields.
				if (anObject instanceof Date && format instanceof StateFullSimpleDateFormat)
				{
					StateFullSimpleDateFormat sfsd = (StateFullSimpleDateFormat)format;
					String selectedFormat = sfsd.format(anObject);
					for (int i = 0; i < dataModel.getSize(); i++)
					{
						try
						{
							Object element = dataModel.getElementAt(i);
							if (!(element instanceof Date)) continue;

							String elementFormat = sfsd.format(element);
							if (selectedFormat.equals(elementFormat))
							{
								found = true;
								break;
							}
						}
						catch (RuntimeException e)
						{
							Debug.error(e);
						}
					}
				}
				else
				{
					if (dataModel instanceof ComboModelListModelWrapper)
					{
						found = ((ComboModelListModelWrapper)dataModel).indexOf(anObject) != -1;
					}
					else for (int i = 0; i < dataModel.getSize(); i++)
					{
						if (anObject.equals(dataModel.getElementAt(i)))
						{
							found = true;
							break;
						}
					}
				}
				if (!found)
				{
					return;
				}
			}

			// Must toggle the state of this flag since this method
			// call may result in ListDataEvents being fired.
			selectingItem = true;
			dataModel.setSelectedItem(anObject);
			selectingItem = false;

			if (selectedItemReminder != dataModel.getSelectedItem())
			{
				// in case a users implementation of ComboBoxModel
				// doesn't fire a ListDataEvent when the selection
				// changes.
				selectedItemChanged();
			}
		}
		fireActionEvent();
	}


	@Override
	protected void fireActionEvent()
	{
		if (!selectingItem)
		{
			super.fireActionEvent();
		}
	}

	public Object getValueObject()
	{
		if (isEditable())
		{
			// get the selected item from the editor (or else editable combo's in portals won't set the value)
			return ((FormattedComboBoxEditor)getEditor()).getItem();
		}
		if (dataModel instanceof ComboModelListModelWrapper< ? >)
		{
			int index = ((ComboModelListModelWrapper< ? >)dataModel).indexOf(dataModel.getSelectedItem());
			if (index != -1) return getListModelWrapper().getRealElementAt(index);
		}

		if (getSelectedIndex() < 0)
		{
			return getSelectedItem();//editted value
		}
		else
		{
			return getListModelWrapper().getRealElementAt(getSelectedIndex());
		}
	}

	/**
	 * @see javax.swing.JComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		if (resolver != null && tooltip != null)
		{
			String oldValue = tooltip;
			tooltip = null;
			super.setToolTipText(Text.processTags(oldValue, resolver));
			tooltip = oldValue;
		}
		return super.getToolTipText();
	}

	/**
	 * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
	 */
	@Override
	public String getToolTipText(MouseEvent event)
	{
		return getToolTipText();
	}


	boolean adjusting = false;

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object data)
	{
		try
		{
			adjusting = true;
			if (editProvider != null) editProvider.setAdjusting(true);
			if (needEntireState)
			{
				if (tooltip != null)
				{
					super.setToolTipText(""); //$NON-NLS-1$
				}
			}
			else
			{
				if (tooltip != null)
				{
					super.setToolTipText(tooltip);
				}
			}
			if (getSelectedItem() == null || !Utils.equalObjects(selectedItemReminder, data))
			{
				int index = getListModelWrapper().realValueIndexOf(data);
				if (index == -1)
				{
					if (isEditable() && accesibleStateHolder.isEditable())
					{
						setSelectedItem(data);
					}
					else
					{
						if (data == null || "".equals(data) || getListModelWrapper().hasRealValues()) //$NON-NLS-1$
						{
							setSelectedItem(null);
						}
						else
						{
							getModel().setSelectedItem(data);
						}
					}
				}
				else if (dataType == IColumnTypes.DATETIME)
				{
					setSelectedItem(data);
				}
				else
				{
					setSelectedIndex(index);
				}
				setValueValid(true, null);
			}
		}
		finally
		{
			adjusting = false;
			if (editProvider != null) editProvider.setAdjusting(false);
		}
	}

	public boolean needEditListner()
	{
		return true;
	}

	private EditProvider editProvider = null;

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this);
			//addItemListener(editProvider);
			editProvider.addEditListener(l);

			if (isEditable())
			{
				formattedComboEditor.getEditorComponent().addFocusListener(editProvider);
			}
			else
			{
				addFocusListener(editProvider);
			}
		}
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	private boolean needEntireState;

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
		getListModelWrapper().setDataProviderID(id);
		if (isEditable())//for debugging so component has name
		{
			Component comp = getEditor().getEditorComponent();
			comp.setName(id);
			if (comp instanceof IDisplayData)
			{
				((IDisplayData)comp).setDataProviderID(dataProviderID);
			}
		}
	}

	/*
	 * _____________________________________________________________ Methods for IDisplayRelatedData
	 */
	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		try
		{
			adjusting = true;
			if (editProvider != null) editProvider.setAdjusting(true);
			Object selected = getSelectedItem();
			getListModelWrapper().fill(state);
			if (isEditable() && !Utils.equalObjects(selected, getSelectedItem()))
			{
				setSelectedItem(selected);
			}
		}
		finally
		{
			adjusting = false;
			if (editProvider != null) editProvider.setAdjusting(false);
		}
	}

	public String getSelectedRelationName()
	{
		if (relationName == null && getListModelWrapper() != null)
		{
			relationName = getListModelWrapper().getRelationName();
		}
		return relationName;
	}

	private String relationName = null;

	public String[] getAllRelationNames()
	{
		String selectedRelationName = getSelectedRelationName();
		if (selectedRelationName == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { selectedRelationName };
		}
	}

	public List<SortColumn> getDefaultSort()
	{
		return null;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (isEditable())
		{
			FormattedComboBoxEditor cbe = (FormattedComboBoxEditor)getEditor();
			DataField editorComponent = (DataField)cbe.getEditorComponent();
			boolean isEditing = editorComponent.hasFocus();
			// we really want to check here an invalid state from datafield
			if (!editorComponent.isValueValid() && !Utils.equalObjects(editorComponent.getValue(), getValueObject())) editorComponent.setValueValid(true, null);
			boolean stopAllowed = cbe.stopEditing(looseFocus);
			if (stopAllowed && isEditing)
			{
				// make sure the value change events are triggered if needed;
				// there was a problem in list view - editing value with the editor, then click on the
				// combo on another row => the new value would not be applied, although stopUIEditing() is
				// called before the selected row is changed. FocusLost - which is used by comboUI to trigger value change events
				// is triggered after the selected row is changed in this scenario - and because of this the new value is not applied.
				getModel().setSelectedItem(editorComponent.getValue());
			}
			return stopAllowed && isValueValid();
		}
		if (!isValueValid())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					requestFocus();
				}
			});
			return false;
		}
		return true;
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	// If component not shown or not added yet 
	// and request focus is called it should wait for the component
	// to be created.
	boolean wantFocus = false;
	private List<ILabel> labels;

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (isEditable)
		{
			// in java 1.5, ending cell editing of a editable combo in tableview results in focus
			// being given to the header part... this fixes it
			Container nearestRoot = (isFocusCycleRoot()) ? this : getFocusCycleRootAncestor();
			FocusTraversalPolicy policy = nearestRoot.getFocusTraversalPolicy();
			if (policy.getClass().getName().indexOf("LegacyGlueFocusTraversalPolicy") >= 0) //$NON-NLS-1$
			{
				DataField editorComponent = (DataField)getEditor().getEditorComponent();
				nearestRoot = (editorComponent.isFocusCycleRoot()) ? this : editorComponent.getFocusCycleRootAncestor();
				policy = nearestRoot.getFocusTraversalPolicy();
				if (policy.getClass().getName().indexOf("LegacyGlueFocusTraversalPolicy") >= 0) //$NON-NLS-1$
				{
					editorComponent.setNextFocusableComponent(getNextFocusableComponent());
				}
			}
		}
		if (wantFocus)
		{
			wantFocus = false;
			requestFocus();
		}
	}

	/*
	 * _____________________________________________________________ Methods for javascript
	 */

	public void js_requestFocus(Object[] vargs)
	{
//		if (!hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0]))
			{
				eventExecutor.skipNextFocusGain();
			}
			if (isDisplayable())
			{
				// Must do it in a runnable or else others after a script can get focus first again..
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						requestFocus();
					}
				});
			}
			else
			{
				wantFocus = true;
			}
		}
	}

	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(getFont());
	}

	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public String js_getValueListName()
	{
		if (vl != null)
		{
			return vl.getName();
		}
		return null;
	}

	public void js_setValueListItems(Object value)
	{
		if (vl != null && getListModelWrapper() != null && (value instanceof JSDataSet || value instanceof IDataSet))
		{
			String name = vl.getName();
			ValueList valuelist = application.getFlattenedSolution().getValueList(name);
			if (valuelist != null && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
			{
				String frmt = getListModelWrapper().getFormat();
				int type = getListModelWrapper().getValueType();
				IValueList newVl = ValueListFactory.fillRealValueList(application, valuelist, ValueList.CUSTOM_VALUES, frmt, type, value);
				vl = newVl;
				getListModelWrapper().register(vl);
			}
		}
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.COMBOBOX;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
	}

	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
	}

	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getBorder());
	}

	/*
	 * visible---------------------------------------------------
	 */
	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}

	@Override
	public void setVisible(boolean flag)
	{
		super.setVisible(flag);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				label.setComponentVisible(flag);
			}
		}
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public String[] js_getLabelForElementNames()
	{
		if (labels != null)
		{
			List<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
	}

	/*
	 * opaque---------------------------------------------------
	 */
	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
	}

	/*
	 * enabled---------------------------------------------------
	 */
	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	public void js_setEnabled(boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(boolean b)
	{
		accesibleStateHolder.setEnabled(b);
	}

	public void setAccessible(boolean b)
	{
		accesibleStateHolder.setAccessible(b);
	}


	/*
	 * readonly/editable---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public void js_setReadOnly(boolean b)
	{
		accesibleStateHolder.setReadOnly(b);
	}

	public boolean js_isEditable()
	{
		return isEditable();
	}

	public void js_setEditable(boolean b)
	{
		setEditable(b);
	}

	/*
	 * titleText---------------------------------------------------
	 */

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	public String js_getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	/*
	 * tooltip---------------------------------------------------
	 */
	public void js_setToolTipText(String txt)
	{
		setToolTipText(txt);
	}

	public String js_getToolTipText()
	{
		return getToolTipText();
	}

	/*
	 * location---------------------------------------------------
	 */
	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		Container parent = getParent();
		while ((parent != null) && !(parent instanceof IDataRenderer))
		{
			parent = parent.getParent();
		}
		if (parent != null)
		{
			return ((IDataRenderer)parent).getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	private Point cachedLocation;

	public void js_setLocation(int x, int y)
	{
		cachedLocation = new Point(x, y);
		setLocation(x, y);
		validate();
	}

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		putClientProperty(key, value);
		if (IApplication.DATE_FORMATTERS_LENIENT.equals(key) || IApplication.DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD.equals(key))
		{
			if (IApplication.DATE_FORMATTERS_LENIENT.equals(key) && format instanceof StateFullSimpleDateFormat)
			{
				((StateFullSimpleDateFormat)format).setLenient(Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.DATE_FORMATTERS_LENIENT,
					Boolean.TRUE)));
			}
			if (formattedComboEditor != null)
			{
				formattedComboEditor.putClientProperty(key, value);
			}
		}
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}

	private Dimension cachedSize;

	/*
	 * size---------------------------------------------------
	 */
	public void js_setSize(int x, int y)
	{
		cachedSize = new Dimension(x, y);
		setSize(x, y);
		validate();
	}

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	public int js_getHeight()
	{
		return getSize().height;
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	public static class FormattedComboBoxEditor implements ComboBoxEditor, FocusListener
	{
		protected DataField editor;//use datafield here because the processfocus is fixed and validation can be disabled
		private final ComboBoxEditor defaultEditor;

		public FormattedComboBoxEditor(IApplication app, ComboBoxEditor oldEditor, final ComboBoxModel model)
		{
			this.defaultEditor = oldEditor;
			editor = new DataField(app)
			{
				// text field should show its readonly-state when not editable
				@Override
				public boolean isOpaque()
				{
					return isEditable() ? super.isOpaque() : true;
				}

				@Override
				public void restorePreviousValidValue()
				{
					super.restorePreviousValidValue();
					model.setSelectedItem(getValue());
				}
			};
			// workaround for MAC OS X default L&F - combobox behavior declared in the editor component was lost when using our
			// custom editor (normally that behavior should be added to the new editor when it is changed...); anyway, what follows makes
			// up / down / select work in editable comboboxes on MAC...
			if (oldEditor != null && Utils.isAppleMacOS())
			{
				Component oldEditorComponent = oldEditor.getEditorComponent();
				if ((oldEditorComponent instanceof JComponent) && (oldEditorComponent.getClass().getName().toUpperCase().indexOf("CUIAQUA") >= 0)) //$NON-NLS-1$
				{
					InputMap oldInputMap = ((JComponent)oldEditorComponent).getInputMap();
					InputMap inputmap = editor.getInputMap();
					inputmap.put(KeyStroke.getKeyStroke("DOWN"), oldInputMap.get(KeyStroke.getKeyStroke("DOWN"))); //$NON-NLS-1$ //$NON-NLS-2$
					inputmap.put(KeyStroke.getKeyStroke("KP_DOWN"), oldInputMap.get(KeyStroke.getKeyStroke("KP_DOWN"))); //$NON-NLS-1$ //$NON-NLS-2$
					inputmap.put(KeyStroke.getKeyStroke("UP"), oldInputMap.get(KeyStroke.getKeyStroke("UP"))); //$NON-NLS-1$ //$NON-NLS-2$
					inputmap.put(KeyStroke.getKeyStroke("KP_UP"), oldInputMap.get(KeyStroke.getKeyStroke("KP_UP"))); //$NON-NLS-1$ //$NON-NLS-2$
					inputmap.put(KeyStroke.getKeyStroke("ENTER"), oldInputMap.get(KeyStroke.getKeyStroke("ENTER"))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
//	        editor.setBorder(null);
		}

		public ComboBoxEditor getDefaultEditor()
		{
			return defaultEditor;
		}

		public void setMaxLength(int i)
		{
			editor.setMaxLength(i);
		}

		public void setMargin(Insets i)
		{
			editor.setMargin(i);
		}

		public void putClientProperty(Object key, Object value)
		{
			editor.js_putClientProperty(key, value);
		}

		public void setFormat(int type, String format)
		{
			editor.setFormat(type, format);
		}

		public String getFormat()
		{
			return editor.getFormat();
		}

		public void setHorizontalAlignment(int a)
		{
			editor.setHorizontalAlignment(a);
		}

		public Component getEditorComponent()
		{
			return editor;
		}

		public Document getDocument()
		{
			return editor.getDocument();
		}

		public void setValidationEnabled(boolean b)//for find mode
		{
			editor.setValidationEnabled(b);
		}

		public boolean stopEditing(boolean looseFocus)
		{
			return editor.stopUIEditing(looseFocus);
		}

		/**
		 * Sets the item that should be edited.
		 * 
		 * @param anObject the displayed value of the editor
		 */
		public void setItem(Object anObject)
		{
			editor.setValueObject(anObject);
		}

		public Object getItem()
		{
			try
			{
				editor.commitEdit();
			}
			catch (ParseException e)
			{
				Debug.error(e);
			}
			Object newValue = editor.getValue();

/*
 * if (oldValue != null && !(oldValue instanceof String)) { // The original value is not a string. Should return the value in it's // original type. if
 * (newValue.equals(oldValue.toString())) { return oldValue; } else { // Must take the value from the editor and get the value and cast it to the new type.
 * Class cls = oldValue.getClass(); try { Method method = cls.getMethod("valueOf", new Class[]{String.class}); newValue = method.invoke(oldValue, new Object[] {
 * editor.getText()}); } catch (Exception ex) { // Fail silently and return the newValue (a String object) } } }
 */
			return newValue;
		}

		public void selectAll()
		{
			editor.selectAll();
			editor.requestFocus();
		}

		// This used to do something but now it doesn't.  It couldn't be
		// removed because it would be an API change to do so.
		public void focusGained(FocusEvent e)
		{
		}

		// This used to do something but now it doesn't.  It couldn't be
		// removed because it would be an API change to do so.
		public void focusLost(FocusEvent e)
		{
		}

		public void addActionListener(ActionListener l)
		{
			editor.addActionListener(l);
		}

		public void removeActionListener(ActionListener l)
		{
			editor.removeActionListener(l);
		}
	}

	/**
	 * Calculates actual JComboBox editable and enabled values based on Servoy accessible, read-only, editable, and enabled values. It then sets them back (when
	 * changed) to the JComboBox (through ComboBoxStateApplier).
	 */
	private static class ComboBoxAccesibleStateHolder
	{
		private boolean accessible = true;
		private boolean editable = false;
		private boolean readOnly = false;
		private boolean enabled = true;

		private final ComboBoxStateApplier applier;

		public ComboBoxAccesibleStateHolder(ComboBoxStateApplier applier)
		{
			if (applier == null) throw new NullPointerException();
			this.applier = applier;
		}

		public void applyState()
		{
			boolean jComboEnabled = accessible && enabled && !readOnly;
			applier.setEnabled(jComboEnabled);
			applier.setEditable(jComboEnabled && editable);
			applier.setLabelsEnabled(accessible && enabled);
		}

		public void setEnabled(boolean enabled)
		{
			this.enabled = enabled;
			applyState();
		}

		public void setEditable(boolean editable)
		{
			this.editable = editable;
			applyState();
		}

		public void setReadOnly(boolean readOnly)
		{
			this.readOnly = readOnly;
			applyState();
		}

		public void setAccessible(boolean accessible)
		{
			this.accessible = accessible;
			applyState();
		}

		public boolean isEditable()
		{
			return editable;
		}

		public boolean isAccessible()
		{
			return accessible;
		}

	}

	/**
	 * Classes that implement this interface are supposed to apply the editable and enabled values to a combobox, with the meaning JComboBox gives to them.
	 */
	private static interface ComboBoxStateApplier
	{
		void setEnabled(boolean comboEnabled);

		void setEditable(boolean editable);

		void setLabelsEnabled(boolean labelsEnabled);
	}

	@Override
	public void repaint()
	{
		// if repaint was requested because of a change in fireOnRender that was run from paint
		// ignore this repaint as the changes are already painted - if not ignored, we will have
		// a cycle calling of repaint -> paintComponent -> fireOnRender -> repaint 
		if (eventExecutor != null && eventExecutor.isOnRenderRunningOnComponentPaint()) return;
		super.repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		if (eventExecutor != null) eventExecutor.fireOnRender(this, hasFocus());
		super.paintComponent(g);
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return eventExecutor;
	}
}
