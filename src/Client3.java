import deck.Card;
import deck.Face;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client3 implements BlackjackConstants {
    private static final String host = "localhost";
    private static final int port = 1337;

    private static ObjectOutputStream outputStream;
    private static ObjectInputStream inputStream;

    private static int player;
    private static final ArrayList<Card> hand = new ArrayList<>();
    private static int points;
    private static boolean attemptedSplit;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(host, port);

        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());

        new Thread(() -> {
            try {
                player = (int) inputStream.readObject();
                System.out.println("Connected to server.");

                // Player 1 specific code starts here;
                if (player == PLAYER1) {
                    System.out.println("You are player 1.");
                    System.out.println("Waiting for other player to join.");

                    inputStream.readObject();

                    System.out.println("Player 2 has joined.");
                    System.out.println("Game can begin.");


                    while (true) {
                        int showScore = (int) inputStream.readObject();
                        if (showScore == 1) {
                            System.out.println((String) inputStream.readObject());
                        }

                        boolean blackjack = initialDraw();
                        outputStream.writeObject(blackjack);
                        outputStream.writeObject(points);

                        System.out.println((String) inputStream.readObject());
                        boolean player2Blackjack = (boolean) inputStream.readObject();

                        Card dealerCard = (Card) inputStream.readObject();
                        System.out.println("Dealer's shown card is " + dealerCard.getFace() + " of " + dealerCard.getSuit());

                        if (!blackjack && !player2Blackjack) {
                            handleTurn();
                            if (!attemptedSplit) {
                                outputStream.writeObject(points);
                            }
                        }

                        System.out.println("Waiting for player 2 to complete his turn.");

                        System.out.println((String) inputStream.readObject());

                        System.out.println("Waiting for dealer to complete his turn.");

                        System.out.println((String) inputStream.readObject());

                        int winner = (int) inputStream.readObject();
                        checkWinner(player, winner);

                        boolean playerChoice = restartGame();
                        outputStream.writeObject(playerChoice);

                        System.out.println("Waiting for other player");

                        int continueGame = (int) inputStream.readObject();
                        if (continueGame == 1) {
                            hand.clear();
                        }
                        if (continueGame == 2) {
                            System.out.println("One or more player did not agree to restart. Closing game.");
                            return;
                        }
                    }
                } else {
                    // Player 2 specific code starts here;
                    System.out.println("You are player 2");

                    inputStream.readObject();

                    System.out.println("Game can begin.");

                    while (true) {
                        int showScore = (int) inputStream.readObject();
                        if (showScore == 1) {
                            System.out.println((String) inputStream.readObject());
                        }

                        boolean blackjack = initialDraw();
                        outputStream.writeObject(blackjack);
                        outputStream.writeObject(points);

                        System.out.println((String) inputStream.readObject());
                        boolean player1Blackjack = (boolean) inputStream.readObject();

                        Card dealerCard = (Card) inputStream.readObject();
                        System.out.println("Dealer's shown card is " + dealerCard.getFace() + " of " + dealerCard.getSuit());

                        System.out.println("Waiting for your turn.");

                        System.out.println((String) inputStream.readObject());

                        if (!blackjack && !player1Blackjack) {
                            handleTurn();
                            if (!attemptedSplit) {
                                outputStream.writeObject(points);
                            }
                        }

                        System.out.println("Waiting for dealer to complete his turn.");

                        System.out.println((String) inputStream.readObject());

                        int winner = (int) inputStream.readObject();
                        checkWinner(player, winner);


                        boolean playerChoice = restartGame();
                        outputStream.writeObject(playerChoice);

                        System.out.println("Waiting for other player");

                        int continueGame = (int) inputStream.readObject();
                        if (continueGame == 1) {
                            System.out.println("A restart has been agreed.\n");
                            hand.clear();
                            points = 0;
                        }
                        if (continueGame == 2) {
                            System.out.println("One or more player did not agree to restart. Closing game.");
                            return;
                        }
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private static boolean initialDraw() throws IOException, ClassNotFoundException {
        Card firstCard = (Card) inputStream.readObject();
        Card secondCard = (Card) inputStream.readObject();

        hand.add(firstCard);
        hand.add(secondCard);

        System.out.println("First card: " + firstCard.getFace() + " of " + firstCard.getSuit());
        System.out.println("Second card: " + secondCard.getFace() + " of " + secondCard.getSuit());

        points = 0;
        for (Card card : hand) {
            if (card.getFace().equals(Face.ACE.name())) {
                if (points <= 10) {
                    points += 11;
                } else {
                    points += card.getValue();
                }
            } else {
                points += card.getValue();
            }
        }
        boolean blackjack = points == 21;
        if (blackjack) {
            System.out.println("You have blackjack.");
        } else {
            System.out.println("You have " + points + " points.");
        }
        return blackjack;
    }

    private static void handleTurn() throws IOException, ClassNotFoundException {
        System.out.println("Your turn.");
        boolean allowSplit = false;
        boolean allowDouble = true;
        if (hand.get(0).getValue() == hand.get(1).getValue()) {
            allowSplit = true;
        }
        Scanner scanner = new Scanner(System.in);
        boolean playerTurn = true;
        while (playerTurn) {
            if (allowSplit && allowDouble) {
                System.out.println("Hit, stand, double or split.");
            } else if (allowDouble) {
                System.out.println("Hit, stand or double.");
            } else {
                System.out.println("Hit or stand.");
            }

            boolean validInput = false;
            while (!validInput) {
                String input = scanner.next();
                if (input.equalsIgnoreCase("hit")) {
                    outputStream.writeObject(input);
                    validInput = true;

                    Card card = (Card) inputStream.readObject();
                    System.out.println("You drew " + card.getFace() + " of " + card.getSuit());
                    if (card.getFace().equals(Face.ACE.name())) {
                        if (points <= 10) {
                            points += 11;
                        } else {
                            points += card.getValue();
                        }
                    } else {
                        points += card.getValue();
                    }

                    if (points > 21) {
                        System.out.println("You busted with " + points + " points.");
                        playerTurn = false;
                        outputStream.writeObject(false);
                    } else {
                        System.out.println("You have " + points + " points.");
                        outputStream.writeObject(true);
                    }
                } else if (input.equalsIgnoreCase("stand")) {
                    outputStream.writeObject(input);
                    validInput = true;
                    System.out.println("You stand with " + points + " points.");
                    playerTurn = false;
                } else if (input.equalsIgnoreCase("double")) {
                    outputStream.writeObject(input);
                    validInput = true;
                    Card card = (Card) inputStream.readObject();
                    outputStream.writeObject(player);
                    System.out.println("You drew " + card.getFace() + " of " + card.getSuit());
                    if (card.getFace().equals(Face.ACE.name())) {
                        if (points <= 10) {
                            points += 11;
                        } else {
                            points += card.getValue();
                        }
                    } else {
                        points += card.getValue();
                    }

                    if (points > 21) {
                        System.out.println("You busted with " + points + " points and will lose score at the end of the round.");
                    } else {
                        System.out.println("You have " + points + " points and a chance to double your win.");
                    }
                    playerTurn = false;
                } else if (input.equalsIgnoreCase("split") && allowSplit) {
                    outputStream.writeObject(input);
                    validInput = true;
                    attemptedSplit = true;
                    Card[] newCards = new Card[]{
                            (Card) inputStream.readObject(),
                            (Card) inputStream.readObject()
                    };
                    System.out.println("Splitting hand.");
                    handleSplit(newCards);
                    playerTurn = false;
                } else {
                    System.out.println("Invalid input. Try again.");
                }
            }
            allowDouble = false;
        }
    }

    private static void handleSplit(Card[] newCards) throws IOException, ClassNotFoundException {
        ArrayList<Card> hand1 = new ArrayList<>();
        hand1.add(hand.get(0));
        hand1.add(newCards[0]);
        Card hand1First = hand1.get(0);
        Card hand1Second = hand1.get(1);

        System.out.println("New hand 1: " + hand1First.getFace() + " of " + hand1First.getSuit()
                + " and " + hand1Second.getFace() + " of " + hand1Second.getSuit());
        int hand1Points = hand1First.getValue() + hand1Second.getValue();
        System.out.println("Value: " + hand1Points);

        ArrayList<Card> hand2 = new ArrayList<>();
        hand2.add(hand.get(1));
        hand2.add(newCards[1]);
        Card hand2First = hand2.get(0);
        Card hand2Second = hand2.get(1);

        System.out.println("New hand 2: " + hand2First.getFace() + " of " + hand2First.getSuit()
                + " and " + hand2Second.getFace() + " of " + hand2Second.getSuit());
        int hand2Points = hand2First.getValue() + hand2Second.getValue();
        System.out.println("Value: " + hand2Points);

        System.out.println("Playing the first hand.");

        hand1Points = splitSimpleHitStand(hand1Points);

        System.out.println("Playing the second hand of " + hand2Points + " points.");

        hand2Points = splitSimpleHitStand(hand2Points);

        outputStream.writeObject(player);
        outputStream.writeObject(hand1Points);
        outputStream.writeObject(hand2Points);
    }

    private static int splitSimpleHitStand(int handPoints) throws IOException, ClassNotFoundException {
        Scanner scanner = new Scanner(System.in);
        boolean handTurn = true;
        while (handTurn) {
            System.out.println("Hit or stand.");
            String input = scanner.next();
            boolean validInput = false;
            while (!validInput) {
                if (input.equalsIgnoreCase("hit")) {
                    outputStream.writeObject(input);
                    validInput = true;
                    Card card = (Card) inputStream.readObject();
                    System.out.println("You drew " + card.getFace() + " of " + card.getSuit());
                    if (card.getFace().equals(Face.ACE.name())) {
                        if (handPoints <= 10) {
                            handPoints += 11;
                        } else {
                            handPoints += card.getValue();
                        }
                    } else {
                        handPoints += card.getValue();
                    }
                    if (handPoints > 21) {
                        System.out.println("You busted with " + handPoints + " points in the current hand.");
                        handTurn = false;
                    } else {
                        System.out.println("You have " + handPoints + " points in the current hand.");
                    }

                } else if (input.equalsIgnoreCase("stand")) {
                    outputStream.writeObject(input);
                    validInput = true;
                    System.out.println("You stand with " + handPoints + " points in the current hand.");
                    handTurn = false;
                } else {
                    System.out.println("Invalid input. Try again.");
                }
            }
        }
        return handPoints;
    }

    private static void checkWinner(int player, int winner) {
        switch (winner) {
            case PLAYER1_WON:
                if (player == PLAYER1) {
                    System.out.println("You have won the game.");
                } else {
                    System.out.println("Player 1 has won the game.");
                }
                break;
            case PLAYER2_WON:
                if (player == PLAYER2) {
                    System.out.println("You have won the game.");
                } else {
                    System.out.println("Player 2 has won the game.");
                }
                break;
            case DEALER_WON:
                System.out.println("Dealer has won the game.");
                break;
            case PLAYER_DRAW:
                System.out.println("The two players have reached a draw.");
                break;
            case NO_WINNER:
                System.out.println("No one has won the game.");
                break;
            case SPLIT_RESULT_DEALER_WON:
                System.out.println("A Split result was made but dealer won so no points awarded.");
                break;
            case SPLIT_RESULT_ONE_POINT_PLAYER1:
                System.out.println("A Split result was made where player 1 earned 1 point.");
                break;
            case SPLIT_RESULT_ONE_POINT_PLAYER2:
                System.out.println("A Split result was made where player 2 earned 1 point.");
                break;
            case SPLIT_RESULT_TWO_POINTS_PLAYER1:
                System.out.println("A Split result was made where player 1 earned 2 points.");
                break;
            case SPLIT_RESULT_TWO_POINTS_PLAYER2:
                System.out.println("A Split result was made where player 2 earned 2 point.");
                break;
            case SPLIT_RESULT_DRAW:
                System.out.println("A Split result was made but no points were awarded.");
        }
    }

    private static boolean restartGame() {
        System.out.println("Would you like to play again? Yes/No");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String input = scanner.next();
            if (input.equalsIgnoreCase("yes")) {
                return true;
            } else if (input.equalsIgnoreCase("no")) {
                return false;
            } else {
                System.out.println("Invalid input. Try again.");
            }
        }
    }
}
