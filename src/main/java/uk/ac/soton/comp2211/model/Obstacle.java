package uk.ac.soton.comp2211.model;

public class Obstacle {
    private String name;

    private int length; // Lenght parallel to the runway.
    private int width;
    private int height;

    private Position position; // Distance from point A on the runway to the start of the obstacle.

    public Obstacle(String _name, int _length, int _width, int _height) {
        name = _name;

        length = _length;
        width = _width;
        height = _height;

        position = new Position(0, 0);
    }

    public String getName() { return name; }

    public int getLenth() { return length; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setPosition(Position _position) { position = _position; }
    public Position getPosition() { return position; }
}
