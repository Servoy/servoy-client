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
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.link.ILinkListener;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.string.Strings;

import com.servoy.base.scripting.api.IJSEvent.EventType;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.server.headlessclient.ByteArrayResource;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WebClientSession;
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
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Base class for buttons in the webbrowser.
 *
 * @author jcompagner
 *
 */
public abstract class WebBaseButton extends Button
	implements IButton, IResourceListener, ILatestVersionResourceListener, IProviderStylePropertyChanges, ILinkListener, IAjaxIndicatorAware,
	IDoubleClickListener, IRightClickListener, ISupportWebBounds, IImageDisplay, ISupportSimulateBoundsProvider, IAnchoredComponent, ISupportOnRender
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
	private AttributeModifier enabledStyle;
	private Media media;
//	private Dimension mediaSize;
	private Media rolloverMedia;
	private String iconUrl;
	private ResourceReference iconReference;
	private ResourceReference rolloverIconReference;
	protected IApplication application;
	private String text_url;
	private String rolloverUrl;
	private AttributeModifier rolloverBehavior;
	private char mnemonic;
	protected final WebEventExecutor eventExecutor;
	private final AbstractRuntimeButton<IButton> scriptable;
	private int anchors = 0;

	private final static ResourceReference TRANSPARENT_IMAGE = new ResourceReference(IApplication.class, "images/transparent.gif"); //$NON-NLS-1$

	public WebBaseButton(IApplication application, AbstractRuntimeButton<IButton> scriptable, String id)
	{
		super(id);
		this.scriptable = scriptable;
		this.application = application;

		halign = ISupportTextSetup.CENTER; // default horizontal align
		valign = ISupportTextSetup.CENTER; // default vertical align

		//we might have html on a button
		setEscapeModelStrings(false);

		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);
	}

	public WebBaseButton(IApplication application, AbstractRuntimeButton<IButton> scriptable, String id, String label)
	{
		this(application, scriptable, id);
		setModel(new Model<String>(label));
	}


	public final AbstractRuntimeButton<IButton> getScriptObject()
	{
		return scriptable;
	}

	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return WebClientSession.get().hideLoadingIndicator() ? null : "indicator"; //$NON-NLS-1$
	}


	@Override
	public void onSubmit()
	{
		eventExecutor.onEvent(EventType.action, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	//only used if <button> tag is used
	public void onLinkClicked()
	{
		getForm().process();
		eventExecutor.onEvent(EventType.action, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	@Override
	public String getInputName()
	{
		if (inputId == null)
		{
			Page page = findPage();
			if (page instanceof MainPage)
			{
				inputId = ((MainPage)page).nextInputNameId();
			}
			else
			{
				return super.getInputName();
			}
		}
		return inputId;
	}

	public void onResourceRequested()
	{
		String url = StripHTMLTagsConverter.getBlobLoaderUrlPart(getRequest());
		if (url != null)
		{
			try
			{
				byte[] bytes = MediaURLStreamHandler.getBlobLoaderMedia(application, url);
				if (bytes != null)
				{
					String mime = MediaURLStreamHandler.getBlobLoaderMimeType(url);
					if (mime == null) mime = MimeTypes.getContentType(bytes);
					String filename = MediaURLStreamHandler.getBlobLoaderFileName(url);
					if (size != null)
					{
						MediaResource tempIcon = new MediaResource(bytes, mediaOptions);
						(tempIcon).checkResize(size);
						bytes = tempIcon.resized;
					}
					new ByteArrayResource(mime, bytes, filename).onResourceRequested();

				}
			}
			catch (IOException ex)
			{
				Debug.error("Error serving blobloader url: " + url, ex); //$NON-NLS-1$
			}
		}
		else
		{
			icon.onResourceRequested();
		}
	}

	@Override
	protected void onComponentTag(final ComponentTag tag)
	{
		super.onComponentTag(tag);

		if (tag.getName().equalsIgnoreCase("button")) //$NON-NLS-1$
		{
			tag.remove("value"); //$NON-NLS-1$

			boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
			if (useAJAX)
			{
				Object oe = scriptable.getClientProperty("ajax.enabled"); //$NON-NLS-1$
				if (oe != null) useAJAX = Utils.getAsBoolean(oe);
			}

			if (!useAJAX)
			{
				CharSequence url = urlFor(ILinkListener.INTERFACE);
				tag.put("onclick", getForm().getJsForInterfaceUrl(url)); //$NON-NLS-1$
				tag.remove("name");//otherwise org.apache.wicket thinks it has cliked, all button names is always present in the submit params! //$NON-NLS-1$

				if (eventExecutor.hasDoubleClickCmd())
				{
					CharSequence urld = urlFor(IDoubleClickListener.INTERFACE);
					tag.put("ondblclick", getForm().getJsForInterfaceUrl(urld)); //$NON-NLS-1$
				}

				if (eventExecutor.hasRightClickCmd())
				{
					CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
					tag.put("oncontextmenu", getForm().getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			else
			{
				// fix for issue 164656: we don't want single clicks to cause a form submit and block the double clicks.
				if ((!eventExecutor.hasActionCmd()) && (eventExecutor.hasDoubleClickCmd())) tag.put("onclick", "return false;"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			char displayMnemonic = (char)getDisplayedMnemonic();
			if (displayMnemonic > 0) tag.put("accesskey", Character.toString(displayMnemonic)); //$NON-NLS-1$
		}
	}

	/**
	 * @see org.apache.wicket.Component#onBeforeRender()
	 */
	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();

		if (WebBaseButton.getImageDisplayURL(this) != null)
		{
			WebCellBasedView wcbw = findParent(WebCellBasedView.class);
			if (wcbw != null) wcbw.addLabelCssClass(getId());
		}
		fireOnRender(false);
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

	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		scriptable.getChangesRecorder().setRendered();
	}

	@Override
	public void renderHead(HtmlHeaderContainer container)
	{
		super.renderHead(container);
		if (WebBaseButton.getImageDisplayURL(this) != null)
		{
			container.getHeaderResponse().renderOnLoadJavascript("Servoy.Utils.setLabelChildHeight('" + getMarkupId() + "', " + valign + ");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * @see wicket.MarkupContainer#onComponentTagBody(wicket.markup.MarkupStream, wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		CharSequence bodyText = getDefaultModelObjectAsString();

		Object modelObject = getModelObject();
		if (HtmlUtils.startsWithHtml(modelObject))
		{
			// ignore script tags for now
			bodyText = StripHTMLTagsConverter.convertBodyText(this, bodyText, scriptable.trustDataAsHtml(), application.getFlattenedSolution()).getBodyTxt();
		}
		instrumentAndReplaceBody(markupStream, openTag, bodyText);
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
		if ((rolloverMedia = application.getFlattenedSolution().getMedia(rollOverId)) != null)
		{
			addRolloverBehaviors();
			rolloverIconReference = new ResourceReference("media"); //$NON-NLS-1$
		}
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setIcon(byte[])
	 */
	public void setIcon(byte[] bs)
	{
		media = null;
//		mediaSize = null;
		iconReference = null;
		if (bs != null && bs.length != 0)
		{
			addEnabledStyleAttributeModifier();
			icon = new MediaResource(bs, mediaOptions);
			if (size != null)
			{
				(icon).checkResize(size);
			}
		}
	}

	private void addRolloverBehaviors()
	{
		if (rolloverBehavior != null) return;

		rolloverBehavior = WebBaseButton.getImageDisplayRolloverBehavior(this);
		add(rolloverBehavior);
		add(WebBaseButton.getImageDisplayRolloutBehavior(this));
	}

	protected void addEnabledStyleAttributeModifier()
	{
		if (enabledStyle != null) return;

		enabledStyle = new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				return scriptable.isEnabled() ? "" : "filter:alpha(opacity=50);-moz-opacity:.50;opacity:.50;"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		add(enabledStyle);
	}

	public int getMediaIcon()
	{
		return media == null ? 0 : media.getID();
	}

	public void setMediaIcon(int iconId)
	{
		this.icon = null;
		this.iconUrl = null;
		this.iconReference = null;
		if ((media = application.getFlattenedSolution().getMedia(iconId)) != null)
		{
			addEnabledStyleAttributeModifier();
			iconReference = new ResourceReference("media"); //$NON-NLS-1$
			text_url = MediaURLStreamHandler.MEDIA_URL_DEF + media.getName();
		}
		else if (enabledStyle != null)
		{
			remove(enabledStyle);
			enabledStyle = null;
		}
//		mediaSize = null;
	}

	private int rotation = 0;

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
		setModel(new Model<String>(txt));
	}

	public String getText()
	{
		Object o = getDefaultModelObject();
		return o == null ? null : o.toString();
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setSize(java.awt.Dimension)
	 */
	public void setSize(Dimension size)
	{
		this.size = size;
		if (icon != null && size != null)
		{
			icon.checkResize(size);
		}
	}

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

	@Override
	public IConverter getConverter(Class< ? > cls)
	{
		if (cls.isArray() && cls.getComponentType().toString().equals("byte")) return new IConverter() //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			public Object convertToObject(String value, Locale locale)
			{
				return null;
			}

			public String convertToString(Object value, Locale locale)
			{
				return null;
			}
		};
		return StripHTMLTagsConverter.htmlStripper;
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
			iconReference = null;
			iconUrl = null;
		}
		else
		{
			int index = textUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index == -1)
			{
				icon = null;
				iconReference = null;
				iconUrl = textUrl;
				addEnabledStyleAttributeModifier();
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
						iconReference = null;
						iconUrl = null;
						addEnabledStyleAttributeModifier();

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
		rolloverIconReference = null;
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
		addRolloverBehaviors();
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


	public void setName(String n)
	{
		name = n;
	}

	private String name;

	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	public boolean isOpaque()
	{
		return opaque;
	}


	/*
	 * tooltip---------------------------------------------------
	 */

	private String tooltip;

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
	public String getToolTipText()
	{
		if (tooltip != null && getDefaultModel() instanceof ITagResolver)
		{
			return Text.processTags(tooltip, (ITagResolver)getDefaultModel());
		}
		return tooltip;
	}

	/*
	 * font---------------------------------------------------
	 */
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	public Font getFont()
	{
		return font;
	}

	public int getFontSize()
	{
		return 0;
	}

	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}


	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
	}

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

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public Point getLocation()
	{
		return location;
	}


	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

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

	public void onDoubleClick()
	{
		// If form validation fails, we don't execute the method.
		if (getForm().process()) eventExecutor.onEvent(EventType.doubleClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	public void onRightClick()
	{
		// If form validation fails, we don't execute the method.
		if (getForm().process()) eventExecutor.onEvent(EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
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

	protected void instrumentAndReplaceBody(MarkupStream markupStream, ComponentTag openTag, CharSequence bodyText)
	{
		boolean designMode = false;
		IFormUIInternal< ? > formui = findParent(IFormUIInternal.class);
		if (formui != null && formui.isDesignMode())
		{
			designMode = true;
		}

		String cssId = null, cssClass = null;
		if (WebBaseButton.getImageDisplayURL(this) != null)
		{
			cssId = getMarkupId() + "_lb"; //$NON-NLS-1$
			WebCellBasedView wcbw = findParent(WebCellBasedView.class);
			if (wcbw != null) cssClass = wcbw.getTableLabelCSSClass(getId());
		}
		int anchor = Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")) ? anchors : 0; //$NON-NLS-1$
		replaceComponentTagBody(markupStream, openTag,
			instrumentBodyText(bodyText, scriptable.trustDataAsHtml(), halign, valign, false, border, margin, cssId, (char)getDisplayedMnemonic(),
				getMarkupId(), getImageDisplayURL(this), size, true, designMode ? null : cursor, false, anchor, cssClass, rotation, scriptable.isEnabled(),
				openTag));
	}

	public static String getImageDisplayURL(IImageDisplay imageDisplay)
	{
		return (String)getImageDisplayURL(imageDisplay, true)[0];
	}

	public static Object[] getImageDisplayURL(IImageDisplay imageDisplay, boolean appendRandomParam)
	{
		String imgURL = null;
		Boolean isRandomParamRemoved = Boolean.FALSE;

		if (imageDisplay instanceof Component)
		{
			MediaResource mr;
			Component imageDisplayComponent = (Component)imageDisplay;
			if ((mr = imageDisplay.getIcon()) != null)
			{
				CharSequence url = imageDisplayComponent.urlFor(ILatestVersionResourceListener.INTERFACE);
				if (appendRandomParam)
				{
					byte[] imageRawData = mr.getRawData();
					Checksum checksum = new CRC32();
					checksum.update(imageRawData, 0, imageRawData.length);
					url = url + "&r=" + checksum.getValue(); //$NON-NLS-1$
				}
				else isRandomParamRemoved = Boolean.TRUE;
				imgURL = Strings.replaceAll(imageDisplayComponent.getResponse().encodeURL(url), "&", "&amp;").toString(); //$NON-NLS-1$ //$NON-NLS-2$
				if (imageDisplay.getMediaOptions() != 0 && imageDisplay.getMediaOptions() != 1)
				{
					imgURL = imgURL + "&option=" + imageDisplay.getMediaOptions() + "&w=" + imageDisplay.getWebBounds().width + "&h=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						imageDisplay.getWebBounds().height;
				}
			}
			else if (imageDisplay.getIconReference() != null && imageDisplay.getMedia() != null)
			{
				String solutionName = J2DBGlobals.getServiceProvider().getSolution().getName();
				if (imageDisplay.getMediaOptions() != 0 && imageDisplay.getMediaOptions() != 1)
				{
					imgURL = imageDisplayComponent.urlFor(imageDisplay.getIconReference()) + "?id=" + imageDisplay.getMedia().getName() + "&s=" + solutionName + //$NON-NLS-1$//$NON-NLS-2$
						"&option=" + //$NON-NLS-1$
						imageDisplay.getMediaOptions() + "&w=" + imageDisplay.getWebBounds().width + "&h=" + imageDisplay.getWebBounds().height + "&l=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						(imageDisplay.getMedia().getMediaData() != null ? +imageDisplay.getMedia().getMediaData().hashCode() : 0);
				}
				else
				{
					imgURL = imageDisplayComponent.urlFor(imageDisplay.getIconReference()) + "?id=" + imageDisplay.getMedia().getName() + "&s=" + solutionName + //$NON-NLS-1$ //$NON-NLS-2$
						"&l=" + (imageDisplay.getMedia().getMediaData() != null ? +imageDisplay.getMedia().getMediaData().hashCode() : 0); //$NON-NLS-1$
				}
			}
			else if (imageDisplay.getIconUrl() != null)
			{
				imgURL = imageDisplay.getIconUrl();

			}
			else if (imageDisplay.getTextUrl() != null)
			{
				String mediaName = imageDisplay.getTextUrl().substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
				if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
				{
					ICrypt urlCrypt = null;
					if (Application.exists()) urlCrypt = Application.get().getSecuritySettings().getCryptFactory().newCrypt();

					imgURL = StripHTMLTagsConverter.generateBlobloaderUrl(imageDisplayComponent, urlCrypt, mediaName);
				}
			}
			else if (imageDisplay.getRolloverIconReference() != null || imageDisplay.getRolloverUrl() != null || imageDisplay.getRolloverMedia() != null)
			{
				imgURL = imageDisplayComponent.urlFor(TRANSPARENT_IMAGE).toString();
			}
		}

		return new Object[] { imgURL, isRandomParamRemoved };
	}

	protected static AttributeModifier getImageDisplayRolloverBehavior(final IImageDisplay imageDisplay)
	{
		if (imageDisplay instanceof Component)
		{
			final Component imageDisplayComponent = (Component)imageDisplay;

			return new AttributeModifier("onmouseover", true, new Model<String>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					String solutionName = J2DBGlobals.getServiceProvider().getSolution().getName();
					String url = ""; //$NON-NLS-1$
					if (imageDisplay.getRolloverIconReference() != null && imageDisplay.getRolloverMedia() != null)
					{
						if (imageDisplay.getMediaOptions() != 0 && imageDisplay.getMediaOptions() != 1)
						{
							url = imageDisplayComponent.urlFor(imageDisplay.getRolloverIconReference()) + "?id=" + imageDisplay.getRolloverMedia().getName() + //$NON-NLS-1$
								"&s=" + solutionName + "&option=" + //$NON-NLS-1$ //$NON-NLS-2$
								imageDisplay.getMediaOptions() + "&w=" + imageDisplay.getWebBounds().width + "&h=" + imageDisplay.getWebBounds().height + //$NON-NLS-1$//$NON-NLS-2$
								"&l=" + //$NON-NLS-1$
								(imageDisplay.getRolloverMedia().getMediaData() != null ? +imageDisplay.getRolloverMedia().getMediaData().hashCode() : 0);
						}
						else
						{
							url = imageDisplayComponent.urlFor(imageDisplay.getRolloverIconReference()) + "?id=" + imageDisplay.getRolloverMedia().getName() + //$NON-NLS-1$
								"&s=" + solutionName + "&l=" + //$NON-NLS-1$ //$NON-NLS-2$
								(imageDisplay.getRolloverMedia().getMediaData() != null ? +imageDisplay.getRolloverMedia().getMediaData().hashCode() : 0);
						}
					}
					else if (imageDisplay.getRolloverUrl() != null)
					{
						if (imageDisplay.getRolloverUrl().startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
						{
							String mediaName = imageDisplay.getRolloverUrl().substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
							if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
							{
								ICrypt urlCrypt = null;
								if (Application.exists()) urlCrypt = Application.get().getSecuritySettings().getCryptFactory().newCrypt();

								url = StripHTMLTagsConverter.generateBlobloaderUrl(imageDisplayComponent, urlCrypt, mediaName);
							}
						}
						else url = imageDisplay.getRolloverUrl();
					}

					return "Servoy.Rollover.onMouseOver('" + imageDisplayComponent.getMarkupId() + "_img','" + url + "')"; //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
				}
			})
			{
				@Override
				protected String newValue(final String currentValue, final String replacementValue)
				{
					return replacementValue + ";" + currentValue;
				}
			};
		}
		return null;
	}

	protected static AttributeModifier getImageDisplayRolloutBehavior(IImageDisplay imageDisplay)
	{
		if (imageDisplay instanceof Component)
		{
			final Component imageDisplayComponent = (Component)imageDisplay;

			return new AttributeModifier("onmouseout", true, new Model<String>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return "Servoy.Rollover.onMouseOut('" + imageDisplayComponent.getMarkupId() + "_img')"; //$NON-NLS-1$  //$NON-NLS-2$
				}
			})
			{
				@Override
				protected String newValue(final String currentValue, final String replacementValue)
				{
					return currentValue + ";" + replacementValue;
				}
			};
		}

		return null;
	}


	@SuppressWarnings("nls")
	protected static String instrumentBodyText(CharSequence bodyText, boolean trustDataAsHtml, int halign, int valign, boolean hasHtmlOrImage, Border border,
		Insets margin, String cssid, char mnemonic, String elementID, String imgURL, Dimension size, boolean isButton, Cursor bodyCursor, boolean isAnchored,
		int anchors, String cssClass, int rotation, boolean isEnabled, ComponentTag openTag)
	{
		boolean isElementAnchored = anchors != IAnchorConstants.DEFAULT;
		Insets padding = null;
		Insets borderMargin = null;
		boolean usePadding = false;
		if (border == null)
		{
			padding = margin;
		}
		// empty border gets handled as margin
		else if (border instanceof EmptyBorder && !(border instanceof MatteBorder))
		{
			usePadding = true;
			padding = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
		}
		// empty border inside compound border gets handled as margin
		else if (border instanceof CompoundBorder)
		{
			Border inside = ((CompoundBorder)border).getInsideBorder();
			if (inside instanceof EmptyBorder && !(border instanceof MatteBorder))
			{
				usePadding = true;
				padding = ComponentFactoryHelper.getBorderInsetsForNoComponent(inside);
			}
			Border outside = ((CompoundBorder)border).getOutsideBorder();
			if (outside != null)
			{
				borderMargin = ComponentFactoryHelper.getBorderInsetsForNoComponent(outside);
			}
		}
		else if (!(border instanceof BevelBorder) && !(border instanceof EtchedBorder))
		{
			if (border instanceof TitledBorder)
			{
				usePadding = true;
				padding = new Insets(5, 7, 5, 7); // margin + border + padding, see beneath
				padding.top += ComponentFactoryHelper.getTitledBorderHeight(border); // add the legend height
			}
			else
			{
				padding = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
			}
		}

		// In order to vertically align the text inside the <button>, we wrap the text inside a <span>, and we absolutely
		// position the <span> in the <button>. However, for centering vertically we drop this absolute positioning and
		// rely on the fact that by default the <button> tag vertically centers its content.
		StringBuilder instrumentedBodyText = new StringBuilder();
		String onFocus = "onfocus=\"this.parentNode.focus()\"";
		if (openTag.getAttribute("onfocus") != null && openTag.getAttribute("onfocus").length() > 0 && openTag.getName().equals("label"))
			onFocus = "onclick=" + "\"" + openTag.getAttribute("onfocus").replaceFirst("this", "this.parentNode") + "\"";
		// the latest browsers (except for IE), do not trigger an onfocus neither on the span nor on the parent label element
		// as a workaround, an onclick is added in the span of label elements that does what the parent onfocus does
		instrumentedBodyText.append("<span ").append(onFocus).append(" style='").append( //$NON-NLS-1$
			(bodyCursor == null ? "" : "cursor: " + (bodyCursor.getType() == Cursor.HAND_CURSOR ? "pointer" : "default") + "; ")).append("display: block;");
		int top = 0;
		int bottom = 0;
		int left = 0;
		int right = 0;
		if (padding != null && usePadding)
		{
			top = padding.top;
			bottom = padding.bottom;
			left = padding.left;
			right = padding.right;
		}

		if (rotation == 0 || size.width >= size.height)
		{
			// Horizontal alignment and anchoring.
			instrumentedBodyText.append(" left: ").append(left).append("px;"); //$NON-NLS-1$ //$NON-NLS-2$
			instrumentedBodyText.append(" right: ").append(right).append("px;"); //$NON-NLS-1$ //$NON-NLS-2$

			// Vertical alignment and anchoring.
			if (cssid == null)
			{
				if (valign == ISupportTextSetup.TOP) instrumentedBodyText.append(" top: ").append(top).append("px;"); //$NON-NLS-1$ //$NON-NLS-2$
				else if (valign == ISupportTextSetup.BOTTOM) instrumentedBodyText.append(" bottom: ").append(bottom).append("px;"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Full height.
			if (hasHtmlOrImage && valign != ISupportTextSetup.CENTER && cssid == null) instrumentedBodyText.append(" height: 100%;"); //$NON-NLS-1$
			else if ((cssid != null) || (valign != ISupportTextSetup.CENTER)) instrumentedBodyText.append(" position: absolute;"); //$NON-NLS-1$
			else if (!isButton && !hasHtmlOrImage && imgURL == null)
			{
				int innerHeight = size.height;
				if (padding != null) innerHeight -= padding.top + padding.bottom;
				if (borderMargin != null) innerHeight -= borderMargin.top + borderMargin.bottom;
				instrumentedBodyText.append("height: ").append(innerHeight).append("px;line-height: ").append(innerHeight).append("px;");
			}

			if (isAnchored)
			{
				instrumentedBodyText.append(" position: relative;"); //$NON-NLS-1$
			}
		}
		else
		{
			// this is a special case, invert width and height so that text is fully visible when rotated
			int innerWidth = size.height;
			if (padding != null) innerWidth -= padding.top + padding.bottom;
			if (borderMargin != null) innerWidth -= borderMargin.top + borderMargin.bottom;

			int innerHeight = size.width;
			if (padding != null) innerHeight -= padding.left + padding.right;
			if (borderMargin != null) innerHeight -= borderMargin.left + borderMargin.right;

			int rotationOffset = (innerWidth - innerHeight) / 2;
			instrumentedBodyText.append(" left: -").append(rotationOffset).append("px;"); //$NON-NLS-1$ //$NON-NLS-2$
			instrumentedBodyText.append(" top: ").append(rotationOffset).append("px;"); //$NON-NLS-1$ //$NON-NLS-2$
			instrumentedBodyText.append(" position: absolute;");
			instrumentedBodyText.append(" height: ").append(innerHeight).append("px;");
			instrumentedBodyText.append(" width: ").append(innerWidth).append("px;");
			instrumentedBodyText.append("line-height: ").append(innerHeight).append("px;");
		}

		if (!hasHtmlOrImage) instrumentedBodyText.append(" overflow: hidden;");

		if (halign == ISupportTextSetup.LEFT) instrumentedBodyText.append(" text-align: left;"); //$NON-NLS-1$
		else if (halign == ISupportTextSetup.RIGHT) instrumentedBodyText.append(" text-align: right;"); //$NON-NLS-1$
		else instrumentedBodyText.append(" text-align: center;"); //$NON-NLS-1$

		if (rotation > 0)
		{
			String rotationCss = "rotate(" + rotation + "deg)";
			instrumentedBodyText.append(" -ms-transform: ").append(rotationCss).append(";").append(" -moz-transform: ").append(rotationCss).append(";").append( //$NON-NLS-1$
				" -webkit-transform: ").append(rotationCss).append(";").append(" -o-transform: ").append(rotationCss).append(";").append(" transform: ").append(
					rotationCss).append(";");
		}

		if (cssid != null && cssClass == null) instrumentedBodyText.append(" visibility: hidden;"); //$NON-NLS-1$

		instrumentedBodyText.append("'"); //$NON-NLS-1$

		if (cssClass != null)
		{
			instrumentedBodyText.append(" class='"); //$NON-NLS-1$
			instrumentedBodyText.append(cssClass);
			instrumentedBodyText.append("'"); //$NON-NLS-1$
		}

		if (cssid != null)
		{
			instrumentedBodyText.append(" id='"); //$NON-NLS-1$
			instrumentedBodyText.append(cssid);
			instrumentedBodyText.append("'"); //$NON-NLS-1$
		}

		instrumentedBodyText.append(">"); //$NON-NLS-1$

		//in ie<8 the filter:alpha(opacity=50) applied on the <button> element is not applied to the <img> element
		String IE8filterFIx = "";
		if (!isEnabled)
		{
			WebClientInfo webClientInfo = new WebClientInfo((WebRequestCycle)RequestCycle.get());
			ClientProperties cp = webClientInfo.getProperties();
			if (cp.isBrowserInternetExplorer() && cp.getBrowserVersionMajor() != -1 && cp.getBrowserVersionMajor() < 9)
			{
				IE8filterFIx = "filter:alpha(opacity=50);";
			}
		}
		if (!Strings.isEmpty(bodyText))
		{
			CharSequence bodyTextValue = sanitize(bodyText, trustDataAsHtml);

			if (mnemonic > 0 && !hasHtmlOrImage)
			{
				StringBuilder sbBodyText = new StringBuilder(bodyTextValue);
				int mnemonicIdx = sbBodyText.indexOf(Character.toString(mnemonic));
				if (mnemonicIdx != -1)
				{
					sbBodyText.insert(mnemonicIdx + 1, "</u>");
					sbBodyText.insert(mnemonicIdx, "<u>");
					bodyTextValue = sbBodyText;
				}
			}

			if (imgURL != null)
			{

				String onLoadCall = isElementAnchored ? " onload=\"Servoy.Utils.setLabelChildHeight('" + elementID + "', " + valign + ");\"" : "";
				StringBuilder sb = new StringBuilder("<img id=\"").append(elementID).append("_img").append("\" src=\"").append(imgURL).append(
					"\" style=\"vertical-align: middle;").append(IE8filterFIx).append(
						(isElementAnchored && (cssClass == null) ? "visibility:hidden;" : "")).append("\"").append(onLoadCall).append("/>");
				sb.append("<span style=\"vertical-align:middle;\">&nbsp;").append(bodyTextValue);
				bodyTextValue = sb;
			}

			instrumentedBodyText.append(bodyTextValue);

			if (imgURL != null)
			{
				instrumentedBodyText.append("</span>");
			}
		}
		else if (imgURL != null)
		{
			instrumentedBodyText.append("<img id=\"");
			instrumentedBodyText.append(elementID).append("_img\"");
			instrumentedBodyText.append("style=\"").append((isElementAnchored && (cssClass == null) ? " visibility:hidden;" : "")).append(IE8filterFIx).append(
				"\""); // hide it until setLabelChildHeight is calculated
			instrumentedBodyText.append(" src=\"");
			instrumentedBodyText.append(imgURL);
			String onLoadCall = isElementAnchored ? " onload=\"Servoy.Utils.setLabelChildHeight('" + elementID + "', " + valign + ");\"" : "";
			instrumentedBodyText.append("\" align=\"middle\"").append(onLoadCall).append("/>");
		}

		instrumentedBodyText.append("</span>"); //$NON-NLS-1$

		if (border instanceof TitledBorder)
		{
			instrumentedBodyText = new StringBuilder(getTitledBorderOpenMarkup((TitledBorder)border)).append(instrumentedBodyText).append(
				getTitledBorderCloseMarkup());
		}
		if (border instanceof CompoundBorder)
		{
			Border outside = ((CompoundBorder)border).getOutsideBorder();
			if (outside != null && outside instanceof TitledBorder)
			{
				instrumentedBodyText = new StringBuilder(getTitledBorderOpenMarkup((TitledBorder)outside)).append(instrumentedBodyText).append(
					getTitledBorderCloseMarkup());
			}
		}
		return instrumentedBodyText.toString();
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
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getIconReference()
	 */
	public ResourceReference getIconReference()
	{
		return iconReference;
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
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getRolloverIconReference()
	 */
	public ResourceReference getRolloverIconReference()
	{
		return rolloverIconReference;
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
