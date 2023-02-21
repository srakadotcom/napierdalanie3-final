cmake_minimum_required(VERSION 3.24)
project(untitled)

set(ENV{JAVA_HOME} "C:\\Users\\azeroy\\.jdks\\corretto-1.8.0_362")
set(CMAKE_CXX_STANDARD 17)

find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})

add_library(untitled SHARED main.cpp)

target_link_libraries(untitled ${JNI_LIBRARIES})