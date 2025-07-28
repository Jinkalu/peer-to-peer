package com.peertopeer.controller;

import com.peertopeer.entity.Message;
import com.peertopeer.service.ChatService;
import com.peertopeer.vo.GroupVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.vo.GroupCreationVO;

import com.peertopeer.service.GroupChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final ChatService chatService;

    @PostMapping("/create")
    public GroupVO createGroup(@RequestBody GroupCreationVO requestBody) {
        return groupChatService.createGroup(requestBody);
    }

    @PostMapping("/add-member")
    public void addMember(@RequestBody GroupCreationVO requestBody) {
        groupChatService.addMember(requestBody);
    }

    @PostMapping("/remove-member")
    public void removeMember(@RequestParam Long conversationId,
                             @RequestParam Long memberId) {
        groupChatService.removeMember(conversationId, memberId);
    }

    @GetMapping("/list-members/{conversationId}")
    public GroupVO membersList(@PathVariable Long conversationId,
                               @RequestParam int page,
                               @RequestParam int size) {
        return groupChatService.membersList(conversationId, page, size);
    }

}
