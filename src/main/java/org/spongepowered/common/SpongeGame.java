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
package org.spongepowered.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.api.Game;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.SimpleCommandManager;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.data.ImmutableDataRegistry;
import org.spongepowered.api.data.manipulator.DataManipulatorRegistry;
import org.spongepowered.api.data.property.PropertyRegistry;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.util.persistence.SerializationManager;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.common.command.SpongeCommandDisambiguator;
import org.spongepowered.common.data.SpongeDataRegistry;
import org.spongepowered.common.data.SpongeImmutableRegistry;
import org.spongepowered.common.data.property.SpongePropertyRegistry;
import org.spongepowered.common.registry.SpongeGameRegistry;
import org.spongepowered.common.config.SpongeConfigManager;
import org.spongepowered.common.util.persistence.SpongeSerializationManager;
import org.spongepowered.common.scheduler.SpongeScheduler;

import java.nio.file.Path;

import javax.inject.Singleton;

@Singleton
public abstract class SpongeGame implements Game {

    public static final String API_VERSION = Objects.firstNonNull(SpongeGame.class.getPackage().getSpecificationVersion(), "DEV");
    public static final String IMPLEMENTATION_VERSION = Objects.firstNonNull(SpongeGame.class.getPackage().getImplementationVersion(), "DEV");

    public static final MinecraftVersion MINECRAFT_VERSION = new SpongeMinecraftVersion("1.8", 47);

    private final PluginManager pluginManager;
    private final EventManager eventManager;
    private final SpongeGameRegistry gameRegistry;
    private final ServiceManager serviceManager;
    private final TeleportHelper teleportHelper;
    private final ConfigManager configManager;
    private final CommandManager commandManager;

    protected SpongeGame(PluginManager pluginManager, EventManager eventManager, SpongeGameRegistry gameRegistry, ServiceManager serviceManager,
            TeleportHelper teleportHelper) {
        this.pluginManager = checkNotNull(pluginManager, "pluginManager");
        this.eventManager = checkNotNull(eventManager, "eventManager");
        this.gameRegistry = checkNotNull(gameRegistry, "gameRegistry");
        this.serviceManager = checkNotNull(serviceManager, "serviceManager");
        this.teleportHelper = checkNotNull(teleportHelper, "teleportHelper");
        this.configManager = new SpongeConfigManager(this.pluginManager);
        this.commandManager = new SimpleCommandManager(this, SpongeBootstrap.slf4jLogger, new SpongeCommandDisambiguator(this));

    }

    @Override
    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    @Override
    public EventManager getEventManager() {
        return this.eventManager;
    }

    @Override
    public SpongeGameRegistry getRegistry() {
        return this.gameRegistry;
    }

    @Override
    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public TeleportHelper getTeleportHelper() {
        return this.teleportHelper;
    }

    @Override
    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public Scheduler getScheduler() {
        return SpongeScheduler.getInstance();
    }

    @Override
    public Server getServer() {
        return (Server) MinecraftServer.getServer();
    }

    @Override
    public abstract Path getSavesDirectory();

    @Override
    public SerializationManager getSerializationManager() {
        return SpongeSerializationManager.getInstance();
    }

    @Override
    public PropertyRegistry getPropertyRegistry() {
        return SpongePropertyRegistry.getInstance();
    }

    @Override
    public DataManipulatorRegistry getManipulatorRegistry() {
        return SpongeDataRegistry.getInstance();
    }

    @Override
    public ImmutableDataRegistry getImmutableDataRegistry() {
        return SpongeImmutableRegistry.getInstance();
    }

}
