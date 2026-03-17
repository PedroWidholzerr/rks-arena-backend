package com.rks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampionshipDTO {
    private String id;
    private String name;
    private String description;
    private String seasonId;
    private String status;
}
