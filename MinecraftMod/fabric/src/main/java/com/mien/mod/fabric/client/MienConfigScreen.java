package com.mien.mod.fabric.client;

import com.mien.mod.MienConfig;
import com.mien.mod.MienRenderHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * 实时调参界面：左侧预览脸部与嘴巴，右侧三个滑块控制偏移与缩放。
 */
public final class MienConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 240;
    private static final int PREVIEW_SIZE = 160;
    private static final int MARGIN = 20;
    private static final int SLIDER_WIDTH = 160;

    private float offsetX;
    private float offsetY;
    private float scale;

    private int panelX;
    private int panelY;

    public MienConfigScreen() {
        super(Text.literal("Mien 调参"));
        MienConfig config = MienConfig.getInstance();
        if (config != null) {
            this.offsetX = config.mouth_offset_x;
            this.offsetY = config.mouth_offset_y;
            this.scale = config.getMouthScale();
        } else {
            this.scale = 1.0F;
        }
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        int sliderX = this.panelX + PREVIEW_SIZE + MARGIN * 2;
        int sliderY = this.panelY + 40;

        this.addDrawableChild(new ValueSlider(sliderX, sliderY, SLIDER_WIDTH, 20, "offsetX", -8.0, 8.0, this.offsetX,
                value -> {
                    this.offsetX = (float) value;
                    MienConfig config = MienConfig.getInstance();
                    if (config != null) {
                        config.mouth_offset_x = this.offsetX;
                    }
                }));
        this.addDrawableChild(new ValueSlider(sliderX, sliderY + 30, SLIDER_WIDTH, 20, "offsetY", -8.0, 8.0, this.offsetY,
                value -> {
                    this.offsetY = (float) value;
                    MienConfig config = MienConfig.getInstance();
                    if (config != null) {
                        config.mouth_offset_y = this.offsetY;
                    }
                }));
        this.addDrawableChild(new ValueSlider(sliderX, sliderY + 60, SLIDER_WIDTH, 20, "scale", 0.1, 3.0, this.scale,
                value -> {
                    this.scale = (float) value;
                    MienConfig config = MienConfig.getInstance();
                    if (config != null) {
                        config.mouth_scale = this.scale;
                    }
                }));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("保存并关闭"), button -> this.saveAndClose())
                .dimensions(this.panelX + (PANEL_WIDTH - SLIDER_WIDTH) / 2, this.panelY + PANEL_HEIGHT - 40, SLIDER_WIDTH, 20)
                .build());
    }

    private void saveAndClose() {
        MienConfig config = MienConfig.getInstance();
        if (config != null) {
            config.mouth_offset_x = this.offsetX;
            config.mouth_offset_y = this.offsetY;
            config.mouth_scale = this.scale;
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

        // 左侧放大版脸部与嘴巴，直接读取滑块字段，保证拖动时实时刷新。
        MienRenderHelper.renderFaceAndMouth(
                context, this.client,
                this.panelX + MARGIN, this.panelY + 40, PREVIEW_SIZE,
                MienRenderHelper.currentState,
                this.offsetX, this.offsetY, this.scale
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
