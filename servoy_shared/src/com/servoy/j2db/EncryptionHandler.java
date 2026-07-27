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
	private static final int GCM_IV_LENGTH = 12;
	private static final SecureRandom secureRandom = new SecureRandom();

	private final SecretKey secretString;
	private final SecretKey secretScript;

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
		byte[] iv = new byte[GCM_IV_LENGTH];
		secureRandom.nextBytes(iv);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.ENCRYPT_MODE, secretString, new GCMParameterSpec(128, iv));
		byte[] ciphertext = cipher.doFinal(value.getBytes());
		byte[] output = new byte[GCM_IV_LENGTH + ciphertext.length];
		System.arraycopy(iv, 0, output, 0, GCM_IV_LENGTH);
		System.arraycopy(ciphertext, 0, output, GCM_IV_LENGTH, ciphertext.length);
		if (urlSafe) return Base64.encodeBase64URLSafeString(output);
		return Utils.encodeBASE64(output);
	}

	public String decryptString(String value) throws Exception
	{
		if (value == null) return value;
		if (secretString == null) return SecuritySupport.decrypt(value);
		byte[] decoded = Base64.decodeBase64(value);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.DECRYPT_MODE, secretString, new GCMParameterSpec(128, decoded, 0, GCM_IV_LENGTH));
		return new String(cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH));
	}

	public String encryptScript(String value) throws Exception
	{
		if (value == null) return value;
		if (secretScript == null)
		{
			return SecuritySupport.encrypt(value);
		}
		byte[] iv = new byte[GCM_IV_LENGTH];
		secureRandom.nextBytes(iv);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.ENCRYPT_MODE, secretScript, new GCMParameterSpec(128, iv));
		byte[] ciphertext = cipher.doFinal(value.getBytes());
		byte[] output = new byte[GCM_IV_LENGTH + ciphertext.length];
		System.arraycopy(iv, 0, output, 0, GCM_IV_LENGTH);
		System.arraycopy(ciphertext, 0, output, GCM_IV_LENGTH, ciphertext.length);
		return Utils.encodeBASE64(output);
	}

	public String decryptScript(String value) throws Exception
	{
		if (value == null) return value;
		if (secretScript == null) return SecuritySupport.decrypt(value);
		byte[] decoded = Base64.decodeBase64(value);
		Cipher cipher = Cipher.getInstance(CRYPT_METHOD);
		cipher.init(Cipher.DECRYPT_MODE, secretScript, new GCMParameterSpec(128, decoded, 0, GCM_IV_LENGTH));
		return new String(cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH));
	}

}
