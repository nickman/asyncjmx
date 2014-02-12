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
		
		switch(state) {
			case OPCODE:				
				opInvocation = new JMXOpInvocation(buffer.readByte());
				checkpoint(JMXOpDecodeStep.REQUESTID);
			//$FALL-THROUGH$
			case REQUESTID:
				opInvocation.setRequestId(buffer.readInt());
				checkpoint(JMXOpDecodeStep.ARGBYTESIZE);
			//$FALL-THROUGH$
			case ARGBYTESIZE:
				responseSize = buffer.readInt();
				if(responseSize==0) {
					return opInvocation;
				}
				checkpoint(JMXOpDecodeStep.ARGS);
			//$FALL-THROUGH$
			case ARGS:
				Object[] response = (Object[])kryoRead(channel, buffer, responseSize);
				for(int i = 0; i < response.length; i++) {
					opInvocation.appendArg(response[i]);
				}				
				return opInvocation;
			default:
				throw new Error("Shouldn't reach here.");
		
		}
	}


}
