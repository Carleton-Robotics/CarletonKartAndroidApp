//
// Created by Frederick Cunningham on 5/9/21.
//
#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#define TAG "NativeLib"

using namespace std;
using namespace cv;

extern "C" {
void JNICALL
Java_com_example_carletonkart_ColorBlobActivity_adaptiveThresholdFromJNI(JNIEnv *env,
                                                               jobject instance,
                                                               jlong matAddr) {

    // get Mat from raw address
    Mat &mat = *(Mat *) matAddr;

    clock_t begin = clock();

    // cv::adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY, 21, 5);
    cv::Canny(mat, mat, 50, 50 * 2, 3, false);
    // log computation time to Android Logcat
    double totalTime = double(clock() - begin) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_INFO, TAG, "adaptiveThreshold computation time = %f seconds\n",
                        totalTime);
}
}

