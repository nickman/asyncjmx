/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
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
package com.heliosapm.asyncjmx.client.notifications;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.serialization.ListenerRegistrationSerializer;

/**
 * <p>Title: ListenerRegistration</p>
 * <p>Description: A registry for locally created and remotely registered notification listeners</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.notifications.ListenerRegistration</code></p>
 */
@DefaultSerializer(ListenerRegistrationSerializer.class)
public class ListenerRegistration implements NotificationListener {
	/** Static class logger */
	protected static final JMXLogger LOG = JMXLogger.getLogger(ListenerRegistration.class);
	
	/** The object name of the listener which will handle the notifications emitted by the registered MBean. */
	protected final ObjectName listenerName;
	/** The listener object which will handle the notifications emitted by the registered MBean. */
	protected final NotificationListener listener;
	/** The filter object. If filter is null, no filtering will be performed before handling notifications. */
	protected final NotificationFilter filter; 
	/** The context to be sent to the listener when a notification is emitted.  */
	protected final Object handback; 
	/** The registration id */
	protected final int registrationId;
	/** A set of non-wildcard mbean ObjectNames that this registration is listening to notifications from */
	protected final Set<ObjectName> subscribedObjectNames = new CopyOnWriteArraySet<ObjectName>();
	
	/** A channel local containing a map of listener registrations keyed by the registration id */
	protected static final ChannelLocal<Map<Integer, ListenerRegistration>> registrations = new ChannelLocal<Map<Integer, ListenerRegistration>>(true) {
		@Override
		protected Map<Integer, ListenerRegistration> initialValue(Channel channel) {
			return new HashMap<Integer, ListenerRegistration>();
		}
	};
	
	/**
	 * Creates a new server side ListenerRegistration from a Kryo input
	 * @param kryo The kryo instance
	 * @param input The lryo input stream
	 * @return The read in ListenerRegistration
	 */
	public static ListenerRegistration readIncoming(Kryo kryo, Input input) {
		return new ListenerRegistration(
				input.readInt(), 
				(NotificationFilter)kryo.readClassAndObject(input)
		);
	}
	
	
	/**
	 * Creates a new ListenerRegistration
	 * @param regId The registration id
	 * @param notificationFilter The notification filter
	 */
	private ListenerRegistration(int regId, NotificationFilter notificationFilter) {
		listenerName = null;
		handback = null;
		listener = null;
		registrationId = regId;		
		filter = notificationFilter;
	}
	
	
	/**
	 * Acquires the ListenerRegistration 
	 * @param channel The channel under which this listener is registered
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 * @return the ListenerRegistration 
	 */
	public static ListenerRegistration getInstance(Channel channel, NotificationListener listener, NotificationFilter filter, Object handback) {
		ListenerRegistration lr = new ListenerRegistration(listener, filter, handback);
		ListenerRegistration registered = registrations.get(channel).get(lr.getRegistrationId());
		if(registered!=null) {
			return registered;
		} 
		registrations.get(channel).put(lr.getRegistrationId(), lr);
		return lr;
	}
	
	/**
	 * Acquires the ListenerRegistration 
	 * @param channel The channel under which this listener is registered
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 * @return the ListenerRegistration 
	 */
	public static ListenerRegistration getInstance(Channel channel, ObjectName listener, NotificationFilter filter, Object handback) {
		ListenerRegistration lr = new ListenerRegistration(listener, filter, handback);
		ListenerRegistration registered = registrations.get(channel).get(lr.getRegistrationId());
		if(registered!=null) {
			return registered;
		} 
		registrations.get(channel).put(lr.getRegistrationId(), lr);
		return lr;
	}
	
	
	/**
	 * Creates a new ListenerRegistration
	 * @param listener The listener object which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 */
	protected ListenerRegistration(NotificationListener listener, NotificationFilter filter, Object handback) {
		this.listener = listener;
		this.filter = filter;
		this.handback = handback;
		this.listenerName = null;
		registrationId = this.hashCode();		
	}
	
	/**
	 * Creates a new ListenerRegistration
	 * @param listener The object name of the listener which will handle the notifications emitted by the registered MBean.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback The context to be sent to the listener when a notification is emitted. 
	 */
	protected ListenerRegistration(ObjectName listener, NotificationFilter filter, Object handback) {
		this.filter = filter;
		this.handback = handback;
		this.listenerName = listener;
		this.listener = null;
		registrationId = this.hashCode();
	}

	/**
	 * Dispatches a notification to the listener with the passed registrationId
	 * @param channel The channel on which the notification was received 
	 * @param registrationId The registration id of the target listener
	 * @param notification The notification to be dispatched
	 */
	public static void handleNotification(Channel channel, int registrationId, Notification notification) {
		ListenerRegistration lr = registrations.get(channel).get(registrationId);
		if(lr!=null) {
			lr.listener.handleNotification(notification, lr.handback);
		} else {
			LOG.warn("Unhandled Notification for id [%s]:[%s]", registrationId, notification);
		}
	}	
	

	/**
	 * Returns the object name of the listener which will handle the notifications emitted by the registered MBean.
	 * @return the listener ObjectName
	 */
	public ObjectName getListenerName() {
		return listenerName;
	}

	/**
	 * Returns the listener object which will handle the notifications emitted by the registered MBean.
	 * @return the listener
	 */
	public NotificationListener getListener() {
		return listener;
	}

	/**
	 * Returns the notification filter
	 * @return the notification filter
	 */
	public NotificationFilter getFilter() {
		return filter;
	}

	/**
	 * Returns the handback returned with notifications for this registration
	 * @return the handback
	 */
	public Object getHandback() {
		return handback;
	}

	/**
	 * Returns the registration id
	 * @return the registration Id
	 */
	public int getRegistrationId() {
		return registrationId;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result
				+ ((handback == null) ? 0 : handback.hashCode());
		result = prime * result
				+ ((listener == null) ? 0 : listener.hashCode());
		result = prime * result
				+ ((listenerName == null) ? 0 : listenerName.hashCode());
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
		ListenerRegistration other = (ListenerRegistration) obj;
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
		if (listenerName == null) {
			if (other.listenerName != null)
				return false;
		} else if (!listenerName.equals(other.listenerName))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("ListenerRegistration [");
		if (listenerName != null) {
			builder.append("listenerName=");
			builder.append(listenerName);
			builder.append(", ");
		}
		if (listener != null) {
			builder.append("listener=");
			builder.append(listener);
			builder.append(", ");
		}
		if (filter != null) {
			builder.append("filter=");
			builder.append(filter);
			builder.append(", ");
		}
		if (handback != null) {
			builder.append("handback=");
			builder.append(handback);
			builder.append(", ");
		}
		builder.append("registrationId=");
		builder.append(registrationId);
		builder.append(", ");
		if (subscribedObjectNames != null) {
			builder.append("subscribedObjectNames=");
			builder.append(toString(subscribedObjectNames, maxLen));
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Converts the subscribed ObjectName set to a string
	 * @param collection The collection of ObjectNames to render
	 * @param maxLen The max number of members to render
	 * @return the rendered string
	 */
	public String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext()
				&& i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}


	/**
	 * Delegates the handling of a notification to the registration's listener
	 * @param notification The notification to dispatch
	 * @param handback The notification handback
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	public void handleNotification(Notification notification, Object handback) {
		listener.handleNotification(notification, handback);
	}

}
