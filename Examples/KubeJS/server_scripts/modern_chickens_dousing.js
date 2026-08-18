// Modern Chickens - Avian Dousing Machine recipes.
// Copy to: kubejs/server_scripts/
//
// Tested against KubeJS 2001.6.5 on Minecraft 1.20.1 / Forge.
//
// The machine turns one chicken into another by consuming a reagent (an item,
// a fluid or a Mekanism chemical) plus energy. Modern Chickens registers a
// KubeJS recipe schema for `chickens:avian_dousing`, so the builder syntax is
// available out of the box. Recipes reload with /reload.
//
// Chicken names are registry names (IronChicken, obsidianChicken, ...), matched
// case-insensitively. They are NOT item IDs.

// KubeJS can also extend the seed tag used by the rooster nest:
// ServerEvents.tags('item', event => event.add('chickens:nest_seeds', 'example:seed'))

ServerEvents.recipes(event => {

  // ==============================================
  // ITEM REAGENT
  // ==============================================
  // avian_dousing(result, input, reagent)
  event.recipes.chickens.avian_dousing(
    'GoldChicken',                                    // result chicken
    'IronChicken',                                    // input chicken
    { type: 'item', id: 'minecraft:gold_ingot', amount: 4 }
  ).energy(10000).id('example:iron_to_gold_chicken')

  // ==============================================
  // MINIMAL RECIPE
  // ==============================================
  // reagent amount defaults to 1, energy defaults to 10000, and the id is
  // generated from the result chicken when you omit .id()
  event.recipes.chickens.avian_dousing(
    'GlowstoneChicken',
    'RedstoneChicken',
    { type: 'item', id: 'minecraft:glowstone_dust' }
  )

  // ==============================================
  // ENERGY AS A FOURTH ARGUMENT
  // ==============================================
  // Identical to chaining .energy(20000)
  event.recipes.chickens.avian_dousing(
    'LavaChicken',
    'MagmaChicken',
    { type: 'fluid', id: 'minecraft:lava', amount: 1000 },   // fluids use mB
    20000
  ).id('example:magma_to_lava_chicken')

  // ==============================================
  // MEKANISM CHEMICAL REAGENT
  // ==============================================
  // Needs Mekanism at runtime. The recipe still loads without it, it just never
  // resolves, so guard it if your pack ships both ways.
  if (Platform.isLoaded('mekanism')) {
    event.recipes.chickens.avian_dousing(
      'FlintChicken',
      'CoalChicken',
      { type: 'chemical', id: 'mekanism:sulfuric_acid', amount: 500 }
    ).energy(15000).id('example:coal_to_flint_chicken')
  }

  // ==============================================
  // GENERATED FROM A TABLE
  // ==============================================
  // Handy for progression chains without repeating yourself.
  const chain = [
    // [input,          result,           reagent item,          amount, RF]
    ['SandChicken',     'ClayChicken',    'minecraft:clay_ball', 4,      8000],
    ['ClayChicken',     'QuartzChicken',  'minecraft:quartz',    4,      12000],
    ['QuartzChicken',   'DiamondChicken', 'minecraft:diamond',   2,      40000]
  ]

  chain.forEach(([input, result, item, amount, energy]) => {
    event.recipes.chickens.avian_dousing(
      result,
      input,
      { type: 'item', id: item, amount: amount }
    ).energy(energy).id(`example:dousing/${input.toLowerCase()}_to_${result.toLowerCase()}`)
  })
})
