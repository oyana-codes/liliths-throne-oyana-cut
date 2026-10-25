package com.lilithsthrone.controller;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import com.lilithsthrone.controller.eventListeners.tooltips.TooltipInformationEventListener;
import com.lilithsthrone.controller.eventListeners.tooltips.TooltipInventoryEventListener;
import com.lilithsthrone.game.dialogue.places.dominion.lilayashome.LaundryDialogue;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.inventory.InventorySlot;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.main.Main;

/**
 * Clicks on the ensuite laundry page.
 * Start and Stop run immediately. Everything else is selected, then a response button moves it.
 */
public class LaundryController {

	private LaundryController() {
	}

	public static void initListeners() {
		Document document = MainController.document;
		if(document == null) {
			return;
		}
		NodeList nodes = document.getElementsByTagName("*");
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if(!(node instanceof EventTarget) || node.getAttributes() == null || node.getAttributes().getNamedItem("id") == null) {
				continue;
			}
			String id = node.getAttributes().getNamedItem("id").getNodeValue();
			if(id == null || !id.startsWith("laundry-")) {
				continue;
			}
			String elementId = id;
			EventTarget target = (EventTarget) node;
			target.addEventListener("click", event -> {
				LaundryDialogue.onClick(elementId);
				Main.game.setContent(new Response("", "", LaundryDialogue.LAUNDRY));
			}, false);
			addTooltip(elementId);
		}
	}

	private static void addTooltip(String id) {
		if(id.startsWith("laundry-equip-")) {
			AbstractClothing clothing = LaundryDialogue.clothingFor(id);
			if(clothing != null) {
				MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setClothing(clothing, Main.game.getPlayer(), null));
				return;
			}
			try {
				InventorySlot slot = InventorySlot.valueOf(id.substring(id.lastIndexOf('-') + 1));
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation(slot.getName(), "Empty. Click a garment, then choose Wear."));
			} catch(IllegalArgumentException ex) {
			}
			return;
		}
		if(id.startsWith("laundry-bag-cloth-") || id.startsWith("laundry-drum-")) {
			AbstractClothing clothing = LaundryDialogue.clothingFor(id);
			if(clothing != null) {
				MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setClothing(clothing, Main.game.getPlayer(), null));
			} else if(id.startsWith("laundry-drum-")) {
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation("Empty", "Click a garment, then choose the washer or the dryer."));
			}
			return;
		}
		if(id.startsWith("laundry-bag-empty-")) {
			MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation("Inventory", "Click a garment or a bottle, then choose where it goes."));
			return;
		}
		if(id.startsWith("laundry-bag-supply-") || id.startsWith("laundry-supply-")) {
			AbstractItem item = LaundryDialogue.supplyFor(id);
			if(item != null) {
				MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setItem(item, Main.game.getPlayer(), null));
				return;
			}
			if(id.endsWith("washer")) {
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation("Detergent", "One bottle sits in this slot. 1 to 6 pieces uses 2 fl oz, 7 to 16 uses 4, and 17 to 24 uses 6."));
			} else if(id.endsWith("dryer")) {
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation("Dryer sheets", "One box sits in this slot. A sheet is optional, and the dryer still runs without one."));
			}
			return;
		}
		if(id.startsWith("laundry-start-")) {
			MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation("Start", "The cycle follows how many pieces are inside. 1 to 6 is 30 minutes, 7 to 16 is 60, and 17 to 24 is 90. It keeps running if you leave."));
			return;
		}
		if(id.startsWith("laundry-stop-")) {
			MainController.addTooltipListeners(id, new TooltipInformationEventListener().setInformation("Stop", "Open the door. What has already been used stays used."));
		}
	}
}
