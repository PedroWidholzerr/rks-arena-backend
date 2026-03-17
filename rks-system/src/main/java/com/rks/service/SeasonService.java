package com.rks.service;

import com.rks.dto.SeasonDTO;
import com.rks.entity.Season;
import com.rks.repository.SeasonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeasonService {
    @Autowired
    private SeasonRepository seasonRepository;

    public List<SeasonDTO> getAllSeasons() {
        return seasonRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public SeasonDTO getSeasonById(String id) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Season not found"));
        return convertToDTO(season);
    }

    public SeasonDTO createSeason(SeasonDTO dto) {
        Season season = Season.builder()
                .name(dto.getName())
                .year(dto.getYear())
                .build();
        season = seasonRepository.save(season);
        return convertToDTO(season);
    }

    public SeasonDTO updateSeason(String id, SeasonDTO dto) {
        Season season = seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Season not found"));
        season.setName(dto.getName());
        season.setYear(dto.getYear());
        season = seasonRepository.save(season);
        return convertToDTO(season);
    }

    public void deleteSeason(String id) {
        seasonRepository.deleteById(id);
    }

    private SeasonDTO convertToDTO(Season season) {
        return SeasonDTO.builder()
                .id(season.getId())
                .name(season.getName())
                .year(season.getYear())
                .build();
    }
}
