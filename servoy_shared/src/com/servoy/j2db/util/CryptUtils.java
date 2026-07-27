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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
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

	// New-format constants — these values are fixed and must never be changed once deployed,
	// because changing them would silently break decryption of all previously encrypted files.
	private static final byte[] NEW_FORMAT_MAGIC = { 'S', 'E', 'N', 'C' };
	private static final byte NEW_FORMAT_VERSION = 1;
	private static final String NEW_CRYPT_METHOD = "AES/GCM/NoPadding"; //$NON-NLS-1$
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"; //$NON-NLS-1$
	private static final int PBKDF2_ITERATIONS = 100_000;
	private static final int SALT_LENGTH = 16;
	private static final int GCM_IV_LENGTH = 12;
	private static final int AES_KEY_BITS = 256;

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
	 * Open an encrypting stream that writes AES-256-GCM output (with PBKDF2-derived key) to
	 * {@code dest}. The format header (magic, version, salt, IV) is written to {@code dest}
	 * immediately; the caller writes plaintext to the returned stream and must close it when done
	 * so the GCM authentication tag is flushed.
	 *
	 * Output layout: MAGIC(4) | VERSION(1) | SALT(16) | IV(12) | AES-256-GCM ciphertext+tag
	 *
	 * Replaces {@link #createCipher(String, int)} with {@code Cipher.ENCRYPT_MODE}:
	 * <pre>
	 *   // before
	 *   try (CipherOutputStream cos = new CipherOutputStream(dest, CryptUtils.createCipher(pwd, Cipher.ENCRYPT_MODE))) { ... }
	 *   // after
	 *   try (OutputStream cos = CryptUtils.newEncryptingStream(dest, pwd)) { ... }
	 * </pre>
	 *
	 * @param dest destination stream for the encrypted output
	 * @param password user-supplied password
	 * @return stream to write plaintext into; caller must close it
	 */
	public static OutputStream newEncryptingStream(OutputStream dest, String password) throws Exception
	{
		byte[] salt = new byte[SALT_LENGTH];
		byte[] iv = new byte[GCM_IV_LENGTH];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(salt);
		sr.nextBytes(iv);

		dest.write(NEW_FORMAT_MAGIC);
		dest.write(NEW_FORMAT_VERSION);
		dest.write(salt);
		dest.write(iv);

		SecretKey key = deriveKey(password, salt);
		Cipher cipher = Cipher.getInstance(NEW_CRYPT_METHOD);
		cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
		return new CipherOutputStream(dest, cipher);
	}

	/**
	 * Convenience wrapper: encrypt all bytes from {@code source} to {@code dest}.
	 * Delegates to {@link #newEncryptingStream(OutputStream, String)}.
	 */
	public static void fileEncryption(InputStream source, OutputStream dest, String password) throws Exception
	{
		try (OutputStream cos = newEncryptingStream(dest, password))
		{
			source.transferTo(cos);
		}
	}

	/**
	 * Decrypt a file previously encrypted with {@link #fileEncryption} or the legacy
	 * {@link #createCipher} / MD5+AES-ECB path.
	 *
	 * The format is auto-detected from the file header: files starting with the "SENC" magic
	 * use the new AES-256-GCM path; all other files fall back to the legacy MD5+AES-ECB path.
	 * The legacy path exists solely for backward compatibility with solution files encrypted
	 * before the AES-GCM migration and should be removed once all such files have been
	 * re-encrypted with {@link #fileEncryption}.
	 *
	 * @param file encrypted file
	 * @param password user-supplied password
	 * @return temporary file containing the decrypted content, or null on failure
	 */
	public static File fileDecryption(File file, String password)
	{
		File tempFile = null;
		OutputStream out = null;
		try
		{
			byte[] fileBytes = Files.readAllBytes(file.toPath());
			byte[] decrypted = isNewFormat(fileBytes) ? decryptNew(fileBytes, password) : decryptLegacy(fileBytes, password);

			tempFile = File.createTempFile("import", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
			tempFile.deleteOnExit();
			out = new FileOutputStream(tempFile);
			out.write(decrypted);
			out.flush();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			Utils.close(out);
		}
		return tempFile;
	}

	/**
	 * Create an AES cipher using MD5 key derivation (legacy).
	 *
	 * @deprecated For encryption use {@link #newEncryptingStream(OutputStream, String)};
	 *             for decryption use {@link #fileDecryption(File, String)}.
	 *             This method uses MD5 key derivation and AES/ECB — both cryptographically weak.
	 *             It is kept only to support legacy-format decryption inside {@link #fileDecryption}.
	 */
	@Deprecated
	public static Cipher createCipher(String passwd, int mode) throws Exception
	{
		MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		byte[] hash = md.digest(passwd.getBytes("UTF-8")); //$NON-NLS-1$
		SecretKeySpec skeySpec = new SecretKeySpec(hash, ENCRYPTION_ALGORITHM);
		Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
		cipher.init(mode, skeySpec);
		return cipher;
	}

	private static boolean isNewFormat(byte[] data)
	{
		if (data.length < NEW_FORMAT_MAGIC.length) return false;
		for (int i = 0; i < NEW_FORMAT_MAGIC.length; i++)
		{
			if (data[i] != NEW_FORMAT_MAGIC[i]) return false;
		}
		return true;
	}

	private static byte[] decryptNew(byte[] data, String password) throws Exception
	{
		int offset = NEW_FORMAT_MAGIC.length + 1; // skip magic (4) + version (1)
		byte[] salt = Arrays.copyOfRange(data, offset, offset + SALT_LENGTH);
		offset += SALT_LENGTH;
		byte[] iv = Arrays.copyOfRange(data, offset, offset + GCM_IV_LENGTH);
		offset += GCM_IV_LENGTH;
		SecretKey key = deriveKey(password, salt);
		Cipher cipher = Cipher.getInstance(NEW_CRYPT_METHOD);
		cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
		return cipher.doFinal(data, offset, data.length - offset);
	}

	// Backward compatibility: MD5-derived key, AES/ECB — used only for files encrypted before the AES-GCM migration
	private static byte[] decryptLegacy(byte[] data, String password) throws Exception
	{
		return createCipher(password, Cipher.DECRYPT_MODE).doFinal(data);
	}

	private static SecretKey deriveKey(String password, byte[] salt) throws Exception
	{
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
		byte[] keyBytes = skf.generateSecret(spec).getEncoded();
		spec.clearPassword();
		return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
	}

}
