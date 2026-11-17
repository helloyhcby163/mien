from PIL import Image, ImageDraw
import os

OUTPUT_DIR = r"E:\helloyhc\Mien\Mien\MinecraftMod\common\src\main\resources\assets\mien\textures\gui"
os.makedirs(OUTPUT_DIR, exist_ok=True)

SIZE = 256
BROWN = (80, 40, 20, 255)
PINK = (200, 100, 120, 255)
WHITE = (255, 255, 255, 255)

def draw_triangle(draw, cx, cy, size, color):
    # 画一个尖角朝下的三角形（虎牙）
    draw.polygon([(cx - size, cy), (cx + size, cy), (cx, cy + size * 2)], fill=color)

def generate_mouth(filename, state, curve=0, thickness=0.05, open_h=0.5, fang=False):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    lw = int(thickness * SIZE)
    x1, x2 = int(0.12 * SIZE), int(0.88 * SIZE)
    mx, my = (x1 + x2) / 2, 0.5 * SIZE

    if state in ("flat", "smile", "sad", "badsmile"):
        # 坏笑是不对称的，左边高一点
        if state == "badsmile":
            cy = my - 0.2 * SIZE # 整体往上
            pts = [(x1, my + 0.1*SIZE), (mx, cy), (x2, cy - 0.1*SIZE)]
        else:
            cy = my - curve * 0.35 * SIZE
            pts = []
            for i in range(50):
                t = i / 49
                x = (1 - t) ** 2 * x1 + 2 * (1 - t) * t * mx + t ** 2 * x2
                y = (1 - t) ** 2 * my + 2 * (1 - t) * t * cy + t ** 2 * my
                pts.append((x, y))
        draw.line(pts, fill=BROWN, width=lw, joint="curve")
        
        # 在线的两端加虎牙
        if fang or state == "badsmile":
            draw_triangle(draw, x1 + lw, my, int(lw*1.5), WHITE)
            draw_triangle(draw, x2 - lw, my, int(lw*1.5), WHITE)

    elif state == "open":
        top_y = my - 0.1 * SIZE
        height = open_h * SIZE
        draw.arc([x1, top_y, x2, top_y + height], start=0, end=180, fill=BROWN, width=lw)
        draw.pieslice([x1 + lw, top_y + lw, x2 - lw, top_y + height - lw], start=0, end=180, fill=PINK)
        
        if fang:
            draw_triangle(draw, x1 + lw*2, top_y + lw*2, int(lw*1.5), WHITE)
            draw_triangle(draw, x2 - lw*2, top_y + lw*2, int(lw*1.5), WHITE)

    img.save(os.path.join(OUTPUT_DIR, filename))
    print(f"已生成: {filename}")

# 生成全部贴图（注意这里 curve 的正负，符合直觉：正数向下是微笑U，负数向上是沮丧^）
generate_mouth("mouth_flat.png", "flat", curve=0, thickness=0.05)
generate_mouth("mouth_smile.png", "smile", curve=0.4, thickness=0.06)
generate_mouth("mouth_sad.png", "sad", curve=-0.4, thickness=0.06)
generate_mouth("mouth_open.png", "open", thickness=0.05, open_h=0.6)
generate_mouth("mouth_badsmile.png", "badsmile", thickness=0.06, fang=True)
generate_mouth("mouth_smile_fang.png", "smile", curve=0.4, thickness=0.06, fang=True)
generate_mouth("mouth_open_fang.png", "open", thickness=0.05, open_h=0.6, fang=True)

print("全部生成完毕！")