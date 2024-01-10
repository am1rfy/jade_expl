package etc;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class PongAgent extends Agent {
    private Logger myLogger = Logger.getMyLogger(this.getClass().getName());

    public PongAgent() {
    }

    private void sendMessage() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType( "PingAgent" );
        dfd.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            ACLMessage msg= new ACLMessage(ACLMessage.REQUEST);
            msg.setContent("ping");
            for(DFAgentDescription agent: result) {
                msg.addReceiver(agent.getName());
            }
            send(msg);
        }catch(FIPAException e) {

        }
    }

    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("PongAgent");
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            sendMessage();
            WaitPongAndReplyBehaviour pongBehaviour = new WaitPongAndReplyBehaviour(this);
            addBehaviour(pongBehaviour);
        } catch (FIPAException var4) {
            this.myLogger.log(Logger.SEVERE, "Agent " + this.getLocalName() + " - Cannot register with DF", var4);
            this.doDelete();
        }
    }

    protected void takeDown()
    {
        try { DFService.deregister(this); }
        catch (Exception e) {}
    }

    private class WaitPongAndReplyBehaviour extends CyclicBehaviour {
        public WaitPongAndReplyBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            ACLMessage msg = this.myAgent.receive();
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (msg.getPerformative() == ACLMessage.REQUEST || msg.getPerformative() == ACLMessage.INFORM) {
                    String content = msg.getContent();
                    String senderName = msg.getSender().getLocalName(); // Имя отправителя
                    String agentType = PongAgent.this.getLocalName(); // Тип агента (PongAgent)

                    if (content != null && content.indexOf("pong") != -1) {
                        PongAgent.this.myLogger.log(Logger.INFO, "Agent " + agentType + " " + senderName + " - Received PONG Request from " + msg.getSender().getLocalName() + " with characteristics: " + agentType);
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("ping");
                    } else {
                        PongAgent.this.myLogger.log(Logger.INFO, "Agent " + agentType + " " + senderName + " - Unexpected request [" + content + "] received from " + msg.getSender().getLocalName() + " with characteristics: " + agentType + ", ...");
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("( UnexpectedContent (" + content + "))");
                    }
                } else {
                    PongAgent.this.myLogger.log(Logger.INFO, "Agent " + PongAgent.this.getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(msg.getPerformative()) + "] received from " + msg.getSender().getLocalName());
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("( (Unexpected-act " + ACLMessage.getPerformative(msg.getPerformative()) + ") )");
                }

                PongAgent.this.send(reply);
            } else {
                this.block();
            }
        }
    }
}