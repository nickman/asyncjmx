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
package com.heliosapm.asyncjmx.shared;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.heliosapm.asyncjmx.shared.serialization.NonSerializable;
import com.heliosapm.asyncjmx.shared.serialization.NullResult;
import com.heliosapm.asyncjmx.shared.serialization.ObjectNameSerializer;
import com.heliosapm.asyncjmx.shared.serialization.VoidResult;

/**
 * <p>Title: KryoFactory</p>
 * <p>Description: A factory for Kryo instance creation and serializer registration</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.KryoFactory</code></p>
 */

public class KryoFactory {
	/** The singleton instance */
	private static volatile KryoFactory instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Registered class index serial */
	private final AtomicInteger classIndex = new AtomicInteger(100);
	/** Registered classes */
	private final Map<Integer, Class<?>> registeredClasses = new ConcurrentSkipListMap<Integer, Class<?>>();
	/** Serializers associated to specific classes */
	private final Map<Class<?>, Serializer<?>> registeredSerializers = new ConcurrentHashMap<Class<?>, Serializer<?>>();
	
	/** A channel local for channel dedicated Kryo instances */
	protected final ChannelLocal<Kryo> channelKryo = new ChannelLocal<Kryo>(true){
		@Override
		protected Kryo initialValue(Channel channel) {
			return newKryo();
		}
	};	
	
	/**
	 * Returns the KryoFactory singleton instance
	 * @return the KryoFactory singleton instance
	 */
	public static KryoFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new KryoFactory();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new KryoFactory
	 */
	private KryoFactory() {
		
	}
	
	/**
	 * Returns a Kryo uniquely associated with the passed channel
	 * @param channel The channel to get a Kryo for
	 * @return an initialized Kryo instance
	 */
	public Kryo getKryo(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null");
		return channelKryo.get(channel);
	}
	
	/**
	 * Returns a new initialized Kryo instance
	 * @return a new initialized Kryo instance
	 */
	public Kryo newKryo() {
		Kryo kryo = new Kryo();
		for(Map.Entry<Class<?>, Serializer<?>> entry: REG_CLASS_SERIALIZERS.entrySet()) {
			kryo.register(entry.getKey(), entry.getValue());
		}

		for(Class<?> clazz: REG_CLASSES) {
			kryo.register(clazz);
		}
		
		for(Map.Entry<Integer, Class<?>> entry: registeredClasses.entrySet()) {
			Serializer<?> ser = registeredSerializers.get(entry.getValue());
			if(ser!=null) {
				kryo.register(entry.getValue(), ser, entry.getKey());
			} else {
				kryo.register(entry.getValue(), entry.getKey());
			}
		}		
		return kryo;
	}
	
	/**
	 * Registers a new class with Kryo
	 * @param clazz The class to register
	 * @param ser An optional serializer to register for the class
	 */
	public void registerClass(Class<?> clazz, Serializer<?> ser) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		if(!registeredClasses.containsValue(clazz)) {
			synchronized(registeredClasses) {
				if(!registeredClasses.containsValue(clazz)) {
					registeredClasses.put(classIndex.incrementAndGet(), clazz);
				}
			}
		}
		if(ser!=null) {
			registeredSerializers.put(clazz, ser);
		}
	}

	/**
	 * Registers a new class with Kryo
	 * @param clazz The class to register
	 */
	public void registerClass(Class<?> clazz) {
		registerClass(clazz, null);
	}
	
	/** A map to associate the statically registered serializers to the classes they serialize for */
	private static final Map<Class<?>, Serializer<?>> REG_CLASS_SERIALIZERS = new HashMap<Class<?>, Serializer<?>>();

	/** Don't change this order unless you know what you're doing */
	private static final Serializer<?>[] REG_SERIALIZERS = {
		new ObjectNameSerializer()
	};
	
	static {
		for(Serializer<?> ser: REG_SERIALIZERS) {
			try {
				//read(Kryo kryo, Input input, Class<ObjectName> type)
				Class<?> clazz = ser.getClass().getDeclaredMethod("read", Kryo.class, Input.class, Class.class).getReturnType();
				REG_CLASS_SERIALIZERS.put(clazz, ser);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to resolve serializer [" + ser.getClass().getName() + "]", ex);
			}
		}
	}
	
	
	/** Don't change this order unless you know what you're doing */
	private static final Class<?>[] REG_CLASSES = {
		NullResult.class, VoidResult.class, NonSerializable.class, IntrospectionException.class, 
		NotCompliantMBeanException.class, Set.class, ReflectionException.class, 
		IOException.class, ListenerNotFoundException.class, MBeanException.class, 
		ObjectName.class, String[].class, Object[].class, InstanceNotFoundException.class, 
		AttributeList.class, MBeanInfo.class, MBeanRegistrationException.class, QueryExp.class, 
		ObjectInstance.class, Attribute.class, InstanceAlreadyExistsException.class, InvalidAttributeValueException.class, 
		NotificationListener.class, AttributeNotFoundException.class, String.class, NotificationFilter.class		
	};
	
	//ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
	
}
