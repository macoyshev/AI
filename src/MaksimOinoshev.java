import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Maksim Oinoshev
 */
public class MaksimOinoshev{
    public static void main(String[] args) {
        var scanner = new Scanner(System.in);
        var coordinates = parseCoordinates(scanner.nextLine());
        var game_mode = parseGameMode(scanner.nextLine());
        scanner.close();
        
        final char[][] jackObserve;
        if (game_mode == 1) {
            jackObserve = new char[][]{
                {'#', '#', '#'},
                {'#', '#', '#'},
                {'#', '#', '#'},
            };
        } else {
            jackObserve = new char[][]{
                {'-', '-', '#', '-', '-'},
                {'-', '#', '#', '#', '-'},
                {'#', '#', '#', '#', '#'},
                {'-', '#', '#', '#', '-'},
                {'-', '-', '#', '-', '-'},

            };
        }

        final char[][] krakenAreaEffect = {
            {'-', '#', '-'},
            {'#', '#', '#'},
            {'-', '#', '-'},
        };

        final char[][] davyAreaEffect = {
            {'#', '#', '#'},
            {'#', '#', '#'},
            {'#', '#', '#'},
        };
        
        var jack = new Player(coordinates.get(0), "jack", jackObserve);
        var davy = new Enemy(coordinates.get(1), "davy", davyAreaEffect);
        var kraken = new Enemy(coordinates.get(2), "kraken", krakenAreaEffect, false);
        var stone = new Enemy(coordinates.get(3), "stone");
        var treasure = new Goal(coordinates.get(4), "brilliant");
        var tartuga = new Support(coordinates.get(5), "tartuga");

        var map = new TreasureMap(treasure, jack, tartuga, new Enemy[]{kraken, davy, stone});
        map.aStar();
    }

    public static int parseGameMode(String input) {
        var mode = -1;
        try {
            mode = Integer.parseInt(input);
        } catch (Exception e) {
            InvalidInput();
        }
        
        if (mode == 1 || mode == 2)
            return mode;
            
        InvalidInput();
        return 0;
    }

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

    private final Goal goal;
    private final Player player;
    private final Support support;
    private final Enemy[] enemies;

    private final Cell[][] body = new Cell[width][height];

    public TreasureMap(Goal goal, Player player, Support support, Enemy[] enemies) {
        this.goal = goal;
        this.player = player;
        this.support = support;
        this.enemies = enemies;
        
        placeEnemiesOnMap(enemies);
        fillMapWithEmptyCells();
    }

    public void aStar() {
        printMap();
        var initCell = getEntityCell(player);
        var deadCells = new HashSet<Cell>();
        var cheapestCells = new ArrayList<Cell>();
        var proceededCells = new ArrayList<Cell>();
        Cell goalCell = null;

        cheapestCells.add(initCell);
        while (!cheapestCells.isEmpty()) {
            var cell = cheapestCells.remove(0);

            if (cellContains(cell, goal)) {
                goalCell = cell;
                break;
            };

            var neighborCells = getNeighborCells(cell);
            if (neighborCells.isEmpty()) {
                deadCells.add(cell);
                continue;
            }
            
            deduplicate(neighborCells, cheapestCells, proceededCells, deadCells);
            
            recalculateTotalCost(cell, neighborCells);

            cheapestCells.addAll(neighborCells);
            cheapestCells.sort(Comparator.comparing(Cell::getTotalConst));
            proceededCells.add(cell);
        }
        
        if (goalCell != null) {
            System.out.println("WIN!");
            markWinPath(goalCell);
            printMap();
        } else
            System.out.println("NO WIN!");
    }

    private void deduplicate(ArrayList<Cell> neighborCells, ArrayList<Cell> cheapestCells, ArrayList<Cell> procededCells, HashSet<Cell> deadCells) {
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

            // remove collision of dead and neighbor cells
            for (Cell dead : deadCells) 
                if (dead.id == neighbor.id) 
                    neighborCellsToRemove.add(neighbor);
        }

        for (Cell cellToRemove : neighborCellsToRemove)
            neighborCells.remove(cellToRemove);

        for (Cell cellToRemove : cheapestCellsToRemove) 
            cheapestCells.remove(cellToRemove);
    }

    private void markWinPath(Cell goalCell) {
        var cell = goalCell;
        var initCell = getEntityCell(player);
        while (cell.id != initCell.id) {
            body[cell.y][cell.x].isWinPath = true;
            cell = cell.parent;
        }
    }

    private void printMap() {
        var entities = new ArrayList<Entity>(Arrays.asList(player, goal, support));
        entities.addAll(Arrays.asList(enemies));
        System.out.println(" 012345678");
        for(int i = 0; i < height; i++) {
            System.out.print(i);

            for(int j = 0; j < width; j++) {
                var cell = body[i][j];
                
                var isEntity = false;
                for (Entity entity : entities) {
                    if (cell == getEntityCell(entity)) {
                        System.out.print(entity.getName().charAt(0));
                        isEntity = true;
                        break;
                    }
                }
                if (isEntity) continue;

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

    private ArrayList<Cell> getNeighborCells(Cell cell) {
        int x = cell.x;
        int y = cell.y;

        var neighbors = new ArrayList<Cell>();
        var observeArea = player.getObserveArea();
        int heightShiftInArea = observeArea.length / 2;
        int WidthShiftInArea = observeArea[0].length / 2;

        for(int i = -heightShiftInArea; i < heightShiftInArea + 1; i++) {
            for(int j = -WidthShiftInArea; j < WidthShiftInArea + 1; j++) {
                if (i == 0 && j == 0) continue;  //cell itself

                int yMap = y + i;
                int xMap = x + j;

                if (inMap(yMap, xMap) && !inEffectArea(yMap, xMap)){
                    var neighbor = body[yMap][xMap];
                    int yArea = i + heightShiftInArea;
                    int xArea = j + WidthShiftInArea;
                    
                    if (cell.parent != neighbor && observeArea[yArea][xArea] == '#') {
                        var neighborCopy = neighbor.copy();
                        neighborCopy.parent = cell;
                        neighbors.add(neighborCopy);
                    }
                }
            }
        }
        return neighbors;
    }

    private Cell getEntityCell(Entity entity) {
        return body[entity.getY()][entity.getX()];
    }

    private void recalculateTotalCost(Cell cell, ArrayList<Cell> neighbors){
        for (Cell neighbor : neighbors) {
            int[] costs = calculateCost(neighbor);
            int newCost = costs[0];
            int manhatanCost = costs[1];
            int newtotalCost = costs[2];

            neighbor.cost = newCost;
            neighbor.manhatanCost = manhatanCost;
            neighbor.totalCost = newtotalCost;
        }
    }

    private int[] calculateCost(Cell cell) {
        int cathetus_x = Math.abs(goal.getX() - cell.x);
        int cathetus_y = Math.abs(goal.getY() - cell.y);
        int manhatanCost = Math.max(cathetus_x, cathetus_y);

        int newCost = cell.cost;
        if (cell.parent != null) newCost += cell.parent.cost;

        int newTotalCost = newCost + manhatanCost;
        
        return new int[] {newCost, manhatanCost, newTotalCost};
    }

    private boolean inMap(int y, int x) {
        return xInMap(x) && yInMap(y);
    }

    private boolean inEffectArea(int y, int x) {
        return body[y][x].cost == -1;
    }

    private boolean xInMap(int x) {
        return x < width && x >= 0;
    }

    private boolean yInMap(int y) {
        return y < width && y >= 0;
    }

    private boolean cellContains(Cell cell, Entity entity) {
        return cell.x == entity.getX() && cell.y == entity.getY();
    }

    private void placeSupportOnMap(Support support) {
        int x = support.getX();
        int y = support.getY();
        body[y][x] = new Cell(enemyCellCost, y, x);
    }
    
    private void placeEnemiesOnMap(Enemy[] enemies) {
        for (Enemy enemy : enemies) {
            int x = enemy.getX();
            int y = enemy.getY();
            body[y][x] = new Cell(enemyCellCost, y, x);
            placeEffectAreaOnMap(enemy);
        }
    }
    
    private void placeEffectAreaOnMap(Enemy enemy) {
        var area = enemy.getEffectArea();
        if (area == null)
            return;

        int area_height = area.length;
        int area_width = area[0].length;

        // relative position of the enemy in the area
        var enemy_pos_y_in_area = area_height / 2;
        var enemy_pos_x_in_area = area_width / 2;

        for(int i = 0; i < area_height; i++) {
            for(int j = 0; j < area_width; j++) {
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

    private void fillMapWithEmptyCells() {
        for(int i  = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                if (body[i][j] == null)
                    body[i][j] = new Cell(emptyCellCost, i, j);
            }
        }
    }
    
    class Cell{
        private int id;
        private int x;
        private int y;

        private boolean isSupportPoint = false;
        private boolean isWinPath = false;

        private int cost;
        private int manhatanCost = -1;
        private int totalCost = -1;

        private Cell parent = null;
        
        public Cell(int cost, int y, int x) {
            this.cost = cost;
            this.x = x;
            this.y = y;
            this.id = (x + 1) + (y) * 9 ;
        }
        
        public Cell(int id, int x, int y, boolean isSupportPoint, boolean isWinPath, int cost, int manhatanCost, int totalCost, Cell parent) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.isSupportPoint = isSupportPoint;
            this.isWinPath = isWinPath;
            this.cost = cost;
            this.manhatanCost = manhatanCost;
            this.totalCost = totalCost;
            this.parent = parent;
        }

        public int getTotalConst() {
            return totalCost;
        }

        private Cell copy() {
            return new Cell(id, x, y, isSupportPoint, isWinPath, cost, manhatanCost, totalCost, parent);
        }

        @Override
        public String toString() {
            return "id:" + id + "," + "y:" + y + "," + "x:" + x ;
        }

        
    }
}

class Player extends Entity {
    private char[][] observeArea;

    public Player(int[] coordinates, String name, char[][] observeArea) {
        super(coordinates, name);
        this.observeArea = observeArea;
    }

    public char[][] getObserveArea() {
        return observeArea;
    } 
}


class Enemy extends Entity {
    private char[][] effectArea = null;
    private boolean is_immortal = true;

    public Enemy(int[] coordinates, String name) {
        super(coordinates, name);
    }

    public Enemy(int[] coordinates, String name, char[][]effectArea) {
        super(coordinates, name);
        this.effectArea = effectArea;
    }

    public Enemy(int[] coordinates, String name, char[][] effectArea, boolean is_immortal) {
        super(coordinates, name);
        this.effectArea = effectArea;
        this.is_immortal = is_immortal;
    }

    public char[][] getEffectArea() {
        return effectArea;
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
