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

import javax.management.Attribute;
import javax.management.AttributeList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: AttributeListSerializer</p>
 * <p>Description: A serialzer for {@link AttributeList} instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.AttributeListSerializer</code></p>
 */

public class AttributeListSerializer extends BaseSerializer<AttributeList> {
	
	@Override
	protected void doWrite(Kryo kryo, Output output, AttributeList object) {
		output.writeInt(object.size());
		log.info("AttributeList Size:%s", object.size());
		for(Attribute attr: object.asList()) {
			kryo.writeClassAndObject(output, attr);
		}		
	}

	@Override
	protected AttributeList doRead(Kryo kryo, Input input, Class<AttributeList> type) {
		try {
			int size = input.readInt();
			log.info("AttributeList Size:%s", size);
			AttributeList attrs = new AttributeList(size);
			for(int i = 0; i < size; i++) {				
				attrs.add((Attribute)kryo.readClassAndObject(input));
			}
			return attrs;
		} catch (Exception ex) {
			log.error("Failed to read attr", ex);
			throw new RuntimeException(ex);
		}
	}

}
