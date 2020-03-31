/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.command.manager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.Singleton;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.manager.CommandFailedRegistrationException;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.registrar.CommandRegistrar;
import org.spongepowered.api.command.registrar.tree.CommandTreeBuilder;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.command.brigadier.SpongeCommandDispatcher;
import org.spongepowered.common.command.registrar.SpongeParameterizedCommandRegistrar;
import org.spongepowered.common.command.registrar.SpongeRawCommandRegistrar;
import org.spongepowered.common.command.registrar.tree.RootCommandTreeBuilder;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.plugin.PluginContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class SpongeCommandManager implements CommandManager {

    private final Map<String, SpongeCommandMapping> commandMappings = new HashMap<>();
    private final Multimap<SpongeCommandMapping, String> inverseCommandMappings = HashMultimap.create();
    private final Multimap<PluginContainer, SpongeCommandMapping> pluginToCommandMap = HashMultimap.create();

    @NonNull
    public CommandMapping registerAlias(
            @NonNull final CommandRegistrar registrar,
            @NonNull final PluginContainer container,
            @NonNull final LiteralArgumentBuilder<CommandSource> rootArgument,
            @NonNull final String @NonNull... secondaryAliases)
            throws CommandFailedRegistrationException {
        final String requestedPrimaryAlias = rootArgument.getLiteral();

        // Get the mapping, if any.
        return this.registerAliasInternal(
                registrar,
                container,
                requestedPrimaryAlias,
                secondaryAliases
        );
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public CommandMapping registerAlias(
            @NonNull final CommandRegistrar<?> registrar,
            @NonNull final PluginContainer container,
            final CommandTreeBuilder.@NonNull Basic parameterTree,
            @NonNull final Predicate<CommandCause> requirement,
            @NonNull final String primaryAlias,
            @NonNull final String @NonNull ... secondaryAliases)
            throws CommandFailedRegistrationException {
        final CommandMapping mapping = this.registerAliasInternal(registrar, container, primaryAlias, secondaryAliases);

        // In general, this won't be executed as we will intercept it before this point. However,
        // this is as a just in case - a mod redirect or something.
        final com.mojang.brigadier.Command<CommandSource> command = context -> {
            final org.spongepowered.api.command.parameter.CommandContext spongeContext =
                    (org.spongepowered.api.command.parameter.CommandContext) context;
            final String[] command1 = context.getInput().split(" ", 2);
            try {
                return registrar.process(spongeContext, command1[0], command1.length == 2 ? command1[1] : "").getResult();
            } catch (final CommandException e) {
                throw new SimpleCommandExceptionType(SpongeTexts.toComponent(e.getText())).create();
            }
        };

        final Collection<CommandNode<CommandSource>> commandSourceRootCommandNode = ((RootCommandTreeBuilder) parameterTree)
                .createArgumentTree(command);

        // From the primary alias...
        final LiteralArgumentBuilder<CommandSource> node = LiteralArgumentBuilder.literal(mapping.getPrimaryAlias());

        // CommandSource == CommandCause, so this will be fine.
        node.requires((Predicate<CommandSource>) (Object) requirement).executes(command);
        for (final CommandNode<CommandSource> commandNode : commandSourceRootCommandNode) {
            node.then(commandNode);
        }

        final LiteralCommandNode<CommandSource> commandToAppend =
                ((SpongeCommandDispatcher) SpongeImpl.getServer().getCommandManager().getDispatcher()).registerInternal(node);
        for (final String secondaryAlias : mapping.getAllAliases()) {
            if (!secondaryAlias.equals(mapping.getPrimaryAlias())) {
                ((SpongeCommandDispatcher) SpongeImpl.getServer().getCommandManager().getDispatcher())
                        .registerInternal(LiteralArgumentBuilder.<CommandSource>literal(secondaryAlias).redirect(commandToAppend));
            }
        }

        return mapping;
    }

    @Override
    @NonNull
    public CommandRegistrar<Command> getStandardRegistrar() {
        return SpongeRawCommandRegistrar.INSTANCE;
    }

    @NonNull
    private CommandMapping registerAliasInternal(
            @NonNull final CommandRegistrar<?> registrar,
            @NonNull final PluginContainer container,
            @NonNull final String primaryAlias,
            @NonNull final String @NonNull... secondaryAliases)
            throws CommandFailedRegistrationException {
        // Check it's been registered:
        if (primaryAlias.contains(" ") || Arrays.stream(secondaryAliases).anyMatch(x -> x.contains(" "))) {
                throw new CommandFailedRegistrationException("Aliases may not contain spaces.");
        }

        // We have a Sponge command, so let's start by checking to see what
        // we're going to register.
        final String primaryAliasLowercase = primaryAlias.toLowerCase(Locale.ENGLISH);
        final String namespacedAlias = container.getMetadata().getId() + ":" + primaryAlias.toLowerCase(Locale.ENGLISH);
        if (this.commandMappings.containsKey(namespacedAlias)) {
            // It's registered.
            throw new CommandFailedRegistrationException(
                    "The command alias " + primaryAlias + " has already been registered for this plugin");
        }

        final Set<String> aliases = new HashSet<>();
        aliases.add(primaryAliasLowercase);
        aliases.add(namespacedAlias);
        for (final String secondaryAlias : secondaryAliases) {
            aliases.add(secondaryAlias.toLowerCase(Locale.ENGLISH));
        }

        // Okay, what can we register?
        aliases.removeIf(this.commandMappings::containsKey);

        // We need to consider the configuration file - if there is an entry in there
        // then remove an alias if the command is not entitled to use it.
        SpongeImpl.getGlobalConfigAdapter().getConfig()
                .getCommands()
                .getAliases()
                .entrySet()
                .stream()
                .filter(x -> !x.getValue().equalsIgnoreCase(container.getMetadata().getId()))
                .filter(x -> aliases.contains(x.getKey()))
                .forEach(x -> aliases.remove(x.getKey()));

        if (aliases.isEmpty()) {
            // If the mapping is empty, throw an exception. Shouldn't happen, but you never know.
            throw new CommandFailedRegistrationException("No aliases could be registered for the supplied command.");
        }

        // Create the mapping
        final SpongeCommandMapping mapping = new SpongeCommandMapping(
                primaryAlias,
                aliases,
                container,
                registrar
        );

        this.pluginToCommandMap.put(container, mapping);
        aliases.forEach(key -> {
            this.commandMappings.put(key, mapping);
            this.inverseCommandMappings.put(mapping, key);
        });
        return mapping;
    }

    @Override
    @NonNull
    public Collection<PluginContainer> getPlugins() {
        return ImmutableSet.copyOf(this.pluginToCommandMap.keySet());
    }

    public int process(@NonNull final CommandSource source, @NonNull final String commandToSend) throws net.minecraft.command.CommandException {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(source);
            return this.process(commandToSend).getResult();
        } catch (final CommandException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof net.minecraft.command.CommandException) {
                throw (net.minecraft.command.CommandException) cause;
            }

            source.sendErrorMessage(SpongeTexts.toComponent(ex.getText()));
            return 0;
        }
    }

    @Override
    @NonNull
    public CommandResult process(@NonNull final String arguments) throws CommandException {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.COMMAND_STRING.get(), arguments);
            final String[] splitArg = arguments.split(" ", 2);
            final String command = splitArg[0];
            final String args = splitArg.length == 2 ? splitArg[1] : "";
            final SpongeCommandMapping mapping = this.commandMappings.get(command.toLowerCase());
            if (mapping == null) {
                // no command.
                throw new CommandException(Text.of(TextColors.RED, "Unknown command. Type /help for a list of commands."));
            }
            return mapping.getRegistrar().process(CommandCause.of(frame.getCurrentCause()), mapping.getPrimaryAlias(), args);
        }
    }

    @Override
    @NonNull
    public <T extends Subject & MessageReceiver> CommandResult process(
            @NonNull final T subjectReceiver,
            @NonNull final String arguments) throws CommandException {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT.get(), subjectReceiver);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL.get(), MessageChannel.to(subjectReceiver));
            return this.process(arguments);
        }
    }

    @Override
    @NonNull
    public CommandResult process(
            @NonNull final Subject subject,
            @NonNull final MessageChannel receiver,
            @NonNull final String arguments) throws CommandException {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT.get(), subject);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL.get(), receiver);
            return this.process(arguments);
        }
    }

    @Override
    @NonNull
    public List<String> suggest(@NonNull final String arguments) {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.COMMAND_STRING.get(), arguments);
            final String[] splitArg = arguments.split(" ", 2);
            final String command = splitArg[0].toLowerCase();

            if (splitArg.length == 2) {
                // we have a subcommand, suggest on that if it exists, else
                // return nothing
                final SpongeCommandMapping mapping = this.commandMappings.get(command);
                if (mapping == null) {
                    return Collections.emptyList();
                }

                return mapping.getRegistrar().suggestions(
                        CommandCause.of(frame.getCurrentCause()), mapping.getPrimaryAlias(), splitArg[1]);
            }

            return this.commandMappings.keySet()
                    .stream()
                    .filter(x -> x.startsWith(command))
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public <T extends Subject & MessageReceiver> List<String> suggest(
            @NonNull final T subjectReceiver,
            @NonNull final String arguments) {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT.get(), subjectReceiver);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL.get(), MessageChannel.to(subjectReceiver));
            return this.suggest(arguments);
        }
    }

    @Override
    @NonNull
    public List<String> suggest(
            @NonNull final Subject subject,
            @NonNull final MessageChannel receiver,
            @NonNull final String arguments) {
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SUBJECT.get(), subject);
            frame.addContext(EventContextKeys.MESSAGE_CHANNEL.get(), receiver);
            return this.suggest(arguments);
        }
    }

}
