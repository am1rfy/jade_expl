import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.logging.Level;

//сравнение задач, еще параметры

public class DeveloperAgent extends Agent {
    private Logger myLogger = Logger.getMyLogger(this.getClass().getName());

    boolean isBusy=false;//свободен ли в данный момент
    int experience=0; //опыт работы
    TypeOfDeveloper type=TypeOfDeveloper.Junior;
    String infoString;


    ArrayList<String> tasks=new ArrayList<String>();

    @Override
    protected void setup() {
        Object[] args=getArguments();
        if(args!=null && args.length==2) {
            type=(TypeOfDeveloper)args[0];
            experience=(int)args[1];
        }

        infoString=type+";"+experience+";";
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("DeveloperAgent");
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
            myLogger.log(Level.INFO, "DeveloperAgent was registered");
        }
        catch(FIPAException e) {
            this.myLogger.log(Level.SEVERE, "Agent " + this.getLocalName() + " - Cannot register with DF", e);
            e.printStackTrace();
            doDelete();
        }
        addBehaviour(new RequestsServer());
        addBehaviour(new ReceiveJobServer());
    }

    protected void takeDown()
    {
        try
        {
            DFService.deregister(this);
        }
        catch (FIPAException fe)
        {
            fe.printStackTrace();
        }
        myLogger.log(Level.INFO,"DeveloperAgent "+getAID().getName()+" terminating");
    }

    private class ReceiveJobServer extends CyclicBehaviour {
        private ReceiveJobServer() {
        }

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            ACLMessage msg = this.myAgent.receive(mt);
            if (msg != null) {
                String infoTask = msg.getContent();
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                String info = msg.getContent();
                String[] paramTask=info.split(";");
                if(isBusy)
                    reply.setContent("Reject");
                else {
                    //сравнение задач???????????

                    reply.setContent("Success");
                    isBusy=true;
                    tasks.add(infoTask);
                    myLogger.log(Level.INFO, type + " " + experience+" have taken "+paramTask[0] + " " + paramTask[1]);
                    // FIXME

                    String taskType = infoTask.split(";")[0];
                    long timeout = 0;
                    if (taskType == "Easy") {
                        timeout = 1000L;
                    } else if (taskType == "Medium") {
                        timeout = 2000L;
                    } else {
                        timeout = 3000L;
                    }

                    addBehaviour(new JobWorkingBehaviour(this.myAgent, timeout));
                }

                this.myAgent.send(reply);
            } else {
                this.block();
            }

        }
    }

    private class RequestsServer extends CyclicBehaviour {
        private RequestsServer() {
        }

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = this.myAgent.receive(mt);//получаем запросы
            if (msg != null) {
                String info = msg.getContent();
                String[] paramTask=info.split(";");
                ACLMessage reply = msg.createReply();
                if(isBusy) {//разработчик занят
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    reply.setContent("Developer is busy");
                    myLogger.log(Level.INFO, paramTask[0] + " " + paramTask[1]+": Developer "+type + " " + experience+" is busy");////////////////////////////////
                }
                else {

                    TypeOfTask typeTask=TypeOfTask.valueOf(paramTask[0]);
                    System.out.println(paramTask[0] + " " + paramTask[1] + "; " + type + " " + experience);
                    //оценка собственных возможностей
                    boolean isAccept=false;
                    if(typeTask==TypeOfTask.Difficult) {
                        if (type == TypeOfDeveloper.Senior || (type == TypeOfDeveloper.Middle && experience > 5))
                            //только Senior или Middle с опытом более 5 лет могут брать такие задачи
                            isAccept = true;
                    }
                    else if(typeTask==TypeOfTask.Medium) {
                        if (type == TypeOfDeveloper.Middle || type == TypeOfDeveloper.Senior || experience > 2) //Junior с опытом более 2 лет может брать такие задачи
                            isAccept = true;
                    }
                    else if(typeTask==TypeOfTask.Easy && (type==TypeOfDeveloper.Junior || type==TypeOfDeveloper.Middle))
                        isAccept=true;
                    //подготовка ответа
                    if(isAccept) {
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent(infoString);
                        myLogger.log(Level.WARNING, type + " " + experience + " accept proposal from "+ paramTask[0] + " " + paramTask[1]);
                    }
                    else {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("Reject");
                        myLogger.log(Level.OFF, type + " " + experience+" reject proposal from "+ paramTask[0] + " " + paramTask[1]);
                    }
                }
                this.myAgent.send(reply);//отправка ответа
            } else {
                this.block();//ожидание запроса
            }
        }
    }

    private class JobWorkingBehaviour extends SimpleBehaviour { //моделирует занятость разработчика после получения задачи
        private long timeout, wakeupTime;
        private boolean finished = false;

        public JobWorkingBehaviour(Agent a, long timeout) {
            super(a);
            this.timeout = timeout;
        }

        public void onStart() {
            wakeupTime = System.currentTimeMillis() + timeout;
        }

        public void action()
        {
            long dt = wakeupTime - System.currentTimeMillis();
            if (dt <= 0) {
                finished = true;
                handleElapsedTimeout();
            } else
                block(dt);
        }

        protected void handleElapsedTimeout() //изменяем значение флага
        {
            isBusy=false;
        }

        public boolean done() { return finished; }
    }
}
