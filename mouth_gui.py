import json
import tkinter as tk
from tkinter import ttk, filedialog, messagebox
from PIL import Image, ImageDraw, ImageTk

FACE_X, FACE_Y, FACE_SIZE = 8, 8, 8
ZOOM = 32
STATES = ["flat", "smile", "sad", "open"]
STATE_LABELS = {
    "flat": "flat 平直",
    "smile": "smile 微笑",
    "sad": "sad 沮丧",
    "open": "open 张嘴"
}

def hex_to_rgb(s):
    s = s.lstrip("#")
    if len(s) != 6: raise ValueError
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))

def bezier_quad(p0, p1, p2, n=32):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t ** 2 * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t ** 2 * p2[1]
        pts.append((x, y))
    return pts

def draw_overlay(state, color, curve, thickness, open_h):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    S = 256
    lw = max(2, int(thickness * S))
    x1, x2 = 0.12 * S, 0.88 * S
    mx, my = (x1 + x2) / 2, 0.5 * S

    if state in ("flat", "smile", "sad"):
        c = 0.0
        if state == "smile": c = curve
        elif state == "sad": c = -curve
        cy = my - c * 0.35 * S
        pts = bezier_quad((x1, my), (mx, cy), (x2, my))
        d.line(pts, fill=color, width=lw, joint="curve")
    elif state == "open":
        top_y = my - 0.1 * S
        radius = (x2 - x1) / 2
        d.arc([x1, top_y, x2, top_y + radius * 2 * open_h], start=0, end=180, fill=color, width=lw)
        inner_box = [x1 + lw, top_y + lw, x2 - lw, top_y + radius * 2 * open_h - lw]
        if inner_box[3] > inner_box[1]:
            d.pieslice(inner_box, start=0, end=180, fill=(200, 100, 120, 255))
    return img

class MouthGui(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Mien - Mouth Overlay Tuner v3 (Auto Memory)")
        self.geometry("820x680")
        
        # 默认皮肤
        self.skin = Image.new("RGBA", (64, 64), (230, 180, 150, 255))
        ImageDraw.Draw(self.skin).rectangle([8, 8, 15, 15], fill=(235, 185, 155, 255))
        self.scale = 1
        
        # 核心数据：按状态保存字典
        # 默认初始值
        base_state = {"x": 10.0, "y": 12.0, "w": 4.0, "h": 2.5, "curve": 0.5, "open_h": 0.6, "thickness": 0.08}
        self.data = {state: base_state.copy() for state in STATES}
        
        self.color = (80, 40, 20)
        self.state = "flat"
        
        self.drag_off = None
        self.photo = None
        self._build_ui()
        self._load_state_to_ui()
        self.redraw()

    def _build_ui(self):
        self.grid_columnconfigure(1, weight=1)
        self.grid_rowconfigure(0, weight=1)

        left = tk.Frame(self)
        left.grid(row=0, column=0, padx=12, pady=12, sticky="n")
        self.canvas = tk.Canvas(left, width=FACE_SIZE * ZOOM, height=FACE_SIZE * ZOOM, bg="#222", highlightthickness=0)
        self.canvas.pack()
        self.canvas.bind("<Button-1>", self.on_press)
        self.canvas.bind("<B1-Motion>", self.on_drag)
        self.canvas.bind("<ButtonRelease-1>", self.on_release)
        tk.Label(left, text="拖动移动，滑块微调\n每个状态独立记忆").pack(pady=(8, 0))

        right = tk.Frame(self)
        right.grid(row=0, column=1, padx=12, pady=12, sticky="nsew")
        right.grid_columnconfigure(1, weight=1)

        tk.Button(right, text="加载皮肤 PNG", command=self.load_skin).grid(row=0, column=0, columnspan=2, sticky="ew", pady=6)

        tk.Label(right, text="状态").grid(row=1, column=0, sticky="w", pady=4)
        self.state_var = tk.StringVar(value=STATE_LABELS[self.state])
        cb = ttk.Combobox(right, textvariable=self.state_var, values=[STATE_LABELS[s] for s in STATES], state="readonly")
        cb.grid(row=1, column=1, sticky="ew", pady=4)
        cb.bind("<<ComboboxSelected>>", self.on_state)

        self.sl_w = self._slider(right, 2, "嘴宽", 1.0, 8.0, lambda v: self.set_param("w", v))
        self.sl_h = self._slider(right, 3, "嘴高", 1.0, 6.0, lambda v: self.set_param("h", v))
        self.sl_x = self._slider(right, 4, "锚点 x", 0.0, 15.0, lambda v: self.set_param("x", v))
        self.sl_y = self._slider(right, 5, "锚点 y", 0.0, 15.0, lambda v: self.set_param("y", v))
        self.sl_cv = self._slider(right, 6, "曲线", -1.0, 1.0, lambda v: self.set_param("curve", v))
        self.sl_oh = self._slider(right, 7, "张嘴高度", 0.1, 1.5, lambda v: self.set_param("open_h", v))
        self.sl_th = self._slider(right, 8, "粗细", 0.02, 0.20, lambda v: self.set_param("thickness", v))

        tk.Label(right, text="颜色 #RRGGBB").grid(row=9, column=0, sticky="w", pady=4)
        self.color_entry = tk.Entry(right)
        self.color_entry.insert(0, "#502814")
        self.color_entry.grid(row=9, column=1, sticky="ew", pady=4)
        self.color_entry.bind("<KeyRelease>", self.on_color)

        tk.Button(right, text="保存预设 JSON (包含所有状态)", command=self.export_json, bg="#aaddaa").grid(row=10, column=0, columnspan=2, sticky="ew", pady=16)

    def _slider(self, parent, row, label, lo, hi, cb):
        tk.Label(parent, text=label).grid(row=row, column=0, sticky="w", pady=2)
        s = tk.Scale(parent, from_=lo, to=hi, resolution=(hi - lo) / 100, orient="horizontal", showvalue=True, command=cb)
        s.grid(row=row, column=1, sticky="ew", pady=2)
        return s

    def _load_state_to_ui(self):
        """把当前状态的数据加载到滑块上"""
        d = self.data[self.state]
        self.sl_w.set(d["w"])
        self.sl_h.set(d["h"])
        self.sl_x.set(d["x"])
        self.sl_y.set(d["y"])
        self.sl_cv.set(d["curve"])
        self.sl_oh.set(d["open_h"])
        self.sl_th.set(d["thickness"])
        self._update_slider_states()

    def _update_slider_states(self):
        if self.state in ("smile", "sad"):
            self.sl_cv.config(state="normal")
        else:
            self.sl_cv.config(state="disabled")
        if self.state == "open":
            self.sl_oh.config(state="normal")
        else:
            self.sl_oh.config(state="disabled")

    def set_param(self, key, v):
        """滑块变动时，写入当前状态的数据"""
        self.data[self.state][key] = float(v)
        self.redraw()

    def on_state(self, _e=None):
        """切换状态时，把界面刷新为新状态的数据"""
        for s, lab in STATE_LABELS.items():
            if lab == self.state_var.get():
                self.state = s
                break
        self._load_state_to_ui()
        self.redraw()

    def on_color(self, _e=None):
        try:
            self.color = hex_to_rgb(self.color_entry.get())
            self.redraw()
        except ValueError:
            pass

    def compose_preview(self):
        face_box = (FACE_X * self.scale, FACE_Y * self.scale, (FACE_X + FACE_SIZE) * self.scale, (FACE_Y + FACE_SIZE) * self.scale)
        face = self.skin.crop(face_box).resize((FACE_SIZE * ZOOM, FACE_SIZE * ZOOM), Image.NEAREST).convert("RGBA")
        
        d = self.data[self.state]
        overlay = draw_overlay(self.state, self.color + (255,), d["curve"], d["thickness"], d["open_h"])
        
        ow = max(1, int(round(d["w"] * ZOOM)))
        oh = max(1, int(round(d["h"] * ZOOM)))
        overlay = overlay.resize((ow, oh), Image.LANCZOS)
        
        ox = int(round((d["x"] - FACE_X) * ZOOM))
        oy = int(round((d["y"] - FACE_Y) * ZOOM))
        face.alpha_composite(overlay, dest=(ox, oy))
        return face

    def redraw(self):
        img = self.compose_preview()
        self.photo = ImageTk.PhotoImage(img)
        self.canvas.delete("all")
        self.canvas.create_image(0, 0, anchor="nw", image=self.photo)
        for i in range(FACE_SIZE + 1):
            p = i * ZOOM
            self.canvas.create_line(p, 0, p, FACE_SIZE * ZOOM, fill="#333")
            self.canvas.create_line(0, p, FACE_SIZE * ZOOM, p, fill="#333")

    def on_press(self, e):
        lx = FACE_X + e.x / ZOOM
        ly = FACE_Y + e.y / ZOOM
        d = self.data[self.state]
        self.drag_off = (lx - (d["x"] + d["w"] / 2), ly - (d["y"] + d["h"] / 2))

    def on_drag(self, e):
        if not self.drag_off: return
        lx = FACE_X + e.x / ZOOM
        ly = FACE_Y + e.y / ZOOM
        d = self.data[self.state]
        d["x"] = lx - self.drag_off[0] - d["w"] / 2
        d["y"] = ly - self.drag_off[1] - d["h"] / 2
        self.sl_x.set(d["x"])
        self.sl_y.set(d["y"])
        self.redraw()

    def on_release(self, _e):
        self.drag_off = None

    def load_skin(self):
        path = filedialog.askopenfilename(filetypes=[("PNG", "*.png")])
        if not path: return
        img = Image.open(path).convert("RGBA")
        if img.width != img.height or img.width < 64 or img.width % 64 != 0:
            messagebox.showerror("错误", "请选择正方形、64 的整数倍尺寸的皮肤。")
            return
        self.skin = img
        self.scale = img.width // 64
        self.redraw()

    def export_json(self):
        # 最终定稿的数据结构
        final_data = {
            "skin_scale": self.scale,
            "face": {"x": FACE_X, "y": FACE_Y, "size": FACE_SIZE},
            "color": list(self.color),
            "states": self.data
        }
        path = filedialog.asksaveasfilename(defaultextension=".json", filetypes=[("JSON", "*.json")], initialfile="mien_preset.json")
        if path:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(final_data, f, ensure_ascii=False, indent=2)
            messagebox.showinfo("导出成功", f"已保存到 {path}\n\n你可以把这个 JSON 发给开发者了！")

if __name__ == "__main__":
    MouthGui().mainloop()