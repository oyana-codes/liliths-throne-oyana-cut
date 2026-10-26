package com.lilithsthrone.game.bath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.body.valueEnums.GenitalArrangement;
import com.lilithsthrone.game.character.body.valueEnums.LegConfiguration;
import com.lilithsthrone.game.character.effects.StatusEffect;
import com.lilithsthrone.game.character.fetishes.Fetish;
import com.lilithsthrone.game.dialogue.places.dominion.lilayashome.BathArea;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.InventorySlot;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.DisplacementType;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.colours.PresetColour;

/**
 * One bathing scene. It follows the sex turn, and it is not a sex scene.
 * The page keeps every action. Each non-player present, once added, takes their own action afterwards.
 * Clothes are taken off into the bag before anyone gets in, and they are not washed.
 */
public class Bath {

	private static boolean started;
	private static int turn = 1;
	private static String description = "";
	private static BathFacility facility = BathFacility.BOTH;
	private static BathManager manager = new DefaultBathManager();
	private static BathAction lastPlayerAction;
	private static int soapIndex = -1;
	private static int strippedCount;
	private static int droppedCount;
	private static boolean keptJewellery;

	private static final List<GameCharacter> participants = new ArrayList<>();
	private static final Map<String, BathPosition> positions = new HashMap<>();
	private static final Map<String, BathPace> paces = new HashMap<>();
	private static final Map<String, Integer> scrubs = new HashMap<>();
	private static final Set<String> scrubbed = new HashSet<>();
	private static final Set<String> internalDone = new HashSet<>();
	private static final Set<String> insisted = new HashSet<>();
	private static final Set<String> performed = new HashSet<>();
	private static final List<String> stuckNames = new ArrayList<>();
	private static final Map<String, BathPace> pressPaces = new HashMap<>();
	private static final Set<String> edged = new HashSet<>();
	/** Held at 100 until the player chooses orgasm, prepare, or deny. A wash does not climax by itself. */
	private static final Map<String, PendingClimax> pending = new HashMap<>();
	private static boolean paceChosen;

	/** The wash that crossed 100. Resolved only when the player chooses to cum. */
	private static final class PendingClimax {
		private final BathArea area;
		private final BathPace pace;
		private final boolean deep;

		private PendingClimax(BathArea area, BathPace pace, boolean deep) {
			this.area = area;
			this.pace = pace;
			this.deep = deep;
		}
	}

	/** One soak adds every submerged part, then stops here so a full body does not climax in a single dunk. */
	private static final float SOAK_AROUSAL_CAP = 38f;

	private Bath() {
	}

	public static void start(BathFacility place) {
		start(place, new DefaultBathManager());
	}

	public static void start(BathFacility place, BathManager bathManager) {
		started = true;
		turn = 1;
		facility = place == null ? BathFacility.BOTH : place;
		manager = bathManager == null ? new DefaultBathManager() : bathManager;
		lastPlayerAction = null;
		soapIndex = -1;
		strippedCount = 0;
		droppedCount = 0;
		keptJewellery = false;
		participants.clear();
		positions.clear();
		paces.clear();
		scrubs.clear();
		scrubbed.clear();
		internalDone.clear();
		insisted.clear();
		performed.clear();
		stuckNames.clear();
		pressPaces.clear();
		edged.clear();
		pending.clear();
		paceChosen = false;

		GameCharacter player = Main.game.getPlayer();
		addParticipant(player);
		BathPosition openingPosition = facility.hasShower() ? BathPosition.STANDING : BathPosition.SITTING;
		setPosition(player, openingPosition);
		strip(player);
		description = opening(player);
		Main.game.setResponseTab(0);
		Main.game.setResponsePage(0);
	}

	public static boolean isStarted() {
		return started;
	}

	public static boolean isContinuing() {
		return started && turn > 1;
	}

	public static String getDescription() {
		return description == null ? "" : description;
	}

	public static String getSceneLabel() {
		BathPosition position = getPosition(Main.game.getPlayer());
		if(position == BathPosition.STANDING && !facility.hasShower()) {
			return "Standing wash";
		}
		return position.getSceneLabel();
	}

	public static BathFacility getFacility() {
		return facility;
	}

	public static void setManager(BathManager bathManager) {
		manager = bathManager == null ? new DefaultBathManager() : bathManager;
	}

	public static List<GameCharacter> getParticipants() {
		return Collections.unmodifiableList(participants);
	}

	/** Another person in the water. Their position and pace are their own. Solo play adds nobody. A full room refuses them. */
	public static void addParticipant(GameCharacter character) {
		if(!hasSpaceFor(character)) {
			return;
		}
		for(GameCharacter existing : participants) {
			if(existing.getId().equals(character.getId())) {
				return;
			}
		}
		participants.add(character);
		if(!positions.containsKey(character.getId())) {
			positions.put(character.getId(), getPosition(Main.game.getPlayer()));
		}
		if(!paces.containsKey(character.getId())) {
			paces.put(character.getId(), BathPace.NORMAL);
		}
	}

	/** True when this character is already in the water, or the room still has a free place. */
	public static boolean hasSpaceFor(GameCharacter character) {
		if(character == null) {
			return false;
		}
		for(GameCharacter existing : participants) {
			if(existing.getId().equals(character.getId())) {
				return true;
			}
		}
		return participants.size() < facility.getCapacity();
	}

	/**
	 * A taur or feral body in a room that was not built for one.
	 * The rear half can still be soaked or sprayed, but it comes out less clean. Someone else helping avoids that.
	 */
	public static boolean isCramped(GameCharacter character) {
		if(character == null || facility.fitsTaur()) {
			return false;
		}
		if(character.isFeral()) {
			return true;
		}
		return character.getLegConfiguration() != LegConfiguration.BIPEDAL;
	}

	public static boolean isPaceChosen() {
		return paceChosen;
	}

	public static boolean isParticipant(GameCharacter character) {
		if(character == null) {
			return false;
		}
		for(GameCharacter existing : participants) {
			if(existing.getId().equals(character.getId())) {
				return true;
			}
		}
		return false;
	}

	/** True once a wash has brought this character to the edge and they can still choose how it ends. */
	public static boolean isReadyToOrgasm(GameCharacter character) {
		return character != null && pending.containsKey(character.getId());
	}

	/** Pace of the wash that put this character on the edge. Null when they are not waiting. */
	public static BathPace getPendingPace(GameCharacter character) {
		PendingClimax held = character == null ? null : pending.get(character.getId());
		return held == null ? null : held.pace;
	}

	/** The pace picked at the start, and the pace of a first wash. It does not wash anything. */
	public static void chooseOpeningPace(BathPace pace) {
		GameCharacter player = Main.game.getPlayer();
		setPace(player, pace == null ? BathPace.NORMAL : pace);
		paceChosen = true;
		commitTurn("<p>" + UtilText.parse(player, openingPaceLine(getPace(player))) + "</p>");
	}

	public static boolean hasPerformed(GameCharacter character, String actionId) {
		if(character == null || actionId == null) {
			return false;
		}
		return performed.contains(character.getId() + "|" + actionId);
	}

	public static boolean hasHelperFor(GameCharacter character) {
		for(GameCharacter other : participants) {
			if(!other.getId().equals(character.getId())) {
				return true;
			}
		}
		return false;
	}

	public static BathPosition getPosition(GameCharacter character) {
		if(character == null) {
			return BathPosition.STANDING;
		}
		BathPosition position = positions.get(character.getId());
		return position == null ? BathPosition.STANDING : position;
	}

	public static void setPosition(GameCharacter character, BathPosition position) {
		if(character == null || position == null) {
			return;
		}
		positions.put(character.getId(), position);
	}

	public static BathPace getPace(GameCharacter character) {
		if(character == null) {
			return BathPace.NORMAL;
		}
		BathPace pace = paces.get(character.getId());
		return pace == null ? BathPace.NORMAL : pace;
	}

	public static void setPace(GameCharacter character, BathPace pace) {
		if(character == null || pace == null) {
			return;
		}
		paces.put(character.getId(), pace);
	}

	public static boolean isPositionAllowed(GameCharacter character, BathPosition position) {
		switch(position) {
			case STANDING:
			case SITTING:
				return true;
			case LEG_UP:
				return character.getLegConfiguration() == LegConfiguration.BIPEDAL && character.hasLegs();
			case SOAKING:
				return facility.hasBath();
			case HANDHELD:
				return facility.hasHandheld();
			default:
				return false;
		}
	}

	public static boolean canReach(GameCharacter character, BathArea area) {
		if(area == null || !area.isAvailable(character)) {
			return false;
		}
		if(hasHelperFor(character)) {
			return true;
		}
		BathPosition where = getPosition(character);
		if(area.isHardToReach(character)) {
			return where.reachesHardParts();
		}
		if(area.isGroinOrRear() && !where.allowsHandsOnGroin()) {
			return false;
		}
		return true;
	}

	public static String reachReason(GameCharacter character, BathArea area) {
		if(area.isHardToReach(character)) {
			List<String> ways = new ArrayList<>();
			if(facility.hasBath()) {
				ways.add("soaking");
			}
			if(facility.hasHandheld()) {
				ways.add("a shower head");
			}
			ways.add("another person");
			return "Hands cannot reach that on this body. " + Util.capitaliseSentence(Util.stringsToStringChoice(ways, false)) + " can.";
		}
		return "Standing cannot reach that. Sit, prop a leg up, soak, or take the shower head.";
	}

	public static boolean isWilling(GameCharacter target) {
		if(target.isPlayer()) {
			return true;
		}
		if(isOwnedSlave(target)) {
			return target.getObedienceValue() >= 50;
		}
		return target.getAffection(Main.game.getPlayer()) >= 0;
	}

	/** A companion below affection 0 must be insisted once. Owned slaves never do. Not used while the scene is solo. */
	public static boolean mustInsist(GameCharacter target) {
		return !target.isPlayer()
				&& !isOwnedSlave(target)
				&& target.getAffection(Main.game.getPlayer()) < 0
				&& !insisted.contains(target.getId());
	}

	public static void insist(GameCharacter target) {
		if(target != null) {
			insisted.add(target.getId());
		}
	}

	public static boolean hasScrubbed(GameCharacter target, BathArea area) {
		return scrubbed.contains(target.getId() + "|" + area.name());
	}

	public static void perform(BathAction action) {
		perform(action, null);
	}

	/**
	 * @param paceForThisPress The pace of this one press. Null uses the pace chosen at the start, or the pace tab after that.
	 *        A repeated wash does not change that default.
	 */
	public static void perform(BathAction action, BathPace paceForThisPress) {
		GameCharacter player = Main.game.getPlayer();
		lastPlayerAction = action;
		String inner = action.resolveInner(player, paceForThisPress);
		if(paceForThisPress == null) {
			pressPaces.remove(player.getId());
		} else {
			pressPaces.put(player.getId(), paceForThisPress);
		}
		String extra;
		try {
			extra = action.apply(player);
		} finally {
			pressPaces.remove(player.getId());
		}
		commitTurn("<p>" + inner + "</p>" + extra);
	}

	public static void uncover(BathArea area) {
		GameCharacter player = Main.game.getPlayer();
		lastPlayerAction = null;
		Map.Entry<AbstractClothing, DisplacementType> blocking = area.blocking(player);
		if(blocking == null) {
			commitTurn("<p>" + UtilText.parse(player, "There is nothing in the way.") + "</p>");
			return;
		}
		AbstractClothing clothing = blocking.getKey();
		String name = clothing.getName(false, false);
		if(blocking.getValue() == DisplacementType.REMOVE_OR_EQUIP) {
			if(!player.isAbleToUnequip(clothing, true, player)) {
				commitTurn("<p>That will not come off.</p>");
				return;
			}
			player.unequipClothingIntoInventory(clothing, true, player);
		} else {
			player.isAbleToBeDisplaced(clothing, blocking.getValue(), true, true, player);
		}
		commitTurn("<p>" + UtilText.parse(player, "[npc.Name] [npc.verb(move)] the " + name + " aside. It is not washed.") + "</p>");
	}

	public static void cycleSoap() {
		GameCharacter player = Main.game.getPlayer();
		lastPlayerAction = null;
		List<AbstractItem> soaps = soaps();
		String text;
		if(soaps.isEmpty()) {
			soapIndex = -1;
			text = "There is no soap in the bag. Water will do.";
		} else {
			soapIndex++;
			if(soapIndex >= soaps.size()) {
				soapIndex = -1;
				text = "[npc.Name] [npc.verb(set)] the soap aside. Water will do.";
			} else {
				text = "[npc.Name] [npc.verb(pick)] up the " + soaps.get(soapIndex).getName(false, false) + ". It will not be used up.";
			}
		}
		commitTurn("<p>" + UtilText.parse(player, text) + "</p>");
	}

	public static AbstractItem selectedSoap() {
		List<AbstractItem> soaps = soaps();
		if(soapIndex < 0 || soapIndex >= soaps.size()) {
			return null;
		}
		return soaps.get(soapIndex);
	}

	public static List<AbstractItem> soaps() {
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

	public static void finish() {
		StringBuilder end = new StringBuilder();
		for(GameCharacter target : new ArrayList<>(participants)) {
			int minutes = bathedMinutes(target);
			target.removeStatusEffect(StatusEffect.CLEANED_SHOWER);
			target.removeStatusEffect(StatusEffect.CLEANED_BATH);
			target.removeStatusEffect(StatusEffect.CLEANED_SPA);
			target.addStatusEffect(StatusEffect.CLEANED_BATH, minutes * 60);
			end.append(UtilText.parse(target, "<p>[npc.Name] [npc.verb(step)] out of the water. The warmth of the wash lingers.</p>"));
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
		}
		end.append(clothesExitLine());
		Main.game.getTextEndStringBuilder().append(end.toString());
		endSession();
	}

	public static void leave() {
		Main.game.getTextEndStringBuilder().append(UtilText.parse(Main.game.getPlayer(),
				"<p>[npc.Name] [npc.verb(step)] back into the room. No bathed status is gained.</p>"));
		Main.game.getTextEndStringBuilder().append(clothesExitLine());
		endSession();
	}

	static String washArea(GameCharacter target, BathArea area) {
		if(area == null || !area.isAvailable(target) || area.blocking(target) != null) {
			return "";
		}
		boolean filthy = area.isFilthy(target);
		clearArea(target, area);
		addCleanliness(target, filthy, area.isHardToReach(target));
		String heat = applyHeat(target, area, arousalGain(target, area, false), false);
		markScrub(target, area);
		return heat;
	}

	/** Empties the inside of an orifice. A plug or a seal means this does nothing. */
	static String deepClean(GameCharacter target, BathArea area) {
		if(area == null || !area.supportsDeepClean(target) || area.isDeepBlocked(target) || area.blocking(target) != null) {
			return "";
		}
		boolean filthy = area.isFilthy(target);
		for(InventorySlot slot : area.getSlots()) {
			target.removeDirtySlot(slot, true);
		}
		for(SexAreaOrifice orifice : area.getDeepOrifices()) {
			target.clearFluidsStored(orifice);
			internalDone.add(target.getId() + "|" + area.name() + "|" + orifice.name());
		}
		addCleanliness(target, filthy, area.isHardToReach(target));
		String heat = applyHeat(target, area, arousalGain(target, area, true), true);
		markScrub(target, area);
		return heat;
	}

	static String soak(GameCharacter target) {
		float total = 0f;
		float hottestGain = -1f;
		BathArea hottest = null;
		int parts = 0;
		for(BathArea area : BathArea.values()) {
			if(!area.isAvailable(target) || !area.isSubmerged() || area.blocking(target) != null) {
				continue;
			}
			boolean filthy = area.isFilthy(target);
			clearArea(target, area);
			addCleanliness(target, filthy, area.isHardToReach(target));
			markScrub(target, area);
			parts++;
			float gain = arousalGain(target, area, false);
			total += gain;
			if(gain > hottestGain) {
				hottestGain = gain;
				hottest = area;
			}
		}
		if(parts == 0 || total <= 0f || hottest == null) {
			return "";
		}
		return applyHeat(target, hottest, Math.min(SOAK_AROUSAL_CAP, total), false);
	}

	/** True when washing this part can raise arousal. Armpits count only with a liking for them. */
	static boolean stirs(GameCharacter target, BathArea area) {
		if(target == null || area == null) {
			return false;
		}
		return baseArousal(target, area, false) > 0f || baseArousal(target, area, true) > 0f;
	}

	private static BathPace paceOf(GameCharacter target) {
		BathPace pressed = pressPaces.get(target.getId());
		return pressed != null ? pressed : getPace(target);
	}

	private static float arousalGain(GameCharacter target, BathArea area, boolean deep) {
		float base = baseArousal(target, area, deep);
		if(base <= 0f) {
			return 0f;
		}
		BathPace pace = paceOf(target);
		float paceMultiplier = pace == BathPace.EAGER ? 1.6f : pace == BathPace.ROUGH ? 1.3f : 1f;
		float fetishMultiplier = likesThis(target, area, deep) ? 1.25f : 1f;
		return base * paceMultiplier * fetishMultiplier * target.getLustLevel().getArousalModifier();
	}

	private static float baseArousal(GameCharacter target, BathArea area, boolean deep) {
		switch(area) {
			case COCK:
			case PUSSY:
			case ASS:
				return 14f + (deep ? 8f : 0f);
			case MOUND:
				return 14f;
			case CHEST:
			case CROTCH:
			case SPINNERET:
				return 10f + (deep ? 8f : 0f);
			case THIGHS:
				return 5f;
			case ARMPITS:
				if(target.getFetishDesire(Fetish.FETISH_ARMPIT_RECEIVING).isPositive()) {
					return 8f;
				}
				return 0f;
			default:
				return 0f;
		}
	}

	private static boolean likesThis(GameCharacter target, BathArea area, boolean deep) {
		switch(area) {
			case COCK:
				return target.getFetishDesire(Fetish.FETISH_PENIS_RECEIVING).isPositive();
			case PUSSY:
				return target.getFetishDesire(Fetish.FETISH_VAGINAL_RECEIVING).isPositive();
			case ASS:
				return target.getFetishDesire(Fetish.FETISH_ANAL_RECEIVING).isPositive();
			case CHEST:
			case CROTCH:
				if(target.getFetishDesire(Fetish.FETISH_BREASTS_SELF).isPositive()) {
					return true;
				}
				return deep && target.getFetishDesire(Fetish.FETISH_LACTATION_SELF).isPositive();
			case ARMPITS:
				return target.getFetishDesire(Fetish.FETISH_ARMPIT_RECEIVING).isPositive();
			default:
				return false;
		}
	}

	private static String applyHeat(GameCharacter target, BathArea area, float gain, boolean deep) {
		if(gain <= 0f || area == null) {
			return "";
		}
		target.incrementArousal(gain);
		if(!target.isAbleToOrgasm()) {
			if(target.getArousal() >= 95f && edged.add(target.getId())) {
				return edgeText(target);
			}
			return "";
		}
		if(target.getArousal() >= 100f) {
			// Someone else in the water still finishes on their own. Only the player gets the orgasm menu.
			if(!target.isPlayer()) {
				return climax(target, area, paceOf(target), deep);
			}
			target.setArousal(100f);
			BathPace pace = paceOf(target);
			boolean alreadyWaiting = pending.containsKey(target.getId());
			pending.put(target.getId(), new PendingClimax(area, pace, deep));
			if(alreadyWaiting) {
				return "";
			}
			return brinkText(target, area, pace);
		}
		return "";
	}

	/** Let the held climax happen. Counts and fluids stay in climax. */
	public static void orgasm() {
		GameCharacter player = Main.game.getPlayer();
		PendingClimax held = pending.remove(player.getId());
		if(held == null) {
			commitTurn("<p>" + UtilText.parse(player, "The feeling has already passed.") + "</p>");
			return;
		}
		commitTurn(climax(player, held.area, held.pace, held.deep));
	}

	/** Stay at the edge. Repeatable. Does not count as an orgasm and does not spend anything. */
	public static void prepare() {
		GameCharacter player = Main.game.getPlayer();
		PendingClimax held = pending.get(player.getId());
		if(held == null) {
			commitTurn("<p>" + UtilText.parse(player, "The feeling has already passed.") + "</p>");
			return;
		}
		player.setArousal(100f);
		commitTurn(prepareText(player, held));
	}

	/**
	 * Step back from the edge. Arousal drops by 30, matching the sex denial.
	 * It does not count as an orgasm, does not apply fluids, and does not apply the frustrated status.
	 */
	public static void deny() {
		GameCharacter player = Main.game.getPlayer();
		pending.remove(player.getId());
		player.incrementArousal(-30f);
		commitTurn(denyText(player));
	}

	/**
	 * A bath climax is not a sex scene. Counts and fluids are recorded on the character.
	 * Sex.incrementNumberOfOrgasms needs an active sex scene, so it is not called.
	 */
	private static String climax(GameCharacter target, BathArea area, BathPace pace, boolean deep) {
		target.setLastTimeOrgasmedSeconds(Main.game.getSecondsPassed());
		target.incrementDaysOrgasmCount(1);
		target.incrementTotalOrgasmCount(1);
		target.setArousal(target.getLust() / 4f);

		boolean canCum = target.hasPenisIgnoreDildo() && target.getPenisRawCumStorageValue() > 0;
		int cumAmount = 0;
		if(canCum) {
			cumAmount = target.getPenisRawOrgasmCumQuantity();
			target.applyOrgasmCumEffect(1f, false);
		}
		boolean condom = cumAmount > 0 && target.isWearingCondom();

		boolean squirted = false;
		if(target.hasVagina()) {
			int amount = target.getVaginaWetness().getValue();
			if(target.isVaginaSquirter()) {
				amount = Math.max(1, amount) * 5;
			}
			if(amount > 0) {
				target.addFluidStored(SexAreaOrifice.VAGINA, new FluidStored(target.getId(), target.getGirlcum(), amount));
				squirted = true;
			}
		}

		boolean milk = beadOfMilk(target, area);
		messJustWashed(target, area);

		StringBuilder raw = new StringBuilder();
		String part = partPhrase(target, area);
		if(pace == BathPace.EAGER) {
			raw.append("[npc.Name] [npc.verb(keep)] [npc.her] hands on [npc.her] ").append(part)
					.append(", lingering, until it crests. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(Do not stop. I am coming.)] ");
		} else if(pace == BathPace.ROUGH) {
			raw.append("The rough pass over [npc.her] ").append(part)
					.append(" hits too hard to hold. [npc.She] [npc.verb(let)] out [npc.a_moan+] as [npc.she] [npc.verb(come)]. [npc.speech(Too much. I am coming.)] ");
		} else {
			raw.append("The steady wash over [npc.her] ").append(part)
					.append(" catches [npc.herHim] at the crest. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(Oh. That did it.)] ");
		}
		if(deep) {
			raw.append("The deeper rinse is what pushes [npc.herHim] over. ");
		}
		if(condom) {
			raw.append("The condom catches [npc.her] [npc.cum], and it stays on. ");
		} else if(cumAmount > 0) {
			if(isCloaca(target)) {
				raw.append("[npc.Her] [npc.cum] spills from [npc.her] cloaca over the skin [npc.she] just washed. ");
			} else {
				raw.append("[npc.Her] [npc.cum] spills over the skin [npc.she] just washed. ");
			}
		}
		if(squirted) {
			if(isCloaca(target)) {
				raw.append("A rush of [npc.girlcum] wells out of [npc.her] cloaca. ");
			} else {
				raw.append("A rush of [npc.girlcum] wells out of [npc.her] [npc.pussy]. ");
			}
		}
		if(milk && area == BathArea.CHEST) {
			raw.append("A bead of [npc.milk] wells at [npc.her] [npc.nipples]. ");
		} else if(milk && area == BathArea.CROTCH) {
			raw.append("A bead of [npc.crotchMilk] wells at [npc.her] [npc.nipplesCrotch]. ");
		}
		if(cumAmount <= 0 && !squirted && !milk) {
			raw.append("Heat breaks through [npc.herHim], with nothing to show for it but the shaking. ");
		}
		return "<p>" + UtilText.parse(target, raw.toString().trim()) + "</p>";
	}

	private static String brinkText(GameCharacter target, BathArea area, BathPace pace) {
		String part = partPhrase(target, area);
		String raw;
		if(pace == BathPace.EAGER) {
			raw = "[npc.Name] [npc.verb(keep)] going over [npc.her] " + part
					+ ", and the heat snaps right up to the edge. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(I am about to come.)]";
		} else if(pace == BathPace.ROUGH) {
			raw = "The rough pass over [npc.her] " + part
					+ " shoves [npc.herHim] right up to the edge. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(I am about to come.)]";
		} else {
			raw = "The steady wash over [npc.her] " + part
					+ " brings [npc.herHim] right up to the edge. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(I am about to come.)]";
		}
		return "<p>" + UtilText.parse(target, raw) + "</p>";
	}

	private static String prepareText(GameCharacter target, PendingClimax held) {
		String part = partPhrase(target, held.area);
		String raw;
		if(held.pace == BathPace.EAGER) {
			raw = "[npc.Name] [npc.verb(hold)] the feeling at the crest of [npc.her] " + part
					+ ", shaking, and [npc.verb(refuse)] to let it break. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(Not yet. Hold it.)]";
		} else if(held.pace == BathPace.ROUGH) {
			raw = "[npc.Name] [npc.verb(push)] through the rough heat over [npc.her] " + part
					+ " and [npc.verb(hold)] it there. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(Not yet.)]";
		} else {
			raw = "[npc.Name] [npc.verb(pause)] with [npc.her] hands on [npc.her] " + part
					+ " and [npc.verb(hold)] the feeling right on the edge. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(Not yet.)]";
		}
		return "<p>" + UtilText.parse(target, raw) + "</p>";
	}

	private static String denyText(GameCharacter target) {
		String raw;
		if(target.getFetishDesire(Fetish.FETISH_DENIAL_SELF).isPositive()) {
			raw = "[npc.Name] [npc.verb(pull)] back at the last moment, and the denied crest aches in the best way. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(No. I want to stay like this.)]";
		} else {
			raw = "[npc.Name] [npc.verb(pull)] [npc.her] hands away and [npc.verb(let)] the crest fall. [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(Not this time.)]";
		}
		return "<p>" + UtilText.parse(target, raw) + "</p>";
	}

	private static String edgeText(GameCharacter target) {
		String raw = "[npc.Name] [npc.verb(shake)] and [npc.verb(hold)] still, right on the edge. Whatever [npc.she] still [npc.verb(have)] on will not let [npc.herHim] finish."
				+ " [npc.She] [npc.verb(let)] out [npc.a_moan+]. [npc.speech(I cannot. It will not let me.)]";
		return "<p>" + UtilText.parse(target, raw) + "</p>";
	}

	private static boolean beadOfMilk(GameCharacter target, BathArea area) {
		if(area == BathArea.CHEST) {
			float stored = target.getBreastRawStoredMilkValue();
			if(stored <= 0f) {
				return false;
			}
			float bead = Math.min(5f, stored);
			target.addFluidStored(SexAreaOrifice.NIPPLE, new FluidStored(target.getId(), target.getMilk(), bead));
			target.setBreastStoredMilk(stored - bead);
			return true;
		}
		if(area == BathArea.CROTCH) {
			float stored = target.getBreastCrotchRawStoredMilkValue();
			if(stored <= 0f) {
				return false;
			}
			float bead = Math.min(5f, stored);
			target.addFluidStored(SexAreaOrifice.NIPPLE_CROTCH, new FluidStored(target.getId(), target.getMilkCrotch(), bead));
			target.setBreastCrotchStoredMilk(stored - bead);
			return true;
		}
		return false;
	}

	/** The climax soils the part that was just washed. Two slots at most, so the cleanliness dip stays small. */
	private static void messJustWashed(GameCharacter target, BathArea area) {
		int dirtied = 0;
		for(InventorySlot slot : area.getSlots()) {
			if(dirtied >= 2) {
				break;
			}
			if(target.addDirtySlot(slot)) {
				dirtied++;
			}
		}
	}

	private static boolean isCloaca(GameCharacter target) {
		GenitalArrangement arrangement = target.getGenitalArrangement();
		return arrangement == GenitalArrangement.CLOACA || arrangement == GenitalArrangement.CLOACA_BEHIND;
	}

	private static String partPhrase(GameCharacter target, BathArea area) {
		boolean cloaca = isCloaca(target);
		switch(area) {
			case COCK:
				return cloaca ? "[npc.assCloaca]" : "[npc.cock]";
			case PUSSY:
				return cloaca ? "[npc.assCloaca]" : "[npc.pussy]";
			case MOUND:
				return cloaca ? "[npc.assCloaca]" : "mound";
			case ASS:
				if(cloaca) {
					return "[npc.assCloaca]";
				}
				if(!Main.game.isAnalContentEnabled()) {
					return "rear";
				}
				return "[npc.ass] and [npc.anus]";
			case CHEST:
				return "[npc.breasts]";
			case CROTCH:
				return "[npc.breastsCrotch]";
			case SPINNERET:
				return "[npc.spinneret]";
			case THIGHS:
				return "thighs";
			case ARMPITS:
				return "[npc.armpits]";
			default:
				return "skin";
		}
	}

	public static int bathedMinutes(GameCharacter target) {
		return Math.min(270, 30 + (10 * scrubsOf(target)));
	}

	private static void commitTurn(String parsedHtml) {
		StringBuilder sb = new StringBuilder(parsedHtml);
		if(manager != null) {
			for(GameCharacter other : new ArrayList<>(participants)) {
				if(other.isPlayer()) {
					continue;
				}
				BathAction npcAction = manager.chooseAction(other, lastPlayerAction);
				if(npcAction == null) {
					continue;
				}
				String inner = npcAction.resolveInner(other);
				String extra = npcAction.apply(other);
				sb.append("<br/><p><span style='color:")
						.append(PresetColour.TEXT_GREY.toWebHexString())
						.append(";'>&gt; ")
						.append(UtilText.parse(other, "[npc.Name]"))
						.append(": ")
						.append(UtilText.parse(other, npcAction.getTitle()))
						.append("</span><br/>")
						.append(inner)
						.append("</p>")
						.append(extra);
			}
		}
		description = sb.toString();
		turn++;
	}

	private static void strip(GameCharacter character) {
		List<AbstractClothing> equipped = new ArrayList<>(character.getClothingCurrentlyEquipped());
		equipped.sort((a, b) -> a.getSlotEquippedTo().getZLayer() - b.getSlotEquippedTo().getZLayer());
		for(AbstractClothing clothing : equipped) {
			if(clothing.getSlotEquippedTo().isJewellery()) {
				keptJewellery = true;
				continue;
			}
			if(!character.isAbleToUnequip(clothing, true, character)) {
				stuckNames.add(clothing.getName(false, false));
				continue;
			}
			boolean bagFull = character.isInventoryFull() && !character.hasClothing(clothing);
			character.unequipClothingIntoInventory(clothing, true, character);
			if(bagFull) {
				droppedCount++;
			} else {
				strippedCount++;
			}
		}
	}

	private static String opening(GameCharacter character) {
		StringBuilder raw = new StringBuilder();
		raw.append("<p>");
		raw.append(stripSentence());
		raw.append(" ");
		raw.append(facilitySentence());
		if(hasHardToReach(character)) {
			raw.append(" ");
			raw.append(reachSentence());
		}
		if(isCramped(character)) {
			raw.append(" ");
			raw.append("This body does not fit the room. The human half can be washed by hand. The rest comes out less clean, unless someone else is here to help.");
		}
		raw.append("</p>");
		raw.append("<p style='text-align:center;'><b style='color:")
				.append(PresetColour.ATTRIBUTE_CLEANLINESS.toWebHexString())
				.append(";'>Cleanliness ")
				.append(Math.round(character.getAttributeValue(Attribute.CLEANLINESS)))
				.append(".</b> ");
		List<String> filthy = new ArrayList<>();
		for(BathArea area : BathArea.values()) {
			if(area.isAvailable(character) && area.isFilthy(character)) {
				filthy.add(area.getPartTag());
			}
		}
		if(filthy.isEmpty()) {
			raw.append("Nothing is stained. A wash will still freshen [npc.her] skin.");
		} else {
			raw.append("Stained: ").append(Util.stringsToStringList(filthy, false)).append(".");
		}
		List<AbstractItem> soap = soaps();
		if(soap.isEmpty()) {
			raw.append(" There is no soap in the bag. Water will do.");
		} else {
			raw.append(" Soap in the bag can be picked up from the options, or water will do.");
		}
		raw.append(" Choose a pace to start. That pace is used the first time a part is washed. Wash it again, and it can be done regularly, eagerly, or roughly.");
		raw.append("</p>");
		return UtilText.parse(character, raw.toString());
	}

	private static String openingPaceLine(BathPace pace) {
		switch(pace) {
			case EAGER:
				return "[npc.Name] [npc.verb(step)] into the heat already smiling, and [npc.verb(let)] out [npc.a_moan+] at how good it feels."
						+ " [npc.speech(I am going to enjoy every minute of this.)]";
			case ROUGH:
				return "[npc.Name] [npc.verb(set)] [npc.her] jaw and [npc.verb(get)] under the water, already scrubbing at the first dirty patch [npc.she] [npc.verb(find)]."
						+ " [npc.speech(Get it off me. I am not lingering.)]";
			case NORMAL:
			default:
				return "[npc.Name] [npc.verb(settle)] into the water and [npc.verb(let)] it run warm before [npc.she] [npc.verb(start)]."
						+ " [npc.speech(Slow is fine. I want to be clean, not rushed.)]";
		}
	}

	private static String stripSentence() {
		String jewellery = keptJewellery ? " Jewellery stays on." : "";
		String stuck = stuckNames.isEmpty() ? "" : " Still worn, because it will not come off: " + Util.stringsToStringList(stuckNames, false) + ".";
		if(strippedCount == 0 && droppedCount == 0) {
			return "[npc.Name] [npc.has] nothing that needs to come off." + jewellery + stuck + " [npc.She] [npc.verb(step)] into the water.";
		}
		String where = droppedCount > 0 ? " What does not fit in the bag is left on the floor." : " It goes into [npc.her] bag, and it is not washed.";
		return "[npc.Name] [npc.verb(strip)] before getting in." + where + jewellery + stuck;
	}

	private static String facilitySentence() {
		return facility.describe();
	}

	private static boolean hasHardToReach(GameCharacter character) {
		for(BathArea area : BathArea.values()) {
			if(area.isAvailable(character) && area.isHardToReach(character)) {
				return true;
			}
		}
		return false;
	}

	private static String reachSentence() {
		List<String> ways = new ArrayList<>();
		if(facility.hasBath()) {
			ways.add("soaking");
		}
		if(facility.hasHandheld()) {
			ways.add("a shower head");
		}
		ways.add("another person");
		return "[npc.Her] hands cannot reach every part of this body. " + Util.capitaliseSentence(Util.stringsToStringChoice(ways, false)) + " can.";
	}

	private static String clothesExitLine() {
		String stuck = stuckNames.isEmpty() ? "" : " Still on: " + Util.stringsToStringList(stuckNames, false) + ".";
		String dropped = droppedCount > 0 ? " What did not fit in the bag is on the floor." : "";
		if(strippedCount == 0 && droppedCount == 0) {
			return "<p>What was washed stays clean." + stuck + "</p>";
		}
		return "<p>Clothes taken off were not washed." + dropped + stuck + " What was washed stays clean.</p>";
	}

	private static void clearArea(GameCharacter target, BathArea area) {
		for(com.lilithsthrone.game.inventory.InventorySlot slot : area.getSlots()) {
			target.removeDirtySlot(slot, true);
		}
		for(SexAreaOrifice orifice : area.getOrifices()) {
			cleanOrifice(target, area, orifice);
		}
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

	private static void addCleanliness(GameCharacter target, boolean filthy, boolean hardPart) {
		AbstractItem soap = selectedSoap();
		int gain = filthy ? (soap == null ? 10 : 12) : (soap == null ? 2 : 4);
		if(hardPart && isCramped(target) && !hasHelperFor(target)) {
			gain = Math.max(1, gain / 2);
		}
		target.incrementAttribute(Attribute.CLEANLINESS, gain);
	}

	private static void markScrub(GameCharacter target, BathArea area) {
		scrubbed.add(target.getId() + "|" + area.name());
		scrubs.put(target.getId(), scrubsOf(target) + 1);
	}

	static void markPerformed(GameCharacter character, String actionId) {
		if(character != null && actionId != null) {
			performed.add(character.getId() + "|" + actionId);
		}
	}

	private static int scrubsOf(GameCharacter target) {
		Integer count = scrubs.get(target.getId());
		return count == null ? 0 : count;
	}

	private static boolean isOwnedSlave(GameCharacter target) {
		return target.isSlave() && target.getOwner() != null && target.getOwner().isPlayer();
	}

	private static void endSession() {
		started = false;
		participants.clear();
		positions.clear();
		paces.clear();
		pressPaces.clear();
		edged.clear();
		pending.clear();
	}
}
