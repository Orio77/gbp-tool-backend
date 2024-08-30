package com.Orio.gbp_tool.repository;

import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.Orio.gbp_tool.exception.ChartNotFoundException;
import com.Orio.gbp_tool.exception.FileAlreadyInTheDatabaseException;
import com.Orio.gbp_tool.exception.FileDataReadingException;
import com.Orio.gbp_tool.model.ChartData;
import com.Orio.gbp_tool.model.FileEntity;
import com.Orio.gbp_tool.model.TextSearchResult;

public interface ISQLRepo {

    void saveFile(MultipartFile file, String title) throws FileDataReadingException, FileAlreadyInTheDatabaseException;

    FileEntity getFile(String title) throws FileNotFoundException;

    void removeFile(String title) throws FileNotFoundException;

    List<FileEntity> getTexts() throws Exception;

    TextSearchResult getTexts(List<String> names);

    FileEntity getText(String title) throws FileNotFoundException;

    void saveChart(ChartData data);

    void removeChart(String label) throws ChartNotFoundException;

    ChartData getChart(String label) throws ChartNotFoundException;
}
