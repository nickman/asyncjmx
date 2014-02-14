/**
 * 
 */
package asyncjmx.openmbean;



import java.lang.management.ManagementFactory;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.junit.Assert;
import org.junit.Test;

import asyncjmx.base.BaseTest;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.util.JMXHelper;

/**
 * <p>Title: CompositeDataTest</p>
 * <p>Description:   Serialization tests for CompositeData</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>asyncjmx.openmbean.CompositeDataTest</code></b>
 */

public class CompositeDataTest extends BaseTest {
	static final Kryo K = KryoFactory.getInstance().newKryo(); 
	@Test
	public void testWriteReadCompositeData() {
		CompositeData cd = (CompositeData)JMXHelper.getAttribute(JMXHelper.objectName(ManagementFactory.MEMORY_MXBEAN_NAME), "HeapMemoryUsage");
		ByteArrayOutput bao = new ByteArrayOutput();		
		K.writeClassAndObject(bao, cd);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = K.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		Assert.assertTrue("Not instance of CompositeData", (o instanceof CompositeData));		
	}
	
	@Test
	public void testWriteReadTabularType() {
		TabularData td = (TabularData)JMXHelper.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "SystemProperties");
		TabularType tt = td.getTabularType();
		ByteArrayOutput bao = new ByteArrayOutput();		
		K.writeClassAndObject(bao, tt);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = K.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		Assert.assertTrue("Not instance of TabularType", (o instanceof TabularType));		
		
	}
	
	@Test
	public void testWriteReadTabularData() {
//		K.register(TabularDataSupport.class, new JavaSerializer());
		TabularData td = (TabularData)JMXHelper.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "SystemProperties");
		ByteArrayOutput bao = new ByteArrayOutput();		
		K.writeClassAndObject(bao, td);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = K.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		Assert.assertTrue("Not instance of CompositeData", (o instanceof TabularData));		
	}
	
}
