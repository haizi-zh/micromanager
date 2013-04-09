#include <jni.h>
#include <list>
#include <vector>
#include <numeric>
#include <algorithm>
#include <windows.h>
using namespace std;

#define ERR_OK 0.0;
#define ERR_BALL_OUTOF_IMAGE 1.0;
#define ERR_GET_CENTER_FALSE 2.0;
#define ERR_GET_ZPOS_FALSE 3.0;
#define ERR_UNSUPORT_IMAGE_FORMAT 4.0;
#define ERR_IMAGE_NULL_POINTER 5.0;

//LAST_ERR[0] ERR_CODE,LAST_ERR[1] cost time
static double * LAST_ERR;
static double * sOpt;
static vector<double*>  sCalProfile;
static vector<double*>  sPosProfie;
static vector<double> sCalPos;

struct CorrData{
	int start;
	int end;
	double* posProfile;
	double* calProfile;
	double* corrValue;
	int posProfleLen;

};
struct PolarIntegralData{
	int start;
	int end;
	double* currPosProfile;
	double rInterStep;
	double xpos;
	double ypos;
	void* image;
	int imgwidth;
	int bitDepth;
};
struct getCalProfileData{
	int bitDepth;
	int imgwidth;
	int currIndex;
	double r;
	double xpos;
	double ypos;
	void* image;
	double* calProfile;

};
struct ParameterPackage{
	void* image;
	int xCenter;
	int yCenter;
	double zPos;
	int currRoiIndex;
	int currZIndex;
	double*location;
};

struct GosseData{
	double* sum;
	double* location;
	double origin;
};

struct Option
{
	int beanRadius;
	double rInterStep;
	int bitDepth;
	int halfQuadWidth;
	int imgWidth;
	int imgHeight;
	double zStart;
	double zRange;
	double zStep;
	int polarIntegralPartNum;
	int zIndexCorrPartNum;
};

bool IsBallOutOfImage(double xpos_,double ypos_,double radius_,double imgwidth_,double imgheight_);

double convolution(double* posProfile_,double*  pcalProfile_,int len_);
void quadraticFit(double* x_, double* Pdata_, int start, int len, double* para_);
void serialization(double * pdata_,int len_);

void addCalPos(double zPos);
void initialize(double* pOpt_);
void initializeCalProfile(int roiNum);
void initializePosProfile(int roiNum);

void releaseBuffer();

DWORD WINAPI getXYCenter(void* parameter);
DWORD WINAPI gosseCenter(void* parameter);
DWORD WINAPI calibration(void* parameter);
DWORD WINAPI getZPostion(void* parameter);
DWORD WINAPI getCorrelation(void* parameter);

vector<double> getCalPos();
double* GetErrCode();
void getOption(Option &opt_);

vector<double*> getCalProfile();
vector<double*> getPosProfile();

double * getCalProfile(int currRoiIndex,int offset);
double * getPosProfile(int currRoiIndex);

void* getImage(JNIEnv * env,jobject image,int bitDepth);
void releaseImage(JNIEnv * env,jobject image,void* pImage,int bitDepth);
void deleteRoi(int index);
int myLog2(long length);

