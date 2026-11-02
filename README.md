# Eywa-Head

HeadDrop × AxGraves compatibility addon for Paper.

> Status: initial integration scaffold. The inspected AxGraves 1.32.2 source exposes a spawned grave and its inventory via `GraveSpawnEvent`. The addon must capture HeadDrop's **original** dropped player-head ItemStack before it is removed, then insert that exact ItemStack into the matching grave.

## Compatibility

- Paper servers
- AxGraves (API inspected: 1.32.2 source)
- HeadDrop by fr.gab (the exact artifact/API still needs validation against the installed release)

## Important

Do not replace the HeadDrop item with a newly-created player skull. HeadDrop's item may contain a custom display name, lore, PersistentDataContainer values, enchantments, and other metadata. Preserve the original ItemStack by cloning it.

This repository currently contains project documentation only. A compiled plugin is not published yet.
