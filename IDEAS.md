# Modern Chickens Idea Board

This document is for cataloguing and detailing potential ideas for features.

## Mega Chicken
- Modifier Items
    - Increase Scale
        * Nether Star
        - Maximum uses: 5
        - Increases scale to a total of 2x over the course of the 5 nether stars
    - Decrease Scale
        * Dragon's Breath
        - Maximum uses: 5
        - Increases scale to a total of -2x over the course of the 5 bottles
- Using either of these items on the mega chicken results in the desired outcome 

## Mega Chicken Skins 
- I have added a plethora of new mega chicken skins;
    ```
        newTextures\chickens\Mega\zombie_chicken.png
        newTextures\chickens\Mega\valentines_chicken.png
        newTextures\chickens\Mega\toxic_chicken.png
        newTextures\chickens\Mega\reptar_chicken.png
        newTextures\chickens\Mega\rambo_chicken.png
        newTextures\chickens\Mega\pink_chicken.png
        newTextures\chickens\Mega\fox_chicken.png
        newTextures\chickens\Mega\duck_chicken.png
        newTextures\chickens\Mega\dodo_chicken.png
        newTextures\chickens\Mega\deepdark_chicken.png
        newTextures\chickens\Mega\creeper_chicken.png
        newTextures\chickens\Mega\bigbrain_chicken.png
        newTextures\chickens\Mega\aviator_chicken.png   
    ```
- I want to add a new item; the 'Skin Crate' 
    - the Skin Crates will be added to end-game loot tables such as the dragon, wither, etc. and have a very small chance to drop 
    - the Skin Crate will 'contain' one of these respective skins, and when right-click used on an owned mega chicken, applies the respective skin to the chicken and consumes the item 
    - the Skin Crate will have a Cyan name, and show what skin is inside in the tooltip 


## New runtime chicken skin generation idea
- Use `newTextures\chicken_base.png` for all chickens (bottom layer)
- Use `newTextures\chicken_resource.png` and tint it in runtime based on the respective material (middle layer)
- Use `newTextures\chicken_parts.png` for face/beak, etc. (top layer)
- Apply tinted resource and then the features to the base chicken skin, created a spotted chicken tinted in runtime based on the respective material used to create/tint the chicken
- Continue to use the bone white/skeleton chicken for fluid/chemical chickens with their existing fluid/chemical representation on their bodies













## Lava Chicken
- Lava Chicken Buff asset broken
    - `src\main\resources\assets\modern_companions\textures\gui\burning.png`
    - `src\main\resources\assets\modern_companions\textures\gui\burning18.png`
    - `src\main\resources\assets\modern_companions\textures\gui\burning32.png`
- Should ignite player, and leave fire trail - is not
    - Player should visually be ignited, but receive no damage
    - Player should ignite the ground they are standing on during the duration of the buff
        - Fire placed at the players feet does not spread and does not damage/destroy blocks or entities
- Needs JEI recipe for dumping lava on a chicken
    - `src\main\resources\assets\chickens\textures\gui\lava_chicken.png`
        - The lava bucket should be displayed at `39,5` to `54,20`
        - The lava chicken item should be displayed at `38,51` to `53,66`
        - The overall JEI recipe area is `0,0` to `90,77`

## RF Upgrade Item
- This upgrade should have similar vanilla-friendly recipe, and JEI entry
- This will increase the capacity of the total RF of the machine it is inserted into
- Each upgrade doubles the RF capacity, cumulatively. 
- Each machine can hold a maximum of 3 RF Upgrades
- Item asset; `src\main\resources\assets\chickens\textures\item\rfupgrade.png`

## Avian Dousing Machine
- Hidden slot under the progress bar, this needs removed so "No Itegm Reagent Stored" tooltip is not visible
    - ![alt text](image.png)
- Include Speed / Energy Upgrade support
    - Added two new slots to the Dousing GUI
        - Speed Upgrade
            - Located; `103,63` to `118,78`
        - RF Upgrade
            - Located; `124,63` to `139,78`
    - Dousing Machie supports `3` RF upgrades and `5` Speed Upgrades

## Roost Collector
- Should only takes items out of Roosts and Mechanical Roosts
- Ability to see the collector's range

## Chemical Chickens
- Mekanism Extras have generic recipes for chemical chickens, needs fixed so they make more sense 

## Rooster
- No Spawn egg in creative
- Need to make sure t has the working GUI when right clicked with an empty hand
- Need to make sure it is functional as intended

## Mechanical Roost
- Consuming a LOT of power for 10/10/10 chickens
- Enable RF Capacity Upgrade
- Heavily increase the rate in which it can receive power, it takes power much too slow 
- Display how much RF each operation is consuming when hovering the RF bar
- Stack Upgrade superfulous with internal virtual inventory, I have replaced the Stack Slot with an RF Upgrade Slot

## Upgrades
- Clear indicator of what the upgrade does in their respective tooltips, with accurate numbers such as % increased, etc.
- All upgrade items should stack to 64 respectively 
- We need to make sure that upgrade affects are stacking effectively and accurately

## Nest
- Using Jade, I would like to see; 
    - range
    - boost duration
    - boost multiplier
    - if there is a conflicting nest in the area/range

## Misc
- Add IO ports to all these machines; Avian Flux Converter, Avian Dousing Machine, Avian Fluid Converter, Avian Chemical Converter, Mechanical Roost, Roost Collector, Incubator, Henhouses, Roost, Chicken Breeder on all sides

## Converter Machines
- The bars in the Avian Fluid and Chemical converter are just red, we need to be coloring them according to their contents
    - I suspect because these machines are reusing the flux converter gui, so I created a new one; `src\main\resources\assets\chickens\textures\gui\fluidchemicalconverter.png`
    - The new GUI is identical to the Flux Converter GUI (the current one), except the fill bar is grey and should be recolored in runtime based on the chemical / fluid. 




















## Implemented

##  Current/Plausible

* Dedicated Mod-Book - Have an in-game guide-book for the Modern Chickens mod.

* Achievements - Add achievements for milestones in Modern Chickens.

## Far-Out Ideas

* Mob Farming/Grinding - Develop a 'Toxic Hen House' where players can house toxic (Radioactive, toxic chemical) chickens and have their toxicity radiate outwards from the Toxic Hen House - killing anything nearby.

* Funny/Gag/Meme Chickens - Use existing unused assets to create joke chickens.

* Time Egg - When thrown speeds up tick time in a small area for a short while (imagine accelerating crop growth or machine speed – basically a chicken-based Time in a Bottle).

* Weather Chicken - Controls rain and thunder by its clucking (laid eggs that, when cracked, can change weather).

* Chicken Generator - Turns standard chicken byproducts (Meat, Feathers, Eggs) into RF

* DNA Splicing - Ability to extract DNA from high-stat chickens to apply to other chickens.

* Summoning & Familiars – Add new mobs or summonable creatures via chicken items.
    * Chest Chicken - Has an internal storage that can carry items for you!
    * Nether Chest Chicken - Same as above, but nether!
    * A Chicken that attacks enemies. (Fireball??)
    * Crafting Chicken - Acts as a mobile crafting table (saves contents)
    * Furnace Chicken - Cooking on the go!
    * Shoulder Chicken - Small chicken that rides on the players' shoulder.
    * Vacuum Chicken - Has an internal storage, vacuums nearby dropped items. (saves contents)
    * Battery Chicken - Walking RF Storage - Wirelessly supplies to nearby machines.

* Chicken Dimension - For a huge exploration addition, consider a new dimension or area themed around chickens. This could be a “Sky Hen Sanctuary” dimension accessed by using a special egg (similar in spirit to the Aether or Twilight Forest portals). In this realm, players find floating islands made of giant nests, exclusive breeds (like Cloud Chickens or Thunderbirds), and perhaps a unique dungeon or mini-boss (Mother Hen boss guarding rare eggs). While creating a full dimension is a large undertaking, even a smaller-scale area or structure could work: e.g. Chicken Temples that spawn in the world, filled with hostile “feral chickens” and guarding a loot chest of rare eggs. This adds exploration gameplay – players have reasons to leave their base and seek out legendary chickens in far-off lands or structures.

* Cybernetic “Smart Chickens” – Introduce cyber-chickens augmented with robotic parts and AI. Players could craft an Avian Augmentation Station to install modules (e.g. night vision, item scanning, increased production) onto chickens. These cyborg chickens might interface with Redstone Flux power: for example, a cyber chicken could directly charge devices or act as a walking battery. This would build on the mod’s existing RF-generating chickens, taking automation further with programmable, upgradeable chickens. Imagine a Motherboard Hen that links to a base’s network and manages all your coops autonomously – a playful nod to mods that let you automate animal farms with high-tech machines (similar in spirit to Industrial Foregoing’s animal rancher, but chicken-themed).

* Quantum Coop Chunk Loader – A high-tech coop structure that keeps areas loaded and working even when players are far away. Inspired by the ChickenChunks mod’s eponymous chunk loader (a block that keeps chunks loaded in memory), this feature could be a Quantum Entangled Henhouse: a coop that, when supplied with power or special feed, generates a field keeping nearby chunks active. The model could humorously resemble a chicken sitting on a satellite dish. This ensures your automated chicken farms and machines run 24/7. It ties into Tech by using energy (perhaps Flux Eggs as fuel) and into Modern Chickens’ theme by looking like a futuristic coop. Such a block would let creative players build grand automated farms without worrying about chunk unloading – useful for maintaining breeding or resource output continuously, much like existing chunk loaders in tech mods.

* Robotic Chicken Drones – Unlockable in late Tech tiers, players could build flying chicken drones that perform tasks. These drone companions (shaped like metallic chickens with rotor blades) would automatically collect drops, fertilize crops with manure, or attack hostile mobs in patrol mode. This adds a lighthearted futuristic take on automation – think miniature chicken robots zipping around your base carrying items. They could run on energy (recharged via sitting on a powered roost pad). This idea draws from the automation helpers in mods like OpenComputers or PneumaticCraft drones, but in a chicken-themed comedic form. It extends Modern Chickens into the realm of buildable contraptions, giving tech-savvy players new toys to engineer.

* Chicken Ore Processing - Develop a method for chickens to drive ore multiplication/production.

* Wireless Chicken Power - Develop a way for Chickens to produce RF Wirelessly to a set area.
