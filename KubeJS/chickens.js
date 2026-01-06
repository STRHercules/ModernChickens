ChickensEvents.registry(event => {
    
    // ==============================================
    // DIRT CHICKEN
    // ==============================================
    // Basic tier 1 chicken that lays dirt blocks
    event.create('dirt_chicken')
        .displayName('Dirt Chicken')                    // Custom display name shown in-game
        .layItem('minecraft:dirt')                      // Item produced when chicken lays
        .dropItem('minecraft:dirt', 2)                  // Item dropped when chicken dies
        .tier(1)                                        // Breeding tier (1 = easiest)
        .spawnType('NORMAL')                            // Can spawn naturally in overworld
        .texturePath('chickenosto.png')                 // or 'textures/entity/chickenosto.png' or 'modid:textures/entity/..'
        .itemTexturePath('coalchicken.png')             // -> chickens:textures/item/chicken/coalchicken.png
        .primaryColor(0x8B4513)                         // Saddle brown - main body color
        .secondaryColor(0x654321)                       // Dark brown - accent color
        .layCoefficient(1.0);                           // Normal laying speed
    
    // ==============================================
    // GRASS BLOCK CHICKEN
    // ==============================================
    // Tier 2 chicken requiring breeding from dirt chickens
    event.create('grass_block_chicken')
        .displayName('Grass Block Chicken')
        .layItem('minecraft:grass_block')               // Lays grass blocks
        .dropItem('minecraft:grass_block')              
        .parent1('dirt_chicken')                        // Requires dirt chicken as parent 1
        .parent2('dirt_chicken')                        // Requires dirt chicken as parent 2
        .tier(2)                                        // Medium tier - less common breeding result
        .spawnType('NORMAL')                            // Can spawn in normal biomes
        .primaryColor(0x7CFC00)                         // Lawn green - grass color
        .secondaryColor(0x228B22)                       // Forest green - darker grass
        .layCoefficient(0.8);                           // Lays 20% slower than normal
    
    // ==============================================
    // CRAFTING TABLE CHICKEN
    // ==============================================
    // Advanced tier 3 chicken producing crafting tables
    event.create('crafting_table_chicken')
        .displayName('Crafting Table Chicken')
        .layItem('minecraft:crafting_table')            // Lays crafting tables
        .dropItem('minecraft:oak_planks', 4)            // Drops 4 oak planks on death
        .parent1('grass_block_chicken')                 // Requires grass block chicken
        .parent2('dirt_chicken')                        // And dirt chicken to breed
        .tier(3)                                        // Higher tier = rarer from breeding
        .spawnType('NONE')                              // Cannot spawn naturally
        .primaryColor('#C4A574')                        // Tan/wood color (hex string format)
        .secondaryColor('#8B4513')                      // Saddle brown accent
        .layCoefficient(0.5)                            // Lays 50% slower (valuable item)
        .generatedTexture(true);                        // Use auto-generated texture from colors
});

// ======================================================================
// MACHINE RECIPE EXAMPLES
// ======================================================================
// These examples show how to add KubeJS-driven recipes for Modern Chickens
// machines (dousing + converters). They are additive and do not remove the
// built-in defaults unless a custom recipe matches the same input/reagent.
ChickensMachineRecipes.register(event => {
    // --------------------------------------------------
    // AVIAN DOUSING MACHINE
    // --------------------------------------------------
    // Syntax:
    //   event.dousingFluid(inputChicken, outputChicken, fluidId, fluidAmount, energyCost)
    //   event.dousingChemical(inputChicken, outputChicken, chemicalId, chemicalAmount, energyCost)
    //
    // - inputChicken / outputChicken: "dirt_chicken" or "chickens:dirt_chicken" (or numeric id)
    // - fluidId / chemicalId: resource location (e.g., "minecraft:water", "mekanism:oxygen")
    // - amounts are in mB; energyCost is RF
    event.dousingFluid('smart_chicken', 'diamond_chicken', 'minecraft:water', 1000, 5000);
    event.dousingChemical('smart_chicken', 'uranium_chicken', 'mekanism:uranium', 1000, 100000);

    // --------------------------------------------------
    // AVIAN FLUID CONVERTER
    // --------------------------------------------------
    // Syntax:
    //   event.fluidConverter(inputFluidId, outputFluidId, outputAmount)
    //
    // - inputFluidId matches the liquid egg's fluid (i.e., the chicken you insert)
    // - outputFluidId is what that chicken produces in the converter
    // - outputAmount is in mB
    // Example: a lava chicken produces lava (not water).
    event.fluidConverter('minecraft:lava', 'minecraft:lava', 1000);

    // --------------------------------------------------
    // AVIAN CHEMICAL CONVERTER
    // --------------------------------------------------
    // Syntax:
    //   event.chemicalConverter(inputChemicalId, outputChemicalId, outputAmount)
    //
    // - inputChemicalId matches the chemical/gas egg's chemical id (the chicken you insert)
    // - outputChemicalId is what that chicken produces in the converter
    // - outputAmount is in mB
    // Example: an oxygen chicken produces oxygen.
    event.chemicalConverter('mekanism:oxygen', 'mekanism:oxygen', 1000);
});

/*
==============================================================================
FIELD DOCUMENTATION
============================================================================== 

event.create(id)
----------------
Creates a new chicken builder with the given ID.
Parameter: id (String) - Unique identifier for the chicken
  - Accepts: "dirt_chicken" or "chickens:dirt_chicken"
  - Must be unique across all registered chickens
  - Used in breeding system to reference parents
Returns: ChickenBuilder instance for method chaining

.displayName(name)
------------------
Sets the display name shown to players in-game.
Parameter: name (String) - The visible name
  - Example: "Dirt Chicken", "Legendary Diamond Fowl"
  - If omitted, auto-generates from ID (dirt_chicken → "Dirt Chicken")
  - Supports formatting codes with § symbol

.layItem(itemId) / .layItem(itemId, count)
-------------------------------------------
Sets what item the chicken produces when laying eggs.
Parameters:
  - itemId (String) - Resource location of item (e.g., "minecraft:dirt")
  - count (int) - Amount produced per lay (default: 1)
REQUIRED: Every chicken must have a layItem or it won't register
Examples:
  - .layItem('minecraft:diamond')           // 1 diamond per lay
  - .layItem('minecraft:iron_ingot', 3)     // 3 iron ingots per lay

.dropItem(itemId) / .dropItem(itemId, count)
---------------------------------------------
Sets what item drops when the chicken dies.
Parameters:
  - itemId (String) - Resource location of item
  - count (int) - Amount to drop (default: 1)
OPTIONAL: If omitted, chicken drops standard chicken loot
Examples:
  - .dropItem('minecraft:feather')          // 1 feather
  - .dropItem('minecraft:cooked_chicken', 2) // 2 cooked chicken

.parent1(name) / .parent2(name)
-------------------------------
Sets the parent chickens required for breeding.
Parameter: name (String) - Entity name of parent chicken
  - Must match the ID of another registered chicken
  - Both parent1 and parent2 needed for breeding to work
  - Can use same parent twice if only one type needed
  - If parents not set, chicken cannot be bred
Examples:
  - .parent1('dirt_chicken')                // First parent
  - .parent2('grass_block_chicken')         // Second parent
  - .parent1('book_chicken').parent2('book_chicken')  // Same parent twice

.tier(level)
------------
Sets the breeding difficulty tier.
Parameter: level (int) - Tier level
  - Range: 1-10 (1 = most common, 10 = rarest)
  - Affects breeding probability: higher tier = lower chance from parents
  - Tier 1: ~40% chance, Tier 2: ~25%, Tier 3: ~15%, Tier 5: ~5%
  - Default: 1 if not specified
Recommended tiers:
  - Tier 1: Base resource chickens (dirt, wood, stone)
  - Tier 2-3: Processed materials (iron, gold)
  - Tier 4-5: Rare materials (diamonds, netherite)
  - Tier 6+: Ultra-rare special chickens

.spawnType(type)
----------------
Sets where the chicken can spawn naturally.
Parameter: type (String) - Spawn location type
Valid values:
  - "NORMAL" - Overworld normal biomes (plains, forests, etc.)
  - "SNOW"   - Cold/snowy biomes (taiga, tundra, ice plains)
  - "HELL"   - Nether dimension
  - "END"    - End dimension
  - "NONE"   - Cannot spawn naturally (breeding only)
Default: "NONE" if not specified
Note: Natural spawning requires canSpawn to be enabled

.primaryColor(color) / .primaryColor(hexColor)
----------------------------------------------
Sets the main body color for generated textures.
Parameters:
  - color (int) - RGB color as hex integer (e.g., 0xFFFFFF)
  - hexColor (String) - Hex color as string (e.g., "#FF0000")
Only used when generatedTexture(true)
Examples:
  - .primaryColor(0xFF0000)                 // Red (integer format)
  - .primaryColor('#00FF00')                // Green (string format)
  - .primaryColor('0x0000FF')               // Blue (0x prefix)
Default: 0xFFFFFF (white) if not specified

.secondaryColor(color) / .secondaryColor(hexColor)
--------------------------------------------------
Sets the accent/detail color for generated textures.
Parameters: Same as primaryColor
Only used when generatedTexture(true)
Used for: Wings, tail, comb, wattle, and other details
Default: 0xFFFF00 (yellow) if not specified

.layCoefficient(coefficient)
----------------------------
Multiplier for laying speed - affects how often chicken lays.
Parameter: coefficient (double) - Speed multiplier
  - 1.0 = Normal speed (baseline laying rate)
  - <1.0 = Slower (e.g., 0.5 = half speed, 0.25 = quarter speed)
  - >1.0 = Faster (e.g., 2.0 = double speed, 3.0 = triple speed)
  - Range: 0.1 - 5.0 recommended (extreme values may cause issues)
Default: 1.0 if not specified
Use cases:
  - 0.2-0.5: Valuable items (diamonds, netherite)
  - 0.5-0.8: Uncommon items (gold, emeralds)
  - 1.0: Standard items (iron, copper)
  - 1.5-3.0: Common items (dirt, cobblestone)

.generatedTexture(generated)
----------------------------
Whether to auto-generate texture from colors or use custom file.
Parameter: generated (boolean)
  - true: Auto-generate using primaryColor and secondaryColor
  - false: Use custom texture file specified in texturePath
Default: true if not specified
Note: If true, primaryColor/secondaryColor are required

.texturePath(path)
------------------
Sets custom texture file location.
Parameter: path (String) - Resource location of texture PNG file
  - Format: "namespace:path/to/texture.png"
  - Example: "chickens:textures/entity/my_chicken.png"
  - File location: assets/chickens/textures/entity/my_chicken.png
Only used when generatedTexture(false)
Texture requirements:
  - Size: 64x32 pixels (standard Minecraft chicken texture)
  - Format: PNG with transparency support
  - Must follow Minecraft's chicken UV mapping

==============================================================================
SPAWN TYPE DETAILS
==============================================================================

NORMAL: 
  - Spawns in regular overworld biomes
  - Includes: plains, forests, meadows, rivers, etc.
  - Excludes: snowy, desert, nether, end biomes

SNOW:
  - Spawns in cold/snowy biomes
  - Includes: snowy plains, ice spikes, frozen rivers, taiga
  - Temperature-based biome detection

HELL:
  - Spawns in the Nether dimension only
  - Includes: all nether biomes (wastes, crimson forest, etc.)
  - Requires nether-safe chickens

END:
  - Spawns in the End dimension only
  - Includes: end highlands, end islands, void
  - Very rare spawn type

NONE:
  - Never spawns naturally in any biome
  - Must be obtained through: breeding, eggs, or creative mode
  - Recommended for high-tier or special chickens

==============================================================================
BREEDING TIER SYSTEM EXPLAINED
==============================================================================

How tiers affect breeding:
  - Parents can produce children of equal or higher tier
  - Higher tier = lower probability in breeding calculation
  - Two Tier 1 parents can produce Tier 1, 2, or 3 children
  - Tier determines rarity in breeding pool

Breeding probability formula:
  chance = (maxTier - childTier + 1) / totalPossibilities
  Where maxTier = highest tier among possible children

Example breeding chain:
  Dirt Chicken (T1) + Dirt Chicken (T1) → Grass Block Chicken (T2)
  Possible outcomes: Dirt (T1, high chance), Grass Block (T2, lower chance)
  
  Grass Block (T2) + Dirt (T1) → Crafting Table (T3)
  Possible outcomes: Dirt (T1), Grass Block (T2), Crafting Table (T3, rare)

Recommended tier progression:
  T1: Base resources (dirt, sand, gravel, wood)
  T2: Common ores (coal, copper, iron)
  T3: Uncommon ores (gold, redstone, lapis)
  T4: Rare materials (diamond, emerald)
  T5: Ultra-rare (netherite, dragon egg, star)

==============================================================================
COLOR SYSTEM
==============================================================================

Color formats accepted:
  1. Integer hex: 0xRRGGBB (e.g., 0xFF0000 for red)
  2. String hex with #: "#RRGGBB" (e.g., "#00FF00" for green)
  3. String hex with 0x: "0xRRGGBB" (e.g., "0x0000FF" for blue)

RGB breakdown:
  - RR = Red channel (00-FF)
  - GG = Green channel (00-FF)
  - BB = Blue channel (00-FF)

Common colors:
  - White: 0xFFFFFF
  - Black: 0x000000
  - Red: 0xFF0000
  - Green: 0x00FF00
  - Blue: 0x0000FF
  - Yellow: 0xFFFF00
  - Cyan: 0x00FFFF
  - Magenta: 0xFF00FF
  - Orange: 0xFFA500
  - Purple: 0x800080
  - Brown: 0x8B4513
  - Gray: 0x808080

Material-based colors:
  - Dirt: 0x8B4513 (brown)
  - Grass: 0x7CFC00 (lawn green)
  - Stone: 0x808080 (gray)
  - Iron: 0xD8D8D8 (light gray)
  - Gold: 0xFFD700 (gold)
  - Diamond: 0x00FFFF (cyan)
  - Emerald: 0x50C878 (emerald green)
  - Redstone: 0xFF0000 (red)
  - Lapis: 0x1E90FF (dodger blue)

==============================================================================
COMPLETE WORKFLOW EXAMPLE
==============================================================================

Step 1: Create base chickens (no parents)
  - Dirt Chicken (T1)
  - Sand Chicken (T1)
  - Wood Chicken (T1)

Step 2: Create first-generation hybrids
  - Mud Chicken (T2): dirt + sand parents
  - Plank Chicken (T2): wood + wood parents

Step 3: Create advanced chickens
  - Clay Chicken (T3): mud + sand parents
  - Crafting Table (T3): plank + wood parents

Step 4: Create ultra-rare chickens
  - Brick Chicken (T4): clay + clay parents
  - Diamond Chicken (T5): complex multi-parent chain

*/
