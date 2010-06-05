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



import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileView;

public class ImageFileView extends FileView {
    ImageIcon jpgIcon = new ImageIcon("images/jpgIcon.gif"); //$NON-NLS-1$
    ImageIcon gifIcon = new ImageIcon("images/gifIcon.gif"); //$NON-NLS-1$
    ImageIcon tiffIcon = new ImageIcon("images/tiffIcon.gif"); //$NON-NLS-1$
    
    public String getName(File f) {
        return null; // let the L&F FileView figure this out
    }
    
    public String getDescription(File f) {
        return null; // let the L&F FileView figure this out
    }
    
    public Boolean isTraversable(File f) {
        return null; // let the L&F FileView figure this out
    }
    
    public String getTypeDescription(File f) {
        String extension = getExtension(f);
        String type = null;

        if (extension != null) {
            if (extension.equals("jpeg") || //$NON-NLS-1$
                extension.equals("jpg")) { //$NON-NLS-1$
                type = "JPEG Image"; //$NON-NLS-1$
            } else if (extension.equals("gif")){ //$NON-NLS-1$
                type = "GIF Image"; //$NON-NLS-1$
            } else if (extension.equals("tiff") | //$NON-NLS-1$|
                       extension.equals("tif")) { //$NON-NLS-1$
                type = "TIFF Image"; //$NON-NLS-1$
            } 
        }
        return type;
    }
    
    public Icon getIcon(File f) {
        String extension = getExtension(f);
        Icon icon = null;
        if (extension != null) {
            if (extension.equals("jpeg") || //$NON-NLS-1$
                extension.equals("jpg")) { //$NON-NLS-1$
                icon = jpgIcon;
            } else if (extension.equals("gif")) { //$NON-NLS-1$
                icon = gifIcon;
            } else if (extension.equals("tiff") || //$NON-NLS-1$
                       extension.equals("tif")) { //$NON-NLS-1$
                icon = tiffIcon;
            } 
        }
        return icon;
    }
    
    // Get the extension of this file. Code is factored out
    // because we use this in both getIcon and getTypeDescription
    private String getExtension(File f) {

        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
}
