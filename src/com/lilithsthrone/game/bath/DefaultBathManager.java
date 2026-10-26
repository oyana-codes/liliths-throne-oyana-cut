package com.lilithsthrone.game.bath;

import java.util.ArrayList;
import java.util.List;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.dialogue.places.dominion.lilayashome.BathArea;
import com.lilithsthrone.utils.Util;

/**
 * Partners wash their own dirty parts. If the dirty part is out of reach, they change their own
 * position to soaking or the shower head, and wash on a later turn. They do not change the player's pace.
 */
public class DefaultBathManager implements BathManager {

	@Override
	public BathAction chooseAction(GameCharacter performer, BathAction playerAction) {
		boolean filthyOutOfReach = false;
		boolean submergedFilthy = false;
		for(BathArea area : BathArea.values()) {
			if(!area.isAvailable(performer) || !area.isFilthy(performer)) {
				continue;
			}
			if(area.isSubmerged()) {
				submergedFilthy = true;
			}
			if(!Bath.canReach(performer, area)) {
				filthyOutOfReach = true;
			}
		}

		List<BathAction> filthyWashes = new ArrayList<>();
		List<BathAction> moving = new ArrayList<>();
		for(BathAction action : BathActionManager.getAll()) {
			if(!action.isPerformable(performer)) {
				continue;
			}
			if(action.getCategory() == BathCategory.WASH || action.getCategory() == BathCategory.SOAK || action.getCategory() == BathCategory.DEEP) {
				BathArea area = action.getArea();
				if(area != null && area.isFilthy(performer)) {
					filthyWashes.add(action);
				} else if(action.getCategory() == BathCategory.SOAK && submergedFilthy) {
					filthyWashes.add(action);
				}
			} else if(action.getCategory() == BathCategory.POSITION
					&& action.getSetsPosition() != null
					&& action.getSetsPosition().reachesHardParts()) {
				moving.add(action);
			}
		}

		if(!filthyWashes.isEmpty()) {
			return Util.randomItemFrom(filthyWashes);
		}
		if(filthyOutOfReach && !moving.isEmpty()) {
			return Util.randomItemFrom(moving);
		}
		return null;
	}
}
