cmake_minimum_required(VERSION 3.22.1)
project("crazycat_crypto")

add_library(keeloq_jni SHARED keeloq_jni.cpp)
target_link_libraries(keeloq_jni android log)
