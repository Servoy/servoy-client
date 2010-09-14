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
package com.servoy.j2db.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.debug.DebugJ2DBClient.DebugSwingFormMananger;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.LazyCompilationScope;
import com.servoy.j2db.util.Debug;

public class DebugUtils
{
	public interface DebugUpdateFormSupport
	{
		public void updateForm(Form form);
	}

	public static List<FormController>[] getScopesAndFormsToReload(ClientState clientState, Collection<IPersist> changes)
	{
		List<FormController> scopesToReload = new ArrayList<FormController>();
		List<FormController> formsToReload = new ArrayList<FormController>();

		Set<Form> formsUpdated = new HashSet<Form>();
		for (IPersist persist : changes)
		{

			clientState.getFlattenedSolution().updatePersistInSolutionCopy(persist);
			if (persist instanceof ScriptMethod)
			{
				if (persist.getParent() instanceof Form)
				{
					Form form = (Form)persist.getParent();
					List<FormController> cachedFormControllers = ((FormManager)clientState.getFormManager()).getCachedFormControllers(form);

					for (FormController formController : cachedFormControllers)
					{
						scopesToReload.add(formController);
					}
				}
				else if (persist.getParent() instanceof Solution)
				{
					LazyCompilationScope scope = clientState.getScriptEngine().getSolutionScope().getGlobalScope();
					scope.remove((IScriptProvider)persist);
					scope.put((IScriptProvider)persist, (IScriptProvider)persist);
				}
				if (clientState instanceof DebugJ2DBClient)
				{
					((DebugJ2DBClient)clientState).clearUserWindows();
					((DebugSwingFormMananger)((DebugJ2DBClient)clientState).getFormManager()).fillScriptMenu();
				}
			}
			else if (persist instanceof ScriptVariable)
			{
				ScriptVariable sv = (ScriptVariable)persist;
				if (persist.getParent() instanceof Solution)
				{
					clientState.getScriptEngine().getSolutionScope().getGlobalScope().put(sv);
				}
				if (persist.getParent() instanceof Form)
				{
					Form form = (Form)persist.getParent();
					List<FormController> cachedFormControllers = ((FormManager)clientState.getFormManager()).getCachedFormControllers(form);

					for (FormController formController : cachedFormControllers)
					{
						FormScope scope = formController.getFormScope();
						scope.put(sv);
					}
				}
			}
			else if (persist.getAncestor(IRepository.FORMS) != null)
			{
				Form form = (Form)persist.getAncestor(IRepository.FORMS);
				if (!formsUpdated.contains(form))
				{
					formsUpdated.add(form);
					List<FormController> cachedFormControllers = ((FormManager)clientState.getFormManager()).getCachedFormControllers(form);

					for (FormController formController : cachedFormControllers)
					{
						formsToReload.add(formController);
					}
				}
				if (persist instanceof Form)
				{
					((DebugUtils.DebugUpdateFormSupport)clientState.getFormManager()).updateForm((Form)persist);
				}
			}
			else if (persist instanceof ScriptCalculation)
			{
				ScriptCalculation sc = (ScriptCalculation)persist;
				if (((RemoteDebugScriptEngine)clientState.getScriptEngine()).recompileScriptCalculation(sc))
				{
					List<String> al = new ArrayList<String>();
					al.add(sc.getDataProviderID());
					try
					{
						String dataSource = clientState.getFoundSetManager().getDataSource(sc.getTable());
						((FoundSetManager)clientState.getFoundSetManager()).getRowManager(dataSource).clearCalcs(null, al);
						((FoundSetManager)clientState.getFoundSetManager()).flushSQLSheet(dataSource);
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				if (clientState instanceof DebugJ2DBClient)
				{
					((DebugJ2DBClient)clientState).clearUserWindows();
				}
			}
			else if (persist instanceof Relation)
			{
				((FoundSetManager)clientState.getFoundSetManager()).flushSQLSheet((Relation)persist);

				List<FormController> cachedFormControllers = ((FormManager)clientState.getFormManager()).getCachedFormControllers();

				try
				{
					String primary = ((Relation)persist).getPrimaryDataSource();
					for (FormController formController : cachedFormControllers)
					{
						if (primary.equals(formController.getDataSource()))
						{
							formsToReload.add(formController);
						}
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else if (persist instanceof ValueList)
			{
				ComponentFactory.flushValueList((ValueList)persist);
				List<FormController> cachedFormControllers = ((FormManager)clientState.getFormManager()).getCachedFormControllers();
				for (FormController formController : cachedFormControllers)
				{
					formsToReload.add(formController);
				}
			}
			else if (persist instanceof Style)
			{
				ComponentFactory.flushStyle(((Style)persist));
				List<FormController> cachedFormControllers = ((FormManager)clientState.getFormManager()).getCachedFormControllers();

				String styleName = ((Style)persist).getName();
				for (FormController formController : cachedFormControllers)
				{
					if (styleName.equals(formController.getForm().getStyleName()))
					{
						formsToReload.add(formController);
					}
				}
			}
		}

		return new List[] { scopesToReload, formsToReload };
	}
}
