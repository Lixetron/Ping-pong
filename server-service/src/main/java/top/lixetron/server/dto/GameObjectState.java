package top.lixetron.server.dto;

import lombok.Data;
import org.springframework.data.geo.Point;
import top.lixetron.server.enums.GameObjectType;

@Data
public class GameObjectState {
    private GameObjectType type;
    private Point position;
    private String clientId;
}
