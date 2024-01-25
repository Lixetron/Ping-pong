package top.lixetron.server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.geo.Point;
import top.lixetron.server.enums.BallAbscissaDirection;
import top.lixetron.server.enums.BallOrdinateDirection;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "games")
public class Game extends BaseEntity {

    private String uid;

    @Transient
    private Point ballPosition;
    @Transient
    private BallAbscissaDirection ballAbscissaDirection;
    @Transient
    private BallOrdinateDirection ballOrdinateDirection;
    @Transient
    private double ballOrdinateDirectionPower;

    @Transient
    private Point score;

    @Transient
    private Map<String, Point> playersPosition;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GameStatusEnum status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "winner", referencedColumnName = "id")
    private Player winner;

    @CreatedBy
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "first_player_id", referencedColumnName = "id")
    private Player firstPlayer;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "second_player_id", referencedColumnName = "id")
    private Player secondPlayer;
}
