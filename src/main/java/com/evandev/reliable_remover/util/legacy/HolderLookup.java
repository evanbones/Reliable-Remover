package com.evandev.reliable_remover.util.legacy;

//? if <1.19.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;
import java.util.stream.Stream;

public final class HolderLookup {
    private HolderLookup() {
    }

    public interface Provider {
        <T> Optional<RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey);

        static Provider of(RegistryAccess registryAccess) {
            return new Provider() {
                @Override
                public <T> Optional<RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                    return registryAccess.<T>registry(registryKey).map(registry -> new RegistryLookup<>(registry));
                }
            };
        }
    }

    public record RegistryLookup<T>(Registry<T> registry) {
        public Stream<Holder.Reference<T>> listElements() {
            return registry.holders();
        }
    }
}
*///?}
