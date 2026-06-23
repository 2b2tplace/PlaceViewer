#pragma once

#include <zvcr/region/segment.hpp>
#include <mc_cpp/types.hpp>

namespace placeviewer {

    [[nodiscard]]
    auto createChunkDataPacket(const zvcr::Region &region, zvcr::DimensionType dimensionType,
                               int32_t absChunkX, int32_t absChunkZ, time_t timestamp) -> mc::ByteBuf;

    [[nodiscard]]
    auto createEmptyChunkData(zvcr::DimensionType dimensionType) -> mc::ByteBuf;

}
