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
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: ArrayTypeSerializer</p>
 * <p>Description: Serializer for {@link ArrayType} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.ArrayTypeSerializer</code></p>
 */

@SuppressWarnings("rawtypes")
public class ArrayTypeSerializer extends BaseSerializer<ArrayType> {

	@Override
	protected void doWrite(Kryo kryo, Output output, ArrayType at) {		
		if(at.isPrimitiveArray()) {
			output.writeByte(0);
			 
		} else {
			output.writeByte(1);
			output.writeInt(at.getDimension());
		}
		kryo.writeClassAndObject(output, at.getElementOpenType());		
	}

	@Override
	protected ArrayType doRead(Kryo kryo, Input input, Class<ArrayType> type) {
		boolean primitive = input.readByte()==0;
		int dimension = -1;
		OpenType otype = null;
		if(!primitive) {
			dimension = input.readInt();
		}
		otype = (OpenType)kryo.readClassAndObject(input);
		if(primitive) {
			try {
				return new ArrayType((SimpleType)otype, true);
			} catch (OpenDataException e) {
				throw new RuntimeException("OpenDataException when reading primitive ArrayType", e);
			}
		} else {
			try {
				return new ArrayType(dimension, otype);
			} catch (OpenDataException e) {
				throw new RuntimeException("OpenDataException when reading primitive ArrayType", e);
			}			
		}
	}

}
