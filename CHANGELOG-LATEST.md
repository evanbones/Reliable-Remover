### Added

- `replace_with` can now modify the replacement item's components (1.21+) or NBT (1.20.1), either inline (e.g. `"minecraft:diamond_sword[damage=100]"`) or through the new `replace_components` field.
- `replace_with` now works with effects: a `remove_effect` rule can swap the removed effect for another one, keeping its duration and amplifier.

### Changed

- Removed effects now have their related effect tooltips removed as well.