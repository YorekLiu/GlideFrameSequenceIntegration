# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.10.2)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.
INCLUDE_DIRECTORIES(.)

FILE(
        GLOB
        SRC_LIST
        "src/dec/alpha_dec.c"
        "src/dec/buffer_dec.c"
        "src/dec/frame_dec.c"
        "src/dec/idec_dec.c"
        "src/dec/io_dec.c"
        "src/dec/quant_dec.c"
        "src/dec/tree_dec.c"
        "src/dec/vp8_dec.c"
        "src/dec/vp8l_dec.c"
        "src/dec/webp_dec.c"
        "src/demux/anim_decode.c"
        "src/demux/demux.c"
        "src/dsp/alpha_processing.c"
        "src/dsp/alpha_processing_neon.c"
        "src/dsp/alpha_processing_sse2.c"
        "src/dsp/alpha_processing_sse41.c"
        "src/dsp/cpu.c"
        "src/dsp/dec.c"
        "src/dsp/dec_clip_tables.c"
        "src/dsp/dec_neon.c"
        "src/dsp/dec_sse2.c"
        "src/dsp/dec_sse41.c"
        "src/dsp/filters.c"
        "src/dsp/filters_neon.c"
        "src/dsp/filters_sse2.c"
        "src/dsp/lossless.c"
        "src/dsp/lossless_neon.c"
        "src/dsp/lossless_sse2.c"
        "src/dsp/rescaler.c"
        "src/dsp/rescaler_neon.c"
        "src/dsp/rescaler_sse2.c"
        "src/dsp/ssim.c"
        "src/dsp/ssim_sse2.c"
        "src/dsp/upsampling.c"
        "src/dsp/upsampling_neon.c"
        "src/dsp/upsampling_sse2.c"
        "src/dsp/upsampling_sse41.c"
        "src/dsp/yuv.c"
        "src/dsp/yuv_neon.c"
        "src/dsp/yuv_sse2.c"
        "src/dsp/yuv_sse41.c"
        "src/utils/bit_reader_utils.c"
        "src/utils/color_cache_utils.c"
        "src/utils/filters_utils.c"
        "src/utils/huffman_utils.c"
        "src/utils/quant_levels_dec_utils.c"
        "src/utils/random_utils.c"
        "src/utils/rescaler_utils.c"
        "src/utils/thread_utils.c"
        "src/utils/utils.c"
        "src/mux/muxedit.c"
        "src/mux/muxinternal.c"
        "src/mux/muxread.c"
)

add_definitions(-Wall -DANDROID -DHAVE_MALLOC_H -DHAVE_PTHREAD -DWEBP_USE_THREAD -DWEBP_FORCE_ALIGNED)

link_directories(src)

add_library( # Specifies the name of the library.
        webp-decoder

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SRC_LIST})

## Specifies libraries CMake should link to your target library. You
## can link multiple libraries, such as libraries you define in the
## build script, prebuilt third-party libraries, or system libraries.

include(AndroidNdkModules)
android_ndk_import_module_cpufeatures()

target_link_libraries( # Specifies the target library.
        webp-decoder

        # Links the target library to the log library
        # included in the NDK.
        cpufeatures
        log)