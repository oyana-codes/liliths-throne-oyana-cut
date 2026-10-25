package com.lilithsthrone.controller;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import com.lilithsthrone.game.dialogue.places.dominion.lilayashome.LaundryDialogue;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.main.Main;

/**
 * Clicks and drags on the ensuite laundry page.
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
			if(id.startsWith("laundry-drop-")) {
				((EventTarget) node).addEventListener("dragover", event -> event.preventDefault(), false);
				((EventTarget) node).addEventListener("dragenter", event -> event.preventDefault(), false);
				((EventTarget) node).addEventListener("drop", event -> {
					event.preventDefault();
					LaundryDialogue.onDrop(LaundryDialogue.dragSource, elementId);
					LaundryDialogue.dragSource = "";
					Main.game.setContent(new Response("", "", LaundryDialogue.LAUNDRY));
				}, false);
				continue;
			}
			((EventTarget) node).addEventListener("click", event -> {
				LaundryDialogue.onClick(elementId);
				Main.game.setContent(new Response("", "", LaundryDialogue.LAUNDRY));
			}, false);
			((EventTarget) node).addEventListener("dragstart", event -> {
				LaundryDialogue.dragSource = elementId;
				allowHtmlDrag(event, elementId);
			}, false);
		}
	}

	/** WebKit ignores a drag that sets no data. The Java DOM event may not expose the transfer. */
	private static void allowHtmlDrag(org.w3c.dom.events.Event event, String elementId) {
		try {
			Object transfer = event.getClass().getMethod("getDataTransfer").invoke(event);
			if(transfer != null) {
				transfer.getClass().getMethod("setData", String.class, String.class).invoke(transfer, "text/plain", elementId);
			}
		} catch(Exception ex) {
		}
	}
}
