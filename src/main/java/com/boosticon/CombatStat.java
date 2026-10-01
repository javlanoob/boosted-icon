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
}
