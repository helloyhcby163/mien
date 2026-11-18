package com.mien.mod;

import java.util.List;

/**
 * 动作状态机：统一管理嘴部状态的三种触发方式。
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

    private AnimationStateMachine() {
    }

    public static void setState(String state) {
        if (state == null || state.equals(MienRenderHelper.currentState)) {
            return;
        }
        MienRenderHelper.currentState = state;
        System.out.println("[Mien] current state: " + state
                + " -> " + MienRenderHelper.getMouthTexture(state));
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
        setState(TOGGLE_STATE_B.equals(MienRenderHelper.currentState) ? TOGGLE_STATE_A : TOGGLE_STATE_B);
    }

    /**
     * CYCLE：按固定顺序循环全部状态。
     */
    public static void cycle() {
        int index = CYCLE_ORDER.indexOf(MienRenderHelper.currentState);
        setState(CYCLE_ORDER.get((index + 1) % CYCLE_ORDER.size()));
    }
}
