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

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

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
			itemTypes[i] =  (OpenType<?>)kryo.readClassAndObject(input); //otSerialzier.read(kryo, input, OpenType.class);
		}
		try {
			return new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
