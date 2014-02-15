/**
 * 
 */
package com.heliosapm.asyncjmx.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.server.serialization.JMXOpDecodeStep;
import com.heliosapm.asyncjmx.server.serialization.JMXOpDecoder2;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.serialization.BaseSerializer;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: JMXOp</p>
 * <p>Description: Wraps a JMX invocation</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.client.JMXOp</code></b>
 */
@DefaultSerializer(JMXOp.JMXOpSerializer.class)
public class JMXOp {
	/** The JMX Client Channel */
	private Channel jmxChannel;
	/** The JMX Op Code */
	private JMXOpCode jmxOpCode;
	/** The JMX Op arguments */
	private Object[] opArguments;
	
	/** The number of arguments read so far */
	private transient int argsRead = 0; 

	/** The JMX Op sequence */
	private int opSeq;
	/** The channel specific request id serial number factory */
	private static ChannelLocal<AtomicInteger> serial = new ChannelLocal<AtomicInteger>(true) {
		@Override
		protected AtomicInteger initialValue(Channel channel) {
			return new AtomicInteger(0);
		}
	};
	

	/**
	 * Creates a new JMXOp for the passed channel
	 * @param channel The channel to create the JMXOp for
	 * @param jmxOpCode The JMX Op Code 
	 * @param opArguments The JMX Op arguments
	 * @return the JMXOp
	 */
	public static JMXOp newOp(Channel channel, JMXOpCode jmxOpCode, Object...opArguments) {
		return new JMXOp(channel, serial.get(channel).incrementAndGet(), jmxOpCode, opArguments);
	}
	
	/**
	 * Creates a new JMXOp
	 * @param channel The channel issuing the JMX op
	 * @param opSeq The JMX Op sequence
	 * @param jmxOpCode The JMX Op code
	 * @param opArguments The JMX Op arguments
	 */
	private JMXOp(Channel channel, int opSeq, JMXOpCode jmxOpCode, Object...opArguments) {
		this.jmxChannel = channel;
		this.opSeq = opSeq;
		this.jmxOpCode = jmxOpCode;
		this.opArguments = opArguments;
	}
	
	/**
	 * Creates a new empty jmx op 
	 */
	private JMXOp() {
		this.jmxChannel = null;
	}
	
	/**
	 * Returns the JMX Op arguments
	 * @return the opArguments
	 */
	public Object[] getOpArguments() {
		return Arrays.copyOf(opArguments, opArguments.length);
	}
	
	/**
	 * Returns the JMX Connection Channel
	 * @return the JMX Connection Channel
	 */
	public Channel getJmxChannel() {
		return jmxChannel;
	}

	/**
	 * Returns the JMXOpCode 
	 * @return the jmxOpCode
	 */
	public JMXOpCode getJmxOpCode() {
		return jmxOpCode;
	}

	/**
	 * Returns the request id
	 * @return the request id
	 */
	public int getOpSeq() {
		return opSeq;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMXOp [");
		if (jmxChannel != null) {
			builder.append("jmxChannel [");
			builder.append(jmxChannel);
			builder.append("], ");
		}
		builder.append("opSeq [");
		builder.append(opSeq);
		builder.append("],");
		
		if (jmxOpCode != null) {
			builder.append("jmxOpCode [");
			builder.append(jmxOpCode.name());
			builder.append("], ");
		}
		if (opArguments != null) {
			builder.append("opArguments [");
			builder.append(Arrays.toString(opArguments));
			builder.append("], ");
		}
		builder.append("]");
		return builder.toString();
	}




	/**
	 * <p>Title: JMXOpSerializer</p>
	 * <p>Description: A serializer for {@link JMXOp} instances</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.client.JMXOp.JMXOpSerializer</code></b>
	 */
	public static class JMXOpSerializer extends BaseSerializer<JMXOp> {
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
		
		private final JMXOpDecoder2 replay;
		
		/**
		 * Creates a new JMXOpSerializer with a reference to the currently running replaying decoder
		 * @param replay The replaying decoder to callback on as a step is complete
		 */
		public JMXOpSerializer(JMXOpDecoder2 replay) {
			super();
			this.replay = replay;
		}
		
		/**
		 * Creates a new JMXOpSerializer 
		 */
		public JMXOpSerializer() {
			super();
			this.replay = null;
		}
		
		@Override
		protected void doWrite(Kryo kryo, Output output, JMXOp op) {
			output.writeInt(op.opSeq);
			output.writeByte(op.jmxOpCode.opCode);
			output.writeByte(op.opArguments.length);
			for(Object o: op.opArguments) {
				kryo.writeClassAndObject(output, o);
			}
		}
		

		@Override
		protected JMXOp doRead(Kryo kryo, Input input, Class<JMXOp> type) {
			if(replay==null) {
				return doReadNoReplay(kryo, input, type);
			} else {
				return doReadWithReplay(kryo, input, type);
			}
		}
		
		@SuppressWarnings("unchecked")
		protected JMXOp doReadWithReplay(Kryo kryo, Input input, Class<JMXOp> type) {
			JMXOp state = (JMXOp)kryo.getContext().get("JMXOp");
			if(state==null) {
				state = new JMXOp();
				kryo.getContext().put("JMXOp", state);
			}
			try {
				switch(replay.getState()) {
					case REQUESTID:					
						state.opSeq = input.readInt();
						replay.checkpoint(JMXOpDecodeStep.OPCODE);					
					case OPCODE:
						state.jmxOpCode = JMXOpCode.decode(input.readByte());
						replay.checkpoint(JMXOpDecodeStep.ARGCOUNT);
					case ARGCOUNT:
						int argCount = input.readInt();
						state.opArguments = new Object[argCount];
						if(argCount==0) {
							kryo.getContext().remove("JMXOp");
							replay.checkpoint(JMXOpDecodeStep.BYTESIZE);
							return state;
						}
						replay.checkpoint(JMXOpDecodeStep.ARGS);
					case ARGS:
						while(state.argsRead < state.opArguments.length) {
							state.opArguments[state.argsRead] = kryo.readClassAndObject(input);
							state.argsRead++;
							replay.checkpoint(JMXOpDecodeStep.ARGS2);
						}					
						kryo.getContext().remove("JMXOp");
						replay.checkpoint(JMXOpDecodeStep.BYTESIZE);
						return state;
					case ARGS2:
						while(state.argsRead < state.opArguments.length) {
							state.opArguments[state.argsRead] = kryo.readClassAndObject(input);
							state.argsRead++;
							replay.checkpoint(JMXOpDecodeStep.ARGS);
						}					
						kryo.getContext().remove("JMXOp");
						replay.checkpoint(JMXOpDecodeStep.BYTESIZE);
						return state;
						
					default:
						throw new Error("Should not reach here");
				}
			} catch (Throwable ex) {
				log.warn("Kryo decode failed: [%s]", ex.toString(), ex);				
				throw REPLAY_ERROR;
			}
		}
		
		protected JMXOp doReadNoReplay(Kryo kryo, Input input, Class<JMXOp> type) {			
			int opSeq = input.readInt();
			JMXOpCode opCode = JMXOpCode.decode(input.readByte());
			byte argCount = input.readByte();
			Object[] args = new Object[argCount];
			for(byte i = 0; i < argCount; i++) {
				args[i] = kryo.readClassAndObject(input);
			}
			return new JMXOp(null, opSeq, opCode, args);						
		}
		
	}
	
	
}
