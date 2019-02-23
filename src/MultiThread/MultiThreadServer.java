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
                    else if(strFromClient.startsWith("G:")) {
                        String str = strFromClient.split(":")[1];
                        groupChat(str);
                    }
                    //私聊功能  P:client1-hello(发给client1的信息)
                    else if(strFromClient.startsWith("P:")) {
                        //获取私聊的对象名与内容
                        String userName = strFromClient.split(":")[1].split("-")[0];
                        String str = strFromClient.split(":")[1].split("-")[1];
                        privateChat(str,userName);
                    }
                    //退出聊天室  client1:byebye (client1退出聊天室)
                    else if(strFromClient.contains("byebye")) {
                        //获取客户端信息
                        String userName = strFromClient.split(":")[0];
                        //该用户退出聊天室
                        quitChatRoom(userName);
                        break;
                    }
                    else {
                        help();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //新客户注册  userName:XXX
        private void userRegister(String userName, Socket client) {
            PrintStream printStream = null;
            try {
                //获取输出流
                printStream = new PrintStream(client.getOutputStream(),true,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(isAlive(userName)){
                printStream.println("该用户名已被注册...");
                return;
            }
            if(isRegister(client)) {
                printStream.println("该客户端已经注册 ...");
                return;
            }

            clientLists.put(userName,client);
            printStream.println("注册成功！！");
            printStream.println("当前在线用户数为："+clientLists.size());
            System.out.println("用户"+userName+"上线了！！");
            System.out.println("当前在线用户数为："+clientLists.size());
        }

        //群聊   G:hello world
        private void groupChat(String msg) {
            //判断当前客户端是否已经注册
            PrintStream printStream = null;
            try {
               printStream = new PrintStream(client.getOutputStream(),true,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //当前客户端还没有注册
            if(!isRegister(this.client)) {
                printStream.println("当前客户端还没有注册，请先进行注册 ...");
                return;
            }
            //当前客户端已经注册
            if(1 == clientLists.size()) {
                printStream.println("当前聊天室只有你一人 ...");
            }
            else{
                //将Map转换为Set
                Set<Map.Entry<String,Socket>> clientEntry = clientLists.entrySet();
                //迭代器遍历
                Iterator<Map.Entry<String,Socket>> iterator = clientEntry.iterator();
                while(iterator.hasNext()) {
                    //取出每一个客户端实体
                    Map.Entry<String,Socket> client = iterator.next();
                    //拿到客户端输出流输出群聊信息
                    try {
                        PrintStream print = new PrintStream(client.getValue().getOutputStream(),true,"UTF-8");
                        print.println("群聊信息为："+msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //私聊  P:userName:hello world
        private void privateChat(String msg,String userName) {
            PrintStream printStream = null;
            try {
                printStream = new PrintStream(this.client.getOutputStream(),true,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //判断当前客户端是否已经注册
            if(!isRegister(this.client)) {
                printStream.println("当前客户端还未注册，请先进行注册 ...");
                return;
            }
            //判断私聊对象是否存在
            if(!isAlive(userName)) {
                printStream.println("您要发送的客户端已经下线");
                return;
            }
            //判断私聊对象是否是自己
            if(isSelf(userName)) {
                printStream.println("请选择正确的私聊对象");
                return;
            }
            //取出userName对应的Socket
            Socket client = clientLists.get(userName);
            try {
                //获取输出流
                PrintStream print = new PrintStream(client.getOutputStream(),true,"UTF-8");
                printStream.println("私聊信息为：" +msg);
                print.println("私聊信息为：" +msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //退出聊天室  userName:byebye
        private void quitChatRoom(String userName) {
            PrintStream printStream = null;
            try {
                printStream = new PrintStream(client.getOutputStream(),true,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //判断该用户名是否存在
            if(!isAlive(userName)) {
                printStream.println("要关闭的客户端不存在");
                return;
            }
            //判断当前客户端是否是要关闭的客户端
            if(!isSelf(userName)) {
                printStream.println("要关闭的客户端不是当前客户端 ...");
                return;
            }
            printStream.println("客户端退出聊天室 ...");
            clientLists.remove(userName);
            System.out.println("用户"+userName+"已经下线");
            System.out.println("当前聊天室人数为："+clientLists.size());
        }

        //判断输入的客户端是否是当前客户端
        private boolean isSelf(String userName) {
            String thisUserName = "";
            int port = this.client.getPort();

            //将Map转换为Set
            Set<Map.Entry<String,Socket>> clientEntry = clientLists.entrySet();
            //迭代器遍历
            Iterator<Map.Entry<String,Socket>> iterator = clientEntry.iterator();
            while(iterator.hasNext()) {
                //取出每一个客户端实体
                Map.Entry<String, Socket> client = iterator.next();
                //判断当前用户名是否已经存在，若存在返回true，否则返回false
                if(port == client.getValue().getPort()) {
                    if(userName.equals(client.getKey())) {
                        return true;
                    }
                    else{
                        return false;
                    }
                }
            }
            return false;
        }

        //判断该用户名是否已经存在
        private boolean isAlive(String userName) {
            //将Map转换为Set
            Set<Map.Entry<String,Socket>> clientEntry = clientLists.entrySet();
            //迭代器遍历
            Iterator<Map.Entry<String,Socket>> iterator = clientEntry.iterator();
            while(iterator.hasNext()) {
                //取出每一个客户端实体
                Map.Entry<String, Socket> client = iterator.next();
                //判断当前用户名是否已经存在，若存在返回true，否则返回false
                if(userName.equals(client.getKey())) {
                    return true;
                }
            }
            return false;
        }

        //判断当前socket是否已经注册过，若注册过，返回true，否则返回false
        private boolean isRegister(Socket client) {
            //将Map转换为Set
            Set<Map.Entry<String,Socket>> clientEntry = clientLists.entrySet();
            //迭代器遍历
            Iterator<Map.Entry<String,Socket>> iterator = clientEntry.iterator();
            while(iterator.hasNext()) {
                //取出每一个客户端实体
                Map.Entry<String, Socket> socket = iterator.next();
                //判断socket是否已经注册，若已经注册返回true，否则返回false
                if(client.getPort() == socket.getValue().getPort()) {
                    return true;
                }
            }
            return false;
        }

        //输入标准，当输入命令错误时执行
        private void help() {
            try {
                PrintStream printStream = new PrintStream(client.getOutputStream());
                printStream.println("输入标准：");
                printStream.println("注册：userName：XXX");
                printStream.println("群聊：G：hello world");
                printStream.println("私聊：P：ToUserName-hello world");
                printStream.println("退出聊天室：userName：byebye");
            } catch (IOException e) {
                e.printStackTrace();
            }
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
