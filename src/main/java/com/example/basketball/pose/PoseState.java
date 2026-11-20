package com.example.basketball.pose;

import java.util.HashMap;
import java.util.Map;

/** Клиентское хранилище поз игроков. Только данные - безопасно грузить на сервере. */
public final class PoseState {
    public static final int NONE = 0, SHOOT = 2, PASS = 3, BLOCK = 4, STEAL = 5, DUNK = 6, CATCH = 7,
            STUMBLE = 10, SPIN = 11, STEPBACK = 12, CROSS_R = 13, CROSS_L = 14;

    public record Timed(int pose, long start) { }
    private record Drib(long time, float side) { }

    private static final Map<Integer, Timed> TIMED = new HashMap<>();
    private static final Map<Integer, Drib> DRIBBLE = new HashMap<>();

    public static void setTimed(int entityId, int pose, long now) { TIMED.put(entityId, new Timed(pose, now)); }
    public static Timed timed(int entityId) { return TIMED.get(entityId); }

    /** side: +1 мяч в правой руке, -1 в левой, значения между - во время кроссовера. */
    public static void markDribble(int entityId, long now, float side) { DRIBBLE.put(entityId, new Drib(now, side)); }

    public static boolean dribbling(int entityId, long now) {
        Drib d = DRIBBLE.get(entityId);
        return d != null && now - d.time() <= 2;
    }

    public static float dribbleSide(int entityId) {
        Drib d = DRIBBLE.get(entityId);
        return d == null ? 1f : d.side();
    }

    public static int duration(int pose) {
        return switch (pose) {
            case SHOOT -> 14;
            case PASS -> 8;
            case BLOCK -> 14;
            case STEAL -> 8;
            case DUNK -> 18;
            case CATCH -> 6;
            case STUMBLE -> 24;
            case SPIN -> 12;
            case STEPBACK -> 12;
            case CROSS_R, CROSS_L -> 10;
            default -> 1;
        };
    }

    private PoseState() { }
}
