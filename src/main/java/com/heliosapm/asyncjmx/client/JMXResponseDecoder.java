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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.JMXResponseType;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: JMXResponseDecoder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXResponseDecoder</code></p>
 */

public class JMXResponseDecoder extends ReplayingDecoder<JMXResponseDecodeStep> {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	/** The chanel buffer's input stream */
	protected ChannelBufferInputStream is = null;
	/** The JMX Response Type */
	protected JMXResponseType responseType = null;
	/** The JMX Request Op Code if the response type is an Op return */
	protected JMXOpCode opCode = null;
	/** The JMX Request original request id if the response type is an Op return */
	protected int requestId = -1;
	/** The response to an op call, or the notification object for async notifications */
	protected Object response = null;
	
	/** The name of the response handler in the pipeline */
	public static final String RESPONSE_HANDLER_NAME = "responseHandler";

	
	/**
	 * Creates a new JMXResponseDecoder
	 */
	public JMXResponseDecoder() {
		super(JMXResponseDecodeStep.TYPECODE);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer, JMXResponseDecodeStep state) throws Exception {
		switch(state) {
			case TYPECODE:
				responseType = JMXResponseType.decode(buffer.readByte());
				log.info("ResponseType:" + responseType);
				switch(responseType) {
				case CACHE_OP:
				case JMX_NOTIFICATION:
					checkpoint(JMXResponseDecodeStep.RESPONSE);
					break;
				case JMX_RESPONSE:
					checkpoint(JMXResponseDecodeStep.OPCODE);
				}
			//$FALL-THROUGH$
			case OPCODE:
				opCode = JMXOpCode.decode(buffer.readByte());
				log.info("JMXOpCode:" + opCode);
				checkpoint(JMXResponseDecodeStep.REQUESTID);
			//$FALL-THROUGH$
			case REQUESTID:
				requestId = buffer.readInt();
				log.info("RequestID:" + requestId);
				checkpoint(JMXResponseDecodeStep.RESPONSE);
			//$FALL-THROUGH$
			case RESPONSE:
				log.info("Waiting on full Response");
				try {
					buffer.markReaderIndex();
					ChannelBuffer cb = buffer.readBytes(super.actualReadableBytes());
					log.info("Read [" + cb.readableBytes() + "] bytes");
					UnsafeInput input = new UnsafeInput(new ChannelBufferInputStream(cb));
//					UnsafeInput input = new UnsafeInput(bytes);
					response = KryoFactory.getInstance().getKryo(channel).readClassAndObject(input);
					log.info("Response:" + response);
					ctx.sendUpstream(new UpstreamMessageEvent(channel, new JMXOpResponse(opCode, requestId, response), channel.getLocalAddress()));
					return null;
					
				} catch (Error err) {
					if("ReplayError".equals(err.getClass())) {
						log.info("REPLAYERROR");
						UnsafeAdapter.throwException(err);
					}											
				} catch (Exception ex) {
					buffer.resetReaderIndex();
					log.log(Level.SEVERE, "Failed to read response payload", ex);
					break;
				}
			case CACHEOP:
				break;
		
		}
		return null;
	}

}
