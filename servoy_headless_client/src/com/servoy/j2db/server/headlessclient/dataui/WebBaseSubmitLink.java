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

import javax.swing.border.Border;

import org.apache.wicket.Component;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IAnchoredComponent;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Utils;

/**
 * Base class from labels with an action event.
 *
 * @author jcompagner, jblok
 */
public class WebBaseSubmitLink extends Component
	implements ILabel, IProviderStylePropertyChanges, ISupportSecuritySettings,
	ISupportWebBounds, IButton, IImageDisplay, IAnchoredComponent, ISupportSimulateBoundsProvider, ISupportOnRender
{
	private static final long serialVersionUID = 1L;

	private int halign;
	private int valign;
	private int mediaOptions;
//	private int rotation;
//	private boolean showFocus;
	private Cursor cursor;
	private Insets margin;
	private String inputId;

	protected MediaResource icon;
	private Media media;
//	private Dimension mediaSize;
	private Media rolloverMedia;
	private String iconUrl;
	protected final IApplication application;
	private int anchors = 0;
	private String text_url;
	private String rolloverUrl;
	private final WebEventExecutor eventExecutor;

	protected IFieldComponent labelForComponent;
	private final AbstractRuntimeBaseComponent< ? extends ILabel> scriptable;

	private int rotation = 0;

	private String txt;


	public WebBaseSubmitLink(IApplication application, AbstractRuntimeBaseComponent< ? extends ILabel> scriptable, String id)
	{
		super(id);
		this.application = application;

		halign = ISupportTextSetup.LEFT; // default horizontal align
		valign = ISupportTextSetup.CENTER; // default vertical align

		eventExecutor = new WebEventExecutor(this, true);
		this.scriptable = scriptable;
	}

	public final AbstractRuntimeBaseComponent< ? extends ILabel> getScriptObject()
	{
		return scriptable;
	}


	protected CharSequence getBodyText()
	{
		return txt;
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return "indicator"; //$NON-NLS-1$
	}


	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setMediaOption(int)
	 */
	public void setMediaOption(int mediaOptions)
	{
		this.mediaOptions = mediaOptions;
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setActionCommand(java.lang.String, Object[])
	 */
	public void setActionCommand(String actionCommand, Object[] args)
	{
		eventExecutor.setActionCmd(actionCommand, args);
	}

	public void setDoubleClickCommand(String doubleClickCommand, Object[] args)
	{
		eventExecutor.setDoubleClickCmd(doubleClickCommand, args);
	}

	public void setRightClickCommand(String rightClickCommand, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCommand, args);
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setRolloverIcon(byte[])
	 */
	public void setRolloverIcon(String rolloverMediaUUID)
	{
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setIcon(byte[])
	 */
	public void setIcon(byte[] bs)
	{
		media = null;
//		mediaSize = null;
		if (bs != null && bs.length != 0)
		{
			icon = new MediaResource(bs, mediaOptions);
			if (size != null)
			{
				(icon).checkResize(size);
			}
		}
		else
		{
			icon = new MediaResource(WebDataImgMediaField.emptyImage, mediaOptions);
		}
	}

	public String getMediaIcon()
	{
		return media != null ? media.getUUID().toString() : null;
	}

	public void setMediaIcon(String iconUUID)
	{
		this.icon = null;
		this.iconUrl = null;
		this.media = null;
		if ((media = application.getFlattenedSolution().getMedia(iconUUID)) != null)
		{
			text_url = MediaURLStreamHandler.MEDIA_URL_DEF + media.getName();
		}
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setRotation(int)
	 */
	public void setRotation(int rotation)
	{
		this.rotation = rotation;
	}

	public void setFocusPainted(boolean showFocus)
	{
//		this.showFocus = showFocus;
	}

	public void setHorizontalAlignment(int c)
	{
		this.halign = c;
	}

	public int getVerticalAlignment()
	{
		return valign;
	}

	public void setVerticalAlignment(int t)
	{
		this.valign = t;
	}

	public void setText(String txt)
	{
		this.txt = txt;
	}

	public String getText()
	{
		return txt;
	}

	@Override
	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}


	public void setRolloverEnabled(boolean b)
	{
		// TODO Auto-generated method stub
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public Insets getMargin()
	{
		return margin;
	}

	public void setTextTransform(int mode)
	{
		// ignore should be done through css
	}

	public void setImageURL(String textUrl)
	{
		this.text_url = textUrl;
	}

	public String getImageURL()
	{
		return text_url;
	}

	public String getRolloverImageURL()
	{
		return rolloverUrl;
	}

	public void setRolloverImageURL(String imageUrl)
	{
		this.rolloverUrl = imageUrl;
		if (rolloverUrl != null)
		{
			int index = imageUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index != -1)
			{
				String nm = rolloverUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				Media m = application.getFlattenedSolution().getMedia(nm);
				if (m != null)
				{
					setRolloverIcon(m.getUUID().toString());
				}
			}
		}
	}

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return WebBaseLabel.getThumbnailJPGImage(width, height, icon, text_url, media != null ? media.getUUID().toString() : null, (mediaOptions & 8) == 8,
			application);
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

	protected String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	private String tooltip;

	@Override
	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			this.tooltip = null;
		}
		else
		{
			this.tooltip = tooltip;
		}
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


	/*
	 * bgcolor---------------------------------------------------
	 */

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


	/*
	 * fgcolor---------------------------------------------------
	 */

	private Color foreground;

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
		setVisible(visible);
	}

	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			scriptable.getChangesRecorder().setChanged();
		}
	}

	protected boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		this.viewable = b;
		setComponentVisible(b);
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
	 * client properties for ui---------------------------------------------------
	 */

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	private int mnemonic;

	@Override
	public Dimension getSize()
	{
		return size;
	}

	public int getFontSize()
	{
		int fontSize = 11;
		Font fnt = getFont();
		if (fnt != null)
		{
			fontSize = fnt.getSize();
			fontSize = (int)(fontSize * (4 / (double)3));
		}
		return fontSize;
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)scriptable.getChangesRecorder()).calculateWebSize(size.width, size.height, border, border != null ? null : margin,
			getFontSize(), null, false, valign); // border already have the margin
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)scriptable.getChangesRecorder()).getPaddingAndBorder(size.height, border, border != null ? null : margin, getFontSize(), null,
			false, valign); // border already have the margin
	}


	@Override
	public void setSize(Dimension size)
	{
		this.size = size;
	}

	/**
	 * @see com.servoy.j2db.ui.IStandardLabel#setDisplayedMnemonic(char)
	 */
	public void setDisplayedMnemonic(char mnemonic)
	{
		this.mnemonic = mnemonic;

	}

	public void setLabelFor(IFieldComponent component)
	{
		this.labelForComponent = component;
	}

	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	protected boolean isAnchored()
	{
		return false;
	}

	protected String getCSSId()
	{
		return hasHtmlOrImage() ? getMarkupId() + "_lb" : null;
	}

	protected String getCSSClass()
	{
		WebCellBasedView wcbw = findParent(WebCellBasedView.class);
		return hasHtmlOrImage() && wcbw != null ? wcbw.getTableLabelCSSClass(getId()) : null;
	}

	protected boolean hasHtmlOrImage()
	{
		if (!HtmlUtils.startsWithHtml(getText()))
		{
			return false;
		}
		return true;
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable instanceof ISupportOnRenderCallback)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			if (force) ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor().setRenderStateChanged();
			((ISupportOnRenderCallback)scriptable).getRenderEventExecutor().fireOnRender(isFocused);
		}
	}

	public void requestFocusToComponent()
	{
	}

	public int getDisplayedMnemonic()
	{
		return mnemonic;
	}

	public Object getLabelFor()
	{
		return labelForComponent;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getIcon()
	 */
	public MediaResource getIcon()
	{
		return icon;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getMedia()
	 */
	public Media getMedia()
	{
		return media;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getMediaOptions()
	 */
	public int getMediaOptions()
	{
		return mediaOptions;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getIconUrl()
	 */
	public String getIconUrl()
	{
		return iconUrl;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getTextUrl()
	 */
	public String getTextUrl()
	{
		return text_url;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getRolloverMedia()
	 */
	public Media getRolloverMedia()
	{
		return rolloverMedia;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getRolloverUrl()
	 */
	public String getRolloverUrl()
	{
		return rolloverUrl;
	}

	public void setAnchors(int arg)
	{
		this.anchors = arg;
	}

	public String getParameterValue(String param)
	{
		return null;
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}
}
