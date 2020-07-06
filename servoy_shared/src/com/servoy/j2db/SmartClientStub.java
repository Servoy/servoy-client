/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.ISmartClient;
import com.servoy.j2db.dataprocessing.ThinIDataSetCopy;
import com.servoy.j2db.scripting.StartupArguments;

/**
 * @author jcompagner
 *
 */
public class SmartClientStub extends ClientStub implements ISmartClient
{
	// Command buffer commands. THIS IS A COPY OF ClientProxy
	static final Integer CMD_ALERT = new Integer(0);
	static final Integer CMD_CLOSE_SOLUTION = new Integer(3);
	static final Integer CMD_SHUTDOWN = new Integer(4);
	static final Integer CMD_DATA_CHANGE = new Integer(5);
	static final Integer CMD_FLUSH_CACHED_DATABASE_DATA = new Integer(6);
	static final Integer CMD_ISALIVE = new Integer(7);
	static final Integer CMD_ACTIVATE_SOLUTION_METHOD = new Integer(8);

	/**
	 * @param c
	 */
	public SmartClientStub(ClientState c)
	{
		super(c);
	}

	@Override
	public void processMessages(Object[][] messages)
	{
		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				for (Object[] message : messages)
				{
					Integer command = (Integer)message[0];

					if (CMD_ALERT.equals(command))
					{
						alert((String)message[1]);
					}
					else if (CMD_CLOSE_SOLUTION.equals(command))
					{
						closeSolution();
					}
					else if (CMD_DATA_CHANGE.equals(command))
					{
						Object message3 = message[3];
						if (message3 instanceof ThinIDataSetCopy)
						{
							message3 = ((ThinIDataSetCopy)message3).toBufferedDataSet();
						} // else it's null
						notifyDataChange((String)message[1], (String)message[2], (IDataSet)message3, ((Integer)message[4]).intValue(),
							(Object[])message[5]);
					}
					else if (CMD_FLUSH_CACHED_DATABASE_DATA.equals(command))
					{
						flushCachedDatabaseData((String)message[1]);
					}
					else if (CMD_ACTIVATE_SOLUTION_METHOD.equals(command))
					{
						activateSolutionMethod((String)message[1], (StartupArguments)message[2]);
					}
					else if (CMD_ISALIVE.equals(command))
					{
						isAlive();
					}
					else if (CMD_SHUTDOWN.equals(command))
					{
						shutDown();
					}
				}
			}
		});
	}

}
