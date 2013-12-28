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
char str[20];
uchar ret;


/************************************************************

 ************************************************************/
void parseCMD(uchar rec[])
{	
	ulong step = 0;
	ret = 99;	
	if(rec[1] == 'M' && rec[2] == 'U'){
		step = *((ulong *)(rec+3));
		ltoa(step,str);
		LCD_Printf1("Rec 'MU'");
		ret = Move(step,0);
	}
	if(rec[1] == 'M' && rec[2] == 'D'){
		step =  *((ulong *)(rec+3));
		ltoa(step,str);
		LCD_Printf1("Rec 'MD'");
		ret = Move(step,1);
	}
	if(rec[1] == 'S' && rec[2] == 'R'){
		runningdelay =  *((ulong *)(rec+3));
		ltoa(runningdelay,str);
			LCD_Printf1("Rec 'SR'");
		ret = DEVICE_OK;
	}
	if(rec[1] == 'S' && rec[2] == 'S'){
		startdelay =  *((ulong *)(rec+3));
		ltoa(startdelay,str);
			LCD_Printf1("Rec 'SS'");
			ret = DEVICE_OK;
	}
	if(rec[1] == 'F' && rec[2] == 'L'){
		step =  *((ulong *)(rec+3));
		FindUpLimit(step);
		ltoa(step,str);
		LCD_Printf1("Rec 'FL'");
		LCD_Printf2(str);
		ret = DEVICE_OK;
	}
	 
	if(rec[1] == 'R' && rec[2] == 'E'){
		step =  *((ulong *)(rec+3));
		if(step == 1)
			_releasePort = 1;
		else
			_releasePort = 0;
		ltoa(step,str);
		LCD_Printf1("Rec 'RE'");
		LCD_Printf2(str);
		ret = DEVICE_OK;
	}
	if(ret == DEVICE_OK){
		SendStr("DEVICE_OK");
		LCD_Printf2(str);
	}
	if(ret == 99){
		SendStr("BAD_COMMAND");
		LCD_Printf1("ERROR!BAD_COMMAND");
	}
	if(ret == DEVICE_BUSY){
		SendStr("DEVICE_BUSY");
		LCD_Printf1("ERROR!DEVICE_BUSY");
	}
	if(ret == OUT_OF_LOW_LIMIT){
		SendStr("OUT_OF_LOW_LIMIT");
		LCD_Printf1("ERROR!OUT_OF_LOW_LIMIT");
	}
	if(ret == OUT_OF_HIGH_LIMIT){
		SendStr("OUT_OF_HIGH_LIMIT");
		LCD_Printf1("ERROR!OUT_OF_HIGH_LIMIT");
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

		for(i=0;i<middle && ((_directionPort  ==  0 &&_lowLimitPort == 1) || (_directionPort  ==  1 &&_highLimitPort== 1));i++){
			_plusePort = 1;
			delay(startdelay-i);
			_plusePort = 0;
			delay(startdelay-i);//加速
			acturalStep ++;
		}
		for(i=0;i<rest && ((_directionPort  ==  0 &&_lowLimitPort == 1) || (_directionPort  ==  1 &&_highLimitPort== 1));i++){
			_plusePort = 1;
			delay(startdelay-middle+i);
			_plusePort = 0;
			delay(startdelay-middle+i);//减速
			acturalStep ++;
		}

	}
	if(step>20){

		for(i=startdelay;i>runningdelay  &&((_directionPort  ==  0 &&_lowLimitPort == 1) || (_directionPort  ==  1 &&_highLimitPort== 1));i--){
			_plusePort = 1;
			delay(i);
			_plusePort = 0;
			delay(i);//加速
			acturalStep ++;
		}
		rest = step - 2*(startdelay - runningdelay);

		for(i=0;i<rest &&((_directionPort  ==  0 &&_lowLimitPort == 1) || (_directionPort  ==  1 &&_highLimitPort== 1));i++){
			_plusePort = 1;
			delay(runningdelay);
			_plusePort = 0;
			delay(runningdelay);//加速
			acturalStep ++;
		}

		for(i=runningdelay;i<startdelay  &&((_directionPort  ==  0 &&_lowLimitPort == 1) || (_directionPort  ==  1 &&_highLimitPort== 1));i++){
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

/************************************************************

 ************************************************************/
void delay(uchar _interval)
{
	uchar i=0;
	for(i;i<_interval;i++);	
}

/************************************************************

 ************************************************************/
void delay_ms(unsigned int xms) //ms级延时子程序
{ unsigned int x,y; 
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
 
 	if(step ==0){
		*str  = '0';
		str++;
		*str  = '\0';
		return;
	}
/*	if(step >N9){
		*str = step/T10; // 取第十位
		*str += '0';
		str++;
		*str  = ',';
		str++;
		
		step   %= T10;
		*str = step/T9;
		*str += '0';
		str++;
		step   %= T9;
		*str = step/T8;
		*str += '0';
		str++;
		step   %= T8;
		*str = step/T7;
		*str += '0';
		str++;
		step   %= T7;

		*str  = ',';
		str++;
		*str = step/T6;
		*str += '0';
		str++;
		step   %= T6;
		*str = step/T5;
		*str += '0';
		str++;
		step   %= T5;
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		return; 
	}
	if(step >N8){
		*str = step/T9;
		*str += '0';
		str++;
		step   %= T9;
		*str = step/T8;
		*str += '0';
		str++;
		step   %= T8;
		*str = step/T7;
		*str += '0';
		str++;
		step   %= T7;

		*str  = ',';
		str++;
		*str = step/T6;
		*str += '0';
		str++;
		step   %= T6;
		*str = step/T5;
		*str += '0';
		str++;
		step   %= T5;
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		return; 
	}
	if(step >N7){
		*str = step/T8;
		*str += '0';
		str++;
		step   %= T8;
		*str = step/T7;
		*str += '0';
		str++;
		step   %= T7;

		*str  = ',';
		str++;
		*str = step/T6;
		*str += '0';
		str++;
		step   %= T6;
		*str = step/T5;
		*str += '0';
		str++;
		step   %= T5;
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		return; 
	}
	if(step >N6){
		*str = step/T7;
		*str += '0';
		str++;
		step   %= T7;

		*str  = ',';
		str++;
		*str = step/T6;
		*str += '0';
		str++;
		step   %= T6;
		*str = step/T5;
		*str += '0';
		str++;
		step   %= T5;
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
	return;	 
	}	*/
	if(step >N5){
		*str = step/T6;
		*str += '0';
		str++;
		step   %= T6;
		*str = step/T5;
		*str += '0';
		str++;
		step   %= T5;
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		return; 
	}
	if(step >N4){
		*str = step/T5;
		*str += '0';
		str++;
		step   %= T5;
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		return; 
	}
		if(step >N3){
		*str = step/T4;
		*str += '0';
		str++;
		step   %= T4;

		*str  = ',';
		str++;

		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		return; 
	}
	if(step >N2){
		*str = step/T3;
		*str += '0';
		str++;
		step   %= T3;
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
		 return;
	}	
	if(step >N1){
		*str = step/T2;
		*str += '0';
		str++;
		step   %= T2;
		*str = step/T1;
		*str += '0';
		str++;
		*str = '\0';
	return;	 
	}
	if(step >0){
		*str = step;
		*str += '0';
		str++;
		*str = '\0';
		 
	}	
}
