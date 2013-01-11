#include <jni.h>
//#include <afxtempl.h>
#include <list>
#include <numeric>
#include <algorithm>
using namespace std;

#define ERR_OK 0.0;
#define ERR_BALL_OUTOF_IMAGE 1.0;
#define ERR_GET_CENTER_FALSE 2.0;
#define ERR_GET_ZPOS_FALSE 3.0;
#define ERR_UNSUPORT_IMAGE_FORMAT 4.0;
#define ERR_IMAGE_NULL_POINTER 5.0;
//LAST_ERR[0] ERR_CODE,LAST_ERR[1] cost time
static double * LAST_ERR;
static double * sCalProfile ;
static double * sCalPos;
static double * sOpt;
static double* sum;
static double* sum2;
static double* corrProfile;
typedef list<double> LISTDOUBLE;
static  LISTDOUBLE  myXpos;
static  LISTDOUBLE  myYpos;
static  LISTDOUBLE  myZpos;



namespace DeadNight
{	 
	namespace bioscope
	{ 

		
		struct Option
		{
		   	// opt_[13]	
			// radius,rInterStep,bitDepth,halfQuadWidth,imgWidth,imgHeight,zStart,zScale,zStep,movingWindowLen

			double radius;
			double rInterStep;
			int bitDepth;
			int halfQuadWidth;
			int imgWidth;
			int imgHeight;
			double zStart;
			double zScale;
			double zStep;						
			int movingWindowLen;
			int zInterStep;
			int method;

		};

		struct ROI
		{
			int x;
			int y;
			int width;
			int height;
		};
		
		void getMeanSTD(double* pos);
		void gosse(JNIEnv * env_, jobject image_,int*  roi_, double*  result,int * opt);
		void GetCalProfile(JNIEnv * env_, jobject image_, int* roi_,  int zX_, double* pos,double* calProfile);
		void GetZPosition(JNIEnv * env_, jobject image_,int index_,int*  roi_,double* pos);
		void StartCounter();
		double GetCounter();
				
		void quadraticFit(double* x_, double* Pdata_, int start, int len, double* para_);
		void zscore(double * pdata_,int len_);
		void cart2pol(void* image,double xpos,double ypos,int zX_,double* calProfile);
		double corr(double* posProfile_,double*  pcalProfile_,int len_);
		int  log2(long length_);
		bool IsBallOutOfImage(double xpos_,double ypos_,double radius_,double imgwidth_,double imgheight_);
		double* GetErrCode();
		void getOption(Option &opt_);
		void DataInit(double* pOpt_);
		void DeleteData();
		int getLen();
		int getZLen();
		double* getcalProfileX(int zX);
		void SetBitDepth(int bitDepth_);
		double getSTD(LISTDOUBLE mylist,double mean,int len);
		double getMean(LISTDOUBLE mylist,int len);
		double * getCorrProfile();
		
	}
}