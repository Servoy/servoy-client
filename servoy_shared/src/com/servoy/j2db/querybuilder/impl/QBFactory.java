/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.ITableAndRelationProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;
import com.servoy.j2db.scripting.annotations.AnnotationManager;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author rgansevles
 *
 */
public class QBFactory implements IQueryBuilderFactory
{
	private Scriptable scriptableParent;
	private final ITableAndRelationProvider tableProvider;
	private final IGlobalValueEntry globalScopeProvider;
	private final IDataProviderHandler dataProviderHandler;

	public QBFactory(ITableAndRelationProvider tableProvider, IGlobalValueEntry globalScopeProvider, IDataProviderHandler dataProviderHandler)
	{
		this.tableProvider = tableProvider;
		this.globalScopeProvider = globalScopeProvider;
		this.dataProviderHandler = dataProviderHandler;
	}

	/**
	 * @param scriptableParent the scriptableParent to set
	 */
	public void setScriptableParent(Scriptable scriptableParent)
	{
		this.scriptableParent = scriptableParent;
	}

	public QBSelect createSelect(String dataSource, String alias) throws RepositoryException
	{
		QBSelect queryBuilder = new QBSelect(tableProvider, globalScopeProvider, dataProviderHandler, dataSource, alias);
		queryBuilder.setScriptableParent(scriptableParent);
		return queryBuilder;
	}

	public QBSelect createSelect(String dataSource) throws RepositoryException
	{
		return createSelect(dataSource, null);
	}

	public static Map<String, NativeJavaMethod> getJsFunctions(Class< ? > clazz)
	{
		Map<String, NativeJavaMethod> jsFunctions = new HashMap<String, NativeJavaMethod>();
		try
		{
			for (Method method : clazz.getMethods())
			{
				if (AnnotationManager.getInstance().isAnnotationPresent(method, JSFunction.class))
				{
					String name = method.getName();
					NativeJavaMethod nativeJavaMethod = jsFunctions.get(name);
					if (nativeJavaMethod == null)
					{
						nativeJavaMethod = new NativeJavaMethod(method, name);
					}
					else
					{
						nativeJavaMethod = new NativeJavaMethod(Utils.arrayAdd(nativeJavaMethod.getMethods(), new MemberBox(method), true), name);
					}
					jsFunctions.put(name, nativeJavaMethod);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return jsFunctions;
	}
}
