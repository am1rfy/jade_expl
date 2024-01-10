import jade.core.Agent;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
//создает агентов
//имеет параметры: имя файла с задачами и имя файла с разработчиками
//E:\1\tasks.txt, E:\1\developers.txt
public class CreaterAgents extends Agent {
    private Logger myLogger = Logger.getMyLogger(this.getClass().getName());

    public CreaterAgents() {}

    @Override
    protected void setup() {
        myLogger.log(Level.INFO, "CreaterAgents is ready to create agents");
        Object[] args = this.getArguments();
        if (args != null && args.length == 2) {
            String filenameTasks=(String)args[0];//сначала tasks.txt
            String filenameDev=(String)args[1];
            AgentContainer container=getContainerController();
            try(BufferedReader input=new BufferedReader(new InputStreamReader(new FileInputStream(filenameDev), "cp1251"))) {
                String s; int count=1;
                while ((s=input.readLine())!=null) {
                    String name="DeveloperAgent"+count;
                    String[] substrs=s.split(";");//0-type, 1-experience
                    //создаем разработчика с параметрами в substrs
                    try{
                        AgentController controller = container.createNewAgent(name, "DeveloperAgent",
                                new Object[]{TypeOfDeveloper.valueOf(substrs[0]), Integer.parseInt(substrs[1])});
                        controller.start();
                    }
                    catch(StaleProxyException e) {
                        myLogger.log(Level.SEVERE, "Cant create agent"+name);
                    }
                    catch(NumberFormatException e) {
                        myLogger.log(Level.SEVERE, name+": Experience has wrong format");
                    }
                    count++;
                }
            }
            catch(IOException e) {
                myLogger.log(Level.SEVERE, "CreaterAgents: IOException - File with developers");
                e.printStackTrace();
                this.doDelete();
            }
            //создание задач
            try(BufferedReader input=new BufferedReader(new InputStreamReader(new FileInputStream(filenameTasks), "cp1251"))) {
                String s; int count=1;
                while ((s=input.readLine())!=null) {
                    String[] substrs=s.split(";");//0-type, 1-payment
                    //создаем задачу с параметрами в substrs
                    String name="TaskAgent"+count;
                    try{
                        AgentController controller = container.createNewAgent(name, "TaskAgent",
                                new Object[]{TypeOfTask.valueOf(substrs[0]), Integer.parseInt(substrs[1])});
                        controller.start();
                    }
                    catch(StaleProxyException e) {
                        myLogger.log(Level.SEVERE, "Cant create agent"+name);
                    }
                    catch(NumberFormatException e) {
                        myLogger.log(Level.SEVERE, name+": Payment has wrong format");
                    }
                    count++;
                }
            }
            catch(IOException e) {
                myLogger.log(Level.SEVERE, "CreaterAgents: IOException - File with tasks");
                e.printStackTrace();
                this.doDelete();
            }

        }
        else {
            myLogger.log(Level.SEVERE, "One of filenames is null");
        }
        this.doDelete();
    }
}
