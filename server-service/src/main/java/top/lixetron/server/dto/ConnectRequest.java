package top.lixetron.server.dto;

import lombok.Data;
import top.lixetron.server.model.Player;

@Data
public class ConnectRequest {
    private Player player;
    private String gameUid;
}
