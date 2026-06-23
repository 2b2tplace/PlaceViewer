#include <viewer/jni/jni_native_region3d.h>
#include <viewer/placeviewer.hpp>
#include <viewer/chunk_packet_builder.hpp>
#include <zvcr/dimension.hpp>
#include <zvcr/io/file_location.hpp>
#include <zvcr/io/serialize/deserialize.hpp>
#include <filesystem>
#include <malloc.h>

JNIEXPORT void JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_initialize(JNIEnv *jEnv, jclass, jstring jRegistryDirectory) {
    namespace fs = std::filesystem;

    const char* registryDirectoryCString = jEnv->GetStringUTFChars(jRegistryDirectory, nullptr);
    if (!registryDirectoryCString) return;

    const auto registryDirectory = fs::path{registryDirectoryCString};
    jEnv->ReleaseStringUTFChars(jRegistryDirectory, registryDirectoryCString);

    const auto success = placeviewer::initialize(registryDirectory);
    if (success) return;

    const auto jExceptionClass = jEnv->FindClass("dev/place/placeviewer/systems/region/jni/NativeRegionException");
    if (!jExceptionClass) return;

    const auto jConstructor = jEnv->GetMethodID(jExceptionClass, "<init>", "(Ljava/lang/String;I)V");
    if (!jConstructor) return;

    const auto jErrorMessage = jEnv->NewStringUTF(success.error().c_str());
    const auto jException = jEnv->NewObject(jExceptionClass, jConstructor, jErrorMessage, 1);

    jEnv->Throw(reinterpret_cast<jthrowable>(jException));
}

JNIEXPORT jlong JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_newRegionFromDisk(JNIEnv *jEnv, jclass, const jstring jParentDirectory, const jint rx, const jint rz, const jint jDimensionType) {
    namespace fs = std::filesystem;

    const char* parentDirectoryCString = jEnv->GetStringUTFChars(jParentDirectory, nullptr);
    if (!parentDirectoryCString) return 0L;

    const auto parentDirectory = fs::path{parentDirectoryCString};
    jEnv->ReleaseStringUTFChars(jParentDirectory, parentDirectoryCString);

    const auto dimensionType = static_cast<zvcr::DimensionType>(jDimensionType);
    const auto location = zvcr::RegionLocation{rx, rz, dimensionType};

    const auto regionFile = zvcr::readFileAt(parentDirectory, location);
    if (!regionFile) {
        const auto &errorMessage = regionFile.error().what();
        const auto errorCode = static_cast<int>(regionFile.error().type);

        const auto jExceptionClass = jEnv->FindClass("dev/place/placeviewer/systems/region/jni/NativeRegionException");
        if (!jExceptionClass) return 0L;

        const auto jConstructor = jEnv->GetMethodID(jExceptionClass, "<init>", "(Ljava/lang/String;I)V");
        if (!jConstructor) return 0L;

        const auto jErrorMessage = jEnv->NewStringUTF(errorMessage.c_str());
        const auto jException = jEnv->NewObject(jExceptionClass, jConstructor, jErrorMessage, errorCode);

        jEnv->Throw(reinterpret_cast<jthrowable>(jException));
        return 0L;
    }
    // copy constructor pretty trivial; array of 32 x 32 pointers.
    const auto sharedRegion = std::make_shared<zvcr::Region>(regionFile->region);
    const auto regionObjectID = placeviewer::REGION_BUFFERS.write([&](placeviewer::RegionBuffers &regionBuffers) {
        const auto id = ++regionBuffers.regionObjectID;
        regionBuffers.regions[id] = sharedRegion;
        return id;
    });
    return static_cast<jlong>(regionObjectID);
}

JNIEXPORT void JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_freeRegion(JNIEnv*, jclass, const jlong jRegionObjectID) {
    const auto regionObjectID = static_cast<uint64_t>(jRegionObjectID);
    placeviewer::REGION_BUFFERS.write([&](placeviewer::RegionBuffers &regionBuffers) {
        // shared_ptr will get its memory freed when it is no longer used by any thread. does not necessarily happen right here
        regionBuffers.regions.erase(regionObjectID);
    });
}

JNIEXPORT jbyteArray JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_createChunkDataPacket(JNIEnv *jEnv, jclass, const jlong jRegionObjectID,
    const jint jDimensionType, const jint absChunkX, const jint absChunkZ, const jlong timestamp) {
    const auto regionObjectID = static_cast<uint64_t>(jRegionObjectID);
    if (!regionObjectID) return nullptr;

    const auto regionPtr = placeviewer::REGION_BUFFERS.read([&](const placeviewer::RegionBuffers &regionBuffers) {
        return regionBuffers.regions.contains(regionObjectID) ? regionBuffers.regions.at(regionObjectID) : nullptr;
    });
    if (!regionPtr) return nullptr;

    const auto dimensionType = static_cast<zvcr::DimensionType>(jDimensionType);
    const auto packetByteBuf = placeviewer::createChunkDataPacket(*regionPtr, dimensionType, absChunkX, absChunkZ, timestamp);
    if (packetByteBuf.empty()) return nullptr;

    const auto len = static_cast<jsize>(packetByteBuf.size());
    const auto jPacketByteBuf = jEnv->NewByteArray(len);
    if (!jPacketByteBuf) return nullptr;

    jEnv->SetByteArrayRegion(jPacketByteBuf, 0, len, reinterpret_cast<const jbyte*>(packetByteBuf.data()));
    return jPacketByteBuf;
}

JNIEXPORT jbyteArray JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_createEmptyChunkData(JNIEnv *jEnv, jclass, const jint jDimensionType) {
    const auto dimensionType = static_cast<zvcr::DimensionType>(jDimensionType);
    const auto packetByteBuf = placeviewer::createEmptyChunkData(dimensionType);

    const auto len = static_cast<jsize>(packetByteBuf.size());
    const auto jPacketByteBuf = jEnv->NewByteArray(len);
    if (!jPacketByteBuf) return nullptr;

    jEnv->SetByteArrayRegion(jPacketByteBuf, 0, len, reinterpret_cast<const jbyte*>(packetByteBuf.data()));
    return jPacketByteBuf;
}

JNIEXPORT jlongArray JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_indexRegionEpochs(JNIEnv *jEnv, jclass,
    const jlong jRegionObjectID, const jint relChunkX, const jint relChunkZ) {
    const auto regionObjectID = static_cast<uint64_t>(jRegionObjectID);
    if (!regionObjectID) return nullptr;

    const auto regionPtr = placeviewer::REGION_BUFFERS.read([&](const placeviewer::RegionBuffers &regionBuffers) {
        return regionBuffers.regions.contains(regionObjectID) ? regionBuffers.regions.at(regionObjectID) : nullptr;
    });
    if (!regionPtr) return nullptr;

    std::unordered_set<time_t> epochSet;
    epochSet.reserve(zvcr::MAX_SECTION_COUNT);
    const auto &segment = regionPtr->get(static_cast<uint8_t>(relChunkX), static_cast<uint8_t>(relChunkZ));
    if (!segment) return nullptr;

    for (const auto &section : segment->blockSections.sections) {
        for (const auto &[_, timestamp] : section.reverseDeltas) {
            epochSet.emplace(timestamp * 1000L); // seconds -> millis
        }
    }
    for (const auto &[timestamp, _] : segment->tileEntities.reverseDeltas) {
        epochSet.emplace(timestamp * 1000L); // seconds -> millis
    }
    const auto epochs = std::vector<time_t>{epochSet.begin(), epochSet.end()};
    const auto len = static_cast<jsize>(epochs.size());
    const auto jEpochs = jEnv->NewLongArray(len);
    if (!jEpochs) return nullptr;

    jEnv->SetLongArrayRegion(jEpochs, 0, len, reinterpret_cast<const jlong*>(epochs.data()));
    return jEpochs;
}

JNIEXPORT void JNICALL Java_dev_place_placeviewer_systems_region_jni_NativeRegion_mallocTrim(JNIEnv *, jclass) {
    malloc_trim(0);
}