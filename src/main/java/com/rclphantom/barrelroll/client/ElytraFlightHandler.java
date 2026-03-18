package com.rclphantom.barrelroll.client;

import com.rclphantom.barrelroll.BarrelRollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Gestionnaire de vol élytre fluide style "Do a Barrel Roll".
 *
 * Deux angles distincts :
 *   targetRoll  → ce que la souris/touches demandent
 *   currentRoll → ce qui est affiché (suit targetRoll via lerp)
 *
 * La conversion souris → repère corps utilise currentRoll pour la caméra
 * mais targetRoll pour les calculs physiques, évitant le décalage visuel.
 */
@EventBusSubscriber(modid = BarrelRollMod.MOD_ID, value = Dist.CLIENT)
public class ElytraFlightHandler {

    // ── État ────────────────────────────────────────────────────────────────────
    public static float currentRoll = 0f;   // roll visuel interpolé
    public static float targetRoll  = 0f;   // roll cible

    // ── Constantes ──────────────────────────────────────────────────────────────
    /** Vitesse de lerp caméra → cible (plus haut = plus réactif, moins lisse) */
    private static final float ROLL_LERP         = 0.20f;
    /** Vitesse de lacet A/D (degrés/tick) */
    private static final float YAW_SPEED         = 4.5f;
    /** Banking automatique par tick A/D */
    private static final float BANK_FACTOR       = 2.2f;
    /** Décroissance du banking quand A/D relâchés */
    private static final float ROLL_DECAY        = 0.85f;
    /** Roll max (180 = looping complet) */
    private static final float MAX_ROLL          = 180f;

    // ── Caméra ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (isFlying()) {
            event.setRoll(currentRoll);
        }
    }

    // ── Tick ────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;

        if (!player.isFallFlying()) {
            // Retour neutre hors vol
            targetRoll  *= 0.55f;
            currentRoll  = Mth.lerp(0.25f, currentRoll, targetRoll);
            if (Math.abs(currentRoll) < 0.05f) { currentRoll = 0f; targetRoll = 0f; }
            return;
        }

        Options options = mc.options;
        boolean leftDown  = options.keyLeft.isDown();
        boolean rightDown = options.keyRight.isDown();

        if (leftDown && !rightDown) {
            player.setYRot(player.getYRot() - YAW_SPEED);
            targetRoll -= BANK_FACTOR;
        } else if (rightDown && !leftDown) {
            player.setYRot(player.getYRot() + YAW_SPEED);
            targetRoll += BANK_FACTOR;
        } else {
            targetRoll *= ROLL_DECAY;
            if (Math.abs(targetRoll) < 0.05f) targetRoll = 0f;
        }

        targetRoll  = Mth.clamp(targetRoll, -MAX_ROLL, MAX_ROLL);
        // Interpolation douce : la caméra suit le roll cible
        currentRoll = Mth.lerp(ROLL_LERP, currentRoll, targetRoll);
    }

    // ── Entrée souris (appelée depuis MouseHandlerMixin) ─────────────────────────

    /**
     * @param scaledX delta horizontal déjà scalé par la sensibilité (= delta yaw vanilla)
     * @param scaledY delta vertical déjà scalé par la sensibilité (= delta pitch vanilla)
     */
    public static void handleMouseInput(LocalPlayer player, double scaledX, double scaledY) {
        // Souris X → roll cible (accumulé)
        targetRoll += (float) scaledX;
        targetRoll  = Mth.clamp(targetRoll, -MAX_ROLL, MAX_ROLL);

        // Mise à jour immédiate du currentRoll pour que le pitch/yaw soit cohérent
        currentRoll = Mth.lerp(ROLL_LERP, currentRoll, targetRoll);

        // Souris Y → pitch + yaw en repère corps → monde
        float rollRad       = (float) Math.toRadians(currentRoll);
        double worldPitch   = scaledY * Math.cos(rollRad);
        double yawFromPitch = scaledY * Math.sin(rollRad);

        player.turn(yawFromPitch, worldPitch);
    }

    // ── Utilitaire ──────────────────────────────────────────────────────────────

    public static boolean isFlying() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isFallFlying();
    }
}
