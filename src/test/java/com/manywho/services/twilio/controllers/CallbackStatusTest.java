package com.manywho.services.twilio.controllers;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URISyntaxException;
import static org.junit.Assert.assertEquals;
import com.manywho.services.test.HttpClientForTest;
import com.manywho.services.test.FlowResponseMock;
import com.manywho.services.test.TwilioServiceFunctionalTest;
import com.manywho.services.twilio.managers.CacheManager;
import com.mashape.unirest.http.Unirest;
import org.json.JSONException;
import org.junit.Test;

public class CallbackStatusTest extends TwilioServiceFunctionalTest {
    @Test
    public void testMessageCallbackStatusSent() throws URISyntaxException, IOException, JSONException {

        MultivaluedMap<String,Object> headers = defaultHeadersFromTwilio();

        final Form form = new Form();
        form.param("AccountSid", "mockAppSid");
        form.param("ApiVersion", "2010-04-01");
        form.param("From", "+440123456789");
        form.param("MessageSid", "SMd931e01d8ce64158b8c962c6a1b24e5c");
        form.param("MessageStatus", "sent");
        form.param("SmsSid", "SMd931e01d8ce64158b8c962c6a1b24e5c");
        form.param("SmsStatus", "sent");
        form.param("To", "+441234567899");

        //HttpClientForTest httpClientMock = new HttpClientForTest();
        Unirest.setHttpClient(mockHttpClient);

        // I will do to calls to the flow to know the status
        FlowResponseMock httpResponse = new FlowResponseMock();
        mockHttpClient.addResponse(httpResponse);

        FlowResponseMock httpResponse2 = new FlowResponseMock();
        mockHttpClient.addResponse(httpResponse2);

        FlowResponseMock httpResponse3 = new FlowResponseMock(
                FlowResponseMock.getFullListHeaders(), "HTTP", 1, 1, 200, "ok","Content-Type: application/json; charset=utf-8",
                getJsonFormatFileContent("CallbackStatusTest/MessageCallbackStatus/join.json")
        );
        mockHttpClient.addResponse(httpResponse3);


        Entity entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        String redisKey =  String.format(
                CacheManager.REDIS_KEY_MESSAGES,
                "mockAppSid",
                "SMd931e01d8ce64158b8c962c6a1b24e5c"
        );

        mockJedis.set(
                redisKey,
                getJsonFormatFileContent("CallbackStatusTest/callbackstatus1-redis.json")
        );

        // this call is coming from Twilio
        Response responseMsg = target("/callback/status/message")
                .request()
                .headers(headers)
                .post(entity);

        assertJsonSame(
                getJsonFormatFileContent("CallbackStatusTest/MessageCallbackStatus/forward-flow-call.json"),
                mockHttpClient.getExpectedRequestBody(0)
        );

        assertJsonSame(
                getJsonFormatFileContent("CallbackStatusTest/MessageCallbackStatus/wait-message-flow-call.json"),
                mockHttpClient.getExpectedRequestBody(1)
        );

        // headers used to call the flow in first call
        checkHeaders(mockHttpClient, 0);
        checkHeaders(mockHttpClient, 1);

        assertEquals(3, mockHttpClient.getResponsesHistory().size());
        assertEquals(204, responseMsg.getStatus());
        assertEquals("", responseMsg.readEntity(String.class));
    }

    @Test
    public void testMessageCallbackStatusDelivered() throws URISyntaxException, IOException, JSONException {

        MultivaluedMap<String,Object> headers = defaultHeadersFromTwilio();

        final Form form = new Form();
        form.param("AccountSid", "mockAppSid");
        form.param("ApiVersion", "2010-04-01");
        form.param("From", "+440123456789");
        form.param("MessageSid", "SMd931e01d8ce64158b8c962c6a1b24e5c");
        form.param("MessageStatus", "delivered");
        form.param("SmsSid", "SMd931e01d8ce64158b8c962c6a1b24e5c");
        form.param("SmsStatus", "delivered");
        form.param("To", "+441234567899");

        HttpClientForTest httpClientMock = new HttpClientForTest();
        Unirest.setHttpClient(httpClientMock);

        Entity entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        // this call is coming from Twilio, and we don't process it because is not consistent for all the carriers
        Response responseMsg = target("/callback/status/message")
                .request()
                .headers(headers)
                .post(entity);

        assertEquals(204, responseMsg.getStatus());
        assertEquals("", responseMsg.readEntity(String.class));
    }

    @Test
    public void testMessageCallbackStatusWithReply() throws URISyntaxException, IOException, JSONException {

        MultivaluedMap<String,Object> headers = defaultHeadersFromTwilio();

        final Form form = new Form();
        form.param("AccountSid", "mockAppSid");
        form.param("ApiVersion", "2010-04-01");
        form.param("Body","Hi");
        form.param("From", "+44123456788");
        form.param("FromCity","");
        form.param("FromCountry","GB");
        form.param("FromState","");
        form.param("FromZip","");
        form.param("MessageSid", "SMd931e01d8ce64158b8c962c6a1b24e5c");
        form.param("NumMedia","0");
        form.param("NumSegments","1");
        form.param("SmsMessageSid","SM16a2ee26ba827371462e6de51c2cdca6");
        form.param("SmsSid", "SM16a2ee26ba827371462e6de51c2cdca6");
        form.param("SmsStatus", "received");
        form.param("To", "+44123456789");
        form.param("ToCity","");
        form.param("ToCountry","GB");
        form.param("ToState","Ely");
        form.param("ToZip","");

        Entity entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        String redisKey =  String.format(
                CacheManager.REDIS_KEY_MESSAGES,
                "mockAppSid",
                "+44123456789+44123456788"
        );

        mockJedis.set(
                redisKey,
                getJsonFormatFileContent("CallbackStatusTest/callbackstatus1-redis.json")
        );

        HttpClientForTest httpClientMock = new HttpClientForTest();
        Unirest.setHttpClient(httpClientMock);

        // I will do to calls to the flow to know the status
        FlowResponseMock httpResponse = new FlowResponseMock();
        httpClientMock.addResponse(httpResponse);

        Response responseMsg = target("/callback/status/message")
                .request()
                .headers(headers)
                .post(entity);

        assertJsonSame(
                getJsonFormatFileContent("CallbackStatusTest/MessageReplyCallbackStatus/user-replay-flow-call.json"),
                httpClientMock.getExpectedRequestBody(0)
        );

        checkHeaders(httpClientMock, 0);

        assertEquals(204, responseMsg.getStatus());
        assertEquals("", responseMsg.readEntity(String.class));
    }



    @Test
    public void testMessageCallbackStatusLocalWithReply() throws URISyntaxException, IOException, JSONException {

        MultivaluedMap<String,Object> headers = defaultHeadersFromTwilio();

        final Form form = new Form();
        form.param("AccountSid", "mockAppSid");
        form.param("ApiVersion", "2010-04-01");
        form.param("Body","Hi");
        form.param("From", "123456788");
        form.param("FromCity","");
        form.param("FromCountry","US");
        form.param("FromState","");
        form.param("FromZip","");
        form.param("MessageSid", "SMd931e01d8ce64158b8c962c6a1b24e5c");
        form.param("NumMedia","0");
        form.param("NumSegments","1");
        form.param("SmsMessageSid","SM16a2ee26ba827371462e6de51c2cdca6");
        form.param("SmsSid", "SM16a2ee26ba827371462e6de51c2cdca6");
        form.param("SmsStatus", "received");
        form.param("To", "123456789");
        form.param("ToCity","PHILADELPHIA");
        form.param("ToCountry","US");
        form.param("ToState","PA");
        form.param("ToZip","");

        Entity entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        String redisKey =  String.format(
                CacheManager.REDIS_KEY_MESSAGES,
                "mockAppSid",
                "+1123456789+1123456788"
        );

        mockJedis.set(
                redisKey,
                getJsonFormatFileContent("CallbackStatusTest/MessageCallbackStatusLocalWithReply/callbackstatus1-redis.json")
        );

        HttpClientForTest httpClientMock = new HttpClientForTest();
        Unirest.setHttpClient(httpClientMock);

        // I will do to calls to the flow to know the status
        FlowResponseMock httpResponse = new FlowResponseMock();
        httpClientMock.addResponse(httpResponse);

        Response responseMsg = target("/callback/status/message")
                .request()
                .headers(headers)
                .post(entity);

        assertJsonSame(
                getJsonFormatFileContent("CallbackStatusTest/MessageCallbackStatusLocalWithReply/user-replay-flow-call.json"),
                httpClientMock.getExpectedRequestBody(0)
        );

        checkHeaders(httpClientMock, 0);

        assertEquals(204, responseMsg.getStatus());
        assertEquals("", responseMsg.readEntity(String.class));
    }

    private void checkHeaders(HttpClientForTest httpClientMock, Integer index) {
        // headers used to call the flow
        assertEquals(null, httpClientMock.getExpectedRequestHeader(index, "Authorization").getValue());
        assertEquals("bbc6f524-c83a-11e6-9d9d-cec0c932ce01", httpClientMock.getExpectedRequestHeader(index, "ManyWhoTenant").getValue());
        assertEquals("gzip", httpClientMock.getExpectedRequestHeader(index, "accept-encoding").getValue());
        assertEquals("application/json", httpClientMock.getExpectedRequestHeader(index, "Content-Type").getValue());
    }

    private MultivaluedMap<String,Object> defaultHeadersFromTwilio() {
        MultivaluedMap<String,Object> headers = new MultivaluedHashMap<>();
        headers.add("Accept", "*/*");
        headers.add("Accept-Encoding", "gzip,deflate");
        headers.add("Cache-Control", "max-age=259200");
        headers.add("Connection", "close");
        headers.add("Content-Length", "229");
        headers.add("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        headers.add("User-Agent", "TwilioProxy/1.1");
        headers.add("X-Forwarded-For", "54.165.71.167");
        headers.add("X-Forwarded-Proto", "https");
        headers.add("X-Twilio-Signature","yNFtLun3RciDiywtxuGTZSTRkoY=");

        return headers;
    }
}
