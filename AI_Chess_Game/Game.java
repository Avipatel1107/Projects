
class Game {

    private Board board;


    private Player whiteplayer, blackplayer;

    Game(Player wp, Player bp) {

        this.whiteplayer = wp;
        this.blackplayer = bp;
        board = new Board();
        board.boardStart();
    }
    Board getBoard ( ) {

        return board;
    }
}