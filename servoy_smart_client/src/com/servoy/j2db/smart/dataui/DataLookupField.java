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


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISupportVisibleChangeListener;
import com.servoy.j2db.IVisibleChangeListener;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IDisplayDependencyData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;

/**
 * The Typeahead/DataLookup field that pops up a list and filters that list as you type.
 * 
 * @author jcompagner
 */
public class DataLookupField extends DataField implements IDisplayRelatedData, IDisplayDependencyData
{
	private static final long serialVersionUID = 1L;

	protected JWindow popup;
	protected JScrollPane scroller;
	protected JList jlist;

	protected LookupListModel dlm;
	protected ChangeListener changeListener;
	private boolean focusGainedOrValidationChange;

	/** maximum height for the popup that displays the jlist */
	protected final static int MAX_POPUP_HEIGHT = 150;
	/** maximum width; note that horizontal scroll bar is set to WHEN_NEEDED */
	protected final static int MAX_POPUP_WIDTH = 500;

	private IRecordInternal parentState;

	private boolean keyBindingChangedValBeforeFocusEvent = false;
	private boolean consumeEnterReleased;


	private static Timer timer = new Timer("Lookup ValueList Timer", true); //$NON-NLS-1$

	private final IVisibleChangeListener popupParentVisibleChangeListener = new IVisibleChangeListener()
	{
		public void beforeVisibleChange(final ISupportVisibleChangeListener component, boolean newVisibleState)
		{
			if (!newVisibleState && popup != null)
			{
				popup.setVisible(false);
			}
		}
	};

	public DataLookupField(IApplication app, CustomValueList list)
	{
		super(app, list);
		this.application = app;
		super.setEditable(true);
		registerKeyboardAction(new HidePopup(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		dlm = new LookupListModel(application, list);
		changeListener = new ChangeListener();
		list.addListDataListener(changeListener);
	}

	public DataLookupField(IApplication app, final LookupValueList list)
	{
		super(app, list);
		this.application = app;
		super.setEditable(true);
		registerKeyboardAction(new HidePopup(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		dlm = new LookupListModel(app, list);

		if (dlm.isShowValues() != dlm.isReturnValues())
		{
			try
			{
				changeListener = new ChangeListener();
				list.addListDataListener(changeListener);
			}
			catch (Exception e)
			{
				Debug.error("Error registering table"); //$NON-NLS-1$
				Debug.error(e);
			}
		}
	}

	/**
	 * @param application2
	 */
	public DataLookupField(IApplication application, String serverName, String tableName, String dataProviderID)
	{
		super(application);
		super.setEditable(true);
		registerKeyboardAction(new HidePopup(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		this.application = application;
		dlm = new LookupListModel(application, serverName, tableName, dataProviderID);
	}

	/**
	 * @see com.servoy.j2db.smart.dataui.DataField#js_getElementType()
	 */
	@Override
	public String js_getElementType()
	{
		return IScriptBaseMethods.TYPE_AHEAD;
	}

	@Override
	public void js_setValueListItems(Object value)
	{
		if (list instanceof CustomValueList && changeListener != null)
		{
			list.removeListDataListener(changeListener);
		}
		super.js_setValueListItems(value);
		if (list instanceof CustomValueList)
		{
			dlm = new LookupListModel(application, (CustomValueList)list);
			if (changeListener == null) changeListener = new ChangeListener();
			list.addListDataListener(changeListener);
			if (jlist != null)
			{
				jlist.setModel(dlm);
			}
		}
		setValue(getValue()); // force update the display value
	}

	/*
	 * @see javax.swing.JFormattedTextField#setDocument(javax.swing.text.Document)
	 */
	@Override
	public void setDocument(Document doc)
	{
		super.setDocument(doc);
	}

	private LookupDocumentListener listner;

	@Override
	protected void processFocusEvent(FocusEvent e)
	{
		if (listner == null) //only needed on renderers
		{
			getDocument().addDocumentListener(listner = new LookupDocumentListener());
		}

		focusGainedOrValidationChange = true;
		try
		{
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_LOST && !e.isTemporary() && popup != null)
			{
				popup.setVisible(false);
			}
			else if (e.getID() == FocusEvent.FOCUS_GAINED && !e.isTemporary() && e.getOppositeComponent() != this &&
				Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN, Boolean.TRUE)))
			{
				// do not call fillValueList(true) directly because we need to work around this problem:
				// Tableview, tab to a type-ahead and type a char (for example "a"). Then type-ahead cell enters edit mode,
				// key bindings get processed on the type-ahead (changing the content) and only after that the focus gain event arrives.
				// In this case we must avoid showing all available options (instead of the ones starting with "a"), because char typing 
				// behavior should have precedence on focus gain.
				fillValueList(!keyBindingChangedValBeforeFocusEvent);
			}
		}
		finally
		{
			focusGainedOrValidationChange = false;
			keyBindingChangedValBeforeFocusEvent = false;
		}
	}

	/*
	 * @see javax.swing.text.JTextComponent#removeNotify()
	 */
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		if (popup != null)
		{
			popup.setVisible(false);
		}
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		String prev = getText();
		boolean processed = super.processKeyBinding(ks, e, condition, pressed);
		String current = getText();
		if ((prev != null && !prev.equals(current)) || (prev == null && current != null))
		{
			keyBindingChangedValBeforeFocusEvent = true;
		}
		return processed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#processKeyEvent(java.awt.event.KeyEvent)
	 */
	@Override
	protected void processKeyEvent(KeyEvent e)
	{
		if (list instanceof LookupValueList)
		{
			((LookupValueList)list).setDoNotQuery(true);
		}
		try
		{
			if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				boolean doFill = false;
				synchronized (this)
				{
					if (task != null)
					{
						task.cancel();
						task = null;
						doFill = true;
					}
				}
				if (doFill) fillValueListImpl(false);

				Object value = getValue();
				commitSelectedValue();
				super.processKeyEvent(e);
				if (!eventExecutor.getValidationEnabled() && !Utils.equalObjects(value, getValue()))
				{
					// consume the enter release key in find mode.
					consumeEnterReleased = true;
				}
			}
			else if (e.getID() == KeyEvent.KEY_PRESSED && jlist != null && popup.isVisible())
			{
				if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					int index = jlist.getSelectedIndex() + 1;
					if (index < dlm.getSize())
					{
						jlist.setSelectedIndex(index);
					}
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					int index = jlist.getSelectedIndex() - 1;
					if (index >= 0)
					{
						jlist.setSelectedIndex(index);
					}
					e.consume();
				}
				else
				{
					super.processKeyEvent(e);
				}
			}
			else if (e.getID() == KeyEvent.KEY_RELEASED && consumeEnterReleased)
			{
				// consume the enter release key in find mode.
				consumeEnterReleased = false;
				e.consume();
				super.processKeyEvent(e);
			}
			else
			{
				super.processKeyEvent(e);
			}
		}
		finally
		{
			if (list instanceof LookupValueList)
			{
				((LookupValueList)list).setDoNotQuery(false);
			}
		}
	}

	/**
	 * 
	 */
	private void commitSelectedValue()
	{
		int index = jlist.getSelectedIndex();
		if (index >= 0)
		{
			Object real = dlm.getRealElementAt(index);

			Object display = real;
			if (dlm.getSize() != 0)
			{
				display = dlm.getElementAt(index);
			}
			if (list instanceof LookupValueList)
			{
				((LookupValueList)list).addRow(real, display);
			}
			Object currentValue = getValue();
			setValueObject(real);
			if (Utils.equalObjects(currentValue, real))
			{
				// for example you have entry "aabb" as current value, and "aab" in field resulting in the selection of "aabb" in list;
				// so in this case although the current value is the same as the one seleced in the list, the text in the field is not up to date;
				// commit selected value must make sure the text is updated as well in this case...
				AbstractFormatter formatter = getFormatter();
				if (formatter != null)
				{
					try
					{
						setText(formatter.valueToString(currentValue));
					}
					catch (ParseException e)
					{
						Debug.error("Cannot put back text for already selected value when commiting value from list in lookup field: ", e); //$NON-NLS-1$
					}
				}
			}
			if (editProvider != null)
			{
				editProvider.commitData();
			}
			popup.setVisible(false);
		}
	}

	private TimerTask task = null;

	protected void fillValueList(final boolean firstTime)
	{
		synchronized (this)
		{
			if (task != null)
			{
				task.cancel();
				task = null;
			}
		}
		if (firstTime)
		{
			fillValueListImpl(firstTime);
		}
		else
		{
			synchronized (this)
			{
				task = new TimerTask()
				{
					@Override
					public void run()
					{
						application.invokeLater(new Runnable()
						{
							public void run()
							{
								synchronized (DataLookupField.this)
								{
									task = null;
								}
								fillValueListImpl(firstTime);
							}
						});
					}
				};
				timer.schedule(task, 500);
			}
		}
	}


	/**
	 * @param firstTime
	 */
	private void fillValueListImpl(final boolean firstTime)
	{

		try
		{
			if (list != null && changeListener != null) list.removeListDataListener(changeListener);
			String txt = getText();
			if (txt.length() > 0 || Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY, Boolean.TRUE)))
			{
				dlm.fill(parentState, dataProviderID, txt, firstTime);

				if (jlist != null)
				{
					jlist.setModel(dlm);
					jlist.setSelectedValue(txt, true);
				}
				showPopup();
			}
			else if (popup != null)
			{
				popup.setVisible(false);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			if (list != null && changeListener != null) list.addListDataListener(changeListener);
		}
	}

	protected void searchValueList()
	{
		dlm.filter(getText());
		showPopup();
	}

	/*
	 * @see javax.swing.text.JTextComponent#selectAll()
	 */
	@Override
	public void selectAll()
	{
		if (!eventExecutor.getSelectOnEnter() && popup != null && popup.isVisible()) return;
		super.selectAll();
	}

	protected void showPopup()
	{
		if (!isEditable() || !hasFocus() || !isDisplayable() || dlm.getSize() == 0 || (dlm.getSize() == 1 && "".equals(dlm.getElementAt(0)))) //$NON-NLS-1$
		{
			if (popup != null && popup.isVisible())
			{
				popup.setVisible(false);
			}
			return;
		}

		final Window windowParent = SwingUtilities.getWindowAncestor(this);
		if (windowParent == null)
		{
			return;
		}
		if (popup != null && popup.getParent() != windowParent)
		{
			popup.dispose();
			popup = null;
		}

		if (popup == null)
		{
			popup = new JWindow(windowParent)
			{
				@Override
				public void setVisible(boolean b)
				{
					super.setVisible(b);

					if (windowParent instanceof ISupportVisibleChangeListener)
					{
						if (b) ((ISupportVisibleChangeListener)windowParent).addVisibleChangeListener(popupParentVisibleChangeListener);
						else ((ISupportVisibleChangeListener)windowParent).removeVisibleChangeListener(popupParentVisibleChangeListener);
					}
				}
			};
			popup.setFocusable(false);
			popup.getContentPane().setLayout(new BorderLayout());

			addAncestorListener(new AncestorListener()
			{

				public void ancestorAdded(AncestorEvent event)
				{ /* not used */
				}

				public void ancestorMoved(AncestorEvent event)
				{
					if (popup != null && popup.isVisible())
					{
						Rectangle visibleRect = new Rectangle();
						DataLookupField.this.computeVisibleRect(visibleRect);
						if (visibleRect.isEmpty())
						{
							popup.setVisible(false); // type-ahead probably scrolled outside of visible area
						}
						else
						{
							setPopupLocation();
						}
					}
				}

				public void ancestorRemoved(AncestorEvent event)
				{ /* not used */
				}

			});
			popup.addComponentListener(new ComponentAdapter()
			{
				@Override
				public void componentResized(ComponentEvent e)
				{
					if (popup != null && popup.isVisible())
					{
						setPopupLocation();
					}
				}
			});

			if (jlist == null)
			{
				jlist = new JList(dlm);
				jlist.setCellRenderer(new FixedListCellRenderer());
				jlist.addMouseListener(new ListMouseListener());

				jlist.setFocusable(false);
				jlist.addListSelectionListener(new ListSelectionListener()
				{
					public void valueChanged(ListSelectionEvent e)
					{
						jlist.ensureIndexIsVisible(jlist.getSelectedIndex());
					}
				});
				jlist.setSelectedValue(getText(), true);
			}
			scroller = new JScrollPane(jlist, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroller.setFocusable(false);
			scroller.getVerticalScrollBar().setFocusable(false);

			popup.getContentPane().add(scroller, BorderLayout.CENTER);
		}

		int popupHeight = (int)jlist.getPreferredSize().getHeight() + scroller.getInsets().bottom + scroller.getInsets().top;
		int popupWidth = (int)jlist.getPreferredSize().getWidth() + scroller.getInsets().right + scroller.getInsets().left;
		Border viewportBorder = scroller.getViewportBorder();
		if (viewportBorder != null)
		{
			Insets viewportBorderInsets = viewportBorder.getBorderInsets(this.scroller);
			popupWidth += viewportBorderInsets.left + viewportBorderInsets.right;
			popupHeight += viewportBorderInsets.top + viewportBorderInsets.bottom;
		}
		boolean heightLimited = false;
		if (popupHeight > MAX_POPUP_HEIGHT)
		{
			// height was limited => we will have vertical scrollbar; make with larger so that we avoid horiz. scroll bar if possible
			popupHeight = MAX_POPUP_HEIGHT;
			popupWidth += 25;
			heightLimited = true;
		}
		int fieldWidth = this.getWidth();
		if (popupWidth > fieldWidth)
		{
			if (popupWidth > MAX_POPUP_WIDTH)
			{
				popupWidth = MAX_POPUP_WIDTH;
				// width limited - so we will have horiz. scrollbar; if we have more space to grow in height, do so to avoid vertical scrollbar
				if (!heightLimited && (popupHeight + 25) <= MAX_POPUP_HEIGHT)
				{
					popupHeight += 25;
				}
			}
		}
		else
		{
			popupWidth = fieldWidth;
		}
		scroller.setPreferredSize(new Dimension(popupWidth, popupHeight));

		popup.pack();

		if (dlm.getSize() > 0 && jlist.getSelectedIndex() < 0)
		{
			jlist.setSelectedIndex(0);
		}
		if (!popup.isVisible() && this.getParent() != null)
		{
			setPopupLocation();
			popup.setVisible(true);
		}
	}

	private void setPopupLocation()
	{
		Point p = this.getLocation();
		SwingUtilities.convertPointToScreen(p, this.getParent());
		Window window = SwingUtilities.getWindowAncestor(this);
		Rectangle screenBounds = window.getGraphicsConfiguration().getBounds();
		p.y = p.y + this.getHeight();
		if (screenBounds != null)
		{
			// decide based on screen (try to make pop-up fit inside screen)
			if (((p.y + popup.getHeight()) > (screenBounds.getY() + screenBounds.getHeight())) &&
				((p.y - popup.getHeight() - this.getHeight()) >= screenBounds.getY()))
			{
				p.y = p.y - popup.getHeight() - this.getHeight();
			}
			if (((p.x + popup.getWidth()) > (screenBounds.getX() + screenBounds.getWidth())) &&
				((p.x - popup.getWidth() + this.getWidth()) >= screenBounds.getX()))
			{
				p.x = p.x - popup.getWidth() + this.getWidth();
			}
		}
		else
		{
			// decide based on window (try to make pop-up fit inside window) - this is just a back-up case that shouldn't happen
			if (((p.y + popup.getHeight()) > (window.getHeight() + window.getY())) && ((p.y - popup.getHeight() - this.getHeight()) >= window.getY()))
			{
				p.y = p.y - popup.getHeight() - this.getHeight();
			}
			if (((p.x + popup.getWidth()) > (window.getWidth() + window.getX())) && ((p.x - popup.getWidth() + this.getWidth()) >= window.getX()))
			{
				p.x = p.x - popup.getWidth() + this.getWidth();
			}
		}

		popup.setLocation(p);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#setFoundSet(com.servoy.j2db.dataprocessing.IRecord, com.servoy.j2db.dataprocessing.IFoundSet,
	 * boolean)
	 */
	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		if (this.parentState == parentState) return;
		dependencyChanged(parentState);
	}

	public void dependencyChanged(IRecordInternal record)
	{
		this.parentState = record;
		if (list instanceof LookupValueList || list instanceof GlobalMethodValueList)
		{
			Object o = getValue();

			int index = -1;
			if (!dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				index = dataProviderID.lastIndexOf('.');
			}
			if (index == -1 || parentState == null)
			{
				list.fill(parentState);
			}
			else
			{
				IFoundSetInternal relatedFoundSet = parentState.getRelatedFoundSet(dataProviderID.substring(0, index));
				if (relatedFoundSet == null || relatedFoundSet.getSize() == 0)
				{
					list.fill(null);
				}
				else
				{
					IRecordInternal relatedRecord = relatedFoundSet.getRecord(relatedFoundSet.getSelectedIndex());
					list.fill(relatedRecord);
				}
			}

			if (editProvider != null)
			{
				editProvider.setAdjusting(true);
			}
			try
			{
				setValue(null);
				setValue(o);
			}
			finally
			{
				if (editProvider != null)
				{
					editProvider.setAdjusting(false);
				}
			}
		}
	}

	public String getSelectedRelationName()
	{
		if (relationName == null && list != null)
		{
			relationName = list.getRelationName();
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

	/**
	 * @see com.servoy.j2db.smart.dataui.DataField#setValidationEnabled(boolean)
	 */
	@Override
	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;

		if (list != null && list.getFallbackValueList() != null)
		{
			IValueList vlist = list;
			if (!b)
			{
				vlist = list.getFallbackValueList();
			}
			if (vlist instanceof CustomValueList)
			{
				dlm = new LookupListModel(application, ((CustomValueList)vlist));
			}
			else
			{
				dlm = new LookupListModel(application, ((LookupValueList)vlist));
			}
			if (jlist != null)
			{
				jlist.setModel(dlm);
			}
		}
		try
		{
			focusGainedOrValidationChange = true;
			eventExecutor.setValidationEnabled(b);
			consumeEnterReleased = false;
			boolean prevEditState = editState;
			if (b)
			{
				setEditable(wasEditable);
				if (editProvider != null)
				{
					editProvider.setAdjusting(true);
				}
				try
				{
					setValue(null);//prevent errors
				}
				finally
				{
					if (editProvider != null)
					{
						editProvider.setAdjusting(false);
					}
				}
			}
			else
			{
				wasEditable = isEditable();
				setEditable(true);//allow search
			}
			editState = prevEditState;
		}
		finally
		{
			focusGainedOrValidationChange = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#getDefaultSort()
	 */
	public List<SortColumn> getDefaultSort()
	{
		return dlm.getDefaultSort();
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#deregister()
	 */
	@Override
	public void destroy()
	{
		super.destroy();
		if (popup != null)
		{
			popup.dispose();
			popup = null;
			jlist = null;
		}
		if (list != null && changeListener != null) list.removeListDataListener(changeListener);
	}

	/**
	 * @author jcompagner
	 * 
	 */
	private final class ChangeListener implements ListDataListener
	{
		void changed()
		{
			Object value = getValue();
			if (value != null)
			{
				boolean needEntireState = needEntireState();
				setNeedEntireState(false);
				setValueObject(null);
				setValueObject(value);
				setNeedEntireState(needEntireState);
			}
		}

		public void intervalAdded(ListDataEvent e)
		{
			changed();
		}

		public void intervalRemoved(ListDataEvent e)
		{
			changed();
		}

		public void contentsChanged(ListDataEvent e)
		{
			changed();
		}
	}

	private class HidePopup implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (popup != null)
			{
				popup.setVisible(!popup.isVisible());
			}
		}
	}

	private class LookupDocumentListener implements DocumentListener
	{
		public void insertUpdate(DocumentEvent e)
		{
			if (focusGainedOrValidationChange || skipPropertyChange) return;
			if (dlm != null && !dlm.hasMoreRows() && getText().length() != 1 && (e.getOffset() + e.getLength()) == e.getDocument().getLength())
			{
				if (editProvider == null || !editProvider.isAdjusting())
				{
					searchValueList();
				}
			}
			else
			{
				if (editProvider == null || !editProvider.isAdjusting())
				{
					fillValueList(false);
				}
			}
		}

		public void removeUpdate(DocumentEvent e)
		{
			if (focusGainedOrValidationChange || skipPropertyChange) return;
			if (editProvider == null || !editProvider.isAdjusting())
			{
				fillValueList(false);
			}
		}

		public void changedUpdate(DocumentEvent e)
		{
			if (focusGainedOrValidationChange || skipPropertyChange) return;
			if (editProvider == null || !editProvider.isAdjusting())
			{
				fillValueList(false);
			}
		}
	}

	private class ListMouseListener extends MouseAdapter
	{
		@Override
		public void mouseReleased(MouseEvent e)
		{
			commitSelectedValue();
		}
	}
	private class FixedListCellRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList lst, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			if (value instanceof String && ((String)value).trim().length() == 0)
			{
				setText("A"); //$NON-NLS-1$
				Dimension size = getPreferredSize();
				setText(""); //$NON-NLS-1$
				setPreferredSize(size);
			}
			else
			{
				setPreferredSize(null); // let UI decide
			}
			return super.getListCellRendererComponent(lst, value, index, isSelected, cellHasFocus);
		}
	}


}
