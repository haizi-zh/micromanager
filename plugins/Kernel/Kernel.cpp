#include "Kernel.h"
#include "Operater.h"
#include <cstring>
#include <windows.h>
using namespace std;


JNIEXPORT jdoubleArray JNICALL _Java_org_ndaguan_micromanager_Kernel_gosseCenter
(JNIEnv * env, jobject obj, jobject image, jobjectArray roilist){

	//get data
	int roiNum = env->GetArrayLength(roilist);

	//initialize data
	double* location = new double[roiNum*3];
	memset(location,0,roiNum*3*sizeof(double));

	//initialize thread data
	HANDLE* aThread = new HANDLE[roiNum];

	//initialize Option
	ParameterPackage* parameter = new ParameterPackage[roiNum];
	Option opt;
	getOption(opt);

	//create thread
	void* pImage= getImage(env,image,opt.bitDepth);
	if(pImage){
		for (int i = 0; i < roiNum; i++) {
			jdoubleArray oRoi = (jdoubleArray)env->GetObjectArrayElement(roilist, i);
			double * roi = env->GetDoubleArrayElements(oRoi,0);
			parameter[i].image = pImage;
			parameter[i].xCenter = (int)roi[0];
			parameter[i].yCenter = (int)roi[1];
			parameter[i].location = location + i*3;
			aThread[i] = CreateThread(NULL, 0, getXYCenter,(void* )&parameter[i], 0, NULL);
			env->ReleaseDoubleArrayElements(oRoi,(jdouble*)roi,JNI_ABORT);
		}
		WaitForMultipleObjects(roiNum, aThread, TRUE, INFINITE);
		for (int i = 0;i < roiNum; ++ i) {
			CloseHandle(aThread[i]);
		}
		releaseImage(env,image,pImage,opt.bitDepth);
	}
	delete[] aThread;
	delete[] parameter;

	jdoubleArray jLoc = env->NewDoubleArray(roiNum*3);
	env->SetDoubleArrayRegion(jLoc, 0, roiNum*3, (const jdouble*) location);
	delete[] location;
	return jLoc ;

}
JNIEXPORT jdoubleArray JNICALL _Java_org_ndaguan_micromanager_Kernel_calibration
(JNIEnv * env, jobject obj, jobject image, jobjectArray roilist, jint zIndex, jdouble zPos){

	//get data
	int roiNum = env->GetArrayLength(roilist);

	//initialize data
	double* location = new double[roiNum*3];
	memset(location,0,roiNum*3*sizeof(double));

	//initialize thread data
	HANDLE* aThread = new HANDLE[roiNum];

	//initialize Option
	ParameterPackage* parameter = new ParameterPackage[roiNum];
	Option opt;
	getOption(opt);
	if(getCalProfile().empty()){
		initializeCalProfile(roiNum);
		initializePosProfile(roiNum);
	}
	addCalPos(zPos);
	//create thread
	void* pImage= getImage(env,image,opt.bitDepth);
	if(pImage){
		for (int i = 0; i < roiNum; i++) {
			jdoubleArray oRoi = (jdoubleArray)env->GetObjectArrayElement(roilist, i);
			double * roi = env->GetDoubleArrayElements(oRoi,0);
			parameter[i].image = pImage;
			parameter[i].xCenter = (int)roi[0];
			parameter[i].yCenter = (int)roi[1];
			parameter[i].location = location + i*3;

			parameter[i].currRoiIndex = i;
			parameter[i].currZIndex = zIndex;

			aThread[i] = CreateThread(NULL, 0, calibration,(void* )&parameter[i], 0, NULL);
			env->ReleaseDoubleArrayElements(oRoi,(jdouble*)roi,JNI_ABORT);
		}
		WaitForMultipleObjects(roiNum, aThread, TRUE, INFINITE);

		for (int i = 0;i < roiNum; ++ i) {
			CloseHandle(aThread[i]);
		}
		releaseImage(env,image,pImage,opt.bitDepth);
	}
	delete[] aThread;
	delete[] parameter;


	//modify return
	jdoubleArray jLoc = env->NewDoubleArray(roiNum*3);
	env->SetDoubleArrayRegion(jLoc, 0, roiNum*3, (const jdouble*) location);
	delete[] location;
	return jLoc ;
}
JNIEXPORT jdoubleArray JNICALL _Java_org_ndaguan_micromanager_Kernel_getZPosition
(JNIEnv * env, jobject obj, jobject image, jobjectArray roilist ){
	//get data
	int roiNum = env->GetArrayLength(roilist);

	//initialize data
	double* location = new double[roiNum*3];
	memset(location,0,roiNum*3*sizeof(double));

	//initialize thread data
	HANDLE* aThread = new HANDLE[roiNum];

	//initialize Option
	ParameterPackage* parameter = new ParameterPackage[roiNum];
	Option opt;
	getOption(opt);
	//create thread
	void* pImage= getImage(env,image,opt.bitDepth);
	if(pImage){
		for (int i = 0; i < roiNum; i++) {
			jdoubleArray oRoi = (jdoubleArray)env->GetObjectArrayElement(roilist, i);
			double * roi = env->GetDoubleArrayElements(oRoi,0);
			parameter[i].image = pImage;
			parameter[i].xCenter = (int)roi[0];
			parameter[i].yCenter = (int)roi[1];
			parameter[i].location = location + i*3;

			parameter[i].currRoiIndex = i;
			parameter[i].currZIndex = -1;

			aThread[i] = CreateThread(NULL, 0, getZPostion,(void* )&parameter[i], 0, NULL);
			env->ReleaseDoubleArrayElements(oRoi,(jdouble*)roi,JNI_ABORT);
		}
		WaitForMultipleObjects(roiNum, aThread, TRUE, INFINITE);
		for (int i = 0;i < roiNum; ++ i) {
			CloseHandle(aThread[i]);
		}
		releaseImage(env,image,pImage,opt.bitDepth);
	}
	delete[] aThread;
	delete[] parameter;
	//modify return
	jdoubleArray jLoc = env->NewDoubleArray(roiNum*3);
	env->SetDoubleArrayRegion(jLoc, 0, roiNum*3, (const jdouble*) location);
	delete[] location;
	return jLoc ;
}

JNIEXPORT void JNICALL _Java_org_ndaguan_micromanager_Kernel_initialize
(JNIEnv *env_, jobject obj_, jdoubleArray opt_)
{
	double* pOpt_ =env_->GetDoubleArrayElements(opt_, JNI_FALSE);
	initialize(pOpt_);
	env_->ReleaseDoubleArrayElements(opt_,pOpt_,0);
}

JNIEXPORT void JNICALL _Java_org_ndaguan_micromanager_Kernel_deleteRoi
(JNIEnv * env, jobject obj, jint index){
	deleteRoi(index);
}

JNIEXPORT void JNICALL _Java_org_ndaguan_micromanager_Kernel_releaseBuffer
(JNIEnv *env_, jobject obj_)
{
	releaseBuffer();
}

