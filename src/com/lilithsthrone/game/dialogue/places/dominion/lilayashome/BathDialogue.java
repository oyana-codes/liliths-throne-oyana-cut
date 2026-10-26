package com.lilithsthrone.game.dialogue.places.dominion.lilayashome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.lilithsthrone.game.bath.Bath;
import com.lilithsthrone.game.bath.BathAction;
import com.lilithsthrone.game.bath.BathActionManager;
import com.lilithsthrone.game.bath.BathCategory;
import com.lilithsthrone.game.bath.BathFacility;
import com.lilithsthrone.game.bath.BathPace;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.DisplacementType;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.game.sex.SexPace;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.colours.Colour;
import com.lilithsthrone.utils.colours.PresetColour;

/**
 * The wash scene. Each action is appended under the last one, the way sex does it.
 * The lines themselves come from res/bath.
 * The pace chosen at the start is the default. A part washed once then offers that wash again, plus eager and rough.
 * Reaching the edge replaces the tabs with orgasm, prepare, and deny.
 */
public class BathDialogue {

	private static final int UNCOVER_SECONDS = 60;

	public static void start() {
		Bath.start(BathFacility.BOTH);
	}

	public static final DialogueNode WASH = new DialogueNode("Bath", "", true) {

		@Override
		public boolean isContinuesDialogue() {
			return Bath.isContinuing();
		}

		@Override
		public boolean isDisplaysActionTitleOnContinuesDialogue() {
			return true;
		}

		@Override
		public boolean isContentParsed() {
			return false;
		}

		@Override
		public String getContent() {
			if(!Bath.isStarted()) {
				Bath.start(BathFacility.BOTH);
			}
			return Bath.getDescription();
		}

		@Override
		public String getResponseTabTitle(int index) {
			if(!Bath.isPaceChosen() || Bath.isReadyToOrgasm(Main.game.getPlayer())) {
				return null;
			}
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
				case POSITION:
					return "Position";
				case PACE:
					return "Pace";
				case OPTIONS:
					return "Options";
				default:
					return null;
			}
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if(!Bath.isStarted()) {
				Bath.start(BathFacility.BOTH);
			}
			if(!Bath.isPaceChosen()) {
				return openingPaceResponse(index);
			}
			if(Bath.isReadyToOrgasm(Main.game.getPlayer())) {
				return orgasmResponse(index);
			}
			BathTab tab = tabAt(responseTab);
			if(tab == null) {
				return null;
			}
			if(tab == BathTab.OPTIONS) {
				return optionsResponse(index);
			}
			return paged(responsesOn(tab), index);
		}
	};

	private static Response openingPaceResponse(int index) {
		if(index == 1) {
			return paceStart("Regularly", BathPace.NORMAL, "Start at an easy pace. The first wash of each part uses this until you change it.");
		}
		if(index == 2) {
			return paceStart("Eagerly", BathPace.EAGER, "Start eager. The first wash of each part uses the eager lines until you change it.");
		}
		if(index == 3) {
			return paceStart("Roughly", BathPace.ROUGH, "Start rough. The first wash of each part uses the rough lines until you change it.");
		}
		if(index == 4) {
			return new Response("Leave",
					"Step back into the room. Nothing is washed, and no bathed status is gained.",
					RoomPlayer.ROOM) {
				@Override
				public void effects() {
					Bath.leave();
				}
			};
		}
		return null;
	}

	private static Response paceStart(String title, BathPace pace, String tooltip) {
		return new Response(title, tooltip, WASH) {
			@Override
			public void effects() {
				Bath.chooseOpeningPace(pace);
			}
			@Override
			public int getSecondsPassed() {
				return 0;
			}
			@Override
			public SexPace getSexPace() {
				return toSexPace(pace);
			}
			@Override
			public Colour getHighlightColour() {
				return paceColour(pace);
			}
		};
	}

	/**
	 * The wash that crossed 100 already happened. These three decide the ending, and they take no further time.
	 * Index 0 stays empty, the same way a sex orgasm menu does.
	 */
	private static Response orgasmResponse(int index) {
		GameCharacter player = Main.game.getPlayer();
		BathPace pace = Bath.getPendingPace(player);
		if(index == 1) {
			return new Response("Orgasm", "Let it happen. Arousal falls afterwards.", WASH) {
				@Override
				public void effects() {
					Bath.orgasm();
				}
				@Override
				public int getSecondsPassed() {
					return 0;
				}
				@Override
				public SexPace getSexPace() {
					return toSexPace(pace);
				}
				@Override
				public Colour getHighlightColour() {
					Colour colour = paceColour(pace);
					return colour == null ? PresetColour.GENERIC_SEX : colour;
				}
			};
		}
		if(index == 2) {
			return new Response("Prepare", "Stay on the edge. Nothing is spent, and the choice stays.", WASH) {
				@Override
				public void effects() {
					Bath.prepare();
				}
				@Override
				public int getSecondsPassed() {
					return 0;
				}
				@Override
				public Colour getHighlightColour() {
					return PresetColour.GENERIC_SEX;
				}
			};
		}
		if(index == 3) {
			return new Response("Deny", "Pull back. Arousal drops, and this does not count as an orgasm.", WASH) {
				@Override
				public void effects() {
					Bath.deny();
				}
				@Override
				public int getSecondsPassed() {
					return 0;
				}
				@Override
				public Colour getHighlightColour() {
					return PresetColour.BASE_CRIMSON;
				}
			};
		}
		return null;
	}

	/**
	 * Page 0 uses indexes 1 to 14, then index 0 as the fifteenth.
	 * Later pages are asked for as a flat index, the same way sex pages its actions.
	 */
	private static Response paged(List<Response> responses, int index) {
		int listIndex;
		if(index == 0) {
			listIndex = 14;
		} else if(index < 15) {
			listIndex = index - 1;
		} else {
			listIndex = index;
		}
		if(listIndex < 0 || listIndex >= responses.size()) {
			return null;
		}
		return responses.get(listIndex);
	}

	private static Response optionsResponse(int index) {
		GameCharacter player = Main.game.getPlayer();
		if(index == 1) {
			int minutes = Bath.bathedMinutes(player);
			return new Response("Finish",
					"Step out. " + minutes + " minutes of the bathed status, and no heal. Clothes are not washed.",
					RoomPlayer.ROOM) {
				@Override
				public void effects() {
					Bath.finish();
				}
			};
		}
		if(index == 2) {
			return new Response("Leave",
					"Step back into the room. Parts already washed stay clean. No bathed status.",
					RoomPlayer.ROOM) {
				@Override
				public void effects() {
					Bath.leave();
				}
			};
		}
		if(index == 3) {
			return soapResponse();
		}
		return null;
	}

	private static Response soapResponse() {
		List<AbstractItem> soaps = Bath.soaps();
		if(soaps.isEmpty()) {
			return new Response("Soap", "There is no soap in the bag. Water still works.", null);
		}
		AbstractItem selected = Bath.selectedSoap();
		String using = selected == null ? "Water only." : "Using " + selected.getName(false, false) + ".";
		return new Response("Soap", using + " Cycle to the next bar, or back to water. The bar is not used up.", WASH) {
			@Override
			public void effects() {
				Bath.cycleSoap();
			}
		};
	}

	private static List<Response> responsesOn(BathTab tab) {
		GameCharacter player = Main.game.getPlayer();
		List<Response> responses = new ArrayList<>();
		for(BathAction action : actionsOn(tab)) {
			if(isRepeatable(action)) {
				responses.addAll(repeatableResponses(player, action));
			} else if(!action.isPerformable(player)) {
				responses.add(new Response(action.getTitle(), action.disabledReason(player), null));
			} else {
				responses.add(actionResponse(action, null, action.getTitle()));
			}
		}
		return responses;
	}

	private static boolean isRepeatable(BathAction action) {
		return action.getCategory() == BathCategory.WASH
				|| action.getCategory() == BathCategory.SOAK
				|| action.getCategory() == BathCategory.DEEP;
	}

	private static List<Response> repeatableResponses(GameCharacter player, BathAction action) {
		List<Response> responses = new ArrayList<>();
		BathArea area = action.getArea();
		if(area != null && (action.getCategory() == BathCategory.WASH || action.getCategory() == BathCategory.DEEP)) {
			if(!Bath.canReach(player, area)) {
				responses.add(new Response(action.getTitle(), Bath.reachReason(player, area), null));
				return responses;
			}
			Map.Entry<AbstractClothing, DisplacementType> blocking = area.blocking(player);
			if(blocking != null) {
				if(action.getCategory() == BathCategory.WASH) {
					responses.add(uncoverResponse(area, blocking));
				}
				return responses;
			}
		}
		if(!action.isPerformable(player)) {
			responses.add(new Response(action.getTitle(), action.disabledReason(player), null));
			return responses;
		}
		if(Bath.hasPerformed(player, action.getId())) {
			responses.add(actionResponse(action, BathPace.NORMAL, pacedTitle(action, BathPace.NORMAL)));
			responses.add(actionResponse(action, BathPace.EAGER, pacedTitle(action, BathPace.EAGER)));
			responses.add(actionResponse(action, BathPace.ROUGH, pacedTitle(action, BathPace.ROUGH)));
		} else {
			responses.add(actionResponse(action, null, action.getTitle()));
		}
		return responses;
	}

	private static String pacedTitle(BathAction action, BathPace pace) {
		String base = action.getTitle();
		if(base == null || base.isEmpty()) {
			base = "wash";
		}
		String lower = Character.toLowerCase(base.charAt(0)) + base.substring(1);
		switch(pace) {
			case EAGER:
				return "Eagerly " + lower;
			case ROUGH:
				return "Roughly " + lower;
			case NORMAL:
			default:
				return base;
		}
	}

	/** Null when this button is not a pace. Position, soap, finish, and uncover stay plain. */
	private static BathPace shownPace(BathAction action, BathPace pace) {
		if(action.getSetsPace() != null) {
			return action.getSetsPace();
		}
		if(pace != null) {
			return pace;
		}
		if(isRepeatable(action)) {
			return Bath.getPace(Main.game.getPlayer());
		}
		return null;
	}

	/** Same icons sex uses. There is no dominant eager pace, so eager uses the eager submissive icon. */
	private static SexPace toSexPace(BathPace pace) {
		if(pace == null) {
			return null;
		}
		switch(pace) {
			case EAGER:
				return SexPace.SUB_EAGER;
			case ROUGH:
				return SexPace.DOM_ROUGH;
			case NORMAL:
			default:
				return SexPace.DOM_NORMAL;
		}
	}

	private static Colour paceColour(BathPace pace) {
		if(pace == null) {
			return null;
		}
		switch(pace) {
			case EAGER:
				return PresetColour.BASE_PINK_DEEP;
			case ROUGH:
				return PresetColour.BASE_CRIMSON;
			case NORMAL:
			default:
				return PresetColour.BASE_PINK;
		}
	}

	private static Response actionResponse(BathAction action, BathPace pace, String title) {
		GameCharacter player = Main.game.getPlayer();
		String tooltip = action.getTooltip(player, pace);
		int seconds = action.getSeconds();
		BathPace shown = shownPace(action, pace);
		return new Response(title, tooltip, WASH) {
			@Override
			public void effects() {
				Bath.perform(action, pace);
			}
			@Override
			public int getSecondsPassed() {
				return seconds;
			}
			@Override
			public SexPace getSexPace() {
				return toSexPace(shown);
			}
			@Override
			public Colour getHighlightColour() {
				Colour colour = paceColour(shown);
				return colour == null ? super.getHighlightColour() : colour;
			}
		};
	}

	private static Response uncoverResponse(BathArea area, Map.Entry<AbstractClothing, DisplacementType> blocking) {
		if(!canMove(blocking)) {
			return new Response(area.getUncoverTitle(), "That will not come off, so this part cannot be washed yet.", null);
		}
		return new Response(area.getUncoverTitle(),
				"One minute. Moves it aside. Does not wash it, and does not wash the part yet.",
				WASH) {
			@Override
			public void effects() {
				Bath.uncover(area);
			}
			@Override
			public int getSecondsPassed() {
				return UNCOVER_SECONDS;
			}
		};
	}

	private static boolean canMove(Map.Entry<AbstractClothing, DisplacementType> blocking) {
		GameCharacter player = Main.game.getPlayer();
		AbstractClothing clothing = blocking.getKey();
		if(blocking.getValue() == DisplacementType.REMOVE_OR_EQUIP) {
			return player.isAbleToUnequip(clothing, true, player);
		}
		return player.isAbleToBeDisplaced(clothing, blocking.getValue(), false, true, player);
	}

	private static List<BathTab> tabs() {
		List<BathTab> tabs = new ArrayList<>();
		tabs.add(BathTab.HEAD);
		tabs.add(BathTab.BODY);
		tabs.add(BathTab.LOWER);
		if(hasExtra()) {
			tabs.add(BathTab.EXTRA);
		}
		tabs.add(BathTab.POSITION);
		tabs.add(BathTab.PACE);
		tabs.add(BathTab.OPTIONS);
		return tabs;
	}

	private static BathTab tabAt(int index) {
		List<BathTab> tabs = tabs();
		if(index < 0 || index >= tabs.size()) {
			return null;
		}
		return tabs.get(index);
	}

	private static boolean hasExtra() {
		GameCharacter player = Main.game.getPlayer();
		for(BathArea area : BathArea.values()) {
			if(area.getTab() == BathTab.EXTRA && area.isAvailable(player)) {
				return true;
			}
		}
		return false;
	}

	private static List<BathAction> actionsOn(BathTab tab) {
		GameCharacter player = Main.game.getPlayer();
		List<BathAction> found = new ArrayList<>();
		for(BathAction action : BathActionManager.getAll()) {
			if(!action.isShown(player)) {
				continue;
			}
			if(tabFor(action) != tab) {
				continue;
			}
			found.add(action);
		}
		found.sort(Comparator
				.comparingInt((BathAction action) -> action.getCategory() == BathCategory.SOAK ? -1 : 0)
				.thenComparingInt(BathDialogue::orderOf)
				.thenComparingInt(BathAction::getButtonPriority)
				.thenComparingInt(action -> action.getCategory() == BathCategory.DEEP ? 1 : 0));
		return found;
	}

	private static int orderOf(BathAction action) {
		if(action.getSetsPosition() != null) {
			return action.getSetsPosition().ordinal();
		}
		if(action.getSetsPace() != null) {
			return action.getSetsPace().ordinal();
		}
		if(action.getArea() != null) {
			return action.getArea().ordinal();
		}
		return 0;
	}

	private static BathTab tabFor(BathAction action) {
		switch(action.getCategory()) {
			case POSITION:
				return BathTab.POSITION;
			case PACE:
				return BathTab.PACE;
			case SOAK:
				return BathTab.LOWER;
			case DEEP:
			case WASH:
			default:
				if(action.getArea() == null) {
					return BathTab.BODY;
				}
				return action.getArea().getTab();
		}
	}
}
