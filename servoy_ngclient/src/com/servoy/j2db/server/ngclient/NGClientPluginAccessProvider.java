/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IMediaUploadCallback;

/**
 * @author gboros
 *
 */
public class NGClientPluginAccessProvider extends ClientPluginAccessProvider implements INGClientPluginAccess
{
	private final NGClient ngClient;

	public NGClientPluginAccessProvider(NGClient client)
	{
		super(client);
		this.ngClient = client;
	}

	public boolean showURL(String url, String target, String target_options)
	{
		return getApplication().showURL(url, target, target_options, 0, true);
	}

	public boolean showURL(String url, String target, String target_options, int timeout)
	{
		return getApplication().showURL(url, target, target_options, timeout, true);
	}

	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		return getApplication().showURL(url, target, target_options, timeout, onRootFrame);
	}

	public String serveResource(String filename, byte[] bs, String mimetype)
	{
		return serveResource(filename, bs, mimetype, null);
	}

	public String serveResource(String filename, byte[] bs, String mimetype, String contentDisposition)
	{
		return ngClient.serveResource(filename, bs, mimetype, contentDisposition);
	}

	@Override
	public void showFileOpenDialog(IMediaUploadCallback callback, String fileNameHint, boolean multiSelect, String[] filter, int selection, String dialogTitle)
	{
		StringBuilder acceptFilter = new StringBuilder();
		if (filter != null && filter.length > 1)
		{
			for (int i = 1; i < filter.length; i++)
			{
				if (i > 1) acceptFilter.append(',');
				acceptFilter.append('.').append(filter[i]);
			}
		}
		ngClient.showFileOpenDialog(callback, multiSelect, acceptFilter.toString(), dialogTitle);
	}
}
