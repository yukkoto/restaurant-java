package ru.mirea.restaurant;

import java.util.Scanner;
abstract class AbstractArray {
abstract void inputArray();

}
class MyArray extends AbstractArray {
@Override
void inputArray() {
    Scanner scanner = new Scanner(System.in);

    System.out.print("Введите размер массива: ");
    int n = scanner.nextInt();

    int[] arr = new int[n];

    System.out.println("Введите элементы массива:");

    for (int i = 0; i < n; i++) {
        arr[i] = scanner.nextInt();
    }

    System.out.println("Массив:");

    for (int i = 0; i < n; i++) {
        System.out.print(arr[i] + " ");
    }
}


}
