package com.mien.mod.neoforge.client;

import com.mien.mod.MienConfig;
import com.mien.mod.MienRenderHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 * 实时调参界面：左侧按当前状态实时预览脸部与嘴巴，右侧切换状态并调整该状态专属的 x / y / w / h。
 */
public final class MienConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 260;
    private static final int PREVIEW_SIZE = 160;
    private static final int MARGIN = 20;
    private static final int CONTROL_X = 200;
    private static final int CONTROL_WIDTH = 200;

    private String stateName;
    private ValueSlider xSlider;
    private ValueSlider ySlider;
    private ValueSlider wSlider;
    private ValueSlider hSlider;

    private int panelX;
    private int panelY;

    public MienConfigScreen() {
        super(Text.literal("Mien 调参"));
        this.stateName = resolveInitialState();
    }

    private static String resolveInitialState() {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.states == null || config.states.isEmpty()) {
            return MienRenderHelper.currentState;
        }
        if (config.states.containsKey(MienRenderHelper.currentState)) {
            return MienRenderHelper.currentState;
        }
        return config.states.keySet().iterator().next();
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        int controlX = this.panelX + CONTROL_X;
        int buttonY = this.panelY + 45;

        // 状态循环切换按钮：列出配置中所有状态。
        this.addDrawableChild(ButtonWidget.builder(Text.literal("状态: " + this.stateName), button -> {
            cycleState();
            button.setMessage(Text.literal("状态: " + this.stateName));
        }).dimensions(controlX, buttonY, CONTROL_WIDTH, 20).build());

        MienConfig.StateData data = currentState();
        float x = data == null ? 12.0F : data.x;
        float y = data == null ? 13.0F : data.y;
        float w = data == null ? 2.0F : data.w;
        float h = data == null ? 2.5F : data.h;

        int sliderY = buttonY + 30;
        this.xSlider = this.addDrawableChild(new ValueSlider(controlX, sliderY, CONTROL_WIDTH, 20, "x", 0.0, 32.0, x,
                value -> setStateField(0, (float) value)));
        this.ySlider = this.addDrawableChild(new ValueSlider(controlX, sliderY + 30, CONTROL_WIDTH, 20, "y", 0.0, 32.0, y,
                value -> setStateField(1, (float) value)));
        this.wSlider = this.addDrawableChild(new ValueSlider(controlX, sliderY + 60, CONTROL_WIDTH, 20, "w", 0.1, 16.0, w,
                value -> setStateField(2, (float) value)));
        this.hSlider = this.addDrawableChild(new ValueSlider(controlX, sliderY + 90, CONTROL_WIDTH, 20, "h", 0.1, 16.0, h,
                value -> setStateField(3, (float) value)));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("保存并关闭"), button -> this.saveAndClose())
                .dimensions(this.panelX + (PANEL_WIDTH - CONTROL_WIDTH) / 2, this.panelY + PANEL_HEIGHT - 40, CONTROL_WIDTH, 20)
                .build());
    }

    private MienConfig.StateData currentState() {
        return MienRenderHelper.getStateData(this.stateName);
    }

    /**
     * 滑块直接写回当前状态在 MienConfig 中的专属字段。
     */
    private void setStateField(int field, float value) {
        MienConfig.StateData data = currentState();
        if (data == null) {
            return;
        }
        switch (field) {
            case 0 -> data.x = value;
            case 1 -> data.y = value;
            case 2 -> data.w = value;
            case 3 -> data.h = value;
            default -> {
            }
        }
    }

    /**
     * 切换状态后，把滑块同步为新状态在 JSON 中的专属值。
     */
    private void cycleState() {
        MienConfig config = MienConfig.getInstance();
        if (config == null || config.states == null || config.states.isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>(config.states.keySet());
        int index = names.indexOf(this.stateName);
        this.stateName = names.get((index + 1) % names.size());
        // 世界中的 3D 嘴巴也跟随到当前编辑的状态。
        MienRenderHelper.currentState = this.stateName;
        syncSliders();
    }

    private void syncSliders() {
        MienConfig.StateData data = currentState();
        if (data == null) {
            return;
        }
        this.xSlider.setRawValue(data.x);
        this.ySlider.setRawValue(data.y);
        this.wSlider.setRawValue(data.w);
        this.hSlider.setRawValue(data.h);
    }

    private void saveAndClose() {
        MienConfig config = MienConfig.getInstance();
        if (config != null && config.states != null) {
            for (Map.Entry<String, MienConfig.StateData> entry : config.states.entrySet()) {
                MienConfig.StateData data = entry.getValue();
                System.out.println("[Mien] save state " + entry.getKey()
                        + " x=" + data.x + " y=" + data.y + " w=" + data.w + " h=" + data.h);
            }
        }
        MienConfig.save();
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明黑色背景：整屏压暗 + 中央面板。
        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT, 0xC0101010);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.panelX + PANEL_WIDTH / 2, this.panelY + 10, 0xFFFFFF);

        // 左侧预览：按当前状态的贴图与 x / y / w / h 每帧重绘，拖动滑块即时生效。
        MienRenderHelper.renderFaceAndMouth(
                context, this.client,
                this.panelX + MARGIN, this.panelY + 45, PREVIEW_SIZE,
                this.stateName
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private static final class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer onChange;

        private ValueSlider(int x, int y, int width, int height, String label, double min, double max,
                            double value, DoubleConsumer onChange) {
            super(x, y, width, height, Text.literal(label), normalize(value, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            this.updateMessage();
        }

        private static double normalize(double value, double min, double max) {
            return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
        }

        private double currentValue() {
            return this.min + (this.max - this.min) * this.value;
        }

        /**
         * 由外部（切换状态）同步数值，只更新滑块位置与文字，不触发写回。
         */
        private void setRawValue(double value) {
            this.value = normalize(value, this.min, this.max);
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(String.format(Locale.ROOT, "%s: %.2f", this.label, this.currentValue())));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.currentValue());
        }
    }
}
