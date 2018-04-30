package config.logger;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Victor de la Cruz
 * @version 1.0.0
 * Class to visualize rules activation for Rule Engine Processor
 * */
public class CustomAgendaEventListener implements org.kie.api.event.rule.AgendaEventListener {

    private final Logger logger = LoggerFactory.getLogger(CustomAgendaEventListener.class);


    @Override
    public void matchCreated(MatchCreatedEvent event) {

    }

    @Override
    public void matchCancelled(MatchCancelledEvent event) {

    }

    @Override
    public void beforeMatchFired(BeforeMatchFiredEvent event) {
        logger.info("Rule match: {}",event.getMatch().getRule().getName());
    }

    @Override
    public void afterMatchFired(AfterMatchFiredEvent event) {
        logger.info("Leave match fired {}",event.getMatch().getRule().getName());
    }

    @Override
    public void agendaGroupPopped(AgendaGroupPoppedEvent event) {

    }

    @Override
    public void agendaGroupPushed(AgendaGroupPushedEvent event) {

    }

    @Override
    public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {

    }

    @Override
    public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {

    }

    @Override
    public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {

    }

    @Override
    public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {

    }
}
