/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.codec.binary.Base64;

import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

public class SecuritySupport
{
	private static KeyStore passwordKeyStore;
	private static KeyStore sslKeyStore;
	private static char[] passphrase;
	private static SecretKeySpec keySpec;

//	public static void main(String[] args) throws Exception
//	{
//		Settings settings = Settings.getDefaultSettings(false, null);
//
//		//KeyStore ks = getKeyStore(settings);
//		initKeyStoreAndPassphrase(settings);
//
//		Enumeration e = keyStore.aliases();
//		if (e.hasMoreElements())
//		{
//			String alias = (String) e.nextElement();
//			Key desKey = new SecretKeySpec(new DESedeKeySpec(keyStore.getKey(alias, passphrase).getEncoded()).getKey(),"DESede");
//		    Cipher desCipher = Cipher.getInstance("DESede");
//		    desCipher.init(Cipher.ENCRYPT_MODE, desKey);
//
//		    //  Our cleartext
//		    byte[] cleartext = "This is just an example".getBytes();
//
//		    // Encrypt the cleartext
//		    byte[] ciphertext = desCipher.doFinal(cleartext);
//
//		    System.out.println(new String(ciphertext) + " lenght "+ciphertext.length);
//
//		    // Initialize the same cipher for decryption
//		    desCipher.init(Cipher.DECRYPT_MODE, desKey);
//
//		    // Decrypt the ciphertext
//		    byte[] cleartext1 = desCipher.doFinal(ciphertext);
//
//		    System.out.println(new String(cleartext1) + " lenght "+cleartext1.length);
//		    System.out.println(Utils.encodeBASE64(ciphertext));
//		}
//	}

	public static SSLContext getSSLContext(Properties settings) throws Exception
	{

		// set up key manager to do server authentication
		SSLContext ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
		KeyManagerFactory kmf = null;
		try
		{
			kmf = KeyManagerFactory.getInstance("SunX509"); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			Debug.log("couldn't get SunX509, now trying ibm");
			kmf = KeyManagerFactory.getInstance("IbmX509"); //$NON-NLS-1$
		}

		initSSLKeyStoreAndPassphrase(settings);

		kmf.init(sslKeyStore, passphrase);
		ctx.init(kmf.getKeyManagers(), null, null);

		return ctx;

	}

	public static SSLServerSocketFactory getSSLServerSocketFactory(Settings settings) throws Exception
	{
		return getSSLContext(settings).getServerSocketFactory();
	}


	public static Key getCryptKey() throws Exception
	{
		if (passwordKeyStore == null)
		{
			InputStream is = null;
			try
			{
				is = SecuritySupport.class.getResourceAsStream("background.gif");
				passwordKeyStore = KeyStore.getInstance("JKS");
				passwordKeyStore.load(is, "passphrase".toCharArray());
				Enumeration e = passwordKeyStore.aliases();
				while (e.hasMoreElements())
				{
					String alias = (String)e.nextElement();
					if (passwordKeyStore.isKeyEntry(alias))
					{
						keySpec = new SecretKeySpec(new DESedeKeySpec(passwordKeyStore.getKey(alias, "passphrase".toCharArray()).getEncoded()).getKey(),
							"DESede");
						break;
					}
				}
			}
			finally
			{
				Utils.closeInputStream(is);
			}
		}

		return keySpec;
	}

	@SuppressWarnings("nls")
	public static String encrypt(String value) throws Exception
	{
		if (value == null) return value;
		Cipher cipher = Cipher.getInstance("DESede");
		cipher.init(Cipher.ENCRYPT_MODE, SecuritySupport.getCryptKey());
		return Utils.encodeBASE64(cipher.doFinal(value.getBytes()));
	}

	@SuppressWarnings("nls")
	public static String decrypt(String value) throws Exception
	{
		if (value == null) return value;
		Cipher cipher = Cipher.getInstance("DESede");
		cipher.init(Cipher.DECRYPT_MODE, SecuritySupport.getCryptKey());
		return new String(cipher.doFinal(Utils.decodeBASE64(value)));
	}

	@SuppressWarnings("nls")
	public static String encryptUrlSafe(String value) throws Exception
	{
		if (value == null) return value;
		Cipher cipher = Cipher.getInstance("DESede");
		cipher.init(Cipher.ENCRYPT_MODE, SecuritySupport.getCryptKey());
		return Base64.encodeBase64URLSafeString(cipher.doFinal(value.getBytes()));
	}

	@SuppressWarnings("nls")
	private static void initSSLKeyStoreAndPassphrase(Properties settings) throws Exception
	{
		if (sslKeyStore == null)
		{
			InputStream is = null;
			try
			{
				passphrase = "passphrase".toCharArray();
				String filename = settings.getProperty("SocketFactory.SSLKeystorePath", "");
				if (!"".equals(filename)) //$NON-NLS-1$
				{
					try
					{
						File file = new File(filename);
						if (!file.exists())
						{
							IApplicationServerSingleton appServer = ApplicationServerRegistry.exists() ? ApplicationServerRegistry.get() : null;
							if (appServer != null && appServer.getServoyApplicationServerDirectory() != null)
							{
								String applicationServerDirectory = appServer.getServoyApplicationServerDirectory();
								file = new File(applicationServerDirectory, filename);
							}
							if (!file.exists())
							{
								Debug.error("couldn't resolve the ssl keystore file " + file.getAbsolutePath() + ", maybe the user dir (" +
									System.getProperty("user.dir") +
									") of the application server is incorrect, please specify the system property: servoy.application_server.dir to point to the right directory [servoy_install]/application_server");
							}
						}
						is = new FileInputStream(file);
						passphrase = settings.getProperty("SocketFactory.SSLKeystorePassphrase", "").toCharArray();
					}
					catch (Exception e)
					{
						Debug.error("SSLKeystorePath not found: " + filename + " fallback to default one", e);
					}
				}
				if (is == null)
				{
					is = SecuritySupport.class.getResourceAsStream("background2.gif");
				}
				sslKeyStore = KeyStore.getInstance("JKS");
				sslKeyStore.load(is, passphrase);
			}
			finally
			{
				Utils.closeInputStream(is);
			}
		}
	}

	/**
	 *
	 */
	public static void clearCryptKey()
	{
		sslKeyStore = null;
		passwordKeyStore = null;
		keySpec = null;
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length < 2)
		{
			System.err.println("only supported with 'decyrpt value' or 'encrypt value'"); //$NON-NLS-1$
			return;
		}
		if ("decrypt".equals(args[0])) //$NON-NLS-1$
		{
			System.out.println(decrypt(args[1]));
		}
		else if ("encrypt".equals(args[0])) //$NON-NLS-1$
		{
			System.out.println(encrypt(args[1]));
		}
	}
}
