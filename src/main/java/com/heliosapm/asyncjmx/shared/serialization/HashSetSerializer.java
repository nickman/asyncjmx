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
package com.heliosapm.asyncjmx.shared.serialization;

import java.util.HashSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: HashSetSerializer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.HashSetSerializer</code></p>
 */

public class HashSetSerializer extends Serializer<HashSet> {

	/**
	 * {@inheritDoc}
	 * @see com.esotericsoftware.kryo.Serializer#write(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Output, java.lang.Object)
	 */
	@Override
	public void write(Kryo kryo, Output output, HashSet object) {
		if(object.isEmpty()) {
			output.writeInt(0);
		} else {
			output.writeInt(object.size());
			for(Object o: object) {
				kryo.writeClassAndObject(output, o);
			}
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see com.esotericsoftware.kryo.Serializer#read(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
	 */
	@Override
	public HashSet read(Kryo kryo, Input input, Class<HashSet> type) {
		try {
			int itemCount = input.readInt();
			HashSet set = new HashSet(itemCount);
			for(int i = 0; i < itemCount; i++) {
				Object o = kryo.readClassAndObject(input);
				System.out.println("Object #" + i + " type:[" + o.getClass().getName() + "] value:[" + o + "]");
				set.add(o);
			}
			return set;
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}

}
