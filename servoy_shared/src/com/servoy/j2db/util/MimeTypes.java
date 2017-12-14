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
package com.servoy.j2db.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Helper class to get mimetype
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class MimeTypes
{
	public static String CSS = "text/css";

	public static String getContentType(byte[] data)
	{
		return getContentType(data, null);
	}

	public static String getContentType(byte[] data, String name)
	{
		if (data == null)
		{
			return null;
		}
		byte[] header = new byte[11];
		System.arraycopy(data, 0, header, 0, Math.min(data.length, header.length));
		int c1 = header[0] & 0xff;
		int c2 = header[1] & 0xff;
		int c3 = header[2] & 0xff;
		int c4 = header[3] & 0xff;
		int c5 = header[4] & 0xff;
		int c6 = header[5] & 0xff;
		int c7 = header[6] & 0xff;
		int c8 = header[7] & 0xff;
		int c9 = header[8] & 0xff;
		int c10 = header[9] & 0xff;
		int c11 = header[10] & 0xff;

		if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE)
		{
			return "application/java-vm";
		}

		if (c1 == 0xD0 && c2 == 0xCF && c3 == 0x11 && c4 == 0xE0 && c5 == 0xA1 && c6 == 0xB1 && c7 == 0x1A && c8 == 0xE1)
		{
			// if the name is set then check if it can be validated by name, because it could be a xls or powerpoint
			String contentType = guessContentTypeFromName(name);
			if (contentType != null)
			{
				return contentType;
			}
			return "application/msword";
		}
		if (c1 == 0x25 && c2 == 0x50 && c3 == 0x44 && c4 == 0x46 && c5 == 0x2d && c6 == 0x31 && c7 == 0x2e)
		{
			return "application/pdf";
		}

		if (c1 == 0x38 && c2 == 0x42 && c3 == 0x50 && c4 == 0x53 && c5 == 0x00 && c6 == 0x01)
		{
			return "image/photoshop";
		}

		if (c1 == 0x25 && c2 == 0x21 && c3 == 0x50 && c4 == 0x53)
		{
			return "application/postscript";
		}

		if (c1 == 0xff && c2 == 0xfb && c3 == 0x30)
		{
			return "audio/mp3";
		}

		if (c1 == 0x49 && c2 == 0x44 && c3 == 0x33)
		{
			return "audio/mp3";
		}

		if (c1 == 0xAC && c2 == 0xED)
		{
			// next two bytes are version number, currently 0x00 0x05
			return "application/x-java-serialized-object";
		}

		if (c1 == '<')
		{
			if (c2 == '!' ||
				((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' || c3 == 'e' && c4 == 'a' && c5 == 'd') ||
					(c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y'))) ||
				((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' || c3 == 'E' && c4 == 'A' && c5 == 'D') ||
					(c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y'))))
			{
				return "text/html";
			}

			if (c2 == '?' && c3 == 'x' && c4 == 'm' && c5 == 'l' && c6 == ' ')
			{
				return "application/xml";
			}
		}

		// big and little endian UTF-16 encodings, with byte order mark
		if (c1 == 0xfe && c2 == 0xff)
		{
			if (c3 == 0 && c4 == '<' && c5 == 0 && c6 == '?' && c7 == 0 && c8 == 'x')
			{
				return "application/xml";
			}
		}

		if (c1 == 0xff && c2 == 0xfe)
		{
			if (c3 == '<' && c4 == 0 && c5 == '?' && c6 == 0 && c7 == 'x' && c8 == 0)
			{
				return "application/xml";
			}
		}

		if (c1 == 'B' && c2 == 'M')
		{
			return "image/bmp";
		}

		if (c1 == 0x49 && c2 == 0x49 && c3 == 0x2a && c4 == 0x00)
		{
			return "image/tiff";
		}

		if (c1 == 0x4D && c2 == 0x4D && c3 == 0x00 && c4 == 0x2a)
		{
			return "image/tiff";
		}

		if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8')
		{
			return "image/gif";
		}

		if (c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f')
		{
			return "image/x-bitmap";
		}

		if (c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' && c5 == 'M' && c6 == '2')
		{
			return "image/x-pixmap";
		}

		if (c1 == 137 && c2 == 80 && c3 == 78 && c4 == 71 && c5 == 13 && c6 == 10 && c7 == 26 && c8 == 10)
		{
			return "image/png";
		}

		if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF)
		{
			if (c4 == 0xE0)
			{
				return "image/jpeg";
			}

			/**
			 * File format used by digital cameras to store images. Exif Format can be read by any application supporting JPEG. Exif Spec can be found at:
			 * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
			 */
			if ((c4 == 0xE1) && (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 == 'f' && c11 == 0))
			{
				return "image/jpeg";
			}

			if (c4 == 0xEE)
			{
				return "image/jpg";
			}
		}

		/**
		 * According to http://www.opendesign.com/files/guestdownloads/OpenDesign_Specification_for_.dwg_files.pdf
		 * first 6 bytes are of type "AC1018" (for example) and the next 5 bytes are 0x00.
		 */
		if ((c1 == 0x41 && c2 == 0x43) && (c7 == 0x00 && c8 == 0x00 && c9 == 0x00 && c10 == 0x00 && c11 == 0x00))
		{
			return "application/acad";
		}

		if (c1 == 0x2E && c2 == 0x73 && c3 == 0x6E && c4 == 0x64)
		{
			return "audio/basic"; // .au
			// format,
			// big
			// endian
		}

		if (c1 == 0x64 && c2 == 0x6E && c3 == 0x73 && c4 == 0x2E)
		{
			return "audio/basic"; // .au
			// format,
			// little
			// endian
		}

		if (c1 == 'R' && c2 == 'I' && c3 == 'F' && c4 == 'F')
		{
			/*
			 * I don't know if this is official but evidence suggests that .wav files start with "RIFF" - brown
			 */
			return "audio/x-wav";
		}

		if (c1 == 'P' && c2 == 'K')
		{
			// its application/zip but this could be a open office thing if name is given
			String contentType = guessContentTypeFromName(name);
			if (contentType != null)
			{
				return contentType;
			}
			return "application/zip";
		}
		return guessContentTypeFromName(name);
	}

	private static final Map<String, String> mimeTypes = new HashMap<String, String>();

	public static String guessContentTypeFromName(String name)
	{
		if (name == null) return null;

		int lastIndex = name.lastIndexOf('.');
		if (lastIndex != -1)
		{
			String extention = name.substring(lastIndex + 1).toLowerCase();
			if (mimeTypes.size() == 0)
			{
				HashMap<String, String> tempMap = new HashMap<String, String>();
				InputStream is = MimeTypes.class.getResourceAsStream("mime.types.properties");
				try
				{
					Properties properties = new Properties();
					properties.load(is);
					for (Object key : properties.keySet())
					{
						String property = properties.getProperty((String)key);
						StringTokenizer st = new StringTokenizer(property, " ");
						while (st.hasMoreTokens())
						{
							tempMap.put(st.nextToken(), (String)key);
						}
					}
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
				finally
				{
					try
					{
						is.close();
					}
					catch (IOException e)
					{
						Debug.error(e);
					}
				}
				synchronized (mimeTypes)
				{
					mimeTypes.putAll(tempMap);
				}
			}
			return mimeTypes.get(extention);
		}
		return null;
	}
}
