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

package com.servoy.j2db.documentation;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Enum for tagging which clients are supported for the property.
 * 
 * @author rgansevles
 *
 * NOTE: this class has a copy in com.servoy.eclipse.docgenerator.metamodel - please keep them in sync
 */
public enum ClientSupport
{
	None(0),
	mc(1),
	wc(2),
	sc(4),
	ng(8),
	mc_wc(mc.bits | wc.bits),
	mc_sc(mc.bits | sc.bits),
	wc_sc(wc.bits | sc.bits),
	ng_wc(ng.bits | wc.bits),
	ng_sc(ng.bits | sc.bits),
	ng_mc(ng.bits | mc.bits),
	mc_wc_sc(mc.bits | wc.bits | sc.bits),
	ng_wc_sc(ng.bits | wc.bits | sc.bits),
	mc_ng_sc(mc.bits | ng.bits | sc.bits),
	mc_wc_ng(mc.bits | wc.bits | ng.bits),
	ng_mc_wc_sc(ng.bits | mc.bits | wc.bits | sc.bits);

	private final int bits;

	public static final ClientSupport Default = ng_wc_sc;
	public static final ClientSupport All = ng_mc_wc_sc;

	private ClientSupport(int bits)
	{
		this.bits = bits;
	}

	public static ClientSupport fromString(String s)
	{
		if (s == null) return null;
		if (s.length() == 0) return None;
		return fromBits(bits(s, ng) | bits(s, mc) | bits(s, wc) | bits(s, sc));
	}

	private static int bits(String s, ClientSupport supp)
	{
		return s.indexOf(supp.name()) >= 0 ? supp.bits : 0;
	}

	private static ClientSupport fromBits(int bits)
	{
		for (ClientSupport supp : ClientSupport.values())
		{
			if (supp.bits == bits) return supp;
		}

		return null;
	}

	public String toAttribute()
	{
		return append(append(append(append(new StringBuilder(), ng), mc), wc), sc).toString();
	}

	private StringBuilder append(StringBuilder sb, ClientSupport supp)
	{
		if ((bits & supp.bits) == supp.bits)
		{
			if (sb.length() > 0) sb.append(',');
			sb.append(supp.name());
		}
		return sb;
	}

	/**
	 * Check if the current ClientSupport has (partly) support for the csp argument
	 */
	public boolean hasSupport(ClientSupport csp)
	{
		return csp != null && (bits & csp.bits) != 0;
	}

	/**
	 * Check if the current ClientSupport fully supports the csp argument
	 */
	public boolean supports(ClientSupport csp)
	{
		return csp != null && (bits & csp.bits) == csp.bits;
	}

	public ClientSupport union(ClientSupport scp)
	{
		return scp == null ? this : fromBits(bits | scp.bits);
	}

	public ClientSupport remove(ClientSupport scp)
	{
		return scp == null ? this : fromBits(bits & ~scp.bits);
	}

	public ClientSupport intersect(ClientSupport scp)
	{
		return scp == null ? null : fromBits(bits & scp.bits);
	}

	public static ClientSupport create(boolean support_ng, boolean support_mc, boolean support_wc, boolean support_sc)
	{
		return fromBits((support_ng ? ng.bits : 0) | (support_mc ? mc.bits : 0) | (support_wc ? wc.bits : 0) | (support_sc ? sc.bits : 0));
	}

	public static ClientSupport fromAnnotation(ServoyClientSupport csp)
	{
		return csp == null ? null : create(csp.ng(), csp.mc(), csp.wc(), csp.sc());
	}
}