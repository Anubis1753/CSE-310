"""A small, dependency-free first-person 3D voxel sandbox.

Run with: python Minecraft/minecraft.py

Controls:
    W/S                  Move forward/back
    A/D                  Strafe left/right
    Left/right arrows    Turn
    Mouse movement       Look around
    Space                Jump one block
    Hold left click      Mine the block under the crosshair
    Right click          Place the selected block
    1-5                  Select grass, dirt, stone, wood, or leaves
    R                    Generate a new world
    Esc                  Release the mouse look

The renderer is a simple software ray caster. It uses a real X/Y/Z block
world, so this remains playable without installing pygame or an OpenGL library.
"""

import math
import random
import tkinter as tk


WIDTH = 960
HEIGHT = 640
FOV = math.radians(70)
MAX_REACH = 7.0
RAY_STEP = 3
GRAVITY = 18.0
PLAYER_HEIGHT = 2.0
PLAYER_RADIUS = 0.28
BLOCK_HEIGHT = 1.0
# A small margin compensates for discrete frame updates while staying at one block.
JUMP_SPEED = math.sqrt(2 * GRAVITY * 1.12)

BLOCKS = {
    0: ("Air", "#87CEEB"),
    1: ("Grass", "#58A942"),
    2: ("Dirt", "#9B642F"),
    3: ("Stone", "#777777"),
    4: ("Wood", "#A8733D"),
    5: ("Leaves", "#3E963B"),
}


class VoxelWorld:
    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("Tiny 3D Minecraft")
        self.root.resizable(False, False)
        self.canvas = tk.Canvas(root, width=WIDTH, height=HEIGHT,
                                bg="#87CEEB", highlightthickness=0)
        self.canvas.pack()
        self.canvas.focus_set()

        self.size_x = 28
        self.size_y = 10
        self.size_z = 28
        self.world: list[list[list[int]]] = []
        self.keys: set[str] = set()
        self.selected = 1
        self.mining = False
        self.mine_timer = 0.0
        self.jump_requested = False
        self.mouse_look = True
        self.last_mouse = None
        self.cursor = (WIDTH // 2, HEIGHT // 2)
        self.player = {"x": 14.5, "y": 4.0, "z": 14.5,
                       "vy": 0.0, "yaw": 0.0, "pitch": 0.0}
        self.on_ground = False
        self.make_world()

        root.bind("<KeyPress>", self.key_down)
        root.bind("<KeyRelease>", self.key_up)
        root.bind("<Escape>", self.release_mouse)
        self.canvas.bind("<Motion>", self.mouse_move)
        self.canvas.bind("<ButtonPress-1>", self.start_mining)
        self.canvas.bind("<ButtonRelease-1>", self.stop_mining)
        self.canvas.bind("<Button-3>", self.place_block)
        self.loop()

    def make_world(self) -> None:
        self.world = [
            [[0 for _ in range(self.size_z)] for _ in range(self.size_x)]
            for _ in range(self.size_y)
        ]
        for x in range(self.size_x):
            for z in range(self.size_z):
                surface = 2 + (1 if random.random() < 0.12 else 0)
                for y in range(surface):
                    self.world[y][x][z] = 3 if y == 0 else 2
                self.world[surface][x][z] = 1

        for x, z in ((6, 7), (11, 20), (20, 9), (23, 22)):
            surface = self.highest_block(x, z)
            for y in range(surface + 1, min(self.size_y, surface + 4)):
                self.world[y][x][z] = 4
            for lx in range(x - 2, x + 3):
                for lz in range(z - 2, z + 3):
                    for ly in range(surface + 2, min(self.size_y, surface + 5)):
                        if (lx - x) ** 2 + (lz - z) ** 2 < 5 and self.in_bounds(lx, ly, lz):
                            if self.world[ly][lx][lz] == 0:
                                self.world[ly][lx][lz] = 5

        self.player.update(x=14.5, y=self.highest_block(14, 14) + 1.02,
                           z=14.5, vy=0.0, yaw=0.0, pitch=0.0)
        self.on_ground = False

    def in_bounds(self, x: int, y: int, z: int) -> bool:
        return 0 <= x < self.size_x and 0 <= y < self.size_y and 0 <= z < self.size_z

    def block_at(self, x: int, y: int, z: int) -> int:
        return self.world[y][x][z] if self.in_bounds(x, y, z) else 3

    def highest_block(self, x: int, z: int) -> int:
        for y in range(self.size_y - 1, -1, -1):
            if self.in_bounds(x, y, z) and self.world[y][x][z]:
                return y
        return 0

    def key_down(self, event: tk.Event) -> None:
        key = event.keysym.lower()
        if key == "space" and key not in self.keys:
            self.jump_requested = True
        if key in "12345":
            self.selected = int(key)
        elif key == "r":
            self.make_world()
        self.keys.add(key)

    def key_up(self, event: tk.Event) -> None:
        self.keys.discard(event.keysym.lower())

    def release_mouse(self, _event: tk.Event) -> None:
        self.mouse_look = False

    def mouse_move(self, event: tk.Event) -> None:
        self.cursor = (event.x, event.y)
        if self.last_mouse is not None and self.mouse_look:
            dx = event.x - self.last_mouse[0]
            dy = event.y - self.last_mouse[1]
            self.player["yaw"] += dx * 0.004
            # Screen Y grows downward, so moving the mouse up must look up.
            self.player["pitch"] = max(-0.65, min(0.65, self.player["pitch"] - dy * 0.003))
        self.last_mouse = (event.x, event.y)

    def collides(self, x: float, y: float, z: float) -> bool:
        if x - PLAYER_RADIUS < 0 or x + PLAYER_RADIUS >= self.size_x:
            return True
        if z - PLAYER_RADIUS < 0 or z + PLAYER_RADIUS >= self.size_z:
            return True
        if y < 0 or y + PLAYER_HEIGHT > self.size_y:
            return True
        for bx in range(math.floor(x - PLAYER_RADIUS), math.floor(x + PLAYER_RADIUS) + 1):
            for bz in range(math.floor(z - PLAYER_RADIUS), math.floor(z + PLAYER_RADIUS) + 1):
                for by in range(math.floor(y), math.floor(y + PLAYER_HEIGHT - 0.001) + 1):
                    if self.block_at(bx, by, bz):
                        return True
        return False

    def update(self, dt: float) -> None:
        dt = min(dt, 0.05)
        yaw = self.player["yaw"]
        forward = ("w" in self.keys) - ("s" in self.keys)
        strafe = ("d" in self.keys) - ("a" in self.keys)
        turn = ("right" in self.keys) - ("left" in self.keys)
        self.player["yaw"] += turn * 2.4 * dt

        length = math.hypot(forward, strafe) or 1
        speed = 4.2 * dt / length
        dx = (math.cos(yaw) * forward - math.sin(yaw) * strafe) * speed
        dz = (math.sin(yaw) * forward + math.cos(yaw) * strafe) * speed
        self.move_horizontal(dx, dz)

        if self.jump_requested and self.on_ground:
            self.player["vy"] = JUMP_SPEED
            self.on_ground = False
        self.jump_requested = False
        self.player["vy"] -= GRAVITY * dt
        new_y = self.player["y"] + self.player["vy"] * dt
        if not self.collides(self.player["x"], new_y, self.player["z"]):
            self.player["y"] = new_y
            self.on_ground = False
        elif self.player["vy"] < 0:
            self.player["y"] = math.floor(new_y) + 1.0
            self.player["vy"] = 0
            self.on_ground = True
        else:
            self.player["y"] = math.ceil(new_y + PLAYER_HEIGHT) - PLAYER_HEIGHT
            self.player["vy"] = 0

        if self.mine_timer > 0:
            self.mine_timer -= dt
        if self.mining and self.mine_timer <= 0:
            self.mine_target()
            self.mine_timer = 0.12

    def ray(self, angle: float, pitch: float = 0.0):
        px, py, pz = self.player["x"], self.player["y"] + 1.6, self.player["z"]
        dx, dz = math.cos(angle), math.sin(angle)
        distance = 0.05
        previous = None
        while distance <= MAX_REACH:
            x, z = px + dx * distance, pz + dz * distance
            bx, bz = math.floor(x), math.floor(z)
            vertical = py + math.tan(pitch) * distance
            by = math.floor(vertical)
            current = (bx, by, bz)
            if self.in_bounds(bx, by, bz) and self.world[by][bx][bz]:
                return distance, x, z, current, previous
            previous = current
            distance += 0.05
        return MAX_REACH, px + dx * MAX_REACH, pz + dz * MAX_REACH, None, previous

    def screen_ray(self, screen_x: int, screen_y: int):
        """Cast a ray through an actual screen position, not just the crosshair."""
        relative_angle = (screen_x / WIDTH - 0.5) * FOV
        horizon = HEIGHT / 2 - self.player["pitch"] * HEIGHT
        elevation = math.atan((horizon - screen_y) / (HEIGHT * 0.82))
        return self.ray(self.player["yaw"] + relative_angle,
                        self.player["pitch"] + elevation)

    def move_horizontal(self, dx: float, dz: float) -> None:
        """Move freely, or climb a one-block step when there is headroom."""
        stepped = False
        next_x = self.player["x"] + dx
        next_z = self.player["z"] + dz
        if not self.collides(next_x, self.player["y"], self.player["z"]):
            self.player["x"] = next_x
        elif not stepped and not self.collides(next_x, self.player["y"] + BLOCK_HEIGHT, self.player["z"]):
            self.player["x"] = next_x
            self.player["y"] += BLOCK_HEIGHT
            self.player["vy"] = 0
            self.on_ground = True
            stepped = True

        next_x = self.player["x"]
        next_z = self.player["z"] + dz
        if not self.collides(next_x, self.player["y"], next_z):
            self.player["z"] = next_z
        elif not stepped and not self.collides(next_x, self.player["y"] + BLOCK_HEIGHT, next_z):
            self.player["z"] = next_z
            self.player["y"] += BLOCK_HEIGHT
            self.player["vy"] = 0
            self.on_ground = True

    def mine_target(self) -> None:
        hit = self.screen_ray(*self.cursor)
        if hit[3] is not None:
            x, y, z = hit[3]
            if y > 0:
                self.world[y][x][z] = 0

    def start_mining(self, event: tk.Event) -> None:
        self.cursor = (event.x, event.y)
        self.mining = True
        self.mine_timer = 0

    def stop_mining(self, _event: tk.Event) -> None:
        self.mining = False

    def place_block(self, _event: tk.Event) -> None:
        self.cursor = (_event.x, _event.y)
        hit = self.screen_ray(*self.cursor)
        target, previous = hit[3], hit[4]
        if target is None or previous is None:
            return
        x, y, z = previous
        if self.in_bounds(x, y, z) and self.world[y][x][z] == 0:
            self.world[y][x][z] = self.selected
            if self.collides(self.player["x"], self.player["y"], self.player["z"]):
                self.world[y][x][z] = 0

    def render(self) -> None:
        self.canvas.delete("all")
        # Looking up moves the horizon down the screen; looking down moves it up.
        horizon = HEIGHT / 2 - self.player["pitch"] * HEIGHT
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#87CEEB", outline="")
        self.canvas.create_rectangle(0, max(0, horizon), WIDTH, HEIGHT,
                                     fill="#5D9E45", outline="")

        scale = HEIGHT * 0.82
        for screen_x in range(0, WIDTH, RAY_STEP):
            relative = (screen_x / WIDTH - 0.5) * FOV
            ray_angle = self.player["yaw"] + relative
            ray_dx, ray_dz = math.cos(ray_angle), math.sin(ray_angle)
            visible_blocks = []
            seen_cells = set()
            distance = 0.05
            while distance <= MAX_REACH:
                bx = math.floor(self.player["x"] + ray_dx * distance)
                bz = math.floor(self.player["z"] + ray_dz * distance)
                cell = (bx, bz)
                if cell not in seen_cells and 0 <= bx < self.size_x and 0 <= bz < self.size_z:
                    seen_cells.add(cell)
                    for by in range(self.size_y):
                        block = self.world[by][bx][bz]
                        if block:
                            visible_blocks.append((distance, by, block))
                distance += 0.05

            # Paint far blocks first, allowing closer blocks to occlude them.
            for distance, by, block in sorted(visible_blocks, reverse=True):
                corrected = max(0.05, distance * math.cos(relative))
                top = horizon - ((by + BLOCK_HEIGHT) - (self.player["y"] + 1.6)) * scale / corrected
                bottom = horizon - (by - (self.player["y"] + 1.6)) * scale / corrected
                color = BLOCKS[block][1]
                shade = max(0.45, min(1.0, 1.0 - corrected / 18))
                color = self.shade(color, shade)
                self.canvas.create_rectangle(screen_x, max(0, top), screen_x + RAY_STEP,
                                             min(HEIGHT, max(top + 1, bottom)),
                                             fill=color, outline="")

        self.draw_hud()

    @staticmethod
    def shade(color: str, amount: float) -> str:
        channels = [int(color[i:i + 2], 16) for i in (1, 3, 5)]
        return "#" + "".join(f"{max(0, min(255, int(c * amount))):02X}" for c in channels)

    def draw_hud(self) -> None:
        cx, cy = WIDTH // 2, HEIGHT // 2
        self.canvas.create_line(cx - 8, cy, cx + 8, cy, fill="white", width=2)
        self.canvas.create_line(cx, cy - 8, cx, cy + 8, fill="white", width=2)
        self.canvas.create_rectangle(10, 10, 355, 72, fill="#18232B", outline="white")
        self.canvas.create_text(20, 27, anchor="w", fill="white",
                                text="WASD move  |  Mouse/arrows look  |  Space jump")
        self.canvas.create_text(20, 52, anchor="w", fill="white",
                                text=f"Hold left mine at cursor  |  Right place  |  Selected: {BLOCKS[self.selected][0]}")
        if not self.mouse_look:
            self.canvas.create_text(WIDTH // 2, HEIGHT - 24, fill="white",
                                    text="Press Escape released mouse look; move the mouse over the game to resume")

    def loop(self) -> None:
        now = self.root.tk.call("clock", "milliseconds") / 1000
        last = getattr(self, "last_time", now)
        self.last_time = now
        self.update(now - last)
        self.render()
        self.root.after(16, self.loop)


if __name__ == "__main__":
    VoxelWorld(tk.Tk()).root.mainloop()
