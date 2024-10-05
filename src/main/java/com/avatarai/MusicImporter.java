package com.avatarai;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Goal
// Identify for each sample the musical notes included (and their levels)
// Treating these samples as "words" convert them into embeddings
//
// For a given sound file, if the sampling frequency is F and the fft resolution is R
// Each data point in the fft represents a frequency step of F/R
// Only the first half of the FFT is usable as it reflects on the centre line
// Hence, frequency range is 0Hz to (F/2R)Hz
//
// For typical music frequency range is 27.5Hz (A0) to 5000Hz (D#8)
// This represents 91 musical notes
//
// For music sampled at 44.1KHz (typical) a sample set of 32768 gives:
// Frequency step: 1.35Hz
// Sample length: 0.74s
//
// Note gaps (approx):
// Octave 0: 1.5Hz (Upper end of octave)
// Octave 3: 10Hz
// Octave 5: 30Hz
// Octave 7: 200Hz
//

public class MusicImporter {
    // Note number 0 = C, 12 = B
    public record Sample(double[] levels, double sampleFrequency) {}
    public record Spectrum(double[] levels, double sampleFrequency) {}
    public record MusicalNote(String name, int octave, int number) {}
    public record MusicalWord(double[][] levels, double amplitude) {}

    public static final String[] NOTE_NAMES = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static final double[][] RELATIVE_LOUDNESS = new double[][]{
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}
    };
    public static double MIN_FREQ = 16.351; // C0
    public static double MAX_FREQ = 15804.264; // B9
//    public static double MIN_FREQ = 32.703; // C1
//    public static double MAX_FREQ = 3951.066; // B7

    public static double[] getEmbeddings(String audioFilename) throws Exception {
        double[] embeddings = null;
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(audioFilename));
        AudioFormat format = inputStream.getFormat();
        System.out.println(format.toString());
        long numFrames = inputStream.getFrameLength();
        int frameSize = format.getFrameSize();

        inputStream.skipNBytes(512L * 1024 * frameSize); // Arbitrary skip to get past silent intro
        double time1 = System.currentTimeMillis();
//        for (int sample=0; sample<numFrames/8192; sample++) {
            Sample data = readSample(inputStream, 4096);
            Spectrum spectrum = sampleToSpectrum(data);
            MusicalWord notes = spectrumToNotes(spectrum);
//        }
        double time2 = System.currentTimeMillis();
        System.out.println("Time taken: " + (time2-time1) + "ms");
        System.out.println("Samples taken: " + (numFrames/8192));
        System.out.println("Note name, Octave, Note number, Level");
        for (int octave = 0; octave < notes.levels().length; octave++) {
            for (int note = 0; note < notes.levels()[octave].length; note++) {
                System.out.println(NOTE_NAMES[note] + octave + ", " + octave + ", " + note + ", " +notes.levels()[octave][note]);
            }
        }
        return embeddings;
    }

    public static Sample readSample(AudioInputStream inputStream, int sampleSize) throws IOException {
        double[] data = new double[sampleSize];
        AudioFormat format = inputStream.getFormat();
        byte[] bytes = inputStream.readNBytes(sampleSize * format.getFrameSize());
        ByteOrder byteOrder = (format.isBigEndian() ? ByteOrder.BIG_ENDIAN: ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < data.length; i++) {
            for (int channel = 0; channel < format.getChannels(); channel++) {
                int valueSize = format.getFrameSize()/format.getChannels();
                byte[] sampleBytes = new byte[valueSize];
                System.arraycopy(bytes, (i* format.getChannels()+channel) * valueSize, sampleBytes, 0, valueSize);
                if (valueSize == 1)
                    data[i] += (((char)sampleBytes[0])-Byte.MAX_VALUE) / (double)Byte.MAX_VALUE; // 8 bit wav files are UINT8 format)
                else if (valueSize == 2)
                    data[i] += ByteBuffer.wrap(sampleBytes).order(byteOrder).getShort() / (double)Short.MAX_VALUE;
                else if (valueSize == 4)
                    data[i] += ByteBuffer.wrap(sampleBytes).order(byteOrder).getInt() / (double)Integer.MAX_VALUE;
                else if (valueSize == 8)
                    data[i] += ByteBuffer.wrap(sampleBytes).order(byteOrder).getLong() / (double)Long.MAX_VALUE;
            }
            data[i] /= format.getChannels(); // Average value across channels if more than one
        }
        return new Sample(data, format.getSampleRate());
    }

    public static MusicalNote freqToNote(double frequency) {
        int noteNum = (int)Math.rint(12*Math.log(frequency/440)/Math.log(2))+57;
        int octave = noteNum / 12;
        noteNum = noteNum % 12;
        return new MusicalNote(NOTE_NAMES[noteNum], octave, noteNum);
    }

    public static MusicalWord spectrumToNotes(Spectrum spectrum) {
        double[][] noteLevels = new double[10][12]; // To cover 10 octaves, 12 notes each
        double frequencyResolution = spectrum.sampleFrequency() / (spectrum.levels().length * 2);
        double amplitude = 0.0;
        for (int i=1; i<spectrum.levels().length; i++) {
            double freq = frequencyResolution * i;
            if (freq >= MIN_FREQ && freq <= MAX_FREQ) {
                MusicalNote note = freqToNote(freq);
                noteLevels[note.octave][note.number] = Math.max(noteLevels[note.octave][note.number], spectrum.levels()[i] * RELATIVE_LOUDNESS[note.octave][note.number]);
                amplitude = Math.max(noteLevels[note.octave][note.number], amplitude);
            }
        }
        return new MusicalWord(noteLevels, amplitude);
    }

    public static Spectrum sampleToSpectrum(Sample sample) {
        Complex[] results = SignalProcessor.fft(sample.levels);
        double[] spectrum = new double[results.length/2];
        for (int i = 0; i < spectrum.length; i++) {
            spectrum[i] = results[i].abs();
            if (i>0) spectrum[i] += results[results.length-i].abs();
        }
        return new Spectrum(spectrum, sample.sampleFrequency);
    }
}
