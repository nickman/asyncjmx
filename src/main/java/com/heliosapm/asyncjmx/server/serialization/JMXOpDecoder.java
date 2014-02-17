/**
 * 
 */
package com.heliosapm.asyncjmx.server.serialization;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.shared.JMXOp;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.util.ConfigurationHelper;



/**
 * <p>Title: JMXOpDecoder</p>
 * <p>Description:   </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.server.serialization.JMXOpDecoder</code></b>
 */

public class JMXOpDecoder extends ReplayingDecoder<JMXOpDecodeStep> {
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
	/** The JMXOp kryo replaying deserializer */
	protected JMXOp.JMXOpSerializer ser = new JMXOp.JMXOpSerializer(this);
	/** The kryo input */
	protected Input input = null;
	/** The payload size allocated channel buffer */
	protected ChannelBuffer buff = null;
	/** The number of bytes read from the replay buffer */
	protected int replayBytesRead = 0;
	/** The payload size of the currently processing decode */
	int payloadSize = -1;
	
	/**
	 * Creates a new JMXOpDecoder
	 */
	public JMXOpDecoder() {
		super(JMXOpDecodeStep.BYTESIZE);
	}
	
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, JMXOpDecodeStep state) throws Exception {
		
		if(state==JMXOpDecodeStep.BYTESIZE) {
			payloadSize = buffer.readInt();
			log.info("JMXOp Decode Starting. Payload Size: [%s] bytes", payloadSize);
			checkpoint(JMXOpDecodeStep.REQUESTID);
			replayBytesRead += 4;
			buff = bufferFactory.getBuffer(payloadSize);						
		}
		
		int readableBytesAvailable = super.actualReadableBytes();
		log.info("PRE: Payload Size: [%s], Replay Bytes Available: [%s]", payloadSize, readableBytesAvailable);
		buff.writeBytes(buffer, Math.min(readableBytesAvailable, payloadSize));
		input = new UnsafeInput(new ChannelBufferInputStream(buff));
		log.info("POST: Buff Bytes Available: [%s], Input Available: [%s]",buff.readableBytes(), input.available());
		return ser.read(KryoFactory.getInstance().getKryo(channel), input, JMXOp.class);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#getState()
	 */
	@Override
	public JMXOpDecodeStep getState() {
		return super.getState();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#checkpoint(java.lang.Enum)
	 */
	@Override
	public void checkpoint(JMXOpDecodeStep state) {
		super.checkpoint(state);
	}


	/**
	 * Returns the replaying decoder's managed buffer
	 * @return the replaying decoder's managed buffer
	 */
	public ChannelBuffer getBuff() {
		return buff;
	}

}
