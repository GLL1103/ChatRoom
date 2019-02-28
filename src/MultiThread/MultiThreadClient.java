package MultiThread;


import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

//从服务器接收信息
class ReadFromServer implements Runnable {
    private Socket client;
    public ReadFromServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            //获取客户端的输入流
            Scanner scanner = new Scanner(client.getInputStream());
            while(true) {
                if(client.isClosed()) {
                    System.out.println("客户端已退出 ...");
                    scanner.close();
                    break;
                }
                String str = "";
                if(scanner.hasNext()) {
                    str = scanner.nextLine();
                    System.out.println("从服务器发来的消息是:"+str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//向服务器发送信息
class SendToServer implements Runnable {
    private Socket client;
    public SendToServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            //获取客户端的输出流
            PrintStream printStream = new PrintStream(client.getOutputStream(),true,"UTF-8");
            //获取用户输入
            Scanner scanner = new Scanner(System.in);
            while(true) {
                System.out.println("请输入要发送的内容");
                String strFromUser = "";
                if(scanner.hasNext()) {
                    strFromUser = scanner.nextLine();
                }
                //向服务器发送信息
                printStream.println(strFromUser+":"+this.client.getLocalPort());
                if(strFromUser.contains("byebye")) {
                    printStream.close();
                    scanner.close();
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

//多线程客户端
public class MultiThreadClient {
    public static void main(String[] args) throws Exception{
        Socket client = new Socket("127.0.0.1",6666);
        Thread readThread = new Thread(new ReadFromServer(client));
        Thread sendThread = new Thread(new SendToServer(client));
        readThread.start();
        sendThread.start();
    }
}
