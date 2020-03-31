package org.spongepowered.common.command.parameter.subcommand;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SpongeSubcommandParameterBuilder implements Parameter.Subcommand.Builder {

    private final Set<String> aliases = new HashSet<>();
    private Command.@Nullable Parameterized command;

    @Override
    public Parameter.Subcommand.@NonNull Builder alias(@NonNull final String alias) {
        this.aliases.add(alias);
        return this;
    }

    public SpongeSubcommandParameterBuilder aliases(@NonNull final Collection<String> alias) {
        this.aliases.addAll(alias);
        return this;
    }

    @Override
    public Parameter.Subcommand.@NonNull Builder setSubcommand(final Command.@NonNull Parameterized command) {
        this.command = command;
        return this;
    }

    @Override
    public Parameter.@NonNull Subcommand build() {
        Preconditions.checkState(!this.aliases.isEmpty(), "At least one alias must be specified.");
        Preconditions.checkState(this.command != null, "A Command.Parameterized must be supplied.");
        return new SpongeSubcommandParameter(ImmutableSet.copyOf(this.aliases), this.command);
    }

    @Override
    public Parameter.Subcommand.@NonNull Builder reset() {
        this.aliases.clear();
        this.command = null;
        return this;
    }

}
