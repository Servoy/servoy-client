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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

/**
 * Helper class to load image via ImageIO
 * 
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class ImageLoader
{

	public static String getContentType(byte[] imageData)
	{
		return getContentType(imageData, null);
	}

	public static String getContentType(byte[] imageData, String name)
	{
		if (imageData == null)
		{
			return null;
		}
		byte[] header = new byte[11];
		System.arraycopy(imageData, 0, header, 0, Math.min(imageData.length, header.length));
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
			String contentType = contentTypeByName(name);
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
				((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' || c3 == 'e' && c4 == 'a' && c5 == 'd') || (c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y'))) ||
				((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' || c3 == 'E' && c4 == 'A' && c5 == 'D') || (c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y'))))
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
			String contentType = contentTypeByName(name);
			if (contentType != null)
			{
				return contentType;
			}
			return "application/zip";
		}
		return contentTypeByName(name);
	}

	private static final Map<String, String> mimeTypes = new HashMap<String, String>();

	private static String contentTypeByName(String name)
	{
		if (name == null) return null;

		int lastIndex = name.lastIndexOf('.');
		if (lastIndex != -1)
		{
			String extention = name.substring(lastIndex + 1).toLowerCase();
			if (mimeTypes.size() == 0)
			{
				HashMap<String, String> tempMap = new HashMap<String, String>();
				InputStream is = ImageLoader.class.getResourceAsStream("mime.types.properties");
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

	public static ImageIcon getIcon(byte[] array, int width, int height, boolean keepAspect)
	{
		if (array == null || array.length == 0) return null;
		ImageIcon icon = null;
		if (array.length < 150000)
		{
			// this method is faster for smaller images
			icon = new ImageIcon(array);
			if (icon.getIconHeight() == -1) // icon creation failed.
			{
				// Try it through the imageio way
				Image image = getBufferedImage(array, width, height, keepAspect);
				if (image != null)
				{
					icon = new ImageIcon(image);
				}
				else return null;
			}
		}
		else
		{
			Image image = getBufferedImage(array, width, height, keepAspect);
			if (image != null)
			{
				icon = new ImageIcon(image);
			}
			else
			{
				// try the backup, slow way for large images.
				icon = new ImageIcon(array);
				if (icon.getIconHeight() == -1) // icon creation failed.
				{
					return null;
				}
			}
		}
		return resizeImageIcon(icon, width, height, keepAspect);
	}

	public static ImageIcon resizeImageIcon(ImageIcon icon, int width, int height, boolean keepAspect)
	{
		float widthChange = width > 0 ? icon.getIconWidth() / (float)width : 1;
		float heightChange = height > 0 ? icon.getIconHeight() / (float)height : 1;
		boolean resize = widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99;
		if (resize)
		{
			if (keepAspect)
			{
				if (widthChange > heightChange)
				{
					width = (int)(icon.getIconWidth() / widthChange);
					height = (int)(icon.getIconHeight() / widthChange);
				}
				else
				{
					width = (int)(icon.getIconWidth() / heightChange);
					height = (int)(icon.getIconHeight() / heightChange);
				}
			}
			if (width > 0 && height > 0)
			{
				icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
			}
		}
		return icon;
	}

	public static BufferedImage getBufferedImage(byte[] array, int width, int height, boolean keepAspect)
	{
		BufferedImage bi = null;

		ByteArrayInputStream bais = new ByteArrayInputStream(array);
		ImageInputStream iis = null;
		ImageReader ir = null;
		try
		{
			iis = ImageIO.createImageInputStream(bais);
			Iterator it = ImageIO.getImageReaders(iis);
			if (it.hasNext())
			{

				ir = (ImageReader)it.next();
				ir.setInput(iis, true, true);
				float fWidth = 0, fHeigth = 0;
				if (width != -1) fWidth = ((float)ir.getWidth(0)) / width;
				if (height != -1) fHeigth = ((float)ir.getHeight(0)) / height;

				ImageReadParam param = ir.getDefaultReadParam();

				// Workaround for quality loss when decoding JPEG in Java 5.
				// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372769 for details.
				// JPEG quality is not lost in Java 1.4 and Java 6, just in Java 5.
				Iterator imageTypes = ir.getImageTypes(0);
				boolean looking = true;
				while (looking && imageTypes.hasNext())
				{
					ImageTypeSpecifier type = (ImageTypeSpecifier)imageTypes.next();
					ColorSpace cs = type.getColorModel().getColorSpace();
					if (cs.isCS_sRGB())
					{
						param.setDestinationType(type);
						looking = false;
					}
				}

				if (fWidth != 0 || fHeigth != 0)
				{
					if (keepAspect)
					{
						int min = (int)Math.min(fWidth, fHeigth);
						if (min == 0) min = (int)Math.max(fWidth, fHeigth);
						if (min > 0)
						{
							param.setSourceSubsampling(min, min, 0, 0);
						}
					}
					else
					{
						int iWidth = (int)fWidth;
						if (iWidth == 0) iWidth = 1;
						int iHeigth = (int)fHeigth;
						if (iHeigth == 0) iHeigth = 1;
						param.setSourceSubsampling(iWidth, iHeigth, 0, 0);
					}
				}
				bi = ir.read(0, param);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			if (ir != null)
			{
				ir.dispose();
			}
			try
			{
				if (iis != null)
				{
					iis.close();
				}
			}
			catch (Exception e1)
			{
			}
		}
		return bi;
	}

	/**
	 * @param iconArray
	 * @return the dimensions of the image, (0,0) if the byte array is null
	 */
	public static Dimension getSize(File icon)
	{
		if (icon == null) return new Dimension(0, 0);
		ImageInputStream iis = null;
		ImageReader ir = null;
		try
		{
			iis = ImageIO.createImageInputStream(icon);
			Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
			if (it.hasNext())
			{

				ir = it.next();
				ir.setInput(iis, true, true);
				return new Dimension(ir.getWidth(0), ir.getHeight(0));
			}
		}
		catch (Exception e)
		{
			try
			{
				ImageIcon image = new ImageIcon(Utils.readFile(icon, -1));
				return new Dimension(image.getIconWidth(), image.getIconHeight());
			}
			catch (Exception ex)
			{
				// ignore
			}

			Debug.error(e);
		}
		finally
		{
			if (ir != null)
			{
				ir.dispose();
			}
			try
			{
				if (iis != null) iis.close();
			}
			catch (Exception e1)
			{
				Debug.error(e1);
			}
		}
		return new Dimension(0, 0);
	}

	/**
	 * @param iconArray
	 * @return the dimensions of the image, (0,0) if the byte array is null
	 */
	public static Dimension getSize(byte[] iconArray)
	{
		if (iconArray == null) return new Dimension(0, 0);
		ByteArrayInputStream bais = new ByteArrayInputStream(iconArray);
		ImageInputStream iis = null;
		ImageReader ir = null;
		try
		{
			iis = ImageIO.createImageInputStream(bais);
			Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
			if (it.hasNext())
			{

				ir = it.next();
				ir.setInput(iis, true, true);
				return new Dimension(ir.getWidth(0), ir.getHeight(0));
			}
		}
		catch (Exception e)
		{
			try
			{
				ImageIcon icon = new ImageIcon(iconArray);
				return new Dimension(icon.getIconWidth(), icon.getIconHeight());
			}
			catch (Exception ex)
			{
				// ignore
			}

			Debug.error(e);
		}
		finally
		{
			if (ir != null)
			{
				ir.dispose();
			}
			try
			{
				if (iis != null) iis.close();
			}
			catch (Exception e1)
			{
				Debug.error(e1);
			}
		}
		return new Dimension(0, 0);
	}

	public static byte[] resize(byte[] imageData, int width, int height)
	{
		return resize(imageData, width, height, true);
	}

	public static byte[] resize(byte[] imageData, int width, int height, boolean aspect)
	{
		String contentType = ImageLoader.getContentType(imageData);
		if (contentType == null || contentType.toLowerCase().indexOf("gif") != -1) contentType = "image/png";
		ImageIcon icon = ImageLoader.getIcon(imageData, width, height, aspect);
		if (icon == null)
		{
			Debug.warn("Cannot resize image, returning original image; check the log for errors");
			return imageData;
		}
		java.awt.Image image = icon.getImage();
		BufferedImage bufImage = null;
		if (image instanceof BufferedImage)
		{
			bufImage = (BufferedImage)image;
		}
		else
		{
			int type = BufferedImage.TYPE_INT_RGB;
			if (contentType.equals("image/png"))
			{
				type = BufferedImage.TYPE_INT_ARGB_PRE;
			}
			else if (contentType.toLowerCase().indexOf("gif") != -1)
			{
				type = BufferedImage.TYPE_BYTE_INDEXED;
			}

			bufImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), type);
			Graphics g2 = bufImage.createGraphics();
			g2.drawImage(image, 0, 0, null);
			g2.dispose();
		}

		try
		{
			return getByteArray(contentType, bufImage);
		}
		finally
		{
			bufImage.flush();
		}
	}

	public static byte[] getByteArray(String contentType, RenderedImage bufImage)
	{
		byte[] resized = null;
		Iterator iterator = ImageIO.getImageWritersByMIMEType(contentType);
		if (iterator.hasNext())
		{
			ImageWriter iw = (ImageWriter)iterator.next();
			resized = writeImage(iw, bufImage);
			iw.dispose();
		}
		if (resized == null)
		{
			if ("image/bmp".equals(contentType))
			{
				iterator = ImageIO.getImageWritersByMIMEType("image/bmp, image/x-bmp, image/x-windows-bmp");
				if (iterator.hasNext())
				{
					ImageWriter iw = (ImageWriter)iterator.next();
					resized = writeImage(iw, bufImage);
					iw.dispose();
				}
			}
			if (resized == null)
			{
				iterator = ImageIO.getImageWritersByMIMEType("image/png");
				if (iterator.hasNext())
				{
					ImageWriter iw = (ImageWriter)iterator.next();
					resized = writeImage(iw, bufImage);
					iw.dispose();
				}
				if (resized == null)
				{
					iterator = ImageIO.getImageWritersByMIMEType("image/jpeg");
					if (iterator.hasNext())
					{
						ImageWriter iw = (ImageWriter)iterator.next();
						resized = writeImage(iw, bufImage);
						iw.dispose();
					}
				}
			}
		}
		return resized;
	}

	private static byte[] writeImage(ImageWriter iw, RenderedImage bufImage)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
		try
		{
			ImageOutputStream os = ImageIO.createImageOutputStream(baos);
			iw.setOutput(os);
			iw.write(bufImage);
			os.close();
		}
		catch (IOException e)
		{
			Debug.error("resizing images error", e);
		}
		if (baos.size() > 0)
		{
			return baos.toByteArray();
		}
		return null;
	}


	// This method returns a buffered image with the contents of an image
	public static BufferedImage imageToBufferedImage(Image image)
	{
		if (image instanceof BufferedImage)
		{
			return (BufferedImage)image;
		}

		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();

		// Determine if the image has transparent pixels
		boolean hasAlpha = imageHasAlpha(image);

		BufferedImage bimage = null;


		// Create a buffered image
		int width = image.getWidth(null);
		width = width > 0 ? width : 1;
		int height = image.getHeight(null);
		height = height > 0 ? height : 1;
		bimage = new BufferedImage(width, height, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

		// Copy image to buffered image
		Graphics g = bimage.createGraphics();

		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bimage;
	}


	// This method returns true if the specified image has transparent pixels
	public static boolean imageHasAlpha(Image image)
	{
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage)
		{
			BufferedImage bimage = (BufferedImage)image;
			return bimage.getColorModel().hasAlpha();
		}

		// Use a pixel grabber to retrieve the image's color model;
		// grabbing a single pixel is usually sufficient
		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try
		{
			pg.grabPixels();
		}
		catch (InterruptedException e)
		{
			Debug.error(e);
		}

		// Get the image's color model
		ColorModel cm = pg.getColorModel();

		return cm != null ? cm.hasAlpha() : false;
	}


}
