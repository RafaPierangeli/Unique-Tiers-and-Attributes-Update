# Unique Tiers and Attributes Items (Tiered)

Unique Tiers and Attributes Items, (old Tiered) is a Fabric mod inspired by [Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools). Every tool you make will have a special modifier, as seen below:

### Installation
This mod built for the [Fabric Loader](https://fabricmc.net/). It requires [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api), [ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu) and [Cloth](https://www.curseforge.com/minecraft/mc-mods/cloth-config) to show configs and be installed separately; all other dependencies are installed with the mod.

### Customization

Tiered is entirely data-driven, which means you can add, modify, and remove modifiers as you see fit. The base path for modifiers is `data/modid/item_attributes`, and tiered modifiers are stored under the modid of tiered. Here's an example modifier called "Hasteful," which grants more dig speed when any of the valid tools are held:
```json
{
  "id": "tiered:common_tool_1",
  "weight": 60,
  "style": {
    "color": "gray",
    "bold": false
  },
  "verifiers": [
    {
      "tag": "minecraft:axes"
    },
    {
      "tag": "minecraft:pickaxes"
    },
    {
      "tag": "minecraft:shovels"
    },
    {
      "tag": "minecraft:hoes"
    }
  ],
  "roll_templates": [
    { "positive": 1, "negative": 1, "weight": 40 },
    { "positive": 1, "negative": 2, "weight": 30 },
    { "positive": 0, "negative": 1, "weight": 20 },
    { "positive": 1, "negative": 0, "weight": 10 }
  ],
  "positive_pool": [
    {
      "type": "minecraft:attack_damage",
      "min": 1,
      "max": 2,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "tiered:durable",
      "min": 0.1,
      "max": 0.2,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]

    },
    {
      "type": "minecraft:mining_efficiency",
      "min": 1,
      "max": 2,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:submerged_mining_speed",
      "min": 0.1,
      "max": 0.2,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },

    {
      "type": "minecraft:attack_speed",
      "min": 0.1,
      "max": 0.2,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "tiered:dig_speed",
      "min": 1,
      "max": 2,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:movement_speed",
      "min": 0.1,
      "max": 0.4,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:block_interaction_range",
      "amount": 1,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:luck",
      "amount": 1,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    }
  ],

  "negative_pool": [
    {
      "type": "minecraft:attack_damage",
      "min": -4,
      "max": -2,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "tiered:durable",
      "min": -0.5,
      "max": -0.25,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]

    },
    {
      "type": "minecraft:block_interaction_range",
      "amount": -2,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]

    },
    {
      "type": "minecraft:attack_speed",
      "min": -0.05,
      "max": -0.01,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:mining_efficiency",
      "min": -0.5,
      "max": -0.01,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:max_health",
      "min": -2.0,
      "max": -1.0,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:movement_speed",
      "min": -0.03,
      "max": -0.01,
      "operation": "add_multiplied_total",
      "required_equipment_slots": ["mainhand"]
    },
    {
      "type": "minecraft:luck",
      "min": -2,
      "max": -1,
      "operation": "add_value",
      "required_equipment_slots": ["mainhand"]
    }
  ]
}
```
Every jsons accept a variable attributes and chance to sort positive and negative rools. Armor equipment no need to specific a slot equipment ( this mod is work for you)
#### Attributes

Tiered currently provides 5 custom attributes: Dig Speed, Crit chance, Durability and Range Attack Damage. Dig Speed increases the speed of your block breaking (think: haste), Crit Chance offers a random chance to crit when using a tool and Durability increases, who would have thought it, the durability of an item.

Vanilla types:
- "armor"
- "armor_toughness"
- "attack_damage"
- "attack_knockback"
- "attack_speed"
- "block_break_speed"
- "block_interaction_range"
- "burning_time"
- "explosion_knockback_resistance"
- "entity_interaction_range"
- "fall_damage_multiplier"
- "flying_speed"
- "follow_range"
- "gravity"
- "jump_strength"
- "knockback_resistance"
- "luck"
- "max_absorption"
- "max_health"
- "mining_efficiency"
- "movement_efficiency"
- "movement_speed"
- "oxygen_bonus"
- "safe_fall_distance"
- "scale"
- "sneaking_speed"
- "zombie.spawn_reinforcements"
- "step_height"
- "submerged_mining_speed"
- "sweeping_damage_ratio"
- "water_movement_efficiency"

Tiered Types:
- "tiered:dig_speed"
- "tiered:dig_speed"
- "tiered:critical_chance"
- "tiered:critical_damage"
- "tiered:range_attack_damage"

#### Verifiers

A verifier (specified in the "verifiers" array of your modifier json file) defines whether or not a given tag or tool is valid for the modifier. 

A specific item ID can be specified with:
```json
"id": "minecraft:apple"
```

and a tag can be specified with:
```json
"tag": "minecraft:head_armor"
```

Tiered doesn't provide tags.  
Example tags in vanilla minecraft: `minecraft:pickaxes`, `minecraft:axes`, `minecraft:shovels`,`minecraft:hoes`, `minecraft:swords` and several more.  
Item tags can be found on the [wiki](https://minecraft.wiki/w/Tag#Item_tags_2).

#### Weight

The weight determines the commonness of the tier. Higher weights increase the chance of being applied on the item and vice versa.

#### Tooltip
Since V1.2, tooltip borders and ornaments can be disable and get set via a resource pack.
- The border texture has to be in the `assets\tiered\textures\gui` folder.
- The file has to be a json file and put inside the `assets\tiered\tooltips` folder.
- The gradients has to be hex code, check transparency here: [https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4](https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4)


#### Reforge

Reforging items to get other tiers can be done at the anvil. There is a slot which is called "base" on the left and a slot called "ADD_VALUE" on the right.
The ADD_VALUE slot can only contain items which are stated in the `tiered:REFORGE_ADDITION` item tag. The base slot can contain the reforging item material item if existent, otherwise it can only contain `tiered:reforge_base_item` tag items. The base slot item can get changed via datapack, an example can be found below and has to get put in the `tiered:reforge_items` folder.  
The `tiered:modifier_restricted` item tag can be used to restrict item modifiers including reforging.

```json
{
  "items": [
    "minecraft:bow"
  ],
  "base": [
    "minecraft:string"
  ]
}
```

### License
Tiered is licensed under MIT. You are free to use the code inside this repo as you want.
