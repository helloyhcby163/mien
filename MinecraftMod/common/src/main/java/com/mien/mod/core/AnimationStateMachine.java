package com.mien.mod.core;

import java.util.List;

/**
 * 动作状态机：统一管理嘴部状态的三种触发方式。
 * 纯 Java 逻辑，不引用任何 Minecraft 类型；当前状态由渲染层读取。
 */
public final class AnimationStateMachine {
    public enum TriggerMode {
        /** 长按触发，松开恢复。 */
        HOLD,
        /** 点按在两个状态之间切换。 */
        TOGGLE,
        /** 点按循环全部状态。 */
        CYCLE
    }

    /** M 键循环顺序。 */
    public static final List<String> CYCLE_ORDER = List.of(
            "flat", "smile", "sad", "open", "badsmile", "smile_fang", "open_fang");

    private static final String HOLD_STATE = "open";
    private static final String HOLD_RELEASE_STATE = "flat";
    private static final String TOGGLE_STATE_A = "flat";
    private static final String TOGGLE_STATE_B = "smile";

    /** 当前状态名，渲染层（HUD / 3D / GUI 预览）统一读取这里。 */
    public static String currentState = "flat";

    private AnimationStateMachine() {
    }

    public static void setState(String state) {
        if (state == null || state.equals(currentState)) {
            return;
        }
        currentState = state;
        System.out.println("[Mien] current state: " + state);
    }

    /**
     * HOLD：按下进入张嘴。
     */
    public static void onHoldStart() {
        setState(HOLD_STATE);
    }

    /**
     * HOLD：松开立刻恢复平直。
     */
    public static void onHoldEnd() {
        setState(HOLD_RELEASE_STATE);
    }

    /**
     * TOGGLE：flat 与 smile 之间来回切换。
     */
    public static void toggle() {
        setState(TOGGLE_STATE_B.equals(currentState) ? TOGGLE_STATE_A : TOGGLE_STATE_B);
    }

    /**
     * CYCLE：按固定顺序循环全部状态。
     */
    public static void cycle() {
        int index = CYCLE_ORDER.indexOf(currentState);
        setState(CYCLE_ORDER.get((index + 1) % CYCLE_ORDER.size()));
    }
}
