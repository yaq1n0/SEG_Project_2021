package uk.ac.soton.comp2211.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Airport {
    private String name;
    private List<Tarmac> tarmacs;
    private File dataFile;

    public Airport(String _name, Tarmac[] _tarmacs, File _dataFile) {
        name = _name;

        tarmacs = new ArrayList<Tarmac>();
        for (Tarmac tarmac : _tarmacs) tarmacs.add(tarmac);

        dataFile = _dataFile;
    }

    public String getName() { return name; }
    public List<Tarmac> getTarmacs() { return tarmacs; }
    public File getDataFile() { return dataFile; }

    public Runway[] getRunways() {
        List<Runway> runways = new ArrayList<Runway>();
        for (Tarmac tarmac : tarmacs) { runways.addAll(Arrays.asList(tarmac.getRunways())); }
        return runways.toArray(new Runway[0]);
    }
}
