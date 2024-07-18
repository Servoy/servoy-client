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
import javax.crypto.spec.GCMParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompanger
 *
 * @since 2023.3.4
 */
@SuppressWarnings("nls")
public class EncryptionHandler
{
	private static final String CRYPT_METHOD = "AES/GCM/NoPadding";

	private final SecretKey secretString;
	private final GCMParameterSpec gcmParameterSpecStr;

	private final SecretKey secretScript;
	private final GCMParameterSpec gcmParameterSpecScript;

	public EncryptionHandler()
	{
		SecretKey key1 = null, key2 = null;
		try
		{
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			key1 = keyGenerator.generateKey();
			key2 = keyGenerator.generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			Debug.error("Falling back to default script encryption", e);
		}
		secretString = key1;
		secretScript = key2;

		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		gcmParameterSpecStr = new GCMParameterSpec(128, iv);
		new SecureRandom().nextBytes(iv);
		gcmParameterSpecScript = new GCMParameterSpec(128, iv);
	}

	public String encryptString(String value) throws Exception
	{
		return encryptString(value, false);
	}

	public String encryptString(String value, boolean urlSafe) throws Exception
	{
		if (value == null) return value;
		if (secretString == null)
		{
			if (urlSafe) return SecuritySupport.encryptUrlSafe(value);
			return SecuritySupport.encrypt(value);
		}
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.ENCRYPT_MODE, secretString, gcmParameterSpecStr);
		byte[] bytes = cipher.doFinal(value.getBytes());
		if (urlSafe) return Base64.encodeBase64URLSafeString(bytes);

		return Utils.encodeBASE64(bytes);
	}

	public String decryptString(String value) throws Exception
	{
		if (value == null) return value;
		if (secretString == null) return SecuritySupport.decrypt(value);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.DECRYPT_MODE, secretString, gcmParameterSpecStr);
		return new String(cipher.doFinal(Utils.decodeBASE64(value)));
	}

	public String encryptScript(String value) throws Exception
	{
		if (value == null) return value;
		if (secretScript == null)
		{
			return SecuritySupport.encrypt(value);
		}
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.ENCRYPT_MODE, secretScript, gcmParameterSpecScript);
		byte[] bytes = cipher.doFinal(value.getBytes());

		return Utils.encodeBASE64(bytes);
	}

	public String decryptScript(String value) throws Exception
	{
		if (value == null) return value;
		if (secretScript == null) return SecuritySupport.decrypt(value);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.DECRYPT_MODE, secretScript, gcmParameterSpecScript);
		return new String(cipher.doFinal(Utils.decodeBASE64(value)));
	}

}
