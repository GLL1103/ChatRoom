package SingleThread;

import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

//客户端
public class SingleThreadClient {
    public static void main(String[] args) throws Exception{
        // 1. 连接服务器,本机IP127.0.0.1，端口号-服务端指定
        Socket socket = new Socket("127.0.0.1",6666);
        // 2. 连接后，进行数据的输入输出
        Scanner scanner = new Scanner(socket.getInputStream());
        if(scanner.hasNext()) {
            System.out.println(scanner.nextLine());
        }
        //客户端与服务器端编码方式必须一致，否则会出现乱码
        PrintStream printStream = new PrintStream(socket.getOutputStream(),true,"UTF-8");
        printStream.println("I am Client!!");
        // 3. 关闭流
        scanner.close();
        printStream.close();
        socket.close();
    }
}
