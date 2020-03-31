package org.spongepowered.common.command.brigadier.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueUsage;
import org.spongepowered.api.text.Text;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.argument.parser.ArgumentParser;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class SpongeArgumentType<T> implements ArgumentType<T> {

    private final ArgumentParser<? extends T> parser;
    private final Parameter.Key<? super T> key;
    @Nullable private final ValueUsage usage;

    public SpongeArgumentType(
            final Parameter.Key<? super T> key,
            final ArgumentParser<? extends T> parser) {
        this(key, parser, null);
    }

    public SpongeArgumentType(
            final Parameter.Key<? super T> key,
            final ArgumentParser<? extends T> parser,
            @Nullable final ValueUsage usage) {
        this.parser = parser;
        this.key = key;
        this.usage = usage;
    }

    public T parse(final SpongeCommandContextBuilder contextBuilder, final SpongeStringReader reader) throws CommandSyntaxException {
        return this.parser.parse(this.key, contextBuilder, reader);
    }

    public ArgumentType<?> getClientCompletionType() {
        return this.parser.getClientCompletionType();
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException {
        // So, we hid the context in our string reader...
        final SpongeStringReader stringReader = ((SpongeStringReader) reader);
        final SpongeCommandContextBuilder builder = stringReader.getCommandContextBuilder();
        return this.parse(builder, stringReader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return this.parser.listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return this.parser.getExamples();
    }

    @Nullable
    public Text getUsage() {
        if (this.usage == null) {
            return null;
        }
        return this.usage.getUsage(Text.of(this.key.key()));
    }

}
