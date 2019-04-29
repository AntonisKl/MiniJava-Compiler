class MyExample {

    public static void main(String[] args) {
        A a;
        B b;
        a = new A();
        b = a;
    }

}

class B {}

class A extends B {
    public int foo() {
        return 1;
    }
}
