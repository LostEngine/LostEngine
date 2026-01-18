## Building from source:
Make sure you have [git](https://git-scm.com/install/) and Java 21+ installed
```
git clone https://github.com/LostEngine/LostEngine.git
cd LostEngine
# Linux/Mac
./gradlew shadowJar
# Windows
gradlew.bat shadowJar
```
The build will be located in `build/libs/`
> [!TIP]
> You can use (Linux/Mac) `./gradlew runServer` (Windows) `gradlew.bat runServer` to start a PaperMC server with the plugin automatically built and installed (use `runFolia` for Folia)

## TODO:
- [ ] Custom Items
    - [X] Regular Items
    - [X] Tools
    - [X] Armors
    - [X] Elytra
    - [X] Trident
    - [ ] Bows/Crossbows
- [ ] Custom Blocks
    - [X] Dynamic block hardness
    - [X] Drops/Required tool
    - [ ] Custom sounds
    - [ ] Transparent blocks
    - [ ] Lights/Redstone
- [ ] Web editor
    - [X] Upload/Download/Create files
    - [X] View/Edit files
    - [ ] Config editor
        - [X] View items/materials
        - [ ] Create/manage items/materials
- [ ] Furniture
    - [ ] Furniture Entity
    - [ ] Interactable Furniture
- [ ] Crafts
  - [ ] Inventory/Crafting table
  - [ ] Furnace
- [ ] Glyphs (can copy https://github.com/LostEngine/Glyphs)
- [ ] GUIs
- [ ] World generation
- [ ] Escape menu
- [ ] Bedrock compatibility
    - [ ] Items
        - [X] Regular items
        - [X] Tools
        - [ ] Armor/Elytra/Trident
    - [ ] Translations (generated but not working correctly with geyser)
    - [ ] Custom blocks
- [ ] Commands
  - [X] `/give`
  - [X] `/setblock`
  - [ ] `/fill`
  - [ ] `/summon` (for furniture)
