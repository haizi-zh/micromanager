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


void debug(char rec[])
{
	uchar d = 0x00;
	rec[2] = 0x00;
	rec[3] = 0x00;
	rec[4] = 0x00;
	rec[5] = 0x00;

	switch(rec[1]){
	case SetZeroPosition:
	break;
	case MoveUp:
	rec[5] = 0x0A;
	break;
	case MoveDown:
	rec[5] = 0x0f;
	break;
	case SetRunningDelay:
	rec[5] = 0x00;
	break;
	case SetStartDelay:
	rec[5] = 0x10;
	break;
	case FindLimit:
	rec[5] = d;
	break;
	case ReleasePower:
	rec[5] = d;
	break;
	case SetPosition:
	rec[5] = 0x10;
	break;
	case SetUM2Step:
	rec[5] = 0x01;
	rec[4] = 0x00;
	rec[3] = 0x01;
	rec[2] = 0x00;
	break;
	}
	rec[6] = checksumCalc(rec);
}
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
	debug(rec);
	if(checksum(rec) == 0){
		ret = CHECK_SUM_ERROR;
	}else{
		rec++;//skip @
		cmd = *rec;
		rec++;
		recData = *(ulong *)rec;
		switch(cmd){

		case QueryPosition:
			ltoa(currPosition,str);	
			SendStr(str);
			SendByte('E');
			return;
			break;
		case SetPosition:
			ret = SetStagePosition(recData);
			break;

		case QueryStage:
			SendStr("@DEADNIGHT");
			return;
			break;
		case SetUM2Step:
			step2nm = *(float *)rec;
			rec = DEVICE_OK;
			break;	  
		case SetZeroPosition:
			currPosition = 0;
			break;

		case MoveUp:
			ret = Move(recData,0);
			break;

		case MoveDown:
			ret = Move(recData,1);
			break;

		case SetRunningDelay:
			runningdelay = recData;
			break;

		case SetStartDelay:
			startdelay = recData;
			break;

		case FindLimit:
			FindUpLimit(recData);
			break;

		case ReleasePower:
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
uchar SetStagePosition(ulong pos)
{
	if(pos > currPosition){
	  Move((pos - currPosition)/step2nm,1);
	}else{
	  Move((currPosition - pos )/step2nm,1);
	}
	return DEVICE_OK;
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
	refLCD();
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
		currPosition -= acturalStep*step2nm;
	}else{
		currPosition += acturalStep*step2nm;
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
