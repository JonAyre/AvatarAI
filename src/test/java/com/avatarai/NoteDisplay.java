package com.avatarai;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

public class NoteDisplay extends Canvas {
    private final ArrayList<MusicImporter.MusicalWord> spectra = new ArrayList<>();
    private static final int MAX_SIZE = 20;

    public NoteDisplay() {
        super();
    }

    @Override
    public void paint(Graphics g) {
        BufferStrategy bs = this.getBufferStrategy();
        Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
        super.paint(g2d);
        displayNoteSpectra(g2d);
        g2d.dispose();
        g.dispose();
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public void addNoteSpectrum(MusicImporter.MusicalWord word) {
        spectra.add(word);
        if (spectra.size() == MAX_SIZE+1) spectra.removeFirst();
    }

    public void displayNoteSpectra(Graphics2D g2d) {
        double barHeight = (double)this.getHeight() / (double)(MAX_SIZE+1);
        double barWidth = this.getWidth() / 120.0;
        for (int i = 0; i < spectra.size(); i++) {
            MusicImporter.MusicalWord noteSpectrum = MusicImporter.tokeniseWord(spectra.get(i));
            for (int noteNum = 0; noteNum < noteSpectrum.levels().length; noteNum++) {
                int note = noteNum % 12;
                int octave = noteNum / 12;
                if (note == 0)
                    g2d.setColor(Color.cyan);
                else if (MusicImporter.NOTE_NAMES[note].contains("#"))
                    g2d.setColor(Color.black);
                else
                    g2d.setColor(Color.white);
                double level = noteSpectrum.levels()[noteNum];
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
