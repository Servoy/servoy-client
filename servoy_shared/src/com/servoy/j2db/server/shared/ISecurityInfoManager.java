package com.servoy.j2db.server.shared;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.servoy.j2db.ISolutionSecurityManager.SecurityAccessInfo;
import com.servoy.j2db.ISolutionSecurityManager.TableAndFormSecurityAccessInfo;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistIdentifier;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Common interface for managing security info for users/groups.
 * Provides default implementations for shared logic between user manager classes.
 */
public interface ISecurityInfoManager
{

	/**
	 * Add the accessMask & identifier or replace them if the identifier already exists.
	 *
	 * @return true if the identifier existed and was replaced; false if the info was just added.
	 */
	default boolean setSecurityInfo(List<SecurityInfo> securityInfo, String element_uid, int accessMask)
	{
		boolean replaced = false;
		Iterator<SecurityInfo> it = securityInfo.iterator();
		while (it.hasNext())
		{
			if (it.next().element_uid.equals(element_uid))
			{
				replaced = true;
				it.remove();
			}
		}
		securityInfo.add(new SecurityInfo(element_uid, accessMask));
		return replaced;
	}

	/**
	 * Classes must implement this to provide their groupInfos list.
	 */
	List<GroupSecurityInfo> getGroupInfos();

	/**
	 * Default implementation to find a group by name.
	 */
	default GroupSecurityInfo getGroupSecurityInfo(String group)
	{
		if (group == null) return null;
		for (GroupSecurityInfo gsi : getGroupInfos())
		{
			if (gsi.getName().equals(group)) return gsi;
		}
		return null;
	}

	/**
	 * Default implementation to add or update form security access for a group.
	 */
	default void addFormSecurityAccess(String groupName, Integer accessMask, String elementUUID, UUID formUuid)
	{
		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);
		if (gsi != null)
		{
			java.util.List<SecurityInfo> securityInfo = gsi.formSecurity.get(formUuid);
			if (securityInfo == null)
			{
				securityInfo = new java.util.ArrayList<>();
				gsi.formSecurity.put(formUuid, securityInfo);
			}
			setSecurityInfo(securityInfo, elementUUID, accessMask.intValue());
		}
		else
		{
			Debug.warn("addFormSecurityAccess(...) cannot find the group with the given name!");
		}
	}

	default Pair<Map<Object, Integer>, Set<Object>> getSecurityAccess(String clientId, UUID[] solution_uuids, int[] releaseNumbers, String[] groups)
		throws ServoyException
	{
		Map<Object, Integer> explicitRights = new HashMap<Object, Integer>();
		Set<Object> implicitRights = new HashSet<>();

		gatherSecurityAccess(new SecAccessGatherer<String>()
		{

			@Override
			public void explicitFormElementAccessFoundForMerge(String uid, int element_access)
			{
				UUID elementUUID = null;
				try
				{
					elementUUID = UUID.fromString(uid);
				}
				catch (IllegalArgumentException e)
				{
					// it's not an UUID, it's probably an FCC child that is a PersistIdentifier.toJSONString()
					// and this deprecated getSecurityAccess(...) does not support that; skip
				}
				if (elementUUID != null) explicitRights.put(elementUUID, Integer.valueOf(Utils.getAsInteger(explicitRights.get(elementUUID)) | element_access));
			}

			@Override
			public void implicitFormElementAccess(String uid)
			{
				UUID elementUUID = null;
				try
				{
					elementUUID = UUID.fromString(uid);
				}
				catch (IllegalArgumentException e)
				{
					// it's not an UUID, it's probably an FCC child that is a PersistIdentifier.toJSONString()
					// and this deprecated getSecurityAccess(...) does not support that; skip
				}
				if (elementUUID != null) implicitRights.add(elementUUID);
			}

			@Override
			public void explicitColumnAccessFoundForMerge(CharSequence qualifiedColumn, int columninfo_access)
			{
				explicitRights.put(qualifiedColumn, Integer.valueOf(Utils.getAsInteger(explicitRights.get(qualifiedColumn)) | columninfo_access));
			}

			@Override
			public void implicitColumnAccess(CharSequence lastQualifiedColumn)
			{
				implicitRights.add(lastQualifiedColumn);
			}

		}, clientId, solution_uuids, releaseNumbers, groups);

		return new Pair<Map<Object, Integer>, Set<Object>>(explicitRights, implicitRights);
	}

	default boolean formIsChildOfPersist(IPersist persist, final UUID formUUID)
	{
		Object result = null;
		result = persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (formUUID.equals(o.getUUID()))
				{
					return o.getTypeID() == IRepository.FORMS ? Boolean.TRUE : Boolean.FALSE;
				}
				return CONTINUE_TRAVERSAL;
			}
		});
		if (result instanceof Boolean)
		{
			return ((Boolean)result).booleanValue();
		}
		return false;
	}

	default void setTableSecurityAccess(String clientId, String groupName, Integer accessMask, String connectionName, String tableName, String columnName,
		boolean isOperational, boolean addMissingGroup)
		throws ServoyException
	{
		if (groupName == null || groupName.length() == 0 || accessMask == null || !isOperational)
		{
			Debug.error("Invalid parameters received, or manager is not operational - setTableSecurityAccess(...)");
			return;
		}

		checkForAdminUser(clientId, null);

		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);
		if (gsi == null)
		{
			if (!addMissingGroup)
			{
				Debug.warn("setTableSecurityAccess(...) cannot find the group with the given name!: " + groupName);
				return;
			}
			getGroupInfos().add(new GroupSecurityInfo(groupName));
			gsi = getGroupSecurityInfo(groupName);
		}
		List<SecurityInfo> securityInfo = gsi.tableSecurity.get(Utils.getDotQualitfied(connectionName, tableName));
		if (securityInfo == null)
		{
			securityInfo = new SortedList<SecurityInfo>();
			gsi.tableSecurity.put(Utils.getDotQualitfied(connectionName, tableName), securityInfo);
		}
		setSecurityInfo(securityInfo, columnName, accessMask.intValue());
		writeSecurityInfoIfNeeded(connectionName, tableName, false);
	}

	default void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, String elementUUID, boolean isOperational,
		boolean addMissingGroup, Solution solution)
		throws ServoyException
	{
		if (groupName == null || groupName.length() == 0 || accessMask == null || elementUUID == null || solution == null || !isOperational)
		{
			Debug
				.error("Invalid parameters: permision: " + groupName + ", accessMask: " + accessMask + ", element: " + elementUUID + ", solution: " + solution +
					" received, or manager is not operational: " + isOperational + " - setFormSecurityAccess(...)");
			return;
		}

		checkForAdminUser(clientId, null);

		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);
		if (gsi == null)
		{
			if (!addMissingGroup)
			{
				Debug.warn("setFormSecurityAccess(...) cannot find the group with the given name!: " + groupName);
				return;
			}
			getGroupInfos().add(new GroupSecurityInfo(groupName));
			gsi = getGroupSecurityInfo(groupName);
		}

		// now we must find the Form's UUID from the elementUUID
		Form form = null;
		if (solution != null)
		{
			Iterator<Form> it = solution.getForms(null, false);
			while (it.hasNext())
			{
				Form f = it.next();
				if (isElementChildOfForm(elementUUID.toString(), f))
				{
					form = f;
				}
			}
		}

		if (form != null)
		{
			addFormSecurityAccess(groupName, accessMask, elementUUID, form.getUUID());

			writeSecurityInfoIfNeeded(form, false);
		}
		else
		{
			Debug.warn("setFormSecurityAccess(...) cannot find the form for the given element UUID!: " + elementUUID);
		}
	}

	default void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID formUUID, String elementUUID, boolean isOperational,
		boolean addMissingGroup, Solution solution)
		throws ServoyException, RemoteException
	{
		if (groupName == null || groupName.length() == 0 || accessMask == null || elementUUID == null || solution == null || !isOperational)
		{
			Debug
				.error("Invalid parameters: permision: " + groupName + ", accessMask: " + accessMask + ", element: " + elementUUID + ", solution: " + solution +
					" received, or manager is not operational: " + isOperational + " - setFormSecurityAccess(...)");
			return;
		}

		checkForAdminUser(clientId, null);

		GroupSecurityInfo gsi = getGroupSecurityInfo(groupName);
		if (gsi == null)
		{
			if (!addMissingGroup)
			{
				Debug.warn("setFormSecurityAccess(...) cannot find the group with the given name!: " + groupName);
				return;
			}
			getGroupInfos().add(new GroupSecurityInfo(groupName));
			gsi = getGroupSecurityInfo(groupName);
		}

		// now we must find the Form's UUID from the elementUUID
		Form form = null;

		Iterator<Form> it = solution.getForms(null, false);
		while (it.hasNext())
		{
			Form f = it.next();
			if (formUUID.equals(f.getUUID()))
			{
				form = f;
				break;
			}
		}

		if (form != null)
		{
			addFormSecurityAccess(groupName, accessMask, elementUUID, form.getUUID());

			writeSecurityInfoIfNeeded(form, false);
		}
		else
		{
			Debug.warn("setFormSecurityAccess(...) cannot find the form for the given element UUID!: " + elementUUID);
		}
	}

	default boolean isElementChildOfForm(final String elementUUID, Form form)
	{
		// if the elementUID is nested inside a form component component, we need to check if the top-most form component component parent is
		// part of this form; otherwise uidOfFormLevelElement will be the same as elementUID (an UUID string representation) - for a normal component of the form
		String uidOfFormLevelElement = PersistIdentifier.fromJSONString(elementUUID).persistUUIDAndFCPropAndComponentPath()[0];
		Object retVal = form.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (uidOfFormLevelElement.equals(o.getUUID().toString()))
				{
					return Boolean.TRUE;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		return retVal == Boolean.TRUE;
	}

	default TableAndFormSecurityAccessInfo getSecurityAccessForTablesAndForms(String clientId, UUID[] solution_uuids, int[] releaseNumbers, String[] groups)
		throws ServoyException
	{
		TableAndFormSecurityAccessInfo newWaySecAccess = new TableAndFormSecurityAccessInfo(new SecurityAccessInfo(new HashMap<>(), new HashSet<>()),
			new SecurityAccessInfo(new HashMap<>(), new HashSet<>()));

		gatherSecurityAccess(new SecAccessGatherer<String>()
		{

			@Override
			public void explicitFormElementAccessFoundForMerge(String uid, int element_access)
			{
				newWaySecAccess.formSecurityAccessInfo().explicitIdentifierToAccessMap().put(uid, Integer
					.valueOf(Utils.getAsInteger(newWaySecAccess.formSecurityAccessInfo().explicitIdentifierToAccessMap().get(uid)) | element_access));
			}

			@Override
			public void implicitFormElementAccess(String uid)
			{
				newWaySecAccess.formSecurityAccessInfo().implicitAccessIdentifiers().add(uid);
			}

			@Override
			public void explicitColumnAccessFoundForMerge(CharSequence qualifiedColumn, int columninfo_access)
			{
				newWaySecAccess.tableSecurityAccessInfo().explicitIdentifierToAccessMap().put(qualifiedColumn.toString(), Integer.valueOf(
					Utils.getAsInteger(newWaySecAccess.tableSecurityAccessInfo().explicitIdentifierToAccessMap().get(qualifiedColumn.toString())) |
						columninfo_access));
			}

			@Override
			public void implicitColumnAccess(CharSequence lastQualifiedColumn)
			{
				newWaySecAccess.tableSecurityAccessInfo().implicitAccessIdentifiers().add(lastQualifiedColumn.toString());
			}
		}, clientId, solution_uuids, releaseNumbers, groups);

		return newWaySecAccess;
	}

	default void gatherSecurityAccess(SecAccessGatherer<String> gatherer, String clientId, UUID[] solution_uuids, int[] releaseNumbers,
		String[] groups) throws ServoyException
	{
		// first take legacy sec. info from tables (this used to be done after the rest of the code in old deprecated getSecurityAccess
		// that added these to the resulting map still first (so it has lower prio / gets overwritten by the war in-memory security data)
		// now we give it to the "gatherer" first so that it can be overwritten as well
		gatherUserManagerSecurityAccess(gatherer, clientId, solution_uuids, releaseNumbers, groups);

		// now get the real war kept security data! NOTE: this data is added to this manager through the setters each time the war starts
		Map<String, Integer> groupCountWithNonDefaultAccessForFormsAndComponents = new HashMap<>();
		Map<String, Integer> groupCountWithNonDefaultAccessForTableColumns = new HashMap<>();

		for (int i = 0; i < solution_uuids.length; i++)
		{
			UUID solution_uuid = solution_uuids[i];
			int releaseNumber = releaseNumbers[i];

			IRootObject solution = null;
			if (solution_uuid != null)
			{
				try
				{
					solution = ApplicationServerRegistry.get().getLocalRepository().getActiveRootObject(solution_uuid);
				}
				catch (RepositoryException e)
				{
					Debug.error("Cannot get security access for solution with id, release = " + solution_uuid + ", " + releaseNumber, e);
					return;
				}
				if (solution == null)
				{
					Debug.error("Cannot get security access because of missing solution with id, release = " + solution_uuid + ", " + releaseNumber, null);
					return;
				}
			}

			if (groups != null)
			{
				for (String group : groups)
				{
					GroupSecurityInfo gsi = getGroupSecurityInfo(group);
					if (gsi != null && solution != null)
					{
						for (Entry<UUID, List<SecurityInfo>> formSecurityEntry : gsi.formSecurity.entrySet())
						{
							UUID formUUID = formSecurityEntry.getKey();
							if (formIsChildOfPersist(solution, formUUID))
							{
								List<SecurityInfo> lsi = formSecurityEntry.getValue();
								for (SecurityInfo si : lsi)
								{
									String uid = si.element_uid;

									Integer old = groupCountWithNonDefaultAccessForFormsAndComponents.get(uid);
									if (old == null)
									{
										groupCountWithNonDefaultAccessForFormsAndComponents.put(uid, Integer.valueOf(1));
									}
									else
									{
										groupCountWithNonDefaultAccessForFormsAndComponents.put(uid, Integer.valueOf(old.intValue() + 1));
									}

									gatherer.explicitFormElementAccessFoundForMerge(uid, si.access);
								}
							}
						}
					}
				}
			}
		}

		for (String group : groups)
		{
			GroupSecurityInfo gsi = getGroupSecurityInfo(group);
			if (gsi == null) continue;
			for (Entry<String, List<SecurityInfo>> entry : gsi.tableSecurity.entrySet())
			{
				String s_t = entry.getKey();
				List<SecurityInfo> lsi = entry.getValue();
				for (SecurityInfo si : lsi)
				{
					String cid = Utils.getDotQualitfied(s_t, si.element_uid);

					Integer old = groupCountWithNonDefaultAccessForTableColumns.get(cid);
					if (old == null)
					{
						groupCountWithNonDefaultAccessForTableColumns.put(cid, Integer.valueOf(1));
					}
					else
					{
						groupCountWithNonDefaultAccessForTableColumns.put(cid, Integer.valueOf(old.intValue() + 1));
					}

					gatherer.explicitColumnAccessFoundForMerge(cid, si.access); // server.table.column -> int
				}
			}
		}
		if (groups.length > 1)
		{
			// implicit values for all values that are in the map that are not already VIEWABLE|ACCESSIBLE must be looked at if they would have that implicit value
			for (Entry<String, Integer> entry : groupCountWithNonDefaultAccessForTableColumns.entrySet())
			{
				if (entry.getValue().intValue() < groups.length)
				{
					gatherer.implicitColumnAccess(entry.getKey());
				}
			}
			for (Entry<String, Integer> entry : groupCountWithNonDefaultAccessForFormsAndComponents.entrySet())
			{
				if (entry.getValue().intValue() < groups.length)
				{
					gatherer.implicitFormElementAccess(entry.getKey());
				}
			}
		}
	}

	void gatherUserManagerSecurityAccess(SecAccessGatherer<String> gatherer, String clientId, UUID[] solution_uuids, int[] releaseNumbers,
		String[] groups)
		throws ServoyException;

	void checkForAdminUser(String clientId, String ownerUserId) throws RepositoryException;

	void writeSecurityInfoIfNeeded(String serverName, String tableName, boolean later) throws RepositoryException;

	void writeSecurityInfoIfNeeded(Form form, boolean later) throws RepositoryException;
}