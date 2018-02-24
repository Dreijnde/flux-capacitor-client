package io.fluxcapacitor.javaclient.givenwhenthen;

import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.MockException;
import io.fluxcapacitor.javaclient.test.TestFixture;
import io.fluxcapacitor.javaclient.tracking.handling.HandleCommand;
import io.fluxcapacitor.javaclient.tracking.handling.HandleEvent;
import io.fluxcapacitor.javaclient.tracking.handling.HandleQuery;
import lombok.Value;
import org.junit.Test;
import org.mockito.InOrder;

import static org.hamcrest.CoreMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;

public class GivenWhenThenTest {

    private final CommandHandler commandHandler = spy(new CommandHandler());
    private final EventHandler eventHandler = spy(new EventHandler());
    private final QueryHandler queryHandler = spy(new QueryHandler());
    private final TestFixture subject = TestFixture.create(commandHandler, eventHandler, queryHandler);

    @Test
    public void testExpectNoEventsAndNoResult() {
        subject.givenNoPriorActivity().whenCommand(new YieldsNoResult()).expectNoEvents().expectNoResult();
    }

    @Test
    public void testExpectResultButNoEvents() {
        subject.givenNoPriorActivity().whenCommand(new YieldsResult()).expectNoEvents().expectResult(isA(String.class));
    }

    @Test
    public void testExpectExceptionButNoEvents() {
        subject.givenNoPriorActivity().whenCommand(new YieldsException()).expectNoEvents().expectException(isA(MockException.class));
    }

    @Test
    public void testExpectEventButNoResult() {
        YieldsEventAndNoResult command = new YieldsEventAndNoResult();
        subject.givenNoPriorActivity().whenCommand(command).expectOnlyEvents(command).expectNoResult();
    }

    @Test
    public void testExpectResultAndEvent() {
        YieldsEventAndResult command = new YieldsEventAndResult();
        subject.givenNoPriorActivity().whenCommand(command).expectOnlyEvents(command).expectResult(isA(String.class));
    }

    @Test
    public void testExpectExceptionAndEvent() {
        YieldsEventAndException command = new YieldsEventAndException();
        subject.givenNoPriorActivity().whenCommand(command).expectOnlyEvents(command).expectException(isA(MockException.class));
    }

    @Test
    public void testWithGivenCommandsAndResult() {
        subject.givenCommands(new YieldsNoResult()).whenCommand(new YieldsResult()).expectResult(isA(String.class)).expectNoEvents();
    }

    @Test
    public void testWithGivenCommandsAndNoResult() {
        subject.givenCommands(new YieldsResult()).whenCommand(new YieldsNoResult()).expectNoResult().expectNoEvents();
    }

    @Test
    public void testWithGivenCommandsAndEventsFromGiven() {
        subject.givenCommands(new YieldsEventAndResult()).whenCommand(new YieldsNoResult()).expectNoResult().expectNoEvents();
    }

    @Test
    public void testWithGivenCommandsAndEventsFromCommand() {
        YieldsEventAndNoResult command = new YieldsEventAndNoResult();
        subject.givenCommands(new YieldsNoResult()).whenCommand(command).expectNoResult().expectEvents(command);
    }

    @Test
    public void testWithMultipleGivenCommands() {
        YieldsEventAndNoResult command = new YieldsEventAndNoResult();
        subject.givenCommands(new YieldsNoResult(), new YieldsResult(), command, command).whenCommand(command).expectNoResult().expectOnlyEvents(command);
    }

    @Test
    public void testAndGivenCommands() {
        subject.givenCommands(new YieldsResult()).andGivenCommands(new YieldsEventAndNoResult()).whenCommand(new YieldsNoResult()).expectNoResult().expectNoEvents();
        InOrder inOrder = inOrder(commandHandler);
        inOrder.verify(commandHandler).handle(new YieldsResult());
        inOrder.verify(commandHandler).handle(new YieldsEventAndNoResult());
        inOrder.verify(commandHandler).handle(new YieldsNoResult());
    }

    @Test
    public void testExpectCommands() {
        subject.whenEvent("some event").expectCommands(new YieldsNoResult()).expectNoEvents().expectNoResult();
    }

    @Test
    public void testExpectCommandsAndIndirectEvents() {
        subject.whenEvent(123).expectNoResult().expectCommands(new YieldsEventAndResult()).expectEvents(new YieldsEventAndResult());
    }

    @Test
    public void testQuery() {
        subject.whenQuery("bla").expectResult("bla");
    }

    private static class CommandHandler {
        @HandleCommand
        public void handle(YieldsNoResult command) {
            //no op
        }

        @HandleCommand
        public String handle(YieldsResult command) {
            return "result";
        }

        @HandleCommand
        public void handle(YieldsException command) {
            throw new MockException();
        }

        @HandleCommand
        public void handle(YieldsEventAndNoResult command) {
            FluxCapacitor.publishEvent(command);
        }

        @HandleCommand
        public String handle(YieldsEventAndResult command) {
            FluxCapacitor.publishEvent(command);
            return "result";
        }

        @HandleCommand
        public void handle(YieldsEventAndException command) {
            FluxCapacitor.publishEvent(command);
            throw new MockException();
        }
    }

    private static class EventHandler {
        @HandleEvent
        public void handle(String event) {
            FluxCapacitor.sendCommand(new YieldsNoResult());
        }

        @HandleEvent
        public void handle(Integer event) throws Exception {
            FluxCapacitor.sendCommand(new YieldsEventAndResult()).get();
        }
    }

    private static class QueryHandler {
        @HandleQuery
        public String handle(String query) {
            return query;
        }
    }

    @Value
    private static class YieldsNoResult {
    }

    @Value
    private static class YieldsResult {
    }

    @Value
    private static class YieldsException {
    }

    @Value
    private static class YieldsEventAndNoResult {
    }

    @Value
    private static class YieldsEventAndResult {
    }

    @Value
    private static class YieldsEventAndException {
    }

}