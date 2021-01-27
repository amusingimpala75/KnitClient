package com.github.amusingimpala.knitClient.tests;

final class TestClass implements Comparable<Integer> {

    private final String name;

    TestClass(String name) {
        this.name = name;
    }

    private String getName() {
        return this.name;
    }

    protected final void doSomething() {
        System.out.println("Doing something");
    }

    @Override
    public int compareTo(Integer o) {
        return 0;
    }
}
