#include "StepMotorDriver.h"
#include "SerialDriver.h"
#include "1602LCDDriver.h" 
#include <stdlib.h>
#include <string.h>

/*------------------------------------------------
                   变量声明
------------------------------------------------*/
bool isBusy = false;
uchar	startdelay = 32;
uchar	runningdelay = 0; 
float   currPosition = 0;//nm
float	step2Um = 0.49827043;//50 XIFEN
bit	    isSetZero = 0;	
uchar str[12];
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
	rec[5] = 0xFF;
	break;
	case MoveDown:
	rec[5] = 0xFF;
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
	return ((uchar)rec[0])^((uchar)rec[1])^((uchar)rec[2])^((uchar)rec[3])^((uchar)rec[4])^((uchar)rec[5]);
}
 
void parseCMD(uchar rec[])
{	

	ulong recData = 0;
	char cmd = 0;
	ret = DEVICE_OK;
	//debug(rec);
	if( 0 == checksum(rec) ){
		ret = CHECK_SUM_ERROR;
	}else{
		rec++;//skip @
		cmd = *rec;
		rec++;
		recData = *(ulong *)rec;
		switch(cmd){

		case QueryPosition:
			longToRaw(currPosition,str);
			str[0] = 0;	
			SendStr(str);
			return;
			break;
		case SetPosition:
		    
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"-SP"));
	 	    ret =  SetStagePosition(recData);
			break;

		case QueryStage:	
			SendStr("DEADN");
			return;
			break;
		case SetUM2Step:
			step2Um = *(float *)rec;
			ltoa(step2Um*1000,str);
			LCD_Printf1(strcat(str,"-ST"));
			rec = DEVICE_OK;
			break;	  
		case SetZeroPosition:
			currPosition = 0;
			break;

		case MoveUp:
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"-MU"));
			ret = Move(recData,0);
			break;

		case MoveDown:
			ltoa(recData,str);
			LCD_Printf1(strcat(str,"-MD"));
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

	SendErr(ret);
	str[0]  = ret;
	str[1] = '\0';
	if(ret != DEVICE_OK){		
	LCD_Printf1(strcat(str,"--ERROR!"));		
	}

	refLCD();
	
}
/************************************************************

 ************************************************************/
bool InitDevice()
{
	isBusy = false;
	currPosition = 0;   
	FindUpLimit(0);			
	return true;
}
uchar SetStagePosition(ulong pos)
{
	if(pos > currPosition){
	  return Move((pos - currPosition)/step2Um,0);	//up
	}else{
	  return Move((currPosition - pos )/step2Um,1);//down
	}  
	 
}
/************************************************************

 ************************************************************/
uchar Move(ulong step,bit flag)
{

	if(isBusy == 1)
		return DEVICE_BUSY;

	_directionPort = flag;
	delay(100);
	return SendPluse(step);

}
/************************************************************

 ************************************************************/
uchar FindUpLimit(bit flag)
{
	if(flag == 0){
	_directionPort = 0;
	LCD_Printf1("FindUpLimit");	
	while(_highLimitPort == 1)
	{
		ManualMove(0,1);//up fast
	}
	currPosition =  52000;	
	}
 if(flag == 1){
	_directionPort = 1;
		LCD_Printf1("FindlowLimit");	
		while(_lowLimitPort== 1)
		{
			ManualMove(1,1);
		}
	currPosition = 0;
	}
	ltoa(currPosition,str);
	LCD_Printf2(str);
	refLCD();
	return DEVICE_OK;
}
/************************************************************

 ************************************************************/
void ManualMove(bit deriction,bit flag)//deriction 0 up,1 down,flag 1 fast 0 low
{

	uchar _interval = 0;
 
	if(deriction ==0) 
		currPosition += step2Um;
	else{
		currPosition -= step2Um;
		if(currPosition >24294967)
			currPosition = 0;
	}

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
	ulong i = 0,temp = 0,acturalStep = 0;
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
		currPosition += acturalStep*step2Um;
	}else{
		currPosition -= acturalStep*step2Um;
	}
	ltoa(currPosition,str);
	LCD_Printf1("ZPos:[um]");

	isBusy =0;

	if(_lowLimitPort ==0)
		return OUT_OF_LOW_LIMIT;
	if(_highLimitPort ==0)
		return OUT_OF_HIGH_LIMIT;

	return DEVICE_OK;
}
bool checkBoundary()
{
	return (currPosition >=0) && ( _directionPort  ==  0 &&_highLimitPort == 1) || (currPosition>0 &&_directionPort  ==  1 && _lowLimitPort== 1);
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
void ltoa(ulong step,uchar* str)
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

/************************************************************

 ************************************************************/
void longToRaw(ulong step,uchar* str)
{
	str[4] =  step%256;
	step /= 256;
	str[3] =  step%256;
	step /= 256;
	str[2] =  step%256;
	step /= 256;
	str[1] =  step%256;
	 
}
