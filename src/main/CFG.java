package main;

import java.util.*;

public class CFG {

    private char startSymbol;

    private List<Character> terminals;

    private HashMap<Character, NonTerminal> nonTerminals;

    public CFG() {
        startSymbol = 'S';
        terminals = new ArrayList<>();
        nonTerminals = new LinkedHashMap<>();
    }

    public void addTerminal(char symbol) {
        if(!terminals.contains(symbol))
            terminals.add(symbol);
    }

    public void replaceStart(char newStartSymbol) {
        if(nonTerminals.containsKey(newStartSymbol))
            throw new RuntimeException("CFG already contains the symbol: " + newStartSymbol + ".");
        HashMap<Character, NonTerminal> newMap = new LinkedHashMap<>();
        NonTerminal nonTerminal = new NonTerminal(newStartSymbol);
        newMap.put(newStartSymbol, nonTerminal);
        newMap.putAll(nonTerminals);
        nonTerminal.addProduction(Character.toString(startSymbol));
        startSymbol = newStartSymbol;
        nonTerminals = newMap;
    }

    public NonTerminal addNonTerminal(char symbol) {
        if(nonTerminals.containsKey(symbol))
            return nonTerminals.get(symbol);
        NonTerminal nonTerminal = new NonTerminal(symbol);
        nonTerminals.put(symbol, nonTerminal);
        return nonTerminal;
    }

    public NonTerminal addNonTerminalNewMap(char symbol) {
        NonTerminal nonTerminal = new NonTerminal(symbol);
        HashMap<Character, NonTerminal> newMap = new LinkedHashMap<>(nonTerminals);
        newMap.put(symbol, nonTerminal);
        nonTerminals = newMap;
        return nonTerminal;
    }

    // Based off of https://en.wikipedia.org/wiki/CYK_algorithm
    public boolean runCYK(String input) {
        List<NonTerminal> nTList = new ArrayList<>(nonTerminals.values());
        List<Production> nonTerminalProductions = new ArrayList<>();
        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList())
                if(p.getExpression().length() != 1)
                    nonTerminalProductions.add(p);
        boolean[][][] table = new boolean[input.length()][input.length()][nTList.size()];
        for(int s = 0; s < input.length(); s++) // substrings of length 1 (bottom row of table)
            for(NonTerminal n : nonTerminals.values())
                for(Production p : n.getProductionList())
                    if(p.getExpression().equals(Character.toString(input.charAt(s))))
                        table[0][s][nTList.indexOf(p.getNonTerminal())] = true;
        for(int l = 2; l <= input.length(); l++)
            for(int s = 0; s < input.length() - l + 1; s++)
                for(int p = 1; p <= l - 1; p++)
                    for (Production production : nonTerminalProductions) {
                        int b = nTList.indexOf(nonTerminals.get(production.getExpression().charAt(0)));
                        int c = nTList.indexOf(nonTerminals.get(production.getExpression().charAt(1)));
                        if (table[p - 1][s][b] && table[l - p - 1][s + p][c])
                            table[l - 1][s][nTList.indexOf(production.getNonTerminal())] = true;
                    }
        return table[input.length() - 1][0][0];
    }

    public void convertToCNF(char newStartSymbol) {
        boolean newStart = false;
        for (NonTerminal nonterminal : nonTerminals.values())
            for (Production production : nonterminal.getProductionList())
                if (production.getExpression().contains(Character.toString(startSymbol))) {
                    newStart = true;
                    break;
                }

        if (newStart)
            replaceStart(newStartSymbol);

        simplify();

        System.out.println("Simplified:\n" + toString());

        HashMap<String, Character> standAlones = new HashMap<>(); // used to avoid making duplicate standalone productions when separating
        for (NonTerminal n : nonTerminals.values())
            if(n.getProductionList().size() == 1 && n.getProductionList().get(0).satisfiesCNF())
                standAlones.put(n.getProductionList().get(0).getExpression(), n.getSymbol());

        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList()) {
                if(p.satisfiesCNF())
                    continue;
                for(int i = 0; i < p.getExpression().length(); i++) { // turn all terminals into non-terminals
                    char c = p.getExpression().charAt(i);
                    if(terminals.contains(c))
                        if(standAlones.containsKey(Character.toString(c)))
                            p.replaceChar(i, standAlones.get(Character.toString(c)));
                        else {
                            char newSymbol = getNextUnusedSymbol();
                            NonTerminal nonTerminal = addNonTerminalNewMap(newSymbol);
                            nonTerminal.addProduction(Character.toString(c));
                            p.replaceChar(i, newSymbol);
                            standAlones.put(Character.toString(c), newSymbol);
                    }
                }
                while(!p.satisfiesCNF()) { // only productions with a length of at least 3 that contain only non-terminals should remain at this point
                    char c1 = p.getExpression().charAt(0), c2 = p.getExpression().charAt(1), c3 = p.getExpression().charAt(2);
                    String s = standAlones.containsKey("" + c1 + c2) ? "" + c1 + c2 : standAlones.containsKey("" + c1 + c3) ? "" + c1 + c3 :
                            standAlones.containsKey("" + c2 + c3) ? "" + c2 + c3 : "" + c1 + c2; // check all combinations for pre-existing standalones
                    if(standAlones.containsKey(s))
                        p.replaceFirstPairWithChar(s, standAlones.get(s));
                    else {
                        char newSymbol = getNextUnusedSymbol();
                        NonTerminal nonTerminal = addNonTerminalNewMap(newSymbol);
                        nonTerminal.addProduction(s);
                        p.replaceFirstPairWithChar(s, newSymbol);
                        standAlones.put(s, newSymbol);
                    }
                }
            }

    }

    public void simplify() {
        /* Lambda removal */
        List<NonTerminal> nonTerminalsWithLambda = getNonTerminalsContaining(Main.LAMBDA);
        for(NonTerminal n : nonTerminalsWithLambda)
            for(Production production : n.getProductionList())
                if(production.getExpression().contains(Character.toString(Main.LAMBDA)))
                    n.removeProduction(production);
        while(true) { // find all nonterminal "paths" to lambda productions
            List<NonTerminal> newNullables = new ArrayList<>();
            for(NonTerminal nonTerminal : nonTerminalsWithLambda) {
                List<NonTerminal> oneLevelUp = getNonTerminalsContaining(nonTerminal.getSymbol());
                for(NonTerminal n : oneLevelUp)
                    if(!newNullables.contains(n) && !nonTerminalsWithLambda.contains(n) && n.getSymbol() != 'S') // stop at original start symbol
                        newNullables.add(n);
            }
            if(newNullables.size() == 0)
                break;
            nonTerminalsWithLambda.addAll(newNullables);
        }
        for(NonTerminal n : nonTerminalsWithLambda) { // do for all nonTerminals with lambda
            List<Production> relevantProductions = getProductionsContaining(n.getSymbol());
            for(Production production : relevantProductions) { // do for each production containing the non-terminal n
                if(production.getExpression().length() == 1) // ignore unit productions
                    continue;
                for(int i = 0; i < production.getExpression().length(); i++)
                    if(production.getExpression().charAt(i) == n.getSymbol())
                        production.getNonTerminal().addProduction(production.getExpression().substring(0, i) + production.getExpression().substring(i + 1)); // new production with the offending nullable character having been extracted
            }
        }

        /* Unit production removal */
        List<Production> unitProductions = getUnitProductions();
        while(unitProductions.size() > 0) {
            for(Production unitProduction : unitProductions) {
                NonTerminal unitProductionSymbol = nonTerminals.get(unitProduction.getExpression().charAt(0));
                for(Production production : unitProductionSymbol.getProductionList()) // add all productions from the unit production's non-terminal
                   unitProduction.getNonTerminal().addProduction(production.getExpression()); // addProduction method takes care of any duplicates
                unitProduction.getNonTerminal().removeProduction(unitProduction);
            }
            unitProductions = getUnitProductions(); // update list each time because unit productions might be moved around a few times before they're completely gone
        }

        /* Useless production removal */
        Set<Character> containsTerminal = getTerminatingNonTerminals();
        if(!containsTerminal.contains(startSymbol))
            throw new RuntimeException("This grammar does not terminate on start symbol: " + startSymbol + ".");
        Iterator<Map.Entry<Character, NonTerminal>> iter = nonTerminals.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Character, NonTerminal> entry = iter.next();
            if (containsTerminal.contains(entry.getValue().getSymbol()))
                continue;
            List<Production> toBeRemoved = getProductionsContaining(entry.getValue().getSymbol());
            toBeRemoved.forEach(p -> p.getNonTerminal().removeProduction(p)); // remove all occurrences of productions that contain this non-terminal's symbol
            iter.remove();
        }
        Queue<Character> next = new LinkedList<>();
        Set<Character> visited = new HashSet<>();
        next.add(startSymbol);
        while(next.size() > 0) { // determine which non-terminals can be reached from the start symbol
            NonTerminal n = nonTerminals.get(next.poll());
            for(Production p : n.getProductionList())
                for(char c : p.getExpression().toCharArray())
                    if(nonTerminals.containsKey(c) && !visited.contains(c) && !next.contains(c))
                        next.add(c);
            visited.add(n.getSymbol());
        }
        nonTerminals.keySet().removeIf(character -> !visited.contains(character)); // remove all non-terminals that couldn't be reached from the start symbol
    }

    public List<NonTerminal> getNonTerminalsContaining(char symbol) {
        List<NonTerminal> nTs = new ArrayList<>();
        for(NonTerminal nonTerminal : nonTerminals.values())
            for(Production production : nonTerminal.getProductionList())
                if(production.getExpression().contains(Character.toString(symbol)))
                    nTs.add(nonTerminal);
        return nTs;
    }

    public List<Production> getProductionsContaining(char symbol) {
        List<Production> productions = new ArrayList<>();
        for(NonTerminal nonTerminal : nonTerminals.values())
            for(Production production : nonTerminal.getProductionList())
                if(production.getExpression().contains(Character.toString(symbol)))
                    productions.add(production);
        return productions;
    }

    public List<Production> getUnitProductions() {
        List<Production> unitProductions = new ArrayList<>();
        for(NonTerminal n : nonTerminals.values())
            for(Production p : n.getProductionList())
                if(p.getExpression().length() == 1 && !terminals.contains(p.getExpression().charAt(0)))
                    unitProductions.add(p);
        return unitProductions;
    }

    public Set<Character> getTerminatingNonTerminals() {
        Set<Character> terminatingNonTerminals = new HashSet<>();
        while(true) { // find all non-terminals that terminate directly and use those that have been found so far to find those that indirectly terminate until there are no new results
            int before = terminatingNonTerminals.size();
            for (NonTerminal n : nonTerminals.values())
                for (Production p : n.getProductionList())
                    if (p.getExpression().contains(Character.toString(Main.LAMBDA)) || p.getExpression().matches("[a-z]*")) // non-terminals that directly terminate
                        terminatingNonTerminals.add(n.getSymbol());
                    else {
                        boolean terminates = true;
                        for(char c : p.getExpression().toCharArray())
                            if (nonTerminals.containsKey(c) && !terminatingNonTerminals.contains(c)) { // at this point the non-terminal must contain at least one production that's an expression that fully terminates
                                terminates = false;
                                break;
                            }
                        if(terminates)
                            terminatingNonTerminals.add(n.getSymbol());
                    }
            if(before == terminatingNonTerminals.size())
                break;
        }
        return terminatingNonTerminals;
    }

    public char getNextUnusedSymbol() {
        char c = 'A';
        while(nonTerminals.containsKey(c))
            c++;
        if(c > 'Z')
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
