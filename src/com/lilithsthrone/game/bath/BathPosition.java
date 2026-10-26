package com.lilithsthrone.game.bath;

/**
 * How the body is arranged in the water. The position decides which parts can be reached.
 */
public enum BathPosition {
	STANDING("Stand", "Stand under the water.", "Shower"),
	SITTING("Sit", "Sit, so the hands can reach the legs and groin.", "Sitting wash"),
	LEG_UP("Leg up", "Prop one foot up to reach between the legs.", "Leg up"),
	SOAKING("Get in", "Sink into the bath. The water reaches what the hands cannot.", "Bath"),
	HANDHELD("Shower head", "Take the shower head down and spray what the hands cannot reach.", "Shower head");

	private final String title;
	private final String tooltip;
	private final String sceneLabel;

	BathPosition(String title, String tooltip, String sceneLabel) {
		this.title = title;
		this.tooltip = tooltip;
		this.sceneLabel = sceneLabel;
	}

	public String getTitle() {
		return title;
	}

	public String getTooltip() {
		return tooltip;
	}

	public String getSceneLabel() {
		return sceneLabel;
	}

	/** Soaking and a loose shower head reach a feral rear that hands cannot. */
	public boolean reachesHardParts() {
		return this == SOAKING || this == HANDHELD;
	}

	/** Standing is a poor angle for the groin and rear. The other poses open them. */
	public boolean allowsHandsOnGroin() {
		return this != STANDING;
	}
}
