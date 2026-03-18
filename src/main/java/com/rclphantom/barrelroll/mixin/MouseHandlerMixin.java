package com.rclphantom.barrelroll.mixin;

import com.rclphantom.barrelroll.client.ElytraFlightHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepte turnPlayer() au HEAD — avant que NeoForge ou vanilla
 * ne traitent les deltas souris — pour appliquer notre logique de vol.
 *
 * Approche "accessor" : on lit accumulatedDX/DY directement,
 * on les remet à zéro, on calcule la sensibilité comme vanilla,
 * puis on délègue à ElytraFlightHandler.
 * Complètement indépendant du corps de la méthode → 0 conflit NeoForge.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayerHead(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!ElytraFlightHandler.isFlying()) return;
        if (!mc.isWindowActive()) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        // Lire et consommer les deltas accumulés
        MouseHandlerAccessor accessor = (MouseHandlerAccessor)(Object)this;
        double dx = accessor.barrelroll$getAccumulatedDX();
        double dy = accessor.barrelroll$getAccumulatedDY();
        accessor.barrelroll$setAccumulatedDX(0.0);
        accessor.barrelroll$setAccumulatedDY(0.0);

        // Même calcul de sensibilité que vanilla
        double sens = mc.options.sensitivity().get() * 0.6 + 0.2;
        double scale = sens * sens * sens * 8.0;
        double scaledX = dx * scale;
        double scaledY = dy * scale;

        // Déléguer au gestionnaire de vol
        ElytraFlightHandler.handleMouseInput(player, scaledX, scaledY);
        ci.cancel();
    }
}
