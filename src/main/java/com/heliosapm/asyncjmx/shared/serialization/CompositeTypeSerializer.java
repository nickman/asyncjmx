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
package com.heliosapm.asyncjmx.shared.serialization;

import java.beans.ConstructorProperties;
import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

/**
 * <p>Title: CompositeTypeSerializer</p>
 * <p>Description: Serializer for {@link CompositeType} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.CompositeTypeSerializer</code></p>
 */

public class CompositeTypeSerializer extends Serializer<CompositeType> {
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());

	@Override
	public void write(Kryo kryo, Output output, CompositeType cType) {
		// String typeName, String description, String[] itemNames, String[] itemDescriptions, OpenType<?>[] itemTypes
		output.writeString(cType.getTypeName());
		output.writeString(cType.getDescription());
		output.writeInt(cType.keySet().size());
		for(String s: cType.keySet()) {
			output.writeString(s);
			output.writeString(cType.getDescription(s));
			kryo.writeClassAndObject(output, cType.getType(s));	
			log.info("Wrote Composite Type Member [%s]/[%s]/[%s]-->[%s]", s, cType.getDescription(s), cType.getType(s), cType.getType(s).getClass().getName());
			
		}				
	}

	@Override
	public CompositeType read(Kryo kryo, Input input, Class<CompositeType> type) {
		String typeName = input.readString();
		String description = input.readString();
		int size = input.readInt();
		String[] itemNames = new String[size];
		String[] itemDescriptions = new String[size];
		OpenType<?>[] itemTypes = new OpenType[size];
		for(int i = 0; i < size; i++) {
			itemNames[i] = input.readString();
			itemDescriptions[i] = input.readString();
			Object value = kryo.readClassAndObject(input);
			if(!(value instanceof OpenType)) {
				log.warn("Ooops. Not an OpenType [%s]", value);
				itemTypes[i] =  toCompositeType(value);
			} else {
				itemTypes[i] =  (OpenType<?>)value; //otSerialzier.read(kryo, input, OpenType.class);
			}
		}
		try {
			return new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	protected static final Map<Class<?>, OpenType<?>> TYPE_MAPPING = new ConcurrentHashMap<Class<?>, OpenType<?>>();
	
	static {
		TYPE_MAPPING.put(Void.class, SimpleType.VOID);
		TYPE_MAPPING.put(void.class, SimpleType.VOID);
		TYPE_MAPPING.put(Boolean.class, SimpleType.BOOLEAN);
		TYPE_MAPPING.put(boolean.class, SimpleType.BOOLEAN);
		TYPE_MAPPING.put(Character.class, SimpleType.CHARACTER);
		TYPE_MAPPING.put(char.class, SimpleType.CHARACTER);
		TYPE_MAPPING.put(Byte.class, SimpleType.BYTE);
		TYPE_MAPPING.put(byte.class, SimpleType.BYTE);
		TYPE_MAPPING.put(Short.class, SimpleType.SHORT);
		TYPE_MAPPING.put(short.class, SimpleType.SHORT);
		TYPE_MAPPING.put(Integer.class, SimpleType.INTEGER);
		TYPE_MAPPING.put(int.class, SimpleType.INTEGER);
		TYPE_MAPPING.put(Long.class, SimpleType.LONG);
		TYPE_MAPPING.put(long.class, SimpleType.LONG);
		TYPE_MAPPING.put(Float.class, SimpleType.FLOAT);
		TYPE_MAPPING.put(float.class, SimpleType.FLOAT);
		TYPE_MAPPING.put(Double.class, SimpleType.DOUBLE);
		TYPE_MAPPING.put(double.class, SimpleType.DOUBLE);
		TYPE_MAPPING.put(String.class, SimpleType.STRING);
		TYPE_MAPPING.put(BigDecimal.class, SimpleType.BIGDECIMAL);
		TYPE_MAPPING.put(BigInteger.class, SimpleType.BIGINTEGER);
		TYPE_MAPPING.put(Date.class, SimpleType.DATE);
		TYPE_MAPPING.put(ObjectName.class, SimpleType.OBJECTNAME);		
	}
	
	protected OpenType toCompositeType(Object value) {
		try {
			Class<?> clazz = value.getClass();
			OpenType ct = TYPE_MAPPING.get(clazz);
			if(ct==null) {
				synchronized(TYPE_MAPPING) {
					ct = TYPE_MAPPING.get(clazz);
					if(ct==null) {
						ConstructorProperties cp = null;
						for(Constructor<?> ctor : clazz.getDeclaredConstructors()) {
							cp = ctor.getAnnotation(ConstructorProperties.class);
							if(cp!=null) break;
						}
						 
						if(cp==null) throw new Exception("No ConstructorProperties Found");
						int fieldCount = cp.value().length;
						String[] names = new String[fieldCount];
						String[] descrs = new String[fieldCount];
						OpenType[] types = new OpenType[fieldCount];
						
						for(int i = 0; i < fieldCount; i++) {
							String name = cp.value()[i];
							
							String methodName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
							Class<?> javaType = clazz.getDeclaredMethod(methodName).getReturnType();
							OpenType<?> openType = TYPE_MAPPING.get(javaType);
							names[i] = name;
							types[i] = openType;
							descrs[i] = "The value for " + name;				
						}
						ct = new CompositeType(clazz.getName(), "CompositeType for " + clazz.getSimpleName(), names, descrs, types);
						TYPE_MAPPING.put(clazz, ct);						
					}
				}
			}
			return ct;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get composite type for [" + value.getClass().getName() + "]", ex);
		}
	}

}
