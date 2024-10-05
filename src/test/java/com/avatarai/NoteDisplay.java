package com.avatarai;

import java.awt.*;
import java.util.ArrayList;

public class NoteDisplay extends Canvas {
    private final ArrayList<MusicImporter.MusicalWord> spectra = new ArrayList<>();
    private static final int MAX_SIZE = 20;

    public NoteDisplay() {
        super();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        displayNoteSpectra((Graphics2D)g);
        g.dispose();
        Toolkit.getDefaultToolkit().sync();
    }

    public void addNoteSpectrum(MusicImporter.MusicalWord word) {
        spectra.add(word);
        if (spectra.size() == MAX_SIZE+1) spectra.removeFirst();
    }

    public void displayNoteSpectra(Graphics2D g2d) {
        double barHeight = (double)this.getHeight() / (double)(MAX_SIZE+1);
        double barWidth = this.getWidth() / 120.0;
        for (int i = 0; i < spectra.size(); i++) {
            MusicImporter.MusicalWord noteSpectrum = spectra.get(i);
            for (int octave = 0; octave < noteSpectrum.levels().length; octave++) {
                for (int note = 0; note < noteSpectrum.levels()[octave].length; note++) {
                    if (note == 0)
                        g2d.setColor(Color.cyan);
                    else if (MusicImporter.NOTE_NAMES[note].contains("#"))
                        g2d.setColor(Color.black);
                    else
                        g2d.setColor(Color.white);
                    //double level = 15.0*noteSpectrum.levels()[octave][note];
                    double level = (noteSpectrum.levels()[octave][note] > noteSpectrum.amplitude()/3.0 ? 1.0 : 0.0);
                    int x = (int)((note + octave * 12) * barWidth);
                    int y = this.getHeight() - (int)Math.rint((spectra.size() - i + level) * barHeight);
                    if (i == spectra.size() - 1)
                        g2d.fillRect(x, y, (int)barWidth, (int)((level+1)*barHeight));
                    else
                        g2d.fillRect(x, y, (int)barWidth, (int)(level*barHeight));
                }
            }

        }
    }

}
