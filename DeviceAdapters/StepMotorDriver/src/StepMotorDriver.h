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

#define DEVICE_OK 0x00  
#define DEVICE_BUSY 0x02 +'J'-2
#define OUT_OF_LOW_LIMIT 0x03 +'Z'-3
#define OUT_OF_HIGH_LIMIT 0x04 +'L'-4
#define CHECK_SUM_ERROR 0x05  +'M'-5
#define BAD_COMMAND	    0x06 +'N'-6

//command string
#define SetZeroPosition 0x07 +'Z'-7
#define MoveUp	        0x08 +'U'-8
#define MoveDown	    0x09 +'D'-9
#define SetRunningDelay 0x0A +'R'-10
#define SetStartDelay 	0x0B +'S'-11	
#define FindLimit		0x0C +'L'-12
#define ReleasePower	0x0D +'P'-13
#define QueryPosition   0x0E +'Q'-14
#define QueryStage   	0x0F +'E'-15
#define SetPosition	    0x10 + 'T' - 16
#define SetUM2Step	    0x11 + 'M'-17

/*------------------------------------------------
                   º¯ÊýÉùÃ÷
------------------------------------------------*/
void parseCMD(uchar rec[]);
bool checksum(uchar rec[]);
uchar Move(ulong step,bit flag);
uchar SetStagePosition(ulong step);
uchar SendPluse(ulong step);
uchar FindUpLimit(bit flag);
void refLCD();
bool InitDevice();
bool checkBoundary();
uchar checksumCalc(uchar rec[]);

void ltoa(ulong step,uchar* str);
void longToRaw(ulong step,uchar* str);
void ManualMove(bit deriction,bit flag);
void delay(uchar interval);
void delay_ms(uchar xms);

void debug(uchar rec[]);
#endif
