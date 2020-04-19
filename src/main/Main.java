package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    public static final char LAMBDA = 'λ', ARROW = '→';

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("./data/input.txt");
        Scanner scanner = new Scanner(file);

        CFG cfg = new CFG();
        while(scanner.hasNext()) {
            String line = scanner.nextLine();
            char symbol = line.charAt(0);
            NonTerminal nonTerminal = cfg.addNonTerminal(symbol); // takes care of any productions under the same non-terminal symbol
            int arrowIndex = line.indexOf(ARROW); // index of the special arrow, but searches for -> instead if not found
            String[] productions = line.substring(arrowIndex == -1 ? line.indexOf("->") + 2 : arrowIndex + 1).split("\\|"); // split's everything after the arrow by the vertical bar
            for(String production : productions) {
                production = production.trim();
                nonTerminal.addProduction(production);
                for(char c : production.toCharArray())
                    if(Character.isDigit(c) || !Character.isUpperCase(c)) // terminals are either lowercase letters or any number
                        cfg.addTerminal(c);
            }
        }
        System.out.println("Original: \n" + cfg.toString());
        cfg.convertToCNF('Z');
        System.out.println("CNF Form: \n" + cfg.toString());

        String input;
        scanner = new Scanner(System.in);

        while(true) {

            System.out.print("Enter a string to check membership (or 'exit'): ");
            input = scanner.nextLine();

            if(input.toLowerCase().contains("exit") || input.length() == 0) {
                System.out.println("Bye!");
                break;
            }

            System.out.println("-> Is \"" + input + "\" a member of the given language?: " + (cfg.runCYK(input) ? "Yes" : "No") + "\n");
        }
        scanner.close();
    }
}
