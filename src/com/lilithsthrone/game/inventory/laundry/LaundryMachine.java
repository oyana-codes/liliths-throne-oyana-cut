package com.lilithsthrone.game.inventory.laundry;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.main.Main;

/**
 * One home washer or dryer. The cycle is a timestamp on the game clock, so it finishes while the player is away.
 */
public class LaundryMachine {

	public enum Kind {
		WASHER,
		DRYER;
	}

	public enum Phase {
		IDLE,
		RUNNING,
		DONE;
	}

	public enum LoadSize {
		SMALL(4, 30 * 60, 2, "Small"),
		MEDIUM(8, 60 * 60, 4, "Medium"),
		LARGE(12, 90 * 60, 6, "Large");

		private final int capacity;
		private final int seconds;
		private final int ounces;
		private final String label;

		LoadSize(int capacity, int seconds, int ounces, String label) {
			this.capacity = capacity;
			this.seconds = seconds;
			this.ounces = ounces;
			this.label = label;
		}

		public int getCapacity() {
			return capacity;
		}

		public int getSeconds() {
			return seconds;
		}

		public int getOunces() {
			return ounces;
		}

		public String getLabel() {
			return label;
		}

		public static LoadSize fromName(String name) {
			for(LoadSize size : values()) {
				if(size.name().equals(name)) {
					return size;
				}
			}
			return SMALL;
		}
	}

	public static final int DRUM_LIMIT = LoadSize.LARGE.getCapacity();

	private final Kind kind;
	private Phase phase = Phase.IDLE;
	private LoadSize loadSize = LoadSize.MEDIUM;
	private long readyAtSeconds;
	private String rememberedScentId = "";
	private int rememberedScentSeconds;
	private final List<AbstractClothing> drum = new ArrayList<>();
	private AbstractItem supply;

	public LaundryMachine(Kind kind) {
		this.kind = kind;
	}

	public Kind getKind() {
		return kind;
	}

	public Phase getPhase() {
		return phase;
	}

	public LoadSize getLoadSize() {
		return loadSize;
	}

	public List<AbstractClothing> getDrum() {
		return drum;
	}

	public AbstractItem getSupply() {
		return supply;
	}

	public boolean isLocked() {
		return phase == Phase.RUNNING;
	}

	public int getRemainingSeconds() {
		if(phase != Phase.RUNNING) {
			return 0;
		}
		long remaining = readyAtSeconds - Main.game.getSecondsPassed();
		if(remaining < 0) {
			return 0;
		}
		if(remaining > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) remaining;
	}

	public String getTimeLabel() {
		int seconds = getRemainingSeconds();
		if(phase == Phase.DONE) {
			return "Finished";
		}
		if(phase != Phase.RUNNING) {
			return loadSize.getLabel() + ", " + (loadSize.getSeconds() / 60) + " min";
		}
		int minutes = Math.max(1, (seconds + 59) / 60);
		return minutes + " min left";
	}

	public void finishIfReady() {
		if(phase == Phase.RUNNING && Main.game.getSecondsPassed() >= readyAtSeconds) {
			complete();
		}
	}

	public String setLoadSize(LoadSize size) {
		if(isLocked()) {
			return "The dial won't turn while it's running.";
		}
		this.loadSize = size;
		return "";
	}

	public String addClothing(GameCharacter owner, AbstractClothing clothing) {
		finishIfReady();
		if(isLocked() || phase == Phase.DONE) {
			return phase == Phase.DONE ? "Empty it before loading another wash." : "The door is shut.";
		}
		if(clothing == null || clothing.isMilkingEquipment()) {
			return "That doesn't go in.";
		}
		if(drum.size() >= DRUM_LIMIT) {
			return "The drum is full.";
		}
		if(kind == Kind.DRYER && !clothing.isWet()) {
			return "The washer comes first.";
		}
		if(!owner.getInventory().removeClothing(clothing, 1)) {
			return "That isn't in your inventory.";
		}
		drum.add(new AbstractClothing(clothing) {});
		return "";
	}

	public String takeClothing(GameCharacter owner, int index) {
		finishIfReady();
		if(isLocked()) {
			return "The door is shut.";
		}
		if(index < 0 || index >= drum.size()) {
			return "";
		}
		AbstractClothing clothing = drum.remove(index);
		owner.addClothing(clothing, false);
		if(drum.isEmpty() && phase == Phase.DONE) {
			phase = Phase.IDLE;
		}
		return "";
	}

	public String moveClothingTo(LaundryMachine other, int index) {
		finishIfReady();
		other.finishIfReady();
		if(isLocked() || other.isLocked() || other.phase == Phase.DONE) {
			return "That machine isn't ready.";
		}
		if(index < 0 || index >= drum.size()) {
			return "";
		}
		AbstractClothing clothing = drum.get(index);
		if(other.kind == Kind.DRYER && !clothing.isWet()) {
			return "The washer comes first.";
		}
		if(other.drum.size() >= DRUM_LIMIT) {
			return "The drum is full.";
		}
		other.drum.add(drum.remove(index));
		if(drum.isEmpty() && phase == Phase.DONE) {
			phase = Phase.IDLE;
		}
		return "";
	}

	public String setSupply(GameCharacter owner, AbstractItem item) {
		finishIfReady();
		if(isLocked()) {
			return "The dispenser is shut.";
		}
		if(item == null) {
			return takeSupply(owner);
		}
		boolean washerSupply = kind == Kind.WASHER && LaundrySupplies.isDetergent(item);
		boolean dryerSupply = kind == Kind.DRYER && LaundrySupplies.isDryerSheet(item);
		if(!washerSupply && !dryerSupply) {
			return kind == Kind.WASHER ? "The washer takes detergent." : "The dryer takes a box of sheets.";
		}
		if(supply != null) {
			owner.addItem(supply, false);
			supply = null;
		}
		if(!owner.getInventory().removeItem(item, 1)) {
			return "That isn't in your inventory.";
		}
		AbstractItem copy = Main.game.getItemGen().generateItem(item.getItemType());
		copy.setColour(0, item.getColour(0));
		copy.setSupplyRemaining(item.getSupplyRemaining());
		supply = copy;
		return "";
	}

	public String takeSupply(GameCharacter owner) {
		finishIfReady();
		if(isLocked()) {
			return "The dispenser is shut.";
		}
		if(supply == null) {
			return "";
		}
		owner.addItem(supply, false);
		supply = null;
		return "";
	}

	public String start() {
		finishIfReady();
		if(phase != Phase.IDLE) {
			return phase == Phase.DONE ? "Empty it first." : "It's already running.";
		}
		if(drum.isEmpty()) {
			return "The drum is empty.";
		}
		if(drum.size() > loadSize.getCapacity()) {
			return "That's more than a " + loadSize.getLabel().toLowerCase() + " load. Turn the dial up, or take something out.";
		}
		rememberedScentId = "";
		rememberedScentSeconds = 0;
		if(kind == Kind.WASHER) {
			if(supply == null || !LaundrySupplies.isDetergent(supply)) {
				return "It needs detergent.";
			}
			int ounces = loadSize.getOunces();
			if(supply.getSupplyRemaining() < ounces) {
				return "This bottle only has " + supply.getSupplyRemaining() + " fl oz left. A " + loadSize.getLabel().toLowerCase() + " load needs " + ounces + ".";
			}
			rememberedScentId = LaundrySupplies.scentId(supply);
			rememberedScentSeconds = LaundrySupplies.scentSeconds(supply);
			int left = supply.getSupplyRemaining() - ounces;
			if(left <= 0) {
				supply = null;
			} else {
				supply.setSupplyRemaining(left);
			}
		} else if(supply != null && LaundrySupplies.isDryerSheet(supply)) {
			rememberedScentId = LaundrySupplies.scentId(supply);
			rememberedScentSeconds = LaundrySupplies.scentSeconds(supply);
			int left = supply.getSupplyRemaining() - 1;
			if(left <= 0) {
				supply = null;
			} else {
				supply.setSupplyRemaining(left);
			}
		}
		phase = Phase.RUNNING;
		readyAtSeconds = Main.game.getSecondsPassed() + loadSize.getSeconds();
		return "";
	}

	public String stop(GameCharacter owner) {
		finishIfReady();
		if(phase != Phase.RUNNING) {
			return "";
		}
		phase = Phase.IDLE;
		rememberedScentId = "";
		rememberedScentSeconds = 0;
		if(kind == Kind.WASHER) {
			for(AbstractClothing clothing : drum) {
				clothing.setWet(true);
			}
		}
		unload(owner);
		return "";
	}

	public void unload(GameCharacter owner) {
		if(phase == Phase.RUNNING) {
			return;
		}
		for(AbstractClothing clothing : drum) {
			owner.addClothing(clothing, false);
		}
		drum.clear();
		phase = Phase.IDLE;
	}

	private void complete() {
		if(kind == Kind.WASHER) {
			for(AbstractClothing clothing : drum) {
				clothing.clearConditionInPlace();
				clothing.setWet(true);
				clothing.setPendingFreshScent(rememberedScentId, rememberedScentSeconds);
			}
		} else {
			for(AbstractClothing clothing : drum) {
				clothing.setWet(false);
				if(rememberedScentId != null && !rememberedScentId.isEmpty() && rememberedScentSeconds > 0) {
					clothing.setPendingFreshScent("", 0);
					clothing.setFreshScent(null, rememberedScentId, LaundrySupplies.SCENT_STRENGTH, rememberedScentSeconds);
				} else if(clothing.hasPendingFreshScent()) {
					clothing.setFreshScent(null, clothing.getPendingFreshScentId(), LaundrySupplies.SCENT_STRENGTH, clothing.getPendingFreshScentSeconds());
					clothing.setPendingFreshScent("", 0);
				} else {
					clothing.setPendingFreshScent("", 0);
				}
			}
		}
		rememberedScentId = "";
		rememberedScentSeconds = 0;
		phase = Phase.DONE;
	}

	public Element saveAsXML(Element parent, Document doc) {
		Element element = doc.createElement("machine");
		parent.appendChild(element);
		XMLUtil.addAttribute(doc, element, "kind", kind.name());
		XMLUtil.addAttribute(doc, element, "phase", phase.name());
		XMLUtil.addAttribute(doc, element, "loadSize", loadSize.name());
		XMLUtil.addAttribute(doc, element, "readyAtSeconds", String.valueOf(readyAtSeconds));
		XMLUtil.addAttribute(doc, element, "scentId", rememberedScentId == null ? "" : rememberedScentId);
		XMLUtil.addAttribute(doc, element, "scentSeconds", String.valueOf(rememberedScentSeconds));
		if(supply != null) {
			Element supplyElement = doc.createElement("supply");
			element.appendChild(supplyElement);
			supply.saveAsXML(supplyElement, doc);
		}
		Element drumElement = doc.createElement("drum");
		element.appendChild(drumElement);
		for(AbstractClothing clothing : drum) {
			clothing.saveAsXML(drumElement, doc);
		}
		return element;
	}

	public static LaundryMachine loadFromXML(Element parent, Document doc) {
		Kind kind;
		try {
			kind = Kind.valueOf(parent.getAttribute("kind"));
		} catch(Exception ex) {
			kind = Kind.WASHER;
		}
		LaundryMachine machine = new LaundryMachine(kind);
		try {
			machine.phase = Phase.valueOf(parent.getAttribute("phase"));
		} catch(Exception ex) {
			machine.phase = Phase.IDLE;
		}
		machine.loadSize = LoadSize.fromName(parent.getAttribute("loadSize"));
		try {
			machine.readyAtSeconds = Long.parseLong(parent.getAttribute("readyAtSeconds"));
		} catch(Exception ex) {
			machine.readyAtSeconds = 0;
		}
		machine.rememberedScentId = parent.getAttribute("scentId");
		try {
			if(!parent.getAttribute("scentSeconds").isEmpty()) {
				machine.rememberedScentSeconds = Integer.parseInt(parent.getAttribute("scentSeconds"));
			}
		} catch(Exception ex) {
			machine.rememberedScentSeconds = 0;
		}
		NodeList supplies = parent.getElementsByTagName("supply");
		if(supplies.getLength() > 0) {
			Element supplyElement = (Element) supplies.item(0);
			NodeList items = supplyElement.getElementsByTagName("item");
			if(items.getLength() > 0) {
				machine.supply = AbstractItem.loadFromXML((Element) items.item(0), doc);
			}
		}
		NodeList drums = parent.getElementsByTagName("drum");
		if(drums.getLength() > 0) {
			Element drumElement = (Element) drums.item(0);
			NodeList children = drumElement.getChildNodes();
			for(int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if(node instanceof Element && "clothing".equals(((Element) node).getTagName())) {
					AbstractClothing clothing = AbstractClothing.loadFromXML((Element) node, doc);
					if(clothing != null) {
						machine.drum.add(clothing);
					}
				}
			}
		}
		return machine;
	}
}
