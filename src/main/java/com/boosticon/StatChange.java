package com.boosticon;

/**
 * A combat stat that is not where it would be with nothing acting on it, as of the last tick.
 */
class StatChange
{
	private final CombatStat stat;
	private final int level;
	private final int change;

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
}
