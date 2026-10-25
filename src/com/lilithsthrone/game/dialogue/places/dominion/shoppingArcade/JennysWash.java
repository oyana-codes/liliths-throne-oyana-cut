package com.lilithsthrone.game.dialogue.places.dominion.shoppingArcade;

import com.lilithsthrone.game.character.npc.dominion.Jenny;
import com.lilithsthrone.game.character.npc.dominion.JennysSoaps;
import com.lilithsthrone.game.dialogue.DialogueFlagValue;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.dialogue.responses.ResponseTrade;
import com.lilithsthrone.main.Main;

/**
 * @since 0.4.11.9
 * @version 0.4.11.9
 * @author oyana
 */
public class JennysWash {

	public static final DialogueNode EXTERIOR = new DialogueNode("Jenny's Wash (Exterior)", "", false) {

		@Override
		public String getContent() {
			if(!Main.game.isExtendedWorkTime()) {
				return "<p>"
							+ "The lights are off, and the door to Jenny's Wash is locked. The machines inside are still."
						+ "</p>";
			}
			return "<p>"
						+ "Washers are turning behind the glass. The sign over the door reads 'Jenny's Wash'."
					+ "</p>";
		}

		@Override
		public String getResponseTabTitle(int index) {
			return ShoppingArcadeDialogue.getCoreResponseTab(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if(responseTab==0) {
				if(index==1) {
					if(!Main.game.isExtendedWorkTime()) {
						return new Response("Enter", "Jenny's Wash is currently closed. You'll have to come back during opening hours.", null);
					}
					return new Response("Enter", "Step inside Jenny's Wash.", INTERIOR);
				}
			}
			return ShoppingArcadeDialogue.getFastTravelResponses(responseTab, index);
		}
	};

	public static final DialogueNode INTERIOR = new DialogueNode("Jenny's Wash", "", true) {

		@Override
		public String getContent() {
			if(!Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.jennyIntroduced)) {
				return "<p>"
							+ "You push the door open and step inside. Washers are turning along the back wall, and the air is warm. A cow-girl looks up from behind the counter."
						+ "</p>"
						+ "<p>"
							+ "[jenny.speech(Oh, hello. I'm Jenny. Detergent and dryer sheets are on the counter.)]"
						+ "</p>";
			}
			return "<p>"
						+ "You step back into Jenny's Wash. The machines are still going, and Jenny is behind the counter."
					+ "</p>"
					+ "<p>"
						+ "[jenny.speech(Hello again. Detergent and dryer sheets are on the counter.)]"
					+ "</p>";
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new ResponseTrade("Supplies", "Buy detergent, dryer sheets, and freshener.", Main.game.getNpc(Jenny.class)) {
					@Override
					public void effects() {
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.jennyIntroduced, true);
					}
				};

			} else if(index==2) {
				if(Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.jennySoapOffered)) {
					return new ResponseTrade("Soap", "Look at the soap Jenny makes.", Main.game.getNpc(JennysSoaps.class)) {
						@Override
						public void effects() {
							Main.game.getDialogueFlags().setFlag(DialogueFlagValue.jennyIntroduced, true);
						}
					};
				}
				return new Response("Talk", "Ask Jenny what else she sells.", SOAP) {
					@Override
					public void effects() {
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.jennyIntroduced, true);
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.jennySoapOffered, true);
					}
				};

			} else if(index==0) {
				return new Response("Leave", "Leave Jenny's Wash.", EXTERIOR) {
					@Override
					public void effects() {
						Main.game.setResponseTab(0);
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.jennyIntroduced, true);
					}
				};

			} else {
				return null;
			}
		}
	};

	public static final DialogueNode SOAP = new DialogueNode("Jenny's Wash", "", true, true) {

		@Override
		public String getContent() {
			return "<p>"
						+ "[pc.speech(Is it just laundry?)]"
					+ "</p>"
					+ "<p>"
						+ "Jenny nods at the machines, then at a shelf behind the counter."
					+ "</p>"
					+ "<p>"
						+ "[jenny.speech(I make soap in the back, while the machines are going. It's just a little thing. Did you want to look?)]"
					+ "</p>";
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Continue", "Have a look at what else is on the counter.", INTERIOR);
			}
			return null;
		}
	};
}
