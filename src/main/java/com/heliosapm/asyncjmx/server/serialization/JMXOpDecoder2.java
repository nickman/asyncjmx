/**
 * 
 */
package com.heliosapm.asyncjmx.server.serialization;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.heliosapm.asyncjmx.client.JMXOp;
import com.heliosapm.asyncjmx.shared.KryoFactory;



/**
 * <p>Title: JMXOpDecoder2</p>
 * <p>Description:   </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.server.serialization.JMXOpDecoder2</code></b>
 */

public class JMXOpDecoder2 extends ReplayingDecoder<JMXOpDecodeStep> {
	/** The JMXOp kryo replaying deserializer */
	protected JMXOp.JMXOpSerializer ser = new JMXOp.JMXOpSerializer(this);
	/** The kryo input */
	protected Input input = null;
	/**
	 * Creates a new JMXOpDecoder
	 */
	public JMXOpDecoder2() {
		super(JMXOpDecodeStep.BYTESIZE);
	}
	
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, JMXOpDecodeStep state) throws Exception {
		if(state==JMXOpDecodeStep.BYTESIZE) {
			buffer.readInt();
			checkpoint(JMXOpDecodeStep.REQUESTID);
			input = new UnsafeInput(new ChannelBufferInputStream(buffer));
		}		
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

}
