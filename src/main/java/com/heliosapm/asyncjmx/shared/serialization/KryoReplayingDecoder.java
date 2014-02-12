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
package com.heliosapm.asyncjmx.shared.serialization;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.server.JMXOpInvocation;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: KryoReplayingDecoder</p>
 * <p>Description: Extension of  {@link ReplayingDecoder} to fine-tune kryo object deserializaton.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.KryoReplayingDecoder</code></p>
 * @param <T> The enum defining the replay steps for this decoder
 */

public abstract class KryoReplayingDecoder<T extends Enum<T>> extends ReplayingDecoder<T> {
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());

	
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
	
	
	
	/**
	 * Creates a new KryoReplayingDecoder
	 */
	public KryoReplayingDecoder() {
		super();
	}



	/**
	 * Creates a new KryoReplayingDecoder
	 * @param unfold
	 */
	public KryoReplayingDecoder(boolean unfold) {
		super(unfold);
	}



	/**
	 * Creates a new KryoReplayingDecoder
	 * @param initialState
	 * @param unfold
	 */
	public KryoReplayingDecoder(T initialState, boolean unfold) {
		super(initialState, unfold);
	}



	/**
	 * Creates a new KryoReplayingDecoder
	 * @param initialState
	 */
	public KryoReplayingDecoder(T initialState) {
		super(initialState);
	}

	/**
	 * Reads a kryo deserialized object from the passed channel buffer in the scope of this replaying decoder
	 * @param channel The current channel
	 * @param buffer The replaying decoder buffer
	 * @param responseSize The minimum response size required to kryo-deserialize
	 * @return the read object
	 */
	protected Object kryoRead(Channel channel, ChannelBuffer buffer, int responseSize) {
		UnsafeInput input = null;
		ChannelBufferInputStream cbis = null;
		try {
			buffer.markReaderIndex();
			//log.info("Readable Bytes:%s", super.actualReadableBytes());
			
			if(super.actualReadableBytes()<responseSize) {
				log.info("Need [%s] bytes but only [%s] available. Replaying....", responseSize, super.actualReadableBytes());
				throw REPLAY_ERROR;
			}
			ChannelBuffer cb = buffer.readBytes(super.actualReadableBytes());
			cbis = new ChannelBufferInputStream(cb);
			input = new UnsafeInput(cbis);			
			log.info("Kryo decoding...");
			input.mark(1024);
			Registration reg = KryoFactory.getInstance().getKryo(channel).readClass(input);
			log.info("================Reg: %s", reg==null ? "<null>" : reg.toString());
			Object obj = KryoFactory.getInstance().getKryo(channel).readObject(input, reg.getType());
			log.info("Kryo Decoded [%s]", obj.getClass().getName());
			return obj;
		} catch (Exception ex) {
			log.warn("Kryo decode failed: [%s]", ex.toString(), ex);
			buffer.resetReaderIndex();
			throw REPLAY_ERROR;
		} finally {
			if(input!=null) try { input.close(); } catch (Exception x) { /* No Op */ }
			if(cbis!=null) try { cbis.close(); } catch (Exception x) { /* No Op */ }
		}
	}

	/**
	 * Reads a kryo deserialized object from the passed channel buffer in the scope of this replaying decoder
	 * @param channel The current channel
	 * @param buffer The replaying decoder buffer
	 * @param serializer The kryo serializer designated by the JMXOpCode
	 * @param type The expected response type
	 * @param responseSize The minimum response size required to kryo-deserialize
	 * @return the read object
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object kryoRead(Channel channel, ChannelBuffer buffer, Serializer<?> serializer, Class type, int responseSize) {
		UnsafeInput input = null;
		ChannelBufferInputStream cbis = null;
		try {
			buffer.markReaderIndex();
			ChannelBuffer cb = null;
			if(responseSize > 0) {
				if(super.actualReadableBytes()<responseSize) {
					log.info("Need [%s] bytes but only [%s] available. Replaying....", responseSize, super.actualReadableBytes());
					throw REPLAY_ERROR;
				}				
			}
			cb = buffer.readBytes(super.actualReadableBytes());
			cbis = new ChannelBufferInputStream(cb);
			input = new UnsafeInput(cbis);
			if(type==null) {
				type = serializer.getClass().getDeclaredMethod("read", Kryo.class, Input.class, Class.class).getReturnType();
			}
			Object obj = serializer.read(KryoFactory.getInstance().getKryo(channel), input, type);
			log.info("Kryo Decoded [%s]", obj.getClass().getName());
			return obj;
		} catch (Throwable ex) {
			log.warn("Kryo decode failed: [%s]", ex.toString(), ex);
			buffer.resetReaderIndex();
			throw REPLAY_ERROR;
		} finally {
			if(input!=null) try { input.close(); } catch (Exception x) { /* No Op */ }
			if(cbis!=null) try { cbis.close(); } catch (Exception x) { /* No Op */ }
		}		
	}
	
	/**
	 * Kryo reads one or more objects we don't know anything about
	 * @param channel The channel we're reading from
	 * @param buffer The current buffer

	 */
	protected void kryoRead(Channel channel, ChannelBuffer buffer, JMXOpInvocation inv) {
		int TOREAD = inv.getPendingArgCount();
		if(TOREAD<1) return;
		UnsafeInput input = null;
		ChannelBufferInputStream cbis = null;
		try {
			buffer.markReaderIndex();
			int readableBytes = super.actualReadableBytes();
			log.info("Need to read [%s] args with [%s] available bytes", TOREAD, readableBytes);
			ChannelBuffer cb = buffer.readBytes(readableBytes);	
			cbis = new ChannelBufferInputStream(cb);
			input = new UnsafeInput(cbis);			
			for(int i = 0; i < TOREAD; i++) {
				Registration reg = KryoFactory.getInstance().getKryo(channel).readClass(input);
				log.info("================Reg: %s", reg==null ? "<null>" : reg.toString());				
				Object obj = KryoFactory.getInstance().getKryo(channel).readObject(input, reg.getType());
				buffer.markReaderIndex();
				log.info("Kryo Decoded [%s]", obj.getClass().getName());
				inv.appendArg(obj);
			}			
		} catch (Exception ex) {
			log.warn("Kryo decode failed: [%s]", ex.toString(), ex);
			buffer.resetReaderIndex();
			throw REPLAY_ERROR;
		} finally {
			if(input!=null) try { input.close(); } catch (Exception x) { /* No Op */ }
			if(cbis!=null) try { cbis.close(); } catch (Exception x) { /* No Op */ }
		}				
	}

}
