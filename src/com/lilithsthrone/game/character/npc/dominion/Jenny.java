package com.lilithsthrone.game.character.npc.dominion;

import java.time.Month;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lilithsthrone.game.character.CharacterImportSetting;
import com.lilithsthrone.game.character.EquipClothingSetting;
import com.lilithsthrone.game.character.body.coverings.BodyCoveringType;
import com.lilithsthrone.game.character.body.coverings.Covering;
import com.lilithsthrone.game.character.body.types.BreastType;
import com.lilithsthrone.game.character.body.types.HornType;
import com.lilithsthrone.game.character.body.valueEnums.AreolaeSize;
import com.lilithsthrone.game.character.body.valueEnums.AssSize;
import com.lilithsthrone.game.character.body.valueEnums.BodyHair;
import com.lilithsthrone.game.character.body.valueEnums.BodySize;
import com.lilithsthrone.game.character.body.valueEnums.BreastShape;
import com.lilithsthrone.game.character.body.valueEnums.CoveringPattern;
import com.lilithsthrone.game.character.body.valueEnums.CupSize;
import com.lilithsthrone.game.character.body.valueEnums.FluidRegeneration;
import com.lilithsthrone.game.character.body.valueEnums.HairLength;
import com.lilithsthrone.game.character.body.valueEnums.HairStyle;
import com.lilithsthrone.game.character.body.valueEnums.HipSize;
import com.lilithsthrone.game.character.body.valueEnums.HornLength;
import com.lilithsthrone.game.character.body.valueEnums.Lactation;
import com.lilithsthrone.game.character.body.valueEnums.LipSize;
import com.lilithsthrone.game.character.body.valueEnums.Muscle;
import com.lilithsthrone.game.character.body.valueEnums.NippleSize;
import com.lilithsthrone.game.character.effects.Perk;
import com.lilithsthrone.game.character.effects.PerkCategory;
import com.lilithsthrone.game.character.effects.PerkManager;
import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.character.npc.NPC;
import com.lilithsthrone.game.character.persona.NameTriplet;
import com.lilithsthrone.game.character.persona.Occupation;
import com.lilithsthrone.game.character.persona.PersonalityTrait;
import com.lilithsthrone.game.character.persona.SexualOrientation;
import com.lilithsthrone.game.character.race.RaceStage;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.inventory.AbstractCoreItem;
import com.lilithsthrone.game.inventory.CharacterInventory;
import com.lilithsthrone.game.inventory.ItemTag;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.Util.Value;
import com.lilithsthrone.utils.colours.PresetColour;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * @since 0.4.11.9
 * @version 0.4.11.9
 * @author oyana
 */
public class Jenny extends NPC {

	public static final String DESCRIPTION = "Jenny runs Jenny's Wash, a laundromat in the Shopping Arcade. She makes soap in the back while the machines are going.";

	public static final String[] LAUNDRY_IDS = new String[] {
			"oyana_cleaning_unscented_detergent",
			"oyana_cleaning_lavender_detergent",
			"oyana_cleaning_citrus_detergent",
			"oyana_cleaning_linen_detergent",
			"oyana_cleaning_lavender_dryer_sheet",
			"oyana_cleaning_citrus_dryer_sheet",
			"oyana_cleaning_linen_dryer_sheet",
			"oyana_cleaning_garment_freshener"
	};

	public static final String[] SOAP_IDS = new String[] {
			"oyana_soap_unscented",
			"oyana_soap_lavender_lemongrass",
			"oyana_soap_rosemary_lemon_mint",
			"oyana_soap_rose_geranium",
			"oyana_soap_peppermint_eucalyptus",
			"oyana_soap_orange_clove",
			"oyana_soap_tangerine_basil",
			"oyana_soap_oatmeal_lavender"
	};

	public Jenny() {
		this(false);
	}

	public Jenny(boolean isImported) {
		super(isImported, new NameTriplet("Jenny"), "Walsh",
				DESCRIPTION,
				28, Month.JUNE, 3,
				4, Gender.F_V_B_FEMALE, Subspecies.COW_MORPH, RaceStage.GREATER,
				new CharacterInventory(false, 2000), WorldType.SHOPPING_ARCADE, PlaceType.SHOPPING_ARCADE_JENNYS_WASH, true);

		if(!isImported) {
			dailyUpdate();
		}
	}

	@Override
	public void loadFromXML(Element parentElement, Document doc, CharacterImportSetting... settings) {
		loadNPCVariablesFromXML(this, null, parentElement, doc, settings);
	}

	@Override
	public void setupPerks(boolean autoSelectPerks) {
		PerkManager.initialisePerks(this,
				Util.newArrayListOfValues(),
				Util.newHashMapOfValues(
						new Value<>(PerkCategory.PHYSICAL, 0),
						new Value<>(PerkCategory.LUST, 0),
						new Value<>(PerkCategory.ARCANE, 0)));
	}

	@Override
	public void setStartingPersona(boolean setPersonality, boolean setFetishes, boolean setOrientation, boolean setHistory, boolean setSpells) {
		applyPersona(this, setPersonality, setFetishes, setOrientation, setHistory);
	}

	public static void applyPersona(NPC npc, boolean setPersonality, boolean setFetishes, boolean setOrientation, boolean setHistory) {
		if(setPersonality) {
			npc.setPersonalityTraits(
					PersonalityTrait.KIND,
					PersonalityTrait.INNOCENT);
		}

		if(setFetishes) {
			npc.clearFetishDesires();
			npc.clearFetishes();
		}

		if(setOrientation) {
			npc.setSexualOrientation(SexualOrientation.AMBIPHILIC);
		}

		if(setHistory) {
			npc.setHistory(Occupation.NPC_STORE_OWNER);
		}
	}

	@Override
	public void setStartingBody(boolean setPersona) {
		if(setPersona) {
			setStartingPersona();
		}
		applyBody(this);
	}

	public static void applyBody(NPC npc) {
		npc.setHeight(176);
		npc.setFemininity(80);
		npc.setMuscle(Muscle.ZERO_SOFT.getMedianValue());
		npc.setBodySize(BodySize.FOUR_HUGE.getMedianValue());
		npc.setHornType(HornType.CURVED);
		npc.setHornLength(HornLength.ONE_SMALL.getMedianValue());

		npc.setEyeCovering(new Covering(BodyCoveringType.EYE_COW_MORPH, PresetColour.EYE_BROWN));
		npc.setSkinCovering(new Covering(BodyCoveringType.BOVINE_FUR, CoveringPattern.SPOTTED, PresetColour.COVERING_WHITE, false, PresetColour.COVERING_BLACK, false), true);
		npc.setSkinCovering(new Covering(BodyCoveringType.HUMAN, PresetColour.SKIN_LIGHT), true);

		npc.setHairCovering(new Covering(BodyCoveringType.HAIR_BOVINE_FUR, PresetColour.COVERING_BLACK), true);
		npc.setHairLength(HairLength.THREE_SHOULDER_LENGTH);
		npc.setHairStyle(HairStyle.LOW_PONYTAIL);

		npc.setHairCovering(new Covering(BodyCoveringType.BODY_HAIR_HUMAN, PresetColour.COVERING_BLACK), false);
		npc.setHairCovering(new Covering(BodyCoveringType.BODY_HAIR_BOVINE_FUR, PresetColour.COVERING_BLACK), false);
		npc.setUnderarmHair(BodyHair.ZERO_NONE);
		npc.setAssHair(BodyHair.FOUR_NATURAL);
		npc.setPubicHair(BodyHair.FOUR_NATURAL);
		npc.setFacialHair(BodyHair.ZERO_NONE);

		npc.setFaceVirgin(true);
		npc.setLipSize(LipSize.TWO_FULL);

		npc.setNippleVirgin(true);
		npc.setBreastSize(CupSize.H);
		npc.setBreastShape(BreastShape.WIDE);
		npc.setNippleSize(NippleSize.THREE_LARGE);
		npc.setAreolaeSize(AreolaeSize.THREE_LARGE);
		npc.setNippleCountPerBreast(4);
		if(Main.game.isLactationContentEnabled()) {
			npc.setBreastMilkStorage(Lactation.FOUR_LARGE_AMOUNT.getMedianValue());
			npc.fillMilkToMaxStorage();
			npc.setBreastLactationRegeneration(FluidRegeneration.ONE_AVERAGE.getMedianRegenerationValuePerDay());
		}

		npc.setAssVirgin(true);
		npc.setAssSize(AssSize.FIVE_HUGE);
		npc.setHipSize(HipSize.FIVE_VERY_WIDE);

		npc.setVaginaVirgin(true);

		// Unique shopkeeper. Udders follow the content toggle, not the random-spawn slider.
		if(Main.game.isUdderContentEnabled()) {
			npc.setBreastCrotchType(BreastType.COW_MORPH);
			npc.setBreastCrotchShape(BreastShape.UDDERS);
			npc.setBreastCrotchRows(1);
			npc.setBreastCrotchSize(CupSize.H);
			npc.setNippleCrotchCountPerBreast(4);
			if(Main.game.isLactationContentEnabled()) {
				npc.setBreastCrotchMilkStorage(Lactation.FOUR_LARGE_AMOUNT.getMedianValue());
				npc.fillMilkCrotchToMaxStorage();
				npc.setBreastCrotchLactationRegeneration(FluidRegeneration.ONE_AVERAGE.getMedianRegenerationValuePerDay());
			}
		}
	}

	public static void equipWorkClothes(NPC npc) {
		npc.unequipAllClothingIntoVoid(true, true);
		npc.equipClothingFromNowhere(Main.game.getItemGen().generateClothing("innoxia_torso_blouse", PresetColour.CLOTHING_BLUE_GREY, false), true, npc);
		npc.equipClothingFromNowhere(Main.game.getItemGen().generateClothing("innoxia_leg_skirt", PresetColour.CLOTHING_GREY_DARK, false), true, npc);
		npc.equipClothingFromNowhere(Main.game.getItemGen().generateClothing("innoxia_torsoOver_apron", PresetColour.CLOTHING_WHITE, false), true, npc);
	}

	public static void restock(NPC shopkeeper, String[] ids, int count) {
		shopkeeper.clearNonEquippedInventory(false);
		if(shopkeeper.getMoney() < 2000) {
			shopkeeper.setMoney(2000);
		}
		for(String id : ids) {
			shopkeeper.addItem(Main.game.getItemGen().generateItem(id), count, false, false);
		}
	}

	public static boolean buys(AbstractCoreItem item) {
		if(item.getItemTags().contains(ItemTag.CONTRABAND_LIGHT)
				|| item.getItemTags().contains(ItemTag.CONTRABAND_MEDIUM)
				|| item.getItemTags().contains(ItemTag.CONTRABAND_HEAVY)) {
			return false;
		}
		return item instanceof AbstractItem;
	}

	@Override
	public void equipClothing(List<EquipClothingSetting> settings) {
		equipWorkClothes(this);
	}

	@Override
	public boolean isUnique() {
		return true;
	}

	@Override
	public void dailyUpdate() {
		restock(this, LAUNDRY_IDS, 8);
	}

	@Override
	public void turnUpdate() {
		if(!Main.game.getCharactersPresent().contains(this)) {
			if(Main.game.isExtendedWorkTime()) {
				this.returnToHome();
			} else {
				this.setLocation(WorldType.EMPTY, PlaceType.GENERIC_HOLDING_CELL, false);
			}
		}
	}

	@Override
	public void changeFurryLevel() {
	}

	@Override
	public DialogueNode getEncounterDialogue() {
		return null;
	}

	@Override
	public String getTraderDescription() {
		return "<p>"
					+ "Jenny sets a crate on the counter."
				+ "</p>"
				+ "<p>"
					+ "[jenny.speech(Detergent, dryer sheets, and a freshener if you only need one thing done. I restock in the morning.)]"
				+ "</p>";
	}

	@Override
	public boolean isTrader() {
		return true;
	}

	@Override
	public float getSellModifier(AbstractCoreItem item) {
		float price = Main.game.getPlayer().hasTrait(Perk.JOB_STUDENT, true) ? 0.75f : 1f;
		return Math.max(getBuyModifier(), price);
	}

	@Override
	public boolean willBuy(AbstractCoreItem item) {
		return buys(item);
	}
}
