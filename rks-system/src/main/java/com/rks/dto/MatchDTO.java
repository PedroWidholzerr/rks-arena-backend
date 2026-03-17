package com.rks.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDTO {
    private String id;
    private String championshipId;
    private String teamAId;
    private String teamBId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'")
    private LocalDateTime scheduledAt;
    private Integer scoreA;
    private Integer scoreB;
    private String comment;
    private String mvpPlayerId;
}
