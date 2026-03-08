#pragma once

#include <filesystem>
#include <result.hpp>
#include <variant>
#include <absl/container/flat_hash_map.h>
#include <mc_cpp/registry/minecraft.hpp>
#include <mc_cpp/common/mutex.hpp>
#include <zvcr/region/dim3/segment3.hpp>

namespace placeviewer {

    namespace fs = std::filesystem;

    inline absl::flat_hash_map<mc::BlockState, mc::TileEntityEntry> tileEntityBlockStates;
    inline mc::BlockState airBlockState;
    inline mc::BiomeType voidBiome;

    struct RegionBuffers {
        uint64_t regionObjectID{};
        absl::flat_hash_map<uint64_t, std::shared_ptr<zvcr::Region3d>> regions{};
    };

    inline mc::Mutex<RegionBuffers> REGION_BUFFERS;

    auto initialize(const fs::path &registryDirectory) -> result::Result<std::monostate, std::string>;

}
