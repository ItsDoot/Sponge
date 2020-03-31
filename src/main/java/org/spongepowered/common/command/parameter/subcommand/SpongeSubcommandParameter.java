package org.spongepowered.common.command.parameter.subcommand;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;

import java.util.Set;

public class SpongeSubcommandParameter implements Parameter.Subcommand {

    private final Set<String> aliases;
    private final Command.Parameterized command;

    public SpongeSubcommandParameter(@NonNull final Set<String> aliases, final Command.@NonNull Parameterized command) {
        this.aliases = aliases;
        this.command = command;
    }

    // A subcommand is always considered optional.
    @Override
    public boolean isOptional() {
        return true;
    }

    @Override
    public Command.@NonNull Parameterized getCommand() {
        return this.command;
    }

    @Override
    @NonNull
    public Set<String> getAliases() {
        return ImmutableSet.copyOf(this.aliases);
    }

}
