/**
 * 
 */
package com.heliosapm.asyncjmx.shared.serialization;

import javax.management.MBeanServerNotification;
import javax.management.ObjectName;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: MBeanServerNotificationSerializer</p>
 * <p>Description: Serializer for {@link MBeanServerNotification} instances </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanServerNotificationSerializer</code></b>
 */

public class MBeanServerNotificationSerializer extends BaseSerializer<MBeanServerNotification> {

	@Override
	protected void doWrite(Kryo kryo, Output output, MBeanServerNotification n) {
		output.writeString(n.getType());
		output.writeLong(n.getSequenceNumber());
		output.writeLong(n.getTimeStamp());
		log.info("MBeanServerNotification Source: type [%s],  val [%s]", n.getSource().getClass().getName(), n.getSource());
		kryo.writeClassAndObject(output, n.getSource());
		kryo.writeClassAndObject(output, n.getUserData());
		kryo.writeClassAndObject(output, n.getMBeanName());
	}

	@Override
	protected MBeanServerNotification doRead(Kryo kryo, Input input, Class<MBeanServerNotification> type) {
		String mtype = input.readString();
		long seq = input.readLong();
		long ts = input.readLong();
		Object src = kryo.readClassAndObject(input);
		Object userData = kryo.readClassAndObject(input);
		ObjectName objectName = (ObjectName)kryo.readClassAndObject(input);
		MBeanServerNotification n = new MBeanServerNotification(mtype, src, seq, objectName);
		if(userData!=null) n.setUserData(userData);
		n.setTimeStamp(ts);
		return n;
	}

}
