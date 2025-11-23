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

        // NOUVEAU : Créer un ordre de suppression aléatoire pour les 81 cases
        int[] indices = new int[SIZE * SIZE];
        for(int i = 0; i < indices.length; i++) indices[i] = i;

        // Mélanger les indices (shuffle)
        for (int i = 0; i < indices.length; i++) {
            int j = rand.nextInt(indices.length);
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
        }

        // Parcourir dans l'ordre aléatoire
        for (int index : indices) {
            if (removed >= emptyCells) break;

            int r = index / SIZE; // Calcul de la ligne
            int c = index % SIZE; // Calcul de la colonne

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
        if (count > 1) return count;
        int r = -1, c = -1;

        for (r = 0; r < SIZE; r++) {
            for (c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) { // Trouvé une case vide

                    for (int num = 1; num <= SIZE; num++) {
                        if (isSafe(grid, r, c, num)) {
                            grid[r][c] = num;
                            count = countSolutions(grid, count);
                            grid[r][c] = 0; // Backtrack
                        }
                    }
                    return count;
                }
            }
        }
        return count + 1; // Solution trouvée
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
