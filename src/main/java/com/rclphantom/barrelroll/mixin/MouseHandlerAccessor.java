package com.rclphantom.barrelroll.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Expose les deltas souris accumulés de MouseHandler.
 * Ces champs sont remis à zéro après chaque appel à turnPlayer(),
 * donc on les lit avant que vanilla ne le fasse.
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("accumulatedDX") double barrelroll$getAccumulatedDX();
    @Accessor("accumulatedDY") double barrelroll$getAccumulatedDY();
    @Accessor("accumulatedDX") void barrelroll$setAccumulatedDX(double value);
    @Accessor("accumulatedDY") void barrelroll$setAccumulatedDY(double value);
}
