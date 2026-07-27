package net.atlas.atlascore.client.gui.elements;

import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record BoundNarratablesList(Supplier<NarrationPriority> priority, List<NarratableEntry> narratables) implements NarratableEntry {
    @Override
    public @NonNull NarrationPriority narrationPriority() {
        return this.priority.get();
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        this.narratables.forEach(entry -> entry.updateNarration(output));
    }

    @Override
    public @NonNull Collection<? extends NarratableEntry> getNarratables() {
        return this.narratables;
    }
}
