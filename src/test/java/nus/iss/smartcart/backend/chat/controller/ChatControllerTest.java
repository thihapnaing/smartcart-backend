package nus.iss.smartcart.backend.chat.controller;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.chat.dto.ChatRequest;
import nus.iss.smartcart.backend.chat.dto.ChatResponse;
import nus.iss.smartcart.backend.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The controller is a thin delegate to ChatService - these tests check it wires request data
 * to the service correctly (including the null-userId case, since userId is an optional query
 * param) rather than re-testing ChatService's own logic, which PythonAiChatServiceTest covers.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Test
    void startSession_passesUserIdThroughAndReturnsServiceResponse() {
        ChatController controller = new ChatController(chatService);
        ChatResponse expected = new ChatResponse();
        expected.setSessionId("session-1");
        when(chatService.startSession(7L)).thenReturn(expected);

        ChatResponse actual = controller.startSession(7L);

        assertSame(expected, actual);
        verify(chatService).startSession(7L);
    }

    @Test
    void startSession_allowsNullUserIdForAnonymousChats() {
        ChatController controller = new ChatController(chatService);
        ChatResponse expected = new ChatResponse();
        expected.setSessionId("session-anon");
        when(chatService.startSession(null)).thenReturn(expected);

        ChatResponse actual = controller.startSession(null);

        assertSame(expected, actual);
        verify(chatService).startSession(null);
    }

    @Test
    void sendMessage_passesSessionIdAndMessageBodyThroughToService() {
        ChatController controller = new ChatController(chatService);
        ChatRequest request = new ChatRequest();
        request.setMessage("Show me new arrivals");
        ChatResponse expected = new ChatResponse();
        expected.setReply("Here you go");
        when(chatService.handleMessage("session-1", "Show me new arrivals")).thenReturn(expected);

        ChatResponse actual = controller.sendMessage("session-1", request);

        assertEquals("Here you go", actual.getReply());
        verify(chatService).handleMessage("session-1", "Show me new arrivals");
    }
}
