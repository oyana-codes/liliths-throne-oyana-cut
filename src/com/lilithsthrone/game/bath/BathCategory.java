package com.lilithsthrone.game.bath;

/** Which tab a bath action belongs on. Options such as soap and leaving are not actions. */
public enum BathCategory {
	WASH,
	/** Rinse inside an orifice. Hidden while that orifice is plugged or sealed. */
	DEEP,
	SOAK,
	POSITION,
	PACE
}
