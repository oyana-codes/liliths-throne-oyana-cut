package com.lilithsthrone.game.dialogue.places.dominion.lilayashome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.game.inventory.laundry.LaundryMachine;
import com.lilithsthrone.game.inventory.laundry.LaundryMachine.LoadSize;
import com.lilithsthrone.game.inventory.laundry.LaundrySupplies;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.colours.PresetColour;

/**
 * The washer and dryer in the player's ensuite.
 */
public class LaundryDialogue {

	private static String note = "";
	public static String dragSource = "";
	private static boolean suppressClick;

	public static final DialogueNode LAUNDRY = new DialogueNode("Laundry", "", true) {
		@Override
		public String getContent() {
			return getPage();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			refresh();
			if(index == 1) {
				return new Response("Leave", "Step back into your room. A running machine keeps going.", RoomPlayer.ROOM);
			}
			if(index == 2) {
				int seconds = soonestRemainingSeconds();
				if(seconds <= 0) {
					return new Response("Wait here", "Neither machine is running.", null);
				}
				int minutes = Math.max(1, (seconds + 59) / 60);
				return new Response("Wait here", "Wait about " + minutes + " minutes, until the next machine finishes.", LAUNDRY) {
					@Override
					public int getSecondsPassed() {
						return soonestRemainingSeconds();
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
		if(machine.getPhase() != LaundryMachine.Phase.DONE || machine.getDrum().isEmpty()) {
			return new Response(title, "There's nothing finished in there.", null);
		}
		return new Response(title, "Take everything out.", LAUNDRY) {
			@Override
			public void effects() {
				machine.unload(Main.game.getPlayer());
				note = "";
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
		GameCharacter player = Main.game.getPlayer();
		StringBuilder sb = new StringBuilder();
		sb.append("<style>");
		sb.append(".laundry-row{display:flex;gap:12px;align-items:stretch;}");
		sb.append(".laundry-machine{flex:1;border:1px solid ").append(PresetColour.BASE_AQUA.toWebHexString()).append(";border-radius:10px;padding:8px;min-width:0;}");
		sb.append(".laundry-tile{display:flex;gap:8px;align-items:center;margin:4px 0;padding:4px;border-radius:6px;background:rgba(255,255,255,0.04);cursor:pointer;}");
		sb.append(".laundry-tile svg{width:36px;height:36px;}");
		sb.append(".laundry-dial{display:flex;gap:6px;margin:6px 0;}");
		sb.append(".laundry-dial div{flex:1;text-align:center;padding:6px 0;border-radius:16px;border:1px solid ").append(PresetColour.BASE_GREY.toWebHexString()).append(";cursor:pointer;}");
		sb.append(".laundry-selected{background:").append(PresetColour.BASE_AQUA.toWebHexString()).append(";color:#111;}");
		sb.append(".laundry-button{display:inline-block;margin:4px 6px 0 0;padding:4px 10px;border-radius:6px;background:").append(PresetColour.BASE_AQUA.toWebHexString()).append(";color:#111;cursor:pointer;}");
		sb.append("</style>");
		sb.append("<p>The washer and dryer sit against the ensuite wall. Click a garment to load it. Drag it if you want to choose the other machine. A click on something already in a machine takes it back out, unless the washer has finished, in which case a click sends it to the dryer.</p>");
		if(note != null && !note.isEmpty()) {
			sb.append("<p><i>").append(escape(note)).append("</i></p>");
		}
		sb.append("<div class='laundry-row'>");
		sb.append(machinePanel(Main.game.getHomeWasher()));
		sb.append(machinePanel(Main.game.getHomeDryer()));
		sb.append("</div>");
		sb.append("<div class='laundry-machine' id='laundry-drop-basket' style='margin-top:12px;'>");
		sb.append("<b>Your clothes</b>");
		int index = 0;
		for(Map.Entry<AbstractClothing, Integer> entry : player.getAllClothingInInventory().entrySet()) {
			AbstractClothing clothing = entry.getKey();
			if(clothing.isMilkingEquipment()) {
				continue;
			}
			sb.append(tile("laundry-cloth-basket-" + index, clothing.getSVGString(), clothing.getName() + clothNote(clothing) + (entry.getValue() > 1 ? " x" + entry.getValue() : "")));
			index++;
		}
		if(index == 0) {
			sb.append("<p>No clothes in your inventory.</p>");
		}
		sb.append("<b>Detergent and sheets</b>");
		int supplyIndex = 0;
		for(Map.Entry<AbstractItem, Integer> entry : player.getAllItemsInInventory().entrySet()) {
			if(!LaundrySupplies.isSupply(entry.getKey())) {
				continue;
			}
			sb.append(tile("laundry-supply-basket-" + supplyIndex, entry.getKey().getSVGString(), entry.getKey().getName() + " (" + LaundrySupplies.fillLabel(entry.getKey()) + ")" + (entry.getValue() > 1 ? " x" + entry.getValue() : "")));
			supplyIndex++;
		}
		if(supplyIndex == 0) {
			sb.append("<p>No detergent or dryer sheets.</p>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private static String clothNote(AbstractClothing clothing) {
		if(clothing.isWet()) {
			return " (wet)";
		}
		return "";
	}

	private static String machinePanel(LaundryMachine machine) {
		String kind = machine.getKind().name().toLowerCase();
		String title = machine.getKind() == LaundryMachine.Kind.WASHER ? "Washer" : "Dryer";
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='laundry-machine' id='laundry-drop-").append(kind).append("'>");
		sb.append("<b>").append(title).append("</b> ");
		sb.append("<span style='color:").append(PresetColour.BASE_AQUA.toWebHexString()).append(";'>").append(escape(machine.getTimeLabel())).append("</span>");
		sb.append("<div>").append(machine.getDrum().size()).append(" / ").append(machine.getLoadSize().getCapacity()).append("</div>");
		sb.append("<div class='laundry-dial'>");
		for(LoadSize size : LoadSize.values()) {
			boolean selected = machine.getLoadSize() == size;
			sb.append("<div id='laundry-dial-").append(kind).append("-").append(size.name()).append("'");
			if(selected) {
				sb.append(" class='laundry-selected'");
			}
			sb.append(">").append(size.getLabel()).append("<br/>").append(size.getCapacity()).append("</div>");
		}
		sb.append("</div>");
		if(machine.getPhase() == LaundryMachine.Phase.IDLE) {
			sb.append("<div class='laundry-button' id='laundry-start-").append(kind).append("'>Start</div>");
		}
		int index = 0;
		for(AbstractClothing clothing : machine.getDrum()) {
			sb.append(tile("laundry-cloth-" + kind + "-" + index, clothing.getSVGString(), clothing.getName() + clothNote(clothing)));
			index++;
		}
		if(machine.getDrum().isEmpty()) {
			sb.append("<p>The drum is empty.</p>");
		}
		sb.append("<div id='laundry-supply-").append(kind).append("' class='laundry-tile'>");
		if(machine.getSupply() == null) {
			sb.append(machine.getKind() == LaundryMachine.Kind.WASHER ? "Detergent: empty" : "Sheets: none");
		} else {
			sb.append(escape(machine.getSupply().getName())).append(" (").append(escape(LaundrySupplies.fillLabel(machine.getSupply()))).append(")");
		}
		sb.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private static String tile(String id, String svg, String label) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='laundry-tile' draggable='true' id='").append(id).append("'>");
		if(svg != null && !svg.isEmpty()) {
			sb.append("<div style='width:36px;height:36px;overflow:hidden;'>").append(svg).append("</div>");
		}
		sb.append("<span>").append(escape(label)).append("</span></div>");
		return sb.toString();
	}

	private static String escape(String text) {
		if(text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static void onClick(String id) {
		if(suppressClick) {
			suppressClick = false;
			return;
		}
		if(id == null || id.isEmpty()) {
			return;
		}
		act(id, "");
	}

	public static void onDrop(String sourceId, String zoneId) {
		suppressClick = true;
		if(sourceId == null || sourceId.isEmpty() || zoneId == null) {
			return;
		}
		act(sourceId, zoneId);
	}

	private static void act(String id, String zoneId) {
		refresh();
		GameCharacter player = Main.game.getPlayer();
		if(id.startsWith("laundry-dial-")) {
			String[] parts = id.split("-");
			if(parts.length >= 4) {
				LaundryMachine machine = "washer".equals(parts[2]) ? Main.game.getHomeWasher() : Main.game.getHomeDryer();
				note = machine.setLoadSize(LoadSize.fromName(parts[3]));
			}
			return;
		}
		if(id.startsWith("laundry-start-")) {
			LaundryMachine machine = id.endsWith("washer") ? Main.game.getHomeWasher() : Main.game.getHomeDryer();
			note = machine.start();
			return;
		}
		if(id.startsWith("laundry-supply-")) {
			if(id.contains("basket")) {
				AbstractItem item = supplyAt(player, indexOf(id));
				if(item == null) {
					return;
				}
				LaundryMachine machine = destinationSupply(item, zoneId);
				note = machine.setSupply(player, item);
			} else if(id.endsWith("washer")) {
				note = Main.game.getHomeWasher().takeSupply(player);
			} else if(id.endsWith("dryer")) {
				note = Main.game.getHomeDryer().takeSupply(player);
			}
			return;
		}
		if(!id.startsWith("laundry-cloth-")) {
			return;
		}
		String[] parts = id.split("-");
		if(parts.length < 4) {
			return;
		}
		String where = parts[2];
		int index = indexOf(id);
		if("basket".equals(where)) {
			AbstractClothing clothing = clothingAt(player, index);
			if(clothing == null) {
				return;
			}
			LaundryMachine machine = destinationClothing(clothing, zoneId);
			note = machine.addClothing(player, clothing);
			return;
		}
		LaundryMachine source = "washer".equals(where) ? Main.game.getHomeWasher() : Main.game.getHomeDryer();
		if(zoneId.contains("dryer") && source.getKind() == LaundryMachine.Kind.WASHER) {
			note = source.moveClothingTo(Main.game.getHomeDryer(), index);
			return;
		}
		if(zoneId.contains("washer") && source.getKind() == LaundryMachine.Kind.DRYER) {
			note = source.moveClothingTo(Main.game.getHomeWasher(), index);
			return;
		}
		if(zoneId.contains("basket") || zoneId.isEmpty()) {
			if(zoneId.isEmpty() && source.getKind() == LaundryMachine.Kind.WASHER && source.getPhase() == LaundryMachine.Phase.DONE) {
				note = source.moveClothingTo(Main.game.getHomeDryer(), index);
				return;
			}
			note = source.takeClothing(player, index);
		}
	}

	private static LaundryMachine destinationClothing(AbstractClothing clothing, String zoneId) {
		if(zoneId.contains("dryer")) {
			return Main.game.getHomeDryer();
		}
		if(zoneId.contains("washer")) {
			return Main.game.getHomeWasher();
		}
		if(clothing.isWet()) {
			return Main.game.getHomeDryer();
		}
		return Main.game.getHomeWasher();
	}

	private static LaundryMachine destinationSupply(AbstractItem item, String zoneId) {
		if(zoneId.contains("dryer")) {
			return Main.game.getHomeDryer();
		}
		if(zoneId.contains("washer")) {
			return Main.game.getHomeWasher();
		}
		if(LaundrySupplies.isDryerSheet(item)) {
			return Main.game.getHomeDryer();
		}
		return Main.game.getHomeWasher();
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
