/**
 * 
 */
package com.heliosapm.asyncjmx.client.protocol;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: ClientProvider</p>
 * <p>Description: {@link JMXConnectorProvider} implementation for a plain socket AsyncJMX JMX connector.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.client.protocol.ClientProvider</code></b>
 */

public class ClientProvider implements JMXConnectorProvider {

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		if(serviceURL==null) throw new IllegalArgumentException("The passed JMXServiceURL was null", new Throwable());
		if("ajmx".equals(serviceURL.getProtocol()) || "syncajmx".equals(serviceURL.getProtocol())) {
			SocketJMXConnector connector = new SocketJMXConnector("ajmx".equals(serviceURL.getProtocol()));
			connector.serviceURL = serviceURL;
			return connector;
		} else if("ajmxs".equals(serviceURL.getProtocol())) {
			throw new UnsupportedOperationException("ajmxs not implemented yet");
		} else {
            throw new MalformedURLException("Not AsyncJMX protocol: " + serviceURL.getProtocol());
        }		
	}
}
