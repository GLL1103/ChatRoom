package MultiThread;


import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//多线程服务器端
public class MultiThreadServer {
    private static Map<String,Socket> clientLists = new ConcurrentHashMap<>();
    //专门用来处理每个客户端的输入，输出请求
    private static class ExecuteClientRequest implements Runnable {
        private Socket client;
        public ExecuteClientRequest(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                //获取用户输入流，读取用户发来的信息
                Scanner in = new Scanner(client.getInputStream());
                String strFromClient = "";
                while(true) {
                    if(in.hasNext()) {
                        strFromClient = in.nextLine();
                        //Windows下消除用户输入自带的\r
                        //将\r替换为空字符串
                        Pattern pattern = Pattern.compile("\r");
                        Matcher matcher = pattern.matcher(strFromClient);
                        strFromClient = matcher.replaceAll("");
                    }
                    //注册功能   userName:client1
                    if(strFromClient.startsWith("userName:")) {
                        //获取到用户名
                        String userName = strFromClient.split(":")[1];
                        userRegister(userName,client);
                    }
                    //群聊功能   G:hello world
                    if(strFromClient.startsWith("G:")) {
                        String str = strFromClient.split(":")[1];
                        groupChat(str);
                    }
                    //私聊功能  P:client1-hello(发给client1的信息)
                    if(strFromClient.startsWith("P:")) {
                        //获取私聊的对象名与内容
                        String userName = strFromClient.split(":")[1].split("-")[0];
                        String str = strFromClient.split(":")[1].split("-")[1];
                        privateChat(str,userName);
                    }
                    //退出聊天室  client1:byebye (client1退出聊天室)
                    if(strFromClient.contains("byebye")) {
                        //获取客户端信息
                        String userName = strFromClient.split(":")[0];
                        //该用户退出聊天室
                        quitChatRoom(userName);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //新客户注册
        private void userRegister(String userName, Socket client) {
            PrintStream printStream = null;
            try {
                //获取输出流
                printStream = new PrintStream(client.getOutputStream(),true,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            clientLists.put(userName,client);
            printStream.println("注册成功！！");
            printStream.println("当前在线用户数为："+clientLists.size());
            System.out.println("用户"+userName+"上线了！！");
            System.out.println("当前在线用户数为："+clientLists.size());
        }

        private boolean isAlive(String userName) {
            //将Map转换为Set
            Set<Map.Entry<String,Socket>> clientEntry = clientLists.entrySet();
            //迭代器遍历
            Iterator<Map.Entry<String,Socket>> iterator = clientEntry.iterator();
            while(iterator.hasNext()) {
                //取出每一个客户端实体
                Map.Entry<String, Socket> client = iterator.next();
                System.out.println(client.getValue());
                if(userName.equals(client.getValue())) {
                    return true;
                }
            }
            return false;
        }
        //群聊
        private void groupChat(String msg) {
            //将Map转换为Set
            Set<Map.Entry<String,Socket>> clientEntry = clientLists.entrySet();
            //迭代器遍历
            Iterator<Map.Entry<String,Socket>> iterator = clientEntry.iterator();
            while(iterator.hasNext()) {
                //取出每一个客户端实体
                Map.Entry<String,Socket> client = iterator.next();
                //拿到客户端输出流输出群聊信息
                try {
                    PrintStream printStream = new PrintStream(client.getValue().getOutputStream());
                    printStream.println("群聊信息为："+msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //私聊
        private void privateChat(String msg,String userName) {
            //取出userName对于的Socket
            Socket client = clientLists.get(userName);
            //获取输出流
            try {
                PrintStream printStream = new PrintStream(client.getOutputStream());
                printStream.println("私聊信息为：" +msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //退出聊天室
        private void quitChatRoom(String userName) {
            clientLists.remove(userName);
            System.out.println("用户"+userName+"已经下线");
            System.out.println("当前聊天室人数为："+clientLists.size());
        }
    }
    public static void main(String[] args) throws Exception{
        //建立基站
        ServerSocket server = new ServerSocket(6666);
        //使用线程池来同时处理多个客户端连接
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        System.out.println("等待客户端连接 ...");
        for(int i = 0;i<20;++i) {
            Socket client = server.accept();
            System.out.println("有新客户端连接，端口号为："+client.getPort());
            executorService.submit(new ExecuteClientRequest(client));
        }
        //关闭线程池与服务端
        executorService.shutdown();
        server.close();
    }
}
