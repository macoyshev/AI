import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
        var initCell = body[player.getY()][player.getX()];
        var cheapestCells = new LinkedList<Cell>();
        
        cheapestCells.add(initCell);
        while (!cheapestCells.isEmpty()) {
            var cell = cheapestCells.remove(0);

            var neighborCells = getNeighborCells(cell);
            if (neighborCells.isEmpty()) break;
            
            // PATH Calc
            // int cathetus_x = Math.abs(goal.getX() - x);
            // int cathetus_y = Math.abs(goal.getY() - y);
            // manhatanCost = cathetus_x + cathetus_y;
            // totalCost = cost + manhatanCost;

            cheapestCells.addAll(neighborCells);
            cheapestCells.sort(Comparator.comparing(Cell::getTotalConst));
        }
    }

    private ArrayList<Cell> getNeighborCells(Cell cell) {
        int x = cell.getX();
        int y = cell.getY();

        var neighbors = new ArrayList<Cell>();
        for(int i = -1; i < 2; i++) {
            for(int j = -1; j < 2; j++) {
                if (i == 0 && y == 0) continue;  //cell itself

                if (inMap(y + i, x + y) && inEffectArea(y + i, x + j)) {
                    var neighbor = body[y + i][x + i];
                    if (neighbor.parent != cell) {
                        neighbor.parent = neighbor;
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
    }


    private boolean inMap(int y, int x) {
        return xInMap(x) && yInMap(y);
    }

    private boolean inEffectArea(int y, int x) {
        return body[y][x].cost != -1;
    }

    private boolean xInMap(int x) {
        return x < width && x >= 0;
    }

    private boolean yInMap(int y) {
        return y < width && y >= 0;
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

        int area_height = area.length / 2;
        int area_width = area[0].length / 2;

        // relative position of the enemy in the area
        var enemy_pos_y_in_area = area_height;
        var enemy_pos_x_in_area = area_width;

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
    
    class Cell implements Comparable<Cell> {
        private int x;
        private int y;

        private int cost;
        private int manhatanCost;
        private int totalCost;

        private Cell parent = null;
        
        public Cell(int cost, int y, int x) {
            this.cost = cost;
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(TreasureMap.Cell anotheCell) {
            return Integer.compare(this.getTotalConst(), anotheCell.getTotalConst());
        }

        public int getTotalConst() {
            return manhatanCost + cost;
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