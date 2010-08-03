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
package com.servoy.j2db.server.headlessclient;

import org.apache.wicket.Application;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.MarkupCache;

/**
 * @author jcompagner
 * 
 */
public class ServoyMarkupCache extends MarkupCache
{

//	private final WeakHashMap<MarkupContainer, Markup> cache = new WeakHashMap<MarkupContainer, Markup>();

	/**
	 * @param application
	 */
	public ServoyMarkupCache(Application application)
	{
		super(application);
	}

	public void removeFromCache(WebForm form)
	{
//		synchronized (this)
//		{
//			cache.remove(form);
//		}
	}

	/**
	 * @see org.apache.wicket.markup.MarkupCache#putIntoCache(java.lang.String, org.apache.wicket.MarkupContainer, org.apache.wicket.markup.Markup)
	 */
	@Override
	protected Markup putIntoCache(String locationString, MarkupContainer container, Markup markup)
	{
		if (container instanceof WebForm)
		{
//			synchronized (this)
//			{
//				cache.put(container, markup);
//			}
			return markup;
		}
		else
		{
			return super.putIntoCache(locationString, container, markup);
		}
	}

	/**
	 * @see org.apache.wicket.markup.MarkupCache#getMarkupFromCache(java.lang.CharSequence, org.apache.wicket.MarkupContainer)
	 */
	@Override
	protected Markup getMarkupFromCache(CharSequence cacheKey, MarkupContainer container)
	{
		if (container instanceof WebForm)
		{
//			synchronized (this)
//			{
//				return cache.get(container);
//			}
			return null;
		}
		else
		{
			return super.getMarkupFromCache(cacheKey, container);
		}
	}

}
