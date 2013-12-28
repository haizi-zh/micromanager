#include "StepMotorDriver.h"
#include "SerialDriver.h"
#include "1602LCDDriver.h" 
#include <stdlib.h>
#include <string.h>

/*------------------------------------------------
                   变量声明
------------------------------------------------*/
bool isBusy = false;
uchar	startdelay = 16;
uchar	runningdelay = 0; 
float   currPosition = 0;//nm
float	step2nm = 0.09969;//50 XIFEN
bit	    isSetZero = 0;	
uchar str[20];
uchar ret;


/************************************************************

 ************************************************************/
bool checksum(uchar rec[])//rec[] = @ C XXXX C
{
	if(rec[6] == checksumCalc(rec))
		return 1;
	else
		return 0;
}
uchar checksumCalc(uchar rec[])
{
	uchar checksum = rec[0];
	uchar i=1;
	for (i; i < 6; i++)
	{
		checksum = checksum ^ rec[i];
	}
	return checksum;
} 
void parseCMD(uchar rec[])
{	

	ulong recData = 0;
	char cmd = 0;
	ret = DEVICE_OK;
	if(checksum(rec) == 0){
		ret = CHECK_SUM_ERROR;
	}else{
		rec++;//skip @
		cmd = *rec;
		rec++;
		recData = *(ulong *)rec;
		switch(cmd){

		case QueryPosition:
			LCD_Printf1("CMD:QueryPosition");

			ltoa(currPosition,str);
			SendStr(str);
			return;
			break;
		case QueryStage:
			LCD_Printf1("CMD:QueryStage");
			SendStr("@DEADNIGHT");
			return;
			break;
			  
		case SetZeroPosition:
			LCD_Printf1("CMD:SetZero");
			currPosition = 0;
			break;

		case MoveUp:
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"--CMD:MOVEUP"));

			ret = Move(recData,0);
			break;

		case MoveDown:
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"--CMD:MOVEDOWN"));

			ret = Move(recData,1);
			break;

		case SetRunningDelay:
			runningdelay = recData;
			ltoa(runningdelay,str);
			LCD_Printf1(strcat(str,"--CMD:SetRunningDelay"));
			break;

		case SetStartDelay:
			startdelay = recData;
			ltoa(startdelay,str);
			LCD_Printf1(strcat(str,"--CMD:SetStartDelay"));
			break;

		case FindLimit:
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"--CMD:FindLimit"));
			FindUpLimit(recData);
			break;

		case ReleasePower:
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"--CMD:ReleasePower"));
			_releasePort = recData;
			break;
		default:
			ret = BAD_COMMAND;
			break;
		}
	}
	if(ret != DEVICE_OK){
		//ltoa(ret,str);
		str[0] = ret;
		str[1] = '\0';
		LCD_Printf1(strcat(str,"--ERROR!"));
		SendStr(str);
	}
	refLCD();
}
/************************************************************

 ************************************************************/
bool InitDevice()
{
	isBusy = false;
	return true;
}
/************************************************************

 ************************************************************/
uchar Move(ulong step,bit flag)
{

	if(isBusy == 1)
		return DEVICE_BUSY;

	if(flag == 0){// up	   
		_directionPort = 0;
		delay(100);
		return SendPluse(step);
	}else{		  // down
		_directionPort = 1;
		delay(100);
		return  SendPluse(step);
	}
}
/************************************************************

 ************************************************************/
uchar FindUpLimit(bit flag)
{
	if(flag ==1){
		while(_highLimitPort== 1)
		{
			ManualMove(0,1);
		}
		if(_highLimitPort== 0){
			currPosition = 0;
			isSetZero = 1;
			ltoa(0,str);
			LCD_Printf2(str);
		}
	}else{
		while(_lowLimitPort== 1)
		{
			ManualMove(1,1);
		}
		if(_lowLimitPort== 0){
			currPosition = 521557;
			isSetZero = 1;
			ltoa(521557,str);
			LCD_Printf2(str);
		}
	}
	return DEVICE_OK;
}
/************************************************************

 ************************************************************/
void ManualMove(bit deriction,bit flag)//deriction 1 up,0 down,flag 1 fast 0 low
{
	uchar _interval = 0;
	if(deriction ==0) 
		currPosition -= step2nm;
	else
		currPosition += step2nm;

	if(flag ==1)
		_interval = runningdelay;
	else
		_interval = startdelay; 

	_plusePort = 1;
	delay(_interval);
	_plusePort = 0;
	delay(_interval);

}
/************************************************************

 ************************************************************/
uchar SendPluse(ulong step)
{
	ulong i = 0,temp = 0,acturalStep = 0;;
	isBusy =1;
	if(step <=20){
		temp = step/2;
		for(i=0;i<temp && checkBoundary();i++){
			_plusePort = 1;
			delay(startdelay-i);
			acturalStep ++;
			_plusePort = 0;
			delay(startdelay-i);//加速
			
		}
		temp = step - temp;
		for(i=0;i<temp && checkBoundary();i++){
			_plusePort = 1;
			delay(startdelay-temp+i);
			acturalStep ++;
			_plusePort = 0;
			delay(startdelay-temp+i);//减速
			
		}

	}
	if(step>20){

		for(i=startdelay;i>runningdelay  && checkBoundary();i--){
			_plusePort = 1;
			delay(i);
			acturalStep ++;
			_plusePort = 0;
			delay(i);//加速			
		}
		temp = step - 2*(startdelay - runningdelay);
		for(i=0;i<temp && checkBoundary();i++){
			_plusePort = 1;
			delay(runningdelay);
			acturalStep ++;
			_plusePort = 0;
			delay(runningdelay);//加速
		}

		for(i=runningdelay;i<startdelay  && checkBoundary();i++){
			_plusePort = 1;
			delay(i);
			acturalStep ++;
			_plusePort = 0;
			delay(i);//减速
		}

	}

	if(_directionPort == 0){
		currPosition -= acturalStep;
	}else{
		currPosition += acturalStep;
	}
	isBusy =0;
	if(_lowLimitPort ==0)
		return OUT_OF_LOW_LIMIT;
	if(_highLimitPort ==0)
		return OUT_OF_HIGH_LIMIT;
	return DEVICE_OK;
}
bool checkBoundary()
{
	return (_directionPort  ==  1 &&_lowLimitPort == 1) || (_directionPort  ==  0 &&_highLimitPort== 1);
}

/************************************************************

 ************************************************************/
void delay(uchar _interval)
{
	uchar i=0;
	for(i;i<_interval;i++);	
}

/************************************************************

 ************************************************************/
void delay_ms(uchar xms) //ms级延时子程序
{ 
	uchar x,y; 
	for(x=xms;x>0;x--)
		for(y=130;y>0;y--);
}

void refLCD(  )
{ 
	ltoa(currPosition,str);
	LCD_Printf2(str);
}

/************************************************************

 ************************************************************/
void ltoa(ulong step,char* str)
{

	uchar i,j=1;
	if(step ==0){*str = '0';str++;*str = '\0';return;}
	if(step > 0){str++;*str = ',';}
	if(step >9){str++;*str = ',';}
	if(step >99){str++;*str = ',';}
	if(step >999){str++;*str = ',';str++;*str = ',';}
	if(step >9999){str++;*str = ',';}
	if(step >99999){str++;*str = ',';}
	if(step >999999){str++;*str = ',';str++;*str = ',';}
	if(step >9999999){str++;*str = ',';}
	if(step >99999999){str++;*str = ',';}
	if(step >999999999){str++;*str = ',';str++;*str = ',';}
	if(step >9999999999){str++;*str = ',';}
	if(step >99999999999){str++;*str = ',';}
	*str = '\0';

	while (step >0 )
	{	
		str--;
		i = step % 10;
		step /= 10;
		*str = i+'0';
		if(j % 3 == 0)str--;
		j++;
	}
}
