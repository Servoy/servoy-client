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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import javax.swing.TransferHandler;

import org.jdesktop.xswingx.PromptSupport;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.dnd.FormDataTransferHandler;
import com.servoy.j2db.dnd.ISupportDragNDropTextTransfer;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportPlaceholderText;
import com.servoy.j2db.ui.scripting.RuntimeDataPassword;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;

/**
 * Runtime swing password field
 * @author jblok
 */
public class DataPassword extends JPasswordField
	implements IFieldComponent, IDisplayData, ISupportCachedLocationAndSize, ISupportDragNDropTextTransfer, ISupportPlaceholderText, ISupportOnRender
{
	private String dataProviderID;
	private final EventExecutor eventExecutor;
	private final IApplication application;
	private MouseAdapter rightclickMouseAdapter = null;
	private final RuntimeDataPassword scriptable;

	public DataPassword(IApplication app, RuntimeDataPassword scriptable)
	{
		super();
		this.scriptable = scriptable;
		application = app;
		eventExecutor = new EventExecutor(this)
		{
			@Override
			public void fireLeaveCommands(Object display, boolean focusEvent, int modifiers)
			{
				if (hasLeaveCmds())
				{
					editProvider.focusLost(new FocusEvent(DataPassword.this, FocusEvent.FOCUS_LOST));
				}

				super.fireLeaveCommands(display, focusEvent, modifiers);
			}
		};

		addKeyListener(new KeyAdapter()
		{
			private boolean enterKeyPressed = false;

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					enterKeyPressed = true;
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !Utils.equalObjects(previousValue, getValueObject()))
				{
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER && enterKeyPressed)
				{
					enterKeyPressed = false;
					eventExecutor.actionPerformed(e.getModifiers());
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !Utils.equalObjects(previousValue, getValueObject()))
				{
					JPasswordField field = (JPasswordField)e.getSource();
					field.setText((String)previousValue);
					e.consume();
				}
			}
		});
		addMouseListener(eventExecutor);
		addKeyListener(eventExecutor);
	}

	public final RuntimeDataPassword getScriptObject()
	{
		return scriptable;
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
						eventExecutor.fireRightclickCommand(true, DataPassword.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(rightclickMouseAdapter);
		}
	}

	private boolean wasEditable;

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;

		boolean prevEditState = editState;
		eventExecutor.setValidationEnabled(b);
		if (b)
		{
			setEditable(wasEditable);
		}
		else
		{
			wasEditable = isEditable();
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
		}
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
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
		eventExecutor.fireChangeCommand(previousValidValue != null ? previousValidValue : oldVal, newVal, false, this);
	}

	//_____________________________________________________________


	private Object previousValue;

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object o)
	{
		try
		{
			if (editProvider != null) editProvider.setAdjusting(true);

			if (needEntireState)
			{
				if (resolver != null)
				{
					if (tooltip != null)
					{
						super.setToolTipText(Text.processTags(tooltip, resolver));
					}
				}
				else
				{
					if (tooltip != null)
					{
						super.setToolTipText(null);
					}
				}
			}
			if (!Utils.equalObjects(previousValue, o))
			{
				previousValue = o;
				if (o != null)
				{
					setText(TagResolver.formatObject(o, application));
				}
				else
				{
					setText(""); //$NON-NLS-1$
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
			scriptable.getRenderEventExecutor().fireOnRender(hasFocus());
		}
	}

	public Object getValueObject()
	{
		return new String(getPassword());
	}

	public boolean needEditListener()
	{
		return true;
	}

	private EditProvider editProvider = null;

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this, true);
			addFocusListener(editProvider);
			getDocument().addDocumentListener(editProvider);
			addActionListener(editProvider);
			editProvider.addEditListener(l);
			editProvider.setEditable(isEditable());
		}
	}

	@Override
	public void setEditable(boolean b)
	{
		editState = b;
		super.setEditable(b);
		if (editProvider != null) editProvider.setEditable(b);
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	private boolean needEntireState;
	private List<ILabel> labels;

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int i)
	{
		setDocument(new ValidatingDocument(new LengthDocumentValidator(i)));
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
			for (ILabel label : labels)
			{
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

	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			if (labels != null)
			{
				for (ILabel label : labels)
				{
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

	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEditable();
	}

	private boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !isEditable()) return;
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
		return titleText;
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

	public void requestFocusToComponent()
	{
//		if (!hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
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
			requestFocus();
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

	@Override
	public void setMargin(Insets m)
	{
		//super.setMargin(m);
		setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(m.top, m.left, m.bottom, m.right)));
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	private TransferHandler textTransferHandler;

	/*
	 * @see com.servoy.j2db.dnd.ISupportTextTransfer#clearTransferHandler()
	 */
	public void clearTransferHandler()
	{
		textTransferHandler = getTransferHandler();
		setTransferHandler(null);
	}

	public TransferHandler getTextTransferHandler()
	{
		return textTransferHandler;
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

	@Override
	public void setPlaceholderText(String text)
	{
		PromptSupport.uninstall(this);
		PromptSupport.setPrompt(application.getI18NMessageIfPrefixed(text), this);
		repaint();
	}
}
