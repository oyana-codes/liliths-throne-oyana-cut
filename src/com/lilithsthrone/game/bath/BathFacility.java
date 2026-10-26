package com.lilithsthrone.game.bath;

/**
 * What a wash place allows. A later home can sell other presets. The flags are queried by name from bath XML.
 * {@link #BOTH} is Lilaya's ensuite: luxury, shower and bath, a loose shower head, room for two, and space for a taur.
 */
public final class BathFacility {

	/** A standing shower for one. The head comes off the hook. A taur body does not fit. */
	public static final BathFacility SHOWER = new BathFacility(true, false, true, 1, false, false);
	/** A tub for one, with no shower and no loose head. A taur body does not fit. */
	public static final BathFacility BATH = new BathFacility(false, true, false, 1, false, false);
	/** Lilaya's ensuite. Luxury, shower, tub, loose head, room for two, and space for a taur. */
	public static final BathFacility BOTH = new BathFacility(true, true, true, 2, true, true);
	/** A tub for one, with a hose or head that can be taken down. A taur body does not fit. */
	public static final BathFacility BATH_WITH_HEAD = new BathFacility(false, true, true, 1, false, false);

	private final boolean shower;
	private final boolean bath;
	private final boolean handheld;
	private final int capacity;
	private final boolean fitsTaur;
	private final boolean luxury;

	/** A place that is not luxury, fits one person, and does not fit a taur. */
	public BathFacility(boolean shower, boolean bath, boolean handheld) {
		this(shower, bath, handheld, 1, false, false);
	}

	/**
	 * @param capacity How many people can be in the water at once. At least 1.
	 * @param fitsTaur False when a taur or feral body has to wash badly, because the room was not built for one.
	 * @param luxury A deep, well-fitted room. Lilaya's ensuite is luxury. A bought tub does not have to be.
	 */
	public BathFacility(boolean shower, boolean bath, boolean handheld, int capacity, boolean fitsTaur, boolean luxury) {
		this.shower = shower;
		this.bath = bath;
		this.handheld = handheld;
		this.capacity = Math.max(1, capacity);
		this.fitsTaur = fitsTaur;
		this.luxury = luxury;
	}

	public boolean hasShower() {
		return shower;
	}

	public boolean hasBath() {
		return bath;
	}

	public boolean hasHandheld() {
		return handheld;
	}

	public int getCapacity() {
		return capacity;
	}

	public boolean fitsTaur() {
		return fitsTaur;
	}

	public boolean isLuxury() {
		return luxury;
	}

	public boolean hasRoomForMoreThanOne() {
		return capacity > 1;
	}

	/** One sentence for the opening of the scene. */
	public String describe() {
		if(luxury && shower && bath && handheld && fitsTaur && capacity > 1) {
			return "This is a luxury ensuite, with a shower, a deep bath, room for " + capacity + " people, and space for a taur. The shower head lifts off its hook.";
		}
		StringBuilder text = new StringBuilder();
		if(shower && bath) {
			text.append(luxury ? "This is a luxury room, with a shower and a bath" : "The room has a shower and a bath");
		} else if(shower) {
			text.append(luxury ? "This is a luxury shower" : "There is a shower");
		} else if(bath) {
			text.append(luxury ? "This is a luxury bath" : "There is a bath");
		} else {
			text.append("There is water to wash with");
		}
		if(capacity > 1) {
			text.append(", with room for ").append(capacity).append(" people");
		} else {
			text.append(", only big enough for one person");
		}
		if(fitsTaur) {
			text.append(", and space for a taur body");
		} else {
			text.append(". A taur or feral body will not fit properly");
		}
		if(handheld) {
			text.append(". The shower head lifts off its hook");
		}
		text.append(".");
		return text.toString();
	}

	/**
	 * @param requirement SHOWER, BATH, HANDHELD, BOTH, MULTI, TAUR, LUXURY, or ANY. Unknown values do not match.
	 */
	public boolean meets(String requirement) {
		if(requirement == null || requirement.isEmpty() || "ANY".equals(requirement)) {
			return true;
		}
		switch(requirement) {
			case "SHOWER":
				return shower;
			case "BATH":
				return bath;
			case "HANDHELD":
				return handheld;
			case "BOTH":
				return shower && bath;
			case "MULTI":
				return capacity > 1;
			case "TAUR":
				return fitsTaur;
			case "LUXURY":
				return luxury;
			default:
				return false;
		}
	}
}
