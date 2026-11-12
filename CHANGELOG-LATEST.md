### Changed

- Removing or restoring an item no longer refreshes EMI, JEI, or RRV on its own.
- Now requires Reliable Recipes 3.9.0 or newer.

### Fixed

- `remove_recipe` rules now remove the recipes that make the item.
- Tag-based rules no longer stop working on the server after removing or restoring an item without a reload.
