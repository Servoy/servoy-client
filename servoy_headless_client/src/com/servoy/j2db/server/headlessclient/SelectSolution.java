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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.protocol.http.WebResponse;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * A special wicket {@link WebPage} that displays the solution that can be loaded if the property servoy.allowSolutionBrowsing is not set to false.
 * 
 * @author jcompagner,jblok
 */
public class SelectSolution extends WebPage
{
	private static final long serialVersionUID = 1L;

	public SelectSolution(final PageParameters parameters) throws RepositoryException
	{
		this();
	}

	/**
	 * Constructor that is invoked when page is invoked without a session.
	 * 
	 * @param parameters Page parameters
	 */
	public SelectSolution() throws RepositoryException
	{
		List<Solution> data = new ArrayList<Solution>();
		try
		{
			IApplicationServerSingleton as = ApplicationServerSingleton.get();
			if (as.isDeveloperStartup())
			{
				data.add(as.getDebugClientHandler().getDebugSmartClient().getCurrent());
			}
			else
			{
				if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.allowSolutionBrowsing", "true")))
				{
					RootObjectMetaData[] smds = as.getLocalRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
					int solutionType;
					for (RootObjectMetaData element : smds)
					{
						solutionType = ((SolutionMetaData)element).getSolutionType();
						if ((solutionType & (SolutionMetaData.SOLUTION + SolutionMetaData.WEB_CLIENT_ONLY)) > 0)
						{
							Solution solution = (Solution)as.getLocalRepository().getActiveRootObject(element.getRootObjectId());
							if (solution != null)
							{
								data.add(solution);
							}
						}
					}
				}
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}

		add(new ListView<Solution>("solutions", data)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * Populate the table with Wicket elements
			 */
			@Override
			protected void populateItem(final ListItem<Solution> listItem)
			{
				Solution sol = listItem.getModelObject();
				PageParameters parameters = new PageParameters();
				parameters.put("solution", sol.getName());
				Link l = new BookmarkablePageLink("solution_link", SolutionLoader.class, parameters);
				listItem.add(l);
				l.add(new Label("solution_name", sol.getName()));
				listItem.add(new Image("login_req", new ResourceReference(IApplication.class, sol.requireAuthentication() ? "images/lock.gif"
					: "images/empty.gif")));
//				listItem.add(new Label("release", ""+sd.getActiveRelease()));
			}
		});
	}

	@Override
	protected void setHeaders(WebResponse response)
	{
		HTTPUtils.setNoCacheHeaders(response.getHttpServletResponse());
	}

}
