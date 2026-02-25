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
- [X] Custom Blocks
    - [X] Dynamic block hardness
    - [X] Drops/Required tool
    - [X] Custom sounds (using blocks that already have it)
- [ ] Web editor
    - [X] Upload/Download/Create files
    - [X] View/Edit files
    - [ ] Config editor
        - [X] View items
        - [X] Create items
        - [X] Manage items
        - [ ] Edit items
- [ ] Furniture
    - [ ] Furniture Entity
    - [ ] Interactable Furniture
- [ ] Crafts
  - [ ] Inventory/Crafting table
  - [ ] Furnace
- [X] Glyphs
- [ ] GUIs
- [ ] World generation
- [ ] Escape menu
- [ ] Language overrides
- [ ] Components
  - [X] Enchantment Glint Override
  - [X] Fire Resistant
  - [X] Food
  - [X] Max Damage/Durability
  - [X] Max Stack Size
  - [X] Rarity
  - [ ] Toottip Display (only hide components)
  - [X] Unbreakable
  - [X] Use Cooldown
  - [ ] Blocked Enchantments
- [ ] Bedrock compatibility
    - [ ] Items
        - [X] Regular items
        - [X] Tools
        - [X] Armor
        - [ ] Elytra/Trident
    - [X] Translations
    - [ ] Custom blocks
- [ ] Commands
  - [X] `/give`
  - [X] `/setblock`
  - [ ] `/fill`
  - [ ] `/summon` (for furniture)
