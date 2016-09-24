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

import javax.swing.border.Border;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.base.scripting.api.IJSEvent.EventType;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.server.headlessclient.ByteArrayResource;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WebClientSession;
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
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Base class from labels with an action event.
 *
 * @author jcompagner, jblok
 */
public class WebBaseSubmitLink extends SubmitLink
	implements ILabel, IResourceListener, ILatestVersionResourceListener, IProviderStylePropertyChanges, ISupportSecuritySettings, IAjaxIndicatorAware,
	IDoubleClickListener, IRightClickListener, ISupportWebBounds, IButton, IImageDisplay, IAnchoredComponent, ISupportSimulateBoundsProvider, ISupportOnRender
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
	private AttributeModifier enabledStyle;
	private Media media;
//	private Dimension mediaSize;
	private Media rolloverMedia;
	private String iconUrl;
	private ResourceReference iconReference;
	private ResourceReference rolloverIconReference;
	protected final IApplication application;
	private int anchors = 0;
	private String text_url;
	private String rolloverUrl;
	private final WebEventExecutor eventExecutor;
	private AttributeModifier rolloverBehavior;

	protected IFieldComponent labelForComponent;
	private final AbstractRuntimeBaseComponent< ? extends ILabel> scriptable;

	public WebBaseSubmitLink(IApplication application, AbstractRuntimeBaseComponent< ? extends ILabel> scriptable, String id)
	{
		super(id);
		this.application = application;

		halign = ISupportTextSetup.LEFT; // default horizontal align
		valign = ISupportTextSetup.CENTER; // default vertical align

		setEscapeModelStrings(false);
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);

		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				if (!isEnabled())
				{
					return "cursor: default;"; //$NON-NLS-1$
				}
				return null;
			}
		}));

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);
		this.scriptable = scriptable;
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(null, TemplateGenerator.DEFAULT_LABEL_PADDING);
	}

	public final AbstractRuntimeBaseComponent< ? extends ILabel> getScriptObject()
	{
		return scriptable;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@SuppressWarnings("nls")
	@Override
	public void renderHead(HtmlHeaderContainer container)
	{
		super.renderHead(container);
		if (hasHtmlOrImage())
		{
			container.getHeaderResponse().renderOnLoadJavascript("Servoy.Utils.setLabelChildHeight('" + getMarkupId() + "', " + valign + ");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	protected CharSequence getBodyText()
	{
		return StripHTMLTagsConverter.convertBodyText(this, getDefaultModelObjectAsString(), scriptable.trustDataAsHtml(),
			application.getFlattenedSolution()).getBodyTxt();
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

	/**
	 * @see wicket.markup.html.form.Button#onSubmit()
	 */
	@Override
	public void onSubmit()
	{
		eventExecutor.onEvent(EventType.action, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	/**
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		scriptable.getChangesRecorder().setRendered();
	}

	/**
	 * @see wicket.MarkupContainer#onComponentTagBody(wicket.markup.MarkupStream, wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		instrumentAndReplaceBody(markupStream, openTag, getDefaultModelObjectAsString());
	}

	/**
	 * @see wicket.markup.html.form.FormComponent#getInputName()
	 */
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

	/**
	 * @see wicket.IResourceListener#onResourceRequested()
	 */
	public void onResourceRequested()
	{
		String mediaName = RequestCycle.get().getRequest().getParameter("media"); //$NON-NLS-1$
		if (mediaName != null)
		{
			Media m;
			try
			{
				m = application.getFlattenedSolution().getMedia(mediaName);
				byte[] bytes = m.getMediaData();
				new ByteArrayResource(MimeTypes.getContentType(bytes), bytes, null).onResourceRequested();
			}
			catch (Exception ex)
			{
				Debug.error("Error serving media: " + mediaName, ex); //$NON-NLS-1$
			}
		}
		else if (getRequest().getParameter(StripHTMLTagsConverter.BLOB_LOADER_PARAM) != null)
		{
			String url = StripHTMLTagsConverter.getBlobLoaderUrlPart(getRequest());
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
	public void setRolloverIcon(int rolloverMediaId)
	{
		if ((rolloverMedia = application.getFlattenedSolution().getMedia(rolloverMediaId)) != null)
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
		else
		{
			icon = new MediaResource(WebDataImgMediaField.emptyImage, mediaOptions);
		}
	}

	private void addRolloverBehaviors()
	{
		if (rolloverBehavior != null) return;

		rolloverBehavior = WebBaseButton.getImageDisplayRolloverBehavior(this);
		add(rolloverBehavior);
		add(WebBaseButton.getImageDisplayRolloutBehavior(this));
	}

	private void addEnabledStyleAttributeModifier()
	{
		if (enabledStyle != null) return;

		enabledStyle = new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				return scriptable.isEnabled() ? "" : "filter:alpha(opacity=50);-moz-opacity:.50;opacity:.50;"; //$NON-NLS-1$  //$NON-NLS-2$
			}
		});
		add(enabledStyle);
	}

	public int getMediaIcon()
	{
		return media != null ? media.getID() : 0;
	}

	public void setMediaIcon(int iconId)
	{
		this.icon = null;
		this.iconUrl = null;
		this.iconReference = null;
		this.media = null;
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
		setDefaultModel(new Model<String>(txt));
	}

	public String getText()
	{
		Object o = getDefaultModelObject();
		return o == null ? null : o.toString();
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
				String nm = textUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				try
				{
					Media m = application.getFlattenedSolution().getMedia(nm);
					if (m != null)
					{
						setMediaIcon(m.getID());
					}
					else if (nm.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
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
	}

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return WebBaseLabel.getThumbnailJPGImage(width, height, icon, text_url, media != null ? media.getID() : 0, (mediaOptions & 8) == 8, application);
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
	 * titleText---------------------------------------------------
	 */

	protected String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

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
			final ITagResolver resolver = (ITagResolver)getDefaultModel();
			return Text.processTags(tooltip, resolver);
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


	/*
	 * bgcolor---------------------------------------------------
	 */

	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	/*
	 * fgcolor---------------------------------------------------
	 */

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
		setVisible(visible);
	}

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

	public void setLocation(Point location)
	{
		this.location = location;
	}

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

	public Dimension getSize()
	{
		return size;
	}

	public int getFontSize()
	{
		int fontSize = TemplateGenerator.DEFAULT_FONT_SIZE;
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
	protected void onComponentTag(final ComponentTag tag)
	{
		super.onComponentTag(tag);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			Object oe = scriptable.getClientProperty("ajax.enabled"); //$NON-NLS-1$
			if (oe != null) useAJAX = Utils.getAsBoolean(oe);
		}

		if (!useAJAX)
		{
			if (eventExecutor.hasDoubleClickCmd())
			{
				CharSequence urld = urlFor(IDoubleClickListener.INTERFACE);
				tag.put("ondblclick", getForm().getJsForInterfaceUrl(urld)); //$NON-NLS-1$
			}

			if (eventExecutor.hasRightClickCmd())
			{
				CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
				// We need a "return false;" so that the context menu is not displayed in the browser.
				tag.put("oncontextmenu", getForm().getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
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
		boolean hasHtmlOrImage = hasHtmlOrImage();

		String cssid = getCSSId();
		String cssclass = getCSSClass();

		boolean designMode = false;
		IFormUIInternal< ? > formui = findParent(IFormUIInternal.class);
		if (formui != null && formui.isDesignMode())
		{
			designMode = true;
		}
		int anchor = Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")) ? anchors : 0; //$NON-NLS-1$
		replaceComponentTagBody(markupStream, openTag,
			WebBaseButton.instrumentBodyText(bodyText, scriptable.trustDataAsHtml(), halign, valign, hasHtmlOrImage, border, margin, cssid,
				(char)getDisplayedMnemonic(), getMarkupId(), WebBaseButton.getImageDisplayURL(this), size, false, designMode ? null : cursor, isAnchored(),
				anchor, cssclass, rotation, scriptable.isEnabled(), openTag));
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
		if (!HtmlUtils.startsWithHtml(getDefaultModelObject()))
		{
			return WebBaseButton.getImageDisplayURL(this) != null;
		}
		return true;
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();

		if (getCSSId() != null)
		{
			WebCellBasedView wcbw = findParent(WebCellBasedView.class);
			if (wcbw != null) wcbw.addLabelCssClass(getId());
		}
		fireOnRender(false);
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

	public String getParameterValue(String param)
	{
		RequestCycle cycle = RequestCycle.get();
		if (cycle != null)
		{
			Request req = cycle.getRequest();
			if (req != null)
			{
				return req.getParameter(param);
			}
		}
		return null;
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

	public void setAnchors(int arg)
	{
		this.anchors = arg;
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}
}
