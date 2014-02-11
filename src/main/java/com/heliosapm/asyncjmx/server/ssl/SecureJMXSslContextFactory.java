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

import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.heliosapm.asyncjmx.server.AsyncJMXServer;

/**
 * <p>Title: SecureJMXSslContextFactory</p>
 * <p>Description: A factory for {@link SSLContext}s when the {@link AsyncJMXServer} is running SSL</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.SecureJMXSslContextFactory</code></p>
 */

public class SecureJMXSslContextFactory {
	   private static final String PROTOCOL = "TLS";
	    private static final SSLContext SERVER_CONTEXT;
	    private static final SSLContext CLIENT_CONTEXT;

	    static {
	        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
	        if (algorithm == null) {
	            algorithm = "SunX509";
	        }

	        SSLContext serverContext = null;
	        SSLContext clientContext = null;
	        try {
	            KeyStore ks = KeyStore.getInstance("JKS");
	            ks.load(SecureJMXKeyStore.asInputStream(),
	            		SecureJMXKeyStore.getKeyStorePassword());

	            // Set up key manager factory to use our key store
	            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
	            kmf.init(ks, SecureJMXKeyStore.getCertificatePassword());

	            // Initialize the SSLContext to work with our key managers.
	            serverContext = SSLContext.getInstance(PROTOCOL);
	            serverContext.init(kmf.getKeyManagers(), null, null);
	        } catch (Exception e) {
	            throw new Error(
	                    "Failed to initialize the server-side SSLContext", e);
	        }

	        try {
	            clientContext = SSLContext.getInstance(PROTOCOL);
	            clientContext.init(null, SecureJMXTrustManagerFactory.getTrustManagers(), null);
	        } catch (Exception e) {
	            throw new Error(
	                    "Failed to initialize the client-side SSLContext", e);
	        }

	        SERVER_CONTEXT = serverContext;
	        CLIENT_CONTEXT = clientContext;
	    }

	    public static SSLContext getServerContext() {
	        return SERVER_CONTEXT;
	    }

	    public static SSLContext getClientContext() {
	        return CLIENT_CONTEXT;
	    }
}
