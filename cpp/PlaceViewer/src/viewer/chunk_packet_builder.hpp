#pragma once

#include <zvcr/region/dim3/segment3.hpp>
#include <mc_cpp/types.hpp>
#include <mc_cpp/game/block.hpp>
#include <mc_cpp/game/biome.hpp>

namespace placeviewer {

    [[nodiscard]]
    auto createChunkDataPacket(const zvcr::Region3d &region, zvcr::DimensionType dimensionType,
                               int32_t absChunkX, int32_t absChunkZ, time_t timestamp) -> mc::ByteBuf;

    [[nodiscard]]
    auto createEmptyChunkData(zvcr::DimensionType dimensionType) -> mc::ByteBuf;

}
