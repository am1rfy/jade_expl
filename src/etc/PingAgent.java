package etc;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class PingAgent extends Agent {
    private Logger myLogger = Logger.getMyLogger(this.getClass().getName());

    public PingAgent() {
    }

    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("PingAgent");
        sd.setName(this.getName());
        dfd.setName(this.getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            PingAgent.WaitPingAndReplyBehaviour PingBehaviour = new PingAgent.WaitPingAndReplyBehaviour(this);
            this.addBehaviour(PingBehaviour);
        } catch (FIPAException var4) {
            this.myLogger.log(Logger.SEVERE, "Agent " + this.getLocalName() + " - Cannot register with DF", var4);
            this.doDelete();
        }

    }

    private class WaitPingAndReplyBehaviour extends CyclicBehaviour {
        public WaitPingAndReplyBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            ACLMessage msg = this.myAgent.receive();
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if (msg.getPerformative() == ACLMessage.INFORM || msg.getPerformative()==ACLMessage.REQUEST) {
                    String content = msg.getContent();
                    if (content != null && content.indexOf("ping") != -1) {
                        PingAgent.this.myLogger.log(Logger.INFO, "Agent " + PingAgent.this.getLocalName() + " - Received PING Request from " + msg.getSender().getLocalName());
                        reply.setPerformative(7);
                        reply.setContent("pong");
                    } else {
                        PingAgent.this.myLogger.log(Logger.INFO, "Agent " + PingAgent.this.getLocalName() + " - Unexpected request [" + content + "] received from " + msg.getSender().getLocalName());
                        reply.setPerformative(14);
                        reply.setContent("( UnexpectedContent (" + content + "))");
                    }
                } else {
                    PingAgent.this.myLogger.log(Logger.INFO, "Agent " + PingAgent.this.getLocalName() + " - Unexpected message [" + ACLMessage.getPerformative(msg.getPerformative()) + "] received from " + msg.getSender().getLocalName());
                    reply.setPerformative(10);
                    reply.setContent("( (Unexpected-act " + ACLMessage.getPerformative(msg.getPerformative()) + ") )");
                }

                PingAgent.this.send(reply);
            } else {
                this.block();
            }

        }
    }
}
