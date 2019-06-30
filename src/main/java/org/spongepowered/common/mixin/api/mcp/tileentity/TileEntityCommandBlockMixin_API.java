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
package org.spongepowered.common.mixin.api.mcp.tileentity;

import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.tileentity.TileEntityCommandBlock;
import org.spongepowered.api.block.tileentity.CommandBlock;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.util.Constants;

import java.util.List;

@NonnullByDefault
@Mixin(TileEntityCommandBlock.class)
public abstract class TileEntityCommandBlockMixin_API extends TileEntityMixin_API implements CommandBlock {

    @Shadow public abstract CommandBlockBaseLogic getCommandBlockLogic();

    @Override
    public void supplyVanillaManipulators(List<DataManipulator<?, ?>> manipulators) {
        super.supplyVanillaManipulators(manipulators);
        manipulators.add(getCommandData());
    }

    @Override
    public void execute() {
        getCommandBlockLogic().trigger(this.world);
    }

    @Override
    public String getName() {
        return getCommandBlockLogic().getName();
    }

    @Override
    @SuppressWarnings("deprecated")
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        container.set(Constants.TileEntity.CommandBlock.STORED_COMMAND, this.getCommandBlockLogic().getCommand());
        container.set(Constants.TileEntity.CommandBlock.SUCCESS_COUNT, this.getCommandBlockLogic().getSuccessCount());
        container.set(Constants.TileEntity.CUSTOM_NAME, this.getCommandBlockLogic().getName());
        container.set(Constants.TileEntity.CommandBlock.DOES_TRACK_OUTPUT, this.getCommandBlockLogic().shouldTrackOutput());
        if (this.getCommandBlockLogic().shouldTrackOutput()) {
            container.set(Constants.TileEntity.CommandBlock.TRACKED_OUTPUT, SpongeTexts.toLegacy(this.getCommandBlockLogic().getLastOutput()));
        }
        return container;
    }
}
