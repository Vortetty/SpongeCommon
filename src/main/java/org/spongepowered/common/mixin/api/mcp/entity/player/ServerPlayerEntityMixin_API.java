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
package org.spongepowered.common.mixin.api.mcp.entity.player;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.network.play.server.SWorldBorderPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.advancement.AdvancementProgress;
import org.spongepowered.api.advancement.AdvancementTree;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.GameModeData;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.data.type.SkinPart;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.living.player.CooldownTracker;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.network.PlayerConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.chat.ChatVisibility;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.advancements.AdvancementBridge;
import org.spongepowered.common.bridge.advancements.PlayerAdvancementsBridge;
import org.spongepowered.common.bridge.api.text.title.TitleBridge;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.bridge.network.ServerPlayNetHandlerBridge;
import org.spongepowered.common.bridge.network.play.server.SSendResourcePackPacketBridge;
import org.spongepowered.common.bridge.scoreboard.ServerScoreboardBridge;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeGameModeData;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeJoinData;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.effect.particle.SpongeParticleEffect;
import org.spongepowered.common.effect.particle.SpongeParticleHelper;
import org.spongepowered.common.effect.record.SpongeRecordType;
import org.spongepowered.common.effect.sound.SoundEffectHelper;
import org.spongepowered.common.entity.player.tab.SpongeTabList;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.util.BookFaker;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.LocaleCache;
import org.spongepowered.common.util.NetworkUtil;
import org.spongepowered.common.world.storage.SpongePlayerDataHandler;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(ServerPlayerEntity.class)
@Implements(@Interface(iface = Player.class, prefix = "api$"))
public abstract class ServerPlayerEntityMixin_API extends PlayerEntityMixin_API implements Player {

    @Shadow @Final public MinecraftServer server;
    @Shadow @Final public PlayerInteractionManager interactionManager;
    @Shadow @Final private PlayerAdvancements advancements;
    @Shadow private String language;
    @Shadow public ServerPlayNetHandler connection;
    @Shadow private PlayerEntity.EnumChatVisibility chatVisibility = PlayerEntity.EnumChatVisibility.FULL;
    @Shadow private boolean chatColours;

    @Shadow public abstract Entity getSpectatingEntity();
    @Shadow public abstract void setSpectatingEntity(Entity entity);

    private boolean api$sleepingIgnored;
    private TabList api$tabList = new SpongeTabList((ServerPlayerEntity) (Object) this);
    @Nullable private WorldBorder api$worldBorder;

    @Override
    public GameProfile getProfile() {
        return ((ServerPlayerEntityBridge) this).bridge$getUser().getProfile();
    }

    @Override
    public boolean isOnline() {
        return ((ServerPlayerEntityBridge) this).bridge$getUser().isOnline();
    }

    @Override
    public Optional<Player> getPlayer() {
        return Optional.of(this);
    }

    @Override
    public Locale getLocale() {
        return LocaleCache.getLocale(this.language);
    }

    @Override
    public int getViewDistance() {
        return ((ServerPlayerEntityBridge) this).bridge$getViewDistance();
    }

    @Override
    public ChatVisibility getChatVisibility() {
        return (ChatVisibility) (Object) this.chatVisibility;
    }

    @Override
    public boolean isChatColorsEnabled() {
        return this.chatColours;
    }

    @Override
    public Set<SkinPart> getDisplayedSkinParts() {
        return ((ServerPlayerEntityBridge) this).bridge$getSkinParts();
    }

    @Override
    public void sendMessage(final ChatType type, final Text message) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            return;
        }
        checkNotNull(type, "type");
        checkNotNull(message, "message");

        ITextComponent component = SpongeTexts.toComponent(message);
        if (type == ChatTypes.ACTION_BAR) {
            component = SpongeTexts.fixActionBarFormatting(component);
        }

        this.connection.sendPacket(new SChatPacket(component, (net.minecraft.util.text.ChatType) (Object) type));
    }

    @Override
    public void sendBookView(final BookView bookView) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            return;
        }
        BookFaker.fakeBookView(bookView, this);
    }

    @Override
    public void sendTitle(final Title title) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            return;
        }
        ((TitleBridge) (Object) title).bridge$send((ServerPlayerEntity) (Object) this);
    }

    @Override
    public void spawnParticles(final ParticleEffect particleEffect, final Vector3d position) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            return;
        }
        this.spawnParticles(particleEffect, position, Integer.MAX_VALUE);
    }

    @Override
    public void spawnParticles(final ParticleEffect particleEffect, final Vector3d position, final int radius) {
        if (this.impl$isFake) {
            // Don't bother sending messages to fake players
            return;
        }
        checkNotNull(particleEffect, "The particle effect cannot be null!");
        checkNotNull(position, "The position cannot be null");
        checkArgument(radius > 0, "The radius has to be greater then zero!");

        final List<IPacket<?>> packets = SpongeParticleHelper.toPackets((SpongeParticleEffect) particleEffect, position);

        if (!packets.isEmpty()) {
            if (position.sub(this.posX, this.posY, this.posZ).lengthSquared() < (long) radius * (long) radius) {
                for (final IPacket<?> packet : packets) {
                    this.connection.sendPacket(packet);
                }
            }
        }
    }

    @Override
    public PlayerConnection getConnection() {
        return (PlayerConnection) this.connection;
    }

    /**
     * @author Minecrell - August 22nd, 2016
     * @reason Use InetSocketAddress#getHostString() where possible (instead of
     *     inspecting SocketAddress#toString()) to support IPv6 addresses
     */
    @Overwrite
    public String getPlayerIP() {
        return NetworkUtil.getHostString(this.connection.netManager.getRemoteAddress());
    }

    @Override
    public String getIdentifier() {
        return ((ServerPlayerEntityBridge) this).bridge$getUser().getIdentifier();
    }

    @Override
    public void setScoreboard(final Scoreboard scoreboard) {
        if (((ServerPlayerEntityBridge) this).bridge$hasDelegate()) {
            ((Player) ((ServerPlayerEntityBridge) this).bridge$getDelegate()).setScoreboard(scoreboard);
        }
        ((ServerScoreboardBridge) ((ServerPlayerEntityBridge) this).bridge$getScoreboard()).bridge$removePlayer((ServerPlayerEntity) (Object) this, true);
        ((ServerPlayerEntityBridge) this).bridge$replaceScoreboard(scoreboard);
        ((ServerScoreboardBridge) ((ServerPlayerEntityBridge) this).bridge$getScoreboard()).bridge$addPlayer((ServerPlayerEntity) (Object) this, true);
    }

    @Override
    public Text getTeamRepresentation() {
        return Text.of(this.shadow$getName());
    }

    @Override
    public Scoreboard getScoreboard() {
        return ((ServerPlayerEntityBridge) this).bridge$getScoreboard();
    }

    @Override
    public void kick() {
        this.kick(Text.of(SpongeImpl.getGame().getRegistry().getTranslationById("disconnect.disconnected").get()));
    }

    @Override
    public void kick(final Text message) {
        final ITextComponent component = SpongeTexts.toComponent(message);
        this.connection.disconnect(component);
    }

    @Override
    public void playSound(final SoundType sound, final SoundCategory category, final Vector3d position, final double volume) {
        this.playSound(sound, category, position, volume, 1);
    }

    @Override
    public void playSound(final SoundType sound, final SoundCategory category, final Vector3d position, final double volume, final double pitch) {
        this.playSound(sound, category, position, volume, pitch, 0);
    }

    @Override
    public void playSound(
        final SoundType sound, final SoundCategory category, final Vector3d position, final double volume, final double pitch, final double minVolume) {
        final SoundEvent event;
        try {
            // Check if the event is registered (ie has an integer ID)
            event = SoundEvents.getRegisteredSoundEvent(sound.getId());
        } catch (IllegalStateException e) {
            // Otherwise send it as a custom sound
            this.connection.sendPacket(new SPlaySoundPacket(sound.getId(), (net.minecraft.util.SoundCategory) (Object) category,
                    position.getX(), position.getY(), position.getZ(), (float) Math.max(minVolume, volume), (float) pitch));
            return;
        }

        this.connection.sendPacket(new SPlaySoundEffectPacket(event, (net.minecraft.util.SoundCategory) (Object) category, position.getX(),
                position.getY(), position.getZ(), (float) Math.max(minVolume, volume), (float) pitch));
    }

    @Override
    public void stopSounds() {
        this.stopSounds0(null, null);
    }

    @Override
    public void stopSounds(final SoundType sound) {
        this.stopSounds0(checkNotNull(sound, "sound"), null);
    }

    @Override
    public void stopSounds(final SoundCategory category) {
        this.stopSounds0(null, checkNotNull(category, "category"));
    }

    @Override
    public void stopSounds(final SoundType sound, final SoundCategory category) {
        this.stopSounds0(checkNotNull(sound, "sound"), checkNotNull(category, "category"));
    }

    private void stopSounds0(@Nullable final SoundType sound, @Nullable final SoundCategory category) {
        this.connection.sendPacket(SoundEffectHelper.createStopSoundPacket(sound, category));
    }

    @Override
    public void playRecord(final Vector3i position, final MusicDisc recordType) {
        this.playRecord0(position, checkNotNull(recordType, "recordType"));
    }

    @Override
    public void stopRecord(final Vector3i position) {
        this.playRecord0(position, null);
    }

    private void playRecord0(final Vector3i position, @Nullable final MusicDisc recordType) {
        this.connection.sendPacket(SpongeRecordType.createPacket(position, recordType));
    }

    @Override
    public void sendResourcePack(final ResourcePack pack) {
        final SSendResourcePackPacket packet = new SSendResourcePackPacket();
        ((SSendResourcePackPacketBridge) packet).bridge$setSpongePack(pack);
        this.connection.sendPacket(packet);
    }

    @Inject(method = "markPlayerActive()V", at = @At("HEAD"))
    private void onPlayerActive(final CallbackInfo ci) {
        ((ServerPlayNetHandlerBridge) this.connection).bridge$resendLatestResourcePackRequest();
    }

    @Override
    public boolean isSleepingIgnored() {
        return this.api$sleepingIgnored;
    }

    @Override
    public void setSleepingIgnored(final boolean sleepingIgnored) {
        this.api$sleepingIgnored = sleepingIgnored;
    }

    @Override
    public Vector3d getVelocity() {
        if (((ServerPlayerEntityBridge) this).bridge$getVelocityOverride() != null) {
            return ((ServerPlayerEntityBridge) this).bridge$getVelocityOverride();
        }
        return super.getVelocity();
    }

    @Override
    public TabList getTabList() {
        return this.api$tabList;
    }

    @Override
    public JoinData getJoinData() {
        return new SpongeJoinData(SpongePlayerDataHandler.getFirstJoined(this.getUniqueID()).get(), Instant.now());
    }

    @Override
    public Value.Mutable<Instant> firstPlayed() {
        return new SpongeValue<>(Keys.FIRST_DATE_PLAYED, Instant.EPOCH, SpongePlayerDataHandler.getFirstJoined(this.getUniqueID()).get());
    }

    @Override
    public Value.Mutable<Instant> lastPlayed() {
        return new SpongeValue<>(Keys.LAST_DATE_PLAYED, Instant.EPOCH, Instant.now());
    }

    @Override
    public boolean hasPlayedBefore() {
        final Instant instant = SpongePlayerDataHandler.getFirstJoined(this.getUniqueId()).get();
        final Instant toTheMinute = instant.truncatedTo(ChronoUnit.MINUTES);
        final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        final Duration timeSinceFirstJoined = Duration.of(now.minusMillis(toTheMinute.toEpochMilli()).toEpochMilli(), ChronoUnit.MINUTES);
        return timeSinceFirstJoined.getSeconds() > 0;
    }

    @Override
    public GameModeData getGameModeData() {
        return new SpongeGameModeData((GameMode) (Object) this.interactionManager.getGameType());
    }

    @Override
    public Value.Mutable<GameMode> gameMode() {
        return new SpongeValue<>(Keys.GAME_MODE, Constants.Catalog.DEFAULT_GAMEMODE,
                (GameMode) (Object) this.interactionManager.getGameType());
    }

    @Override
    public void spongeApi$supplyVanillaManipulators(final Collection<? super org.spongepowered.api.data.DataManipulator.Mutable<?, ?>> manipulators) {
        super.spongeApi$supplyVanillaManipulators(manipulators);
        manipulators.add(this.getJoinData());
        manipulators.add(this.getGameModeData());
    }

    public void sendBlockChange(final BlockPos pos, final net.minecraft.block.BlockState state) {
        final SChangeBlockPacket packet = new SChangeBlockPacket();
        packet.pos = pos;
        packet.blockState = state;
        this.connection.sendPacket(packet);
    }

    @Override
    public void sendBlockChange(final int x, final int y, final int z, final BlockState state) {
        checkNotNull(state, "state");
        this.sendBlockChange(new BlockPos(x, y, z), (net.minecraft.block.BlockState) state);
    }

    @Override
    public void resetBlockChange(final int x, final int y, final int z) {
        final SChangeBlockPacket packet = new SChangeBlockPacket(this.world, new BlockPos(x, y, z));
        this.connection.sendPacket(packet);
    }

    @Override
    public boolean respawnPlayer() {
        if (this.getHealth() > 0.0F) {
            return false;
        }
        this.connection.player = this.server.getPlayerList().recreatePlayerEntity((ServerPlayerEntity) (Object) this, this.dimension, false);
        return true;
    }

    @Override
    public Optional<org.spongepowered.api.entity.Entity> getSpectatorTarget() {
        // For the API, return empty if we're spectating ourself.
        @Nonnull final Entity entity = this.getSpectatingEntity();
        return entity == (Object) this ? Optional.empty() : Optional.of((org.spongepowered.api.entity.Entity) entity);
    }

    @Override
    public void setSpectatorTarget(@Nullable final org.spongepowered.api.entity.Entity entity) {
        this.setSpectatingEntity((Entity) entity);
    }

    @Override
    public MessageChannelEvent.Chat simulateChat(final Text message, final Cause cause) {
        checkNotNull(message, "message");

        final TranslationTextComponent component = new TranslationTextComponent("chat.type.text", SpongeTexts.toComponent(((EntityBridge) this).bridge$getDisplayNameText()),
                SpongeTexts.toComponent(message));
        final Text[] messages = SpongeTexts.splitChatMessage(component);

        final MessageChannel originalChannel = this.getMessageChannel();
        final MessageChannelEvent.Chat event = SpongeEventFactory.createMessageChannelEventChat(
                cause, originalChannel, Optional.of(originalChannel),
                new MessageEvent.MessageFormatter(messages[0], messages[1]), message, false
        );
        if (!SpongeImpl.postEvent(event) && !event.isMessageCancelled()) {
            event.getChannel().ifPresent(channel -> channel.send(this, event.getMessage(), ChatTypes.CHAT));
        }
        return event;
    }

    @Override
    public Optional<WorldBorder> getWorldBorder() {
        return Optional.ofNullable(this.api$worldBorder);
    }

    @Override
    public void setWorldBorder(@Nullable final WorldBorder border, final Cause cause) {
        if (this.api$worldBorder == border) {
            return; //do not fire an event since nothing would have changed
        }
        if (!SpongeImpl.postEvent(SpongeEventFactory.createChangeWorldBorderEventTargetPlayer(cause, Optional.ofNullable(this.api$worldBorder), Optional.ofNullable(border), this))) {
            if (this.api$worldBorder != null) { //is the world border about to be unset?
                ((WorldBorderBridge) this.api$worldBorder).accessor$getListeners().remove(((ServerPlayerEntityBridge) this).bridge$getWorldBorderListener()); //remove the listener, if so
            }
            this.api$worldBorder = border;
            if (this.api$worldBorder != null) {
                ((net.minecraft.world.border.WorldBorder) this.api$worldBorder).addListener(((ServerPlayerEntityBridge) this).bridge$getWorldBorderListener());
                this.connection.sendPacket(new SWorldBorderPacket((net.minecraft.world.border.WorldBorder) this.api$worldBorder, SWorldBorderPacket.Action.INITIALIZE));
            } else { //unset the border if null
                this.connection.sendPacket(new SWorldBorderPacket(this.world.getWorldBorder(), SWorldBorderPacket.Action.INITIALIZE));
            }
        }
    }

    @Override
    public CooldownTracker getCooldownTracker() {
        return (CooldownTracker) this.shadow$getCooldownTracker();
    }

    @Override
    public AdvancementProgress getProgress(final Advancement advancement) {
        checkNotNull(advancement, "advancement");
        checkState(((AdvancementBridge) advancement).bridge$isRegistered(), "The advancement must be registered");
        return (AdvancementProgress) this.advancements.getProgress((net.minecraft.advancements.Advancement) advancement);
    }

    @Override
    public Collection<AdvancementTree> getUnlockedAdvancementTrees() {
        return ((PlayerAdvancementsBridge) this.advancements).bridge$getAdvancementTrees();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This is an internal method not intended for use with Players " +
                "as it causes the player to be placed into an undefined state. " +
                "Consider putting them through the normal death process instead.");
    }

    @Override
    public Optional<UUID> getWorldUniqueId() {
        return Optional.of(this.getWorld().getUniqueId());
    }

    @Override
    public boolean setLocation(final Vector3d position, final UUID world) {
        final WorldProperties prop = Sponge.getServer().getWorldProperties(world).orElseThrow(() -> new IllegalArgumentException("Invalid World: No world found for UUID"));
        final World loaded = Sponge.getServer().loadWorld(prop).orElseThrow(() -> new IllegalArgumentException("Invalid World: Could not load world for UUID"));
        return this.setLocation(new Location<>(loaded, position));
    }
}