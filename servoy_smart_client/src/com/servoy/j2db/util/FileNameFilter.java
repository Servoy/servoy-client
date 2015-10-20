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

import javax.swing.filechooser.FileFilter;

public class FileNameFilter extends FileFilter 
{
	private String extension;
	public FileNameFilter(String a_extension)
	{
		extension = a_extension;
	}
    
    // Accept all directories and all gif, jpg, or tiff files.
    public boolean accept(File f) {

        if (f.isDirectory()) {
            return true;
        }

        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            String ext = s.substring(i+1);
            if (extension.equalsIgnoreCase(ext) ) 
			{
				return true;
            } 
			else 
			{
                return false;
            }
        }

        return false;
    }
    
    // The description of this filter
    public String getDescription() 
	{
        return "*." + extension; //$NON-NLS-1$
    }
}
