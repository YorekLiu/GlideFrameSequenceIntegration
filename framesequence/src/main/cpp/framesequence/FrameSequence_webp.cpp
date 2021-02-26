/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <string.h>
#include "JNIHelpers.h"
#include "utils/log.h"
#include "utils/math.h"
#include "../webp/src/webp/format_constants.h"
#include "FrameSequence_webp.h"
// update by yorek.liu >> begin TODO debug only
#define WEBP_DEBUG 0
//#define WEBP_DEBUG 1
// update by yorek.liu >> end
////////////////////////////////////////////////////////////////////////////////
// Frame sequence
////////////////////////////////////////////////////////////////////////////////
static uint32_t GetLE32(const uint8_t* const data) {
    return MKFOURCC(data[0], data[1], data[2], data[3]);
}
// Returns true if the frame covers full canvas.
static bool isFullFrame(const WebPIterator& frame, int canvasWidth, int canvasHeight) {
    return (frame.width == canvasWidth && frame.height == canvasHeight);
}
// Returns true if the rectangle defined by 'frame' contains pixel (x, y).
// update by yorek.liu >> begin
//static bool FrameContainsPixel(const WebPIterator& frame, int x, int y) {
//    const int left = frame.x_offset;
//    const int right = left + frame.width;
//    const int top = frame.y_offset;
//    const int bottom = top + frame.height;
//    return x >= left && x < right && y >= top && y < bottom;
//}
static bool FrameContainsPixel(const WebPIterator& frame, int x, int y, int sampleSize) {
    const int left = frame.x_offset / sampleSize;
    const int right = left + frame.width / sampleSize;
    const int top = frame.y_offset / sampleSize;
    const int bottom = top + frame.height / sampleSize;
    return x >= left && x < right && y >= top && y < bottom;
}
// update by yorek.liu >> end
// Construct mIsKeyFrame array.
void FrameSequence_webp::constructDependencyChain() {
    const size_t frameCount = getFrameCount();
    mIsKeyFrame = new bool[frameCount];
    const int canvasWidth = getWidth();
    const int canvasHeight = getHeight();
    WebPIterator prev;
    WebPIterator curr;
    // Note: WebPDemuxGetFrame() uses base-1 counting.
    int ok = WebPDemuxGetFrame(mDemux, 1, &curr);
    ALOG_ASSERT(ok, "Could not retrieve frame# 0");
    mIsKeyFrame[0] = true;  // 0th frame is always a key frame.
    for (size_t i = 1; i < frameCount; i++) {
        prev = curr;
        ok = WebPDemuxGetFrame(mDemux, i + 1, &curr);  // Get ith frame.
        ALOG_ASSERT(ok, "Could not retrieve frame# %d", i);
        if ((!curr.has_alpha || curr.blend_method == WEBP_MUX_NO_BLEND) &&
            isFullFrame(curr, canvasWidth, canvasHeight)) {
            mIsKeyFrame[i] = true;
        } else {
            mIsKeyFrame[i] = (prev.dispose_method == WEBP_MUX_DISPOSE_BACKGROUND) &&
                             (isFullFrame(prev, canvasWidth, canvasHeight) || mIsKeyFrame[i - 1]);
        }
    }
    WebPDemuxReleaseIterator(&prev);
    WebPDemuxReleaseIterator(&curr);
#if WEBP_DEBUG
    ALOGD("Dependency chain:");
    for (size_t i = 0; i < frameCount; i++) {
        ALOGD("Frame# %zu: %s", i, mIsKeyFrame[i] ? "Key frame" : "NOT a key frame");
    }
#endif
}
FrameSequence_webp::FrameSequence_webp(Stream* stream)
        : mDemux(NULL)
        , mIsKeyFrame(NULL)
        , mRawByteBuffer(NULL) {
    if (stream->getRawBuffer() != NULL) {
        mData.size = stream->getRawBufferSize();
        mData.bytes = stream->getRawBufferAddr();
        mRawByteBuffer = stream->getRawBuffer();
    } else {
        // Read RIFF header to get file size.
        uint8_t riff_header[RIFF_HEADER_SIZE];
        if (stream->read(riff_header, RIFF_HEADER_SIZE) != RIFF_HEADER_SIZE) {
            ALOGE("WebP header load failed");
            return;
        }
        uint32_t readSize = GetLE32(riff_header + TAG_SIZE);
        if (readSize > MAX_CHUNK_PAYLOAD) {
            ALOGE("WebP got header size too large");
            return;
        }
        mData.size = CHUNK_HEADER_SIZE + readSize;
        if(mData.size < RIFF_HEADER_SIZE) {
            ALOGE("WebP file malformed");
            return;
        }
        mData.bytes = new uint8_t[mData.size];
        memcpy((void*)mData.bytes, riff_header, RIFF_HEADER_SIZE);
        // Read rest of the bytes.
        void* remaining_bytes = (void*)(mData.bytes + RIFF_HEADER_SIZE);
        size_t remaining_size = mData.size - RIFF_HEADER_SIZE;
        if (stream->read(remaining_bytes, remaining_size) != remaining_size) {
            ALOGE("WebP full load failed");
            return;
        }
    }
    // Construct demux.
    mDemux = WebPDemux(&mData);
    if (!mDemux) {
        ALOGE("Parsing of WebP container file failed");
        return;
    }
    mLoopCount = WebPDemuxGetI(mDemux, WEBP_FF_LOOP_COUNT);
    mFormatFlags = WebPDemuxGetI(mDemux, WEBP_FF_FORMAT_FLAGS);
#if WEBP_DEBUG
    ALOGD("FrameSequence_webp created with size = %d x %d, number of frames = %d, flags = 0x%X",
          getWidth(), getHeight(), getFrameCount(), mFormatFlags);
#endif
    constructDependencyChain();
}
FrameSequence_webp::~FrameSequence_webp() {
    WebPDemuxDelete(mDemux);
    delete[] mIsKeyFrame;
    if (mRawByteBuffer == NULL) {
        delete[] mData.bytes;
    }
}
FrameSequenceState* FrameSequence_webp::createState() const {
    return new FrameSequenceState_webp(*this);
}
////////////////////////////////////////////////////////////////////////////////
// draw helpers
////////////////////////////////////////////////////////////////////////////////
static bool willBeCleared(const WebPIterator& iter) {
    return iter.dispose_method == WEBP_MUX_DISPOSE_BACKGROUND;
}
// return true if area of 'target' completely covers area of 'covered'
static bool checkIfCover(const WebPIterator& target, const WebPIterator& covered) {
    const int covered_x_max = covered.x_offset + covered.width;
    const int target_x_max = target.x_offset + target.width;
    const int covered_y_max = covered.y_offset + covered.height;
    const int target_y_max = target.y_offset + target.height;
    return target.x_offset <= covered.x_offset
           && covered_x_max <= target_x_max
           && target.y_offset <= covered.y_offset
           && covered_y_max <= target_y_max;
}
// Clear all pixels in a line to transparent.
static void clearLine(Color8888* dst, int width) {
    memset(dst, 0, width * sizeof(*dst));  // Note: Assumes TRANSPARENT == 0x0.
}
// Copy all pixels from 'src' to 'dst'.
static void copyFrame(const Color8888* src, int srcStride, Color8888* dst, int dstStride,
                      int width, int height) {
    for (int y = 0; y < height; y++) {
        memcpy(dst, src, width * sizeof(*dst));
        src += srcStride;
        dst += dstStride;
    }
}
////////////////////////////////////////////////////////////////////////////////
// Frame sequence state
////////////////////////////////////////////////////////////////////////////////
FrameSequenceState_webp::FrameSequenceState_webp(const FrameSequence_webp& frameSequence) :
        mFrameSequence(frameSequence) {
    WebPInitDecoderConfig(&mDecoderConfig);
    mDecoderConfig.output.is_external_memory = 1;
    mDecoderConfig.output.colorspace = MODE_rgbA;  // Pre-multiplied alpha mode.
    const int canvasWidth = mFrameSequence.getWidth();
    const int canvasHeight = mFrameSequence.getHeight();
    // update by yorek.liu >> begin
    mPreservedBufferSize = canvasWidth * canvasHeight;
    // update by yorek.liu >> end
    mPreservedBuffer = new Color8888[canvasWidth * canvasHeight];
}
FrameSequenceState_webp::~FrameSequenceState_webp() {
    delete[] mPreservedBuffer;
}
// update by yorek.liu >> begin
void FrameSequenceState_webp::initializeFrame(const WebPIterator& currIter, Color8888* currBuffer,
                                              int currStride, const WebPIterator& prevIter, const Color8888* prevBuffer, int prevStride, int sampleSize) {
// update by yorek.liu >> end
    const int canvasWidth = mFrameSequence.getWidth();
    const int canvasHeight = mFrameSequence.getHeight();
    // update by yorek.liu >> begin
    const int downsampledWidth = canvasWidth / sampleSize;
    const int downsampledHeight = canvasHeight / sampleSize;
    // update by yorek.liu >> end
    const bool currFrameIsKeyFrame = mFrameSequence.isKeyFrame(currIter.frame_num - 1);
    if (currFrameIsKeyFrame) {  // Clear canvas.
        // update by yorek.liu >> begin
        for (int y = 0; y < downsampledHeight; y++) {
        // update by yorek.liu >> end
            Color8888* dst = currBuffer + y * currStride;
            // update by yorek.liu >> begin
            clearLine(dst, downsampledWidth);
            // update by yorek.liu >> end
        }
    } else {
        // Preserve previous frame as starting state of current frame.
        // update by yorek.liu >> begin
        copyFrame(prevBuffer, prevStride, currBuffer, currStride, downsampledWidth, downsampledHeight);
        // update by yorek.liu >> end
        // Dispose previous frame rectangle to Background if needed.
        bool prevFrameCompletelyCovered =
                (!currIter.has_alpha || currIter.blend_method == WEBP_MUX_NO_BLEND) &&
                checkIfCover(currIter, prevIter);
        if ((prevIter.dispose_method == WEBP_MUX_DISPOSE_BACKGROUND) &&
            !prevFrameCompletelyCovered) {
            // update by yorek.liu >> begin
//            Color8888* dst = currBuffer + prevIter.x_offset + prevIter.y_offset * currStride;
//            for (int j = 0; j < prevIter.height; j++) {
//                clearLine(dst, prevIter.width);
//                dst += currStride;
//            }
            Color8888* dst = currBuffer + (prevIter.x_offset / sampleSize) + (prevIter.y_offset / sampleSize) * currStride;
            for (int j = 0; j < prevIter.height / sampleSize; j++) {
                clearLine(dst, prevIter.width / sampleSize);
                dst += currStride;
            }
            // update by yorek.liu >> end
        }
    }
}
// update by yorek.liu >> begin
bool FrameSequenceState_webp::decodeFrame(const WebPIterator& currIter, Color8888* currBuffer,
                                          int currStride, const WebPIterator& prevIter, const Color8888* prevBuffer, int prevStride, int sampleSize) {
// update by yorek.liu >> end
    // update by yorek.liu >> begin
//    Color8888* dst = currBuffer + currIter.x_offset + currIter.y_offset * currStride;
    Color8888* dst = currBuffer + (currIter.x_offset / sampleSize) + (currIter.y_offset / sampleSize) * currStride;
    // update by yorek.liu >> end
    mDecoderConfig.output.u.RGBA.rgba = (uint8_t*)dst;
    mDecoderConfig.output.u.RGBA.stride = currStride * 4;
    // update by yorek.liu >> begin
    mDecoderConfig.output.u.RGBA.size = mDecoderConfig.output.u.RGBA.stride * (currIter.height / sampleSize);
//    mDecoderConfig.output.is_external_memory = 1;
    // add by yorek.liu to scale
    if (sampleSize > 1) {
        mDecoderConfig.options.no_fancy_upsampling = 1;
        mDecoderConfig.options.use_scaling = 1;
        mDecoderConfig.options.scaled_width = currIter.width / sampleSize;
        mDecoderConfig.options.scaled_height = currIter.height / sampleSize;
    }
    // update by yorek.liu >> end
    const WebPData& currFrame = currIter.fragment;
    if (WebPDecode(currFrame.bytes, currFrame.size, &mDecoderConfig) != VP8_STATUS_OK) {
        // update by yorek.liu >> begin
        ALOGE("currIter: x_offset %d, y_offset %d, width %d, height %d, \n    scaledWidth %d, scaledHeight %d",
                currIter.x_offset, currIter.y_offset, currIter.width, currIter.height, mDecoderConfig.options.scaled_width, mDecoderConfig.options.scaled_height);
        // update by yorek.liu >> end
        return false;
    }
    const int canvasWidth = mFrameSequence.getWidth();
    const int canvasHeight = mFrameSequence.getHeight();
    const bool currFrameIsKeyFrame = mFrameSequence.isKeyFrame(currIter.frame_num - 1);
    // During the decoding of current frame, we may have set some pixels to be transparent
    // (i.e. alpha < 255). However, the value of each of these pixels should have been determined
    // by blending it against the value of that pixel in the previous frame if WEBP_MUX_BLEND was
    // specified. So, we correct these pixels based on disposal method of the previous frame and
    // the previous frame buffer.
    if (currIter.blend_method == WEBP_MUX_BLEND && !currFrameIsKeyFrame) {
        if (prevIter.dispose_method == WEBP_MUX_DISPOSE_NONE) {
            // update by yorek.liu >> begin
//            for (int y = 0; y < currIter.height; y++) {
//                const int canvasY = currIter.y_offset + y;
//                for (int x = 0; x < currIter.width; x++) {
//                    const int canvasX = currIter.x_offset + x;
//                    Color8888& currPixel = currBuffer[canvasY * currStride + canvasX];
//                    // FIXME: Use alpha-blending when alpha is between 0 and 255.
//                    if (!(currPixel & COLOR_8888_ALPHA_MASK)) {
//                        const Color8888 prevPixel = prevBuffer[canvasY * prevStride + canvasX];
//                        currPixel = prevPixel;
//                    }
//                }
//            }
            for (int y = 0; y < currIter.height / sampleSize; y++) {
                const int canvasY = (currIter.y_offset / sampleSize) + y;
                for (int x = 0; x < currIter.width / sampleSize; x++) {
                    const int canvasX = (currIter.x_offset / sampleSize) + x;
                    Color8888& currPixel = currBuffer[canvasY * currStride + canvasX];
                    // FIXME: Use alpha-blending when alpha is between 0 and 255.
                    if (!(currPixel & COLOR_8888_ALPHA_MASK)) {
                        const Color8888 prevPixel = prevBuffer[canvasY * prevStride + canvasX];
                        currPixel = prevPixel;
                    }
                }
            }
            // update by yorek.liu >> end
        } else {  // prevIter.dispose_method == WEBP_MUX_DISPOSE_BACKGROUND
            // Need to restore transparent pixels to as they were just after frame initialization.
            // That is:
            //   * Transparent if it belongs to previous frame rectangle <-- This is a no-op.
            //   * Pixel in the previous canvas otherwise <-- Need to restore.
            // update by yorek.liu >> begin
//            for (int y = 0; y < currIter.height; y++) {
//                const int canvasY = currIter.y_offset + y;
//                for (int x = 0; x < currIter.width; x++) {
//                    const int canvasX = currIter.x_offset + x;
//                    Color8888& currPixel = currBuffer[canvasY * currStride + canvasX];
//                    // FIXME: Use alpha-blending when alpha is between 0 and 255.
//                    if (!(currPixel & COLOR_8888_ALPHA_MASK)
//                        && !FrameContainsPixel(prevIter, canvasX, canvasY)) {
//                        const Color8888 prevPixel = prevBuffer[canvasY * prevStride + canvasX];
//                        currPixel = prevPixel;
//                    }
//                }
//            }
            for (int y = 0; y < currIter.height / sampleSize; y++) {
                const int canvasY = (currIter.y_offset / sampleSize) + y;
                for (int x = 0; x < currIter.width / sampleSize; x++) {
                    const int canvasX = (currIter.x_offset / sampleSize) + x;
                    Color8888& currPixel = currBuffer[canvasY * currStride + canvasX];
                    // FIXME: Use alpha-blending when alpha is between 0 and 255.
                    if (!(currPixel & COLOR_8888_ALPHA_MASK)
                        && !FrameContainsPixel(prevIter, canvasX, canvasY, sampleSize)) {
                        const Color8888 prevPixel = prevBuffer[canvasY * prevStride + canvasX];
                        currPixel = prevPixel;
                    }
                }
            }
            // update by yorek.liu >> end
        }
    }
    return true;
}
// update by yorek.liu >> begin
long FrameSequenceState_webp::drawFrame(int frameNr,
                                        Color8888* outputPtr, int outputPixelStride, int previousFrameNr, int sampleSize) {
// update by yorek.liu >> end
    WebPDemuxer* demux = mFrameSequence.getDemuxer();
    ALOG_ASSERT(demux, "Cannot drawFrame, mDemux is NULL");
#if WEBP_DEBUG
    ALOGD("  drawFrame called for frame# %d, previous frame# %d, sampleSize %d", frameNr, previousFrameNr, sampleSize);
#endif
    const int canvasWidth = mFrameSequence.getWidth();
    const int canvasHeight = mFrameSequence.getHeight();
    // update by yorek.liu >> begin
    const int downsampledWidth = canvasWidth / sampleSize;
    const int downsampledHeight = canvasHeight / sampleSize;
    if (sampleSize > 1 && mPreservedBuffer && (mPreservedBufferSize == canvasWidth * canvasHeight)) {
#if WEBP_DEBUG
        ALOGD("  delete the original mPreservedBuffer due to the sampleSize %d", sampleSize);
#endif
        delete[] mPreservedBuffer;
        mPreservedBufferSize = downsampledWidth * downsampledHeight;
        mPreservedBuffer = new Color8888[mPreservedBufferSize];
    }
    // update by yorek.liu >> end
    // Find the first frame to be decoded.
    int start = max(previousFrameNr + 1, 0);
    int earliestRequired = frameNr;
    while (earliestRequired > start) {
        if (mFrameSequence.isKeyFrame(earliestRequired)) {
            start = earliestRequired;
            break;
        }
        earliestRequired--;
    }
    WebPIterator currIter;
    WebPIterator prevIter;
    int ok = WebPDemuxGetFrame(demux, start, &currIter);  // Get frame number 'start - 1'.
    ALOG_ASSERT(ok, "Could not retrieve frame# %d", start - 1);
    // Use preserve buffer only if needed.
    Color8888* prevBuffer = (frameNr == 0) ? outputPtr : mPreservedBuffer;
    // update by yorek.liu >> begin
//    int prevStride = (frameNr == 0) ? outputPixelStride : canvasWidth;
    int prevStride = (frameNr == 0) ? outputPixelStride : downsampledWidth;
    // update by yorek.liu >> end
    Color8888* currBuffer = outputPtr;
    int currStride = outputPixelStride;
    for (int i = start; i <= frameNr; i++) {
        prevIter = currIter;
        ok = WebPDemuxGetFrame(demux, i + 1, &currIter);  // Get ith frame.
        ALOG_ASSERT(ok, "Could not retrieve frame# %d", i);
#if WEBP_DEBUG
        ALOGD("      producing frame %d (has_alpha = %d, dispose = %s, blend = %s, duration = %d)",
              i, currIter.has_alpha,
              (currIter.dispose_method == WEBP_MUX_DISPOSE_NONE) ? "none" : "background",
              (currIter.blend_method == WEBP_MUX_BLEND) ? "yes" : "no", currIter.duration);
#endif
        // We swap the prev/curr buffers as we go.
        Color8888* tmpBuffer = prevBuffer;
        prevBuffer = currBuffer;
        currBuffer = tmpBuffer;
        int tmpStride = prevStride;
        prevStride = currStride;
        currStride = tmpStride;
#if WEBP_DEBUG
        ALOGD("            prev = %p, curr = %p, out = %p, tmp = %p, prevStride = %d, currStride = %d",
              prevBuffer, currBuffer, outputPtr, mPreservedBuffer, prevStride, currStride);
#endif
        // Process this frame.
        // update by yorek.liu >> begin
        initializeFrame(currIter, currBuffer, currStride, prevIter, prevBuffer, prevStride, sampleSize);
//        initializeFrame(currIter, currBuffer, currStride, prevIter, prevBuffer, prevStride);
        // update by yorek.liu >> end
        if (i == frameNr || !willBeCleared(currIter)) {
            // update by yorek.liu >> begin
            if (!decodeFrame(currIter, currBuffer, currStride, prevIter, prevBuffer, prevStride, sampleSize)) {
            // update by yorek.liu >> end
                ALOGE("Error decoding frame# %d", i);
                return -1;
            }
        }
    }
    if (outputPtr != currBuffer) {
        // update by yorek.liu >> begin
//        copyFrame(currBuffer, currStride, outputPtr, outputPixelStride, canvasWidth, canvasHeight);
        copyFrame(currBuffer, currStride, outputPtr, outputPixelStride, downsampledWidth, downsampledHeight);
        // update by yorek.liu >> end
    }
    // Return last frame's delay.
    const int frameCount = mFrameSequence.getFrameCount();
    const int lastFrame = (frameNr + frameCount - 1) % frameCount;
    ok = WebPDemuxGetFrame(demux, lastFrame + 1, &currIter);
    ALOG_ASSERT(ok, "Could not retrieve frame# %d", lastFrame);
    const int lastFrameDelay = currIter.duration;
    WebPDemuxReleaseIterator(&currIter);
    WebPDemuxReleaseIterator(&prevIter);
    return lastFrameDelay;
}
////////////////////////////////////////////////////////////////////////////////
// Registry
////////////////////////////////////////////////////////////////////////////////
#include "Registry.h"
static bool isWebP(void* header, int header_size) {
    const uint8_t* const header_str = (const uint8_t*)header;
    return (header_size >= RIFF_HEADER_SIZE) &&
           !memcmp("RIFF", header_str, 4) &&
           !memcmp("WEBP", header_str + 8, 4);
}
static bool acceptsWebPBuffer() {
    return true;
}
static FrameSequence* createFramesequence(Stream* stream) {
    return new FrameSequence_webp(stream);
}
static RegistryEntry gEntry = {
        RIFF_HEADER_SIZE,
        isWebP,
        createFramesequence,
        NULL,
        acceptsWebPBuffer,
};
static Registry gRegister(gEntry);