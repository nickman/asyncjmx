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
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.UnsafeOutput;
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
		if(msg instanceof ChannelBuffer) return msg;		
		log.info("Serializing response");
		Kryo kryo = KryoFactory.getInstance().getKryo(channel);
		ChannelBuffer buf = ChannelBuffers.dynamicBuffer(estimateSize(msg), bufferFactory);
		ChannelBufferOutputStream cos = new ChannelBufferOutputStream(buf);
		UnsafeOutput out = new UnsafeOutput(cos);
		try {
			log.info("Writing out [" + msg + "]");
			kryo.writeClassAndObject(out, msg);
			out.flush();
			cos.flush();
		} catch (Exception ex) {
			log.warning("Write failed. Writing NonSer");
			buf = KryoFactory.getInstance().getNonSerializable(kryo, msg);
		}
		log.info("Write out complete");
		ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), buf, channel.getRemoteAddress()));
		return null;
	}

}
