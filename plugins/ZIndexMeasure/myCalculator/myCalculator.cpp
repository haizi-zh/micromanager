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
	return NULL;
 
}
 

JNIEXPORT jobjectArray JNICALL Java_org_ndaguan_micromanager_myCalculator_GetZPosition
  (JNIEnv * env_, jobject obj_, jobject image_, jintArray roi_,jint index_)
{ 	
	int* pRoi_ =(int*)env_->GetIntArrayElements(roi_, JNI_FALSE);	
	//opt:Radius,RInterstep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zX,zN,zlen,zSize 
	double * pos = new double[12];
	//pos 0~5 x y z dx dy dz  6~11  <x>^2 <y>^2 <z>^2 mean(x) mean(y) mean(z) 
	memset(pos,-1,12*sizeof(double)); 
	GetZPosition(env_,image_,index_,pRoi_,pos);
	env_->ReleaseIntArrayElements(roi_,(jint*)pRoi_,JNI_ABORT);
	
	jdoubleArray jpos = env_->NewDoubleArray(12);  
    env_->SetDoubleArrayRegion(jpos, 0,12, (const jdouble*)pos); 
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
  (JNIEnv * env_, jobject obj_, jobject image_, jintArray roi_,jintArray opt_)
{  	
	
	int* pRoi_ =(int*)env_->GetIntArrayElements(roi_, JNI_FALSE);	
	int* popt_ =(int*)env_->GetIntArrayElements(opt_, JNI_FALSE);	

	//Pos_:x,yPosition,rx,ry
	double* Pos_ = new double[6];
	memset(Pos_,-1,6*sizeof(double));
		

	gosse(env_,image_,pRoi_,Pos_,popt_);
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