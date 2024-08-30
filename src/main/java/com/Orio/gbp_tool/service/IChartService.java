package com.Orio.gbp_tool.service;

import java.util.List;

import com.Orio.gbp_tool.exception.NoPdfFoundException;
import com.Orio.gbp_tool.model.ChartDataResult;

public interface IChartService {

    ChartDataResult createChart(List<String> concepts, List<String> pdfNames, String label) throws NoPdfFoundException;
}
