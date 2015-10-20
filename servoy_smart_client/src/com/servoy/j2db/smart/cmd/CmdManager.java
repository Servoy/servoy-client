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
package com.servoy.j2db.smart.cmd;


import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.cmd.ICmd;
import com.servoy.j2db.cmd.ICmdManagerInternal;
import com.servoy.j2db.cmd.IHandleUndoRedo;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.smart.SwingFormManager;
import com.servoy.j2db.util.Debug;

/**
 * The manager handling cmds, executing them and enables disables!
 * 
 * @author jblok
 */
public class CmdManager implements ICmdManagerInternal, PropertyChangeListener, IHandleUndoRedo
{
	private UndoManager undoManager;
	private ControllerUndoManager controllerUndoManager;

	private final UndoAction undoAction;
	private final RedoAction redoAction;

	protected HashMap<String, Action> actions = new HashMap<String, Action>();

	protected ISmartClientApplication application;

	/**
	 * Constructor I
	 */
	public CmdManager(ISmartClientApplication app)
	{
		application = app;

//		undoManager = new UndoManager();
//		undoManager.setLimit(50);

		redoAction = new RedoAction(app, this);
		undoAction = new UndoAction(app, this);

		actions.put("cmdredo", redoAction); //$NON-NLS-1$
		actions.put("cmdundo", undoAction); //$NON-NLS-1$

//this is now handled in Displays adapter, due to jdk 131
//		propListener = new PropertyListener();
//		FocusManager focus = DefaultFocusManager.getCurrentManager();
//		focus.addPropertyChangeListener("focusOwner",propListener);
	}

	public void setCurrentUndoManager(UndoManager man)
	{
		undoManager = man;
		// set to null so that it doesn't hold the state if it was a tabpanel
		if (man instanceof ControllerUndoManager)
		{
			((ControllerUndoManager)undoManager).setFormUndoManager(null);
		}
		if (controllerUndoManager != null)
		{
			controllerUndoManager.setFormUndoManager(man);
		}
	}

	public void setControllerUndoManager(ControllerUndoManager controllerUndo)
	{
		if (this.controllerUndoManager != controllerUndo)
		{
			if (this.controllerUndoManager != null) this.controllerUndoManager.setFormUndoManager(null);
			this.controllerUndoManager = controllerUndo;
			if (this.controllerUndoManager != null) this.controllerUndoManager.setFormUndoManager(undoManager);
		}
	}

	/**
	 * Register a named Action
	 */
	public void registerAction(String name, Action a)
	{
		actions.put(name.toLowerCase(), a);
	}

	/**
	 * get a previous registered action
	 */
	public Action getRegisteredAction(String name)
	{
		return actions.get(name.toLowerCase());
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		String name = evt.getPropertyName();
		if ("repository".equals(name)) //$NON-NLS-1$
		{
			final IRepository repository = (IRepository)evt.getNewValue();

			Action cmdnewsolution = actions.get("cmdnewsolution"); //$NON-NLS-1$ 
			if (cmdnewsolution != null) cmdnewsolution.setEnabled(repository != null);

			if (autoOpenSolutionSelectDialog)
			{
				final Action cmdopensolution = actions.get("cmdopensolution"); //$NON-NLS-1$
				if (cmdopensolution != null)
				{
					application.invokeLater(new Runnable()
					{
						public void run()
						{
							cmdopensolution.setEnabled(repository != null);
							if (repository != null)
							{
								try
								{
									if (repository.getRootObjectMetaDatasForType(IRepository.SOLUTIONS).length != 0 && application.getSolution() == null &&
										(application.getFlattenedSolution() == null || !application.getFlattenedSolution().isLoadingSolution()))
									{
										executeCmd((ICmd)cmdopensolution, null);
									}
								}
								catch (Exception ex)
								{
									Debug.error(ex);
								}
							}
						}
					});
				}
			}
		}
		else if ("solution".equals(name)) //$NON-NLS-1$
		{
			Solution solution = (Solution)evt.getNewValue();

			ableFormRelatedBrowseActions(solution != null);
			ableFormRelatedDataEditActions(solution != null);

			undoAction.setEnabled(solution != null);
			redoAction.setEnabled(solution != null);

			Action cmdclose = actions.get("cmdclose"); //$NON-NLS-1$
			if (cmdclose != null) cmdclose.setEnabled(solution != null);

			Action cmdsolutionsettings = actions.get("cmdsolutionsettings"); //$NON-NLS-1$
			if (cmdsolutionsettings != null) cmdsolutionsettings.setEnabled(solution != null);

//			Action cmdprint = (Action) actions.get("cmdprint"); 
//			if (cmdprint != null) cmdprint.setEnabled(enable);

			//TODO:could be optimized by using fast method getFormCount
			if (solution != null && application.getFlattenedSolution().getForms(false).hasNext())
			{
				ableFormRelatedActions(true);
			}
			else
			{
				ableFormRelatedActions(false);
			}
		}
		else if ("mode".equals(name)) //$NON-NLS-1$
		{
			int oldmode = ((Integer)evt.getOldValue()).intValue();

			Action menuselectaction = actions.get("menuselectaction"); //$NON-NLS-1$
			int mode = ((Integer)evt.getNewValue()).intValue();
			switch (mode)
			{
				case IModeManager.FIND_MODE :
					break;

				case IModeManager.PREVIEW_MODE :
					break;

				case IModeManager.EDIT_MODE :
				default :
					if (menuselectaction != null) menuselectaction.setEnabled(true);
			}

			ableFormRelatedFindActions(mode == IModeManager.FIND_MODE);

			Action cmdfindmode = actions.get("cmdfindmode"); //$NON-NLS-1$
			if (cmdfindmode != null) cmdfindmode.setEnabled(mode == IModeManager.EDIT_MODE);

			if (mode == IModeManager.FIND_MODE)
			{
				ableFormRelatedBrowseActions(false);
				ableFormRelatedDataEditActions(true);
			}
			else
			{
				ableFormRelatedBrowseActions(mode == IModeManager.EDIT_MODE);
			}
		}
		else if ("formCreated".equals(name)) //$NON-NLS-1$
		{
			ableFormRelatedActions(evt.getNewValue() != null);
		}
		else if ("undomanager".equals(name)) //$NON-NLS-1$
		{
			String sUndoRedo = (String)evt.getOldValue();
			Boolean bValue = (Boolean)evt.getNewValue();
			Action menuUndoRedo = actions.get(sUndoRedo);
			if (menuUndoRedo != null) menuUndoRedo.setEnabled(bValue.booleanValue());
		}
	}

	protected void ableFormRelatedActions(boolean enable)
	{
//		Action menuwindowaction = (Action) actions.get("menuwindowaction"); 
//		if (menuwindowaction != null) menuwindowaction.setEnabled(enable);

		Action menumethodsaction = actions.get("menumethodsaction"); //$NON-NLS-1$
		if (menumethodsaction != null) menumethodsaction.setEnabled(enable);

		Action menuviewaction = actions.get("menuviewaction"); //$NON-NLS-1$
		if (menuviewaction != null) menuviewaction.setEnabled(enable);

		Action menuselectaction = actions.get("menuselectaction"); //$NON-NLS-1$
		if (menuselectaction != null) menuselectaction.setEnabled(enable);

		//enable modes
		Action cmdbrowsemode = actions.get("cmdbrowsemode"); //$NON-NLS-1$
		if (cmdbrowsemode != null) cmdbrowsemode.setEnabled(enable);
		Action cmdpreviewmode = actions.get("cmdpreviewmode"); //$NON-NLS-1$
		if (cmdpreviewmode != null) cmdpreviewmode.setEnabled(enable);
		Action cmdfindmode = actions.get("cmdfindmode"); //$NON-NLS-1$
		if (cmdfindmode != null) cmdfindmode.setEnabled(enable);

		Action action = actions.get("cmdpaste"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);
		action = actions.get("cmdcopy"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);
		action = actions.get("cmdcut"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);
		action = actions.get("cmdselectall"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);

		action = actions.get("cmdshowscriptdebugger"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);
		action = actions.get("cmdshowi18n"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);
		action = actions.get("cmdshowmethods"); //$NON-NLS-1$
		if (action != null) action.setEnabled(enable);
	}

	protected void ableFormRelatedDataEditActions(boolean enable)
	{
		Action cmdfindall = actions.get("cmdfindall"); //$NON-NLS-1$
		if (cmdfindall != null) cmdfindall.setEnabled(enable);

		Action cmdsavedata = actions.get("cmdsavedata"); //$NON-NLS-1$
		if (cmdsavedata != null) cmdsavedata.setEnabled(enable);

		Action cmdrecopyvalues = actions.get("cmdrecopyvalues"); //$NON-NLS-1$
		if (cmdrecopyvalues != null) cmdrecopyvalues.setEnabled(enable);

		Action cmddeleterecord = actions.get("cmddeleterecord"); //$NON-NLS-1$
		if (cmddeleterecord != null) cmddeleterecord.setEnabled(enable);

		Action cmdnewrecord = actions.get("cmdnewrecord"); //$NON-NLS-1$
		if (cmdnewrecord != null) cmdnewrecord.setEnabled(enable);

		Action cmdduplicaterecord = actions.get("cmdduplicaterecord"); //$NON-NLS-1$
		if (cmdduplicaterecord != null) cmdduplicaterecord.setEnabled(enable);

		Action cmdnextrecord = actions.get("cmdnextrecord"); //$NON-NLS-1$
		if (cmdnextrecord != null) cmdnextrecord.setEnabled(enable);

		Action cmdprevrecord = actions.get("cmdprevrecord"); //$NON-NLS-1$
		if (cmdprevrecord != null) cmdprevrecord.setEnabled(enable);
	}

	public void ableFormRelatedFindActions(boolean enable)
	{
		FormManager fm = (FormManager)application.getFormManager();
		IForm[] visibleRootFormsInFind = (fm != null) ? fm.getVisibleRootFormsInFind() : null;
		boolean thereAreUsableVisibleFormsInFind = ((visibleRootFormsInFind != null) && (visibleRootFormsInFind.length > 0));

		// if there are visible forms that are still in find mode, even though enabled = false (for example
		// when another form exits find mode - see unrelated tab panels) the actions must still remain enabled (although it is a strange case)
		boolean shouldPerform = false;
		if (thereAreUsableVisibleFormsInFind)
		{
			for (IForm f : visibleRootFormsInFind)
			{
				if (!(f instanceof FormController) || ((FormController)f).getForm() == null || ((FormController)f).getForm().getOnSearchCmdMethodID() >= 0)
				{
					shouldPerform = true;
					break;
				}
			}
		}
		else
		{
			shouldPerform = true;
		}
		// if we want to enable find actions we check for onsearchcmd of the form; if set to none, actions are not enabled
		if (shouldPerform)
		{
			Action cmdperformfind = actions.get("cmdperformfind"); //$NON-NLS-1$
			if (cmdperformfind != null) cmdperformfind.setEnabled(enable || thereAreUsableVisibleFormsInFind);
			Action cmdreducefind = actions.get("cmdreducefind"); //$NON-NLS-1$
			if (cmdreducefind != null) cmdreducefind.setEnabled(enable || thereAreUsableVisibleFormsInFind);
			Action cmdextendfind = actions.get("cmdextendfind"); //$NON-NLS-1$
			if (cmdextendfind != null) cmdextendfind.setEnabled(enable || thereAreUsableVisibleFormsInFind);
		}

		// this action must be enabled even if onsearchcmd is set to none...
		Action cmdstopsearchfindall = actions.get("cmdstopsearchfindall"); //$NON-NLS-1$
		if (cmdstopsearchfindall != null) cmdstopsearchfindall.setEnabled(enable);
	}

	protected void ableFormRelatedBrowseActions(boolean enable)
	{
		FormManager fm = (FormManager)application.getFormManager();
		int view = FormController.LOCKED_LIST_VIEW;
		if (enable)
		{
			IForm formPanel = fm.getCurrentForm();
			if (formPanel != null)
			{
				view = formPanel.getView();
			}
		}
		Action cmdviewasform = actions.get("cmdviewasform"); //$NON-NLS-1$
		if (cmdviewasform != null) cmdviewasform.setEnabled(view < 3);
		Action cmdviewaslist = actions.get("cmdviewaslist"); //$NON-NLS-1$
		if (cmdviewaslist != null) cmdviewaslist.setEnabled(view < 3);
//		Action cmdviewastable = (Action) actions.get("cmdviewastable"); //$NON-NLS-1$
//		if (cmdviewastable != null) cmdviewastable.setEnabled(enable);//TODO:enable when implemented

//		Action cmdduplicaterecord = (Action) actions.get("cmdduplicaterecord"); //$NON-NLS-1$
//		if (cmdduplicaterecord != null) cmdduplicaterecord.setEnabled(enable);
//		Action cmdomitrecord = (Action) actions.get("cmdomitrecord"); //$NON-NLS-1$
//		if (cmdomitrecord != null) cmdomitrecord.setEnabled(enable);
//		Action cmdrevertrecords = (Action) actions.get("cmdrevertrecords"); //$NON-NLS-1$
//		if (cmdrevertrecords != null) cmdrevertrecords.setEnabled(enable);
//		Action cmdshowomitrecords = (Action) actions.get("cmdshowomitrecords"); //$NON-NLS-1$
//		if (cmdshowomitrecords != null) cmdshowomitrecords.setEnabled(enable);
//		Action cmdsort = (Action) actions.get("cmdsort"); //$NON-NLS-1$
//		if (cmdsort != null) cmdsort.setEnabled(enable);
//		Action cmddeleteallrecord = (Action) actions.get("cmddeleteallrecord"); //$NON-NLS-1$
//		if (cmddeleteallrecord != null) cmddeleteallrecord.setEnabled(enable);
//
//		Action cmdnextrecord = (Action) actions.get("cmdnextrecord"); //$NON-NLS-1$
//		if (cmdnextrecord != null) cmdnextrecord.setEnabled(enable);
//
//		Action cmdprevrecord = (Action) actions.get("cmdprevrecord"); //$NON-NLS-1$
//		if (cmdprevrecord != null) cmdprevrecord.setEnabled(enable);

		if (fm instanceof SwingFormManager)
		{
			((SwingFormManager)fm).enableCmds(enable);
		}

		//if plugins have added menus
		if (application.getImportMenu().getMenuComponents().length != 0)
		{
			Action a = actions.get("menuimportaction"); //$NON-NLS-1$
			if (a != null) a.setEnabled(enable);
		}
		if (application.getExportMenu().getMenuComponents().length != 0)
		{
			Action a = actions.get("menuexportaction"); //$NON-NLS-1$
			if (a != null) a.setEnabled(enable);
		}
	}

	/**
	 * The shell for Cmds. This method executes the given Cmd in response to the given event (some Cmds look at the Event that invoke them, even though this is
	 * discouraged). The Editor executes the Cmd in a safe environment so that buggy actions cannot crash the whole App.
	 */
	public void executeCmd(final ICmd c, final java.util.EventObject ie)
	{
		if (c == null) return;
		if (stopTime != 0 && System.currentTimeMillis() > stopTime)
		{
			JOptionPane.showMessageDialog(application.getMainApplicationFrame(),
				application.getI18NMessage("servoy.license.text"), application.getI18NMessage("servoy.license.label"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
//					//save any outstanding change,THIS ALSO DOES UI UPDATES IN 'saveData'!!!! 
//					FormManager fm = application.getFormManager();
//					if (fm != null)
//					{
//						IDataManipulator dm = fm.getCurrentMainDataManipulator();
//						if (dm != null) dm.saveData();//make sure all data is saved on close();
//					}

					UndoableEdit ue = c.doIt(ie);
					if (ue != null && undoManager != null && ue.canUndo())
					{
						undoManager.addEdit(ue);
					}
				}
			});
		}
		catch (java.lang.Throwable ex)
		{
			Debug.error("While executing " + c + //$NON-NLS-1$
				" on event " + ie + //$NON-NLS-1$
				" the following error occured:"); //$NON-NLS-1$
			Debug.error(ex);
		}
	}

	private long stopTime = 0;

	public void setTrailMode(int time)
	{
		if (time != -1)
		{
			stopTime = System.currentTimeMillis() + time;
		}
		else
		//if (stopTime != 0)
		{
			stopTime = 0;
		}
	}

	private boolean autoOpenSolutionSelectDialog = true;

	public void setAutoOpenSolutionSelectDialog(boolean b)
	{
		autoOpenSolutionSelectDialog = b;
	}

	/**
	 * Does assume that the registerd action is a ICmd
	 */
	public void executeRegisteredAction(String name)
	{
		Action action = actions.get(name.toLowerCase());
		action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "hmm")); //$NON-NLS-1$
//		if (action != null && action instanceof ICmd)
//		{
//			executeCmd((ICmd)action,null);
//		}
	}

	/**
	 * Returns the action todo the undo, when executed it does one step back in history when possible.
	 */
	public Action getUndoAction()
	{
		return undoAction;
	}

	/**
	 * Return the action todo the redo, when executed it does one step forward when possible.
	 */
	public Action getRedoAction()
	{
		return redoAction;
	}

	public void undo()
	{
		if (undoManager != null && undoManager.canUndo())
		{
			undoManager.undo();
		}
	}

	public void redo()
	{
		if (undoManager != null && undoManager.canRedo())
		{
			undoManager.redo();
		}
	}

	public void addUndoableEdit(UndoableEdit undoableEdit)
	{
		if (undoableEdit != null && undoManager != null)
		{
			undoManager.addEdit(undoableEdit);
		}
	}

	public void flushCachedItems()
	{
//		actions = new HashMap(); to dangerous
	}

	/**
	 * @see com.servoy.j2db.IManager#init()
	 */
	public void init()
	{
		//ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.cmd.ICmdManager#refresh()
	 */
	public void i18nRefresh()
	{
		Iterator<Action> it = actions.values().iterator();
		while (it.hasNext())
		{
			MessageTextAction action = (MessageTextAction)it.next();
			action.refresh();
		}
	}
}
