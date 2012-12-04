// Veloz.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"
#include "Veloz.h"
#include <stdexcept>
#include <cassert>
#include <windows.h>
#include "convlv.h"

using namespace std;

namespace DeadNight
{
	namespace bioscope
	{
		double PCFreq = 0.0;
		__int64 CounterStart = 0;		
		void StartCounter()
		{
			LARGE_INTEGER li;
			QueryPerformanceFrequency(&li);
			PCFreq = double(li.QuadPart)/1000.0;

			QueryPerformanceCounter(&li);
			CounterStart = li.QuadPart;
		}
		double GetCounter()
		{
			LARGE_INTEGER li;
			QueryPerformanceCounter(&li);
			return double(li.QuadPart-CounterStart)/PCFreq;
		}
		void DataInit(double* pOpt_){

			sOpt = new double[13];
			memcpy(sOpt,pOpt_,13*sizeof(double));
			Option opt;
			getOption(opt);


			int zLen = (int)(opt.zScale/opt.zStep);
			int zSize = (int)(opt.radius/opt.rInterStep);

			LAST_ERR =  new double[2];
			sCalPos =  new double[zLen];
			sCalProfile = new double[zLen*zSize];
			sum = new double[3];//x y z
			sum2 = new double[3];//x y z
			memset(sCalProfile,0,zLen*zSize*sizeof(double));
			memset(sum,0,3*sizeof(double));
			memset(sum2,0,3*sizeof(double));

			LAST_ERR[0]  = ERR_OK;

			for(int i = 0;i<zLen;i++){
				sCalPos[i] = opt.zStart + i*opt.zStep;
			}		
		}
 
		
		void getOption(Option &opt){
					   	
			// opt_[13]	
			// radius,rInterStep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep,DNALen,Temperature,DNAPersLen,frame2calcForce
			opt.radius=sOpt[0];
			opt.rInterStep=sOpt[1];
			opt.bitDepth=(int)sOpt[2];
			opt.halfQuadWidth=(int)sOpt[3];
			opt.imgWidth=(int)sOpt[4];
			opt.imgHeight=(int)sOpt[5];
			opt.zStart=sOpt[6];
			opt.zScale=sOpt[7];
			opt.zStep=sOpt[8];	

			opt.DNALen =  sOpt[9];
			opt.Temperature =  sOpt[10];
			opt.DNAPersLen =  sOpt[11];
			opt.frame2calcForce =(int) sOpt[12];
		}		
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


		void zscore(double* pdata,int len)
		{

			double mean = 0;
			double std = 0;
			for(int j =0;j<len;j++)
			{
				double v = pdata[j];
				mean += v;

			}
			mean /= len;
			for(int j =0;j<len;j++)
			{
				double v =pdata[j]-mean;	
				std += v*v;
			}
			std = sqrt(std/len);
			for(int j =0;j<len;j++)
			{
				pdata[j] = ( pdata[j]-mean)/std;
			}
		}
		double* GetErrCode(){
			return LAST_ERR;
		}
		void DeleteData(){
			if(sCalPos)
				delete[] sCalPos;
			if(sCalProfile)
				delete[] sCalProfile;
			if(sOpt)
				delete[] sOpt;
			if(LAST_ERR)
				delete[] LAST_ERR;
			if(sum)
				delete[] sum;
			if( sum2)
				delete[]  sum2;
		}
		void SetBitDepth(int bitDepth_){
			sOpt[2] = bitDepth_;
		}
		void gosse(JNIEnv * env_, jobject image_,int*  roi_, double* result,int * opt)
		{

			//roi:x,y,width,height
			//opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight
			StartCounter();
			int roiX = roi_[0];
			int roiY = roi_[1];
			int roiWidth = roi_[2];
			int roiHeight = roi_[3];

			//Option opt;
			//getOption(opt);
			//int bitDepth = opt.bitDepth;
			//int halfQuadWindow = opt.halfQuadWidth;
			//int imgwidth = opt.imgWidth;
			//int imgheight = opt.imgHeight;
			//assert(roiX+roiWidth<=imgwidth); 
			//assert(roiY+roiHeight<=imgheight);

			//Opt:bitDepth,halfQuadWidth,imgWidth,imgHeight
			int bitDepth = opt[0];
			int halfQuadWindow = opt[1];
			int imgwidth = opt[2];
			int imgheight = opt[3];

			// Calculate the 1-D arrays
			double* pSumX = new double[roiWidth];
			double* pSumY = new double[roiHeight];		
			double* pCorrX = new double[2*roiWidth-1];
			double* pCorrY = new double[2*roiHeight-1];
			memset(pSumX, 0, roiWidth * sizeof(double));
			memset(pSumY, 0, roiHeight * sizeof(double));	
			jarray myarray = NULL;
			switch (bitDepth)
			{
			case 16:
			{ 
				unsigned short * row =(unsigned short *) env_->GetShortArrayElements((jshortArray)image_,JNI_FALSE);
				if(!row){
					LAST_ERR[0] = ERR_IMAGE_NULL_POINTER;
					return;
				}
				for (int i = 0; i < roiHeight; i++) {
					for (int j = 0; j <roiWidth; j++) {
						pSumX[j]  += row[(i+roiY)*imgwidth+j+roiX];
						pSumY[i]  += row[(i+roiY)*imgwidth+j+roiX];
					}
				}
				env_->ReleaseShortArrayElements((jshortArray)image_,(jshort*)row,0);	
				}					
	
			break;			
			}

			zscore( pSumX,roiWidth);				
			zscore( pSumY,roiHeight);				

			int len = 0; // Full length with zero panding
			int convSize = 0;//M+N-1	
			int pos = -1;
			int start = 0;
			double* pArray = NULL;
			double para[4];

			//x track 		
			convSize = 2*roiWidth-1;
			len = 1<<log2(convSize);
			VecDoub SumX(len);			
			for(int i = 0;i<roiWidth;i++){
				SumX[i] = pSumX[i];
			}		
			for(int i = roiWidth;i< len;i++){
				SumX[i] =0;				 
			}
			delete[] pSumX;

			VecDoub_O ConvOutX(len);	
			convlv(SumX,SumX,1,ConvOutX);
			double max_val = 0;
			for(int j =0;j<convSize;j++)
			{
				pCorrX[j] = ConvOutX[j];
				if(max_val < pCorrX[j])
				{
					max_val =  pCorrX[j];
					pos = j;
				}
			}

			if(pos == -1){
				LAST_ERR[0] = ERR_GET_CENTER_FALSE;
				return;
			}		
			start = pos - halfQuadWindow;
			if (start<0)
				start = 0;
			else if (start >= convSize - 2*halfQuadWindow)
				start = convSize- 2*halfQuadWindow - 1;

			assert(start >= 0 && start <convSize - 2*halfQuadWindow);

			pArray = new double[convSize];
			for (int i=0; i<convSize; i++)
				pArray[i] = i;
			quadraticFit(pArray,pCorrX, start, 2*halfQuadWindow+1, para);
			result[0] = roiX - para[1] / (4 * para[0]);
			result[3] = para[2];
			delete[] pArray;
			delete[] pCorrX;

			//y track	
			convSize = 2*roiHeight-1;
			len = 1<<log2(convSize); 
			VecDoub SumY(len);			
			for(int i = 0;i<roiHeight;i++){
				SumY[i] = pSumY[i];
			}		
			for(int i = roiHeight;i< len;i++){
				SumY[i] =0;				 
			}			
			delete[] pSumY;
			VecDoub_O ConvOutY(len);	
			convlv(SumY,SumY,1,ConvOutY);
			max_val = 0;
			for(int j =0;j<convSize;j++)
			{
				pCorrY[j] = ConvOutY[j];
				if(max_val < pCorrY[j])
				{
					max_val =  pCorrY[j];
					pos = j;
				}
			}

			if(pos == -1){
				LAST_ERR[0] = ERR_GET_CENTER_FALSE;
				return;
			}	
			start = pos - halfQuadWindow;
			if (start<0)
				start = 0;
			else if (start >= convSize - 2*halfQuadWindow)
				start = convSize- 2*halfQuadWindow - 1;
			assert(start >= 0 && start <convSize - 2*halfQuadWindow);

			pArray = new double[convSize];
			for (int i=0; i<convSize; i++)
				pArray[i] = i;
			quadraticFit(pArray,pCorrY, start, 2*halfQuadWindow+1, para);
			result[1] = roiY - para[1] / (4 * para[0]);
			result[4] = para[2];

			delete[] pArray;
			delete[] pCorrY;

			LAST_ERR[1] = GetCounter();
		}

		bool IsBallOutOfImage(double xpos,double ypos,double radius,double imgwidth,double imgheight){
			if(xpos + radius >imgwidth)
				return true;
			if(xpos - radius <0)
				return true;
			if(ypos + radius >imgheight)
				return true;
			if(ypos - radius <0)
				return true;
			return false;

		}

		void GetCalProfile(JNIEnv * env_, jobject image_,int*   roi_, int zX_,double* pos,double* calProfile)
		{

			//   opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight
			Option opt;
			getOption(opt);

			double radius = opt.radius;
			double step = opt.rInterStep;
			int bitDepth = opt.bitDepth;
			int imgwidth = opt.imgWidth;
			int imgheight = opt.imgHeight;
			int len = (int)(radius/step);
			//Opt:bitDepth,halfQuadWidth,imgWidth,imgHeight
			int* iopt = new int[4];
			iopt[0] = bitDepth;
			iopt[1] = opt.halfQuadWidth;
			iopt[2] = imgwidth;
			iopt[3] = imgheight;
			gosse(env_,image_,roi_,pos,iopt);
			delete[] iopt;
			double xpos = pos[0],ypos = pos[1];	 

			if(IsBallOutOfImage(xpos,ypos,radius,imgwidth,imgheight))
			{
				LAST_ERR[0] = ERR_BALL_OUTOF_IMAGE;
				return;
			}
			jarray myarray = NULL;
			double r = 0;

			double S00 = 0, S01 =0, S10 = 0, S11 =0;
			double *sCalProfileX;

			if(calProfile){
				sCalProfileX = calProfile;
			}else{
				sCalProfileX= sCalProfile +  zX_*len;
			}

			switch(bitDepth)	
			{	     			
			case 16:
			{
				short * pImage =(short*) env_->GetShortArrayElements((jshortArray)image_,JNI_FALSE);							 								
				sCalProfileX[0] = pImage[(int)xpos+((int)ypos)*imgwidth];
				for(int i = 1;i< len;i++)
				{
					r += step;
					double sumr = 0;
					double dTheta = 1/r;
					int nTheta =(int) (2*3.141592653579/dTheta);	
					for(int j = 0;j<nTheta;j++)
					{
						double x = (xpos+r*cos(dTheta*j));						
						double y = (ypos+r*sin(dTheta*j));
						int x0 = (int)x;
						int y0 = (int)y;
						int x1 = x0 +1;
						int y1 = y0 +1;
						double dx = x - x0;
						double dy = y - y0;

						S00 = pImage[x0 + y0*imgwidth];
						S01 = pImage[x1 + y0*imgwidth];
						S10 = pImage[x0 + y1*imgwidth];
						S11 = pImage[x1 + y1*imgwidth];
						double Sxy = S00*(1-dx)*(1-dy)+S01*dy*(1-dx)+S10*dx*(1-dy) +S11*dx*dy;					
						sumr += Sxy;	 
					}
					sCalProfileX[i] =sumr/nTheta;			
				}
				zscore(sCalProfileX,len);								
				env_->ReleaseShortArrayElements((jshortArray)image_,(jshort*)pImage,JNI_ABORT);	
			}
			break;			
			}//switch
		}

		int getLen(){
			Option opt;
			getOption(opt);
			return (int)(opt.radius/opt.rInterStep);
		}
		double* getcalProfileX(int zX){
			return sCalProfile +  zX*getLen();
		}


		void Double2Roi(double *roi_,ROI& roi)
		{
			roi.x = (int) roi_[0];
			roi.y = (int) roi_[1];
			roi.width =  (int) roi_[2];
			roi.height = (int) roi_[3];
		}
		/*************************************************************************  
		 *  
		 * 函数名称：  
		 *  log2()  
		 *  
		 * 参数:  
		 *   length                      - 输入序列长度  
		 *  
		 * 说明:  
		 *   该函数取得输入length对应的log2  
		 *  
		 ************************************************************************/
		int log2(long length)
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

		double getSTD(LISTDOUBLE mylist,double mean,int len){
			double std = 0;
			LISTDOUBLE::iterator i;
			for (i = mylist.begin(); i != mylist.end(); ++i)
				{
					double v = (*i - mean); 
					std += v*v;
				}
			std = (std/len);
			return sqrt( std);
		}
		double getMean(LISTDOUBLE mylist,int len){
			double mean = 0;
			LISTDOUBLE::iterator i;
			for (i = mylist.begin(); i != mylist.end(); ++i)
				{
					mean +=*i;
				}
			mean = (mean/len);
			return  mean;
		}
		void getForce(double* pos){
			Option opt;					
			getOption(opt);			 
			int len = opt.frame2calcForce;
			double xpos = pos[0];
			double ypos = pos[1];
			double zpos = pos[2];

			myXpos.push_back (xpos);
			myYpos.push_back (ypos);
			myZpos.push_back (zpos);

			int currListSize = myXpos.size();
			if(currListSize <= len){//save since data is not enough 			
				sum[0] += xpos;
				sum[1] += ypos;
				sum[2] += zpos;
								
				sum2[0] += xpos*xpos;
				sum2[1] += ypos*ypos;
				sum2[2] += zpos*zpos;

				pos[9] = sum[0]/currListSize;
				pos[10] = sum[1]/currListSize;
				pos[11] = sum[2]/currListSize;	

				pos[6] = sqrt(sum2[0]/currListSize - pos[9]*pos[9]);
				pos[7] = sqrt(sum2[1]/currListSize - pos[10]*pos[10]);
				pos[8] = sqrt(sum2[2]/currListSize - pos[11]*pos[11]);				
			}			
			else{//the rest			 
				double xfirst = myXpos.front();
				double yfirst = myYpos.front();
				double zfirst = myZpos.front();

				myXpos.pop_front();
				myYpos.pop_front();
				myZpos.pop_front();
				 

				sum[0] += (xpos-xfirst);
				sum[1] += (ypos-yfirst);
				sum[2] += (zpos-zfirst);
								
			 					
				sum2[0] += (xpos*xpos-xfirst*xfirst);
				sum2[1] += (ypos*ypos-yfirst*yfirst);
				sum2[2] += (zpos*zpos-zfirst*zfirst);

				pos[9] = sum[0]/(currListSize-1);
				pos[10] = sum[1]/(currListSize-1);
				pos[11] = sum[2]/(currListSize-1);	

				pos[6] = sqrt(sum2[0]/(currListSize-1) - pos[9]*pos[9]);
				pos[7] = sqrt(sum2[1]/(currListSize-1) - pos[10]*pos[10]);
				pos[8] = sqrt(sum2[2]/(currListSize-1) - pos[11]*pos[11]);
			}
				
		}
		void  GetZPosition(JNIEnv * env_, jobject image_,int index_,int*  roi_,double* pos){
			StartCounter();
			Option opt;					
			getOption(opt);
			int zLen =(int)( opt.zScale/opt.zStep);
			int zSize = (int)(opt.radius/opt.rInterStep);
			int halfQuadWindow =opt.halfQuadWidth;
			int zX_ = 0;
			double* calProfileX = new double[zSize];
			memset(calProfileX,0,zSize*sizeof(double));

			GetCalProfile(env_,image_,roi_,zX_,pos,calProfileX);
			double zPos = 0;
			double* val = new double[zLen];
			double max = 0;
			int ind = -1;
			for(int i = 0;i<zLen; i++){
				val[i] = corr(calProfileX,sCalProfile+i*zSize,zSize);
				if(max <val[i]){
					max = val[i];
					ind = i;
				}
			}
			if(calProfileX){
				delete[] calProfileX;
				calProfileX =NULL;
			}
			if(ind == -1){
				LAST_ERR[0] = ERR_GET_ZPOS_FALSE;
				if(val){
					delete[] val;
					val=NULL;
				}
				return;
			}	

			int start =ind - halfQuadWindow;
			if (start<0)
				start = 0;
			else if (start >= zLen - 2*halfQuadWindow)
				start = zLen- 2*halfQuadWindow - 1;
			assert(start >= 0 && start <zLen - 2*halfQuadWindow);

			double para[4];
			quadraticFit(sCalPos, val, start, 2*halfQuadWindow+1, para);
			if(val){
				delete[] val;
				val=NULL;
			}
			pos[2] = - para[1] /(2 * para[0]);
			pos[5] = para[2];
			if(index_ != -1){			
				getForce(pos);
			}
			LAST_ERR[1] = GetCounter();

		}
		 

		double corr(double* posProfile_,double*  pcalProfile_,int len){

			double sum = 0;
			for(int i = 0;i<len;i++){
				sum += posProfile_[i]*pcalProfile_[i];
			}
			sum /=len;

			return sum;	 
		}

	}
}