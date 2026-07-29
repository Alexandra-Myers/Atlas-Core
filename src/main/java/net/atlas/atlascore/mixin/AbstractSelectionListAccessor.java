package net.atlas.atlascore.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor<E> {
    @Accessor("children")
    List<E> getChildren();
}
