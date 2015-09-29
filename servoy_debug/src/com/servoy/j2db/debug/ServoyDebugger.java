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

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame;
import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.sablo.websocket.CurrentWindow;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;


/**
 * @author jcompagner
 */
public class ServoyDebugger extends DBGPDebugger
{
	private static class ProfileInfo
	{
		private ServoyDebugFrame first;

		private final Stack<ServoyDebugFrame> current = new Stack<ServoyDebugFrame>();

		private final HashMap<ServoyDebugFrame, ArrayList<ServoyDebugFrame>> map = new HashMap<ServoyDebugFrame, ArrayList<ServoyDebugFrame>>();

		/**
		 * @param servoyDebugFrame
		 */
		public void push(ServoyDebugFrame servoyDebugFrame)
		{
			if (!current.isEmpty())
			{
				ServoyDebugFrame top = current.peek();
				ArrayList<ServoyDebugFrame> calllist = map.get(top);
				if (calllist == null)
				{
					calllist = new ArrayList<ServoyDebugFrame>();
					map.put(top, calllist);
				}
				calllist.add(servoyDebugFrame);
			}
			else
			{
				first = servoyDebugFrame;
				map.clear();
			}
			this.current.push(servoyDebugFrame);

		}

		public ServoyDebugFrame peek()
		{
			if (current.empty()) return null;
			return current.peek();
		}

		public boolean pop(ServoyDebugFrame frame)
		{
			if (current.pop() != frame)
			{
				System.err.println("shouldnt happen!!");
			}
			return current.isEmpty();
		}

		public ProfileData getProfileData()
		{
			return getProfileData(first);
		}

		private ProfileData getProfileData(ServoyDebugFrame frame)
		{
			ProfileData profileData = frame.getProfileData();
			ArrayList<ServoyDebugFrame> arrayList = map.get(frame);
			if (arrayList != null)
			{
				for (ServoyDebugFrame servoyDebugFrame : arrayList)
				{
					profileData.addChild(getProfileData(servoyDebugFrame));
				}
			}
			return profileData;
		}

	}

	private final ThreadLocal<ProfileInfo> profileInfo = new ThreadLocal<ProfileInfo>();
	private final List<IProfileListener> profilelisteners;

	/**
	 * @param socket
	 * @param file
	 * @param string
	 * @param ct
	 * @param profilelisteners
	 * @throws IOException
	 */
	public ServoyDebugger(Socket socket, String file, String string, Context ct, List<IProfileListener> profilelisteners) throws IOException
	{
		super(socket, file, string, ct);
		this.profilelisteners = profilelisteners;
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.DBGPDebugger#getFrame(org.mozilla.javascript.Context, org.mozilla.javascript.debug.DebuggableScript)
	 */
	@Override
	public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript)
	{
		final IServiceProvider client = J2DBGlobals.getServiceProvider();
		if (profilelisteners.size() > 0)
		{
			ProfileInfo info = profileInfo.get();
			return new ServoyDebugFrame(cx, fnOrScript, this, info != null ? info.peek() : null);
		}
		return new DBGPDebugFrame(cx, fnOrScript, this)
		{
			@Override
			public Object eval(String value)
			{
				if (client != null && J2DBGlobals.getServiceProvider() == null)
				{
					J2DBGlobals.setServiceProvider(client);
				}
				boolean reset = false;
				try
				{
					if (client instanceof NGClient && !CurrentWindow.exists())
					{
						// make sure that for an NGClient the current window is set.
						CurrentWindow.set(new NGClientWebsocketSessionWindows(((NGClient)client).getWebsocketSession()));
						reset = true;
					}
					return super.eval(value);
				}
				finally
				{
					if (reset) CurrentWindow.set(null);
				}
			}
		};
	}

	public void onenter(ServoyDebugFrame servoyDebugFrame)
	{
		ProfileInfo info = profileInfo.get();
		if (info == null)
		{
			info = new ProfileInfo();
			profileInfo.set(info);
		}
		info.push(servoyDebugFrame);
	}

	/**
	 * @param servoyDebugFrame
	 */
	public void onexit(ServoyDebugFrame servoyDebugFrame)
	{
		if (profileInfo.get().pop(servoyDebugFrame))
		{
			// last call

			for (IProfileListener listener : profilelisteners)
			{
				listener.addProfileData(profileInfo.get().getProfileData());
			}
			profileInfo.remove();
		}
	}

}
