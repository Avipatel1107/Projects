class Pawn extends Piece {

    private boolean firstMove;
    private boolean canEnPassant;
    private int enPassantMove;

    Pawn ( Colours c ) {

        firstMove = true;
        canEnPassant = false;
        setValue(1);
        setType(Pieces.Pawn);
        setColour(c);
    }

    @Override
    boolean isFirstMove ( ) {

        return firstMove;
    }

    @Override
    void firstMoveMade ( boolean twice,  int moves ) {

        firstMove = false;
        if (twice) {
            canEnPassant = true;
            enPassantMove = moves;
        }
    }

    @Override
    void setFirstMove ( ) {

        firstMove = true;
    }

    @Override
    boolean canEnPassant ( ) {

        return canEnPassant;
    }

    @Override
    void flipEnPassant ( ) {

        canEnPassant = false;
    }

    @Override
    int moveEnPassant ( ) {

        return enPassantMove;
    }

    boolean moveChoice ( Coordinate from, Coordinate to ) {

        switch (getColour()) {
            case White:
                if (from.getColumn() == to.getColumn() && from.getRow() - to.getRow() == 1) { // one move up
                    return true;
                }
                if (from.getColumn() - to.getColumn() == -1 && from.getRow() - to.getRow() == 1) { // diagonal moves
                    return true;
                }
                if (from.getColumn() - to.getColumn() == 1 && from.getRow() - to.getRow() == 1) {
                    return true;
                }
                if (isFirstMove()) {// first move can move twice.
                    return from.getColumn() == to.getColumn() && from.getRow() - to.getRow() == 2;
                }
                return false;

            case Black:

                if (from.getColumn() == to.getColumn() && from.getRow() - to.getRow() == -1) { // one move down
                    return true;
                }
                if (from.getColumn() - to.getColumn() == -1 && from.getRow() - to.getRow() == -1) { // diagonal moves
                    return true;
                }
                if (from.getColumn() - to.getColumn() == 1 && from.getRow() - to.getRow() == -1) {
                    return true;
                }
                if (isFirstMove()) {// first move can move twice.
                    return from.getColumn() == to.getColumn() && from.getRow() - to.getRow() == -2;
                }
                return false;
        }
        return false;
    }
}