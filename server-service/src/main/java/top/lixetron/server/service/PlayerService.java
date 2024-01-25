package top.lixetron.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lixetron.server.model.Player;
import top.lixetron.server.repository.PlayerRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public Optional<Player> findByName(String name) {
        return playerRepository.findFirstByName(name);
    }
}
