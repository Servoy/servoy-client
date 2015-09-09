/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.design;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.TableScope;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
public class DesignNGClient extends NGClient
{
	private final IDesignerSolutionProvider solutionProvider;
	private boolean showData = true;

	/**
	 * @param wsSession
	 */
	public DesignNGClient(INGClientWebsocketSession wsSession, IDesignerSolutionProvider solutionProvider, boolean showData) throws Exception
	{
		super(wsSession);
		this.solutionProvider = solutionProvider;
		this.showData = showData;
		setUseLoginSolution(false);
		setTimeZone(TimeZone.getDefault());
		setLocale(Locale.getDefault());
		this.putClientProperty(IApplication.VALUELIST_MAX_ROWS, 20);
	}

	@Override
	public void showDefaultLogin() throws ServoyException
	{
		getClientInfo().setUserUid("designer");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.NGClient#getSolutionTypeFilter()
	 */
	@Override
	protected int getSolutionTypeFilter()
	{
		return super.getSolutionTypeFilter() | SolutionMetaData.MODULE;
	}

	@Override
	public void loadSolution(String solutionName) throws com.servoy.j2db.persistence.RepositoryException
	{
		SolutionMetaData metaData = (SolutionMetaData)solutionProvider.getActiveEditingSolution().getMetaData();
		if (getSolution() == null || !getSolution().getName().equals(metaData.getName()))
		{
			loadSolution(metaData);
		}
		// fake this so that it seems to be in solution model node.
		solutionRoot.getSolutionCopy(true);
	}

	@Override
	protected IActiveSolutionHandler createActiveSolutionHandler()
	{
		return new AbstractActiveSolutionHandler(getApplicationServer())
		{

			@Override
			protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
			{
				return solutionProvider.getEditingSolution(solutionDef.getName());
			}

			@Override
			public IRepository getRepository()
			{
				try
				{
					return getApplicationServerAccess().getRepository();
				}
				catch (RemoteException e)
				{
					Debug.error(e);
				}
				return null;
			}
		};
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new ScriptEngine(this)
		{
			@Override
			public Object executeFunction(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException)
				throws Exception
			{
				return Boolean.TRUE;
			}

			@Override
			public Object eval(Scriptable scope, String eval_string)
			{
				return Boolean.TRUE;
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see com.servoy.j2db.scripting.ScriptEngine#getTableScope(com.servoy.j2db.persistence.ITable)
			 */
			@Override
			public Scriptable getTableScope(final ITable table)
			{
				Context.enter();
				try
				{
					return new TableScope(solutionScope, this, table, application.getFlattenedSolution(), new ISupportScriptProviders()
					{
						public Iterator< ? extends IScriptProvider> getScriptMethods(boolean sort)
						{
							return application.getFlattenedSolution().getScriptCalculations(table, false);
						}

						public Iterator<ScriptVariable> getScriptVariables(boolean b)
						{
							return Collections.<ScriptVariable> emptyList().iterator();
						}

						public ScriptMethod getScriptMethod(int methodId)
						{
							return null; // is not used for calculations
						}
					})
					{
						@Override
						public Object getCalculationValue(String calcName, Scriptable start)
						{
							Scriptable record = getPrototype();
							if (record instanceof Record)
							{
								return ((Record)record).getRawData().getValue(calcName);
							}
							return null;
						}
					};
				}
				finally
				{
					Context.exit();
				}
			}
		};
	}

	@Override
	protected void showInfoPanel()
	{
		//ignore
	}

	@Override
	public boolean isInDesigner()
	{
		return true;
	}

	public boolean getShowData()
	{
		return showData;
	}

	public void setShowData(boolean showData)
	{
		this.showData = showData;
	}
}