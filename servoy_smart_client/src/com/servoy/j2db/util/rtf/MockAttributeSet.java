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
package com.servoy.j2db.util.rtf;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;


/* This AttributeSet is made entirely out of tofu and Ritz Crackers
   and yet has a remarkably attribute-set-like interface! */
class MockAttributeSet
    implements AttributeSet, MutableAttributeSet
{
    public Dictionary backing;

    public boolean isEmpty()
    {
         return backing.isEmpty();
    }
    
    public int getAttributeCount()
    {
         return backing.size();
    }

    public boolean isDefined(Object name)
    {
         return ( backing.get(name) ) != null;
    }

    public boolean isEqual(AttributeSet attr)
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }

    public AttributeSet copyAttributes()
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }
    
    public Object getAttribute(Object name)
    {
        return backing.get(name);
    }

    public void addAttribute(Object name, Object value)
    {
        backing.put(name, value);
    }
    
    public void addAttributes(AttributeSet attr)
    {
        Enumeration as = attr.getAttributeNames();
	while(as.hasMoreElements()) {
	    Object el = as.nextElement();
	    backing.put(el, attr.getAttribute(el));
	}
    }

    public void removeAttribute(Object name)
    {
        backing.remove(name);
    }

    public void removeAttributes(AttributeSet attr)
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }

    public void removeAttributes(Enumeration en)
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }

    public void setResolveParent(AttributeSet pp)
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }

    
    public Enumeration getAttributeNames()
    {
         return backing.keys();
    }
    
    public boolean containsAttribute(Object name, Object value)
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }

    public boolean containsAttributes(AttributeSet attr)
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }

    public AttributeSet getResolveParent()
    {
         throw new InternalError("MockAttributeSet: charade revealed!"); //$NON-NLS-1$
    }
}
    
    
