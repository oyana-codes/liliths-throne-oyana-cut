package com.lilithsthrone.game.dialogue.places.dominion.lilayashome;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.CoverableArea;
import com.lilithsthrone.game.character.body.valueEnums.LegConfiguration;
import com.lilithsthrone.game.inventory.InventorySlot;
import com.lilithsthrone.game.inventory.ItemTag;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.DisplacementType;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.main.Main;

/**
 * One washable part of a body. Buttons are omitted when the body does not have that part.
 */
public enum BathArea {

	HAIR(BathTab.HEAD, "Wash hair", "Uncover hair", "[npc.hair]", CoverableArea.HAIR, null, null, false,
			new InventorySlot[] { InventorySlot.HAIR, InventorySlot.HEAD },
			new SexAreaOrifice[] {}),
	FACE(BathTab.HEAD, "Wash face", "Uncover face", "face", CoverableArea.MOUTH, null, null, false,
			new InventorySlot[] { InventorySlot.EYES, InventorySlot.MOUTH, InventorySlot.HEAD },
			new SexAreaOrifice[] { SexAreaOrifice.MOUTH }),
	HORNS(BathTab.HEAD, "Wash horns", "Uncover horns", "[npc.horns]", null, null, InventorySlot.HORNS, false,
			new InventorySlot[] { InventorySlot.HORNS },
			new SexAreaOrifice[] {}),
	NECK(BathTab.HEAD, "Wash neck", "Uncover neck", "neck", null, null, InventorySlot.NECK, false,
			new InventorySlot[] { InventorySlot.NECK },
			new SexAreaOrifice[] {}),

	ARMS(BathTab.BODY, "Wash arms", "Uncover arms", "[npc.arms]", CoverableArea.HANDS, null, null, false,
			new InventorySlot[] { InventorySlot.HAND, InventorySlot.FINGER, InventorySlot.WRIST },
			new SexAreaOrifice[] {}),
	ARMPITS(BathTab.BODY, "Wash armpits", "Uncover armpits", "[npc.armpits]", CoverableArea.ARMPITS, null, null, false,
			new InventorySlot[] {},
			new SexAreaOrifice[] { SexAreaOrifice.ARMPITS }),
	CHEST(BathTab.BODY, "Wash chest", "Uncover chest", "[npc.breasts]", CoverableArea.BREASTS, CoverableArea.NIPPLES, null, true,
			new InventorySlot[] { InventorySlot.CHEST, InventorySlot.NIPPLE },
			new SexAreaOrifice[] { SexAreaOrifice.BREAST, SexAreaOrifice.NIPPLE }),
	STOMACH(BathTab.BODY, "Wash stomach", "Uncover stomach", "stomach", CoverableArea.STOMACH, CoverableArea.BACK, null, false,
			new InventorySlot[] { InventorySlot.STOMACH },
			new SexAreaOrifice[] {}),

	COCK(BathTab.LOWER, "Wash cock", "Uncover cock", "[npc.cock]", CoverableArea.PENIS, CoverableArea.TESTICLES, null, true,
			new InventorySlot[] { InventorySlot.PENIS, InventorySlot.GROIN },
			new SexAreaOrifice[] { SexAreaOrifice.URETHRA_PENIS }),
	PUSSY(BathTab.LOWER, "Wash pussy", "Uncover pussy", "[npc.pussy]", CoverableArea.VAGINA, null, null, true,
			new InventorySlot[] { InventorySlot.VAGINA, InventorySlot.GROIN },
			new SexAreaOrifice[] { SexAreaOrifice.VAGINA, SexAreaOrifice.URETHRA_VAGINA }),
	MOUND(BathTab.LOWER, "Wash mound", "Uncover mound", "mound", CoverableArea.MOUND, null, null, true,
			new InventorySlot[] { InventorySlot.GROIN },
			new SexAreaOrifice[] {}),
	ASS(BathTab.LOWER, "Wash ass", "Uncover ass", "[npc.ass]", CoverableArea.ASS, null, null, true,
			new InventorySlot[] { InventorySlot.ANUS },
			new SexAreaOrifice[] { SexAreaOrifice.ASS, SexAreaOrifice.ANUS }),
	THIGHS(BathTab.LOWER, "Wash thighs", "Uncover thighs", "thighs", CoverableArea.THIGHS, null, null, false,
			new InventorySlot[] {},
			new SexAreaOrifice[] { SexAreaOrifice.THIGHS }),
	LEGS(BathTab.LOWER, "Wash legs", "Uncover legs", "[npc.legs]", CoverableArea.LEGS, CoverableArea.FEET, null, false,
			new InventorySlot[] { InventorySlot.LEG, InventorySlot.FOOT, InventorySlot.ANKLE },
			new SexAreaOrifice[] {}),

	CROTCH(BathTab.EXTRA, "Wash crotch", "Uncover crotch", "[npc.breastsCrotch]", CoverableArea.BREASTS_CROTCH, CoverableArea.NIPPLES_CROTCH, null, true,
			new InventorySlot[] {},
			new SexAreaOrifice[] { SexAreaOrifice.BREAST_CROTCH, SexAreaOrifice.NIPPLE_CROTCH }),
	TAIL(BathTab.EXTRA, "Wash tail", "Uncover tail", "[npc.tail]", CoverableArea.TAIL, null, null, false,
			new InventorySlot[] { InventorySlot.TAIL },
			new SexAreaOrifice[] {}),
	WINGS(BathTab.EXTRA, "Wash wings", "Uncover wings", "[npc.wings]", null, null, InventorySlot.WINGS, false,
			new InventorySlot[] { InventorySlot.WINGS },
			new SexAreaOrifice[] {}),
	SPINNERET(BathTab.EXTRA, "Wash spinneret", "Uncover spinneret", "[npc.spinneret]", null, null, null, true,
			new InventorySlot[] {},
			new SexAreaOrifice[] { SexAreaOrifice.SPINNERET });

	private final BathTab tab;
	private final String washTitle;
	private final String uncoverTitle;
	private final String partTag;
	private final CoverableArea coverable;
	private final CoverableArea alsoCoverable;
	private final InventorySlot slotInTheWay;
	private final boolean intimate;
	private final List<InventorySlot> slots;
	private final List<SexAreaOrifice> orifices;

	BathArea(BathTab tab, String washTitle, String uncoverTitle, String partTag,
			CoverableArea coverable, CoverableArea alsoCoverable, InventorySlot slotInTheWay, boolean intimate,
			InventorySlot[] slots, SexAreaOrifice[] orifices) {
		this.tab = tab;
		this.washTitle = washTitle;
		this.uncoverTitle = uncoverTitle;
		this.partTag = partTag;
		this.coverable = coverable;
		this.alsoCoverable = alsoCoverable;
		this.slotInTheWay = slotInTheWay;
		this.intimate = intimate;
		List<InventorySlot> slotList = new ArrayList<>();
		Collections.addAll(slotList, slots);
		this.slots = Collections.unmodifiableList(slotList);
		List<SexAreaOrifice> orificeList = new ArrayList<>();
		Collections.addAll(orificeList, orifices);
		this.orifices = Collections.unmodifiableList(orificeList);
	}

	public BathTab getTab() {
		return tab;
	}

	public boolean isIntimate() {
		return intimate;
	}

	public List<InventorySlot> getSlots() {
		return slots;
	}

	public List<SexAreaOrifice> getOrifices() {
		return orifices;
	}

	public boolean isAvailable(GameCharacter target) {
		switch(this) {
			case HAIR:
				return target.hasHair();
			case HORNS:
				return target.hasHorns();
			case COCK:
				return target.hasPenis();
			case PUSSY:
				return target.hasVagina();
			case MOUND:
				return !target.hasPenis() && !target.hasVagina();
			case THIGHS:
			case LEGS:
				return target.hasLegs();
			case CROTCH:
				return target.hasBreastsCrotch();
			case TAIL:
				return target.hasTail();
			case WINGS:
				return target.hasWings();
			case SPINNERET:
				return target.hasSpinneret();
			default:
				return true;
		}
	}

	/**
	 * True when this part sits on a feral half the hands cannot reach.
	 * Centaurs are quadrupedal. Driders are arachnid. The same test covers the other taur bodies.
	 */
	public boolean isHardToReach(GameCharacter character) {
		LegConfiguration legs = character.getLegConfiguration();
		switch(this) {
			case COCK:
			case PUSSY:
			case MOUND:
			case ASS:
				return !legs.isBipedalPositionedGenitals();
			case CROTCH:
				return !legs.isBipedalPositionedCrotchBoobs();
			case THIGHS:
			case LEGS:
			case TAIL:
				return legs != LegConfiguration.BIPEDAL;
			case SPINNERET:
				return true;
			default:
				return false;
		}
	}

	/** Groin and rear are awkward while standing, even on a biped. */
	public boolean isGroinOrRear() {
		return this == COCK || this == PUSSY || this == MOUND || this == ASS;
	}

	/** Parts a bath covers, including the ones hands may not reach. */
	public boolean isSubmerged() {
		switch(this) {
			case CHEST:
			case STOMACH:
			case COCK:
			case PUSSY:
			case MOUND:
			case ASS:
			case THIGHS:
			case LEGS:
			case CROTCH:
			case TAIL:
			case SPINNERET:
				return true;
			default:
				return false;
		}
	}

	public String getWashTitle() {
		if(this == ASS && !Main.game.isAnalContentEnabled()) {
			return "Wash rear";
		}
		return washTitle;
	}

	public String getUncoverTitle() {
		if(this == ASS && !Main.game.isAnalContentEnabled()) {
			return "Uncover rear";
		}
		return uncoverTitle;
	}

	public String getPartTag() {
		if(this == ASS && !Main.game.isAnalContentEnabled()) {
			return "rear";
		}
		return partTag;
	}

	public String getDeepTitle() {
		switch(this) {
			case PUSSY:
				return "Clean pussy deeper";
			case COCK:
				return "Clean cock deeper";
			case ASS:
				return Main.game.isAnalContentEnabled() ? "Clean ass deeper" : "Clean rear deeper";
			case CHEST:
				return "Clean nipples deeper";
			case CROTCH:
				return "Clean crotch nipples deeper";
			case SPINNERET:
				return "Clean spinneret deeper";
			default:
				return "Clean deeper";
		}
	}

	/**
	 * Orifices a deeper rinse can empty. The mouth is a surface rinse, handled by washing the face.
	 * The ass entry is omitted when anal content is off, so the button and the prose never name it.
	 */
	public List<SexAreaOrifice> getDeepOrifices() {
		List<SexAreaOrifice> found = new ArrayList<>();
		switch(this) {
			case PUSSY:
				found.add(SexAreaOrifice.VAGINA);
				found.add(SexAreaOrifice.URETHRA_VAGINA);
				break;
			case COCK:
				if(Main.game.isUrethraEnabled()) {
					found.add(SexAreaOrifice.URETHRA_PENIS);
				}
				break;
			case ASS:
				if(Main.game.isAnalContentEnabled()) {
					found.add(SexAreaOrifice.ANUS);
				}
				break;
			case CHEST:
				found.add(SexAreaOrifice.NIPPLE);
				break;
			case CROTCH:
				found.add(SexAreaOrifice.NIPPLE_CROTCH);
				break;
			case SPINNERET:
				found.add(SexAreaOrifice.SPINNERET);
				break;
			default:
				break;
		}
		return found;
	}

	/** True when this body has an orifice here that can be rinsed out, and the content options allow it. */
	public boolean supportsDeepClean(GameCharacter character) {
		return isAvailable(character) && !getDeepOrifices().isEmpty();
	}

	/**
	 * A plug or a seal blocks the deeper rinse. The outside can still be washed.
	 * Crotch nipples use the same nipple plug and seal tags as chest nipples.
	 */
	public boolean isDeepBlocked(GameCharacter character) {
		for(SexAreaOrifice orifice : getDeepOrifices()) {
			if(character.isOrificePlugged(orifice)) {
				return true;
			}
			if(orifice == SexAreaOrifice.NIPPLE_CROTCH && wearsNippleBlock(character)) {
				return true;
			}
		}
		return false;
	}

	private static boolean wearsNippleBlock(GameCharacter character) {
		for(AbstractClothing clothing : character.getClothingCurrentlyEquipped()) {
			if(clothing.getItemTags().contains(ItemTag.PLUGS_NIPPLES) || clothing.getItemTags().contains(ItemTag.SEALS_NIPPLES)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFilthy(GameCharacter target) {
		for(InventorySlot slot : slots) {
			if(target.isDirtySlot(slot)) {
				return true;
			}
		}
		for(SexAreaOrifice orifice : orifices) {
			if(target.getTotalFluidInArea(orifice) > 0) {
				return true;
			}
		}
		return false;
	}

	/** The next garment in the way, or null when the part can be washed. */
	public Map.Entry<AbstractClothing, DisplacementType> blocking(GameCharacter target) {
		Map.Entry<AbstractClothing, DisplacementType> blocked = blockedBy(target, coverableArea(target));
		if(blocked != null) {
			return blocked;
		}
		blocked = blockedBy(target, alsoCoverable);
		if(blocked != null) {
			return blocked;
		}
		if(slotInTheWay != null) {
			AbstractClothing clothing = target.getClothingInSlot(slotInTheWay);
			if(clothing != null) {
				return new AbstractMap.SimpleEntry<>(clothing, DisplacementType.REMOVE_OR_EQUIP);
			}
		}
		return null;
	}

	private CoverableArea coverableArea(GameCharacter target) {
		if(this == SPINNERET) {
			return SexAreaOrifice.SPINNERET.getRelatedCoverableArea(target);
		}
		return coverable;
	}

	private static Map.Entry<AbstractClothing, DisplacementType> blockedBy(GameCharacter target, CoverableArea area) {
		if(area == null || target.isAbleToAccessCoverableArea(area, false)) {
			return null;
		}
		Map.Entry<AbstractClothing, DisplacementType> entry = target.getNextClothingToRemoveForCoverableAreaAccess(area);
		if(entry == null || entry.getKey() == null) {
			return null;
		}
		return entry;
	}

	public String washText() {
		switch(this) {
			case HAIR:
				return "You wet [npc.namePos] [npc.hair] and work your fingers from the scalp outward, rinsing until the water runs clear.";
			case FACE:
				return "You cup warm water over [npc.namePos] face and wipe around [npc.her] [npc.eyes] and [npc.mouth]. You tip [npc.her] chin up and rinse [npc.her] mouth out, then let [npc.herHim] spit into the drain.";
			case HORNS:
				return "You run a wet hand along [npc.namePos] [npc.horns], into every ridge, and rinse them until they shine.";
			case NECK:
				return "You wash [npc.namePos] neck, under the jaw and down to the collarbone, and let the water carry the sweat away.";
			case ARMS:
				return "You wash along [npc.namePos] [npc.arms], shoulder to wrist, and rinse [npc.her] [npc.hands] until the skin feels bare again.";
			case ARMPITS:
				return "You lift [npc.namePos] [npc.arms] and wash [npc.her] [npc.armpits], slow enough to get the hair and the crease, then rinse them clean.";
			case CHEST:
				return "You wash over [npc.namePos] [npc.breasts], under them and across [npc.her] [npc.nipples], with a hand that lingers and then moves on. The water sheets off [npc.her] chest.";
			case STOMACH:
				return "You wash [npc.namePos] stomach and the small of [npc.her] back, following the curve of [npc.her] waist, and rinse both clean.";
			case COCK:
				return "You wash [npc.namePos] [npc.cock] in your hand, from the base over [npc.her] [npc.balls] and up to the tip, then rinse [npc.herHim] carefully until nothing sticky remains.";
			case PUSSY:
				return "You wash between [npc.namePos] thighs and over [npc.her] [npc.pussy], parting [npc.herHim] just enough to rinse what had collected there, then let the water run clear.";
			case MOUND:
				return "You wash the smooth mound between [npc.namePos] thighs, gentle and thorough, and rinse [npc.herHim] until the skin is clean.";
			case ASS:
				if(Main.game.isAnalContentEnabled()) {
					return "You wash over [npc.namePos] [npc.ass] and between the cheeks, rinsing [npc.her] [npc.asshole] until the water runs clean. Your fingers stay at the work of washing.";
				}
				return "You wash over [npc.namePos] rear and between the cheeks, rinsing until the water runs clean.";
			case THIGHS:
				return "You wash [npc.namePos] thighs, inside and out, and rinse the sweat from the soft skin there.";
			case LEGS:
				return "You wash down [npc.namePos] [npc.legs] to [npc.her] [npc.feet], between the toes and back up the calves, and rinse the dirt away.";
			case CROTCH:
				return "You wash [npc.namePos] [npc.breastsCrotch], under them and over the nipples, taking the weight in one hand while the other rinses them clean.";
			case TAIL:
				return "You draw [npc.namePos] [npc.tail] through your hands, from the base out to the tip, and rinse it until the water runs clear.";
			case WINGS:
				return "You wash along [npc.namePos] [npc.wings], into the folds and out to the edges, and rinse them clean.";
			case SPINNERET:
				return "You wash [npc.namePos] [npc.spinneret], careful around the opening, and rinse away what had gathered there.";
			default:
				return "You wash [npc.namePos] " + partTag + " and rinse [npc.herHim] clean.";
		}
	}

	public String uncoverText(String clothingName) {
		return "You move [npc.namePos] " + clothingName + " aside so you can reach. The garment itself is not washed.";
	}
}
