import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.io.File;
import java.util.*;

@Data
class GameData {
    public String title;
    public String startBlock;
    public Map<String, PropertyInfo> properties;
    public Map<String, Block> blocks;
}

@Data
class PropertyInfo {
    public int min;
    public int max;
    public int initial;
    public String hudLabel;
    public boolean visibleInHud;
    public int hudOrder;
    public String onMinBlock;
    public String onMaxBlock;
}

@Data
class Block {
    public String text;
    public List<Decision> decisions;
}

@Data
class Decision {
    public String text;
    public String targetBlock;
    public Condition condition;
    public List<Effect> effects;
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ComparisonCondition.class, name = "COMPARISON"),
    @JsonSubTypes.Type(value = AndCondition.class, name = "AND"),
    @JsonSubTypes.Type(value = OrCondition.class, name = "OR")
})
interface Condition {
    boolean evaluate(Map<String, Integer> stats);
}

@Data
class ComparisonCondition implements Condition {
    public String property;
    public String operator;
    public int value;

    @Override
    public boolean evaluate(Map<String, Integer> stats) {
        int val = stats.getOrDefault(property, 0);
        switch (operator) {
            case "<": return val < value;
            case "<=": return val <= value;
            case ">": return val > value;
            case ">=": return val >= value;
            case "==": return val == value;
            case "!=": return val != value;
            default: return false;
        }
    }
}

@Data
class AndCondition implements Condition {
    public List<Condition> conditions;

    @Override
    public boolean evaluate(Map<String, Integer> stats) {
        for (Condition c : conditions) {
            if (!c.evaluate(stats)) return false;
        }
        return true;
    }
}

@Data
class OrCondition implements Condition {
    public List<Condition> conditions;

    @Override
    public boolean evaluate(Map<String, Integer> stats) {
        for (Condition c : conditions) {
            if (c.evaluate(stats)) return true;
        }
        return false;
    }
}

@Data
class Effect {
    public String type;
    public String property;
    public int value;
}

public class joc {
    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File jsonFile = new File("src/main/resources/joc.json");
            GameData game = mapper.readValue(jsonFile, GameData.class);
            
            Scanner scanner = new Scanner(System.in);
            Map<String, Integer> currentStats = new HashMap<>();
            game.properties.forEach((k, v) -> currentStats.put(k, v.initial));
            
            String currentBlockId = game.startBlock;

            while (currentBlockId != null) {
                Block currentBlock = game.blocks.get(currentBlockId);
                if (currentBlock == null) break;

                System.out.println("\n\n================================================");
                System.out.println("   " + game.title.toUpperCase());
                System.out.println("================================================");
                
                game.properties.entrySet().stream()
                    .filter(e -> e.getValue().isVisibleInHud())
                    .sorted(Comparator.comparingInt(e -> e.getValue().getHudOrder()))
                    .forEach(e -> System.out.print("[" + e.getValue().getHudLabel() + ": " + currentStats.get(e.getKey()) + "] "));
                
                System.out.println("\n------------------------------------------------");
                System.out.println(currentBlock.text);
                
                if (currentBlock.decisions == null || currentBlock.decisions.isEmpty()) {
                    System.out.println("\n--- JOCUL S-A TERMINAT ---");
                    break;
                }
                
                List<Decision> availableDecisions = new ArrayList<>();
                for (Decision d : currentBlock.decisions) {
                    if (d.getCondition() == null || d.getCondition().evaluate(currentStats)) {
                        availableDecisions.add(d);
                    }
                }
                
                System.out.println("\nAlege actiunea:");
                for (int i = 0; i < availableDecisions.size(); i++) {
                    System.out.println((i + 1) + ". " + availableDecisions.get(i).text);
                }
                
                System.out.print("\nIntrodu numarul: ");
                int choiceIndex = scanner.nextInt() - 1;
                
                if (choiceIndex < 0 || choiceIndex >= availableDecisions.size()) {
                    System.out.println("Optiune invalida!");
                    continue;
                }

                Decision choice = availableDecisions.get(choiceIndex);
                
                if (choice.effects != null) {
                    for (Effect e : choice.effects) {
                        if (currentStats.containsKey(e.property)) {
                            int currentVal = currentStats.get(e.property);
                            if (e.type.equals("ADD")) currentVal += e.value;
                            else if (e.type.equals("SUB")) currentVal -= e.value;
                            
                            PropertyInfo info = game.properties.get(e.property);
                            if (currentVal < info.min) currentVal = info.min;
                            if (currentVal > info.max) currentVal = info.max;
                            currentStats.put(e.property, currentVal);
                        }
                    }
                }
                
                String triggeredBlock = null;
                for (Map.Entry<String, Integer> entry : currentStats.entrySet()) {
                    PropertyInfo info = game.properties.get(entry.getKey());
                    if (entry.getValue() == info.max && info.onMaxBlock != null) {
                        triggeredBlock = info.onMaxBlock;
                        break;
                    }
                    if (entry.getValue() == info.min && info.onMinBlock != null) {
                        triggeredBlock = info.onMinBlock;
                        break;
                    }
                }

                if (triggeredBlock != null) {
                    currentBlockId = triggeredBlock;
                } else {
                    currentBlockId = choice.targetBlock;
                }
            }
        } catch (Exception e) {
            System.out.println("Eroare la rulare!");
            e.printStackTrace();
        }
    }
}