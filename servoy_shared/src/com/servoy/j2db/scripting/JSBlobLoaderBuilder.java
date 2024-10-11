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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.INGClientApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;

/**
 * Utility object for building a blob loader url.
 * @author jcompagner
 * @since 2022.03.9
 */
@SuppressWarnings("hiding")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSBlobLoaderBuilder")
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
public class JSBlobLoaderBuilder
{

	public static final String MIMETYPE_ARG = "mimetype="; //$NON-NLS-1$
	public static final String FILENAME_ARG = "filename="; //$NON-NLS-1$
	public static final String MULTIPLE_ROWID_ARG_PREFIX = "rowid"; //$NON-NLS-1$
	public static final String ROWID_ARG = MULTIPLE_ROWID_ARG_PREFIX + '=';
	public static final String GLOBAL_ARG = "global="; //$NON-NLS-1$
	public static final String DATAPROVIDER_ARG = "dataprovider="; //$NON-NLS-1$
	public static final String TABLENAME_ARG = "tablename="; //$NON-NLS-1$
	public static final String SERVERNAME_ARG = "servername="; //$NON-NLS-1$
	public static final String DATASOURCE_ARG = "datasource="; //$NON-NLS-1$

	public static final String ENCRYPTED_CLIENTNR_ARG = "&clientnr="; //$NON-NLS-1$
	public static final String ENCRYPTED_BLOB_PREFIX_ARG = "blob="; //$NON-NLS-1$

	private final INGClientApplication application;
	private final String dataprovider;
	private final int clientnr;
	private String servername;
	private String tablename;
	private String datasource;
	private String mimetype;
	private Object[] rowid;
	private String filename;

	public JSBlobLoaderBuilder(INGClientApplication application, String dataprovider, int clientnr)
	{
		this.application = application;
		this.dataprovider = dataprovider;
		this.clientnr = clientnr;
	}

	/**
	 * Sets the server name and table name of the builder's column dataprovider.
	 *
	 * @param servername The servername for this builder's column dataprovider.
	 * @param tablename The tablename for this builder's column dataprovider.
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
	 * Sets the datasource (server/table combination) of the builder's column dataprovider.
	 *
	 * @param datasource The datasource for this builder's column dataprovider.
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder datasource(String datasource)
	{
		this.datasource = datasource;
		return this;
	}

	/**
	 * Sets the rowid (single pk or composite pk) of the table.
	 *
	 * @param rowid The rowid, can be a single value or an array of values
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder rowid(Object rowid)
	{
		this.rowid = new Object[] { rowid };
		return this;
	}

	/**
	 * Sets the rowids (single pk or composite pk) of the table.
	 *
	 * @param rowid The rowid; can be a single value or an array of values.
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder rowid(Object[] rowid)
	{
		this.rowid = rowid;
		return this;
	}

	/**
	 * Sets the filename of the data in the dataprovider.<br/>
	 * If given, it will set the Content-disposition header to: attachment; filename=filename
	 *
	 * @param filename The filename for the data.
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder filename(String filename)
	{
		this.filename = filename;
		return this;
	}

	/**
	 * Sets the mimetype of the data in the dataprovider.<br/>
	 * This will be set in the content type header of the response for this data.
	 *
	 * @param mimetype the mime type of the data (set as the content type header)
	 * @return return the builder itself
	 */
	@JSFunction
	public JSBlobLoaderBuilder mimetype(String mimetype)
	{
		this.mimetype = mimetype;
		return this;
	}

	/**
	 * Builds the blobloader url string that can be used in custom html or send to the browser as a redirect url for direct downloads.
	 *
	 * @return the blobloader url pointing to the data of the given dataprovider
	 */
	@SuppressWarnings("nls")
	@JSFunction
	public String build()
	{
		boolean isGlobalDP = true;

		StringBuilder sb = new StringBuilder();
		if (datasource != null)
		{
			sb.append(DATASOURCE_ARG).append(datasource).append('&');
			isGlobalDP = false;
		}
		else if (servername != null && tablename != null)
		{
			sb.append(SERVERNAME_ARG).append(servername).append('&').append(TABLENAME_ARG).append(tablename).append('&');
			isGlobalDP = false;
		}

		if (isGlobalDP) sb.append(GLOBAL_ARG).append(dataprovider).append('&');
		else sb.append(DATAPROVIDER_ARG).append(dataprovider).append('&');

		if (rowid != null)
		{
			if (rowid.length == 1)
				sb.append(ROWID_ARG).append(rowid[0]).append('&');
			else for (int i = 0; i < rowid.length; i++)
			{
				sb.append(MULTIPLE_ROWID_ARG_PREFIX).append(i).append("=").append(rowid[i]).append('&');
			}
		}
		if (filename != null)
		{
			sb.append(FILENAME_ARG).append(filename).append('&');
		}
		if (mimetype != null)
		{
			sb.append(MIMETYPE_ARG).append(mimetype).append('&');
		}

		String blobpart = sb.toString();
		try
		{
			blobpart = application.getFlattenedSolution().getEncryptionHandler().encryptString(blobpart, true);
			blobpart = "resources/servoy_blobloader?" + ENCRYPTED_BLOB_PREFIX_ARG + blobpart + ENCRYPTED_CLIENTNR_ARG + clientnr;
		}
		catch (Exception e1)
		{
			Debug.error("could not encrypt blobloaderpart: " + blobpart);
		}
		return blobpart;
	}

}
