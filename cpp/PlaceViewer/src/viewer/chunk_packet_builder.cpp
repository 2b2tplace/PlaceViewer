#include <viewer/chunk_packet_builder.hpp>
#include <viewer/placeviewer.hpp>
#include <mc_cpp/anvil/anvil_chunk.hpp>
#include <protocolCraft/BinaryReadWrite.hpp>
#include <protocolCraft/Types/BlockEntityInfo.hpp>

namespace placeviewer {

    namespace pc = ProtocolCraft;

    [[nodiscard]]
    inline auto regionalChunkCoordinate(const int32_t chunkCoordinate) -> int8_t {
        auto regionalChunkCoordinate = static_cast<int8_t>(chunkCoordinate % static_cast<int32_t>(zvcr::REGION_SIDELENGTH_SEGMENTS));

        if (regionalChunkCoordinate < 0)
            regionalChunkCoordinate += zvcr::REGION_SIDELENGTH_SEGMENTS;

        return regionalChunkCoordinate;
    }

    template<typename SectionDataType>
    void buildPalette(mc::anvil::Palette &palette, mc::anvil::PaletteIndices &indices, const mc::anvil::UnpackedData<SectionDataType> &unpacked) {
        std::bitset<UINT16_MAX + 1> unique;
        for (const auto atom : unpacked) {
            if (unique.test(atom)) continue;
            unique.set(atom);

            indices[atom] = static_cast<uint16_t>(palette.size());
            palette.push_back(atom);
        }
    }

    template<typename SectionDataType>
    auto packPalettedData(mc::anvil::PackedData &data, const uint64_t bitsPerEntry,
                          const std::function<auto(size_t) -> int32_t> &valueGetter) -> void {
        const auto entryMask = (static_cast<uint64_t>(1) << bitsPerEntry) - 1;
        const auto entriesPerLong = 64 / bitsPerEntry;
        const auto packedSize = (mc::anvil::SectionDataInfo<SectionDataType>::sectionSize + entriesPerLong - 1) / entriesPerLong;

        data.resize(packedSize);
        for (size_t i = 0; i < mc::anvil::SectionDataInfo<SectionDataType>::sectionSize; i++) {
            const auto longIndex = i / entriesPerLong;
            const auto bitIndex = i % entriesPerLong * bitsPerEntry;

            const auto packedValue = static_cast<int64_t>(valueGetter(i) & entryMask);

            assert(longIndex < data.size());
            data[longIndex] &= ~(static_cast<int64_t>(entryMask) << bitIndex);
            data[longIndex] |= packedValue << bitIndex;
        }
    }

    using Palette = std::variant<mc::BlockState, mc::anvil::Palette>;

    template<typename SectionDataType>
    auto writePalettedContainer(const mc::anvil::UnpackedData<SectionDataType> &unpacked, mc::ByteBuf &buffer) -> Palette {
        mc::anvil::PaletteIndices indices;
        mc::anvil::Palette palette;
        buildPalette<SectionDataType>(palette, indices, unpacked);

        if (palette.size() == 1) { // single valued palette
            pc::WriteData<uint8_t>(0, buffer); // single valued palette, bits per entry = 0
            pc::WriteData<pc::VarInt>(palette[0], buffer); // single valued palette
            pc::WriteData<pc::VarInt>(0, buffer); // NOTE packed data length is no longer sent in 1.21.5
            return palette[0];
        }
        auto bitsPerEntry = mc::anvil::getBitsPerIndex<SectionDataType>(palette.size());
        std::function<auto(size_t) -> int32_t> valueGetter = [&](const size_t i) {
            assert(i < unpacked.size());
            assert(unpacked[i] < indices.size());
            return indices[unpacked[i]];
        };
        if (bitsPerEntry > mc::anvil::SectionDataInfo<SectionDataType>::paletteMaxBits) {
            bitsPerEntry = 15; // direct palette
            valueGetter = [&](const size_t i) {
                assert(i < unpacked.size());
                return unpacked[i];
            };
        }
        pc::WriteData<uint8_t>(static_cast<uint8_t>(bitsPerEntry), buffer);
        pc::WriteData<pc::VarInt>(static_cast<int32_t>(palette.size()), buffer);
        for (const auto entry : palette)
            pc::WriteData<pc::VarInt>(entry, buffer);

        mc::anvil::PackedData packedData;
        packPalettedData<SectionDataType>(packedData, bitsPerEntry, valueGetter);
        pc::WriteData<pc::VarInt>(static_cast<int32_t>(packedData.size()), buffer); // NOTE packed data length is no longer sent in 1.21.5
        for (const auto packedEntry : packedData)
            pc::WriteData<int64_t>(packedEntry, buffer);

        return palette;
    }

    auto writeFullBrightLightData(mc::ByteBuf &buffer, const size_t sectionCount) -> void {
        int64_t lightMask{};
        for (size_t i = 1; i <= sectionCount; i++) {
            lightMask |= 1 << i;
        }
        pc::WriteData<pc::VarInt>(1, buffer);
        pc::WriteData<int64_t>(lightMask, buffer);

        pc::WriteData<pc::VarInt>(1, buffer);
        pc::WriteData<int64_t>(lightMask, buffer);

        pc::WriteData<pc::VarInt>(0, buffer);
        pc::WriteData<pc::VarInt>(0, buffer);

        static const std::vector<uint8_t> FULL_BRIGHT_LIGHT_NIBBLES(zvcr::SECTION_3D_SIZE_BLOCKS / 2, 0xFF);
        for (uint8_t i = 0; i < 2; ++i) {
            pc::WriteData<pc::VarInt>(static_cast<int32_t>(sectionCount), buffer);
            for (size_t sectionIdx = 0; sectionIdx < sectionCount; ++sectionIdx) {
                pc::WriteData<pc::VarInt>(zvcr::SECTION_3D_SIZE_BLOCKS / 2, buffer);
                pc::WriteByteArray(FULL_BRIGHT_LIGHT_NIBBLES, buffer);
            }
        }
    }

    auto createEmptyChunkData(const zvcr::DimensionType dimensionType) -> mc::ByteBuf {
        const auto sectionCount = getProperties(dimensionType).height / zvcr::SEGMENT_SIDELENGTH_BLOCKS;
        mc::ByteBuf buffer;

        // heightmaps can be left empty
        pc::WriteData<uint8_t>(10, buffer);
        pc::WriteData<uint8_t>(0, buffer);

        mc::ByteBuf data;
        for (size_t sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            auto blockSection = mc::anvil::UnpackedData<mc::anvil::Blocks>{};
            auto biomeSection = mc::anvil::UnpackedData<mc::anvil::Biomes>{};

            blockSection.fill(airBlockState);
            biomeSection.fill(voidBiome);

            pc::WriteData<int16_t>(0, data);
            writePalettedContainer<mc::anvil::Blocks>(blockSection, data);
            writePalettedContainer<mc::anvil::Biomes>(biomeSection, data);
        }
        pc::WriteData<pc::VarInt>(static_cast<int32_t>(data.size()), buffer);
        pc::WriteByteArray(data, buffer);

        pc::WriteData<pc::VarInt>(0, buffer); // no tile entities
        writeFullBrightLightData(buffer, sectionCount);
        return buffer;
    }

    auto createChunkDataPacket(const zvcr::Region3d &region, const zvcr::DimensionType dimensionType,
                               const int32_t absChunkX, const int32_t absChunkZ, const time_t timestamp) -> mc::ByteBuf {
        const auto relChunkX = regionalChunkCoordinate(absChunkX);
        const auto relChunkZ = regionalChunkCoordinate(absChunkZ);
        const auto &segment = region.get(relChunkX, relChunkZ);
        if (!segment) return {};

        const auto minChunkY = getProperties(dimensionType).minY / zvcr::SEGMENT_SIDELENGTH_BLOCKS;
        const auto sectionCount = getProperties(dimensionType).height / zvcr::SEGMENT_SIDELENGTH_BLOCKS;

        mc::ByteBuf buffer;
        pc::WriteData<int32_t>(absChunkX, buffer);
        pc::WriteData<int32_t>(absChunkZ, buffer);

        // heightmaps can be left empty
        pc::WriteData<uint8_t>(10, buffer);
        pc::WriteData<uint8_t>(0, buffer);

        std::vector<pc::BlockEntityInfo> tileEntities;

        mc::ByteBuf data;
        for (size_t sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            const auto &blockSection = segment->blockSections.sections[sectionIndex].snapshotFrom(timestamp);
            const auto &biomeSection = segment->biomeSections.sections[sectionIndex].snapshotFrom(timestamp);
            if (!blockSection || !biomeSection) return {};

            pc::WriteData<int16_t>(zvcr::SECTION_3D_SIZE_BLOCKS, data);
            const auto blockPalette = writePalettedContainer<mc::anvil::Blocks>(*blockSection, data);
            writePalettedContainer<mc::anvil::Biomes>(*biomeSection, data);

            bool addTileEntities{};
            if (std::holds_alternative<mc::anvil::Palette>(blockPalette)) {
                const auto &palette = std::get<mc::anvil::Palette>(blockPalette);
                for (const auto value : palette) {
                    if (tileEntityBlockStates.contains(value)) {
                        addTileEntities = true;
                        break;
                    }
                }
            } else if (std::holds_alternative<mc::BlockState>(blockPalette)) {
                const auto singleValue = std::get<mc::BlockState>(blockPalette);
                addTileEntities = tileEntityBlockStates.contains(singleValue);
            }
            if (addTileEntities) {
                zvcr::UnpackedView view{zvcr::SEGMENT_SIDELENGTH_BLOCKS, *blockSection};
                for (uint8_t by = 0; by < zvcr::SEGMENT_SIDELENGTH_BLOCKS; by++) {
                    for (uint8_t bz = 0; bz < zvcr::SEGMENT_SIDELENGTH_BLOCKS; bz++) {
                        for (uint8_t bx = 0; bx < zvcr::SEGMENT_SIDELENGTH_BLOCKS; bx++) {
                            const auto state = view.getVoxel(bx, by, bz);
                            if (!tileEntityBlockStates.contains(state)) continue;

                            const auto &tileEntity = tileEntityBlockStates.at(state);
                            const auto absY = zvcr::SEGMENT_SIDELENGTH_BLOCKS * (static_cast<int>(sectionIndex) + minChunkY) + by;

                            pc::BlockEntityInfo info{};
                            info.SetPackedXZ((bx & 15) << 4 | bz & 15);
                            info.SetY(static_cast<int16_t>(absY));
                            info.SetType(tileEntity.id);

                            tileEntities.push_back(info);
                        }
                    }
                }
            }
        }
        pc::WriteData<pc::VarInt>(static_cast<int32_t>(data.size()), buffer);
        pc::WriteByteArray(data, buffer);

        pc::WriteData<std::vector<pc::BlockEntityInfo>>(tileEntities, buffer);

        writeFullBrightLightData(buffer, segment->blockSections.sectionCount);
        return buffer;
    }

}
