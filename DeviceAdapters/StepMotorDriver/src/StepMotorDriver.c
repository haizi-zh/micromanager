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
void parseCMD(uchar rec[])
{	
	ulong step = 0;
	ret = 99;
	rec++;//skip @	
	if( 0 == memcmp(rec,"QP",2) ){
		LCD_Printf1("--Rec 'QP'");
		ltoa(currPosition,str);
		SendStr(str);
		ret = DEVICE_OK;
	}
	if( 0 == memcmp(rec,"SZ",2) ){
		LCD_Printf1("--Rec 'SZ'");
	    currPosition = 0;
		ret = DEVICE_OK;
	}
	if( 0 == memcmp(rec,"MU",2) ){
		rec += 2;
		step = *(ulong *)rec;
		ltoa(step,str);
		LCD_Printf1(strcat(str,"--Rec 'MU'"));
		ret = Move(step,0);
	}
	if(0 == memcmp(rec,"MD",2)){
		rec += 2;
		step = *(ulong *)rec;
		ltoa(step,str);
		LCD_Printf1("Rec 'MD'");
		ret = Move(step,1);
	}
	if(0 == memcmp(rec,"SR",2)){
	rec += 2;
		runningdelay = *(ulong *)rec;
		ltoa(runningdelay,str);
			LCD_Printf1("Rec 'SR'");
		ret = DEVICE_OK;
	}
	if(0 == memcmp(rec,"SS",2)){
		
		rec += 2;
		startdelay = *(ulong *)rec;
		ltoa(startdelay,str);
			LCD_Printf1("Rec 'SS'");
			ret = DEVICE_OK;
	}
	if(0 == memcmp(rec,"FL",2)){
			rec += 2;
		step = *(ulong *)rec;
		ltoa(step,str);
		LCD_Printf1("Rec 'FL'");
		LCD_Printf2(str);
		FindUpLimit(step);
		
		ret = DEVICE_OK;
	}
	 
	if(0 == memcmp(rec,"RE",2)){
		rec += 2;
		step = *(ulong *)rec;
		if(step == 1)
			_releasePort = 1;
		else
			_releasePort = 0;
		ltoa(step,str);
		LCD_Printf1("Rec 'RE'");
		LCD_Printf2(str);
		ret = DEVICE_OK;
	}
//	if(ret == DEVICE_OK){
	//	SendStr("DEVICE_OK");
	//	LCD_Printf2(str);
//	}
	if(ret == 99){
		SendStr("BAD_COMMAND");
		LCD_Printf1("ERR!BAD_COMMAND");
	}
	if(ret == DEVICE_BUSY){
		SendStr("DEVICE_BUSY");
		LCD_Printf1("ERR!DEVICE_BUSY");
	}
	if(ret == OUT_OF_LOW_LIMIT){
		SendStr("OUT_OF_LOW_LIMIT");
		LCD_Printf1("ERR!OUT_LOW_LIMIT");
	}
	if(ret == OUT_OF_HIGH_LIMIT){
		SendStr("OUT_OF_HIGH_LIMIT");
		LCD_Printf1("ERR!OUT_HIGH_LIMIT");
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
	ulong i = 0,middle = 0,rest=0,acturalStep = 0;;
	isBusy =1;
	if(step <=20){
		middle = step/2;
		rest = step - middle;

		for(i=0;i<middle && checkBoundary();i++){
			_plusePort = 1;
			delay(startdelay-i);
			_plusePort = 0;
			delay(startdelay-i);//加速
			acturalStep ++;
		}
		for(i=0;i<rest && checkBoundary();i++){
			_plusePort = 1;
			delay(startdelay-middle+i);
			_plusePort = 0;
			delay(startdelay-middle+i);//减速
			acturalStep ++;
		}

	}
	if(step>20){

		for(i=startdelay;i>runningdelay  && checkBoundary();i--){
			_plusePort = 1;
			delay(i);
			_plusePort = 0;
			delay(i);//加速
			acturalStep ++;
		}
		rest = step - 2*(startdelay - runningdelay);

		for(i=0;i<rest && checkBoundary();i++){
			_plusePort = 1;
			delay(runningdelay);
			_plusePort = 0;
			delay(runningdelay);//加速
			acturalStep ++;
		}

		for(i=runningdelay;i<startdelay  && checkBoundary();i++){
			_plusePort = 1;
			delay(i);
			_plusePort = 0;
			delay(i);//减速
			acturalStep ++;
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
