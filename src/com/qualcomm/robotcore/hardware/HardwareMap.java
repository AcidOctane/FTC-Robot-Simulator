/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.robotcore.hardware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.qualcomm.robotcore.util.RobotLog;

/**
 * HardwareMap provides a means of retrieving runtime HardwareDevice instances according to the
 * names with which the corresponding physical devices were associated during robot configuration.
 *
 * <p>
 * A HardwareMap also contains an associated application context in which it was instantiated. Through their
 * {@link com.qualcomm.robotcore.eventloop.opmode.OpMode#hardwareMap hardwareMap}, this provides access to a {@link Context} for
 * OpModes, as such an appropriate instance is needed by various system APIs.
 * </p>
 */
public class HardwareMap implements Iterable<HardwareDevice> {

	// ------------------------------------------------------------------------------------------------
	// State
	// ------------------------------------------------------------------------------------------------

	public DeviceMapping<DcMotorController> dcMotorController = new DeviceMapping<DcMotorController>();
	public DeviceMapping<DcMotor> dcMotor = new DeviceMapping<DcMotor>();

	public DeviceMapping<ServoController> servoController = new DeviceMapping<ServoController>();
	public DeviceMapping<Servo> servo = new DeviceMapping<Servo>();
	public DeviceMapping<CRServo> crservo = new DeviceMapping<CRServo>();

	public DeviceMapping<LegacyModule> legacyModule = new DeviceMapping<LegacyModule>();
	public DeviceMapping<TouchSensorMultiplexer> touchSensorMultiplexer = new DeviceMapping<TouchSensorMultiplexer>();

	public DeviceMapping<DeviceInterfaceModule> deviceInterfaceModule = new DeviceMapping<DeviceInterfaceModule>();
	public DeviceMapping<AnalogInput> analogInput = new DeviceMapping<AnalogInput>();
	public DeviceMapping<DigitalChannel> digitalChannel = new DeviceMapping<DigitalChannel>();
	public DeviceMapping<OpticalDistanceSensor> opticalDistanceSensor = new DeviceMapping<OpticalDistanceSensor>();
	public DeviceMapping<TouchSensor> touchSensor = new DeviceMapping<TouchSensor>();
	public DeviceMapping<PWMOutput> pwmOutput = new DeviceMapping<PWMOutput>();
	public DeviceMapping<I2cDevice> i2cDevice = new DeviceMapping<I2cDevice>();
	public DeviceMapping<I2cDeviceSynch> i2cDeviceSynch = new DeviceMapping<I2cDeviceSynch>();
	public DeviceMapping<AnalogOutput> analogOutput = new DeviceMapping<AnalogOutput>();
	public DeviceMapping<ColorSensor> colorSensor = new DeviceMapping<ColorSensor>();
	public DeviceMapping<LED> led = new DeviceMapping<LED>();

	public DeviceMapping<AccelerationSensor> accelerationSensor = new DeviceMapping<AccelerationSensor>();
	public DeviceMapping<CompassSensor> compassSensor = new DeviceMapping<CompassSensor>();
	public DeviceMapping<GyroSensor> gyroSensor = new DeviceMapping<GyroSensor>();
	public DeviceMapping<IrSeekerSensor> irSeekerSensor = new DeviceMapping<IrSeekerSensor>();
	public DeviceMapping<LightSensor> lightSensor = new DeviceMapping<LightSensor>();
	public DeviceMapping<UltrasonicSensor> ultrasonicSensor = new DeviceMapping<UltrasonicSensor>();
	public DeviceMapping<VoltageSensor> voltageSensor = new DeviceMapping<VoltageSensor>();

	protected Map<String, List<HardwareDevice>> allDevicesMap = new ConcurrentHashMap<String, List<HardwareDevice>>(); // concurrency
// is paranoia
	protected List<HardwareDevice> allDevicesList = null; // cache for iteration

	public final Context appContext;

	// ------------------------------------------------------------------------------------------------
	// Construction
	// ------------------------------------------------------------------------------------------------

	public HardwareMap(final Context appContext) {
		this.appContext = appContext;
	}

	// ------------------------------------------------------------------------------------------------
	// Retrieval
	// ------------------------------------------------------------------------------------------------

	/**
	 * Retrieves the (first) device with the indicated name which is also an instance of the
	 * indicated class or interface. If no such device is found, an exception is thrown. Example:
	 *
	 * <pre>
	 *    DcMotor motorLeft = hardwareMap.get(DcMotor.class, "motorLeft");
	 *    ColorSensor colorSensor = hardwareMap.get(ColorSensor.class, "myColorSensor");
	 * </pre>
	 *
	 * @param classOrInterface the class or interface indicating the type of the device object to be retrieved
	 * @param deviceName the name of the device object to be retrieved
	 * @return a device with the indicated name which is an instance of the indicated class or interface
	 * @see #get(String)
	 * @see #getAll(Class)
	 * @see com.qualcomm.robotcore.hardware.HardwareMap.DeviceMapping#get(String)
	 */
	public <T> T get(final Class<? extends T> classOrInterface, final String deviceName) {
		final List<HardwareDevice> list = allDevicesMap.get(deviceName);
		if (list != null) {
			for (final HardwareDevice device : list) {
				if (classOrInterface.isInstance(device)) {
					return classOrInterface.cast(device);
				}
			}
		}
		throw new IllegalArgumentException(String.format("Unable to find a hardware device with name \"%s\" and type %s", deviceName, classOrInterface.getSimpleName()));
	}

	/**
	 * Returns the (first) device with the indicated name. If no such device is found, an exception is thrown.
	 * Note that the compile-time type of the return value of this method is {@link HardwareDevice},
	 * which is usually not what is desired in user code. Thus, the programmer usually casts the
	 * return type to the target type that the programmer knows the returned value to be:
	 *
	 * <pre>
	 *    DcMotor motorLeft = (DcMotor)hardwareMap.get("motorLeft");
	 *    ColorSensor colorSensor = (ColorSensor)hardwareMap.get("myColorSensor");
	 * </pre>
	 *
	 * @param deviceName the name of the device object to be retrieved
	 * @return a device with the indicated name.
	 * @see #get(Class, String)
	 * @see com.qualcomm.robotcore.hardware.HardwareMap.DeviceMapping#get(String)
	 */
	public HardwareDevice get(final String deviceName) {
		final List<HardwareDevice> list = allDevicesMap.get(deviceName);
		if (list != null) {
			for (final HardwareDevice device : list) {
				return device;
			}
		}
		throw new IllegalArgumentException(String.format("Unable to find a hardware device with name \"%s\"", deviceName));
	}

	/**
	 * Returns all the devices which are instances of the indicated class or interface.
	 * 
	 * @param classOrInterface the class or interface indicating the type of the device object to be retrieved
	 * @return all the devices registered in the map which are instances of classOrInterface
	 * @see #get(Class, String)
	 */
	public <T> List<T> getAll(final Class<? extends T> classOrInterface) {
		final List<T> result = new LinkedList<T>();
		for (final HardwareDevice device : this) {
			if (classOrInterface.isInstance(device)) {
				result.add(classOrInterface.cast(device));
			}
		}
		return result;
	}

	/**
	 * Puts a device in the overall map without having it also reside in a type-specific DeviceMapping.
	 * 
	 * @param deviceName the name by which the device is to be known (case sensitive)
	 * @param device the device to be stored by that name
	 */
	public void put(final String deviceName, final HardwareDevice device) {
		List<HardwareDevice> list = allDevicesMap.get(deviceName);
		if (list == null) {
			list = new ArrayList<HardwareDevice>(1);
			allDevicesMap.put(deviceName, list);
		}
		if (!list.contains(device)) {
			allDevicesList = null;
			list.add(device);
		}
	}

	/**
	 * Removes a device from the overall map, if present. If the device is also present in a
	 * DeviceMapping, then the device should be removed using
	 * {@link com.qualcomm.robotcore.hardware.HardwareMap.DeviceMapping#remove(String, HardwareDevice) DeviceMapping.remove()}
	 * instead of calling this method.
	 *
	 * <p>
	 * This is normally called only by code in the SDK itself, not by user code.
	 * </p>
	 *
	 * @param deviceName the name of the device to remove
	 * @param device the device to remove under that name
	 * @return whether a device was removed or not
	 */
	public boolean remove(final String deviceName, final HardwareDevice device) {
		final List<HardwareDevice> list = allDevicesMap.get(deviceName);
		if (list != null) {
			list.remove(device);
			if (list.isEmpty()) {
				allDevicesMap.remove(deviceName);
			}
			allDevicesList = null;
			return true;
		}
		return false;
	}

	private void buildAllDevicesList() {
		if (allDevicesList == null) {
			final Set<HardwareDevice> set = new HashSet<HardwareDevice>();
			for (final String key : allDevicesMap.keySet()) {
				set.addAll(allDevicesMap.get(key));
			}
			allDevicesList = new ArrayList<HardwareDevice>(set);
		}
	}

	/**
	 * Returns the number of unique device objects currently found in this HardwareMap.
	 * 
	 * @return the number of unique device objects currently found in this HardwareMap.
	 * @see #iterator()
	 */
	public int size() {
		buildAllDevicesList();
		return allDevicesList.size();
	}

	/**
	 * Returns an iterator of all the devices in the HardwareMap.
	 * 
	 * @return an iterator of all the devices in the HardwareMap.
	 * @see #size()
	 */
	@Override
	public Iterator<HardwareDevice> iterator() {
		buildAllDevicesList();
		return allDevicesList.iterator();
	}

	// ------------------------------------------------------------------------------------------------
	// Types
	// ------------------------------------------------------------------------------------------------

	/**
	 * A DeviceMapping contains a subcollection of the devices registered in a {@link HardwareMap} comprised of all the devices
	 * of a particular device type
	 *
	 * @param <DEVICE_TYPE>
	 * @see com.qualcomm.robotcore.hardware.HardwareMap.DeviceMapping#get(String)
	 * @see #get(String)
	 */
	public class DeviceMapping<DEVICE_TYPE extends HardwareDevice> implements Iterable<DEVICE_TYPE> {
		private final Map<String, DEVICE_TYPE> map = new HashMap<String, DEVICE_TYPE>();

		public DEVICE_TYPE get(final String deviceName) {
			final DEVICE_TYPE device = map.get(deviceName);
			if (device == null) {
				final String msg = String.format("Unable to find a hardware device with the name \"%s\"", deviceName);
				throw new IllegalArgumentException(msg);
			}
			return device;
		}

		/**
		 * Registers a new device in this DeviceMapping under the indicated name. Any existing device
		 * with this name in this DeviceMapping is removed. The new device is also added to the
		 * overall collection in the overall map itself. Note that this method is normally called
		 * only by code in the SDK itself, not by user code.
		 *
		 * @param deviceName the name by which the new device is to be known (case sensitive)
		 * @param device the new device to be named
		 * @see HardwareMap#put(String, HardwareDevice)
		 */
		public void put(final String deviceName, final DEVICE_TYPE device) {

			// Remove any existing guy
			remove(deviceName);

			// Remember the new guy in the overall list
			HardwareMap.this.put(deviceName, device);

			// Remember the new guy here locally, too
			map.put(deviceName, device);
		}

		/**
		 * Removes the device with the indicated name (if any) from this DeviceMapping. The device
		 * is also removed under that name in the overall map itself. Note that this method is normally
		 * called only by code in the SDK itself, not by user code.
		 *
		 * @param deviceName the name of the device to remove.
		 * @return whether any modifications were made to this DeviceMapping
		 * @see HardwareMap#remove(String, HardwareDevice)
		 */
		public boolean remove(final String deviceName) {
			final HardwareDevice device = map.remove(deviceName);
			if (device != null) {
				HardwareMap.this.remove(deviceName, device);
				return true;
			}
			return false;
		}

		/**
		 * Returns an iterator over all the devices in this DeviceMapping.
		 * 
		 * @return an iterator over all the devices in this DeviceMapping.
		 */
		@Override
		public Iterator<DEVICE_TYPE> iterator() {
			return map.values().iterator();
		}

		/**
		 * Returns a collection of all the (name, device) pairs in this DeviceMapping.
		 * 
		 * @return a collection of all the (name, device) pairs in this DeviceMapping.
		 */
		public Set<Map.Entry<String, DEVICE_TYPE>> entrySet() {
			return map.entrySet();
		}

		/**
		 * Returns the number of devices currently in this DeviceMapping
		 * 
		 * @return the number of devices currently in this DeviceMapping
		 */
		public int size() {
			return map.size();
		}
	}

	// ------------------------------------------------------------------------------------------------
	// Utility
	// ------------------------------------------------------------------------------------------------

	private static final String LOG_FORMAT = "%-50s %-30s %s";

	public void logDevices() {
		RobotLog.i("========= Device Information ===================================================");
		RobotLog.i(String.format(LOG_FORMAT, "Type", "Name", "Connection"));

		for (final Map.Entry<String, List<HardwareDevice>> entry : allDevicesMap.entrySet()) {
			final List<HardwareDevice> list = entry.getValue();
			for (final HardwareDevice d : list) {
				final String conn = d.getConnectionInfo();
				final String name = entry.getKey();
				final String type = d.getDeviceName();
				RobotLog.i(String.format(LOG_FORMAT, type, name, conn));
			}
		}
	}
}
