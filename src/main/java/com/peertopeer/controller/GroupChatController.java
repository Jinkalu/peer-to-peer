package com.peertopeer.controller;

import com.peertopeer.vo.GroupVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.vo.GroupCreationVO;
import org.springframework.http.MediaType;
import com.peertopeer.service.GroupChatService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;

    @PostMapping("/create")
    public GroupVO createGroup(@RequestBody GroupCreationVO requestBody) {
        return groupChatService.createGroup(requestBody);
    }

    @PostMapping(value = "/set-group-icon/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload group avatar", description = "Upload a single image file as group avatar")
    @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    @ApiResponse(responseCode = "500", description = "Server error")
    public Map<String, String> uploadGroupAvatar(
            @Parameter(description = "Group ID", required = true, example = "123")
            @PathVariable Long id,
            @Parameter(description = "Avatar image file (JPG, PNG, GIF)", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam(name = "file", required = false) MultipartFile file) {
        return Map.of("avatarUrl", groupChatService.setGroupIcon(id, file));
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
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return groupChatService.membersList(conversationId, page, size);
    }

}
