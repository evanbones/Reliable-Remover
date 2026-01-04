# Reliable Recipes

A powerful, developer-friendly utility designed for manipulating recipes and tags through simple JSON configuration, or an in-game interface. This mod allows and modpack creators to effortlessly add, remove, or modify recipes and tags using standard JSON files, without the need for complex scripts (looking at you, KubeJS!)

## Features

* **Multi-loader Support:** Built for Fabric (1.20.1+), Forge (1.20.1), and NeoForge (1.21+) using a unified codebase.
* **Dynamic Recipe Control:** Remove hardcoded recipes or inject custom ones at runtime.
* **Tag Manipulation:** Add or remove items/blocks from tags via config files.
* **No Scripting Required:** Uses standard Minecraft-style JSON syntax for ease of use.

## Mod Compatibility
* Automatically removes recipes and tags from items in [Item Obliterator](https://modrinth.com/mod/item-obliterator)'s blacklist.
* Press the delete key while hovering over a recipe output in EMI while in dev mode to automatically generate a removal JSON.

## Usage

The mod watches a specific folder in your instance (e.g., `./config/reliable_remover`) for JSON files. Upon server startup or data reload, it injects these changes into the internal registries.


### JSON Structure

The file contains two main sections: `recipe_modifications` for recipes and `tag_modifications` for item tags.

```json
{
  "recipe_modifications": [ ... ],
  "tag_modifications": [ ... ]
}

```

---

## 1. Filtering Recipes

The `filter` object determines which recipes are affected.
A recipe must match all provided fields to be selected.

### Basic Filters

You can filter by simple strings, or use **arrays** to match multiple values (acting as an OR condition).

| Field | Description | Example |
| --- | --- | --- |
| `output` | The registry name of the item produced. | `"minecraft:stone_pickaxe"` |
| `id` | The specific ID of a recipe. | `"minecraft:black_bed_from_white_bed"` |
| `mod` | The mod ID that owns the recipe. | `"farmersdelight"` or `["create", "mekanism"]` |
| `type` | The recipe type. | `"minecraft:smoking"` |
| `input` | Matches if the recipe contains this ingredient. | `"minecraft:stick"` or `"#minecraft:logs"` |

### Advanced Filters (Regex & Logic)

* **Regex:** Wrap strings in `/` to use Regular Expressions.
  *Example:* `"/minecraft:.*_log/"` matches all vanilla logs.
* **Logic:** Use `not`, `or`, and `and` for complex conditions.

**Example:** Remove all recipes that output Gold items, *except* those from Minecraft.

```json
{
  "action": "remove",
  "filter": {
    "output": "/.*gold.*/",
    "not": {
      "mod": "minecraft"
    }
  }
}

```

---

## 2. Recipe Actions

### Action: `remove`

Completely removes the matching recipes from the game.
You can use arrays to remove large batches of recipes at once.

**Example:** Remove specific recipes by ID.

```json
{
  "action": "remove",
  "filter": {
    "id": [
      "minecraft:wooden_pickaxe",
      "minecraft:wooden_hoe"
    ]
  }
}

```

### Action: `replace_input`

Scans ingredients and replaces a target with a new one.

* **`target`**: The item ID (e.g., `minecraft:stick`) or tag (e.g., `#minecraft:logs`) to find.
* **`replacement`**: The item or tag to use instead.
* **Single Item:** `"minecraft:bamboo"`
* **Compound (Array):** `["minecraft:bamboo", "minecraft:stick"]` (Accepts either item).



**Example:** Replace Sticks with Bamboo OR Sticks.

```json
{
  "action": "replace_input",
  "target": "minecraft:stick",
  "replacement": ["minecraft:bamboo", "minecraft:stick"],
  "filter": {
    "mod": "minecraft"
  }
}

```

### Action: `replace_output`

Changes the result of the matching recipes.

* **`replacement`**: The new item ID for the output.

**Example:** Make the Cake recipe craft a Golden Apple instead.

```json
{
  "action": "replace_output",
  "replacement": "minecraft:golden_apple",
  "filter": {
    "id": "minecraft:cake"
  }
}

```

---

## 3. Tag Modifications

The `tag_modifications` section allows you to strip tags from items. This is useful for cleaning up recipe viewers (JEI/EMI) or removing unobtainable items from tag groups.

### Action: `remove_all_tags`

Removes **all** tags from the specified items. This effectively orphans the item from tag-based recipe lookups.

* **`items`**: A single item ID string or an array of item IDs.

**Example:** Remove all tags from specific items.

```json
{
  "action": "remove_all_tags",
  "items": [
    "minecraft:stick",
    "minecraft:cake"
  ]
}

```

### Action: `remove_from_tag`

Removes items from a **specific** tag, while leaving them in other tags.

* **`tag`**: The tag ID to modify (e.g., `minecraft:planks`).
* **`items`**: The item(s) to remove from that tag.

**Example:** Remove Oak Planks from the generic planks tag.

```json
{
  "action": "remove_from_tag",
  "tag": "minecraft:planks",
  "items": "minecraft:oak_planks"
}

```

### Action: `clear_tag`

The `clear_tag` action allows you to completely empty a tag of all its associated items.
This is useful for clearing out mod-added tags that you want to disable entirely or rebuild from scratch.

* **`tags`**: A single tag ID string or an array of tag IDs to be cleared.

**Example:** Clear all items from specific modded tags.

```json
{
  "action": "clear_tag",
  "tags": [
    "createaddition:plant_foods",
    "caverns_and_chasms:experience_boost_items",
    "curios:artifact"
  ]
}

```

---

### License

This project is licensed under the **MIT License**.

---

### Contributing

Contributions are welcome! If you find a bug or have a feature request, please open an issue or submit a pull request.
