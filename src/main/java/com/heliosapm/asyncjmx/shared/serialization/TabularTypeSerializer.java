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
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: TabularTypeSerializer</p>
 * <p>Description: Serializer for {@link TabularType} instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.TabularTypeSerializer</code></p>
 */

public class TabularTypeSerializer extends Serializer<TabularType> {

	@Override
	public void write(Kryo kryo, Output output, TabularType tType) {
		output.writeString(tType.getTypeName());
		output.writeString(tType.getDescription());
		int indexCount = tType.getIndexNames().size();
		output.writeInt(indexCount);
		for(String s: tType.getIndexNames()) {
			output.writeString(s);
		}
//		kryo.writeObject(output, tType.getIndexNames().toArray(new String[0]));
		OpenTypeSerializer.ctSerializer.write(kryo, output, tType.getRowType());
		
	}

	@Override
	public TabularType read(Kryo kryo, Input input, Class<TabularType> type) {
		String typeName = input.readString();
		String description = input.readString();
		int indexCount = input.readInt();
		String[] indexNames = new String[indexCount];
		for(int i = 0; i < indexCount; i++) {
			indexNames[i] = input.readString();
		}
		CompositeType ct = OpenTypeSerializer.ctSerializer.read(kryo, input, CompositeType.class);
		try {
			return new TabularType(typeName, description, ct, indexNames);
		} catch (OpenDataException e) {
			throw new RuntimeException(e);
		}
	}

}
