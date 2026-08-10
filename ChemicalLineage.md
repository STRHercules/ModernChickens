# Mekanism Chicken Progression

```mermaid
flowchart LR
    D["Smart Chicken + chemical reagent"] -. dousing .-> S["Bio / Carbon / Redstone starters"]

    S --> F["Foundational infusions and pigments"]
    W["Water / Lava / Brine chickens"] --> A["Atmospheric and industrial chemistry"]
    SU["Sulfur + Water fluid line"] --> AC["Sulfuric acid / Hydrofluoric acid"]
    AC --> SL["Dirty slurries"]
    SL --> CW["Clean slurries + Water"]

    U["Uranium resource chicken"] --> N["Uranium oxide → UF6 → Fissile fuel"]
    N --> NW["Nuclear waste → Spent waste"]
    NW --> P["Plutonium / Polonium"]
    P --> AM["Antimatter"]
    AM --> T["Americium → Curium → Californium"]

    F --> E["Evolved Mekanism late branches"]