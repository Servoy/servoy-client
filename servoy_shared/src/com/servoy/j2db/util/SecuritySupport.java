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

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Enumeration;

import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class SecuritySupport
{
	private static KeyStore keyStore;
	private static char[] passphrase;

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

	public static SSLContext getSSLContext(Settings settings) throws Exception
	{

		// set up key manager to do server authentication
		SSLContext ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509"); //$NON-NLS-1$

		initKeyStoreAndPassphrase(settings);

		kmf.init(keyStore, passphrase);
		ctx.init(kmf.getKeyManagers(), null, null);

		return ctx;

	}

	public static SSLServerSocketFactory getSSLServerSocketFactory(Settings settings) throws Exception
	{
		return getSSLContext(settings).getServerSocketFactory();
	}


	public static Key getCryptKey(Settings settings) throws Exception
	{
		initKeyStoreAndPassphrase(settings);

		Enumeration e = keyStore.aliases();
		while (e.hasMoreElements())
		{
			String alias = (String)e.nextElement();
			if (keyStore.isKeyEntry(alias))
			{
				return new SecretKeySpec(new DESedeKeySpec(keyStore.getKey(alias, passphrase).getEncoded()).getKey(), "DESede");
			}
		}
		return null;
	}

	private static void initKeyStoreAndPassphrase(Settings settings) throws Exception
	{
		if (keyStore == null)
		{
			InputStream is = null;
			try
			{
				passphrase = "passphrase".toCharArray(); //$NON-NLS-1$
				String filename = settings.getProperty("SocketFactory.SSLKeystorePath", ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (!"".equals(filename)) //$NON-NLS-1$
				{
					try
					{
						is = new FileInputStream(filename);
						passphrase = settings.getProperty("SocketFactory.SSLKeystorePassphrase", "").toCharArray(); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (Exception e)
					{
						Debug.error("SSLKeystorePath not found: " + filename + " fallback to default one", e);
					}
				}
				if (is == null)
				{
					is = SecuritySupport.class.getResourceAsStream("background.gif"); //$NON-NLS-1$
				}
				keyStore = KeyStore.getInstance("JKS"); //$NON-NLS-1$
				keyStore.load(is, passphrase);
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
		keyStore = null;
	}
}
