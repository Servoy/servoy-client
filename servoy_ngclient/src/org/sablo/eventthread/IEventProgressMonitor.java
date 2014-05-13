package org.sablo.eventthread;

/**
 * @author jcompagner
 *
 */
public interface IEventProgressMonitor
{
	public boolean isExecuting();

	public void runInBackground();
}