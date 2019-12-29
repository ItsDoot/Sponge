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
package org.spongepowered.common.mixin.api.mcp.entity.monster;

import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.mutable.DyeableData;
import org.spongepowered.api.data.manipulator.mutable.block.DirectionalData;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.projectile.EntityTargetingProjectile;
import org.spongepowered.api.util.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.entity.monster.ShulkerEntityBridge;
import org.spongepowered.common.data.manipulator.mutable.SpongeDyeableData;
import org.spongepowered.common.data.manipulator.mutable.block.SpongeDirectionalData;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.entity.projectile.ProjectileLauncher;
import org.spongepowered.common.mixin.api.mcp.entity.passive.GolemEntityMixin_API;
import org.spongepowered.common.util.Constants;
import Mutable;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.entity.monster.ShulkerEntity;

@Mixin(ShulkerEntity.class)
public abstract class ShulkerEntityMixin_API extends GolemEntityMixin_API implements Shulker {


    @Override
    public DyeableData getDyeData() {
        return new SpongeDyeableData(((ShulkerEntityBridge) this).bridge$getColor());
    }

    @Override
    public Mutable<DyeColor> color() {
        return new SpongeValue<>(Keys.DYE_COLOR, Constants.Catalog.DEFAULT_SHULKER_COLOR, ((ShulkerEntityBridge) this).bridge$getColor());
    }

    @Override
    public DirectionalData getDirectionalData() {
        return new SpongeDirectionalData(((ShulkerEntityBridge) this).bridge$getDirection());
    }

    @Override
    public Mutable<Direction> direction() {
        return new SpongeValue<>(Keys.DIRECTION, ((ShulkerEntityBridge) this).bridge$getDirection());
    }

    @Override
    public <P extends EntityTargetingProjectile> Optional<P> launchWithTarget(final Class<P> projectileClass,
        final org.spongepowered.api.entity.Entity target) {
        return ProjectileLauncher.launchWithArgs(projectileClass, Shulker.class, this, null, target);
    }

    @Override
    public void spongeApi$supplyVanillaManipulators(final Collection<? super org.spongepowered.api.data.DataManipulator.Mutable<?, ?>> manipulators) {
        super.spongeApi$supplyVanillaManipulators(manipulators);
        manipulators.add(this.getDyeData());
        manipulators.add(this.getDirectionalData());
    }
}