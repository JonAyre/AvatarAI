package com.avatarai;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class IntMatrix {
    private final short[][] matrix;

    public IntMatrix(short[][] other) {
        matrix = new short[other.length][other[0].length];
        for (int i = 0; i < other.length; i++) {
            for (int j = 0; j < other[i].length; j++) {
                matrix[i][j] = other[i][j];
            }
        }
    }

    public IntMatrix(double[][] other) {
        matrix = new short[other.length][other[0].length];
        for (int i = 0; i < other.length; i++) {
            for (int j = 0; j < other[i].length; j++) {
                matrix[i][j] = doubleToShort(other[i][j]);
            }
        }
    }

    public IntMatrix(IntMatrix other) {
        matrix = new short[other.matrix.length][other.matrix[0].length];
        for (int i = 0; i < other.matrix.length; i++) {
            for (int j = 0; j < other.matrix[i].length; j++) {
                matrix[i][j] = other.matrix[i][j];
            }
        }
    }

    public IntMatrix(int rows, int columns) {
        matrix = new short[rows][columns];
    }

    public static IntMatrix randomMatrix(int rows, int columns, double min, double max) {
        IntMatrix newMatrix = new IntMatrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                newMatrix.matrix[i][j] = doubleToShort(min + Math.random() * (max - min));
            }
        }
        return newMatrix;
    }

    // Convert a floating point number from the range -1.0 to 1,0 to the range -32767 to 32767 (and vice versa)
    public static short doubleToShort(double d) {
        return (short)Math.rint(Math.clamp(d, -1.0, 1.0) * Short.MAX_VALUE);
    }
    public static double shortToDouble(short s) {
        return (double)s / (double)Short.MAX_VALUE;
    }
    public static short shortAdd(short a, short b) {
        int c = a + b;
        return (short)Math.clamp(c, -Short.MAX_VALUE, Short.MAX_VALUE);
    }
    public static short shortSubtract(short a, short b) {
        int c = a - b;
        return (short)Math.clamp(c, -Short.MAX_VALUE, Short.MAX_VALUE);
    }
    public static short shortMultiply(short a, short b) {
        int c = a * b;
        return (short)(1+(c / 32768));
    }
    public static short shortDivide(short a, short b) {
        int c = (a * 32768) / b;
        return (short)(c-1);
    }

    public int rows() {
        return matrix.length;
    }

    public int columns() {
        return matrix[0].length;
    }

    public void setValue(int row, int column, double value) {
        matrix[row][column] = doubleToShort(value);
    }
    public double getValue(int row, int column) {
        return shortToDouble(matrix[row][column]);
    }

    public double[] column(int column) {
        double[] columnValues = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            columnValues[i] = shortToDouble(matrix[i][column]);
        }
        return columnValues;
    }

    public double[] row(int row) {
        double[] rowValues = new double[matrix[row].length];
        for (int i = 0; i < matrix[row].length; i++) {
            rowValues[i] = shortToDouble(matrix[row][i]);
        }
        return rowValues;
    }

    public static IntMatrix identityMatrix(int size) {
        IntMatrix identity = new IntMatrix(size, size);
        for (int i = 0; i < size; i++) {
            identity.matrix[i][i] = Short.MAX_VALUE;
        }
        return identity;
    }

    public IntMatrix add(IntMatrix other) {
        if (this.matrix.length != other.matrix.length && this.matrix[0].length != other.matrix[0].length) {
            return null;
        }

        IntMatrix newMatrix = new IntMatrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] = shortAdd(newMatrix.matrix[row][col], other.matrix[row][col]);
            }
        }

        return newMatrix;
    }

    public IntMatrix subtract(IntMatrix other) {
        if (this.matrix.length != other.matrix.length && this.matrix[0].length != other.matrix[0].length) {
            return null;
        }

        IntMatrix newMatrix = new IntMatrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] = shortSubtract(newMatrix.matrix[row][col], other.matrix[row][col]);
            }
        }

        return newMatrix;
    }

    public IntMatrix multiply(IntMatrix other) {
        if (this.matrix[0].length != other.matrix.length && this.matrix.length != other.matrix[0].length) {return null;}

        IntMatrix newMatrix = new IntMatrix(this.matrix.length, other.matrix[0].length);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < other.matrix[0].length; col++) {
                for (int cell = 0; cell < this.matrix[row].length; cell++) {
                    short product = shortMultiply(this.matrix[row][col], other.matrix[row][col]);
                    newMatrix.matrix[row][col] = shortAdd(newMatrix.matrix[row][col], product);
                }
            }
        }

        return newMatrix;
    }

    public IntMatrix transpose() {
        IntMatrix newMatrix = new IntMatrix(this.matrix[0].length, this.matrix.length);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[col][row] = this.matrix[row][col];
            }
        }
        return newMatrix;
    }

    public IntMatrix scale(double scale) {
        short scaleValue = doubleToShort(scale);
        IntMatrix newMatrix = new IntMatrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] = shortMultiply(this.matrix[row][col], scaleValue);
            }
        }
        return newMatrix;
    }

    public IntMatrix applyFunction(Function<Short, Short> function) {
        IntMatrix newMatrix = new IntMatrix(this.matrix);
        for (int row = 0; row < this.matrix.length; row++) {
            for (int col = 0; col < this.matrix[row].length; col++) {
                newMatrix.matrix[row][col] = function.apply(this.matrix[row][col]);
            }
        }
        return newMatrix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntMatrix matrix1 = (IntMatrix) o;
        return Objects.deepEquals(matrix, matrix1.matrix);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("[");
        for (int i = 0; i < this.matrix.length; i++) {
            if (i > 0) string.append(", ");
            double[] doubleMatrix = new double[matrix[i].length];
            for (int j = 0; j < matrix[i].length; j++) {
                doubleMatrix[j] = shortToDouble(matrix[i][j]);
            }
            string.append(Arrays.toString(doubleMatrix));
        }
        string.append("]");
        return string.toString();
    }
}
