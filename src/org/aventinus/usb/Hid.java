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
import java.util.List;
import java.io.*;

import com.sun.jna.*; 
import com.sun.jna.win32.*;
import com.sun.jna.ptr.*;
import com.sun.jna.platform.win32.*;

import org.aventinus.util.*;

public class Hid
{
    private static Logger logger = Logger.logger(Hid.class);

    public static class Id
    {
        private int vendor;
        private int product;

        public Id(int vendor, int product)
        {
            this.vendor = vendor;
            this.product = product;
        }

        @Override
        public String toString()
        {
            return "vendor=0x" + Hex.toHex(vendor).substring(4) + " product=0x" + Hex.toHex(product).substring(4);
        }
    }

    private String path;
    private ReadThread readThread;
    private WinNT.HANDLE writeHandle;

    private UsbLinux linux;

    public void open(Id id)
    {
        if (Platform.isWindows())
        {
            List<String> candidates = Scanner.findDevices(id.vendor, id.product);
            if (candidates.size() == 0)
                throw new RuntimeException("Failed to find device");
            if (candidates.size() > 1)
                throw new RuntimeException("Found more than one matching device");

            String path = candidates.get(0);

            this.path = path;

            writeHandle = Kernel32.INSTANCE.CreateFile(path, WinNT.GENERIC_WRITE, WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
                                                       null, WinNT.OPEN_EXISTING, 0, null);
            if (writeHandle == null)
            {
                throw new RuntimeException("error=" + Native.getLastError());
            }

            readThread = new ReadThread();
            while (readThread.status == 0)
            {
// need timeout
            }
        }
        else
        {
            UsbLinux linux = new UsbLinux();
            linux.open(id.vendor, id.product);
            this.linux = linux;
        }
    }

    public boolean isOpen()
    {
        if (Platform.isWindows())
        {
            if (readThread == null)
                return false;

            if (readThread.status == 2)
            {
                closeHandle(writeHandle);
                writeHandle = null;
                readThread = null;
                return false;
            }

            return true;
        }
        else
        {
            return (linux != null);
        }
    }

    public void setClosing()
    {
        if (Platform.isWindows())
        {
            if (readThread != null)
                readThread.closing = true;
        }
        else
        {
        }
    }

    public void close()
    {
        if (Platform.isWindows())
        {
            closeHandle(writeHandle);
            writeHandle = null;

            if (readThread == null)
                return;

            while (readThread.status != 2)
            {
// need timeout
            }
            readThread = null;
        }
        else
        {
            linux.close();
        }
    }

    public byte[] read(int endPoint, int length)
    {
        return read(endPoint, length, 1000);
    }

    public byte[] read(int endPoint, int length, int timeout)
    {
        if (Platform.isWindows())
        {
            while (true)
            {
                synchronized(readThread.inbound)
                {
                    if (readThread.inbound.size() == 0)
                        Toolbox.wait(readThread.inbound, timeout);

                    if (readThread.inbound.size() == 0)
                        throw new RuntimeException("Timeout");
                    if (readThread.inbound.size() > 1)
                        throw new RuntimeException("More than one response");
                    if (readThread.inbound.get(0).length != length)
                        throw new RuntimeException("Invalid length response");

                    return readThread.inbound.remove(0);
                }
            }
        }
        else
        {   byte[] response = linux.read(endPoint, length);
            if (response.length != length)
                throw new RuntimeException("Invalid length response");

            return response;
        }
    }

    public void write(int endPoint, byte[] request)
    {
        if (Platform.isWindows())
        {
            byte[] buffer = new byte[1 + request.length];
            System.arraycopy(request, 0, buffer, 1, request.length);

            IntByReference bytesWritten = new IntByReference();

            boolean resp = Kernel32.INSTANCE.WriteFile(writeHandle, buffer, buffer.length, bytesWritten, null);
            if ((resp == false) || (bytesWritten.getValue() != buffer.length))
            {
                throw new RuntimeException("error=" + Native.getLastError() + " bytes=" + bytesWritten.getValue());
            }
        }
        else
        {
            linux.write(endPoint, request);
        }
    }

    private class ReadThread implements Runnable
    {
        private volatile int status = 0;
        private volatile boolean closing = false;
        private List<byte[]> inbound = new ArrayList<byte[]>();

        private ReadThread()
        {
            start();
        }

        public void start()
        {
            Thread thread = new Thread(this);
            thread.setName("ReadThread");
            thread.start();
        }

        public void run()
        {
            WinNT.HANDLE readHandle = null;
            try
            {
                readHandle = Kernel32.INSTANCE.CreateFile(path, WinNT.GENERIC_READ, WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
                                                                null, WinNT.OPEN_EXISTING, 0, null);
                if (readHandle == null)
                {
                    throw new RuntimeException("error=" + Native.getLastError());
                }

                status = 1;

                Memory buffer = new Memory(1024);
                IntByReference bytesRead = new IntByReference();
                while (true)
                {
                    boolean resp = Kernel32.INSTANCE.ReadFile(readHandle, buffer, (int)buffer.size(), bytesRead, null);
                    if (resp == false)
                    {
                        if (closing)
                            break;
                        throw new RuntimeException("error=" + Native.getLastError() + " bytes=" + bytesRead.getValue());
                    }

                    if (bytesRead.getValue() < 2)
                        throw new RuntimeException("too few bytes ??");

                    synchronized(inbound)
                    {
                        // we have to lose the 1 byte prefix
                        byte[] data = new byte[bytesRead.getValue() - 1];
                        buffer.read(1, data, 0, data.length);
                        inbound.add(data);
                        inbound.notify();
                    }
                }
            }
            catch (Exception exception)
            {
                logger.info("", exception);
            }
            finally
            {
                status = 2;

                closeHandle(readHandle);
                readHandle = null;
                logger.info("read exit");
            }
        }
    }

    private static void closeHandle(WinNT.HANDLE handle)
    {
        if (handle != null)
            Kernel32.INSTANCE.CloseHandle(handle);
    }
}
