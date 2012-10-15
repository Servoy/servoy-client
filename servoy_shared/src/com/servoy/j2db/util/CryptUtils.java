/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.util;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for encryption/decryption methods
 * 
 * @author tpopovici
 *
 */
public class CryptUtils
{

	private static final String ENCRYPTION_ALGORITHM = "AES"; //$NON-NLS-1$

	/**
	 * Check is the specified file is under encryption or not
	 * 
	 * @param file
	 * @return boolean
	 * 
	 */
	public static boolean checkEncryption(File file)
	{
		boolean encrypted = false;
		try
		{
			JarFile jarFile = new JarFile(file, true);
			jarFile.close();
		}
		catch (ZipException e)
		{
			encrypted = true;
		}
		catch (IOException e)
		{
			encrypted = true;
		}
		return encrypted;
	}

	/**
	 * Create an AES Cipher DECRYPT_MODE by a specified password.
	 * 
	 * @param password
	 * @param mode represent cipher mode (ex. Cipher.DECRYPT_MODE, ENCRYPT_MODE)
	 * @return cipher
	 * 
	 * @throws Exception if an error occurs in the encryption
	 */
	public static Cipher createCipher(String passwd, int mode) throws Exception
	{
		MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		byte[] hash = md.digest(passwd.getBytes("UTF-8")); //$NON-NLS-1$

		SecretKeySpec skeySpec = new SecretKeySpec(hash, ENCRYPTION_ALGORITHM);
		Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
		cipher.init(mode, skeySpec);
		return cipher;
	}


}
