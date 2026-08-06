package com.servoy.j2db.server.ngclient.auth;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite that runs all authenticator type tests together to analyze combined coverage
 * of StatelessLoginHandler, OAuthHandler, AuthenticatorManager, DefaultLoginManager,
 * and CloudStatelessAccessManager.
 *
 * Run this suite to get a single coverage report across all authenticator types:
 * - DEFAULT (DefaultLoginManagerTest)
 * - AUTHENTICATOR (AuthenticatorManagerTest)
 * - OAUTH / OAUTH_AUTHENTICATOR (OAuthHandlerTest)
 * - SERVOY_CLOUD (CloudStatelessAccessManagerTest)
 * - LoginResult (LoginResultTest)
 *
 * @author emera
 */
@RunWith(Suite.class)
@SuiteClasses({
	DefaultLoginManagerTest.class,
	AuthenticatorManagerTest.class,
	OAuthHandlerTest.class,
	CloudStatelessAccessManagerTest.class,
	LoginResultTest.class
})
public class AllAuthenticatorTypesTestSuite
{
	// This class is intentionally empty. It's used only as a holder for the above annotations.
}
