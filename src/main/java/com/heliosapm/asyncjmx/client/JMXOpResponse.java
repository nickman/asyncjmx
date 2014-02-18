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

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.serialization.BaseSerializer;
import com.heliosapm.asyncjmx.shared.serialization.HistogramKeyProvider;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: JMXOpResponse</p>
 * <p>Description: Wrapper for JMX Op responses</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXOpResponse</code></p>
 */
@DefaultSerializer(JMXOpResponse.JMXOpResponseSerializer.class)
public class JMXOpResponse implements HistogramKeyProvider<Class<?>> {
	/** The JMXOpCode of the original request */
	protected JMXOpCode opCode;	
	/** The request id of the original request */
	protected int requestId;
	/** The JMX Op response */
	protected Object response;
	
	/**
	 * Creates a new JMXOpResponse
	 * @param opCode The JMXOpCode of the original request
	 * @param requestId The request id of the original request
	 * @param response The op response
	 */
	public JMXOpResponse(JMXOpCode opCode, int requestId, Object response) {
		super();
		this.opCode = opCode;
		this.requestId = requestId;
		this.response = response;
	}
	
	/**
	 * Creates a new JMXOpResponse
	 */
	private JMXOpResponse() {
		
	}

	/**
	 * Returns the JMXOpCode of the original request
	 * @return the opCode
	 */
	public JMXOpCode getOpCode() {
		return opCode;
	}

	/**
	 * Returns the request id of the original request
	 * @return the requestId
	 */
	public int getRequestId() {
		return requestId;
	}

	/**
	 * Returns the JMX Op response 
	 * @return the response
	 */
	public Object getResponse() {
		return response;
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.asyncjmx.shared.serialization.HistogramKeyProvider#getHistogramKey()
	 */
	@Override
	public Class<?> getHistogramKey() {
		return response==null ? null : response.getClass();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.asyncjmx.shared.serialization.HistogramKeyProvider#getVoidHistogramSize()
	 */
	@Override
	public int getVoidHistogramSize() {
		return 10;
	}	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((opCode == null) ? 0 : opCode.hashCode());
		result = prime * result + requestId;
		result = prime * result
				+ ((response == null) ? 0 : response.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JMXOpResponse other = (JMXOpResponse) obj;
		if (opCode != other.opCode)
			return false;
		if (requestId != other.requestId)
			return false;
		if (response == null) {
			if (other.response != null)
				return false;
		} else if (!response.equals(other.response))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMXOpResponse [");
		if (opCode != null) {
			builder.append("opCode=");
			builder.append(opCode);
			builder.append(", ");
		}
		builder.append("requestId=");
		builder.append(requestId);
		builder.append(", ");
		if (response != null) {
			builder.append("response=");
			builder.append(response);
		}
		builder.append("]");
		return builder.toString();
	}
	
	
	/**
	 * <p>Title: JMXOpResponseSerializer</p>
	 * <p>Description: The Kryo serializer for {@link JMXOpResponse} instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.client.JMXOpResponse.JMXOpResponseSerializer</code></p>
	 */
	public static class JMXOpResponseSerializer extends BaseSerializer<JMXOpResponse> {
		/** Static class logger */
		protected static final JMXLogger log = JMXLogger.getLogger(JMXOpResponseSerializer.class);
		
		/** A replay error instance */
		protected static final Error REPLAY_ERROR;
		
		static {
			try {
				Class<Error> clazz = (Class<Error>) Class.forName("org.jboss.netty.handler.codec.replay.ReplayError");			
				REPLAY_ERROR = (Error)UnsafeAdapter.allocateInstance(clazz);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to initialize replay error", ex);
			}
		}
		
		/** The parent replaying decoder */
		private final JMXResponseDecoder replay;
		
		/**
		 * Creates a new JMXResponseDecoder with a reference to the currently running replaying decoder
		 * @param replay The replaying decoder to callback on as a step is complete
		 */
		public JMXOpResponseSerializer(JMXResponseDecoder replay) {
			super();
			this.replay = replay;
		}
		
		/**
		 * Creates a new JMXOpResponseSerializer 
		 */
		public JMXOpResponseSerializer() {
			this.replay = null;
		}

		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.asyncjmx.shared.serialization.BaseSerializer#doWrite(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Output, java.lang.Object)
		 */
		@Override
		protected void doWrite(Kryo kryo, Output output, JMXOpResponse jmxOpResp) {
			output.writeByte(jmxOpResp.opCode.opCode);
			output.writeInt(jmxOpResp.requestId);
			int pre = output.position();
			kryo.writeClassAndObject(output, jmxOpResp.response);
			int objSize = output.position()-pre;
			log.info("---->OBJ SIZE:[%s]", objSize);			
		}
		
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.asyncjmx.shared.serialization.BaseSerializer#doRead(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
		 */
		@Override
		protected JMXOpResponse doRead(Kryo kryo, Input input, Class<JMXOpResponse> type) {
			if(replay==null) {
				return doReadWithNoReplay(kryo, input, type);
			}
			return doReadWithReplay(kryo, input, type);
		}
		
		/**
		 * Reads a JMXOpResponse without a parent replaying decoder
		 * @param kryo The kryo instance
		 * @param input THe kryo input stream
		 * @param type The decoding type
		 * @return the read JMXOpResponse
		 */
		protected JMXOpResponse doReadWithNoReplay(Kryo kryo, Input input, Class<JMXOpResponse> type) {
			JMXOpResponse jmxOpResp = new JMXOpResponse();
			jmxOpResp.opCode = JMXOpCode.decode(input.readByte());
			jmxOpResp.requestId = input.readInt();
			jmxOpResp.response = kryo.readClassAndObject(input);
			return jmxOpResp;
		}
		
		/**
		 * Reads a JMXOpResponse with the parent replaying decoder
		 * @param kryo The kryo instance
		 * @param input THe kryo input stream
		 * @param type The decoding type
		 * @return the read JMXOpResponse
		 */
		protected JMXOpResponse doReadWithReplay(Kryo kryo, Input input, Class<JMXOpResponse> type) {
			JMXOpResponse state = (JMXOpResponse)kryo.getContext().get("JMXOpResponse");
			if(state==null) {
				state = new JMXOpResponse();
				kryo.getContext().put("JMXOpResponse", state);
			}
			ChannelBuffer buff = replay.getBuff();
			//buff.markReaderIndex();
			try {				
				log.info("Readable Bytes: [%s], Index: [%s]", buff.readableBytes(), buff.readerIndex());
				switch(replay.getState()) {
					case OPCODE:					
						byte opCode = input.readByte();
						state.opCode = JMXOpCode.decode(opCode);
						replay.checkpoint(JMXResponseDecodeStep.REQUESTID);
						//$FALL-THROUGH$
					case REQUESTID:
						state.requestId = input.readInt();
						replay.checkpoint(JMXResponseDecodeStep.RESPONSE);
						//$FALL-THROUGH$						
					case RESPONSE:
						log.info("Pre-ReadResponse: Avail:[%s], Index:[%s] InputPos:[%s]", buff.readableBytes(), buff.readerIndex(), input.position());
						state.response = kryo.readClassAndObject(input);
						replay.checkpoint(JMXResponseDecodeStep.BYTESIZE);
						kryo.getContext().remove("JMXOpResponse");
						//$FALL-THROUGH$
						return state;
					default:
						throw new Error("Should not reach here");
				}
			} catch (Throwable ex) {
				int ri = buff.readerIndex(), rb = buff.readableBytes();				
				replay.reset();
				log.info("Buff RESET, Pre:[%s][%s], Post:[%s][%s]", ri, rb, buff.readerIndex(), buff.readableBytes());
				throw REPLAY_ERROR;
			}
		}		
	}
	
//	protected JMXOpResponse doReadWithReplay(Kryo kryo, Input input, Class<JMXOpResponse> type) {
//		JMXOpResponse state = (JMXOpResponse)kryo.getContext().get("JMXOpResponse");
//		if(state==null) {
//			state = new JMXOpResponse();
//			kryo.getContext().put("JMXOpResponse", state);
//		}
//		int readerIndex = 0;
//		try {
//			readerIndex = replay.getBuff().readerIndex(); 
//			log.info("Readable Bytes: [%s], Index: [%s]", replay.getBuff().readableBytes(), readerIndex);
//			switch(replay.getState()) {
//				case REQUESTID:
//					readerIndex = replay.getBuff().readerIndex(); 						
//					log.info("Buff reset to [%s], Readable:[%s], Buff:[%s]", readerIndex, replay.getBuff().readableBytes(), replay.getBuff());						
//					state.response = input.readInt();
//					replay.checkpoint(JMXResponseDecodeStep.OPCODE);
//					//$FALL-THROUGH$
//				case OPCODE:
//					readerIndex = replay.getBuff().readerIndex(); 						
//					log.info("Buff reset to [%s], Readable:[%s], Buff:[%s]", readerIndex, replay.getBuff().readableBytes(), replay.getBuff());						
//					state.opCode = JMXOpCode.decode(input.readByte());
//					replay.checkpoint(JMXResponseDecodeStep.RESPONSE);
//					//$FALL-THROUGH$						
//				case RESPONSE:
//					readerIndex = replay.getBuff().readerIndex(); 						
//					log.info("Buff reset to [%s], Readable:[%s], Buff:[%s]", readerIndex, replay.getBuff().readableBytes(), replay.getBuff());						
//					state.response = kryo.readClassAndObject(input);
//					replay.checkpoint(JMXResponseDecodeStep.BYTESIZE);
//					kryo.getContext().remove("JMXOpResponse");
//					//$FALL-THROUGH$
//					return state;
//				default:
//					throw new Error("Should not reach here");
//			}
//		} catch (Throwable ex) {				
//			replay.getBuff().readerIndex(readerIndex);
//			log.warn("Kryo decode failed:[%s], Decoding Buffer Reset:[%s](%s)", ex.toString(), replay.getBuff().readableBytes(), replay.getBuff().toString());
//			throw REPLAY_ERROR;
//		}
//	}		
//}
	

}
