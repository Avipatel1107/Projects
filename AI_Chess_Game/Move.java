
class Move {

    private final Coordinate to;
    private final Coordinate from;

    Move ( Coordinate f, Coordinate t ) {

        from = f;
        to = t;
    }

    Coordinate getFrom ( ) {

        return from;
    }

    Coordinate getTo ( ) {

        return to;
    }
}