package com.Orio.gbp_tool.model;

import java.util.List;

import lombok.Data;

@Data
public class ChartDataResult {
    private ChartData chartData;
    private List<String> pdfsNotFound;

}
