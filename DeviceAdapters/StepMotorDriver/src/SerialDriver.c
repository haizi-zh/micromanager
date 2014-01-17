#include "SerialDriver.h"
/************************************************************

 ************************************************************/
void  InitSerial()
{

	SCON  = 0x50;//工作方式1，允许串行接收
	PCON =0x80;

	TMOD |= 0x20;//计时器一 方式二 自动重载初始值
 
	TH1   = 0xFD; //19200
 
	ET1 = 0;//禁止计数器引起中断
	TR1   = 1; //定时器1 开始工作
	RI = 0;
	TI = 0;
	ES    = 1;	 //开串口中断
	PS	  = 1 ;	 //串口中断高优先级
}                            
/************************************************************

 ************************************************************/
void SendByte(unsigned char dat)
{
	SBUF = dat;
	while(!TI);
	TI = 0;
}
/************************************************************

 ************************************************************/
void SendStr(unsigned char *s)
{
	SendByte('@');
	SendByte(s[0]);// 0OK 1NG
	SendByte(s[1]);
	SendByte(s[2]);
	SendByte(s[3]);
	SendByte(s[4]);
	SendByte( ((uchar)'@')^((uchar)s[0])^((uchar)s[1])^((uchar)s[2])^((uchar)s[3])^((uchar)s[4]));
}
void SendErr(unsigned char s)
{
	SendByte('@');
	SendByte(s);// 0OK 1NG
	SendByte('X');
	SendByte('X');
	SendByte('X');
	SendByte('X');
	SendByte( ((uchar)'@')^((uchar)s)^((uchar)'X')^((uchar)'X')^((uchar)'X')^((uchar)'X'));
}
