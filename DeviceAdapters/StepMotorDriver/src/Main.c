#include "SerialDriver.h"
#include "1602LCDDriver.h" 
#include "StepMotorDriver.h" 

main()
{	
 			
	TCON =0x08; 
	EX1 =  1;
	LCD_Initial();
	P1 = 0xff;
	LCD_Printf1("Device Init ok!");
	refLCD(  );
	InitSerial(); //Serial
	InitDevice(); //StepMotor
	SendStr("Device Init ok\r\n");
	
	while(1){}
}

/*------------------------------------------------
                     串口中断程序
------------------------------------------------*/
void serial () interrupt 4  
{ 
    unsigned char ch;
 	unsigned char rec[9];
	static unsigned char i=0;
	static unsigned char databegin = 0;
	if(RI) {	
	    RI=0;
		ch=SBUF;
		TI=1; //置SBUF空
		if(ch == '@'){//begin
			databegin = 1;	
		}
		if(databegin ==1){
			rec[i] = ch;
			i++;	
			if(i==8){
				
				parseCMD(rec);
	
				databegin = 0;
				i=0;
			}
		}
		TI=0;
	}
}

/*------------------------------------------------
                     外部中断程序
------------------------------------------------*/
void key_scan() interrupt 2 //使用了外部中断0的键盘扫描子函数
{ 
	uchar tick = 0;
	if(_manualUpPort==0) //有键按下吗？（k1=0 ?）
	{ 
		delay_ms(10); //延时消抖
		if(_manualUpPort==0)     //确实是有键按下，则：
		{
			_directionPort = 0;
				LCD_Printf1("MOVE UP [um]");
		 	if(_manualAcceleratePort ==  0){
		 
			    while(_manualUpPort == 0 &&  _highLimitPort== 1)
				{
				  	ManualMove(0,1);
					 
				}
				refLCD();
			}else{
		 
			    while(_manualUpPort == 0 &&  _highLimitPort== 1)
				{
				  	 ManualMove(0,0);
					 
					}
					refLCD();
				}
			}
		} //等待按键放开
	  


	if(_manualDownPort==0) //有键按下吗？（k1=0 ?）
	{ 
		delay_ms(10); //延时消抖
		if(_manualDownPort==0)     //确实是有键按下，则：
		{
			_directionPort = 1;
			LCD_Printf1("MOVE DOWN [um]");
		 	if(_manualAcceleratePort ==  0){
			
			    while(_manualDownPort  == 0 && _lowLimitPort == 1 )
				{
				  	ManualMove(1,1);
				}
					refLCD();
			}else{
		 
			    while(_manualDownPort  == 0 && _lowLimitPort == 1)
				{
				  	ManualMove(1,0);
				
				}
					refLCD();
				
			}
		} //等待按键放开
	} 
}
