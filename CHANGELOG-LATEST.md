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