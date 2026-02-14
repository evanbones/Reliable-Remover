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