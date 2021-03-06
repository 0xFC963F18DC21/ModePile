package zeroxfc.nullpo.custom.modes.objects.gemswap;

public class ScoreEvent {
    private final int scoreValue;
    private final int x;
    private final int y;
    private final int origin;
    private final int colour;

    public ScoreEvent(int scoreValue, int x, int y, int origin, int colour) {
        this.scoreValue = scoreValue;
        this.x = x;
        this.y = y;
        this.origin = origin;
        this.colour = colour;
    }

    public int getColour() {
        return colour;
    }

    public int getOrigin() {
        return origin;
    }

    public int getScoreValue() {
        return scoreValue;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
