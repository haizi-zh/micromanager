#include "myCalculator.h"
#include "Veloz.h"
#include <cstring>
#include <cmath>

using namespace DeadNight::bioscope;

/*
 * Class:     myCalculator
 * Method:    GetCalibration
 */
JNIEXPORT jobjectArray JNICALL Java_org_ndaguan_micromanager_myCalculator_Calibration
  (JNIEnv * env_, jobject obj_,  jobject image_, jintArray roi_,jint zX_)
{ 
	 
	int* pRoi_ =(int*)env_->GetIntArrayElements(roi_, JNI_FALSE);	
	double * pos = new double[6];
	memset(pos,-1,6*sizeof(double));
	double* calProfile = NULL;
	GetCalProfile(env_,image_,pRoi_,zX_,pos,calProfile);		
	env_->ReleaseIntArrayElements(roi_,(jint*)pRoi_,JNI_ABORT);
	jdoubleArray jPos = env_->NewDoubleArray(6);  
    env_->SetDoubleArrayRegion(jPos, 0, 6, (const jdouble*) pos); 
	delete[] pos;

	jdoubleArray jErr_Code = env_->NewDoubleArray(2);
	env_->SetDoubleArrayRegion(jErr_Code, 0, 2, (const jdouble*)GetErrCode());

	jclass doubleArrayClass = env_->FindClass("[D");  
    jobjectArray jRet  =  env_->NewObjectArray(3 ,doubleArrayClass , NULL); 

	env_->SetObjectArrayElement(jRet , 0 ,jPos);
	env_->SetObjectArrayElement(jRet , 1 ,jErr_Code);
	int len = getLen();
   	jdoubleArray jCalprofile = env_->NewDoubleArray(len);  
    env_->SetDoubleArrayRegion(jCalprofile, 0, len, (const jdouble*) getcalProfileX(zX_)); 
	env_->SetObjectArrayElement(jRet , 2 ,jCalprofile);	

	return jRet; 
} 
/*
 * Class:     myCalculator
 * Method:    GetForce
 * Signature: ([D[D)D
 */
JNIEXPORT jdoubleArray JNICALL Java_org_ndaguan_micromanager_myCalculator_GetForce
  (JNIEnv * env_, jobject _obj,jdoubleArray data_,jdoubleArray Opt_)
{
	StartCounter();
	double* pData_= env_->GetDoubleArrayElements((jdoubleArray)data_, JNI_FALSE);
	double* pOpt_= env_->GetDoubleArrayElements(Opt_, JNI_FALSE);

	double  L = pOpt_[0]*pow(10.0,-6.0);  
	double  T = pOpt_[1];
	double  P = pOpt_[2]*pow(10.0,-9.0); 
	int     len = (int)pOpt_[3];
	double  Kb = 1.3806505 * pow(10.0,-23.0); 
    double* temp = new double[len];
	double mean = 0;
	double std = 0;

	for(int i = 0;i<len;i++){
		temp[i] = pData_[i]*74*pow(10.0,-9.0);//nM  1piexl = 74nm(7.4um/piexl/100)  
		mean +=temp[i];
	}
	mean /=len;
 
	for(int i = 0;i<len;i++){
		std +=(temp[i]-mean)*(temp[i]-mean);
	}
	std = (std/len);
	delete[] temp;	
	env_->ReleaseDoubleArrayElements(data_,pData_,JNI_ABORT);
	env_->ReleaseDoubleArrayElements(Opt_,pOpt_,JNI_ABORT);
	 
	double	theta = 1/(Kb*T);
	double  A = theta*std/L;  
	double  B = 4*P*theta-4*A;
  
	double    a = (A*A)*B;
	double    b = A - 2*A*B;
	double    c = B -2*A;
	double    detal =b*b - 4*a*c; 
	double Force = -1;
	if (detal > 0 ){
		Force = ( -b - sqrt(detal))/(2*a) ;
		Force *=pow(10.0,12);//N->pN
	} 
 
	double* ret = new double[2];
	ret[0] = Force;
	ret[1] = GetCounter();
	jdoubleArray jret = env_->NewDoubleArray(2);  
    env_->SetDoubleArrayRegion(jret, 0,2, (const jdouble*)ret); 
	delete[] ret;
	return jret ;  
 
}
 

JNIEXPORT jobjectArray JNICALL Java_org_ndaguan_micromanager_myCalculator_GetZPosition
  (JNIEnv * env_, jobject obj_, jobject image_, jintArray roi_,jint index_)
{ 	
	int* pRoi_ =(int*)env_->GetIntArrayElements(roi_, JNI_FALSE);	
	//opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zX,zN,zlen,zSize 
	double * pos = new double[14];
	//pos 0~5 x y z dx dy dz  6~11  <x>^2 <y>^2 <z>^2 mean(x) mean(y) mean(z) 12 forcex 13 forcey
	memset(pos,-1,14*sizeof(double)); 
	GetZPosition(env_,image_,index_,pRoi_,pos);
	env_->ReleaseIntArrayElements(roi_,(jint*)pRoi_,JNI_ABORT);
	
	jdoubleArray jpos = env_->NewDoubleArray(14);  
    env_->SetDoubleArrayRegion(jpos, 0,14, (const jdouble*)pos); 
	delete[] pos;
	pos =NULL;
	jdoubleArray jErr_Code = env_->NewDoubleArray(2);
	 
	env_->SetDoubleArrayRegion(jErr_Code, 0, 2, (const jdouble*)GetErrCode()); 

	jclass doubleArrayClass = env_->FindClass("[D");  
    jobjectArray jret  =  env_->NewObjectArray(2 ,doubleArrayClass , NULL); 

	env_->SetObjectArrayElement(jret , 0 ,jpos);
	env_->SetObjectArrayElement(jret , 1 ,jErr_Code);
   

	return jret ; 
	
}

/*
 * Class:     myCalculator
 * Method:    GosseCenter
 * Signature: ([F[D[D)[D
 */
JNIEXPORT jobjectArray JNICALL Java_org_ndaguan_micromanager_myCalculator_GosseCenter
  (JNIEnv * env_, jobject obj_, jobject image_, jintArray roi_)
{  	
	
	int* pRoi_ =(int*)env_->GetIntArrayElements(roi_, JNI_FALSE);	

	//Pos_:x,yPosition,rx,ry
	double* Pos_ = new double[6];
	memset(Pos_,-1,6*sizeof(double));
		
	gosse(env_,image_,pRoi_,Pos_);
	env_->ReleaseIntArrayElements(roi_,(jint*)pRoi_,JNI_ABORT);

	jdoubleArray jPos_ = env_->NewDoubleArray(6);  
    env_->SetDoubleArrayRegion(jPos_, 0, 6, (const jdouble*) Pos_); 
	delete[] Pos_;

	jdoubleArray jErr_Code = env_->NewDoubleArray(2);
	env_->SetDoubleArrayRegion(jErr_Code, 0, 2, (const jdouble*)GetErrCode()); 
	
	jclass doubleArrayClass = env_->FindClass("[D");  
    jobjectArray jRet  =  env_->NewObjectArray(2 ,doubleArrayClass , NULL); 

	env_->SetObjectArrayElement(jRet , 0 ,jPos_);
	env_->SetObjectArrayElement(jRet , 1 ,jErr_Code);    
	return jRet ; 
  
}

JNIEXPORT void JNICALL Java_org_ndaguan_micromanager_myCalculator_DataInit
  (JNIEnv *env_, jobject obj_, jdoubleArray opt_)
{
    double* pOpt_ =env_->GetDoubleArrayElements(opt_, JNI_FALSE);
	DataInit(pOpt_);
	env_->ReleaseDoubleArrayElements(opt_,pOpt_,0);
}

JNIEXPORT void JNICALL Java_org_ndaguan_micromanager_myCalculator_DeleteData
  (JNIEnv *env_, jobject obj_)
{
	DeleteData();
   
}

JNIEXPORT void JNICALL Java_org_ndaguan_micromanager_myCalculator_SetBitDepth
	(JNIEnv * env_, jobject obj_, jint bitDepth_){

		SetBitDepth(bitDepth_);
}