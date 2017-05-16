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

package com.servoy.j2db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.dataprocessing.EditRecordList;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.ScopesScope;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 *
 */
public abstract class BasicFormManager implements IBasicFormManager
{
	private static final int MAX_FORMS_LOADED;

	static
	{
		// TODO web clients?? Can they also get 160 forms??
		long maxMem = Runtime.getRuntime().maxMemory();
		if (maxMem > 300000000)
		{
			MAX_FORMS_LOADED = 192;
		}
		else if (maxMem > 200000000)
		{
			MAX_FORMS_LOADED = 128;
		}
		else if (maxMem > 100000000)
		{
			MAX_FORMS_LOADED = 64;
		}
		else
		{
			MAX_FORMS_LOADED = 32;
		}
		Debug.trace("MaxFormsLoaded set to:" + MAX_FORMS_LOADED); //$NON-NLS-1$
	}

	protected final ConcurrentMap<String, Form> possibleForms; // formName -> Form
	protected final IApplication application;
	private final LinkedList<IFormController> leaseHistory;

	public BasicFormManager(IApplication application)
	{
		this.application = application;
		possibleForms = new ConcurrentHashMap<String, Form>();
		leaseHistory = new LinkedList<IFormController>();
	}

	public void addForm(Form form, boolean selected)
	{
		Form f = possibleForms.put(form.getName(), form);

		if (f != null && form != f)
		{
			// replace all occurrences to the previous form to this new form (newFormInstances)
			Iterator<Entry<String, Form>> iterator = possibleForms.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<String, Form> next = iterator.next();
				if (next.getValue() == f)
				{
					next.setValue(form);
				}
			}
		}
	}

	public boolean removeForm(Form form)
	{
		boolean removed = destroyFormInstance(form.getName());
		if (removed)
		{
			possibleForms.remove(form.getName());
		}
		return removed;
	}

	public Iterator<String> getPossibleFormNames()
	{
		return possibleForms.keySet().iterator();
	}

	public Form getPossibleForm(String name)
	{
		return possibleForms.get(name);
	}

	public boolean isPossibleForm(String formName)
	{
		return possibleForms.containsKey(formName);
	}

	public boolean createNewFormInstance(String designFormName, String newInstanceScriptName)
	{
		Form f = possibleForms.get(designFormName);
		Form test = possibleForms.get(newInstanceScriptName);
		if (f != null && test == null)
		{
			possibleForms.put(newInstanceScriptName, f);
			return true;
		}
		return false;
	}

	public boolean destroyFormInstance(String formName)
	{
		Form test = possibleForms.get(formName);
		if (test != null)
		{
			// If form found, test if there is a formcontroller alive.
			IFormController fc = getCachedFormController(formName);
			if (fc != null)
			{
				// if that one can be deleted destroy it.
				if (!canBeDeleted(fc))
				{
					return false;
				}
				fc.destroy();
			}
			// if the formname is an alias then remove the alias.
			if (!test.getName().equals(formName))
			{
				possibleForms.remove(formName);
			}
			setFormReadOnly(formName, false);
			return true;
		}
		return false;
	}

	protected boolean canBeDeleted(IFormController fp)
	{
		if (fp.isFormVisible())
		{
			return false;
		}

		// it has a parent form, that is not destroyed, form is used when parent form is visible, skip delete
		if (fp.hasParentForm())
		{
			return false;
		}

		//  cannot be deleted if a global var has a ref
		ScopesScope scopesScope = application.getScriptEngine().getScopesScope();
		if (hasReferenceInScriptable(scopesScope, fp, new HashSet<Scriptable>()))
		{
			return false;
		}

		if (fp.isFormExecutingFunction())
		{
			return false;
		}

		// if this controller uses a separate foundset
		if (fp.getForm().getUseSeparateFoundSet())
		{
			// test if that foundset has edited records that can't be saved
			EditRecordList editRecordList = application.getFoundSetManager().getEditRecordList();
			if (editRecordList.stopIfEditing(fp.getFoundSet()) != ISaveConstants.STOPPED)
			{
				return false;
			}
		}

		// the cached currentcontroller may not be destroyed
		SolutionScope ss = application.getScriptEngine().getSolutionScope();
		return ss == null || fp.initForJSUsage() != ss.get("currentcontroller", ss); //$NON-NLS-1$
	}

	private boolean hasReferenceInScriptable(Scriptable scriptVar, IFormController fc, Set<Scriptable> seen)
	{
		if (!seen.add(scriptVar))
		{
			// endless recursion
			return false;
		}
		if (scriptVar instanceof FormScope)
		{
			return ((FormScope)scriptVar).getFormController().equals(fc);
		}
		if (scriptVar instanceof GlobalScope || scriptVar instanceof ScopesScope || scriptVar instanceof NativeArray) // if(o instanceof Scriptable) for all scriptable ?
		{
			Object[] propertyIDs = scriptVar.getIds();
			if (propertyIDs != null)
			{
				Object propertyValue;
				for (Object element : propertyIDs)
				{
					if (element != null)
					{
						if (element instanceof Integer)
						{
							propertyValue = scriptVar.get(((Integer)element).intValue(), scriptVar);
						}
						else
						{
							propertyValue = scriptVar.get(element.toString(), scriptVar);
						}

						if (propertyValue != null && propertyValue.equals(fc))
						{
							return true;
						}
						if (propertyValue instanceof Scriptable && hasReferenceInScriptable((Scriptable)propertyValue, fc, seen))
						{
							return true;
						}
					}
				}
			}
		}

		return false;
	}


	public boolean isFormReadOnly(String formName)
	{
		return readOnlyCheck.contains(formName);
	}

	protected List<String> readOnlyCheck = new ArrayList<String>();

	public void setFormReadOnly(String formName, boolean readOnly)
	{
		if (readOnly && readOnlyCheck.contains(formName)) return;

		if (readOnly)
		{
			readOnlyCheck.add(formName);
		}
		else
		{
			readOnlyCheck.remove(formName);
		}
	}

	public boolean isFormEnabled(String formName)
	{
		return !enabledCheck.contains(formName);
	}

	protected List<String> enabledCheck = new ArrayList<String>();

	public void setFormEnabled(String formName, boolean enabled)
	{
		if (!enabled && enabledCheck.contains(formName)) return;

		if (!enabled)
		{
			enabledCheck.add(formName);
		}
		else
		{
			enabledCheck.remove(formName);
		}
	}

	/**
	 * @param formName
	 * @return
	 */
	public abstract IFormController getCachedFormController(String formName);

	public void touch(IFormController fp)
	{
		addAsLastInLeaseHistory(fp);
	}

	protected void hideFormIfVisible(IFormController fc)
	{
		if (fc.isFormVisible())
		{
			fc.getFormUI().setComponentVisible(false);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			fc.notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
		}
	}

	protected int getMaxFormsLoaded()
	{
		return MAX_FORMS_LOADED;
	}

	protected final void clearLeaseHistory()
	{
		leaseHistory.clear();
	}

	protected final boolean removeAllFromLeaseHistory()
	{
		boolean hadFormPanels = false;
		synchronized (leaseHistory)
		{
			hadFormPanels = leaseHistory.size() > 0;
			while (leaseHistory.size() > 0)
			{
				IFormController fp = leaseHistory.removeFirst();
				fp.destroy();
			}
		}
		return hadFormPanels;
	}

	protected final void removeFromLeaseHistory(IFormController fc)
	{
		synchronized (leaseHistory)
		{
			leaseHistory.remove(fc);
		}
	}

	protected final void addAsLastInLeaseHistory(IFormController fc)
	{
		synchronized (leaseHistory)
		{
			if (!leaseHistory.isEmpty() && leaseHistory.getLast() != fc)
			{
				leaseHistory.remove(fc);//to prevent the panel is added more than once
				leaseHistory.add(fc);
			}
		}
	}

	protected final void updateLeaseHistory(IFormController fp)
	{
		IFormController toBeRemoved = null;
		synchronized (leaseHistory)
		{
			int leaseHistorySize = leaseHistory.size();
			if (leaseHistorySize > getMaxFormsLoaded())
			{
				for (int i = 0; i < leaseHistorySize; i++)
				{
					IFormController fc = leaseHistory.get(i);
					if (canBeDeleted(fc))
					{
						toBeRemoved = fc;
						break;
					}
				}
			}
			leaseHistory.remove(fp);//to prevent the panel is added more than once
			leaseHistory.add(fp);
			if (Debug.tracing())
			{
				Debug.trace("FormPanel '" + fp.getName() + "' created, Loaded forms: " + leaseHistory.size() + " of " + getMaxFormsLoaded() + " (max)."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		if (toBeRemoved != null)
		{
			if (Debug.tracing())
			{
				Debug.trace("FormPanel '" + toBeRemoved.getName() + "' removed because of MAX_FORMS_LOADED (" + getMaxFormsLoaded() + //$NON-NLS-1$ //$NON-NLS-2$
					") was passed."); //$NON-NLS-1$
			}
			toBeRemoved.destroy();
		}
	}
}