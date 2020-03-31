package org.spongepowered.common.command.registrar.tree;

import org.spongepowered.api.command.registrar.tree.CommandTreeBuilder;

public final class SpongeRootCommandTreeBuilderFactory implements CommandTreeBuilder.RootNodeFactory {

    public final static SpongeRootCommandTreeBuilderFactory INSTANCE = new SpongeRootCommandTreeBuilderFactory();

    private SpongeRootCommandTreeBuilderFactory() {
    }

    @Override
    public CommandTreeBuilder<CommandTreeBuilder.Basic> create() {
        return new RootCommandTreeBuilder();
    }

}
