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
package com.heliosapm.asyncjmx.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.shared.KryoFactory;

/**
 * <p>Title: JMXOpDecoder</p>
 * <p>Description: The JMX Op Decoder</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.JMXOpDecoder</code></p>
 */

public class JMXOpDecoder extends  ReplayingDecoder<JMXOpDecodeStep> {

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
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, JMXOpDecodeStep state) throws Exception {
		
		switch(state) {
			case OPCODE:				
				opInvocation = new JMXOpInvocation(buffer.readByte());
				checkpoint(JMXOpDecodeStep.REQUESTID);
			//$FALL-THROUGH$
			case REQUESTID:
				opInvocation.setRequestId(buffer.readInt());
				checkpoint(JMXOpDecodeStep.ARGS);
			//$FALL-THROUGH$
			case ARGS:
				while(opInvocation.hasMoreArgs()) {
					if(buffer.readByte()==0) {
						opInvocation.appendArg(null);
					} else {
						if(is==null) {
							is = new ChannelBufferInputStream(buffer);
							input = new UnsafeInput(is);
						}
						opInvocation.appendArg(KryoFactory.getInstance().getKryo(channel).readClassAndObject(input));						
					}
				}	
				if(is!=null) {
					try { is.close(); } catch (Exception x) { /* No Op */ }
				}
				return opInvocation;
			default:
				throw new Error("Shouldn't reach here.");
		
		}
	}


}
