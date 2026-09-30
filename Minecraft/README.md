# Tiny 3D Minecraft

A barebones first-person 3D voxel sandbox made with Python's built-in Tkinter library.
It uses a real X/Y/Z block world and a small software ray caster, so no extra game
libraries are required. The player is two blocks tall and terrain blocks are one
block tall.

## Run it

From the `CSE 310` folder:

```powershell
python Minecraft/minecraft.py
```

## Controls

- `W`/`S`: move forward/back
- `A`/`D`: strafe left/right
- Mouse movement or left/right arrows: look/turn (mouse up looks up)
- `Space`: jump approximately one block high; one-block ledges can be climbed
- Hold left click: mine the block under the mouse cursor
- Right click: place the selected block
- `1` through `5`: select grass, dirt, stone, wood, or leaves
- `R`: generate a new world
- `Esc`: release mouse look
