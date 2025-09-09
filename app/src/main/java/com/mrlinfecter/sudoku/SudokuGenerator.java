package com.mrlinfecter.sudoku;

import java.util.Random;

public class SudokuGenerator {
    private static final int SIZE = 9;

    public int[][] generateSolution() {
        int[][] grid = new int[SIZE][SIZE];
        fillGrid(grid);
        return grid;
    }

    public int[][] generatePuzzle(int[][] solution, int emptyCells) {
        int[][] puzzle = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            puzzle[i] = solution[i].clone();
        }

        Random rand = new Random();
        int removed = 0;

        while (removed < emptyCells) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);

            if (puzzle[r][c] != 0) {
                int backup = puzzle[r][c];
                puzzle[r][c] = 0;

                // Vérifie si la grille a toujours UNE solution unique
                if (!hasUniqueSolution(puzzle)) {
                    puzzle[r][c] = backup; // rollback
                } else {
                    removed++;
                }
            }
        }
        return puzzle;
    }

    private boolean hasUniqueSolution(int[][] grid) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            copy[i] = grid[i].clone();
        }
        return countSolutions(copy, 0) == 1;
    }

    private int countSolutions(int[][] grid, int count) {
        if (count > 1) return count; // pas besoin de chercher plus

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    for (int num = 1; num <= SIZE; num++) {
                        if (isSafe(grid, r, c, num)) {
                            grid[r][c] = num;
                            count = countSolutions(grid, count);
                            grid[r][c] = 0;
                        }
                    }
                    return count; // stop après le premier vide
                }
            }
        }
        return count + 1; // solution trouvée
    }



    private boolean fillGrid(int[][] grid) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == 0) {
                    int[] numbers = shuffledNumbers();
                    for (int num : numbers) {
                        if (isSafe(grid, row, col, num)) {
                            grid[row][col] = num;
                            if (fillGrid(grid)) return true;
                            grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private int[] shuffledNumbers() {
        int[] numbers = {1,2,3,4,5,6,7,8,9};
        Random rand = new Random();
        for (int i = 0; i < numbers.length; i++) {
            int j = rand.nextInt(numbers.length);
            int tmp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = tmp;
        }
        return numbers;
    }

    private boolean isSafe(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == num || grid[i][col] == num) return false;
        }
        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[startRow + i][startCol + j] == num) return false;
            }
        }
        return true;
    }
}
