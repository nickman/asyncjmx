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

import java.util.Set;

import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * <p>Title: MBeanServerConnectionCallback</p>
 * <p>Description: The async JMX callback site for {@link MBeanServerConnection} responses.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.MBeanServerConnectionCallback</code></p>
 */

public interface MBeanServerConnectionCallback {
	/**
	 * Asynch response handler for {@link MBeanServerConnection#queryMBeans(javax.management.ObjectName,javax.management.QueryExp)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onQueryMBeans(int rId, Set<ObjectInstance> ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#queryMBeans(javax.management.ObjectName,javax.management.QueryExp)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onQueryMBeansFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#queryNames(javax.management.ObjectName,javax.management.QueryExp)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onQueryNames(int rId, Set<ObjectName> ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#queryNames(javax.management.ObjectName,javax.management.QueryExp)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onQueryNamesFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#getMBeanCount()} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetMBeanCount(int rId, Integer ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getMBeanCount()} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetMBeanCountFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#getDefaultDomain()} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetDefaultDomain(int rId, String ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getDefaultDomain()} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetDefaultDomainFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#getDomains()} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetDomains(int rId, String[] ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getDomains()} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetDomainsFail(int rId, Throwable t);

	/**
	 * Asynch completion handler for {@link MBeanServerConnection#addNotificationListener(javax.management.ObjectName,javax.management.ObjectName,javax.management.NotificationFilter,java.lang.Object)} 
	 * @param rId The serial number of the request
	 */
	public void onAddNotificationListener(int rId);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#addNotificationListener(javax.management.ObjectName,javax.management.ObjectName,javax.management.NotificationFilter,java.lang.Object)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onAddNotificationListenerFail(int rId, Throwable t);

	/**
	 * Asynch completion handler for {@link MBeanServerConnection#removeNotificationListener(javax.management.ObjectName,javax.management.NotificationListener,javax.management.NotificationFilter,java.lang.Object)} 
	 * @param rId The serial number of the request
	 */
	public void onRemoveNotificationListener(int rId);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#removeNotificationListener(javax.management.ObjectName,javax.management.NotificationListener,javax.management.NotificationFilter,java.lang.Object)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onRemoveNotificationListenerFail(int rId, Throwable t);


	/**
	 * Asynch response handler for {@link MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetMBeanInfo(int rId, MBeanInfo ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetMBeanInfoFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#createMBean(java.lang.String,javax.management.ObjectName,javax.management.ObjectName,java.lang.Object[],java.lang.String[])} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onCreateMBean(int rId, ObjectInstance ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#createMBean(java.lang.String,javax.management.ObjectName,javax.management.ObjectName,java.lang.Object[],java.lang.String[])} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onCreateMBeanFail(int rId, Throwable t);


	/**
	 * Asynch completion handler for {@link MBeanServerConnection#unregisterMBean(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 */
	public void onUnregisterMBean(int rId);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#unregisterMBean(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onUnregisterMBeanFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#getObjectInstance(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetObjectInstance(int rId, ObjectInstance ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getObjectInstance(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetObjectInstanceFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#invoke(javax.management.ObjectName,java.lang.String,java.lang.Object[],java.lang.String[])} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onInvoke(int rId, Object ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#invoke(javax.management.ObjectName,java.lang.String,java.lang.Object[],java.lang.String[])} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onInvokeFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#isRegistered(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onIsRegistered(int rId, boolean ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#isRegistered(javax.management.ObjectName)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onIsRegisteredFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#getAttributes(javax.management.ObjectName,java.lang.String[])} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetAttributes(int rId, AttributeList ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getAttributes(javax.management.ObjectName,java.lang.String[])} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetAttributesFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#getAttribute(javax.management.ObjectName,java.lang.String)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onGetAttribute(int rId, Object ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#getAttribute(javax.management.ObjectName,java.lang.String)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onGetAttributeFail(int rId, Throwable t);

	/**
	 * Asynch completion handler for {@link MBeanServerConnection#setAttribute(javax.management.ObjectName,javax.management.Attribute)} 
	 * @param rId The serial number of the request
	 */
	public void onSetAttribute(int rId);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#setAttribute(javax.management.ObjectName,javax.management.Attribute)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onSetAttributeFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#isInstanceOf(javax.management.ObjectName,java.lang.String)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onIsInstanceOf(int rId, boolean ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#isInstanceOf(javax.management.ObjectName,java.lang.String)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onIsInstanceOfFail(int rId, Throwable t);

	/**
	 * Asynch response handler for {@link MBeanServerConnection#setAttributes(javax.management.ObjectName,javax.management.AttributeList)} 
	 * @param rId The serial number of the request
	 * @param ret The return value of the remote call
	 */ 
	public void onSetAttributes(int rId, AttributeList ret);

	/**
	 * Asynch exception handler for {@link MBeanServerConnection#setAttributes(javax.management.ObjectName,javax.management.AttributeList)} 
	 * @param rId The serial number of the request
	 * @param t The thrown exception from the remote call
	 */ 
	public void onSetAttributesFail(int rId, Throwable t);
}
