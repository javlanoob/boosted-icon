package com.boosticon;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Combat stats for a party that has nobody sharing levels already, as levels rather than as the boosts
 * and drains worked out from them, so everyone's own settings decide what they see of them. Named after
 * the plugin because every message on the party connection has to be named differently to every other
 * plugin's.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class BoostIconStats extends PartyMemberMessage
{
	/**
	 * The level each combat stat is at now, in {@link CombatStat} order.
	 */
	@SerializedName("b")
	int[] boosted;

	/**
	 * The level each one would be at with nothing acting on it, in the same order.
	 */
	@SerializedName("r")
	int[] real;
}
