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
package com.servoy.j2db.server.headlessclient;

import java.awt.Dimension;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.DynamicWebResource;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Time;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.headlessclient.dataui.MediaResource;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;

/**
 * @author jcompagner
 * 
 */
@SuppressWarnings("nls")
public final class SharedMediaResource extends DynamicWebResource
{
	private static final long serialVersionUID = 1L;

	private Time time = Time.valueOf(System.currentTimeMillis());

	public SharedMediaResource()
	{
		setCacheable(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.markup.html.DynamicWebResource#setHeaders(org.apache.wicket.protocol.http.WebResponse)
	 */
	@Override
	protected void setHeaders(WebResponse response)
	{
		super.setHeaders(response);
		response.setHeader("Cache-Control", "public, max-age=" + getCacheDuration());
	}

	@Override
	protected ResourceState getResourceState()
	{
		final String iconId = getParameters().getString("id");
		final String solutionName = getParameters().getString("s");
		int mediaOptions = 0;
		int width = 0;
		int height = 0;
		try
		{
			mediaOptions = getParameters().getInt("option", 0);
			width = getParameters().getInt("w", 0);
			height = getParameters().getInt("h", 0);
		}
		catch (StringValueConversionException ex)
		{
			Debug.error(ex);
		}
		ResourceState rs = getResource(iconId, solutionName);
		if (rs != null && rs.getData() != null && rs.getData().length > 0 && mediaOptions != 0 && mediaOptions != 1 && width != 0 && height != 0)
		{
			MediaResource mr = new MediaResource(rs.getData(), mediaOptions, rs.lastModifiedTime());

			mr.checkResize(new Dimension(width, height));
			return mr.getResourceState();
		}
		return rs;
	}

	private ResourceState getResource(final String iconId, final String solutionName)
	{
		return new ResourceState()
		{
			private String contentType;
			private int length;
			byte[] array = null;

			@Override
			public Time lastModifiedTime()
			{
				try
				{
					IRootObject solution = ApplicationServerSingleton.get().getLocalRepository().getActiveRootObject(solutionName, IRepository.SOLUTIONS);
					if (solution != null) return Time.valueOf(solution.getLastModifiedTime());
				}
				catch (Exception e)
				{
					Debug.trace(e);
				}
				return time;
			}

			@Override
			public byte[] getData()
			{
				if (array == null)
				{
					boolean closeFS = false;
					try
					{
						final IRepository repository = ApplicationServerSingleton.get().getLocalRepository();
						FlattenedSolution fs = null;
						try
						{
							if (Session.exists() && ((WebClientSession)Session.get()).getWebClient() != null)
							{
								fs = ((WebClientSession)Session.get()).getWebClient().getFlattenedSolution();
							}

							if (fs == null)
							{
								closeFS = true;
								fs = new FlattenedSolution((SolutionMetaData)repository.getRootObjectMetaData(solutionName, IRepository.SOLUTIONS),
									new AbstractActiveSolutionHandler()
									{
										@Override
										public IRepository getRepository()
										{
											return repository;
										}

									});
							}

							Media m = fs.getMedia(iconId);
							if (m == null)
							{
								try
								{
									Integer iIconID = new Integer(iconId);
									m = fs.getMedia(iIconID.intValue());
								}
								catch (NumberFormatException ex)
								{
									Debug.error("no media found for: " + iconId);
								}
							}
							if (m != null)
							{
								array = m.getMediaData();
								contentType = m.getMimeType();
							}
						}
						finally
						{
							if (closeFS && fs != null)
							{
								fs.close(null);
							}
						}
						if (array != null)
						{
							if (contentType == null)
							{
								contentType = ImageLoader.getContentType(array);
							}
							length = array.length;
						}
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
				return array == null ? new byte[0] : array;
			}

			/**
			 * @see wicket.markup.html.DynamicWebResource.ResourceState#getLength()
			 */
			@Override
			public int getLength()
			{
				return length;
			}

			@Override
			public String getContentType()
			{
				return contentType;
			}
		};
	}

	public void touchTime()
	{
		time = Time.valueOf(System.currentTimeMillis());
	}
}