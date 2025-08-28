package com.peertopeer.script;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Users;
import com.peertopeer.enums.ConversationType;
import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;

@Configuration
public class ScriptRunner {

    private final UserRepository userRepository;
    private final ConversationsRepository conversationsRepository;
    private final PasswordEncoder encoder;

    public ScriptRunner(UserRepository userRepository,
                        ConversationsRepository conversationsRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.conversationsRepository = conversationsRepository;
        this.encoder = encoder;
    }


    @Bean
    public CommandLineRunner runner() {
        return args -> {
            userRepository.saveAllAndFlush(List.of(Users.builder()
                    .username("u1")
                    .password(encoder.encode("12345"))
                    .build(), Users.builder()
                    .username("u2")
                    .password(encoder.encode("12345"))
                    .build(), Users.builder()
                    .username("u3")
                    .password(encoder.encode("12345"))
                    .build(), Users.builder()
                    .username("u4")
                    .password(encoder.encode("12345"))
                    .build()));

            conversationsRepository.saveAllAndFlush(List.of(Conversations.builder()
                    .users(new HashSet<>(userRepository.findAllById(List.of(1L, 2L))))
                    .type(ConversationType.PRIVATE_CHAT)
                    .build(), Conversations.builder()
                    .users(new HashSet<>(userRepository.findAllById(List.of(1L, 3L))))
                    .type(ConversationType.PRIVATE_CHAT)
                    .build(), Conversations.builder()
                    .users(new HashSet<>(userRepository.findAllById(List.of(1L, 4L))))
                    .type(ConversationType.PRIVATE_CHAT)
                    .build(), Conversations.builder()
                    .users(new HashSet<>(userRepository.findAllById(List.of(2L, 3L))))
                    .type(ConversationType.PRIVATE_CHAT)
                    .build(), Conversations.builder()
                    .users(new HashSet<>(userRepository.findAllById(List.of(2L, 4L))))
                    .type(ConversationType.PRIVATE_CHAT)
                    .build(), Conversations.builder()
                    .users(new HashSet<>(userRepository.findAllById(List.of(3L, 4L))))
                    .type(ConversationType.PRIVATE_CHAT)
                    .build()));
        };
    }
}
