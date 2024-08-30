package com.Orio.gbp_tool.service;

import java.util.List;

import com.Orio.gbp_tool.model.FileEntity;
import com.Orio.gbp_tool.model.PDFText;

public interface ITextProcessorService {

    List<PDFText> createText(FileEntity file);
}
