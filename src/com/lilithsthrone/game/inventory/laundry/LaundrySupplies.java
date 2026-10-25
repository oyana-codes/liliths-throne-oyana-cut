package com.lilithsthrone.game.inventory.laundry;

import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.item.AbstractItem;

/**
 * Detergent bottles and dryer-sheet boxes bought from Jenny.
 * A bottle holds one gallon. A box holds forty sheets.
 */
public final class LaundrySupplies {

	public static final int DETERGENT_OUNCES = 128;
	public static final int SHEET_COUNT = 40;
	public static final int SCENT_STRENGTH = 80;
	public static final int SHEET_SCENT_SECONDS = 72 * 60 * 60;

	private LaundrySupplies() {
	}

	public static boolean isDetergent(AbstractItem item) {
		return item != null && item.getItemType().getId().startsWith("oyana_cleaning_") && item.getItemType().getId().endsWith("_detergent");
	}

	public static boolean isDryerSheet(AbstractItem item) {
		return item != null && item.getItemType().getId().startsWith("oyana_cleaning_") && item.getItemType().getId().endsWith("_dryer_sheet");
	}

	public static boolean isSupply(AbstractItem item) {
		return isDetergent(item) || isDryerSheet(item);
	}

	/** Short scent name, or empty when the product leaves no scent. */
	public static String scentId(AbstractItem item) {
		if(item == null) {
			return "";
		}
		String id = item.getItemType().getId();
		if(id.contains("unscented")) {
			return "";
		}
		if(id.contains("lavender")) {
			return "lavender";
		}
		if(id.contains("citrus")) {
			return "citrus";
		}
		if(id.contains("linen")) {
			return "linen";
		}
		return "";
	}

	public static int scentSeconds(AbstractItem item) {
		if(scentId(item).isEmpty()) {
			return 0;
		}
		if(isDryerSheet(item)) {
			return SHEET_SCENT_SECONDS;
		}
		return AbstractClothing.FRESH_SCENT_DURATION_SECONDS;
	}

	public static String fillLabel(AbstractItem item) {
		if(isDetergent(item)) {
			return item.getSupplyRemaining() + "/" + DETERGENT_OUNCES + " fl oz";
		}
		if(isDryerSheet(item)) {
			return item.getSupplyRemaining() + " sheets";
		}
		return "";
	}
}
