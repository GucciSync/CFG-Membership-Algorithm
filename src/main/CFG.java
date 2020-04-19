package main;

import java.util.*;

public class CFG {

    private char startSymbol;

    /* A list of all terminal symbols */
    private List<Character> terminals;

    /* A HashMap of all non-terminals in the CFG, where the key is each non-terminal's symbol */
    private HashMap<Character, NonTerminal> nonTerminals;

    public CFG() {
        startSymbol = 'S';
        terminals = new ArrayList<>();
        nonTerminals = new LinkedHashMap<>(); // LinkedHashMap to preserve order for displaying
    }

    /* Add a terminal symbol to the terminals list if it isn't already in there */
    public void addTerminal(char symbol) {
        if(!terminals.contains(symbol))
            terminals.add(symbol);
    }

    /* Add a non-terminal with the given symbol to a newly created non-terminals map, with the rest of the non-terminals in the old map to follow (to preserve ordering) */
    public void replaceStart(char newStartSymbol) {
        if(nonTerminals.containsKey(newStartSymbol)) // the desired new start symbol cannot already exist in the map
            throw new RuntimeException("CFG already contains the symbol: " + newStartSymbol + ".");
        HashMap<Character, NonTerminal> newMap = new LinkedHashMap<>();
        NonTerminal nonTerminal = new NonTerminal(newStartSymbol);
        newMap.put(newStartSymbol, nonTerminal);
        newMap.putAll(nonTerminals);
        nonTerminal.addProduction(Character.toString(startSymbol)); // add the old start symbol as a production for the new one
        startSymbol = newStartSymbol;
        nonTerminals = newMap;
    }

    /* Add a terminal symbol with the given symbol to the non-terminals map, if it isn't already in there */
    public NonTerminal addNonTerminal(char symbol) {
        if(nonTerminals.containsKey(symbol))
            return nonTerminals.get(symbol);
        NonTerminal nonTerminal = new NonTerminal(symbol);
        nonTerminals.put(symbol, nonTerminal);
        return nonTerminal;
    }

    /* Add a terminal symbol with the given symbol to a newly created non-terminals map, with the rest of the non-terminals in the old map to follow (for the purposes of addition while iterating) */
    public NonTerminal addNonTerminalNewMap(char symbol) {
        NonTerminal nonTerminal = new NonTerminal(symbol);
        HashMap<Character, NonTerminal> newMap = new LinkedHashMap<>(nonTerminals);
        newMap.put(symbol, nonTerminal);
        nonTerminals = newMap;
        return nonTerminal;
    }

    // Based off of https://en.wikipedia.org/wiki/CYK_algorithm
    public boolean runCYK(String input) {
        List<NonTerminal> nTList = new ArrayList<>(nonTerminals.values()); // an index-able list of all non-terminals used for referencing in the table below
        List<Production> nonTerminalProductions = new ArrayList<>(); // a list of all productions containing only non-terminal symbols
        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList())
                if(p.getExpression().length() != 1) // productions containing only non-terminal symbols (should be any productions with a length not equal to 1 because of CNF form)
                    nonTerminalProductions.add(p);
        boolean[][][] table = new boolean[input.length()][input.length()][nTList.size()]; // the boolean array used to keep track of substring matches, where the third dimension refers to the indexes of the non-terminals in the list above
        for(int s = 0; s < input.length(); s++)
            for(NonTerminal n : nonTerminals.values())
                for(Production p : n.getProductionList())
                    if(p.getExpression().equals(Character.toString(input.charAt(s)))) // matching productions for substrings of length 1, which is the character at string[s]
                        table[0][s][nTList.indexOf(p.getNonTerminal())] = true; // bottom row of table
        for(int l = 2; l <= input.length(); l++)
            for(int s = 0; s < input.length() - l + 1; s++)
                for(int p = 1; p <= l - 1; p++)
                    for (Production production : nonTerminalProductions) {
                        int b = nTList.indexOf(nonTerminals.get(production.getExpression().charAt(0))); // index of the first non-terminal in each production
                        int c = nTList.indexOf(nonTerminals.get(production.getExpression().charAt(1))); // index of the second non-terminal in each production
                        if (table[p - 1][s][b] && table[l - p - 1][s + p][c]) // if both current substring references in bottom rows of the table are true
                            table[l - 1][s][nTList.indexOf(production.getNonTerminal())] = true;
                    }
        return table[input.length() - 1][0][0]; // true if the top of the 'CYK triangle' is true (if the top contains the start symbol)
    }

    public void convertToCNF(char newStartSymbol) {
        boolean newStart = false;
        for (NonTerminal nonterminal : nonTerminals.values())
            for (Production production : nonterminal.getProductionList())
                if (production.getExpression().contains(Character.toString(startSymbol))) { // determine if any production contains the start symbol so that we know to create a new start
                    newStart = true;
                    break;
                }

        if (newStart) // if needed, replace the current start with a new one
            replaceStart(newStartSymbol);

        simplify();

        System.out.println("Simplified:\n" + toString());

        HashMap<String, Character> standAlones = new HashMap<>(); // used to avoid making duplicate standalone productions when separating productions that don't satisfy CNF form (with the key being the production expression itself so we can quickly lookup if one already exists)
        for (NonTerminal n : nonTerminals.values())
            if(n.getProductionList().size() == 1 && n.getProductionList().get(0).satisfiesCNF()) // to be a standalone the non-terminal must have only 1 production, and that 1 production must be in CNF form
                standAlones.put(n.getProductionList().get(0).getExpression(), n.getSymbol());

        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList()) { // go through each production and convert it to CNF form if it isn't already
                if(p.satisfiesCNF())
                    continue;
                for(int i = 0; i < p.getExpression().length(); i++) { // turn all terminals into non-terminals
                    char c = p.getExpression().charAt(i);
                    if(terminals.contains(c)) // check if the character is a terminal
                        if(standAlones.containsKey(Character.toString(c))) // check if there's a standalone with the current character already
                            p.replaceChar(i, standAlones.get(Character.toString(c))); // if so, we can just replace the character with the character to the non-terminal symbol the standalone belongs to
                        else { // otherwise create a new non-terminal with a single production containing the current character, replace the current character with the one of the newly created non-terminal, and add it to standalones
                            char newSymbol = getNextUnusedSymbol();
                            NonTerminal nonTerminal = addNonTerminalNewMap(newSymbol);
                            nonTerminal.addProduction(Character.toString(c));
                            p.replaceChar(i, newSymbol);
                            standAlones.put(Character.toString(c), newSymbol);
                    }
                }
                while(!p.satisfiesCNF()) { // only productions with a length of at least 3 that contain only non-terminals should remain at this point
                    char c1 = p.getExpression().charAt(0), c2 = p.getExpression().charAt(1), c3 = p.getExpression().charAt(2); // the first three characters in the production
                    String chosenExpression = standAlones.containsKey("" + c1 + c2) ? "" + c1 + c2 : standAlones.containsKey("" + c1 + c3) ? "" + c1 + c3 :
                            standAlones.containsKey("" + c2 + c3) ? "" + c2 + c3 : "" + c1 + c2; // check all combinations for pre-existing standalones to ensure we get the most simplified form
                    if(standAlones.containsKey(chosenExpression)) // if there's already a standalone with the chosen expression, replace the chosen expression with the non-terminal symbol the standalone belongs to
                        p.replaceFirstPairWithChar(chosenExpression, standAlones.get(chosenExpression));
                    else { // otherwise create a new non-terminal with a single production containing the chosen expression, replace the current chosen expression with the one of the newly created non-terminal, and add it to standalones
                        char newSymbol = getNextUnusedSymbol();
                        NonTerminal nonTerminal = addNonTerminalNewMap(newSymbol);
                        nonTerminal.addProduction(chosenExpression);
                        p.replaceFirstPairWithChar(chosenExpression, newSymbol);
                        standAlones.put(chosenExpression, newSymbol);
                    }
                }
            }

    }

    public void simplify() {
        /* Lambda removal */
        List<NonTerminal> nullables = getNonTerminalsContaining(Main.LAMBDA); // the list of all non-terminals that lead to a lambda
        for(NonTerminal n : nullables)
            for(Production production : n.getProductionList())
                if(production.getExpression().contains(Character.toString(Main.LAMBDA)))
                    n.removeProduction(production);
        while(true) { // find all nonterminal indirect "paths" to lambda productions
            List<NonTerminal> newNullables = new ArrayList<>(); // the list of newly found nullables
            for(NonTerminal nonTerminal : nullables) {
                List<NonTerminal> oneLevelUp = getNonTerminalsContaining(nonTerminal.getSymbol());
                for(NonTerminal n : oneLevelUp)
                    if(!newNullables.contains(n) && !nullables.contains(n) && n.getSymbol() != 'S') // stop at original start symbol
                        newNullables.add(n);
            }
            if(newNullables.size() == 0) // if no new nullables have been found, exit the loop
                break;
            nullables.addAll(newNullables); // add the new nullables to the list of nullables
        }
        for(NonTerminal n : nullables) { // for each nullable, find every production that contains the non-terminal's symbol
            List<Production> relevantProductions = getProductionsContaining(n.getSymbol());
            for(Production production : relevantProductions) {
                if(production.getExpression().length() == 1) // ignore unit productions
                    continue;
                for(int i = 0; i < production.getExpression().length(); i++)
                    if(production.getExpression().charAt(i) == n.getSymbol()) // 1 character at a time because there could be more than one occurrence of the nullable's character, which would require its own new producution
                        production.getNonTerminal().addProduction(production.getExpression().substring(0, i) + production.getExpression().substring(i + 1)); // new production with the offending nullable character having been extracted
            }
        }

        /* Unit production removal */
        List<Production> unitProductions = getUnitProductions(); // the list of all unit productions
        while(unitProductions.size() > 0) {
            for(Production unitProduction : unitProductions) {
                NonTerminal unitProductionSymbol = nonTerminals.get(unitProduction.getExpression().charAt(0)); // the non-terminal the character the unit production refers to
                for(Production production : unitProductionSymbol.getProductionList()) // add all productions from the unit production's non-terminal
                   unitProduction.getNonTerminal().addProduction(production.getExpression()); // addProduction method takes care of any duplicates
                unitProduction.getNonTerminal().removeProduction(unitProduction); // remove the current unit production from the list
            }
            unitProductions = getUnitProductions(); // update list each time because unit productions might be moved around a few times before they're completely gone
        }

        /* Useless production removal */
        Set<Character> containsTerminal = getTerminatingNonTerminals(); // the list of all non-terminals that terminate
        if(!containsTerminal.contains(startSymbol)) // the start symbol must terminate
            throw new RuntimeException("This grammar does not terminate on start symbol: " + startSymbol + ".");
        Iterator<Map.Entry<Character, NonTerminal>> iter = nonTerminals.entrySet().iterator();
        while(iter.hasNext()) { // iterate through the non-terminals map
            Map.Entry<Character, NonTerminal> entry = iter.next();
            if (containsTerminal.contains(entry.getValue().getSymbol())) // no need to do anything if the non-terminal terminates
                continue;
            List<Production> toBeRemoved = getProductionsContaining(entry.getValue().getSymbol()); // the list of all productions that contain the non-terminal symbol
            toBeRemoved.forEach(p -> p.getNonTerminal().removeProduction(p)); // remove all occurrences of productions that contain this non-terminal's symbol
            iter.remove();
        }
        Queue<Character> next = new LinkedList<>(); // the queue used for looping, filled with unvisited non-terminals
        Set<Character> visited = new HashSet<>(); // the set of non-terminal characters that can be reached from the start symbol
        next.add(startSymbol);
        while(next.size() > 0) { // determine which non-terminals can be reached from the start symbol
            NonTerminal n = nonTerminals.get(next.poll());
            for(Production p : n.getProductionList())
                for(char c : p.getExpression().toCharArray())
                    if(nonTerminals.containsKey(c) && !visited.contains(c) && !next.contains(c)) // add all new non-terminal symbols that haven't been visited yet to the queue
                        next.add(c);
            visited.add(n.getSymbol()); // add the current non-terminal's symbol to visited since it was reachable through this iteration
        }
        nonTerminals.keySet().removeIf(character -> !visited.contains(character)); // remove all non-terminals that couldn't be reached from the start symbol
    }

    /* Get the list of non-terminals that contain a production that contain the given symbol */
    public List<NonTerminal> getNonTerminalsContaining(char symbol) {
        List<NonTerminal> nTs = new ArrayList<>();
        for(NonTerminal nonTerminal : nonTerminals.values())
            for(Production production : nonTerminal.getProductionList())
                if(production.getExpression().contains(Character.toString(symbol)))
                    nTs.add(nonTerminal);
        return nTs;
    }

    /* Get the list of productions that contain the given symbol */
    public List<Production> getProductionsContaining(char symbol) {
        List<Production> productions = new ArrayList<>();
        for(NonTerminal nonTerminal : nonTerminals.values())
            for(Production production : nonTerminal.getProductionList())
                if(production.getExpression().contains(Character.toString(symbol)))
                    productions.add(production);
        return productions;
    }

    /* Get the list of productions that are unit productions (a singular non-terminal symbol) */
    public List<Production> getUnitProductions() {
        List<Production> unitProductions = new ArrayList<>();
        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList())
                if(p.getExpression().length() == 1 && !terminals.contains(p.getExpression().charAt(0)))
                    unitProductions.add(p);
        return unitProductions;
    }

    /* Get the set of non-terminal symbols that terminate */
    public Set<Character> getTerminatingNonTerminals() {
        Set<Character> terminatingNonTerminals = new HashSet<>();
        while(true) { // find all non-terminals that terminate directly and use those that have been found so far to find those that indirectly terminate until there are no new results
            int before = terminatingNonTerminals.size(); // the number of characters currently in the set before attempting to add more - used for terminating the loop if unchanged
            for (NonTerminal n : nonTerminals.values())
                for (Production p : n.getProductionList())
                    if (p.getExpression().contains(Character.toString(Main.LAMBDA)) || p.getExpression().matches("[a-z]*")) // non-terminals that directly terminate (contains lambda or a production of all terminals)
                        terminatingNonTerminals.add(n.getSymbol()); // add the production's non-terminal character to the current set
                    else {
                        boolean terminates = true;
                        for(char c : p.getExpression().toCharArray())
                            if (nonTerminals.containsKey(c) && !terminatingNonTerminals.contains(c)) { // at this point the non-terminal must contain at least one production that's an expression that fully terminates
                                terminates = false; // if not, the non-terminal doesn't terminate with the current characters in the terminating set
                                break;
                            }
                        if(terminates) // if the non-terminal does terminate we add it to the set of terminating characters and run the loop again with the new set
                            terminatingNonTerminals.add(n.getSymbol());
                    }
            if(before == terminatingNonTerminals.size()) // if there was no change there's no reason to run the loop anymore
                break;
        }
        return terminatingNonTerminals;
    }

    /* Find the first unused character from A - Z */
    public char getNextUnusedSymbol() {
        char c = 'A';
        while(nonTerminals.containsKey(c))
            c++;
        if(c > 'Z') // currently only supports 26 non-terminals
            throw new RuntimeException("No available non-terminal characters left.");
        return c;
    }

    @SuppressWarnings("unused")
    public char[] getSymbols() {
        char[] symbols = new char[terminals.size() + nonTerminals.size()];
        int count = 0;
        for(char c : terminals)
            symbols[count++] = c;
        for(char c : nonTerminals.keySet())
            symbols[count++] = c;
        return symbols;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for(NonTerminal nonTerminal : nonTerminals.values())
            result.append(nonTerminal.getSymbol()).append(" ").append(Main.ARROW).append(" ").append(nonTerminal.productionsToString()).append("\n");
        return result.toString();
    }

}
