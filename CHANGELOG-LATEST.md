### Changed

- Removing or restoring an item no longer refreshes EMI, JEI or RRV on its own. With `reloadAfterRemoval` on, the
  `/reload` updates them; with it off, they update on the next `/reload` (#101).
- Now requires Reliable Recipes 3.9.0 or newer.

### Fixed

- `remove_recipe` rules now remove the recipes that make the item (#103).
- Tag-based rules no longer stop working on the server after removing or restoring an item without a reload.
