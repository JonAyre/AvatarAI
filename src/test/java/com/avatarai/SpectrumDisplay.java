package com.avatarai;

import java.awt.*;
import java.awt.image.BufferStrategy;

public class SpectrumDisplay extends Canvas {
    public MusicImporter.Spectrum spectrum = new MusicImporter.Spectrum(new double[10], 0.0);

    public SpectrumDisplay() {
        super();
    }

    @Override
    public void paint(Graphics g) {
        BufferStrategy bs = this.getBufferStrategy();
        Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
        super.paint(g2d);
        displaySpectrum(g2d);
        g2d.dispose();
        g.dispose();
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public void displaySpectrum(Graphics2D g2d) {
        g2d.setColor(Color.black);
        int samplesToShow = (int)(6000.0/(spectrum.sampleFrequency() / spectrum.levels().length));
        double barWidth = (double)this.getWidth() / samplesToShow;
        for (int i = 0; i < samplesToShow; i++) {
            double level = 20.0*spectrum.levels()[i];
            int x = (int)(i * barWidth);
            int y = (int) Math.rint((1.0 - level) * this.getHeight());
            g2d.fillRect(x, y, (int)barWidth, this.getHeight()-y);
        }
    }

}
