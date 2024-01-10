import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.Arrays;
import java.util.logging.Level;

//параметры, оценка предложений от разработчиков и выбор лучшего, удаляется ли агент после


//посылает запрос всем программистам
public class TaskAgent extends Agent {
    private Logger myLogger = Logger.getMyLogger(this.getClass().getName());

    boolean isFree=true;
    TypeOfTask type=TypeOfTask.Easy;
    int payment=0;//оплата за выполнение

    private AID[] devAgents;
    String infoString; //параметры задачи для отсылки разработчикам

    @Override
    protected void takeDown() {
        System.out.println("TaskAgent " + this.getAID().getName() + " terminating.");
    }

    @Override
    protected void setup() {
        Object[] args=getArguments();
        if(args!=null && args.length==2) {
            type=(TypeOfTask)args[0];
            payment=(int)args[1];
        }
        infoString=type+";"+payment+";";
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("TaskAgent");
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
            myLogger.log(Level.INFO, "TaskAgent " + this.getLocalName() + " was registered");
        }
        catch(FIPAException e) {
            this.myLogger.log(Level.SEVERE, "TaskAgent " + this.getLocalName() + " - Cannot register with DF", e);
            e.printStackTrace();
            doDelete();
        }
        this.addBehaviour(new TickerBehaviour(this, 1000L) {//через каждые 60000 мс (1000 мс в с)
            @Override
            protected void onTick() {
                if (isFree) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("DeveloperAgent"); // ищем разработчиков
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(this.myAgent, template); // сам поиск, в результате все такие агенты
                        devAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i)
                        {
                            devAgents[i] = result[i].getName(); // берем имя и складываем
                        }
                    }
                    catch(FIPAException e) {
                        e.printStackTrace();
                        myLogger.log(Level.SEVERE, myAgent.getName()+": DeveloperAgents not found");
                    }
                    this.myAgent.addBehaviour(new SendRequestsBehaviour());
                }
            }
        });

    }

    private class SendRequestsBehaviour extends CyclicBehaviour {
        private int step=0;
        private MessageTemplate mt;
        private int repliesCnt=0;
        private AID bestDeveloper=null;
        private int maxExperience=0;
        private TypeOfDeveloper maxType=TypeOfDeveloper.Junior;

        private void compareDevelopers(String[] devParams, AID developer) {
            TypeOfDeveloper typeDev=TypeOfDeveloper.valueOf(devParams[0]);
            int experience=Integer.parseInt(devParams[1]);
            //тип важнее опыта
            if(bestDeveloper==null || typeDev.ordinal()>maxType.ordinal() || experience>maxExperience) {
                bestDeveloper=developer;
                maxExperience=experience;
                maxType=typeDev;
            }
        }

        private int calculateRateOfDev(String[] params) {
            int rate=0;

            return rate;
        }

        public void action() {
            ACLMessage reply;
            switch(step) {
                    case 0://рассылка запросов
                        ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE); // делаем запрос ко всем разработчикам
                        for (int i = 0; i < devAgents.length; ++i)
                        {
                            cfp.addReceiver(devAgents[i]); // и добавляем их в получателей
                        }
                        cfp.setConversationId("Task-Devs"); // это задаем для поиска сообщений
                        cfp.setReplyWith("cfp"+System.currentTimeMillis());
                        cfp.setContent(infoString);
                        myAgent.send(cfp); // отправляем всем сообщение про нас

                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Task-Devs"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith())); //шаблон для ответов
                        step = 1; // переходим на следующий этап отношений
                        block(); // замораживаем отношения до тех пор, пока не придет ответ
                        break;
                    case 1: //получение ответа
                        reply = this.myAgent.receive(this.mt);
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {//положительный ответ
                                String replyFromDev = reply.getContent();//ответ разработчика
                                String[] devParams=replyFromDev.split(";");
                                //сравним этого разработчика с лучшим на данный момент
                                compareDevelopers(devParams, reply.getSender());//меняет bestDeveloper
                            }
                            ++this.repliesCnt;
                            if (this.repliesCnt >= devAgents.length) {
                                this.step = 2;
                            }
                        }
                        else {//ждем ответы
                            this.block();
                        }
                        break;
                    case 2: //отправляем ответ
                        ACLMessage order = new ACLMessage(ACLMessage.CONFIRM);
                        order.addReceiver(this.bestDeveloper);
                        order.setContent(infoString);//???????????????????????????????????????????????????????????????????????????
                        order.setConversationId("Task-Devs");
                        order.setReplyWith("order" + System.currentTimeMillis());
                        this.myAgent.send(order);

                        this.mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Task-Devs"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                        this.step = 3;
                        break;
                    case 3: //окончательный ответ от разработчика
                        reply = myAgent.receive(mt); // получаем сообщение с нужными характеристиками
                        if (reply != null) // если нашелся ответ
                        {
                            String info = reply.getContent();
                            //String[] paramDev=info.split(";");
                            if (reply.getPerformative() == ACLMessage.INFORM) // если тип этого сообщения - информационное
                            {
                                if(reply.getContent().equals("Success")) // если разработчик взял задачу
                                {
                                    isFree = false;
                                    myLogger.log(Level.INFO,"Task "+ type + payment +" will be performed by Developer "+maxType + maxExperience);
                                    //удалить агента????????????????
                                    myAgent.doDelete();
                                    step=4;
                                }
                                else
                                {
                                    myLogger.log(Level.INFO, "" + type + payment +": Developer " + maxType + maxExperience +" is busy");
                                }
                            }
                        }
                        else
                        {
                            block(); //ждем ответов
                        }
                        break;
                    default: break;
            }
        }
    }
}


