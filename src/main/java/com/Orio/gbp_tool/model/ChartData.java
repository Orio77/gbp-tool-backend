package com.Orio.gbp_tool.model;

import java.util.List;
import java.util.Map;

import com.Orio.gbp_tool.config.converter.MapConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ChartData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Convert(converter = MapConverter.class)
    @Column(length = 100000000)
    private Map<String, List<SimilarityScore>> data;
    private String label;
}
