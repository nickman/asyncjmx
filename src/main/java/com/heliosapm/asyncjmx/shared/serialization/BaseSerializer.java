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
	
	protected static String ind() {
		return indent.get().toString();
	}
	
	protected static String indent() {
		return indent.get().append("-").toString();
	}
	
	protected static String undent() {
		StringBuilder b = indent.get(); 
		b.deleteCharAt(0);
		if(b.length()==0) indent.remove();
		return b.toString();
	}
	
	
	@Override
	public void write(Kryo kryo, Output output, T object) {
		final String ind = indent();
		if(object==null) {
			log.debug("%s Writing null", ind);
		} else {
			log.debug("%s Writing instance of [%s]", ind, object.getClass().getName());
		}
		try {
			doWrite(kryo, output, object);
			if(object==null) {
				log.debug("%s Wrote null", ind);
			} else {
				log.debug("%s Wrote instance of [%s] -->[%s]", ind, object.getClass().getName(), object.toString());
			}
			
		} catch (Exception ex) {
			log.error("Serializer write failed:[%s]", ex.toString());
			throw new RuntimeException("Serializer write [" + getClass().getSimpleName() + "] failed:" + ex);
		} finally {
			undent();
		}
	}

	@Override
	public T read(Kryo kryo, Input input, Class<T> type) {
		final String ind = indent();
		log.debug("%s Reading instance of [%s]", ind, type.getName());
		try {
			T t = doRead(kryo, input, type);
			if(t!=null) {
				log.debug("%s Read instance of [%s]-->[%s]", ind, type.getName(), t);
			} else {
				log.debug("%s Read null instance of [%s]", ind, type.getName());
			}
			return t;
		} catch (Exception ex) {
			//log.error("Serializer read failed:[%s]", ex);
//			ex.printStackTrace(System.err);
			throw new RuntimeException("Serializer read [" + getClass().getSimpleName() + "] failed:" , ex);
		} finally {
			undent();
		}
	}
	
	protected abstract void doWrite(Kryo kryo, Output output, T object);
	protected abstract T doRead(Kryo kryo, Input input, Class<T> type);

}
