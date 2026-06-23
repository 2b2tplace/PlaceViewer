cmake_minimum_required(VERSION 3.25)
project(PlaceViewer)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_FLAGS "-fPIC")
set(BOTCRAFT_GAME_VERSION "1.21.4" CACHE STRING "Minecraft game version")
set(PROTOCOLCRAFT_STATIC ON)
find_package(JNI REQUIRED)

file(GLOB_RECURSE LIBS "src/*.hpp" "src/*.h" "src/*.cpp" "src/*.c")
include_directories(PlaceViewer ${CMAKE_CURRENT_SOURCE_DIR}/src)
include_directories(${JNI_INCLUDE_DIRS})

include(FetchContent)
FetchContent_Declare(zvcr
        GIT_REPOSITORY git@github.com:2b2tplace/zvcr.git
        GIT_TAG        1.0.0.0
        GIT_PROGRESS TRUE
        GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(zvcr)

FetchContent_Declare(mc_cpp
        GIT_REPOSITORY git@github.com:2b2tplace/mc-cpp.git
        GIT_TAG        0.1.12.6
        GIT_PROGRESS TRUE
        GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(mc_cpp)

add_library(PlaceViewer SHARED ${LIBS})
target_include_directories(PlaceViewer PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/src)
target_link_libraries(PlaceViewer
        PRIVATE mc_cpp
        PRIVATE absl::flat_hash_map
        PRIVATE zvcr_lib
        PRIVATE protocolCraft
)
target_compile_options(PlaceViewer PRIVATE $<$<CONFIG:Release>:-O3>)
set_target_properties(PlaceViewer PROPERTIES OUTPUT_NAME "PlaceViewer-${CMAKE_SYSTEM_PROCESSOR}")