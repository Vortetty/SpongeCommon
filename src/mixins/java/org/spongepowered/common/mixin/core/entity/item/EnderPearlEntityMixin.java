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
package org.spongepowered.common.mixin.core.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.teleport.MovementTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.mixin.core.entity.projectile.ThrowableEntityMixin;
import org.spongepowered.common.util.Constants;

import javax.annotation.Nullable;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin extends ThrowableEntityMixin {

    @Shadow private LivingEntity perlThrower;

    private double impl$damageAmount;

    @ModifyArg(method = "onImpact",
            at = @At(value = "INVOKE",
                target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private float impl$onAttackEntityFromWithDamage(final float damage) {
        return (float) this.impl$damageAmount;
    }

    @Redirect(method = "onImpact",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ServerPlayerEntity;isSleeping()Z"))
    private boolean impl$onEnderPearlImpact(final ServerPlayerEntity player) {
        if (player.isSleeping()) {
            return true;
        }

        // TODO Minecraft 1.14 - Re-write this hook to not use the Pearl's position but the position from the event
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.MOVEMENT_TYPE, MovementTypes.ENDER_PEARL);
            frame.addContext(EventContextKeys.PROJECTILE_SOURCE, (Player) player);

            final ServerLocation fromLocation = ((ServerPlayer) player).getServerLocation();
            final ServerLocation toLocation = ((org.spongepowered.api.entity.Entity) this).getServerLocation();
            final MoveEntityEvent.Position event = SpongeEventFactory.createMoveEntityEventPosition(frame.getCurrentCause(), (ServerPlayer) player,
                fromLocation.getPosition(), toLocation.getPosition());
            if (event.isCancelled()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void impl$readFromSpongeCompound(final CompoundNBT compound) {
        super.impl$readFromSpongeCompound(compound);
        if (compound.contains(Constants.Sponge.Entity.Projectile.PROJECTILE_DAMAGE_AMOUNT)) {
            this.impl$damageAmount = compound.getDouble(Constants.Sponge.Entity.Projectile.PROJECTILE_DAMAGE_AMOUNT);
        }
    }

    @Override
    public void impl$writeToSpongeCompound(final CompoundNBT compound) {
        super.impl$writeToSpongeCompound(compound);
        compound.putDouble(Constants.Sponge.Entity.Projectile.PROJECTILE_DAMAGE_AMOUNT, this.impl$damageAmount);
    }

    @Override
    @Nullable
    public Entity shadow$changeDimension(final DimensionType dimensionIn) {
        final Entity entity = super.shadow$changeDimension(dimensionIn);

        if (entity instanceof EnderPearlEntity) {
            // We actually teleported so...
            this.perlThrower = null;
            this.owner = null;
        }

        return entity;
    }
}
