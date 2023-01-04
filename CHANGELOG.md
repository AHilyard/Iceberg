# Changelog

### 1.1.4
- Fixed a bug that caused resource-pack defined item models with custom scaling to render improperly with custom item renderer.
- Fixed a bug that caused custom item renderer 3D models to have incorrect rotations.

### 1.1.3
- Fixed regression causing missing method crashes with some mods.
- Fixed a crash that could occur when rendering custom tooltips.

### 1.1.2
- Fixed a bug with custom item renderer causing items rendered with alpha to be upside down and with incorrect lighting.

### 1.1.1
- Fixed a bug that could cause tooltip titles to be positioned incorrectly for explicitly-positioned tooltips.

### 1.1.0
- Added wildcard selector.
- Added selector negation.
- Added selector combination.
- Added detail model renderer to custom item renderer.
- Added vertex collector and checked buffer source helpers.
- Fixed a bug that could cause tooltip titles to be incorrectly identified.
- Added support for Minecraft 1.19.3.

### 1.0.49
- Added support for tooltips with non-text components placed above the title.

### 1.0.48
- Removed explicit Configured version requirement.

### 1.0.47
- Added support for Configured 2.0.0.

### 1.0.46
- Added support for Minecraft 1.19.1 and 1.19.2.
- Added support for latest version of Forge.
- Added support for Configured 1.5.5.

### 1.0.45
- Added support for Configured configuration menus to IcebergConfig.

### 1.0.44
- Fixed incompatibility with some mods causing a crash at startup.

### 1.0.43
- Fixed update checking not working for multiple dependent mods.

### 1.0.42
- Increased required Forge version to 40.1.6 to prevent a crash.

### 1.0.41
- Fixed crash in IcebergConfig system.
- Bumped required Forge version to 40.1.

### 1.0.40
- Fixed a bug in configuration system that caused mod configs to sometimes fail to register properly.

### 1.0.39
- Added new config system that improves upon Forge's with subconfig support, improved reload reliability, and reduced boilerplate.

### 1.0.38
- Added support for tooltip components that use tooltip component generation event.

### 1.0.37
- Reverted first change from 1.0.35, as it was causing conflicts with other mod's tooltips.

### 1.0.36
- Fixed a crash issue with modded tooltip components that are not properly added to tooltip component factory.

### 1.0.35
- Fixed an issue that could cause dependent client-side mods to crash when run on a dedicated server.
- Fixed various warnings in latest.log file.
- Fixed bug causing incorrect tooltip background alpha.

### 1.0.34
- Improved item color detection for mods that do not properly implement item name colors.

### 1.0.33
- Improved extended tooltip events to better support multiple simultaneous tooltip rendering.
- Fixed bug in Minecraft's text color handling to fully support alpha values.

### 1.0.32
- Fixed a crash bug caused by invalid color codes in item names.
- Improved NBT selector to recognize tags in lists.  ("&id=minecraft:sharpness" works for anything enchanted with Sharpness, for example)
- Bumped required Forge version.

### 1.0.31
- Fixed a rare issue that could prevent the game from loading.

### 1.0.30
- Added support for ItemEdit item name colors.
- Fixed tooltip rect calculation issues.
- Added constrained tooltip component gathering helper.

### 1.0.29
- Overhauled tooltip handling to support 1.18 tooltip components.
- First Forge 1.18 release.

### 1.0.28
- Added support for color-code specified item name colors.

### 1.0.27
- Fixed a bug with dynamic resource packs that prevented dynamic resources from properly overriding.
- Added minimum width constrain option for tooltip rendering.

### 1.0.26
- Added NBT selector.
- Added selector syntax validator.

### 1.0.25
- Added dynamic resource pack helper.
- First Fabric 1.18 release.

### 1.0.24
- Rewrote client-side item pickup event so Iceberg is no longer required on servers (for that event).

### 1.0.23
- Fixed an issue with tooltip events causing inaccurate tooltip bounding box for tooltips near the edge of the screen.

### 1.0.22
- Consolidated configuration item selector logic for dependent mods, so new selectors can be added without requiring mod updates.

### 1.0.21
- Fixed a crash bug caused by an incompatibility with Architectury.

### 1.0.20
- Added rendertick event, added color results for color event.

### 1.0.19
- Fixed a crash bug when rendering custom gradients.

### 1.0.18
- Network protocol rewrite.
- Fixed final warning.

### 1.0.17
- Fixed jar versioning for Mod Menu (Fabric).

### 1.0.16
- Fixed a crash bug due to missing fonts on tooltips.
- First Fabric 1.17 release.

### 1.0.15
- Added StringRecomposer helper.

### 1.0.14
- Readded a post-tooltip rendering event, since RenderTooltipEvent.PostText was removed from Forge.

### 1.0.13
- Fixed an access issue on the custom ItemRenderer class.

### 1.0.12
- Fixed a mod compatibility issue for mods that interacted with advancements. (Such as Clickable Advancements)
- First Forge 1.17 release.

### 1.0.11
- Fixed a rare null-pointer exception when calculating tooltips.

### 1.0.10
- Added extended tooltip rendering events to better facilitate tooltip customization.

### 1.0.9
- Added GuiHelper class with generic rendering helper methods.
- Added Color easing helper function to Easing class.

### 1.0.8
- Fixed a bug that was preventing auto-registered sounds from working in multiplayer.

### 1.0.7
- Added helper entity registry methods.

### 1.0.6
- Added renderer support for auto registration system.

### 1.0.5
- Added network protocol and new event for remote pre-item pickup events.

### 1.0.4
- Added new helper method to render item tooltips without vanilla positioning restrictions.

### 1.0.3
- Added helper functions to auto registration class for easy registration of entities and sounds.

### 1.0.2
- Added a simple automatic registration utility class.

### 1.0.1
- Added tooltip helper.

### 1.0.0
- Initial release.
