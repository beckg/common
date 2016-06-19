#include "iocfg.h"

#include "USB/usb.h"
#include "USB/usb_function_hid.h"

#if defined(USB_ACTIVE)
#else

void USBCBSuspend(void)
{
}

void USBCBWakeFromSuspend(void)
{
}

void USBCB_SOF_Handler(void)
{
}

void USBCBErrorHandler(void)
{
}

void USBCBCheckOtherReq(void)
{
}

void USBCBStdSetDscHandler(void)
{
}

void USBCBInitEP(void)
{
}

void USBCBSendResume(void)
{
}

BOOL USER_USB_CALLBACK_EVENT_HANDLER(USB_EVENT event, void *pdata, WORD size)
{
}
#endif
