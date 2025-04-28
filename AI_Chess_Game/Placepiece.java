
class Placepiece {

    private Piece piece;

    Placepiece(  ) {

        piece = null;
    }

    boolean hasPiece ( ) {  // Returns true if a piece is occupying the square, false otherwise.
        return piece != null;
    }

    Piece getPiece ( ) { // Returns the piece that is occupying the square if it exists, null otherwise.

        if (hasPiece()) {
            return piece;
        } else {
            return null;
        }
    }

    Piece takePiece ( ) {

        Piece pie;
        if (hasPiece()) {
            pie = piece;
            piece = null;
            return pie;
        } else {
            return null;
        }
    }

    void setPiece (Piece p) { // Occupies the square with the passed piece if previously unoccupied.

        if (!hasPiece()) {
            this.piece = p;
        }
    }
}