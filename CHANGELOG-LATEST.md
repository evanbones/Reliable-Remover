### Added

- Ported 1.20.1 to stonecutter (Fabric and Forge).
- The `nbt`/`components` filter now supports vanilla item component syntax on 1.21+ (the same syntax as commands), e.g. `[damage=0]` or `[custom_data~{key:value}]`.

### Changed

- Now requires Reliable Recipes 3.3.0 or newer.
- SNBT in the `nbt` filter on 1.21+ now logs an error instead of silently failing.

### Fixed

- Fixed a crash with Clutter No More on 26.x when creating or loading a world.
- Fixed a crash with Clutter No More when its shape maps are disabled.
- Fixed a crash on 1.21.1 when a rule blocked right-clicking with an item.
