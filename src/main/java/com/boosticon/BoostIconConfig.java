package com.boosticon;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(BoostIconConfig.GROUP)
public interface BoostIconConfig extends Config
{
	String GROUP = "boostIconPlugin";

	@ConfigSection(
		name = "Stats",
		description = "Which combat stats get an icon.",
		position = 100
	)
	String statsSection = "stats";

	@Range(min = -12, max = 100)
	@ConfigItem(
		keyName = "size",
		name = "Size",
		description = "Adjust the size of the icons. Below zero for smaller than they are now.",
		position = 1
	)
	default int size()
	{
		return 0;
	}

	@Range(min = -200, max = 200)
	@ConfigItem(
		keyName = "drop",
		name = "Lower by",
		description = "How far below the health bar the icons sit. Below zero puts them above it.",
		position = 2
	)
	default int drop()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "anchor",
		name = "Anchor",
		description = "What the icons are measured from: the player's head, middle or feet.",
		position = 3
	)
	default IconAnchor iconAnchor()
	{
		return IconAnchor.TOP;
	}

	@ConfigItem(
		keyName = "position",
		name = "Icon position",
		description = "Change what side of the HP bar the icons appear.",
		position = 4
	)
	default IconSide iconSide()
	{
		return IconSide.RIGHT;
	}

	@ConfigItem(
		keyName = "statText",
		name = "Show",
		description = "What to write next to each icon.",
		position = 5
	)
	default StatText statText()
	{
		return StatText.CHANGE;
	}

	@ConfigItem(
		keyName = "showBuffs",
		name = "Show buffs",
		description = "Show stats that are above their real level.",
		position = 6
	)
	default boolean showBuffs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDebuffs",
		name = "Show debuffs",
		description = "Show stats that are below their real level.",
		position = 7
	)
	default boolean showDebuffs()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "buffColor",
		name = "Buff colour",
		description = "Colour of the number on a buffed stat.",
		position = 8
	)
	default Color buffColor()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
		keyName = "debuffColor",
		name = "Debuff colour",
		description = "Colour of the number on a debuffed stat.",
		position = 9
	)
	default Color debuffColor()
	{
		return new Color(238, 51, 51);
	}

	@Alpha
	@ConfigItem(
		keyName = "expiringColor",
		name = "Running out colour",
		description = "Colour of the number once a buff is down to its last few levels.",
		position = 10
	)
	default Color expiringColor()
	{
		return Color.YELLOW;
	}

	@Range(max = 99)
	@ConfigItem(
		keyName = "buffThreshold",
		name = "Buff threshold",
		description = "How many levels a buff has left for it to count as running out. 0 for never.",
		position = 11
	)
	default int buffThreshold()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "showSelf",
		name = "Show on yourself",
		description = "Show your own boosted and drained stats over your head.",
		position = 12
	)
	default boolean showSelf()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partyStats",
		name = "Party stats",
		description = "Show your party's boosted and drained stats over their heads. Team mates already sharing their stats with the party, as Party Panel does, need nothing else.",
		position = 13
	)
	default boolean partyStats()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showAttack",
		name = "Attack",
		description = "Show an icon when Attack is boosted or drained.",
		section = statsSection,
		position = 101
	)
	default boolean showAttack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStrength",
		name = "Strength",
		description = "Show an icon when Strength is boosted or drained.",
		section = statsSection,
		position = 102
	)
	default boolean showStrength()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDefence",
		name = "Defence",
		description = "Show an icon when Defence is boosted or drained.",
		section = statsSection,
		position = 103
	)
	default boolean showDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRanged",
		name = "Ranged",
		description = "Show an icon when Ranged is boosted or drained.",
		section = statsSection,
		position = 104
	)
	default boolean showRanged()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMagic",
		name = "Magic",
		description = "Show an icon when Magic is boosted or drained.",
		section = statsSection,
		position = 105
	)
	default boolean showMagic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayer",
		name = "Prayer",
		description = "Show an icon for your prayer points. Off by default, since the orb already shows them.",
		section = statsSection,
		position = 106
	)
	default boolean showPrayer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showHitpoints",
		name = "Hitpoints",
		description = "Show an icon for your hitpoints. Off by default, since the orb already shows them.",
		section = statsSection,
		position = 107
	)
	default boolean showHitpoints()
	{
		return false;
	}
}
