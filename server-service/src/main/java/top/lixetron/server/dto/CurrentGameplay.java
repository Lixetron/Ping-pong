package top.lixetron.server.dto;

import lombok.Data;
import org.springframework.data.geo.Point;

import java.util.Map;

@Data
public class CurrentGameplay {
    private Map<String, Point> playersPosition;
    private Point ballPosition;
}
