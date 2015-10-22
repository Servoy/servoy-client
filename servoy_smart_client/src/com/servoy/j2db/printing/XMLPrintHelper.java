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
package com.servoy.j2db.printing;


import java.awt.Component;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Writer;

import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 * 
 *         To change this generated comment go to Window>Preferences>Java>Code Generation>Code Template
 */
public class XMLPrintHelper
{
	public static void handleComponent(Writer w, Component element, Rectangle rec, Object obj) throws IOException
	{
		if (element instanceof IDisplayData)
		{
			w.write("<DATACOMPONENT x=\"" + rec.x + "\" y=\"" + rec.y + "\" width=\"" + rec.width + "\" height=\"" + rec.height + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			w.write("<VALUE dataprovider=\"" + ((IDisplayData)element).getDataProviderID() + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$
			CharSequence val = ""; //$NON-NLS-1$
			if (obj == null) obj = ((IDisplayData)element).getValueObject();
			if (obj != null) val = Utils.escapeMarkup(obj.toString());
			w.write(val == null ? null : val.toString());
			w.write("</VALUE>"); //$NON-NLS-1$
			w.write("</DATACOMPONENT>"); //$NON-NLS-1$
		}
		else if (element instanceof ILabel)
		{
			w.write("<TEXTCOMPONENT x=\"" + rec.x + "\" y=\"" + rec.y + "\" width=\"" + rec.width + "\" height=\"" + rec.height + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			w.write("<TEXT>"); //$NON-NLS-1$
			CharSequence val = ""; //$NON-NLS-1$
			if (obj == null) obj = ((ILabel)element).getText();
			if (obj != null) val = Utils.escapeMarkup(obj.toString());
			w.write(val == null ? null : val.toString());
			w.write("</TEXT>"); //$NON-NLS-1$
			w.write("</TEXTCOMPONENT>"); //$NON-NLS-1$
		}
		else if (element instanceof ISupportXMLOutput)
		{
			((ISupportXMLOutput)element).toXML(w, rec);
		}
	}
}
