package net.atlas.atlaslib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.atlas.atlaslib.command.argument.ExtendedArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArgumentCommandNode.class)
public class ArgumentCommandNodeMixin {
    @SuppressWarnings("unchecked")
    @WrapOperation(method = "parse", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/arguments/ArgumentType;parse(Lcom/mojang/brigadier/StringReader;)Ljava/lang/Object;"), remap = false)
    public <T, S> T modifyParseCall(ArgumentType<T> instance, StringReader reader, Operation<T> original, @Local(ordinal = 0, argsOnly = true) CommandContextBuilder<S> commandContextBuilder) throws CommandSyntaxException {
        if (instance instanceof ExtendedArgumentType<?>) {
            return ((ExtendedArgumentType<T>)instance).parse(reader, commandContextBuilder);
        } else {
            return original.call(instance, reader);
        }
    }
}
