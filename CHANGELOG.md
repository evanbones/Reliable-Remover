# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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