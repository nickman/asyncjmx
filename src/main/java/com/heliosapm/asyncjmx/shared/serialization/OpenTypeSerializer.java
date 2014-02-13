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

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: OpenTypeSerializer</p>
 * <p>Description: Serializer for {@link OpenType} instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.OpenTypeSerializer</code></p>
 */

public class OpenTypeSerializer extends Serializer<OpenType> {
	
	protected static final ArrayTypeSerializer arrSerializer = new ArrayTypeSerializer();
	protected static final SimpleTypeSerializer stSerializer = new SimpleTypeSerializer();
	protected static final TabularTypeSerializer tabSerializer = new TabularTypeSerializer();
	protected static final CompositeTypeSerializer ctSerializer = new CompositeTypeSerializer();
	protected static final OpenTypeSerializer otSerializer = new OpenTypeSerializer();
	
	
	@Override
	public void write(Kryo kryo, Output output, OpenType ot) {
		if(ot instanceof ArrayType) {
			output.writeByte(0);
			arrSerializer.write(kryo, output, (ArrayType)ot);
		} else if(ot instanceof SimpleType) {
			output.writeByte(1);
			stSerializer.write(kryo, output, (SimpleType)ot);
		} else if(ot instanceof CompositeType) {
			output.writeByte(2);
			ctSerializer.write(kryo, output, (CompositeType)ot);
		} else if(ot instanceof TabularType) {
			output.writeByte(3);
			tabSerializer.write(kryo, output, (TabularType)ot);
		}
		else throw new RuntimeException("Unrecognized OpenType [" + ot.getClass().getName() + "]");
	}

	@Override
	public OpenType read(Kryo kryo, Input input, Class<OpenType> type) {
		byte t = input.readByte();
		switch(t) {
		case 0:
			return arrSerializer.read(kryo, input, ArrayType.class);
		case 1:
			return stSerializer.read(kryo, input, SimpleType.class);
		case 2:
			return tabSerializer.read(kryo, input, TabularType.class);
		case 3:
			return ctSerializer.read(kryo, input, CompositeType.class);
		default:
			throw new RuntimeException("Unrecognized OpenType [" + type.getName() + "]");
		}		
	}

}
