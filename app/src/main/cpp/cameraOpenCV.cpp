#include <jni.h>
#include <string>
#include <math.h>
#include <numeric>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/calib3d.hpp>
//#include <opencv/cv.hpp>

#include <aruco3/aruco.h>
#include <aruco3/cameraparameters.h>

#include <android/log.h>
#include <opencv2/calib3d/calib3d_c.h>
//#include <opencv2/aruco/aruco.hpp>

#define  LOG_TAG    "jnilog"
#define  ALOG(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
// LOG("Log comes from C vaule = %d.", arrSize);

using namespace std;

extern "C" {

JNIEXPORT jdoubleArray JNICALL
Java_com_tuielectronics_aruco_1demo_CameraProcess_openCVProcess(
        JNIEnv *env, jobject /* this */,
        jint width, jint height,
        jbyteArray NV21FrameData,
        jintArray outPixels, jint pixel_width, jint pixel_height) {

    jbyte *pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);
    jint *poutPixels = env->GetIntArrayElements(outPixels, 0);

    vector<double> arr;
    aruco::MarkerDetector makerDetector;
    makerDetector.getParameters().setDetectionMode((aruco::DetectionMode) 1,
                                                   100 / 1000);//minMarkerSize
    makerDetector.getParameters().setCornerRefinementMethod(
            (aruco::CornerRefinementMethod) 2);//aruco::CORNER_SUBPIX
    /*
     * CORNER_SUBPIX: uses the opencv subpixel function. As previously indicated, in some cases the use of enclosed markers can achieve better results. However, in my experience there is not significant difference.
     * CORNER_LINES: this method uses all the pixels of corner border to analytically estimate the 4 lines of the square. Then the corners are estimated by intersecting the lines. This method is more robust to noise. However, it only works if input image is not resized. So, the value minMarkerSize will be set to 0 if you use this corner refinement method.
     * CORNER_NONE: Does not corner refinement. In some problems, camera pose does not need to be estimated, so you can use it. Also, in some very noisy environments, this can be a better option.
     */

    // mode = 0: detect border 8 points + map ID
    // mode = 1: to detect border 8 points + calibration robot
    // mode = 2: to detect robots and objects
    // mode = 3: check border 8 points

    makerDetector.getParameters().detectEnclosedMarkers(false);
    makerDetector.getParameters().ThresHold = 120;

    makerDetector.setDictionary((aruco::Dictionary::DICT_TYPES) 4, 0);//ARUCO_MIP_16h3
    makerDetector.getParameters().error_correction_rate = 3.0;

    vector<aruco::Marker> makers_detected;

    if (pNV21FrameData != NULL) {

        cv::Mat mGray(height, width, CV_8UC1, (unsigned char *) pNV21FrameData);
        cv::Mat mResult(pixel_height, pixel_width, CV_8UC4, (unsigned char *) poutPixels);

        cv::Mat imgRGB(height, width, CV_8UC4);

        makers_detected = makerDetector.detect(mGray);

        cv::cvtColor(mGray, imgRGB, cv::COLOR_GRAY2BGRA,
                     4);
        stringstream text_string;
        text_string << "demo: ";
        text_string << width;
        text_string << " x ";
        text_string << height;
        string out_string = text_string.str();
        cv::putText(imgRGB, out_string, cvPoint(40, 40),
                    cv::FONT_HERSHEY_DUPLEX, 1.2, cv::Scalar(0, 255, 0, 255), 1, CV_AA);
        for (int i = 0; i < makers_detected.size(); i++) {
            // draw robot fram
            makers_detected[i].draw(imgRGB, cv::Scalar(0, 255, 0, 255), 4, true, false);

            double position_angle = atan2((makers_detected[i][0].y + makers_detected[i][3].y) / 2 -
                              (makers_detected[i][1].y + makers_detected[i][2].y) / 2,
                              (makers_detected[i][0].x + makers_detected[i][3].x) / 2 -
                              (makers_detected[i][1].x + makers_detected[i][2].x) / 2) *
                        180 /
                        M_PI;
            arr.push_back(makers_detected[i].getCenter().x);
            arr.push_back(makers_detected[i].getCenter().y);
            arr.push_back(position_angle);
            arr.push_back(double(makers_detected[i].id));
        }

        cv::resize(imgRGB, mResult, cv::Size(mResult.cols, mResult.rows), 0, 0, CV_INTER_LINEAR);
        env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);
    }

    int arr_size = arr.size();
    //ALOG("JNI LOG image processing = %d.", arrSize);
    jdoubleArray result = env->NewDoubleArray(arr_size);//2 x 8
    jdouble *pResult = env->GetDoubleArrayElements(result, 0);
    for (int i = 0; i < arr_size; i++) {
        pResult[i] = (jdouble) arr[i];
    }
    env->SetDoubleArrayRegion(result, 0, arr_size, pResult);
    env->ReleaseIntArrayElements(outPixels, poutPixels, 0);
    return result;
}

}



