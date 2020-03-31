package org.spongepowered.common.command.brigadier.argument.parser;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ArgumentParser<T> {

    T parse(
            Parameter.Key<? super T> key,
            SpongeCommandContextBuilder contextBuilder,
            SpongeStringReader reader) throws CommandSyntaxException;

    CompletableFuture<Suggestions> listSuggestions(
            com.mojang.brigadier.context.CommandContext<?> context,
            SuggestionsBuilder builder);

    Collection<String> getExamples();

    ArgumentType<?> getClientCompletionType();

}
