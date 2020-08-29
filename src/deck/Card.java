package deck;

import java.io.Serializable;

public class Card implements Serializable {
    private final int value;
    private final String face;
    private final String suit;

    public Card(int value, String face, String suit) {
        this.value = value;
        this.face = face;
        this.suit = suit;
    }

    public int getValue() {
        return value;
    }

    public String getFace() {
        return face;
    }

    public String getSuit() {
        return suit;
    }
}
