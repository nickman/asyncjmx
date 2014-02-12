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
package com.heliosapm.asyncjmx.server.serialization;

import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.KryoFactory;

/**
 * <p>Title: JMXResponseEncoder</p>
 * <p>Description: Encodes the downstream JMX invocation response</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.serialization.JMXResponseEncoder</code></p>
 */

public class JMXResponseEncoder extends OneToOneEncoder {
	/** The buffer factory for sending op invocations */
	protected ChannelBufferFactory bufferFactory = new HeapChannelBufferFactory();
	
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass().getName());

	
	/**
	 * Estimates the size of the payload
	 * @param payload The payload to extimate the size of
	 * @return the estimated size in bytes
	 */
	protected int estimateSize(Object payload) {
		return 1024;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if(!(msg instanceof Object[])) return msg;		
		log.info("Serializing response");
//		{JMXResponseType.JMX_RESPONSE.opCode, op.opCode, op.requestId, response}
		Object[] resp = (Object[])msg;
		 
		ChannelBuffer header = ChannelBuffers.buffer(10);
		
		header.writeByte((Byte)resp[0]);  // response type	1
		header.writeByte(((JMXOpCode)resp[1]).opCode);  // jmx op 1
		header.writeInt((Integer)resp[2]); // request if 4
		final int sizeOffset = header.writerIndex();
		header.writeInt(-1);				// the size of the payload placeholder 4
		ChannelBuffer body = ChannelBuffers.dynamicBuffer(estimateSize(resp[3]), bufferFactory);
		Kryo kryo = KryoFactory.getInstance().getKryo(channel);
		ChannelBufferOutputStream cos = new ChannelBufferOutputStream(body);
		UnsafeOutput out = new UnsafeOutput(cos);
		try {
			log.info("Writing out [" + resp[3] + "]");
			kryo.writeClassAndObject(out, resp[3]);
			out.flush();
			cos.flush();
		} catch (Exception ex) {
			log.warning("Write failed. Writing NonSer");
			body = KryoFactory.getInstance().getNonSerializable(kryo, msg);
		}
		header.setInt(sizeOffset, body.writerIndex());
		log.info("Write out complete");		
		//ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), buf, channel.getRemoteAddress()));
		return ChannelBuffers.wrappedBuffer(header, body);
	}

}
