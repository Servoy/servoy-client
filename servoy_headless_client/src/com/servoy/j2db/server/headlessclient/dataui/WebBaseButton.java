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
import javax.swing.border.TitledBorder;

import org.apache.wicket.Component;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.util.HCUtils;
import com.servoy.j2db.ui.IAnchoredComponent;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeButton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Base class for buttons in the webbrowser.
 *
 * @author jcompagner
 *
 */
public abstract class WebBaseButton extends Component
	implements IButton, IProviderStylePropertyChanges, ISupportWebBounds, IImageDisplay, ISupportSimulateBoundsProvider, IAnchoredComponent, ISupportOnRender
{
	private int mediaOptions;
//	private int rotation;
//	private boolean showFocus;
	private int halign;
	private int valign;
	private Cursor cursor;
	private String inputId;
	private Insets margin;
	protected MediaResource icon;
	private Media media;
//	private Dimension mediaSize;
	private Media rolloverMedia;
	private String iconUrl;
	protected IApplication application;
	private String text_url;
	private String rolloverUrl;
	private char mnemonic;
	protected final WebEventExecutor eventExecutor;
	private final AbstractRuntimeButton<IButton> scriptable;
	private int anchors = 0;

	public WebBaseButton(IApplication application, AbstractRuntimeButton<IButton> scriptable, String id)
	{
		super(id);
		this.scriptable = scriptable;
		this.application = application;

		halign = ISupportTextSetup.CENTER; // default horizontal align
		valign = ISupportTextSetup.CENTER; // default vertical align

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
	}

	public WebBaseButton(IApplication application, AbstractRuntimeButton<IButton> scriptable, String id, String label)
	{
		this(application, scriptable, id);
		this.txt = label;
	}


	public final AbstractRuntimeButton<IButton> getScriptObject()
	{
		return scriptable;
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return "indicator"; //$NON-NLS-1$
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(isFocused);
		}
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

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
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
	public void setRolloverIcon(int rollOverId)
	{
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setIcon(byte[])
	 */
	public void setIcon(byte[] bs)
	{
		media = null;
	}


	public int getMediaIcon()
	{
		return media == null ? 0 : media.getID();
	}

	public void setMediaIcon(int iconId)
	{
		this.icon = null;
		this.iconUrl = null;
		if ((media = application.getFlattenedSolution().getMedia(iconId)) != null)
		{
			text_url = MediaURLStreamHandler.MEDIA_URL_DEF + media.getName();
		}
	}

	private int rotation = 0;
	private String txt;

	/**
	 * @see com.servoy.j2db.ui.ILabel#setRotation(int)
	 */
	public void setRotation(int rotation)
	{
		this.rotation = rotation;
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setFocusPainted(boolean)
	 */
	public void setFocusPainted(boolean showFocus)
	{
		//this.showFocus = showFocus;
	}

	/**
	 * @see com.servoy.j2db.ui.IStandardLabel#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int c)
	{
		this.halign = c;
	}

	public int getHorizontalAlignment()
	{
		return halign;
	}

	/**
	 * @see com.servoy.j2db.ui.IStandardLabel#setVerticalAlignment(int)
	 */
	public void setVerticalAlignment(int t)
	{
		this.valign = t;
	}

	public int getVerticalAlignment()
	{
		return valign;
	}

	public void setText(String txt)
	{
		this.txt = txt;
	}

	public String getText()
	{
		return txt;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setSize(java.awt.Dimension)
	 */
	@Override
	public void setSize(Dimension size)
	{
		this.size = size;
		if (icon != null && size != null)
		{
			icon.checkResize(size);
		}
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


	/*
	 * jsmethods---------------------------------------------------
	 */
	public void requestFocusToComponent()
	{
		// is the current main container always the right one?
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	public void setImageURL(String textUrl)
	{
		this.text_url = textUrl;
		if (textUrl == null)
		{
			icon = null;
			iconUrl = null;
		}
		else
		{
			int index = textUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index == -1)
			{
				icon = null;
				iconUrl = textUrl;
			}
			else
			{
				String mediaName = textUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				try
				{
					Media m = application.getFlattenedSolution().getMedia(mediaName);
					if (m != null)
					{
						setMediaIcon(m.getID());
					}
					else if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
					{
						// clear previous images
						icon = null;
						iconUrl = null;

					}
				}
				catch (Exception ex)
				{
					Debug.error("Error loading media for url: " + textUrl, ex); //$NON-NLS-1$
				}
			}
		}
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
					setRolloverIcon(m.getID());
				}
			}
		}
		scriptable.getChangesRecorder().setChanged();
	}

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return WebBaseLabel.getThumbnailJPGImage(width, height, icon, text_url, media != null ? media.getID() : 0, (mediaOptions & 8) == 8, application);
	}

	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
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
	 * tooltip---------------------------------------------------
	 */

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

	public int getFontSize()
	{
		return 0;
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
		if (viewable)
		{
			setVisible(visible);
		}
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
		if (size != null)
		{
			Dimension d = ((ChangesRecorder)scriptable.getChangesRecorder()).calculateWebSize(size.width, size.height, border, margin, 0, null, true, valign);
			return new Rectangle(location, d);
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)scriptable.getChangesRecorder()).getPaddingAndBorder(size == null ? 0 : size.height, border, margin, 0, null, true, valign);
	}


	/**
	 * @see com.servoy.j2db.ui.IStandardLabel#setDisplayedMnemonic(char)
	 */
	public void setDisplayedMnemonic(char mnemonic)
	{
		this.mnemonic = mnemonic;
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

	/**
	 * @param bodyText
	 * @param trustDataAsHtml
	 * @return
	 */
	public static CharSequence sanitize(CharSequence bodyText, boolean trustDataAsHtml)
	{
		if (trustDataAsHtml || bodyText == null)
		{
			return bodyText;
		}

		CharSequence sanitized = HCUtils.sanitize(bodyText);
		if (HtmlUtils.startsWithHtml(bodyText) && !HtmlUtils.equalsIgnoreWhitespaceAndCase(sanitized, bodyText))
		{
			Debug.warn("Html was modified by sanitizer: " + //
				"\nOriginal html: '" + bodyText + "'," + //
				"\nSanitized html: '" + sanitized + "'");
		}

		return sanitized;
	}

	public static String getTitledBorderOpenMarkup(TitledBorder titledBorder)
	{
		String align = "left";
		if (titledBorder.getTitleJustification() == TitledBorder.CENTER)
		{
			align = "center";
		}
		if (titledBorder.getTitleJustification() == TitledBorder.RIGHT)
		{
			align = "right";
		}
		StringBuilder fieldsetMarkup = new StringBuilder(
			"<fieldset style='top:0px;bottom:0px;left:0px;right:0px;position:absolute;margin:0px 2px 2px;padding:3px;'><legend align='");
		fieldsetMarkup.append(align);
		fieldsetMarkup.append("' style='");
		if (titledBorder.getTitleColor() != null)
		{
			fieldsetMarkup.append("color:");
			fieldsetMarkup.append(PersistHelper.createColorString(titledBorder.getTitleColor()));
			fieldsetMarkup.append(";");
		}
		if (titledBorder.getTitleFont() != null)
		{
			Pair<String, String>[] fontPropertiesPair = PersistHelper.createFontCSSProperties(PersistHelper.createFontString(titledBorder.getTitleFont()));
			if (fontPropertiesPair != null)
			{
				for (Pair<String, String> element : fontPropertiesPair)
				{
					if (element == null) continue;
					fieldsetMarkup.append(element.getLeft());
					fieldsetMarkup.append(":");
					fieldsetMarkup.append(element.getRight());
					fieldsetMarkup.append(";");
				}
			}
		}
		fieldsetMarkup.append("line-height:1em;");
		fieldsetMarkup.append("'>");
		fieldsetMarkup.append(titledBorder.getTitle());
		fieldsetMarkup.append("</legend>"); //$NON-NLS-1$
		return fieldsetMarkup.toString();
	}

	public static String getTitledBorderCloseMarkup()
	{
		return "</fieldset>"; //$NON-NLS-1$
	}

	public Object getLabelFor()
	{
		return null;
	}

	public int getDisplayedMnemonic()
	{
		return mnemonic;
	}

	public String getParameterValue(String param)
	{
		return null;
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

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}

	public void setAnchors(int arg)
	{
		this.anchors = arg;
	}
}
