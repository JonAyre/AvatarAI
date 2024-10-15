package com.avatarai;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
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
    public record MusicalWord(double[] levels, double amplitude) {}

    public static final String[] NOTE_NAMES = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static double MIN_FREQ = 16.351; // C0
    public static double MAX_FREQ = 15804.264; // B9
    public static double TOKENISATION_FLOOR = 0.01; // Any peak amplitude below this is treated as this value tokenisation
    public static double TOKENISATION_RATIO = 0.333; // The proportion of peak amplitude above which a level is considered to be 1.0 for tokenisation purposes
//    public static double MIN_FREQ = 32.703; // C1
//    public static double MAX_FREQ = 3951.066; // B7

    private MusicEmbeddingsModel model;

    public MusicImporter(String modelFilename) throws IOException {
        model = new MusicEmbeddingsModel(modelFilename);
    }

    public static Sample readSample(AudioInputStream inputStream, int sampleSize) {
        double[] data = new double[sampleSize];
        AudioFormat format = inputStream.getFormat();
        byte[] bytes;
        try {
            bytes = inputStream.readNBytes(sampleSize * format.getFrameSize());
            if (bytes.length < sampleSize * format.getFrameSize()) return null;
        } catch (IOException e){
            return null;
        }
        ByteOrder byteOrder = (format.isBigEndian() ? ByteOrder.BIG_ENDIAN: ByteOrder.LITTLE_ENDIAN);
        int valueSize = format.getFrameSize()/format.getChannels();

        for (int i = 0; i < data.length; i++) {
            for (int channel = 0; channel < format.getChannels(); channel++) {
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
        double[] noteLevels = new double[120]; // To cover 10 octaves, 12 notes each
        double frequencyResolution = spectrum.sampleFrequency() / (spectrum.levels().length * 2);
        double amplitude = 0.0;
        for (int i=1; i<spectrum.levels().length; i++) {
            double freq = frequencyResolution * i;
            if (freq >= MIN_FREQ && freq <= MAX_FREQ) {
                MusicalNote note = freqToNote(freq);
                int noteNum = note.octave() * 12 + note.number();
                noteLevels[noteNum] = Math.max(noteLevels[noteNum], spectrum.levels()[i]);
                amplitude = Math.max(noteLevels[noteNum], amplitude);
            }
        }
        return new MusicalWord(noteLevels, amplitude);
    }

    public static MusicalWord tokeniseWord(MusicalWord word) {
        double magnitude = 0.0;
        double[] newLevels = new double[word.levels().length];
        for (int i=0; i<word.levels().length; i++) {
            if (word.levels()[i] > Math.max(word.amplitude, TOKENISATION_FLOOR) * TOKENISATION_RATIO)
                newLevels[i] = 1.0;
            else
                newLevels[i] = 0.0;
            magnitude += newLevels[i];
        }
        return new MusicalWord(newLevels, Math.sqrt(magnitude));
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
