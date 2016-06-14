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
//import java.util.List;
//import java.io.*;

import com.sun.jna.*; 
import com.sun.jna.win32.*;
import com.sun.jna.ptr.*;
import com.sun.jna.platform.win32.*;

import org.aventinus.util.*;

public class Scanner
{ 
    private static Logger logger = Logger.logger(Scanner.class);;

    public interface Hid extends StdCallLibrary 
    { 
        Hid INSTANCE = (Hid)Native.loadLibrary("hid", Hid.class, W32APIOptions.UNICODE_OPTIONS); 
         
        void HidD_GetHidGuid(Guid.GUID guid); 
    } 

    public static List<String> findDevices(int vid, int pid)
    {
        try
        {
            List<String> candidates = new ArrayList<String>();

            Guid.GUID guid = new Guid.GUID();
            Hid.INSTANCE.HidD_GetHidGuid(guid);
            Guid.GUID.ByReference guidRef = new Guid.GUID.ByReference(guid);           
//            logger.info("GUID=" + guid);

            WinNT.HANDLE handle;

            handle = SetupApi2.INSTANCE.SetupDiGetClassDevs(guidRef, null, null, SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
//            handle = SetupApi2.INSTANCE.SetupDiGetClassDevs(null, null, null, SetupApi.DIGCF_ALLCLASSES);
//            logger.info("Pointer=" + handle.getPointer());

            for (int index = 0; index < 1000; index++)
            {
                SetupApi2.SP_DEVICE_INTERFACE_DATA.ByReference deviceInterfaceData = new SetupApi2.SP_DEVICE_INTERFACE_DATA.ByReference();
                boolean resp = SetupApi2.INSTANCE.SetupDiEnumDeviceInterfaces(handle, null, guidRef, index, deviceInterfaceData);
//                boolean resp = SetupApi2.INSTANCE.SetupDiEnumDeviceInterfaces(handle, null, null, index, deviceInterfaceData);
                if (resp == false)
                {
                    if (Native.getLastError() == WinNT.ERROR_NO_MORE_ITEMS)
                        break;
                    int error = Native.getLastError();
                    SetupApi2.INSTANCE.SetupDiDestroyDeviceInfoList(handle);
                    throw new RuntimeException("error=" + error);
                }

//                logger.info("index=" + index);

                IntByReference length = new IntByReference(); 
                resp = SetupApi2.INSTANCE.SetupDiGetDeviceInterfaceDetail(handle, deviceInterfaceData, null, 0, length, null);
                if ((resp == true) || (Native.getLastError() != WinNT.ERROR_INSUFFICIENT_BUFFER))
                {
                    int error = Native.getLastError();
                    SetupApi2.INSTANCE.SetupDiDestroyDeviceInfoList(handle);
                    throw new RuntimeException("error=" + error);
                }

                SetupApi2.SP_DEVICE_INTERFACE_DETAIL_DATA.ByReference deviceInterfaceDetailData = new SetupApi2.SP_DEVICE_INTERFACE_DETAIL_DATA.ByReference(length.getValue());
                resp = SetupApi2.INSTANCE.SetupDiGetDeviceInterfaceDetail(handle, deviceInterfaceData, deviceInterfaceDetailData, length.getValue(), null, null); 
                if (resp == false)
                {
                    int error = Native.getLastError();
                    SetupApi2.INSTANCE.SetupDiDestroyDeviceInfoList(handle);
                    throw new RuntimeException("error=" + error);
                }

                String path = Native.toString(deviceInterfaceDetailData.devicePath);
//logger.info("... path[" + path + "]");

                if (vid >= 0)
                {
                    if (path.indexOf("vid_" + Hex.toHex(vid).substring(4).toLowerCase()) < 0)
                        continue;
                }

                if (pid >= 0)
                {
                    if (path.indexOf("pid_" + Hex.toHex(pid).substring(4).toLowerCase()) < 0)
                        continue;
                }

                candidates.add(path);
            }

            SetupApi.INSTANCE.SetupDiDestroyDeviceInfoList(handle);

            return candidates;
        }
        catch (Exception exception)
        {
            throw new RuntimeException(exception);
        }
    }
} 
