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

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.heliosapm.asyncjmx.shared.KryoFactory;

/**
 * <p>Title: ObjectNameSerializer</p>
 * <p>Description: An optimized kryo JMX ObjectName serializer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.ObjectNameSerializer</code></p>
 */

public class ObjectNameSerializer extends Serializer<ObjectName> {
	/** A map of ObjectName encodes keyed by the ObjectName */
	protected static final Map<ObjectName, Integer> objectNameEncodes = new ConcurrentHashMap<ObjectName, Integer>();
	/** A map of ObjectName decodes keyed by the int */
	protected static final Map<Integer, ObjectName> objectNameDecodes = new ConcurrentHashMap<Integer, ObjectName>();
	
	@Override
	public void write(Kryo kryo, Output output, ObjectName object) {
		Integer encode = objectNameEncodes.get(object);
		if(encode==null) {
			output.writeByte(0);
			output.writeString(object.getCanonicalName());
		} else {
			output.writeByte(1);
			output.writeInt(encode);
		}
	}

	@Override
	public ObjectName read(Kryo kryo, Input input, Class<ObjectName> type) {
		byte format = input.readByte();
		if(format==0) {
			String os = input.readString();
			try {
				return new ObjectName(os);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create ObjectName from string [" + os + "]", ex);
			}
		} else {
			int key = input.readInt();
			ObjectName on = objectNameDecodes.get(key);
			if(on==null) {
				throw new RuntimeException("Failed to decode ObjectName from int [" + key + "]");
			}
			return on;
		}
	}

	public static void main(String[] args) {
		log("KryoObjectName Test");
		try {
			ObjectName on = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
			Kryo k = new Kryo();
			k.setAsmEnabled(true);
			write("Plain", on, k);
			k.register(ObjectName.class);
			write("Reg", on, k);
			k = KryoFactory.getInstance().newKryo();
			k.register(ObjectName.class, new ObjectNameSerializer());
			write("Ser", on, k);

			
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void write(String desc, ObjectName on, Kryo kryo) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			UnsafeOutput out = new UnsafeOutput(baos);
			log("[%s] Writing Out", desc);
			kryo.writeClassAndObject(out, on);
			//k.writeObject(out, Instance);
			out.flush();
			baos.flush();
			log("[%s] Write Complete", desc);
			byte[] bytes = baos.toByteArray();
			log("[%s] Ser Size: %s", desc, bytes.length);
			log("[%s] Reading in", desc);
			UnsafeInput in = new UnsafeInput(bytes);
			Object o = kryo.readClassAndObject(in);
			log("[%s] Read in [%s]", desc, o);
		} catch (Exception ex) {
			log("Fail: [%s]", ex);		
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
	
}
