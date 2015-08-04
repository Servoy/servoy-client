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
package com.servoy.j2db.server.ngclient.component.spec;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.servoy.j2db.persistence.ClientMethodTemplatesLoader;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.util.Debug;

public class MethodTemplatesLoader
{
	public static void loadMethodTemplatesFromXML()
	{
		synchronized (ClientMethodTemplatesLoader.getLock())
		{
			// even if it was loaded before with "ClientMethodTemplatesLoader.loadClientMethodTemplatesIfNeeded" we must load the XML, as we are in developer and we need all the method template info
			ClientMethodTemplatesLoader.setLoaded();

			try
			{
				InputStream is = new FileInputStream("../../servoy-eclipse/com.servoy.eclipse.core/src/com/servoy/eclipse/core/doc/methodtemplates.xml");

				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = builder.parse(is);
				Element rootElement = doc.getDocumentElement();
				NodeList eventElements = rootElement.getElementsByTagName("event");
				int counter = 0;
				for (int i = 0; i < eventElements.getLength(); i++)
				{
					Element eventElem = (Element)eventElements.item(i);
					String name = eventElem.getAttribute("name");
					NodeList templElements = eventElem.getElementsByTagName("methodtemplate");
					if (templElements.getLength() > 0)
					{
						Element methTempl = (Element)templElements.item(0);
						MethodTemplate mt = MethodTemplate.fromXML(methTempl);
						MethodTemplate.COMMON_TEMPLATES.put(name, mt);
						counter++;
					}
				}
				System.out.println("Loaded " + counter + " method templates.");
			}
			catch (Throwable e)
			{
				Debug.error("Exception while loading method templates.", e);
			}
		}
	}

}
