class MyExample {

    public static void main(String[] args) {
        A a;
        int i;
        a = new A();
        i =a.foo(4);
    }

}

class A {
    public int foo(int arg) {
        return 1;
    }
}
