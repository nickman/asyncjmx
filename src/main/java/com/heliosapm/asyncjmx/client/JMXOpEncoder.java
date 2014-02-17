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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import com.heliosapm.asyncjmx.shared.JMXOp;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.serialization.PayloadSizeHistogram;
import com.heliosapm.asyncjmx.unsafe.collections.ConcurrentLongSlidingWindow;

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
	/** The JMX Op payload size estimator */
	protected final PayloadSizeHistogram<JMXOpCode> payloadSizeEstimator = new PayloadSizeHistogram<JMXOpCode>(); 

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if(msg instanceof JMXOp) {
			final JMXOp jmxOp = (JMXOp)msg;
			Output kout = null;
			ChannelBufferOutputStream out = null;
			try {
				ChannelBuffer body = ChannelBuffers.dynamicBuffer(payloadSizeEstimator.estimateSize(jmxOp), bufferFactory);
				body.writeInt(0);
				out = new ChannelBufferOutputStream(body);			
				Kryo kryo = KryoFactory.getInstance().getKryo(channel);			
				kout = new UnsafeOutput(out);
				kryo.writeObject(kout, jmxOp);
				kout.flush();
				int payloadSize = body.writerIndex() - 4;
				body.setInt(0, payloadSize);
				out.flush();						
				log.info("Sending Encoded Op with [%s] bytes.  Total Payload: [%s].  Op: %s", payloadSize, body.writerIndex(), jmxOp);
				payloadSizeEstimator.sample(jmxOp, body.writerIndex());
				return body;
			} catch (Exception ex) {
				log.error("JMXOpEncoder failure", ex);
				throw ex;
			} finally {
				if(kout!=null) try { kout.close(); } catch (Exception x) { /* No Op */ }
				if(out!=null) try { out.close(); } catch (Exception x) { /* No Op */ }
			}
		}
		return null;
	}
}
