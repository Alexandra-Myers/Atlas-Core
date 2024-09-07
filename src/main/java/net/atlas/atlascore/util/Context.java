package net.atlas.atlascore.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public record Context(ResourceKey<Level> dimension, Boolean onDedicatedServer) {
    private static final Context EMPTY = new Context(null, null);
    public static Context empty() {
        return EMPTY;
    }
    public ContextBuilder copy() {
        return new ContextBuilder(this);
    }
    public static ContextBuilder builder() {
        return new ContextBuilder(empty());
    }
    public static class ContextBuilder {
        private ResourceKey<Level> dimension;
        private Boolean onDedicatedServer;
        private ContextBuilder(Context base) {
            dimension = base.dimension;
            onDedicatedServer = base.onDedicatedServer;
        }
        public ContextBuilder applyInformationFromEntity(Entity entity) {
            applyInformationFromLevel(entity.level());
            return this;
        }
        public ContextBuilder applyInformationFromCommandSourceStack(CommandSourceStack commandSourceStack) {
            withDimension(commandSourceStack.getLevel().dimension());
            putOnDedicated(commandSourceStack.getServer().isDedicatedServer());
            return this;
        }
        public ContextBuilder applyInformationFromLevel(Level level) {
            withDimension(level.dimension());
            if (!level.isClientSide)
                putOnDedicated(level.getServer().isDedicatedServer());
            return this;
        }
        public ContextBuilder withDimension(ResourceKey<Level> dimension) {
            this.dimension = dimension;
            return this;
        }
        public ContextBuilder putOnDedicated(boolean onDedicatedServer) {
            this.onDedicatedServer = onDedicatedServer;
            return this;
        }
        public Context build() {
            return new Context(dimension, onDedicatedServer);
        }
    }
}
