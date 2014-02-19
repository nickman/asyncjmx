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
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ManagementPermission;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.AttributeValueExp;
import javax.management.BadAttributeValueExpException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadStringOperationException;
import javax.management.DescriptorKey;
import javax.management.ImmutableDescriptor;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidApplicationException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanPermission;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.MBeanServerPermission;
import javax.management.MBeanTrustPermission;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.Query;
import javax.management.QueryEval;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.ServiceNotFoundException;
import javax.management.StandardEmitterMBean;
import javax.management.StandardMBean;
import javax.management.StringValueExp;
import javax.management.loading.DefaultLoaderRepository;
import javax.management.loading.MLet;
import javax.management.loading.MLetContent;
import javax.management.loading.PrivateMLet;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataInvocationHandler;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.NotificationResult;
import javax.management.remote.SubjectDelegationPermission;
import javax.management.remote.TargetedNotification;
import javax.management.timer.Timer;
import javax.management.timer.TimerNotification;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.heliosapm.asyncjmx.shared.serialization.ArrayTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.AttributeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.CompositeDataSupportSerializer;
import com.heliosapm.asyncjmx.shared.serialization.CompositeTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer;
import com.heliosapm.asyncjmx.shared.serialization.MBeanServerNotificationSerializer;
import com.heliosapm.asyncjmx.shared.serialization.NonSerializable;
import com.heliosapm.asyncjmx.shared.serialization.NullResult;
import com.heliosapm.asyncjmx.shared.serialization.ObjectInstanceSerializer;
import com.heliosapm.asyncjmx.shared.serialization.ObjectNameSerializer;
import com.heliosapm.asyncjmx.shared.serialization.SimpleTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.TabularDataSupportSerializer;
import com.heliosapm.asyncjmx.shared.serialization.TabularTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.UnmodifiableRandomAccessListSerializer;
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
	
	/** A {@link VoidResult} serialized into a ChannelBuffer */
	private final ChannelBuffer voidResultBuffer;
	/** A {@link NullResult} serialized into a ChannelBuffer */
	private final ChannelBuffer nullResultBuffer;
	
	
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
	 * Returns a shrink-wrapped ChannelBuffer containing the passed object Kryo serialized  
	 * @param kryo The kryo to serialize with
	 * @param obj The object to serialize
	 * @return The ChannelBuffer containing the serialized object
	 */
	private ChannelBuffer serialize(Kryo kryo, Object obj) {
		try {
			ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
			ChannelBufferOutputStream cbos = new ChannelBufferOutputStream(cb);
			Output out = new Output(cbos);
			kryo.writeClassAndObject(out, obj);
			out.flush();
			cbos.flush();
			return ChannelBuffers.wrappedBuffer(cb.array());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to serialize object [" + obj + "]", ex);
		}
		
		
	}
	
	/**
	 * Creates a new KryoFactory
	 */
	private KryoFactory() {
		Kryo k = newKryo();
		voidResultBuffer = ChannelBuffers.unmodifiableBuffer(serialize(k, VoidResult.Instance));
		nullResultBuffer = ChannelBuffers.unmodifiableBuffer(serialize(k, NullResult.Instance));
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
	 * Returns a serialized VoidResult instance
	 * @return a ChannelBuffer containing a serialized VoidResult instance
	 */
	public ChannelBuffer getVoidResult() {
		return ChannelBuffers.copiedBuffer(voidResultBuffer);
	}
	
	/**
	 * Returns a serialized NullResult instance
	 * @return a ChannelBuffer containing a serialized NullResult instance
	 */
	public ChannelBuffer getNullResult() {
		return ChannelBuffers.copiedBuffer(nullResultBuffer);
	}
	
	/**
	 * Serializes the passed object to a NonSerializable instance in a ChannelBuffer
	 * @param kryo The kryo to use. If null, a new one will be created
	 * @param obj The object to serialized a NonSerializable for
	 * @return A ChannelBuffer containing a NonSerializable for the passed object 
	 */
	public ChannelBuffer getNonSerializable(Kryo kryo, Object obj) {
		return serialize(kryo==null ? newKryo() : kryo, new NonSerializable(obj));
	}
	
	/**
	 * Serializes the passed object to a NonSerializable instance in a ChannelBuffer
	 * @param obj The object to serialized a NonSerializable for
	 * @return A ChannelBuffer containing a NonSerializable for the passed object 
	 */
	public ChannelBuffer getNonSerializable(Object obj) {
		return getNonSerializable(null, obj);
	}
	
	/**
	 * Returns a new initialized Kryo instance
	 * @return a new initialized Kryo instance
	 */
	public Kryo newKryo() {
		Kryo kryo = new Kryo();
//		kryo.setDefaultSerializer(AsyncJMXSerializerFactory.getInstance());
		kryo.setAsmEnabled(true);
		kryo.setReferences(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		for(Class<?> clazz: REG_CLASSES) {
			kryo.register(clazz);
		}
//		AsyncJMXSerializerFactory.getInstance().addRegistered(kryo);
		kryo.register(runtimeForName("java.util.Collections$UnmodifiableRandomAccessList"), new UnmodifiableRandomAccessListSerializer());
//		kryo.register(runtimeForName("sun.management.GcInfoCompositeData"), new CompositeDataSupportSerializer());
//		kryo.register(HashSet.class, new CollectionSerializer());
		kryo.register(ObjectName.class, new ObjectNameSerializer());
		kryo.register(ObjectInstance.class, new ObjectInstanceSerializer());
		kryo.register(Attribute.class, new AttributeSerializer());
		kryo.register(MBeanServerNotification.class, new MBeanServerNotificationSerializer());
		kryo.register(MBeanInfo.class, new MBeanInfoSerializer());
		kryo.register(JMXOp.class, new JMXOp.JMXOpSerializer());
		
		/* ===  Having trouble with these
		kryo.register(ArrayType.class, new ArrayTypeSerializer());
		kryo.register(SimpleType.class, new SimpleTypeSerializer());
		kryo.register(TabularType.class, new TabularTypeSerializer());
		kryo.register(CompositeType.class, new CompositeTypeSerializer());
		*/
		kryo.register(TabularDataSupport.class, new TabularDataSupportSerializer());		
		kryo.register(CompositeDataSupport.class, new CompositeDataSupportSerializer());		
		
		JavaSerializer javaSer = new JavaSerializer();
		
		kryo.register(ArrayType.class, javaSer);
		kryo.register(SimpleType.class, javaSer);
		kryo.register(TabularType.class, javaSer);
		kryo.register(CompositeType.class, javaSer);
//		kryo.register(TabularDataSupport.class, javaSer);		
//		kryo.register(CompositeDataSupport.class, javaSer);

		return kryo;
	}
	
	public Class<?> getClassForId(int id) {
		return newKryo().getRegistration(id).getType();
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
	}

	/**
	 * Registers a new class with Kryo
	 * @param clazz The class to register
	 */
	public void registerClass(Class<?> clazz) {
		registerClass(clazz, null);
	}
	
	
	
	/** Don't change this order unless you know what you're doing */
	private static final Class<?>[] REG_CLASSES = {
		NullResult.class, VoidResult.class, NonSerializable.class, IntrospectionException.class, 
		NotCompliantMBeanException.class, Set.class, ReflectionException.class, 
		IOException.class, ListenerNotFoundException.class, MBeanException.class, 
		Object[].class, InstanceNotFoundException.class, 
		AttributeList.class, MBeanInfo.class, MBeanRegistrationException.class, QueryExp.class, 
		InstanceAlreadyExistsException.class, InvalidAttributeValueException.class, 
		NotificationListener.class, AttributeNotFoundException.class, NotificationFilter.class, HashSet.class, 
		ObjectName.class, ObjectInstance.class, Attribute.class, runtimeForName("java.util.Collections$UnmodifiableRandomAccessList"),
		ArrayType.class, CompositeDataInvocationHandler.class, CompositeDataSupport.class, CompositeType.class,
		OpenMBeanAttributeInfoSupport.class, OpenMBeanConstructorInfoSupport.class, OpenMBeanInfoSupport.class,
		OpenMBeanOperationInfoSupport.class, OpenMBeanParameterInfoSupport.class, OpenType.class, SimpleType.class,
		TabularDataSupport.class, TabularType.class, DefaultLoaderRepository.class, MLet.class, MLetContent.class, PrivateMLet.class,
		DescriptorSupport.class, ModelMBeanAttributeInfo.class, ModelMBeanConstructorInfo.class, ModelMBeanInfoSupport.class, 
		ModelMBeanNotificationInfo.class, ModelMBeanOperationInfo.class, RequiredModelMBean.class, JMXConnectionNotification.class, 
		JMXConnectorFactory.class, JMXConnectorServer.class, JMXConnectorServerFactory.class, JMXPrincipal.class, JMXServiceURL.class, 
		NotificationResult.class, SubjectDelegationPermission.class, TargetedNotification.class, Timer.class, TimerNotification.class,
		LockInfo.class, ManagementFactory.class, ManagementPermission.class, MemoryNotificationInfo.class, MemoryUsage.class, MonitorInfo.class, ThreadInfo.class, 
		MemoryType.class, Attribute.class, AttributeChangeNotification.class, AttributeChangeNotificationFilter.class, 
		AttributeList.class, AttributeValueExp.class, javax.management.DefaultLoaderRepository.class, ImmutableDescriptor.class, 
		MBeanAttributeInfo.class, MBeanConstructorInfo.class, MBeanFeatureInfo.class, MBeanInfo.class, MBeanNotificationInfo.class, 
		MBeanOperationInfo.class, MBeanParameterInfo.class, MBeanPermission.class, MBeanServerBuilder.class, MBeanServerDelegate.class, 
		MBeanServerFactory.class, MBeanServerInvocationHandler.class, MBeanServerNotification.class, MBeanServerPermission.class, 
		MBeanTrustPermission.class, Notification.class, NotificationBroadcasterSupport.class, NotificationFilterSupport.class, 
		ObjectInstance.class, ObjectName.class, Query.class, QueryEval.class, StandardEmitterMBean.class, StandardMBean.class, 
		StringValueExp.class, AttributeNotFoundException.class, BadAttributeValueExpException.class, BadBinaryOpValueExpException.class, 
		BadStringOperationException.class, InstanceAlreadyExistsException.class, InstanceNotFoundException.class, IntrospectionException.class, 
		InvalidApplicationException.class, InvalidAttributeValueException.class, JMException.class, JMRuntimeException.class, 
		ListenerNotFoundException.class, MalformedObjectNameException.class, MBeanException.class, MBeanRegistrationException.class, 
		NotCompliantMBeanException.class, OperationsException.class, ReflectionException.class, RuntimeErrorException.class, 
		RuntimeMBeanException.class, RuntimeOperationsException.class, ServiceNotFoundException.class, DescriptorKey.class, MXBean.class, 





		
		// ObjectName.class, ObjectInstance.class, Attribute.class
	};
	
	public static Class<?> runtimeForName(String className) {
		try {
			return Class.forName(className);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load class [" + className + "]", ex);
		}
	}
	
	//ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
	
}
