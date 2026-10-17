### Added

- Full JEI integration on every version and loader:
  - Hides recipes with an input or output that only holds removed items or fluids.
  - Dev mode: hover over an item in JEI's item list or bookmarks and press **Delete** to remove it (`enableJeiRemoval`, `showJeiToast`, `showJeiChatMessages`).
  - Hidden items update when the server sends new rules, without needing a reload.

### Changed

- Removing or restoring an item (dev mode, `/rremover remove` or `/rremover undo`) now updates EMI, JEI and RRV right away, without a reload. 
  - In EMI, recipes for a restored item come back on the next reload.

### Fixed

- Anvil recipes for removed enchantments no longer show up in JEI and EMI with a blank result (#96).
- Turning off `removeItemsFromCreativeTabs` no longer stops removed items from being hidden in recipe viewers.
- Info pages in JEI are now only hidden once every item on them is removed, matching EMI.
- Fixed TechReborn progress breaking on Fabric.