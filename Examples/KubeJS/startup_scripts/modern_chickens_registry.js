// Modern Chickens - chicken registration from KubeJS startup scripts.
// Copy to: kubejs/startup_scripts/
//
// Tested against KubeJS 2001.6.5 on Minecraft 1.20.1 / Forge.
//
// The event fires once during mod setup, after every item in the game exists
// and after config/TOML chickens have been loaded. It does NOT fire again on
// `/kubejs reload_startup_scripts`, so a full game restart is required to see
// changes to this file.

ChickensEvents.registry(event => {

    // ==============================================
    // INSPECTING WHAT ALREADY EXISTS
    // ==============================================
    // Useful to keep a script quiet when a pack does not ship a given chicken.
    // console.info(`${event.getNames().size()} chickens are registered`)
    // if (event.exists('ironChicken')) { /* ... */ }

    // ==============================================
    // DOUSABLE CHICKEN
    // ==============================================
    // Chickens must opt in before the Avian Dousing Machine accepts them as an
    // input. Pair this with a recipe in server_scripts/.
    event.create('terracotta_chicken')
        .displayName('Terracotta Chicken')
        .layItem('minecraft:terracotta')
        .parents('dirtChicken', 'dirtChicken')          // Shorthand for parent1 + parent2
        .primaryColor('#985E43')
        .secondaryColor('#6D4534')
        .allowDousing(true)                             // Valid dousing input
        .liquidDousingCost(4000)                        // 4 buckets when doused with a fluid

    // ==============================================
    // TEACHING AN ITEM TO A CHICKEN
    // ==============================================
    // The trigger accepts an item id, an Item or an ItemStack.
    // event.teach('minecraft:paper', 'IronChicken')

    // ==============================================
    // OVERRIDING AN EXISTING CHICKEN
    // ==============================================
    // event.modify('IronChicken').spawnWeight(4).layItem('minecraft:iron_nugget')

    // ==============================================
    // RETUNING A FLUID CHICKEN
    // ==============================================
    // `fluid` targets the chicken that lays the liquid egg for that fluid, so
    // the fluid must already have a chicken. Parents cannot be descendants of
    // the chicken you are retuning; when they are, Modern Chickens logs
    // "has unusable parents" and clears the breeding data instead.
    // event.fluid('minecraft:lava').parents('RedChicken', 'YellowChicken')

    // ==============================================
    // RETUNING A CHEMICAL CHICKEN
    // ==============================================
    // Same idea for Mekanism gases and chemicals. Modern Chickens does not ship
    // chickens that lay chemical or gas eggs, so this only resolves when your
    // pack adds one (through custom_chickens.toml or `event.create` above).
    // Unknown ids are skipped with a "targets an unknown resource" warning.
    // event.chemical('mekanism:polonium').parents('UraniumChicken', 'LavaChicken')

    // ==============================================
    // EGG TUNING
    // ==============================================
    // event.modifyEgg('minecraft:lava').volume(500).eggColor(0xFF3300).hazards('hot', 'toxic')

    // ==============================================
    // CUSTOM TEXTURE
    // ==============================================
    // Drop the PNG in kubejs/assets/chickens/textures/entity/ and reference it
    // here. 64x32, standard Minecraft chicken UV mapping.
    // event.create('my_chicken')
    //     .layItem('minecraft:diamond')
    //     .generatedTexture(false)
    //     .texturePath('chickens:textures/entity/my_chicken.png')
})
