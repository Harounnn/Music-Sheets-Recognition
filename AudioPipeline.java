import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.sound.midi.*;
import javax.sound.sampled.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.audiveris.proxymusic.ScorePartwise;
import org.jfugue.pattern.Pattern;
import org.jfugue.midi.MidiFileManager;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class AudioPipeline {

  public static List<String> readNotes(String mxlPath) throws Exception {
    var events = new java.util.ArrayList<String>();
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mxlPath))) {
      ZipEntry e;
      while ((e = zis.getNextEntry()) != null) {
        if (e.getName().endsWith(".xml")) {
          SAXParserFactory spf = SAXParserFactory.newInstance();
          spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
          spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
          spf.setXIncludeAware(false);
          XMLReader reader = spf.newSAXParser().getXMLReader();
          SAXSource src = new SAXSource(reader, new InputSource(zis));
          JAXBContext jc = JAXBContext.newInstance(ScorePartwise.class);
          Unmarshaller u = jc.createUnmarshaller();
          ScorePartwise score = (ScorePartwise) u.unmarshal(src);

          score.getPart().forEach(p ->
            p.getMeasure().forEach(m ->
              m.getNoteOrBackupOrForward().stream()
                .filter(o -> o instanceof org.audiveris.proxymusic.Note)
                .map(o -> (org.audiveris.proxymusic.Note)o)
                .forEach(n -> {
                  String pitch = n.getPitch().getStep().name() + n.getPitch().getOctave();
                  int dur = n.getDuration().intValue();
                  String durToken = switch (dur) {
                    case 4 -> "w";
                    case 2 -> "h";
                    case 1 -> "q";
                    default -> "/" + (dur / 4.0);
                  };
                  events.add(pitch + durToken);
                })
            )
          );
          break;
        }
        zis.closeEntry();
      }
    }
    return events;
  }

  public static void createMidi(List<String> seq, File midiFile) throws Exception {
    String pattern = String.join(" ", seq);
    MidiFileManager.savePatternToMidi(new Pattern(pattern), midiFile);
    System.out.println("✅ MIDI saved at " + midiFile);
  }

  public static void synthesizeMidiToWav(File midiFile, File wavFile) throws Exception {
    Sequence seq = MidiSystem.getSequence(midiFile);

    Synthesizer synth = MidiSystem.getSynthesizer();
    synth.open();

    Sequencer sequencer = MidiSystem.getSequencer(false);
    sequencer.open();
    sequencer.setSequence(seq);
    sequencer.getTransmitter().setReceiver(synth.getReceiver());
    sequencer.start();

    AudioFormat fmt = new AudioFormat(44100, 16, 2, true, false);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
    line.open(fmt);
    line.start();

    try (AudioInputStream ais = new AudioInputStream(line)) {
      AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
    }

    sequencer.stop();
    sequencer.close();
    synth.close();
    line.stop();
    line.close();

    System.out.println("✅ WAV generated at " + wavFile);
  }

  public static void main(String[] args) throws Exception {
    List<String> notes = readNotes("output/sheet3.opus.mxl");

    File midi = new File("output/music.mid");
    createMidi(notes, midi);

    File wav = new File("output/music.wav");
    synthesizeMidiToWav(midi, wav);

    System.out.println("🎵 Pipeline complete — Play " + wav.getAbsolutePath());
  }
}
