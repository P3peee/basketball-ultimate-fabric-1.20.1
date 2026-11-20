package com.example.basketball.mixin;

import com.example.basketball.pose.PoseState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Движение всего тела: прокрут спина, наклоны, падение после анкл-брейкера, покачивание при дриблинге. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerTransformsMixin {
    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFF)V",
            at = @At("TAIL"))
    private void basketball$transforms(AbstractClientPlayerEntity player, MatrixStack m, float animationProgress,
                                       float bodyYaw, float tickDelta, CallbackInfo ci) {
        long now = player.getWorld().getTime();
        int id = player.getId();
        float lean = 0f;   // + наклон вперёд
        float roll = 0f;
        float spin = 0f;
        float shift = 0f;

        PoseState.Timed t = PoseState.timed(id);
        if (t != null) {
            float p = ((now - t.start()) - 1 + tickDelta) / PoseState.duration(t.pose());
            if (p >= 0f && p <= 1f) {
                double s = Math.sin(Math.PI * p);
                switch (t.pose()) {
                    case PoseState.SPIN -> spin = 360f * (p * p * (3f - 2f * p));
                    case PoseState.STEPBACK -> lean = (float) (-14.0 * s);
                    case PoseState.CROSS_R -> roll = (float) (10.0 * s);
                    case PoseState.CROSS_L -> roll = (float) (-10.0 * s);
                    case PoseState.SHOOT -> lean = (float) (-5.0 * s);
                    case PoseState.DUNK -> lean = (float) (12.0 * s);
                    case PoseState.STUMBLE -> {
                        lean = (float) (-75.0 * s);
                        shift = (float) (-0.25 * s);
                    }
                    default -> { }
                }
            }
        }
        if (PoseState.dribbling(id, now)) {
            double tt = (now - 1) + tickDelta;
            double phase = (((tt % 10) + 10) % 10) / 10.0;
            double b = Math.abs(Math.cos(Math.PI * phase));
            lean += 7f;                          // корпус вперёд
            shift += (float) (-0.05 * (1.0 - b)); // небольшое приседание в момент толчка
        }

        if (shift != 0f) m.translate(0.0, shift, 0.0);
        if (spin != 0f) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        if (lean != 0f) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-lean));
        if (roll != 0f) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
    }
}
