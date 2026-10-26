package com.lilithsthrone.game.bath;

/**
 * How hard the current wash is. The same action reads differently at each pace.
 */
public enum BathPace {
	NORMAL("Normally", "Wash at an easy pace."),
	EAGER("Eagerly", "Wash eagerly. What is said changes."),
	ROUGH("Roughly", "Scrub hard. What is said changes.");

	private final String title;
	private final String tooltip;

	BathPace(String title, String tooltip) {
		this.title = title;
		this.tooltip = tooltip;
	}

	public String getTitle() {
		return title;
	}

	public String getTooltip() {
		return tooltip;
	}
}
