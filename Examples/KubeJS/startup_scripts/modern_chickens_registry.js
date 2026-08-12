// Modern Chickens - chicken registration from KubeJS startup scripts.
// Copy to: kubejs/startup_scripts/
//
// The event fires once during mod setup, after every item in the game exists
// and after config/TOML chickens have been loaded. It does NOT fire again on
// `/kubejs reload_startup_scripts`, so a full game restart is required to see
// changes to this file.

ChickensEvents.registry(event => {

    // ==============================================
    // DOUSABLE CHICKEN
    // ==============================================
    // Chickens must opt in before the Avian Dousing Machine accepts them as an
    // input. Pair this with a recipe in server_scripts/.
    event.create('terracotta_chicken')
        .displayName('Terracotta Chicken')
        .layItem('minecraft:terracotta')
        .parents('dirtChicken', 'dirtChicken')        // Shorthand for parent1 + parent2
        .primaryColor('#985E43')
        .secondaryColor('#6D4534')
        .allowDousing(true)                             // Valid dousing input
        .liquidDousingCost(4000)                        // 4 buckets when doused with a fluid

    // ==============================================
    // OVERRIDES AND AUTOMATIC LINEAGE
    // ==============================================
    // These are examples of the additional integration. Leave them commented
    // or adapt the names to your pack.
    // event.teach('minecraft:paper', 'IronChicken')
    // event.modify('IronChicken').spawnWeight(4).layItem('minecraft:iron_nugget')
    // event.fluid('minecraft:lava').parents('BlazeChicken', 'WaterChicken')
    // event.chemical('mekanism:polonium').parents('UraniumChicken', 'LavaChicken')
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
