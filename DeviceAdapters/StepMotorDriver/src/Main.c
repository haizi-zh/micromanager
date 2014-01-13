#include "SerialDriver.h"
#include "1602LCDDriver.h" 
#include "StepMotorDriver.h" 

	unsigned char ch;
	unsigned char rec[8];
	unsigned char i=0;
	bool databegin = 0;

main()
{
    EA = 1;//开总中断
    EX1 = 1 ;// 开外部中断1
	LCD_Initial();
	P1 = 0xff;
	LCD_Printf1("Device Init ok!");
	
	InitSerial(); //Serial
	InitDevice(); //StepMotor

	SendStr("Device Init ok\r\n");
	refLCD(  );
	while(1){}
}

/*------------------------------------------------
                     串口中断程序
------------------------------------------------*/
void serial () interrupt 4
{

	if(RI) {	
	
		ch=SBUF;
		
		
		if(ch == '@'){//begin
			databegin = 1;	
		}
	
		if(databegin ==1){
			rec[i] = ch;
			i++;	
			
			if(i==7){ 
			databegin = 0;
			i=0;
			rec[7] = '\0';
			parseCMD(rec);				
			}

		}
	 	RI=0;
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
