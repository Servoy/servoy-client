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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.Component;

import com.servoy.j2db.persistence.IPersist;

/**
 * Classes that are able to manipulate header components associated to elements
 * will implement this interface.
 * 
 * @author Andrei Costescu
 */
public interface IHeaders
{

    /**
     * Registers the given header component with it's matching IPersist element.
     * 
     * @param matchingElement
     *            the element who's header component is to be registered.
     * @param headerComponent
     *            the header component to be registered.
     */
    void registerHeader(IPersist matchingElement, Component headerComponent);

}
