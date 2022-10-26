import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Maksim Oinoshev
 */
public class MaksimOinoshev {
    static final char[][] krakenAreaEffect = {
            { '-', '#', '-' },
            { '#', '#', '#' },
            { '-', '#', '-' },
    };

    static final char[][] davyAreaEffect = {
            { '#', '#', '#' },
            { '#', '#', '#' },
            { '#', '#', '#' },
    };

    public static void main(String[] args) {
        ArrayList<int[]> coordinates;
        var scanner = new Scanner(System.in);
        coordinates = parseCoordinates(scanner.nextLine());
        if (!isValid(coordinates))
            InvalidInput();

        var gamemode = parseGameMode(scanner.nextLine());
        scanner.close();

        var jack = new Player(coordinates.get(0), "jack");
        var davy = new Enemy(coordinates.get(1), "davy", davyAreaEffect);
        var kraken = new Enemy(coordinates.get(2), "kraken", krakenAreaEffect, false);
        var stone = new Enemy(coordinates.get(3), "stone");
        var treasure = new Goal(coordinates.get(4), "c");
        var tartuga = new Support(coordinates.get(5), "tartuga");

        var map = new TreasureMap(gamemode, treasure, jack, tartuga, new Enemy[] { kraken, davy, stone });

        // AStar
        System.out.println("ASTAR");
        map.aStar();

        if (map.getWinCost() != -1)
            System.out.println("WIN:" + map.getWinCost());
        else
            System.out.println("LOSE");

        map.printMap();
        writeMapRes(map, "outputAstar.txt");

        // Clear values
        jack = new Player(coordinates.get(0), "jack");
        davy = new Enemy(coordinates.get(1), "davy", davyAreaEffect);
        kraken = new Enemy(coordinates.get(2), "kraken", krakenAreaEffect, false);
        stone = new Enemy(coordinates.get(3), "stone");
        treasure = new Goal(coordinates.get(4), "c");
        tartuga = new Support(coordinates.get(5), "tartuga");

        map = new TreasureMap(gamemode, treasure, jack, tartuga, new Enemy[] { kraken, davy, stone });

        // Backtracking
        System.out.println("Backtracking");
        map.backTracing();

        if (map.getWinCost() != -1)
            System.out.println("WIN:" + map.getWinCost());
        else
            System.out.println("LOSE");

        map.printMap();
        writeMapRes(map, "outputBacktracking.txt");

    }

    public static void staticalAnalysis(int count) {
        int gamemode;
        var tests = generateTest(count);
        for (int opt = 0; opt < 4; opt++) {
            var wins = new ArrayList<Integer>();
            var times = new ArrayList<Long>();
            int winCount = 0;
            int loseCount = 0;

            for (int i = 0; i < count; i++) {
                var coordinates = tests.get(i);
                var jack = new Player(coordinates.get(0), "jack");
                var davy = new Enemy(coordinates.get(1), "davy", davyAreaEffect);
                var kraken = new Enemy(coordinates.get(2), "kraken", krakenAreaEffect, false);
                var stone = new Enemy(coordinates.get(3), "stone");
                var treasure = new Goal(coordinates.get(4), "chest");
                var tartuga = new Support(coordinates.get(5), "tartuga");

                if (opt % 2 == 0)
                    gamemode = 1;
                else
                    gamemode = 2;

                var map = new TreasureMap(gamemode, treasure, jack, tartuga, new Enemy[] { kraken, davy, stone });

                // one time exection time
                long start = System.currentTimeMillis();

                if (opt < 2)
                    map.aStar();
                else
                    map.backTracing();

                long end = System.currentTimeMillis();
                long time = end - start;
                times.add(time);

                if (map.getWinCost() != -1) {
                    winCount += 1;
                    wins.add(map.getWinCost());
                } else
                    loseCount += 1;
            }

            long winsSum = wins.stream()
                    .mapToLong(Integer::longValue)
                    .sum();

            double mean = winsSum / (double) winCount;

            Collections.sort(wins);
            int meadian = wins.get(wins.size() / 2);

            Map<Integer, Integer> counter = new HashMap<>();
            for (int win : wins) {
                if (counter.get(win) == null)
                    counter.put(win, 1);
                else
                    counter.put(win, counter.get(win) + 1);
            }
            Optional<Entry<Integer, Integer>> maxEntry = counter.entrySet().stream()
                    .max(Comparator.comparing(Map.Entry::getValue));
            int mode = maxEntry.get().getKey();

            long timesSum = 0;
            for (Long time : times) {
                timesSum += time;
            }

            double timesMean = timesSum / (double) count;

            long diationMul = 0;
            for (Long time : times) {
                diationMul += Math.pow(time - timesMean, 2);
            }
            double deviation = Math.sqrt(diationMul / (double) (count - 1));
            int winPer = (int) ((double) count / (double) winCount * 100);
            int losePer = (int) ((double) count / (double) loseCount * 100);

            if (opt == 0)
                System.out.println("A* variant 1");

            if (opt == 1)
                System.out.println("A* variant 2");

            if (opt == 2)
                System.out.println("Backtracking variant 1");

            if (opt == 3)
                System.out.println("Backtracking variant 2");

            System.out.println("mean: " + mean);
            System.out.println("median: " + meadian);
            System.out.println("mode: " + mode);
            System.out.println("standart deviation: " + deviation);
            System.out.println("wins count: " + winCount);
            System.out.println("loses count: " + loseCount);
            System.out.println("wins percent: " + winPer);
            System.out.println("loses percent: " + losePer);
            System.out.println("\n\n");
        }
    }

    /**
     * Write game result, map and path to output file
     */
    public static void writeMapRes(TreasureMap map, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);

            if (map.getWinCost() == -1) {
                writer.write("Lose");
                writer.close();
                return;
            }

            writer.write("Win\n");
            var res = map.getWinPath();
            Collections.reverse(res);
            for (int[] coor : res) {
                writer.append(String.format("[%d,%d] ", coor[0], coor[1]));
            }
            writer.append("\n");

            for (int i = -1; i < map.getHeight(); i++) {
                for (int j = -1; j < map.getWidth(); j++) {
                    if (i == -1 && j == -1)
                        writer.append(" ");
                    else if (i == -1 && j != -1)
                        writer.append(String.valueOf(j));
                    else if (i != -1 && j == -1)
                        writer.append(String.valueOf(i));
                    else if (in(i, j, res))
                        writer.append("*");
                    else
                        writer.append("-");
                    writer.append(" ");
                }
                writer.append("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the given (y, x) cooresponds to any given coordinates
     */
    public static boolean in(int y, int x, ArrayList<int[]> coordinates) {
        for (int[] cor : coordinates)
            if (cor[1] == x && cor[0] == y)
                return true;
        return false;
    }

    /**
     * Check if coodinates is valid
     */
    public static boolean isValid(ArrayList<int[]> coords) {
        var jackCoordinates = coords.get(0);
        var davyCoordinates = coords.get(1);
        var krakenCoordinates = coords.get(2);
        var stoneCoordinates = coords.get(3);
        var treasureCoordinates = coords.get(4);
        var tortuga = coords.get(5);

        var forbiddenCoords = new ArrayList<>(
                Arrays.asList(stoneCoordinates, jackCoordinates, krakenCoordinates, treasureCoordinates, tortuga));
        if (hasCollision(davyCoordinates, forbiddenCoords))
            return false;

        forbiddenCoords = new ArrayList<>(
                Arrays.asList(jackCoordinates, davyCoordinates, treasureCoordinates, tortuga));
        if (hasCollision(krakenCoordinates, forbiddenCoords))
            return false;

        forbiddenCoords = new ArrayList<>(
                Arrays.asList(davyCoordinates, jackCoordinates, treasureCoordinates, tortuga));
        if (hasCollision(stoneCoordinates, forbiddenCoords))
            return false;

        forbiddenCoords = new ArrayList<>(Arrays.asList(jackCoordinates));
        forbiddenCoords.addAll(krakenArea(krakenCoordinates[0], krakenCoordinates[1]));
        forbiddenCoords.addAll(krakenArea(davyCoordinates[0], davyCoordinates[1]));
        if (hasCollision(treasureCoordinates, forbiddenCoords))
            return false;

        forbiddenCoords = new ArrayList<>(Arrays.asList(treasureCoordinates));
        forbiddenCoords.addAll(krakenArea(krakenCoordinates[0], krakenCoordinates[1]));
        forbiddenCoords.addAll(krakenArea(davyCoordinates[0], davyCoordinates[1]));
        if (hasCollision(tortuga, forbiddenCoords))
            return false;

        return true;
    }

    /**
     * Generates given count of coordinates that pass isValid
     */
    public static ArrayList<ArrayList<int[]>> generateTest(int count) {
        var coordinatesList = new ArrayList<ArrayList<int[]>>();
        for (int i = 0; i < count; i++) {
            var jackCoordinates = generateCoordinates();
            var davyCoordinates = generateCoordinates();
            var krakenCoordinates = generateCoordinates();
            var stoneCoordinates = generateCoordinates();
            var treasureCoordinates = generateCoordinates();
            var tortuga = generateCoordinates();

            var temp = new ArrayList<>(Arrays.asList(
                    davyCoordinates,
                    stoneCoordinates,
                    jackCoordinates,
                    krakenCoordinates,
                    treasureCoordinates,
                    tortuga));
            while (!isValid(temp)) {
                jackCoordinates = generateCoordinates();
                davyCoordinates = generateCoordinates();
                krakenCoordinates = generateCoordinates();
                stoneCoordinates = generateCoordinates();
                treasureCoordinates = generateCoordinates();
                tortuga = generateCoordinates();

                temp = new ArrayList<>(Arrays.asList(
                        davyCoordinates,
                        stoneCoordinates,
                        jackCoordinates,
                        krakenCoordinates,
                        treasureCoordinates,
                        tortuga));
            }
            coordinatesList.add(temp);
        }

        return coordinatesList;
    }

    /**
     * Returns coordinates of effect area of Davy Jones around the given (y, x)
     * 
     * @param y coordinate
     * @param x coordinate
     */
    public static ArrayList<int[]> daveArea(int y, int x) {
        var area = new ArrayList<int[]>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; i < 2; i++) {
                area.add(new int[] { y + i, x + j });
            }
        }
        return area;
    }

    /**
     * Returns coordinates of effect area of kraken around the given (y, x)
     * 
     * @param y coordinate
     * @param x coordinate
     */
    public static ArrayList<int[]> krakenArea(int y, int x) {
        var area = new ArrayList<int[]>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(i) == 1)
                    continue;

                area.add(new int[] { y + i, x + j });
            }
        }
        return area;
    }

    /**
     * Checks if the given coodinate in the given other coodinates
     * 
     * @param c
     */
    public static boolean hasCollision(int[] coordinate, ArrayList<int[]> coordinates) {
        for (int[] anotherCoor : coordinates) {
            if (coordinate[0] == anotherCoor[0] && coordinate[1] == anotherCoor[1])
                return true;
        }
        return false;
    }

    public static int[] generateCoordinates() {
        var randomizer = new Random();
        var x = randomizer.nextInt(9);
        var y = randomizer.nextInt(9);

        return new int[] { x, y };
    }

    /**
     * Call InvalidInput if input does not equal to 1 or 2
     * 
     * @return game mode
     */
    public static int parseGameMode(String input) {
        var mode = -1;
        try {
            mode = Integer.parseInt(input);
            if (mode == 1 || mode == 2)
                return mode;
        } catch (Exception e) {
            InvalidInput();
        }

        InvalidInput();
        return 0;
    }

    /**
     * Translate the input string to the list of arrays format, where
     * each array contain pair of (y, x) coordinates
     */
    public static ArrayList<int[]> parseCoordinates(String input) {
        Pattern pattern = Pattern.compile("(\\[[0-8],[0-8]\\]{1} ){5}\\[[0-8],[0-8]\\]{1}$");
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find())
            InvalidInput();

        // divide line to chunks
        var strCoordinates = input.split(" ");
        var coordinates = new ArrayList<int[]>();

        for (String strCoordinate : strCoordinates) {
            // remove square braces and get int array by comma
            var coordinate = Arrays.stream(strCoordinate.replaceAll("[\\[\\]]", "")
                    .split(",")).mapToInt(Integer::parseInt)
                    .toArray();
            coordinates.add(coordinate);
        }
        return coordinates;
    }

    /**
     * Finish the program with invalid input message
     */
    public static void InvalidInput() {
        System.out.println("Invalid input");
        System.exit(0);
    }
}

class TreasureMap {
    private final int emptyCellCost = 1;
    private final int enemyCellCost = -1;
    private final int width = 9;
    private final int height = 9;
    private final int gamemode;
    private boolean envRecalced = false;
    private ArrayList<int[]> win = new ArrayList<>();
    private ArrayList<Cell> notCheck = new ArrayList<>();
    private int winCost = -1;

    private final Goal goal;
    private final Player player;
    private final Support support;
    private final Enemy[] enemies;

    private final Cell[][] body = new Cell[width][height];

    public TreasureMap(int gamemode, Goal goal, Player player, Support support, Enemy[] enemies) {
        this.goal = goal;
        this.player = player;
        this.support = support;
        this.enemies = enemies;
        this.gamemode = gamemode;

        placeEnemiesOnMap(enemies);
        fillMapWithEmptyCells();
    }

    public void backTracing() {
        Cell playerCell = getEntityCell(player);
        Cell supportCell = getEntityCell(support);
        Cell goalCell = getEntityCell(goal);
        Cell cell = null;

        if (inEffectArea(player.getY(), player.getX()))
            return;

        cell = getShortestBackTracking(playerCell, goalCell, goal);

        if (cell != null) {
            markWinPath(cell, player);
            winCost = cell.cost - 1;
        } else {
            var toSupport = getShortestBackTracking(playerCell, supportCell, support);
            if (toSupport != null) {
                player.hasSupport = true;
                var toGoal = getShortestBackTracking(supportCell, goalCell, goal);
                if (toGoal != null) {
                    markWinPath(toSupport, player);
                    markWinPath(toGoal, support);
                    winCost = toGoal.cost + toSupport.cost - 2;
                }
            }
        }
    }

    public void aStar() {
        Cell playerCell = getEntityCell(player);
        Cell supportCell = getEntityCell(support);
        Cell goalCell = getEntityCell(goal);
        Cell cell = null;

        if (inEffectArea(player.getY(), player.getX()))
            return;

        cell = getShortestaStar(playerCell, goalCell, goal);

        if (cell != null) {
            markWinPath(cell, player);
            winCost = cell.cost - 1;
        } else {
            var toSupport = getShortestaStar(playerCell, supportCell, support);
            if (toSupport != null) {
                player.hasSupport = true;
                var toGoal = getShortestaStar(supportCell, goalCell, goal);
                if (toGoal != null) {
                    markWinPath(toSupport, player);
                    markWinPath(toGoal, support);
                    winCost = toGoal.cost + toSupport.cost - 2;
                }
            }
        }
    }

    private Cell getShortestBackTracking(Cell from, Cell to, Entity of) {
        var cellsQueue = new ArrayList<Cell>();
        var proceededCells = new ArrayList<Cell>();
        var cellsSeekedGoal = new ArrayList<Cell>();
        cellsQueue.add(from);
        while (!cellsQueue.isEmpty()) {
            var cell = cellsQueue.remove(0);
            if (cellContains(cell, of))
                cellsSeekedGoal.add(cell);
            var neighborCells = getNeighborCells(cell);
            removeProceeded(neighborCells, proceededCells);
            recalculateTotalCost(cell, neighborCells);
            proceededCells.add(cell);
            cellsQueue.addAll(neighborCells);
        }

        Cell winCell = null;
        int minVal = -1;
        for (Cell cell : cellsSeekedGoal) {
            var val = pathLen(cell, from);
            if (minVal == -1 || val < minVal) {
                winCell = cell;
                minVal = val;
            }
        }
        return winCell;
    }

    private int pathLen(Cell goalCell, Cell initCell) {
        var cell = goalCell.copy();
        var counter = 0;
        body[initCell.y][initCell.x].isWinPath = true;
        while (cell.id != initCell.id) {
            cell = cell.parent.copy();
            counter++;
        }
        return counter;
    }

    private Cell getShortestaStar(Cell from, Cell to, Entity goal) {
        var cheapestCellFirst = new ArrayList<Cell>();
        var proceededCells = new ArrayList<Cell>();

        cheapestCellFirst.add(from);

        while (!cheapestCellFirst.isEmpty()) {
            var cell = cheapestCellFirst.remove(0);

            if (cellContains(cell, goal)) {
                return cell;
            }

            var neighborCells = getNeighborCells(cell);
            if (neighborCells.isEmpty()) {
                continue;
            }

            deduplicate(neighborCells, cheapestCellFirst, proceededCells);
            recalculateTotalCost(cell, neighborCells);
            proceededCells.add(cell);
            cheapestCellFirst.addAll(neighborCells);
            cheapestCellFirst.sort(Comparator.comparing(Cell::getTotalConst));
        }

        return null;
    }

    /**
     * Removes proceeded element from the neighborCells and cheapestCells lists
     * 
     * @param neighborCells - array list of neighbors
     *                      // * @param proceededCells - array list of proceded
     *                      cells from cheapestCells
     */
    private void deduplicate(ArrayList<Cell> neighborCells, ArrayList<Cell> cheapestCells,
            ArrayList<Cell> procededCells) {
        var cheapestCellsToRemove = new ArrayList<Cell>();
        var neighborCellsToRemove = new ArrayList<Cell>();

        for (Cell neighbor : neighborCells) {
            // remove collision of cheapest and neighbor cells
            for (Cell cheap : cheapestCells)
                if (cheap.id == neighbor.id)
                    if (cheap.totalCost < calculateCost(neighbor)[2])
                        neighborCellsToRemove.add(neighbor);
                    else
                        cheapestCellsToRemove.add(cheap);

            // remove collision of proceded and neighbor cells
            for (Cell proceded : procededCells)
                if (proceded.id == neighbor.id && proceded.totalCost < calculateCost(neighbor)[2])
                    neighborCellsToRemove.add(neighbor);
        }

        for (Cell cellToRemove : neighborCellsToRemove)
            neighborCells.remove(cellToRemove);

        for (Cell cellToRemove : cheapestCellsToRemove)
            cheapestCells.remove(cellToRemove);
    }

    private void removeProceeded(ArrayList<Cell> neighborCells, ArrayList<Cell> procededCells) {
        var neighborCellsToRemove = new ArrayList<Cell>();

        for (Cell neighbor : neighborCells) {
            // remove collision of proceded and neighbor cells
            for (Cell proceded : procededCells)
                if (proceded.id == neighbor.id && proceded.totalCost < calculateCost(neighbor)[2])
                    neighborCellsToRemove.add(neighbor);
        }

        for (Cell cellToRemove : neighborCellsToRemove)
            neighborCells.remove(cellToRemove);
    }

    /**
     * Sets isWiningPath to true for the shortest path
     */
    private void markWinPath(Cell goalCell, Entity init) {
        var cell = goalCell;
        var initCell = getEntityCell(init);
        body[initCell.y][initCell.x].isWinPath = true;
        while (cell.id != initCell.id) {
            body[cell.y][cell.x].isWinPath = true;
            win.add(new int[] { cell.y, cell.x });
            cell = cell.parent;
        }
        win.add(new int[] { initCell.y, initCell.x });
    }

    /**
     * Display map to stdout
     */
    public void printMap() {
        var entities = new ArrayList<Entity>(Arrays.asList(player, goal, support));
        entities.addAll(Arrays.asList(enemies));

        System.out.println(" 012345678");
        for (int i = 0; i < height; i++) {
            System.out.print(i);

            for (int j = 0; j < width; j++) {
                var cell = body[i][j];

                var isEntity = false;
                for (Entity entity : entities) {
                    if (cell == getEntityCell(entity)) {
                        System.out.print(entity.getName().charAt(0));
                        isEntity = true;
                        break;
                    }
                }
                if (isEntity)
                    continue;

                if (cell.cost == enemyCellCost) {
                    System.out.print(Colors.RED + "#" + Colors.STOP);
                    continue;
                }

                if (cell.belongsTo != null && cell.belongsTo.equals("kraken")) {
                    System.out.print(Colors.RED + "#" + Colors.STOP);
                    continue;
                }

                if (cell.isWinPath) {
                    System.out.print(Colors.GREEN + "*" + Colors.STOP);
                    continue;
                }
                System.out.print("-");
            }
            System.out.print("\n");
        }
    }

    /**
     * Returns neighbors of the cell. A neighbor is cell which can be accepted
     * from the given cell by one move
     * 
     * @param cell
     * @return neighbors of the cell
     */
    private ArrayList<Cell> getNeighborCells(Cell cell) {
        int x = cell.x;
        int y = cell.y;

        var neighbors = new ArrayList<Cell>();

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i == 0 && j == 0)
                    continue;

                int yMap = y + i;
                int xMap = x + j;

                if (!inMap(yMap, xMap))
                    continue;

                if (getMortalEnemyIn(yMap, xMap) != null && player.hasSupport && !envRecalced) {
                    envRecalced = true;
                    removeEnemyCells(getMortalEnemyIn(yMap, xMap));
                    neighbors = getNeighborCells(cell);
                    break;
                }

                if (notCheck.size() != 0) {
                    for (Cell not : notCheck) {
                        if (body[yMap][xMap].id == not.id)
                            continue;
                    }
                }

                if (!inEffectArea(yMap, xMap)) {
                    var neighbor = body[yMap][xMap].copy();
                    neighbor.parent = cell;
                    neighbors.add(neighbor);
                }

                if (gamemode == 2) {
                    int y1 = y + 2;
                    int y2 = y - 2;
                    int x1 = x + 2;
                    int x2 = x - 2;
                    if (inMap(y1, x) && inEffectArea(y1, x))
                        notCheck.add(body[y1][x]);

                    if (inMap(y2, x) && inEffectArea(y2, x))
                        notCheck.add(body[y2][x]);

                    if (inMap(y, x1) && inEffectArea(y, x1))
                        notCheck.add(body[y][x1]);

                    if (inMap(y, x2) && inEffectArea(y, x2))
                        notCheck.add(body[y][x2]);
                }
            }
        }
        return neighbors;
    }

    public int getWinCost() {
        return winCost;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private Enemy getMortalEnemyIn(int y, int x) {
        for (Enemy enemy : enemies) {
            if (!enemy.isImmortal() && enemy.getX() == x && enemy.getY() == y)
                return enemy;
        }
        return null;
    }

    /**
     * Returns cell that contains the given entity
     */
    private Cell getEntityCell(Entity entity) {
        return body[entity.getY()][entity.getX()];
    }

    private void removeEnemyCells(Enemy enemy) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (body[i][j].belongsTo != null && body[i][j].belongsTo.equals(enemy.getName())) {
                    body[i][j].cost = emptyCellCost;
                }
            }
        }
    }

    /**
     * Assign new costs for the neighbor if go from the cell to the neighbor
     * 
     * @param cell      - cell to go from
     * @param neighbors - list of neighbors of the cell
     */
    private void recalculateTotalCost(Cell cell, ArrayList<Cell> neighbors) {
        for (Cell neighbor : neighbors) {
            int[] costs = calculateCost(neighbor);
            int newCost = costs[0];
            int estimatedCost = costs[1];
            int newtotalCost = costs[2];

            neighbor.cost = newCost;
            neighbor.estimatedCost = estimatedCost;
            neighbor.totalCost = newtotalCost;
        }
    }

    /**
     * Calculates new costs for the cell
     * 
     * @return the array of new self cost, manhatan cost and total cost
     */
    private int[] calculateCost(Cell cell) {
        int cathetus_x = Math.abs(goal.getX() - cell.x);
        int cathetus_y = Math.abs(goal.getY() - cell.y);
        int estimatedCost = Math.max(cathetus_x, cathetus_y);

        int newCost = cell.cost;
        if (cell.parent != null)
            newCost += cell.parent.cost;

        int newTotalCost = newCost + estimatedCost;

        return new int[] { newCost, estimatedCost, newTotalCost };
    }

    /**
     * Check if (x, y) in the map
     */
    private boolean inMap(int y, int x) {
        return x < width && x >= 0 && y < width && y >= 0;
    }

    /**
     * Check if (x, y) in the enemy effect area
     */
    private boolean inEffectArea(int y, int x) {
        return body[y][x].cost == -1;
    }

    private boolean inEffectAreaOf(Enemy enemy, int y, int x) {

        return body[y][x].cost == -1;
    }

    /**
     * Check if the given cell contains the given entity
     */
    private boolean cellContains(Cell cell, Entity entity) {
        return cell.x == entity.getX() && cell.y == entity.getY();
    }

    /**
     * Sets enemyCellCost for enemies and it's effect area cells
     */
    private void placeEnemiesOnMap(Enemy[] enemies) {
        for (Enemy enemy : enemies) {
            int x = enemy.getX();
            int y = enemy.getY();
            body[y][x] = new Cell(enemyCellCost, y, x, enemy.getName());
            placeEffectAreaOnMap(enemy);
        }
    }

    /**
     * Sets enemyCellCost for enemies effect area cells
     */
    private void placeEffectAreaOnMap(Enemy enemy) {
        var area = enemy.getEffectArea();
        if (area == null)
            return;

        int area_height = area.length;
        int area_width = area[0].length;

        // relative position of the enemy in the area
        var enemy_pos_y_in_area = area_height / 2;
        var enemy_pos_x_in_area = area_width / 2;

        for (int i = 0; i < area_height; i++) {
            for (int j = 0; j < area_width; j++) {
                if (area[i][j] == '#') {
                    int dis_x = j - enemy_pos_x_in_area;
                    int dis_y = i - enemy_pos_y_in_area;

                    int effect_pos_x = enemy.getX() + dis_x;
                    int effect_pos_y = enemy.getY() + dis_y;

                    if (inMap(effect_pos_y, effect_pos_x))
                        body[effect_pos_y][effect_pos_x] = new Cell(enemyCellCost, effect_pos_y, effect_pos_x,
                                enemy.getName());
                }
            }
        }
    }

    /**
     * Put cells with emptyCellCost in the map body
     */
    private void fillMapWithEmptyCells() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (body[i][j] == null)
                    body[i][j] = new Cell(emptyCellCost, i, j);
            }
        }
    }

    public ArrayList<int[]> getWinPath() {
        return win;
    }

    class Cell {
        private int id;
        private int x;
        private int y;

        private boolean isSupportPoint = false;
        private boolean isWinPath = false;
        private String belongsTo = null;

        private int cost;
        private int estimatedCost = -1;
        private int totalCost = -1;

        private Cell parent = null;

        public Cell(int cost, int y, int x) {
            this.cost = cost;
            this.x = x;
            this.y = y;
            this.id = (x + 1) + (y) * 9;
        }

        public Cell(int cost, int y, int x, String belongsTo) {
            this.cost = cost;
            this.x = x;
            this.y = y;
            this.belongsTo = belongsTo;
            this.id = (x + 1) + (y) * 9;
        }

        public Cell(int id, int x, int y, boolean isSupportPoint,
                boolean isWinPath, int cost, int estimatedCost,
                int totalCost, Cell parent) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.isSupportPoint = isSupportPoint;
            this.isWinPath = isWinPath;
            this.cost = cost;
            this.estimatedCost = estimatedCost;
            this.totalCost = totalCost;
            this.parent = parent;
        }

        public int getTotalConst() {
            return totalCost;
        }

        private Cell copy() {
            return new Cell(id, x, y, isSupportPoint, isWinPath, cost, estimatedCost, totalCost, parent);
        }

        @Override
        public String toString() {
            return "id:" + id + "," + "y:" + y + "," + "x:" + x;
        }

        public int getId() {
            return id;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isSupportPoint() {
            return isSupportPoint;
        }

        public boolean isWinPath() {
            return isWinPath;
        }

        public String getBelongsTo() {
            return belongsTo;
        }

        public int getCost() {
            return cost;
        }

    }
}

class Player extends Entity {
    public boolean hasSupport = false;
    public ArrayList<Enemy> knownEnemies = new ArrayList<>();

    public Player(int[] coordinates, String name) {
        super(coordinates, name);
    }

}

class Enemy extends Entity {
    private char[][] effectArea = null;
    private boolean isImmortal = true;

    public Enemy(int[] coordinates, String name) {
        super(coordinates, name);
    }

    public Enemy(int[] coordinates, String name, char[][] effectArea) {
        super(coordinates, name);
        this.effectArea = effectArea;
    }

    public Enemy(int[] coordinates, String name, char[][] effectArea, boolean isImmortal) {
        super(coordinates, name);
        this.effectArea = effectArea;
        this.isImmortal = isImmortal;
    }

    public char[][] getEffectArea() {
        return effectArea;
    }

    public boolean isImmortal() {
        return isImmortal;
    }
}

class Support extends Entity {
    public Support(int[] coordinates, String name) {
        super(coordinates, name);
    }
}

class Goal extends Entity {
    public Goal(int[] coordinates, String name) {
        super(coordinates, name);
    }
}

class Entity {
    private int x;
    private int y;
    private String name;

    public Entity(int[] coordinates, String name) {
        this.y = coordinates[0];
        this.x = coordinates[1];
        this.name = name;
    }

    public void move(int y, int x) {
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

enum Colors {
    STOP("\033[0m"),
    RED("\033[0;31m"),
    GREEN("\033[0;32m"),
    YELLOW("\033[0;33m");

    private String code;

    Colors(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
