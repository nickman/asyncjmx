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

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.serialization.VoidResult.VoidResultSerializer;

/**
 * <p>Title: VoidResult</p>
 * <p>Description: Represents a void return value (not a null)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.VoidResult</code></p>
 */
@DefaultSerializer(VoidResultSerializer.class)
public class VoidResult implements PlaceHolder {
	/** The single static VoidResult instance */
	public static final VoidResult Instance = new VoidResult();

	private VoidResult() {}
	
		
	/**
	 * <p>Title: VoidResultSerializer</p>
	 * <p>Description: The default serializer for a VoidResult</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.shared.VoidResult.VoidResultSerializer</code></p>
	 */
	public static class VoidResultSerializer extends Serializer<VoidResult> {

		@Override
		public void write(Kryo kryo, Output output, VoidResult object) {
			/* No Op */
		}

		@Override
		public VoidResult read(Kryo kryo, Input input, Class<VoidResult> type) {
			return Instance;
		}
		
	}

}
