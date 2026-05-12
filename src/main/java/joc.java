import lombok.Data;
import java.util.List;
import java.io.File;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
class GameData {
    private Stare stare_initiala;
    private List<Scena> scene;
}

@Data
class Stare {
    private int viata_nora;
    private int timp;
    private int suspiciune;
}

@Data
class Scena {
    private String id;
    private String text;
    private List<Optiune> optiuni;
}

@Data
class Optiune {
    private String text;
    private String urmatoarea;
}

public class joc {
    public static void main(String[] args) {
        System.out.println("Proiectul Carry-On este configurat corect!");
    }
}