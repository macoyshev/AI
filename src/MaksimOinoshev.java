import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Maksim Oinoshev
 */
public class MaksimOinoshev {
    public static void main(String[] args) {
        Random rand = new Random();
        // coor
        var scanner = new Scanner(System.in);
        // var coordinates = parseCoordinates(scanner.nextLine());
        var coordinates = new ArrayList<int[]>();
        for(int i = 0; i < 6; i++)
            coordinates.add(new int[] {rand.nextInt(9), rand.nextInt(9)});

        for (int[] is : coordinates) {
            System.out.print("[" + is[0]+","+is[1]+"] ");
        }
        System.out.print("\n");
        var gamemode = 1;
        scanner.close();

        final char[][] krakenAreaEffect = {
                { '-', '#', '-' },
                { '#', '#', '#' },
                { '-', '#', '-' },
        };

        final char[][] davyAreaEffect = {
                { '#', '#', '#' },
                { '#', '#', '#' },
                { '#', '#', '#' },
        };

        var jack = new Player(coordinates.get(0), "jack");
        var davy = new Enemy(coordinates.get(1), "davy", davyAreaEffect);
        var kraken = new Enemy(coordinates.get(2), "kraken", krakenAreaEffect, false);
        var stone = new Enemy(coordinates.get(3), "stone");
        var treasure = new Goal(coordinates.get(4), "brilliant");
        var tartuga = new Support(coordinates.get(5), "tartuga");

        var map = new TreasureMap(gamemode, treasure, jack, tartuga, new Enemy[] { kraken, davy, stone });
        map.aStar();
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
        Pattern pattern = Pattern.compile("(\\[\\d,\\d\\]{1} ){5}\\[\\d,\\d\\]{1}$");
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

    public void aStar() {
        printMap();

        Cell goalCell = null;
        var initCell = getEntityCell(player);
        var cheapestCellFirst = new ArrayList<Cell>();
        var proceededCells = new ArrayList<Cell>();
        cheapestCellFirst.add(initCell);

        if (!inEffectArea(player.getY(), player.getX()))
            while (!cheapestCellFirst.isEmpty()) {
                var cell = cheapestCellFirst.remove(0);

                if (cellContains(cell, goal)) {
                    goalCell = cell;
                    break;
                }

                var neighborCells = getNeighborCells(cell);
                if (neighborCells.isEmpty()) {
                    continue;
                }
                
                // used for optimization
                removeProcededCells(neighborCells, proceededCells);
                recalculateTotalCost(cell, neighborCells);

                proceededCells.add(cell);
                cheapestCellFirst.addAll(neighborCells);
                cheapestCellFirst.sort(Comparator.comparing(Cell::getTotalConst));
            }

        if (goalCell != null) {
            var pathCost = goalCell.cost - 1; // since player spawn cell costs 1
            markWinPath(goalCell);

            System.out.println("WIN!");
            printMap();
            System.out.println("Cost: " + pathCost);
        } else
            System.out.println("NO WIN!");
    }

    /**
     * Removes proceeded element from the neighborCells and cheapestCells lists
     * 
     * @param neighborCells - array list of neighbors
     * @param proceededCells - array list of proceded cells from cheapestCells
     */
    private void removeProcededCells(ArrayList<Cell> neighborCells, ArrayList<Cell> proceededCells) {
        var neighborCellsToRemove = new ArrayList<Cell>();

        for (Cell neighbor : neighborCells) {
            // remove collision of proceded and neighbor cells
            for (Cell proceded : proceededCells)
                if (proceded.id == neighbor.id && proceded.totalCost < calculateCost(neighbor)[2])
                    neighborCellsToRemove.add(neighbor);
        }

        for (Cell cellToRemove : neighborCellsToRemove)
            neighborCells.remove(cellToRemove);
    }

    /**
     * Sets isWiningPath to true for the shortest path
     */
    private void markWinPath(Cell goalCell) {
        var cell = goalCell;
        var initCell = getEntityCell(player);
        while (cell.id != initCell.id) {
            body[cell.y][cell.x].isWinPath = true;
            cell = cell.parent;
        }
    }

    /**
     * Display map to stdout
     */
    private void printMap() {
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
                    continue; // cell itself

                int yMap = y + i;
                int xMap = x + j;

                if (inMap(yMap, xMap) && !inEffectArea(yMap, xMap)) {
                    var neighbor = body[yMap][xMap].copy();
                    neighbor.parent = cell;
                    neighbors.add(neighbor);
                }

                // if (inMap(yMap, xMap) && isMortalEnemyFound(yMap, xMap))
                //     player.knownMortalEnemies.add(en);
            }
        }
        return neighbors;
    }

    /**
     * Returns cell that contains the given entity
     */
    private Cell getEntityCell(Entity entity) {
        return body[entity.getY()][entity.getX()];
    }

    private Enemy getEnemyWith(int y, int x) {
        for (Enemy enemy : enemies) {
            if (enemy.getX() == x && enemy.getY() == y)
                return enemy;
        }
        return null;
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

    private boolean isMortalEnemyFound(int y, int x) {
        for (Enemy enemy : enemies) {
            if (enemy.isImmortal())
                return enemy.getX() == x && enemy.getY() == y;
        }
        return false;
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
            body[y][x] = new Cell(enemyCellCost, y, x);
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
                        body[effect_pos_y][effect_pos_x] = new Cell(enemyCellCost, i, j);
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

    class Cell {
        private int id;
        private int x;
        private int y;

        private boolean isSupportPoint = false;
        private boolean isWinPath = false;

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

    }
}

class Player extends Entity {
    private boolean hasSupport = false;
    private ArrayList<Enemy> knownMortalEnemies = new ArrayList<>();

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
