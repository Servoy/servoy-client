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
package com.servoy.j2db;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;

import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * Class to handle the media:///<name> or media:///blob_loader?x=y urls
 * 
 * @author jblok
 */
public class MediaURLStreamHandler extends URLStreamHandler
{
	public static final String MEDIA_URL_DEF = "media:///"; //$NON-NLS-1$
	public static final String MEDIA_URL_BLOBLOADER = "servoy_blobloader"; //$NON-NLS-1$

	final IServiceProvider application;

	public MediaURLStreamHandler(IServiceProvider application)
	{
		this.application = application;
	}

	public MediaURLStreamHandler()
	{
		this.application = null;
	}

	@Override
	public URLConnection openConnection(URL u) throws IOException
	{
		final IServiceProvider app = this.application == null ? J2DBGlobals.getServiceProvider() : this.application;
		String fname = u.getFile();
		if (fname.startsWith("/")) //$NON-NLS-1$
		{
			fname = fname.substring(1);
		}
		final String filename = fname;
		if (filename.startsWith(MEDIA_URL_BLOBLOADER))
		{
			URLConnection urlc = new URLConnection(u)
			{
				private byte[] array;
				private String mimeType = "application/binary"; //$NON-NLS-1$

				@Override
				public void connect() throws IOException
				{
					if (url != null)
					{
						array = getBlobLoaderMedia(app, url.getQuery());
						connected = array != null;
						mimeType = getBlobLoaderMimeType(url.getQuery());
					}
				}

				@Override
				public String getHeaderField(String name)
				{
					if ("content-length".equals(name)) //$NON-NLS-1$
					{
						if (array == null)
						{
							return "0"; //$NON-NLS-1$
						}
						else
						{
							return Integer.toString(array.length);
						}
					}
					else if ("content-type".equals(name)) //$NON-NLS-1$
					{
						if (mimeType == null && array != null)
						{
							return ImageLoader.getContentType(array);
						}
						else
						{
							return mimeType;
						}
					}
					return super.getHeaderField(name);
				}

				@Override
				public InputStream getInputStream() throws IOException
				{
					if (!connected) connect();

					if (array != null)
					{
						ByteArrayInputStream bais = new ByteArrayInputStream(array);
						return bais;
					}
					else
					{
						return new InputStream()
						{
							@Override
							public int available() throws IOException
							{
								return 0;
							}

							@Override
							public int read() throws IOException
							{
								return -1;
							}
						};
					}
				}

				@Override
				public String toString()
				{
					return "MediaURL:" + url; //$NON-NLS-1$
				}
			};
			return urlc;
		}
		else
		{
			URLConnection urlc = new URLConnection(u)
			{
				private Media m;
				private byte[] array;

				@Override
				public void connect() throws IOException
				{

					m = getMedia(filename, app);
					if (m != null)
					{
						array = m.getMediaData();
						connected = true;
					}
				}

				@Override
				public String getHeaderField(String name)
				{
					if ("content-length".equals(name)) //$NON-NLS-1$
					{
						if (array == null)
						{
							return "0"; //$NON-NLS-1$
						}
						else
						{
							return Integer.toString(array.length);
						}
					}
					else if ("content-type".equals(name)) //$NON-NLS-1$
					{
						if (m == null)
						{
							return null;
						}
						else
						{
							return m.getMimeType();
						}
					}
					return super.getHeaderField(name);
				}

				@Override
				public InputStream getInputStream() throws IOException
				{
					if (!connected) connect();

					if (array != null)
					{
						ByteArrayInputStream bais = new ByteArrayInputStream(array);
						return bais;
					}
					else
					{
						return new InputStream()
						{
							@Override
							public int available() throws IOException
							{
								return 0;
							}

							@Override
							public int read() throws IOException
							{
								return -1;
							}
						};
					}
				}

				@Override
				public String toString()
				{
					return "MediaURL:" + url; //$NON-NLS-1$
				}
			};
			return urlc;
		}
	}

	public Media getMedia(String name, IServiceProvider app)
	{
		if (app == null)
		{
			return null;
		}
		FlattenedSolution s = app.getFlattenedSolution();
		if (s != null)
		{
			return s.getMedia(name);
		}
		return null;
	}

	public static byte[] getBlobLoaderMedia(IServiceProvider application, String urlQueryPart) throws IOException
	{
		if (application.getSolution() == null) return null;//cannot work without a solution

		String datasource = null;
		String serverName = null;
		String tableName = null;
		try
		{
			if (urlQueryPart == null) return null;

			String dataProvider = null;
			List<String> pks = new SafeArrayList<String>();
			StringTokenizer tk = new StringTokenizer(urlQueryPart, "?&"); //$NON-NLS-1$
			while (tk.hasMoreTokens())
			{
				String token = tk.nextToken();
				if (token.startsWith("global=")) //$NON-NLS-1$
				{
					String globalName = token.substring("global=".length()); //$NON-NLS-1$
					Object obj = application.getScriptEngine().getScopesScope().get(null, globalName);
					if (obj instanceof byte[])
					{
						return (byte[])obj;
					}
					if (obj instanceof String)
					{
						// TODO check can we always just convert to the default encoding of this machine (server if web)
						return ((String)obj).getBytes();
					}
					return null;
				}

				if (token.startsWith("servername=")) //$NON-NLS-1$
				{
					serverName = token.substring("servername=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("tablename=")) //$NON-NLS-1$
				{
					tableName = token.substring("tablename=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("datasource=")) //$NON-NLS-1$
				{
					datasource = token.substring("datasource=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("dataprovider=")) //$NON-NLS-1$
				{
					dataProvider = token.substring("dataprovider=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("rowid")) //$NON-NLS-1$
				{
					int index = Utils.getAsInteger(token.substring(5, 6));//get id
					if (index > 0)
					{
						pks.add(index - 1, token.substring(7));//get value after 'rowidX='
					}
					else
					{
						pks.add(0, token.substring(6));//get value after 'rowid='
					}
				}
			}
			if (datasource == null && serverName != null && tableName != null)
			{
				datasource = DataSourceUtils.createDBTableDataSource(serverName, tableName);
			}

			if (application.getFoundSetManager().getTable(datasource) == null)
			{
				throw new IOException(Messages.getString("servoy.exception.serverAndTableNotFound", DataSourceUtils.getDBServernameTablename(datasource))); //$NON-NLS-1$	
			}
			FoundSet fs = (FoundSet)application.getFoundSetManager().getNewFoundSet(datasource);

			List<Object[]> rows = new ArrayList<Object[]>(1); // use mutable list here, elements are overwritten with Column.getAsRightType equivalent
			rows.add(pks.toArray());
			if (!fs.loadExternalPKList(new BufferedDataSet(null, null, rows)))
			{
				return null;
			}
			IRecordInternal rec = fs.getRecord(0);
			if (rec == null)
			{
				return null;
			}
			Object blob_value = rec.getValue(dataProvider);
			if (blob_value instanceof byte[])
			{
				return (byte[])blob_value;
			}
			if (blob_value instanceof String)
			{
				// TODO check can we always just convert to the default encoding of this machine (server if web)
				return ((String)blob_value).getBytes();
			}
		}
		catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}

		return null;
	}

	public static String getBlobLoaderMimeType(String urlQueryPart)
	{
		if (urlQueryPart == null) return null;

		StringTokenizer tk = new StringTokenizer(urlQueryPart, "?&"); //$NON-NLS-1$
		while (tk.hasMoreTokens())
		{
			String token = tk.nextToken();
			if (token.startsWith("mimetype=")) //$NON-NLS-1$
			{
				return token.substring("mimetype=".length()); //$NON-NLS-1$
			}
		}
		return null;
	}

	/**
	 * @param url
	 * @return
	 */
	public static String getBlobLoaderFileName(String urlQueryPart)
	{
		if (urlQueryPart == null) return null;

		StringTokenizer tk = new StringTokenizer(urlQueryPart, "?&"); //$NON-NLS-1$
		while (tk.hasMoreTokens())
		{
			String token = tk.nextToken();
			if (token.startsWith("filename=")) //$NON-NLS-1$
			{
				return token.substring("filename=".length()); //$NON-NLS-1$
			}
		}
		return null;
	}

	/***
	 *   gets translated relative URL from media:///name to  resources/servoy/media?s=sol_name&id=name
	 *   used in web client
	 * @param solution
	 * @param url
	 * @return
	 */
	public static String getTranslatedMediaURL(FlattenedSolution solution, String url)
	{
		ResourceReference rr = new ResourceReference("media"); //$NON-NLS-1$
		String lowercase = url.toLowerCase();
		if (lowercase.startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
		{
			String name = url.substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
			Media media = solution.getMedia(name);
			if (media != null)
			{
				return RequestCycle.get().urlFor(rr) + "?id=" + media.getName() + "&s=" + solution.getSolution().getName();
			}
		}
		return null;
	}

}
