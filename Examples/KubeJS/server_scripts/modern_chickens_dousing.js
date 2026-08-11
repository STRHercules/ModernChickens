// Modern Chickens exposes a normal server recipe type; no Java/KubeJS bridge is required.
ServerEvents.recipes(event => {
  event.custom({
    type: 'chickens:avian_dousing',
    input: 'IronChicken',
    result: 'GoldChicken',
    reagent: {
      type: 'item',
      id: 'minecraft:gold_ingot',
      amount: 4
    },
    energy: 10000
  }).id('example:iron_to_gold_chicken')
})
