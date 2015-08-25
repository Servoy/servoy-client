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


/**
 * @author jcompagner
 *
 */
public abstract class BasicFormManager implements IBasicFormManager
{
	protected final ConcurrentMap<String, Form> possibleForms; // formName -> Form
	protected final IApplication application;


	public BasicFormManager(IApplication application)
	{
		this.application = application;
		possibleForms = new ConcurrentHashMap<String, Form>();
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


}
