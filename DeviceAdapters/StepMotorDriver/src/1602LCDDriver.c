/************************************************************

 ************************************************************/

#include"1602LCDDriver.h"   

/************************************************************

 ************************************************************/
void LCD_Initial()
{
	LCD_DelayTime(15);
	LCD_Write(LCD_Command, 0x38);              //设置显示模式，8位数据端口,2行显示,5*7点阵
	LCD_DelayTime(5);
	LCD_Write(LCD_Command, 0x38);
	LCD_DelayTime(5);
	LCD_Write(LCD_Command, 0x38);
	LCD_DelayTime(4);         //检测忙信号（Proteus仿真用）
	//void LCD_BusyCheck();        //检测忙信号（实际硬件电路用）
	LCD_Write(LCD_Command, 0x38);
	LCD_DelayTime(4);
	//void LCD_BusyCheck();
	LCD_Write(LCD_Command, LCD_CLOSE);
	LCD_DelayTime(4);
	//void LCD_BusyCheck();
	LCD_Write(LCD_Command, LCD_CLEAR_SCREEN);
	LCD_DelayTime(4);
	//void LCD_BusyCheck();
	LCD_Write(LCD_Command, LCD_SHOW_ON | LCD_CURSOR_OFF);

}

void LCD_Clear()
{
	LCD_Write(LCD_Command, LCD_CLEAR_SCREEN);
	LCD_DelayTime(4);
}
/************************************************************

 ************************************************************/
void LCD_Write(bit bRS, unsigned char ucPort)
{
	LCDEn = 0;
	LCDRS = bRS;
	LCDRW = 0;
	_nop_();
	DBPort = ucPort;
	_nop_();
	_nop_();
	LCDEn = 1;
	_nop_();
	_nop_();
	_nop_();
	LCDEn = 0;
	_nop_();
	//void LCD_BusyCheck();
	LCD_DelayTime(3);
}


/************************************************************

 ************************************************************/
void LCD_CoordinateXY(unsigned char X, unsigned char Y)
{
	if(Y == 0)
	{
		LCD_Write(LCD_Command, 0x80 | X);
	}
	else if(Y == 1)
	{
		LCD_Write(LCD_Command, 0x80 | (X+0x40));
	}
	else
	{
		LCD_Write(LCD_Command, 0x80);
		LCD_Printf1("Error XY!");
	}
}

/************************************************************

 ************************************************************/
void LCD_Printf0(unsigned char *ucStr)
{
	unsigned char i=0 ;
	LCD_Clear();
	LCD_CoordinateXY(0,0);
	while(*ucStr != '\0')
	{
		LCD_Write(LCD_Data, *ucStr);
		ucStr++;
		i++;
		if(i==15){
			LCD_CoordinateXY(0,1);
		}
	}
}
/************************************************************

 ************************************************************/
void LCD_Printf1(unsigned char *ucStr)
{
	unsigned char i=0;
	LCD_CoordinateXY(0,0);
	for(i=0;i<16;i++)LCD_Write(LCD_Data,' ');
	LCD_CoordinateXY(0,0);
	while(*ucStr != '\0')
	{
		LCD_Write(LCD_Data, *ucStr);
		ucStr++;
	}
}

/************************************************************

 ************************************************************/
void LCD_Printf2(unsigned char *ucStr)
{
	unsigned char i=0;
	LCD_CoordinateXY(0,1);
	for(i=0;i<16;i++)LCD_Write(LCD_Data,' ');
	LCD_CoordinateXY(0,1);
	while(*ucStr != '\0')
	{
		LCD_Write(LCD_Data, *ucStr);
		ucStr++;
	}
}


/************************************************************

 ************************************************************/
void LCD_DelayTime(unsigned char ucCount)
{
	unsigned char i,j;
	for(i=0;i<ucCount;i++)
	{
		for(j=0;j<125;j++);
	}
}

/************************************************************

 ************************************************************/
/* 
void LCD_BusyCheck()   
{
    LCDRS = 0;
 LCDRW = 1;
 _nop_();
 LCDEn = 1;
 _nop_();
 while(DBPort & 0x80);//RS=0,RW=1， BF=1 busy
 _nop_();
 LCDEn = 0;
}

 */
