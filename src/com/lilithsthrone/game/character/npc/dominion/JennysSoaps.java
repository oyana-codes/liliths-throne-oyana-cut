package com.lilithsthrone.game.character.npc.dominion;

import java.time.Month;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lilithsthrone.game.character.CharacterImportSetting;
import com.lilithsthrone.game.character.EquipClothingSetting;
import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.character.npc.NPC;
import com.lilithsthrone.game.character.persona.NameTriplet;
import com.lilithsthrone.game.character.race.RaceStage;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.inventory.AbstractCoreItem;
import com.lilithsthrone.game.inventory.CharacterInventory;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * Off-map stock for Jenny's soap shelf. She is not placed in the shop, so the player only meets Jenny.
 *
 * @since 0.4.11.9
 * @version 0.4.11.9
 * @author oyana
 */
public class JennysSoaps extends NPC {

	public JennysSoaps() {
		this(false);
	}

	public JennysSoaps(boolean isImported) {
		super(isImported, new NameTriplet("Jenny"), "Walsh",
				Jenny.DESCRIPTION,
				28, Month.JUNE, 3,
				4, Gender.F_V_B_FEMALE, Subspecies.COW_MORPH, RaceStage.GREATER,
				new CharacterInventory(false, 2000), WorldType.EMPTY, PlaceType.GENERIC_HOLDING_CELL, false);

		if(!isImported) {
			dailyUpdate();
		}
	}

	@Override
	public void loadFromXML(Element parentElement, Document doc, CharacterImportSetting... settings) {
		loadNPCVariablesFromXML(this, null, parentElement, doc, settings);
	}

	@Override
	public void setStartingPersona(boolean setPersonality, boolean setFetishes, boolean setOrientation, boolean setHistory, boolean setSpells) {
		Jenny.applyPersona(this, setPersonality, setFetishes, setOrientation, setHistory);
	}

	@Override
	public void setStartingBody(boolean setPersona) {
		if(setPersona) {
			setStartingPersona();
		}
		Jenny.applyBody(this);
	}

	@Override
	public void equipClothing(List<EquipClothingSetting> settings) {
		Jenny.equipWorkClothes(this);
	}

	@Override
	public boolean isUnique() {
		return true;
	}

	@Override
	public void dailyUpdate() {
		Jenny.restock(this, Jenny.SOAP_IDS, 6);
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
					+ "Jenny turns and lifts a tray of bars from the shelf behind her."
				+ "</p>"
				+ "<p>"
					+ "[jenny.speech(These are the ones I made this week. They're for skin, not clothes.)]"
				+ "</p>";
	}

	@Override
	public boolean isTrader() {
		return true;
	}

	@Override
	public float getSellModifier(AbstractCoreItem item) {
		return Main.game.getNpc(Jenny.class).getSellModifier(item);
	}

	@Override
	public boolean willBuy(AbstractCoreItem item) {
		return Jenny.buys(item);
	}
}
