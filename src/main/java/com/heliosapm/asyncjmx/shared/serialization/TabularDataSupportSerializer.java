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

import java.util.Arrays;
import java.util.List;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: TabularDataSupportSerializer</p>
 * <p>Description: Serializer for {@link TabularDataSupport} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.TabularDataSupportSerializer</code></p>
 */

public class TabularDataSupportSerializer extends BaseSerializer<TabularDataSupport> {

	@Override
	protected void doWrite(Kryo kryo, Output output, TabularDataSupport tds) {
		final String ind = ind();
		log.debug("%s Writing TabularDataSupport [%s]", ind, tds.getTabularType().getDescription());
		kryo.writeClassAndObject(output, tds.getTabularType());		
		int size = tds.size();
		log.debug("%s Writing TabularDataSupport Size:[%s]", ind, size);
		output.writeInt(size);
		output.flush();
		for(Object key: tds.keySet()) {
			String[] keys = ((List<String>)key).toArray(new String[0]);
			Object value = ((CompositeDataSupport)tds.get(keys)).get("value");
			kryo.writeClassAndObject(output, keys);
			kryo.writeClassAndObject(output, value);
			log.debug("Key/Value:[%s]--[%s]", Arrays.toString(keys), value);
		}
	}

	@Override
	protected TabularDataSupport doRead(Kryo kryo, Input input, Class<TabularDataSupport> type) {
		final String ind = ind();
		try {
//			kryo.setReferences(true);			
			TabularType ttype = (TabularType)kryo.readClassAndObject(input);
			int size = input.readInt();
			log.debug("%s Read TabularDataSupport Size:[%s]", ind, size);
			TabularDataSupport tds = new TabularDataSupport(ttype, size, 0.75f);
			CompositeType ct = ttype.getRowType();
			String[] ctKeys = ct.keySet().toArray(new String[0]);
			for(int i = 0; i < size; i++) {
				String[] key = (String[])kryo.readClassAndObject(input);
				log.debug("Read Key [%s]", Arrays.toString(key));
				Object value = kryo.readClassAndObject(input);
				log.debug("Read Value [%s]", value);
				try {
					CompositeDataSupport cds = new CompositeDataSupport(ttype.getRowType(), ctKeys, new Object[] {key[0], value}); 
					tds.put(cds);
				} catch (Exception ex) {
					log.warn("Failed to process CDS for tabular type: %s", ex.toString());
				}
			}
			return tds;
		} finally {
//			kryo.setReferences(false);
		}
	}

}
