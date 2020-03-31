package org.spongepowered.common.command.brigadier.argument.parser;

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

/**
 * For use with other argument types
 */
public class CustomArgumentParser<T> implements ArgumentParser<T>, SuggestionProvider<CommandSource> {

    private static final StringArgumentType COMPLETION_KEY = StringArgumentType.string();
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    private final Collection<ValueParser<? extends T>> parsers;
    private final ValueCompleter completer;

    public CustomArgumentParser(final Collection<ValueParser<? extends T>> parsers, final ValueCompleter completer) {
        this.parsers = parsers;
        this.completer = completer;
    }

    @Override
    public T parse(final Parameter.Key<? super T> key, final SpongeCommandContextBuilder contextBuilder, final SpongeStringReader reader)
            throws CommandSyntaxException {
        List<ArgumentParseException> exceptions = null;
        final ArgumentReader.Immutable state = reader.getImmutable();
        final org.spongepowered.api.command.parameter.CommandContext.Builder.Transaction transaction = contextBuilder.startTransaction();
        Optional<? extends T> value;
        for (final ValueParser<? extends T> parser : this.parsers) {
            try {
                value = parser.getValue(key, (ArgumentReader.Mutable) reader, contextBuilder);
                contextBuilder.commit(transaction);
                return value.orElse(null);
            } catch (final ArgumentParseException e) {
                if (exceptions == null) {
                    exceptions = new ArrayList<>();
                }
                exceptions.add(e);
            }

            // reset the state as it did not go through.
            reader.setState(state);
            contextBuilder.rollback(transaction);
        }

        if (exceptions != null) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                    .createWithContext(reader,
                            Text.joinWith(Text.newLine(), exceptions.stream().map(ArgumentParseException::getSuperText).collect(Collectors.toList())));
        }

        // TODO: Check this - don't want Brig to blow up. If that happens, mandate everything returns an object.
        return null;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(
            final com.mojang.brigadier.context.CommandContext<?> context,
            final SuggestionsBuilder builder) {
        for (final String s : this.completer.complete((SpongeCommandContext) context)) {
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
        return null;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder)
            throws CommandSyntaxException {
        return this.listSuggestions(context, builder);
    }
}
