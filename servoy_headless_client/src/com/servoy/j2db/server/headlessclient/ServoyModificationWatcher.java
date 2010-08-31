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
package com.servoy.j2db.server.headlessclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.markup.MarkupResourceStream;
import org.apache.wicket.util.listener.ChangeListenerSet;
import org.apache.wicket.util.listener.IChangeListener;
import org.apache.wicket.util.thread.ICode;
import org.apache.wicket.util.thread.Task;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.watch.IModifiable;
import org.apache.wicket.util.watch.IModificationWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Monitors one or more <code>IModifiable</code> objects, calling a {@link IChangeListener
 * IChangeListener} when a given object's modification time changes.
 * 
 * @author jcompagner
 * @since 5.2
 */
public class ServoyModificationWatcher implements IModificationWatcher
{
	/** logger */
	private static final Logger log = LoggerFactory.getLogger(ServoyModificationWatcher.class);

	/** maps <code>IModifiable</code> objects to <code>Entry</code> objects */
	private final Map<IModifiable, Entry> modifiableToEntry = new ConcurrentHashMap<IModifiable, Entry>();

	/** the <code>Task</code> to run */
	private Task task;

	/**
	 * Container class for holding modifiable entries to watch.
	 */
	private static final class Entry
	{
		// The most recent lastModificationTime polled on the object
		Time lastModifiedTime;

		// The set of listeners to call when the modifiable changes
		final ChangeListenerSet listeners = new ChangeListenerSet();

		// The modifiable thing
		IModifiable modifiable;
	}

	/**
	 * Default constructor for two-phase construction.
	 */
	public ServoyModificationWatcher()
	{
	}

	/**
	 * Constructor that accepts a <code>Duration</code> argument representing the poll frequency.
	 * 
	 * @param pollFrequency
	 *            how often to check on <code>IModifiable</code>s
	 */
	public ServoyModificationWatcher(final Duration pollFrequency)
	{
		start(pollFrequency);
	}

	/**
	 * @see org.apache.wicket.util.watch.IModificationWatcher#add(org.apache.wicket.util.watch.IModifiable, org.apache.wicket.util.listener.IChangeListener)
	 */
	public final boolean add(final IModifiable modifiable, final IChangeListener listener)
	{
		if (modifiable instanceof MarkupResourceStream)
		{
			if (WebForm.class.isAssignableFrom(((MarkupResourceStream)modifiable).getContainerInfo().getContainerClass()))
			{
				return false;
			}
		}
		// Look up entry for modifiable
		final Entry entry = modifiableToEntry.get(modifiable);

		// Found it?
		if (entry == null)
		{
			Time lastModifiedTime = modifiable.lastModifiedTime();
			if (lastModifiedTime != null)
			{
				// Construct new entry
				final Entry newEntry = new Entry();

				newEntry.modifiable = modifiable;
				newEntry.lastModifiedTime = lastModifiedTime;
				newEntry.listeners.add(listener);

				// Put in map
				modifiableToEntry.put(modifiable, newEntry);
			}
			else
			{
				// The IModifiable is not returning a valid lastModifiedTime
				log.info("Cannot track modifications to resource " + modifiable);
			}

			return true;
		}
		else
		{
			// Add listener to existing entry
			return entry.listeners.add(listener);
		}
	}

	/**
	 * @see org.apache.wicket.util.watch.IModificationWatcher#remove(org.apache.wicket.util.watch.IModifiable)
	 */
	public IModifiable remove(final IModifiable modifiable)
	{
		final Entry entry = modifiableToEntry.remove(modifiable);
		if (entry != null)
		{
			return entry.modifiable;
		}
		return null;
	}

	/**
	 * @see org.apache.wicket.util.watch.IModificationWatcher#start(org.apache.wicket.util.time.Duration)
	 */
	public void start(final Duration pollFrequency)
	{
		// Construct task with the given polling frequency
		task = new Task("ModificationWatcher");

		task.run(pollFrequency, new ICode()
		{
			public void run(final Logger log)
			{
				// Iterate over a copy of the list of entries to avoid concurrent modification
				// problems without the associated liveness issues of holding a lock while
				// potentially polling file times!
				Iterator<Entry> iter = new ArrayList<Entry>(modifiableToEntry.values()).iterator();
				while (iter.hasNext())
				{
					final Entry entry = iter.next();

					// If the modifiable has been modified after the last known
					// modification time
					final Time modifiableLastModified = entry.modifiable.lastModifiedTime();
					if ((modifiableLastModified != null) && modifiableLastModified.after(entry.lastModifiedTime))
					{
						// Notify all listeners that the modifiable was modified
						entry.listeners.notifyListeners();

						// Update timestamp
						entry.lastModifiedTime = modifiableLastModified;
					}
				}
			}
		});
	}

	/**
	 * @see org.apache.wicket.util.watch.IModificationWatcher#destroy()
	 */
	public void destroy()
	{
		if (task != null)
		{
			// task.stop();
			task.interrupt();
		}
	}

	/**
	 * @see org.apache.wicket.util.watch.IModificationWatcher#getEntries()
	 */
	public final Set<IModifiable> getEntries()
	{
		return modifiableToEntry.keySet();
	}
}
