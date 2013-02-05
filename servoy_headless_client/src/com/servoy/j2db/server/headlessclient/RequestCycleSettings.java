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

package com.servoy.j2db.server.headlessclient;

import java.util.List;

import org.apache.wicket.IResponseFilter;
import org.apache.wicket.settings.IExceptionSettings.UnexpectedExceptionDisplay;
import org.apache.wicket.settings.IRequestCycleSettings;
import org.apache.wicket.util.time.Duration;

/**
 * IRequestCycleSettings wrapper that is able to temporarily overwrite some of the returned settings.
 * @author acostescu
 */
public class RequestCycleSettings implements IRequestCycleSettings
{

	private final ThreadLocal<Duration> alternateTimeout = new ThreadLocal<Duration>();

	private final IRequestCycleSettings superSettings;

	public RequestCycleSettings(IRequestCycleSettings superSettings)
	{
		this.superSettings = superSettings;
	}

	/**
	 * You should always use {@link #restoreTimeout()} with a try-finally after using this method. 
	 */
	public void overrideTimeout(long duration)
	{
		alternateTimeout.set(Duration.milliseconds(duration));
	}

	public void restoreTimeout()
	{
		alternateTimeout.set(null);
	}

	@Override
	public void addResponseFilter(IResponseFilter responseFilter)
	{
		superSettings.addResponseFilter(responseFilter);
	}

	@Override
	public boolean getBufferResponse()
	{
		return superSettings.getBufferResponse();
	}

	@Override
	public boolean getGatherExtendedBrowserInfo()
	{
		return superSettings.getGatherExtendedBrowserInfo();
	}

	@Override
	public RenderStrategy getRenderStrategy()
	{
		return superSettings.getRenderStrategy();
	}

	@Override
	public List<IResponseFilter> getResponseFilters()
	{
		return superSettings.getResponseFilters();
	}

	@Override
	public String getResponseRequestEncoding()
	{
		return superSettings.getResponseRequestEncoding();
	}

	@Override
	public Duration getTimeout()
	{
		Duration t = alternateTimeout.get();
		if (t == null)
		{
			t = superSettings.getTimeout();
		}
		return t;
	}

	@Override
	public UnexpectedExceptionDisplay getUnexpectedExceptionDisplay()
	{
		return superSettings.getUnexpectedExceptionDisplay();
	}

	@Override
	public void setBufferResponse(boolean bufferResponse)
	{
		superSettings.setBufferResponse(bufferResponse);
	}

	@Override
	public void setGatherExtendedBrowserInfo(boolean gatherExtendedBrowserInfo)
	{
		superSettings.setGatherExtendedBrowserInfo(gatherExtendedBrowserInfo);
	}

	@Override
	public void setRenderStrategy(RenderStrategy renderStrategy)
	{
		superSettings.setRenderStrategy(renderStrategy);
	}

	@Override
	public void setResponseRequestEncoding(String responseRequestEncoding)
	{
		superSettings.setResponseRequestEncoding(responseRequestEncoding);
	}

	@Override
	public void setTimeout(Duration timeout)
	{
		superSettings.setTimeout(timeout);
	}

	@Override
	public void setUnexpectedExceptionDisplay(UnexpectedExceptionDisplay unexpectedExceptionDisplay)
	{
		superSettings.setUnexpectedExceptionDisplay(unexpectedExceptionDisplay);
	}

}
