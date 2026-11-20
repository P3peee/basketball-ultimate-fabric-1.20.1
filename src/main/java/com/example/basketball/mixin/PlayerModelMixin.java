package com.example.basketball.mixin;

import com.example.basketball.pose.PoseState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Позы рук: дриблинг с переносом мяча между руками, бросок, пас, блок, перехват, дэнк, спин, падение. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends BipedEntityModel<T> {
    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;

    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void basketball$poses(LivingEntity entity, float limbAngle, float limbDistance,
                                  float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) return;

        long now = entity.getWorld().getTime();
        float delta = animationProgress - (float) Math.floor(animationProgress);
        int id = entity.getId();
        boolean changed = false;

        PoseState.Timed timed = PoseState.timed(id);
        if (timed != null) {
            float p = ((now - timed.start()) - 1 + delta) / PoseState.duration(timed.pose());
            if (p >= 0f && p <= 1f) changed = applyTimed(timed.pose(), p);
        }
        if (!changed && PoseState.dribbling(id, now)) {
            dribbleArms(now, delta, id);
            changed = true;
        }
        if (changed) {
            this.leftSleeve.copyTransform(this.leftArm);
            this.rightSleeve.copyTransform(this.rightArm);
        }
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private void dribbleArms(long now, float delta, int id) {
        double tt = (now - 1) + delta;
        double phase = (((tt % 10) + 10) % 10) / 10.0;
        float b = (float) Math.abs(Math.cos(Math.PI * phase)); // 1 - мяч у руки, 0 - у пола
        float wR = Math.max(0f, Math.min(1f, (PoseState.dribbleSide(id) + 1f) / 2f));
        float wL = 1f - wR;
        float dribblePitch = -0.35f - 0.55f * b;
        float guardPitch = -0.95f;

        this.rightArm.pitch = lerp(guardPitch, dribblePitch, wR);
        this.rightArm.yaw = lerp(-0.4f, -0.15f, wR);
        this.leftArm.pitch = lerp(guardPitch, dribblePitch, wL);
        this.leftArm.yaw = lerp(0.4f, 0.15f, wL);
    }

    /** true - поза управляет руками; false - руки остаются как при дриблинге. */
    private boolean applyTimed(int pose, float p) {
        switch (pose) {
            case PoseState.SHOOT -> {
                if (p < 0.45f) {
                    float k = p / 0.45f;
                    this.rightArm.pitch = lerp(-1.4f, -3.0f, k);
                    this.leftArm.pitch = lerp(-1.4f, -2.4f, k);
                } else {
                    float k = (p - 0.45f) / 0.55f;
                    this.rightArm.pitch = lerp(-3.0f, -2.2f, k);
                    this.leftArm.pitch = lerp(-2.4f, -0.3f, k);
                }
                this.rightArm.yaw = -0.1f;
                this.leftArm.yaw = 0.3f;
                return true;
            }
            case PoseState.PASS -> {
                float s = (float) Math.sin(p * Math.PI);
                this.rightArm.pitch = -1.0f - 0.55f * s;
                this.leftArm.pitch = -1.0f - 0.55f * s;
                this.rightArm.yaw = -0.15f;
                this.leftArm.yaw = 0.15f;
                return true;
            }
            case PoseState.BLOCK -> {
                this.rightArm.pitch = -3.1f;
                this.leftArm.pitch = -3.1f;
                this.rightArm.roll = 0.12f;
                this.leftArm.roll = -0.12f;
                return true;
            }
            case PoseState.STEAL -> {
                this.rightArm.pitch = -1.4f - 0.4f * (float) Math.sin(p * Math.PI);
                this.rightArm.yaw = lerp(-0.9f, 0.9f, p);
                this.leftArm.pitch = -0.5f;
                this.leftArm.yaw = 0.4f;
                return true;
            }
            case PoseState.DUNK -> {
                if (p < 0.5f) {
                    float k = p / 0.5f;
                    this.rightArm.pitch = lerp(-1.0f, -3.2f, k);
                    this.leftArm.pitch = lerp(-1.0f, -3.0f, k);
                } else {
                    float k = (p - 0.5f) / 0.5f;
                    this.rightArm.pitch = lerp(-3.2f, -0.3f, k);
                    this.leftArm.pitch = lerp(-3.0f, -0.4f, k);
                }
                return true;
            }
            case PoseState.CATCH -> {
                this.rightArm.pitch = -1.3f + 0.3f * p;
                this.leftArm.pitch = -1.3f + 0.3f * p;
                this.rightArm.yaw = -0.2f;
                this.leftArm.yaw = 0.2f;
                return true;
            }
            case PoseState.SPIN -> {
                this.rightArm.pitch = -1.1f;
                this.leftArm.pitch = -1.1f;
                this.rightArm.yaw = -0.5f;
                this.leftArm.yaw = 0.5f;
                return true;
            }
            case PoseState.STUMBLE -> {
                float flail = (float) Math.sin(p * 14.0);
                this.rightArm.pitch = -2.2f + 0.5f * flail;
                this.leftArm.pitch = -2.2f - 0.5f * flail;
                this.rightArm.roll = 0.6f;
                this.leftArm.roll = -0.6f;
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
