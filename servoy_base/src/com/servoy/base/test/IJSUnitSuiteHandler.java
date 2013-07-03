/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.test;

/**
 * A bridge test suite that is also able to provide what's needed for jsUnit execution.
 * 
 * @author acostescu
 */
public interface IJSUnitSuiteHandler extends IBridgedTestSuite, IBridgedTestListener
{

	public static final String BRIDGE_ID_ARG = "bid"; //$NON-NLS-1$
	public static final String NO_INIT_SMC_ARG = "noinitsmc"; //$NON-NLS-1$

	public static final String JAVA_OBJECTS_CONTEXT_JNDI_PATH = "java:/comp/env"; //$NON-NLS-1$
	public static final String SERVOY_SHARED_MAP_JNDI_PATH = "servoy/sharedMap"; //$NON-NLS-1$
	public static final String SERVOY_BRIDGE_KEY = "jsUnitBridge"; //$NON-NLS-1$

	int getId();

	/**
	 * When this gets called, the test session should end with an error.
	 */
	void reportUnexpectedThrowable(String msg, Throwable t);

	void registerRunStartListener(TestCycleListener l);

	/**
	 * Returns either null for no credentials or a String[2] where index 0 is 'username' and index 1 is 'password' - for automatic
	 * test client authentication.
	 * @return see description.
	 */
	String[] getCredentials();


	public static interface TestCycleListener
	{

		void started();

		void finished();

	}

}
