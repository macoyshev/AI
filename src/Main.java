import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

class Coordinates {
    int x = 0;
    int y = 0;

    Coordinates (int y, int x){
        this.x = x;
        this.y = y;
    }
}

class Cell {
    double estimatedDistance = 0;
    double totalCost = 0;
    double cost = 0;

    boolean withKraken = false;
    boolean withStone = false;
    boolean withDavy = false;
    boolean danger;

    Cell parent = null;

    int x = 0;
    int y = 0;

    Cell() {}

    Cell(int y, int x){
        this.x = x;
        this.y = y;
    }
   
    Cell(Coordinates coordinates){
        this.x = coordinates.x;
        this.y = coordinates.y;
    }

    public double getTotalCost(){
        return totalCost;
    }

    public double getEstimatedCost(){
        return estimatedDistance;
    }

    public boolean isEqual(Cell cell){
        return cell.x == this.x && cell.y == this.y;
    }

    public boolean withEnemy() {
        return withKraken && withStone && withDavy;
    }
}


public class Main {
    static int globalCounter = 0;
    static boolean krakenLife = true;
    static int sizeX = 9;
    static int sizeY = 9;
    static int scenario;
    static int [] adjacent1 = {1, 1, -1, -1, 1, -1,0, 0};
    static int [] adjacent2 = {1, -1, 1, -1, 0, 0, 1, -1};
    static Coordinates jackCoords;
    static Coordinates tortugaCoords;
    static Coordinates chestCoords;
    static Coordinates rockCoordinates;
    static Coordinates davyCoordinates;
    static Coordinates krakenCoordinates;
    static Cell rockCell;
    static Cell davyCell;
    static Cell jackCell;
    static Cell tortugaCell;
    static Cell chestCell;
    static Cell krakenCell;
    static int [][] ans = new int[10][10];
    static int length;
    static ArrayList<Cell> cells;

    public static void main(String[] args) {
        Cell[][] map = new Cell[sizeY][sizeX];
        Cell[][] map1 = new Cell[sizeY][sizeX];
        Cell[][] map2 = new Cell[sizeY][sizeX];

        getInput();

        generateMap(cells, map);
        generateMap(cells, map1);
        generateMap(cells, map2);

        printMap(map);
        System.out.println();

        writeShortestPath(aStarAlgorithm(map2), "outputAStar.txt");
        writeShortestPath(backtrackingAlgorithm(map1), "outputBacktracking.txt");
    }

    public static void getInput(){
        Scanner scanner = new Scanner(System.in);
        cells = getCoordinatesInput(scanner);
        try {
            scenario = scanner.nextInt();
        } catch (InputMismatchException e){
            System.out.println("Invalid input");
            System.exit(0);
        }
    }

    public ArrayList<ArrayList<int[]>> generateTest(int count) {
        var coordinatesList = new ArrayList<ArrayList<int[]>>();
        for(int i = 0; i < count; i++) {
            var jackCoordinates = generateCoordinates();
            var davyCoordinates = generateCoordinates();
            var krakenCoordinates = generateCoordinates();
            var stoneCoordinates = generateCoordinates();
            var treasureCoordinates = generateCoordinates();
            var tortuga = generateCoordinates();

            while (Arrays.equals(davyCoordinates, stoneCoordinates) &&
                   Arrays.equals(davyCoordinates, jackCoordinates) &&
                   Arrays.equals(davyCoordinates, krakenCoordinates) &&
                   Arrays.equals(davyCoordinates, treasureCoordinates) &&
                   Arrays.equals(davyCoordinates, tortuga)
            ) davyCoordinates = generateCoordinates();

            while (Arrays.equals(krakenCoordinates, jackCoordinates) &&
                   Arrays.equals(krakenCoordinates, davyCoordinates) &&
                   Arrays.equals(krakenCoordinates, treasureCoordinates) &&
                   Arrays.equals(krakenCoordinates, tortuga)
            ) krakenCoordinates = generateCoordinates();
            

            while (Arrays.equals(stoneCoordinates, jackCoordinates) &&
                   Arrays.equals(stoneCoordinates, davyCoordinates) &&
                   Arrays.equals(stoneCoordinates, treasureCoordinates) &&
                   Arrays.equals(stoneCoordinates, tortuga)
            ) krakenCoordinates = generateCoordinates();
        }
        
        return coordinatesList;
    }

    public int[] generateCoordinates() {
        var randomizer = new Random();
        var x = randomizer.nextInt(9);
        var y = randomizer.nextInt(9);

        return new int[] {x, y};
    }

    public static ArrayList<Cell> getCoordinatesInput(Scanner scanner){
        String input = scanner.nextLine();
        Pattern pattern = Pattern.compile("(\\[[0-8],\\d\\]{1} ){5}\\[\\d,\\d\\]{1}$");
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find())
        {
            System.out.println("Invalid input");
            System.exit(0);
        }

        var str_coordinates = input.split(" ");
        ArrayList<Cell> cells = new ArrayList<>();

        for (String str_coordinate : str_coordinates) {
            var coordinate = Arrays.stream(str_coordinate.replaceAll("[\\[\\]]", "")
                            .split(",")).mapToInt(Integer::parseInt)
                    .toArray();
            Cell cell = new Cell();
            cell.y = coordinate[0];
            cell.x = coordinate[1];
            cells.add(cell);
        }
        return cells;
    }

    public static void generateMap(ArrayList<Cell> cells, Cell [][]map){
        for (int i = 0;i < sizeY;i++){
            for (int j = 0;j < sizeX;j++){
                map[i][j] = new Cell(i,j);
            }
        }
        // Davy
        createDavyDangerZone(map, cells.get(1).y, cells.get(1).x);

        // Kraken
        createKrakenDangerZone(map, cells.get(2).y, cells.get(2).x);
        // Rock
        createRockDangerZone(map, cells.get(3).y, cells.get(3).x);

        map[cells.get(1).y][cells.get(1).x].withDavy = true;
        map[cells.get(2).y][cells.get(2).x].withKraken = true;
        map[cells.get(3).y][cells.get(3).x].withStone = true;

        jackCoords = new Coordinates(cells.get(0).y, cells.get(0).x);
        davyCoordinates = new Coordinates(cells.get(1).y, cells.get(1).x);
        krakenCoordinates = new Coordinates(cells.get(2).y, cells.get(2).x);
        rockCoordinates = new Coordinates(cells.get(3).y, cells.get(3).x);
        chestCoords = new Coordinates(cells.get(4).y, cells.get(4).x);
        tortugaCoords = new Coordinates(cells.get(5).y, cells.get(5).x);
        rockCoordinates = new Coordinates(cells.get(3).y, cells.get(3).x);
        jackCell = new Cell(jackCoords);
        krakenCell = new Cell(krakenCoordinates);
        chestCell = new Cell(chestCoords);
        tortugaCell = new Cell(tortugaCoords);
        rockCell = new Cell(rockCoordinates);
        davyCell = new Cell(davyCoordinates);
        rockCell = new Cell(rockCoordinates);
    }

    public static void printMap(Cell [][]map){
        for (int i = 0;i < sizeY;i++){
            for (int j = 0;j < sizeX;j++){
                if (map[i][j].danger) System.out.print("D ");
                else
                    if (i == jackCoords.y && j == jackCoords.x) System.out.print("J ");
                    else
                        if (i == chestCoords.y && j == chestCoords.x) System.out.print("C ");
                        else
                            if (i == tortugaCoords.y && j == tortugaCoords.x) System.out.print("T ");
                            else
                                System.out.print("- ");
            }
            System.out.println();
        }
    }

    public static boolean checkBorders(Coordinates point){
        boolean check1 = point.x >= 0 && point.x < 9;
        boolean check2 = point.y >= 0 && point.y < 9;
        return check1 && check2;
    }

    public static boolean checkBorders(int y, int x){
        boolean check1 = x >= 0 && x < 9;
        boolean check2 = y >= 0 && y < 9;
        return check1 && check2;
    }

    public static boolean checkMoving(Cell[][] map, int jackY, int jackX){
        return (jackX >= 0 && jackY >= 0 && jackX < sizeX && jackY < sizeY &&
                (!map[jackY][jackX].danger));
    }

    public static boolean checkInput(String input){

        return true;
    }

    public static int backTracking(Cell [][] map, Cell start, Cell goal, boolean weapon, boolean[][] visited,
                                   Coordinates[][] rec){
        globalCounter++;
        //if (globalCounter > 2000000) return 100000;
        if (!checkMoving(map, start.y, start.x)) return 100000;
        //if (start.x == tortugaCoords.x && start.y == tortugaCoords.y) weapon = true;
        if (weapon) killKraken(map, start, krakenCell);
        if (visited[start.y][start.x]) return ans[start.y][start.x];
        if (start.x == goal.x && start.y == goal.y) {
            ans[start.y][start.x] = 0;
            rec[start.y][start.x] = new Coordinates(20, 20);
            return 0;
        }
        visited[start.y][start.x] = true;
        ans[start.y][start.x] = 100000;
        rec[start.y][start.x] = new Coordinates(-1, -1);
        ArrayList<Cell> cells1 = new ArrayList<>();
        for (int i = 0;i < 8;i++){
            if (checkMoving(map, start.y + adjacent2[i], start.x + adjacent1[i])){

                Cell newCell = new Cell(start.y + adjacent2[i], start.x + adjacent1[i]);
                newCell.estimatedDistance = getDistance(newCell, goal);
                cells1.add(newCell);
                cells1.sort(Comparator.comparing(Cell::getEstimatedCost));
                int curLength = backTracking(map, cells1.remove(0), goal, weapon, visited, rec);
                if (ans[start.y][start.x] > curLength +1){
                    ans[start.y][start.x] = curLength + 1;
                    rec[start.y][start.x] = new Coordinates(start.y + adjacent2[i], start.x + adjacent1[i]);
                }
            }
        }
        return ans[start.y][start.x];
    }

    public static ArrayList<Cell> aStarAlgorithm(Cell [][]map){
        ArrayList<Cell> path1 = new ArrayList<>();
        ArrayList<Cell> path2 = new ArrayList<>();
        ArrayList<Cell> path3 = new ArrayList<>();

        path1 = aStar(path1, map, chestCell, jackCell, false);
        path3 = aStar(path3, map, tortugaCell, jackCell, false);
        path2 = aStar(path2, map, chestCell, tortugaCell, true);
        if (pathLength(path1) < pathLength(path2) + pathLength(path3)){
            return path1;
        }
        else
        {
            ArrayList<Cell> answer = new ArrayList<>(path2);
            if (path3!=null)
            for (int i = 1;i < path3.size();i++){
                answer.add(path3.get(i));
            }
            return answer;
        }
    }

    public static ArrayList<Cell> backtrackingAlgorithm(Cell[][] map){
        var map1 = map.clone();
        var map2 = map.clone();
        boolean [][]visited1 = new boolean[10][10];
        boolean [][]visited2 = new boolean[10][10];
        boolean [][]visited3 = new boolean[10][10];
        Coordinates[][] rec1 = new Coordinates[10][10];
        Coordinates[][] rec2 = new Coordinates[10][10];
        Coordinates[][] rec3 = new Coordinates[10][10];
        int length1 = backTracking(map1, jackCell, chestCell, false, visited1, rec1);
        globalCounter = 0;
        int length2 = backTracking(map2, jackCell, tortugaCell, false, visited2, rec2);
        globalCounter = 0;
        int length3 = backTracking(map2, tortugaCell, chestCell, true, visited3, rec3);
        globalCounter = 0;
        if (length1 == 100000 && ((length2 == 100000) || (length3 == 100000))){
            return null;
        }
        else
        if (length1 < length2 + length3){
            ArrayList<Cell> ans = new ArrayList<>();
            ans.addAll(findPath(rec1));
            ans.add(chestCell);
            return ans;
        }
        else {
            ArrayList<Cell> path3 = findPath(rec2);
            ArrayList<Cell> path2 = findPath(rec3);
            ArrayList<Cell> ans = new ArrayList<>(path2);
            if (path3!=null)
                for (int i = 0;i < path3.size();i++){
                    ans.add(path3.get(i));
                }
            return ans;
        }

    }

    public static ArrayList<Cell> aStar(ArrayList<Cell> path, Cell[][]map, Cell goal, Cell currentPosition,
                                        boolean weapon)     {
        ArrayList<Cell> openCells = new ArrayList<>();
        ArrayList<Cell> closedCells = new ArrayList<>();

        boolean betterCell;
        double cost;
        currentPosition.cost = 0;
        currentPosition.estimatedDistance = getDistance(currentPosition, goal);
        currentPosition.totalCost = currentPosition.cost + currentPosition.estimatedDistance;

        openCells.add(currentPosition);
        while (!openCells.isEmpty()){
            openCells.sort(Comparator.comparing(Cell::getTotalCost).thenComparing(Cell::getEstimatedCost));
            Cell currentCell = openCells.remove(0);
            if (currentCell.isEqual(tortugaCell)) weapon = true;
            if (weapon) killKraken(map, currentCell, krakenCell);
            if (currentCell.x == goal.x && currentCell.y == goal.y) return createPath(path, currentPosition, currentCell);
            closedCells.add(currentCell);

            for (Cell neighbor:getNeighborCells(currentCell, map)){
                if (closedCells.contains(neighbor)) continue;

                cost = currentCell.cost + getDistance(currentCell, neighbor);
                if (!openCells.contains(neighbor)){
                    openCells.add(neighbor);
                    betterCell = true;
                }
                else
                    if (cost < neighbor.cost){
                        betterCell = true;
                    }
                    else
                        betterCell = false;
                    if (betterCell){
                        neighbor.cost = cost;
                        neighbor.estimatedDistance = getDistance(neighbor, chestCell);
                        neighbor.totalCost = neighbor.cost + neighbor.estimatedDistance;
                        neighbor.parent = currentCell;
                    }
            }
        }
        return null;
    }

    public static double getDistance(Cell fst, Cell snd){
        double costX = Math.abs(snd.x - fst.x);
        double costY = Math.abs(snd.y - fst.y);
        return Math.max(costY, costX);
    }

    public static ArrayList<Cell> getNeighborCells(Cell currentCell, Cell[][] map){
        ArrayList<Cell> neighbors = new ArrayList<>();
        Cell neighbor;
        for (int i = 0;i < 8;i++){
            if (checkMoving(map, currentCell.y + adjacent2[i], currentCell.x + adjacent1[i])){
                neighbor = map[currentCell.y + adjacent2[i]][currentCell.x + adjacent1[i]];
                neighbor.cost = currentCell.cost + 1;
                neighbor.estimatedDistance = getDistance(neighbor, chestCell);
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    public static ArrayList<Cell> createPath(ArrayList<Cell> path, Cell start, Cell finish){
        Cell current = finish;
        while (current != null){
            path.add(current);

            current = current.parent;
        }
        return path;
    }

    public static int pathLength(ArrayList<Cell> path){
        if (path!=null)
        return path.size() - 1;
        return 100000;
    }

    public static ArrayList<Cell> findPath(Coordinates [][]rec) {
        int x = jackCoords.x;
        int y = jackCoords.y;
        ArrayList<Cell> path = new ArrayList<>();
        while (rec[y][x].x != 20) {
            if (rec[y][x].x != -1){
                int xx = x;
                x = rec[y][x].x;
                y = rec[y][xx].y;

                path.add(new Cell(y,x));
            }

        }
        return path;
    }

    public static void writeShortestPath(ArrayList<Cell> path, String fileName){
        var result = new StringBuilder();

        if (path == null){
            // add game status
            result.append("Lose\n");
        } else {
            Collections.reverse(path);
            path.add(jackCell);
            
            // add game status
            result.append("Win\n");
            result.append(path.size()).append("\n");

            // add wininig path
            for (Cell cell : path) {
                result.append(String.format("[%d,%d]",cell.y ,cell.x));
                result.append(" ");
            }
            result.append("\n\n");

            for(int i = -1; i < sizeY; i++) {
                for(int j = -1; j < sizeX; j++) {
                    if (i == -1 && j == -1) result.append(" ");
                    else if (i == -1 && j != -1) result.append(j);
                    else if (i != -1 && j == -1) result.append(i);
                    else if (cellWith(i, j, path) != null) result.append("*");
                    else result.append("-");
                    result.append(" ");
                }
                result.append("\n");
            }
        }

        try {
            FileWriter writer = new FileWriter(fileName);
            writer.write(result.toString());
            writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public static Cell cellWith(int y, int x, ArrayList<Cell>from) {
        for (Cell cell : from)
            if (cell.x == x && cell.y == y) return cell;
        return null;
    }

    public static void printPath(ArrayList<Cell> path){
        String [][] map = new String[9][9];
        for (int i = 0;i < 9;i++){
            for (int j = 0;j < 9;j++){
                map[i][j] = "_";
            }
        }
        map[jackCoords.y][jackCoords.x] = "*";
        if (path!=null)
            for (int i = 0;i < path.size();i++){
                map[path.get(i).y][path.get(i).x] = "*";
            }
        for (int i = 0;i < 19;i++) System.out.print("-");
        System.out.println();
        System.out.print("  ");
        for (int i = 0;i < 9;i++) System.out.print(i+" ");
        System.out.println();
        for (int i = 0;i < 9;i++){
            System.out.print(i+" ");
            for (int j = 0;j < 9;j++){
                System.out.print(map[i][j]+" ");
            }
            System.out.println();
        }
        for (int i = 0;i < 19;i++) System.out.print("-");
        System.out.println();
    }

    public static void killKraken(Cell[][] map, Cell currentPosition, Cell krakenPosition){
        for (int i = 0;i < 4;i++){
            if (currentPosition.y == krakenPosition.y + adjacent2[i] &&
                    currentPosition.x == krakenPosition.x + adjacent1[i]){
                krakenLife = false;
            }
        }
        if (!krakenLife){
            for (int i = -1;i < 2;i++){
                if (checkBorders(krakenPosition.y+i, krakenPosition.x))
                    map[krakenPosition.y+i][krakenPosition.x].danger = false;
            }
            if (checkBorders(krakenPosition.y, krakenPosition.x-1))
                map[krakenPosition.y][krakenPosition.x-1].danger = false;
            if (checkBorders(krakenPosition.y, krakenPosition.x+1))
                map[krakenPosition.y][krakenPosition.x+1].danger = false;
            createDavyDangerZone(map, davyCell);
            createRockDangerZone(map,rockCell);
        }
    }

    public static void createDavyDangerZone(Cell [][] map, int davyY, int davyX){
        for (int i =davyY-1;i <= davyY + 1;i++){
            for (int j = davyX-1;j <= davyX+1;j++){
                if (checkBorders(i,j))
                    map[i][j].danger = true;
            }
        }
    }

    public static void createDavyDangerZone(Cell [][] map, Cell davyCell){
        int davyY = davyCell.y;
        int davyX = davyCell.x;
        for (int i =davyY-1;i <= davyY + 1;i++){
            for (int j = davyX-1;j <= davyX+1;j++){
                if (checkBorders(i,j))
                    map[i][j].danger = true;
            }
        }
    }

    public static void createKrakenDangerZone(Cell [][]map, int krakenY, int krakenX){
        for (int i = krakenY-1; i <= krakenY+1;i++){
            if (checkBorders(i, krakenX)) map[i][krakenX].danger = true;
        }
        if (checkBorders(krakenY, krakenX-1))
        {
            map[krakenY][krakenX-1].danger = true;
        }
        if (checkBorders(krakenY, krakenX+1))
        {
            map[krakenY][krakenX+1].danger = true;
        }
    }

    public static void createKrakenDangerZone(Cell [][]map, Cell kraken){
        int krakenY = kraken.y;
        int krakenX = kraken.x;
        for (int i = krakenY-1; i <= krakenY+1;i++){
            if (checkBorders(i, krakenX)) map[i][krakenX].danger = true;
        }
        if (checkBorders(krakenY, krakenX-1))
        {
            map[krakenY][krakenX-1].danger = true;
        }
        if (checkBorders(krakenY, krakenX+1))
        {
            map[krakenY][krakenX+1].danger = true;
        }
    }

    public static void createRockDangerZone(Cell[][] map, int y, int x){
        map[y][x].danger = true;
    }

    public static void createRockDangerZone(Cell[][] map, Cell rockCell){
        int y = rockCell.y;
        int x = rockCell.x;
        map[y][x].danger = true;
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
