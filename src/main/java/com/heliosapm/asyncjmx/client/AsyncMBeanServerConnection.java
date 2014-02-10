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
package com.heliosapm.asyncjmx.client;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.netty.channel.Channel;

/**
 * <p>Title: AsyncMBeanServerConnection</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.AsyncMBeanServerConnection</code></p>
 */

public class AsyncMBeanServerConnection implements MBeanServerConnection {
	/** The connection to the JMX server */
	protected final Channel channel;
	/** Indicates if the client is synchronous */
	protected final boolean sync;
	/** The timeout for JMX invocations in ms */
	protected final long timeout;
	/** The request id serial number factory */
	protected final AtomicInteger serial = new AtomicInteger(0);
	
	
	
	
	
	/**
	 * Creates a new AsyncMBeanServerConnection
	 * @param channel The netty channel connection to the JMX server
	 * @param sync true for a sync client, false for async
	 * @param timeout The timeout in ms.
	 */
	public AsyncMBeanServerConnection(Channel channel, boolean sync, long timeout) {
		this.channel = channel;
		this.sync = sync;
		this.timeout = timeout;
	}

	/**
	 * Writes the JMX invocation request to the remote server
	 * @param opCode The op code for the jmx operation being invoked
	 * @param args The arguments to the invocation
	 * @return the response to the request if sync, or null if async
	 */
	protected Object writeRequest(byte opCode, Object...args) {
		int rId = serial.incrementAndGet();
		channel.write(new Object[] {opCode, rId, args});
		return rId;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException,
			InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name,
			Object[] params, String[] signature) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException,
			IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ObjectInstance getObjectInstance(ObjectName name)
			throws InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRegistered(ObjectName name) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Integer getMBeanCount() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(ObjectName name, String attribute)
			throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException,
			ReflectionException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object invoke(ObjectName name, String operationName,
			Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException,
			ReflectionException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDefaultDomain() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getDomains() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addNotificationListener(ObjectName name, ObjectName listener,
			NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException,
			IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name,
			ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException,
			IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener) throws InstanceNotFoundException,
			ListenerNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException,
			ListenerNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInstanceOf(ObjectName name, String className)
			throws InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return false;
	}

}
