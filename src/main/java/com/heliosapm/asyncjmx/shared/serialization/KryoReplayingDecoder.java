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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: KryoReplayingDecoder</p>
 * <p>Description: </p> 
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
		// TODO Auto-generated constructor stub
	}



	/**
	 * Creates a new KryoReplayingDecoder
	 * @param unfold
	 */
	public KryoReplayingDecoder(boolean unfold) {
		super(unfold);
		// TODO Auto-generated constructor stub
	}



	/**
	 * Creates a new KryoReplayingDecoder
	 * @param initialState
	 * @param unfold
	 */
	public KryoReplayingDecoder(T initialState, boolean unfold) {
		super(initialState, unfold);
		// TODO Auto-generated constructor stub
	}



	/**
	 * Creates a new KryoReplayingDecoder
	 * @param initialState
	 */
	public KryoReplayingDecoder(T initialState) {
		super(initialState);
		// TODO Auto-generated constructor stub
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
			log.info("Readable Bytes:%s", super.actualReadableBytes());
			ChannelBuffer cb = buffer.readBytes(super.actualReadableBytes());
			if(cb.readableBytes()<responseSize) {
				log.info("Not enough bytes. Replaying....");
				throw REPLAY_ERROR;
			}
			cbis = new ChannelBufferInputStream(cb);
			input = new UnsafeInput(cbis);
			log.info("Kryo decoding...");
			return KryoFactory.getInstance().getKryo(channel).readClassAndObject(input);
		} catch (Exception ex) {
			log.warn("Kryo decode failed: [%s]", ex.toString());
			buffer.resetReaderIndex();
			throw REPLAY_ERROR;
		} finally {
			if(input!=null) try { input.close(); } catch (Exception x) { /* No Op */ }
			if(cbis!=null) try { cbis.close(); } catch (Exception x) { /* No Op */ }
		}
	}

}
