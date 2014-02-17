/**
 * 
 */
package asyncjmx.openmbean;



import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
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
import org.objenesis.strategy.StdInstantiatorStrategy;

import asyncjmx.base.BaseTest;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.SerializerFactory;
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
	static final Kryo JK = new Kryo();
	
	static {
		JK.setDefaultSerializer(JavaSerializer.class);
		JK.setAsmEnabled(true);
		JK.setReferences(false);
		JK.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}
	
	
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
	

	@Test
	public void testWriteReadAllAttributeLists() throws Exception {
		long kBytesOut = 0, kBytesIn = 0, jBytesOut = 0, jBytesIn = 0; 
		for(ObjectName on: JMXHelper.query((ObjectName)null)) {
			String[] names = JMXHelper.getAttributeNames(on);
			AttributeList attrs = JMXHelper.getHeliosMBeanServer().getAttributes(on, names);
			int[] bytes = writeAndReadAttributeList(attrs);
			kBytesOut+= bytes[0];
			kBytesIn+= bytes[1];
			bytes = writeAndReadAttributeListWJavaSer(attrs);
			jBytesOut+= bytes[0];
			jBytesIn+= bytes[1];							
		}
		log("=========== CompositeData Data Volumes ===========");
		log("\t Kryo Bytes Out: %s", kBytesOut);
		log("\t Kryo Bytes In: %s", kBytesIn);
		log("\t Java Bytes Out: %s", jBytesOut);
		log("\t Java Bytes In: %s", jBytesIn);		
	}
	
	@Test
	public void MBeanInfoWriteAndReadTest() throws Exception {
		long kBytesOut = 0, kBytesIn = 0, jBytesOut = 0, jBytesIn = 0;
		List<MBeanInfo> infos = new ArrayList<MBeanInfo>();
		for(ObjectName on: JMXHelper.query((ObjectName)null)) {
			infos.add(JMXHelper.getHeliosMBeanServer().getMBeanInfo(on));
		}
		int[] bytes = writeAndReadMBeanInfos(infos);
		kBytesOut+= bytes[0];
		kBytesIn+= bytes[1];
		bytes = writeAndReadMBeanInfosWJavaSer(infos);
		jBytesOut+= bytes[0];
		jBytesIn+= bytes[1];							
		
		log("=========== CompositeData Data Volumes ===========");
		log("\t Kryo Bytes Out: %s", kBytesOut);
		log("\t Kryo Bytes In: %s", kBytesIn);
		log("\t Java Bytes Out: %s", jBytesOut);
		log("\t Java Bytes In: %s", jBytesIn);		
		
	}
	
	public int[] writeAndReadMBeanInfos(Collection<MBeanInfo> infos) {
		ByteArrayOutput bao = new ByteArrayOutput();
		MBeanInfo[] mis = infos.toArray(new MBeanInfo[infos.size()]);
		MBeanInfo[] rin = new MBeanInfo[mis.length];
		for(MBeanInfo mi: mis) {
			K.writeClassAndObject(bao, mi);
		}
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		for(int i = 0; i < mis.length; i++) {
			rin[i] = (MBeanInfo)K.readClassAndObject(in);
			Assert.assertNotNull(rin[i]);
		}
		log("Input Bytes:%s", in.getBytesRead());
		return new int[] {bao.getBytes().length, in.getBytesRead()};
	}
	
	public int[] writeAndReadMBeanInfosWJavaSer(Collection<MBeanInfo> infos) {
		Kryo k = new Kryo();
		k.register(MBeanInfo.class, new JavaSerializer());
		ByteArrayOutput bao = new ByteArrayOutput();
		MBeanInfo[] mis = infos.toArray(new MBeanInfo[infos.size()]);
		MBeanInfo[] rin = new MBeanInfo[mis.length];
		for(MBeanInfo mi: mis) {
			k.writeClassAndObject(bao, mi);
		}
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		for(int i = 0; i < mis.length; i++) {
			rin[i] = (MBeanInfo)k.readClassAndObject(in);
			Assert.assertNotNull(rin[i]);
		}
		log("Input Bytes:%s", in.getBytesRead());
		return new int[] {bao.getBytes().length, in.getBytesRead()};
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
//		JK.register(CompositeDataSupport.class, new JavaSerializer());
		JK.writeClassAndObject(bao, cd);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = JK.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		log("Input Bytes:%s", in.getBytesRead());
		Assert.assertTrue("Not instance of CompositeData", (o instanceof CompositeData));
		Assert.assertEquals(cd, o);		
		return new int[] {bao.getBytes().length, in.getBytesRead()};
	}
	
	public int[] writeAndReadAttributeList(AttributeList attrs) {
		ByteArrayOutput bao = new ByteArrayOutput();		
		K.writeClassAndObject(bao, attrs);
		bao.flush();
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = K.readClassAndObject(in);
		Assert.assertNotNull(o);
		Assert.assertTrue("Not instance of AttributeList", (o instanceof AttributeList));
		//Assert.assertEquals(attrs, o);
		compareAttributeLists(attrs, (AttributeList)o);
		return new int[] {bao.getBytes().length, in.getBytesRead()};
	}
	
	public int[] writeAndReadAttributeListWJavaSer(AttributeList attrs) {
		ByteArrayOutput bao = new ByteArrayOutput();	
		JK.setDefaultSerializer(new SerializerFactory(){
			final Serializer ser = new JavaSerializer();
			@Override
			public Serializer makeSerializer(Kryo kryo, Class<?> type) {				
				return ser;
			}
		});		
		JK.writeClassAndObject(bao, attrs);
		bao.flush();
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = JK.readClassAndObject(in);
		Assert.assertNotNull(o);
		Assert.assertTrue("Not instance of AttributeList", (o instanceof AttributeList));
		//Assert.assertEquals(attrs, o);
		compareAttributeLists(attrs, (AttributeList)o);
		return new int[] {bao.getBytes().length, in.getBytesRead()};
	}
	
	public void compareAttributeLists(AttributeList alOne, AttributeList alTwo) {
		Assert.assertEquals("Unequal Size", alOne.size(), alTwo.size());
		for(int i = 0; i < alOne.size(); i++) {
			Attribute a = (Attribute) alOne.get(i);
			Attribute b = (Attribute) alTwo.get(i);
			Assert.assertEquals("Attribute Name Mismatch", a.getName(), b.getName());
			Object oa = a.getValue();
			Object ob = b.getValue();
			Assert.assertTrue("Attribute Value was null [" + a.getName() + "]", (oa==null && ob==null) || (oa!=null && ob!=null));
			if(oa==null) {
				log("Null Attribute Value [%s]", a.getName());
				continue;
			}
//			Assert.assertEquals("Attribute Value Class Mismatch [" + a.getName() + "]", oa.getClass(), ob.getClass());
			if(oa.getClass().isArray()) {
				int asize = Array.getLength(oa); int bsize = Array.getLength(ob);
				Assert.assertEquals("Attribute Value Unequal Array Size [" + a.getName() + "]", asize, bsize);
				for(int x = 0; x < asize; x++) {
					Object oaValue = Array.get(oa, x);
					Object obValue = Array.get(ob, x);
					Assert.assertEquals("Attribute Value Mismatch [" + a.getName() + "] Index [" + x + "]", oaValue, obValue);
				}
			} else {
				Assert.assertEquals("Attribute Value Mismatch [" + a.getName() + "]", oa, ob);
			}
			
		}
	}
	

	@Test
	public void testWriteReadTabularDataWJavaSer() {
		
		JK.register(TabularDataSupport.class, new JavaSerializer());
		TabularData td = (TabularData)JMXHelper.getAttribute(JMXHelper.objectName(ManagementFactory.RUNTIME_MXBEAN_NAME), "SystemProperties");
		ByteArrayOutput bao = new ByteArrayOutput();		
		JK.writeClassAndObject(bao, td);
		bao.flush();
		log("Output Bytes:%s", bao.getBytes().length);
		ByteArrayInput in = new ByteArrayInput(bao.getBytes());
		Object o = JK.readClassAndObject(in);
		Assert.assertNotNull(o);
		log("Read in [%s]", o);
		log("Input Bytes:%s", in.getBytesRead());
		Assert.assertTrue("Not instance of TabularData", (o instanceof TabularData));
		
	}
	
}
