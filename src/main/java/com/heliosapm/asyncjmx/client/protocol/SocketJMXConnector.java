/**
 * 
 */
package com.heliosapm.asyncjmx.client.protocol;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import com.heliosapm.asyncjmx.client.AsyncJMXClient;

/**
 * <p>Title: SocketJMXConnector</p>
 * <p>Description:   </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.client.protocol.SocketJMXConnector</code></b>
 */

public class SocketJMXConnector implements JMXConnector {
	/** The MBeanServer the connector connects to */
	protected MBeanServer mbeanServer = null;
	/** The provided JMXServiceURL */
	protected JMXServiceURL serviceURL = null;
	/** The JMXServiceURL provided default domain name */
	protected String domain = null;
	/** The faux connection ID assigned to this imaginary connection */
	protected String connectionId = null;
	/** A connection ID serial number generator */
	protected static final AtomicLong serial = new AtomicLong(0);
	
	protected MBeanServerConnection connection = null;
	
	protected static volatile AsyncJMXClient socketJmxClient = null;
	protected static final Object bootLock = new Object();
	
	protected final boolean async;
	
	SocketJMXConnector(boolean async) {
		this.async = async;
		if(socketJmxClient==null) {
			synchronized(bootLock) {
				if(socketJmxClient==null) {
					socketJmxClient = new AsyncJMXClient();
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect()
	 */
	@Override
	public void connect() throws IOException {		
		connection = socketJmxClient.connectMBeanServerConnection(serviceURL.getHost(), serviceURL.getPort(), async);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect(java.util.Map)
	 */
	@Override
	public void connect(Map<String, ?> env) throws IOException {
		connect();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection()
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return connection;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection(javax.security.auth.Subject)
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
		return mbeanServer;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#addConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getConnectionId()
	 */
	@Override
	public String getConnectionId() throws IOException {
		return connectionId;
	}

	public static void main(String[] args) {
		try {
			JMXServiceURL url = new JMXServiceURL("service:jmx:syncajmx://localhost:9061");
			String[] domains = JMXConnectorFactory.connect(url).getMBeanServerConnection().getDomains();
			System.out.println(Arrays.toString(domains));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
}
