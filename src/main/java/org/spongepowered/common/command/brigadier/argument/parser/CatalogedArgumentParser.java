package org.spongepowered.common.command.brigadier.argument.parser;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.command.parameter.managed.standard.CatalogedValueParameter;
import org.spongepowered.api.text.Text;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContext;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class CatalogedArgumentParser<T> implements ArgumentParser<T>, SuggestionProvider<CommandSource>, CatalogedValueParameter<T> {

    private static final StringArgumentType COMPLETION_KEY = StringArgumentType.string();
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    @Override
    public T parse(final Parameter.Key<? super T> key, final SpongeCommandContextBuilder contextBuilder, final SpongeStringReader reader)
            throws CommandSyntaxException {
        final ArgumentReader.Immutable state = reader.getImmutable();
        final org.spongepowered.api.command.parameter.CommandContext.Builder.Transaction transaction = contextBuilder.startTransaction();
        try {
            final Optional<? extends T> value = this.getValue(key, reader, contextBuilder);
            contextBuilder.commit(transaction);
            if (value.isPresent()) {
                return value.get();
            }
        } catch (final ArgumentParseException e) {
            // reset the state as it did not go through.
            reader.setState(state);
            contextBuilder.rollback(transaction);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                    .createWithContext(reader, e.getSuperText());
        }
        return null;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<?> context,
            final SuggestionsBuilder builder) {
        for (final String s : this.complete((SpongeCommandContext) context)) {
            if (INTEGER_PATTERN.matcher(s).matches()) {
                try {
                    builder.suggest(Integer.parseInt(s));
                } catch (final NumberFormatException ex) {
                    builder.suggest(s);
                }
            } else {
                builder.suggest(s);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public ArgumentType<?> getClientCompletionType() {
        return COMPLETION_KEY;
    }

    @Override
    public Collection<String> getExamples() {
        return ImmutableList.of();
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
        return this.listSuggestions(context, builder);
    }
}
