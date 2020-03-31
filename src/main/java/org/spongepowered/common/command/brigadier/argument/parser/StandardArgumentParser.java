package org.spongepowered.common.command.brigadier.argument.parser;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.CatalogedValueParameter;
import org.spongepowered.api.text.Text;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * For use with ArgumentTypes in the base game
 */
public class StandardArgumentParser<S, T> implements ArgumentParser<T>, CatalogedValueParameter<T> {

    public static <T> StandardArgumentParser<T, T> createIdentity(final String key, final ArgumentType<T> type) {
        return new StandardArgumentParser<>(key, type, (c, x) -> x);
    }

    public static <S, T> StandardArgumentParser<S, T> createCast(final String key, final ArgumentType<S> type, final Class<T> castType) {
        return new StandardArgumentParser<>(key, type, (c, x) -> castType.cast(x));
    }

    public static <S, T> StandardArgumentParser<S, T> createConverter(
            final String key,
            final ArgumentType<S> type,
            final StandardArgumentParser.Converter<S, T> converter) {
        return new StandardArgumentParser<>(key, type, converter);
    }

    // ---

    private final CatalogKey key;
    private final ArgumentType<S> type;
    private final StandardArgumentParser.Converter<S, T> converter;

    private StandardArgumentParser(
            final String key,
            final ArgumentType<S> type,
            final StandardArgumentParser.Converter<S, T> converter) {
        this.key = CatalogKey.sponge(key);
        this.type = type;
        this.converter = converter;
    }

    @Override
    @NonNull
    public CatalogKey getKey() {
        return this.key;
    }

    @Override
    public T parse(
            final Parameter.Key<? super T> key,
            final SpongeCommandContextBuilder contextBuilder,
            final SpongeStringReader reader) throws CommandSyntaxException {
        return this.converter.convert(contextBuilder, this.type.parse(reader));
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(
            final com.mojang.brigadier.context.CommandContext<?> context,
            final SuggestionsBuilder builder) {
        return this.type.listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return this.type.getExamples();
    }

    @Override
    public ArgumentType<?> getClientCompletionType() {
        return this.type;
    }

    @Override
    @NonNull
    public List<String> complete(@NonNull final CommandContext context) {
        final SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder("", 0);
        this.listSuggestions((com.mojang.brigadier.context.CommandContext<?>) context, suggestionsBuilder);
        return suggestionsBuilder.build().getList().stream().map(Suggestion::getText).collect(Collectors.toList());
    }

    @Override
    @NonNull
    public Optional<? extends T> getValue(final Parameter.@NonNull Key<? super T> parameterKey, final ArgumentReader.@NonNull Mutable reader,
            final CommandContext.@NonNull Builder context)
            throws ArgumentParseException {
        try {
            return Optional.of(this.parse(parameterKey, (SpongeCommandContextBuilder) context, (SpongeStringReader) reader));
        } catch (final CommandSyntaxException e) {
            throw new ArgumentParseException(Text.of(e.getMessage()), e, e.getInput(), e.getCursor());
        }
    }


    @FunctionalInterface
    public interface Converter<S, T> {

        T convert(SpongeCommandContextBuilder contextBuilder, S input) throws CommandSyntaxException;

    }

}
