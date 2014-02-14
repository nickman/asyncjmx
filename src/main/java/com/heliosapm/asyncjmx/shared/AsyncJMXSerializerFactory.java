/**
 * 
 */
package com.heliosapm.asyncjmx.shared;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.MBeanInfo;
import javax.management.MBeanServerNotification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.serialization.ArrayTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.AttributeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.CompositeDataSupportSerializer;
import com.heliosapm.asyncjmx.shared.serialization.CompositeTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer;
import com.heliosapm.asyncjmx.shared.serialization.MBeanServerNotificationSerializer;
import com.heliosapm.asyncjmx.shared.serialization.ObjectInstanceSerializer;
import com.heliosapm.asyncjmx.shared.serialization.ObjectNameSerializer;
import com.heliosapm.asyncjmx.shared.serialization.SimpleTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.TabularDataSupportSerializer;
import com.heliosapm.asyncjmx.shared.serialization.TabularTypeSerializer;
import com.heliosapm.asyncjmx.shared.serialization.UnmodifiableRandomAccessListSerializer;

/**
 * <p>Title: AsyncJMXSerializerFactory</p>
 * <p>Description:  A shared Kryo serializer factory to pick the best serializer for JMX constructs./p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.shared.AsyncJMXSerializerFactory</code></b>
 */

public class AsyncJMXSerializerFactory implements SerializerFactory {
	/** The singleton instance */
	private static volatile AsyncJMXSerializerFactory instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** Instance logger */
	private final JMXLogger log = JMXLogger.getLogger(getClass());
	
	/** A registry of class to serializer mappings */
	private final Map<Class<?>, Serializer<?>> classRegistry = new ConcurrentHashMap<Class<?>, Serializer<?>>();
	/** A registry of interface to serializer mappings */
	private final Map<Class<?>, Serializer<?>> ifaceRegistry = new ConcurrentHashMap<Class<?>, Serializer<?>>();
	
	/** The default serializer factory */
	private final SerializerFactory defaultSerializerFactory = new ReflectionSerializerFactory(FieldSerializer.class);
	/** The fallback serializer */
	private final JavaSerializer javaSerializer = new JavaSerializer();
	
	/**
	 * Acquires the singleton AsyncJMXSerializerFactory instance
	 * @return the singleton AsyncJMXSerializerFactory instance
	 */
	public static AsyncJMXSerializerFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new AsyncJMXSerializerFactory();
				}
			}
		}
		return instance;
	}
	
	private AsyncJMXSerializerFactory() {		
		classRegistryPut(runtimeForName("java.util.Collections$UnmodifiableRandomAccessList"), new UnmodifiableRandomAccessListSerializer());
		classRegistryPut(runtimeForName("sun.management.GcInfoCompositeData"), new CompositeDataSupportSerializer());
		classRegistryPut(ArrayType.class, new ArrayTypeSerializer());
		classRegistryPut(SimpleType.class, new SimpleTypeSerializer());
		classRegistryPut(TabularType.class, new TabularTypeSerializer());
		classRegistryPut(TabularDataSupport.class, new TabularDataSupportSerializer());
		classRegistryPut(CompositeType.class, new CompositeTypeSerializer());
		classRegistryPut(CompositeDataSupport.class, new CompositeDataSupportSerializer());
		classRegistryPut(HashSet.class, new CollectionSerializer());
		classRegistryPut(ObjectName.class, new ObjectNameSerializer());
		classRegistryPut(ObjectInstance.class, new ObjectInstanceSerializer());
		classRegistryPut(Attribute.class, new AttributeSerializer());
		classRegistryPut(MBeanServerNotification.class, new MBeanServerNotificationSerializer());
		classRegistryPut(MBeanInfo.class, new MBeanInfoSerializer());
		classRegistryPut(javax.management.QueryEval.class, javaSerializer);
		log.info("Created Singleton AsyncJMXSerializerFactory Instance");
	}
/*	new OpenTypeSerializer(),
	new MBeanServerNotificationSerializer()
*/	
	
	private void classRegistryPut(Class<?> clazz, Serializer<?> ser) {
		if(clazz!=null && ser!=null) {
			classRegistry.put(clazz, ser);
		}
	}
	
	void addRegistered(Kryo kryo) {
		for(Map.Entry<Class<?>, Serializer<?>> entry: classRegistry.entrySet()) {
			kryo.register(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.esotericsoftware.kryo.factories.SerializerFactory#makeSerializer(com.esotericsoftware.kryo.Kryo, java.lang.Class)
	 */
	@Override
	public Serializer<?> makeSerializer(Kryo kryo, Class<?> type) {
		Serializer<?> ser = classRegistry.get(type);
		if(ser!=null) return ser;
		return defaultSerializerFactory.makeSerializer(kryo, type);
	}
	
	public static Class<?> runtimeForName(String className) {
		try {
			return Class.forName(className);
		} catch (Exception ex) {
			return null;
			//throw new RuntimeException("Failed to load class [" + className + "]", ex);
		}
	}
	

}
