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
package com.heliosapm.asyncjmx.client;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: JMXOpEncoder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXOpEncoder</code></p>
 */
@ChannelHandler.Sharable
public class JMXOpEncoder extends OneToOneEncoder {
	/** The buffer factory for sending op invocations */
	protected ChannelBufferFactory bufferFactory = new HeapChannelBufferFactory();
	
	/**
	 * Estimates the size of the payload
	 * @param opCode The op code of the jmx op being serialized
	 * @param payload The payload to extimate the size of
	 * @return the estimated size in bytes
	 */
	protected int estimateSize(byte opCode, Object[] payload) {
		return 1024;
	}
	


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if(msg instanceof Object[]) {
			Object[]  payload = (Object[])msg;
			Object[]  args = (Object[])payload[2];
			byte opCode = (Byte)payload[0];
			ChannelBuffer buf = ChannelBuffers.dynamicBuffer(estimateSize(opCode, payload), bufferFactory);
			buf.writeByte(opCode);
			buf.writeInt((Integer)payload[1]);
			buf.writeByte(args==null ? 0 : args.length);
			if(args.length > 0) {
				ChannelBufferOutputStream out = new ChannelBufferOutputStream(buf);
				Kryo kryo = null;
				Output kout = null;		
				try {
					for(int i = 0; i < args.length; i++) {
						if(args[i]==null) {
							out.writeByte(0);
						} else {
							out.writeByte(1);
							if(kryo==null) {
								kryo = new Kryo();
								kout = new Output(out);
							}
							kryo.writeObject(kout, args[1]);
						}
					}
				} finally {
					try { out.flush(); } catch (Exception x) {/* No Op */}
					try { out.close(); } catch (Exception x) {/* No Op */}
					if(kout!=null) {
						try { kout.flush(); } catch (Exception x) {/* No Op */}
						try { kout.close(); } catch (Exception x) {/* No Op */}
					}
				}
			}
			return buf;
		}
		return null;
	}
}
