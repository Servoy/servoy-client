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

import java.awt.Dimension;

import org.apache.wicket.markup.html.DynamicWebResource;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Time;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.ImageLoader;

/**
 * A {@link DynamicWebResource} that will or can resize itself based on the {@link Dimension} it gets.
 *
 * @author jcompagner
 */
public final class MediaResource extends DynamicWebResource
{
	/**
	 * @author jcompagner
	 *
	 */
	private final class MediaResourceState extends ResourceState
	{
		/**
		 * @param lastModifiedTime
		 */
		public MediaResourceState(Time lastModifiedTime)
		{
			this.lastModifiedTime = lastModifiedTime;
		}

		/**
		 * @see wicket.resource.DynamicByteArrayResource.ResourceState#getData()
		 */
		@Override
		public byte[] getData()
		{
			return resized == null ? bs : resized;
		}

		/**
		 * @see wicket.resource.DynamicByteArrayResource.ResourceState#getContentType()
		 */
		@Override
		public String getContentType()
		{
			return contentType;
		}

		/**
		 * @see wicket.resource.DynamicByteArrayResource.ResourceState#getLength()
		 */
		@Override
		public int getLength()
		{
			return getData().length;
		}
	}

	private static final long serialVersionUID = 1L;

	private boolean reduce;
	private boolean enlarge;
	private boolean keepAspect;

	byte[] resized;
	private Dimension resizedSize;

	private final byte[] bs;

	String contentType;

	private Dimension succededScaledDimension;

	private double iconWidth;

	private double iconHeight;

	private final Time lastModifiedTime;

	MediaResource()
	{
		bs = new byte[0];
		lastModifiedTime = null;
	}

	public MediaResource(byte[] bs, int mediaOptions)
	{
		this(bs, mediaOptions, null);
	}

	public MediaResource(byte[] bs, int mediaOptions, Time lastModifiedTime)
	{
		this.lastModifiedTime = lastModifiedTime;
		this.bs = bs == null ? new byte[0] : bs;
		this.succededScaledDimension = this.resizedSize = ImageLoader.getSize(this.bs);
		this.iconWidth = succededScaledDimension.getWidth();
		this.iconHeight = succededScaledDimension.getHeight();
		this.reduce = (mediaOptions & 2) == 2;
		this.enlarge = (mediaOptions & 4) == 4;
		this.keepAspect = (mediaOptions & 8) == 8;
		this.contentType = ImageLoader.getContentType(bs);

		this.setCacheable(true);

	}

	@Override
	public ResourceState getResourceState()
	{
		int mediaOptions = 0;
		int width = 0;
		int height = 0;
		try
		{
			mediaOptions = getParameters().getInt("option", 0); //$NON-NLS-1$
			width = getParameters().getInt("w", 0); //$NON-NLS-1$
			height = getParameters().getInt("h", 0); //$NON-NLS-1$
		}
		catch (StringValueConversionException ex)
		{
			Debug.error(ex);
		}
		if (mediaOptions != 0 && mediaOptions != 1 && width != 0 && height != 0)
		{
			checkResize(new Dimension(width, height));
		}
		return new MediaResourceState(lastModifiedTime);
	}


	public String getContentType()
	{
		return contentType;
	}

	public void checkResize(Dimension size)
	{
		boolean resize = succededScaledDimension == null ? true : !succededScaledDimension.equals(size);
		double widthChange = 0;
		double heightChange = 0;

		double width = size.getWidth();
		double height = size.getHeight();

		// if it doesn't fit then resize == true
		// check then if we have to,or can, resize the original.
		if (resize)
		{
			succededScaledDimension = size;
			widthChange = iconWidth / width;
			heightChange = iconHeight / height;
			resize = widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99;
			if (resize)
			{
				if (!(reduce && enlarge))
				{
					if (reduce)
					{
						if (widthChange < 0.99 && heightChange < 0.99)
						{
							// may only reduce, so no resize
							resize = false;
						}
						else if (widthChange > 1.01 && heightChange > 1.01)
						{
							if (keepAspect)
							{
								if (widthChange > heightChange)
								{
									width = (int)(iconWidth / widthChange);
									height = (int)(iconHeight / widthChange);
								}
								else
								{
									width = (int)(iconWidth / heightChange);
									height = (int)(iconHeight / heightChange);
								}
							}
						}
						else if (widthChange > 1.01)
						{
							width = (int)(iconWidth / widthChange);
							if (keepAspect)
							{
								height = (int)(iconHeight / widthChange);
							}
						}
						else if (heightChange > 1.01)
						{
							height = (int)(iconHeight / heightChange);
							if (keepAspect)
							{
								width = (int)(iconWidth / heightChange);
							}
						}
					}
					else if (enlarge)
					{
						if (widthChange > 1.01 && heightChange > 1.01)
						{
							// may only reduce, so no resize
							resize = false;
						}
						else if (widthChange < 0.99 && heightChange < 0.99)
						{
							if (keepAspect)
							{
								if (widthChange > heightChange)
								{
									width = (int)(iconWidth / widthChange);
									height = (int)(iconHeight / widthChange);
								}
								else
								{
									width = (int)(iconWidth / heightChange);
									height = (int)(iconHeight / heightChange);
								}
							}
						}
						else if (widthChange < 0.99)
						{
							width = (int)(iconWidth / widthChange);
							if (keepAspect)
							{
								height = (int)(iconHeight / widthChange);
							}
						}
						else if (heightChange < 0.99)
						{
							height = (int)(iconHeight / heightChange);
							if (keepAspect)
							{
								width = (int)(iconWidth / heightChange);
							}
						}
					}
					else
					{
						resize = false;
					}
				}
				else
				{
					if (keepAspect)
					{
						if (widthChange > heightChange)
						{
							width = (int)(iconWidth / widthChange);
							height = (int)(iconHeight / widthChange);
						}
						else
						{
							width = (int)(iconWidth / heightChange);
							height = (int)(iconHeight / heightChange);
						}
					}
				}
			}
		}
		if (resize)
		{
			resized = ImageLoader.resize(this.bs, width <= 0 ? 1 : (int)width, height <= 0 ? 1 : (int)height, keepAspect);
		}

		if (resized == null)
		{
			resized = bs;
		}
		resizedSize = ImageLoader.getSize(resized);
		contentType = ImageLoader.getContentType(resized);
		return;
	}

	/**
	 * @return
	 */
	public int getWidth()
	{
		return resizedSize == null ? 0 : resizedSize.width;
	}

	/**
	 * @return
	 */
	public int getHeight()
	{
		return resizedSize == null ? 0 : resizedSize.height;
	}

	/**
	 * @see wicket.markup.html.WebResource#setHeaders(wicket.protocol.http.WebResponse)
	 */
	@Override
	protected void setHeaders(WebResponse response)
	{
		super.setHeaders(response);
		if (!isCacheable())
		{
			HTTPUtils.setNoCacheHeaders(response.getHttpServletResponse());
//			response.setHeader("Cache-Control", "no-cache");
//			response.setHeader("Pragma", "no-cache");
//			response.setDateHeader("Expires", -1);
		}

	}

	public byte[] getRawData()
	{
		return bs;
	}
}