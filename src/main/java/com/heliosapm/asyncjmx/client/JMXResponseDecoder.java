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
package com.heliosapm.asyncjmx.client;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.server.serialization.JMXOpDecodeStep;
import com.heliosapm.asyncjmx.shared.JMXOp;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.JMXResponseType;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.util.ConfigurationHelper;

/**
 * <p>Title: JMXResponseDecoder</p>
 * <p>Description: Netty decoder for responses and callbacks from the JMX server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXResponseDecoder</code></p>
 */

public class JMXResponseDecoder extends ReplayingDecoder<JMXResponseDecodeStep> {
	/** The channel buffer factory for decoding JMXOp buffers */
	protected static final ChannelBufferFactory bufferFactory;
	
	static {
		boolean directBuffer = ConfigurationHelper.getBooleanSystemThenEnvProperty("com.heliosapm.asyncjmx.server.buffers.direct", false);
		if(directBuffer) {
			bufferFactory = new DirectChannelBufferFactory();
		} else {
			bufferFactory = new HeapChannelBufferFactory();
		}
	}
	
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());
	
	
	/** The name of the response handler in the pipeline */
	public static final String RESPONSE_HANDLER_NAME = "responseHandler";

	
	/** The JMXOpResponse kryo replaying deserializer */
	protected JMXOpResponse.JMXOpResponseSerializer ser = new JMXOpResponse.JMXOpResponseSerializer(this);
	/** The kryo input */
	protected Input input = null;
	/** The payload size allocated channel buffer */
	protected ChannelBuffer buff = null;
	/** The payload size allocated channel buffer input stream */
	protected ChannelBufferInputStream niffIs = null;
	
	/** The payload size of the currently processing decode */
	int payloadSize = -1;
	
	/**
	 * Creates a new JMXResponseDecoder
	 */
	public JMXResponseDecoder() {
		super(JMXResponseDecodeStep.BYTESIZE);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#getState()
	 */
	@Override
	public JMXResponseDecodeStep getState() {
		return super.getState();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#checkpoint(java.lang.Enum)
	 */
	@Override
	public void checkpoint(JMXResponseDecodeStep state) {
		super.checkpoint(state);
		log.info("------->KInput:[%s]", input.position());		
	}
	
	/**
	 * Resets the managed buffer to the offset specified by the last checkpoint 
	 */
	public void reset() {		
		buff.readerIndex(getState().offset);
		log.info("Set ManagedBuffer Reader Index to [%s]:[%s]", getState().name(), getState().offset);
	}
	
	/**
	 * Returns the replaying decoder's managed buffer
	 * @return the replaying decoder's managed buffer
	 */
	public ChannelBuffer getBuff() {
		return buff;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#internalBuffer()
	 */
	@Override
	public ChannelBuffer internalBuffer() {
		return super.internalBuffer();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, JMXResponseDecodeStep state) throws Exception {
		log.info("Replay Buffer Readable Bytes: [%s]", super.actualReadableBytes());
		if(state==JMXResponseDecodeStep.BYTESIZE) {
			payloadSize = buffer.readInt()-4;
			log.info("JMXOpResponse Decode Starting. Remaining Payload Size: [%s] bytes", payloadSize);					
			buff = bufferFactory.getBuffer(payloadSize);			
			checkpoint(JMXResponseDecodeStep.OPCODE);
		}
		
		int readableBytesAvailable = super.actualReadableBytes();
//		log.info("PRE: Payload Size: [%s], Replay Bytes Available: [%s]", payloadSize, readableBytesAvailable);
		log.info("PRE: Replay Readable:[%s], ManagedBuff Reader/Writer Index:[%s]/[%s], Optimal Read:[%s]", readableBytesAvailable, buff.readerIndex(), buff.writerIndex(), payloadSize-buff.writerIndex());
		int toRead = Math.min(readableBytesAvailable, payloadSize-buff.writerIndex());
		log.info("Reading [%s] bytes from REPLAY to MANAGED:[%s]", toRead, buff.writerIndex());
		buff.writeBytes(buffer.readBytes(toRead));
		niffIs = new ChannelBufferInputStream(buff);
		input = new UnsafeInput(niffIs);
		log.info("POST: Buff Bytes Available:[%s], Reader Index:[%s]",buff.readableBytes(), buff.readerIndex());		
		return ser.read(KryoFactory.getInstance().getKryo(channel), input, JMXOpResponse.class);		
	}

}
