# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-04-06

### Added

- Added new separated rules for the "remove" action. You can now specify exactly where an item should be
  restricted:
    - `remove_drops`: Removes items when dropped in the world.
    - `remove_inventory`: Removes items from player inventories.
    - `remove_creative`: Hides items from creative tabs and EMI/JEI/REI.
    - `remove_equipment`: Removes items from mob hands and armor slots.
    - `remove_recipe`: Hides recipes that result in the item.
    - `remove_storage`: Removes items from chests, barrels, etc. when opened.
- Added new config options in Cloth Config to toggle these specific removal types globally.
- Expanded the API with specific check methods for each removal type.

### Changed

- Improved recipe filtering to apply across all recipe types more consistently.

### Fixed

- Fixed potential memory leaks in rule caching.
- Recipes with multiple outputs (e.g., Create crushing) are now only removed if **all** of their output items are
  restricted. If at least one output remains valid, the recipe remains accessible.

## [1.8.1] - 2026-04-04

### Fixed

- Fixed (Neo)Forge crashing on dedicated servers.

## [1.8.0] - 2026-04-03

### Added

- Added `replace_with` filter to swap recipe outputs before removing items.
    - See the docs for more details on this.
- Added a keybind for EMI deletion (default key is 'delete').

## [1.7.5] - 2026-03-25

### Fixed

- Fixed "War Pigs" advancement not triggering correctly (#48).

## [1.7.4] - 2026-03-16

### Fixed

- Fixed component matching aggressively caching IDs.

## [1.7.3] - 2026-03-15

### Fixed

- Fixed removed items still showing up in EMI tags.

## [1.7.2] - 2026-03-15

### Fixed

- Fixed regex matching to properly support components.

## [1.7.1] - 2026-03-15

### Added

- Added filtering for removals by registry and tag type (see docs for more information).
- Added tag whitelist (defaults to `c:hidden_from_recipe_viewers`).

### Fixed

- Fixed eager registry loading causing issues.

## [1.7.0] - 2026-03-14

### Changed

- Items given with `/give` will now be removed instantly and inform the player.
- Hardened blacklisted item detection.

### Fixed

- Fixed creative mode hot reloading.

## [1.6.0] - 2026-03-09

### Changed

- Bumped Reliable Recipes version.

### Fixed

- `remove_potion` rules now properly support tags.

## [1.5.1] - 2026-02-27

### Changed

- `remove_interaction` will now prevent players from eating foods.
- Moved blacklist in Cloth Config to its own category.

## [1.5.0] - 2026-02-27

### Added

- Added proper developer API.
- Removed items are now automatically removed from inventories when opened.

### Changed

- Backend changes and reworks.

## [1.4.1] - 2026-02-25

### Fixed

- `remove_enchantment` now removes enchantments from items instead of removing the item itself.

## [1.4.0] - 2026-02-23

### Added

- Added support for Sawmill recipe removal.
- Generated removals using EMI now show up in the config JSON.
    - This means they can be directly modified from the Cloth Config screen, or edited in `reliable_remover.json`.
    - Existing generated removals will continue to work as expected.
- Added `remove_info` action to remove item info from JEI.
- Removed enchantments are now hidden from Anvil recipes.
- Removed items will now automatically be removed from JEI info tabs.

### Fixed

- Fixed issues with removal by tag.

## [1.3.1] - 2026-02-19

### Fixed

- Fixed dev mode using the old command to remove items.

## [1.3.0] - 2026-02-19

### Added

- Removed items are now removed from mob equipment.
- Removed items are now removed from JEI/EMI info.
- Removed items are now removed from grindstone and anvil repairing recipes.
- Added filter for mob equipment specific removal.

## [1.2.4] - 2026-02-18

### Changed

- Changed command prefix from `/reliable_remover` to `/rremover` (you're welcome, Emma!)

## [1.2.3] - 2026-02-18

### Fixed

- Fixed possible concurrency issues with EMI and rule loading.

## [1.2.2] - 2026-02-18

### Added

- Added support for `nbt` arrays.

### Fixed

- Fixed `nbt` filters requiring `items` array or similar.
- Fixed startup crash with mods that early register potions and enchantments.

## [1.2.1] - 2026-02-14

### Fixed

- Fixed validation error in `remove_potion` and `remove_enchantment` actions.

## [1.2.0] - 2026-02-13

### Changed

- Removed logic for parsing legacy `filter` blocks.
    - IMPORTANT: This can affect legacy removal rules! Please remove any `filter` blocks from your JSONs as they are now
      redundant.

### Added

- Added config options for dev behaviour using EMI, like deleting items in-game by pressing the delete key.
- Added support for item tags in actions, using a `#` prefix or a `tags` block.
- Added `remove_trade` action to remove items only from Villager trades.
- Added `remove_loot` action to remove items only from loot tables.
- Added `remove_hand_swing` action to disable hand swinging.
- Added message when trying to attack or swing with items targeted by `remove_attack` or `remove_hand_swing` actions.

## [1.1.5] - 2026-02-12

### Added

- Removed items will now be automatically removed from containers when opened.
- Improved rule parsing performance.

## [1.1.4] - 2026-02-10

### Fixed

- Fixed crash with EMI loot.

## [1.1.3i] - 2026-02-09

### Fixed

- Bump Reliable Recipes version.

## [1.1.3h] - 2026-02-09

### Fixed

- Fixed issue with empty tags in recipe viewers.

## [1.1.3] - 2026-02-09

### Fixed

- Fixed incompatibility with Immersive Engineering.
- Fix log warning about missing EMI loot.

## [1.1.2] - 2026-02-04

### Changed

- Reliable Recipes is now bundled by default in the mod.

## [1.1.1] - 2026-01-30

### Added

- `/reliable_remover` command for dumping item ids
    - Supports `hotbar`, `inventory`, and `hand`

### Fixed

- Hopefully fixed issue with certain mods that could lead to loot tables being removed.

## [1.1.0] - 2026-01-30

### Added

- Support for arrays in Regex pattern matching.
- Much more lenient formatting for removal jsons.
    - Arrays and single strings should generally be interchangeable in actions and filters.
- Added dedicated Potion and Enchantment removal actions.
- Enchantment removals now also remove enchantments from Enchanting Tables.

### Fixed

- Fixed Regex patterns not re-applying on /reload.

### Changed

- Removed `filter` keyword for simplicity.
    - Existing patterns will still work, but it's good practice to remove the `filter` block.

## [1.0.13] - 2026-01-21

### Added

- Improved loot table removal to be more flexible with modded loot tables.

### Fixed

- Fix removed loot tables still showing up in EMI/JEI.

## [1.0.12] - 2026-01-12

### Fixed

- Fix "not" rule handling with dimension filtering.