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

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.serialization.NullResult.NullResultSerializer;

/**
 * <p>Title: NullResult</p>
 * <p>Description: Represents a null return value (not a void)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.NullResult</code></p>
 */
@DefaultSerializer(NullResultSerializer.class)
public class NullResult implements PlaceHolder {
	/** The single static NullRsult instance */
	public static final NullResult Instance = new NullResult();
	
	public static void main(String[] args) {
		log("KryoNullResult Test");
		try {
			Kryo k = KryoFactory.getInstance().newKryo();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			UnsafeOutput out = new UnsafeOutput(baos);
			log("Writing Out");
			k.writeClassAndObject(out, Instance);
			//k.writeObject(out, Instance);
			out.flush();
			baos.flush();
			log("Write Complete");
			byte[] bytes = baos.toByteArray();
			log("Ser Size: %s", bytes.length);
			log("Reading in");
			UnsafeInput in = new UnsafeInput(bytes);
			Object o = k.readClassAndObject(in);
			log("Read in [%s]", o);
			
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
	
	private NullResult() {}
	/**
	 * <p>Title: NullResultSerializer</p>
	 * <p>Description: The default serializer for a NullResult</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.shared.NullResult.NullResultSerializer</code></p>
	 */
	public static class NullResultSerializer extends Serializer<NullResult> {

		@Override
		public void write(Kryo kryo, Output output, NullResult object) {
		}

		@Override
		public NullResult read(Kryo kryo, Input input, Class<NullResult> type) {
			return Instance;
		}
		
	}
}

