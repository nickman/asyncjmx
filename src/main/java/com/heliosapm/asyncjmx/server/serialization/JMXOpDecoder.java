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
package com.heliosapm.asyncjmx.server.serialization;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.client.JMXOpResponse;
import com.heliosapm.asyncjmx.server.JMXOpInvocation;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.serialization.KryoReplayingDecoder;

/**
 * <p>Title: JMXOpDecoder</p>
 * <p>Description: The JMX Op Decoder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.serialization.JMXOpDecoder</code></p>
 */

public class JMXOpDecoder extends  KryoReplayingDecoder<JMXOpDecodeStep> {

	/**
	 * Creates a new JMXOpDecoder
	 */
	public JMXOpDecoder() {
		super(JMXOpDecodeStep.OPCODE);
	}
	
	
	/** The op invocation being decoded */
	protected JMXOpInvocation opInvocation = null;
	/** The chanel buffer's input stream */
	protected ChannelBufferInputStream is = null;
	/** The kryo input associated to the channel buffer input stream */
	protected UnsafeInput input = null;
	
	/** The caller sent response size */
	protected int responseSize = -1;
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, JMXOpDecodeStep state) throws Exception {
//		header.writeByte(opCode);  // 1  byte for op code
//		header.writeInt((Integer)payload[1]); // 4 bytes for request id
//		final int sizeOffset = header.writerIndex();
//		header.writeInt(0);	// 4 byte for args indicator
		
		switch(state) {
			case OPCODE:				
				opInvocation = new JMXOpInvocation(buffer.readByte());
				log.info("Read JMXOp [%s]", opInvocation.opCode);
				checkpoint(JMXOpDecodeStep.REQUESTID);
			//$FALL-THROUGH$
			case REQUESTID:
				opInvocation.setRequestId(buffer.readInt());
				log.info("Read RequestID [%s]", opInvocation.getRequestId());
				if(opInvocation.hasMoreArgs()) {
					checkpoint(JMXOpDecodeStep.ARGS);
				} else {
					checkpoint(JMXOpDecodeStep.OPCODE);
					return opInvocation;
				}
			//$FALL-THROUGH$
//			case ARGBYTESIZE:
//				responseSize = buffer.readInt();
//				log.info("Read Arg Size [%s]", responseSize);
//				if(responseSize==0) {
//					checkpoint(JMXOpDecodeStep.OPCODE);
//					return opInvocation;
//				}
//				checkpoint(JMXOpDecodeStep.ARGS);
//			//$FALL-THROUGH$
			case ARGS:
				checkpoint(JMXOpDecodeStep.ARGS);
				kryoRead(channel, buffer, opInvocation);
				checkpoint(JMXOpDecodeStep.OPCODE);
				return opInvocation;
			default:
				throw new Error("Shouldn't reach here.");
		
		}
	}


}
