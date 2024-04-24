/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.j2db.scripting;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.INGClientApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2022.03.9
 */
@SuppressWarnings("hiding")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSBlobLoaderBuilder")
public class JSBlobLoaderBuilder
{
	private final INGClientApplication application;
	private final String dataprovider;
	private final int clientnr;
	private String servername;
	private String tablename;
	private String datasource;
	private String mimetype;
	private Object[] rowid;
	private String filename;

	/**
	 * @param application
	 * @param dataprovider
	 */
	public JSBlobLoaderBuilder(INGClientApplication application, String dataprovider, int clientnr)
	{
		this.application = application;
		this.dataprovider = dataprovider;
		this.clientnr = clientnr;
	}

	/**
	 *  Sets the server name of the dataprovider.
	 *
	 * @param {String} servername
	 * @param {String} tablename
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder serverAndTable(String servername, String tablename)
	{
		this.servername = servername;
		this.tablename = tablename;
		return this;
	}

	/**
	 *  Sets the datasource (server/table combination) of the dataprovider.
	 *
	 * @param {String} datasource
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder datasource(String datasource)
	{
		this.datasource = datasource;
		return this;
	}

	/**
	 *  Sets the rowid (single pk) of the table
	 *
	 * @param {Object} rowid
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder rowid(Object rowid)
	{
		this.rowid = new Object[] { rowid };
		return this;
	}

	/**
	 *  Sets the rowids (composite pk) of the table
	 *
	 * @param {Object[]} rowid
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder rowid(Object[] rowid)
	{
		this.rowid = rowid;
		return this;
	}

	/**
	 *  Sets the filename of the data of the dataprovider.
	 *
	 * @param {String} filename
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder filename(String filename)
	{
		this.filename = filename;
		return this;
	}

	/**
	 *  Sets the mimetype of the data of the dataprovider.
	 *
	 * @param {String} mimetype
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder mimetype(String mimetype)
	{
		this.mimetype = mimetype;
		return this;
	}


	@SuppressWarnings("nls")
	@JSFunction
	public String create()
	{
		StringBuilder sb = new StringBuilder();
		if (datasource != null)
		{
			sb.append("datasource=").append(datasource).append("&");
		}
		else if (servername != null && tablename != null)
		{
			sb.append("servername=").append(servername).append("&").append("tablename=").append(tablename).append("&");
		}

		sb.append("dataprovider=").append(dataprovider).append("&");
		if (rowid != null)
		{
			if (rowid.length == 1)
				sb.append("rowid=").append(rowid[0]).append("&");
			else for (int i = 0; i < rowid.length; i++)
			{
				sb.append("rowid").append(i).append("=").append(rowid[i]).append("&");
			}
		}
		if (filename != null)
		{
			sb.append("filename=").append(filename).append("&");
		}
		if (mimetype != null)
		{
			sb.append("mimetype=").append(mimetype).append("&");
		}

		String blobpart = sb.toString();
		try
		{
			blobpart = application.getFlattenedSolution().getEncryptionHandler().encryptString(blobpart, true);
			blobpart = "resources/servoy_blobloader?blob=" + blobpart + "&clientnr=" + clientnr;
		}
		catch (Exception e1)
		{
			Debug.error("could not encrypt blobloaderpart: " + blobpart);
		}
		return blobpart;
	}

}
