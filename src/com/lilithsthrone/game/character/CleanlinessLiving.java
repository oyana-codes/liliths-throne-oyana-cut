package com.lilithsthrone.game.character;

import java.util.ArrayList;

import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.effects.StatusEffect;
import com.lilithsthrone.game.character.npc.NPC;
import com.lilithsthrone.game.character.npc.dominion.SlaveInStocks;
import com.lilithsthrone.game.character.persona.Occupation;
import com.lilithsthrone.game.occupantManagement.slave.SlaveJob;
import com.lilithsthrone.game.occupantManagement.slave.SlaveJobFlag;
import com.lilithsthrone.game.occupantManagement.slave.SlavePermissionSetting;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.world.places.PlaceType;

/**
 * How cleanliness moves outside a bath.
 * Time passing costs about 4 points an hour. Sex and combat cost more, on the spot.
 * People you have not been standing next to still live their day, so a shop is sweaty by evening
 * and a prostitute is washed for the next client.
 * The player is never snapped to a lifestyle value. Only time, sex, and combat move them.
 */
public final class CleanlinessLiving {

	private CleanlinessLiving() {
	}

	public static void drop(GameCharacter character, int amount) {
		if(amount<=0 || !tracks(character)) {
			return;
		}
		character.incrementAttribute(Attribute.CLEANLINESS, -amount);
	}

	/** Tenths added to each 12-minute tick. A fresh wash and the player stay on the base rate. */
	public static int extraTenths(GameCharacter character) {
		if(!tracks(character) || character.isPlayer() || hasFreshWash(character)) {
			return 0;
		}
		int extra = spread(character, 3);
		if(isStocks(character, hour())) {
			extra += 16;
		}
		return extra;
	}

	/**
	 * Old saves still have everyone at 100, because that is the base value.
	 * Give those people a lived-in number for the hour the save is on.
	 * Anyone already off 100, or still wearing a fresh-wash status, is left alone.
	 */
	public static void catchUpLoadedWorld() {
		if(Main.game==null) {
			return;
		}
		for(NPC npc : new ArrayList<>(Main.game.getAllNPCs())) {
			if(!tracks(npc) || hasFreshWash(npc) || Math.round(npc.getAttributeValue(Attribute.CLEANLINESS))<100) {
				continue;
			}
			applySpawnValue(npc);
		}
	}

	/** First value for someone who just entered the world. Loaded saves skip this and keep their number. */
	public static void applySpawnValue(GameCharacter character) {
		if(!tracks(character) || character.isPlayer()) {
			return;
		}
		int hour = hour();
		if(isStocks(character, hour)) {
			set(character, 18+spread(character, 23));
			return;
		}
		if(isSexWorker(character, hour)) {
			set(character, mayWash(character, hour) ? 84+spread(character, 15) : 40+spread(character, 36));
			return;
		}
		if(isMaid(character, hour)) {
			set(character, mayWash(character, hour) ? 94+spread(character, 7) : 45+spread(character, 21));
			return;
		}
		if(!mayWash(character, hour)) {
			set(character, 40+spread(character, 36));
			return;
		}
		int sinceWash = hour>=7 ? hour-7 : hour+17;
		int start = isShopkeeper(character) ? 94+spread(character, 7) : 80+spread(character, 21);
		int perHour = 4+(spread(character, 3)==2 ? 1 : 0);
		set(character, start-sinceWash*perHour);
	}

	/**
	 * Once an hour, for someone who is not in the room with the player.
	 * Sex workers wash between clients. Maids stay tidy if they are allowed to.
	 * Everyone else who can wash does it at 07:00 and then sweats through the day.
	 */
	public static void applyHour(GameCharacter character, int hour, boolean withPlayer) {
		if(!tracks(character) || character.isPlayer() || withPlayer || inScene(character)) {
			return;
		}
		if(isStocks(character, hour)) {
			return;
		}
		if(isSexWorker(character, hour)) {
			if(mayWash(character, hour)) {
				set(character, 84+spread(character, 15));
			}
			return;
		}
		if(isMaid(character, hour)) {
			if(mayWash(character, hour)) {
				if(value(character)<90) {
					set(character, 94+spread(character, 7));
				}
			} else if(value(character)<30) {
				set(character, 30+spread(character, 16));
			}
			return;
		}
		if(!mayWash(character, hour)) {
			return;
		}
		if(hour==7) {
			set(character, isShopkeeper(character) ? 94+spread(character, 7) : 80+spread(character, 21));
		}
	}

	public static boolean isOutInTheWorld(NPC npc) {
		if(npc==null || npc.isElemental()) {
			return false;
		}
		try {
			return !npc.getLocationPlace().getPlaceType().equals(PlaceType.GENERIC_EMPTY_TILE);
		} catch(Exception ex) {
			return false;
		}
	}

	private static boolean tracks(GameCharacter character) {
		return character!=null
				&& !character.isElemental()
				&& character.attributes!=null
				&& character.attributes.containsKey(Attribute.CLEANLINESS);
	}

	private static boolean hasFreshWash(GameCharacter character) {
		return character.hasStatusEffect(StatusEffect.CLEANED_SHOWER)
				|| character.hasStatusEffect(StatusEffect.CLEANED_BATH)
				|| character.hasStatusEffect(StatusEffect.CLEANED_SPA);
	}

	private static boolean inScene(GameCharacter character) {
		if(Main.game.isInSex() && Main.sex.getAllParticipants().contains(character)) {
			return true;
		}
		return Main.game.isInCombat() && Main.combat.getAllCombatants(true).contains(character);
	}

	private static boolean mayWash(GameCharacter character, int hour) {
		if(!character.isSlave()) {
			return true;
		}
		if(!character.hasSlavePermissionSetting(SlavePermissionSetting.CLEANLINESS_WASH_BODY)) {
			return false;
		}
		SlaveJob job = character.getSlaveJob(hour);
		return job==null || !job.hasFlag(SlaveJobFlag.CLEANING_UNAVAILABLE);
	}

	private static boolean isStocks(GameCharacter character, int hour) {
		if(character instanceof SlaveInStocks) {
			return true;
		}
		return character.isSlave() && character.getSlaveJob(hour)==SlaveJob.PUBLIC_STOCKS;
	}

	private static boolean isSexWorker(GameCharacter character, int hour) {
		if(character.getOccupation()==Occupation.NPC_PROSTITUTE) {
			return true;
		}
		return character.isSlave() && character.getSlaveJob(hour)==SlaveJob.PROSTITUTE;
	}

	private static boolean isMaid(GameCharacter character, int hour) {
		Occupation occupation = character.getOccupation();
		if(occupation==Occupation.MAID || occupation==Occupation.NPC_MAID || occupation==Occupation.NPC_BUTLER) {
			return true;
		}
		return character.isSlave() && character.getSlaveJob(hour)==SlaveJob.CLEANING;
	}

	private static boolean isShopkeeper(GameCharacter character) {
		if(character instanceof NPC && ((NPC) character).isTrader()) {
			return true;
		}
		Occupation occupation = character.getOccupation();
		return occupation==Occupation.NPC_STORE_OWNER
				|| occupation==Occupation.NPC_CLOTHING_STORE_OWNER
				|| occupation==Occupation.NPC_GYM_OWNER
				|| occupation==Occupation.NPC_CASINO_OWNER
				|| occupation==Occupation.NPC_BUSINESS_OWNER
				|| occupation==Occupation.NPC_TAVERN_OWNER
				|| occupation==Occupation.NPC_TRADER
				|| occupation==Occupation.NPC_BEAUTICIAN;
	}

	private static int hour() {
		return Main.game.getHourOfDay();
	}

	private static int value(GameCharacter character) {
		return Math.round(character.getAttributeValue(Attribute.CLEANLINESS));
	}

	private static void set(GameCharacter character, int amount) {
		if(!tracks(character)) {
			return;
		}
		if(amount<0) {
			amount = 0;
		} else if(amount>100) {
			amount = 100;
		}
		character.setAttribute(Attribute.CLEANLINESS, amount, false);
	}

	/** Stable per person, so two shopkeepers are not the same number. */
	private static int spread(GameCharacter character, int bound) {
		String id = character.getId();
		int hash = id==null ? character.getNameIgnoresPlayerKnowledge().hashCode() : id.hashCode();
		return Math.floorMod(hash, bound);
	}
}
