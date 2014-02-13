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
package com.heliosapm.asyncjmx.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.heliosapm.asyncjmx.shared.serialization.AttributeListSerializer;
import com.heliosapm.asyncjmx.shared.serialization.ObjectInstanceSerializer;
import com.heliosapm.asyncjmx.shared.serialization.ObjectNameSerializer;

/**
 * <p>Title: JMXOpCode</p>
 * <p>Description: Enumerates the JMX Ops of {@link MBeanServerConnection}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.JMXOpCode</code></p>
 */

public enum JMXOpCode {
	/** JMX Op enum member for {@link MBeanServerConnection#queryMBeans(javax.management.ObjectName,javax.management.QueryExp)} */
	QUERYMBEANS((byte)0, java.util.HashSet.class, new CollectionSerializer(ObjectInstance.class, new ObjectInstanceSerializer(), false), ObjectName.class, QueryExp.class),
	/** JMX Op enum member for {@link MBeanServerConnection#queryNames(javax.management.ObjectName,javax.management.QueryExp)} */
	QUERYNAMES((byte)1, java.util.HashSet.class, new CollectionSerializer(ObjectName.class, new ObjectNameSerializer(), false), ObjectName.class, QueryExp.class),
	/** JMX Op enum member for {@link MBeanServerConnection#getMBeanCount()} */
	GETMBEANCOUNT((byte)2, java.lang.Integer.class),
	/** JMX Op enum member for {@link MBeanServerConnection#getDefaultDomain()} */
	GETDEFAULTDOMAIN((byte)3, java.lang.String.class),
	/** JMX Op enum member for {@link MBeanServerConnection#getDomains()} */
	GETDOMAINS((byte)4, java.lang.String[].class),
	/** JMX Op enum member for {@link MBeanServerConnection#addNotificationListener(javax.management.ObjectName,javax.management.ObjectName,javax.management.NotificationFilter,java.lang.Object)} */
	ADDNOTIFICATIONLISTENER_OONO((byte)5, void.class, ObjectName.class, ObjectName.class, NotificationFilter.class, Object.class),
	/** JMX Op enum member for {@link MBeanServerConnection#addNotificationListener(javax.management.ObjectName,javax.management.NotificationListener,javax.management.NotificationFilter,java.lang.Object)} */
	ADDNOTIFICATIONLISTENER_ONNO((byte)6, void.class, ObjectName.class, NotificationListener.class, NotificationFilter.class, Object.class),
	/** JMX Op enum member for {@link MBeanServerConnection#removeNotificationListener(javax.management.ObjectName,javax.management.NotificationListener,javax.management.NotificationFilter,java.lang.Object)} */
	REMOVENOTIFICATIONLISTENER_ONNO((byte)7, void.class, ObjectName.class, NotificationListener.class, NotificationFilter.class, Object.class),
	/** JMX Op enum member for {@link MBeanServerConnection#removeNotificationListener(javax.management.ObjectName,javax.management.ObjectName,javax.management.NotificationFilter,java.lang.Object)} */
	REMOVENOTIFICATIONLISTENER_OONO((byte)8, void.class, ObjectName.class, ObjectName.class, NotificationFilter.class, Object.class),
	/** JMX Op enum member for {@link MBeanServerConnection#removeNotificationListener(javax.management.ObjectName,javax.management.ObjectName)} */
	REMOVENOTIFICATIONLISTENER_OO((byte)9, void.class, ObjectName.class, ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#removeNotificationListener(javax.management.ObjectName,javax.management.NotificationListener)} */
	REMOVENOTIFICATIONLISTENER_ON((byte)10, void.class, ObjectName.class, NotificationListener.class),
	/** JMX Op enum member for {@link MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)} */
	GETMBEANINFO((byte)11, javax.management.MBeanInfo.class, new JavaSerializer(), ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#createMBean(java.lang.String,javax.management.ObjectName,javax.management.ObjectName,java.lang.Object[],java.lang.String[])} */
	CREATEMBEAN_SOOOS((byte)12, javax.management.ObjectInstance.class, String.class, ObjectName.class, ObjectName.class, Object[].class, String[].class),
	/** JMX Op enum member for {@link MBeanServerConnection#createMBean(java.lang.String,javax.management.ObjectName,java.lang.Object[],java.lang.String[])} */
	CREATEMBEAN_SOOS((byte)13, javax.management.ObjectInstance.class, String.class, ObjectName.class, Object[].class, String[].class),
	/** JMX Op enum member for {@link MBeanServerConnection#createMBean(java.lang.String,javax.management.ObjectName,javax.management.ObjectName)} */
	CREATEMBEAN_SOO((byte)14, javax.management.ObjectInstance.class, String.class, ObjectName.class, ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#createMBean(java.lang.String,javax.management.ObjectName)} */
	CREATEMBEAN_SO((byte)15, javax.management.ObjectInstance.class, String.class, ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#unregisterMBean(javax.management.ObjectName)} */
	UNREGISTERMBEAN((byte)16, void.class, ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#getObjectInstance(javax.management.ObjectName)} */
	GETOBJECTINSTANCE((byte)17, javax.management.ObjectInstance.class, ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#invoke(javax.management.ObjectName,java.lang.String,java.lang.Object[],java.lang.String[])} */
	INVOKE((byte)18, java.lang.Object.class, ObjectName.class, String.class, Object[].class, String[].class),
	/** JMX Op enum member for {@link MBeanServerConnection#isRegistered(javax.management.ObjectName)} */
	ISREGISTERED((byte)19, boolean.class, ObjectName.class),
	/** JMX Op enum member for {@link MBeanServerConnection#getAttributes(javax.management.ObjectName,java.lang.String[])} */
//	GETATTRIBUTES((byte)20, javax.management.AttributeList.class, new CollectionSerializer(Attribute.class, new AttributeSerializer(), false), ObjectName.class, String[].class),
	GETATTRIBUTES((byte)20, javax.management.AttributeList.class, new AttributeListSerializer(), ObjectName.class, String[].class),
	/** JMX Op enum member for {@link MBeanServerConnection#getAttribute(javax.management.ObjectName,java.lang.String)} */
	GETATTRIBUTE((byte)21, java.lang.Object.class, ObjectName.class, String.class),
	/** JMX Op enum member for {@link MBeanServerConnection#setAttribute(javax.management.ObjectName,javax.management.Attribute)} */
	SETATTRIBUTE((byte)22, void.class, ObjectName.class, Attribute.class),
	/** JMX Op enum member for {@link MBeanServerConnection#isInstanceOf(javax.management.ObjectName,java.lang.String)} */
	ISINSTANCEOF((byte)23, boolean.class, ObjectName.class, String.class),
	/** JMX Op enum member for {@link MBeanServerConnection#setAttributes(javax.management.ObjectName,javax.management.AttributeList)} */
	SETATTRIBUTES((byte)24, javax.management.AttributeList.class, ObjectName.class, AttributeList.class);	
	
	/** A map of JMXOpCode keyed by the byte op code */
	public static final Map<Byte, JMXOpCode> CODE2OP;
	
	static {
		JMXOpCode[] values = JMXOpCode.values();
		Map<Byte, JMXOpCode> tmp = new HashMap<Byte, JMXOpCode>(values.length);
		for(JMXOpCode oc: values) {
			tmp.put(oc.opCode, oc);
		}
		CODE2OP = Collections.unmodifiableMap(tmp);
	}
	
	JMXOpCode(byte opCode, Class<?> returnType, Serializer<?> serializer, Class<?>...params) {
		this.opCode = opCode;
		this.params = params;
		this.returnType = returnType;
		this.serializer = serializer;
	}
	
	JMXOpCode(byte opCode, Class<?> returnType, Class<?>...params) {
		this(opCode, returnType, null, params);
	}
	
	
	/** The JMX Op opCode byte */
	public final byte opCode;
	/** The JMX Op return type */
	public final Class<?> returnType;
	/** The JMX Op signature */
	private final Class<?>[] params;
	/** The serializer for the response type, if applicable */
	private final Serializer<?> serializer;
	
	public boolean hasSerializer() {
		return serializer!=null;
	}
	
	public Serializer<?> getSerializer() {
		return serializer;
	}
	
	/**
	 * Decodes the passed byte JMXOpCode code
	 * @param opCode The JMXOpCode code
	 * @return the decoded JMXOpCode
	 */
	public static JMXOpCode decode(byte opCode) {
		JMXOpCode jop = CODE2OP.get(opCode);
		if(jop==null) throw new IllegalArgumentException("Invalid op code [" + opCode + "] for JMXOpCode");
		return jop;
	}
	
	/**
	 * Returns the JMX Op method signature
	 * @return the JMX Op method signaturet
	 */
	public Class<?>[] signature() {
		return params.clone();
	}
	
}
