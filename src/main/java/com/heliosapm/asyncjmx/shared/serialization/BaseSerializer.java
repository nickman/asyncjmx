/**
 * 
 */
package com.heliosapm.asyncjmx.shared.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

/**
 * <p>Title: BaseSerializer</p>
 * <p>Description: Base serializer</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.BaseSerializer</code></b>
 */

public abstract class BaseSerializer<T> extends Serializer<T> {
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());
	
	protected static final ThreadLocal<StringBuilder> indent = new ThreadLocal<StringBuilder>() {
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder("");
		}
	};
	
	private static String indent() {
		return indent.get().append("-").toString();
	}
	private static String undent() {
		StringBuilder b = indent.get(); 
		b.deleteCharAt(0);
		if(b.length()==0) indent.remove();
		return b.toString();
	}
	
	
	@Override
	public void write(Kryo kryo, Output output, T object) {
		final String ind = indent();
		if(object==null) {
			log.info("%s Writing null", ind);
		} else {
			log.info("%s Writing instance of [%s]", ind, object.getClass().getName());
		}
		try {
			doWrite(kryo, output, object);
			if(object==null) {
				log.info("%s Wrote null", ind);
			} else {
				log.info("%s Wrote instance of [%s] -->[%s]", ind, object.getClass().getName(), object.toString());
			}
			
		} catch (Exception ex) {
			log.error("Serializer write failed", ex);
			throw new RuntimeException("Serializer write failed", ex);
		} finally {
			undent();
		}
	}

	@Override
	public T read(Kryo kryo, Input input, Class<T> type) {
		final String ind = indent();
		log.info("%s Reading instance of [%s]", ind, type.getName());
		try {
			T t = doRead(kryo, input, type);
			if(t!=null) {
				log.info("%s Read instance of [%s]-->[%s]", ind, type.getName(), t);
			} else {
				log.info("%s Read null instance of [%s]", ind, type.getName());
			}
			return t;
		} catch (Exception ex) {
			log.error("Serializer read failed", ex);
			throw new RuntimeException("Serializer read failed", ex);
		} finally {
			undent();
		}
	}
	
	protected abstract void doWrite(Kryo kryo, Output output, T object);
	protected abstract T doRead(Kryo kryo, Input input, Class<T> type);

}
