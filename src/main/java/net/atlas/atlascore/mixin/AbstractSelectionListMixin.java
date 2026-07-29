package net.atlas.atlascore.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.atlas.atlascore.client.gui.ConfigCategory;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractSelectionList.class)
public class AbstractSelectionListMixin {
    @WrapMethod(method = "repositionEntries")
    public void repositionEntries(Operation<Void> original) {
        if (AbstractSelectionList.class.cast(this) instanceof ConfigCategory category) category.atlas_core$repositionEntries();
        else original.call();
    }
}
