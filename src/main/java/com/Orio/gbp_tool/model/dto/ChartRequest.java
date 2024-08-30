package com.Orio.gbp_tool.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class ChartRequest {
    private List<String> concepts;
    private List<String> pdfs;
    private String label;
}
