import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;


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
        var tartuga = new Support(coordinates.get(4), "tartuga");
        var treasure = new Goal(coordinates.get(5), "treasure");

        var map = new TreasureMap(treasure, jack, tartuga, new Enemy[]{kraken, davy, stone});
        map.aStar();
    }

    public static int parseGameMode(String input) {
        return Integer.parseInt(input);
    }

    public static ArrayList<int[]> parseCoordinates(String input) {
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
        var initCell = getEntityCell(player);
        var deadCells = new HashSet<Cell>();
        var cheapestCells = new ArrayList<Cell>();
        var proceededCells = new HashSet<Cell>();

        cheapestCells.add(initCell);
        while (!cheapestCells.isEmpty()) {
            var cell = cheapestCells.remove(0);
            proceededCells.add(cell);

            if (cellContains(cell, goal)) break;

            var neighborCells = getNeighborCells(cell);
            if (neighborCells.isEmpty()) {
                deadCells.add(cell);
                continue;
            }

            // deduplication
            for (Cell proceeded : proceededCells) {
                neighborCells.remove(proceeded);
            }
            for (Cell deadCell : deadCells) {
                neighborCells.remove(deadCell);
            }
            
            recalculateTotalCost(cell, neighborCells);
            cheapestCells.addAll(neighborCells);
            cheapestCells.sort(Comparator.comparing(Cell::getTotalConst));
        }

        var goalCell = getEntityCell(goal);
        if (goalCell.parent != null) {
            System.out.println("WIN!");
            markWinPath();
            printMap();
        } else
            System.out.println("NO WIN!");
    }

    private void markWinPath() {
        var cell = getEntityCell(goal);
        while (cell.id != 1) {
            cell.is_win_path = true;
            cell = cell.parent;
        }
    }

    private boolean isFamily(Cell cell1, Cell cell2) {
        while (cell1.parent != null) {
            if (cell1.id == cell2.id) return true;
            cell1 = cell1.parent;
        }

        return false;
    }

    private void printMap() {
        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                var cell = body[i][j];
                if (cell.cost == enemyCellCost) {
                    System.out.print("#");
                    continue;
                }

                if (cell.is_win_path) {
                    System.out.print("*");
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
        for(int i = -1; i < 2; i++) {
            for(int j = -1; j < 2; j++) {
                if (i == 0 && j == 0) continue;  //cell itself

                if (inMap(y + i, x + j) && !inEffectArea(y + i, x + j)) {
                    var neighbor = body[y + i][x + j];
                    if (cell.parent != neighbor) {
                        neighbors.add(neighbor);
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
            int cathetus_x = Math.abs(goal.getX() - neighbor.x);
            int cathetus_y = Math.abs(goal.getY() - neighbor.y);
            int manhatanCost = cathetus_x + cathetus_y;

            int newtotalCost = cell.cost + manhatanCost + emptyCellCost;
            if (neighbor.totalCost == -1 || neighbor.totalCost > newtotalCost) {
                neighbor.parent = cell;
                neighbor.totalCost = newtotalCost;
                neighbor.manhatanCost = manhatanCost;
            }
        }
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
    
    class Cell {
        private int id;
        private int x;
        private int y;

        private boolean is_win_path = false;

        private int cost;
        private int manhatanCost = -1;
        private int totalCost = -1;

        private Cell parent = null;
        
        public Cell(int cost, int y, int x) {
            this.cost = cost;
            this.x = x;
            this.y = y;
            this.id = (x + 1) * (y + 1);
        }

        public int getTotalConst() {
            return totalCost;
        }
    }
}

class Player extends Entity {
    private char[][] observeArea;

    public Player(int[] coordinates, String name, char[][] observeArea) {
        super(coordinates, name);
        this.observeArea = observeArea;
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