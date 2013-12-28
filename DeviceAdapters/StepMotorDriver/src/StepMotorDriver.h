#include <reg52.h>

#ifndef STEPMOTOR_20131219
#define STEPMOTOR_20131219


sbit _plusePort     		= P1^0;
sbit _directionPort 		= P1^1;
sbit _releasePort   		= P1^2;
sbit _highLimitPort  		= P1^3;
sbit _lowLimitPort 		= P1^4;
sbit _manualUpPort  		= P1^5;
sbit _manualDownPort  		= P1^6;
sbit _manualAcceleratePort  = P1^7;
 
#define uchar unsigned char  //0~255
#define uint unsigned int	 //0^65535
#define ulong unsigned long
#define bool bit
#define true 1
#define false 0

#define DEVICE_OK 1
#define DEVICE_BUSY 2
#define OUT_OF_LOW_LIMIT 3
#define OUT_OF_HIGH_LIMIT 4

/*------------------------------------------------
                   º¯ÊýÉùÃ÷
------------------------------------------------*/
void parseCMD(uchar rec[]);
uchar Move(ulong step,bit flag);
uchar SendPluse(ulong step);
uchar FindUpLimit(bit flag);
void refLCD();
bool InitDevice();
bool checkBoundary();


void ltoa(ulong step,char* str);
void ManualMove(bit deriction,bit flag);
void delay(uchar interval);
void delay_ms(uchar xms);
#endif