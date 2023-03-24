package cz.uhk.diplomovaprace.PianoRoll;

import android.content.Context;
import android.media.SoundPool;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cz.uhk.diplomovaprace.R;


public class SoundJava {

    private Context context;
    private SoundPool soundPool;
    private float LEFT_VOL = 1.0f;
    private float RIGHT_VOL = 1.0f;
    private int PRIORITY = 1;
    private int LOOP = 0;
    private float RATE = 1.0f;
    private Map<String, Integer> sounds = new HashMap<String, Integer>();

    public SoundJava (Context context) {
        this.context = context;
        loadSounds();
    }

    public void loadSounds() {
        SoundPool.Builder builder = new SoundPool.Builder().setMaxStreams(12);
        soundPool = builder.build();
        /*sounds.put("c0", soundPool.load(context.getApplicationContext(), R.raw.c0,1));
        sounds.put("cis0", soundPool.load(context.getApplicationContext(), R.raw.cis0,1));
        sounds.put("d0", soundPool.load(context.getApplicationContext(), R.raw.d0,1));
        sounds.put("dis0", soundPool.load(context.getApplicationContext(), R.raw.dis0,1));
        sounds.put("e0", soundPool.load(context.getApplicationContext(), R.raw.e0,1));
        sounds.put("f0", soundPool.load(context.getApplicationContext(), R.raw.f0,1));
        sounds.put("fis0", soundPool.load(context.getApplicationContext(), R.raw.fis0,1));
        sounds.put("g0", soundPool.load(context.getApplicationContext(), R.raw.g0,1));
        sounds.put("gis0", soundPool.load(context.getApplicationContext(), R.raw.gis0,1));
        sounds.put("a0", soundPool.load(context.getApplicationContext(), R.raw.a0,1));
        sounds.put("ais0", soundPool.load(context.getApplicationContext(), R.raw.ais0,1));
        sounds.put("b0", soundPool.load(context.getApplicationContext(), R.raw.b0,1));
        sounds.put("c1", soundPool.load(context.getApplicationContext(), R.raw.c1,1));
        sounds.put("cis1", soundPool.load(context.getApplicationContext(), R.raw.cis1,1));
        sounds.put("d1", soundPool.load(context.getApplicationContext(), R.raw.d1,1));
        sounds.put("dis1", soundPool.load(context.getApplicationContext(), R.raw.dis1,1));
        sounds.put("e1", soundPool.load(context.getApplicationContext(), R.raw.e1,1));
        sounds.put("f1", soundPool.load(context.getApplicationContext(), R.raw.f1,1));
        sounds.put("fis1", soundPool.load(context.getApplicationContext(), R.raw.fis1,1));
        sounds.put("g1", soundPool.load(context.getApplicationContext(), R.raw.g1,1));
        sounds.put("gis1", soundPool.load(context.getApplicationContext(), R.raw.gis1,1));
        sounds.put("a1", soundPool.load(context.getApplicationContext(), R.raw.a1,1));
        sounds.put("ais1", soundPool.load(context.getApplicationContext(), R.raw.ais1,1));
        sounds.put("b1", soundPool.load(context.getApplicationContext(), R.raw.b1,1));*/
        sounds.put("c2", soundPool.load(context.getApplicationContext(), R.raw.c2,1));
        sounds.put("cis2", soundPool.load(context.getApplicationContext(), R.raw.cis2,1));
        sounds.put("d2", soundPool.load(context.getApplicationContext(), R.raw.d2,1));
        sounds.put("dis2", soundPool.load(context.getApplicationContext(), R.raw.dis2,1));
        sounds.put("e2", soundPool.load(context.getApplicationContext(), R.raw.e2,1));
        sounds.put("f2", soundPool.load(context.getApplicationContext(), R.raw.f2,1));
        sounds.put("fis2", soundPool.load(context.getApplicationContext(), R.raw.fis2,1));
        sounds.put("g2", soundPool.load(context.getApplicationContext(), R.raw.g2,1));
        sounds.put("gis2", soundPool.load(context.getApplicationContext(), R.raw.gis2,1));
        sounds.put("a2", soundPool.load(context.getApplicationContext(), R.raw.a2,1));
        sounds.put("ais2", soundPool.load(context.getApplicationContext(), R.raw.ais2,1));
        sounds.put("b2", soundPool.load(context.getApplicationContext(), R.raw.b2,1));
        sounds.put("c3", soundPool.load(context.getApplicationContext(), R.raw.c3,1));
        sounds.put("cis3", soundPool.load(context.getApplicationContext(), R.raw.cis3,1));
        sounds.put("d3", soundPool.load(context.getApplicationContext(), R.raw.d3,1));
        sounds.put("dis3", soundPool.load(context.getApplicationContext(), R.raw.dis3,1));
        sounds.put("e3", soundPool.load(context.getApplicationContext(), R.raw.e3,1));
        sounds.put("f3", soundPool.load(context.getApplicationContext(), R.raw.f3,1));
        sounds.put("fis3", soundPool.load(context.getApplicationContext(), R.raw.fis3,1));
        sounds.put("g3", soundPool.load(context.getApplicationContext(), R.raw.g3,1));
        sounds.put("gis3", soundPool.load(context.getApplicationContext(), R.raw.gis3,1));
        sounds.put("a3", soundPool.load(context.getApplicationContext(), R.raw.a3,1));
        sounds.put("ais3", soundPool.load(context.getApplicationContext(), R.raw.ais3,1));
        sounds.put("b3", soundPool.load(context.getApplicationContext(), R.raw.b3,1));
        sounds.put("c4", soundPool.load(context.getApplicationContext(), R.raw.c4,1));
        sounds.put("cis4", soundPool.load(context.getApplicationContext(), R.raw.cis4,1));
        sounds.put("d4", soundPool.load(context.getApplicationContext(), R.raw.d4,1));
        sounds.put("dis4", soundPool.load(context.getApplicationContext(), R.raw.dis4,1));
        sounds.put("e4", soundPool.load(context.getApplicationContext(), R.raw.e4,1));
        sounds.put("f4", soundPool.load(context.getApplicationContext(), R.raw.f4,1));
        sounds.put("fis4", soundPool.load(context.getApplicationContext(), R.raw.fis4,1));
        sounds.put("g4", soundPool.load(context.getApplicationContext(), R.raw.g4,1));
        sounds.put("gis4", soundPool.load(context.getApplicationContext(), R.raw.gis4,1));
        sounds.put("a4", soundPool.load(context.getApplicationContext(), R.raw.a4,1));
        sounds.put("ais4", soundPool.load(context.getApplicationContext(), R.raw.ais4,1));
        sounds.put("b4", soundPool.load(context.getApplicationContext(), R.raw.b4,1));
        sounds.put("c5", soundPool.load(context.getApplicationContext(), R.raw.c5,1));
        sounds.put("cis5", soundPool.load(context.getApplicationContext(), R.raw.cis5,1));
        sounds.put("d5", soundPool.load(context.getApplicationContext(), R.raw.d5,1));
        sounds.put("dis5", soundPool.load(context.getApplicationContext(), R.raw.dis5,1));
        sounds.put("e5", soundPool.load(context.getApplicationContext(), R.raw.e5,1));
        sounds.put("f5", soundPool.load(context.getApplicationContext(), R.raw.f5,1));
        sounds.put("fis5", soundPool.load(context.getApplicationContext(), R.raw.fis5,1));
        sounds.put("g5", soundPool.load(context.getApplicationContext(), R.raw.g5,1));
        sounds.put("gis5", soundPool.load(context.getApplicationContext(), R.raw.gis5,1));
        sounds.put("a5", soundPool.load(context.getApplicationContext(), R.raw.a5,1));
        sounds.put("ais5", soundPool.load(context.getApplicationContext(), R.raw.ais5,1));
        sounds.put("b5", soundPool.load(context.getApplicationContext(), R.raw.b5,1));
        sounds.put("c6", soundPool.load(context.getApplicationContext(), R.raw.c6,1));
        sounds.put("cis6", soundPool.load(context.getApplicationContext(), R.raw.cis6,1));
        sounds.put("d6", soundPool.load(context.getApplicationContext(), R.raw.d6,1));
        sounds.put("dis6", soundPool.load(context.getApplicationContext(), R.raw.dis6,1));
        sounds.put("e6", soundPool.load(context.getApplicationContext(), R.raw.e6,1));
        sounds.put("f6", soundPool.load(context.getApplicationContext(), R.raw.f6,1));
        sounds.put("fis6", soundPool.load(context.getApplicationContext(), R.raw.fis6,1));
        sounds.put("g6", soundPool.load(context.getApplicationContext(), R.raw.g6,1));
        sounds.put("gis6", soundPool.load(context.getApplicationContext(), R.raw.gis6,1));
        sounds.put("a6", soundPool.load(context.getApplicationContext(), R.raw.a6,1));
        sounds.put("ais6", soundPool.load(context.getApplicationContext(), R.raw.ais6,1));
        sounds.put("b6", soundPool.load(context.getApplicationContext(), R.raw.b6,1));
        sounds.put("c7", soundPool.load(context.getApplicationContext(), R.raw.c7,1));
    }

    public int playSound(String note) { // sound is already loaded
        return soundPool.play(sounds.get(note), LEFT_VOL, RIGHT_VOL, PRIORITY, LOOP, RATE);
    }

    public void stopSound(Integer streamId) {
        soundPool.stop(streamId);
    }
}
