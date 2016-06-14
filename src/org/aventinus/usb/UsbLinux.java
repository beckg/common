//-----------------------------------------------------------------------------------
// Copyright (c) 2009-2013, Gordon Beck (gordon.beck@aventinus.org). All rights reserved.
//
//    This file is part of a suite of tools. 
//
//    The tools are free software: you can redistribute it and/or modify 
//    it under the terms of the GNU General Public License as published by 
//    the Free Software Foundation, either version 3 of the License, or 
//    (at your option) any later version. 
// 
//    The tools are distributed in the hope that they will be useful, 
//    but WITHOUT ANY WARRANTY; without even the implied warranty of 
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
//    GNU General Public License for more details. 
// 
//    You should have received a copy of the GNU General Public License 
//    along with these tools.  If not, see <http://www.gnu.org/licenses/>.
//-----------------------------------------------------------------------------------
package org.aventinus.usb;

import java.util.*;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

import org.aventinus.util.*;

//http://lwn.net/Articles/143397
//ls -l /sys/bus/usb/drivers/usbhid
//echo -n "2-1:1.0" > /sys/bus/usb/drivers/usbhid/unbind

//http://www.reactivated.net/writing_udev_rules.html
//more /etc/udev/rules.d/10-usb-custom.rules
//-----------------------------------------------------------------------------------------------
//# USB device 0x04d8:0x0041
//SUBSYSTEM=="usb", ATTR{idVendor}=="04d8", ATTR{idProduct}=="0041" SYMLINK+="pic41", GROUP="pic"
//-----------------------------------------------------------------------------------------------

//usermod -G gordon,pic gordon

public class UsbLinux
{
    private Logger logger = Logger.logger(UsbLinux.class);

    public static void main(final String[] args) 
    {
        new UsbLinux().test(0x4d8, 0x0041);
    }

    public interface UsbLibrary extends Library
    { 
        public static final int DIRECTION_OUT = 0x00;
        public static final int DIRECTION_IN = 0x80;

        UsbLibrary INSTANCE = (UsbLibrary)Native.loadLibrary("usb-1.0", UsbLibrary.class); 
 
        int libusb_init(PointerByReference context);
        void libusb_exit(Pointer context);
        int libusb_get_device_list(Pointer context, PointerByReference plist);
        void libusb_free_device_list(Pointer[] devices, int unrefDevices);
        int libusb_get_device_descriptor(Pointer device, DeviceDescriptor.ByReference ddeviceDescriptor);

        int libusb_open(Pointer device, PointerByReference handle);
        void libusb_close(Pointer device);
        int libusb_detach_kernel_driver(Pointer handle, int iterfaceNumber);
        int libusb_set_configuration(Pointer handle, int configuration);
        int libusb_reset_device(Pointer handle);

        int libusb_control_transfer(Pointer handle, byte bmRequestType, byte bRequest, short wValue, 
                                    short wIndex, byte[] data, short wLength, int timeout);
        int  libusb_interrupt_transfer(Pointer handle, byte endpoint, byte[] data, int length, 
                                       IntByReference transferred, int timeout);
        int  libusb_bulk_transfer(Pointer handle, byte endpoint, byte[] data, int length, 
                                  IntByReference transferred, int timeout);
    }

    public static class DeviceDescriptor extends Structure
    {
        public byte bLength;
        public byte bDescriptorType;
        public short bcdUSB;
        public byte bDeviceClass;
        public byte bDeviceSubClass;
        public byte bDeviceProtocol;
        public byte bMaxPacketSize0;
        public short idVendor;
        public short idProduct;
        public short bcdDevice;
        public byte iManufacturer;
        public byte iProduct;
        public byte iSerialNumber;
        public byte bNumConfigurations;

        public static class ByReference extends DeviceDescriptor implements Structure.ByReference 
        { 
        }

        @Override
        public List<String> getFieldOrder()
        {
            return Arrays.asList(new String[] {"bLength", "bDescriptorType", "bcdUSB", "bDeviceClass",
                                               "bDeviceSubClass", "bDeviceProtocol", "bMaxPacketSize0", "idVendor",
                                               "idProduct", "bcdDevice", "iManufacturer", "iProduct",
                                               "iSerialNumber", "bNumConfigurations"});  
        }
    }

    private PointerByReference context = null;
    private Pointer handle;

    public void open(int vendor, int product)
    {
        try
        {
            context = new PointerByReference();
            int rc = UsbLibrary.INSTANCE.libusb_init(context); 
            if (rc != 0)
                throw new RuntimeException("libusb_init rc=" + rc);

            PointerByReference devicesByRef = new PointerByReference();
            rc = UsbLibrary.INSTANCE.libusb_get_device_list(context.getValue(), devicesByRef);
            if (rc < 0)
                throw new RuntimeException("libusb_get_device_list rc=" + rc);

            Pointer[] devices = null;
            try
            {
                devices = devicesByRef.getValue().getPointerArray(0, rc);
                for (int i = 0; i < devices.length; i++)
                {
                    DeviceDescriptor.ByReference deviceDescriptor = new DeviceDescriptor.ByReference();
                    rc = UsbLibrary.INSTANCE.libusb_get_device_descriptor(devices[i], deviceDescriptor);
                    if (rc < 0)
                        throw new RuntimeException("libusb_get_device_descriptor rc=" + rc);

//                    logger.info("size=" + Hex.toHex(deviceDescriptor.bLength) 
//                                + " vendor=" + Hex.toHex(deviceDescriptor.idVendor) 
//                                + " product=" + Hex.toHex(deviceDescriptor.idProduct));

                    if ((deviceDescriptor.idVendor == vendor) && (deviceDescriptor.idProduct == product))
                    {
                        PointerByReference handleByRef = new PointerByReference();
                        rc = UsbLibrary.INSTANCE.libusb_open(devices[i], handleByRef);
                        if (rc < 0)
                            logger.info("libusb_open rc=" + rc);

                        handle = handleByRef.getValue();

                        // we need to have permission to do this - the simplest way is through udev rules
                        rc = UsbLibrary.INSTANCE.libusb_detach_kernel_driver(handle, 0);
                        if (rc < 0)
                            logger.info("libusb_detach_kernel_driver rc=" + rc);

                        return;
                     }
                }

                throw new RuntimeException("Failed to find device");
            }
            finally
            {
                if (devices != null)
                    UsbLibrary.INSTANCE.libusb_free_device_list(devices, 1);
            }
        }
        catch (Exception exception)
        {
            try
            {
                close();
                throw new RuntimeException(exception);
            }
            catch (Exception exception2)
            {
                exception2.initCause(exception);
                throw new RuntimeException(exception2);
            }
        }
    }

    public void close()
    {
        if (handle != null)
            UsbLibrary.INSTANCE.libusb_close(handle);
        handle = null;

        if (context != null)
            UsbLibrary.INSTANCE.libusb_exit(context.getValue());
        context = null;
    }

    public void write(int endPoint, byte[] request)
    {
        IntByReference trf = new IntByReference();
        int rc = interruptTransfer(UsbLibrary.DIRECTION_OUT | endPoint, request, request.length, trf, 1000);
        if (rc < 0)
            throw new RuntimeException("transfer OUT " + rc);
        if (trf.getValue() != request.length)
            throw new RuntimeException("transfer OUT trf=" + trf.getValue());
    }

    public byte[] read(int endPoint, int length)
    {
        byte[] data = new byte[length];
        IntByReference trf = new IntByReference();
        int rc = interruptTransfer(UsbLibrary.DIRECTION_IN | endPoint, data, data.length, trf, 1000);
        if (rc < 0)
            throw new RuntimeException("transfer IN rc=" + rc + " trf=" + trf.getValue());
        if (trf.getValue() == 0)
            throw new RuntimeException("transfer IN trf=" + trf.getValue());

        byte[] response = new byte[trf.getValue()];
        System.arraycopy(data, 0, response, 0, trf.getValue());
        return response;
    }

    public int controlTransfer(int bmRequestType, int bRequest, int wValue, int wIndex, byte[] data, int wLength, int timeout)
    {
        if (handle == null)
            throw new RuntimeException("open() failed");

        return UsbLibrary.INSTANCE.libusb_control_transfer(handle, (byte)bmRequestType, (byte)bRequest, (short)wValue,
                                                           (short)wIndex, data, (short)wLength, timeout);
    }

    public int interruptTransfer(int endpoint, byte[] data, int length, IntByReference transferred, int timeout)
    {
        if (handle == null)
            throw new RuntimeException("open() failed");

        return UsbLibrary.INSTANCE.libusb_interrupt_transfer(handle, (byte)endpoint, data, length, transferred, timeout);
    }
 
    public int bulkTransfer(int endpoint, byte[] data, int length, IntByReference transferred, int timeout)
    {
        if (handle == null)
            throw new RuntimeException("open() failed");

        return UsbLibrary.INSTANCE.libusb_bulk_transfer(handle, (byte)endpoint, data, length, transferred, timeout);
    }

    private void test(int vendor, int product)
    {
        open(vendor, product);
        test1();
        test2();
//        test3();
        close();
    }

    private void test1()
    {
        int rc;
        byte[] data;

        data = new byte[64];
        rc = controlTransfer(UsbLibrary.DIRECTION_IN | 0x00, 0x06, 0x0100, 0x0000, data, data.length, 1000);
        if (rc < 0)
            throw new RuntimeException("getDeviceDesc " + rc);
        logger.info("getDeviceDesc=" + Hex.toHex(data, 0, rc));

        int configurations = data[17];
        for (int i = 0; i < configurations; i++)
        {
            data = new byte[64];
            rc = controlTransfer(UsbLibrary.DIRECTION_IN | 0x00, 0x06, 0x0200, 0x0000, data, data.length, 1000);
            if (rc < 0)
                throw new RuntimeException("getConfigDesc " + rc);
            logger.info("getConfigDesc[" + i +"]=" + Hex.toHex(data, 0, rc));
        }

        data = new byte[64];
        rc = controlTransfer(UsbLibrary.DIRECTION_IN | 0x01, 0x06, 0x2100, 0x0000, data, data.length, 1000);
        if (rc < 0)
            throw new RuntimeException("getHidDesc " + rc);
        logger.info("getHidDesc=" + Hex.toHex(data, 0, rc));

        data = new byte[64];
        rc = controlTransfer(UsbLibrary.DIRECTION_IN | 0x01, 0x06, 0x2200, 0x0000, data, data.length, 1000);
        if (rc < 0)
            throw new RuntimeException("getRptDesc " + rc);
        logger.info("getRptDesc=" + Hex.toHex(data, 0, rc));
    }

    private void test2()
    {
        int rc;
        byte[] data;

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
        {
            data = new byte[8];
            data[0] = 0x07;

            IntByReference trf = new IntByReference();
            rc = interruptTransfer(UsbLibrary.DIRECTION_OUT | 0x01, data, data.length, trf, 1000);
            if (rc < 0)
                throw new RuntimeException("transfer OUT " + rc);
            if (trf.getValue() != data.length)
                throw new RuntimeException("transfer OUT trf=" + trf.getValue());
            logger.info("transfer OUT trf=" + trf.getValue() + " data=" + Hex.toHex(data));

            data = new byte[64];
            rc = interruptTransfer(UsbLibrary.DIRECTION_IN | 0x01, data, data.length, trf, 1000);
            if (rc < 0)
                throw new RuntimeException("transfer IN rc=" + rc + " trf=" + trf.getValue());
            if (trf.getValue() != data.length)
                throw new RuntimeException("transfer IN trf=" + trf.getValue());
            logger.info("transfer IN trf=" + trf.getValue() + " data=" + Hex.toHex(data));
        }

        logger.info("duration " + (System.currentTimeMillis() - start));
    }

    private void test3(int vendor, int product)
    {
        int rc;
        byte[] data;

// For reasons that are not clear the following returns 0 rather than 1
// An attempt to set 1 (without reset) hangs the device
// I suspect that this is because there is only one configuration and that this gets defaulted
// Also since we are allowing the device to be enumerated as a hid and then releasing it it might be 
// that the config is already done.
// It is also possible that we are required to use the libusb API - but that also fails to set 1
// Obviously there might also be a problem with the firmware.
// Needs more prodding

        data = new byte[1];
        rc = controlTransfer(UsbLibrary.DIRECTION_OUT | 0x00, 0x08, 0x0101, 0x0000, data, data.length, 1000);
        if (rc < 0)
            throw new RuntimeException("getConfiguration " + rc);
        logger.info("getConfiguration=0x" + Hex.toHex(data, 0, 32));

//        rc = UsbLibrary.INSTANCE.libusb_reset_device(handle);
//        if (rc < 0)
//            throw new RuntimeException("libusb_reset_device " + rc);

        if (data[0] != 0x01)
        {
            rc = controlTransfer(UsbLibrary.DIRECTION_OUT | 0x00, 0x09, 0x0101, 0x0000, null, 0, 1000);
            if (rc < 0)
                throw new RuntimeException("setConfiguration " + rc);
            logger.info("setConfiguration");
        }

//        rc = UsbLibrary.INSTANCE.libusb_set_configuration(handle, 1);
//        if (rc < 0)
//            throw new RuntimeException("libusb_set_configuration " + rc);
    }
}
