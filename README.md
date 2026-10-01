# Boost Icon
Icons by your health bar for the combat stats that are boosted or drained.

When a combat stat is above or below its real level, the stat's own icon from the skills tab appears
next to your health bar with the change beside it: green while the stat is boosted, yellow once the
boost is nearly gone, red while it is drained. The icons stack while more than one stat is off its
level, and they disappear as each one comes back.

Based on [Frozen Icon](https://github.com/HamzehAdawi/FrozenIcon) by HamzehAdawi, which does the same
thing for freezes and binds.

## Settings
**Size**: how much bigger or smaller the icons are drawn. Each one keeps its own shape rather than
being squared off.

**Lower by**: how far below the health bar the column of icons sits. Below zero puts it above the bar.

**Icon position**: which side of the health bar the icons appear on.

**Show**: what goes next to each icon — the change (`+5`, `-3`), the level the stat is at now, or
nothing.

**Show buffs** and **Show debuffs**: whether stats above their level, below their level, or both get
an icon.

**Buff colour**, **Debuff colour** and **Running out colour**: the colour of the number. The defaults
are the ones the game's own Boosts plugin uses, so a stat reads the same in both.

**Buff threshold**: how many levels a buff has left for it to count as running out and turn the colour
above. 0 never does.

**Party stats**: with this on, the same icons appear over the heads of the party members around you.

Party members do not need this plugin for that. Party Panel already shares every skill's level with the
party, so anyone running it is already sending everything needed, and their boosts and drains show up
without them doing anything. Whatever reaches this plugin is levels rather than the boosts worked out
from them, so your own settings decide what you see of theirs.

For a party where nobody is sharing levels already, Boost Icon shares your own, and anyone else running
it with the setting on will show up as well.

### Stats
Every combat stat can be turned on or off on its own. Attack, Strength, Defence, Ranged and Magic are
on; Prayer and Hitpoints are off, since their points are already on the orbs and would otherwise sit
there in red for most of a trip. Turn them on if you want brews and overloads shown too.
