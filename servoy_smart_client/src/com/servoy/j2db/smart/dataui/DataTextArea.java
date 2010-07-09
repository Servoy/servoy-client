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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Keymap;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.printing.IFixedPreferredWidth;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptTextAreaMethods;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.LengthDocumentValidator;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.ValidatingDocument;

/**
 * Runtime swing component which makes a text area
 * @author jblok
 */
public class DataTextArea extends EnableScrollPanel implements IDisplayData, IFieldComponent, IScrollPane, IScriptTextAreaMethods, IFixedPreferredWidth,
	ISupplyFocusChildren<Component>, ISupportCachedLocationAndSize
{
	private final JTextArea enclosedComponent;
	private String dataProviderID;
	private final Document plainDocument;
	private Document editorDocument;
	private final EventExecutor eventExecutor;
	private final IApplication application;
	private MouseAdapter rightclickMouseAdapter = null;

	public DataTextArea(IApplication app)
	{
		super();
		application = app;
		getViewport().setView(new MyTextArea());
		enclosedComponent = (JTextArea)getViewport().getView();
		eventExecutor = new EventExecutor(this, enclosedComponent);
		enclosedComponent.addKeyListener(eventExecutor);
		enclosedComponent.addMouseListener(eventExecutor);

		plainDocument = editorDocument = enclosedComponent.getDocument();
		enclosedComponent.setBorder(BorderFactory.createEmptyBorder());

		TreeSet<KeyStroke> set = new TreeSet<KeyStroke>();
		set.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
		enclosedComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
		set = new TreeSet<KeyStroke>();
		set.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		enclosedComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);

		Keymap keymap = enclosedComponent.getKeymap();
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e)
			{
				((JTextArea)e.getSource()).replaceSelection("\t"); //$NON-NLS-1$
			}
		});

		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e)
			{
				((JTextArea)e.getSource()).setText(previousValue);
			}
		});
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
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;

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
			setEditable(true);//allow search
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
		enclosedComponent.setBorder(BorderFactory.createCompoundBorder(enclosedComponent.getBorder(), BorderFactory.createEmptyBorder(m.top, m.left, m.bottom,
			m.right)));
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

	public int getDataType()
	{
		return dataType;
	}

	private int dataType;


	private String format;

	public void setFormat(int dataType, String format)
	{
		this.dataType = dataType;
		this.format = format;
	}

	public String getFormat()
	{
		return format;
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


	public class MyTextArea extends JTextArea implements ISkinnable
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
				application.updateInsertMode(DataTextArea.this);
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
					setValueEx(Text.processTags(TagResolver.formatObject(obj != null ? obj : "", application.getSettings()), resolver)); //$NON-NLS-1$
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
					setValueEx(TagResolver.formatObject(obj, application.getSettings()));
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
	}

	/*
	 * @see IDisplayData#needFocusListner()
	 */
	public boolean needEditListner()
	{
		return true;
	}

	private EditProvider editProvider = null;
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


	/*
	 * caret---------------------------------------------------
	 */
	public int js_getCaretPosition()
	{
		if (enclosedComponent == null) return 0;
		return enclosedComponent.getCaretPosition();
	}

	public void js_setCaretPosition(int pos)
	{
		if (enclosedComponent == null) return;
		if (pos < 0) enclosedComponent.setCaretPosition(0);
		else if (pos > getDocument().getLength()) enclosedComponent.setCaretPosition(getDocument().getLength());
		else enclosedComponent.setCaretPosition(pos);
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
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


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(enclosedComponent.getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
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


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	/*
	 * scroll---------------------------------------------------
	 */
	public void js_setScroll(int x, int y)
	{
		enclosedComponent.scrollRectToVisible(new Rectangle(x, y, getWidth(), getHeight()));
	}

	public int js_getScrollX()
	{
		return enclosedComponent.getVisibleRect().x;
	}

	public int js_getScrollY()
	{
		return enclosedComponent.getVisibleRect().y;
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
			ArrayList<String> al = new ArrayList<String>(labels.size());
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

	@Override
	public void setOpaque(boolean b)
	{
		if (enclosedComponent != null) enclosedComponent.setOpaque(b);
		getViewport().setOpaque(b);
		super.setOpaque(b);
	}


	/*
	 * enabled---------------------------------------------------
	 */
	public void js_setEnabled(final boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
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

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}


	/*
	 * editable---------------------------------------------------
	 */
	@Deprecated
	public boolean js_isEditable()
	{
		return enclosedComponent.isEditable();
	}

	@Deprecated
	public void js_setEditable(boolean b)
	{
		setEditable(b);
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


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	private boolean editState;

	public void js_setReadOnly(boolean b)
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
		this.validate();
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
		this.validate();
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


	/*
	 * textselect---------------------------------------------------
	 */
	public String js_getSelectedText()
	{
		return enclosedComponent.getSelectedText();
	}

	public void js_selectAll()
	{
		enclosedComponent.selectAll();
	}

	public void js_replaceSelectedText(String s)
	{
		if (editProvider != null) editProvider.startEdit();
		enclosedComponent.replaceSelection(s);
		if (editProvider != null) editProvider.commitData();

	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}

	public String js_getElementType()
	{
		return "TEXT_AREA"; //$NON-NLS-1$
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
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

	public void js_requestFocus(Object[] vargs)
	{
//		if (!enclosedComponent.hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
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
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		if (eventExecutor.mustFireFocusLostCommand())
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
			if (application.getModeManager().getMode() == IModeManager.PREVIEW_MODE)
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
}
