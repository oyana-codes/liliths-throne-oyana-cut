package com.lilithsthrone.game.bath;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.lilithsthrone.game.dialogue.places.dominion.lilayashome.BathArea;
import com.lilithsthrone.utils.Util;

/**
 * Loads {@code res/bath} and {@code res/mods/(author)/bath/actions}. Mods use the same root element.
 */
public class BathActionManager {

	private static final List<BathAction> actions = new ArrayList<>();

	static {
		try {
			load(Util.getExternalModFilesById("/bath/actions"));
			load(Util.getExternalFilesById("res/bath"));
		} catch(Exception ex) {
			System.err.println("Bath actions failed to load.");
			ex.printStackTrace(System.err);
		}
		ensureFallbacks();
	}

	private static void load(Map<String, Map<String, File>> files) {
		for(Entry<String, Map<String, File>> author : files.entrySet()) {
			for(Entry<String, File> file : author.getValue().entrySet()) {
				try {
					if(!"bathAction".equals(Util.getXmlRootElementName(file.getValue()))) {
						continue;
					}
					String id = file.getKey().replace("actions_", "");
					actions.add(new BathAction(file.getValue(), id));
				} catch(Exception ex) {
					System.err.println("Loading bath action failed. File path: " + file.getValue().getAbsolutePath());
					ex.printStackTrace(System.err);
				}
			}
		}
	}

	private static void ensureFallbacks() {
		for(BathArea area : BathArea.values()) {
			if(!hasWash(area)) {
				actions.add(BathAction.fallback(BathCategory.WASH, area, null, null, area.washText()));
			}
			if(!hasDeep(area)) {
				actions.add(BathAction.fallback(BathCategory.DEEP, area, null, null,
						"[npc.Name] [npc.verb(work)] water into the part that needs a deeper rinse, then [npc.verb(let)] it run clear. [npc.speech(Inside as well.)]"));
			}
		}
		if(!hasCategory(BathCategory.SOAK)) {
			actions.add(BathAction.fallback(BathCategory.SOAK, null, null, null,
					"[npc.Name] [npc.verb(lie)] in the water and [npc.verb(let)] it work. [npc.speech(Just soak.)]"));
		}
		for(BathPosition position : BathPosition.values()) {
			if(!hasPosition(position)) {
				actions.add(BathAction.fallback(BathCategory.POSITION, null, position, null,
						"[npc.Name] [npc.verb(move)] into place to keep washing."));
			}
		}
		for(BathPace pace : BathPace.values()) {
			if(!hasPace(pace)) {
				actions.add(BathAction.fallback(BathCategory.PACE, null, null, pace,
						"[npc.Name] [npc.verb(change)] pace. [npc.speech(" + pace.getTitle() + ".)]"));
			}
		}
	}

	private static boolean hasWash(BathArea area) {
		for(BathAction action : actions) {
			if(action.getCategory() == BathCategory.WASH && action.getArea() == area) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasDeep(BathArea area) {
		for(BathAction action : actions) {
			if(action.getCategory() == BathCategory.DEEP && action.getArea() == area) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasCategory(BathCategory category) {
		for(BathAction action : actions) {
			if(action.getCategory() == category) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasPosition(BathPosition position) {
		for(BathAction action : actions) {
			if(action.getSetsPosition() == position) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasPace(BathPace pace) {
		for(BathAction action : actions) {
			if(action.getSetsPace() == pace) {
				return true;
			}
		}
		return false;
	}

	public static List<BathAction> getAll() {
		return Collections.unmodifiableList(actions);
	}
}
