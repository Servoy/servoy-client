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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.text.html.CSS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.Media;

/**
 * Helper class to load image via ImageIO
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class ImageLoader
{
	private static final Logger log = LoggerFactory.getLogger(ImageLoader.class.getCanonicalName());

	public static ImageIcon getIcon(byte[] array, int width, int height, boolean keepAspect)
	{
		return getIcon(array, width, height, keepAspect, null);
	}

	public static ImageIcon getIcon(byte[] array, int width, int height, boolean keepAspect, Boolean fixedWidth)
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
		return resizeImageIcon(icon, width, height, keepAspect, fixedWidth);
	}

	public static ImageIcon resizeImageIcon(ImageIcon icon, int width, int height, boolean keepAspect)
	{
		return resizeImageIcon(icon, width, height, keepAspect, null);
	}

	public static ImageIcon resizeImageIcon(ImageIcon icon, int width, int height, boolean keepAspect, Boolean fixedWidth)
	{
		float widthChange = width > 0 ? icon.getIconWidth() / (float)width : 1;
		float heightChange = height > 0 ? icon.getIconHeight() / (float)height : 1;
		boolean resize = widthChange > 1.01 || heightChange > 1.01 || widthChange < 0.99 || heightChange < 0.99;
		if (resize)
		{
			if (keepAspect)
			{
				float ratio = 0;
				if (fixedWidth == null)
				{
					ratio = Math.max(widthChange, heightChange);
				}
				else if (fixedWidth.booleanValue())
				{
					ratio = widthChange;
				}
				else
				{
					ratio = heightChange;
				}

				width = (int)(icon.getIconWidth() / ratio);
				height = (int)(icon.getIconHeight() / ratio);
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
			log.error("Error reading image", e);
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

			log.error("Error getting image size", e);
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
				log.error("Error getting image size", e1);
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
		boolean imageIOCache = ImageIO.getUseCache();
		try
		{
			ImageIO.setUseCache(false); // this makes sure that it uses in memory, no temp file is created.
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

			log.error("Error getting image size", e);
		}
		finally
		{
			ImageIO.setUseCache(imageIOCache);
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
				log.error("Error getting image size", e1);
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
		return resize(imageData, width, height, aspect, null);
	}

	public static byte[] resize(byte[] imageData, int width, int height, boolean aspect, Boolean fixedWidth)
	{
		String contentType = MimeTypes.getContentType(imageData);
		if (contentType == null || contentType.toLowerCase().indexOf("gif") != -1) contentType = "image/png";
		ImageIcon icon = ImageLoader.getIcon(imageData, width, height, aspect, fixedWidth);
		if (icon == null)
		{
			return null;
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
			log.error("resizing images error", e);
		}
		if (baos.size() > 0)
		{
			return baos.toByteArray();
		}
		return null;
	}

	public static boolean imageHasAlpha(java.awt.Image image, long pixelGrabberTimeout)
	{
		if (image instanceof BufferedImage)
		{
			return ((BufferedImage)image).getColorModel().hasAlpha();
		}

		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try
		{
			pg.grabPixels(pixelGrabberTimeout);
		}
		catch (InterruptedException e)
		{
			return false;
		}
		return pg.getColorModel() != null && pg.getColorModel().hasAlpha();
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
		boolean hasAlpha = imageHasAlpha(image, 0);

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

	public static void paintImage(Graphics graphics, IStyleRule styleRule, IApplication application, Dimension parentSize)
	{
		if (styleRule != null && styleRule.hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString()))
		{
			String url = styleRule.getValue(CSS.Attribute.BACKGROUND_IMAGE.toString());
			int start = url.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (start != -1)
			{
				paintBackgroundImage(graphics, styleRule, application, url, parentSize);
			}
			else if (url.indexOf("linear-gradient") != -1 && graphics instanceof Graphics2D)
			{
				paintGradientColor(graphics, styleRule, application, url, parentSize);
			}
		}
	}

	private static void paintBackgroundImage(Graphics graphics, IStyleRule styleRule, IApplication application, String url, Dimension parentSize)
	{
		int start = url.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
		if (start != -1)
		{
			String name = url.substring(start + MediaURLStreamHandler.MEDIA_URL_DEF.length());
			if (name.endsWith("')") || name.endsWith("\")")) name = name.substring(0, name.length() - 2);
			if (name.endsWith(")")) name = name.substring(0, name.length() - 1);
			Media media = application.getFlattenedSolution().getMedia(name);
			if (media != null)
			{
				byte[] imageData = media.getMediaData();
				if (styleRule.hasAttribute(CSSName.BACKGROUND_SIZE.toString()))
				{
					PropertyDeclaration declaration = ((ServoyStyleRule)styleRule).getPropertyDeclaration(CSSName.BACKGROUND_SIZE.toString());
					if (declaration.getValue() instanceof PropertyValue && ((PropertyValue)declaration.getValue()).getValues() != null &&
						((PropertyValue)declaration.getValue()).getValues().size() == 2)
					{
						boolean autoWidth = "auto".equals(((CSSPrimitiveValue)((PropertyValue)declaration.getValue()).getValues().get(0)).getCssText());
						boolean autoHeight = "auto".equals(((CSSPrimitiveValue)((PropertyValue)declaration.getValue()).getValues().get(1)).getCssText());
						Boolean fixedWidth = null;
						if (autoWidth && !autoHeight)
						{
							fixedWidth = Boolean.FALSE;
						}
						else if (autoHeight && !autoWidth)
						{
							fixedWidth = Boolean.TRUE;
						}

						int width = getImageSize(parentSize.width,
							((CSSPrimitiveValue)((PropertyValue)declaration.getValue()).getValues().get(0)).getCssText());
						int height = getImageSize(parentSize.height,
							((CSSPrimitiveValue)((PropertyValue)declaration.getValue()).getValues().get(1)).getCssText());
						imageData = resize(imageData, width, height, autoWidth || autoHeight, fixedWidth);
					}
				}
				Image image = ImageLoader.getBufferedImage(imageData, -1, -1, false);
				int offsetWidth = 0;
				int offsetHeight = 0;
				int imageWidth = image.getWidth(null);
				int imageHeight = image.getHeight(null);

				if (styleRule.hasAttribute(CSSName.BACKGROUND_POSITION.toString()))
				{
					PropertyDeclaration declaration = ((ServoyStyleRule)styleRule).getPropertyDeclaration(CSSName.BACKGROUND_POSITION.toString());
					if (declaration.getValue() instanceof PropertyValue && ((PropertyValue)declaration.getValue()).getValues() != null &&
						((PropertyValue)declaration.getValue()).getValues().size() == 2)
					{
						offsetWidth = getImagePosition(parentSize.width, imageWidth,
							((CSSPrimitiveValue)((PropertyValue)declaration.getValue()).getValues().get(0)).getCssText());
						offsetHeight = getImagePosition(parentSize.height, imageHeight,
							((CSSPrimitiveValue)((PropertyValue)declaration.getValue()).getValues().get(1)).getCssText());
					}
				}
				boolean hRepeat = true;
				boolean vRepeat = true;
				if (styleRule.hasAttribute(CSSName.BACKGROUND_REPEAT.toString()))
				{
					String repeat = styleRule.getValue(CSSName.BACKGROUND_REPEAT.toString());
					hRepeat = false;
					vRepeat = false;
					if ("repeat".equals(repeat) || "repeat-x".equals(repeat)) hRepeat = true;
					if ("repeat".equals(repeat) || "repeat-y".equals(repeat)) vRepeat = true;
				}
				if (hRepeat)
				{
					offsetWidth = adjustRepeatCoordinate(offsetWidth, imageWidth);
				}
				if (vRepeat)
				{
					offsetHeight = adjustRepeatCoordinate(offsetHeight, imageHeight);
				}
				if (!hRepeat && !vRepeat)
				{
					graphics.drawImage(image, offsetWidth, offsetHeight, null);
				}
				else if (hRepeat && vRepeat)
				{
					for (int x = offsetWidth; x < parentSize.width; x += imageWidth)
					{
						for (int y = offsetHeight; y < parentSize.height; y += imageHeight)
						{
							graphics.drawImage(image, x, y, null);
						}
					}
				}
				else if (hRepeat)
				{
					for (int x = offsetWidth; x < parentSize.width; x += imageWidth)
					{
						graphics.drawImage(image, x, offsetHeight, null);
					}
				}
				else if (vRepeat)
				{
					for (int y = offsetHeight; y < parentSize.height; y += imageHeight)
					{
						graphics.drawImage(image, offsetWidth, y, null);
					}
				}
			}
		}
	}


	private static void paintGradientColor(Graphics graphics, IStyleRule styleRule, IApplication application, String url, Dimension parentSize)
	{
		if (url.indexOf("linear-gradient") != -1 && graphics instanceof Graphics2D)
		{
			List<String> tokens = PersistHelper.splitStringWithBracesOnSeparator(url.substring(url.indexOf("(") + 1, url.lastIndexOf(")")), ',');
			if (tokens.size() >= 2)
			{
				Iterator<String> tokenizer = tokens.iterator();
				String firstToken = tokenizer.next();
				float startX = parentSize.width / 2;
				float startY = 0;
				float endX = parentSize.width / 2;
				float endY = parentSize.height;
				Color color1 = getColor(firstToken);
				if (color1 == null)
				{
					if ("left".equals(firstToken))
					{
						startX = 0;
						startY = parentSize.height;
						endX = parentSize.width;
					}
					else if ("right".equals(firstToken))
					{
						startX = parentSize.width;
						endX = 0;
						endY = 0;
					}
					else if ("bottom".equals(firstToken))
					{
						startY = parentSize.height;
						endY = 0;
					}
					color1 = getColor(tokenizer.next());
				}
				Color color2 = null;
				if (color1 != null)
				{
					color2 = getColor(tokenizer.next());
					if (color2 != null)
					{
						GradientPaint gradientPaint = new GradientPaint(startX, startY, color1, endX, endY, color2);
						Paint tmpPaint = ((Graphics2D)graphics).getPaint();
						((Graphics2D)graphics).setPaint(gradientPaint);
						graphics.fillRect(0, 0, parentSize.width, parentSize.height);
						((Graphics2D)graphics).setPaint(tmpPaint);
					}
				}
				if (color1 == null || color2 == null)
				{
					// fallback mechanism
					String[] values = styleRule.getValues(CSS.Attribute.BACKGROUND_IMAGE.toString());
					if (values.length > 1)
					{
						for (int i = 1; i < values.length; i++)
						{
							if (values[i].equals(url))
							{
								if (values[i - 1].indexOf("linear-gradient") != -1)
								{
									paintGradientColor(graphics, styleRule, application, values[i - 1], parentSize);
								}
								else
								{
									paintBackgroundImage(graphics, styleRule, application, values[i - 1], parentSize);
								}
							}
						}
					}
				}
			}
		}
	}

	private static int getImageSize(int containerSize, String cssText)
	{
		if (cssText != null)
		{
			if (cssText.endsWith("px"))
			{
				return Utils.getAsInteger(cssText.substring(0, cssText.length() - 2));
			}
			else if (cssText.endsWith("%"))
			{
				int percent = Utils.getAsInteger(cssText.substring(0, cssText.length() - 1));
				return percent * containerSize / 100;
			}
		}
		return -1;
	}

	private static int getImagePosition(int containerSize, int imageSize, String cssText)
	{
		if (cssText != null)
		{
			if (cssText.endsWith("px"))
			{
				return Utils.getAsInteger(cssText.substring(0, cssText.length() - 2));
			}
			else if (cssText.endsWith("%"))
			{
				int percent = Utils.getAsInteger(cssText.substring(0, cssText.length() - 1));
				return percent * (containerSize - imageSize) / 100;
			}
			else if ("left".equals(cssText) || "top".equals(cssText))
			{
				return 0;
			}
			else if ("right".equals(cssText) || "bottom".equals(cssText))
			{
				return (containerSize - imageSize);
			}
			else if ("center".equals(cssText))
			{
				return (containerSize - imageSize) / 2;
			}
		}
		return 0;
	}

	private static int adjustRepeatCoordinate(int startPosition, int imageSize)
	{
		int result = startPosition;
		while (result > 0)
		{
			result = result - imageSize;
		}
		return result;
	}

	private static Color getColor(String cssDefinition)
	{
		if (cssDefinition != null)
		{
			return PersistHelper.createColorWithTransparencySupport(cssDefinition);
		}
		return null;
	}
}
