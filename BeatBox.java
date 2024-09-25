package Chapter_15.BeatBox;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

import static javax.sound.midi.ShortMessage.*;

public class BeatBox {
    // instance vars
    private ArrayList<JCheckBox> checkBoxArrayList; // store checkboxes in arraylists
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;

    // instrument array
    String [] instrumentNames = {
            "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic snare",
            "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo",
            "Maracas", "Whistle", "Low Conga", "Cowbell",
            "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"
    };
    int [] instruments = {
            35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63
    }; // these represent actual drum keys The drum channel is like a piano, except each key on piano is a different drum

    public static void main(String[] args) {
        new  BeatBox().buildGUI();
    }

    private void buildGUI() {
        JFrame frame = new JFrame("Cyber BeatBox");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        // an empty border gives us a margin between the edges of the panel and where the components are placed
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box buttnBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(e -> buildTrackAndStart());
        buttnBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> sequencer.stop());
        buttnBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(e -> changeTempo(1.03f));
        buttnBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(e -> changeTempo(0.97f));
        buttnBox.add(downTempo);

        JButton serialize = new JButton("Serialize it");
        serialize.addActionListener(e -> write());
        buttnBox.add(serialize);


        JButton restoreBtn = new JButton("Restore");
        restoreBtn.addActionListener(e -> readFile());
        buttnBox.add(restoreBtn);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(String instrumentName: instrumentNames){
            JLabel instrumentLabel = new JLabel(instrumentName);
            instrumentLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
            nameBox.add(instrumentLabel);
        }
        background.add(BorderLayout.EAST, buttnBox);
        background.add(BorderLayout.WEST, nameBox);

        frame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);

        JPanel mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        checkBoxArrayList = new ArrayList<>();
        for (int i = 0; i < 256; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxArrayList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        frame.setBounds(50, 50, 300, 300);
        frame.pack();
        frame.setVisible(true);
    }

    private void setUpMidi() {
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
    The tempo Factor scales the seuencer's tempo by the factor provided, slowing the beat down or speeding it up
     */
    private void changeTempo(float v) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor(tempoFactor * v);
    }

    private void buildTrackAndStart() {
        int [] trackList;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++){
            trackList = new int[16];

            int key = instruments[i];

            for (int j = 0; j < 16; j++){
                JCheckBox jc = checkBoxArrayList.get(j + 16 * i);
                /*
                  Is the checkbox at this beat selected? If yes, put the key value in this slot in the array
                  (the slot that represents this beat). Otherwise, the instrument is NOT supposed to play at this beat,
                  so set it to zero
                 */
                if(jc.isSelected())
                    trackList[j] = key;
                else
                    trackList[j] = 0;
            }
            makeTracks(trackList);
            track.add(makeEvent(CONTROL_CHANGE, 1, 127, 0, 16));
        }
        track.add(makeEvent(PROGRAM_CHANGE, 9, 1, 0, 15));

        try{
            sequencer.setSequence(sequence);
            // lets specify the number of loop iterations
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(120);
            sequencer.start();// now playing
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private MidiEvent makeEvent( int cmd, int chnl, int one, int two, int tick) {
        MidiEvent event = null;
        try{
            // this is the utility method
            ShortMessage msg = new ShortMessage();
            msg.setMessage(cmd, chnl, one, two);
            event= new MidiEvent(msg, tick);
        }catch (Exception e){
            e.printStackTrace();
        }
        return event;
    }

    /*
    This makes events for one instrument at at time, for all 16 beats. So it might get an int[] for the Bass drum, and each
    index in the array will hold either the key of that instrument or a zero. If it's a zero, the instrument isn't supposed
    to play at that beat. Otherwise, make an event and add it up to the track
     */
    private void makeTracks(int[] trackList) {
        for (int i = 0; i<16; i++){
            int key = trackList[i];
            /*
            Make the NOTE_ON and NOTE_OFF events, and add them to the track
             */
            if (key != 0){
                track.add(makeEvent(NOTE_ON, 9, key, 100, i));
                track.add(makeEvent(NOTE_OFF, 9, key, 100, i+1));
            }
        }
    }

    /**
     *
     */
    private void write(){
        // make a boolean array to hold the state of each checkbox
        boolean[]checkboxState = new boolean[256];
        /*
         * Walk throught the checkbox list
         * Get the state of each one
         * Add it to the boolean array
         */
        for (int i = 0; i < 256; i++) {
            JCheckBox check = checkBoxArrayList.get(i);
            if (check.isSelected()){
                checkboxState[i] = true;
            }
        }
        // try-with-resources
        try(ObjectOutputStream os =
                new ObjectOutputStream(new FileOutputStream("./src/chapter_15/checkbox.ser"))){
            os.writeObject(checkboxState);
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private void readFile(){
        boolean[] checkboxState = null;
        try (ObjectInputStream is =
                new ObjectInputStream(new FileInputStream("./src/chapter_15/checkbox.ser"))){
            /*
             * Read the single object in the file and cast it back to a boolean array
             * `readObject()` returns a reference of type Object()
             */
            checkboxState = (boolean[]) is.readObject();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        /*
         * Restore the state of each of the checboxes in the arrayList of actual JCheckBox objects
         */
        for (int i = 0; i < 256; i++) {
            JCheckBox check = checkBoxArrayList.get(i);
            check.setSelected(checkboxState[i]);
        }
        /*
         * Now stop whatever is currently playing and rebuild the sequence using the new
         * state of the checkboxes in the ArrayList
         */
        sequencer.stop();
        buildTrackAndStart();
    }

}
