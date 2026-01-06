# Reliable Remover

A lightweight, developer-friendly utility designed for completely removing items, blocking interactions, or disabling attacks through simple JSON configuration. This mod allows modpack creators to effortlessly ban items or restrict their usage using standard JSON files, without the need for complex scripts.

## Features

* Built for Fabric (1.20.1+), Forge (1.20.1), and NeoForge (1.21.1+)

* Disable only specific actions, such as attacking or interacting (right-clicking) with an item.
* Remove items by ID, mod ID, regex patterns, or even NBT data.
* Configure global settings via Cloth Config to toggle messages, removal behavior, and more.
* Automatically hides items from:
    * Creative mode tabs
    * EMI/JEI
    * Player inventories
    * Villager trades
    * Loot tables

## Usage

The mod watches a specific folder in your instance for JSON files: `./config/reliable_remover/`.
You can place as many JSON files as you like in this folder or in subfolders.

### Global Configuration

Global settings can be configured via the **Cloth Config** screen in-game or by editing `config/reliable_remover.json`.

### JSON Structure

Each file can contain a single rule object or an array of rule objects.

```json
[
  {
    "action": "remove",
    "filter": { ... }
  },
  {
    "action": "remove_attacks",
    "filter": { ... }
  }
]

```

---

## 1. Filtering Items

The `filter` object determines which items are affected by the rule.

### Basic Filters

You can filter by simple strings or use sets of IDs.

| Field | Description | Example |
| --- | --- | --- |
| `items` | A list of specific item IDs to match. | `["minecraft:tnt", "minecraft:lava_bucket"]` |
| `mod` | The mod ID to target. Matches all items from that mod. | `"farmersdelight"` |
| `pattern` | A Regex pattern to match item IDs. | `"/.*_sword/"` |
| `nbt` | A Regex pattern to match NBT data strings. | `"{.*Enchantments.*}"` |

### Logic Filters

* **Logic:** Use `not` to invert conditions.

**Example:** Remove all items from a specific mod, *except* for one item.

```json
{
  "action": "remove",
  "filter": {
    "mod": "examplemod",
    "not": {
      "items": [
        "examplemod:safe_item"
      ]
    }
  }
}

```

---

## 2. Removal Actions

### Action: `remove`

The nuclear option. This completely scrubs the item from the game context based on your global config.
By default, this will:

1. Remove the item from Creative Tabs.
2. Remove the item from EMI/Recipe Viewers.
3. Delete the item if found in a player's inventory.
4. Delete the item if it is dropped in the world.

**Example:** Remove TNT and Bedrock.

```json
{
  "action": "remove",
  "filter": {
    "items": [
      "minecraft:tnt",
      "minecraft:bedrock"
    ]
  }
}

```

### Action: `remove_interactions`

Prevents the player from "using" the item (Right-Click). This is useful if you want an item to exist for crafting but not be usable (e.g., banning a specific wand or tool).

* **Effect**: Cancels `useItem` and `useOn` events.
* **Message**: Displays "Item interactions are disabled" to the player (configurable).

**Example:** Prevent players from using Lava Buckets.

```json
{
  "action": "remove_interactions",
  "filter": {
    "items": ["minecraft:lava_bucket"]
  }
}

```

### Action: `remove_attacks`

Prevents the player from attacking entities while holding the specified item.

* **Effect**: Cancels the attack event.
* **Message**: Displays "Attacking with this item is disabled" to the player (configurable).

**Example:** Disable attacking with any swords from the mod Simply Swords.

```json
{
  "action": "remove_attacks",
  "filter": {
    "pattern": "/simplyswords:.*_sword/"
  }
}

```

---

### License

This project is licensed under the **MIT License**.

---

### Contributing

Contributions are welcome! If you find a bug or have a feature request, please open an issue or submit a pull request.