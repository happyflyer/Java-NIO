package org.example.java_nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO 服务器端
 *
 * @author lifei
 */
public class NioServer {

    /**
     * 启动
     *
     * @throws IOException IOExceptions
     */
    public void start() throws IOException {
        /*
          1. 创建Selector
         */
        Selector selector = Selector.open();
        /*
          2. 通过ServerSocketChannel查u你更加爱你channel
         */
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        /*
          3. 为channel绑定监听端口
         */
        serverSocketChannel.bind(new InetSocketAddress(8000));
        /*
          4. 设置Channel为非阻塞模式
         */
        serverSocketChannel.configureBlocking(false);
        /*
          5. 将Channel注册到Selector上，监听连接事件
         */
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器端启动成功！");
        /*
          6. 循环等待新接入的连接
         */
        for (; ; ) {
            /*
              获取可用channel数量
             */
            int readChannels = selector.select();
            if (readChannels == 0) {
                continue;
            }
            /*
              获取可用channel地集合
             */
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                /*
                  selectionKey实例
                 */
                SelectionKey selectionKey = (SelectionKey) iterator.next();
                /*
                  移除Set中当前selectionKey实例
                 */
                iterator.remove();
                /*
                  7. 根据对应状态，调用对应方法处理业务逻辑
                 */
                if (selectionKey.isAcceptable()) {
                    /*
                      如果是接入事件
                     */
                    acceptHandler(serverSocketChannel, selector);
                }
                if (selectionKey.isReadable()) {
                    /*
                      如果是可读事件
                     */
                    readHandler(selectionKey, selector);
                }
            }
        }
    }

    /**
     * 接入事件处理器
     *
     * @param serverSocketChannel serverSocketChannel
     * @param selector            selector
     * @throws IOException IOException
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel,
                               Selector selector)
            throws IOException {
        /*
          如果要是接入事件，创建socketChannel
         */
        SocketChannel socketChannel = serverSocketChannel.accept();
        /*
          将socketChannel设置为非阻塞工作模式
         */
        socketChannel.configureBlocking(false);
        /*
          将channel注册到selector上，监听可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);
        /*
          回复客户端提示信息
         */
        socketChannel.write(StandardCharsets.UTF_8
                .encode("你与聊天室里其他人都不是朋友关系，请注意隐私安全"));
    }

    /**
     * 可读事件处理器
     *
     * @param selectionKey selectionKey
     * @param selector     selector
     * @throws IOException IOException
     */
    private void readHandler(SelectionKey selectionKey, Selector selector)
            throws IOException {
        /*
          要从SelectionKey中获取到已经就绪的channel
         */
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        /*
          创建buffer
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        /*
          循环读取客户端请求信息
         */
        StringBuilder request = new StringBuilder();
        while (socketChannel.read(byteBuffer) > 0) {
            /*
              切换buffer为读模式
             */
            byteBuffer.flip();
            /*
              读取buffer中的内容
             */
            request.append(StandardCharsets.UTF_8.decode(byteBuffer));
        }
        /*
          将channel再次注册到selector上，监听他的可读事件
         */
        socketChannel.register(selector, SelectionKey.OP_READ);
        /*
          将客户端发送的请求信息，广播给其他客户端
         */
        if (request.length() > 0) {
            boardCast(selector, socketChannel, request.toString());
        }
    }

    private void boardCast(Selector selector, SocketChannel sourceChannel,
                           String request) {
        /*
          获取到所有已接入的客户端channel
         */
        Set<SelectionKey> selectionKeySet = selector.keys();
        /*
          循环向所有channel广播信息
         */
        selectionKeySet.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();
            /*
              剔除发消息的客户端
             */
            if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel) {
                try {
                    /*
                      将消息发送到targetChannel客户端
                     */
                    ((SocketChannel) targetChannel).write(StandardCharsets.UTF_8.encode(request));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer();
        nioServer.start();
    }
}
