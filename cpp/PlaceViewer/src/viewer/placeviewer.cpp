#include <viewer/placeviewer.hpp>

namespace placeviewer {

    auto initialize(const fs::path &registryDirectory) -> result::Result<std::monostate, std::string> {
        TRY(mc::loadRegistries(registryDirectory, std::vector{mc::RELEASE_1_21_4}));
        const auto &registry = mc::getRegistry(mc::RELEASE_1_21_4);

        for (const auto &tileEntity : registry.tileEntities.tileEntitiesById | std::views::values) {
            for (const auto state : tileEntity.blockStates) {
                tileEntityBlockStates[state] = tileEntity;
            }
        }
        airBlockState = registry.blockState("air");
        voidBiome = registry.biomeType("the_void");

        return {};
    }


}
