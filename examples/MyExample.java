class MyExample {

    public static void main(String[] args) {
        A a;
        B b;
        // a = new A();
        // b = a;
        // System.out.println(b);
    }

}

class A {
    int i;
    public int foo() {
        return 1;
    }

    public boolean fa() {
        return false;
    }
}

class B extends A {
    A type;
    int i;
    boolean flag;

    public int foo() {
        return 1;
    }

    public boolean bla() {
        return true;
    }
}