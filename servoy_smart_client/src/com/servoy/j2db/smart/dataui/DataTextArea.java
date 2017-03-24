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


import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Keymap;

import org.jdesktop.xswingx.PromptSupport;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.dnd.FormDataTransferHandler;
import com.servoy.j2db.dnd.ISupportDragNDropTextTransfer;
import com.servoy.j2db.printing.IFixedPreferredWidth;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEditProvider;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportPlaceholderText;
import com.servoy.j2db.ui.scripting.RuntimeTextArea;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;

/**
 * Runtime swing component which makes a text area
 * @author jblok
 */
public class DataTextArea extends EnableScrollPanel
	implements IDisplayData, IFieldComponent, IScrollPane, IFixedPreferredWidth, ISupplyFocusChildren<Component>, ISupportCachedLocationAndSize,
	ISupportDragNDropTextTransfer, ISupportEditProvider, ISupportPlaceholderText, ISupportOnRender
{
	private final JTextArea enclosedComponent;
	private String dataProviderID;
	private final Document plainDocument;
	private Document editorDocument;
	private final EventExecutor eventExecutor;
	private final IApplication application;
	private MouseAdapter rightclickMouseAdapter = null;
	private final RuntimeTextArea scriptable;

	public DataTextArea(IApplication app, RuntimeTextArea scriptable)
	{
		super();
		application = app;
		getViewport().setView(new MyTextArea());
		enclosedComponent = (JTextArea)getViewport().getView();
		eventExecutor = new EventExecutor(this, enclosedComponent)
		{
			@Override
			public void fireLeaveCommands(Object display, boolean focusEvent, int modifiers)
			{
				if (hasLeaveCmds())
				{
					editProvider.focusLost(new FocusEvent(DataTextArea.this, FocusEvent.FOCUS_LOST));
				}

				super.fireLeaveCommands(display, focusEvent, modifiers);
			}
		};
		enclosedComponent.addKeyListener(eventExecutor);

		plainDocument = editorDocument = enclosedComponent.getDocument();
		enclosedComponent.setBorder(BorderFactory.createEmptyBorder());

		enclosedComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
			Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
		enclosedComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));

		Keymap keymap = enclosedComponent.getKeymap();
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e)
			{
				((JTextArea)e.getSource()).replaceSelection("\t"); //$NON-NLS-1$
			}
		});
		enclosedComponent.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					JTextArea ta = (JTextArea)e.getSource();
					if (ta.hasFocus() && ta.isEditable() && ta.isEnabled() && !Utils.equalObjects(previousValue, ta.getText()))
					{
						e.consume();
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					JTextArea ta = (JTextArea)e.getSource();
					if (ta.hasFocus() && ta.isEditable() && ta.isEnabled() && !Utils.equalObjects(previousValue, ta.getText()))
					{
						ta.setText(previousValue);
						e.consume();
					}
				}
			}
		});
		this.scriptable = scriptable;
		scriptable.setTextComponent(enclosedComponent);
	}

	public final RuntimeTextArea getScriptObject()
	{
		return scriptable;
	}

	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
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
		return isValueValid;
	}

	private boolean isValueValid = true;
	private Object previousValidValue;

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
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
			previousValidValue = null;
		}
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		if (previousValidValue != null)
		{
			oldVal = previousValidValue;
		}

		previousValue = (newVal == null ? null : newVal.toString());

		eventExecutor.fireChangeCommand(oldVal, newVal, false, this);

		// if change cmd is not succeeded also don't call action cmd?
		if (isValueValid)
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
						eventExecutor.fireRightclickCommand(true, DataTextArea.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			enclosedComponent.addMouseListener(rightclickMouseAdapter);
			addMouseListener(rightclickMouseAdapter);
		}
	}

	private boolean wasEditable;

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && ScopesUtils.isVariableScope(dataProviderID)) return;

		eventExecutor.setValidationEnabled(b);

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
			enclosedComponent.setDocument(editorDocument);
			previousValue = enclosedComponent.getText();
//			Commented out this part because, when executing search with a button action - with the mouse
//			it would not show any contents in the field afterwards (it doesn't show the content of the found records).
//			try
//			{
//				plainDocument.remove(0, plainDocument.getLength());
//			}
//			catch (BadLocationException e)
//			{
//				Debug.error(e);
//			}
		}
		else
		{
			wasEditable = enclosedComponent.isEditable();
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
			// keep the old text
			String text = enclosedComponent.getText();
			enclosedComponent.setDocument(plainDocument);
			enclosedComponent.setText(text);
		}
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	@Override
	public void setName(String name)
	{
		super.setName(name);
		enclosedComponent.setName(name);
	}

	public void setMaxLength(int i)
	{
		enclosedComponent.setDocument(editorDocument = new ValidatingDocument(new LengthDocumentValidator(i)));
	}

	public Document getDocument()
	{
		return enclosedComponent.getDocument();
	}

	public void setMargin(Insets m)
	{
//		enclosedComponent.setMargin(i); seems to have no effect
		enclosedComponent.setBorder(
			BorderFactory.createCompoundBorder(enclosedComponent.getBorder(), BorderFactory.createEmptyBorder(m.top, m.left, m.bottom, m.right)));
	}

	public Insets getMargin()
	{
		return null;
	}

	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		if (enclosedComponent != null) enclosedComponent.setFont(f);
	}

	@Override
	public Font getFont()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getFont();
		}
		return super.getFont();
	}

	@Override
	public void setHorizontalScrollBarPolicy(int policy)
	{
		if (enclosedComponent != null)
		{
			if (policy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS || policy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
			{
				enclosedComponent.setLineWrap(false);
				enclosedComponent.setWrapStyleWord(false);
			}
			else
			{
				enclosedComponent.setLineWrap(true);
				enclosedComponent.setWrapStyleWord(true);
			}
		}
		super.setHorizontalScrollBarPolicy(policy);
	}

	public void setHorizontalAlignment(int a)
	{
		//not supported by textarea
	}

	private boolean isInsertMode;

	public boolean getOverwriteMode()
	{
		return !isInsertMode;
	}


	public class MyTextArea extends JTextArea implements ISkinnable, ISupportDragNDropTextTransfer
	{
		private final Caret defaultCaret;
		private final Caret overtypeCaret;

		MyTextArea()
		{
			super();
			setDragEnabledEx(true);
			defaultCaret = getCaret();
			overtypeCaret = new OvertypeCaret();
			overtypeCaret.setBlinkRate(defaultCaret.getBlinkRate());
			setInsertMode(true);

		}

		/*
		 * Return the insert/overtype mode
		 */
		public boolean isInsertMode()
		{
			return isInsertMode;
		}

		/*
		 * Set the caret to use depending on insert/overtype mode
		 */
		public void setInsertMode(boolean isInsertMode)
		{
			DataTextArea.this.isInsertMode = isInsertMode;
			int pos = getCaretPosition();

			if (isInsertMode())
			{
				setCaret(defaultCaret);
			}
			else
			{
				setCaret(overtypeCaret);
			}

			setCaretPosition(pos);
		}

		/*
		 * Override method from JComponent
		 */
		@Override
		public void replaceSelection(String text)
		{
			//  Implement overtype mode by selecting the character at the current
			//  caret position

			if (!isInsertMode())
			{
				int pos = getCaretPosition();

				if (getSelectedText() == null && pos < getDocument().getLength())
				{
					moveCaretPosition(pos + 1);
				}
			}

			super.replaceSelection(text);
		}

		/*
		 * Override method from JComponent
		 */
		@Override
		protected void processKeyEvent(KeyEvent e)
		{

			super.processKeyEvent(e);

			//  Handle release of Insert key to toggle insert/overtype mode

			if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_INSERT)
			{
				setInsertMode(!isInsertMode());
				((ISmartClientApplication)application).updateInsertModeIcon(DataTextArea.this);
			}
		}

		// MAC FIX
		@Override
		public Insets getInsets()
		{
			Insets insets = super.getInsets();
			if (insets == null)
			{
				insets = new Insets(0, 0, 0, 0);
			}
			return insets;
		}

		// MAC FIX
		@Override
		public Insets getMargin()
		{
			Insets insets = super.getMargin();
			if (insets == null)
			{
				insets = new Insets(0, 0, 0, 0);
			}
			return insets;
		}

		private void setDragEnabledEx(boolean b)
		{
			try
			{
				Method m = getClass().getMethod("setDragEnabled", new Class[] { boolean.class }); //$NON-NLS-1$
				m.invoke(this, new Object[] { new Boolean(b) });
			}
			catch (Exception e)
			{
//				Debug.trace(e);//is intenionaly trace, becouse fails before 1.4
			}
		}

		@Override
		public void setUI(ComponentUI ui)
		{
			super.setUI(ui);
		}

		@Override
		public void setText(String t)
		{
			super.setText(t);
			setCaretPosition(0);
		}

		//fix for incorrect clipping
		@Override
		public void print(Graphics g)
		{
			Shape saveClip = g.getClip();
			try
			{
				int w = DataTextArea.this.getWidth();
				w -= DataTextArea.this.getInsets().left + DataTextArea.this.getInsets().right;
				int h = DataTextArea.this.getHeight();
				h -= DataTextArea.this.getInsets().top + DataTextArea.this.getInsets().bottom;
				if (saveClip != null)
				{
					g.setClip(saveClip.getBounds().intersection(new Rectangle(0, 0, w, h)));
				}
				else
				{
					g.setClip(0, 0, w, h);
				}
				super.print(g);
			}
			finally
			{
				g.setClip(saveClip);
			}
		}

		@Override
		public void copy()
		{
			if (textTransferHandler != null)
			{
				Action copyAction = FormDataTransferHandler.getCopyFormDataAction();
				copyAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)copyAction.getValue(Action.NAME),
					EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
			}
			else super.copy();
		}

		@Override
		public void cut()
		{
			if (textTransferHandler != null && isEditable() && isEnabled())
			{
				Action cutAction = FormDataTransferHandler.getCutFormDataAction();
				cutAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)cutAction.getValue(Action.NAME),
					EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
			}
			else super.cut();
		}

		@Override
		public void paste()
		{
			if (textTransferHandler != null && isEditable() && isEnabled())
			{
				Action pasteAction = FormDataTransferHandler.getPasteFormDataAction();
				pasteAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)pasteAction.getValue(Action.NAME),
					EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
			}
			else super.paste();
		}

		private int getCurrentEventModifiers()
		{
			int modifiers = 0;
			AWTEvent currentEvent = EventQueue.getCurrentEvent();
			if (currentEvent instanceof InputEvent)
			{
				modifiers = ((InputEvent)currentEvent).getModifiers();
			}
			else if (currentEvent instanceof ActionEvent)
			{
				modifiers = ((ActionEvent)currentEvent).getModifiers();
			}
			return modifiers;
		}

		private TransferHandler textTransferHandler;

		/*
		 * @see com.servoy.j2db.dnd.ISupportDragNDropTextTransfer#clearTransferHandler()
		 */
		public void clearTransferHandler()
		{
			textTransferHandler = getTransferHandler();
			setTransferHandler(null);
		}

		/*
		 * @see com.servoy.j2db.dnd.ISupportDragNDropTextTransfer#getTextTransferHandler()
		 */
		public TransferHandler getTextTransferHandler()
		{
			return textTransferHandler;
		}
	}

	/*
	 * @see IDisplayData#getValue()
	 */
	public Object getValueObject()
	{
		previousValue = enclosedComponent.getText();
		return previousValue;
	}

	@Override
	public void addFocusListener(FocusListener fl)
	{
		if (enclosedComponent != null) enclosedComponent.addFocusListener(fl);
	}

	@Override
	public void removeFocusListener(FocusListener fl)
	{
		if (enclosedComponent != null) enclosedComponent.removeFocusListener(fl);
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


	private String previousValue;

	private void setValueEx(String txt)
	{
		if (!Utils.equalObjects(previousValue, txt))
		{
			previousValue = txt;
			enclosedComponent.setText(txt);
//			if ((enclosedComponent.getWidth() == 0 || enclosedComponent.getHeight() == 0) && !"".equals(txt))
//			{
//				Dimension thisSize = getSize();
//				enclosedComponent.setSize(thisSize);
//				Dimension d = enclosedComponent.getPreferredSize();
//				enclosedComponent.setSize(d);
//				enclosedComponent.invalidate();
//				validate();
//				doLayout();
//			}
		}
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/*
	 * @see IDisplayData#setValue(Object)
	 */
	public void setValueObject(Object obj)
	{
		try
		{
			if (editProvider != null) editProvider.setAdjusting(true);
			if (needEntireState)
			{
				if (resolver != null)
				{
					setValueEx(Text.processTags(TagResolver.formatObject(obj != null ? obj : "", application), resolver)); //$NON-NLS-1$
					if (tooltip != null)
					{
						enclosedComponent.setToolTipText(Text.processTags(tooltip, resolver));
					}
				}
				else
				{
					setValueEx(""); //$NON-NLS-1$
					if (tooltip != null)
					{
						enclosedComponent.setToolTipText(null);
					}
				}
			}
			else
			{
				if (obj == null)
				{
					setValueEx(""); //$NON-NLS-1$
				}
				else
				{
					setValueEx(TagResolver.formatObject(obj, application));
				}
				if (tooltip != null)
				{
					enclosedComponent.setToolTipText(tooltip);
				}
			}
		}
		finally
		{
			if (editProvider != null) editProvider.setAdjusting(false);
		}
		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(enclosedComponent.hasFocus());
		}
	}

	/*
	 * @see IDisplayData#needFocusListner()
	 */
	public boolean needEditListener()
	{
		return true;
	}

	private EditProvider editProvider = null;

	public IEditProvider getEditProvider()
	{
		return editProvider;
	}

	private ArrayList<ILabel> labels;

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this, true);
			addFocusListener(editProvider);
			plainDocument.addDocumentListener(editProvider);
			editorDocument.addDocumentListener(editProvider);
			editProvider.addEditListener(l);
			editProvider.setEditable(enclosedComponent.isEditable());
			try
			{
				DropTarget dt = enclosedComponent.getDropTarget();
				if (dt != null) dt.addDropTargetListener(editProvider);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}


	/*
	 * @see IDisplayData#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	@Override
	public void setBackground(Color bg)
	{
		if (enclosedComponent != null) enclosedComponent.setBackground(bg);
		super.setBackground(bg);
	}

	@Override
	public Color getBackground()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getBackground();
		}
		return super.getBackground();
	}


	@Override
	public Color getForeground()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getForeground();
		}
		return super.getForeground();
	}

	@Override
	public void setForeground(Color fg)
	{
		if (enclosedComponent != null) enclosedComponent.setForeground(fg);
		super.setForeground(fg);
	}


	public void setComponentVisible(boolean b_visible)
	{
		if (viewable || !b_visible)
		{
			setVisible(b_visible);
		}
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

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}


	@Override
	public void setOpaque(boolean b)
	{
		if (enclosedComponent != null) enclosedComponent.setOpaque(b);
		getViewport().setOpaque(b);
		super.setOpaque(b);
	}


	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
					label.setComponentEnabled(b);
				}
			}
		}
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		if (!b) setComponentVisible(b);
		this.viewable = b;
	}

	public boolean isViewable()
	{
		return viewable;
	}

	public void setEditable(boolean b)
	{
		editState = b;
		enclosedComponent.setEditable(b);
		if (editProvider != null) editProvider.setEditable(b);
		if (b)
		{
			enclosedComponent.removeMouseListener(eventExecutor);
		}
		else
		{
			enclosedComponent.addMouseListener(eventExecutor);//listen when not editable
		}
	}

	public boolean isEditable()
	{
		return enclosedComponent.isEditable();
	}

	private boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !enclosedComponent.isEditable()) return;
		if (b)
		{
			setEditable(false);
			editState = true;
		}
		else
		{
			setEditable(editState);
		}
	}

	public boolean isReadOnly()
	{
		return !enclosedComponent.isEditable();
	}

	/*
	 * titleText---------------------------------------------------
	 */

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}


	private String tooltip;

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
				enclosedComponent.setToolTipText(tip);
			}
			else if (HtmlUtils.hasUsefulHtmlContent(tip))
			{
				enclosedComponent.setToolTipText(tip);
			}
		}
		else
		{
			enclosedComponent.setToolTipText(null);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
	 */
	@Override
	public String getToolTipText(MouseEvent event)
	{
		String txt = super.getToolTipText(event);
		if (txt == null || txt.length() == 0)
		{
			return enclosedComponent.getToolTipText(event);
		}
		return null;
	}


	public int getAbsoluteFormLocationY()
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

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	public void setCachedLocation(Point location)
	{
		this.cachedLocation = location;
	}

	public void setCachedSize(Dimension size)
	{
		this.cachedSize = size;
	}

	private Dimension cachedSize;


	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	// If component not shown or not added yet
	// and request focus is called it should wait for the component
	// to be created.
	boolean wantFocus = false;

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (wantFocus)
		{
			wantFocus = false;
			enclosedComponent.requestFocus();
		}
	}

	public void requestFocusToComponent()
	{
//		if (!enclosedComponent.hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (isDisplayable())
			{
				// Must do it in a runnable or else others after a script can get focus first again..
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						enclosedComponent.requestFocus();
					}
				});
			}
			else
			{
				wantFocus = true;
			}
		}
	}

	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (editProvider != null) editProvider.forceCommit();

		if (!isValueValid)
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

		if (looseFocus && eventExecutor.mustFireFocusLostCommand())
		{
			eventExecutor.skipNextFocusLost();
			eventExecutor.fireLeaveCommands(this, false, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
		return true;
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	@Override
	protected void printChildren(Graphics g)
	{
		super.printChildren(g);
		super.printBorder(g); // print border after children print, to prevent child backgrounds ontop of border
	}

	@Override
	protected void printBorder(Graphics g)
	{
		//intentionally empty to have the border drawn after the content
	}

	private int preferredWidth = -1;

	public void setPreferredWidth(int preferredWidth)
	{
		this.preferredWidth = preferredWidth;
	}

	@Override
	public Dimension getPreferredSize()
	{
		if (preferredWidth < 0)
		{
			// normal behavior (used when not in print preview... and normally in print preview)
			// preview and printing should behave the same
			if (application.getModeManager().getMode() == IModeManager.PREVIEW_MODE || Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")))
			{
				// in print preview, the preferred size of the text area must calculate it's width as well;
				// this happens only if line wrap is false
				boolean lineWrap = enclosedComponent.getLineWrap();
				int height = 0;
				if (lineWrap)
				{
					height = super.getPreferredSize().height;
					enclosedComponent.setLineWrap(false);
				}
				Dimension d = super.getPreferredSize();
				if (lineWrap)
				{
					d.height = height;
					enclosedComponent.setLineWrap(true);
				}
				return d;
			}
			return super.getPreferredSize();
		}
		else
		{
			// in print preview, with grow horizontal + shrink or grow vertical, this is what happens
			Dimension preferredSize = new Dimension();
			preferredSize.width = preferredWidth;

			// compute preferred height for the given width (preferredWidth)
			Insets insets = getInsets();
			Dimension oldSize = enclosedComponent.getSize();
			enclosedComponent.setSize(preferredWidth - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
			boolean lineWrap = enclosedComponent.getLineWrap(); // if line wrap is true, the preferred size reported
			// by a JTextArea only calculates the height and uses the current component width
			if (!lineWrap) enclosedComponent.setLineWrap(true);
			preferredSize.height = super.getPreferredSize().height;
			if (!lineWrap) enclosedComponent.setLineWrap(false);
			enclosedComponent.setSize(oldSize);

			return preferredSize;
		}
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { enclosedComponent };
	}

	/*
	 * @see com.servoy.j2db.dnd.ISupportTextTransfer#clearTransferHandler()
	 */
	public void clearTransferHandler()
	{
		setTransferHandler(null);
		if (enclosedComponent instanceof ISupportDragNDropTextTransfer) ((ISupportDragNDropTextTransfer)enclosedComponent).clearTransferHandler();
	}

	/*
	 * @see com.servoy.j2db.dnd.ISupportTextTransfer#getTextTransferHandler()
	 */
	public TransferHandler getTextTransferHandler()
	{
		return enclosedComponent instanceof ISupportDragNDropTextTransfer ? ((ISupportDragNDropTextTransfer)enclosedComponent).getTextTransferHandler() : null;
	}

	@Override
	public void setPlaceholderText(String text)
	{
		PromptSupport.uninstall(enclosedComponent);
		PromptSupport.setPrompt(application.getI18NMessageIfPrefixed(text), enclosedComponent);
		repaint();
	}

	@Override
	public void requestFocus()
	{
		// always focus the inner component
		enclosedComponent.requestFocus();
	}
}
