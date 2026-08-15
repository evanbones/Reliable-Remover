# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.12.0] - 2026-08-14

### Changed

- Updated API.

## [2.11.3] - 2026-08-04

### Fixed

- Fixed certain `remove_interactions` rules not working.

## [2.11.2] - 2026-08-02

### Fixed

- Fixed EMI info entries covering several items being removed entirely when only some of those
  items were removed.
- Fixed errors while filtering EMI.
- Fixed a race between tag rule expansion and EMI reloading.

## [2.11.1] - 2026-07-30

### Fixed

- Fixed more issues with `remove_interactions`.

## [2.11.0] - 2026-07-30

### Changed

- `remove_effect` and `remove_potion` now function separately.
- Vastly improved `remove_enchantment` system.

### Fixed

- Fixed issues with `remove_interactions` in certain contexts.

## [2.10.9] - 2026-07-29

### Changed

- Removal rules now support arrays in actions.

## [2.10.8] - 2026-07-27

### Fixed

- Fixed certain recipes being removed from EMI entirely when the recipe's placeholder preview item
  was hidden.
- Fixed issues with configs not saving while actively open.

## [2.10.7] - 2026-07-26

### Fixed

- Fixed issues with EMI's armor trim recipes.

## [2.10.6] - 2026-07-26

### Fixed

- Fixed issues with JEED.

## [2.10.5] - 2026-07-24

### Changed

- Updated Reliable Recipes version.

## [2.10.4] - 2026-07-24

### Fixed

- Fixed Clutter No More integration.

## [2.10.3] - 2026-07-22

### Fixed

- Fixed server crash.

## [2.10.2] - 2026-07-21

### Fixed

- Fixed `remove_effect` not working with `remove_creative`.
- Improved JEED integration.

## [2.10.1] - 2026-07-21

### Added

- Added `remove_effect` action (separate from `remove_potion`) to specifically block status effect applications.

## [2.10.0] - 2026-07-21

### Added

- Status effects are now properly blocked from applying when removed.
- Added support for right-click interaction removal on in-world blocks (such as anvils, beds, crafting tables, chests,
  levers, etc.) via `remove_interactions`.
- Added dedicated `blocks`, `fluids`, and `effects` options in rule definitions.

### Fixed

- Fixed crash when removing/saving items in the blacklist config screen on the main menu.
- Fixed rare startup crash.

## [2.9.5] - 2026-07-19

### Fixed

- Fixed issues with fluid/effect removal.

## [2.9.4] - 2026-07-17

### Fixed

- Made `pattern` parsing more lenient.
- Fixed issues with chest loot specific removal.

## [2.9.3] - 2026-07-13

### Fixed

- Fixed tag-based filtering not removing items properly.

## [2.9.2] - 2026-07-11

### Fixed

- Fixed issues with invalid IDs not properly being logged.

## [2.9.1] - 2026-07-08

### Fixed

- Fixed incompatibility with Crafttweaker.

## [2.9.0] - 2026-07-02

### Changed

- Switched from Cloth Config to YACLv3.
    - Existing configs should still work fine.

### Fixed

- Improved performance with advancement-based filters.
- Fixed config on-disk changes being reset when saving via the in-game config.

## [2.8.6] - 2026-06-27

### Fixed

- Fixed issues with the remove from inventory config option.

## [2.8.5] - 2026-06-24

### Fixed

- Improvements to advancement-based filtering.

## [2.8.4] - 2026-06-22

### Fixed

- Fixed crash with certain mods that modify creative mode tabs.

## [2.8.3] - 2026-06-19

### Fixed

- Fixed items not being removed from Create Simulated tabs.

## [2.8.2] - 2026-06-19

### Fixed

- Improved loot table removal.

## [2.8.1] - 2026-06-17

### Fixed

- Fixed issues with fluid removal rules.

## [2.8.0] - 2026-06-11

### Added

- Effects and fluids can now be hidden using Reliable Remover.

## [2.7.0] - 2026-06-11

### Added

- Added the ability to remove items based on advancements that haven't been unlocked yet.
    - Syntax: ```"advancements": [
      "minecraft:nether/root",
      "minecraft:end/root"
      ]```
    - See the "advancement-rules" page on the wiki for more information.
- Added a config option to propagate Clutter No More removals to child shapes (default true).

## [2.6.0] - 2026-06-09

### Added

- Added optional Clutter No More integration for item removals.
    - Removed items will be removed from shape maps.

## [2.5.1] - 2026-06-03

### Fixed

- Fixed issues with certain items in tags still showing up in EMI.

## [2.5.0] - 2026-05-16

### Changed

- Update Reliable Recipes dependency.

### Fixed

- Improved `replace_with` to properly catch all item contexts.

## [2.4.0] - 2026-05-10

### Changed

- Reliable Recipes is **no longer bundled with Reliable Remover**. Instead, it's marked as a required dependency and
  will be automatically downloaded.

### Fixed

- Fixed incompatibility with latest EMI.

## [2.3.1] - 2026-05-10

### Fixed

- Fixed early loading crash.

## [2.3.0] - 2026-05-02

### Fixed

- Fixed removed potions still being brewable.

### Changed

- Update Reliable Recipes version.

## [2.2.4] - 2026-04-23

### Changed

- Update Reliable Recipes version.

## [2.2.3] - 2026-04-21

### Changed

- Update Reliable Recipes version.

## [2.2.2] - 2026-04-17

### Fixed

- Hopefully fixed issues with EMI on dedicated servers.

## [2.2.1] - 2026-04-14

### Changed

- Bump Reliable Recipes version.

## [2.2.0] - 2026-04-12

### Fixed

- Performance improvements.

## [2.1.0] - 2026-04-11

### Changed

- Massively improve Regex matching.
- Improved logging for invalid Regex rules.

## [2.0.3] - 2026-04-09

### Changed

- Bump Reliable Recipes version.

## [2.0.2] - 2026-04-08

### Changed

- Bump Reliable Recipes version.

## [2.0.1] - 2026-04-07

### Changed

- Improved removal of items affected by loot modifiers.

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