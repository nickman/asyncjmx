/**
 * 
 */
package com.heliosapm.asyncjmx.shared.serialization;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.OpenType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: MBeanInfoSerializer</p>
 * <p>Description: Serializer for {@link MBeanInfo} instances</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer</code></b>
 */

public class MBeanInfoSerializer extends BaseSerializer<MBeanInfo> {
	protected static final MBeanAttributeInfoSerializer maiSer = new MBeanAttributeInfoSerializer();
	protected static final MBeanConstructorInfoSerializer mciSer = new MBeanConstructorInfoSerializer();
	protected static final MBeanOperationInfoSerializer moiSer = new MBeanOperationInfoSerializer();
	protected static final MBeanNotificationInfoSerializer mniSer = new MBeanNotificationInfoSerializer();
	protected static final MBeanParameterInfoSerializer mpiSer = new MBeanParameterInfoSerializer();
	protected static final DescriptorSerializer dSer = new DescriptorSerializer(); 
	
	@Override
	protected void doWrite(Kryo kryo, Output output, MBeanInfo mi) {
		log.debug("Writing MBeanInfo for class type [%s] desc [%s]", mi.getClassName(), mi.getDescription());
		output.writeString(mi.getClassName());
		output.writeString(mi.getDescription());
		
		MBeanAttributeInfo[] attributes = mi.getAttributes();
		MBeanConstructorInfo[] constructors = mi.getConstructors();
		MBeanOperationInfo[] operations = mi.getOperations();
		MBeanNotificationInfo[] notifications = mi.getNotifications();
		
		output.writeInt(attributes.length);
		output.writeInt(constructors.length);
		output.writeInt(operations.length);
		output.writeInt(notifications.length);
		
		for(MBeanAttributeInfo mai: attributes) maiSer.write(kryo, output, mai);		
		for(MBeanConstructorInfo mci: constructors) mciSer.write(kryo, output, mci);		
		for(MBeanOperationInfo moi: operations) moiSer.write(kryo, output, moi);		
		for(MBeanNotificationInfo mni: notifications) mniSer.write(kryo, output, mni);
		
		log.debug("Wrote MBeanInfo for class type [%s] desc [%s]", mi.getClassName(), mi.getDescription());
	}

	@Override
	protected MBeanInfo doRead(Kryo kryo, Input input, Class<MBeanInfo> type) {
		String className = input.readString();
		String description = input.readString();
		MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[input.readInt()]; 
		MBeanConstructorInfo[] constructors = new MBeanConstructorInfo[input.readInt()]; 
		MBeanOperationInfo[] operations = new MBeanOperationInfo[input.readInt()];
		MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[input.readInt()];
		
		for(int i = 0; i < attributes.length; i++) {
			attributes[i] = maiSer.read(kryo, input, MBeanAttributeInfo.class);
		}
		for(int i = 0; i < constructors.length; i++) {
			constructors[i] = mciSer.read(kryo, input, MBeanConstructorInfo.class);
		}
		for(int i = 0; i < operations.length; i++) {
			operations[i] = moiSer.read(kryo, input, MBeanOperationInfo.class);
		}
		for(int i = 0; i < notifications.length; i++) {
			notifications[i] = mniSer.read(kryo, input, MBeanNotificationInfo.class);
		}
		log.debug("Returning MBeanInfo for class type [%s] desc [%s]", className, description);
		return new MBeanInfo(className, description, attributes, constructors, operations, notifications);
	}

	/**
	 * <p>Title: MBeanAttributeInfoSerializer</p>
	 * <p>Description: Serializer for {@link MBeanAttributeInfo} instances.</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer.MBeanAttributeInfoSerializer</code></b>
	 */
	public static class MBeanAttributeInfoSerializer extends BaseSerializer<MBeanAttributeInfo> {

		@Override
		protected void doWrite(Kryo kryo, Output output, MBeanAttributeInfo mai) {
			Descriptor d = mai.getDescriptor();
			if(d==null) {
				output.writeByte(0);
			} else {
				output.writeByte(1);
				dSer.write(kryo, output, d);
			}			
			output.writeString(mai.getName());
			output.writeString(mai.getType());
			output.writeString(mai.getDescription());
			output.writeByte(mai.isReadable() ? 1 : 0);
			output.writeByte(mai.isWritable() ? 1 : 0);
			output.writeByte(mai.isIs() ? 1 : 0);
			

		}

		@Override
		protected MBeanAttributeInfo doRead(Kryo kryo, Input input, Class<MBeanAttributeInfo> type) {
			Descriptor d = null;
			if(input.readByte()==1) {
				d = dSer.read(kryo, input, Descriptor.class);
			}
			if(d==null) {
				return new MBeanAttributeInfo(input.readString(), input.readString(), input.readString(), input.readByte()==1, input.readByte()==1, input.readByte()==1);
			}
			return new MBeanAttributeInfo(input.readString(), input.readString(), input.readString(), input.readByte()==1, input.readByte()==1, input.readByte()==1, d);
		}		
	}

	/**
	 * <p>Title: MBeanOperationInfoSerializer</p>
	 * <p>Description: Serializer for {@link MBeanOperationInfo} instances.</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer.MBeanOperationInfoSerializer</code></b>
	 */
	public static class MBeanOperationInfoSerializer extends BaseSerializer<MBeanOperationInfo> {
		
		@Override
		protected void doWrite(Kryo kryo, Output output, MBeanOperationInfo moi) {
			Descriptor d = moi.getDescriptor();
			if(d==null) {
				output.writeByte(0);
			} else {
				output.writeByte(1);
				dSer.write(kryo, output, d);
			}			
			
			output.writeString(moi.getName());
			output.writeString(moi.getDescription());
			output.writeString(moi.getReturnType());
			output.writeInt(moi.getImpact());
			MBeanParameterInfo[] sig = moi.getSignature();
			output.writeInt(sig.length);
			for(MBeanParameterInfo mbi: sig) {
				mpiSer.write(kryo, output, mbi);
			}
			
			
		}

		@Override
		protected MBeanOperationInfo doRead(Kryo kryo, Input input, Class<MBeanOperationInfo> type) {
			Descriptor d = null;
			if(input.readByte()==1) {
				d = dSer.read(kryo, input, Descriptor.class);
			}
			
			String name = input.readString();
			String desc = input.readString();
			String returnType = input.readString();
			int impact = input.readInt();
			int size = input.readInt();
			MBeanParameterInfo[] signature = new MBeanParameterInfo[size];
			for(int i = 0; i < size; i++) {
				signature[i] = mpiSer.read(kryo, input, MBeanParameterInfo.class);
			} 
			if(d==null) {
				return new MBeanOperationInfo(name, desc, signature, returnType, impact);
			} 
			return new MBeanOperationInfo(name, desc, signature, returnType, impact, d);
		}
		
	}
	/**
	 * <p>Title: MBeanNotificationInfoSerializer</p>
	 * <p>Description: Serializer for {@link MBeanNotificationInfo} instances.</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer.MBeanNotificationInfoSerializer</code></b>
	 */
	public static class MBeanNotificationInfoSerializer extends BaseSerializer<MBeanNotificationInfo> {

		@Override
		protected void doWrite(Kryo kryo, Output output, MBeanNotificationInfo mni) {
			Descriptor d = mni.getDescriptor();
			if(d==null) {
				output.writeByte(0);
			} else {
				output.writeByte(1);
				dSer.write(kryo, output, d);
			}			
			
			String[] types = mni.getNotifTypes();
			output.writeInt(types.length);
			for(String s: types) {
				output.writeString(s);
			}
			output.writeString(mni.getName());
			output.writeString(mni.getDescription());
		}

		@Override
		protected MBeanNotificationInfo doRead(Kryo kryo, Input input, Class<MBeanNotificationInfo> type) {
			Descriptor d = null;
			if(input.readByte()==1) {
				d = dSer.read(kryo, input, Descriptor.class);
			}
			
			int size = input.readInt();
			String[] notifTypes = new String[size];
			for(int i = 0; i < size; i++) {
				notifTypes[i] = input.readString();
			}
			if(d==null) {
				return new MBeanNotificationInfo(notifTypes, input.readString(), input.readString());
			}
			return new MBeanNotificationInfo(notifTypes, input.readString(), input.readString(), d);
		}
		
	}
	/**
	 * <p>Title: MBeanConstructorInfoSerializer</p>
	 * <p>Description: Serializer for {@link MBeanConstructorInfo} instances.</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer.MBeanConstructorInfoSerializer</code></b>
	 */
	public static class MBeanConstructorInfoSerializer extends BaseSerializer<MBeanConstructorInfo> {
		@Override
		protected void doWrite(Kryo kryo, Output output, MBeanConstructorInfo mci) {
			Descriptor d = mci.getDescriptor();
			if(d==null) {
				output.writeByte(0);
			} else {
				output.writeByte(1);
				dSer.write(kryo, output, d);
			}			
			
			output.writeString(mci.getName());
			output.writeString(mci.getDescription());
			MBeanParameterInfo[] sig = mci.getSignature();
			output.writeInt(sig.length);
			for(MBeanParameterInfo mbi: sig) {
				mpiSer.write(kryo, output, mbi);
			}
		}

		@Override
		protected MBeanConstructorInfo doRead(Kryo kryo, Input input, Class<MBeanConstructorInfo> type) {
			Descriptor d = null;
			if(input.readByte()==1) {
				d = dSer.read(kryo, input, Descriptor.class);
			}

			String name = input.readString();
			String desc = input.readString();
			int size = input.readInt();
			MBeanParameterInfo[] signature = new MBeanParameterInfo[size];
			for(int i = 0; i < size; i++) {
				signature[i] = mpiSer.read(kryo, input, MBeanParameterInfo.class);
			}
			if(d==null) {
				return new MBeanConstructorInfo(name, desc, signature);
			}
			return new MBeanConstructorInfo(name, desc, signature, d);
		}
		
	}
	/**
	 * <p>Title: MBeanParameterInfoSerializer</p>
	 * <p>Description: Serializer for {@link MBeanParameterInfo} instances.</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer.MBeanParameterInfoSerializer</code></b>
	 */
	public static class MBeanParameterInfoSerializer extends BaseSerializer<MBeanParameterInfo> {

		@Override
		protected void doWrite(Kryo kryo, Output output, MBeanParameterInfo mpi) {
			Descriptor d = mpi.getDescriptor();
			if(d==null) {
				output.writeByte(0);
			} else {
				output.writeByte(1);
				dSer.write(kryo, output, d);
			}						
			output.writeString(mpi.getName());
			output.writeString(mpi.getType());
			output.writeString(mpi.getDescription());
			
		}

		@Override
		protected MBeanParameterInfo doRead(Kryo kryo, Input input, Class<MBeanParameterInfo> type) {	
			Descriptor d = null;
			if(input.readByte()==1) {
				d = dSer.read(kryo, input, Descriptor.class);
			}
			if(d==null) {
				return new MBeanParameterInfo(input.readString(), input.readString(), input.readString());
			}
			return new MBeanParameterInfo(input.readString(), input.readString(), input.readString(), d);
		}
		
	}
	
	/**
	 * <p>Title: DescriptorSerializer</p>
	 * <p>Description: Serializer for {@link Descriptor} instances.</p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>com.heliosapm.asyncjmx.shared.serialization.MBeanInfoSerializer.DescriptorSerializer</code></b>
	 */
	public static class DescriptorSerializer extends BaseSerializer<Descriptor> {

		@Override
		protected void doWrite(Kryo kryo, Output output, Descriptor d) {
			output.writeByte(0);
			//kryo.writeClassAndObject(output, d);
//			if(d instanceof ImmutableDescriptor) {
//				output.writeByte(1);
//			} else {
//				output.writeByte(0);
//			}
//			String[] fieldNames = d.getFieldNames();
//			output.writeInt(fieldNames.length);
//			for(String s: fieldNames) {
//				output.writeString(s);
//				Object value = d.getFieldValue(s);
//				kryo.writeClassAndObject(output, value);
//			}
		}

		@Override
		protected Descriptor doRead(Kryo kryo, Input input, Class<Descriptor> type) {
			input.readByte();
			return null;
//			boolean immutable = input.readByte()==1;
//			int fieldCount = input.readInt();
//			Map<String, Object> fieldValueMap = new LinkedHashMap<String, Object>(fieldCount);
//			for(int i = 0; i < fieldCount; i++) {
//				String key = input.readString();
//				Object value = kryo.readClassAndObject(input);
//				fieldValueMap.put(key, value);
//			}
//			if(immutable) return new ImmutableDescriptor(fieldValueMap);
//			return new DescriptorSupport(fieldValueMap.keySet().toArray(new String[fieldCount]), fieldValueMap.values().toArray());			
		}
		
	}
	
	
}
