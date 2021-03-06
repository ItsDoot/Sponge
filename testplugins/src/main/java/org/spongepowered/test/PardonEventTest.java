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
package org.spongepowered.test;

import com.google.inject.Inject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.PardonIpEvent;
import org.spongepowered.api.event.user.PardonUserEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

@Plugin(id = "pardoneventtest", name = "Pardon Event Test", description = "A plugin to test PardonUserEvent and PardonIpEvent", version = "0.0.0")
public class PardonEventTest implements LoadableModule {

    @Inject private PluginContainer container;

    private final PardonEventListener listener = new PardonEventListener();



    @Override
    public void enable(CommandSource src) {
        Sponge.getEventManager().registerListeners(this.container, this.listener);
    }

    public static class PardonEventListener {
        @Listener
        public void onPardonIpEvent(PardonIpEvent event) {
            event.getCause().first(Player.class).ifPresent(player ->
                    player.sendMessage(Text.of(player.getName() + " removed a " +
                            event.getBan().getType().getName() + " ban")));
        }

        @Listener
        public void onPardonUserEvent(PardonUserEvent event) {
            event.getCause().first(Player.class).ifPresent(player ->
                    player.sendMessage(Text.of(player.getName() + " removed a " +
                            event.getBan().getType().getName() + " ban")));
        }
    }
}
