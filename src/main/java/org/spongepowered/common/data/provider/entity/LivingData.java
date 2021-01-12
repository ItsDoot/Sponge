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
package org.spongepowered.common.data.provider.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.BodyParts;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.common.accessor.entity.EntityAccessor;
import org.spongepowered.common.accessor.entity.LivingEntityAccessor;
import org.spongepowered.common.bridge.entity.LivingEntityBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.event.cause.entity.damage.SpongeDamageSources;
import org.spongepowered.common.util.PotionEffectUtil;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.math.vector.Vector3d;

import java.util.Collection;

public final class LivingData {

    private LivingData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(LivingEntity.class)
                    .create(Keys.ABSORPTION)
                        .get(h -> (double) h.getAbsorptionAmount())
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.setAbsorptionAmount(v.floatValue());
                            return true;
                        })
                    .create(Keys.ACTIVE_ITEM)
                        .get(h -> ItemStackUtil.snapshotOf(h.getUseItem()))
                        .setAnd((h, v) -> {
                            if (v.isEmpty()) {
                                h.releaseUsingItem();
                                return true;
                            }
                            return false;
                        })
                        .delete(LivingEntity::releaseUsingItem)
                    .create(Keys.BODY_ROTATIONS)
                        .get(h -> {
                            final double headYaw = h.getYHeadRot();
                            final double pitch = h.xRot;
                            final double yaw = h.yRot;

                            return ImmutableMap.of(
                                    BodyParts.HEAD.get(), new Vector3d(pitch, headYaw, 0),
                                    BodyParts.CHEST.get(), new Vector3d(pitch, yaw, 0));
                        })
                        .set((h, v) -> {
                            final Vector3d headRotation = v.get(BodyParts.HEAD.get());
                            final Vector3d bodyRotation = v.get(BodyParts.CHEST.get());

                            if (bodyRotation != null) {
                                h.yRot = (float) bodyRotation.getY();
                                h.xRot = (float) bodyRotation.getX();
                            }
                            if (headRotation != null) {
                                h.yHeadRot = (float) headRotation.getY();
                                h.xRot = (float) headRotation.getX();
                            }
                        })
                    .create(Keys.CHEST_ROTATION)
                        .get(h -> new Vector3d(h.xRot, h.yRot, 0))
                        .set((h, v) -> {
                            final float headYaw = (float) v.getY();
                            final float pitch = (float) v.getX();
                            h.setYHeadRot(headYaw);
                            h.xRot = pitch;
                        })
                    .create(Keys.HEAD_ROTATION)
                        .get(h -> new Vector3d(h.xRot, h.getYHeadRot(), 0))
                        .set((h, v) -> {
                            final float yaw = (float) v.getY();
                            final float pitch = (float) v.getX();
                            h.yRot = yaw;
                            h.xRot = pitch;
                        })
                    .create(Keys.HEALTH)
                        .get(h -> (double) h.getHealth())
                        .setAnd((h, v) -> {
                            final double maxHealth = h.getMaxHealth();
                            // Check bounds
                            if (v < 0 || v > maxHealth) {
                                return false;
                            }

                            if (v == 0) {
                                // Cause DestructEntityEvent to fire first
                                h.hurt((DamageSource) SpongeDamageSources.IGNORED, Float.MAX_VALUE);
                            }
                            h.setHealth(v.floatValue());
                            return true;
                        })
                    .create(Keys.IS_ELYTRA_FLYING)
                        .get(LivingEntity::isFallFlying)
                        .set((h, v) -> ((EntityAccessor) h).invoker$setSharedFlag(Constants.Entity.ELYTRA_FLYING_FLAG, v))
                    .create(Keys.LAST_ATTACKER)
                        .get(h -> (Entity) h.getLastHurtByMob())
                        .setAnd((h, v) -> {
                            if (v instanceof LivingEntity) {
                                h.setLastHurtByMob((LivingEntity) v);
                                return true;
                            }
                            return false;
                        })
                        .delete(h -> h.setLastHurtByMob(null))
                    .create(Keys.MAX_AIR)
                        .get(LivingEntity::getMaxAirSupply)
                        .set((h, v) -> ((LivingEntityBridge) h).bridge$setMaxAir(v))
                    .create(Keys.MAX_HEALTH)
                        .get(h -> (double) h.getMaxHealth())
                        .set((h, v) -> h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(v))
                    .create(Keys.POTION_EFFECTS)
                        .get(h -> {
                            final Collection<EffectInstance> effects = h.getActiveEffects();
                            return effects.isEmpty() ? null : PotionEffectUtil.copyAsPotionEffects(effects);
                        })
                        .set((h, v) -> {
                            h.removeAllEffects();
                            for (final PotionEffect effect : v) {
                                h.addEffect(PotionEffectUtil.copyAsEffectInstance(effect));
                            }
                        })
                        .delete(LivingEntity::removeAllEffects)
                    .create(Keys.REMAINING_AIR)
                        .get(h -> Math.max(0, h.getAirSupply()))
                        .setAnd((h, v) -> {
                            if (v < 0 || v > h.getMaxAirSupply()) {
                                return false;
                            }
                            if (v == 0 && h.getAirSupply() < 0) {
                                return false;
                            }
                            h.setAirSupply(v);
                            return true;
                        })
                    .create(Keys.SCALE)
                        .get(h -> (double) h.getScale())
                    .create(Keys.STUCK_ARROWS)
                        .get(LivingEntity::getArrowCount)
                        .setAnd((h, v) -> {
                            if (v < 0 || v > Integer.MAX_VALUE) {
                                return false;
                            }
                            h.setArrowCount(v);
                            return true;
                        })
                    .create(Keys.WALKING_SPEED)
                        .get(h -> h.getAttribute(Attributes.MOVEMENT_SPEED).getValue())
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(v);
                            return true;
                        })
                .asMutable(LivingEntityAccessor.class)
                    .create(Keys.LAST_DAMAGE_RECEIVED)
                        .get(h -> (double) h.accessor$lastHurt())
                        .set((h, v) -> h.accessor$lastHurt(v.floatValue()));
    }
    // @formatter:on
}
