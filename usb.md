<b>USB for PIC</b>
I have found a USB interface very useful on boards with PIC processors, partly for in-circuit 
programming using USB (so much easier than serial), partly as a debugging tool, and 
partly because the final product can talk to the outside world. It takes little space and normally works 
well if you take a little care over the D+ and D- traces.
<br><br>
I wanted to use the PIC16F1455 and its USB module, but then found that I had to use xc8, and that 
would not work with the USB boot-code, and ... I also saw that other people were having the same problem. 
I developed a working solution. Not sure if it is still of interest to anyone but I thought that I would 
publish in any event.
<br><br>

<b>USB basics</b>
<br><br>
This will be familiar and obvious to many people, but it took me a while to get my head around it so this might be
of use to other beginners. It is a simplification but adequate for my purpose. I may have some things wrong
but hopefully nothing material. 
<br><br>
The host is always in control. The device cannot initiate anything. The host converses with the device in 
transactions. Each transaction has  SETUP, DATA and STATUS phases, each phase ends with a handshake. The host 
sends a SETUP message to the device. The message can be generic, it can be specific to the device type; or
specific to the device - it does not matter. The device has two choices it can accept the request, or it 
can reject the request. The SETUP phase may prepare for a DATA phase (either host->device OUT or device->host IN)
or there may not be a DATA phase. 
<br><br>
If the device accepts the message, then it prepares for the DATA phase (if any) If the message is accepted
and there is a DATA phase, then the data is exchanged. The device and host will then exchange a STATUS message.
<br><br>
Each phase SETUP/DATA/STATUS is accompanied by a handshake: STALL - the phase was rejected, NAK - no data or error, 
or ACK - success. 
<br><br>
A device can have one or more communication channels (end-points). Each channel is independent of the other. The PIC
code that we will be using defines two, EP0 the (mandatory) default and EP1. 
<br><br>
Now the problem is that in the pipe between the host-application and the device-application, different parts of this
interaction are handled by different elements depending on the platforms involved.
<br><br>

<b>Windows</b>
<br><br>
Windows will inspect the device. It uses requests (from the generic set) to read the configuration from
the device. It will find that the device is a HID device and load a default HID driver. I could get it to
load a different driver but that is difficult and not necessary for my requirements. The HID device
driver wants to exchange data in "reports". The configuration defines the available reports. In our case 
we have a single (default) report whose id is 0.
<br><br>
Windows will schedule requests to the device. Every 1ms it will perform a transaction with each end-point
in the device. The device will either return STALL, in which case Windows does nothing, or return data.
If the application has opened a file handle, and there is buffer space, then Windows will add the reportId (0) 
and the data to the buffer otherwise it will discard it. The host application can read the data. Notice that 
the application must read the data "quickly enough".
<br><br>
The host application can write reportId (0) and data to the file handle. Windows will send the data to the 
device in a transaction. In this case the application must write "slowly enough".
<br><br>
There is a more complicated API, but the above is adequate for our purposes as long as we understand the 
restrictions.
<br><br>

<b>Linix</b>
<br><br>
Linux will inspect the device. It uses requests (from the generic set) to read the configuration from
the device. It will find that the device is a HID device and load a default HID driver.
<br><br>
We will unload this driver leaving the device naked. There are permissioning issues, but there is a standard 
solution to overcoming these, and we need to overcome them regardless of how we choose to talk to the device.
<br><br>
Using the libusb-1.0 API we can then perform transactions with the device. We are reponsible for scheduling
requests but we have access to the full set of USB APIs.
<br><br>

<b>PIC</b>
<br><br>
The PIC device can operate the end-points in two modes. 
In the first the module stores the packet in shared memory and signals to the host application that a SETUP
has arrived. The application inspects the packet and depending on the request STALLs or ACKs the phase. It 
updates its state. If it is going to ACK it may store response data in shared memory. It then instructs
the module to complete the transfer.
<br><br>
In the second mode the module handles the transaction automatically. If this is an OUT transaction, then it stores 
the incoming data in the available buffer. If this is an IN transaction, then it takes data from the prepared
buffer. In either case, if the buffer is not in the correct state (busy or empty) then it will return STALL.
The application tests the status of the buffers to know if it can send more data or if data has arrived.
<br><br>
The MicroChip boot code uses the first technique for EP0 and the second for EP1.
<br><br>

<b>USB code</b>
<br><br>
I took the basic HID bootloader example code in the MicrochipSolutions library and refactored it for the new include 
files (GenericTypeDefs.h etc) and hence to use the new data typedefs (byte->UINT8 etc). I added some callbacks to 
reduce the use of #defines, and fixed up the memory allocations.
<br><br>
I have made this available in the src/usb and include/usb directories.
I do not think that this re-publish breaks any agreement. All of the existing license information remains intact.
<br><br>
See the BootLoader repository for building and use.
<br><br>

The code needs JavaNativeAccess (jna) to compile - we use jna to invoke the platform specific APIs.
I jave checked in a version of jna but you should really get a version from git.
<a href="https://github.com/twall/jna">jna</a> 
Refer to the git respository for more details. The authors have done a wonderful job. 
<br><br>

<b>Accessing the USB device - Windows</b>
<br><br>
If you look in the Hid.java class you will see how we build up our own view of the hid device for Windows.
We first search for the device accessing standard Windows functions using the JNA API. Notice that we are
searching by vendor/product - this is defined in the core code in usbdsc.c, 0x04d8:0x0041. 
<pre>
    List<String> candidates = Scanner.findDevices(id.vendor, id.product);
</pre>
We open a handle to this device. We start a read thread - remember from the previous discussion this will 
read nothing until the device responses to an IN SETUP request with a DATA phase.
<br><br>
We can write() a request, and read() the response; notice that read() waits for a response and might timeout.
<br><br>
Notice that I do not have complete control - I cannot terminate the ReadFile() call. This will only happen 
when the device drops off the bus - unplugged or USBReset().
<br><br>
<b>Accessing the USB device - Linux</b>
<br><br>
We have to get access to the device. The simplest way is using udev rules. You need to have root access the
first time that you set this up. This is for Fedora, other builds might have slightly different locations.
<br><br>
Create or modify a custom rule file, say "/etc/udev/rules.d/10-usb-custom.rules". Add the following line.
<pre>
    SUBSYSTEM=="usb", ATTR{idVendor}=="04d8", ATTR{idProduct}=="0041" SYMLINK+="pic41", GROUP="pic"
</pre>
    Add yourself to the pic group
<pre>
    usermod +G pic gordon
</pre>
If you look in the  Hid.java class you will see how we build up our own view of the hid device for 
Linux - we leverage UsbLinux.java. We access standard libusb-1.0 functions using the JNA API. We first
search for the device, once we find it we detach it from the hid driver - we have permission because it 
is in the pic group and so are we; and then open a handle. We can now read() and write().
<br><br>
Notice that we use an interrupt transfer to a specific endpoint, in this case 1, for the read and write.
I did not talk about control/interrupt/bulk as I struggle to see any difference - I can use one or the other.
<br><br>
Look at the testn() methods. It is useful to see that we can call the generic and device-type API using 
libusb-1.0 - we can explore how the PIC firmware interacts with all the calls.
