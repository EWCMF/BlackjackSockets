package deck;

import java.util.Collections;
import java.util.Stack;

public class Deck {
    Stack<Card> cards = new Stack<>();

    public void makeDeck() {
        for (Suit suit : Suit.values()) {
            for (Face face : Face.values()) {
                Card card = new Card(face.getValue(), face.name(), suit.name());
                cards.add(card);
            }
        }
        Collections.shuffle(cards);
    }

    public Card draw() {
        return cards.pop();
    }
}
