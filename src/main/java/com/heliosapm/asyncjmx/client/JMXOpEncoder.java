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
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
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

	/** A histogram of JMXOpCode serialization sizes */
	protected Map<JMXOpCode, ConcurrentLongSlidingWindow> opSizeHistory = new ConcurrentHashMap<JMXOpCode, ConcurrentLongSlidingWindow>();
	/** The default sampling size for JMXOp serialization sizes histograms */
	public static final int DEFAULT_HISTOGRAM_SIZE = 128;
	/** The default percentile to calculate estimated sizes with */
	public static final int DEFAULT_HISTOGRAM_PERCENTILE = 90;
	/** The default byte size estimate if the histogram has less than 2 samples */
	public static final int DEFAULT_SIZE = 1024;
	
	/**
	 * Acquires the histogram for the passed JMXOpCode
	 * @param opCode The JMXOpCode to get the histogram for
	 * @return the histogram
	 */
	protected ConcurrentLongSlidingWindow getJMXOpHistogram(JMXOpCode opCode) {
		ConcurrentLongSlidingWindow csw = opSizeHistory.get(opCode);
		if(csw==null) {
			synchronized(opSizeHistory) {
				csw = opSizeHistory.get(opCode);
				if(csw==null) {
					csw = new ConcurrentLongSlidingWindow(DEFAULT_HISTOGRAM_SIZE);
					opSizeHistory.put(opCode, csw);
				}
			}
		}
		return csw;
	}
	
	
	/**
	 * Adds a new sampling to the JMXOpCode histogram
	 * @param op The jmx op for which a sample was taken
	 * @param size The sampled size in bytes 
	 */
	protected void sample(JMXOp op, int size) {
		getJMXOpHistogram(op.getJmxOpCode()).insert(size);
	}
	
	/**
	 * Estimates the size of the payload
	 * @param op The JMX Op being serialized
	 * @return the estimated size in bytes
	 */
	protected int estimateSize(JMXOp op) {
		ConcurrentLongSlidingWindow csw = getJMXOpHistogram(op.getJmxOpCode());
		if(csw.size() < 2) return DEFAULT_SIZE;
		return (int)csw.percentile(DEFAULT_HISTOGRAM_PERCENTILE);
	}
	


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
				ChannelBuffer body = ChannelBuffers.dynamicBuffer(estimateSize(jmxOp), bufferFactory);
				body.setInt(0, 0);
				out = new ChannelBufferOutputStream(body);			
				Kryo kryo = KryoFactory.getInstance().getKryo(channel);			
				kout = new UnsafeOutput(out);
				kryo.writeClassAndObject(kout, jmxOp);
				kout.flush();
				out.flush();
				body.setInt(0, body.writerIndex());
				log.info("Sending Encoded Op with [%s] bytes.  Op: %s", body.writerIndex(), jmxOp);
				sample(jmxOp, body.writerIndex());
				return body;
			} finally {
				if(kout!=null) try { kout.close(); } catch (Exception x) { /* No Op */ }
				if(out!=null) try { out.close(); } catch (Exception x) { /* No Op */ }
			}
		}
		return null;
	}
}
