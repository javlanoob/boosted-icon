package com.boosticon;

import java.util.function.Predicate;
import net.runelite.api.Skill;
import net.runelite.api.gameval.SpriteID;

/**
 * The seven skills that make up your combat level, in the order they sit in the stats tab, each with
 * the stats tab icon the game draws for it, and special attack, which is not a skill but runs down and
 * comes back the way the points among them do.
 */
enum CombatStat
{
	ATTACK(Skill.ATTACK, SpriteID.Staticons.ATTACK, BoostIconConfig::showAttack),
	STRENGTH(Skill.STRENGTH, SpriteID.Staticons.STRENGTH, BoostIconConfig::showStrength),
	DEFENCE(Skill.DEFENCE, SpriteID.Staticons.DEFENCE, BoostIconConfig::showDefence),
	RANGED(Skill.RANGED, SpriteID.Staticons.RANGED, BoostIconConfig::showRanged),
	PRAYER(Skill.PRAYER, SpriteID.Staticons.PRAYER, BoostIconConfig::showPrayer),
	MAGIC(Skill.MAGIC, SpriteID.Staticons.MAGIC, BoostIconConfig::showMagic),
	HITPOINTS(Skill.HITPOINTS, SpriteID.Staticons.HITPOINTS, BoostIconConfig::showHitpoints),
	SPECIAL(null, SpriteID.OrbIcon.SPECIAL, BoostIconConfig::showSpecial);

	/**
	 * A full special attack bar. It is kept as a share of one rather than as a level, so this is the
	 * most there can be of it.
	 */
	static final int FULL_PERCENT = 100;

	private final Skill skill;
	private final int spriteId;
	private final Predicate<BoostIconConfig> enabled;

	CombatStat(Skill skill, int spriteId, Predicate<BoostIconConfig> enabled)
	{
		this.skill = skill;
		this.spriteId = spriteId;
		this.enabled = enabled;
	}

	/**
	 * The skill this stat is, or nothing where it is not a skill at all, as special attack is not.
	 */
	Skill getSkill()
	{
		return skill;
	}

	int getSpriteId()
	{
		return spriteId;
	}

	boolean isEnabled(BoostIconConfig config)
	{
		return enabled.test(config);
	}

	/**
	 * Whether the stat is points that run down and get topped up again rather than a level that sits
	 * where it is until something acts on it. How much of these is left matters more than how far they
	 * are from the level, so they are read and coloured that way.
	 */
	boolean isPoints()
	{
		return this == HITPOINTS || this == PRAYER || this == SPECIAL;
	}

	/**
	 * Whether the stat is a share of a full bar rather than a level, which special attack alone is.
	 */
	boolean isPercent()
	{
		return this == SPECIAL;
	}

	/**
	 * Whether a number is one this stat could be at, so that nothing is made of a stat arriving from
	 * elsewhere as something it cannot be. Levels start at one, while a special attack bar can be
	 * empty, which is where a spec leaves it.
	 */
	boolean isPossible(int level)
	{
		return isPercent() ? level >= 0 && level <= FULL_PERCENT : level >= 1;
	}

	/**
	 * The stat a {@link Skill} ordinal stands for, or nothing where it is a skill with no bearing on
	 * combat, or no skill at all. Ordinals arrive from other players, so the skill itself is never
	 * looked up by one.
	 */
	static CombatStat ofSkill(int skillOrdinal)
	{
		for (CombatStat stat : values())
		{
			if (stat.skill != null && stat.skill.ordinal() == skillOrdinal)
			{
				return stat;
			}
		}

		return null;
	}
}
