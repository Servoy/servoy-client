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
import java.util.StringTokenizer;

import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.SQLSheet;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * Class to handle the media:///<name> or media:///blob_loader?x=y urls
 * 
 * @author Jan Blok
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

	public Media getMedia(String name, IServiceProvider application)
	{
		if (application == null)
		{
			return null;
		}
		FlattenedSolution s = application.getFlattenedSolution();
		if (s != null)
		{
			return s.getMedia(name);
		}
		return null;
	}

	public static byte[] getBlobLoaderMedia(IServiceProvider application, String urlQueryPart) throws IOException
	{
		if (application.getSolution() == null) return null;//cannot work without a solution

		String serverName = null;
		String table = null;
		try
		{
			if (urlQueryPart == null) return null;

			String dataProvider = null;
			SafeArrayList pks = new SafeArrayList();
			StringTokenizer tk = new StringTokenizer(urlQueryPart, "?&"); //$NON-NLS-1$
			while (tk.hasMoreTokens())
			{
				String token = tk.nextToken();
				if (token.startsWith("global=")) //$NON-NLS-1$
				{
					String globalName = token.substring("global=".length()); //$NON-NLS-1$
					Object obj = application.getScriptEngine().getGlobalScope().get(globalName);
					if (obj instanceof byte[])
					{
						return (byte[])obj;
					}
					else if (obj instanceof String)
					{
						// TODO check can we always just convert to the default encoding of this machine (server if web)
						return ((String)obj).getBytes();
					}
					return null;
				}
				else if (token.startsWith("servername=")) //$NON-NLS-1$
				{
					serverName = token.substring("servername=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("tablename=")) //$NON-NLS-1$
				{
					table = token.substring("tablename=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("datasource=")) //$NON-NLS-1$
				{
					String datasource = token.substring("datasource=".length()); //$NON-NLS-1$
					String[] serverAndTableNames = DataSourceUtils.getDBServernameTablename(datasource);
					if (serverAndTableNames != null)
					{
						serverName = serverAndTableNames[0];
						table = serverAndTableNames[1];
					}
				}
				else if (token.startsWith("dataprovider=")) //$NON-NLS-1$
				{
					dataProvider = token.substring("dataprovider=".length()); //$NON-NLS-1$
				}
				else if (token.startsWith("mimedataprovider=")) //$NON-NLS-1$
				{
					//mimedataProvider = token.substring("mimedataprovider=".length()); //$NON-NLS-1$
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
			IServer server = application.getSolution().getServer(serverName);
			if (server != null)
			{
				ITable tableObj = server.getTable(table);
				if (tableObj != null)
				{
					IFoundSetInternal fs = application.getFoundSetManager().getNewFoundSet(tableObj, null);
					if (fs == null) return null;

					((FoundSet)fs).setFindMode();
					IRecordInternal frec = fs.getRecord(0);
					SQLSheet sheet = fs.getSQLSheet();
					String[] pkColumns = sheet.getPKColumnDataProvidersAsArray();
					for (int j = 0; j < Math.min(pkColumns.length, pks.size()); j++)
					{
						frec.setValue(pkColumns[j], pks.get(j));
					}
					//TODO: we should optimize this in foundset if is pk exact query, and retrieve from row cache directly (without doing a pk query)
					int count = ((FoundSet)fs).performFind(true, true, true);
					if (count > 0)
					{
						IRecordInternal rec = fs.getRecord(0);
						Object blob_value = rec.getValue(dataProvider);
						if (blob_value instanceof byte[])
						{
							return (byte[])blob_value;
						}
						else if (blob_value instanceof String)
						{
							// TODO check can we always just convert to the default encoding of this machine (server if web)
							return ((String)blob_value).getBytes();
						}
					}
				}
				return null;
			}
		}
		catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}
		throw new IOException(Messages.getString("servoy.exception.serverAndTableNotFound", new String[] { serverName, table })); //$NON-NLS-1$		
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
}
