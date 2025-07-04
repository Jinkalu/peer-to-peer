package com.peertopeer.service;

import com.peertopeer.entity.Message;

import java.util.List;


public interface ChatService {

    List<Message> getChatHistory(String sender, String receiver);

    void markMessagesAsSeen(String sender, String receiver);
}
