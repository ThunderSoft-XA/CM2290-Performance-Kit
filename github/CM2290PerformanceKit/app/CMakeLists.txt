# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/libs/${ANDROID_ABI})

ADD_SUBDIRECTORY(src/main/cpp/libbitmap)
ADD_SUBDIRECTORY(src/main/cpp/libyuv)
ADD_SUBDIRECTORY(src/main/cpp/BasicUsageEnvironment)
ADD_SUBDIRECTORY(src/main/cpp/groupsock)
ADD_SUBDIRECTORY(src/main/cpp/liveMedia)
ADD_SUBDIRECTORY(src/main/cpp/UsageEnvironment)
ADD_SUBDIRECTORY(src/main/cpp/RtspClient)
ADD_SUBDIRECTORY(src/main/jni)
ADD_SUBDIRECTORY(src/main/cpp)

add_library(affinitylib SHARED src/main/cpp/Affinity/AffinityC.cpp)


LINK_DIRECTORIES(${PROJECT_SOURCE_DIR}/libs/${ANDROID_ABI})


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

target_link_libraries(
        affinitylib
        ${log-lib}
)
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#add_library( # Sets the name of the library.
#             native-lib

# Sets the library as a shared library.
#             SHARED

# Provides a relative path to your source file(s).
#             src/main/cpp/native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#target_link_libraries( # Specifies the target library.
#                       native-lib

# Links the target library to the log library
# included in the NDK.
#                       ${log-lib} )

set(TFLITE_LIBPATH "${CMAKE_CURRENT_SOURCE_DIR}/../libraries/tensorflowlite/jni")
set(TFLITE_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/../libraries/tensorflowlite/headers")
set(TFLITE_GPU_LIBPATH "${CMAKE_CURRENT_SOURCE_DIR}/../libraries/tensorflowlite-gpu/jni")
set(TFLITE_GPU_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/../libraries/tensorflowlite-gpu/headers")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++14")
set(CMAKE_CXX_STANDARD 14)

add_library(SuperResolution SHARED SuperResolution_jni.cpp SuperResolution.cpp)

add_library(lib_tensorflowlite SHARED IMPORTED)
set_target_properties(lib_tensorflowlite PROPERTIES IMPORTED_LOCATION
        ${TFLITE_LIBPATH}/${ANDROID_ABI}/libtensorflowlite_jni.so)

add_library(lib_tensorflowlite_gpu SHARED IMPORTED)
set_target_properties(lib_tensorflowlite_gpu PROPERTIES IMPORTED_LOCATION
        ${TFLITE_GPU_LIBPATH}/${ANDROID_ABI}/libtensorflowlite_gpu_jni.so)

find_library(log-lib log)

include_directories(${TFLITE_INCLUDE})
target_include_directories(SuperResolution PRIVATE
        ${TFLITE_INCLUDE})

include_directories(${TFLITE_GPU_INCLUDE})

target_include_directories(SuperResolution PRIVATE
        ${TFLITE_GPU_INCLUDE})

target_link_libraries(SuperResolution
        android
        lib_tensorflowlite
        lib_tensorflowlite_gpu
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
