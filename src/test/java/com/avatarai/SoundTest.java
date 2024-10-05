package com.avatarai;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class SoundTest {
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
//        testFreqToNote();
//        testReadSample();
//        testSampleToSpectrum();
        testSpectrumToNotes();
//        testSampleToEmbeddings();
    }

    public static void testFreqToNote()
    {
        System.out.println("Frequency, Note name, Octave, Note number");

        for (double freq=MusicImporter.MIN_FREQ; freq<=MusicImporter.MAX_FREQ; freq+=1.0)
        {
            MusicImporter.MusicalNote note = MusicImporter.freqToNote(freq);
            System.out.println(freq + "," + note.name() + "," + note.octave() + "," + note.number());
        }
    }

    public static void testReadSample() throws UnsupportedAudioFileException, IOException {
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File("test_files/audio/sample.wav"));
        AudioFormat format = inputStream.getFormat();
        System.out.println(format.toString());
        inputStream.skipNBytes(256L * 1024 * format.getFrameSize()); // Arbitrary skip to get past silent intro
        MusicImporter.Sample data = MusicImporter.readSample(inputStream, 2048);
        double dt = 1.0 / format.getSampleRate();
        System.out.println("Time (ms), Level");
        for (int i = 0; i < data.levels().length; i++) {
            System.out.println(dt*i + ", " + data.levels()[i]);
        }
    }

    public static void testSampleToSpectrum() throws UnsupportedAudioFileException, IOException {
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File("test_files/audio/sample.wav"));
        AudioFormat format = inputStream.getFormat();
        System.out.println(format.toString());
        inputStream.skipNBytes(256L * 1024 * format.getFrameSize()); // Arbitrary skip to get past silent intro
        MusicImporter.Sample data = MusicImporter.readSample(inputStream, 2048);
        MusicImporter.Spectrum spectrum = MusicImporter.sampleToSpectrum(data);
        double frequencyResolution = spectrum.sampleFrequency() / (spectrum.levels().length * 2);
        System.out.println("Frequency, level");
        for (int i = 0; i < spectrum.levels().length; i++) {
            if (frequencyResolution*i <= 5000)
                System.out.println(frequencyResolution*i + ", " + spectrum.levels()[i]);
        }
    }

    public static void testSpectrumToNotes() throws UnsupportedAudioFileException, IOException {
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File("test_files/audio/sample.wav"));
        AudioFormat format = inputStream.getFormat();
        System.out.println(format.toString());
        inputStream.skipNBytes(256L * 1024 * format.getFrameSize()); // Arbitrary skip to get past silent intro
        MusicImporter.Sample data = MusicImporter.readSample(inputStream, 2048);
        MusicImporter.Spectrum spectrum = MusicImporter.sampleToSpectrum(data);
        MusicImporter.MusicalWord notes = MusicImporter.spectrumToNotes(spectrum);
        System.out.println("Note name, Octave, Note number, Level");
        for (int octave = 0; octave < notes.levels().length; octave++) {
            for (int note = 0; note < notes.levels()[octave].length; note++) {
                System.out.println(MusicImporter.NOTE_NAMES[note] + octave + ", " + octave + ", " + note + ", " +notes.levels()[octave][note]);
            }
        }
    }

    public static void testSampleToEmbeddings()
    {
        try {
            MusicImporter.getEmbeddings("test_files/audio/sample.wav");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
