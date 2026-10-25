package com.lilithsthrone.game.dialogue.places.dominion.lilayashome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.effects.StatusEffect;
import com.lilithsthrone.game.character.npc.NPC;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.inventory.InventorySlot;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.DisplacementType;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.colours.PresetColour;
import com.lilithsthrone.game.dialogue.utils.UtilText;

/**
 * Interactive wash in the player's ensuite.
 * One scene, like sex: each response is an action, and the page is replaced by that action.
 */
public class BathDialogue {

	private static final int WASH_SECONDS = 2 * 60;
	private static final int UNCOVER_SECONDS = 60;

	private static String targetId;
	private static String lastProse;
	private static int soapIndex = -1;
	private static final Set<String> insistedIds = new HashSet<>();
	private static final Map<String, Integer> scrubs = new HashMap<>();
	private static final Set<String> scrubbed = new HashSet<>();
	private static final Set<String> internalDone = new HashSet<>();

	public static void start() {
		targetId = Main.game.getPlayer().getId();
		lastProse = null;
		soapIndex = -1;
		insistedIds.clear();
		scrubs.clear();
		scrubbed.clear();
		internalDone.clear();
	}

	public static final DialogueNode WASH = new DialogueNode("Bath", "", true) {
		@Override
		public boolean isContentParsed() {
			return false;
		}

		@Override
		public String getContent() {
			ensureSession();
			return UtilText.parse(target(), page());
		}

		@Override
		public String getResponseTabTitle(int index) {
			BathTab tab = tabAt(index);
			if(tab == null) {
				return null;
			}
			switch(tab) {
				case HEAD:
					return "Head";
				case BODY:
					return "Body";
				case LOWER:
					return "Lower";
				case EXTRA:
					return "Extra";
				case OPTIONS:
					return "Options";
				default:
					return null;
			}
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			ensureSession();
			if(index == 0) {
				return null;
			}
			BathTab tab = tabAt(responseTab);
			if(tab == null) {
				return null;
			}
			if(tab == BathTab.OPTIONS) {
				return optionsResponse(index);
			}
			List<BathArea> areas = areasOn(tab);
			if(index - 1 >= areas.size()) {
				return null;
			}
			return actionResponse(areas.get(index - 1));
		}
	};

	private static Response optionsResponse(int index) {
		GameCharacter target = target();
		if(mustInsist(target)) {
			if(index == 1) {
				return new Response("Insist", "They do not want this. Insist, and the wash can continue. This does not clean anything.", WASH) {
					@Override
					public void effects() {
						insist();
					}
					@Override
					public int getSecondsPassed() {
						return UNCOVER_SECONDS;
					}
				};
			}
			if(index == 2) {
				return leaveResponse();
			}
			if(index == 3) {
				return switchResponse();
			}
			if(index == 4) {
				return soapResponse();
			}
			return null;
		}
		if(index == 1) {
			final int washed = scrubsOf(target);
			final int minutes = Math.min(270, 30 + (10 * washed));
			return new Response("Finish",
					"Step out. " + minutes + " minutes of the bathed status, and no heal. Clothes are not washed.",
					RoomPlayer.ROOM) {
				@Override
				public void effects() {
					finish(target(), minutes);
				}
			};
		}
		if(index == 2) {
			return leaveResponse();
		}
		if(index == 3) {
			return switchResponse();
		}
		if(index == 4) {
			return soapResponse();
		}
		return null;
	}

	private static Response leaveResponse() {
		return new Response("Leave", "Step back into your room. Parts already washed stay clean. No bathed status.", RoomPlayer.ROOM);
	}

	private static Response switchResponse() {
		if(who().size() < 2) {
			return null;
		}
		return new Response("Switch", "Wash someone else who is here.", WASH) {
			@Override
			public void effects() {
				switchTarget();
			}
		};
	}

	private static Response soapResponse() {
		List<AbstractItem> soaps = soaps();
		if(soaps.isEmpty()) {
			return new Response("Soap", "You have no soap. Water still works.", null);
		}
		AbstractItem selected = selectedSoap();
		String using = selected == null ? "Water only." : "Using " + selected.getName(false, false) + ".";
		return new Response("Soap", using + " Cycle to the next bar, or back to water. The bar is not used up.", WASH) {
			@Override
			public void effects() {
				cycleSoap();
			}
		};
	}

	private static Response actionResponse(BathArea area) {
		GameCharacter target = target();
		Map.Entry<AbstractClothing, DisplacementType> blocking = area.blocking(target);
		if(blocking != null) {
			if(!canMove(target, blocking)) {
				return new Response(area.getUncoverTitle(), "That will not come off, so this part cannot be washed yet.", null);
			}
			String clothingName = blocking.getKey().getName(true);
			return new Response(area.getUncoverTitle(),
					"One minute. Moves " + clothingName + " aside. Does not wash it, and does not wash the part yet.",
					WASH) {
				@Override
				public void effects() {
					uncover(area);
				}
				@Override
				public int getSecondsPassed() {
					return UNCOVER_SECONDS;
				}
			};
		}
		String state;
		if(area.isFilthy(target)) {
			state = "Stained. Two minutes.";
		} else if(isSweaty(target, area)) {
			state = "Not stained, but not washed yet. Two minutes.";
		} else {
			state = "Already washed. Another pass still helps a little. Two minutes.";
		}
		return new Response(area.getWashTitle(), state, WASH) {
			@Override
			public void effects() {
				wash(area);
			}
			@Override
			public int getSecondsPassed() {
				return WASH_SECONDS;
			}
		};
	}

	private static void uncover(BathArea area) {
		GameCharacter target = target();
		Map.Entry<AbstractClothing, DisplacementType> blocking = area.blocking(target);
		if(blocking == null || !canMove(target, blocking)) {
			lastProse = "<p>There is nothing in the way.</p>";
			return;
		}
		AbstractClothing clothing = blocking.getKey();
		String clothingName = clothing.getName(true);
		DisplacementType displacement = blocking.getValue();
		GameCharacter player = Main.game.getPlayer();
		if(displacement == DisplacementType.REMOVE_OR_EQUIP) {
			target.unequipClothingIntoInventory(clothing, true, player);
		} else {
			target.isAbleToBeDisplaced(clothing, displacement, true, true, player);
		}
		lastProse = "<p>" + area.uncoverText(clothingName) + "</p>";
	}

	private static void wash(BathArea area) {
		GameCharacter target = target();
		if(area.blocking(target) != null) {
			lastProse = "<p>Clothing is still in the way.</p>";
			return;
		}
		boolean filthy = area.isFilthy(target);
		for(InventorySlot slot : area.getSlots()) {
			target.removeDirtySlot(slot, true);
		}
		for(SexAreaOrifice orifice : area.getOrifices()) {
			cleanOrifice(target, area, orifice);
		}
		AbstractItem soap = selectedSoap();
		int gain = filthy ? (soap == null ? 10 : 12) : (soap == null ? 2 : 4);
		target.incrementAttribute(Attribute.CLEANLINESS, gain);
		if(area.isIntimate()) {
			target.incrementArousal(5);
		}
		scrubs.put(target.getId(), scrubsOf(target) + 1);
		scrubbed.add(target.getId() + "|" + area.name());

		StringBuilder text = new StringBuilder();
		if(!target.isPlayer() && !isWilling(target)) {
			text.append("[npc.Name] tells you to stop. You do not. ");
		}
		text.append(area.washText());
		if(soap != null) {
			if("oyana_soap_unscented".equals(soap.getItemType().getId())) {
				text.append(" The unscented soap cuts through whatever was on [npc.her] skin.");
			} else {
				text.append(" The ").append(soap.getName(false, false)).append(" leaves its scent on [npc.her] skin.");
			}
		}
		lastProse = "<p>" + text + "</p>";
	}

	private static void cleanOrifice(GameCharacter target, BathArea area, SexAreaOrifice orifice) {
		if(orifice == SexAreaOrifice.MOUTH || !orifice.isInternalOrifice()) {
			target.clearFluidsStored(orifice);
			return;
		}
		String key = target.getId() + "|" + area.name() + "|" + orifice.name();
		if(internalDone.contains(key)) {
			target.clearFluidsStored(orifice);
		} else {
			target.drainTotalFluidsStored(orifice, 500);
			internalDone.add(key);
		}
	}

	private static void insist() {
		GameCharacter target = target();
		insistedIds.add(target.getId());
		lastProse = "<p>[npc.Name] does not want this. You insist, and [npc.she] stops arguing. The water is still running.</p>";
	}

	private static void switchTarget() {
		List<GameCharacter> people = who();
		int index = 0;
		String current = target().getId();
		for(int i = 0; i < people.size(); i++) {
			if(people.get(i).getId().equals(current)) {
				index = i;
				break;
			}
		}
		targetId = people.get((index + 1) % people.size()).getId();
		lastProse = null;
	}

	private static void cycleSoap() {
		List<AbstractItem> soaps = soaps();
		if(soaps.isEmpty()) {
			soapIndex = -1;
			lastProse = "<p>You have no soap. Water will do.</p>";
			return;
		}
		soapIndex++;
		if(soapIndex >= soaps.size()) {
			soapIndex = -1;
			lastProse = "<p>You set the soap aside. Water will do.</p>";
			return;
		}
		lastProse = "<p>You pick up the " + soaps.get(soapIndex).getName(false, false) + ". It will not be used up.</p>";
	}

	private static void finish(GameCharacter target, int minutes) {
		target.removeStatusEffect(StatusEffect.CLEANED_SHOWER);
		target.removeStatusEffect(StatusEffect.CLEANED_BATH);
		target.removeStatusEffect(StatusEffect.CLEANED_SPA);
		target.addStatusEffect(StatusEffect.CLEANED_BATH, minutes * 60);

		StringBuilder end = new StringBuilder();
		end.append(UtilText.parse(target,
				"<p>You step out of the water. [npc.Name] [npc.has] had a proper wash, and the warmth of it lingers.</p>"));
		if(!target.isPlayer()) {
			if(isWilling(target)) {
				end.append(target.incrementAffection(Main.game.getPlayer(), 1));
			} else {
				end.append(target.incrementAffection(Main.game.getPlayer(), -1));
				if(isOwnedSlave(target)) {
					end.append(target.incrementObedience(-1, false));
				}
			}
		}
		Main.game.getTextEndStringBuilder().append(end.toString());
	}

	private static String page() {
		GameCharacter target = target();
		int cleanliness = Math.round(target.getAttributeValue(Attribute.CLEANLINESS));
		String mood;
		if(target.isPlayer()) {
			mood = "This is you";
		} else if(isWilling(target)) {
			mood = "Willing";
		} else {
			mood = "Unwilling";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<p style='text-align:center;'><b style='color:")
				.append(PresetColour.ATTRIBUTE_CLEANLINESS.toWebHexString())
				.append(";'>[npc.Name]</b> — cleanliness ")
				.append(cleanliness)
				.append(". ")
				.append(mood)
				.append(".</p>");
		if(lastProse == null) {
			sb.append(opening(target));
		} else {
			sb.append(lastProse);
		}
		return sb.toString();
	}

	private static String opening(GameCharacter target) {
		List<String> filthy = new ArrayList<>();
		for(BathArea area : BathArea.values()) {
			if(area.isAvailable(target) && area.isFilthy(target)) {
				filthy.add(area.getPartTag());
			}
		}
		String whoLine;
		if(target.isPlayer()) {
			whoLine = "You step into the ensuite and start the water.";
		} else if(mustInsist(target)) {
			whoLine = "[npc.Name] is here, and [npc.she] does not want you washing [npc.herHim].";
		} else if(isWilling(target)) {
			whoLine = "[npc.Name] joins you in the ensuite, and the water is already running.";
		} else {
			whoLine = "[npc.Name] is in the water with you, and [npc.she] is not happy about it.";
		}
		String dirt;
		if(filthy.isEmpty()) {
			dirt = " Nothing on [npc.herHim] is stained. A wash will still freshen [npc.her] skin.";
		} else {
			dirt = " Stained: " + Util.stringsToStringList(filthy, false) + ".";
		}
		AbstractItem soap = selectedSoap();
		String soapLine = soap == null
				? ""
				: " The " + soap.getName(false, false) + " is within reach.";
		return "<p>" + whoLine + dirt + soapLine + "</p>";
	}

	private static boolean isSweaty(GameCharacter target, BathArea area) {
		return target.getAttributeValue(Attribute.CLEANLINESS) < 70
				&& !scrubbed.contains(target.getId() + "|" + area.name());
	}

	private static boolean canMove(GameCharacter target, Map.Entry<AbstractClothing, DisplacementType> blocking) {
		AbstractClothing clothing = blocking.getKey();
		GameCharacter player = Main.game.getPlayer();
		if(blocking.getValue() == DisplacementType.REMOVE_OR_EQUIP) {
			return target.isAbleToUnequip(clothing, true, player);
		}
		return target.isAbleToBeDisplaced(clothing, blocking.getValue(), false, true, player);
	}

	private static boolean mustInsist(GameCharacter target) {
		return !target.isPlayer() && !isOwnedSlave(target) && target.getAffection(Main.game.getPlayer()) < 0
				&& !insistedIds.contains(target.getId());
	}

	private static boolean isWilling(GameCharacter target) {
		if(target.isPlayer()) {
			return true;
		}
		if(isOwnedSlave(target)) {
			return target.getObedienceValue() >= 50;
		}
		return target.getAffection(Main.game.getPlayer()) >= 0;
	}

	private static boolean isOwnedSlave(GameCharacter target) {
		return target.isSlave() && target.getOwner() != null && target.getOwner().isPlayer();
	}

	private static int scrubsOf(GameCharacter target) {
		Integer count = scrubs.get(target.getId());
		return count == null ? 0 : count;
	}

	private static AbstractItem selectedSoap() {
		List<AbstractItem> soaps = soaps();
		if(soapIndex < 0 || soapIndex >= soaps.size()) {
			return null;
		}
		return soaps.get(soapIndex);
	}

	private static List<AbstractItem> soaps() {
		List<AbstractItem> found = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for(Map.Entry<AbstractItem, Integer> entry : Main.game.getPlayer().getAllItemsInInventory().entrySet()) {
			if(entry.getValue() == null || entry.getValue() <= 0) {
				continue;
			}
			AbstractItem item = entry.getKey();
			String id = item.getItemType().getId();
			if(id != null && id.startsWith("oyana_soap_") && seen.add(id)) {
				found.add(item);
			}
		}
		return found;
	}

	private static void ensureSession() {
		List<GameCharacter> people = who();
		GameCharacter found = null;
		if(targetId != null) {
			for(GameCharacter character : people) {
				if(character.getId().equals(targetId)) {
					found = character;
					break;
				}
			}
		}
		if(found == null) {
			found = Main.game.getPlayer();
			lastProse = null;
		}
		targetId = found.getId();
	}

	private static GameCharacter target() {
		if(targetId != null) {
			for(GameCharacter character : who()) {
				if(character.getId().equals(targetId)) {
					return character;
				}
			}
		}
		return Main.game.getPlayer();
	}

	private static List<GameCharacter> who() {
		GameCharacter player = Main.game.getPlayer();
		Map<String, GameCharacter> people = new HashMap<>();
		List<GameCharacter> ordered = new ArrayList<>();
		addPerson(ordered, people, player);
		for(GameCharacter companion : player.getCompanions()) {
			if(companion != null
					&& companion.getWorldLocation() == player.getWorldLocation()
					&& companion.getLocation().equals(player.getLocation())) {
				addPerson(ordered, people, companion);
			}
		}
		for(NPC present : LilayaHomeGeneric.getSlavesAndOccupantsPresent()) {
			addPerson(ordered, people, present);
		}
		return ordered;
	}

	private static void addPerson(List<GameCharacter> ordered, Map<String, GameCharacter> people, GameCharacter character) {
		if(character != null && people.put(character.getId(), character) == null) {
			ordered.add(character);
		}
	}

	private static boolean hasExtra() {
		for(BathArea area : BathArea.values()) {
			if(area.getTab() == BathTab.EXTRA && area.isAvailable(target())) {
				return true;
			}
		}
		return false;
	}

	private static BathTab tabAt(int index) {
		if(mustInsist(target())) {
			return index == 0 ? BathTab.OPTIONS : null;
		}
		if(index == 0) {
			return BathTab.HEAD;
		}
		if(index == 1) {
			return BathTab.BODY;
		}
		if(index == 2) {
			return BathTab.LOWER;
		}
		if(hasExtra()) {
			if(index == 3) {
				return BathTab.EXTRA;
			}
			if(index == 4) {
				return BathTab.OPTIONS;
			}
			return null;
		}
		if(index == 3) {
			return BathTab.OPTIONS;
		}
		return null;
	}

	private static List<BathArea> areasOn(BathTab tab) {
		GameCharacter target = target();
		List<BathArea> areas = new ArrayList<>();
		for(BathArea area : BathArea.values()) {
			if(area.getTab() == tab && area.isAvailable(target)) {
				areas.add(area);
			}
		}
		return areas;
	}
}
