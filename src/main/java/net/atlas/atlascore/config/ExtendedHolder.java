package net.atlas.atlascore.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ExtendedHolder {
    Component getInnerTranslation(String name);
    Component getInnerValue(String name);
    void listInner(String name, Consumer<Component> input);
    void fulfilListing(Consumer<Component> input);
    List<ConfigHolderLike<?>> getUnsetInners();
    ConfigHolderLike<?> findInner(StringReader reader) throws CommandSyntaxException;
    ConfigHolderLike<?> retrieveInner(String name) throws CommandSyntaxException;
    CompletableFuture<Suggestions> suggestInner(StringReader reader, SuggestionsBuilder builder);
    int postUpdate(CommandSourceStack commandSourceStack);
}