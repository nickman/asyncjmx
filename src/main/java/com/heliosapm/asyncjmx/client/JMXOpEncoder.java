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

import java.util.Arrays;

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
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.serialization.NullResult;

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
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());
	
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
			Object[]  payload = (Object[])msg;     // {opCode, rId, args}
			Object[]  args = (Object[])payload[2];
			byte opCode = (Byte)payload[0];
			ChannelBuffer header = ChannelBuffers.buffer(9);			
			header.writeByte(opCode);  // 1  byte for op code
			header.writeInt((Integer)payload[1]); // 4 bytes for request id
			final int sizeOffset = header.writerIndex();
			if(args.length==0) {
				log.info("Sending Zero Arg Encoded Op");
				return header;
			}
			Output kout = null;
			ChannelBufferOutputStream out = null;
			try {
				ChannelBuffer body = ChannelBuffers.dynamicBuffer(estimateSize(opCode, payload), bufferFactory);
				out = new ChannelBufferOutputStream(body);			
				Kryo kryo = KryoFactory.getInstance().getKryo(channel);			
				kout = new UnsafeOutput(out);
				for(Object o: args) {
					if(o==null) kryo.writeClassAndObject(kout, NullResult.Instance);
					else {
						kryo.writeClassAndObject(kout, o);
					}
				}
				//kryo.writeClassAndObject(kout, args);
				kout.flush();
				out.flush();
//				header.setInt(sizeOffset, body.writerIndex());
				log.info("Sending Encoded Op with [%s] args and [%s] bytes.  Args: %s", args.length, body.writerIndex(), Arrays.toString(args));
				return ChannelBuffers.wrappedBuffer(header, body);
			} finally {
				if(kout!=null) try { kout.close(); } catch (Exception x) { /* No Op */ }
				if(out!=null) try { out.close(); } catch (Exception x) { /* No Op */ }
			}
		}
		return null;
	}
}
