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
package com.servoy.j2db.dataprocessing;


import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SerializableObject;

/**
 * @author sebster
 */
public final class ClientInfo implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String clientId;
	private String hostIdentifier;
	private String hostName;
	private String hostAddress;
	private int hostPort = -1;
	private int applicationType;

	private String specialClientIndentifier;

	private String userUid;
	private String userName;

	// last successful authentication
	private String authenticatorType;
	private String authenticatorMethod;
	private String jsCredentials;


	private TimeZone timeZone;

	private transient long loginTimestamp = 0;
	private transient long idleTimestamp = 0;
	private transient String[] groups;

	private long openSolutionTimestamp = 0;
	private int solutionReleaseNumber = -1;
	private int openSolutionId = -1;
	private List<String> infos = new ArrayList<String>();//to make it possible for developer to give a client a meaning full name/description in the admin page
	private Object tenantValue;

	private String solutionIntendedToBeLoaded;

	/**
	 * A private lock to synchronize read/write to the long valued properties. Since this lock is PRIVATE, all deadlock related issues are local to this class.
	 * Since the methods consist of 1 statement, the lock is always given up immediately and no deadlock on this lock can occur. The lock must be a serializable
	 * object because ClientInfo must be serializable, and the lock must always exist (and cannot be transient).
	 *
	 */
	private final SerializableObject lock = new SerializableObject();


	public ClientInfo()
	{
		initHostInfo();
	}

	/**
	 * @param clientInfo
	 */
	public ClientInfo(ClientInfo clientInfo)
	{
		// create a copy
		synchronized (lock)
		{
			clientId = clientInfo.clientId;
			hostIdentifier = clientInfo.hostIdentifier;
			hostName = clientInfo.hostName;
			hostAddress = clientInfo.hostAddress;
			hostPort = clientInfo.hostPort;
			applicationType = clientInfo.applicationType;

			specialClientIndentifier = clientInfo.specialClientIndentifier;

			userUid = clientInfo.userUid;
			userName = clientInfo.userName;

			timeZone = clientInfo.timeZone;

			openSolutionId = clientInfo.openSolutionId;
			infos = new ArrayList<String>(clientInfo.infos);
			solutionIntendedToBeLoaded = clientInfo.solutionIntendedToBeLoaded;
		}
	}

	public void addInfo(String info)
	{
		synchronized (lock)
		{
			if (!infos.contains(info)) infos.add(info);
		}
	}

	public boolean removeInfo(String info)
	{
		synchronized (lock)
		{
			return infos.remove(info);
		}
	}

	public void removeAllInfo()
	{
		synchronized (lock)
		{
			infos.clear();
		}
	}

	public void setInfos(List<String> infos)
	{
		synchronized (lock)
		{
			this.infos = infos;
		}
	}

	public List<String> getInfos()
	{
		synchronized (lock)
		{
			return infos;
		}
	}

	public void setClientId(String clientId)
	{
		synchronized (lock)
		{
			this.clientId = clientId;
		}
	}

	public String getClientId()
	{
		synchronized (lock)
		{
			return clientId;
		}
	}

	public void setHostIdentifier(String hostIdentifier)
	{
		synchronized (lock)
		{
			this.hostIdentifier = hostIdentifier;
		}
	}

	public String getHostIdentifier()
	{
		synchronized (lock)
		{
			return hostIdentifier;
		}
	}

	public void setUserUid(String userUid)
	{
		synchronized (lock)
		{
			this.userUid = userUid;
		}
	}

	public String getUserUid()
	{
		synchronized (lock)
		{
			return userUid;
		}
	}

	/**
	 * Returns the hostName.
	 *
	 * @return String
	 */
	public String getHostName()
	{
		synchronized (lock)
		{
			return hostName;
		}
	}

	/**
	 * Returns the hostAddress.
	 *
	 * @return String
	 */
	public String getHostAddress()
	{
		synchronized (lock)
		{
			return hostAddress;
		}
	}

	/**
	 * Sets the hostName.
	 *
	 * @param hostName The hostName to set
	 */
	public void setHostName(String hostname)
	{
		synchronized (lock)
		{
			this.hostName = hostname;
		}
	}

	/**
	 * Sets the hostAddress.
	 *
	 * @param hostAddress The hostAddress to set
	 */
	public void setHostAddress(String ipAddress)
	{
		synchronized (lock)
		{
			this.hostAddress = ipAddress;
		}
	}

	/**
	 * @return the timeZone
	 */
	public TimeZone getTimeZone()
	{
		return timeZone;
	}

	/**
	 * @param timeZone the timeZone to set
	 */
	public void setTimeZone(TimeZone timeZone)
	{
		this.timeZone = timeZone;
	}

	/**
	 * Returns the userName.
	 *
	 * @return String
	 */
	public String getUserName()
	{
		synchronized (lock)
		{
			return userName;
		}
	}

	/**
	 * Sets the userName.
	 *
	 * @param userName The userName to set
	 */
	public void setUserName(String userName)
	{
		synchronized (lock)
		{
			this.userName = userName;
		}
	}

	public String getSolutionIntendedToBeLoaded()
	{
		synchronized (lock)
		{
			return solutionIntendedToBeLoaded;
		}
	}

	/**
	 * This property is used only in the specific situation when pre-import and post-import
	 * hooks are being executed during solution import. In a common scenario by the time the
	 * post-import hook is executed the server will be in maintenance mode, so normally no
	 * client could connect. By setting this property, the server can check if the solution
	 * that is intended to be loaded is a pre/post-import hook and still let the client
	 * connect.
	 *
	 * @param solutionIntendedToBeLoaded The name of the solution to be loaded.
	 */
	public void setSolutionIntendedToBeLoaded(String solutionIntendedToBeLoaded)
	{
		synchronized (lock)
		{
			this.solutionIntendedToBeLoaded = solutionIntendedToBeLoaded;
		}
	}

	public int getOpenSolutionId()
	{
		synchronized (lock)
		{
			return openSolutionId;
		}
	}

	public int getSolutionReleaseNumber()
	{
		synchronized (lock)
		{
			return solutionReleaseNumber;
		}
	}

	public void setOpenSolutionId(int openSolutionId)
	{
		synchronized (lock)
		{
			this.openSolutionId = openSolutionId;
		}
	}

	public void setSolutionReleaseNumber(int releaseNumber)
	{
		synchronized (lock)
		{
			this.solutionReleaseNumber = releaseNumber;
		}
	}

	public void setLoginTimestamp(long loginTimestamp)
	{
		synchronized (lock)
		{
			this.loginTimestamp = loginTimestamp;
		}
	}

	public long getLoginTimestamp()
	{
		synchronized (lock)
		{
			return loginTimestamp;
		}
	}

	public void setOpenSolutionTimestamp(long openSolutionTimestamp)
	{
		synchronized (lock)
		{
			this.openSolutionTimestamp = openSolutionTimestamp;
		}
	}

	public long getOpenSolutionTimestamp()
	{
		synchronized (lock)
		{
			return openSolutionTimestamp;
		}
	}

	public void setIdleTimestamp(long idleTimestamp)
	{
		synchronized (lock)
		{
			this.idleTimestamp = idleTimestamp;
		}
	}

	public long getIdleTimestamp()
	{
		synchronized (lock)
		{
			return idleTimestamp;
		}
	}

	@Override
	public String toString()
	{
		synchronized (lock)
		{
			StringBuffer buffer = new StringBuffer();

			// User part
			if (userName != null)
			{
				buffer.append(userName);
				buffer.append('@');
			}
			else if (userUid != null)
			{
				buffer.append('[');
				buffer.append(userUid);
				buffer.append(']');
				buffer.append('@');
			}

			// Host part
			if (hostName != null)
			{
				buffer.append(hostName);
				if (hostAddress != null)
				{
					buffer.append('[');
					buffer.append(hostAddress);
					if (hostPort != -1)
					{
						buffer.append(':');
						buffer.append(hostPort);
					}
					buffer.append(']');
				}
			}
			else if (hostAddress != null)
			{
				buffer.append(hostAddress);
			}
			else
			{
				buffer.append("unknown"); //$NON-NLS-1$
			}

			return buffer.toString();
		}
	}

	public void initHostInfo()
	{
		timeZone = TimeZone.getDefault();
		synchronized (lock)
		{
			// TODO check a webclient never sets the timezone
			// so a webclient can always use the default of the server..
			try
			{
				InetAddress inetAddress = InetAddress.getLocalHost();
				hostName = inetAddress.getHostName();
				hostAddress = inetAddress.getHostAddress();
				hostIdentifier = hostName + '_' + hostAddress;
			}
			catch (UnknownHostException e)
			{
				hostIdentifier = "Failed " + new Date().getTime(); //$NON-NLS-1$
				Debug.log("Could not resolve local host: " + e.getMessage()); //$NON-NLS-1$
			}
			catch (Exception e)
			{
				hostIdentifier = "Failed " + new Date().getTime(); //$NON-NLS-1$
				Debug.error(e);
			}
		}
	}

	public String[] getUserGroups()
	{
		synchronized (lock)
		{
			return groups;
		}
	}

	public void setUserGroups(String[] g)
	{
		synchronized (lock)
		{
			groups = g;
		}
	}

	/**
	 *
	 */
	public void clearUserInfo()
	{
		synchronized (lock)
		{
			userUid = null;
			groups = null;
			userName = null;
			authenticatorType = null;
			authenticatorMethod = null;
			jsCredentials = null;
			tenantValue = null;
		}
	}

	public int getApplicationType()
	{
		synchronized (lock)
		{
			return this.applicationType;
		}
	}

	public void setApplicationType(int applicationType)
	{
		synchronized (lock)
		{
			this.applicationType = applicationType;
		}
	}

	/**
	 * @param specialClientIndentifier the specialClientIndentifier to set
	 */
	public void setSpecialClientIndentifier(String specialClientIndentifier)
	{
		synchronized (lock)
		{
			this.specialClientIndentifier = specialClientIndentifier;
		}
	}

	/**
	 * @return the specialClientIndentifier
	 */
	public String getSpecialClientIndentifier()
	{
		synchronized (lock)
		{
			return specialClientIndentifier;
		}
	}

	/**
	 * @param port
	 */
	public void setHostPort(int port)
	{
		synchronized (lock)
		{
			this.hostPort = port;
		}
	}

	public void setLastAuthentication(String authenticatorType, String method, String jsCredentials)
	{
		synchronized (lock)
		{
			this.authenticatorType = authenticatorType;
			this.authenticatorMethod = method;
			this.jsCredentials = jsCredentials;
		}
	}

	public String getAuthenticatorType()
	{
		synchronized (lock)
		{
			return authenticatorType;
		}
	}

	public String getAuthenticatorMethod()
	{
		synchronized (lock)
		{
			return authenticatorMethod;
		}
	}

	public String getJsCredentials()
	{
		synchronized (lock)
		{
			return jsCredentials;
		}
	}

	/**
	 * @param value
	 */
	public void setTenantValue(Object value)
	{
		synchronized (lock)
		{
			this.tenantValue = value;
		}
	}

	/**
	 * @param value
	 */
	public Object getTenantValue()
	{
		synchronized (lock)
		{
			return this.tenantValue;
		}
	}

}
