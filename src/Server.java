import deck.Card;
import deck.Deck;
import deck.Face;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements BlackjackConstants {
    private static int sessionNumber = 1;
    private static final int port = 1337;

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Server started on port 12345");

                while (true) {
                    System.out.println("Waiting for players to join session " + sessionNumber);

                    Socket player1 = serverSocket.accept();

                    System.out.println("Player 1 has joined session " + sessionNumber);
                    System.out.println("Player 1's IP address: " + player1.getInetAddress().getHostAddress());

                    ObjectOutputStream player1Out = new ObjectOutputStream(player1.getOutputStream());
                    player1Out.writeObject(PLAYER1);

                    Socket player2 = serverSocket.accept();

                    System.out.println("Player 2 has joined session " + sessionNumber);
                    System.out.println("Player 2's IP address: " + player2.getInetAddress().getHostAddress());

                    ObjectOutputStream player2Out = new ObjectOutputStream(player2.getOutputStream());
                    player2Out.writeObject(PLAYER2);

                    player1Out.writeObject(1);
                    player2Out.writeObject(1);

                    System.out.println("Starting thread for session " + sessionNumber + "\n");
                    sessionNumber++;

                    new Thread(new HandleASession(player1, player2, player1Out, player2Out)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    static class HandleASession implements Runnable {
        private final Socket player1;
        private final Socket player2;
        private final ObjectOutputStream player1Out;
        private final ObjectOutputStream player2Out;
        private Deck deck;

        private int player1Points;
        private int player2Points;
        private int dealerPoints;

        private int player1Score;
        private int player2Score;
        private int dealerScore;

        private final ArrayList<Integer> attemptedDoubles = new ArrayList<>();
        private final ArrayList<Integer[]> attemptedSplitPlayer1 = new ArrayList<>();
        private final ArrayList<Integer[]> attemptedSplitPlayer2 = new ArrayList<>();

        public HandleASession(Socket player1, Socket player2, ObjectOutputStream player1Out, ObjectOutputStream player2Out) {
            this.player1 = player1;
            this.player2 = player2;
            this.player1Out = player1Out;
            this.player2Out = player2Out;

            player1Score = 0;
            player2Score = 0;
            dealerScore = 0;
        }

        @Override
        public void run() {
            try {

                ObjectOutputStream toPlayer1 = player1Out;
                ObjectInputStream fromPlayer1 = new ObjectInputStream(player1.getInputStream());

                ObjectOutputStream toPlayer2 = player2Out;
                ObjectInputStream fromPlayer2 = new ObjectInputStream(player2.getInputStream());


                while (true) {
                    attemptedDoubles.clear();
                    attemptedSplitPlayer1.clear();
                    attemptedSplitPlayer2.clear();
                    player1Points = 0;
                    player2Points = 0;
                    dealerPoints = 0;
                    String messageToAll;
                    if (player1Score != 0 || player2Score != 0 || dealerScore != 0) {
                        toPlayer1.writeObject(1);
                        toPlayer2.writeObject(1);
                        messageToAll = "Current scores: Player 1: " + player1Score + ", Player 2: " + player2Score + ", Dealer: " + dealerScore;
                        toPlayer1.writeObject(messageToAll);
                        toPlayer2.writeObject(messageToAll);
                    } else {
                        toPlayer1.writeObject(0);
                        toPlayer2.writeObject(0);
                    }
                    deck = new Deck();
                    deck.makeDeck();

                    toPlayer1.writeObject(deck.draw());
                    toPlayer1.writeObject(deck.draw());
                    boolean player1Blackjack = (boolean) fromPlayer1.readObject();
                    player1Points = (int) fromPlayer1.readObject();

                    toPlayer2.writeObject(deck.draw());
                    toPlayer2.writeObject(deck.draw());
                    boolean player2Blackjack = (boolean) fromPlayer2.readObject();
                    player2Points = (int) fromPlayer2.readObject();

                    ArrayList<Card> dealerHand = new ArrayList<>();
                    dealerHand.add(deck.draw());
                    dealerHand.add(deck.draw());

                    if (player1Blackjack) {
                        messageToAll = "Player 1 has blackjack.";
                    } else {
                        messageToAll = "Player 1 starts with " + player1Points + " points.";
                    }
                    toPlayer2.writeObject(messageToAll);
                    toPlayer2.writeObject(player1Blackjack);

                    if (player2Blackjack) {
                        messageToAll = "Player 2 has blackjack.";
                    } else {
                        messageToAll = "Player 2 starts with " + player2Points + " points.";
                    }
                    toPlayer1.writeObject(messageToAll);
                    toPlayer1.writeObject(player2Blackjack);

                    toPlayer1.writeObject(dealerHand.get(0));
                    toPlayer2.writeObject(dealerHand.get(0));

                    if (!player1Blackjack && !player2Blackjack) {
                        handleTurn(fromPlayer1, toPlayer1);
                        if (!attemptedSplitPlayer1.isEmpty()) {
                            messageToAll = "Player 1 split his hand and got "
                                    + attemptedSplitPlayer1.get(0)[0] + " and " + attemptedSplitPlayer1.get(0)[1] + " points respectively.";
                        } else {
                            player1Points = (int) fromPlayer1.readObject();

                            if (player1Points > 21) {
                                messageToAll = "Player 1 busted with " + player1Points + " points.";
                            } else {
                                messageToAll = "Player 1 stands with " + player1Points + " points.";
                            }
                        }
                    } else {
                        messageToAll = "Player 1 turn skipped because of blackjack.";
                    }
                    toPlayer2.writeObject(messageToAll);

                    if (!player2Blackjack && !player1Blackjack) {
                        handleTurn(fromPlayer2, toPlayer2);
                        if (!attemptedSplitPlayer2.isEmpty()) {
                            messageToAll = "Player 2 split his hand and got "
                                    + attemptedSplitPlayer2.get(0)[0] + " and " + attemptedSplitPlayer2.get(0)[1] + " points respectively.";
                        } else {
                            player2Points = (int) fromPlayer2.readObject();

                            if (player2Points > 21) {
                                messageToAll = "Player 2 busted with " + player2Points + " points.";
                            } else {
                                messageToAll = "Player 2 stands with " + player2Points + " points.";
                            }
                        }
                    } else {
                        messageToAll = "Player 2 turn skipped because of blackjack.";
                    }
                    toPlayer1.writeObject(messageToAll);

                    boolean dealerTurn = true;
                    for (Card card : dealerHand) {
                        if (card.getFace().equals(Face.ACE.name())) {
                            if (dealerPoints <= 10) {
                                dealerPoints += 11;
                            }
                        } else {
                            dealerPoints += card.getValue();
                        }
                    }

                    boolean dealerBlackjack = false;
                    if (dealerPoints == 21) {
                        dealerBlackjack = true;
                    }

                    while (dealerTurn && !dealerBlackjack) {
                        if (dealerPoints < 17) {
                            Card card = deck.draw();
                            if (card.getFace().equals(Face.ACE.name())) {
                                if (dealerPoints < 10) {
                                    dealerPoints += 11;
                                } else {
                                    dealerPoints += card.getValue();
                                }
                            } else {
                                dealerPoints += card.getValue();
                            }

                        } else {
                            dealerTurn = false;
                        }
                    }

                    if (!dealerBlackjack) {
                        if (dealerPoints > 21) {
                            messageToAll = "Dealer busted with " + dealerPoints + " points.";
                        } else {
                            messageToAll = "Dealer stands with " + dealerPoints + " points.";
                        }
                        toPlayer1.writeObject(messageToAll);
                        toPlayer2.writeObject(messageToAll);

                        int winner;
                        if (!attemptedSplitPlayer1.isEmpty() || !attemptedSplitPlayer2.isEmpty()) {
                            winner = checkWinWithSplits();

                            toPlayer1.writeObject(winner);
                            toPlayer2.writeObject(winner);
                        } else {


                            winner = checkWin(player1Points, player1Blackjack, player2Points, player2Blackjack, dealerPoints);

                            switch (winner) {
                                case PLAYER1_WON:
                                    player1Score++;
                                    toPlayer1.writeObject(PLAYER1_WON);
                                    toPlayer2.writeObject(PLAYER1_WON);
                                    if (attemptedDoubles.contains(PLAYER1)) {
                                        player1Score++;
                                    }
                                    if (attemptedDoubles.contains(PLAYER2)) {
                                        player2Score--;
                                    }
                                    break;
                                case PLAYER2_WON:
                                    player2Score++;
                                    toPlayer1.writeObject(PLAYER2_WON);
                                    toPlayer2.writeObject(PLAYER2_WON);
                                    if (attemptedDoubles.contains(PLAYER1)) {
                                        player1Score--;
                                    }
                                    if (attemptedDoubles.contains(PLAYER2)) {
                                        player2Score++;
                                    }
                                    break;
                                case DEALER_WON:
                                    dealerScore++;
                                    if (attemptedDoubles.contains(PLAYER1)) {
                                        player1Score--;
                                    }
                                    if (attemptedDoubles.contains(PLAYER2)) {
                                        player2Score--;
                                    }
                                    toPlayer1.writeObject(DEALER_WON);
                                    toPlayer2.writeObject(DEALER_WON);
                                    break;
                                case PLAYER_DRAW:
                                    player1Score++;
                                    player2Score++;
                                    toPlayer1.writeObject(PLAYER_DRAW);
                                    toPlayer2.writeObject(PLAYER_DRAW);
                                    break;
                                case NO_WINNER:
                                    if (attemptedDoubles.contains(PLAYER1)) {
                                        player1Score--;
                                    }
                                    if (attemptedDoubles.contains(PLAYER2)) {
                                        player2Score--;
                                    }
                                    toPlayer1.writeObject(NO_WINNER);
                                    toPlayer2.writeObject(NO_WINNER);
                            }
                        }


                    } else {
                        dealerScore++;
                        toPlayer1.writeObject(DEALER_WON);
                        toPlayer2.writeObject(DEALER_WON);
                        if (attemptedDoubles.contains(PLAYER1)) {
                            player1Score--;
                        }
                        if (attemptedDoubles.contains(PLAYER2)) {
                            player2Score--;
                        }
                    }

                    boolean player1Restart = (boolean) fromPlayer1.readObject();
                    boolean player2Restart = (boolean) fromPlayer2.readObject();

                    if (player1Restart && player2Restart) {
                        toPlayer1.writeObject(1);
                        toPlayer2.writeObject(1);
                    } else {
                        toPlayer1.writeObject(2);
                        toPlayer2.writeObject(2);
                        return;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void handleTurn(ObjectInputStream fromPlayer, ObjectOutputStream toPlayer) throws IOException, ClassNotFoundException {
            boolean playerTurn = true;

            while (playerTurn) {
                String move = (String) fromPlayer.readObject();
                switch (move.toLowerCase()) {
                    case "hit":
                        toPlayer.writeObject(deck.draw());
                        playerTurn = (boolean) fromPlayer.readObject();
                        break;
                    case "stand":
                        playerTurn = false;
                        break;
                    case "double":
                        toPlayer.writeObject(deck.draw());
                        attemptedDoubles.add((Integer) fromPlayer.readObject());
                        playerTurn = false;
                        break;
                    case "split":
                        toPlayer.writeObject(deck.draw());
                        toPlayer.writeObject(deck.draw());
                        boolean handlingSplit1 = true;

                        while (handlingSplit1) {
                            String splitMove = (String) fromPlayer.readObject();
                            if (splitMove.equalsIgnoreCase("hit")) {
                                toPlayer.writeObject(deck.draw());
                            } else if (splitMove.equalsIgnoreCase("stand")) {
                                handlingSplit1 = false;
                            }
                        }

                        boolean handlingSplit2 = true;
                        while (handlingSplit2) {
                            String splitMove = (String) fromPlayer.readObject();
                            if (splitMove.equalsIgnoreCase("hit")) {
                                toPlayer.writeObject(deck.draw());
                            } else if (splitMove.equalsIgnoreCase("stand")) {
                                handlingSplit2 = false;
                            }
                        }
                        int player = (int) fromPlayer.readObject();
                        int result1 = (int) fromPlayer.readObject();
                        int result2 = (int) fromPlayer.readObject();
                        if (player == PLAYER1) {
                            attemptedSplitPlayer1.add(new Integer[]{result1, result2});
                        } else {
                            attemptedSplitPlayer2.add(new Integer[]{result1, result2});
                        }
                        playerTurn = false;
                }
            }
        }

        private int checkWin(int player1Points, boolean player1Blackjack,
                             int player2Points, boolean player2Blackjack,
                             int dealerPoints) {
            if (player1Blackjack) {
                if (!player2Blackjack) {
                    return PLAYER1_WON;
                } else {
                    return PLAYER_DRAW;
                }
            }

            if (player2Blackjack) {
                return PLAYER2_WON;
            }

            if (player1Points <= 21) {
                if (player1Points > player2Points || player2Points > 21) {
                    if (player1Points > dealerPoints || dealerPoints > 21) {
                        return PLAYER1_WON;
                    }
                }
            }

            if (player2Points <= 21) {
                if (player2Points > player1Points || player1Points > 21) {
                    if (player2Points > dealerPoints || dealerPoints > 21) {
                        return PLAYER2_WON;
                    }
                }
            }

            if (dealerPoints <= 21) {
                if (dealerPoints >= player1Points || player1Points > 21) {
                    if (dealerPoints >= player2Points || player2Points > 21) {
                        return DEALER_WON;
                    }
                }
            }

            if (player1Points == player2Points) {
                if (player1Points > dealerPoints) {
                    return PLAYER_DRAW;
                }
            }

            return NO_WINNER;
        }

        private int checkWinWithSplits() {
            if (!attemptedSplitPlayer1.isEmpty() && attemptedSplitPlayer2.isEmpty()) {
                int player1Hand1 = attemptedSplitPlayer1.get(0)[0];
                int player1Hand2 = attemptedSplitPlayer1.get(0)[1];

                boolean win1 = true;

                if (player1Hand1 > 21) {
                    win1 = false;
                }

                if (player1Hand1 < player2Points && player2Points < 21) {
                    win1 = false;
                }

                if (player1Hand1 <= dealerPoints && dealerPoints < 21) {
                    win1 = false;
                }

                boolean win2 = true;

                if (player1Hand2 > 21) {
                    win2 = false;
                }

                if (player1Hand2 < player2Points && player2Points < 21) {
                    win2 = false;
                }

                if (player1Hand2 <= dealerPoints && dealerPoints < 21) {
                    win2 = false;
                }

                if (win1 && win2) {
                    player1Score++;
                    player1Score++;
                    return SPLIT_RESULT_TWO_POINTS_PLAYER1;
                }
                if (win1 || win2) {
                    player1Score++;
                    return SPLIT_RESULT_ONE_POINT_PLAYER1;
                }
            }

            if (attemptedSplitPlayer1.isEmpty() && !attemptedSplitPlayer2.isEmpty()) {
                int player2Hand1 = attemptedSplitPlayer2.get(0)[0];
                int player2Hand2 = attemptedSplitPlayer2.get(0)[1];

                boolean win1 = true;

                if (player2Hand1 > 21) {
                    win1 = false;
                }

                if (player2Hand1 < player2Points && player2Points < 21) {
                    win1 = false;
                }

                if (player2Hand1 <= dealerPoints && dealerPoints < 21) {
                    win1 = false;
                }

                boolean win2 = true;

                if (player2Hand2 > 21) {
                    win2 = false;
                }

                if (player2Hand2 < player2Points && player2Points < 21) {
                    win2 = false;
                }

                if (player2Hand2 <= dealerPoints && dealerPoints < 21) {
                    win2 = false;
                }

                if (win1 && win2) {
                    player2Score++;
                    player2Score++;
                    return SPLIT_RESULT_TWO_POINTS_PLAYER2;
                }
                if (win1 || win2) {
                    player2Score++;
                    return SPLIT_RESULT_ONE_POINT_PLAYER2;
                }
            }

            if (!attemptedSplitPlayer1.isEmpty() && !attemptedSplitPlayer2.isEmpty()) {
                int player1Hand1 = attemptedSplitPlayer1.get(0)[0];
                int player1Hand2 = attemptedSplitPlayer1.get(0)[1];

                int player2Hand1 = attemptedSplitPlayer2.get(0)[0];
                int player2Hand2 = attemptedSplitPlayer2.get(0)[1];

                boolean win1 = true;
                boolean win2 = true;
                if (dealerPoints >= player1Hand1
                        && dealerPoints >= player1Hand2
                        && dealerPoints >= player2Hand1
                        && dealerPoints >= player2Hand2
                        && dealerPoints < 21) {
                    return SPLIT_RESULT_DEALER_WON;
                }

                if (player1Hand1 > 21 && player1Hand2 > 21) {
                    win1 = false;
                }

                if (player2Hand1 > 21 && player2Hand2 > 21) {
                    win2 = false;
                }

                if (player1Hand1 > player1Hand2) {
                    if (player1Hand1 < player2Hand1 && player1Hand1 < 21) {
                        win1 = false;
                    }
                    if (player1Hand1 < player2Hand2 && player2Hand2 < 21) {
                        win1 = false;
                    }
                } else {
                    if (player1Hand2 < player2Hand1 && player2Hand1 < 21) {
                        win1 = false;
                    }

                    if (player1Hand2 < player2Hand2 && player2Hand2 < 21) {
                        win1 = false;
                    }
                }

                if (player2Hand1 > player2Hand2) {
                    if (player2Hand1 > player1Hand1 && player1Hand1 < 21) {
                        win2 = false;
                    }

                    if (player2Hand1 > player1Hand2 && player1Hand2 < 21) {
                        win2 = false;
                    }
                } else {
                    if (player2Hand2 > player1Hand1 && player1Hand1 < 21) {
                        win2 = false;
                    }

                    if (player2Hand2 > player1Hand2 && player1Hand2 < 21) {
                        win2 = false;
                    }
                }

                if (win1) {
                    player1Score++;
                    return SPLIT_RESULT_ONE_POINT_PLAYER1;
                }

                if (win2) {
                    player2Score++;
                    return SPLIT_RESULT_ONE_POINT_PLAYER2;
                }
            }
            return SPLIT_RESULT_DRAW;
        }
    }
}
