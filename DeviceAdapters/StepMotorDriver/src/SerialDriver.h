#include<reg52.h> 

#ifndef COM_20131219
#define COM_20131219


/*------------------------------------------------

------------------------------------------------*/
void InitSerial();
void SendStr(unsigned char *s);
void SendByte(unsigned char dat);
#endif