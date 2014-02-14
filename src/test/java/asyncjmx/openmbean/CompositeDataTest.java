/**
 * 
 */
package asyncjmx.openmbean;



import java.lang.management.ManagementFactory;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
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
		log("Input Bytes:%s", in.getBytesRead());
		Assert.assertTrue("Not instance of TabularData", (o instanceof TabularData));
		Assert.assertEquals(td, o);		
	}
	
	@Test
	public void testWriteReadAllCompositeData() {
		long kBytesOut = 0, kBytesIn = 0, jBytesOut = 0, jBytesIn = 0; 
		for(ObjectName on: JMXHelper.query((ObjectName)null)) {
			MBeanInfo minfo = JMXHelper.getMBeanInfo(on);
			for(MBeanAttributeInfo mainfo: minfo.getAttributes()) {
				if(mainfo.getType().contains("Comp")) {
					log("Type: on[%s] attr[%s]  type[%s]", on, mainfo.getName(), mainfo.getType());
					Object cd = JMXHelper.getAttribute(on, mainfo.getName());
					if(cd==null) continue;
					if(cd.getClass().isArray()) {
						Object[] objs = (Object[])cd;
						for(Object o: objs) {
							int[] bytes = writeAndReadCompositeData(o);
							kBytesOut+= bytes[0];
							kBytesIn+= bytes[1];
							bytes = writeAndReadCompositeDataWJavaSer(o);
							jBytesOut+= bytes[0];
							jBytesIn+= bytes[1];							
						}
					} else {
						int[] bytes = writeAndReadCompositeData(cd);
						kBytesOut+= bytes[0];
						kBytesIn+= bytes[1];						
						bytes = writeAndReadCompositeDataWJavaSer(cd);
						jBytesOut+= bytes[0];
						jBytesIn+= bytes[1];						
					}
					log("Retrieved instance of [%s]", cd.getClass().getName());					 
				}				
			}
		}
		log("=========== CompositeData Data Volumes ===========");
		log("\t Kryo Bytes Out: %s", kBytesOut);
		log("\t Kryo Bytes In: %s", kBytesIn);
		log("\t Java Bytes Out: %s", jBytesOut);
		log("\t Java Bytes In: %s", jBytesIn);		
	}
	
	public int[] writeAndReadCompositeData(Object cd) {
		Assert.assertTrue("Input Not instance of CompositeData", (cd instanceof CompositeData));
		ByteArrayOutput bao = new ByteArrayOutput();		
		K.writeClassAndObject(bao, cd);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = K.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		log("Input Bytes:%s", in.getBytesRead());
		Assert.assertTrue("Not instance of CompositeData", (o instanceof CompositeData));
		Assert.assertEquals(cd, o);		
		return new int[] {bao.getBytes().length, in.getBytesRead()};
	}
	
	public int[] writeAndReadCompositeDataWJavaSer(Object cd) {
		Assert.assertTrue("Input Not instance of CompositeData", (cd instanceof CompositeData));
		ByteArrayOutput bao = new ByteArrayOutput();		
		Kryo k = new Kryo();
		k.register(CompositeDataSupport.class, new JavaSerializer());
		k.writeClassAndObject(bao, cd);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = k.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		log("Input Bytes:%s", in.getBytesRead());
		Assert.assertTrue("Not instance of CompositeData", (o instanceof CompositeData));
		Assert.assertEquals(cd, o);		
		return new int[] {bao.getBytes().length, in.getBytesRead()};
	}
	

	@Test
	public void testWriteReadTabularDataWJavaSer() {
//		K.register(TabularDataSupport.class, new JavaSerializer());
		Kryo k = new Kryo();
		k.register(TabularDataSupport.class, new JavaSerializer());
		TabularData td = (TabularData)JMXHelper.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "SystemProperties");
		ByteArrayOutput bao = new ByteArrayOutput();		
		k.writeClassAndObject(bao, td);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = k.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		log("Input Bytes:%s", in.getBytesRead());
		Assert.assertTrue("Not instance of TabularData", (o instanceof TabularData));
		
	}
	
}
