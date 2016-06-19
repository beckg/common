#include "Compiler.h"
#include "iocfg.h"

//#define GetSystemClock()        (16000000ul)            // Hz
//#define GetSystemClock()        (48000000ul)            // Hz
#define GetInstructionClock()   (GetSystemClock() / 4)
#define GetPeripheralClock()    GetInstructionClock()

#if defined(__18F2550_H)

// ENC28J60 I/O pins
//#define ENC_RST_TRIS            (TRISCbits.TRISC6)
//#define ENC_RST_IO              (LATCbits.LATC6)
//#define ENC_CS_TRIS             (TRISCbits.TRISC7)
//#define ENC_CS_IO               (LATCbits.LATC7)

//#define ENC_SCK_TRIS            (TRISBbits.TRISB0)
//#define ENC_SDI_TRIS            (TRISBbits.TRISB2)
//#define ENC_SDO_TRIS            (TRISBbits.TRISB1)

#define ENC_RST_TRIS            (TRISBbits.TRISB3)
#define ENC_RST_IO              (LATBbits.LATB3)
#define ENC_CS_TRIS             (TRISBbits.TRISB2)
#define ENC_CS_IO               (LATBbits.LATB2)

#define ENC_SCK_TRIS            (TRISBbits.TRISB1)
#define ENC_SDI_TRIS            (TRISBbits.TRISB0)
#define ENC_SDO_TRIS            (TRISCbits.TRISC7)

//#define ENC_SPI_IF              (PIR3bits.SSP2IF)
//#define ENC_SSPBUF              (SSP2BUF)
//#define ENC_SPISTAT             (SSP2STAT)
//#define ENC_SPISTATbits         (SSP2STATbits)
//#define ENC_SPICON1             (SSP2CON1)
//#define ENC_SPICON1bits         (SSP2CON1bits)
//#define ENC_SPICON2             (SSP2CON2)

#define ENC_SPI_IF              (PIR1bits.SSPIF)
#define ENC_SSPBUF              (SSPBUF)
#define ENC_SPISTAT             (SSPSTAT)
#define ENC_SPISTATbits         (SSPSTATbits)
#define ENC_SPICON1             (SSPCON1)
#define ENC_SPICON1bits         (SSPCON1bits)
#define ENC_SPICON2             (SSPCON2)

#define ADCON2          ADCON1

#endif

#if defined(__18F26J50_H)

    // ENC28J60 I/O pins

    #define ENC_SPI_SPEED MAC_SPI_SPEED

//    #define ENC_RST_TRIS            (TRISCbits.TRISC6)
//    #define ENC_RST_IO              (LATCbits.LATC6)
    #define ENC_CS_TRIS             (MAC_CS_TRIS)
    #define ENC_CS_IO               (MAC_CS_LAT)

    #define ENC_SCK_TRIS            (SCK_TRIS)
    #define ENC_SDI_TRIS            (SDI_TRIS)
    #define ENC_SDO_TRIS            (SDO_TRIS)

    #define ENC_SPI_IF              (PIR3bits.SSP2IF)
    #define ENC_SSPBUF              (SSP2BUF)
    #define ENC_SPISTAT             (SSP2STAT)
    #define ENC_SPISTATbits         (SSP2STATbits)
    #define ENC_SPICON1             (SSP2CON1)
    #define ENC_SPICON1bits         (SSP2CON1bits)
    #define ENC_SPICON2             (SSP2CON2)

    #define ADCON2          ADCON1
#endif
