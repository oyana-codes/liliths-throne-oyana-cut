package com.lilithsthrone.game.dialogue.places.dominion.lilayashome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.valueEnums.Femininity;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.inventory.InventorySlot;
import com.lilithsthrone.game.inventory.Rarity;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.game.inventory.laundry.LaundryMachine;
import com.lilithsthrone.game.inventory.laundry.LaundryMachine.LoadSize;
import com.lilithsthrone.game.inventory.laundry.LaundrySupplies;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.rendering.RenderingEngine;
import com.lilithsthrone.rendering.SVGImages;
import com.lilithsthrone.utils.colours.PresetColour;

/**
 * The washer and dryer in the player's ensuite.
 * Click a garment or a bottle, then use the response buttons. Each drum is its own 24-slot inventory.
 */
public class LaundryDialogue {

	private static String note = "";
	private static String selectedId = "";

	/** Eight tiles a row, three rows, so all four panes fit above the response buttons. */
	private static final int COLUMNS = 8;
	private static final int PANE_SLOTS = 24;

	public static final DialogueNode LAUNDRY = new DialogueNode("Laundry", "", true) {
		@Override
		public String getContent() {
			return getPage();
		}

		@Override
		public String getResponseTabTitle(int index) {
			if(index == 0) {
				return "Machines";
			}
			if(index == 1) {
				return "Selected item";
			}
			return null;
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			// Drop any clock preview from the previous Wait before asking how long is left.
			LaundryMachine.setPreviewAdvance(0);
			refresh();
			if(responseTab == 1) {
				return selectedResponse(index);
			}
			if(responseTab != 0) {
				return null;
			}
			if(index == 1) {
				return new Response("Leave", "Step back into your room. A running machine keeps going.", RoomPlayer.ROOM);
			}
			if(index == 2) {
				int seconds = soonestRemainingSeconds();
				if(seconds <= 0) {
					return new Response("Wait here", "Neither machine is running.", null);
				}
				int minutes = Math.max(1, (seconds + 59) / 60);
				final int waited = seconds;
				return new Response("Wait here", "Wait about " + minutes + " minutes, until the next machine finishes.", LAUNDRY) {
					@Override
					public void effects() {
						finishWaitedMachines(waited);
					}

					@Override
					public int getSecondsPassed() {
						return waited;
					}
				};
			}
			if(index == 3) {
				return runningResponse("Stop washer", Main.game.getHomeWasher());
			}
			if(index == 4) {
				return runningResponse("Stop dryer", Main.game.getHomeDryer());
			}
			if(index == 5) {
				return unloadResponse("Unload washer", Main.game.getHomeWasher());
			}
			if(index == 6) {
				return unloadResponse("Unload dryer", Main.game.getHomeDryer());
			}
			if(index == 7) {
				return unloadAndWearResponse("Washer wear", Main.game.getHomeWasher());
			}
			if(index == 8) {
				return unloadAndWearResponse("Dryer wear", Main.game.getHomeDryer());
			}
			return null;
		}
	};

	private static Response runningResponse(String title, LaundryMachine machine) {
		if(machine.getPhase() != LaundryMachine.Phase.RUNNING) {
			return new Response(title, "It isn't running.", null);
		}
		return new Response(title, "Open the door. What's already been used stays used.", LAUNDRY) {
			@Override
			public void effects() {
				machine.stop(Main.game.getPlayer());
				note = "You opened it early.";
			}
		};
	}

	private static Response unloadResponse(String title, LaundryMachine machine) {
		if(machine.getPhase() == LaundryMachine.Phase.RUNNING || machine.isDrumEmpty()) {
			return new Response(title, machine.getPhase() == LaundryMachine.Phase.RUNNING ? "The door is shut." : "There is nothing in there.", null);
		}
		return new Response(title, "Take everything out.", LAUNDRY) {
			@Override
			public void effects() {
				machine.unload(Main.game.getPlayer());
				note = "";
				clearSelection();
			}
		};
	}

	private static Response unloadAndWearResponse(String title, LaundryMachine machine) {
		if(machine.getPhase() == LaundryMachine.Phase.RUNNING) {
			return new Response(title, "The door is shut.", null);
		}
		if(machine.isDrumEmpty()) {
			return new Response(title, "There is nothing in there.", null);
		}
		if(!machine.hasRememberedOutfit()) {
			return new Response(title, "Nothing in here was being worn. Use Unload to take it out.", null);
		}
		String where = machine.getKind() == LaundryMachine.Kind.WASHER ? "washer" : "dryer";
		return new Response(title, "Unload the " + where + " and put back on anything you were wearing. Wet clothes go into your bag.", LAUNDRY) {
			@Override
			public void effects() {
				note = unloadAndWear(machine);
				clearSelection();
			}
		};
	}

	/** Completes every machine this wait will finish, then previews the clock so the page matches the time that is about to pass. */
	private static void finishWaitedMachines(int waited) {
		boolean washer = false;
		boolean dryer = false;
		for(LaundryMachine machine : machines()) {
			if(machine.getPhase() != LaundryMachine.Phase.RUNNING) {
				continue;
			}
			int remaining = machine.getRemainingSeconds();
			if(remaining > 0 && remaining <= waited) {
				machine.completeNow();
				if(machine.getKind() == LaundryMachine.Kind.WASHER) {
					washer = true;
				} else {
					dryer = true;
				}
			}
		}
		LaundryMachine.setPreviewAdvance(waited);
		if(washer && dryer) {
			note = "Both machines finished.";
		} else if(washer) {
			note = "The washer finished. The clothes are clean and still wet.";
		} else if(dryer) {
			note = "The dryer finished. The clothes are dry.";
		}
	}

	private static Response selectedResponse(int index) {
		if(selectedId.isEmpty()) {
			return null;
		}
		if(index == 0) {
			return new Response("Back", "Return to the machine controls.", LAUNDRY) {
				@Override
				public void effects() {
					clearSelection();
				}
			};
		}
		if(selectedId.startsWith("laundry-supply-") || selectedId.startsWith("laundry-bag-supply-")) {
			return supplyResponse(index);
		}
		AbstractClothing clothing = clothingFor(selectedId);
		if(clothing == null) {
			return null;
		}
		if(selectedId.startsWith("laundry-drum-washer")) {
			return drumResponse(index, clothing, Main.game.getHomeWasher(), Main.game.getHomeDryer());
		}
		if(selectedId.startsWith("laundry-drum-dryer")) {
			return drumResponse(index, clothing, Main.game.getHomeDryer(), Main.game.getHomeWasher());
		}
		if(index == 1) {
			return storeResponse("Washer", Main.game.getHomeWasher());
		}
		if(index == 2) {
			return storeResponse("Dryer", Main.game.getHomeDryer());
		}
		return null;
	}

	/** Options for a garment sitting in one machine. Index 1 takes it out, 2 moves it to the other, 3 wears it. */
	private static Response drumResponse(int index, AbstractClothing clothing, LaundryMachine here, LaundryMachine other) {
		if(index == 1) {
			return takeOutResponse();
		}
		if(index == 2) {
			String title = other.getKind() == LaundryMachine.Kind.WASHER ? "To washer" : "To dryer";
			return storeResponse(title, other);
		}
		if(index == 3) {
			return wearResponse(clothing, here);
		}
		return null;
	}

	private static Response supplyResponse(int index) {
		AbstractItem item = supplyFor(selectedId);
		boolean inMachine = selectedId.startsWith("laundry-supply-");
		if(inMachine) {
			if(index == 1) {
				return takeOutSupplyResponse();
			}
			return null;
		}
		if(item == null) {
			return null;
		}
		if(index == 1) {
			return supplyStoreResponse("Washer", Main.game.getHomeWasher(), item);
		}
		if(index == 2) {
			return supplyStoreResponse("Dryer", Main.game.getHomeDryer(), item);
		}
		return null;
	}

	private static Response storeResponse(String title, LaundryMachine dest) {
		GameCharacter player = Main.game.getPlayer();
		AbstractClothing clothing = clothingFor(selectedId);
		if(clothing == null) {
			return null;
		}
		String where = dest.getKind() == LaundryMachine.Kind.WASHER ? "washer" : "dryer";
		if(selectedId.startsWith("laundry-drum-") && machineOf(selectedId) == dest) {
			return new Response(title, "It is already in the " + where + ".", null);
		}
		if(selectedId.startsWith("laundry-equip-") && !canUnequip(player, clothing)) {
			return new Response(title, "It will not come off.", null);
		}
		if(selectedId.startsWith("laundry-drum-") && machineOf(selectedId) != null && machineOf(selectedId).isLocked()) {
			return new Response(title, "The door is shut.", null);
		}
		String rejection = dest.previewRejection(clothing, dest.firstEmptySlot());
		if(!rejection.isEmpty()) {
			return new Response(title, rejection, null);
		}
		return new Response(title, "Put this in the " + where + ".", LAUNDRY) {
			@Override
			public void effects() {
				putSelectedIn(dest);
			}
		};
	}

	private static Response takeOutResponse() {
		LaundryMachine source = machineOf(selectedId);
		if(source == null) {
			return null;
		}
		if(source.isLocked()) {
			return new Response("Take out", "The door is shut.", null);
		}
		return new Response("Take out", "Put this back in your inventory.", LAUNDRY) {
			@Override
			public void effects() {
				takeOutSelected();
			}
		};
	}

	private static Response wearResponse(AbstractClothing clothing, LaundryMachine here) {
		if(here.isLocked()) {
			return new Response("Wear", "The door is shut.", null);
		}
		GameCharacter player = Main.game.getPlayer();
		InventorySlot remembered = here.getWornFrom(indexOf(selectedId));
		InventorySlot slot = remembered != null ? remembered : firstWearableSlot(player, clothing);
		String rejection = equipRejection(player, clothing, slot);
		if(!rejection.isEmpty()) {
			return new Response("Wear", rejection, null);
		}
		String tooltip = remembered != null ? "Put it back on where you were wearing it." : "Put it on.";
		return new Response("Wear", tooltip, LAUNDRY) {
			@Override
			public void effects() {
				wearSelected();
			}
		};
	}

	private static Response takeOutSupplyResponse() {
		LaundryMachine source = "laundry-supply-dryer".equals(selectedId) ? Main.game.getHomeDryer() : Main.game.getHomeWasher();
		if(source.isLocked()) {
			return new Response("Take out", "The dispenser is shut.", null);
		}
		return new Response("Take out", "Put this back in your inventory.", LAUNDRY) {
			@Override
			public void effects() {
				String result = source.takeSupply(Main.game.getPlayer());
				note = result.isEmpty() ? "You took it out." : result;
				if(result.isEmpty()) {
					clearSelection();
				}
			}
		};
	}

	private static Response supplyStoreResponse(String title, LaundryMachine dest, AbstractItem item) {
		boolean accepts = dest.getKind() == LaundryMachine.Kind.WASHER ? LaundrySupplies.isDetergent(item) : LaundrySupplies.isDryerSheet(item);
		String where = dest.getKind() == LaundryMachine.Kind.WASHER ? "washer" : "dryer";
		if(!accepts) {
			return new Response(title, dest.getKind() == LaundryMachine.Kind.WASHER ? "The washer takes detergent." : "The dryer takes a box of sheets.", null);
		}
		if(dest.isLocked()) {
			return new Response(title, "The dispenser is shut.", null);
		}
		return new Response(title, "Set this in the " + where + ".", LAUNDRY) {
			@Override
			public void effects() {
				AbstractItem current = supplyFor(selectedId);
				if(current == null) {
					note = "That is not there anymore.";
					clearSelection();
					return;
				}
				String result = dest.setSupply(Main.game.getPlayer(), current);
				note = result.isEmpty() ? "You set it in the " + where + "." : result;
				if(result.isEmpty()) {
					clearSelection();
				}
			}
		};
	}

	public static int soonestRemainingSeconds() {
		refresh();
		int soonest = 0;
		for(LaundryMachine machine : machines()) {
			if(machine.getPhase() == LaundryMachine.Phase.RUNNING) {
				int remaining = machine.getRemainingSeconds();
				if(remaining > 0 && (soonest == 0 || remaining < soonest)) {
					soonest = remaining;
				}
			}
		}
		return soonest;
	}

	private static void refresh() {
		for(LaundryMachine machine : machines()) {
			machine.finishIfReady();
		}
	}

	private static List<LaundryMachine> machines() {
		List<LaundryMachine> machines = new ArrayList<>();
		machines.add(Main.game.getHomeWasher());
		machines.add(Main.game.getHomeDryer());
		return machines;
	}

	private static String getPage() {
		refresh();
		if(!selectedId.isEmpty() && clothingFor(selectedId) == null && supplyFor(selectedId) == null) {
			clearSelection();
		}
		if(selectedId.isEmpty()) {
			Main.game.setResponseTab(0);
		}
		GameCharacter player = Main.game.getPlayer();
		String background = PresetColour.BACKGROUND.toWebHexString();
		StringBuilder sb = new StringBuilder();
		if(note != null && !note.isEmpty()) {
			sb.append("<p style='margin:0 0 4px 0;'><i>").append(escape(note)).append("</i></p>");
		}
		if(!selectedId.isEmpty()) {
			String label = selectedLabel();
			if(!label.isEmpty()) {
				sb.append("<p style='margin:0 0 4px 0;'>Selected: <b>").append(escape(label)).append("</b></p>");
			}
		}
		appendEquipped(sb, player, background);
		appendMachine(sb, Main.game.getHomeWasher(), background);
		sb.append("<div style='clear:both;'></div>");
		appendBag(sb, player, background);
		appendMachine(sb, Main.game.getHomeDryer(), background);
		sb.append("<div style='clear:both;'></div>");
		return sb.toString();
	}

	/** Same half-width float the shop inventories use, so the four panes sit in two rows. */
	private static void openPane(StringBuilder sb, String background, String titleHtml) {
		sb.append("<div class='container-half-width' style='margin:2px; background:").append(background).append("; border:1px solid #555;'>");
		sb.append("<p style='width:100%; text-align:center; padding:0; margin:0 0 2px 0;'>").append(titleHtml).append("</p>");
	}

	private static void closePane(StringBuilder sb) {
		sb.append("<div style='clear:both;'></div></div>");
	}

	private static void appendEquipped(StringBuilder sb, GameCharacter player, String background) {
		String nameColour = Femininity.valueOf(player.getFemininityValue()).getColour().toWebHexString();
		openPane(sb, background, "<b style='color:" + nameColour + ";'>Equipped</b>");
		Set<InventorySlot> covered = coveredSlots(player);
		for(InventorySlot slot : RenderingEngine.mainInventorySlots) {
			appendEquipSlot(sb, player, slot, covered);
		}
		for(InventorySlot slot : RenderingEngine.secondaryInventorySlots) {
			appendEquipSlot(sb, player, slot, covered);
		}
		for(InventorySlot slot : RenderingEngine.piercingSlots) {
			appendEquipSlot(sb, player, slot, covered);
		}
		closePane(sb);
	}

	private static void appendEquipSlot(StringBuilder sb, GameCharacter player, InventorySlot slot, Set<InventorySlot> covered) {
		AbstractClothing clothing = player.getClothingInSlot(slot);
		if(clothing == null && !slot.isPhysicallyAvailable(player)) {
			return;
		}
		String id = "laundry-equip-" + slot.name();
		boolean blocked = clothing == null && covered.contains(slot);
		if(clothing == null) {
			sb.append(equipEmptySlot(id, slot, blocked));
			return;
		}
		sb.append(itemSlot(id, clothing.getSVGEquippedString(player), clothingBackground(clothing), clothingBadges(clothing, 1), false));
	}

	private static Set<InventorySlot> coveredSlots(GameCharacter player) {
		Set<InventorySlot> covered = new HashSet<>();
		for(AbstractClothing clothing : player.getClothingCurrentlyEquipped()) {
			List<InventorySlot> blocked = clothing.getIncompatibleSlots(player, clothing.getSlotEquippedTo());
			if(blocked != null) {
				covered.addAll(blocked);
			}
		}
		return covered;
	}

	private static void appendBag(StringBuilder sb, GameCharacter player, String background) {
		String nameColour = Femininity.valueOf(player.getFemininityValue()).getColour().toWebHexString();
		openPane(sb, background, "<b style='color:" + nameColour + ";'>Inventory</b>");
		int shown = 0;
		int supplyIndex = 0;
		for(Map.Entry<AbstractItem, Integer> entry : player.getAllItemsInInventory().entrySet()) {
			if(!LaundrySupplies.isSupply(entry.getKey())) {
				continue;
			}
			AbstractItem item = entry.getKey();
			sb.append(itemSlot("laundry-bag-supply-" + supplyIndex, item.getSVGString(), item.getRarity().getBackgroundColour().toWebHexString(), supplyBadges(item, entry.getValue()), false));
			supplyIndex++;
			shown++;
		}
		int clothIndex = 0;
		for(Map.Entry<AbstractClothing, Integer> entry : player.getAllClothingInInventory().entrySet()) {
			AbstractClothing clothing = entry.getKey();
			if(clothing.isMilkingEquipment()) {
				continue;
			}
			sb.append(itemSlot("laundry-bag-cloth-" + clothIndex, clothing.getSVGString(), clothingBackground(clothing), clothingBadges(clothing, entry.getValue()), false));
			clothIndex++;
			shown++;
		}
		int cells = Math.max(PANE_SLOTS, shown);
		int remainder = cells % COLUMNS;
		if(remainder != 0) {
			cells += COLUMNS - remainder;
		}
		for(int i = 0; i < cells - shown; i++) {
			sb.append(emptySlot("laundry-bag-empty-" + i, false));
		}
		closePane(sb);
	}

	private static void appendMachine(StringBuilder sb, LaundryMachine machine, String background) {
		String kind = kindName(machine);
		String title = machine.getKind() == LaundryMachine.Kind.WASHER ? "Washer" : "Dryer";
		boolean locked = machine.getPhase() == LaundryMachine.Phase.RUNNING;
		String aqua = PresetColour.BASE_AQUA.toWebHexString();
		openPane(sb, background, "<b style='color:" + aqua + ";'>" + title + "</b> <b>" + machine.getOccupiedCount() + "/" + LaundryMachine.DRUM_LIMIT + "</b>");
		for(int i = 0; i < machine.getDrum().size(); i++) {
			AbstractClothing clothing = machine.getDrum().get(i);
			String id = "laundry-drum-" + kind + "-" + i;
			if(clothing == null) {
				sb.append(emptySlot(id, locked));
			} else {
				sb.append(itemSlot(id, clothing.getSVGString(), clothingBackground(clothing), clothingBadges(clothing, 1), locked));
			}
		}
		sb.append("<div style='clear:both; width:100%;'>");
		sb.append("<p style='width:100%; text-align:center; padding:1px 0; margin:0;'><b style='color:").append(aqua).append(";'>").append(escape(footerText(machine))).append("</b></p>");
		AbstractItem supply = machine.getSupply();
		String supplyId = "laundry-supply-" + kind;
		if(supply == null) {
			sb.append(emptySlot(supplyId, locked));
		} else {
			sb.append(itemSlot(supplyId, supply.getSVGString(), supply.getRarity().getBackgroundColour().toWebHexString(), supplyBadges(supply, 1), locked));
		}
		boolean idle = machine.getPhase() == LaundryMachine.Phase.IDLE;
		boolean running = machine.getPhase() == LaundryMachine.Phase.RUNNING;
		sb.append(machineButton("laundry-start-" + kind, "Start", idle, aqua));
		sb.append(machineButton("laundry-stop-" + kind, "Stop", running, aqua));
		sb.append("<div style='clear:both;'></div></div>");
		closePane(sb);
	}

	private static String machineButton(String id, String label, boolean enabled, String colour) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div id='").append(id).append("' class='normal-button");
		if(!enabled) {
			sb.append(" disabled");
		}
		sb.append("' style='float:left; width:28%; margin:0.4% 1% 0 1%; padding:8px 0; text-align:center;");
		if(enabled) {
			sb.append(" color:").append(colour).append(";");
		}
		sb.append("'>").append(label).append("</div>");
		return sb.toString();
	}

	private static String footerText(LaundryMachine machine) {
		if(machine.getPhase() == LaundryMachine.Phase.RUNNING) {
			return machine.getTimeLabel();
		}
		if(machine.getPhase() == LaundryMachine.Phase.DONE) {
			return "Finished";
		}
		int count = machine.getOccupiedCount();
		if(count <= 0) {
			return "Empty";
		}
		LoadSize size = LoadSize.forCount(count);
		String text = size.getLabel() + ", " + (size.getSeconds() / 60) + " min";
		if(machine.getKind() == LaundryMachine.Kind.WASHER) {
			text += ", " + size.getOunces() + " oz";
		}
		return text;
	}

	private static final String SLOT_STYLE = "width:11%; margin:0.5%;";

	private static String emptySlot(String id, boolean locked) {
		return "<div class='inventory-item-slot laundry-slot laundry-drop" + (locked ? " disabled" : "") + "' id='" + id + "' style='" + SLOT_STYLE + "'></div>";
	}

	private static String equipEmptySlot(String id, InventorySlot slot, boolean locked) {
		return "<div class='inventory-item-slot laundry-slot laundry-drop" + (locked ? " disabled" : "") + "' id='" + id + "' style='" + SLOT_STYLE + "'>"
				+ "<div style='position:absolute; left:0; width:100%; top:28%; text-align:center; font-size:0.6em; line-height:1.05; color:#9a9a9a;'>"
				+ escape(slot.getName()) + "</div></div>";
	}

	private static String itemSlot(String id, String svg, String background, String badges, boolean locked) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='inventory-item-slot laundry-slot laundry-drop");
		if(id.equals(selectedId)) {
			sb.append(" laundry-hover");
		}
		if(locked) {
			sb.append(" disabled");
		}
		sb.append("' id='").append(id).append("' style='").append(SLOT_STYLE).append(" background-color:").append(background).append(";'>");
		if(svg != null && !svg.isEmpty()) {
			sb.append("<div class='inventory-icon-content'>").append(svg).append("</div>");
		}
		if(badges != null) {
			sb.append(badges);
		}
		sb.append("</div>");
		return sb.toString();
	}

	private static String clothingBackground(AbstractClothing clothing) {
		if(!clothing.isEnchantmentKnown()) {
			return PresetColour.RARITY_UNKNOWN_BACKGROUND.toWebHexString();
		}
		return clothing.getRarity().getBackgroundColour().toWebHexString();
	}

	private static String clothingBadges(AbstractClothing clothing, int count) {
		StringBuilder sb = new StringBuilder();
		if(clothing.showsFreshScent()) {
			sb.append("<div class='cummedIcon'>").append(SVGImages.SVG_IMAGE_PROVIDER.getFreshScentIcon()).append("</div>");
		} else {
			if(clothing.isSweaty()) {
				sb.append("<div class='soilIcon soil-sweat'>").append(SVGImages.SVG_IMAGE_PROVIDER.getSweatIcon()).append("</div>");
			}
			if(clothing.isMusky()) {
				sb.append("<div class='soilIcon soil-musk'>").append(SVGImages.SVG_IMAGE_PROVIDER.getMuskIcon()).append("</div>");
			}
			if(clothing.isGrimy()) {
				sb.append("<div class='soilIcon soil-grime'>").append(SVGImages.SVG_IMAGE_PROVIDER.getGrimeIcon()).append("</div>");
			}
			if(clothing.isDirty()) {
				sb.append("<div class='soilIcon soil-fluids'>").append(SVGImages.SVG_IMAGE_PROVIDER.getDirtyIcon()).append("</div>");
			}
		}
		if(count > 1) {
			sb.append("<div class='item-count'>x").append(count).append("</div>");
		}
		if(clothing.isWet()) {
			sb.append("<div class='item-count' style='left:2px; right:auto; color:").append(PresetColour.BASE_AQUA.toWebHexString()).append(";'>wet</div>");
		}
		return sb.toString();
	}

	private static String supplyBadges(AbstractItem item, int count) {
		StringBuilder sb = new StringBuilder();
		if(count > 1) {
			sb.append("<div class='item-count'>x").append(count).append("</div>");
		}
		String fill = "";
		if(LaundrySupplies.isDetergent(item)) {
			fill = item.getSupplyRemaining() + " oz";
		} else if(LaundrySupplies.isDryerSheet(item)) {
			fill = item.getSupplyRemaining() + " sheets";
		}
		if(!fill.isEmpty()) {
			sb.append("<div class='item-price'>").append(escape(fill)).append("</div>");
		}
		return sb.toString();
	}

	public static AbstractClothing clothingFor(String id) {
		if(id == null) {
			return null;
		}
		GameCharacter player = Main.game.getPlayer();
		if(id.startsWith("laundry-equip-")) {
			InventorySlot slot = slotOf(id);
			return slot == null ? null : player.getClothingInSlot(slot);
		}
		if(id.startsWith("laundry-bag-cloth-")) {
			return clothingAt(player, indexOf(id));
		}
		if(id.startsWith("laundry-drum-")) {
			LaundryMachine machine = machineOf(id);
			int index = indexOf(id);
			if(machine == null || index < 0 || index >= machine.getDrum().size()) {
				return null;
			}
			return machine.getDrum().get(index);
		}
		return null;
	}

	public static AbstractItem supplyFor(String id) {
		if(id == null) {
			return null;
		}
		if(id.startsWith("laundry-bag-supply-")) {
			return supplyAt(Main.game.getPlayer(), indexOf(id));
		}
		if("laundry-supply-washer".equals(id)) {
			return Main.game.getHomeWasher().getSupply();
		}
		if("laundry-supply-dryer".equals(id)) {
			return Main.game.getHomeDryer().getSupply();
		}
		return null;
	}

	private static String escape(String text) {
		if(text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&#34;").replace("'", "&#39;");
	}

	public static void onClick(String id) {
		if(id == null || id.isEmpty()) {
			return;
		}
		if(id.startsWith("laundry-start-") || id.startsWith("laundry-stop-")) {
			actMachineButton(id);
			return;
		}
		if(id.equals(selectedId)) {
			clearSelection();
			return;
		}
		if(clothingFor(id) != null || supplyFor(id) != null) {
			selectedId = id;
			note = "";
			Main.game.setResponseTab(1);
			return;
		}
		clearSelection();
	}

	private static void clearSelection() {
		selectedId = "";
		Main.game.setResponseTab(0);
	}

	private static void actMachineButton(String id) {
		LaundryMachine machine = id.contains("washer") ? Main.game.getHomeWasher() : Main.game.getHomeDryer();
		if(id.startsWith("laundry-start-")) {
			if(machine.getPhase() != LaundryMachine.Phase.IDLE) {
				return;
			}
			note = machine.start();
			return;
		}
		if(machine.getPhase() != LaundryMachine.Phase.RUNNING) {
			return;
		}
		machine.stop(Main.game.getPlayer());
		note = "You opened it early.";
	}

	private static void putSelectedIn(LaundryMachine dest) {
		GameCharacter player = Main.game.getPlayer();
		String sourceId = selectedId;
		AbstractClothing clothing = clothingFor(sourceId);
		if(clothing == null) {
			note = "That is not there anymore.";
			clearSelection();
			return;
		}
		boolean moved = moveOntoMachine(player, sourceId, clothing, dest, dest.firstEmptySlot());
		if(moved || clothingFor(sourceId) == null) {
			clearSelection();
		}
	}

	private static void takeOutSelected() {
		GameCharacter player = Main.game.getPlayer();
		LaundryMachine source = machineOf(selectedId);
		int index = indexOf(selectedId);
		if(source == null || index < 0 || index >= source.getDrum().size() || source.getDrum().get(index) == null) {
			note = "That is not there anymore.";
			clearSelection();
			return;
		}
		if(source.isLocked()) {
			note = "The door is shut.";
			return;
		}
		String result = source.takeClothing(player, index);
		note = result.isEmpty() ? "You took it out." : result;
		clearSelection();
	}

	private static void wearSelected() {
		GameCharacter player = Main.game.getPlayer();
		String sourceId = selectedId;
		AbstractClothing clothing = clothingFor(sourceId);
		if(clothing == null) {
			note = "That is not there anymore.";
			clearSelection();
			return;
		}
		LaundryMachine here = machineOf(sourceId);
		if(here != null && here.isLocked()) {
			note = "The door is shut.";
			return;
		}
		InventorySlot remembered = here == null ? null : here.getWornFrom(indexOf(sourceId));
		InventorySlot slot = remembered != null ? remembered : firstWearableSlot(player, clothing);
		boolean worn = moveOntoBody(player, sourceId, clothing, slot);
		if(worn || clothingFor(sourceId) == null) {
			clearSelection();
		}
	}

	private static boolean moveOntoMachine(GameCharacter player, String sourceId, AbstractClothing clothing, LaundryMachine dest, int slot) {
		String where = dest.getKind() == LaundryMachine.Kind.WASHER ? "washer" : "dryer";
		if(sourceId.startsWith("laundry-drum-")) {
			LaundryMachine source = machineOf(sourceId);
			int index = indexOf(sourceId);
			if(source == dest && index == slot) {
				return false;
			}
			String result = source.moveClothingTo(dest, index, slot);
			note = result.isEmpty() ? "You moved it to the " + where + "." : result;
			return result.isEmpty();
		}
		String rejection = dest.rejectionFor(clothing, slot);
		if(!rejection.isEmpty()) {
			note = rejection;
			return false;
		}
		InventorySlot fromSlot = null;
		if(sourceId.startsWith("laundry-equip-")) {
			fromSlot = slotOf(sourceId);
			if(!takeOff(player, clothing)) {
				return false;
			}
		}
		String result = dest.addClothing(player, clothing, slot, fromSlot);
		note = result.isEmpty() ? "You put it in the " + where + "." : result;
		return result.isEmpty();
	}

	/** @return true when the garment is actually sitting in the slot afterwards. */
	private static boolean moveOntoBody(GameCharacter player, String sourceId, AbstractClothing clothing, InventorySlot slot) {
		String rejection = equipRejection(player, clothing, slot);
		if(!rejection.isEmpty()) {
			note = rejection;
			return false;
		}
		if(sourceId.startsWith("laundry-drum-")) {
			LaundryMachine source = machineOf(sourceId);
			int index = indexOf(sourceId);
			String taken = source.takeClothing(player, index);
			if(!taken.isEmpty() || !player.hasClothing(clothing)) {
				note = taken.isEmpty() ? "The door is shut." : taken;
				return false;
			}
		} else if(sourceId.startsWith("laundry-equip-") && !takeOff(player, clothing)) {
			return false;
		}
		// automaticClothingManagement must be true. With false, a free slot still reports success and never equips, after the garment has already been removed.
		player.equipClothingFromInventory(clothing, slot, true, player, player);
		if(player.getClothingInSlot(slot) == null) {
			if(!player.hasClothing(clothing)) {
				player.addClothing(clothing, false);
			}
			note = "That does not go there.";
			return false;
		}
		note = "You put it back on.";
		return true;
	}

	private static String unloadAndWear(LaundryMachine machine) {
		if(machine.getPhase() == LaundryMachine.Phase.RUNNING) {
			return "The door is shut.";
		}
		GameCharacter player = Main.game.getPlayer();
		List<Integer> remembered = new ArrayList<>();
		List<Integer> plain = new ArrayList<>();
		for(int i = 0; i < machine.getDrum().size(); i++) {
			if(machine.getDrum().get(i) == null) {
				continue;
			}
			if(machine.getWornFrom(i) != null) {
				remembered.add(i);
			} else {
				plain.add(i);
			}
		}
		remembered.sort((a, b) -> Integer.compare(machine.getWornFrom(a).getZLayer(), machine.getWornFrom(b).getZLayer()));
		int worn = 0;
		int wet = 0;
		int bag = 0;
		for(int index : remembered) {
			AbstractClothing clothing = machine.getDrum().get(index);
			InventorySlot slot = machine.getWornFrom(index);
			if(clothing.isWet()) {
				machine.takeClothing(player, index);
				wet++;
				continue;
			}
			if(!equipRejection(player, clothing, slot).isEmpty()) {
				machine.takeClothing(player, index);
				bag++;
				continue;
			}
			machine.takeClothing(player, index);
			if(!player.hasClothing(clothing)) {
				bag++;
				continue;
			}
			player.equipClothingFromInventory(clothing, slot, true, player, player);
			if(player.getClothingInSlot(slot) == null) {
				if(!player.hasClothing(clothing)) {
					player.addClothing(clothing, false);
				}
				bag++;
			} else {
				worn++;
			}
		}
		for(int index : plain) {
			if(machine.getDrum().get(index) != null) {
				machine.takeClothing(player, index);
				bag++;
			}
		}
		if(worn > 0 && wet == 0 && bag == 0) {
			return "You put everything back on.";
		}
		if(worn > 0 && wet > 0) {
			return "You put some of it back on. The wet clothes went into your bag.";
		}
		if(worn > 0) {
			return "You put some of it back on. The rest went into your bag.";
		}
		if(wet > 0) {
			return "They are still wet, so they went into your bag.";
		}
		return "";
	}

	private static InventorySlot firstWearableSlot(GameCharacter player, AbstractClothing clothing) {
		List<InventorySlot> slots = clothing.getClothingType().getEquipSlots();
		if(slots == null || slots.isEmpty()) {
			return null;
		}
		for(InventorySlot slot : slots) {
			if(equipRejection(player, clothing, slot).isEmpty()) {
				return slot;
			}
		}
		return slots.get(0);
	}

	private static String selectedLabel() {
		AbstractClothing clothing = clothingFor(selectedId);
		if(clothing != null) {
			String name = clothing.getName();
			if(name == null) {
				name = "";
			}
			if(clothing.getClothingType().isAppendColourName() && clothing.getColours() != null && !clothing.getColours().isEmpty()) {
				name = clothing.getColourName() + " " + name;
			}
			return name;
		}
		AbstractItem item = supplyFor(selectedId);
		if(item != null && item.getName() != null) {
			return item.getName();
		}
		return "";
	}

	private static String equipRejection(GameCharacter player, AbstractClothing clothing, InventorySlot slot) {
		if(slot == null || slot.isWeapon() || clothing.getClothingType().getEquipSlots() == null || !clothing.getClothingType().getEquipSlots().contains(slot)) {
			return "That doesn't go there.";
		}
		if(player.getClothingInSlot(slot) != null && player.getClothingInSlot(slot) != clothing) {
			return "That slot is taken.";
		}
		if(clothing.isWet()) {
			return "It is soaking wet, and has to dry before it can be worn.";
		}
		if(wouldDisplaceSomething(player, clothing, slot)) {
			return "That doesn't go there.";
		}
		if(!player.isAbleToEquip(clothing, slot, false, player)) {
			return "That doesn't go there.";
		}
		return "";
	}

	/** True when putting this garment in that slot would have to take something else off. */
	private static boolean wouldDisplaceSomething(GameCharacter player, AbstractClothing clothing, InventorySlot slot) {
		for(AbstractClothing worn : player.getClothingCurrentlyEquipped()) {
			if(worn == clothing) {
				continue;
			}
			List<InventorySlot> blocked = worn.getIncompatibleSlots(player, worn.getSlotEquippedTo());
			if(blocked != null && blocked.contains(slot)) {
				return true;
			}
		}
		List<InventorySlot> needsClear = clothing.getIncompatibleSlots(player, slot);
		if(needsClear == null) {
			return false;
		}
		for(InventorySlot other : needsClear) {
			AbstractClothing worn = player.getClothingInSlot(other);
			if(worn != null && worn != clothing) {
				return true;
			}
		}
		return false;
	}

	/** Puts a worn garment into the inventory. A full inventory would drop it on the floor, so that case is refused. */
	private static boolean takeOff(GameCharacter player, AbstractClothing clothing) {
		if(!canUnequip(player, clothing)) {
			note = "It will not come off.";
			return false;
		}
		if(player.isInventoryFull() && clothing.getRarity() != Rarity.QUEST && !player.hasClothing(clothing)) {
			note = "Your inventory is full.";
			return false;
		}
		player.unequipClothingIntoInventory(clothing, true, player);
		if(!player.hasClothing(clothing)) {
			note = "It will not come off.";
			return false;
		}
		note = "";
		return true;
	}

	private static boolean canUnequip(GameCharacter player, AbstractClothing clothing) {
		return player.getInventory().isAbleToUnequip(clothing, false, true, player, player);
	}

	private static LaundryMachine machineOf(String id) {
		if(id.contains("dryer")) {
			return Main.game.getHomeDryer();
		}
		if(id.contains("washer")) {
			return Main.game.getHomeWasher();
		}
		return null;
	}

	private static String kindName(LaundryMachine machine) {
		return machine.getKind().name().toLowerCase();
	}

	private static InventorySlot slotOf(String id) {
		int dash = id.lastIndexOf('-');
		if(dash < 0 || dash >= id.length() - 1) {
			return null;
		}
		try {
			return InventorySlot.valueOf(id.substring(dash + 1));
		} catch(IllegalArgumentException ex) {
			return null;
		}
	}

	private static int indexOf(String id) {
		int dash = id.lastIndexOf('-');
		try {
			return Integer.parseInt(id.substring(dash + 1));
		} catch(Exception ex) {
			return -1;
		}
	}

	private static AbstractClothing clothingAt(GameCharacter player, int index) {
		int seen = 0;
		for(AbstractClothing clothing : player.getAllClothingInInventory().keySet()) {
			if(clothing.isMilkingEquipment()) {
				continue;
			}
			if(seen == index) {
				return clothing;
			}
			seen++;
		}
		return null;
	}

	private static AbstractItem supplyAt(GameCharacter player, int index) {
		int seen = 0;
		for(AbstractItem item : player.getAllItemsInInventory().keySet()) {
			if(!LaundrySupplies.isSupply(item)) {
				continue;
			}
			if(seen == index) {
				return item;
			}
			seen++;
		}
		return null;
	}
}
