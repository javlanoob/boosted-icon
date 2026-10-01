package com.boosticon;

import java.util.function.Predicate;
import net.runelite.api.Skill;
import net.runelite.api.gameval.SpriteID;

/**
 * The seven skills that make up your combat level, in the order they sit in the stats tab, each with
 * the stats tab icon the game draws for it.
 */
enum CombatStat
{
	ATTACK(Skill.ATTACK, SpriteID.Staticons.ATTACK, BoostIconConfig::showAttack),
	STRENGTH(Skill.STRENGTH, SpriteID.Staticons.STRENGTH, BoostIconConfig::showStrength),
	DEFENCE(Skill.DEFENCE, SpriteID.Staticons.DEFENCE, BoostIconConfig::showDefence),
	RANGED(Skill.RANGED, SpriteID.Staticons.RANGED, BoostIconConfig::showRanged),
	PRAYER(Skill.PRAYER, SpriteID.Staticons.PRAYER, BoostIconConfig::showPrayer),
	MAGIC(Skill.MAGIC, SpriteID.Staticons.MAGIC, BoostIconConfig::showMagic),
	HITPOINTS(Skill.HITPOINTS, SpriteID.Staticons.HITPOINTS, BoostIconConfig::showHitpoints);

	private final Skill skill;
	private final int spriteId;
	private final Predicate<BoostIconConfig> enabled;

	CombatStat(Skill skill, int spriteId, Predicate<BoostIconConfig> enabled)
	{
		this.skill = skill;
		this.spriteId = spriteId;
		this.enabled = enabled;
	}

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
		return this == HITPOINTS || this == PRAYER;
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
			if (stat.skill.ordinal() == skillOrdinal)
			{
				return stat;
			}
		}

		return null;
	}
}
