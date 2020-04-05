package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static final char LAMBDA = 'λ', ARROW = '→';

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("input.txt");
        Scanner scanner = new Scanner(file);

        CFG cfg = new CFG();
        while(scanner.hasNext()) {
            String line = scanner.nextLine();
            char symbol = line.charAt(0);
            NonTerminal nonTerminal = cfg.addNonTerminal(symbol);
            int arrowIndex = line.indexOf(ARROW);
            String[] productions = line.substring(arrowIndex == -1 ? line.indexOf("->") + 2 : arrowIndex + 1).split("\\|");
            for(String production : productions) {
                production = production.trim();
                nonTerminal.addProduction(production);
                for(char c : production.toCharArray())
                    if(Character.isDigit(c) || !Character.isUpperCase(c))
                        cfg.addTerminal(c);
            }
        }
        System.out.println("Original: \n" + cfg.toString());
        cfg.simplify();
        System.out.println("Simplified: \n" + cfg.toString());
        System.out.print("Symbols: " + Arrays.toString(cfg.getSymbols()));
    }
}
