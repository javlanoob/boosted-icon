package com.boosticon;

import java.awt.Color;

/**
 * A combat stat that is not where it would be with nothing acting on it, as of the last tick.
 *
 * What the stat reads as, meaning the number beside its icon and the colour that number is in, is worked
 * out here rather than while drawing, since it is the same for every frame of the tick the stat was read
 * on. The settings it was worked out from are kept beside it, and a different set means working it out
 * again, so a setting changed part way through a tick still shows on the very next frame.
 */
class StatChange
{
	private final CombatStat stat;
	private final int level;
	private final int change;

	/**
	 * The settings the two below were worked out from, if they have been worked out at all. Compared by
	 * which set it is rather than by what is in it, since a changed setting is always a new set.
	 */
	private Settings read;
	private String label;
	private Color color;

	StatChange(CombatStat stat, int level, int change)
	{
		this.stat = stat;
		this.level = level;
		this.change = change;
	}

	CombatStat getStat()
	{
		return stat;
	}

	/**
	 * The level the stat is at now, boost or drain included.
	 */
	int getLevel()
	{
		return level;
	}

	/**
	 * How far that is from the real level: positive for a boost, negative for a drain.
	 */
	int getChange()
	{
		return change;
	}

	/**
	 * The level with nothing acting on it, which for hitpoints and prayer is the full amount of them.
	 */
	int getRealLevel()
	{
		return level - change;
	}

	/**
	 * What is written beside the icon, or nothing where the settings ask for no number at all.
	 */
	String label(Settings settings)
	{
		read(settings);

		return label;
	}

	/**
	 * What colour that number is written in.
	 */
	Color color(Settings settings)
	{
		read(settings);

		return color;
	}

	private void read(Settings settings)
	{
		if (settings != read)
		{
			read = settings;
			label = labelFor(settings);
			color = colorFor(settings);
		}
	}

	private String labelFor(Settings settings)
	{
		if (stat.isPoints() && settings.statText() != StatText.NONE)
		{
			// What is left of them is the news, rather than how far off the full amount that is
			return String.valueOf(level);
		}

		switch (settings.statText())
		{
			case CHANGE:
				return change > 0 ? "+" + change : String.valueOf(change);
			case LEVEL:
				return String.valueOf(level);
			default:
				return null;
		}
	}

	/**
	 * The colours the game's own Boosts plugin uses, so a stat reads the same here as it does there:
	 * green while buffed, yellow once the buff is down to its last levels, red while debuffed.
	 */
	private Color colorFor(Settings settings)
	{
		if (stat.isPercent())
		{
			// A spec costs what it costs, so there is no amount of it that counts as being in trouble
			return settings.unchangedColor();
		}

		if (stat.isPoints())
		{
			return pointsColor(settings);
		}

		if (change == 0)
		{
			return settings.unchangedColor();
		}

		if (change < 0)
		{
			return settings.debuffColor();
		}

		int threshold = settings.buffThreshold();

		return threshold > 0 && change <= threshold ? settings.expiringColor() : settings.buffColor();
	}

	/**
	 * Hitpoints and prayer are coloured by how much of them is left rather than by which way they are
	 * off the full amount, so that a brew reads as plenty and a long fight reads as trouble.
	 */
	private Color pointsColor(Settings settings)
	{
		int full = getRealLevel();

		if (full < 1)
		{
			return settings.buffColor();
		}

		int left = level * 100 / full;

		if (left < settings.criticalHp())
		{
			return settings.debuffColor();
		}

		return left < settings.lowHp() ? settings.expiringColor() : settings.buffColor();
	}
}
