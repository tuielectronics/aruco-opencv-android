# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.0)

set(CMAKE_VERBOSE_MAKEFILE on)
set(cvlibs "${CMAKE_SOURCE_DIR}/src/main/jniLibs")
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

add_library(libopencv_java3 SHARED IMPORTED)
set_target_properties(libopencv_java3 PROPERTIES
        IMPORTED_LOCATION "${cvlibs}/${ANDROID_ABI}/libopencv_java3.so")

set(arucoDir "${CMAKE_SOURCE_DIR}/src/main/cpp/include/aruco3")
set(arucoSources
        ${arucoDir}/cameraparameters.cpp
        ${arucoDir}/cvdrawingutils.cpp
        ${arucoDir}/dictionary.cpp
        ${arucoDir}/dictionary_based.cpp
        ${arucoDir}/ippe.cpp
        ${arucoDir}/marker.cpp
        ${arucoDir}/markerdetector_impl.cpp
        ${arucoDir}/markerdetector.cpp
        ${arucoDir}/markerlabeler.cpp
        ${arucoDir}/markermap.cpp

        #${arucoDir}/markerlabelers/dictionary_based.cpp
        #${arucoDir}/posetracker.cpp
        #${arucoDir}/debug.cpp
        #${arucoDir}/markerlabelers/svmmarkers.cpp
        )

set(arucoHeaders
        ${arucoDir}/aruco_export.h
        ${arucoDir}/cameraparameters.h
        ${arucoDir}/cvdrawingutils.h
        ${arucoDir}/dictionary.h
        ${arucoDir}/dictionary_based.h
        ${arucoDir}/ippe.h
        ${arucoDir}/marker.h
        ${arucoDir}/markerdetector_impl.h
        ${arucoDir}/markerdetector.h
        ${arucoDir}/markerlabeler.h
        ${arucoDir}/markermap.h
        ${arucoDir}/aruco.h

        #${arucoDir}/markerlabelers/dictionary_based.h
        #${arucoDir}/posetracker.h
        #${arucoDir}/aruco_export.h
        #${arucoDir}/timers.h
        #${arucoDir}/debug.h
        #${arucoDir}/markerlabelers/svmmarkers.h
        )

add_library(libaruco3 ${arucoSources} ${arucoHeaders})

add_library(imageProcess SHARED src/main/cpp/cameraOpenCV.cpp)


find_library( # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        imageProcess libopencv_java3 libaruco3

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})