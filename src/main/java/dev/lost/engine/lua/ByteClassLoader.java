package dev.lost.engine.lua;

class ByteClassLoader extends ClassLoader {

    public ByteClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public Class<?> define(String name, byte[] bytecode) {
        return defineClass(name, bytecode, 0, bytecode.length);
    }

}