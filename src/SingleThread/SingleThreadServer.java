package SingleThread;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

//服务器端
public class SingleThreadServer {
    public static void main(String[] args) throws Exception{
        // 1.建立基站,通过ServerSocket的构造方法指定端口号
        ServerSocket server = new ServerSocket(6666);
        // 2.等待客户端连接，若没有客户端连接，阻塞
        System.out.println("等待客户端连接 ...");
        Socket socket = server.accept();
        // 3.连接后，进行数据的输入输出
        PrintStream printStream = new PrintStream(socket.getOutputStream(),true,"UTF-8");
        printStream.println("I am Server!!");
        Scanner scanner = new Scanner(socket.getInputStream());
        if(scanner.hasNext()) {
            System.out.println(scanner.nextLine());
        }
        // 4.关闭流
        printStream.close();
        scanner.close();
        server.close();
    }
}
