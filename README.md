# aruco-opencv-android
Aruco 3.0 , OpenCV on Android

![aruco image](/aruco_mip_16h3_00009.png)

C++ library: https://www.uco.es/investiga/grupos/ava/node/26

aruco images download: https://tuielectronics.com/download/aruco/Aruco_3.0_lib.zip

apk download: https://tuielectronics.com/download/aruco/aruco_demo.apk

## Android OpenCV environment wizard:
* 1, download OpenCV SDK from https://opencv.org/releases/
* 2, create folder main/jniLibs, download OpenCV SDK, and copy arm64-v8a and armeabi-v7a folder into it (libopencv_java3.so).
* 3, create folder /main//CPP, copy /include folder into it.
* 4, if NDK is not setup yet, copy/overwrite https://github.com/tuielectronics/aruco-opencv-android/blob/master/app/CMakeLists.txt to the app folder, then Android Studio -> File -> Link C++ project with Gradle (), select this CmakeList file.
* 5, edit Build.gradle, inside `defaultConfig`, add 
``` xml
    externalNativeBuild {
            cmake {
                cppFlags "-std=c++11 -fexceptions"
            }
            ndk {
                abiFilters 'armeabi-v7a','arm64-v8a'
            }
    }
``` 
* 6, create your .CPP inside main/CPP/
* 7, copy the related library source into main/CPP/include/
* 8, modify CmakeList file based on the files you added.
