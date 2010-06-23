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
import java.sql.Types;
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
import javax.swing.text.Document;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.gui.JDateChooser;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptDataCalendarMethods;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;

/**
 * Runtime swing calendar field
 * @author jblok
 */
public class DataCalendar extends EnablePanel implements IFieldComponent, IDisplayData, ActionListener, IDelegate, ISupplyFocusChildren<Component>,
	IScriptDataCalendarMethods, ISupportCachedLocationAndSize
{
	private final DataField enclosedComponent;
	private String dataProviderID;
	private final JButton showCal;
	private final IApplication application;
	private List<ILabel> labels;

	private MouseAdapter rightclickMouseAdapter = null;

	public DataCalendar(IApplication app)
	{
		application = app;
		setLayout(new BorderLayout());
		enclosedComponent = new DataField(app)
		{
			@Override
			public void setFormat(int dataType, String format)
			{
				// calendar field always works with dates (even if it is attached to a dataprovider of another type
				// - for example it could work with a text column that has a Date <-> String converter)
				if (format == null || format.length() == 0)
				{
					// use default locale short date/time format
					format = new SimpleDateFormat().toPattern();
				}
				super.setFormat(Types.DATE, format);
			}
		};

		enclosedComponent.setBorder(BorderFactory.createEmptyBorder());
		enclosedComponent.setOpaque(false);
		add(enclosedComponent, BorderLayout.CENTER);

		showCal = new AbstractScriptButton(app);
		showCal.setText("..."); //$NON-NLS-1$
		showCal.addActionListener(this);
		showCal.setPreferredSize(new Dimension(20, 15));
		showCal.setRequestFocusEnabled(false);
		add(showCal, BorderLayout.EAST);

		setOpaque(true);
		setBackground(Color.white);
		setBorder(BorderFactory.createEtchedBorder());
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

	public void setMargin(Insets i)
	{
		enclosedComponent.setMargin(i);
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

//	private IScriptExecuter actionListner;//allow only one!
	public void addScriptExecuter(IScriptExecuter el)
	{
//		actionListner = el;
		enclosedComponent.addScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return enclosedComponent.getEventExecutor();
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
						((BaseEventExecutor)enclosedComponent.getEventExecutor()).fireRightclickCommand(true, enclosedComponent, e.getModifiers());
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
		JDateChooser chooser = (JDateChooser)application.getWindow("JDateChooser"); //$NON-NLS-1$
		Window windowParent = SwingUtilities.getWindowAncestor(this);
		if (chooser == null || SwingUtilities.getWindowAncestor(chooser) != windowParent)
		{
			if (chooser != null)
			{
				chooser.dispose();
				chooser = null;
				application.registerWindow("JDateChooser", chooser); //$NON-NLS-1$
			}
			String dateFormat = TagResolver.getFormatString(Date.class, application.getSettings());
			if (windowParent instanceof JFrame)
			{
				chooser = new JDateChooser((JFrame)windowParent, application.getI18NMessage("servoy.dateChooser.selectDate"), dateFormat); //$NON-NLS-1$
			}
			else
			{
				chooser = new JDateChooser((JDialog)windowParent, application.getI18NMessage("servoy.dateChooser.selectDate"), dateFormat); //$NON-NLS-1$
			}
			application.registerWindow("JDateChooser", chooser); //$NON-NLS-1$
		}

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
	}

	public void setTagResolver(ITagResolver resolver)
	{
		enclosedComponent.setTagResolver(resolver);
	}

	public Object getValueObject()
	{
		return enclosedComponent.getValue();
	}

	public boolean needEditListner()
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

	public int getDataType()
	{
		return enclosedComponent.getDataType();
	}

	public void setValidationEnabled(boolean b)
	{
		enclosedComponent.setValidationEnabled(b);
		if (dataProviderID != null && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;
		if (b)
		{
			showCal.setEnabled(!readOnly && isEnabled());
		}
		else if (!showCal.isEnabled() && isEnabled())
		{
			showCal.setEnabled(true);
		}
	}

	/*
	 * format---------------------------------------------------
	 */
	public String getFormat()
	{
		return enclosedComponent.getFormat();
	}

	public void setFormat(int dataType, String format)
	{
		enclosedComponent.setFormat(dataType, format);
	}

	public void js_setFormat(String format)
	{
		enclosedComponent.setFormat(enclosedComponent.getDataType(), application.getI18NMessageIfPrefixed(format));
	}

	public String js_getFormat()
	{
		return enclosedComponent.getFormat();
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

	@Override
	public void setForeground(Color fg)
	{
		super.setForeground(fg);
		if (enclosedComponent != null) enclosedComponent.setForeground(fg);
	}


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
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
	 * readonly---------------------------------------------------
	 */
	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public boolean isReadOnly()
	{
		return !showCal.isEnabled() && !enclosedComponent.isEditable();
	}

	private boolean fieldEditState = true;
	private boolean readOnly = false;

	public void js_setReadOnly(boolean readOnly)
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
	public boolean js_isEditable()
	{
		return enclosedComponent.isEditable();
	}

	public void js_setEditable(boolean b)
	{
		setEditable(b);
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

	public String js_getTitleText()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.js_getTitleText();
		}
		return null;
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
			enclosedComponent.js_putClientProperty(key, value);
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

	public int js_getWidth()
	{
		return getSize().width;
	}

	public int js_getHeight()
	{
		return getSize().height;
	}

	public Dimension getCachedSize()
	{
		return cachedSize;
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

	public String js_getElementType()
	{
		return "CALENDAR";
	}

	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}

	public void js_requestFocus(Object[] vargs)
	{
		if (isDisplayable())
		{
			enclosedComponent.js_requestFocus(vargs);
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
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public void addActionListner(ActionListener adapter)
	{
		showCal.addActionListener(adapter);
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}
}
