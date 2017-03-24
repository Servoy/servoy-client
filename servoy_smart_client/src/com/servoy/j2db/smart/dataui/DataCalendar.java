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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.text.Document;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.gui.JDateChooser;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.scripting.RuntimeDataCalendar;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Runtime swing calendar field
 * @author jblok
 */
public class DataCalendar extends EnablePanel implements IFieldComponent, IDisplayData, ActionListener, IDelegate, ISupplyFocusChildren<Component>,
	ISupportCachedLocationAndSize, IFormattingComponent, ISupportOnRender
{
	private final DataField enclosedComponent;
	private String dataProviderID;
	private final JButton showCal;
	private final IApplication application;
	private List<ILabel> labels;

	private MouseAdapter rightclickMouseAdapter = null;
	private final RuntimeDataCalendar scriptable;

	public DataCalendar(IApplication app, RuntimeDataCalendar scriptable)
	{
		this.application = app;
		setLayout(new BorderLayout());
		enclosedComponent = new DataField(app, scriptable);
		enclosedComponent.setIgnoreOnRender(true);
		enclosedComponent.setBorder(BorderFactory.createEmptyBorder());
		enclosedComponent.setOpaque(false);
		add(enclosedComponent, BorderLayout.CENTER);

		showCal = new AbstractScriptButton(app, null /* no scriptable */)
		{
			@Override
			public String toString() // super uses scriptable
			{
				return "show-calendar button for " + DataCalendar.this.toString();
			}
		};
		showCal.setText("..."); //$NON-NLS-1$
		showCal.addActionListener(this);
		showCal.setPreferredSize(new Dimension(20, 15));
		showCal.setRequestFocusEnabled(false);
		add(showCal, BorderLayout.EAST);

		setOpaque(true);
		setBackground(Color.white);
		setBorder(BorderFactory.createEtchedBorder());
		this.scriptable = scriptable;
	}

	public final RuntimeDataCalendar getScriptObject()
	{
		return this.scriptable;
	}

	public Object getDelegate()
	{
		return enclosedComponent;
	}

	public Document getDocument()
	{
		return enclosedComponent.getDocument();
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
		enclosedComponent.setName(name);
	}

	public void installFormat(ComponentFormat componentFormat)
	{
		// calendar field always works with dates (even if it is attached to a dataprovider of another type
		// - for example it could work with a text column that has a Date <-> String converter)
		ComponentFormat cf;
		if (componentFormat.parsedFormat.isEmpty())
		{
			// use default locale short date/time format
			cf = new ComponentFormat(FormatParser.parseFormatProperty(new SimpleDateFormat().toPattern()), IColumnTypes.DATETIME, componentFormat.uiType);
		}
		else
		{
			cf = new ComponentFormat(componentFormat.parsedFormat, IColumnTypes.DATETIME, componentFormat.uiType);
		}
		enclosedComponent.installFormat(cf);
	}

	public void setMargin(Insets i)
	{
		enclosedComponent.setMargin(i);
	}

	public Insets getMargin()
	{
		return enclosedComponent.getMargin();
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

	public void addScriptExecuter(IScriptExecuter el)
	{
		enclosedComponent.addScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return enclosedComponent != null ? enclosedComponent.getEventExecutor() : null;
	}

	public void setEnterCmds(String[] ids, Object[][] args)
	{
		enclosedComponent.setEnterCmds(ids, null);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		enclosedComponent.setLeaveCmds(ids, null);
	}

	public void setActionCmd(String id, Object[] args)
	{
		enclosedComponent.setActionCmd(id, args);
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		enclosedComponent.setRightClickCommand(rightClickCmd, args);
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
						((BaseEventExecutor)enclosedComponent.getEventExecutor()).fireRightclickCommand(true, enclosedComponent, e.getModifiers(),
							e.getPoint());
					}
				}
			};
			addMouseListener(rightclickMouseAdapter);
		}
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		enclosedComponent.notifyLastNewValueWasChange(oldVal, newVal);
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		enclosedComponent.setValueValid(valid, oldVal);
	}

	public boolean isValueValid()
	{
		return enclosedComponent.isValueValid();
	}

	public void setChangeCmd(String id, Object[] args)
	{
		enclosedComponent.setChangeCmd(id, args);
	}

	public void setHorizontalAlignment(int a)
	{
		enclosedComponent.setHorizontalAlignment(a);
	}

	public void setMaxLength(int i)
	{
		//ignore
	}

	public void actionPerformed(ActionEvent e)
	{
		if (Boolean.TRUE.equals(application.getRuntimeProperties().get(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG)) && isValueValid())
		{
			if (Debug.tracing())
			{
				Debug.trace("Calendar not shown because a field is marked invalid"); //$NON-NLS-1$
			}
			return;
		}
		JDateChooser chooser = (JDateChooser)((ISmartClientApplication)application).getWindow("JDateChooser"); //$NON-NLS-1$
		Window windowParent = SwingUtilities.getWindowAncestor(this);
		if (chooser == null || SwingUtilities.getWindowAncestor(chooser) != windowParent)
		{
			if (chooser != null)
			{
				chooser.dispose();
				chooser = null;
				((ISmartClientApplication)application).registerWindow("JDateChooser", chooser); //$NON-NLS-1$
			}
			String dateFormat = TagResolver.getFormatString(Date.class, application);
			if (windowParent instanceof JFrame)
			{
				chooser = new JDateChooser((JFrame)windowParent, application.getI18NMessage("servoy.dateChooser.selectDate"), dateFormat); //$NON-NLS-1$
			}
			else if (windowParent instanceof JDialog)
			{
				chooser = new JDateChooser((JDialog)windowParent, application.getI18NMessage("servoy.dateChooser.selectDate"), dateFormat); //$NON-NLS-1$
			}
			else
			{
				Debug.warn("Cannot create date chooser for parent container " + windowParent);
			}
			if (chooser != null) ((ISmartClientApplication)application).registerWindow("JDateChooser", chooser); //$NON-NLS-1$
		}

		if (chooser != null)
		{
			enclosedComponent.requestFocus();
			Object value = enclosedComponent.getValue();
			if (value != null && value instanceof Date)
			{
				Calendar cal = chooser.getSelectedDate();
				cal.setTime((Date)value);
				chooser.updateCalendar(cal);
			}
			else if (value == null)
			{
				Calendar cal = chooser.getSelectedDate();
				cal.setTime(chooser.format(new Date(), enclosedComponent.getFormat()));
				chooser.updateCalendar(cal);
			}
			if (chooser.showDialog(enclosedComponent.getFormat()) == JDateChooser.ACCEPT_OPTION)
			{
				enclosedComponent.requestFocus();
				Calendar selectedDate = chooser.getSelectedDate();
				enclosedComponent.setValueExFromCalendar(selectedDate.getTime());
			}
		}
	}

	public void addEditListener(IEditListener l)
	{
		if (enclosedComponent != null) enclosedComponent.addEditListener(l);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataui.IFieldComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(String tip)
	{
		if (enclosedComponent != null) enclosedComponent.setToolTipText(tip);
		if (tip == null || tip.indexOf("%%") == -1) //$NON-NLS-1$
		{
			super.setToolTipText(tip);
		}
	}

	public void setValueObject(Object obj)
	{
		enclosedComponent.setValueObject(obj);
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

	public void setTagResolver(ITagResolver resolver)
	{
		enclosedComponent.setTagResolver(resolver);
	}

	public Object getValueObject()
	{
		return enclosedComponent.getValue();
	}

	public boolean needEditListener()
	{
		return true;
	}

	public boolean needEntireState()
	{
		return enclosedComponent.needEntireState();
	}

	public void setNeedEntireState(boolean b)
	{
		enclosedComponent.setNeedEntireState(b);
	}

	public String getDataProviderID()
	{
		return enclosedComponent.getDataProviderID();
	}

	public void setDataProviderID(String id)
	{
		enclosedComponent.setDataProviderID(id);
	}

	public void setValidationEnabled(boolean b)
	{
		enclosedComponent.setValidationEnabled(b);
		if (dataProviderID != null && ScopesUtils.isVariableScope(dataProviderID)) return;
		if (b)
		{
			showCal.setEnabled(!readOnly && isEnabled());
		}
		else if (!showCal.isEnabled() && isEnabled())
		{
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				boolean oldReadonly = readOnly;
				setReadOnly(false);
				readOnly = oldReadonly;
			}
		}
	}

	@Override
	public void setForeground(Color fg)
	{
		super.setForeground(fg);
		if (enclosedComponent != null) enclosedComponent.setForeground(fg);
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

	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			if (b && readOnly) showCal.setEnabled(false);
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

	public boolean isReadOnly()
	{
		return !showCal.isEnabled() && !enclosedComponent.isEditable();
	}

	private boolean fieldEditState = true;
	private boolean readOnly = false;

	@Override
	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		if (readOnly && !showCal.isEnabled()) return;
		if (readOnly)
		{
			enclosedComponent.setEditable(false);
			showCal.setEnabled(false);
		}
		else
		{
			enclosedComponent.setEditable(fieldEditState);
			showCal.setEnabled(isEnabled());
		}
	}


	/*
	 * editable---------------------------------------------------
	 */
	public boolean isEditable()
	{
		return enclosedComponent.isEditable();
	}

	public void setEditable(boolean b)
	{
		fieldEditState = b;
		enclosedComponent.setEditable(b);
	}


	/*
	 * titleText---------------------------------------------------
	 */

	public void setTitleText(String title)
	{
		if (enclosedComponent != null) enclosedComponent.setTitleText(title);
	}

	public String getTitleText()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getTitleText();
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

	private Dimension cachedSize;

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public void setCachedLocation(Point location)
	{
		this.cachedLocation = location;
	}

	public void setCachedSize(Dimension size)
	{
		this.cachedSize = size;
	}

	public void requestFocusToComponent()
	{
		if (isDisplayable())
		{
			enclosedComponent.requestFocusToComponent();
		}
		else
		{
			wantFocus = true;
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
			enclosedComponent.requestFocus();
		}
	}

	@Override
	public void requestFocus()
	{
		enclosedComponent.requestFocus();
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { enclosedComponent, showCal };
	}

	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IDisplay#stopEditing()
	 */
	public boolean stopUIEditing(boolean looseFocus)
	{
		if (enclosedComponent != null) return enclosedComponent.stopUIEditing(looseFocus);
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataui.IFieldComponent#setSelectOnEnter(boolean)
	 */
	public void setSelectOnEnter(boolean b)
	{
		if (enclosedComponent != null) enclosedComponent.setSelectOnEnter(b);
	}

	/**
	 * @param adapter
	 */
	public void addActionListener(ActionListener adapter)
	{
		showCal.addActionListener(adapter);
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}

	@Override
	public void setTransferHandler(TransferHandler newHandler)
	{
		super.setTransferHandler(newHandler);
		enclosedComponent.setTransferHandler(newHandler);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}
}
