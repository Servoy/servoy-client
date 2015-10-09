/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.json.JSONObject;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.websocket.IEventDispatchAwareServerService;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;

/**
 * @author jcompagner
 *
 */
public class I18NService implements IEventDispatchAwareServerService
{
	public static final String NAME = "i18nService"; //$NON-NLS-1$

	private final IApplication application;

	public I18NService(IApplication application)
	{
		this.application = application;

	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if ("getI18NMessages".equals(methodName))
		{
			JSONObject values = new JSONObject();
			for (int i = 0; i < args.length(); i++)
			{
				String key = args.getString(Integer.toString(i));
				values.put(key, application.getI18NMessage(key));

			}
			return values;
		}
		else if ("generateLocaleForNumeralJS".equals(methodName))
		{
			String language = args.getString("language");
			String country = args.optString("country");
			Locale locale;
			if (country == null)
			{
				locale = new Locale(language);
			}
			else
			{
				locale = new Locale(language, country);
			}
			DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(locale);

//			numeral.js on client expects something like this:
//			{
//		        delimiters: {
//		            thousands: ',',
//		            decimal: '.'
//		        },
//		        abbreviations: {
//		            thousand: 'k',
//		            million: 'm',
//		            billion: 'b',
//		            trillion: 't'
//		        },
//		        ordinal: function (number) {
//		            var b = number % 10;
//		            return (~~ (number % 100 / 10) === 1) ? 'th' :
//		                (b === 1) ? 'st' :
//		                (b === 2) ? 'nd' :
//		                (b === 3) ? 'rd' : 'th';
//		        },
//		        currency: {
//		            symbol: '\u00A3'
//		        }
//		    }

			JSONObject languageInfo = new JSONObject();
			
			JSONObject delimiters = new JSONObject();
			delimiters.put("thousands", String.valueOf(dfs.getGroupingSeparator()));
			delimiters.put("decimal", String.valueOf(dfs.getDecimalSeparator()));
			
			JSONObject abbreviations = new JSONObject();
			// TODO do we use these anywhere clientside? (the abbreviations) - currently they are just set identical to english
			abbreviations.put("thousand", "k");
			abbreviations.put("million", "m");
			abbreviations.put("billion", "b");
			abbreviations.put("trillion", "t");
			
			JSONObject currency = new JSONObject();
			currency.put("symbol", String.valueOf(dfs.getCurrencySymbol()));
			
			languageInfo.put("delimiters", delimiters);
			languageInfo.put("abbreviations", abbreviations);
			languageInfo.put("currency", currency);
			
			// TODO "ordinal" is not set but I don't think we use it anyway; client side we generate a function that always returns '.' for "ordinal"
			
			return languageInfo;
		}
		return null;
	}

	@Override
	public int getMethodEventThreadLevel(String methodName, JSONObject arguments, int dontCareLevel)
	{
		if ("generateLocaleForNumeralJS".equals(methodName))
		{
			return IEventDispatcher.EVENT_LEVEL_SYNC_API_CALL;
		}
		return dontCareLevel;
	}
}
