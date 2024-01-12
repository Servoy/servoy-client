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

package com.servoy.j2db;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompanger
 *
 * @since 2023.3.4
 */
@SuppressWarnings("nls")
public class EncryptionHandler
{
	private static final String CRYPT_METHOD = "AES/CBC/PKCS5Padding";

	private final SecretKey secret;

	private final IvParameterSpec ivParameterSpec;

	public EncryptionHandler()
	{
		SecretKey key = null;
		try
		{
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			key = keyGenerator.generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			Debug.error("Falling back to default script encryption", e);
		}
		secret = key;

		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		ivParameterSpec = new IvParameterSpec(iv);
	}

	public String encrypt(String value, boolean urlSafe) throws Exception
	{
		if (value == null) return value;
		if (secret == null)
		{
			if (urlSafe) return SecuritySupport.encryptUrlSafe(Settings.getInstance(), value);
			return SecuritySupport.encrypt(Settings.getInstance(), value);
		}
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.ENCRYPT_MODE, secret, ivParameterSpec);
		byte[] bytes = cipher.doFinal(value.getBytes());
		if (urlSafe) return Base64.encodeBase64URLSafeString(bytes);
		else Base64.encodeBase64String(bytes).trim();

		return Utils.encodeBASE64(bytes);
	}

	public String encrypt(String value) throws Exception
	{
		return encrypt(value, false);
	}

	public String decrypt(String value) throws Exception
	{
		if (value == null) return value;
		if (secret == null) return SecuritySupport.decrypt(Settings.getInstance(), value);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.DECRYPT_MODE, secret, ivParameterSpec);
		return new String(cipher.doFinal(Utils.decodeBASE64(value)));
	}


}
