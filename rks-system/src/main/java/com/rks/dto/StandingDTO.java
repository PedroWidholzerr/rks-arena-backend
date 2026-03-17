package com.rks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandingDTO {
    private String id;
    private String championshipId;
    private String teamId;
    private Integer wins;
    private Integer losses;
    private Integer points;
}
