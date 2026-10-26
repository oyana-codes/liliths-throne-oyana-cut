package com.lilithsthrone.game.bath;

import com.lilithsthrone.game.character.GameCharacter;

/**
 * Chooses what a non-player does after the player's action.
 * A scene can replace the default before or after {@link Bath#start(BathFacility, BathManager)}.
 */
public interface BathManager {

	/**
	 * @param performer The character whose turn it is. Never the player.
	 * @param playerAction What the player just did, or null for soap and uncover.
	 * @return The action they take, or null to skip their turn.
	 */
	BathAction chooseAction(GameCharacter performer, BathAction playerAction);
}
