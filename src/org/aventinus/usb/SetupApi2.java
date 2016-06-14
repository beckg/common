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
import com.sun.jna.win32.*; 
import com.sun.jna.platform.win32.*;
 
public interface SetupApi2 extends StdCallLibrary 
{ 
    SetupApi2 INSTANCE = (SetupApi2)Native.loadLibrary("setupapi", SetupApi2.class, W32APIOptions.DEFAULT_OPTIONS); 
 
    /** 
     * The GUID_DEVINTERFACE_DISK device interface class is defined for hard disk storage devices. 
    */ 
    public static Guid.GUID GUID_DEVINTERFACE_DISK = new Guid.GUID(new byte[] 
    { 
        0x07, 0x63, (byte) 0xf5, 0x53, (byte) 0xbf, (byte) 0xb6, (byte) 0xd0, 0x11, 
        (byte) 0x94, (byte) 0xf2, 0x00, (byte) 0xa0, (byte) 0xc9, (byte) 0x1e, (byte) 0xfb, (byte) 0x8b 
    }); 
 
    /** 
    * Return only the device that is associated with the system default device interface, if one is set, for the 
    * specified device interface classes. 
    */ 
    public int DIGCF_DEFAULT = 0x1; 
 
    /** 
    * Return only devices that are currently present in a system. 
    */ 
    public int DIGCF_PRESENT = 0x2; 
 
    /** 
    * Return a list of installed devices for all device setup classes or all device interface classes. 
    */ 
    public int DIGCF_ALLCLASSES = 0x4; 
 
    /** 
    * Return only devices that are a part of the current hardware profile. 
    */ 
    public int DIGCF_PROFILE = 0x8; 
 
    /** 
    * Return devices that support device interfaces for the specified device interface classes. This flag must be set 
    * in the Flags parameter if the Enumerator parameter specifies a device instance ID. 
    */ 
    public int DIGCF_DEVICEINTERFACE = 0x10; 
 
    WinNT.HANDLE SetupDiGetClassDevs(Guid.GUID.ByReference classGuid, Pointer enumerator, Pointer hwndParent, int flags); 
 
    boolean SetupDiDestroyDeviceInfoList(WinNT.HANDLE hDevInfo); 
 
    boolean SetupDiEnumDeviceInterfaces(WinNT.HANDLE hDevInfo, Pointer devInfo, 
           Guid.GUID.ByReference interfaceClassGuid, int memberIndex, 
           SP_DEVICE_INTERFACE_DATA.ByReference deviceInterfaceData); 
 
    boolean SetupDiGetDeviceInterfaceDetail(WinNT.HANDLE hDevInfo, 
           SP_DEVICE_INTERFACE_DATA.ByReference deviceInterfaceData, SP_DEVICE_INTERFACE_DETAIL_DATA.ByReference deviceInterfaceDetailData, 
           int deviceInterfaceDetailDataSize, IntByReference requiredSize, SP_DEVINFO_DATA.ByReference deviceInfoData); 
 
    boolean SetupDiGetDeviceRegistryProperty(WinNT.HANDLE DeviceInfoSet, SP_DEVINFO_DATA.ByReference DeviceInfoData, 
            int Property, IntByReference PropertyRegDataType, Pointer PropertyBuffer, int PropertyBufferSize, 
            IntByReference RequiredSize); 
 
    public static class SP_DEVICE_INTERFACE_DATA extends Structure 
    { 
        public static class ByReference extends SP_DEVICE_INTERFACE_DATA implements Structure.ByReference 
        { 
            public ByReference() 
            { 
            } 
 
            public ByReference(Pointer memory) 
            {  
                super(memory); 
            } 
        } 
 
        public SP_DEVICE_INTERFACE_DATA() 
        { 
            cbSize = size(); 
        } 
 
        public SP_DEVICE_INTERFACE_DATA(Pointer memory) 
        { 
            super(memory); 
            read(); 
        } 
 
        public int cbSize; 
        public Guid.GUID InterfaceClassGuid; 
        public int Flags; 
        public Pointer Reserved; 

        public List<String> getFieldOrder()
        {
            return Arrays.asList(new String[] {"cbSize", "InterfaceClassGuid", "Flags", "Reserved"});  
        }
    } 
 
    public static class SP_DEVINFO_DATA extends Structure  
    { 
        public static class ByReference extends SP_DEVINFO_DATA implements Structure.ByReference 
        { 
            public ByReference() 
            { 
            } 
 
            public ByReference(Pointer memory) 
            { 
                super(memory); 
            } 
        } 
 
        public SP_DEVINFO_DATA()
        { 
            cbSize = size(); 
        } 
 
        public SP_DEVINFO_DATA(Pointer memory) 
        { 
            super(memory); 
            read(); 
        } 
 
        public int cbSize; 
        public Guid.GUID InterfaceClassGuid; 
        public int DevInst; 
        public Pointer Reserved; 

        public List<String> getFieldOrder()
        {
            return Arrays.asList(new String[] {"cbSize", "InterfaceClassGuid", "DevInst", "Reserved"});  
        }
    } 

    public static class SP_DEVICE_INTERFACE_DETAIL_DATA extends Structure 
    {
        public static class ByReference extends SP_DEVICE_INTERFACE_DETAIL_DATA implements Structure.ByReference 
        { 
            public ByReference(int length) 
            {
                super(length);
            } 
 
            public ByReference(Pointer memory) 
            { 
                super(memory); 
            } 
        } 
 
        public SP_DEVICE_INTERFACE_DETAIL_DATA(int length)
        { 
            setAlignType(Structure.ALIGN_NONE); 
            cbSize = 4 + 1 * 2;
            devicePath = new char[(length - 4) / 2]; 
        } 
 
        public SP_DEVICE_INTERFACE_DETAIL_DATA(Pointer memory)
        { 
            super(memory); 
            read(); 
        } 

        public int cbSize; 
        public char[] devicePath = new char[1]; 

        public List<String> getFieldOrder()
        {
            return Arrays.asList(new String[] {"cbSize", "devicePath"});  
        }
    } 
}
