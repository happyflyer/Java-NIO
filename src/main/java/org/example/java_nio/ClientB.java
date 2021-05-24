package org.example.java_nio;

import java.io.IOException;

/**
 * @author lifei
 */
public class ClientB {
    public static void main(String[] args) throws IOException {
        new NioClient().start("ClientB");
    }
}
