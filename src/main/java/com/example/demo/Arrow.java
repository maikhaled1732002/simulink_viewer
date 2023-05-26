package com.example.demo;
import java.util.ArrayList;
import java.util.List;

public class Arrow {
    private int scrId;
    private int srcPlace;
    private double x;
    private List<Destination> destinations = new ArrayList<>();

    public Arrow(int scrId, int srcPlace, double x) {
        this.scrId = scrId;
        this.srcPlace = srcPlace;
        this.x = x;
    }

    public void addDestination(int destId, double y) {
        Destination destination = new Destination(destId, y);
        destinations.add(destination);
    }

    public int getScrId() {
        return scrId;
    }

    public int getSrcPlace() {
        return srcPlace;
    }

    public double getX() {
        return x;
    }

    public double getY(int index) {
        return destinations.get(index).getY();
    }

    public int getDestinationId(int index) {
        return destinations.get(index).getDestId();
    }

    public int getDestinationsSize() {
        return destinations.size();
    }
}

class Destination {
    private int destId;
    private double y;

    public Destination(int destId, double y) {
        this.destId = destId;
        this.y = y;
    }

    public int getDestId() {
        return destId;
    }

    public double getY() {
        return y;
    }
}