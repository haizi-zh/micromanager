// Operater.cpp

#include "stdafx.h"
#include "Operater.h"
#include <stdexcept>
#include <cassert>
#include <iostream>
#include <windows.h>
#include <math.h>
#include "convlv.h"
#include "interp_1d.h"

using namespace std;


void quadraticFit(double x[], double data[], int start, int len, double parameters[])
{
	LAST_ERR[0] = ERR_OK
			double s40=0;
	double s30=0;
	double s20=0;
	double s10=0;
	double s21=0;
	double s11=0;
	double s01=0;
	double s00= len;
	for (int i=start; i<start+len; i++)
	{
		s40 += pow(x[i], 4);
		s30 += pow(x[i], 3);
		s20 += pow(x[i], 2);
		s10 += x[i];
		s21 += pow(x[i], 2) * data[i];
		s11 += x[i] * data[i];
		s01 += data[i];
	}

	double D = s40*(s20*s00 - s10*s10) - s30*(s30*s00 - s10*s20) + s20*(s30*s10 - s20*s20);
	double Da = s21*(s20*s00 - s10*s10) - s11*(s30*s00 - s10*s20) + s01*(s30*s10 - s20*s20);
	double Db = s40*(s11*s00 - s01*s10) - s30*(s21*s00 - s01*s20) + s20*(s21*s10 - s11*s20);
	double Dc = s40*(s20*s01 - s10*s11) - s30*(s30*s01 - s10*s21) + s20*(s30*s11 - s20*s21);

	parameters[0] = Da/D;
	parameters[1] = Db/D;
	parameters[2] = Dc/D;

	// R2
	double a = parameters[0];
	double b = parameters[1];
	double c = parameters[2];

	double ymean = s01 / len;
	double sumRes = 0;
	double sumTot = 0;
	for (int i=start; i<start+len; i++)
	{
		double vx = x[i];
		double vy = data[i];
		sumRes += pow(vy - a*vx*vx - b*vx - c, 2);
		sumTot += pow(vy - ymean, 2);
	}
	parameters[3] = 1 - sumRes / sumTot;

}

void serialization(double* pdata,int len)
{
	double mean = 0;
	double std = 0;
	for(int j =0;j<len;j++)
	{
		double v = pdata[j];
		mean += v;
		std += v*v;

	}
	mean /= len;
	std = sqrt(std/len - mean*mean);

	for(int j =0;j<len;j++)
	{
		pdata[j] = ( pdata[j]-mean)/std;
	}

}

void initialize(double* pOpt_){
	sOpt = new double[11];
	memcpy(sOpt,pOpt_,11*sizeof(double));

	LAST_ERR =  new double[1];
	LAST_ERR[0]  = ERR_OK;
}
void deleteRoi(int index){
	double* temp;
	if(!sCalProfile.empty()){
		temp =  sCalProfile[index];
		delete[] temp;
		sCalProfile[index] = NULL;
	}

	if( !sPosProfie.empty()){
		temp =  sPosProfie[index];
		delete[] temp;
		sPosProfie[index] = NULL;
	}

}
void releaseBuffer(){
	if(!sCalProfile.empty()){
		for (int i = 0; i < (int) sCalProfile.size(); ++i) {
			if(sCalProfile[i])
				delete[] sCalProfile[i];
		}
	}

	while(!sCalPos.empty())
		sCalPos.pop_back();
	while(!sCalProfile.empty())
		sCalProfile.pop_back();

	if(sOpt){
		delete[] sOpt;
		sOpt = NULL;
	}
	if(LAST_ERR)
	{
		delete[] LAST_ERR;
		LAST_ERR = NULL;
	}
}

DWORD WINAPI gosseCenter(void* parameter){

	GosseData* para = (GosseData*)parameter;
	Option opt;
	getOption(opt);

	int halfQuadWindow = opt.halfQuadWidth;
	int border =(int) 2*opt.beanRadius;
	int corrBorder = 2*border -1;

	serialization(para->sum,border);

	int len = 0; // Full length with zero panding
	int convSize = 2*border-1;//M+N-1

	len = 1<<myLog2(convSize);
	VecDoub SumX(len);
	for(int i = 0;i<border;i++){
		SumX[i] = para->sum[i];
	}
	for(int i = border;i< len;i++){
		SumX[i] =0;
	}

	VecDoub_O ConvOutX(len);
	convlv(SumX,SumX,1,ConvOutX);
	int pos = -1;
	double max_val = 0;
	double* pCorrX = new double[corrBorder];
	for(int j =0;j<convSize;j++)
	{
		pCorrX[j] = ConvOutX[j];
		if(max_val < pCorrX[j])
		{
			max_val =  pCorrX[j];
			pos = j;
		}
	}

	int start = pos - halfQuadWindow;

	if (start<0)
		start = 0;
	if (start > convSize - 2*halfQuadWindow -1  )
		start = convSize- 2*halfQuadWindow -1;



	double* pArray = new double[convSize];
	for (int i=0; i<convSize; i++)
		pArray[i] = i;

	double paraRet[4];
	quadraticFit(pArray,pCorrX, start, 2*halfQuadWindow+1, paraRet);
	delete[] pArray;
	delete[] pCorrX;
	*(para->location) = para->origin + (- paraRet[1] / (2 * paraRet[0]) +1 )/2;
	return 0;
}

DWORD WINAPI getXYCenter(void* parameter){
	ParameterPackage* para = (ParameterPackage*)parameter;
	Option opt;
	getOption(opt);
	if(IsBallOutOfImage(para->xCenter,para->yCenter,opt.beanRadius,opt.imgWidth,opt.imgHeight)){
		*(para->location) = -1;
		*(para->location+1)  =-1;
		printf("Ball out of center\r\n");
		return 0;
	}
	int roiX = para->xCenter - opt.beanRadius;
	int roiY = para->yCenter - opt.beanRadius;

	int border = 2*opt.beanRadius;

	int bitDepth = opt.bitDepth;

	int imgwidth = opt.imgWidth;
	int imgSqure = opt.imgWidth*opt.imgHeight;
	// Calculate the 1-D arrays
	double* pSumX = new double[border];
	double* pSumY = new double[border];
	double intensitySum = 0;
	memset(pSumX, 0, border * sizeof(double));
	memset(pSumY, 0, border * sizeof(double));

	switch (bitDepth)
	{
	case 8:
	{
		signed char* cImage = (signed char*) para->image;
		for (int i = 0; i < border; i++) {
			for (int j = 0; j <border; j++) {
				unsigned char intensity =cImage[(i+roiY)*imgwidth+j+roiX];
				intensitySum += intensity;
				pSumX[j]  += intensity;
				pSumY[i]  += intensity;
			}
		}
	}
	break;
	case 16:
	{
		short* sImage = (short*)para->image;
		for (int i = 0; i < border; i++) {
			for (int j = 0; j <border; j++) {
				short intensity =sImage[(i+roiY)*imgwidth+j+roiX];
				intensitySum += intensity;
				pSumX[j]  += intensity;
				pSumY[i]  += intensity;
			}
		}
	}
	break;
	case 32:
	{
		float* sImage = (float*)para->image;
		for (int i = 0; i < border; i++) {
			for (int j = 0; j <border; j++) {
				float intensity =sImage[(i+roiY)*imgwidth+j+roiX];
				intensitySum += intensity;
				pSumX[j]  += intensity;
				pSumY[i]  += intensity;
			}
		}
	}
	break;
	case 64:
	{
		double* sImage = (double*)para->image;
		for (int i = 0; i < border; i++) {
			for (int j = 0; j <border; j++) {
				double intensity =sImage[(i+roiY)*imgwidth+j+roiX];
				intensitySum += intensity;
				pSumX[j]  += intensity;
				pSumY[i]  += intensity;
			}
		}
	}
	break;
	}

	GosseData xdata;
	xdata.sum = pSumX;
	xdata.origin = roiX;
	xdata.location = para->location;
	GosseData ydata;
	ydata.sum = pSumY;
	ydata.origin = roiY;
	ydata.location = para->location+1;
	//save intensitySum
	*(para->location+2) = intensitySum/imgSqure;

	HANDLE aThread[2];
	aThread[0] = ::CreateThread(NULL, 0, gosseCenter,(void* ) &xdata, 0, NULL);
	aThread[1] = ::CreateThread(NULL, 0, gosseCenter,(void* ) &ydata, 0, NULL);
	WaitForMultipleObjects(2, aThread, TRUE, INFINITE);
	CloseHandle(aThread[0]);
	CloseHandle(aThread[1]);

	return 0;
}

DWORD WINAPI polarIntegral(void* parameter){
	PolarIntegralData* para = (PolarIntegralData*) parameter;
	double S00 = 0, S01 =0, S10 = 0, S11 =0;
	switch(para->bitDepth){
	case 8:{
		BYTE* pImage = (BYTE*)para->image;
		for(int i = para->start;i< para->end;i++)
		{
			double sumr = 0;
			double r =i*para->rInterStep;
			double dTheta = 1/r;
			int nTheta =(int) (2*3.141592653579/dTheta);
			for(int j = 0;j<nTheta;j++)
			{
				double x = (para->xpos+r*cos(dTheta*j));
				double y = (para->ypos+r*sin(dTheta*j));
				int x0 = (int)x;
				int y0 = (int)y;
				int x1 = x0 +1;
				int y1 = y0 +1;
				double dx = x - x0;
				double dy = y - y0;

				S00 = pImage[x0 + y0*para->imgwidth];
				S01 = pImage[x1 + y0*para->imgwidth];
				S10 = pImage[x0 + y1*para->imgwidth];
				S11 = pImage[x1 + y1*para->imgwidth];
				double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
				sumr += Sxy;
			}
			para->currPosProfile[i] =sumr/nTheta;
		}
	}
	break;
	case 16:{
		short* pImage = (short*)para->image;
		for(int i = para->start;i< para->end;i++)
		{
			double sumr = 0;
			double r =i*para->rInterStep;
			double dTheta = 1/r;
			int nTheta =(int) (2*3.141592653579/dTheta);
			for(int j = 0;j<nTheta;j++)
			{
				double x = (para->xpos+r*cos(dTheta*j));
				double y = (para->ypos+r*sin(dTheta*j));
				int x0 = (int)x;
				int y0 = (int)y;
				int x1 = x0 +1;
				int y1 = y0 +1;
				double dx = x - x0;
				double dy = y - y0;

				S00 = pImage[x0 + y0*para->imgwidth];
				S01 = pImage[x1 + y0*para->imgwidth];
				S10 = pImage[x0 + y1*para->imgwidth];
				S11 = pImage[x1 + y1*para->imgwidth];
				double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
				sumr += Sxy;
			}
			para->currPosProfile[i] =sumr/nTheta;
		}
	}
	break;
	case 32:
	{
		float* pImage = (float*)para->image;
		for(int i = para->start;i< para->end;i++)
		{
			double sumr = 0;
			double r =i*para->rInterStep;
			double dTheta = 1/r;
			int nTheta =(int) (2*3.141592653579/dTheta);
			for(int j = 0;j<nTheta;j++)
			{
				double x = (para->xpos+r*cos(dTheta*j));
				double y = (para->ypos+r*sin(dTheta*j));
				int x0 = (int)x;
				int y0 = (int)y;
				int x1 = x0 +1;
				int y1 = y0 +1;
				double dx = x - x0;
				double dy = y - y0;

				S00 = pImage[x0 + y0*para->imgwidth];
				S01 = pImage[x1 + y0*para->imgwidth];
				S10 = pImage[x0 + y1*para->imgwidth];
				S11 = pImage[x1 + y1*para->imgwidth];
				double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
				sumr += Sxy;
			}
			para->currPosProfile[i] =sumr/nTheta;
		}
	}
	break;
	case 64:{
		double* pImage = (double*)para->image;
		for(int i = para->start;i< para->end;i++)
		{
			double sumr = 0;
			double r =i*para->rInterStep;
			double dTheta = 1/r;
			int nTheta =(int) (2*3.141592653579/dTheta);
			for(int j = 0;j<nTheta;j++)
			{
				double x = (para->xpos+r*cos(dTheta*j));
				double y = (para->ypos+r*sin(dTheta*j));
				int x0 = (int)x;
				int y0 = (int)y;
				int x1 = x0 +1;
				int y1 = y0 +1;
				double dx = x - x0;
				double dy = y - y0;

				S00 = pImage[x0 + y0*para->imgwidth];
				S01 = pImage[x1 + y0*para->imgwidth];
				S10 = pImage[x0 + y1*para->imgwidth];
				S11 = pImage[x1 + y1*para->imgwidth];
				double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;
				sumr += Sxy;
			}
			para->currPosProfile[i] =sumr/nTheta;
		}
	}
	break;
	}
	return 0;
}

DWORD WINAPI getCorrelation(void* parameter){
	CorrData* para = (CorrData*)parameter;
	for (int i = para->start; i < para->end; ++i) {
		*(para->corrValue + i) = convolution(para->posProfile,para->calProfile+i*para->posProfleLen,para->posProfleLen);
	}
	return 0;
}

DWORD WINAPI getZPostion(void* parameter){

	ParameterPackage* para = (ParameterPackage*)parameter;
	Option opt;
	getOption(opt);
	int currRoiIndex = para->currRoiIndex;
	if(sCalProfile[currRoiIndex] == NULL)return 0;

	calibration(parameter);
	int posProfilelen = (int)(opt.beanRadius/opt.rInterStep);
	int strackSize = (int)(opt.zRange/opt.zStep);
	int halfQuadWindow = opt.halfQuadWidth;

	double* posProfile = getPosProfile(currRoiIndex);
	double* calProfile = getCalProfile(currRoiIndex,0);

	double* corrValue = new double[strackSize];
	memset(corrValue,0,strackSize*sizeof(double));

	int threadNum = opt.zIndexCorrPartNum;
	int partLen =  strackSize/threadNum;
	if(strackSize%threadNum != 0){
		threadNum += 1;
	}
	HANDLE* aThread = new HANDLE[threadNum];
	CorrData* corrParameter = new CorrData[threadNum];

	for (int i = 0; i < threadNum; ++i) {
		int start = i*partLen;
		int end = (i+1)*partLen;
		if(end >strackSize)
			end = strackSize;
		corrParameter[i].start  =start;
		corrParameter[i].end = end;
		corrParameter[i].corrValue = corrValue;
		corrParameter[i].posProfleLen = posProfilelen;
		corrParameter[i].posProfile = posProfile;
		corrParameter[i].calProfile = calProfile;
		aThread[i] = CreateThread(NULL, 0, getCorrelation,(void* )&corrParameter[i], 0, NULL);
	}
	WaitForMultipleObjects(threadNum, aThread, TRUE, INFINITE);
	for (int i = 0;i < threadNum; ++ i) {
		CloseHandle(aThread[i]);
	}
	delete[] aThread;
	delete[] corrParameter;
	int pos = -1;
	double max = 0;
	for (int i = 0; i < strackSize ; ++i) {
		double temp = corrValue[i];
		if(floor(temp) == 1){
			*(para->location+2) = i;
			return 0;
		}
		if(max < temp){
			pos = i;
			max = temp;
		}
	}
	int start =pos - halfQuadWindow;
	if (start<0)
		start = 0;
	if (start > strackSize - 2*halfQuadWindow)
		start = strackSize - 2*halfQuadWindow;

	double paraRet[4];
	double* calPos = new double[strackSize];
	for (int i = 0; i < strackSize; ++i) {
		calPos[i] = getCalPos()[i];
	}
	quadraticFit(calPos,corrValue,start, 2*halfQuadWindow, paraRet);
	delete[] calPos;
	*(para->location+2) = - paraRet[1] / (2 * paraRet[0]);
	return 0;
}

DWORD WINAPI calibration(void* parameter)
{
	getXYCenter(parameter);
	ParameterPackage* para = (ParameterPackage*)parameter;
	Option opt;
	getOption(opt);


	double radius = opt.beanRadius;
	double step = opt.rInterStep;
	int bitDepth = opt.bitDepth;
	int imgwidth = opt.imgWidth;
	int currCalProfilelen = (int)(radius/step);
	double xpos = para->location[0];
	double ypos = para->location[1];
	if(xpos + 1 <0.001)
		return 0;
	int currRoiIndex = para->currRoiIndex;
	int currZIndex = para->currZIndex;
	int offset = currZIndex*currCalProfilelen;

	double* currPosProfile;
	int threadNum = opt.polarIntegralPartNum;
	int partLen = (currCalProfilelen-1)/threadNum;
	if( (currCalProfilelen-1)%threadNum !=0)
		threadNum += 1;

	int start = 1;
	int end = 0;

	HANDLE* aThread = new HANDLE[threadNum];
	PolarIntegralData* pIParameter = new PolarIntegralData[threadNum];

	if(currZIndex == -1)//get
	{
		currPosProfile= getPosProfile(currRoiIndex);
	}
	else{
		currPosProfile= getCalProfile(currRoiIndex,offset);
	}

	switch(bitDepth)
	{
	case 8:
	{
		BYTE* pImage =(BYTE*) para->image;
		currPosProfile[0] = pImage[(int)xpos+((int)ypos)*imgwidth];
		for (int i = 0; i < threadNum; ++i) {
			end = start+partLen;
			if(end >currCalProfilelen)
				end = currCalProfilelen;

			pIParameter[i].start  = start;
			pIParameter[i].end = end;
			pIParameter[i].currPosProfile = currPosProfile;
			pIParameter[i].rInterStep = step;
			pIParameter[i].bitDepth = bitDepth;
			pIParameter[i].image = para->image;
			pIParameter[i].imgwidth = imgwidth;
			pIParameter[i].xpos = xpos;
			pIParameter[i].ypos = ypos;
			start  += partLen;
			aThread[i] = CreateThread(NULL, 0, polarIntegral,(void* )&pIParameter[i], 0, NULL);
		}
	}
	break;
	case 16:
	{
		short * pImage =(short*) para->image;
		currPosProfile[0] = pImage[(int)xpos+((int)ypos)*imgwidth];
		for (int i = 0; i < threadNum; ++i) {
			end = start+partLen;
			if(end >currCalProfilelen)
				end = currCalProfilelen;

			pIParameter[i].start  = start;
			pIParameter[i].end = end;
			pIParameter[i].currPosProfile = currPosProfile;
			pIParameter[i].rInterStep = step;
			pIParameter[i].bitDepth = bitDepth;
			pIParameter[i].image = para->image;
			pIParameter[i].imgwidth = imgwidth;
			pIParameter[i].xpos = xpos;
			pIParameter[i].ypos = ypos;
			start  += partLen;
			aThread[i] = CreateThread(NULL, 0, polarIntegral,(void* )&pIParameter[i], 0, NULL);
		}
	}
	break;
	case 32:
	{
		float * pImage =(float*) para->image;
		currPosProfile[0] = pImage[(int)xpos+((int)ypos)*imgwidth];
		for (int i = 0; i < threadNum; ++i) {
			end = start+partLen;
			if(end >currCalProfilelen)
				end = currCalProfilelen;

			pIParameter[i].start  = start;
			pIParameter[i].end = end;
			pIParameter[i].currPosProfile = currPosProfile;
			pIParameter[i].rInterStep = step;
			pIParameter[i].bitDepth = bitDepth;
			pIParameter[i].image = para->image;
			pIParameter[i].imgwidth = imgwidth;
			pIParameter[i].xpos = xpos;
			pIParameter[i].ypos = ypos;
			start  += partLen;
			aThread[i] = CreateThread(NULL, 0, polarIntegral,(void* )&pIParameter[i], 0, NULL);
		}
	}
	break;
	case 64:
	{
		double * pImage =(double*) para->image;
		currPosProfile[0] = pImage[(int)xpos+((int)ypos)*imgwidth];
		for (int i = 0; i < threadNum; ++i) {
			end = start+partLen;
			if(end >currCalProfilelen)
				end = currCalProfilelen;

			pIParameter[i].start  = start;
			pIParameter[i].end = end;
			pIParameter[i].currPosProfile = currPosProfile;
			pIParameter[i].rInterStep = step;
			pIParameter[i].bitDepth = bitDepth;
			pIParameter[i].image = para->image;
			pIParameter[i].imgwidth = imgwidth;
			pIParameter[i].xpos = xpos;
			pIParameter[i].ypos = ypos;
			start  += partLen;
			aThread[i] = CreateThread(NULL, 0, polarIntegral,(void* )&pIParameter[i], 0, NULL);
		}
	}
	break;
	}
	WaitForMultipleObjects(threadNum, aThread, TRUE, INFINITE);
	for (int i = 0;i < threadNum; ++ i) {
		CloseHandle(aThread[i]);
	}
	delete[] aThread;
	delete[] pIParameter;
	serialization(currPosProfile,currCalProfilelen);
	return 0;
}

double convolution(double* posProfile_,double*  pcalProfile_,int len){
	double sum = 0;
	for(int i = 0;i<len;i++){
		sum += posProfile_[i]*pcalProfile_[i];
	}
	sum /=len;
	return sum;
}

int myLog2(long length)
{
	for(int i = 0;i<1000;i++)
	{
		double temp =pow(2.0,i);
		if(temp>=length)
		{
			return i;
		}
	}
	return -1;
}

bool IsBallOutOfImage(double xpos,double ypos,double radius_,double imgwidth,double imgheight){
	int border = 10;
	double radius = border+radius_;
	if(floor(xpos + radius - imgwidth) >= 0)
		return true;
	if(floor(xpos - radius) <=0)
		return true;
	if(floor(ypos + radius - imgheight) >=0)
		return true;
	if(floor(ypos - radius) <0)
		return true;
	return false;

}

void  addCalPos(double zPos){
	sCalPos.push_back(zPos);
}

void initializeCalProfile(int roiNum){
	Option opt;
	getOption(opt);
	int size = (int) (opt.zRange/opt.zStep);
	int len = (int) (opt.beanRadius/opt.rInterStep);
	int offset = size*len;
	for (int i= 0; i <roiNum; ++i) {
		double* temp = new double[offset];
		memset(temp,0,offset*sizeof(double));
		sCalProfile.push_back(temp);
	}
}

void initializePosProfile(int roiNum){
	Option opt;
	getOption(opt);

	int len = (int) (opt.beanRadius/opt.rInterStep);
	for (int i= 0; i <roiNum; ++i) {
		double* temp = new double[len];
		memset(temp,0,len*sizeof(double));
		sPosProfie.push_back(temp);
	}
}

double * getCalProfile(int currRoiIndex,int offset){
	return sCalProfile[currRoiIndex]+offset;
}

vector<double* >getCalProfile(){
	return sCalProfile;
}

double * getPosProfile(int currRoiIndex){
	return sPosProfie[currRoiIndex];
}

vector<double* >getPosProfile(){
	return sPosProfie;
}

vector<double> getCalPos(){
	return sCalPos;
}

double* GetErrCode(){
	return LAST_ERR;
}


void getOption(Option &opt){
	opt.beanRadius=(int)sOpt[0];
	opt.rInterStep=sOpt[1];
	opt.bitDepth=(int)sOpt[2];
	opt.halfQuadWidth=(int)sOpt[3];
	opt.imgWidth=(int)sOpt[4];
	opt.imgHeight=(int)sOpt[5];
	opt.zStart=sOpt[6];
	opt.zRange=sOpt[7];
	opt.zStep=sOpt[8];
	opt.polarIntegralPartNum = (int)sOpt[9];
	opt.zIndexCorrPartNum = (int)sOpt[10];

}

void* getImage(JNIEnv * env,jobject image,int bitDepth){
	void * pImage = NULL;
	switch (bitDepth){
	case 8:
		pImage =(signed char*) env->GetByteArrayElements((jbyteArray)image,JNI_FALSE);
		break;
	case 16:
		pImage =(unsigned short *) env->GetShortArrayElements((jshortArray)image,JNI_FALSE);
		break;
	case 32:
		pImage =(float *) env->GetFloatArrayElements((jfloatArray)image,JNI_FALSE);
		break;
	case 64:
		pImage =(double *) env->GetDoubleArrayElements((jdoubleArray)image,JNI_FALSE);
		break;
	default:
		break;
	}
	return pImage;
}

void releaseImage(JNIEnv * env,jobject image,void* pImage,int bitDepth){
	switch (bitDepth){
	case 8:
		env->ReleaseByteArrayElements((jbyteArray)image,(jbyte*)pImage,JNI_ABORT);
		break;
		break;
	case 16:
		env->ReleaseShortArrayElements((jshortArray)image,(jshort*)pImage,JNI_ABORT);
		break;
	case 32:
		env->ReleaseFloatArrayElements((jfloatArray)image,(jfloat*)pImage,JNI_ABORT);
		break;
	case 64:
		env->ReleaseDoubleArrayElements((jdoubleArray)image,(jdouble*)pImage,JNI_ABORT);
		break;
	default:
		break;
	}

}
