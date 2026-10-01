package com.boosticon;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;

/**
 * Everything the plugin draws by, read off the settings in one go.
 *
 * A setting costs a lookup and a parse every time it is asked for, which is nothing once and a great
 * deal of nothing for every stat of every player on the screen, every frame. So the whole set is taken
 * here instead, and taken again whenever one of them changes, which leaves a frame costing nothing in
 * settings at all. Held onto and handed about rather than changed in place, so that the party thread
 * reading one of these is reading a set that all came from the same moment.
 */
class Settings
{
	private final int size;
	private final int drop;
	private final boolean right;
	private final IconAnchor anchor;
	private final StatText statText;
	private final Composite composite;
	private final Color buffColor;
	private final Color debuffColor;
	private final Color expiringColor;
	private final Color unchangedColor;
	private final int buffThreshold;
	private final int lowHp;
	private final int criticalHp;
	private final boolean showSelf;
	private final boolean partyStats;
	private final boolean showUnchanged;
	private final boolean showBuffs;
	private final boolean showDebuffs;

	/**
	 * Which stats are switched on, by {@link CombatStat} ordinal, since that is the order they are read
	 * and drawn in and a setting apiece is eight lookups otherwise.
	 */
	private final boolean[] enabled = new boolean[CombatStat.ALL.length];

	Settings(BoostIconConfig config)
	{
		size = config.size();
		drop = config.drop();
		right = config.iconSide() == IconSide.RIGHT;
		anchor = config.iconAnchor();
		statText = config.statText();
		composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity(config));
		buffColor = config.buffColor();
		debuffColor = config.debuffColor();
		expiringColor = config.expiringColor();
		unchangedColor = config.unchangedColor();
		buffThreshold = config.buffThreshold();
		lowHp = config.lowHp();
		criticalHp = config.criticalHp();
		showSelf = config.showSelf();
		partyStats = config.partyStats();
		showUnchanged = config.showUnchanged();
		showBuffs = config.showBuffs();
		showDebuffs = config.showDebuffs();

		for (int i = 0; i < enabled.length; i++)
		{
			enabled[i] = CombatStat.ALL[i].isEnabled(config);
		}
	}

	/**
	 * How solid everything is drawn, worked out here rather than per frame. Anything outside the settings
	 * panel could have put anything in the setting, so it is held to what it can be.
	 */
	private static float opacity(BoostIconConfig config)
	{
		return Math.max(0, Math.min(100, config.opacity())) / 100f;
	}

	int size()
	{
		return size;
	}

	int drop()
	{
		return drop;
	}

	/**
	 * Whether the icons go to the right of the health bar rather than the left of it.
	 */
	boolean right()
	{
		return right;
	}

	IconAnchor anchor()
	{
		return anchor;
	}

	StatText statText()
	{
		return statText;
	}

	Composite composite()
	{
		return composite;
	}

	Color buffColor()
	{
		return buffColor;
	}

	Color debuffColor()
	{
		return debuffColor;
	}

	Color expiringColor()
	{
		return expiringColor;
	}

	Color unchangedColor()
	{
		return unchangedColor;
	}

	int buffThreshold()
	{
		return buffThreshold;
	}

	int lowHp()
	{
		return lowHp;
	}

	int criticalHp()
	{
		return criticalHp;
	}

	boolean showSelf()
	{
		return showSelf;
	}

	boolean partyStats()
	{
		return partyStats;
	}

	boolean showUnchanged()
	{
		return showUnchanged;
	}

	boolean showBuffs()
	{
		return showBuffs;
	}

	boolean showDebuffs()
	{
		return showDebuffs;
	}

	boolean isEnabled(int stat)
	{
		return enabled[stat];
	}
}
