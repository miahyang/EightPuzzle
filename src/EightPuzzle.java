import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Mia Yang
 * CSDS 391 Intro to AI
 * Eight Puzzle Game
 */
public class EightPuzzle {
    // 8 puzzle game: state representation (Q3.1)
    private int[][] board = new int[3][3];

    // storing current seed for Randomization of moves
    private long currSeed = 0;

    // goal state
    private final int[][] goal =
                {{0, 1, 2},
                 {3, 4, 5},
                 {6, 7, 8}};

    // optional maxNodes argument is set to default value
    private int maxNodes = 1000;

    // stores the number of puzzle states seen for the most recent solve
    private int numNodes = 0;

    // depth limit for solving DFS
    private int depthLimit = 31;

    /**
     * cmd() takes a command string as input, parses it, and calls the appropriate internal method with the specified
     * arguments. Output should be to the console.
     * @param commandString is the command to process
     */
    public void cmd(String commandString) {
        String command = commandString.trim();
        String[] args = command.split(" ", 2);

        // handling optional arguments for 'solve BFS'
        String solveCase1 = "solve BFS";
        if (command.startsWith(solveCase1) && command.length() > solveCase1.length()) {
            args = command.split(" ", 3);
            if (args.length == 3 && args[2].startsWith("maxnodes=") && args[2].length() > 9) {
                try {
                    this.maxNodes = Integer.parseInt(args[2].substring(9));
                } catch (NumberFormatException e) {
                    printErrorLine(command);
                    return;
                }
            } else {
                printErrorLine(command);
                return;
            }
        }

        // handling optional arguments for 'solve DFS'
        String solveCase2 = "solve DFS";
        if (command.startsWith(solveCase2) && command.length() > solveCase2.length()) {
            args = command.split(" ", 4);
            for (int i = 2; i < args.length; i++) {
                try {
                    if (args[i].startsWith("maxnodes=") && args[i].length() > 9) {
                        this.maxNodes = Integer.parseInt(args[i].substring(9));
                    } else if (args[i].startsWith("depthlimit=") && args[i].length() > 11) {
                        this.depthLimit = Integer.parseInt(args[i].substring(11));
                    } else {
                        printErrorLine(command);
                        return;
                    }
                } catch (NumberFormatException e) {
                    printErrorLine(command);
                    return;
                }
            }
        }

        // handling arguments for 'solve A*'
        String solveCase3 = "solve A*";
        if (command.startsWith(solveCase3) && command.length() >= (solveCase3.length() + 3)) {
            args = command.split(" ", 4);

            // heuristic function input can only be 'h1' or 'h2'
            if (!args[2].equals("h1") && !args[2].equals("h2")) {
                printErrorLine(command);
                return;
            }

            if (args.length == 4) {
                if (args[3].startsWith("maxnodes=") && args[3].length() > 9) {
                    try {
                        this.maxNodes = Integer.parseInt(args[3].substring(9));
                    } catch (NumberFormatException e) {
                        printErrorLine(command);
                        return;
                    }
                } else {
                    printErrorLine(command);
                    return;
                }
            }
        } else if (command.startsWith(solveCase3) && command.length() < (solveCase3.length() + 3)) {
            printErrorLine(command);
            return;
        }

        String commandType = args[0];
        switch(commandType) {
            case "setState":
                if (args.length != 2) {
                    printErrorLine(command);
                    return;
                }
                setState(args[1]);
                printState();
                break;
            case "printState":
                if (args.length != 1) {
                    printErrorLine(command);
                    return;
                }
                printState();
                break;
            case "move":
                if (args.length != 2) {
                    printErrorLine(command);
                    return;
                }
                this.board = move(args[1], this.board);
                printState();
                break;
            case "scrambleState":
                if (args.length != 2) {
                    printErrorLine(command);
                    return;
                }
                scrambleState(args[1]);
                printState();
                break;
            case "setSeed":
                if (args.length != 2) {
                    printErrorLine(command);
                    return;
                }
                setSeed(args[1]);
                break;
            case "solve":
                if (args.length == 1) {
                    printErrorLine(command);
                    return;
                }

                if (args[1].equals("BFS")) {
                    List<String> solution = solveBFS();
                    if (!solution.isEmpty()) printSolution(solution);
                    break;
                } else if (args[1].equals("DFS")) {
                    List<String> solution = solveDFS();
                    if (!solution.isEmpty()) printSolution(solution);
                    break;
                } else if (args[1].equals("A*")) {
                    List<String> solution = solveAStar(args[2]);
                    if (!solution.isEmpty()) printSolution(solution);
                    break;
                } else {
                    printErrorLine(command);
                    return;
                }
            default:
                printErrorLine(command);
        }
    }

    /**
     * cmdfile() takes a file name as an argument read lines of commands from a text file and prints the result.
     * @param filename is the test case file
     */
    public void cmdfile(String filename) {
        try (Scanner fileReader = new Scanner(new File(filename))) {
            while (fileReader.hasNextLine()) {
                String line = fileReader.nextLine();

                // all comments, commands, and empty lines are printed to console
                System.out.println(line);

                // calling cmd function to run the actual command
                if (!line.startsWith("//") && !line.isEmpty()) {
                    cmd(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found: " + e.getMessage());
        }
    }


    /**
     * setState() sets the current state of the puzzle based on the order of numbers in input argument
     * @param stateArg specifies the state we want puzzle to be
     */
    public void setState(String stateArg) {
        String state = stateArg.trim();
        String[] states = state.split(" ");

        Set<Integer> uniqNums = new HashSet<>();
        uniqNums.add(Integer.parseInt(states[0]));
        for (int i = 1; i < states.length; i++) {
            if (uniqNums.contains(Integer.parseInt(states[i]))) {
                System.out.println("Error: invalid puzzle state: setState " + stateArg);
                return;
            }
            uniqNums.add(Integer.parseInt(states[i]));
        }

        if (uniqNums.size() != 9) {
            System.out.println("Error: invalid puzzle state: setState " + stateArg);
            return;
        }

        // filling up grid
        for (int k = 0; k < states.length; k++) {
            int curr = Integer.parseInt(states[k]);
            if (curr < 0 || curr > 8) {
                System.out.println("Error: invalid puzzle state: setState " + stateArg);
                board = new int[3][3];
                return;
            }
            int i = k / 3;
            int j = k % 3;
            this.board[i][j] = Integer.parseInt(states[k]);
        }
    }

    /**
     * printState() prints the current puzzle state to the terminal as a 3x3 matrix using a space as the blank tile
     */
    public void printState() {
        for (int i = 0; i < board.length; i++) {
            System.out.print("|");
            for (int j = 0; j < board[i].length; j++) {
                if (j > 0) {
                    System.out.print(" ");
                }
                // Print space for 0 (empty tile)
                if (board[i][j] == 0) {
                    System.out.print(" ");
                } else {
                    System.out.print(board[i][j]);
                }
            }
            System.out.println("|");
        }
    }

    /**
     * move() moves the blank tile in the specified direction
     * @param direction is the specified input direction
     * @param boardState is the board's current state
     * @return the resulting state of the board after making the move
     */
    public int[][] move(String direction, int[][] boardState) {
        // Find blank tile position
        int[] blankTilePos = findBlankTile(boardState);
        if (blankTilePos == null) {
            System.out.println("Error: no blank tile found");
            return boardState;
        }

        // Checking if move is invalid
        if (!isValidMove(direction, blankTilePos[0], blankTilePos[1])) {
            System.out.println("Error: invalid move: move " + direction);
            return boardState;
        }

        this.moveTile(direction, blankTilePos[0], blankTilePos[1], boardState);
        return boardState;
    }

    /**
     * moveTile() is a helper method for move(). Given the specified direction, the position of the blank tile, and
     * a copy of the board's state, it moves the blank tile in the specified direction. This method is only called if
     * the specified move is valid.
     * @param direction is up, down, left, or right; specified as argument in parent function (move)
     * @param xPos is the ith position of the blank tile (row)
     * @param yPos is the jth position of the blank tile (column)
     * @param boardState is the current state of the board before the tile is moved
     */
    private void moveTile(String direction, int xPos, int yPos, int[][] boardState) {
        int temp = boardState[xPos][yPos];
        switch (direction) {
            case "up":
                boardState[xPos][yPos] = boardState[xPos - 1][yPos];
                boardState[xPos - 1][yPos] = temp;
                break;
            case "down":
                boardState[xPos][yPos] = boardState[xPos + 1][yPos];
                boardState[xPos + 1][yPos] = temp;
                break;
            case "left":
                boardState[xPos][yPos] = boardState[xPos][yPos - 1];
                boardState[xPos][yPos - 1] = temp;
                break;
            case "right":
                boardState[xPos][yPos] = boardState[xPos][yPos + 1];
                boardState[xPos][yPos + 1] = temp;
                break;
        }
    }

    /**
     * findBlankTile() is a helper method for move() that finds the row and column index position of the blank tile
     * given the state of the board. Returns null if no blank tile is found.
     * @param currState is a representation of the board's current state
     * @return int[] where int[0] is row index and int[1] is column index of blank tile
     */
    private int[] findBlankTile(int[][] currState) {
        for (int i = 0; i < currState.length; i++) {
            for (int j = 0; j < currState[i].length; j++) {
                if (currState[i][j] == 0) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    /**
     * isValidMove() is a helper method for move() that checks if the specified move is valid given the positions
     * of the blank tile
     * @param direction is the specified move
     * @param i is the blank tile's row position
     * @param j is the blank tile's column position
     * @return true if move is valid; false otherwise
     */
    private boolean isValidMove(String direction, int i, int j) {
        switch (direction.toLowerCase()) {
            case "up":    return i > 0;
            case "down":  return i < board.length - 1;
            case "left":  return j > 0;
            case "right": return j < board[0].length - 1;
            default:      return false;
        }
    }

    /**
     * getValidMoves() is a helper method for scrambleState() that retrieves a list of valid moves for the blank tile
     * @param blankPos is the position of the blank tile
     * @return a list of valid moves for the blank tile's given position
     */
    private ArrayList<String> getValidMoves(int[] blankPos) {
        ArrayList<String> validMoves = new ArrayList<>();
        int i = blankPos[0];
        int j = blankPos[1];

        if (j > 0) validMoves.add("left");
        if (j < board[0].length - 1) validMoves.add("right");
        if (i > 0) validMoves.add("up");
        if (i < board.length - 1) validMoves.add("down");

        return validMoves;
    }

    /**
     * scrambleState() scrambles the state of the puzzle by making n valid moves starting from the goal state
     * @param n is the number of random moves
     */
    public void scrambleState(String n) {
        try {
            int numMoves = Integer.parseInt(n);
            if (numMoves < 0) {
                System.out.println("Error: invalid argument: number of moves must be positive");
                return;
            }
            Random random = new Random(currSeed);

            // start board from goal state
            String goalState = new String("0 1 2 3 4 5 6 7 8");
            this.setState(goalState);

            // Make n random moves
            for (int i = 0; i < numMoves; i++) {
                // for each move, find the blank tile
                int[] blankPos = findBlankTile(this.board);

                // get valid moves for current blank tile's position
                ArrayList<String> validMoves = this.getValidMoves(blankPos);

                // choose random move from valid moves
                if (!validMoves.isEmpty()) {
                    int randomIndex = random.nextInt(validMoves.size());
                    String randomMove = validMoves.get(randomIndex);

                    this.board = move(randomMove, this.board);
                }
            }
        } catch (NumberFormatException e) {
            printErrorLine("scrambleState " + n);
        }
    }

    /**
     * setSeed() can be used to set the random number seed for reproducible results.
     * @param seed is the seed value to produce same random results for
     */
    public void setSeed(String seed) {
        try {
            this.currSeed = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            printErrorLine("setSeed seed");
        }
    }

    /**
     * printErrorLine() prints the text of line that caused the issue
     */
    public void printErrorLine(String line) {
        System.out.println("Error: invalid command: " + line);
    }

    /**
     * solveBFS() solves the 8-puzzle using Breadth-First Search
     * @return a list of moves that collectively represent the solution to solve the puzzle
     */
    public List<String> solveBFS() {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        HashMap<String, ArrayList<String>> parents = new HashMap<>();

        // goal state & copy of board's original state
        String goalState = Arrays.deepToString(goal);
        String origState = Arrays.deepToString(this.board);

        // put current state of board into queue
        queue.add(origState);
        while (!queue.isEmpty()) {
            String state = queue.poll();

            if (state.equals(goalState)) break;
            if (seen.contains(state)) continue;
            seen.add(state);

            if (seen.size() > this.maxNodes) {
                System.out.println("Error: maxnodes limit (" + this.maxNodes + ") reached");
                this.maxNodes = 1000; // set maxNodes back to default
                return new ArrayList<>();
            }

            int[][] stateArr = strToArr(state);
            int[] blankTile = findBlankTile(stateArr);
            ArrayList<String> validMoves = getValidMoves(blankTile);
            for (String move : validMoves) {
                int[][] stateCopy = copyState(stateArr);
                int[][] nextState = move(move, stateCopy);
                queue.add(Arrays.deepToString(nextState));

                if (!parents.containsKey(Arrays.deepToString(nextState))) {
                    ArrayList<String> value = new ArrayList<>();
                    value.add(state);
                    value.add(move);
                    parents.put(Arrays.deepToString(nextState), value);
                }
            }
        }

        this.numNodes = seen.size();
        return traceParents(parents, origState);
    }

    /**
     * solveDFS() solves the 8 puzzle using Depth-First Search
     * @return a list of moves that represent the solution
     */
    public List<String> solveDFS() {
        Stack<ArrayList<String>> stack = new Stack<>();           // frontier: [0]state, [1]parent, [2]depth, [3]move
        HashMap<String, Integer> visited = new HashMap<>();                        // keeping track of visited states
        HashMap<String, ArrayList<String>> parents = new HashMap<>(); // [0]: parent; value[1]: move made from parent

        String origState = Arrays.deepToString(this.board);
        String goalState = Arrays.deepToString(this.goal);

        ArrayList<String> initial = new ArrayList<>();
        initial.add(origState); // the current state
        initial.add(""); // parent state
        initial.add("0"); // depth of state
        initial.add(""); // the move it took to get from parent to this state

        stack.push(initial);
        while (!stack.isEmpty()) {
            var curr = stack.pop();
            String state = curr.get(0);
            String parent = curr.get(1);
            String depth = curr.get(2);
            String prevMove = curr.get(3);

            if (Integer.parseInt(depth) >= this.depthLimit) continue;

            if (visited.containsKey(state) && visited.get(state) <= Integer.parseInt(depth)) continue;
            visited.put(state, Integer.parseInt(depth));

            if (visited.size() > this.maxNodes) {
                System.out.println("Error: maxnodes limit (" + this.maxNodes + ") reached");
                this.maxNodes = 1000; // set maxNodes back to default
                return new ArrayList<>();
            }

            // updating parents
            ArrayList<String> value = new ArrayList<>();
            value.add(parent);
            value.add(prevMove);
            parents.put(state, value);

            if (state.equals(goalState)) break;

            // process its neighbors
            int[][] stateArr = strToArr(state);
            int[] blankTile = findBlankTile(stateArr);
            ArrayList<String> validMoves = getValidMoves(blankTile);

            for (String move : validMoves) {
                int[][] stateCopy = copyState(stateArr);
                int[][] nextState = move(move, stateCopy);

                ArrayList<String> child = new ArrayList<>();
                child.add(Arrays.deepToString(nextState));
                child.add(state);
                child.add(String.valueOf(Integer.parseInt(depth) + 1));
                child.add(move);

                stack.push(child);
            }
        }

        this.numNodes = visited.size();
        if (this.depthLimit != 31) this.depthLimit = 31;

        return traceParents(parents, origState);
    }

    /**
     * solveAStar() solves the 8 puzzle using A* search
     * @param h is the heuristic function
     * @return the solution represented as a list of moves
     */
    public List<String> solveAStar(String h) {
        // [0]: state; [1]: f(n) = g(n) + h(n) --> is num moves to reach state; [2]: g(n) --> num moves to reach state
        PriorityQueue<String[]> frontier = new PriorityQueue<>((a, b) -> {
            int af = Integer.parseInt(a[1]);
            int bf = Integer.parseInt(b[1]);
            return af - bf;
        });
        HashMap<String, Integer> seen = new HashMap<>(); // [0]: visited puzzle state; [1]: cost of path to that state
        HashMap<String, ArrayList<String>> parents = new HashMap<>();

        String origState = Arrays.deepToString(this.board);
        String goalState = Arrays.deepToString(this.goal);

        String[] initial = new String[]{origState, String.valueOf(heuristic(h, copyState(this.board))), "0"};
        frontier.add(initial);
        while (!frontier.isEmpty()) {
            String[] curr = frontier.poll();
            String state = curr[0];
            int fValue = Integer.parseInt(curr[1]);
            int pathCost = Integer.parseInt(curr[2]);

            if (state.equals(goalState)) break;
            if (seen.containsKey(state) && seen.get(state) <= fValue) continue;
            seen.put(state, fValue);
            if (seen.size() > this.maxNodes) {
                System.out.println("Error: maxnodes limit (" + this.maxNodes + ") reached");
                this.maxNodes = 1000; // set maxNodes back to default
                return new ArrayList<>();
            }

            // process next states
            int[][] stateArr = strToArr(state);
            int[] blankTile = findBlankTile(stateArr);
            ArrayList<String> validMoves = getValidMoves(blankTile);
            for (String move : validMoves) {
                int[][] stateCopy = copyState(stateArr);
                int[][] nextState = move(move, stateCopy);

                // update parents
                if (!parents.containsKey(Arrays.deepToString(nextState))) {
                    ArrayList<String> childInfo = new ArrayList<>();
                    childInfo.add(state);
                    childInfo.add(move);
                    parents.put(Arrays.deepToString(nextState), childInfo);
                }

                // add next state to frontier
                int nextFVal = heuristic(h, nextState) + pathCost + 1;
                String[] next = new String[]{Arrays.deepToString(nextState), String.valueOf(nextFVal),
                        String.valueOf(pathCost + 1)};
                frontier.add(next);
            }
        }

        this.numNodes = seen.size();
        return traceParents(parents, origState);
    }

    /**
     * heuristic() applies and calculates the correct heuristic function value given string indicator
     * @param whichH indicates which heuristic function to use
     * @return the value corresponding to specified heuristic
     */
    private int heuristic(String whichH, int[][] state) {
        if (whichH.equals("h1")) return numMisplacedTiles(state);
        if (whichH.equals("h2")) return totalManHattanDist(state);

        return 0;
    }

    /**
     * Calculates the heuristic function value for h1 which is the number of misplaced tiles
     * @param state is the given state of the puzzle
     * @return number of misplaces tiles for provided state
     */
    private int numMisplacedTiles(int[][] state) {
        int counter = 0;
        for (int i = 0; i < state.length; i++) {
            for (int j = 0; j < state[i].length; j++) {
                if (state[i][j] != (3 * i + j)) counter++;
            }
        }
        return counter;
    }

    /**
     * Calculates the heuristic function value for h2 which is the sum of distances of the tiles from their
     * goal positions
     * @param state is the given state of the puzzle
     * @return the sum of manhattan distances for each tile to their goal position
     */
    private int totalManHattanDist(int[][] state) {
        int totalSum = 0;
        for (int i = 0; i < state.length; i++) {
            for (int j = 0; j < state[i].length; j++) {
                int val = state[i][j];
                int manDist = Math.abs(val / 3 - i) + Math.abs(val % 3 - j);
                totalSum += manDist;
            }
        }
        return totalSum;
    }

    /**
     * traceParents() is a helper method that traces through parent states and returns list of moves that collectively
     * represent the solution.
     * @param parents is a hashmap of the parent states for each state
     * @return the solution represented as a list of moves
     */
    private List<String> traceParents(HashMap<String, ArrayList<String>> parents, String origState) {
        List<String> solution = new ArrayList<>();
        String state = Arrays.deepToString(goal);

        while (!state.equals(origState)) {
            ArrayList<String> value = parents.get(state);
            solution.add(0, value.get(1));
            state = value.get(0);
        }

        return solution;
    }

    /**
     * copyState() creates a copy of the inputted state
     * @param state is the board state
     * @return a copy of it
     */
    private int[][] copyState(int[][] state) {
        int[][] copy = new int[3][3];
        for (int i = 0; i < state.length; i++) {
            for (int j = 0; j < state[i].length; j++) {
                copy[i][j] = state[i][j];
            }
        }

        return copy;
    }

    /**
     * strToArr() converts a string representation of board's state to an int[][]
     * @param state is the string representation of the board
     * @return the int[][] of the board state
     */
    private int[][] strToArr(String state) {
        // Remove outer brackets and split into rows
        state = state.substring(1, state.length() - 1); // Remove outer brackets
        String[] rows = state.split("],\\s*\\["); // Split rows, handling spaces

        // Parse rows into a 2D array
        int[][] arr = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            // Remove any remaining brackets and split by commas
            String row = rows[i].replaceAll("[\\[\\]]", "");
            String[] elements = row.split(",\\s*");

            // Convert string elements to integers
            arr[i] = new int[elements.length];
            for (int j = 0; j < elements.length; j++) {
                arr[i][j] = Integer.parseInt(elements[j]);
            }
        }

        return arr;
    }

    /**
     * printSolution() prints the list of moves that collectively make up a solution to the console
     * @param solution is an array that contains information about the solution to solve the puzzle
     */
    public void printSolution(List<String> solution) {
        System.out.println("Nodes created during search: " + this.numNodes);
        resetNumMaxNodes();

        System.out.println("Solution length: " + solution.size());
        System.out.println("Move sequence:");
        for (String step : solution) {
            System.out.println("move " + step);
        }
    }

    /**
     * restNumNodes() is a helper method for printSolution that resets numNodes to default of 0 and maxNodes to its
     * default of 1000
     */
    private void resetNumMaxNodes() {
        this.numNodes = 0;
        this.maxNodes = 1000;
    }

    /**
     * main() to run the commands implemented via command line
     * @param args
     */
    public static void main(String[] args) {
        EightPuzzle myBoard = new EightPuzzle();

        // If reading from text file (cmdfile)
        if (args.length == 1 && args[0].contains(".txt")) {
            String fullFileName = "../" + args[0];
            myBoard.cmdfile(fullFileName);
            return;
        }

        // If using interactive text command prompt
        Scanner terminal = new Scanner(System.in);

        // command line will continue to take commands until 'stop'
        while (true) {
            System.out.print("Enter command: ");
            String input = terminal.nextLine();
            if (input.length() == 0) {
                System.out.println("Error: Please provide a command");
            }
            // String command = args[0];
            if (input.equalsIgnoreCase("stop")) break;

            if (!input.isEmpty()) {
                myBoard.cmd(input);
            }
        }

        terminal.close();
    }
}
