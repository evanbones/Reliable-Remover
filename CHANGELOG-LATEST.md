### Added

- Status effects are now properly blocked from applying when removed.
- Added support for right-click interaction removal on in-world blocks (such as anvils, beds, crafting tables, chests,
  levers, etc.) via `remove_interactions`.
- Added dedicated `blocks`, `fluids`, and `effects` options in rule definitions.

### Fixed

- Fixed crash when removing/saving items in the blacklist config screen on the main menu.
- Fixed rare startup crash.