/**
 * 
 */
package com.heliosapm.asyncjmx.client.protocol;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
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
	/** The provided JMXServiceURL */
	protected JMXServiceURL serviceURL = null;
	/** The JMXServiceURL provided default domain name */
	protected String domain = null;
	/** The connection ID assigned by the remote server */
	protected String connectionId = null;
	/** The MBeanServerConnection underlying the JMX connection */
	protected MBeanServerConnection connection = null;
	/** Async connector indicator */
	protected final boolean async;
	/** A set of registered connection listeners */
	protected final Set<ConnectionNotificationListener> connListeners = new CopyOnWriteArraySet<ConnectionNotificationListener>();
	
	
	/** The shared async jmx connection factory, initialized on first access */
	protected static volatile AsyncJMXClient socketJmxClient = null;
	/** Concurrency lock for the AsyncJMXClient */
	protected static final Object bootLock = new Object();
	
	
	
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
		return connection;
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
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		connListeners.add(new ConnectionNotificationListener(listener, filter, handback));
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		connListeners.remove(new ConnectionNotificationListener(listener));
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		if(listener==null) throw new IllegalArgumentException("The passed listener was null");
		connListeners.remove(new ConnectionNotificationListener(listener, filter, handback));
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
	
	/**
	 * <p>Title: ConnectionNotificationListener</p>
	 * <p>Description: Wraps a notification listener</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.client.protocol.SocketJMXConnector.ConnectionNotificationListener</code></p>
	 */
	static class ConnectionNotificationListener {
		/** The notification listener */
		private final NotificationListener listener;
		/** The optional notification filter */
		private final NotificationFilter filter;
		/** The optional notification handback */
		private final Object handback;
		
		/**
		 * Creates a new ConnectionNotificationListener
		 * @param listener The notification listener
		 * @param filter The optional notification filter
		 * @param handback The optional notification handback
		 */
		public ConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
			this.listener = listener;
			this.filter = filter;
			this.handback = handback;
		}
		
		/**
		 * Creates a new ConnectionNotificationListener with no filter or handback
		 * @param listener The notification listener
		 */
		public ConnectionNotificationListener(NotificationListener listener) {
			this(listener, null, null);
		}
		
		/**
		 * Applies the passed notification against this listener, filtering if a filter has been provided.
		 * @param notification The notification to apply
		 */
		protected void notify(Notification notification) {
			if(notification==null) return;
			if(filter != null) {
				if(filter.isNotificationEnabled(notification)) {
					listener.handleNotification(notification, handback);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((filter == null) ? 0 : filter.hashCode());
			result = prime * result
					+ ((handback == null) ? 0 : handback.hashCode());
			result = prime * result
					+ ((listener == null) ? 0 : listener.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConnectionNotificationListener other = (ConnectionNotificationListener) obj;
			if (filter == null) {
				if (other.filter != null)
					return false;
			} else if (!filter.equals(other.filter))
				return false;
			if (handback == null) {
				if (other.handback != null)
					return false;
			} else if (!handback.equals(other.handback))
				return false;
			if (listener == null) {
				if (other.listener != null)
					return false;
			} else if (!listener.equals(other.listener))
				return false;
			return true;
		}
		
		
	}
}
