# Lifesteal Plugin for Minecraft 1.21.1

A custom lifesteal plugin with heart mechanics, crafting, and special dragon egg bonus.

## Features

✅ **Lifesteal Mechanics**: Gain a heart for each player kill
✅ **Minimum Hearts**: Players cannot go below 3 hearts
✅ **Withdraw Command**: `/withdraw <amount>` - Convert hearts to items
✅ **Craftable Hearts**: Special recipe with ominous trial keys (max 10 hearts from crafting)
✅ **Dragon Egg Bonus**: Max hearts increase from 20 to 25 when holding dragon egg
✅ **Protection**: Players at minimum hearts cannot lose hearts, killers don't gain hearts from them

## Installation

### Requirements
- Minecraft Server 1.21.1
- Paper, Spigot, or Purpur server software
- Java 21

### Steps

1. **Build the Plugin**:
   - Make sure you have Maven installed
   - Navigate to the plugin folder
   - Run: `mvn clean package`
   - The compiled plugin will be in `target/LifestealPlugin-1.0.jar`

2. **Install on Server**:
   - Copy `LifestealPlugin-1.0.jar` to your server's `plugins` folder
   - Restart your server
   - Plugin should load automatically

3. **Alternative - Pre-built**:
   - If you don't want to build it yourself, use the provided JAR file
   - Just drop it into your `plugins` folder

## How It Works

### Heart Crafting Recipe
```
O N O
G S G
O N O
```
Where:
- **O** = Ominous Trial Key
- **N** = Netherite Ingot
- **G** = Gold Block
- **S** = Nether Star

**Note**: Crafted hearts can only get you up to 10 hearts maximum. To go beyond 10 hearts, you must kill other players!

### Commands

- `/withdraw <amount>` - Withdraw hearts from your health and get heart items
  - Example: `/withdraw 2` removes 2 hearts and gives you 2 heart items
  - Cannot withdraw below 3 hearts minimum

### Heart System

- **Default Max**: 20 hearts (40 health)
- **With Dragon Egg**: 25 hearts (50 health) - Just have it in your inventory!
- **Minimum**: 3 hearts (6 health) - Cannot lose hearts below this
- **Crafting Limit**: Can only craft up to 10 hearts (20 health)

### Lifesteal Rules

1. When you kill a player, you gain 1 heart (if not at max)
2. The killed player loses 1 heart (if not at minimum)
3. Players at 3 hearts cannot lose more hearts
4. Killing a player at minimum hearts gives you no heart
5. Dragon egg holders can reach 25 hearts total

## Permissions

- `lifesteal.withdraw` - Use the withdraw command (default: everyone)

## Troubleshooting

**Plugin won't load?**
- Make sure you're running Paper/Spigot 1.21.1
- Check console for errors
- Verify Java 21 is installed

**Hearts not crafting?**
- Verify recipe matches exactly (case-sensitive materials)
- Check if you're already at 10 hearts from crafting

**Dragon egg bonus not working?**
- Make sure dragon egg is in your inventory (not enderchest)
- Try re-logging

## Support

For issues or questions, check your server console logs for error messages.
