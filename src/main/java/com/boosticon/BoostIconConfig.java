package com.boosticon;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("boostIconPlugin")
public interface BoostIconConfig extends Config
{
	@ConfigSection(
		name = "Stats",
		description = "Which combat stats get an icon.",
		position = 100
	)
	String statsSection = "stats";

	@Range(max = 100)
	@ConfigItem(
		keyName = "size",
		name = "Size",
		description = "Adjust the size of the icons.",
		position = 1
	)
	default int size()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "position",
		name = "Icon position",
		description = "Change what side of the HP bar the icons appear.",
		position = 2
	)
	default IconSide iconSide()
	{
		return IconSide.RIGHT;
	}

	@ConfigItem(
		keyName = "statText",
		name = "Show",
		description = "What to write next to each icon.",
		position = 3
	)
	default StatText statText()
	{
		return StatText.CHANGE;
	}

	@ConfigItem(
		keyName = "showBoosts",
		name = "Show boosts",
		description = "Show stats that are above their real level.",
		position = 4
	)
	default boolean showBoosts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDrains",
		name = "Show drains",
		description = "Show stats that are below their real level.",
		position = 5
	)
	default boolean showDrains()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "boostColor",
		name = "Boost colour",
		description = "Colour of the number on a boosted stat.",
		position = 6
	)
	default Color boostColor()
	{
		return new Color(0, 255, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "drainColor",
		name = "Drain colour",
		description = "Colour of the number on a drained stat.",
		position = 7
	)
	default Color drainColor()
	{
		return new Color(255, 48, 48);
	}

	@ConfigItem(
		keyName = "partyStats",
		name = "Party stats",
		description = "Show your party's boosted and drained stats over their heads, and share yours with them.",
		position = 8
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
