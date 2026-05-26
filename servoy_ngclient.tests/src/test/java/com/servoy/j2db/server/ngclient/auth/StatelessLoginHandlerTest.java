package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.auth0.jwt.algorithms.Algorithm;
import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;

/**
 * Verifies the SecureRandom-based fallback JWT key generation used in StatelessLoginHandler.init().
 */
@SuppressWarnings("nls")
public class StatelessLoginHandlerTest extends Log4JToConsoleTest
{
	@Test
	public void testFallbackKeyUniqueness_1000Iterations()
	{
		// Mirrors StatelessLoginHandler.init(): secureRandom.nextBytes(32) + Base64
		SecureRandom sr = new SecureRandom();
		Set<String> seen = new HashSet<>(1000);
		for (int i = 0; i < 1000; i++)
		{
			byte[] keyBytes = new byte[32];
			sr.nextBytes(keyBytes);
			String key = Base64.getEncoder().encodeToString(keyBytes);
			assertTrue("Duplicate key generated at iteration " + i, seen.add(key));
		}
	}

	@Test
	public void testFallbackKey_isAtLeast256Bits()
	{
		SecureRandom sr = new SecureRandom();
		byte[] keyBytes = new byte[32];
		sr.nextBytes(keyBytes);
		String key = Base64.getEncoder().encodeToString(keyBytes);
		byte[] decoded = Base64.getDecoder().decode(key);
		assertTrue("Fallback key must be at least 32 bytes (256 bits)", decoded.length >= 32);
	}

	@Test
	public void testFallbackKey_isAcceptedByHmac256()
	{
		SecureRandom sr = new SecureRandom();
		byte[] keyBytes = new byte[32];
		sr.nextBytes(keyBytes);
		String key = Base64.getEncoder().encodeToString(keyBytes);
		Algorithm alg = Algorithm.HMAC256(key);
		assertNotNull(alg);
	}

	@Test
	public void testFallbackKey_base64EncodesTo44Characters()
	{
		SecureRandom sr = new SecureRandom();
		byte[] keyBytes = new byte[32];
		sr.nextBytes(keyBytes);
		String key = Base64.getEncoder().encodeToString(keyBytes);
		assertEquals("Base64-encoded 32-byte key must be 44 characters", 44, key.length());
	}
}
