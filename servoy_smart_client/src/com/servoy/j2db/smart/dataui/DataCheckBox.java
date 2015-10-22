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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;

import sun.java2d.SunGraphics2D;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.IMarginAwareBorder;
import com.servoy.j2db.component.INullableAware;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.scripting.AbstractRuntimeValuelistComponent;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Runtime swing check box component
 * @author jblok
 */

public class DataCheckBox extends JCheckBox implements IFieldComponent, IDisplayData, ISkinnable, INullableAware, ISupportCachedLocationAndSize,
	ISupportValueList, IMarginAwareBorder, ISupportOnRender
{
	private Object value;
	protected IValueList onValue;
	protected IApplication application;

	private String tooltip;
	private final EventExecutor eventExecutor;
	private MouseAdapter rightclickMouseAdapter = null;
	private boolean allowNull = true;
	private final AbstractRuntimeValuelistComponent<IFieldComponent> scriptable;

	public DataCheckBox(IApplication application, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, String text)
	{
		setText(Text.processTags(text, null));
		this.application = application;
		eventExecutor = new EventExecutor(this);
		addKeyListener(eventExecutor);
		this.scriptable = scriptable;
	}

	public DataCheckBox(IApplication application, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, String text, IValueList onValue)
	{
		this(application, scriptable, text);
		this.onValue = onValue;
	}

	public final AbstractRuntimeValuelistComponent<IFieldComponent> getScriptObject()
	{
		return scriptable;
	}

	/**
	 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5 see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
	 */
	@Override
	public FontMetrics getFontMetrics(Font font)
	{
		if (application != null)//getFontMetrics can be called in the constructor super call before application is assigned
		{
			boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
			if (isPrinting)
			{
				Graphics g = (Graphics)application.getRuntimeProperties().get("printGraphics"); //$NON-NLS-1$
				if (g != null)
				{
					String text = getText();
					// only return print graphics font metrics if text does not start with 'W',
					// because of left side bearing issue
					if (!(text != null && text.length() > 0 && text.charAt(0) == 'W')) return g.getFontMetrics(font);
				}
			}
		}
		return super.getFontMetrics(font);
	}

	public Document getDocument()
	{
		return null;
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
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

	private boolean wasEditable;

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
		repaint(); // foreground color changes
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

	@Override
	public Color getForeground()
	{
		if (isValueValid())
		{
			return super.getForeground();
		}
		return Color.red;
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		if (previousValidValue != null)
		{
			oldVal = previousValidValue;
		}

		eventExecutor.fireChangeCommand(oldVal, newVal, false, this);

		//if change cmd is not succeeded also don't call action cmd?
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
					if (scriptable.isEnabled())
					{
						eventExecutor.fireRightclickCommand(true, DataCheckBox.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(rightclickMouseAdapter);
		}
	}

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
		}
		else
		{
			wasEditable = !isReadOnly();
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
		}
		eventExecutor.setValidationEnabled(b);
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________


	public void setMaxLength(int i)
	{
		//ignore
	}

	private boolean needEntireState;

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
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
			editProvider = new EditProvider(this, application);
			addFocusListener(editProvider);
			addItemListener(editProvider);
			editProvider.addEditListener(l);
		}
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	private String dataProviderID;

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
	}

	@Override
	public void setToolTipText(String tip)
	{
		if (tip != null && tip.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tooltip = tip;
			// register an empty one so that this component is registered as a tooltipper..
			super.setToolTipText(""); //$NON-NLS-1$
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
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValueObject()
	 */
	public Object getValueObject()
	{
		if (onValue != null && onValue.getSize() >= 1)
		{
			return (isSelected() ? onValue.getRealElementAt(0) : null);
		}
		else
		{
			// if value == null and still nothing selected return null (no data change)
			if (this.value == null && !isSelected())
			{
				return null;
			}
			return new Integer((isSelected() ? 1 : 0));
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

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#setValueObject(Object)
	 */
	public void setValueObject(Object data)
	{
		try
		{
			if (editProvider != null) editProvider.setAdjusting(true);

			if (tooltip != null)
			{
				super.setToolTipText(""); //$NON-NLS-1$
			}
			if (!Utils.equalObjects(getValueObject(), data))
			{
				setValueValid(true, null);
			}
			this.value = data;
			if (onValue != null && onValue.getSize() >= 1)
			{
				Object real = onValue.getRealElementAt(0);
				if (real == null)
				{
					setSelected(value == null);
				}
				else
				{
					setSelected(real.equals(data));
				}
			}
			else
			{
				if (data instanceof Number)
				{
					setSelected(((Number)data).intValue() >= 1);
				}
				else if (data == null)
				{
					setSelected(false);
				}
				else
				{
					setSelected("1".equals(data.toString())); //$NON-NLS-1$
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

	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			enabled = b;
			super.setEnabled(enabled && !readonly);
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

	public void setEditable(boolean b)
	{
		if (accessible)
		{
			editState = b;
			readonly = !b;
			super.setEnabled(enabled && !readonly);
		}
	}

	public boolean isEditable()
	{
		return !isReadOnly();
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

	private boolean editState;
	private ArrayList<ILabel> labels;
	private boolean enabled = true;
	private boolean readonly = false;

	public void setReadOnly(boolean b)
	{
		if (b && isReadOnly()) return;
		if (b)
		{
			if (accessible)
			{
				readonly = b;
				super.setEnabled(!readonly && enabled);
			}
		}
		else
		{
			if (accessible)
			{
				readonly = b;
				super.setEnabled(editState && enabled);
			}
		}
	}

	public boolean isReadOnly()
	{
		return readonly;
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
		if (labels == null) labels = new ArrayList(3);
		labels.add(label);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
		setText(Text.processTags(title, resolver));
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
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


	public IValueList getValueList()
	{
		return onValue;
	}

	public ListDataListener getListener()
	{
		return null;
	}

	public void setValueList(IValueList vl)
	{
		onValue = vl;
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

	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
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
		return true;
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}

	/**
	 * If the check-box is linked to a non-null integer table column, it must force null to become 0 (it is normally shown as unchecked for null) so that the
	 * user does not need to check/uncheck it for save. This tells the check-box if it is linked to an allowNull field or not. By default allowNull = true.
	 * 
	 * @param allowNull true if it should allow null values for integer data-providers (for unchecked) and false if it should change null value to value 0.
	 */
	public void setAllowNull(boolean allowNull)
	{
		this.allowNull = allowNull;
	}

	public boolean getAllowNull()
	{
		return allowNull;
	}

	@Override
	public void paint(Graphics g)
	{
		// If we have regular SunGraphics2D object, just forward to superclass.
		if (g instanceof SunGraphics2D)
		{
			super.paint(g);
		}
		else
		{
			// If we are on Mac OS, we paint first to image buffer and then
			// to actual graphics. This is because the Aqua L&F on Mac does
			// not paint properly on graphics object that are not instances
			// of SunGraphics2D.
			if (Utils.isAppleMacOS())
			{
				// Create buffered image and send it to be painted by superclass.
				int width = this.getWidth();
				int height = this.getHeight();
				BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gr1 = (Graphics2D)img.getGraphics().create();
				super.paint(gr1);
				gr1.dispose();

				// Paint the image to the graphics that we received.
				Graphics2D g2d = (Graphics2D)g;
				g2d.drawRenderedImage(img, null);
			}
			// If we are not on Mac OS, just forward to superclass.
			else
			{
				super.paint(g);
			}
		}
	}
}