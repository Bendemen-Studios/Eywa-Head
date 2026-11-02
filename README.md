# Eywa-Head

Paper compatibility addon for HeadDrop × AxGraves.

## What it does
- Tracks a player death and briefly associates it with a player-head item spawned near the death location.
- Matches the dropped head to the dead player using the skull owner UUID.
- Adds a clone of the original HeadDrop ItemStack to the matching AxGraves grave inventory, preserving name, lore, persistent data and other item metadata.
- Removes the separate dropped entity only after the item is successfully inserted. If the grave inventory cannot accept it, the original entity is left alone.

AxGraves is accessed through GraveSpawnEvent using reflection, so AxGraves classes are not bundled.

## Build
Requires Java 21 and Maven. Run: `mvn clean package`
The JAR is written to `target/Eywa-Head-1.0.0.jar`.

## Install
1. Keep HeadDrop and AxGraves installed and enabled.
2. Put the built JAR in `plugins/`.
3. Fully restart Paper.
4. Test with a non-staff test player and verify the original HeadDrop head appears in the AxGraves grave.

## Compatibility notes
- HeadDrop must set the skull owner to the deceased player's UUID for matching.
- API shapes were checked against AxGraves 1.32.2 source; runtime testing against your exact versions is still required.
- If another plugin removes the item before Bukkit fires ItemSpawnEvent, this addon cannot capture it. Check the console logs during a test death.
