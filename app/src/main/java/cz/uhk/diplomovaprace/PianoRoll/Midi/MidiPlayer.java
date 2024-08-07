package cz.uhk.diplomovaprace.PianoRoll.Midi;

import org.billthefarmer.mididriver.MidiConstants;
import org.billthefarmer.mididriver.MidiDriver;

import java.util.ArrayList;

public class MidiPlayer {

    private final MidiDriver midi;
    private ArrayList<Byte> playingNotes = new ArrayList<Byte>();
    private boolean isListening;

    public MidiPlayer () {
        midi = MidiDriver.getInstance();
        playingNotes = new ArrayList<Byte>();
        isListening = false;
    }

    public void playNote(byte pitch) {
        if (!isListening) {
            onMidiStart();
        }

        sendMidi(MidiConstants.NOTE_ON, pitch, 60);
        playingNotes.add(pitch);
    }

    public void stopNote(byte pitch) {
        sendMidi(MidiConstants.NOTE_OFF, pitch, 0);
        for (int i = 0; i < playingNotes.size();i++) {
            if (playingNotes.get(i) == pitch) {
                playingNotes.remove(i);
                break;
            }
        }
    }

    public void stopAllNotes() {
        for (int playingNote: playingNotes) {
            sendMidi(MidiConstants.NOTE_OFF, playingNote, 0);
        }

        playingNotes.clear();
    }

    public void onMidiStart() {
        if (!isListening) {
            midi.start();
            isListening = true;
        }
    }

    public void onMidiStop() {
        if (isListening) {
            midi.stop();
            isListening = false;
        }
    }

    public void sendMidi(int m, int n) {
        byte msg[] = new byte[2];

        msg[0] = (byte) m;
        msg[1] = (byte) n;

        midi.write(msg);
    }

    public void sendMidi(int m, int n, int v) {
        byte[] msg = new byte[3];

        msg[0] = (byte) m;
        msg[1] = (byte) n;
        msg[2] = (byte) v;

        midi.write(msg);
    }
}
