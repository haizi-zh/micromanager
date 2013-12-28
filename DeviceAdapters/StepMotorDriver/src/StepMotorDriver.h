#include <reg52.h>

#ifndef STEPMOTOR_20131219
#define STEPMOTOR_20131219


sbit _plusePort     		= P1^0;
sbit _directionPort 		= P1^1;
sbit _releasePort   		= P1^2;
sbit _highLimitPort  		= P1^3;
sbit _lowLimitPort 			= P1^4;
sbit _manualUpPort  		= P1^5;
sbit _manualDownPort  		= P1^6;
sbit _manualAcceleratePort  = P1^7;

#define uchar unsigned char  //0~255
#define uint unsigned int	 //0^65535
#define ulong unsigned long
#define bool bit
#define true 1
#define false 0


//error code

#define DEVICE_OK 0x01 +'I'-1
#define DEVICE_BUSY 0x02 +'J'-2
#define OUT_OF_LOW_LIMIT 0x03 +'K'-3
#define OUT_OF_HIGH_LIMIT 0x04 +'L'-4
#define CHECK_SUM_ERROR 0x05  +'M'-5
#define BAD_COMMAND	    0x0E +'N'-14

//command string
#define QueryPosition   0x06 +'A'-6
#define SetZeroPosition 0x07 +'B'-7
#define MoveUp	        0x08 +'C'-8
#define MoveDown	    0x09 +'D'-9
#define SetRunningDelay 0x0A +'E'-10
#define SetStartDelay 	0x0B +'F'-11	
#define FindLimit		0x0C +'G'-12
#define ReleasePower	0x0D +'H'-13


/*------------------------------------------------
                   º¯ÊýÉùÃ÷
------------------------------------------------*/
void parseCMD(uchar rec[]);
bool checksum(uchar rec[]);
uchar Move(ulong step,bit flag);
uchar SendPluse(ulong step);
uchar FindUpLimit(bit flag);
void refLCD();
bool InitDevice();
bool checkBoundary();
uchar checksumCalc(uchar rec[]);

void ltoa(ulong step,char* str);
void ManualMove(bit deriction,bit flag);
void delay(uchar interval);
void delay_ms(uchar xms);
#endif
