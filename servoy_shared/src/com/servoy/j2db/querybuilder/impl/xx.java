/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

/**
 * @author rob
 *
 */


public class xx
{
	interface Gift extends GiftBase, Yyy<Gift>
	{
	}
	interface GiftBase extends Xxx
	{
	}
	interface Guest extends GuestBase, Yyy<Guest>
	{
	}
	interface GuestBase extends Xxx
	{
	}
	interface Xxx
	{
		void present();
	}
	interface Yyy<T extends Yyy< ? >>
	{
		T bla();
	}

	interface Presentable extends GiftBase, GuestBase, Yyy<Presentable>
	{
	}

	public static void main(String[] args)
	{
		Presentable johnny = new Presentable()
		{
			@Override
			public void present()
			{
				System.out.println("Heeeereee's Johnny!!!");
			}

			@Override
			public Presentable bla()
			{
				return this;
			}
		};
		johnny.present(); // "Heeeereee's Johnny!!!"

		System.out.println(johnny.bla().getClass().getInterfaces()[0].getSimpleName());

		Gift gift = new Gift()
		{
			@Override
			public void present()
			{
				System.out.println("Heeeereee's Johnny!!!");
			}

			@Override
			public Gift bla()
			{
				return this;
			}
		};

		System.out.println(gift.bla().getClass().getInterfaces()[0].getSimpleName());


//		((GiftBase)johnny).present(); // "Heeeereee's Johnny!!!"
//		((Guest)johnny).present(); // "Heeeereee's Johnny!!!"
//
//		Gift johnnyAsGift = johnny;
//		johnnyAsGift.present(); // "Heeeereee's Johnny!!!"
//
//		Guest johnnyAsGuest = johnny;
//		johnnyAsGuest.present(); // "Heeeereee's Johnny!!!"
	}
}