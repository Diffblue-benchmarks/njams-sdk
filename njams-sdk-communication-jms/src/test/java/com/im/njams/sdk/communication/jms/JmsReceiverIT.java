/*
 */

/*
 */
package com.im.njams.sdk.communication.jms;

import com.faizsiegeln.njams.messageformat.v4.command.Instruction;
import com.faizsiegeln.njams.messageformat.v4.command.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.njams.sdk.communication.CommunicationFactory;
import org.junit.*;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import java.util.Properties;

//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;

/**
 * This class tests some methods that try to use a real JMS connection.
 *
 * @author krautenberg@integrationmatters.com
 * @version 4.0.5
 */
@Ignore
public class JmsReceiverIT {
    /*
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JmsReceiverIT.class);

    private JmsReceiverMock impl;

    private static final Properties FILLEDPROPS = new Properties();

    @BeforeClass
    public static void setUpClass() {
        FILLEDPROPS.put(CommunicationFactory.COMMUNICATION, "JMS");
        FILLEDPROPS.put(JmsConstants.INITIAL_CONTEXT_FACTORY,
                "com.tibco.tibjms.naming.TibjmsInitialContextFactory");
        FILLEDPROPS.put(JmsConstants.SECURITY_PRINCIPAL, "njams");
        FILLEDPROPS.put(JmsConstants.SECURITY_CREDENTIALS, "njams");
        FILLEDPROPS.put(JmsConstants.PROVIDER_URL, "tibjmsnaming://vslems01:7222");
        FILLEDPROPS.put(JmsConstants.CONNECTION_FACTORY, "ConnectionFactory");
        FILLEDPROPS.put(JmsConstants.USERNAME, "njams");
        FILLEDPROPS.put(JmsConstants.PASSWORD, "njams");
        FILLEDPROPS.put(JmsConstants.DESTINATION, "njams4.sdk.test");
    }

    @Before
    public void createNewJmsReceiverImpl() {
        impl = new JmsReceiverMock();
    }

    @After
    public void tearDown() {
        impl.stop();
    }

    //connect tests
    *//**
     * This method checks if the method connect() connects correctly without a
     * real existing topic.
     *//*
    @Test
    public void testConnectWithPropertiesButWithMissingRealDestination() {
        JmsReceiverMock.testBeforeInit(impl);
        impl.init(FILLEDPROPS);
        JmsReceiverMock.testAfterInit(impl, FILLEDPROPS);
        impl.connect();
        JmsReceiverMock.testAfterConnect(impl);
    }

    *//**
     * This method checks if the method connect() connects correctly with a real
     * exisiting topic.
     *//*
    @Test
    public void testConnectWithPropertiesWithRealDestination() {
        Properties props = new Properties();
        props.put(CommunicationFactory.COMMUNICATION, "JMS");
        props.put(JmsConstants.INITIAL_CONTEXT_FACTORY,
                "com.tibco.tibjms.naming.TibjmsInitialContextFactory");
        props.put(JmsConstants.SECURITY_PRINCIPAL, "njams");
        props.put(JmsConstants.SECURITY_CREDENTIALS, "njams");
        props.put(JmsConstants.PROVIDER_URL, "tibjmsnaming://vslems01:7222");
        props.put(JmsConstants.CONNECTION_FACTORY, "ConnectionFactory");
        props.put(JmsConstants.USERNAME, "njams");
        props.put(JmsConstants.PASSWORD, "njams");
        props.put(JmsConstants.DESTINATION, "njams4.dev.kai");
        JmsReceiverMock.testBeforeInit(impl);
        impl.init(props);
        JmsReceiverMock.testAfterInit(impl, props);
        impl.connect();
        JmsReceiverMock.testAfterConnect(impl);
    }

    //stop tests
    *//**
     * This method tests if the connection stops normally if the connection has
     * been established before.
     *//*
    @Test
    public void testStopNormally() {
        JmsReceiverMock.testBeforeInit(impl);
        impl.init(FILLEDPROPS);
        JmsReceiverMock.testAfterInit(impl, FILLEDPROPS);
        impl.connect();
        JmsReceiverMock.testAfterConnect(impl);
        impl.stop();
        JmsReceiverMock.testAfterInit(impl, FILLEDPROPS);
    }
    
    //onMessage tests
    
    *//**
     * This method tests if onMessage delivers messages if there is a connection 
     * established. In this test case a temporary queue will be created where 
     * the message will be sent to.
     * 
     * @throws JMSException isn't thrown
     * @throws JsonProcessingException isn't thrown
     *//*
    @Test
    public void testOnMessageWithJsonContentAndValidInstruction() throws JMSException, JsonProcessingException {
        impl.init(FILLEDPROPS);
        impl.connect();
        TextMessage msg = mock(TextMessage.class);
        when(msg.getStringProperty("NJAMS_CONTENT")).thenReturn("json");
        
        Instruction inst = new Instruction();
        Request req = new Request();
        req.setCommand("SendProjectmessage");
        inst.setRequest(req);
        ObjectMapper mapper = impl.getMapper();
        String writeValueAsString = mapper.writeValueAsString(inst);
        
        when(msg.getText()).thenReturn(writeValueAsString);
        Session session = impl.getSession();
        TemporaryQueue tempQueue = session.createTemporaryQueue();
        LOG.info("The message will be sent to: " + tempQueue.getQueueName());
        //Check if it has been created
        when(msg.getJMSReplyTo()).thenReturn(tempQueue);
        when(msg.getJMSCorrelationID()).thenReturn("TestCorrelationJmsReceiverIT");
        impl.onMessage(msg);
    }*/
}