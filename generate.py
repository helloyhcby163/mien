from PIL import Image, ImageDraw
import os

# 你的贴图存放路径（注意修改成你自己的实际路径）
OUTPUT_DIR = r"E:\helloyhc\Mien\Mien\MinecraftMod\common\src\main\resources\assets\mien\textures\gui"
os.makedirs(OUTPUT_DIR, exist_ok=True)

SIZE = 256
BROWN = (80, 40, 20, 255) # 深棕色
PINK = (200, 100, 120, 255) # 粉色

def generate_mouth(state, curve=0, thickness=0.05, open_h=0.5):
    # 创建 256x256 全透明画布
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    lw = int(thickness * SIZE)
    x1, x2 = int(0.12 * SIZE), int(0.88 * SIZE)
    mx, my = (x1 + x2) / 2, 0.5 * SIZE

    if state in ("flat", "smile", "sad"):
        c = curve
        # 计算控制点，实现弧度
        cy = my - c * 0.35 * SIZE
        # 简单的抛物线/贝塞尔模拟
        pts = []
        for i in range(50):
            t = i / 49
            x = (1 - t) ** 2 * x1 + 2 * (1 - t) * t * mx + t ** 2 * x2
            y = (1 - t) ** 2 * my + 2 * (1 - t) * t * cy + t ** 2 * my
            pts.append((x, y))
        draw.line(pts, fill=BROWN, width=lw, joint="curve")

    elif state == "open":
        # 张嘴：画一个半圆或椭圆
        top_y = my - 0.1 * SIZE
        height = open_h * SIZE
        # 外框
        draw.arc([x1, top_y, x2, top_y + height], start=0, end=180, fill=BROWN, width=lw)
        # 内部粉色
        draw.pieslice([x1 + lw, top_y + lw, x2 - lw, top_y + height - lw], start=0, end=180, fill=PINK)

    # 保存为 PNG
    filename = f"mouth_{state}.png"
    img.save(os.path.join(OUTPUT_DIR, filename))
    print(f"已生成: {filename}")

# 生成四种状态
generate_mouth("flat", curve=0, thickness=0.05)
generate_mouth("smile", curve=0.5, thickness=0.06)
generate_mouth("sad", curve=-0.5, thickness=0.06)
generate_mouth("open", thickness=0.05, open_h=0.6)

print(f"全部生成完毕！请前往 {OUTPUT_DIR} 查看。")