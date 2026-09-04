/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
package com.servoy.mcp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.server.shared.IHeadlessClient;

/**
 * A headless client that does what it is told, on the calling thread.
 *
 * <p>Stands in for the client a tool would really run in. The session it carries is a real
 * {@link ClientInfo} rather than a mock - partly because the class is final, mostly because the
 * question worth asking is what ended up on the session, not which methods were called on the way
 * there.</p>
 *
 * @author Servoy
 */
final class McpFakeClient
{
	final IHeadlessClient client = mock(IHeadlessClient.class);

	final IApplication application = mock(IApplication.class);

	final ClientInfo clientInfo = new ClientInfo();

	final IFoundSetManagerInternal foundSetManager = mock(IFoundSetManagerInternal.class);

	final Solution solution = mock(Solution.class);

	final IClientPluginAccess access;

	McpFakeClient()
	{
		ClientPluginAccessProvider provider = mock(ClientPluginAccessProvider.class);
		when(provider.getApplication()).thenReturn(application);
		when(client.getPluginAccess()).thenReturn(provider);
		when(client.isValid()).thenReturn(Boolean.TRUE);
		access = provider;

		when(application.getClientInfo()).thenReturn(clientInfo);
		when(application.getFoundSetManager()).thenReturn(foundSetManager);

		FlattenedSolution flattened = mock(FlattenedSolution.class);
		when(flattened.getSolution()).thenReturn(solution);
		when(application.getFlattenedSolution()).thenReturn(flattened);

		// the real one hands the work to the client's thread; here it runs where it is called, so
		// that a failure inside surfaces as a failure of the test rather than as a timeout
		doAnswer(invocation -> {
			((Runnable)invocation.getArgument(0)).run();
			return null;
		}).when(application).invokeAndWait(any(Runnable.class));
	}
}
