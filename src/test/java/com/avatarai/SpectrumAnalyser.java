package com.avatarai;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class SpectrumAnalyser extends Frame implements WindowListener, LineListener {
    NoteDisplay noteDisplay;
    SpectrumDisplay spectrumDisplay;
    Timer timer = new Timer();
    double dt;
    AudioInputStream inputStream;
    Clip audioClip;
    int sampleSize;

    public SpectrumAnalyser(int width, int height, double timeStep, String audioFile) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        super("Spectrum analyser");
        addWindowListener(this);
        noteDisplay = new NoteDisplay();
        spectrumDisplay = new SpectrumDisplay();
        noteDisplay.setSize(width, height/2);
        spectrumDisplay.setSize(width, height/2);
        Panel panel = new Panel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(noteDisplay);
        panel.add(spectrumDisplay);
        this.add(panel);
        this.pack();
        noteDisplay.createBufferStrategy(2);
        spectrumDisplay.createBufferStrategy(2);
        inputStream = AudioSystem.getAudioInputStream(new File(audioFile));
        audioClip = getAudioClip(audioFile);
        AudioFormat format = inputStream.getFormat();
        sampleSize = (int)(format.getSampleRate() * timeStep);
        sampleSize = Integer.highestOneBit(sampleSize - 1);
        dt = sampleSize / format.getSampleRate();
    }

    private Clip getAudioClip(String audioFile) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioInputStream playbackStream = AudioSystem.getAudioInputStream(new File(audioFile));
        DataLine.Info info = new DataLine.Info(Clip.class, playbackStream.getFormat());
        Clip clip = (Clip)AudioSystem.getLine(info);
        clip.addLineListener(this);
        clip.open(playbackStream);
        return clip;
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        SpectrumAnalyser app = new SpectrumAnalyser(1500, 1000, 0.075, "test_files/audio/Symphony No. 2 _Resurrection__ IV. Urlicht. O Röschen rot! - Live.wav");
        app.start();
    }

    private void start() {
        this.setVisible(true);
        audioClip.start();
        timer.schedule(new TimerTask() {
            public void run() {step();}
        }, 0, (int)Math.rint(dt*1000));
    }

    private synchronized void step() {
        try {
            this.updateSpectrum();
            noteDisplay.repaint();
            noteDisplay.getBufferStrategy().show();
            spectrumDisplay.repaint();
            spectrumDisplay.getBufferStrategy().show();
        } catch (IOException e) {
            e.printStackTrace();
            this.windowClosing(null);
        }
    }

    public void updateSpectrum() throws IOException {
        MusicImporter.Sample data = MusicImporter.readSample(inputStream, sampleSize);
        MusicImporter.Spectrum spectrum = MusicImporter.sampleToSpectrum(data);
        spectrumDisplay.spectrum = spectrum;
        noteDisplay.addNoteSpectrum(MusicImporter.spectrumToNotes(spectrum));
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        timer.cancel();
        this.dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void update(LineEvent event) {

    }
}
