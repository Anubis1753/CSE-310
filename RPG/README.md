# Tiny Terminal RPG

An extremely simple C++ text-based RPG. The player can create or load a
character, view character information, and explore four locations.

## Build and run

From the `RPG` folder, compile with:

```powershell
g++ -std=c++17 -Wall -Wextra -pedantic main.cpp -o rpg.exe
```

Then run it with:

```powershell
.\rpg.exe
```

The game saves each character as a separate file in the `characters` folder
inside `RPG`. Choose `Load character` on a later run to select which saved
character to load.

## Character flow

1. Choose `Load character` or `Make new character`.
2. New characters enter a name and choose Knight, Mage, Hunter, Preist, or
   Swordsman.
3. Choose a saved character from the load list.
4. Choose `Character info` to view name, health, class, and level.
5. Choose `Inventory` inside Character Info to view items collected.
Select any item to see its description and gear rating, then equip it when
   appropriate.
   Use `-1. Unequip an item` to remove gear even when the inventory is empty.
6. Choose `Explore` to visit the available locations.
7. Choose `Delete character` from the start screen to remove a saved character
   after confirming the deletion.

## Exploration

- The Village contains the Merchant and Village Square.
- The Whispering Woods lead into the Deep Forest, Ancient Grove, and Forest
  Heart.
- Every area has a `Find a creature` option that starts a turn-based encounter.
- Enemies are weaker in outer areas and become stronger in deeper forest areas.
- Combat options are `Attack`, `Defend`, and `Run`. Running returns to the main
  Explore menu.
- Characters and enemies have Strength, Agility, Constitution, and Spirit.
- Maximum health is Constitution multiplied by 10.
- Defeating an enemy awards experience. Level 1 requires 10 experience for
  level 2, and each later level requirement doubles.
- Enemy experience starts at 5 per enemy level. Enemies above the player's
  level provide a +5 experience bonus per level difference; lower-level
  enemies reduce the reward by 2 per level difference, with a minimum reward
  of 1.
- New characters randomly receive values from 1–10, with each class favoring a
  preferred attribute: Knight/Constitution, Hunter/Agility, Mage/Spirit,
  Swordsman/Strength, and Preist/Spirit.
- Character attributes grow with level, and health is restored to full after
  each encounter.
- Each level grants 2 points to the class's preferred attribute and 2 free
  stat points. Free points can be assigned to Strength, Agility, Constitution,
  or Spirit from the Character Info screen.
- Knight, Hunter, and Swordsman attacks use Strength. Mage and Preist attacks
  use Spirit.
- Damage uses `1.5 × attack attribute - enemy Constitution - enemy Armor`,
  with a minimum of 1 for player attacks. Enemy attacks can be reduced to 0
  by Constitution, armor, or defending. Equipped gear ratings contribute to
  Armor.
- The merchant can add Health Potions, Iron Swords, and Leather Armor to the
  character's inventory.
- Exploring the forest can add a Forest Map and Forest Crystal to the inventory.

## Equipment

The inventory displays currently equipped items and lets you equip matching
items from the inventory. Each character has slots for one helmet, chestplate,
bracers, leggings, boots, and necklace; two bracelets; and four rings.

New characters begin with Starter Cloth Chestplate, Starter Cloth Pants, and
Starter Cloth Boots equipped. Starter gear has a gear rating of 2. The
merchant's Basic gear has a gear rating of 3, which is 1.5 times better.

## Current locations

1. The Village
2. The Whispering Woods
3. The Old Ruins
4. The Moonlit Lake
