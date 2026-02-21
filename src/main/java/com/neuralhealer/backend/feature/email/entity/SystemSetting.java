ackage com.neuralhealer.backend.feature.email.entity.SystemSetting;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String key;

    @Column(name = "setting_value", columnDefinition = "jsonb")
    private String value;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_public")
    private Boolean isPublic;
}
