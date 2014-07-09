/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Dimension;
import java.awt.Point;

import org.mozilla.javascript.NativeObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class RhinoConversion
{
	/**
	 * Convert a property value to its corresponding Java object.
	 * @param propertyValue the value to convert
	 * @param pd the property description
	 * @param converterContext
	 * @return the converted property if the property description type is one of:
	 *  dimension, point, color, format, border, media, formscope;
	 *  the same value otherwise
	 */
	public static Object convert(Object propertyValue, PropertyDescription pd, IServoyDataConverterContext converterContext)
	{
		switch (pd.getType().getName())
		{
			case "dimension" :
				if (propertyValue instanceof Object[])
				{
					return new Dimension(Utils.getAsInteger(((Object[])propertyValue)[0]), Utils.getAsInteger(((Object[])propertyValue)[1]));
				}
				if (propertyValue instanceof NativeObject)
				{
					NativeObject value = (NativeObject)propertyValue;
					return new Dimension(Utils.getAsInteger(value.get("width", value)), Utils.getAsInteger(value.get("height", value)));
				}
				break;

			case "point" :
				if (propertyValue instanceof Object[])
				{
					return new Point(Utils.getAsInteger(((Object[])propertyValue)[0]), Utils.getAsInteger(((Object[])propertyValue)[1]));
				}
				if (propertyValue instanceof NativeObject)
				{
					NativeObject value = (NativeObject)propertyValue;
					return new Point(Utils.getAsInteger(value.get("x", value)), Utils.getAsInteger(value.get("y", value)));
				}
				break;

			case "color" :
				if (propertyValue instanceof String)
				{
					return PersistHelper.createColor(propertyValue.toString());
				}
				break;

			case "format" :
				if (propertyValue instanceof String)
				{
					//todo recreate ComponentFormat object (it has quite a lot of dependencies , application,pesist  etc)
					return propertyValue;
				}
				break;

			case "border" :
				if (propertyValue instanceof String)
				{
					return ComponentFactoryHelper.createBorder((String)propertyValue);
				}
				break;


			case "media" :
				Media media = null;
				if (propertyValue instanceof Integer)
				{
					media = converterContext.getSolution().getMedia(((Integer)propertyValue).intValue());
				}
				else if (propertyValue instanceof String && ((String)propertyValue).toLowerCase().startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
				{
					media = converterContext.getSolution().getMedia(((String)propertyValue).substring(MediaURLStreamHandler.MEDIA_URL_DEF.length()));
				}
				if (media != null)
				{
					String url = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" + media.getName();
					Dimension imageSize = ImageLoader.getSize(media.getMediaData());
					boolean paramsAdded = false;
					if (imageSize != null)
					{
						paramsAdded = true;
						url += "?imageWidth=" + imageSize.width + "&imageHeight=" + imageSize.height;
					}
					if (converterContext.getApplication() != null)
					{
						Solution sc = converterContext.getSolution().getSolutionCopy(false);
						if (sc != null && sc.getMedia(media.getName()) != null)
						{
							if (paramsAdded) url += "&";
							else url += "?";
							url += "uuid=" + converterContext.getApplication().getWebsocketSession().getUuid() + "&lm:" + sc.getLastModifiedTime();
						}
					}
					return url;
				}
				else
				{
					Debug.log("cannot convert media " + propertyValue);
				}
				break;
			case "formscope" :
				INGApplication app = converterContext.getApplication();
				if (propertyValue instanceof String && app != null)
				{
					return app.getFormManager().getForm((String)propertyValue).getFormScope();
				}
				break;

			default :
		}
		return propertyValue;
	}
}
