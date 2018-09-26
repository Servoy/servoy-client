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
package com.servoy.j2db.util.xmlxport;

import com.servoy.j2db.persistence.InfoChannel;


/**
 * The <code>IXMLImportUserChannel</code> interface is used to send info messages and to delegate any decisions needed during the import to the user.
 *
 */
public interface IXMLImportUserChannel extends InfoChannel
{

	/**
	 * The constant indicating that a "skip" or "cancel" action was taken by the user.
	 */
	public static final int CANCEL_ACTION = 1;

	/**
	 * The constant indicating that an "ok" action was taken by the user.
	 */
	public static final int OK_ACTION = 2;

	/**
	 * The constant indicating that an "overwrite" or "replace" action was taken by the user.
	 */
	public static final int OVERWRITE_ACTION = 3;

	/**
	 * The constant indicating that a "rename" action was taken by the user.
	 */
	public static final int RENAME_ACTION = 4;

	/**
	 * The constant indicating that the situation has changed and the operation now does have a chance of succeeding.
	 */
	public static final int RETRY_ACTION = 5;

	public static final int SKIP_ACTION = 6;

	public static final int CREATE_ACTION = 7;

	public static final int IMPORT_USER_POLICY_DONT = 0;
	public static final int IMPORT_USER_POLICY_CREATE_U_UPDATE_G = 1;
	public static final int IMPORT_USER_POLICY_OVERWRITE_COMPLETELY = 2;

	/**
	 * Ask which server the user wants to use to import user data that is contained in the repository server in the import.
	 *
	 * @return the name of the server to use, null to cancel
	 */
	public String askServerForRepositoryUserData();


	/**
	 * Ask which server the user wants to use for the import user data
	 *
	 * @param importServerName the name of the server from the import
	 * @return the name of the server to use
	 */
	public String askServerForImportUserData(String importServerName);

	/**
	 * This method asks the user what to do when a server needed in the import is not found. Valid actions are <code>CANCEL_ACTION</code> in which case a
	 * repository exception is thrown, a <code>RENAME_ACTION</code> in which case the import expects the new name specified by the user to be returned by the
	 * next call to <code>getNewName()</code>, and a <code>RETRY_ACTION</code> indicating the server has been created in the mean time and and we can use
	 * the current name (the import WILL check again to make sure it exists, calling this method again if it does not).
	 *
	 * @param name the name of the server that could not be found
	 */
	public int askUnknownServerAction(String name);

	public int askStyleAlreadyExistsAction(String name);

	/**
	 * This method asks the user what to do when the media in the import has different name than the media in the repository by the same uuid. Valid actions are
	 * <code>CANCEL_ACTION</code> in which case the media is skipped, or the <code>OVERWRITE_ACTION</code> in which case the existing media is deleted.
	 *
	 * @param name name of the media which has changed
	 */
	public int askMediaNameCollisionAction(String name);

	/**
	 * This method asks the user what to do when the media in the import is different from the media in the repository with the same uuid. Valid actions are
	 * <code>CANCEL_ACTION</code> in which case the media is skipped, or the <code>OVERWRITE_ACTION</code> in which case the media is overwritten.
	 *
	 * @param name name of the media which has changed
	 */
	public int askMediaChangedAction(String name);

	/**
	 * This method asks the user what to do when a group in the import already exists in the local repository. Valid actions are <code>CANCEL_ACTION</code> in
	 * which case the group is skipped, or the <code>OVERWRITE_ACTION</code> in which case the elements pertaining to the imported solution are deleted in the
	 * local repository and taken from the import.
	 */
	public int askGroupAlreadyExistsAction(String name);

	/**
	 * This method asks the user what to do when a solution by the specified name already exists when trying to import a solution to a specified name. Valid
	 * actions are <code>CANCEL_ACTION</code> which will cause a repository exception, and a <code>RENAME_ACTION</code> in which case the import expects the
	 * new name specified by the user to be returned by the next call to getNewName().
	 *
	 * @param name name of the solution that already exists
	 */
	public int askRenameRootObjectAction(String name, int objectTypeId);

	/**
	 * This method asks the user what to do when the solution name in the import does not match the solution name in the local repository. Valid actions are
	 * <code>CANCEL_ACTION</code> which will cause a repository exception, an <code>OK_ACTION</code> in which case the local name is used, or a
	 * <code>SKIP_ACTION</code> in which case the entire root object is skipped.
	 *
	 * @param name name of the solution in the import
	 * @param localName name of the solution in the local repository
	 */
	public int askImportRootObjectAsLocalName(String name, String localName, int objectTypeId);

	/**
	 * This method asks the user for the import policy given whether or not there is revision information available. The policy should determine the results of
	 * the <code>getMergeEnabled()</code>, the <code>getUseLocalOnMergeConflict()</code>, the <code>getOverwriteEnabled()</code>, and the
	 * <code>getDeleteEnabled</code> methods. As return value we expect <code>OK_ACTION</code> or <code>CANCEL_ACTION</code>.
	 *
	 * @param hasRevisions flag indicating if relevant revision information was found in the import
	 */
	public int askForImportPolicy(String rootObjectName, int rootObjectType, boolean hasRevisions);

	/**
	 * This method returns whether to allow SQL keywords in table and column names on import. Return <code>OK_ACTION</code> to allow,
	 * <code>CANCEL_ACTION</code> to disallow.
	 *
	 * @return whether or not to allow sql keywords
	 */
	public int askAllowSQLKeywords();

	/**
	 * This method returns whether to import included meta data or not. Return <code>OK_ACTION</code> to import meta data, <code>CANCEL_ACTION</code> to
	 * skip it.
	 *
	 * @return whether or not to import included meta data
	 */
	public int askImportMetaData();

	/**
	 * This method returns whether to import included sample data or not. Return <code>OK_ACTION</code> to import sample data, <code>CANCEL_ACTION</code> to
	 * skip it.
	 *
	 * @return whether or not to import included sample data
	 */
	public int askImportSampleData();


	/**
	 * This method asks for the I18N policy, i.e. whether to import it or not, and whether to insert new keys only. This method must determine the result of a
	 * subsequent call to getInsertNewI18NKeysOnly.
	 *
	 * Return <code>OK_ACTION</code> to import it or <code>CANCEL_ACTION</code> to skip it.
	 */
	public int askImportI18NPolicy();

	/**
	 * This method returns whether to insert all I18N keys in the import or new keys only.
	 */
	public boolean getInsertNewI18NKeysOnly();

	/**
	 * Ask whether to override sequence types as specified by the import for the specified server. Return OK_ACTION to override or CANCEL_ACTION to keep the
	 * existing.
	 *
	 * @param serverName
	 */
	public int askOverrideDefaultValues(String serverName);

	/**
	 * Ask whether to override default values as specified by the import for the specified server. Return OK_ACTION to override or CANCEL_ACTION to keep the
	 * existing.
	 *
	 * @param serverName
	 */
	public int askOverrideSequenceTypes(String serverName);

	/**
	 * This method should return true if the user has chosen to try and merge the import with the current solution, false otherwise. Note that if there is no
	 * revision history available, this method may return anything at all.
	 *
	 * @return whether or merging is enabled
	 */
	public boolean getMergeEnabled();

	/**
	 * This method should return true if the user has chosen to use the local version of an object when a merge conflict arises and false if the user wishes to
	 * use the remote version on a merge conflict. Note that if there is no revision history available, this method may return anything at all.
	 *
	 * @return whether to use the local version on a merge conflict
	 */
	public boolean getUseLocalOnMergeConflict();

	/**
	 * This method should return true if the user has chosen not to use merging and the user wishes to overwrite top level objects (which have the solution as
	 * parent) with the import version if the object also exists locally. Note that if merging is enabled this method may return anything at all.
	 *
	 * @return whether to overwrite top level objects with the import version
	 */
	public boolean getOverwriteEnabled();

	/**
	 * This method should return true if the user has chosen not to use merging and the user wishes to delete top level objects (which have the solution as
	 * parent) which do not exist in the import version but do exist locally. Note that if merging is enabled this method may return anything at all.
	 *
	 * @return whether to delete top level objects not in the import version
	 */
	public boolean getDeleteEnabled();

	/**
	 * This method returns the new name after a <code>RENAME_ACTION</code> was returned by one of the methods which asks the user for an action.
	 *
	 * @return the new name after a rename action
	 */
	public String getNewName();

	/**
	 * This method returns the value to set mustAuthenticate to.
	 *
	 * @param the value of the mustAuthenticate flag from the import
	 *
	 * @return the new value of the mustAuthenticate flag
	 */
	public boolean getMustAuthenticate(boolean importMustAuthenticate);

	/**
	 * This method asks for the user import policy, i.e. whether to import users or not, and whether to override existing users. This method must determine the
	 * result of a subsequent call to getAddUsersToAdminstratorGroup.
	 *
	 * Return the import policy. 0 skip 1 add new groups 2 override user (delete first, then add)
	 */
	public int askUserImportPolicy();

	/**
	 * @return whether or not users will be added to the admin group.
	 */
	public boolean getAddUsersToAdministratorGroup();

	/**
	 * This method notifies the user channel that some (undefined chunk of) work has has been done.
	 *
	 * @param total amount how much work has been done (out of 1.00), less than 0 if unknown
	 */
	public void notifyWorkDone(float amount);

	/**
	 * Prompt the user for a protection password.
	 */
	public String askProtectionPassword(String solutionName);

	public int getAllowDataModelChange(String serverName);

	public boolean getDisplayDataModelChange();

	public boolean getSkipDatabaseViewsUpdate();

	/**
	 * Asks if the sequences should be updated for the servers involved in the import.
	 * If OK_ACTION is returned then the sequences will be updated, otherwise they wont be touched.
	 */
	public int askUpdateSequences();

	public String getImporterUsername();


	/**
	 * Should the importer first compact the solutions or not
	 *
	 * @return boolean true if it should first compact
	 */
	public boolean compactSolutions();

	public boolean allowImportEmptySolution();
}
