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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.Component;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportScroll;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.RuntimeMediaField;
import com.servoy.j2db.ui.scripting.RuntimeScriptButton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Component to display media inside the browser has support for up and download of media. Can display the image if it is an image.
 *
 * @author jcompagner,jblok
 */
@SuppressWarnings("nls")
public class WebDataImgMediaField extends Component implements IDisplayData, IFieldComponent, IScrollPane,
	IProviderStylePropertyChanges, ISupportWebBounds, ISupportSimulateBoundsProvider, ISupportScroll
{
	private static final long serialVersionUID = 1L;

	public static byte[] emptyImage = null;
	private static byte[] notEmptyImage = null;

	static
	{
		try
		{
			InputStream is = IApplication.class.getResourceAsStream("images/empty.gif"); //$NON-NLS-1$
			emptyImage = new byte[is.available()];
			is.read(emptyImage);
			is.close();
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
		try
		{
			InputStream is = IApplication.class.getResourceAsStream("images/notemptymedia.gif"); //$NON-NLS-1$
			notEmptyImage = new byte[is.available()];
			is.read(notEmptyImage);
			is.close();
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
	}
//	private Cursor cursor;
	private boolean needEntireState;
//	private int maxLength;
	private Insets margin;
	private int horizontalAlignment = -1;

	private int horizontalScrollBarPolicy;
	private int verticalScrollBarPolicy;

	private int mediaOption;

	private final WebEventExecutor eventExecutor;
	private byte[] previous;

	private MediaResource resource;
	private final Component upload;
	private final Component download;
	private final Component remove;
	private final ImageDisplay imgd;

	private final IApplication application;

	private boolean designMode;
	private final RuntimeMediaField scriptable;

	/**
	 * @param id
	 */
	public WebDataImgMediaField(final IApplication application, RuntimeMediaField scriptable, final String id)
	{
		super(id);
		this.application = application;
		mediaOption = 1;

		this.scriptable = scriptable;

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);

		RuntimeScriptButton imgScriptable = new RuntimeScriptButton(new ChangesRecorder(null, null), application);
		imgd = new ImageDisplay(application, imgScriptable, id); //uses the same name
		imgScriptable.setComponent(imgd, null);
		add(imgd);

		upload = new Component("upload_icon"); //$NON-NLS-1$
		add(upload);

		download = new Component("save_icon"); //$NON-NLS-1$

		add(download);
		remove = new Component("remove_icon");
		add(remove);
	}

	public final RuntimeMediaField getScriptObject()
	{
		return scriptable;
	}


	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}


	public void setHorizontalScrollBarPolicy(int policy)
	{
		setMediaOptions(verticalScrollBarPolicy, policy);
		horizontalScrollBarPolicy = policy;
	}

	/**
	 * @see javax.swing.JScrollPane#setVerticalScrollBarPolicy(int)
	 */
	public void setVerticalScrollBarPolicy(int policy)
	{
		setMediaOptions(policy, horizontalScrollBarPolicy);
		verticalScrollBarPolicy = policy;
	}

	private void setMediaOptions(int vertical, int horizontal)
	{
		if (horizontal == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER || vertical == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
		{
			mediaOption = 10;
		}
		else
		{
			mediaOption = 1;
		}
	}

	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
		imgd.addScriptExecuter(el);
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
		}
		else
		{
			previousValidValue = null;
		}
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataImgMediaField.this);
				}
			});
		}
		else
		{
			setValueValid(true, null);
		}
	}


	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
//		imgd.setActionCommand(id, args);
	}

	public void setValidationEnabled(boolean b)
	{
		eventExecutor.setValidationEnabled(b);
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setNeedEntireState(boolean)
	 */
	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength)
	{
//		this.maxLength = maxLength;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public Insets getMargin()
	{
		return margin;
	}

	public void requestFocusToComponent()
	{
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	@Override
	public void setCursor(Cursor cursor)
	{
//		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		return null;
	}

	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener editListener)
	{
	}

	public Document getDocument()
	{
		return null;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isValueValid)
		{
			return false;
		}
		return true;
	}

	public class ImageDisplay extends WebBaseButton
	{
		private static final long serialVersionUID = 1L;

		public ImageDisplay(IApplication application, RuntimeScriptButton scriptable, String id)
		{
			super(application, scriptable, id);
			setMediaOption(8 + 1);
		}
	}

	private boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !editable) return;
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

	public boolean isEditable()
	{
		return editable;
	}

	public boolean isReadOnly()
	{
		return !editable;
	}

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
	}

	private boolean editable;

	/*
	 * dataprovider---------------------------------------------------
	 */
	private String dataProviderID;

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	@Override
	public void setName(String n)
	{
		name = n;
	}

	private String name;

	@Override
	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	@Override
	public void setBorder(Border border)
	{
		this.border = border;
	}

	@Override
	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	@Override
	public boolean isOpaque()
	{
		return opaque;
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
	public void setToolTipText(String tooltip)
	{
		this.tooltip = Utils.stringIsEmpty(tooltip) ? null : tooltip;
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		return tooltip;
	}

	/*
	 * font---------------------------------------------------
	 */
	@Override
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	@Override
	public Font getFont()
	{
		return font;
	}


	private Color background;

	@Override
	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	@Override
	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	private ArrayList<ILabel> labels;

	@Override
	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	@Override
	public Color getForeground()
	{
		return foreground;
	}


	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (viewable || !visible)
		{
			setVisible(visible);
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}


	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			getStylePropertyChanges().setChanged();
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
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}


	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	@Override
	public Dimension getSize()
	{
		return size;
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, border, margin, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, border, margin, 0, null);
	}


	@Override
	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
	}

	@Override
	public String toString()
	{
		return scriptable.toString("value:" + getValueObject()); //$NON-NLS-1$
	}


	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}

	private final Point scroll = new Point(0, 0);

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportScroll#setScroll(int, int)
	 */
	@Override
	public void setScroll(int x, int y)
	{
		scroll.x = x;
		scroll.y = y;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportScroll#getScroll()
	 */
	@Override
	public Point getScroll()
	{
		return scroll;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportScroll#getScrollComponentMarkupId()
	 */
	@Override
	public String getScrollComponentMarkupId()
	{
		return getMarkupId();
	}
}
