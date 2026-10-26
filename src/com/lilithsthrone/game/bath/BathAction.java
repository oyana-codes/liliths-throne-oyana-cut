package com.lilithsthrone.game.bath;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.lilithsthrone.controller.xmlParsing.Element;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.dialogue.places.dominion.lilayashome.BathArea;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;

/**
 * One bathing action, usually loaded from {@code res/bath}. The root element is {@code bathAction},
 * so the sex loader will not pick these files up.
 * <p>
 * A description may carry {@code pace}, {@code position}, {@code gate} (ANAL or NO_ANAL), and a
 * {@code descriptionConditional}. The first block that matches is used, and one of its {@code text}
 * entries is chosen. The character washing is {@code [npc]}.
 */
public class BathAction {

	private enum Gate {
		ANY,
		ANAL,
		NO_ANAL
	}

	private static final class DescriptionBlock {
		private BathPace pace;
		private BathPosition position;
		private Gate gate = Gate.ANY;
		private String conditional;
		private final List<String> texts = new ArrayList<>();

		private boolean matches(GameCharacter character, BathPace usedPace) {
			if(pace != null && pace != usedPace) {
				return false;
			}
			if(position != null && position != Bath.getPosition(character)) {
				return false;
			}
			if(gate == Gate.ANAL && !Main.game.isAnalContentEnabled()) {
				return false;
			}
			if(gate == Gate.NO_ANAL && Main.game.isAnalContentEnabled()) {
				return false;
			}
			if(conditional != null && !conditional.isEmpty()) {
				String parsed = UtilText.parse(character, conditional).trim();
				if(!Boolean.parseBoolean(parsed)) {
					return false;
				}
			}
			return !texts.isEmpty();
		}
	}

	private final String id;
	private BathCategory category = BathCategory.WASH;
	private BathArea area;
	private BathPosition setsPosition;
	private BathPace setsPace;
	private BathPosition requiresPosition;
	private String requiresFacility;
	private String actionConditional;
	private String title = "Wash";
	private String tooltip = "";
	private int seconds = 120;
	private int buttonPriority = 0;
	private final List<DescriptionBlock> descriptions = new ArrayList<>();

	public BathAction(File xmlFile, String id) throws Exception {
		this.id = id;
		Element root = Element.getDocumentRootElement(xmlFile);
		String categoryText = required(root, "category");
		category = BathCategory.valueOf(categoryText);
		String areaText = optional(root, "area");
		if(areaText != null && !areaText.isEmpty()) {
			area = BathArea.valueOf(areaText);
		}
		String positionText = optional(root, "setsPosition");
		if(positionText != null && !positionText.isEmpty()) {
			setsPosition = BathPosition.valueOf(positionText);
		}
		String paceText = optional(root, "setsPace");
		if(paceText != null && !paceText.isEmpty()) {
			setsPace = BathPace.valueOf(paceText);
		}
		String requiresPositionText = optional(root, "requiresPosition");
		if(requiresPositionText != null && !requiresPositionText.isEmpty()) {
			requiresPosition = BathPosition.valueOf(requiresPositionText);
		}
		requiresFacility = optional(root, "requiresFacility");
		actionConditional = optional(root, "actionConditional");
		String titleText = optional(root, "title");
		if(titleText != null && !titleText.isEmpty()) {
			title = titleText;
		}
		String tooltipText = optional(root, "tooltip");
		if(tooltipText != null) {
			tooltip = tooltipText;
		}
		String secondsText = optional(root, "seconds");
		if(secondsText != null && !secondsText.isEmpty()) {
			seconds = Integer.parseInt(secondsText);
		} else {
			seconds = defaultSeconds(category);
		}
		String priorityText = optional(root, "buttonPriority");
		if(priorityText != null && !priorityText.isEmpty()) {
			buttonPriority = Integer.parseInt(priorityText);
		}
		if(root.getOptionalFirstOf("descriptions").isPresent()) {
			for(Element description : root.getMandatoryFirstOf("descriptions").getAllOf("description")) {
				DescriptionBlock block = new DescriptionBlock();
				String blockPace = optional(description, "pace");
				if(blockPace != null && !blockPace.isEmpty()) {
					block.pace = BathPace.valueOf(blockPace);
				}
				String blockPosition = optional(description, "position");
				if(blockPosition != null && !blockPosition.isEmpty()) {
					block.position = BathPosition.valueOf(blockPosition);
				}
				String gate = optional(description, "gate");
				if(gate != null && !gate.isEmpty()) {
					block.gate = Gate.valueOf(gate);
				}
				block.conditional = optional(description, "descriptionConditional");
				for(Element text : description.getAllOf("text")) {
					String line = text.getTextContent();
					if(line != null && !line.trim().isEmpty()) {
						block.texts.add(line.trim());
					}
				}
				if(!block.texts.isEmpty()) {
					descriptions.add(block);
				}
			}
		}
	}

	private BathAction(String id) {
		this.id = id;
	}

	/** Used when a core action has no XML, so the scene still has its buttons. */
	public static BathAction fallback(BathCategory category, BathArea area, BathPosition position, BathPace pace, String prose) {
		BathAction action = new BathAction("fallback_" + category.name() + "_" + (area != null ? area.name() : position != null ? position.name() : pace.name()));
		action.category = category;
		action.area = area;
		action.setsPosition = position;
		action.setsPace = pace;
		action.title = category == BathCategory.WASH && area != null ? area.getWashTitle()
				: category == BathCategory.DEEP && area != null ? area.getDeepTitle()
				: position != null ? position.getTitle()
				: pace != null ? pace.getTitle()
				: "Soak";
		action.tooltip = position != null ? position.getTooltip() : pace != null ? pace.getTooltip() : "Two minutes.";
		action.seconds = defaultSeconds(category);
		if(category == BathCategory.SOAK) {
			action.requiresPosition = BathPosition.SOAKING;
		}
		DescriptionBlock block = new DescriptionBlock();
		block.texts.add(prose);
		action.descriptions.add(block);
		return action;
	}

	private static int defaultSeconds(BathCategory category) {
		if(category == BathCategory.POSITION) {
			return 30;
		}
		if(category == BathCategory.PACE) {
			return 0;
		}
		return 120;
	}

	public String getId() {
		return id;
	}

	public BathCategory getCategory() {
		return category;
	}

	public BathArea getArea() {
		return area;
	}

	public BathPosition getSetsPosition() {
		return setsPosition;
	}

	public BathPace getSetsPace() {
		return setsPace;
	}

	public int getButtonPriority() {
		return buttonPriority;
	}

	public int getSeconds() {
		return seconds;
	}

	public String getTitle() {
		if(category == BathCategory.DEEP && area != null) {
			return area.getDeepTitle();
		}
		if(area == BathArea.ASS && !Main.game.isAnalContentEnabled() && category == BathCategory.WASH) {
			return "Wash rear";
		}
		if(title == null || title.isEmpty()) {
			return area != null ? area.getWashTitle() : "Wash";
		}
		return title;
	}

	public String getTooltip(GameCharacter character) {
		return getTooltip(character, null);
	}

	public String getTooltip(GameCharacter character, BathPace paceForThisPress) {
		String parsed = UtilText.parse(character, tooltip == null ? "" : tooltip).trim();
		if(category == BathCategory.WASH || category == BathCategory.SOAK || category == BathCategory.DEEP) {
			String state = stainState(character);
			String paceNote = paceNote(character, paceForThisPress);
			String blocked = deepBlockedNote(character);
			String heat = heatNote(character);
			return state + paceNote + blocked + heat + (parsed.isEmpty() ? "" : " " + parsed);
		}
		return parsed;
	}

	private String paceNote(GameCharacter character, BathPace paceForThisPress) {
		BathPace pace = paceForThisPress == null ? Bath.getPace(character) : paceForThisPress;
		if(paceForThisPress == null) {
			return " Uses your " + paceWord(pace) + " pace.";
		}
		return " This pass is " + paceWord(pace) + ".";
	}

	private static String paceWord(BathPace pace) {
		switch(pace) {
			case EAGER:
				return "eager";
			case ROUGH:
				return "rough";
			case NORMAL:
			default:
				return "regular";
		}
	}

	private String heatNote(GameCharacter character) {
		if(category == BathCategory.SOAK) {
			for(BathArea candidate : BathArea.values()) {
				if(candidate.isSubmerged() && candidate.isAvailable(character) && Bath.stirs(character, candidate)) {
					return " Sensitive parts under the water can build toward an orgasm.";
				}
			}
			return "";
		}
		if((category == BathCategory.WASH || category == BathCategory.DEEP) && area != null && Bath.stirs(character, area)) {
			return " Sensitive. Repeated passes can bring an orgasm.";
		}
		return "";
	}

	private String deepBlockedNote(GameCharacter character) {
		if(category != BathCategory.WASH || area == null || !area.supportsDeepClean(character) || !area.isDeepBlocked(character)) {
			return "";
		}
		return " Deeper cleaning is blocked while something plugs or seals it.";
	}

	private String stainState(GameCharacter character) {
		if(category == BathCategory.SOAK) {
			return "Two minutes in the water.";
		}
		if(category == BathCategory.DEEP) {
			return "Inside. Two minutes.";
		}
		if(area != null && area.isFilthy(character)) {
			return "Stained. Two minutes.";
		}
		if(area != null && Bath.hasScrubbed(character, area)) {
			return "Already washed. Another pass still helps a little. Two minutes.";
		}
		return "Not washed yet. Two minutes.";
	}

	public boolean isShown(GameCharacter character) {
		if(requiresFacility != null && !requiresFacility.isEmpty() && !Bath.getFacility().meets(requiresFacility)) {
			return false;
		}
		if(requiresPosition != null && Bath.getPosition(character) != requiresPosition) {
			return false;
		}
		if(setsPosition != null && !Bath.isPositionAllowed(character, setsPosition)) {
			return false;
		}
		if(category == BathCategory.WASH && area != null && !area.isAvailable(character)) {
			return false;
		}
		if(category == BathCategory.DEEP) {
			if(area == null || !area.supportsDeepClean(character) || area.isDeepBlocked(character)) {
				return false;
			}
		}
		if(category == BathCategory.SOAK && !Bath.getFacility().hasBath()) {
			return false;
		}
		return meetsConditional(character);
	}

	public boolean isPerformable(GameCharacter character) {
		if(!isShown(character)) {
			return false;
		}
		if(setsPosition != null && Bath.getPosition(character) == setsPosition) {
			return false;
		}
		if(setsPace != null && Bath.getPace(character) == setsPace) {
			return false;
		}
		if((category == BathCategory.WASH || category == BathCategory.DEEP) && area != null && !Bath.canReach(character, area)) {
			return false;
		}
		return true;
	}

	public String disabledReason(GameCharacter character) {
		if(setsPosition != null && Bath.getPosition(character) == setsPosition) {
			return "Already like this.";
		}
		if(setsPace != null && Bath.getPace(character) == setsPace) {
			return "Already washing at this pace.";
		}
		if((category == BathCategory.WASH || category == BathCategory.DEEP) && area != null && !Bath.canReach(character, area)) {
			return Bath.reachReason(character, area);
		}
		return "Cannot do that from here.";
	}

	private boolean meetsConditional(GameCharacter character) {
		if(actionConditional == null || actionConditional.isEmpty()) {
			return true;
		}
		return Boolean.parseBoolean(UtilText.parse(character, actionConditional).trim());
	}

	/** Parsed prose for this character, using their current pace. */
	public String resolveInner(GameCharacter character) {
		return resolveInner(character, null);
	}

	/**
	 * Parsed prose for this character, with no wrapping paragraph.
	 * @param paceForThisPress Null uses the pace stored for this character.
	 */
	public String resolveInner(GameCharacter character, BathPace paceForThisPress) {
		BathPace usedPace = paceForThisPress == null ? Bath.getPace(character) : paceForThisPress;
		DescriptionBlock matched = null;
		for(DescriptionBlock block : descriptions) {
			if(block.matches(character, usedPace)) {
				matched = block;
				break;
			}
		}
		if(matched == null && !descriptions.isEmpty()) {
			matched = descriptions.get(0);
		}
		String raw;
		if(matched == null) {
			raw = area != null ? area.washText() : "[npc.Name] [npc.verb(stay)] in the water.";
		} else {
			raw = Util.randomItemFrom(matched.texts);
			if(raw == null) {
				raw = area != null ? area.washText() : "[npc.Name] [npc.verb(stay)] in the water.";
			}
		}
		if(matched == null || matched.position == null) {
			raw = raw + reachAside(character);
		}
		raw = raw + soapSentence();
		return UtilText.parse(character, raw);
	}

	private String reachAside(GameCharacter character) {
		if(area == null || !area.isHardToReach(character)) {
			return "";
		}
		if(Bath.hasHelperFor(character)) {
			return " Someone else reaches the part [npc.she] cannot.";
		}
		BathPosition where = Bath.getPosition(character);
		if(where == BathPosition.HANDHELD) {
			return " The shower head does the reaching.";
		}
		if(where == BathPosition.SOAKING) {
			return " The bathwater does the reaching.";
		}
		return "";
	}

	private String soapSentence() {
		if(category != BathCategory.WASH && category != BathCategory.SOAK && category != BathCategory.DEEP) {
			return "";
		}
		AbstractItem soap = Bath.selectedSoap();
		if(soap == null) {
			return "";
		}
		if("oyana_soap_unscented".equals(soap.getItemType().getId())) {
			return " The unscented soap cuts through whatever was on [npc.her] skin.";
		}
		return " The " + soap.getName(false, false) + " leaves its scent on [npc.her] skin.";
	}

	/** Effects of the action, plus a parsed edge paragraph when this pass reaches one. A climax waits for the orgasm button. */
	public String apply(GameCharacter performer) {
		switch(category) {
			case POSITION:
				if(setsPosition != null) {
					Bath.setPosition(performer, setsPosition);
				}
				return "";
			case PACE:
				if(setsPace != null) {
					Bath.setPace(performer, setsPace);
				}
				return "";
			case WASH:
				String washed = Bath.washArea(performer, area);
				Bath.markPerformed(performer, id);
				return washed;
			case DEEP:
				String deepened = Bath.deepClean(performer, area);
				Bath.markPerformed(performer, id);
				return deepened;
			case SOAK:
				String soaked = Bath.soak(performer);
				Bath.markPerformed(performer, id);
				return soaked;
			default:
				return "";
		}
	}

	private static String optional(Element element, String tag) throws Exception {
		if(element.getOptionalFirstOf(tag).isPresent()) {
			return element.getMandatoryFirstOf(tag).getTextContent().trim();
		}
		return null;
	}

	private static String required(Element element, String tag) throws Exception {
		return element.getMandatoryFirstOf(tag).getTextContent().trim();
	}
}
