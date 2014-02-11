/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.asyncjmx.server.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;


/**
 * <p>Title: SecureJMXTrustManagerFactory</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.ssl.SecureJMXTrustManagerFactory</code></p>
 */

public class SecureJMXTrustManagerFactory extends TrustManagerFactorySpi {

	   /** The X509 Trust Manager */
	private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
	        public X509Certificate[] getAcceptedIssuers() {
	            return new X509Certificate[0];
	        }

	        public void checkClientTrusted(
	                X509Certificate[] chain, String authType) throws CertificateException {
	            // Always trust - it is an example.
	            // You should do something in the real world.
	            // You will reach here only if you enabled client certificate auth,
	            // as described in SecureChatSslContextFactory.
	            System.err.println(
	                    "UNKNOWN CLIENT CERTIFICATE: " + chain[0].getSubjectDN());
	        }

	        public void checkServerTrusted(
	                X509Certificate[] chain, String authType) throws CertificateException {
	            // Always trust - it is an example.
	            // You should do something in the real world.
	            System.err.println(
	                    "UNKNOWN SERVER CERTIFICATE: " + chain[0].getSubjectDN());
	        }
	    };

	    /**
	     * Returns the trust managers
	     * @return an array of trust managers
	     */
	    public static TrustManager[] getTrustManagers() {
	        return new TrustManager[] { DUMMY_TRUST_MANAGER };
	    }

	    /**
	     * {@inheritDoc}
	     * @see javax.net.ssl.TrustManagerFactorySpi#engineGetTrustManagers()
	     */
	    @Override
	    protected TrustManager[] engineGetTrustManagers() {
	        return getTrustManagers();
	    }

	    /**
	     * {@inheritDoc}
	     * @see javax.net.ssl.TrustManagerFactorySpi#engineInit(java.security.KeyStore)
	     */
	    @Override
	    protected void engineInit(KeyStore keystore) throws KeyStoreException {
	        // Unused
	    }

	    /**
	     * {@inheritDoc}
	     * @see javax.net.ssl.TrustManagerFactorySpi#engineInit(javax.net.ssl.ManagerFactoryParameters)
	     */
	    @Override
	    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
	        // Unused
	    }
}
